package io.jobcopilot.resumeassistant.application.conversation.service;

import io.jobcopilot.resumeassistant.domain.conversation.entity.Conversation;
import io.jobcopilot.resumeassistant.domain.conversation.entity.Message;
import io.jobcopilot.resumeassistant.domain.conversation.valueobject.MessageRole;
import io.jobcopilot.resumeassistant.domain.embedding.port.VectorGenerationPort;
import io.jobcopilot.resumeassistant.domain.job.entity.Job;
import io.jobcopilot.resumeassistant.domain.job.repository.JobRepository;
import io.jobcopilot.resumeassistant.domain.job.valueobject.JobStatus;
import io.jobcopilot.resumeassistant.domain.job.valueobject.ParsedJobContent;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeVersion;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.ConversationRequestCommand;
import io.jobcopilot.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Loads static context (resume + job) and dispatches conversation requests to the AI service via MQ.
 * Also handles deferred side effects (vector generation) after the current transaction commits.
 * 加载静态上下文（简历+职位）并通过 MQ 向 AI 服务分发对话请求。
 * 同时处理当前事务提交后的延迟副作用（向量生成）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationContextService {

    private final ResumeVersionRepository resumeVersionRepository;
    private final JobRepository jobRepository;
    private final AiMessagePublisherPort aiMessagePublisherPort;
    private final VectorGenerationPort vectorGenerationPort;

    void deferConversationRequest(Conversation conversation, String currentMessage, boolean init) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendConversationRequestWithContext(conversation, currentMessage, init);
                }
            });
        } else {
            sendConversationRequestWithContext(conversation, currentMessage, init);
        }
    }

    void deferVectorGeneration(UUID versionId, String markdown) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        vectorGenerationPort.generateAndSaveVector(versionId.toString(), "RESUME", markdown);
                        log.info("Vector generation triggered after commit for version: {}", versionId);
                    } catch (Exception e) {
                        log.error("Vector generation failed after commit for version: {}", versionId, e);
                    }
                }
            });
        } else {
            vectorGenerationPort.generateAndSaveVector(versionId.toString(), "RESUME", markdown);
        }
    }

    private void sendConversationRequestWithContext(Conversation conversation, String currentMessage, boolean init) {
        String resumeText = null;
        String primaryJobText = null;
        List<String> relatedJobTexts = new ArrayList<>();

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

    List<Map<String, Object>> buildMessageHistory(Conversation conversation) {
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
}
