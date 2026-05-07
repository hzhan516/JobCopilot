package edu.asu.ser594.resumeassistant.domain.shared.service;

// 国际化消息提供接口
// Message provider interface for i18n
public interface MessageProvider {
    String getMessage(String key);

    String getMessage(String key, Object... args);
}
