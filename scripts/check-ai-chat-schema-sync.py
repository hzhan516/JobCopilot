"""Fail when AI Chat migrations, dev bootstrap, and Compose upgrade drift apart."""

from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
INIT_SQL = ROOT / "backend/app/src/main/resources/db/init.sql"
MIGRATION_DIR = ROOT / "backend/app/src/main/resources/db/migration"
COMPOSE_FILE = ROOT / "docker-compose.yml"
HEALTH_INDICATOR = (
    ROOT
    / "backend/app/src/main/java/io/jobcopilot/resumeassistant/configuration/"
    / "AiChatSchemaHealthIndicator.java"
)

REQUIRED_V25_COLUMNS = {
    "context_tokens",
    "total_tokens_used",
    "context_summary",
    "compacted_through_sequence",
}
REQUIRED_V26_CONVERSATION_COLUMNS = {
    "compaction_request_id",
    "ai_reply_request_id",
    "ai_reply_status",
    "ai_reply_error_code",
    "ai_reply_started_at",
    "ai_reply_completed_at",
    "ai_reply_user_message_sequence",
    "ai_reply_assistant_message_sequence",
}
REQUIRED_V26_OUTBOX_COLUMNS = {
    "attempt_count",
    "next_attempt_at",
    "last_error_code",
    "locked_at",
    "worker_id",
}


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def require_all(text: str, values: set[str], label: str, failures: list[str]) -> None:
    missing = sorted(value for value in values if value not in text)
    if missing:
        failures.append(f"{label} is missing: {', '.join(missing)}")


def main() -> int:
    failures: list[str] = []
    try:
        init_sql = read(INIT_SQL)
        v12_sql = read(MIGRATION_DIR / "V12__dynamic_config.sql")
        v25_sql = read(MIGRATION_DIR / "V25__chat_context_compaction.sql")
        v26_sql = read(MIGRATION_DIR / "V26__ai_chat_request_state_and_outbox_retry.sql")
        compose = read(COMPOSE_FILE)
        health = read(HEALTH_INDICATOR)

        if "CREATE TABLE IF NOT EXISTS dynamic_config" not in v12_sql:
            failures.append("V12 dynamic_config migration is incomplete")
        if "CREATE TABLE IF NOT EXISTS dynamic_config" not in init_sql:
            failures.append("dev init.sql does not include dynamic_config")

        require_all(v25_sql, REQUIRED_V25_COLUMNS, "V25 migration", failures)
        require_all(init_sql, REQUIRED_V25_COLUMNS, "dev init.sql V25 block", failures)
        require_all(v26_sql, REQUIRED_V26_CONVERSATION_COLUMNS, "V26 conversation migration", failures)
        require_all(v26_sql, REQUIRED_V26_OUTBOX_COLUMNS, "V26 outbox migration", failures)
        require_all(init_sql, REQUIRED_V26_CONVERSATION_COLUMNS, "dev init.sql V26 block", failures)
        require_all(init_sql, REQUIRED_V26_OUTBOX_COLUMNS, "dev init.sql outbox block", failures)

        if init_sql.index("V25: Chat context compaction") > init_sql.index("V26: AI chat request state"):
            failures.append("dev init.sql applies V26 before V25")

        compose_markers = {
            "db-migrate:",
            "/migrations/V12__dynamic_config.sql",
            "/migrations/V25__chat_context_compaction.sql",
            "/migrations/V26__ai_chat_request_state_and_outbox_retry.sql",
            "condition: service_completed_successfully",
        }
        require_all(compose, compose_markers, "Compose dev upgrade gate", failures)

        required_health_objects = (
            REQUIRED_V25_COLUMNS
            | REQUIRED_V26_CONVERSATION_COLUMNS
            | REQUIRED_V26_OUTBOX_COLUMNS
            | {
                "dynamic_config",
                "idx_conversations_ai_reply_pending",
                "idx_outbox_due",
                "idx_outbox_stale_processing",
            }
        )
        require_all(health, required_health_objects, "AI Chat schema health indicator", failures)
    except (OSError, ValueError) as exception:
        failures.append(str(exception))

    decision = "GO" if not failures else "NO-GO"
    print(json.dumps({"decision": decision, "failures": failures}, ensure_ascii=False))
    return 0 if not failures else 1


if __name__ == "__main__":
    raise SystemExit(main())
