#!/usr/bin/env python3
"""Run live AI evaluation metrics for the JobCopilot project.

The script evaluates real AI-service pipeline functions:
- resume parsing extraction quality
- job posting parsing quality
- single job suitability scoring quality
- optional legacy job ranking quality against a keyword baseline

It writes machine-readable results to eval/results/metrics.json.
"""

from __future__ import annotations

import argparse
import json
import math
import os
import sys
from pathlib import Path
from typing import Any

from dotenv import load_dotenv


EVAL_DIR = Path(__file__).resolve().parent
REPO_ROOT = EVAL_DIR.parent
AI_SERVICE_DIR = REPO_ROOT / "ai-service"
DATA_DIR = EVAL_DIR / "data"
RESULTS_DIR = EVAL_DIR / "results"

sys.path.insert(0, str(AI_SERVICE_DIR))


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def normalize_token(value: str) -> str:
    return "".join(char.lower() for char in value if char.isalnum())


def normalize_items(values: list[str]) -> set[str]:
    normalized = {normalize_token(value) for value in values if normalize_token(value)}
    aliases = {
        "restapi": "restapis",
        "tailwind": "tailwindcss",
        "postgres": "postgresql",
    }
    return {aliases.get(value, value) for value in normalized}


def f1_score(expected: list[str], predicted: list[str]) -> dict[str, float]:
    expected_set = normalize_items(expected)
    predicted_set = normalize_items(predicted)

    if not expected_set and not predicted_set:
        return {"precision": 1.0, "recall": 1.0, "f1": 1.0}
    if not expected_set or not predicted_set:
        return {"precision": 0.0, "recall": 0.0, "f1": 0.0}

    true_positive = len(expected_set & predicted_set)
    precision = true_positive / len(predicted_set)
    recall = true_positive / len(expected_set)
    f1 = 0.0 if precision + recall == 0 else 2 * precision * recall / (precision + recall)

    return {
        "precision": round(precision, 4),
        "recall": round(recall, 4),
        "f1": round(f1, 4),
    }


def dcg(scores: list[float]) -> float:
    return sum(score / math.log2(index + 2) for index, score in enumerate(scores))


def ndcg_at_k(ranked_job_ids: list[str], relevance_by_job_id: dict[str, int], k: int = 5) -> float:
    predicted_scores = [relevance_by_job_id.get(job_id, 0) for job_id in ranked_job_ids[:k]]
    ideal_scores = sorted(relevance_by_job_id.values(), reverse=True)[:k]

    ideal_dcg = dcg(ideal_scores)
    if ideal_dcg == 0:
        return 0.0

    return round(dcg(predicted_scores) / ideal_dcg, 4)


def mean(values: list[float]) -> float:
    if not values:
        return 0.0
    return round(sum(values) / len(values), 4)


def evaluate_resume_parsing() -> dict[str, Any]:
    from app.services.resume_parser import parse_resume_text

    cases = load_json(DATA_DIR / "resume_parsing_cases.json")
    case_results = []

    for case in cases:
        parsed = parse_resume_text(case["resumeText"])
        expected = case["expected"]
        skill_scores = f1_score(expected["skills"], parsed.skills)
        name_exact = int((parsed.name or "").strip().lower() == expected["name"].lower())
        email_exact = int((parsed.email or "").strip().lower() == expected["email"].lower())

        case_results.append(
            {
                "id": case["id"],
                "nameExact": name_exact,
                "emailExact": email_exact,
                "skillScores": skill_scores,
                "expectedSkills": expected["skills"],
                "predictedSkills": parsed.skills,
            }
        )

    return {
        "metric": "resume_parsing_extraction",
        "caseCount": len(case_results),
        "averageSkillF1": mean([item["skillScores"]["f1"] for item in case_results]),
        "nameAccuracy": mean([item["nameExact"] for item in case_results]),
        "emailAccuracy": mean([item["emailExact"] for item in case_results]),
        "cases": case_results,
    }


def evaluate_job_parsing() -> dict[str, Any]:
    from app.services.job_parser import parse_job_text

    cases = load_json(DATA_DIR / "job_parsing_cases.json")
    case_results = []

    for case in cases:
        parsed = parse_job_text(case["jobText"])
        expected = case["expected"]
        requirement_scores = f1_score(expected["requirements"], parsed.requirements)
        title_exact = int(parsed.title.strip().lower() == expected["title"].lower())
        company_exact = int(parsed.company.strip().lower() == expected["company"].lower())

        case_results.append(
            {
                "id": case["id"],
                "titleExact": title_exact,
                "companyExact": company_exact,
                "requirementScores": requirement_scores,
                "expectedRequirements": expected["requirements"],
                "predictedRequirements": parsed.requirements,
            }
        )

    return {
        "metric": "job_posting_parsing_extraction",
        "caseCount": len(case_results),
        "averageRequirementF1": mean([item["requirementScores"]["f1"] for item in case_results]),
        "titleAccuracy": mean([item["titleExact"] for item in case_results]),
        "companyAccuracy": mean([item["companyExact"] for item in case_results]),
        "cases": case_results,
    }


