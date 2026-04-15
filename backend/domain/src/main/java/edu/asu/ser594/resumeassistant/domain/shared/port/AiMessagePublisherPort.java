package edu.asu.ser594.resumeassistant.domain.shared.port;

import edu.asu.ser594.resumeassistant.domain.shared.event.ai.JobParseCommand;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.ResumeParseCommand;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.VectorGenCommand;

public interface AiMessagePublisherPort {

    void sendResumeForParsing(ResumeParseCommand command);

    void sendTextForVectorGeneration(VectorGenCommand command);

    void sendJobForParsing(JobParseCommand command);
}
