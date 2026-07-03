from unittest.mock import MagicMock, patch

from app.schemas import (
    ConversationCompactCommand,
    ConversationMessage,
)
from app.services.compaction_service import (
    process_compaction,
    summarize_history,
    COMPACT_PROMPT,
)


def test_process_compaction_empty_messages():
    """Should return FAILED when no messages are provided."""
    command = ConversationCompactCommand(
        conversationId="conv-1",
        userId="user-1",
        messageHistory=[],
        compactedThroughSequence=0,
    )

    result = process_compaction(command)

    assert result.type == "CONVERSATION_COMPACTED"
    assert result.status == "FAILED"
    assert result.error_message == "No messages to compact"


@patch("app.services.compaction_service._generate_text")
def test_process_compaction_success(mock_generate):
    """Should generate a summary and return COMPLETED event."""
    mock_usage = MagicMock()
    mock_usage.prompt_tokens = 50
    mock_usage.completion_tokens = 20
    mock_usage.total_tokens = 70
    mock_generate.return_value = ("This is a summary of the conversation.", mock_usage)

    messages = [
        ConversationMessage(role="USER", content="Hello"),
        ConversationMessage(role="ASSISTANT", content="Hi there!"),
    ]
    command = ConversationCompactCommand(
        conversationId="conv-1",
        userId="user-1",
        messageHistory=messages,
        compactedThroughSequence=0,
    )

    result = process_compaction(command)

    assert result.type == "CONVERSATION_COMPACTED"
    assert result.status == "COMPLETED"
    assert result.data.summary == "This is a summary of the conversation."
    assert result.data.through_sequence == 2
    assert result.data.context_tokens > 0


@patch("app.services.compaction_service._generate_text")
def test_process_compaction_llm_failure(mock_generate):
    """Should return FAILED event when LLM summarization raises."""
    mock_generate.side_effect = RuntimeError("LLM unavailable")

    messages = [
        ConversationMessage(role="USER", content="Hello"),
    ]
    command = ConversationCompactCommand(
        conversationId="conv-1",
        userId="user-1",
        messageHistory=messages,
        compactedThroughSequence=0,
    )

    result = process_compaction(command)

    assert result.type == "CONVERSATION_COMPACTED"
    assert result.status == "FAILED"


@patch("app.services.compaction_service._generate_text")
def test_summarize_history_includes_all_messages(mock_generate):
    """Should pass all messages to the LLM in the prompt."""
    mock_usage = MagicMock()
    mock_usage.prompt_tokens = 30
    mock_usage.completion_tokens = 10
    mock_usage.total_tokens = 40
    mock_generate.return_value = ("Summary.", mock_usage)

    messages = [
        {"role": "USER", "content": "Hello"},
        {"role": "ASSISTANT", "content": "Hi"},
        {"role": "USER", "content": "Help me"},
    ]

    summary = summarize_history(messages)

    assert summary == "Summary."
    mock_generate.assert_called_once()
    call_args = mock_generate.call_args
    prompt = call_args[1]["messages"][0]["content"]
    assert "Hello" in prompt
    assert "Hi" in prompt
    assert "Help me" in prompt


def test_compact_prompt_is_defined():
    """Sanity: the compaction prompt template contains expected instructions."""
    assert "Summarize" in COMPACT_PROMPT
    assert "{history}" in COMPACT_PROMPT
