from typing import Any

from app.schemas import ParsedResumeContent, ExperienceItem
from app.services.llm_client import generate_json_from_text_prompt


def _normalize_skills(value: Any) -> list[str]:
    if isinstance(value, list):
        return [str(item).strip() for item in value if str(item).strip()]

    if isinstance(value, str) and value.strip():
        return [value.strip()]

    return []


def _normalize_experience(value: Any) -> list[ExperienceItem]:
    if isinstance(value, list):
        normalized: list[ExperienceItem] = []
        for item in value:
            if isinstance(item, dict):
                normalized.append(
                    ExperienceItem(
                        company=str(item.get("company", "")).strip() or None,
                        title=str(item.get("title", "")).strip() or None,
                        duration=str(item.get("duration", "")).strip() or None,
                        summary=str(item.get("summary", "")).strip() or None,
                    )
                )
            elif item is not None:
                normalized.append(ExperienceItem(summary=str(item)))
        return normalized

    if isinstance(value, dict):
        return [
            ExperienceItem(
                company=str(value.get("company", "")).strip() or None,
                title=str(value.get("title", "")).strip() or None,
                duration=str(value.get("duration", "")).strip() or None,
                summary=str(value.get("summary", "")).strip() or None,
            )
        ]

    if isinstance(value, str) and value.strip():
        return [ExperienceItem(summary=value.strip())]

    return []


def parse_resume_text(resume_text: str) -> ParsedResumeContent:
    """Extract structured resume fields using an LLM with normalization post-processing.
    使用 LLM 提取结构化简历字段：限制输入长度（12000 字符）控制成本，
    并对解析结果做归一化后处理，兼容模型返回的多种数据类型（列表、字符串、字典）。"""
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
