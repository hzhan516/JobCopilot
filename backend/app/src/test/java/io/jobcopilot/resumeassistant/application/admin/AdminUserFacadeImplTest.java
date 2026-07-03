package io.jobcopilot.resumeassistant.application.admin;

import io.jobcopilot.resumeassistant.api.admin.dto.AdminUserListRequest;
import io.jobcopilot.resumeassistant.domain.admin.repository.AuditLogRepository;
import io.jobcopilot.resumeassistant.domain.conversation.repository.ConversationRepository;
import io.jobcopilot.resumeassistant.domain.job.repository.JobRepository;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import io.jobcopilot.resumeassistant.domain.user.entity.User;
import io.jobcopilot.resumeassistant.domain.user.repository.UserRepository;
import io.jobcopilot.resumeassistant.types.common.PageResult;
import io.jobcopilot.resumeassistant.types.enums.OAuthProvider;
import io.jobcopilot.resumeassistant.types.enums.UserRole;
import io.jobcopilot.resumeassistant.types.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** AdminUserFacadeImpl 单元测试 / Unit tests for admin user facade */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Admin User Facade Tests")
class AdminUserFacadeImplTest {

    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private UserRepository userRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ResumeGroupRepository resumeGroupRepository;
    @Mock private JobRepository jobRepository;
    @Mock private ConversationRepository conversationRepository;
    @InjectMocks private AdminUserFacadeImpl facade;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.create("user@test.com", OAuthProvider.EMAIL);
        when(auditLogRepository.save(any())).thenReturn(null);
        when(resumeGroupRepository.countByUserId(any())).thenReturn(0L);
        when(jobRepository.countByUserId(any())).thenReturn(0L);
        when(conversationRepository.countByUserId(any())).thenReturn(0L);
    }

    @Test
    @DisplayName("Should list users with pagination")
    void shouldListUsers() {
        when(userRepository.findAll(0, 20))
                .thenReturn(PageResult.of(List.of(testUser), 0, 20, 1));

        var result = facade.listUsers(new AdminUserListRequest(null, null, null, 0, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should get user detail by ID")
    void shouldGetUserDetail() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(resumeGroupRepository.countByUserId(testUser.getId())).thenReturn(3L);
        when(jobRepository.countByUserId(testUser.getId())).thenReturn(5L);
        when(conversationRepository.countByUserId(testUser.getId())).thenReturn(2L);

        var result = facade.getUserDetail(USER_ID);

        assertThat(result.email()).isEqualTo("user@test.com");
        assertThat(result.role()).isEqualTo("JOB_SEEKER");
        assertThat(result.resumeCount()).isEqualTo(3L);
        assertThat(result.jobCount()).isEqualTo(5L);
        assertThat(result.conversationCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should not query cross-entity counts when listing users")
    void shouldNotQueryCountsWhenListingUsers() {
        when(userRepository.findAll(0, 20))
                .thenReturn(PageResult.of(List.of(testUser), 0, 20, 1));

        var result = facade.listUsers(new AdminUserListRequest(null, null, null, 0, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).resumeCount()).isEqualTo(0L);
        assertThat(result.content().get(0).jobCount()).isEqualTo(0L);
        assertThat(result.content().get(0).conversationCount()).isEqualTo(0L);
        verify(resumeGroupRepository, never()).countByUserId(any());
        verify(jobRepository, never()).countByUserId(any());
        verify(conversationRepository, never()).countByUserId(any());
    }

    @Test
    @DisplayName("Should throw when user not found")
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facade.getUserDetail(USER_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should update user role")
    void shouldUpdateUserRole() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        var result = facade.updateUserRole(USER_ID, "ADMIN", ADMIN_ID);

        assertThat(result.role()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Should update user status")
    void shouldUpdateUserStatus() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        var result = facade.updateUserStatus(USER_ID, "SUSPENDED", ADMIN_ID);

        assertThat(result.status()).isEqualTo("SUSPENDED");
    }

    @Test
    @DisplayName("Should soft-delete user by setting DELETED status")
    void shouldSoftDeleteUser() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        facade.deleteUser(USER_ID, ADMIN_ID);

        assertThat(testUser.getStatus()).isEqualTo(UserStatus.DELETED);
    }
}
