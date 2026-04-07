package edu.asu.ser594.resumeassistant.domain.user.service;

public interface PasswordEncoder {
    String encode(String password);

    boolean matches(String rawPassword, String encodedPassword);
}
