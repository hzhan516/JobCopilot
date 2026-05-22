package io.jobcopilot.resumeassistant.domain.user.service;

public interface PasswordEncoder {
    String encode(String password);

    boolean matches(String rawPassword, String encodedPassword);
}
