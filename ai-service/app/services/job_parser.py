from typing import Any

import base64
import re
from pathlib import Path

import httpx

from app.schemas import ParsedJobContent
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
        salary=str(data.get("salary", "")).strip() or None,
        location=str(data.get("location", "")).strip() or None,
    )


def is_job_content_incomplete(content: ParsedJobContent) -> bool:
    """Check whether critical fields are missing so we can trigger vision fallback.
    校验关键字段是否缺失：用于决定是否需要触发截图 vision 解析作为兜底。"""
    return (
        not content.title.strip()
        or not content.company.strip()
        or not content.description.strip()
    )


def parse_job_text(markdown_text: str) -> ParsedJobContent:
    """Extract structured job fields from cleaned page text using an LLM.
    从清洗后的页面文本中提取结构化职位信息：限制输入长度（12000 字符）以控制 token 成本，
    同时通过严格的 JSON schema 约束降低解析失败率。"""
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
  "requirements": ["string", "string"],
  "salary": "string",
  "location": "string"
}}

Rules:
- title: the actual job title
- company: the hiring company name if available
- description: a concise but complete cleaned summary of the role
- requirements: a list of concrete required skills, technologies, qualifications, or experience
- salary: the salary range or compensation details if provided
- location: the geographic location, remote status, or workplace type
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
    # Plain base64 string without data URI prefix defaults to PNG.
    # 无 data URI 前缀的纯 Base64 字符串默认按 PNG 处理。
    image_bytes = base64.b64decode(data)
    return image_bytes, "image/png"


def _load_image_bytes(image_url: str) -> tuple[bytes, str]:
    """Load image bytes from data-URI or HTTP(S) URL only.
    Local file path access is intentionally blocked to prevent LFI vulnerabilities.
    仅支持 data URI 与 HTTP(S) 远程地址加载图片；本地文件路径访问被显式禁止，防止目录遍历攻击。
    """
    if image_url.startswith("data:"):
        return _decode_base64_image(image_url)

    if image_url.startswith("http://") or image_url.startswith("https://"):
        image_response = httpx.get(image_url, timeout=30.0, follow_redirects=True)
        image_response.raise_for_status()
        return image_response.content, image_response.headers.get("Content-Type", "image/png")

    # Reject anything that is not a known remote URL scheme to prevent LFI.
    raise ValueError(f"Unsupported image URL scheme (local file access is blocked): {image_url[:50]}")


def parse_job_from_image(
    screenshot_url: str,
    context_text: str = "",
) -> ParsedJobContent:
    """Extract structured job fields from a screenshot using vision LLM with optional text context.
    从截图中提取职位信息：vision 模型处理可见内容，页面文本作为辅助上下文仅用于填补或澄清细节，
    避免过度依赖可能不准确的 scraped text。"""
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
  "requirements": ["string", "string"],
  "salary": "string",
  "location": "string"
}}

Rules:
- title: the actual job title
- company: the hiring company name if visible or provided in context
- description: a concise but complete cleaned summary of the role
- requirements: concrete required skills, technologies, qualifications, or experience
- salary: the salary range or compensation details if provided
- location: the geographic location, remote status, or workplace type
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
    """Cross-check and correct parsed job fields against a screenshot to reduce hallucination.
    使用截图交叉验证已解析的职位字段：当 scraped text 存在歧义或截断时，
    vision 模型可基于真实页面视觉内容进行校正，降低幻觉风险。"""
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
