package edu.asu.ser594.resumeassistant.trigger.listener.ai;

import edu.asu.ser594.resumeassistant.api.job.dto.response.MatchFactors;
import edu.asu.ser594.resumeassistant.api.job.dto.response.MatchItem;
import edu.asu.ser594.resumeassistant.api.matching.facade.MatchingFacade;
import edu.asu.ser594.resumeassistant.infrastructure.messaging.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 职位精排结果监听器
 * Job rank result listener
 * <p>
 * 监听 Python AI 服务返回的精排结果并保存
 * Listens to ranking results from Python AI service and persists them
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobRankResultListener {

    private final MatchingFacade matchingFacade;

    /**
     * 处理职位精排结果
     * Process job rank result
     *
     * @param payload MQ 消息载荷 / MQ message payload
     */
    @RabbitListener(queues = RabbitMqConfig.QUEUE_RES_JOB_RANK)
    @SuppressWarnings("unchecked")
    public void onJobRankResult(final Map<String, Object> payload) {
        try {
            final String matchId = (String) payload.get("matchId");
            final String status = (String) payload.get("status");
            final Long rankTimeMs = payload.get("rankTimeMs") != null
                    ? ((Number) payload.get("rankTimeMs")).longValue()
                    : 0L;

            log.info("Received job rank result for matchId: {}, status: {}", matchId, status);

            if (!"COMPLETED".equals(status)) {
                log.warn("Job rank failed for matchId: {}, error: {}", matchId, payload.get("errorMessage"));
                return;
            }

            final List<Map<String, Object>> rankedData = (List<Map<String, Object>>) payload.get("rankedResults");
            final List<MatchItem> matchItems = new ArrayList<>();

            if (rankedData != null) {
                for (Map<String, Object> item : rankedData) {
                    final MatchFactors factors = extractMatchFactors(item);
                    matchItems.add(new MatchItem(
                            (String) item.get("jobId"),
                            (String) item.get("title"),
                            (String) item.get("company"),
                            item.get("matchScore") != null ? ((Number) item.get("matchScore")).doubleValue() : 0.0,
                            factors,
                            (String) item.get("description"),
                            (String) item.get("matchReason")
                    ));
                }
            }

            matchingFacade.saveJobRankResult(matchId, matchItems, rankTimeMs);
            log.info("Job rank result saved for matchId: {}", matchId);
        } catch (Exception e) {
            log.error("Failed to process job rank result: {}", payload, e);
        }
    }

    @SuppressWarnings("unchecked")
    private MatchFactors extractMatchFactors(final Map<String, Object> item) {
        final Object factorsObj = item.get("matchFactors");
        if (factorsObj instanceof Map) {
            final Map<String, Object> factorsMap = (Map<String, Object>) factorsObj;
            return new MatchFactors(
                    extractDouble(factorsMap.get("skillMatch")),
                    extractDouble(factorsMap.get("experienceMatch")),
                    extractDouble(factorsMap.get("educationMatch"))
            );
        }
        return new MatchFactors(0.0, 0.0, 0.0);
    }

    private Double extractDouble(final Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }
}
