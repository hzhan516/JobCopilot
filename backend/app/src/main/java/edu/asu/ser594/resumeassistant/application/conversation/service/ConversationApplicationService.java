package edu.asu.ser594.resumeassistant.application.conversation.service;

import edu.asu.ser594.resumeassistant.api.conversation.port.ConversationStreamPort;
import edu.asu.ser594.resumeassistant.application.conversation.command.CreateConversationCommand;
import edu.asu.ser594.resumeassistant.application.conversation.command.SendMessageCommand;
import edu.asu.ser594.resumeassistant.domain.conversation.entity.Conversation;
import edu.asu.ser594.resumeassistant.domain.conversation.entity.Message;
import edu.asu.ser594.resumeassistant.domain.conversation.exception.ConversationException;
import edu.asu.ser594.resumeassistant.domain.conversation.repository.ConversationRepository;
import edu.asu.ser594.resumeassistant.domain.conversation.valueobject.MessageRole;
import edu.asu.ser594.resumeassistant.domain.job.entity.Job;
import edu.asu.ser594.resumeassistant.domain.job.repository.JobRepository;
import edu.asu.ser594.resumeassistant.domain.job.valueobject.JobStatus;
import edu.asu.ser594.resumeassistant.domain.job.valueobject.ParsedJobContent;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeGroup;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeVersion;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.ConversationRequestCommand;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.VectorGenCommand;
import edu.asu.ser594.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import edu.asu.ser594.resumeassistant.domain.shared.service.FileStorageService;
import edu.asu.ser594.resumeassistant.domain.shared.service.MessageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;

