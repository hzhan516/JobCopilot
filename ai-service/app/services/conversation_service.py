import json

from app.schemas import AiResultEvent, ConversationRequestCommand
from app.services.gemini_client import generate_json_from_text_prompt


def _build_conversation_prompt(command: ConversationRequestCommand) -> str:
    history = [
        message.model_dump(by_alias=True)
        for message in command.message_history
    ]

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
  "fileUrl": null
}}

Rules:
- content: your reply to the user
- fileUrl: null unless a generated file URL is explicitly available
- be practical and specific
- do not invent uploaded file contents if they are not provided
- if fileUrls are provided, mention that files are attached but cannot be edited directly yet
- answer in the same language as the user's current message when possible

Conversation ID:
{command.conversation_id}

User ID:
{command.user_id}

Resume Version ID:
{command.resume_version_id}

Attached File URLs:
{json.dumps(command.file_urls, ensure_ascii=False, indent=2)}

Message History:
{json.dumps(history, ensure_ascii=False, indent=2)}

Current Message:
{command.current_message}
""".strip()


def process_conversation(command: ConversationRequestCommand) -> AiResultEvent:
    prompt = _build_conversation_prompt(command)
    result = generate_json_from_text_prompt(prompt)

    content = str(result.get("content", "")).strip()
    file_url = result.get("fileUrl")

    if not content:
        content = "I received your message, but I could not generate a detailed response."

    if file_url is not None:
        file_url = str(file_url).strip() or None

    return AiResultEvent(
        referenceId=command.conversation_id,
        type="CONVERSATION_REPLY",
        status="COMPLETED",
        data={
            "content": content,
            "fileUrl": file_url,
        },
        errorMessage=None,
        eventType=None,
    )
