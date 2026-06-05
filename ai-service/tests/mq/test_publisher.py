import json
import pytest
from unittest.mock import MagicMock

from app.mq.publisher import (
    get_result_routing_key,
    publish_ai_result,
    publish_json_payload,
    publish_job_rank_result,
)
from app.schemas import AiResultEvent
from app.config import (
    AI_DIRECT_EXCHANGE,
    JOB_RANK_RESULT_ROUTING_KEY,
    JOB_PARSE_RESULT_ROUTING_KEY,
    RESUME_PARSE_RESULT_ROUTING_KEY,
    CONVERSATION_RESULT_ROUTING_KEY,
)

def test_get_result_routing_key():
    assert get_result_routing_key("JOB_PARSE") == JOB_PARSE_RESULT_ROUTING_KEY
    assert get_result_routing_key("RESUME_PARSE") == RESUME_PARSE_RESULT_ROUTING_KEY
    assert get_result_routing_key("CONVERSATION_REPLY") == CONVERSATION_RESULT_ROUTING_KEY
    assert get_result_routing_key("JOB_RANK") == JOB_RANK_RESULT_ROUTING_KEY
    
    with pytest.raises(ValueError, match="Unsupported event type: UNKNOWN"):
        get_result_routing_key("UNKNOWN")

def test_publish_ai_result():
    mock_channel = MagicMock()
    event = AiResultEvent(
        referenceId="ref-123",
        type="JOB_PARSE",
        status="COMPLETED",
        data={"key": "value"},
        errorMessage=None,
        eventType="JOB"
    )
    
    publish_ai_result(mock_channel, event)
    
    mock_channel.basic_publish.assert_called_once()
    kwargs = mock_channel.basic_publish.call_args.kwargs
    assert kwargs["exchange"] == AI_DIRECT_EXCHANGE
    assert kwargs["routing_key"] == JOB_PARSE_RESULT_ROUTING_KEY
    
    body = json.loads(kwargs["body"].decode("utf-8"))
    assert body["referenceId"] == "ref-123"
    assert body["type"] == "JOB_PARSE"
    assert body["status"] == "COMPLETED"
    assert body["data"] == {"key": "value"}
    
    props = kwargs["properties"]
    assert props.content_type == "application/json"
    assert props.delivery_mode == 2

def test_publish_json_payload():
    mock_channel = MagicMock()
    payload = {"test": "data"}
    routing_key = "test.routing.key"
    
    publish_json_payload(mock_channel, routing_key, payload)
    
    mock_channel.basic_publish.assert_called_once()
    kwargs = mock_channel.basic_publish.call_args.kwargs
    assert kwargs["exchange"] == AI_DIRECT_EXCHANGE
    assert kwargs["routing_key"] == routing_key
    
    body = json.loads(kwargs["body"].decode("utf-8"))
    assert body == payload

def test_publish_job_rank_result():
    mock_channel = MagicMock()

    publish_job_rank_result(
        mock_channel,
        match_id="match-123",
        status="COMPLETED",
        rank_time_ms=150,
        ranked_results=[
            {
                "jobId": "job-1",
                "title": "Dev",
                "company": "Acme",
                "matchScore": 0.9,
                "matchFactors": {
                    "skillMatch": 0.8,
                    "experienceMatch": 0.7,
                    "locationMatch": 0.6,
                },
                "description": "Build things.",
            }
        ],
    )

    mock_channel.basic_publish.assert_called_once()
    kwargs = mock_channel.basic_publish.call_args.kwargs
    assert kwargs["exchange"] == AI_DIRECT_EXCHANGE
    assert kwargs["routing_key"] == JOB_RANK_RESULT_ROUTING_KEY

    body = json.loads(kwargs["body"].decode("utf-8"))
    assert body["referenceId"] == "match-123"
    assert body["type"] == "JOB_RANK"
    assert body["status"] == "COMPLETED"
    assert body["data"]["rankTimeMs"] == 150
    assert body["data"]["rankedResults"][0]["jobId"] == "job-1"
    assert body["data"]["rankedResults"][0]["matchFactors"]["skillMatch"] == 0.8
    assert body["errorMessage"] is None
