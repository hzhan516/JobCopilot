package edu.asu.ser594.resumeassistant.domain.shared.service;

// Message provider interface for i18n
public interface MessageProvider {
    String getMessage(String key);

    String getMessage(String key, Object... args);
}
