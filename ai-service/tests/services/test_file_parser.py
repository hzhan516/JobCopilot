import pytest
from unittest.mock import MagicMock, patch
from io import BytesIO
from zipfile import ZipFile

from app.services.file_parser import (
    download_file_bytes,
    _extract_text_from_pdf,
    _extract_text_from_docx,
    _extract_text_from_plain,
    extract_resume_text,
)


@patch("httpx.get")
def test_download_file_bytes_success(mock_get):
    mock_response = MagicMock()
    mock_response.content = b"file content"
    mock_response.raise_for_status.return_value = None
    mock_get.return_value = mock_response

    result = download_file_bytes("http://test.com/file")
    assert result == b"file content"
    mock_get.assert_called_once_with(
        "http://test.com/file", timeout=30.0, follow_redirects=True
    )


@patch("httpx.get")
def test_download_file_bytes_failure(mock_get):
    mock_response = MagicMock()
    mock_response.raise_for_status.side_effect = Exception("HTTP Error")
    mock_get.return_value = mock_response

    with pytest.raises(Exception, match="HTTP Error"):
        download_file_bytes("http://test.com/file")


def test_download_file_bytes_from_storage_url(tmp_path, monkeypatch):
    monkeypatch.setenv("FILE_STORAGE_PATH", str(tmp_path))
    target = tmp_path / "resumes" / "myfile.pdf"
    target.parent.mkdir(parents=True)
    target.write_bytes(b"local pdf content")

    result = download_file_bytes("/api/storage/download?key=myfile.pdf&expiry=123456")
    assert result == b"local pdf content"


def test_download_file_bytes_from_object_key(tmp_path, monkeypatch):
    monkeypatch.setenv("FILE_STORAGE_PATH", str(tmp_path))
    target = tmp_path / "resumes" / "2026" / "05" / "03" / "uuid_file.pdf"
    target.parent.mkdir(parents=True)
    target.write_bytes(b"nested file content")

    result = download_file_bytes("uuid_file.pdf")
    assert result == b"nested file content"


def test_download_file_bytes_local_not_found(tmp_path, monkeypatch):
    monkeypatch.setenv("FILE_STORAGE_PATH", str(tmp_path))
    with pytest.raises(ValueError, match="Unsupported file_url"):
        download_file_bytes("nonexistent.pdf")


@patch("app.services.file_parser.PdfReader")
def test_extract_text_from_pdf(mock_reader_class):
    mock_reader = MagicMock()
    mock_page1 = MagicMock()
    mock_page1.extract_text.return_value = "Page 1 text"
    mock_page2 = MagicMock()
    mock_page2.extract_text.return_value = "Page 2 text"
    mock_reader.pages = [mock_page1, mock_page2]
    mock_reader_class.return_value = mock_reader

    result = _extract_text_from_pdf(b"fake pdf bytes")
    assert result == "Page 1 text\nPage 2 text"


def test_extract_text_from_docx():
    xml_content = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
        <w:body>
            <w:p><w:r><w:t>Hello</w:t></w:r></w:p>
            <w:p><w:r><w:t>World</w:t></w:r></w:p>
        </w:body>
    </w:document>
    """

    zip_buffer = BytesIO()
    with ZipFile(zip_buffer, "w") as zip_file:
        zip_file.writestr("word/document.xml", xml_content)

    docx_bytes = zip_buffer.getvalue()

    result = _extract_text_from_docx(docx_bytes)
    assert result == "Hello World"


def test_extract_text_from_plain():
    result = _extract_text_from_plain(b"Hello World")
    assert result == "Hello World"


@patch("app.services.file_parser._extract_text_from_pdf")
def test_extract_resume_text_pdf(mock_extract):
    mock_extract.return_value = "pdf text"
    assert extract_resume_text(b"bytes", "pdf") == "pdf text"
    assert extract_resume_text(b"bytes", "application/pdf") == "pdf text"


@patch("app.services.file_parser._extract_text_from_docx")
def test_extract_resume_text_docx(mock_extract):
    mock_extract.return_value = "docx text"
    assert extract_resume_text(b"bytes", "docx") == "docx text"
    assert (
        extract_resume_text(
            b"bytes",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        )
        == "docx text"
    )


@patch("app.services.file_parser._extract_text_from_plain")
def test_extract_resume_text_plain(mock_extract):
    mock_extract.return_value = "plain text"
    assert extract_resume_text(b"bytes", "txt") == "plain text"
    assert extract_resume_text(b"bytes", "text/plain") == "plain text"
    assert extract_resume_text(b"bytes", "md") == "plain text"


def test_extract_resume_text_unsupported():
    with pytest.raises(ValueError, match="Unsupported resume format: unknown"):
        extract_resume_text(b"bytes", "unknown")
