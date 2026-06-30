package io.jobcopilot.resumeassistant.application.admin;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/** 业务指标收集 / Business metrics collector */
@Component
public class BusinessMetricsService {

    @Getter private final Counter userRegistrations;
    @Getter private final Timer aiParsingDuration;
    @Getter private final AtomicInteger activeConversations = new AtomicInteger(0);

    public BusinessMetricsService(MeterRegistry registry) {
        this.userRegistrations = Counter.builder("jobcopilot.users.registrations")
                .description("User registrations").register(registry);
        this.aiParsingDuration = Timer.builder("jobcopilot.ai.parsing.duration")
                .description("AI parsing duration").register(registry);
    }

    public void recordRegistration() { userRegistrations.increment(); }

    public void recordAiParsing(Runnable task) { aiParsingDuration.record(task); }
}
