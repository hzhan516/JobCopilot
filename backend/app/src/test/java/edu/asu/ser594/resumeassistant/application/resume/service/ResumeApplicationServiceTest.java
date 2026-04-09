package edu.asu.ser594.resumeassistant.application.resume.service;

import edu.asu.ser594.resumeassistant.application.resume.command.ResumeEditCommand;
import edu.asu.ser594.resumeassistant.application.resume.command.ResumeUploadCommand;
import edu.asu.ser594.resumeassistant.application.resume.dto.ResumeDownloadResult;
import edu.asu.ser594.resumeassistant.application.resume.query.ResumeDownloadQuery;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeGroup;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeVersion;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import edu.asu.ser594.resumeassistant.domain.shared.exception.StorageException;
import edu.asu.ser594.resumeassistant.domain.shared.service.DocumentFormatConverter;
import edu.asu.ser594.resumeassistant.domain.shared.service.FileStorageService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ResumeApplicationService Unit Tests
 * 
 * Tests the resume application service following DDD patterns:
 * - Command handling
 * - Query handling
 * - Domain object coordination
 * - Security checks
 */
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

    @InjectMocks
    private ResumeApplicationService resumeService;

    private ResumeGroup testGroup;
    private ResumeVersion testVersion;
    private InputStream testInputStream;

    @BeforeEach
    void setUp() {
        testInputStream = new ByteArrayInputStream("test content".getBytes());
    }

    // ==================== Upload Tests ====================

    @Test
    @DisplayName("Should handle resume upload successfully")
    void shouldHandleResumeUploadSuccessfully() {
        // Given
        ResumeUploadCommand command = ResumeUploadCommand.builder()
                .fileName("resume.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .inputStream(testInputStream)
                .title("My Resume")
                .build();

        doNothing().when(groupRepository).save(any(ResumeGroup.class));

        // When
        ResumeGroup result = resumeService.handleUpload(command, USER_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getTitle()).isEqualTo("My Resume");
        verify(fileStorageService).upload(anyString(), eq(testInputStream), eq(1024L), eq("application/pdf"));
        verify(groupRepository).save(any(ResumeGroup.class));
    }

    @Test
    @DisplayName("Should create original and converted versions on upload")
    void shouldCreateOriginalAndConvertedVersionsOnUpload() {
        // Given
        ResumeUploadCommand command = ResumeUploadCommand.builder()
                .fileName("resume.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .inputStream(testInputStream)
                .title("My Resume")
                .build();

        doNothing().when(groupRepository).save(any(ResumeGroup.class));

        // When
        ResumeGroup result = resumeService.handleUpload(command, USER_ID);

        // Then
        List<ResumeVersion> versions = result.getVersions();
        assertThat(versions).hasSize(2);
        assertThat(versions).extracting(ResumeVersion::getVersionType)
                .containsExactlyInAnyOrder(ResumeVersion.VersionType.ORIGINAL, ResumeVersion.VersionType.CONVERTED);
    }

    @Test
    @DisplayName("Should use default title when title is null")
    void shouldUseDefaultTitleWhenTitleIsNull() {
        // Given
        ResumeUploadCommand command = ResumeUploadCommand.builder()
                .fileName("resume.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .inputStream(testInputStream)
                .title(null)
                .build();

        doNothing().when(groupRepository).save(any(ResumeGroup.class));

        // When
        ResumeGroup result = resumeService.handleUpload(command, USER_ID);

        // Then
        assertThat(result.getTitle()).isEqualTo("Untitled Resume");
    }

    // ==================== Edit Tests ====================

    @Test
    @DisplayName("Should handle resume edit successfully")
    void shouldHandleResumeEditSuccessfully() {
        // Given
        testGroup = createTestGroup();
        testVersion = createTestVersion(ResumeVersion.VersionType.CONVERTED);

        ResumeEditCommand command = ResumeEditCommand.builder()
                .versionId(VERSION_ID)
                .userId(USER_ID)
                .content("Updated markdown content")
                .build();

        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(testVersion));
        when(groupRepository.findById(testVersion.getGroupId())).thenReturn(Optional.of(testGroup));
        doNothing().when(versionRepository).save(any(ResumeVersion.class));

        // When
        ResumeVersion result = resumeService.handleEdit(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Updated markdown content");
        verify(versionRepository).save(testVersion);
    }

    @Test
    @DisplayName("Should throw exception when editing version not found")
    void shouldThrowExceptionWhenEditingVersionNotFound() {
        // Given
        ResumeEditCommand command = ResumeEditCommand.builder()
                .versionId(VERSION_ID)
                .userId(USER_ID)
                .content("Updated content")
                .build();

        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> resumeService.handleEdit(command))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("version.not.found");
    }

    @Test
    @DisplayName("Should throw exception when editing without group ownership")
    void shouldThrowExceptionWhenEditingWithoutGroupOwnership() {
        // Given
        UUID otherUserId = UUID.randomUUID();
        testGroup = createTestGroup();
        testVersion = createTestVersion(ResumeVersion.VersionType.CONVERTED);

        ResumeEditCommand command = ResumeEditCommand.builder()
                .versionId(VERSION_ID)
                .userId(otherUserId)
                .content("Updated content")
                .build();

        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(testVersion));
        when(groupRepository.findById(testVersion.getGroupId())).thenReturn(Optional.of(testGroup));

        // When & Then
        assertThatThrownBy(() -> resumeService.handleEdit(command))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("access.denied");
    }

    // ==================== Delete Group Tests ====================

    @Test
    @DisplayName("Should delete resume group successfully")
    void shouldDeleteResumeGroupSuccessfully() {
        // Given
        testGroup = createTestGroup();
        testVersion = createTestVersion(ResumeVersion.VersionType.ORIGINAL);

        when(groupRepository.findByIdAndUserId(GROUP_ID, USER_ID)).thenReturn(Optional.of(testGroup));
        when(versionRepository.findAllByGroupId(GROUP_ID)).thenReturn(List.of(testVersion));

        // When
        resumeService.handleDelete(GROUP_ID, USER_ID);

        // Then
        verify(versionRepository).deleteAllByGroupId(GROUP_ID);
        verify(groupRepository).delete(GROUP_ID);
    }

    @Test
    @DisplayName("Should delete all files when deleting group")
    void shouldDeleteAllFilesWhenDeletingGroup() {
        // Given
        testGroup = createTestGroup();
        ResumeVersion version1 = createTestVersionWithPath(ResumeVersion.VersionType.ORIGINAL, "path/1");
        ResumeVersion version2 = createTestVersionWithPath(ResumeVersion.VersionType.CONVERTED, "path/2");

        when(groupRepository.findByIdAndUserId(GROUP_ID, USER_ID)).thenReturn(Optional.of(testGroup));
        when(versionRepository.findAllByGroupId(GROUP_ID)).thenReturn(List.of(version1, version2));

        // When
        resumeService.handleDelete(GROUP_ID, USER_ID);

        // Then
        verify(fileStorageService).delete("path/1");
        verify(fileStorageService).delete("path/2");
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent group")
    void shouldThrowExceptionWhenDeletingNonExistentGroup() {
        // Given
        when(groupRepository.findByIdAndUserId(GROUP_ID, USER_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> resumeService.handleDelete(GROUP_ID, USER_ID))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("group.not.found");
    }

    // ==================== Delete Version Tests ====================

    @Test
    @DisplayName("Should delete version successfully")
    void shouldDeleteVersionSuccessfully() {
        // Given
        testGroup = createTestGroup();
        testVersion = createTestVersion(ResumeVersion.VersionType.CONVERTED);

        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(testVersion));
        when(groupRepository.findById(testVersion.getGroupId())).thenReturn(Optional.of(testGroup));

        // When
        resumeService.handleDeleteVersion(VERSION_ID, USER_ID);

        // Then
        verify(versionRepository).delete(VERSION_ID);
    }

    @Test
    @DisplayName("Should throw exception when deleting original version")
    void shouldThrowExceptionWhenDeletingOriginalVersion() {
        // Given
        testGroup = createTestGroup();
        testVersion = createTestVersion(ResumeVersion.VersionType.ORIGINAL);

        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(testVersion));
        when(groupRepository.findById(testVersion.getGroupId())).thenReturn(Optional.of(testGroup));

        // When & Then
        assertThatThrownBy(() -> resumeService.handleDeleteVersion(VERSION_ID, USER_ID))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("version.original.cannot.delete");
    }

    // ==================== Query Tests ====================

    @Test
    @DisplayName("Should get group by id and user id")
    void shouldGetGroupByIdAndUserId() {
        // Given
        testGroup = createTestGroup();
        when(groupRepository.findByIdAndUserId(GROUP_ID, USER_ID)).thenReturn(Optional.of(testGroup));

        // When
        ResumeGroup result = resumeService.getGroup(GROUP_ID, USER_ID);

        // Then
        assertThat(result).isEqualTo(testGroup);
    }

    @Test
    @DisplayName("Should list all user groups")
    void shouldListAllUserGroups() {
        // Given
        testGroup = createTestGroup();
        when(groupRepository.findAllByUserId(USER_ID)).thenReturn(List.of(testGroup));

        // When
        List<ResumeGroup> result = resumeService.listUserGroups(USER_ID);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testGroup);
    }

    @Test
    @DisplayName("Should get version with ownership check")
    void shouldGetVersionWithOwnershipCheck() {
        // Given
        testGroup = createTestGroup();
        testVersion = createTestVersion(ResumeVersion.VersionType.CONVERTED);

        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(testVersion));
        when(groupRepository.findById(testVersion.getGroupId())).thenReturn(Optional.of(testGroup));

        // When
        ResumeVersion result = resumeService.getVersion(VERSION_ID, USER_ID);

        // Then
        assertThat(result).isEqualTo(testVersion);
    }

    // ==================== Download Tests ====================

    @Test
    @DisplayName("Should handle download of original version")
    void shouldHandleDownloadOfOriginalVersion() {
        // Given
        testGroup = createTestGroup();
        testVersion = createTestVersionWithPath(ResumeVersion.VersionType.ORIGINAL, "path/to/file.pdf");
        InputStream fileStream = new ByteArrayInputStream("PDF content".getBytes());

        ResumeDownloadQuery query = ResumeDownloadQuery.builder()
                .versionId(VERSION_ID)
                .userId(USER_ID)
                .targetFormat("original")
                .build();

        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(testVersion));
        when(groupRepository.findById(testVersion.getGroupId())).thenReturn(Optional.of(testGroup));
        when(fileStorageService.download("path/to/file.pdf")).thenReturn(Optional.of(fileStream));

        // When
        ResumeDownloadResult result = resumeService.handleDownload(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getInputStream()).isEqualTo(fileStream);
        assertThat(result.getContentType()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("Should convert format when downloading converted version")
    void shouldConvertFormatWhenDownloadingConvertedVersion() throws IOException {
        // Given
        testGroup = createTestGroup();
        testVersion = ResumeVersion.createConverted(GROUP_ID);
        testVersion.editContent("# Resume\n\nContent");

        InputStream convertedStream = new ByteArrayInputStream("PDF content".getBytes());

        ResumeDownloadQuery query = ResumeDownloadQuery.builder()
                .versionId(VERSION_ID)
                .userId(USER_ID)
                .targetFormat("pdf")
                .build();

        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(testVersion));
        when(groupRepository.findById(testVersion.getGroupId())).thenReturn(Optional.of(testGroup));
        when(documentFormatConverter.convert(any(), eq("md"), eq("pdf"))).thenReturn(convertedStream);

        // When
        ResumeDownloadResult result = resumeService.handleDownload(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContentType()).isEqualTo("application/pdf");
        verify(documentFormatConverter).convert(any(), eq("md"), eq("pdf"));
    }

    // ==================== Helper Methods ====================

    private ResumeGroup createTestGroup() {
        ResumeGroup group = ResumeGroup.create(USER_ID, "Test Resume");
        return ResumeGroup.reconstruct(
                GROUP_ID, USER_ID, "Test Resume", false,
                group.getCreatedAt(), group.getUpdatedAt(), Collections.emptyList()
        );
    }

    private ResumeVersion createTestVersion(ResumeVersion.VersionType type) {
        if (type == ResumeVersion.VersionType.ORIGINAL) {
            return ResumeVersion.createOriginal(GROUP_ID, "resume.pdf", "application/pdf", 1024L, "path/to/file");
        } else {
            return ResumeVersion.createConverted(GROUP_ID);
        }
    }

    private ResumeVersion createTestVersionWithPath(ResumeVersion.VersionType type, String storagePath) {
        if (type == ResumeVersion.VersionType.ORIGINAL) {
            return ResumeVersion.reconstruct(
                    VERSION_ID, GROUP_ID, type, "resume.pdf", "stored.pdf",
                    "application/pdf", 1024L, storagePath, "minio",
                    null, null, ResumeVersion.Status.ACTIVE,
                    java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
            );
        } else {
            ResumeVersion version = ResumeVersion.createConverted(GROUP_ID);
            return ResumeVersion.reconstruct(
                    VERSION_ID, GROUP_ID, type, null, null,
                    "text/markdown", 0L, storagePath, null,
                    "content", null, ResumeVersion.Status.ACTIVE,
                    java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
            );
        }
    }
}
