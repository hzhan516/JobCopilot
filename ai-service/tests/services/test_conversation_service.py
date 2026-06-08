from unittest.mock import patch

from app.schemas import ConversationRequestCommand
from app.services.conversation_service import (
    _fallback_content_from_unparseable_response,
    _infer_file_format,
    process_conversation,
)
from app.services.llm_client import JsonGenerationResult, LlmJsonParseError


def test_infer_file_format():
    assert _infer_file_format("http://example.com/resume.pdf") == "pdf"
    assert _infer_file_format("http://example.com/resume.docx") == "docx"
    assert _infer_file_format("http://example.com/resume.txt") == "txt"
    assert _infer_file_format("http://example.com/resume.md") == "md"
    assert _infer_file_format("http://example.com/resume.jpg") is None
    assert _infer_file_format("http://example.com/resume") is None


@patch("app.services.conversation_service.generate_json_from_text_prompt_with_repair")
def test_process_conversation_preserves_request_context(mock_generate):
    mock_generate.return_value = JsonGenerationResult(
        data={
            "content": "hello",
            "fileUrl": None,
            "resumeModification": {"modified": False, "markdown": ""},
        },
        raw_text='{"content": "hello"}',
        json_text='{"content": "hello"}',
    )
    command = ConversationRequestCommand(
        conversationId="conv-1",
        userId="user-1",
        currentMessage="hello",
        messageHistory=[],
        requestId="req-1",
        locale="zh-CN",
    )

    result = process_conversation(command)

    assert result.status == "COMPLETED"
    assert result.data.request_id == "req-1"
    assert result.data.locale == "zh-CN"


@patch("app.services.conversation_service.generate_json_from_text_prompt_with_repair")
def test_process_conversation_falls_back_when_json_repair_fails(mock_generate):
    raw_text = (
        '{"content": "Based on the job, your resume is a strong match. '
        'The phrase "materials engineer" appears unescaped.", "fileUrl": null'
    )
    mock_generate.side_effect = LlmJsonParseError(
        "bad json",
        raw_text=raw_text,
        original_error=ValueError("invalid json"),
        repair_error=ValueError("repair failed"),
    )
    command = ConversationRequestCommand(
        conversationId="conv-1",
        userId="user-1",
        currentMessage="match?",
        messageHistory=[],
        requestId="req-1",
        locale="en",
    )

    result = process_conversation(command)

    assert result.status == "COMPLETED"
    assert "strong match" in result.data.content
    assert result.data.file_url is None
    assert result.data.resume_modification.modified is False
    assert result.data.request_id == "req-1"


@patch("app.services.conversation_service.generate_json_from_text_prompt_with_repair")
def test_process_conversation_normalizes_missing_resume_modification(mock_generate):
    mock_generate.return_value = JsonGenerationResult(
        data={"content": "plain answer", "fileUrl": "   "},
        raw_text='{"content": "plain answer"}',
        json_text='{"content": "plain answer"}',
        repaired=True,
    )
    command = ConversationRequestCommand(
        conversationId="conv-1",
        userId="user-1",
        currentMessage="hello",
        messageHistory=[],
        requestId="req-1",
        locale="en",
    )

    result = process_conversation(command)

    assert result.status == "COMPLETED"
    assert result.data.content == "plain answer"
    assert result.data.file_url is None
    assert result.data.resume_modification.modified is False


@patch("app.services.conversation_service.generate_json_from_text_prompt_with_repair")
def test_process_conversation_coerces_string_false_resume_modification(mock_generate):
    mock_generate.return_value = JsonGenerationResult(
        data={
            "content": "plain answer",
            "fileUrl": None,
            "resumeModification": {"modified": "false", "markdown": None},
        },
        raw_text='{"content": "plain answer"}',
        json_text='{"content": "plain answer"}',
    )
    command = ConversationRequestCommand(
        conversationId="conv-1",
        userId="user-1",
        currentMessage="hello",
        messageHistory=[],
        requestId="req-1",
        locale="en",
    )

    result = process_conversation(command)

    assert result.data.resume_modification.modified is False
    assert result.data.resume_modification.markdown == ""


def test_fallback_content_extracts_jsonish_content_field():
    raw_text = (
        '{"content": "First line\\nSecond line with an unescaped "quoted" phrase", '
        '"fileUrl": null}'
    )

    content = _fallback_content_from_unparseable_response(raw_text)

    assert "First line\nSecond line" in content
    assert '"quoted"' in content


def test_fallback_content_uses_plain_text_response():
    content = _fallback_content_from_unparseable_response("Plain model answer")

    assert content == "Plain model answer"
