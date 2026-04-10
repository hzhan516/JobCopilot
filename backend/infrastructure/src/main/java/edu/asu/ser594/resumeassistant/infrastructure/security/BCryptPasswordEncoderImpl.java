package edu.asu.ser594.resumeassistant.infrastructure.security;

import edu.asu.ser594.resumeassistant.domain.user.service.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

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

    private String truncatePassword(String password) {
        byte[] bytes = password.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (bytes.length > 72) {
            return new String(bytes, 0, 72, java.nio.charset.StandardCharsets.UTF_8);
        }
        return password;
    }
}