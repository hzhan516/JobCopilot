import json
import logging
from pathlib import Path
from urllib.parse import urlparse

# Conversation workflow helpers: load attachments, build prompts, and produce AI responses.

from app.schemas import AiResultEvent, ConversationRequestCommand
from app.services.file_parser import download_file_bytes, extract_resume_text
from app.services.llm_client import generate_json_from_text_prompt


logger = logging.getLogger(__name__)


# Infer supported attachment type from a URL suffix.
def _infer_file_format(file_url: str) -> str | None:
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


# Download and extract attachment snippets plus warnings for the prompt.
def _load_attachment_context(command: ConversationRequestCommand) -> tuple[list[dict[str, str]], list[str]]:
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


# Build the LLM prompt for the conversation reply.
def _build_conversation_prompt(command: ConversationRequestCommand) -> str:
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


# Run the conversation workflow and package the response as an AI result event.
def process_conversation(command: ConversationRequestCommand) -> AiResultEvent:
    prompt = _build_conversation_prompt(command)
    logger.info(
        "Conversation request: conversation_id=%s, history_count=%d, file_url_count=%d",
        command.conversation_id,
        len(command.message_history),
        len(command.file_urls),
    )
    result = generate_json_from_text_prompt(prompt)

    content = str(result.get("content", "")).strip()
    file_url = result.get("fileUrl")

    if not content:
        content = "I received your message, but I could not generate a detailed response."

    if file_url is not None:
        file_url = str(file_url).strip() or None
        
    resume_modification = result.get("resumeModification")
    logger.info(
        "Conversation model result received: conversation_id=%s, content_length=%d, has_file_url=%s, has_resume_modification=%s",
        command.conversation_id,
        len(content),
        file_url is not None,
        bool(resume_modification),
    )

    return AiResultEvent(
        referenceId=command.conversation_id,
        type="CONVERSATION_REPLY",
        status="COMPLETED",
        data={
            "content": content,
            "fileUrl": file_url,
            "resumeModification": resume_modification
        },
        errorMessage=None,
        eventType=None,
    )
