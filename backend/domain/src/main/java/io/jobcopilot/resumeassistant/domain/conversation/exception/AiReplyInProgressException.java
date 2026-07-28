package io.jobcopilot.resumeassistant.domain.conversation.exception;

/** Raised when a conversation already owns an in-flight AI reply request. */
public class AiReplyInProgressException extends ConversationException {
    public AiReplyInProgressException() {
        super("conversation.ai.reply.in.progress");
    }
}
