package edu.asu.ser594.resumeassistant.domain.shared.port;

import edu.asu.ser594.resumeassistant.domain.shared.event.ai.ConversationRequestCommand;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.JobParseCommand;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.ResumeParseCommand;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.VectorGenCommand;

public interface AiMessagePublisherPort {

    void sendResumeForParsing(ResumeParseCommand command);

    void sendTextForVectorGeneration(VectorGenCommand command);

    void sendJobForParsing(JobParseCommand command);

    /**
     * 发送对话请求到 AI 服务
     * Send conversation request to AI service
     */
    void sendConversationRequest(ConversationRequestCommand command);
}
