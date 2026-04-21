import json
from pathlib import Path

import httpx


INPUT_DIR = Path(__file__).resolve().parents[1] / "input"
OUTPUT_DIR = Path(__file__).resolve().parents[1] / "output"

RESUMES_FILE = INPUT_DIR / "sample_resumes.jsonl"
JOBS_FILE = OUTPUT_DIR / "normalized_jobs_sample.jsonl"
OUTPUT_FILE = OUTPUT_DIR / "training_pairs_sample.jsonl"

SUITABILITY_API_URL = "http://127.0.0.1:8000/api/v1/suitability"

MAX_JOBS_PER_RESUME = 10


def load_jsonl(path: Path) -> list[dict]:
    records: list[dict] = []

    with path.open("r", encoding="utf-8") as file:
        for line in file:
            line = line.strip()
            if not line:
                continue
            records.append(json.loads(line))

    return records


def call_suitability_api(resume: dict, job: dict) -> dict:
    payload = {
        "resume": {
            "name": resume.get("name"),
            "email": resume.get("email"),
            "skills": resume.get("skills", []),
            "experience": resume.get("experience", []),
        },
        "job": {
            "title": job.get("title", ""),
            "company": "",
            "description": job.get("description", ""),
            "requirements": job.get("requirements", []),
        },
    }

    response = httpx.post(SUITABILITY_API_URL, json=payload, timeout=60.0)
    response.raise_for_status()
    return response.json()


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    resumes = load_jsonl(RESUMES_FILE)
    jobs = load_jsonl(JOBS_FILE)

    written = 0

    with OUTPUT_FILE.open("w", encoding="utf-8") as output_file:
        for resume in resumes:
            for job in jobs[:MAX_JOBS_PER_RESUME]:
                suitability = call_suitability_api(resume, job)

                record = {
                    "resume_id": resume.get("resume_id"),
                    "job_id": job.get("job_id"),
                    "label_source": "vertex_suitability_api",
                    "suitable": suitability.get("suitable"),
                    "score": suitability.get("finalScore"),
                    "vertex_score": suitability.get("vertexScore"),
                    "dataset_score": suitability.get("datasetScore"),
                    "summary": suitability.get("summary"),
                    "resume_skills": resume.get("skills", []),
                    "resume_experience": resume.get("experience", []),
                    "job_title": job.get("title", ""),
                    "job_description": job.get("description", ""),
                    "job_requirements": job.get("requirements", []),
                    "job_skills_desc": job.get("skills_desc", ""),
                }

                output_file.write(json.dumps(record, ensure_ascii=False) + "\n")
                written += 1

    print(f"Wrote {written} training pairs to {OUTPUT_FILE}")


if __name__ == "__main__":
    main()
