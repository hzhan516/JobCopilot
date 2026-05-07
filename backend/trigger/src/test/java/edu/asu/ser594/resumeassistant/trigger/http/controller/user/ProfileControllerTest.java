package edu.asu.ser594.resumeassistant.trigger.http.controller.user;

import edu.asu.ser594.resumeassistant.api.user.dto.request.UpdateAvatarRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.request.UpdateProfileRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.response.ProfileResponse;
import edu.asu.ser594.resumeassistant.api.user.facade.ProfileFacade;
import edu.asu.ser594.resumeassistant.trigger.http.security.CurrentUserArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户资料控制器单元测试 (Standalone MockMvc)
 * Profile controller unit tests using standalone MockMvc
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Profile Controller Tests")
class ProfileControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private MockMvc mockMvc;

    @Mock
    private ProfileFacade profileFacade;

    @InjectMocks
    private ProfileController profileController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(profileController)
                .setCustomArgumentResolvers(new TestCurrentUserArgumentResolver(USER_ID))
                .build();
    }

    @Test
    @DisplayName("Should get profile")
    void shouldGetProfile() throws Exception {
        // 给定
        // Given
        when(profileFacade.getProfile(USER_ID))
                .thenReturn(new ProfileResponse(
                        USER_ID,
                        "Alice Zhang",
                        "https://example.com/avatar.png",
                        "+1-555-0199",
                        "Software Engineer",
                        "San Francisco, CA",
                        OffsetDateTime.now(ZoneOffset.UTC).minusDays(7),
                        OffsetDateTime.now(ZoneOffset.UTC).minusDays(1)
                ));

        // 当&那么
        // When&Then
        mockMvc.perform(get("/v1/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.fullName").value("Alice Zhang"))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://example.com/avatar.png"))
                .andExpect(jsonPath("$.data.phone").value("+1-555-0199"))
                .andExpect(jsonPath("$.data.targetPosition").value("Software Engineer"))
                .andExpect(jsonPath("$.data.preferredLocation").value("San Francisco, CA"));
    }

    @Test
    @DisplayName("Should update profile")
    void shouldUpdateProfile() throws Exception {
        // 给定
        // Given
        when(profileFacade.updateProfile(eq(USER_ID), any(UpdateProfileRequest.class)))
                .thenReturn(new ProfileResponse(
                        USER_ID,
                        "Alice Zhang Updated",
                        "https://example.com/avatar.png",
                        "+1-555-0200",
                        "Senior Software Engineer",
                        "Remote",
                        OffsetDateTime.now(ZoneOffset.UTC).minusDays(7),
                        OffsetDateTime.now(ZoneOffset.UTC)
                ));

        // 当&那么
        // When&Then
        mockMvc.perform(put("/v1/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "fullName": "Alice Zhang Updated",
                                    "phone": "+1-555-0200",
                                    "targetPosition": "Senior Software Engineer",
                                    "preferredLocation": "Remote"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.fullName").value("Alice Zhang Updated"))
                .andExpect(jsonPath("$.data.targetPosition").value("Senior Software Engineer"))
                .andExpect(jsonPath("$.data.preferredLocation").value("Remote"));
    }

    @Test
    @DisplayName("Should update avatar")
    void shouldUpdateAvatar() throws Exception {
        // 给定
        // Given
        when(profileFacade.updateAvatar(eq(USER_ID), any(UpdateAvatarRequest.class)))
                .thenReturn(new ProfileResponse(
                        USER_ID,
                        "Alice Zhang",
                        "https://storage.example.com/new-avatar.png",
                        "+1-555-0199",
                        "Software Engineer",
                        "San Francisco, CA",
                        OffsetDateTime.now(ZoneOffset.UTC).minusDays(7),
                        OffsetDateTime.now(ZoneOffset.UTC)
                ));

        // 当&那么
        // When&Then
        mockMvc.perform(put("/v1/profile/avatar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "avatarUrl": "https://storage.example.com/new-avatar.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://storage.example.com/new-avatar.png"));
    }

    /**
     * 测试用的当前用户参数解析器
     * Test current user argument resolver
     */
    private static class TestCurrentUserArgumentResolver extends CurrentUserArgumentResolver {
        private final UUID userId;

        TestCurrentUserArgumentResolver(UUID userId) {
            this.userId = userId;
        }

        @Override
        public Object resolveArgument(org.springframework.core.MethodParameter parameter,
                                      org.springframework.web.method.support.ModelAndViewContainer mavContainer,
                                      org.springframework.web.context.request.NativeWebRequest webRequest,
                                      org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
            return userId;
        }
    }
}
