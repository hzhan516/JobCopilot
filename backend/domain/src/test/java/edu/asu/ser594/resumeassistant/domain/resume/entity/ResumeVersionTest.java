package edu.asu.ser594.resumeassistant.domain.resume.entity;

import edu.asu.ser594.resumeassistant.domain.resume.valueobject.ParseStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        ResumeVersion version = ResumeVersion.createOriginal(
                TEST_GROUP_ID, TEST_FILE_NAME, TEST_FILE_TYPE, TEST_FILE_SIZE, TEST_STORAGE_PATH);

        assertThat(version.getParseStatus()).isEqualTo(ParseStatus.PENDING);
        assertThat(version.getParseErrorMessage()).isNull();

        version.markParsing();
        assertThat(version.getParseStatus()).isEqualTo(ParseStatus.PARSING);

        version.markParseFailed("Connection timeout");
        assertThat(version.getParseStatus()).isEqualTo(ParseStatus.FAILED);
        assertThat(version.getParseErrorMessage()).isEqualTo("Connection timeout");

        version.markParseCompleted("{\"key\":\"value\"}");
        assertThat(version.getParseStatus()).isEqualTo(ParseStatus.COMPLETED);
        assertThat(version.getParsedContent()).isEqualTo("{\"key\":\"value\"}");
    }
}
