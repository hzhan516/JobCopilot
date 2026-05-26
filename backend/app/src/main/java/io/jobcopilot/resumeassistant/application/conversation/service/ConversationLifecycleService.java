package io.jobcopilot.resumeassistant.application.conversation.service;

import io.jobcopilot.resumeassistant.application.conversation.command.CreateConversationCommand;
import io.jobcopilot.resumeassistant.domain.conversation.entity.Conversation;
import io.jobcopilot.resumeassistant.domain.conversation.entity.Message;
import io.jobcopilot.resumeassistant.domain.conversation.exception.ConversationException;
import io.jobcopilot.resumeassistant.domain.conversation.repository.ConversationRepository;
import io.jobcopilot.resumeassistant.domain.conversation.valueobject.MessageRole;
import io.jobcopilot.resumeassistant.domain.job.entity.Job;
import io.jobcopilot.resumeassistant.domain.job.repository.JobRepository;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeVersion;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import io.jobcopilot.resumeassistant.domain.shared.service.MessageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Handles the lifecycle of a conversation: creation, ownership verification, closing, and deletion.
 * 处理对话的生命周期：创建、所有权校验、关闭和删除。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationLifecycleService {

    private final ConversationRepository conversationRepository;
    private final JobRepository jobRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final MessageProvider messageProvider;
    private final ConversationContextService contextService;

    @Transactional(timeout = 30)
    public Conversation createConversation(CreateConversationCommand command) {
        log.info("Creating new conversation for user: {}", command.userId());

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

        contextService.deferConversationRequest(conversation, preset, true);
        return conversation;
    }

    @Transactional(timeout = 30)
    public void closeConversation(UUID conversationId, UUID userId) {
        log.info("Closing conversation: {}", conversationId);
        Conversation conversation = getConversationWithOwnershipCheck(conversationId, userId);
        conversation.close();
        conversationRepository.save(conversation);
    }

    @Transactional(timeout = 30)
    public void deleteConversation(UUID conversationId, UUID userId) {
        log.info("Deleting conversation: {}", conversationId);
        getConversationWithOwnershipCheck(conversationId, userId);
        conversationRepository.deleteById(conversationId);
    }

    Conversation getConversationWithOwnershipCheck(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationException("conversation.not.found"));
        if (!conversation.isOwnedBy(userId)) {
            throw new ConversationException("access.denied");
        }
        return conversation;
    }
}
