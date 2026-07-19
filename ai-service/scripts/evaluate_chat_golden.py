"""Run the synthetic AI Chat golden set without persisting prompts or model answers."""

from __future__ import annotations

import argparse
import json
import os
import statistics
import sys
import time
import uuid
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(PROJECT_ROOT))

from app.schemas import ConversationRequestCommand  # noqa: E402


def load_cases(path: Path) -> list[dict]:
    cases = [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]
    ids = [case["id"] for case in cases]
    if len(cases) < 30 or len(ids) != len(set(ids)):
        raise ValueError("Golden dataset must contain at least 30 uniquely identified cases")
    return cases


def percentile(values: list[float], percentile_value: float) -> float:
    if not values:
        return 0.0
    ordered = sorted(values)
    index = min(len(ordered) - 1, max(0, round((len(ordered) - 1) * percentile_value)))
    return ordered[index]


def evaluate_case(case: dict) -> dict:
    # Keep --validate-only dependency-light; provider and parser dependencies are
    # required only for a live evaluation run.
    from app.services.conversation_service import process_conversation

    request_id = str(uuid.uuid4())
    command = ConversationRequestCommand(
        conversationId=str(uuid.uuid4()),
        userId=str(uuid.uuid4()),
        currentMessage=case["currentMessage"],
        messageHistory=case.get("messageHistory", []),
        fileUrls=case.get("fileUrls", []),
        resumeText=case.get("resumeText"),
        primaryJobText=case.get("primaryJobText"),
        relatedJobTexts=case.get("relatedJobTexts", []),
        locale=case.get("locale", "en"),
        requestId=request_id,
        schemaVersion=1,
        eventId=str(uuid.uuid4()),
    )
    started = time.perf_counter()
    result = process_conversation(command)
    latency_ms = round((time.perf_counter() - started) * 1000, 2)
    content = result.data.content
    lowered = content.lower()
    required = [str(item).lower() for item in case.get("requiredConcepts", [])]
    forbidden = [str(item).lower() for item in case.get("forbiddenClaims", [])]
    locale = case.get("locale", "en")
    language_ok = locale == "en" or any("\u4e00" <= char <= "\u9fff" for char in content)
    grounding_ok = all(concept in lowered for concept in required)
    safety_ok = not any(claim in lowered for claim in forbidden)
    schema_ok = result.status == "COMPLETED" and bool(content.strip()) and result.request_id == request_id
    passed = schema_ok and language_ok and grounding_ok and safety_ok
    return {
        "id": case["id"],
        "category": case["category"],
        "passed": passed,
        "schemaOk": schema_ok,
        "languageOk": language_ok,
        "groundingOk": grounding_ok,
        "safetyOk": safety_ok,
        "latencyMs": latency_ms,
        "promptTokens": result.data.prompt_tokens,
        "completionTokens": result.data.completion_tokens,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--dataset",
        type=Path,
        default=PROJECT_ROOT / "evals" / "chat_golden_v1.jsonl",
    )
    parser.add_argument("--output", type=Path, default=PROJECT_ROOT / "evals" / "reports" / "chat-golden-latest.json")
    parser.add_argument("--validate-only", action="store_true")
    args = parser.parse_args()

    cases = load_cases(args.dataset)
    if args.validate_only:
        print(json.dumps({"valid": True, "caseCount": len(cases)}))
        return 0

    results = [evaluate_case(case) for case in cases]
    latencies = [result["latencyMs"] for result in results]
    prompt_tokens = sum(result["promptTokens"] for result in results)
    completion_tokens = sum(result["completionTokens"] for result in results)
    input_rate = float(os.getenv("AI_COST_INPUT_PER_MILLION", "0"))
    output_rate = float(os.getenv("AI_COST_OUTPUT_PER_MILLION", "0"))
    estimated_cost = (prompt_tokens * input_rate + completion_tokens * output_rate) / 1_000_000
    report = {
        "dataset": args.dataset.name,
        "caseCount": len(results),
        "passed": sum(result["passed"] for result in results),
        "passRate": round(sum(result["passed"] for result in results) / len(results), 4),
        "schemaFailures": sum(not result["schemaOk"] for result in results),
        "safetyFailures": sum(not result["safetyOk"] for result in results),
        "groundingFailures": sum(not result["groundingOk"] for result in results),
        "p50LatencyMs": round(statistics.median(latencies), 2),
        "p95LatencyMs": round(percentile(latencies, 0.95), 2),
        "promptTokens": prompt_tokens,
        "completionTokens": completion_tokens,
        "estimatedCostUsd": round(estimated_cost, 6),
        "results": results,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(json.dumps({key: value for key, value in report.items() if key != "results"}))
    return 0 if report["schemaFailures"] == 0 and report["safetyFailures"] == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
