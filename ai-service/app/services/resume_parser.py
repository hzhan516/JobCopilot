from typing import Any

from app.schemas import ParsedResumeContent
from app.services.llm_client import generate_json_from_text_prompt


def _normalize_skills(value: Any) -> list[str]:
    if isinstance(value, list):
        return [str(item).strip() for item in value if str(item).strip()]

    if isinstance(value, str) and value.strip():
        return [value.strip()]

    return []


def _normalize_experience(value: Any) -> list[dict[str, Any]]:
    if isinstance(value, list):
        normalized: list[dict[str, Any]] = []
        for item in value:
            if isinstance(item, dict):
                normalized.append(item)
            elif item is not None:
                normalized.append({"summary": str(item)})
        return normalized

    if isinstance(value, dict):
        return [value]

    if isinstance(value, str) and value.strip():
        return [{"summary": value.strip()}]

    return []


def parse_resume_text(resume_text: str) -> ParsedResumeContent:
    cleaned_text = resume_text.strip()
    if not cleaned_text:
        raise ValueError("Extracted resume text is empty.")

    prompt = f"""
You are an information extraction system for resumes.

Extract the resume into valid JSON only.
Do not include markdown fences.
Do not include explanations.

Return exactly one JSON object with this shape:
{{
  "name": "string",
  "email": "string",
  "skills": ["string", "string"],
  "experience": [
    {{
      "company": "string",
      "title": "string",
      "duration": "string",
      "summary": "string"
    }}
  ]
}}

Rules:
- name: candidate full name if available
- email: candidate email if available
- skills: concrete skills, tools, technologies, or qualifications
- experience: work experience entries if present
- if a field is missing, return an empty string or empty list
- return concise and clean values
- do not invent facts not supported by the resume

Resume text:
\"\"\"
{cleaned_text[:12000]}
\"\"\"
""".strip()

    parsed = generate_json_from_text_prompt(prompt)

    return ParsedResumeContent(
        name=str(parsed.get("name", "")).strip() or None,
        email=str(parsed.get("email", "")).strip() or None,
        skills=_normalize_skills(parsed.get("skills")),
        experience=_normalize_experience(parsed.get("experience")),
    )
