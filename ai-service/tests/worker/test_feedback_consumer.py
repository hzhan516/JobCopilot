import json
from unittest.mock import patch, MagicMock, AsyncMock
from app.worker.consumers.feedback import handle_feedback_message

def test_handle_feedback_message(mock_redis_buffer):
    # Setup mock Pika channel
    mock_ch = MagicMock()
    mock_method = MagicMock()
    mock_method.delivery_tag = 1
    
    # Create sample feedback event matching Java outbox structure
    payload = {
        "matchId": "match-123",
        "userId": "user-456",
        "resumeVersionId": "res-789",
        "jobId": "job-101",
        "feedbackType": "APPLY",
        "score": 0.95,
        "context": '{"llmOverallScore": 0.8}',
        "timestamp": "2026-05-19T22:00:00Z"
    }
    body = json.dumps(payload).encode("utf-8")
    
    # Act
    mock_redis_buffer.append = AsyncMock()
    with patch("app.worker.consumers.feedback.RedisBuffer", return_value=mock_redis_buffer):
        handle_feedback_message(mock_ch, mock_method, None, body)
    
    # Assert
    mock_redis_buffer.append.assert_called_once()
    appended_data = mock_redis_buffer.append.call_args[0][0]
    
    assert appended_data["type"] == "feedback"
    assert appended_data["user_id"] == "user-456"
    assert appended_data["job_id"] == "job-101"
    assert appended_data["label"] == 1  # APPLY -> 1
    assert appended_data["features"]["llmOverallScore"] == 0.8
    
    mock_ch.basic_ack.assert_called_once_with(delivery_tag=1)

def test_handle_feedback_message_invalid_json(mock_redis_buffer):
    mock_ch = MagicMock()
    mock_method = MagicMock()
    mock_method.delivery_tag = 2
    
    body = b"not json"
    
    mock_redis_buffer.append = AsyncMock()
    with patch("app.worker.consumers.feedback.RedisBuffer", return_value=mock_redis_buffer):
        handle_feedback_message(mock_ch, mock_method, None, body)
    
    mock_redis_buffer.append.assert_not_called()
    mock_ch.basic_nack.assert_called_once_with(delivery_tag=2, requeue=False)
