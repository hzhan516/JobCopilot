from io import BytesIO
from zipfile import ZipFile
import xml.etree.ElementTree as ET

import httpx
from pypdf import PdfReader


def download_file_bytes(file_url: str) -> bytes:
    response = httpx.get(file_url, timeout=30.0, follow_redirects=True)
    response.raise_for_status()
    return response.content


def _extract_text_from_pdf(file_bytes: bytes) -> str:
    reader = PdfReader(BytesIO(file_bytes))
    parts: list[str] = []

    for page in reader.pages:
        text = page.extract_text() or ""
        if text.strip():
            parts.append(text.strip())

    return "\n".join(parts).strip()


def _extract_text_from_docx(file_bytes: bytes) -> str:
    with ZipFile(BytesIO(file_bytes)) as docx_zip:
        xml_bytes = docx_zip.read("word/document.xml")

    root = ET.fromstring(xml_bytes)
    namespaces = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}

    parts: list[str] = []
    for node in root.findall(".//w:t", namespaces):
        if node.text:
            parts.append(node.text)

    return " ".join(parts).strip()


def _extract_text_from_plain(file_bytes: bytes) -> str:
    return file_bytes.decode("utf-8", errors="ignore").strip()


def extract_resume_text(file_bytes: bytes, content_format: str) -> str:
    normalized = content_format.strip().lower()

    if normalized in {"pdf", "application/pdf"}:
        return _extract_text_from_pdf(file_bytes)

    if normalized in {
        "docx",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    }:
        return _extract_text_from_docx(file_bytes)

    if normalized in {"txt", "text/plain", "text/markdown", "md"}:
        return _extract_text_from_plain(file_bytes)

    raise ValueError(f"Unsupported resume format: {content_format}")
