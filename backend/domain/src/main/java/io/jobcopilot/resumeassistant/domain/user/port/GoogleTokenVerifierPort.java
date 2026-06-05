package io.jobcopilot.resumeassistant.domain.user.port;

/**
 * Google ID Token 校验端口
 * Port for Google ID token verification
 * <p>
 * 定义应用层对 Google OAuth 令牌校验的抽象需求，由基础设施层提供具体实现。
 * Defines the application layer's abstract requirement for Google OAuth token verification,
 * implemented by the infrastructure layer.
 */
public interface GoogleTokenVerifierPort {

    /**
     * 验证 Google ID 令牌并提取用户信息
     * Verify Google ID token and extract user information
     *
     * @param idToken Google ID 令牌 / Google ID token
     * @return 解析后的 Google 用户信息 / Parsed Google user info
     */
    GoogleUserInfo verify(String idToken);

    /**
     * Google 用户信息值对象
     * Google user information value object
     *
     * @param email          用户邮箱 / User email
     * @param providerUserId Google 提供的用户唯一标识 / Unique user ID from Google
     * @param displayName    用户显示名称 / User display name
     * @param avatarUrl      用户头像 URL / User avatar URL
     * @param emailVerified  邮箱是否已验证 / Whether email is verified
     */
    record GoogleUserInfo(
            String email,
            String providerUserId,
            String displayName,
            String avatarUrl,
            boolean emailVerified
    ) {
    }
}
