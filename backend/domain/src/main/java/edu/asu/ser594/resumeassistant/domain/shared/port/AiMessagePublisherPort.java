package edu.asu.ser594.resumeassistant.domain.shared.port;

import java.util.Map;

import edu.asu.ser594.resumeassistant.domain.shared.event.ai.ConversationRequestCommand;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.JobParseCommand;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.JobRankCommand;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.ResumeParseCommand;

public interface AiMessagePublisherPort {

    void sendResumeForParsing(ResumeParseCommand command);

    void sendJobForParsing(JobParseCommand command);

    /**
     * 发送对话请求到 AI 服务
     * Send conversation request to AI service
     */
    void sendConversationRequest(ConversationRequestCommand command);

    /**
     * 发送职位精排请求到 AI 服务
     * Send job ranking request to AI service
     */
    void sendJobForRanking(JobRankCommand command);

    /**
     * 发送评分标签到 AI 服务用于增量模型训练
     * Send score label to AI service for incremental model training
     */
    void sendScoreLabel(Map<String, Object> payload);
}
