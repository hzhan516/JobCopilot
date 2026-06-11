from unittest.mock import patch

from app.schemas import ResumeParseCommand, AiResultEvent, ResumeParseData
from app.services.resume_orchestrator import process_resume


def _make_command(
    file_url="https://example.com/resume.pdf", file_type="pdf"
) -> ResumeParseCommand:
    return ResumeParseCommand(
        resumeId="res-1",
        fileUrl=file_url,
        format=file_type,
    )


@patch("app.services.resume_orchestrator.parse_resume_text")
@patch("app.services.resume_orchestrator.extract_resume_text")
@patch("app.services.resume_orchestrator.download_file_bytes")
def test_process_resume_success(mock_download, mock_extract, mock_parse):
    """Should download, extract, parse and return COMPLETED event / 应下载、提取、解析并返回 COMPLETED 事件"""
    mock_download.return_value = b"raw bytes"
    mock_extract.return_value = "resume markdown text"
    mock_parse.return_value = {
        "name": "Alice",
        "email": "alice@example.com",
        "skills": ["Python"],
        "experience": [],
    }

    result = process_resume(_make_command())

    mock_download.assert_called_once_with("https://example.com/resume.pdf")
    mock_extract.assert_called_once_with(b"raw bytes", "pdf")
    mock_parse.assert_called_once_with("resume markdown text")

    assert isinstance(result, AiResultEvent)
    assert result.reference_id == "res-1"
    assert result.type == "RESUME_PARSE"
    assert result.status == "COMPLETED"
    assert result.error_message is None
    assert result.event_type == "RESUME"
    assert result.data is not None


@patch("app.services.resume_orchestrator.parse_resume_text")
@patch("app.services.resume_orchestrator.extract_resume_text")
@patch("app.services.resume_orchestrator.download_file_bytes")
def test_process_resume_with_different_file_type(
    mock_download, mock_extract, mock_parse
):
    """Should pass file_type to extractor / 应将 file_type 传递给提取器"""
    mock_download.return_value = b"docx bytes"
    mock_extract.return_value = "docx text"
    mock_parse.return_value = {
        "name": "Bob",
        "email": "bob@example.com",
        "skills": [],
        "experience": [],
    }

    process_resume(
        _make_command(file_url="https://example.com/resume.docx", file_type="docx")
    )

    mock_extract.assert_called_once_with(b"docx bytes", "docx")


@patch("app.services.resume_orchestrator.parse_resume_text")
@patch("app.services.resume_orchestrator.extract_resume_text")
@patch("app.services.resume_orchestrator.download_file_bytes")
def test_process_resume_event_data_shape(mock_download, mock_extract, mock_parse):
    """Should wrap parsed content in ResumeParseData / 应将解析内容包装在 ResumeParseData 中"""
    mock_download.return_value = b"bytes"
    mock_extract.return_value = "text"
    mock_parse.return_value = {
        "name": "Charlie",
        "email": "charlie@example.com",
        "skills": ["Go"],
        "experience": [],
    }

    result = process_resume(_make_command())

    assert isinstance(result.data, ResumeParseData)
    assert result.data.parsed_content.name == "Charlie"
    assert result.data.summary == ""
