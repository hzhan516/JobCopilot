"""Fail closed unless all AI Chat Phase 5 release evidence meets its gate."""

from __future__ import annotations

import argparse
import json
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path


def load_report(path: Path, name: str) -> dict:
    if not path.is_file():
        raise ValueError(f"{name} report is missing: {path}")
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise ValueError(f"{name} report is unreadable") from exc
    if not isinstance(value, dict):
        raise ValueError(f"{name} report must contain a JSON object")
    return value


def parse_utc(value: object, field: str) -> datetime:
    if not isinstance(value, str):
        raise ValueError(f"{field} is missing")
    try:
        parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError as exc:
        raise ValueError(f"{field} is invalid") from exc
    if parsed.tzinfo is None:
        raise ValueError(f"{field} must include a timezone")
    return parsed.astimezone(timezone.utc)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--golden-report", type=Path, required=True)
    parser.add_argument("--provider-smoke-report", type=Path, required=True)
    parser.add_argument("--fault-report", type=Path, required=True)
    parser.add_argument("--minimum-pass-rate", type=float, default=0.90)
    parser.add_argument("--maximum-p95-ms", type=float, default=90_000)
    parser.add_argument("--maximum-evidence-age-hours", type=float, default=24)
    args = parser.parse_args()

    failures: list[str] = []
    try:
        golden = load_report(args.golden_report, "golden evaluation")
        smoke = load_report(args.provider_smoke_report, "real-provider smoke")
        fault = load_report(args.fault_report, "fault injection")

        if int(golden.get("caseCount", 0)) < 30:
            failures.append("golden dataset has fewer than 30 cases")
        if float(golden.get("passRate", 0)) < args.minimum_pass_rate:
            failures.append("golden pass rate is below the threshold")
        if int(golden.get("schemaFailures", 1)) != 0:
            failures.append("golden evaluation has schema failures")
        if int(golden.get("safetyFailures", 1)) != 0:
            failures.append("golden evaluation has safety failures")
        if float(golden.get("p95LatencyMs", args.maximum_p95_ms + 1)) > args.maximum_p95_ms:
            failures.append("golden p95 latency exceeds the threshold")

        if smoke.get("passed") is not True:
            failures.append("real-provider smoke did not pass")
        if not smoke.get("requestId") or not smoke.get("compactionRequestId"):
            failures.append("real-provider smoke lacks exact correlation IDs")

        required_faults = {"mqInterruption", "providerTimeout", "duplicateResult", "lateResult"}
        fault_results = fault.get("scenarios", {})
        passed_faults = {
            name for name, result in fault_results.items()
            if isinstance(result, dict) and result.get("passed") is True
        }
        missing_faults = sorted(required_faults - passed_faults)
        if missing_faults:
            failures.append("fault scenarios did not pass: " + ", ".join(missing_faults))

        cutoff = datetime.now(timezone.utc) - timedelta(hours=args.maximum_evidence_age_hours)
        for report, field in ((smoke, "completedAt"), (fault, "completedAt")):
            if parse_utc(report.get(field), field) < cutoff:
                failures.append(f"{field} evidence is older than allowed")
    except (TypeError, ValueError) as exc:
        failures.append(str(exc))

    if failures:
        print(json.dumps({"decision": "NO-GO", "failures": failures}, ensure_ascii=False))
        return 1
    print(json.dumps({"decision": "GO", "failures": []}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
