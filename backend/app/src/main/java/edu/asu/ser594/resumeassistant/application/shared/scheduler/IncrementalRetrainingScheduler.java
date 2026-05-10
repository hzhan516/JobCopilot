package edu.asu.ser594.resumeassistant.application.shared.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 增量模型定时重训练调度器 / Incremental model retraining scheduler
 * <p>
 * 每天凌晨 2 点触发 AI Service 的模型权重强制重算，作为增量学习的兜底机制。
 * Triggers AI Service model weight recomputation daily at 2 AM as a safety net for incremental learning.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IncrementalRetrainingScheduler {

    private final RestTemplate restTemplate;

    @Value("${ai.service.base-url:http://localhost:8000}")
    private String aiServiceBaseUrl;

    /**
     * 每天凌晨 2 点调用 AI Service 强制重算模型权重
     * Calls AI Service to force model weight recomputation at 2 AM daily
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @SchedulerLock(name = "IncrementalRetraining", lockAtMostFor = "1h", lockAtLeastFor = "5m")
    public void triggerIncrementalRetraining() {
        String url = aiServiceBaseUrl + "/api/v1/admin/recompute-model";
        log.info("Triggering incremental model retraining at URL: {}", url);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            ).getBody();

            log.info("Incremental model retraining triggered successfully: {}", response);
        } catch (Exception e) {
            log.error("Failed to trigger incremental model retraining: {}", e.getMessage());
            // 非阻塞：定时调度失败不应影响主业务
        }
    }
}
