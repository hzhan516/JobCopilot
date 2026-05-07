import csv
import json
import sys
from pathlib import Path


OUTPUT_DIR = Path(__file__).resolve().parents[1] / "output"
OUTPUT_FILE = OUTPUT_DIR / "normalized_jobs_sample.jsonl"

# Limit output to keep vector sync startup time and embedding costs reasonable.
# 限制输出条数：控制启动时向量同步耗时与 embedding 调用成本。
MAX_ROWS = 200


def load_skill_mapping(skill_mappings_csv: Path) -> dict[str, str]:
    mapping: dict[str, str] = {}

    with skill_mappings_csv.open("r", encoding="utf-8", newline="") as file:
        reader = csv.DictReader(file)
        for row in reader:
            skill_abr = (row.get("skill_abr") or "").strip()
            skill_name = (row.get("skill_name") or "").strip()

            if skill_abr and skill_name:
                mapping[skill_abr] = skill_name

    return mapping


def load_job_skills(job_skills_csv: Path, skill_mapping: dict[str, str]) -> dict[str, list[str]]:
    job_skills: dict[str, list[str]] = {}

    with job_skills_csv.open("r", encoding="utf-8", newline="") as file:
        reader = csv.DictReader(file)
        for row in reader:
            job_id = (row.get("job_id") or "").strip()
            skill_abr = (row.get("skill_abr") or "").strip()

            if not job_id or not skill_abr:
                continue

            skill_name = skill_mapping.get(skill_abr)
            if not skill_name:
                continue

            job_skills.setdefault(job_id, [])
            if skill_name not in job_skills[job_id]:
                job_skills[job_id].append(skill_name)

    return job_skills


def normalize_posting(row: dict[str, str], job_skills: dict[str, list[str]]) -> dict[str, object]:
    """Normalize a raw CSV posting row into a clean JSON record with deduplicated skills.
    归一化职位记录：将原始 CSV 行清洗为结构化 JSON，并关联去重后的技能列表，
    保证下游 embedding 与检索基于干净、一致的数据。"""
    job_id = (row.get("job_id") or "").strip()

    return {
        "job_id": job_id,
        "title": (row.get("title") or "").strip(),
        "description": (row.get("description") or "").strip(),
        "requirements": job_skills.get(job_id, []),
        "skills_desc": (row.get("skills_desc") or "").strip(),
        "location": (row.get("location") or "").strip(),
        "experience_level": (row.get("formatted_experience_level") or "").strip(),
    }


def main() -> None:
    if len(sys.argv) < 2:
        raise SystemExit(
            'Usage: python prepare_jobs_dataset.py "/path/to/archive"'
        )

    archive_dir = Path(sys.argv[1]).expanduser().resolve()

    postings_csv = archive_dir / "postings.csv"
    job_skills_csv = archive_dir / "jobs" / "job_skills.csv"
    skill_mappings_csv = archive_dir / "mappings" / "skills.csv"

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    skill_mapping = load_skill_mapping(skill_mappings_csv)
    job_skills = load_job_skills(job_skills_csv, skill_mapping)

    written = 0

    with postings_csv.open("r", encoding="utf-8", newline="") as input_file, \
         OUTPUT_FILE.open("w", encoding="utf-8") as output_file:
        reader = csv.DictReader(input_file)

        for row in reader:
            normalized = normalize_posting(row, job_skills)

            if not normalized["title"] or not normalized["description"]:
                continue

            output_file.write(json.dumps(normalized, ensure_ascii=False) + "\n")
            written += 1

            if written >= MAX_ROWS:
                break

    print(f"Wrote {written} normalized jobs to {OUTPUT_FILE}")


if __name__ == "__main__":
    main()
