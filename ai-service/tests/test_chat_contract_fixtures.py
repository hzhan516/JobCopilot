import json
from pathlib import Path

from app.schemas import AiResultEvent, ConversationRequestCommand


CONTRACT_ROOT = Path(__file__).resolve().parents[2] / "contracts" / "ai-chat" / "v1"


def _load(name: str) -> dict:
    return json.loads((CONTRACT_ROOT / name).read_text(encoding="utf-8"))


def test_java_request_fixture_validates_with_pydantic():
    command = ConversationRequestCommand.model_validate(_load("conversation-request.json"))
    assert command.schema_version == 1
    assert command.request_id == "00000000-0000-0000-0000-000000000401"
    assert command.current_message not in [message.content for message in command.message_history]


def test_result_fixtures_validate_and_keep_exact_request_id():
    success = AiResultEvent.model_validate(_load("conversation-result-success.json"))
    failure = AiResultEvent.model_validate(_load("conversation-result-failure.json"))
    assert success.status == "COMPLETED"
    assert failure.status == "FAILED"
    assert success.request_id == failure.request_id
    assert success.schema_version == failure.schema_version == 1
