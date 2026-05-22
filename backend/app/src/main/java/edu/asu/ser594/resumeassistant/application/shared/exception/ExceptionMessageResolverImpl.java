package edu.asu.ser594.resumeassistant.application.shared.exception;

import edu.asu.ser594.resumeassistant.api.shared.service.ExceptionMessageResolver;
import edu.asu.ser594.resumeassistant.domain.shared.service.MessageProvider;
import edu.asu.ser594.resumeassistant.domain.user.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ExceptionMessageResolverImpl implements ExceptionMessageResolver {
    private static final Map<AuthException.ErrorType, String> AUTH_ERROR_KEYS = Map.ofEntries(
            Map.entry(AuthException.ErrorType.EMAIL_EXISTS, "auth.error.email.exists"),
            Map.entry(AuthException.ErrorType.EMAIL_NOT_FOUND, "auth.error.email.not.found"),
            Map.entry(AuthException.ErrorType.INVALID_CREDENTIALS, "auth.error.credentials.invalid"),
            Map.entry(AuthException.ErrorType.EMAIL_NOT_VERIFIED, "auth.error.email.not.verified"),
            Map.entry(AuthException.ErrorType.TOKEN_EXPIRED, "auth.error.token.expired"),
            Map.entry(AuthException.ErrorType.TOKEN_INVALID, "auth.error.token.invalid"),
            Map.entry(AuthException.ErrorType.EMAIL_REGISTERED_WITH_PASSWORD, "auth.error.email.registered.with.password"),
            Map.entry(AuthException.ErrorType.CAPTCHA_REQUIRED, "auth.error.captcha.required"),
            Map.entry(AuthException.ErrorType.CAPTCHA_INVALID, "auth.error.captcha.invalid"),
            Map.entry(AuthException.ErrorType.CAPTCHA_EXPIRED, "auth.error.captcha.expired")
    );
    private final MessageProvider messageProvider;

    @Override
    public String resolve(AuthException.ErrorType errorType) {
        String key = AUTH_ERROR_KEYS.getOrDefault(errorType, "error.system");
        return messageProvider.getMessage(key);
    }
}
