import base64
import re
from pathlib import Path
from typing import Any

import httpx

from app.schemas import ParsedJobContent
from app.services.file_parser import download_file_bytes
from app.services.llm_client import (
    generate_json_from_image_prompt,
    generate_json_from_text_prompt,
)


def _normalize_requirements(value: Any) -> list[str]:
    if isinstance(value, list):
        return [str(item).strip() for item in value if str(item).strip()]

    if isinstance(value, str) and value.strip():
        return [value.strip()]

    return []


def _build_job_content(data: dict[str, Any]) -> ParsedJobContent:
    return ParsedJobContent(
        title=str(data.get("title", "")).strip(),
        company=str(data.get("company", "")).strip(),
        description=str(data.get("description", "")).strip(),
        requirements=_normalize_requirements(data.get("requirements")),
    )


def is_job_content_incomplete(content: ParsedJobContent) -> bool:
    return (
        not content.title.strip()
        or not content.company.strip()
        or not content.description.strip()
    )


def parse_job_text(markdown_text: str) -> ParsedJobContent:
    cleaned_text = markdown_text.strip()
    if not cleaned_text:
        raise ValueError("Scraped job page text is empty.")

    prompt = f"""
You are an information extraction system for job postings.

Extract the job posting into valid JSON only.
Do not include markdown fences.
Do not include explanations.

Return exactly one JSON object with this shape:
{{
  "title": "string",
  "company": "string",
  "description": "string",
  "requirements": ["string", "string"]
}}

Rules:
- title: the actual job title
- company: the hiring company name if available
- description: a concise but complete cleaned summary of the role
- requirements: a list of concrete required skills, technologies, qualifications, or experience
- if a field is missing, return an empty string or empty list
- keep requirements short and specific

Job posting text:
\"\"\"
{cleaned_text[:12000]}
\"\"\"
""".strip()

    parsed = generate_json_from_text_prompt(prompt)
    return _build_job_content(parsed)


def _decode_base64_image(data: str) -> tuple[bytes, str]:
    match = re.match(r"data:([\w/]+);base64,(.+)", data)
    if match:
        mime_type = match.group(1)
        image_bytes = base64.b64decode(match.group(2))
        return image_bytes, mime_type
    # 处理纯 Base64 字符串（无 data URI 前缀）
    image_bytes = base64.b64decode(data)
    return image_bytes, "image/png"


def _load_image_bytes(image_url: str) -> tuple[bytes, str]:
    if image_url.startswith("data:"):
        return _decode_base64_image(image_url)

    if image_url.startswith("http://") or image_url.startswith("https://"):
        image_response = httpx.get(image_url, timeout=30.0, follow_redirects=True)
        image_response.raise_for_status()
        return image_response.content, image_response.headers.get("Content-Type", "image/png")

    # 尝试作为本地文件路径，但捕获 OSError（例如超长 Base64 字符串导致的文件名过长）
    try:
        image_path = Path(image_url)
        if image_path.is_file():
            return image_path.read_bytes(), "image/png"
    except OSError:
        pass

    # 兜底：尝试作为纯 Base64 字符串解码（无 data: 前缀）
    return _decode_base64_image(image_url)


def parse_job_from_image(
    screenshot_url: str,
    context_text: str = "",
) -> ParsedJobContent:
    image_bytes, mime_type = _load_image_bytes(screenshot_url)

    prompt = f"""
You are an information extraction system for job posting screenshots.

Return valid JSON only.
Do not include markdown fences.
Do not include explanations.

Extract the visible job posting from the screenshot. Use the optional text context only to fill gaps or clarify details.

Optional text context:
\"\"\"
{context_text[:6000]}
\"\"\"

Return exactly one JSON object with this shape:
{{
  "title": "string",
  "company": "string",
  "description": "string",
  "requirements": ["string", "string"]
}}

Rules:
- title: the actual job title
- company: the hiring company name if visible or provided in context
- description: a concise but complete cleaned summary of the role
- requirements: concrete required skills, technologies, qualifications, or experience
- if a field is missing, return an empty string or empty list
""".strip()

    parsed = generate_json_from_image_prompt(
        prompt=prompt,
        image_bytes=image_bytes,
        mime_type=mime_type,
    )

    return _build_job_content(parsed)


def validate_job_with_vision(
    parsed_content: ParsedJobContent,
    page_text: str,
    screenshot_url: str | None,
) -> ParsedJobContent:
    if not screenshot_url:
        return parsed_content

    image_bytes, mime_type = _load_image_bytes(screenshot_url)

    prompt = f"""
You are validating job posting extraction results using a webpage screenshot.

Return valid JSON only.
Do not include markdown fences.
Do not include explanations.

Review the screenshot and correct the extracted fields if needed.

Current extracted result:
{{
  "title": {parsed_content.title!r},
  "company": {parsed_content.company!r},
  "description": {parsed_content.description!r},
  "requirements": {parsed_content.requirements!r}
}}

Optional page text for context:
\"\"\"
{page_text[:6000]}
\"\"\"

Return exactly one JSON object with this shape:
{{
  "title": "string",
  "company": "string",
  "description": "string",
  "requirements": ["string", "string"]
}}
""".strip()

    parsed = generate_json_from_image_prompt(
        prompt=prompt,
        image_bytes=image_bytes,
        mime_type=mime_type,
    )

    return _build_job_content(parsed)
