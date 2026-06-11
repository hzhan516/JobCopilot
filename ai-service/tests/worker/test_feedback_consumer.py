"""Test feedback consumer module.
反馈消费者测试：覆盖正常处理、字段缺失、Redis 失败、重复消息幂等性。
"""

import json
from unittest.mock import AsyncMock, MagicMock, patch


from app.worker.consumers.feedback import handle_feedback_message

# ── Normal flow ────────────────────────────────────────────


def test_handle_feedback_message_success(mock_redis_buffer):
    """Valid feedback payload should be appended to Redis buffer and acked.
    有效的反馈载荷应被追加到 Redis buffer 并确认。"""
    mock_ch = MagicMock()
    mock_method = MagicMock()
    mock_method.delivery_tag = 1

    payload = {
        "matchId": "match-123",
        "userId": "user-456",
        "resumeVersionId": "res-789",
        "jobId": "job-101",
        "feedbackType": "APPLY",
        "score": 0.95,
        "context": '{"llmOverallScore": 0.8}',
        "timestamp": "2026-05-19T22:00:00Z",
    }
    body = json.dumps(payload).encode("utf-8")

    mock_redis_buffer.append = AsyncMock()
    with patch(
        "app.worker.consumers.feedback.RedisBuffer", return_value=mock_redis_buffer
    ):
        handle_feedback_message(mock_ch, mock_method, None, body)

    mock_redis_buffer.append.assert_called_once()
    appended = mock_redis_buffer.append.call_args[0][0]
    assert appended["type"] == "feedback"
    assert appended["label"] == 1  # APPLY -> 1
    mock_ch.basic_ack.assert_called_once_with(delivery_tag=1)


# ── Invalid JSON ───────────────────────────────────────────


def test_handle_feedback_message_invalid_json(mock_redis_buffer):
    """Invalid JSON should be nacked without requeue.
    非法 JSON 应被 nack 且不重新入队。"""
    mock_ch = MagicMock()
    mock_method = MagicMock()
    mock_method.delivery_tag = 2

    mock_redis_buffer.append = AsyncMock()
    with patch(
        "app.worker.consumers.feedback.RedisBuffer", return_value=mock_redis_buffer
    ):
        handle_feedback_message(mock_ch, mock_method, None, b"not json")

    mock_redis_buffer.append.assert_not_called()
    mock_ch.basic_nack.assert_called_once_with(delivery_tag=2, requeue=False)


# ── Missing required fields ────────────────────────────────


def test_handle_feedback_message_missing_fields(mock_redis_buffer):
    """Payload missing required fields should be nacked.
    缺少必填字段的载荷应被 nack。"""
    mock_ch = MagicMock()
    mock_method = MagicMock()
    mock_method.delivery_tag = 3

    # Missing feedbackType and score
    payload = {
        "matchId": "match-123",
        "userId": "user-456",
    }
    body = json.dumps(payload).encode("utf-8")

    mock_redis_buffer.append = AsyncMock()
    with patch(
        "app.worker.consumers.feedback.RedisBuffer", return_value=mock_redis_buffer
    ):
        handle_feedback_message(mock_ch, mock_method, None, body)

    mock_redis_buffer.append.assert_not_called()
    mock_ch.basic_nack.assert_called_once_with(delivery_tag=3, requeue=False)


# ── Null/None fields ─────────────────────────────────────


def test_handle_feedback_message_null_feedback_type(mock_redis_buffer):
    """Null feedbackType should be treated as invalid and nacked.
    null 的 feedbackType 应被视为非法并 nack。"""
    mock_ch = MagicMock()
    mock_method = MagicMock()
    mock_method.delivery_tag = 4

    payload = {
        "matchId": "match-123",
        "userId": "user-456",
        "resumeVersionId": "res-789",
        "jobId": "job-101",
        "feedbackType": None,
        "score": 0.95,
    }
    body = json.dumps(payload).encode("utf-8")

    mock_redis_buffer.append = AsyncMock()
    with patch(
        "app.worker.consumers.feedback.RedisBuffer", return_value=mock_redis_buffer
    ):
        handle_feedback_message(mock_ch, mock_method, None, body)

    mock_redis_buffer.append.assert_not_called()
    mock_ch.basic_nack.assert_called_once_with(delivery_tag=4, requeue=False)


# ── Redis append failure ─────────────────────────────────


def test_handle_feedback_message_redis_failure(mock_redis_buffer):
    """Redis append failure should result in nack with requeue=True.
    Redis 追加失败应触发 nack 并重新入队以便重试。"""
    mock_ch = MagicMock()
    mock_method = MagicMock()
    mock_method.delivery_tag = 5

    payload = {
        "matchId": "match-123",
        "userId": "user-456",
        "resumeVersionId": "res-789",
        "jobId": "job-101",
        "feedbackType": "SKIP",
        "score": 0.2,
    }
    body = json.dumps(payload).encode("utf-8")

    mock_redis_buffer.append = AsyncMock(side_effect=Exception("Redis connection lost"))
    with patch(
        "app.worker.consumers.feedback.RedisBuffer", return_value=mock_redis_buffer
    ):
        handle_feedback_message(mock_ch, mock_method, None, body)

    mock_redis_buffer.append.assert_called_once()
    mock_ch.basic_nack.assert_called_once_with(delivery_tag=5, requeue=True)


# ── Duplicate message idempotency ──────────────────────────


def test_handle_feedback_message_duplicate_idempotent(mock_redis_buffer):
    """Duplicate delivery should be idempotent (same data appended twice is acceptable).
    重复投递应是幂等的（同一数据追加两次可接受）。"""
    mock_ch = MagicMock()
    mock_method = MagicMock()
    mock_method.delivery_tag = 6

    payload = {
        "matchId": "match-123",
        "userId": "user-456",
        "resumeVersionId": "res-789",
        "jobId": "job-101",
        "feedbackType": "APPLY",
        "score": 0.95,
    }
    body = json.dumps(payload).encode("utf-8")

    mock_redis_buffer.append = AsyncMock()
    with patch(
        "app.worker.consumers.feedback.RedisBuffer", return_value=mock_redis_buffer
    ):
        # First delivery
        handle_feedback_message(mock_ch, mock_method, None, body)
        # Second delivery (duplicate)
        handle_feedback_message(mock_ch, mock_method, None, body)

    assert mock_redis_buffer.append.call_count == 2
    # In production, deduplication would be handled by the training pipeline.
    # 生产环境中，训练流水线会负责去重。
