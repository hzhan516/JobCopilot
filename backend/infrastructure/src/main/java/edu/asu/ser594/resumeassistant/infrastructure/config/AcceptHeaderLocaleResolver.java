package edu.asu.ser594.resumeassistant.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Custom locale resolver that prioritizes Accept-Language header over session or cookie.
 * Falls back to the configured default locale when the header is missing or unsupported.
 * 自定义语言解析器，优先根据 Accept-Language 请求头解析语言，缺失或不支持时回退到默认配置
 */
public class AcceptHeaderLocaleResolver implements LocaleResolver {
    @Setter
    private Locale defaultLocale = Locale.ENGLISH;
    @Setter
    private List<Locale> supportedLocales = new ArrayList<>();

    @Override
    public @NotNull Locale resolveLocale(HttpServletRequest request) {
        String acceptLanguage = request.getHeader("Accept-Language");

        if (!StringUtils.hasText(acceptLanguage)) {
            return defaultLocale;
        }

        List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(acceptLanguage);
        Locale locale = Locale.lookup(ranges, supportedLocales);

        return locale != null ? locale : defaultLocale;
    }

    @Override
    public void setLocale(@NotNull HttpServletRequest request,
                          @Nullable HttpServletResponse response,
                          @Nullable Locale locale) {

    }


}
