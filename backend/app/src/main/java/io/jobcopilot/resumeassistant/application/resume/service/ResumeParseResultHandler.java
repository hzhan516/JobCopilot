package io.jobcopilot.resumeassistant.application.resume.service;

import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeGroup;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeVersion;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import io.jobcopilot.resumeassistant.domain.resume.service.VectorGenerationService;
import io.jobcopilot.resumeassistant.domain.resume.valueobject.ParseStatus;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.AiResultEvent;
import io.jobcopilot.resumeassistant.domain.shared.exception.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Processes async AI parse results for resume versions and propagates
 * parsed content to derived versions.
 * 处理简历版本的异步 AI 解析结果，并将解析内容传播到衍生版本。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeParseResultHandler {

    private final ResumeVersionRepository versionRepository;
    private final ResumeGroupRepository groupRepository;
    private final VectorGenerationService vectorGenerationService;

    @Transactional
    public void handle(AiResultEvent event) {
        ResumeVersion originalVersion = versionRepository.findById(UUID.fromString(event.referenceId()))
                .orElseThrow(() -> new StorageException("version.not.found"));

        if (!"COMPLETED".equals(event.status())) {
            originalVersion.markParseFailed(event.errorMessage() != null ? event.errorMessage() : "Unknown error");
            versionRepository.save(originalVersion);
            return;
        }

        try {
            String parsedJsonStr = extractParsedContent(event);
            originalVersion.markParseCompleted(parsedJsonStr);
            versionRepository.save(originalVersion);

            propagateParsedContent(originalVersion, parsedJsonStr);
            vectorGenerationService.generateForResume(originalVersion.getId(), parsedJsonStr);

            log.info("Resume parsing completed for versionId={}", originalVersion.getId());
        } catch (Exception e) {
            log.error("Failed to process parsed data for versionId={}", originalVersion.getId(), e);
            originalVersion.markParseFailed("Failed to handle parsed result: " + e.getMessage());
            versionRepository.save(originalVersion);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractParsedContent(AiResultEvent event) {
        java.util.Map<String, Object> data = event.data();
        Object parsedContentObj = data != null ? data.get("parsedContent") : null;
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(parsedContentObj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize parsed content", e);
        }
    }

    private void propagateParsedContent(ResumeVersion originalVersion, String parsedJsonStr) {
        ResumeGroup group = groupRepository.findById(originalVersion.getGroupId()).orElse(null);
        if (group == null) return;

        for (ResumeVersion v : group.getVersions()) {
            if (v.getVersionType() != ResumeVersion.VersionType.ORIGINAL
                    && v.getParseStatus() != ParseStatus.COMPLETED) {
                v.markParseCompleted(parsedJsonStr);
                versionRepository.save(v);
                log.info("Copied parsed content to derived version: versionId={}", v.getId());
            }
        }
    }
}
