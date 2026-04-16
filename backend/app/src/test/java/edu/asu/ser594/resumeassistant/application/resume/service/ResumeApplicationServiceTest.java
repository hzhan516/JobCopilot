package edu.asu.ser594.resumeassistant.application.resume.service;

import edu.asu.ser594.resumeassistant.application.resume.command.ResumeEditCommand;
import edu.asu.ser594.resumeassistant.application.resume.command.ResumeUploadCommand;
import edu.asu.ser594.resumeassistant.application.resume.dto.ResumeDownloadResult;
import edu.asu.ser594.resumeassistant.application.resume.query.ResumeDownloadQuery;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeGroup;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeVersion;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import edu.asu.ser594.resumeassistant.domain.resume.valueobject.ParseStatus;
import edu.asu.ser594.resumeassistant.domain.shared.exception.StorageException;
import edu.asu.ser594.resumeassistant.domain.shared.service.DocumentFormatConverter;
import edu.asu.ser594.resumeassistant.domain.shared.service.FileStorageService;
import edu.asu.ser594.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.ResumeParseCommand;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.VectorGenCommand;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.AiResultEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Resume Application Service Tests")
class ResumeApplicationServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID GROUP_ID = UUID.randomUUID();
    private static final UUID VERSION_ID = UUID.randomUUID();

    @Mock
    private ResumeGroupRepository groupRepository;

    @Mock
    private ResumeVersionRepository versionRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private DocumentFormatConverter documentFormatConverter;

    @Mock
    private AiMessagePublisherPort aiMessagePublisherPort;

    @InjectMocks
    private ResumeApplicationService resumeService;

    private ResumeGroup testGroup;
    private ResumeVersion testVersion;
    private InputStream testInputStream;

    @BeforeEach
    void setUp() {
        testInputStream = new ByteArrayInputStream("test content".getBytes());
    }

    @Test
    @DisplayName("Should handle resume upload successfully")
    void shouldHandleResumeUploadSuccessfully() {
        ResumeUploadCommand command = new ResumeUploadCommand(
                "resume.pdf",
                "application/pdf",
                1024L,
                testInputStream,
                "My Resume"
        );

        doNothing().when(groupRepository).save(any(ResumeGroup.class));

        ResumeGroup result = resumeService.handleUpload(command, USER_ID);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getTitle()).isEqualTo("My Resume");
        verify(fileStorageService).upload(anyString(), eq(testInputStream), eq(1024L), eq("application/pdf"));
        verify(groupRepository).save(any(ResumeGroup.class));
        verify(aiMessagePublisherPort).sendResumeForParsing(any(ResumeParseCommand.class));
    }

    @Test
    @DisplayName("Should handle resume edit successfully")
    void shouldHandleResumeEditSuccessfully() {
        testGroup = createTestGroup();
        testVersion = createTestVersion(ResumeVersion.VersionType.CONVERTED);

        ResumeEditCommand command = new ResumeEditCommand(
                VERSION_ID,
                USER_ID,
                "Updated markdown content"
        );

        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(testVersion));
        when(groupRepository.findById(testVersion.getGroupId())).thenReturn(Optional.of(testGroup));
        doNothing().when(versionRepository).save(any(ResumeVersion.class));

        ResumeVersion result = resumeService.handleEdit(command);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Updated markdown content");
        verify(versionRepository).save(testVersion);
    }

    @Test
    @DisplayName("Should process parse result successfully")
    void shouldProcessParseResultSuccessfully() {
        testVersion = createTestVersion(ResumeVersion.VersionType.ORIGINAL);
        when(versionRepository.findById(any())).thenReturn(Optional.of(testVersion));

        AiResultEvent event = new AiResultEvent(
                VERSION_ID.toString(),
                "RESUME_PARSE",
                "COMPLETED",
                Map.of("name", "John Doe"),
                null,
                "RESUME"
        );

        resumeService.handleParseResult(event);

        verify(versionRepository, times(1)).save(testVersion);
        verify(aiMessagePublisherPort).sendTextForVectorGeneration(any(VectorGenCommand.class));
    }

    private ResumeGroup createTestGroup() {
        ResumeGroup group = ResumeGroup.create(USER_ID, "Test Resume");
        return ResumeGroup.reconstruct(
                GROUP_ID, USER_ID, "Test Resume", false,
                group.getCreatedAt(), group.getUpdatedAt(), Collections.emptyList()
        );
    }

    private ResumeVersion createTestVersion(ResumeVersion.VersionType type) {
        if (type == ResumeVersion.VersionType.ORIGINAL) {
            return ResumeVersion.reconstruct(
                    VERSION_ID, GROUP_ID, type, "resume.pdf", "stored.pdf",
                    "application/pdf", 1024L, "path/to/file", "minio",
                    null, null, ParseStatus.PENDING, null, ResumeVersion.Status.ACTIVE,
                    java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
            );
        } else {
            return ResumeVersion.reconstruct(
                    VERSION_ID, GROUP_ID, type, null, null,
                    "text/markdown", 0L, null, null,
                    "", null, ParseStatus.PENDING, null, ResumeVersion.Status.ACTIVE,
                    java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
            );
        }
    }
}