/**
 * 对话应用服务
 * Conversation application service
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
    private final FileStorageService fileStorageService;
    private final MessageProvider messageProvider;
    private final ConversationStreamPort streamPort;

    /**
     * 创建对话
     * Create conversation
     */
    @Transactional
    public Conversation createConversation(CreateConversationCommand command) {
        log.info("Creating new conversation for user: {}", command.userId());

        // 新对话必须同时提供简历版本和职位（新业务规则）
        // New conversations must provide both resume version and job
        if (command.resumeVersionId() != null && command.jobId() == null) {
            throw new ConversationException("conversation.jobId.required");
        }
        if (command.jobId() != null && command.resumeVersionId() == null) {
            throw new ConversationException("conversation.resumeVersionId.required");
        }

        // 校验简历版本 / Validate resume version
        if (command.resumeVersionId() != null) {
            resumeVersionRepository.findById(command.resumeVersionId())
                    .orElseThrow(() -> new ConversationException("version.not.found"));
        }

        // 校验职位 / Validate job
        if (command.jobId() != null) {
            Job job = jobRepository.findById(command.jobId().toString())
                    .orElseThrow(() -> new ConversationException("job.not.found"));
            if (!job.getUserId().equals(command.userId())) {
                throw new ConversationException("access.denied");
            }
        }

        // 创建对话 / Create conversation
        Conversation conversation = Conversation.create(
                command.userId(),
                command.title(),
                command.resumeVersionId(),
                command.jobId()
        );
        conversation = conversationRepository.save(conversation);

        // 发送预设消息 / Send preset message
        String preset = messageProvider.getMessage("conversation.preset.match_score");
        conversation.addMessage(MessageRole.USER, preset);
        conversation.autoGenerateTitle(preset);
        conversationRepository.save(conversation);

        // 加载静态上下文并发送首次 AI 请求 / Load static context and send initial AI request
        sendConversationRequestWithContext(conversation, preset, true);

        return conversation;
    }

    /**
     * 发送消息：保存用户消息并异步投递 AI 请求
     * Send message: persist user message and publish async AI request
     */
    @Transactional
    public Conversation sendMessage(SendMessageCommand command) {
        log.info("Sending message to conversation: {}", command.conversationId());
        Conversation conversation = getConversationWithOwnershipCheck(command.conversationId(), command.userId());

        conversation.addMessage(command.role(), command.content());
        conversation.autoGenerateTitle(command.content());
        Conversation saved = conversationRepository.save(conversation);

        // 判断是否为首次真实用户消息（只有预设消息时视为 init）
        // Determine if this is the first real user message (only preset message means init)
        boolean isInit = conversation.getMessages().size() == 2; // preset + this user message
        sendConversationRequestWithContext(conversation, command.content(), isInit);

        return saved;
    }

    /**
     * 保存 AI 回复消息
     * Save AI reply message
     */
    @Transactional
    public void saveAiReply(UUID conversationId, String content, String fileUrl) {
        saveAiReply(conversationId, content, fileUrl, null);
    }

    /**
     * 保存 AI 回复消息（支持 AI 优化简历）
     * Save AI reply message (supports AI optimized resume)
     */
    @Transactional
    public void saveAiReply(UUID conversationId, String content, String fileUrl, String aiOptimizedMarkdown) {
        log.info("Saving AI reply for conversation: {}", conversationId);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationException("conversation.not.found"));
        conversation.addMessage(MessageRole.ASSISTANT, content, fileUrl);
        conversationRepository.save(conversation);

        // 保存 AI 优化版本 / Save AI optimized version
        if (aiOptimizedMarkdown != null && !aiOptimizedMarkdown.isBlank()
                && conversation.getResumeVersionId() != null) {
            saveAiOptimizedResume(conversation.getResumeVersionId(), aiOptimizedMarkdown);
        }

        log.info("AI reply saved for conversation: {}", conversationId);
    }

    /**
     * 上传对话附件到 MinIO 并返回访问 URL
     * Upload conversation attachment to MinIO and return access URL
     */
    @Transactional
    public String uploadAttachment(UUID conversationId, UUID userId, InputStream inputStream, long size, String contentType, String fileName) {
        log.info("Uploading attachment for conversation: {}", conversationId);
        getConversationWithOwnershipCheck(conversationId, userId); // 验证所有权 / Verify ownership

        String objectKey = "conversations/" + conversationId + "/" + UUID.randomUUID() + "_" + fileName;
        fileStorageService.upload(objectKey, inputStream, size, contentType);

        String fileUrl = fileStorageService.generatePresignedUrl(objectKey, java.time.Duration.ofDays(7));
        log.info("Attachment uploaded for conversation: {}, url: {}", conversationId, fileUrl);
        return fileUrl;
    }

    /**
     * 关闭对话
     * Close conversation
     */
    @Transactional
    public void closeConversation(UUID conversationId, UUID userId) {
        log.info("Closing conversation: {}", conversationId);
        Conversation conversation = getConversationWithOwnershipCheck(conversationId, userId);
        conversation.close();
        conversationRepository.save(conversation);
    }

    /**
     * 删除对话
     * Delete conversation
     */
    @Transactional
    public void deleteConversation(UUID conversationId, UUID userId) {
        log.info("Deleting conversation: {}", conversationId);
        // 确保所有权
        // Ensure ownership
        getConversationWithOwnershipCheck(conversationId, userId);
        conversationRepository.deleteById(conversationId);
    }

    // ==================== 私有辅助方法 ====================
    // ==================== Private helper methods ====================

    /**
     * 获取对话并进行所有权校验
     * Get conversation and verify ownership
     */
    private Conversation getConversationWithOwnershipCheck(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationException("conversation.not.found"));

        if (!conversation.isOwnedBy(userId)) {
            throw new ConversationException("access.denied");
        }

        return conversation;
    }

    /**
     * 构建消息历史记录（供 AI 服务使用）
     * Build message history for AI service
     */
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
     * 发送带静态上下文的对话请求
     * Send conversation request with static context
     */
    private void sendConversationRequestWithContext(Conversation conversation, String currentMessage, boolean init) {
        String resumeText = null;
        String primaryJobText = null;
        List<String> relatedJobTexts = new ArrayList<>();

        if (init) {
            // 加载简历文本 / Load resume text
            if (conversation.getResumeVersionId() != null) {
                Optional<ResumeVersion> versionOpt = resumeVersionRepository.findById(conversation.getResumeVersionId());
                if (versionOpt.isPresent()) {
                    ResumeVersion version = versionOpt.get();
                    resumeText = version.getContent() != null ? version.getContent() : version.getParsedContent();
                }
            }

            // 加载当前职位文本 / Load primary job text
            if (conversation.getJobId() != null) {
                Optional<Job> jobOpt = jobRepository.findById(conversation.getJobId().toString());
                if (jobOpt.isPresent()) {
                    primaryJobText = buildJobText(jobOpt.get());
                }
            }

            // 加载相关职位文本 / Load related job texts
            relatedJobTexts = loadRelatedJobTexts(conversation.getUserId(), conversation.getJobId());
        }

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

    /**
     * 构建职位文本描述
     * Build job text description
     */
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

    /**
     * 加载相关职位文本列表
     * Load related job text list
     */
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

    /**
     * 完成 AI 流式回复
     * Complete AI stream reply.
     */
    public void completeAiReply(UUID conversationId, String content) {
        streamPort.completeReply(conversationId.toString(), content);
        log.info("Completed AI stream reply for conversation: {}", conversationId);
    }

    /**
     * 标记 AI 流式回复失败
     * Mark AI stream reply as failed.
     */
    public void failAiReply(UUID conversationId, String errorMessage) {
        streamPort.failReply(conversationId.toString(), errorMessage);
        log.warn("Failed AI stream reply for conversation: {}, error: {}", conversationId, errorMessage);
    }

    /**
     * 保存 AI 优化后的简历版本
     * Save AI optimized resume version
     */
    private void saveAiOptimizedResume(UUID resumeVersionId, String markdown) {
        ResumeVersion version = resumeVersionRepository.findById(resumeVersionId)
                .orElseThrow(() -> new ConversationException("version.not.found"));

        ResumeGroup group = resumeGroupRepository.findById(version.getGroupId())
                .orElseThrow(() -> new ConversationException("group.not.found"));

        ResumeVersion aiVersion = ResumeVersion.createAiOptimized(group.getId(), markdown);
        aiVersion.markParseCompleted(null);
        resumeVersionRepository.save(aiVersion);

        // 触发向量生成 / Trigger vector generation
        VectorGenCommand vectorCmd = new VectorGenCommand(
                aiVersion.getId().toString(),
                "RESUME",
                markdown
        );
        aiMessagePublisherPort.sendTextForVectorGeneration(vectorCmd);
        log.info("Saved AI optimized resume version: {} for group: {}", aiVersion.getId(), group.getId());
    }
}
