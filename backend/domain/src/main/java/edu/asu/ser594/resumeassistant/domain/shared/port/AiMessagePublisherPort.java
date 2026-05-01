package edu.asu.ser594.resumeassistant.domain.shared.port;

import edu.asu.ser594.resumeassistant.domain.shared.event.ai.*;

public interface AiMessagePublisherPort {

    void sendResumeForParsing(ResumeParseCommand command);

    void sendTextForVectorGeneration(VectorGenCommand command);

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
}
