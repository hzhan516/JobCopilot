package io.jobcopilot.resumeassistant.domain.matching.exception;

import io.jobcopilot.resumeassistant.domain.shared.exception.LocalizedException;

/**
 * 简历向量尚未就绪异常
 * Resume vector is not yet ready exception
 */
public class ResumeVectorNotReadyException extends LocalizedException {

    public ResumeVectorNotReadyException(String resumeVersionId) {
        super("matching.resume.vector.not.ready", resumeVersionId);
    }
}
