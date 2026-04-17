import json
import re
from pathlib import Path


OUTPUT_DIR = Path(__file__).resolve().parents[1] / "output"
PAIRS_FILE = OUTPUT_DIR / "training_pairs_sample.jsonl"
FEATURES_FILE = OUTPUT_DIR / "training_features_sample.jsonl"


def normalize_items(items: list[str]) -> set[str]:
    normalized: set[str] = set()

    for item in items:
        cleaned = item.strip().lower()
        if cleaned:
            normalized.add(cleaned)

    return normalized


def tokenize_text(text: str) -> set[str]:
    tokens = re.findall(r"[a-zA-Z]+", text.lower())
    return {token for token in tokens if len(token) >= 3}


def build_experience_text(experience_items: list[dict]) -> str:
    parts: list[str] = []

    for item in experience_items:
        if not isinstance(item, dict):
            continue

        for key in ("title", "summary", "company"):
            value = item.get(key)
            if value:
                parts.append(str(value))

    return " ".join(parts)


def build_feature_row(row: dict) -> dict:
    resume_skills = normalize_items(row.get("resume_skills", []))
    job_requirements = normalize_items(row.get("job_requirements", []))

    overlap = resume_skills & job_requirements

    if job_requirements:
        skill_overlap_ratio = len(overlap) / len(job_requirements)
    else:
        skill_overlap_ratio = 0.0

    resume_experience_text = build_experience_text(row.get("resume_experience", []))
    job_text = " ".join([
        row.get("job_title", ""),
        row.get("job_description", ""),
        row.get("job_skills_desc", ""),
    ])

    resume_text_tokens = tokenize_text(resume_experience_text)
    job_text_tokens = tokenize_text(job_text)
    title_tokens = tokenize_text(row.get("job_title", ""))

    experience_overlap = resume_text_tokens & job_text_tokens
    title_overlap = resume_skills & title_tokens

    return {
        "resume_id": row.get("resume_id"),
        "job_id": row.get("job_id"),
        "job_title": row.get("job_title", ""),
        "resume_skill_count": len(resume_skills),
        "job_requirement_count": len(job_requirements),
        "skill_overlap_count": len(overlap),
        "skill_overlap_ratio": round(skill_overlap_ratio, 4),
        "title_keyword_overlap": len(title_overlap),
        "experience_description_overlap": len(experience_overlap),
        "label_suitable": row.get("suitable"),
        "label_score": row.get("score"),
    }


def main() -> None:
    written = 0

    with PAIRS_FILE.open("r", encoding="utf-8") as input_file, \
         FEATURES_FILE.open("w", encoding="utf-8") as output_file:
        for line in input_file:
            line = line.strip()
            if not line:
                continue

            row = json.loads(line)
            feature_row = build_feature_row(row)

            output_file.write(json.dumps(feature_row, ensure_ascii=False) + "\n")
            written += 1

    print(f"Wrote {written} feature rows to {FEATURES_FILE}")


if __name__ == "__main__":
    main()
