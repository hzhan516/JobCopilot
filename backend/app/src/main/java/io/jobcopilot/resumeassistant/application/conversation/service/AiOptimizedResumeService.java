package io.jobcopilot.resumeassistant.application.conversation.service;

import io.jobcopilot.resumeassistant.domain.conversation.entity.Conversation;
import io.jobcopilot.resumeassistant.domain.conversation.exception.ConversationException;
import io.jobcopilot.resumeassistant.domain.conversation.repository.ConversationRepository;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeGroup;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeVersion;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Maintains the per-conversation AI_OPTIMIZED resume working copy.
 * 维护每个对话的 AI_OPTIMIZED 简历工作副本。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiOptimizedResumeService {

    private final ConversationRepository conversationRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final ResumeGroupRepository resumeGroupRepository;

    @Transactional(timeout = 30)
    public UUID saveOrUpdateAiOptimizedResume(Conversation conversation, String markdown) {
        // Reload to prevent read-modify-write races on concurrent AI replies
        Conversation fresh = conversationRepository.findById(conversation.getId())
                .orElseThrow(() -> new ConversationException("conversation.not.found"));

        UUID originalVersionId = fresh.getResumeVersionId();
        ResumeVersion originalVersion = resumeVersionRepository.findById(originalVersionId)
                .orElseThrow(() -> new ConversationException("version.not.found"));

        ResumeGroup group = resumeGroupRepository.findById(originalVersion.getGroupId())
                .orElseThrow(() -> new ConversationException("group.not.found"));

        UUID workingVersionId = fresh.getAiOptimizedVersionId();

        if (workingVersionId == null) {
            ResumeVersion aiVersion = ResumeVersion.createAiOptimized(group.getId(), markdown);
            aiVersion.markParseCompleted(null);
            group.addVersion(aiVersion);
            resumeGroupRepository.save(group);

            fresh.setAiOptimizedVersionId(aiVersion.getId());
            conversationRepository.save(fresh);

            log.info("Created AI optimized working copy: {} for conversation: {}", aiVersion.getId(), fresh.getId());
            return aiVersion.getId();
        } else {
            ResumeVersion workingVersion = resumeVersionRepository.findById(workingVersionId)
                    .orElseThrow(() -> new ConversationException("version.not.found"));

            if (workingVersion.getStatus() == ResumeVersion.Status.ACTIVE) {
                workingVersion.editContent(markdown);
                resumeVersionRepository.save(workingVersion);
                log.info("Updated AI optimized working copy: {} for conversation: {}", workingVersion.getId(), fresh.getId());
                return workingVersion.getId();
            } else {
                ResumeVersion aiVersion = ResumeVersion.createAiOptimized(group.getId(), markdown);
                aiVersion.markParseCompleted(null);
                group.addVersion(aiVersion);
                resumeGroupRepository.save(group);

                fresh.setAiOptimizedVersionId(aiVersion.getId());
                conversationRepository.save(fresh);

                log.info("Re-created AI optimized working copy: {} for conversation: {}", aiVersion.getId(), fresh.getId());
                return aiVersion.getId();
            }
        }
    }
}
