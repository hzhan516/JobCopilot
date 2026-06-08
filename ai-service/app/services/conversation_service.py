import json
import logging
import re
from pathlib import Path
from urllib.parse import urlparse

from app.schemas import AiResultEvent, ConversationRequestCommand, ConversationData, ResumeModification
from app.services.file_parser import download_file_bytes, extract_resume_text
from app.services.llm_client import LlmJsonParseError, generate_json_from_text_prompt_with_repair


logger = logging.getLogger(__name__)

CONVERSATION_JSON_CONTRACT = """
{
  "content": "string",
  "fileUrl": null,
  "resumeModification": {
    "modified": false,
    "markdown": "string"
  }
}
""".strip()


def _infer_file_format(file_url: str) -> str | None:
    """Derive supported attachment formats from URL suffix.
    根据 URL 后缀推断附件格式，限制支持范围以降低解析复杂度与安全攻击面。"""
    suffix = Path(urlparse(file_url).path).suffix.lower()
    if suffix == ".pdf":
        return "pdf"
    if suffix == ".docx":
        return "docx"
    if suffix == ".txt":
        return "txt"
    if suffix == ".md":
        return "md"
    return None


def _load_attachment_context(command: ConversationRequestCommand) -> tuple[list[dict[str, str]], list[str]]:
    """Download and extract text snippets from up to 3 attachments for prompt enrichment.
    下载并提取最多 3 个附件的文本片段：限制数量与单片段长度（4000 字符），
    防止超长附件撑爆 prompt token 上限，同时收集告警用于模型自检。"""
    attachments: list[dict[str, str]] = []
    warnings: list[str] = []

    for file_url in command.file_urls[:3]:
        file_format = _infer_file_format(file_url)
        if not file_format:
            warnings.append(f"Skipped unsupported attachment format: {file_url}")
            continue

        try:
            file_bytes = download_file_bytes(file_url)
            extracted_text = extract_resume_text(file_bytes, file_format)
        except Exception as exc:
            warnings.append(f"Failed to read attachment {file_url}: {exc}")
            continue

        snippet = extracted_text.strip()
        if not snippet:
            warnings.append(f"Attachment had no readable text: {file_url}")
            continue

        attachments.append(
            {
                "fileUrl": file_url,
                "format": file_format,
                "textSnippet": snippet[:4000],
            }
        )

    return attachments, warnings


def _build_conversation_prompt(command: ConversationRequestCommand) -> str:
    """Compose a structured LLM prompt that grounds the reply in resume, job, and attachment context.
    构建结构化对话 prompt：将简历、职位、附件及历史消息组织为统一上下文，
    通过严格的 JSON 输出格式约束，保证下游可直接解析而不需额外的后处理清洗。"""
    history = [
        message.model_dump(by_alias=True)
        for message in command.message_history
    ]
    attachments, warnings = _load_attachment_context(command)

    return f"""
You are an AI assistant for a resume and job application support system.

The user may ask for resume improvement, job application advice, interview preparation,
or help understanding how their resume matches a job.

Return valid JSON only.
Do not include markdown fences.
Do not include explanations outside JSON.

Return exactly one JSON object with this shape:
{{
  "content": "string",
  "fileUrl": null,
  "resumeModification": {{
    "modified": false,
    "markdown": "string"
  }}
}}

Rules:
- content: your reply to the user
- fileUrl: null unless a generated file URL is explicitly available
- resumeModification.modified: true ONLY if the user asked you to rewrite/optimize their resume AND you did so
- resumeModification.markdown: the full rewritten markdown of their resume, if modified=true. Otherwise empty string.
- be practical and specific
- use attached file content when readable text is provided below
- do not invent missing attachment contents
- if you do not actually have resume text, job text, or attachment text, say so clearly
- if the user asks whether you can see their resume, answer truthfully based on the provided attachment content
- do not claim you reviewed a resume, job description, or file unless that content is present below
- avoid generic openings such as "Thank you for your question"
- do not give broad advice when the user asks a yes/no or visibility question; answer the question first
- answer in the same language as the user's current message when possible

Conversation ID:
{command.conversation_id}

User ID:
{command.user_id}

Resume Version ID:
{command.resume_version_id}

Main Resume Text:
{command.resume_text or "None provided"}

Primary Job Context:
{command.primary_job_text or "None provided"}

Additional Job Contexts:
{json.dumps(command.related_job_texts or [], ensure_ascii=False, indent=2)}

Attached File URLs:
{json.dumps(command.file_urls, ensure_ascii=False, indent=2)}

Readable Attachment Content:
{json.dumps(attachments, ensure_ascii=False, indent=2)}

Attachment Warnings:
{json.dumps(warnings, ensure_ascii=False, indent=2)}

Message History:
{json.dumps(history, ensure_ascii=False, indent=2)}

Current Message:
{command.current_message}
""".strip()


