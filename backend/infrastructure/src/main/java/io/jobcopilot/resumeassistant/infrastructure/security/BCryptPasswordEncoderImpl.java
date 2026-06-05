package io.jobcopilot.resumeassistant.infrastructure.security;

import io.jobcopilot.resumeassistant.domain.user.service.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

/**
 * Domain PasswordEncoder adapter delegating to Spring Security's BCrypt implementation.
 * Truncates passwords exceeding 72 bytes because BCrypt silently ignores trailing bytes beyond that limit.
 * 领域 PasswordEncoder 适配器，委托给 Spring Security BCrypt；对超过 72 字节的密码做截断，因为 BCrypt 会静默忽略尾部字节
 */
@Component
public class BCryptPasswordEncoderImpl implements PasswordEncoder {

    private static final int STRENGTH = 10;

    @Override
    public String encode(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword cannot be null");
        }
        String truncatedPassword = truncatePassword(rawPassword);
        return BCrypt.hashpw(truncatedPassword, BCrypt.gensalt(STRENGTH));
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        String truncatedPassword = truncatePassword(rawPassword);
        return BCrypt.checkpw(truncatedPassword, encodedPassword);
    }

    // BCrypt silently truncates input at 72 bytes; pre-truncation ensures consistent behavior across implementations
    // BCrypt 在 72 字节处静默截断输入，预截断确保跨实现行为一致
    private String truncatePassword(String password) {
        byte[] bytes = password.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (bytes.length > 72) {
            return new String(bytes, 0, 72, java.nio.charset.StandardCharsets.UTF_8);
        }
        return password;
    }
}
