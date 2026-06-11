import glob
import ipaddress
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

    # Prevent path traversal and glob injection.
    # 防止路径遍历与 glob 注入：拒绝包含路径分隔符、父目录引用或 glob 元字符的 key。
    if ".." in object_key or any(c in object_key for c in ("/", "\\", "*", "?", "[")):
        return None

    # Try direct path first (支持子目录结构)
    direct = base / object_key
    try:
        direct.resolve().relative_to(base.resolve())
        if direct.is_file():
            return direct
    except ValueError:
        return None
    except OSError:
        pass

    # Fallback: search by exact basename under base directory.
    # 兜底：按精确文件名递归查找。
    basename = Path(object_key).name
    for path in base.rglob(glob.escape(basename)):
        if path.is_file():
            try:
                path.resolve().relative_to(base.resolve())
                return path
            except ValueError:
                continue
    return None


def _is_dangerous_url(url: str) -> bool:
    """Block HTTP requests to internal / private IP addresses to mitigate SSRF.
    阻止对内部/私有 IP 地址的 HTTP 请求，降低 SSRF 攻击面。"""
    parsed = urlparse(url)
    hostname = parsed.hostname
    if not hostname:
        return True
    if hostname.lower() in {"localhost", "127.0.0.1", "0.0.0.0", "::1"}:
        return True
    try:
        addr = ipaddress.ip_address(hostname)
        if (
            addr.is_private
            or addr.is_loopback
            or addr.is_reserved
            or addr.is_link_local
            or addr.is_multicast
        ):
            return True
    except ValueError:
        pass
    return False


def download_file_bytes(file_url: str) -> bytes:
    """Download file bytes from HTTP(S), storage download URLs, or plain object keys.
    统一文件下载入口：支持 HTTP(S) 直连、后端存储下载链接（/api/storage/download?key=...）
    以及纯对象键三种场景，屏蔽底层存储差异。"""
    stripped = file_url.strip()

    if stripped.startswith(("http://", "https://")):
        if _is_dangerous_url(stripped):
            raise ValueError(
                f"URL resolves to internal address and is not allowed: {stripped}"
            )
        logger.debug("Downloading file via HTTP: %s", stripped)
        response = httpx.get(stripped, timeout=30.0, follow_redirects=True)
        response_url_str = str(response.url)
        if response_url_str.startswith(("http://", "https://")) and _is_dangerous_url(
            response_url_str
        ):
            raise ValueError(f"Redirected to disallowed URL: {response.url}")
        response.raise_for_status()
        return response.content

    if stripped.startswith("/api/storage/download"):
        parsed = urlparse(stripped)
        keys = parse_qs(parsed.query).get("key", [])
        if keys:
            object_key = keys[0]
            local_path = _resolve_local_path(object_key)
            if local_path:
                logger.info(
                    "Reading file from shared storage (by URL key): %s", local_path
                )
                return local_path.read_bytes()
        raise FileNotFoundError(
            f"Could not resolve local file for storage URL: {stripped}"
        )

    local_path = _resolve_local_path(stripped)
    if local_path:
        logger.info("Reading file from shared storage (by object key): %s", local_path)
        return local_path.read_bytes()

    raise ValueError(f"Unsupported file_url (not HTTP, not local path): {stripped}")


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
