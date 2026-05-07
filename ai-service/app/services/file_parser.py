import logging
import os
from io import BytesIO
from pathlib import Path
from urllib.parse import urlparse, parse_qs
from zipfile import ZipFile
import xml.etree.ElementTree as ET

import httpx
from pypdf import PdfReader

logger = logging.getLogger(__name__)


def _resolve_local_path(object_key: str) -> Path | None:
    """Resolve an object key to a local file path within the shared storage volume.
    将对象键解析为共享存储卷内的本地路径：支持多级目录嵌套，兼容后端多种存储组织方式。"""
    base = Path(os.environ.get("FILE_STORAGE_PATH", "/app/uploads"))
    if not base.exists() or not base.is_dir():
        logger.warning("Local storage base path does not exist: %s", base)
        return None

    for path in base.rglob(object_key):
        if path.is_file():
            logger.debug("Resolved local path: %s", path)
            return path

    return None


def download_file_bytes(file_url: str) -> bytes:
    """Download file bytes from HTTP(S), storage download URLs, or plain object keys.
    统一文件下载入口：支持 HTTP(S) 直连、后端存储下载链接（/api/storage/download?key=...）
    以及纯对象键三种场景，屏蔽底层存储差异。"""
    stripped = file_url.strip()

    if stripped.startswith(("http://", "https://")):
        logger.debug("Downloading file via HTTP: %s", stripped)
        response = httpx.get(stripped, timeout=30.0, follow_redirects=True)
        response.raise_for_status()
        return response.content

    if stripped.startswith("/api/storage/download"):
        parsed = urlparse(stripped)
        keys = parse_qs(parsed.query).get("key", [])
        if keys:
            object_key = keys[0]
            local_path = _resolve_local_path(object_key)
            if local_path:
                logger.info("Reading file from shared storage (by URL key): %s", local_path)
                return local_path.read_bytes()
        raise FileNotFoundError(
            f"Could not resolve local file for storage URL: {stripped}"
        )

    local_path = _resolve_local_path(stripped)
    if local_path:
        logger.info("Reading file from shared storage (by object key): %s", local_path)
        return local_path.read_bytes()

    raise ValueError(
        f"Unsupported file_url (not HTTP, not local path): {stripped}"
    )


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
    # DOCX uses OpenXML namespace; hardcoding avoids external dependency on python-docx.
    # DOCX 采用 OpenXML 命名空间，直接解析 XML 以避免额外依赖。
    namespaces = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}

    parts: list[str] = []
    for node in root.findall(".//w:t", namespaces):
        if node.text:
            parts.append(node.text)

    return " ".join(parts).strip()


def _extract_text_from_plain(file_bytes: bytes) -> str:
    return file_bytes.decode("utf-8", errors="ignore").strip()


def extract_resume_text(file_bytes: bytes, content_format: str) -> str:
    """Route file bytes to the appropriate extractor based on declared MIME type or file extension.
    根据声明的 MIME 类型或文件扩展名路由到对应提取器：支持 PDF、DOCX、TXT/Markdown。"""
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
