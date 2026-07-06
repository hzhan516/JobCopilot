import json
import logging

from app.schemas import (
    AiResultEvent,
    ConversationCompactCommand,
    ConversationCompactedData,
)
from app.services.llm_client import _generate_text
from app.config import LLM_TEXT_MODEL

logger = logging.getLogger(__name__)

COMPACT_PROMPT = """
Summarize the conversation history below into a concise paragraph that preserves
all key information: user requests, AI actions, decisions made, resume changes,
and job-related context.

Rules:
- Keep factual details (names, dates, numbers, URLs) intact.
- Preserve the sequence of important events.
- Omit filler, greetings, and redundant exchanges.
- Output the summary as a single paragraph of plain text.
- Do NOT use markdown or JSON formatting.

Conversation history:
{history}
""".strip()


def summarize_history(messages: list[dict]) -> str:
    """Generate a concise summary of the conversation history using the LLM.
    使用 LLM 生成对话历史的简洁摘要。"""
    history_text = json.dumps(messages, ensure_ascii=False, indent=2)
    prompt = COMPACT_PROMPT.format(history=history_text)

    logger.info("Compaction summarization: message_count=%d", len(messages))
    summary, usage = _generate_text(
        model=LLM_TEXT_MODEL,
        messages=[{"role": "user", "content": prompt}],
    )
    logger.info(
        "Compaction summary generated: summary_length=%d, usage=%s",
        len(summary),
        usage,
    )
    return summary


def process_compaction(command: ConversationCompactCommand) -> AiResultEvent:
    """Generate a summary of the full message history and return a compacted result event.
    生成完整消息历史的摘要并返回压缩结果事件。"""
    messages = [msg.model_dump(by_alias=True) for msg in command.message_history]

    if not messages:
        return AiResultEvent(
            referenceId=command.conversation_id,
            type="CONVERSATION_COMPACTED",
            status="FAILED",
            data=None,
            errorMessage="No messages to compact",
            eventType=None,
        )

    try:
        summary = summarize_history(messages)
        # ponytail: rough token estimate for remaining history; call tiktoken if precision matters
        through_sequence = len(messages)
        context_tokens = max(0, len(summary.split()) * 2)

        return AiResultEvent(
            referenceId=command.conversation_id,
            type="CONVERSATION_COMPACTED",
            status="COMPLETED",
            data=ConversationCompactedData(
                summary=summary,
                throughSequence=through_sequence,
                contextTokens=context_tokens,
            ),
            errorMessage=None,
            eventType=None,
        )
    except Exception as exc:
        logger.exception(
            "Compaction failed for conversation: %s", command.conversation_id
        )
        return AiResultEvent(
            referenceId=command.conversation_id,
            type="CONVERSATION_COMPACTED",
            status="FAILED",
            data=None,
            errorMessage=str(exc),
            eventType=None,
        )
