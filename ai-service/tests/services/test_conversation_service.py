from unittest.mock import patch

from app.schemas import ConversationRequestCommand
from app.services.conversation_service import _infer_file_format, process_conversation

def test_infer_file_format():
    assert _infer_file_format("http://example.com/resume.pdf") == "pdf"
    assert _infer_file_format("http://example.com/resume.docx") == "docx"
    assert _infer_file_format("http://example.com/resume.txt") == "txt"
    assert _infer_file_format("http://example.com/resume.md") == "md"
    assert _infer_file_format("http://example.com/resume.jpg") is None
    assert _infer_file_format("http://example.com/resume") is None

@patch("app.services.conversation_service.generate_json_from_text_prompt")
def test_process_conversation_preserves_request_context(mock_generate):
    mock_generate.return_value = {
        "content": "你好",
        "fileUrl": None,
        "resumeModification": {"modified": False, "markdown": ""},
    }
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
