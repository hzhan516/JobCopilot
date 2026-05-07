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
    private static final Map<AuthException.ErrorType, String> AUTH_ERROR_KEYS = Map.of(
            AuthException.ErrorType.EMAIL_EXISTS, "auth.error.email.exists",
            AuthException.ErrorType.EMAIL_NOT_FOUND, "auth.error.email.not.found",
            AuthException.ErrorType.INVALID_CREDENTIALS, "auth.error.credentials.invalid",
            AuthException.ErrorType.EMAIL_NOT_VERIFIED, "auth.error.email.not.verified",
            AuthException.ErrorType.TOKEN_EXPIRED, "auth.error.token.expired",
            AuthException.ErrorType.TOKEN_INVALID, "auth.error.token.invalid",
            AuthException.ErrorType.EMAIL_REGISTERED_WITH_PASSWORD, "auth.error.email.registered.with.password"
    );
    private final MessageProvider messageProvider;

    @Override
    public String resolve(AuthException.ErrorType errorType) {
        String key = AUTH_ERROR_KEYS.getOrDefault(errorType, "error.system");
        return messageProvider.getMessage(key);
    }
}
