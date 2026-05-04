package edu.asu.ser594.resumeassistant.domain.resume.entity;

import edu.asu.ser594.resumeassistant.domain.resume.valueobject.ParseStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 简历版本实体测试 / Resume version entity tests
 */
@DisplayName("ResumeVersion Entity Tests")
class ResumeVersionTest {

    private static final UUID TEST_GROUP_ID = UUID.randomUUID();
    private static final String TEST_FILE_NAME = "resume.pdf";
    private static final String TEST_FILE_TYPE = "application/pdf";
    private static final String TEST_STORAGE_PATH = "storage/path/file.pdf";
    private static final long TEST_FILE_SIZE = 1024L;

    @Test
    @DisplayName("Should test parsing state transitions")
    void testParsingStateTransitions() {
        // 准备 / Given
        ResumeVersion version = ResumeVersion.createOriginal(
                TEST_GROUP_ID, TEST_FILE_NAME, TEST_FILE_TYPE, TEST_FILE_SIZE, TEST_STORAGE_PATH);

        // 验证初始状态 / Verify initial state
        assertThat(version.getParseStatus()).isEqualTo(ParseStatus.PENDING);
        assertThat(version.getParseErrorMessage()).isNull();

        // 执行并验证：标记为解析中 / When & Then: mark parsing
        version.markParsing();
        assertThat(version.getParseStatus()).isEqualTo(ParseStatus.PARSING);

        // 执行并验证：标记为解析失败 / When & Then: mark parse failed
        version.markParseFailed("Connection timeout");
        assertThat(version.getParseStatus()).isEqualTo(ParseStatus.FAILED);
        assertThat(version.getParseErrorMessage()).isEqualTo("Connection timeout");

        // 执行并验证：标记为解析完成 / When & Then: mark parse completed
        version.markParseCompleted("{\"key\":\"value\"}");
        assertThat(version.getParseStatus()).isEqualTo(ParseStatus.COMPLETED);
        assertThat(version.getParsedContent()).isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    @DisplayName("Should archive and activate version")
    void shouldArchiveAndActivateVersion() {
        // 准备 / Given
        ResumeVersion version = ResumeVersion.createConverted(TEST_GROUP_ID);

        // 验证初始状态 / Verify initial state
        assertThat(version.getStatus()).isEqualTo(ResumeVersion.Status.ACTIVE);

        // 归档 / When: archive
        version.archive();
        assertThat(version.getStatus()).isEqualTo(ResumeVersion.Status.ARCHIVED);

        // 激活 / When: activate
        version.activate();
        assertThat(version.getStatus()).isEqualTo(ResumeVersion.Status.ACTIVE);
    }
}
