package io.jobcopilot.resumeassistant.domain.job.exception;

import io.jobcopilot.resumeassistant.domain.shared.exception.LocalizedException;

/**
 * 职位解析内容尚未就绪异常
 * Job parsed content is not yet ready for scoring.
 */
public class JobContentNotReadyException extends LocalizedException {

    public JobContentNotReadyException() {
        super("job.content.not.ready");
    }

    public JobContentNotReadyException(String messageKey) {
        super(messageKey);
    }
}
