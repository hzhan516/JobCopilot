package edu.asu.ser594.resumeassistant.infrastructure.security;

import edu.asu.ser594.resumeassistant.domain.user.service.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

/** BCrypt 密码编码器实现 / BCrypt password encoder implementation */
@Component
public class BCryptPasswordEncoderImpl implements PasswordEncoder {

    private static final int STRENGTH = 10;

    /** 编码密码 / Encode password */
    @Override
    public String encode(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword cannot be null");
        }
        String truncatedPassword = truncatePassword(rawPassword);
        return BCrypt.hashpw(truncatedPassword, BCrypt.gensalt(STRENGTH));
    }

    /** 校验密码是否匹配 / Check if password matches */
    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        String truncatedPassword = truncatePassword(rawPassword);
        return BCrypt.checkpw(truncatedPassword, encodedPassword);
    }

    // BCrypt 最大支持 72 字节，超长密码需要截断 / BCrypt supports max 72 bytes, truncate if necessary
    private String truncatePassword(String password) {
        byte[] bytes = password.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (bytes.length > 72) {
            return new String(bytes, 0, 72, java.nio.charset.StandardCharsets.UTF_8);
        }
        return password;
    }
}
