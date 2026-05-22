package io.jobcopilot.resumeassistant.application.conversation.service;

import io.jobcopilot.resumeassistant.api.conversation.port.ConversationStreamPort;
import io.jobcopilot.resumeassistant.api.embedding.facade.VectorFacade;
import io.jobcopilot.resumeassistant.application.conversation.command.CreateConversationCommand;
import io.jobcopilot.resumeassistant.application.conversation.command.SendMessageCommand;
import io.jobcopilot.resumeassistant.domain.conversation.entity.Conversation;
import io.jobcopilot.resumeassistant.domain.conversation.entity.Message;
import io.jobcopilot.resumeassistant.domain.conversation.exception.ConversationException;
import io.jobcopilot.resumeassistant.domain.conversation.repository.ConversationRepository;
import io.jobcopilot.resumeassistant.domain.conversation.valueobject.MessageRole;
import io.jobcopilot.resumeassistant.domain.job.entity.Job;
import io.jobcopilot.resumeassistant.domain.job.repository.JobRepository;
import io.jobcopilot.resumeassistant.domain.job.valueobject.JobStatus;
import io.jobcopilot.resumeassistant.domain.job.valueobject.ParsedJobContent;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeGroup;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeVersion;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.ConversationRequestCommand;
import io.jobcopilot.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import io.jobcopilot.resumeassistant.domain.shared.service.FileStorageService;
import io.jobcopilot.resumeassistant.domain.shared.service.MessageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;

