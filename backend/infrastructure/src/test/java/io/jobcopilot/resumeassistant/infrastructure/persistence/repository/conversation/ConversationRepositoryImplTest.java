package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.conversation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ConversationRepositoryImpl 单元测试
 * ConversationRepositoryImpl Unit Tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Conversation Repository Implementation Tests")
class ConversationRepositoryImplTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private JpaConversationRepository jpaRepository;

    @InjectMocks
    private ConversationRepositoryImpl conversationRepository;

    @Test
    @DisplayName("Should count conversations by user ID converting UUID to string")
    void shouldCountConversationsByUserId() {
        // Given
        when(jpaRepository.countByUserId(USER_ID.toString())).thenReturn(4L);

        // When
        long result = conversationRepository.countByUserId(USER_ID);

        // Then
        assertThat(result).isEqualTo(4L);
        verify(jpaRepository).countByUserId(USER_ID.toString());
    }
}
