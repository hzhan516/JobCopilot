package edu.asu.ser594.resumeassistant.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// 基于 Accept-Language 请求头的语言解析器
// Locale resolver based on Accept-Language header
public class AcceptHeaderLocaleResolver implements LocaleResolver {
    @Setter
    private Locale defaultLocale = Locale.ENGLISH;
    @Setter
    private List<Locale> supportedLocales = new ArrayList<>();

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        String acceptLanguage = request.getHeader("Accept-Language");

        if (!StringUtils.hasText(acceptLanguage)) {
            return defaultLocale;
        }

        // 解析 Accept-Language
        List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(acceptLanguage);
        Locale locale = Locale.lookup(ranges, supportedLocales);

        return locale != null ? locale : defaultLocale;
    }

    @Override
    public void setLocale(HttpServletRequest request,
                          @Nullable HttpServletResponse response,
                          @Nullable Locale locale) {

    }


}