def evaluate_suitability_scoring() -> dict[str, Any]:
    from app.schemas import ParsedJobContent, ParsedResumeContent, SuitabilityRequest
    from app.services.suitability_service import evaluate_suitability_with_vertex

    cases = load_json(DATA_DIR / "suitability_cases.json")
    case_results = []

    for case in cases:
        request = SuitabilityRequest(
            resume=ParsedResumeContent(**case["resume"]),
            job=ParsedJobContent(**case["job"]),
        )
        result = evaluate_suitability_with_vertex(request)
        expected = case["expected"]
        min_score = float(expected.get("minFinalScore", 0.0))
        max_score = float(expected.get("maxFinalScore", 1.0))
        final_score = float(result.final_score)

        case_results.append(
            {
                "id": case["id"],
                "expectedSuitable": expected["suitable"],
                "predictedSuitable": result.suitable,
                "decisionExact": int(result.suitable == expected["suitable"]),
                "finalScore": round(final_score, 4),
                "scoreInExpectedRange": int(min_score <= final_score <= max_score),
                "expectedScoreRange": [min_score, max_score],
                "skillScore": result.breakdown.skill_score,
                "experienceScore": result.breakdown.experience_score,
                "overallScore": result.breakdown.overall_score,
                "summary": result.summary,
            }
        )

    return {
        "metric": "single_job_suitability_scoring",
        "caseCount": len(case_results),
        "decisionAccuracy": mean([item["decisionExact"] for item in case_results]),
        "scoreRangeAccuracy": mean([item["scoreInExpectedRange"] for item in case_results]),
        "averageFinalScore": mean([item["finalScore"] for item in case_results]),
        "cases": case_results,
    }


def evaluate_job_ranking() -> dict[str, Any]:
    from app.schemas import JobRankCommand
    from app.services.job_rank_service import rank_jobs

    cases = load_json(DATA_DIR / "job_ranking_cases.json")
    case_results = []

    for case in cases:
        command = JobRankCommand(
            matchId=f"eval-{case['id']}",
            userId="eval-user",
            resumeVersionId="eval-resume",
            resumeText=case["resumeText"],
            query=case["query"],
            recalledJobIds=case["recalledJobIds"],
            jobDetails=case["jobDetails"],
        )
        result = rank_jobs(command)
        system_ranking = [item.job_id for item in result.ranked_results]
        baseline_ranking = case["baselineRanking"]
        relevance = case["idealRelevance"]

        case_results.append(
            {
                "id": case["id"],
                "systemRanking": system_ranking,
                "baselineRanking": baseline_ranking,
                "systemNdcgAt5": ndcg_at_k(system_ranking, relevance, k=5),
                "baselineNdcgAt5": ndcg_at_k(baseline_ranking, relevance, k=5),
                "matchReasonCoverageAt3": round(
                    sum(1 for item in result.ranked_results[:3] if item.match_reason) / 3,
                    4,
                ),
                "rankTimeMs": result.rank_time_ms,
            }
        )

    return {
        "metric": "job_recommendation_ranking",
        "caseCount": len(case_results),
        "averageSystemNdcgAt5": mean([item["systemNdcgAt5"] for item in case_results]),
        "averageBaselineNdcgAt5": mean([item["baselineNdcgAt5"] for item in case_results]),
        "averageMatchReasonCoverageAt3": mean([item["matchReasonCoverageAt3"] for item in case_results]),
        "cases": case_results,
    }


def check_live_environment() -> None:
    text_model = os.getenv("LLM_TEXT_MODEL", "gemini/gemini-2.5-flash").strip()

    provider_key_by_prefix = {
        "gemini/": "GEMINI_API_KEY",
        "openai/": "OPENAI_API_KEY",
        "anthropic/": "ANTHROPIC_API_KEY",
    }

    required_key = None
    for prefix, env_name in provider_key_by_prefix.items():
        if text_model.startswith(prefix):
            required_key = env_name
            break

    if required_key and os.getenv(required_key):
        return

    if required_key:
        raise RuntimeError(
            f"LLM_TEXT_MODEL is set to {text_model!r}, so {required_key} is required. "
            f"Copy .env.example to .env and fill {required_key}, or choose another LiteLLM provider."
        )

    raise RuntimeError(
        f"LLM_TEXT_MODEL is set to {text_model!r}, but eval/run_eval.py does not know which API key "
        "that provider needs. Set the provider credential in .env, then rerun with --skip-env-check "
        "if the provider is supported by LiteLLM."
    )



def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run JobCopilot AI evaluation.")
    parser.add_argument(
        "--output",
        default=str(RESULTS_DIR / "metrics.json"),
        help="Path to write metrics JSON.",
    )
    parser.add_argument(
        "--skip-env-check",
        action="store_true",
        help="Skip API key preflight check. Useful when credentials are supplied by the provider SDK.",
    )
    parser.add_argument(
        "--include-legacy-ranking",
        action="store_true",
        help="Also run the legacy job ranking NDCG@5 evaluation.",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    load_dotenv(REPO_ROOT / ".env")

    if not args.skip_env_check:
        check_live_environment()

    metrics = {
        "resumeParsing": evaluate_resume_parsing(),
        "jobParsing": evaluate_job_parsing(),
        "suitabilityScoring": evaluate_suitability_scoring(),
    }

    if args.include_legacy_ranking:
        metrics["legacyJobRanking"] = evaluate_job_ranking()

    results = {
        "mode": "live",
        "description": "Live evaluation using current ai-service parsing and single-job suitability scoring pipeline functions.",
        "metrics": metrics,
    }

    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(results, indent=2), encoding="utf-8")
    print(json.dumps(results, indent=2))
    print(f"\nWrote evaluation results to {output_path}")


if __name__ == "__main__":
    main()