/**
 * Orchestrates AI-assisted conversations by binding resume versions and job postings as static context,
 * managing a conversational working copy of the resume, and dispatching requests to the AI service via MQ.
 * 编排 AI 辅助对话，将简历版本和职位信息绑定为静态上下文，管理简历的对话工作副本，并通过 MQ 向 AI 服务分发请求
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ConversationApplicationService {

    private final ConversationRepository conversationRepository;
    private final JobRepository jobRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final ResumeGroupRepository resumeGroupRepository;
    private final AiMessagePublisherPort aiMessagePublisherPort;
    private final VectorFacade vectorFacade;
    private final FileStorageService fileStorageService;
    private final MessageProvider messageProvider;
    private final ConversationStreamPort streamPort;

    /**
     * Creates a conversation anchored to both a resume version and a job posting.
     * Both must be provided so the AI can ground its advice in the specific match context.
     * 创建绑定到简历版本和职位的对话。两者必须同时提供，以便 AI 基于具体的匹配场景给出建议
     *
     * @param command Create conversation command / 创建对话命令
     * @return Created conversation / 创建后的对话
     */
    @Transactional
    public Conversation createConversation(CreateConversationCommand command) {
        log.info("Creating new conversation for user: {}", command.userId());

        // Both resume and job are required so the AI copilot has full context for targeted optimization advice
        // 必须同时提供简历和职位，确保 AI 助手拥有完整的上下文以给出针对性的优化建议
        if (command.resumeVersionId() != null && command.jobId() == null) {
            throw new ConversationException("conversation.jobId.required");
        }
        if (command.jobId() != null && command.resumeVersionId() == null) {
            throw new ConversationException("conversation.resumeVersionId.required");
        }

        if (command.resumeVersionId() != null) {
            resumeVersionRepository.findById(command.resumeVersionId())
                    .orElseThrow(() -> new ConversationException("version.not.found"));
        }

        if (command.jobId() != null) {
            Job job = jobRepository.findById(command.jobId().toString())
                    .orElseThrow(() -> new ConversationException("job.not.found"));
            if (!job.getUserId().equals(command.userId())) {
                throw new ConversationException("access.denied");
            }
        }

        Conversation conversation = Conversation.create(
                command.userId(),
                command.title(),
                command.resumeVersionId(),
                command.jobId()
        );
        conversation = conversationRepository.save(conversation);

        String preset = messageProvider.getMessage("conversation.preset.match_score");
        conversation.addMessage(MessageRole.USER, preset);
        conversation.autoGenerateTitle(preset);
        conversation = conversationRepository.save(conversation);

        // Load static context (resume + primary job + related jobs) and trigger the first AI request
        // 加载静态上下文（简历 + 主职位 + 相关职位）并触发首次 AI 请求
        sendConversationRequestWithContext(conversation, preset, true);

        return conversation;
    }

    /**
     * Persists the user's message and publishes an async AI request.
     * Detects the first real user message to decide whether to include the full static context.
     * 保存用户消息并发布异步 AI 请求。通过检测第一条真实用户消息来决定是否携带完整静态上下文
     *
     * @param command Send message command / 发送消息命令
     * @return Updated conversation / 更新后的对话
     */
    @Transactional
    public Conversation sendMessage(SendMessageCommand command) {
        log.info("Sending message to conversation: {}", command.conversationId());
        Conversation conversation = getConversationWithOwnershipCheck(command.conversationId(), command.userId());

        conversation.addMessage(command.role(), command.content());
        conversation.autoGenerateTitle(command.content());
        Conversation saved = conversationRepository.save(conversation);

        // Include full static context only when no AI reply exists yet (first turn)
        // 仅在尚无 AI 回复时（首轮）包含完整静态上下文
        boolean isInit = saved.getMessages().stream()
                .noneMatch(m -> m.getRole() == MessageRole.ASSISTANT);
        sendConversationRequestWithContext(saved, command.content(), isInit);

        return saved;
    }

    @Transactional
    public void saveAiReply(UUID conversationId, String content, String fileUrl) {
        saveAiReply(conversationId, content, fileUrl, null);
    }

    /**
     * Saves the AI reply and, when an AI-optimized resume is provided, persists it as a new
     * AI_OPTIMIZED version linked to the conversation so the user can iterate on resume improvements.
     * 保存 AI 回复；当提供 AI 优化后的简历时，将其持久化为关联到对话的新 AI_OPTIMIZED 版本，使用户可迭代改进简历
     *
     * @param conversationId        Conversation ID / 对话 ID
     * @param content               Reply content / 回复内容
     * @param fileUrl               Optional file URL / 可选文件 URL
     * @param aiOptimizedMarkdown   AI-optimized resume Markdown / AI 优化后的简历 Markdown
     */
    @Transactional
    public void saveAiReply(UUID conversationId, String content, String fileUrl, String aiOptimizedMarkdown) {
        log.info("Saving AI reply for conversation: {}", conversationId);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationException("conversation.not.found"));

        // Append the optimized resume to the reply so users see the changes inline without switching views
        // 将优化后的简历追加到回复中，使用户无需切换视图即可看到修改内容
        String finalContent = content;
        if (aiOptimizedMarkdown != null && !aiOptimizedMarkdown.isBlank()
                && conversation.getResumeVersionId() != null) {
            saveOrUpdateAiOptimizedResume(conversation, aiOptimizedMarkdown);
            finalContent = content + "\n\n---\n\n" + aiOptimizedMarkdown;
        }

        conversation.addMessage(MessageRole.ASSISTANT, finalContent, fileUrl);
        conversationRepository.save(conversation);

        log.info("AI reply saved for conversation: {}", conversationId);
    }

    /**
     * Uploads a conversation attachment to object storage and returns a time-limited presigned URL.
     * 上传对话附件到对象存储并返回限时预签名 URL
     *
     * @param conversationId Conversation ID / 对话 ID
     * @param userId         User ID / 用户 ID
     * @param inputStream    File input stream / 文件输入流
     * @param size           File size / 文件大小
     * @param contentType    Content type / 内容类型
     * @param fileName       File name / 文件名
     * @return Presigned access URL / 预签名访问 URL
     */
    @Transactional
    public String uploadAttachment(UUID conversationId, UUID userId, InputStream inputStream, long size, String contentType, String fileName) {
        log.info("Uploading attachment for conversation: {}", conversationId);
        getConversationWithOwnershipCheck(conversationId, userId);

        if (fileName == null || fileName.isBlank()) {
            throw new ConversationException("attachment.filename.required");
        }
        String objectKey = "conversations/" + conversationId + "/" + UUID.randomUUID() + "_" + fileName;
        fileStorageService.upload(objectKey, inputStream, size, contentType);

        String fileUrl = fileStorageService.generatePresignedUrl(objectKey, java.time.Duration.ofDays(7));
        log.info("Attachment uploaded for conversation: {}, url: {}", conversationId, fileUrl);
        return fileUrl;
    }

    @Transactional
    public void closeConversation(UUID conversationId, UUID userId) {
        log.info("Closing conversation: {}", conversationId);
        Conversation conversation = getConversationWithOwnershipCheck(conversationId, userId);
        conversation.close();
        conversationRepository.save(conversation);
    }

    @Transactional
    public void deleteConversation(UUID conversationId, UUID userId) {
        log.info("Deleting conversation: {}", conversationId);
        getConversationWithOwnershipCheck(conversationId, userId);
        conversationRepository.deleteById(conversationId);
    }

    // ==================== 私有辅助方法 ====================
    // ==================== Private helper methods ====================

    private Conversation getConversationWithOwnershipCheck(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationException("conversation.not.found"));

        if (!conversation.isOwnedBy(userId)) {
            throw new ConversationException("access.denied");
        }

        return conversation;
    }

    private List<Map<String, Object>> buildMessageHistory(Conversation conversation) {
        List<Map<String, Object>> history = new ArrayList<>();
        for (Message msg : conversation.getMessages()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("role", msg.getRole().name());
            entry.put("content", msg.getContent());
            if (msg.getFileUrl() != null) {
                entry.put("fileUrl", msg.getFileUrl());
            }
            history.add(entry);
        }
        return history;
    }

    /**
     * Loads static resume and job context, then dispatches the conversation request to the AI service.
     * Prefer the AI working copy so the assistant sees the most recent user-approved modifications.
     * 加载静态简历和职位上下文，然后将对话请求分发到 AI 服务。优先使用 AI 工作副本，使助手看到用户最新认可的修改
     *
     * @param conversation   Conversation entity / 对话实体
     * @param currentMessage Current user message / 当前用户消息
     * @param init           Whether this is the first turn / 是否为首轮对话
     */
    private void sendConversationRequestWithContext(Conversation conversation, String currentMessage, boolean init) {
        String resumeText = null;
        String primaryJobText = null;
        List<String> relatedJobTexts = new ArrayList<>();

        // Prefer the AI working copy because it reflects edits made within the conversation
        // 优先使用 AI 工作副本，因为它反映了对话中进行的编辑
        UUID versionIdToLoad = conversation.getAiOptimizedVersionId() != null
                ? conversation.getAiOptimizedVersionId()
                : conversation.getResumeVersionId();
        if (versionIdToLoad != null) {
            Optional<ResumeVersion> versionOpt = resumeVersionRepository.findById(versionIdToLoad);
            if (versionOpt.isPresent()) {
                ResumeVersion version = versionOpt.get();
                resumeText = version.getContent() != null ? version.getContent() : version.getParsedContent();
            }
        }

        if (conversation.getJobId() != null) {
            Optional<Job> jobOpt = jobRepository.findById(conversation.getJobId().toString());
            if (jobOpt.isPresent()) {
                primaryJobText = buildJobText(jobOpt.get());
            }
        }

        if (init) {
            relatedJobTexts = loadRelatedJobTexts(conversation.getUserId(), conversation.getJobId());
        }

        log.info("Conversation context loaded: resumeTextLength={}, primaryJobTextLength={}, relatedJobsCount={}",
                resumeText != null ? resumeText.length() : 0,
                primaryJobText != null ? primaryJobText.length() : 0,
                relatedJobTexts.size());

        String locale = LocaleContextHolder.getLocale().toLanguageTag();

        ConversationRequestCommand mqCommand = new ConversationRequestCommand(
                conversation.getId().toString(),
                conversation.getUserId().toString(),
                buildMessageHistory(conversation),
                currentMessage,
                new ArrayList<>(),
                conversation.getResumeVersionId() != null ? conversation.getResumeVersionId().toString() : null,
                resumeText,
                primaryJobText,
                relatedJobTexts,
                init,
                locale
        );
        aiMessagePublisherPort.sendConversationRequest(mqCommand);
        log.info("Published conversation request to MQ for conversation: {}, init={}, locale={}",
                conversation.getId(), init, locale);
    }

    private String buildJobText(Job job) {
        ParsedJobContent parsed = job.getParsedContent();
        if (parsed == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (parsed.title() != null) {
            sb.append(parsed.title()).append("\n");
        }
        if (parsed.company() != null) {
            sb.append(parsed.company()).append("\n");
        }
        if (parsed.description() != null) {
            sb.append(parsed.description()).append("\n");
        }
        if (parsed.requirements() != null && !parsed.requirements().isEmpty()) {
            sb.append(String.join("\n", parsed.requirements()));
        }
        return sb.toString().trim();
    }

    private List<String> loadRelatedJobTexts(UUID userId, UUID currentJobId) {
        List<Job> allJobs = jobRepository.findAllByUserId(userId);
        List<String> relatedTexts = new ArrayList<>();
        int count = 0;
        for (Job job : allJobs) {
            if (currentJobId != null && currentJobId.toString().equals(job.getId())) {
                continue;
            }
            if (job.getStatus() == JobStatus.COMPLETED && job.getParsedContent() != null) {
                relatedTexts.add(buildJobText(job));
                count++;
                if (count >= 5) {
                    break;
                }
            }
        }
        return relatedTexts;
    }

    public void completeAiReply(UUID conversationId, String content) {
        streamPort.completeReply(conversationId.toString(), content);
        log.info("Completed AI stream reply for conversation: {}", conversationId);
    }

    public void failAiReply(UUID conversationId, String errorMessage) {
        streamPort.failReply(conversationId.toString(), errorMessage);
        log.warn("Failed AI stream reply for conversation: {}, error: {}", conversationId, errorMessage);
    }

    /**
     * Maintains a per-conversation AI_OPTIMIZED resume working copy.
     * On first modification a new version is created; subsequent edits update the active copy
     * unless it was archived by another conversation, in which case a fresh copy is spun up.
     * 维护每个对话的 AI_OPTIMIZED 简历工作副本。首次修改时创建新版本；后续编辑更新活跃副本，
     * 除非该副本已被其他对话归档，此时重新创建一个新的副本
     *
     * @param conversation       Conversation entity / 对话实体
     * @param markdown           Optimized resume Markdown / 优化后的简历 Markdown
     */
    private void saveOrUpdateAiOptimizedResume(Conversation conversation, String markdown) {
        UUID originalVersionId = conversation.getResumeVersionId();
        ResumeVersion originalVersion = resumeVersionRepository.findById(originalVersionId)
                .orElseThrow(() -> new ConversationException("version.not.found"));

        ResumeGroup group = resumeGroupRepository.findById(originalVersion.getGroupId())
                .orElseThrow(() -> new ConversationException("group.not.found"));

        UUID workingVersionId = conversation.getAiOptimizedVersionId();

        if (workingVersionId == null) {
            // First edit: create a dedicated AI_OPTIMIZED version linked to this conversation
            // 首次编辑：创建专属于该对话的 AI_OPTIMIZED 版本
            ResumeVersion aiVersion = ResumeVersion.createAiOptimized(group.getId(), markdown);
            aiVersion.markParseCompleted(null);
            group.addVersion(aiVersion); // archives the previous active AI_OPTIMIZED version automatically
            resumeGroupRepository.save(group);

            conversation.setAiOptimizedVersionId(aiVersion.getId());
            conversationRepository.save(conversation);

            vectorFacade.generateAndSaveVector(aiVersion.getId().toString(), "RESUME", markdown);
            log.info("Created AI optimized working copy: {} for conversation: {}", aiVersion.getId(), conversation.getId());
        } else {
            ResumeVersion workingVersion = resumeVersionRepository.findById(workingVersionId)
                    .orElseThrow(() -> new ConversationException("version.not.found"));

            if (workingVersion.getStatus() == ResumeVersion.Status.ACTIVE) {
                workingVersion.editContent(markdown);
                resumeVersionRepository.save(workingVersion);
                vectorFacade.generateAndSaveVector(workingVersion.getId().toString(), "RESUME", markdown);
                log.info("Updated AI optimized working copy: {} for conversation: {}", workingVersion.getId(), conversation.getId());
            } else {
                // The working copy was archived by another conversation creating a new AI_OPTIMIZED version
                // 工作副本已被其他对话创建新 AI_OPTIMIZED 版本时归档，需重新创建
                ResumeVersion aiVersion = ResumeVersion.createAiOptimized(group.getId(), markdown);
                aiVersion.markParseCompleted(null);
                group.addVersion(aiVersion);
                resumeGroupRepository.save(group);

                conversation.setAiOptimizedVersionId(aiVersion.getId());
                conversationRepository.save(conversation);

                vectorFacade.generateAndSaveVector(aiVersion.getId().toString(), "RESUME", markdown);
                log.info("Re-created AI optimized working copy: {} for conversation: {}", aiVersion.getId(), conversation.getId());
            }
        }
    }
}