def _strip_code_fences(text: str) -> str:
    cleaned = text.strip()
    if cleaned.startswith("```"):
        cleaned = re.sub(r"^```(?:json)?\s*", "", cleaned)
        cleaned = re.sub(r"\s*```$", "", cleaned)
    return cleaned.strip()


def _extract_jsonish_string_field(text: str, field_name: str) -> str | None:
    """Extract a string field from malformed JSON-ish model output.

    The scanner tolerates unescaped quotes inside the value and only stops on a quote
    that looks like a JSON field boundary.
    """
    pattern = re.compile(rf'"{re.escape(field_name)}"\s*:\s*"')
    match = pattern.search(text)
    if not match:
        return None

    value_chars: list[str] = []
    i = match.end()
    while i < len(text):
        char = text[i]

        if char == "\\" and i + 1 < len(text):
            next_char = text[i + 1]
            if next_char == "n":
                value_chars.append("\n")
            elif next_char == "r":
                value_chars.append("\r")
            elif next_char == "t":
                value_chars.append("\t")
            else:
                value_chars.append(next_char)
            i += 2
            continue

        if char == '"':
            lookahead = text[i + 1:].lstrip()
            if lookahead.startswith(",") or lookahead.startswith("}"):
                break
            value_chars.append(char)
            i += 1
            continue

        value_chars.append(char)
        i += 1

    value = "".join(value_chars).strip()
    return value or None


def _fallback_content_from_unparseable_response(raw_text: str) -> str:
    """Recover a user-visible reply from malformed JSON before giving up."""
    cleaned = _strip_code_fences(raw_text)
    content = _extract_jsonish_string_field(cleaned, "content")
    if content:
        return content

    # If the model ignored the JSON contract and returned plain text, use it directly.
    if not cleaned.startswith("{"):
        return cleaned

    logger.warning(
        "Could not recover content field from malformed conversation JSON; using generic fallback"
    )
    return "I generated a reply, but it could not be formatted correctly. Please try again."


def _normalize_conversation_result(result: dict) -> tuple[str, str | None, dict]:
    raw_content = result.get("content")
    content = "" if raw_content is None else str(raw_content).strip()
    file_url = result.get("fileUrl")

    if not content:
        content = "I received your message, but I could not generate a detailed response."

    if file_url is not None:
        file_url = str(file_url).strip() or None

    resume_modification = result.get("resumeModification")
    if not isinstance(resume_modification, dict):
        resume_modification = {"modified": False, "markdown": ""}
    else:
        resume_modification = {
            "modified": _coerce_bool(resume_modification.get("modified", False)),
            "markdown": str(resume_modification.get("markdown") or ""),
        }

    return content, file_url, resume_modification


def _coerce_bool(value: object) -> bool:
    """Coerce common model-produced boolean variants without treating 'false' as truthy."""
    if isinstance(value, bool):
        return value
    if isinstance(value, str):
        return value.strip().lower() in {"true", "1", "yes"}
    return bool(value)


def process_conversation(command: ConversationRequestCommand) -> AiResultEvent:
    """Execute the conversation workflow and package the LLM response into a standardized event.
    执行对话工作流：调用 LLM 生成回复，对空内容做兜底处理，并将结果封装为标准事件回传后端。"""
    prompt = _build_conversation_prompt(command)
    logger.info(
        "Conversation request: conversation_id=%s, history_count=%d, file_url_count=%d",
        command.conversation_id,
        len(command.message_history),
        len(command.file_urls),
    )
    fallback_used = False
    repaired = False
    try:
        generation = generate_json_from_text_prompt_with_repair(
            prompt,
            repair_context=CONVERSATION_JSON_CONTRACT,
        )
        result = generation.data
        repaired = generation.repaired
    except LlmJsonParseError as exc:
        fallback_used = True
        result = {
            "content": _fallback_content_from_unparseable_response(exc.raw_text),
            "fileUrl": None,
            "resumeModification": {"modified": False, "markdown": ""},
        }
        logger.warning(
            "Conversation JSON fallback used: conversation_id=%s, raw_text_length=%d, original_error=%s, repair_error=%s",
            command.conversation_id,
            len(exc.raw_text),
            exc.original_error,
            exc.repair_error,
        )

    content, file_url, resume_modification = _normalize_conversation_result(result)
    logger.info(
        "Conversation model result received: conversation_id=%s, content_length=%d, has_file_url=%s, has_resume_modification=%s, repaired=%s, fallback_used=%s",
        command.conversation_id,
        len(content),
        file_url is not None,
        bool(resume_modification),
        repaired,
        fallback_used,
    )

    return AiResultEvent(
        referenceId=command.conversation_id,
        type="CONVERSATION_REPLY",
        status="COMPLETED",
        data=ConversationData(
            content=content,
            fileUrl=file_url,
            requestId=command.request_id,
            locale=command.locale,
            resumeModification=ResumeModification(
                modified=resume_modification["modified"],
                markdown=resume_modification["markdown"],
            ),
        ),
        errorMessage=None,
        eventType=None,
    )
