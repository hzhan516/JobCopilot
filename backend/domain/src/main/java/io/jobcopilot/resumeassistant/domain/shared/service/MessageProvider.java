package io.jobcopilot.resumeassistant.domain.shared.service;

/**
 * Anti-corruption interface that lets the domain layer resolve i18n messages without depending on infrastructure.
 * 防腐层接口，使领域层能够解析国际化消息而不依赖基础设施实现。
 */
public interface MessageProvider {
    String getMessage(String key);

    String getMessage(String key, Object... args);
}
