"""Test file parser module (supplemental).
文件解析补充测试：覆盖空文件、损坏文件、超大文件等边界场景。
"""

from io import BytesIO
from unittest.mock import MagicMock, patch

import pytest

from app.services.file_parser import (
    _extract_text_from_pdf,
    _extract_text_from_docx,
    extract_resume_text,
)

# ── Empty file scenarios ───────────────────────────────────


@patch("app.services.file_parser.PdfReader")
def test_extract_text_from_pdf_empty(mock_reader_class):
    """Empty PDF (0 pages) should return empty string.
    空 PDF（0 页）应返回空字符串。"""
    mock_reader = MagicMock()
    mock_reader.pages = []
    mock_reader_class.return_value = mock_reader

    result = _extract_text_from_pdf(b"empty")
    assert result == ""


def test_extract_text_from_docx_empty():
    """Empty DOCX (no paragraphs) should return empty string.
    空 DOCX（无段落）应返回空字符串。"""
    from zipfile import ZipFile

    xml = '<?xml version="1.0"?><w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body></w:body></w:document>'
    buf = BytesIO()
    with ZipFile(buf, "w") as zf:
        zf.writestr("word/document.xml", xml)

    result = _extract_text_from_docx(buf.getvalue())
    assert result == ""


# ── Corrupted file scenarios ─────────────────────────────


@patch("app.services.file_parser.PdfReader")
def test_extract_text_from_pdf_corrupted(mock_reader_class):
    """Corrupted PDF should raise Exception.
    损坏的 PDF 应抛出 Exception。"""
    mock_reader_class.side_effect = Exception("PDF corrupted")

    with pytest.raises(Exception, match="PDF corrupted"):
        _extract_text_from_pdf(b"not a pdf")


def test_extract_text_from_docx_corrupted():
    """Corrupted DOCX (invalid zip) should raise BadZipFile.
    损坏的 DOCX（非法 zip）应抛出 BadZipFile。"""
    import zipfile

    with pytest.raises(zipfile.BadZipFile):
        _extract_text_from_docx(b"not a zip")


# ── Large file scenarios ───────────────────────────────────


@patch("app.services.file_parser.PdfReader")
def test_extract_text_from_pdf_large(mock_reader_class):
    """Large PDF (many pages) should concatenate all text.
    大 PDF（多页）应拼接所有文本。"""
    mock_reader = MagicMock()
    mock_pages = []
    for i in range(100):
        p = MagicMock()
        p.extract_text.return_value = f"Page {i} content. "
        mock_pages.append(p)
    mock_reader.pages = mock_pages
    mock_reader_class.return_value = mock_reader

    result = _extract_text_from_pdf(b"large")
    assert "Page 0 content." in result
    assert "Page 99 content." in result


# ── extract_resume_text edge cases ─────────────────────────


@patch("app.services.file_parser._extract_text_from_pdf")
def test_extract_resume_text_empty_pdf(mock_extract):
    """Empty PDF content should be returned as-is.
    空 PDF 内容应原样返回。"""
    mock_extract.return_value = ""
    assert extract_resume_text(b"bytes", "pdf") == ""


@patch("app.services.file_parser._extract_text_from_plain")
def test_extract_resume_text_whitespace_only(mock_extract):
    """Whitespace-only content should be preserved.
    仅空白字符的内容应被保留。"""
    mock_extract.return_value = "   \n\t   "
    assert extract_resume_text(b"bytes", "txt") == "   \n\t   "


# ── Unsupported format scenarios ─────────────────────────


def test_extract_resume_text_unsupported_format():
    """Unsupported format should raise ValueError with clear message.
    不支持的格式应抛出带有明确信息的 ValueError。"""
    with pytest.raises(ValueError, match="Unsupported resume format"):
        extract_resume_text(b"bytes", "exe")


def test_extract_resume_text_case_insensitive():
    """Format should be case-insensitive.
    格式应大小写不敏感。"""
    with patch("app.services.file_parser._extract_text_from_pdf") as mock_extract:
        mock_extract.return_value = "text"
        assert extract_resume_text(b"bytes", "PDF") == "text"
        assert extract_resume_text(b"bytes", "Pdf") == "text"
