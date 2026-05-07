import json
import pytest
from unittest.mock import MagicMock, patch

from app.mq.consumer import (
    create_connection,
    setup_all_queues,
    QUEUE_ARGUMENTS,
    parse_job_command,
    parse_resume_command,
    parse_conversation_command,
    parse_job_rank_command,
    build_failed_event,
    handle_job_message,
    handle_resume_message,
    handle_conversation_message,
    handle_job_rank_message,
    _async_handler,
    start_all_consumers,
    JOB_PARSE_FAILED_MESSAGE,
    RESUME_PARSE_FAILED_MESSAGE,
    CONVERSATION_FAILED_MESSAGE,
    JOB_RANK_FAILED_MESSAGE,
)
from app.config import AI_DLQ_QUEUE
from app.schemas import (
    JobParseCommand,
    ResumeParseCommand,
    ConversationRequestCommand,
    JobRankCommand,
    AiResultEvent,
    JobRankResultPayload,
)

@patch("pika.BlockingConnection")
@patch("pika.ConnectionParameters")
@patch("pika.PlainCredentials")
def test_create_connection(mock_creds, mock_params, mock_conn):
    mock_creds.return_value = "creds"
    mock_params.return_value = "params"
    mock_conn.return_value = "connection"
    
    conn = create_connection()
    
    assert conn == "connection"
    mock_creds.assert_called_once()
    mock_params.assert_called_once()
    mock_conn.assert_called_once_with("params")

def test_setup_all_queues():
    mock_channel = MagicMock()
    setup_all_queues(mock_channel)
    
    assert mock_channel.exchange_declare.call_count == 2
    assert mock_channel.queue_declare.call_count == 9
    assert mock_channel.queue_bind.call_count == 9

    dlq_call = mock_channel.queue_declare.call_args_list[0]
    assert dlq_call.kwargs["queue"] == AI_DLQ_QUEUE
    assert dlq_call.kwargs["durable"] is True
    assert "arguments" not in dlq_call.kwargs

    for call in mock_channel.queue_declare.call_args_list[1:]:
        assert call.kwargs["durable"] is True
        assert call.kwargs["arguments"] == QUEUE_ARGUMENTS

def test_parse_commands():
    job_body = json.dumps({"jobId": "job-1", "url": "http://test.com/job.pdf", "imageCheckEnabled": False}).encode("utf-8")
    job_cmd = parse_job_command(job_body)
    assert isinstance(job_cmd, JobParseCommand)
    assert job_cmd.job_id == "job-1"
    assert job_cmd.screenshot_url is None
    
    resume_body = json.dumps({"resumeId": "res-1", "fileUrl": "http://test.com/res.pdf", "format": "pdf"}).encode("utf-8")
    res_cmd = parse_resume_command(resume_body)
    assert isinstance(res_cmd, ResumeParseCommand)
    assert res_cmd.resume_id == "res-1"
    
    conv_body = json.dumps({"conversationId": "conv-1", "userId": "user-1", "currentMessage": "hello", "messageHistory": []}).encode("utf-8")
    conv_cmd = parse_conversation_command(conv_body)
    assert isinstance(conv_cmd, ConversationRequestCommand)
    assert conv_cmd.conversation_id == "conv-1"
    
    rank_body = json.dumps({"matchId": "match-1", "userId": "user-1", "resumeVersionId": "res-1", "query": "test"}).encode("utf-8")
    rank_cmd = parse_job_rank_command(rank_body)
    assert isinstance(rank_cmd, JobRankCommand)
    assert rank_cmd.match_id == "match-1"

def test_build_failed_event():
    event = build_failed_event("ref-1", "JOB_PARSE", "error msg", "JOB")
    assert isinstance(event, AiResultEvent)
    assert event.reference_id == "ref-1"
    assert event.type == "JOB_PARSE"
    assert event.status == "FAILED"
    assert event.error_message == "error msg"
    assert event.event_type == "JOB"

@patch("app.mq.consumer.process_job")
@patch("app.mq.consumer.publish_ai_result")
def test_handle_job_message_success(mock_publish, mock_process):
    mock_channel = MagicMock()
    body = json.dumps({"jobId": "job-1", "url": "http://test.com/job.pdf", "imageCheckEnabled": False}).encode("utf-8")
    
    mock_result = AiResultEvent(referenceId="job-1", type="JOB_PARSE", status="COMPLETED", data={})
    mock_process.return_value = mock_result
    
    handle_job_message(mock_channel, body)
    
    mock_process.assert_called_once()
    mock_publish.assert_called_once_with(mock_channel, mock_result)

@patch("app.mq.consumer.process_job")
@patch("app.mq.consumer.publish_ai_result")
def test_handle_job_message_failure(mock_publish, mock_process):
    mock_channel = MagicMock()
    body = json.dumps({"jobId": "job-1", "url": "http://test.com/job.pdf", "imageCheckEnabled": False}).encode("utf-8")
    
    mock_process.side_effect = Exception("Processing failed")
    
    handle_job_message(mock_channel, body)
    
    mock_process.assert_called_once()
    mock_publish.assert_called_once()
    published_event = mock_publish.call_args[0][1]
    assert published_event.status == "FAILED"
    assert published_event.error_message == JOB_PARSE_FAILED_MESSAGE

@patch("app.mq.consumer.process_resume")
@patch("app.mq.consumer.publish_ai_result")
def test_handle_resume_message_success(mock_publish, mock_process):
    mock_channel = MagicMock()
    body = json.dumps({"resumeId": "res-1", "fileUrl": "http://test.com/res.pdf", "format": "pdf"}).encode("utf-8")
    
    mock_result = AiResultEvent(referenceId="res-1", type="RESUME_PARSE", status="COMPLETED", data={})
    mock_process.return_value = mock_result
    
    handle_resume_message(mock_channel, body)
    
    mock_process.assert_called_once()
    mock_publish.assert_called_once_with(mock_channel, mock_result)

@patch("app.mq.consumer.process_resume")
@patch("app.mq.consumer.publish_ai_result")
def test_handle_resume_message_failure(mock_publish, mock_process):
    mock_channel = MagicMock()
    body = json.dumps({"resumeId": "res-1", "fileUrl": "http://test.com/res.pdf", "format": "pdf"}).encode("utf-8")
    
    mock_process.side_effect = Exception("Processing failed")
    
    handle_resume_message(mock_channel, body)
    
    mock_process.assert_called_once()
    mock_publish.assert_called_once()
    published_event = mock_publish.call_args[0][1]
    assert published_event.status == "FAILED"
    assert published_event.error_message == RESUME_PARSE_FAILED_MESSAGE

@patch("app.mq.consumer.process_conversation")
@patch("app.mq.consumer.publish_ai_result")
def test_handle_conversation_message_success(mock_publish, mock_process):
    mock_channel = MagicMock()
    body = json.dumps({"conversationId": "conv-1", "userId": "user-1", "currentMessage": "hello", "messageHistory": []}).encode("utf-8")
    
    mock_result = AiResultEvent(referenceId="conv-1", type="CONVERSATION_REPLY", status="COMPLETED", data={})
    mock_process.return_value = mock_result
    
    handle_conversation_message(mock_channel, body)
    
    mock_process.assert_called_once()
    mock_publish.assert_called_once_with(mock_channel, mock_result)

@patch("app.mq.consumer.process_conversation")
@patch("app.mq.consumer.publish_ai_result")
def test_handle_conversation_message_failure(mock_publish, mock_process):
    mock_channel = MagicMock()
    body = json.dumps({"conversationId": "conv-1", "userId": "user-1", "currentMessage": "hello", "messageHistory": []}).encode("utf-8")
    
    mock_process.side_effect = Exception("Processing failed")
    
    handle_conversation_message(mock_channel, body)
    
    mock_process.assert_called_once()
    mock_publish.assert_called_once()
    published_event = mock_publish.call_args[0][1]
    assert published_event.status == "FAILED"
    assert published_event.error_message == CONVERSATION_FAILED_MESSAGE

@patch("app.mq.consumer.rank_jobs")
@patch("app.mq.consumer.publish_job_rank_result")
def test_handle_job_rank_message_success(mock_publish, mock_rank):
    mock_channel = MagicMock()
    body = json.dumps({"matchId": "match-1", "userId": "user-1", "resumeVersionId": "res-1", "query": "test"}).encode("utf-8")
    
    mock_result = JobRankResultPayload(matchId="match-1", status="COMPLETED", rankTimeMs=100, rankedResults=[])
    mock_rank.return_value = mock_result

    handle_job_rank_message(mock_channel, body)

    mock_rank.assert_called_once()
    mock_publish.assert_called_once_with(
        mock_channel,
        match_id="match-1",
        status="COMPLETED",
        rank_time_ms=100,
        ranked_results=[],
    )

@patch("app.mq.consumer.rank_jobs")
@patch("app.mq.consumer.publish_job_rank_result")
def test_handle_job_rank_message_failure(mock_publish, mock_rank):
    mock_channel = MagicMock()
    body = json.dumps({"matchId": "match-1", "userId": "user-1", "resumeVersionId": "res-1", "query": "test"}).encode("utf-8")
    
    mock_rank.side_effect = Exception("Processing failed")

    handle_job_rank_message(mock_channel, body)

    mock_rank.assert_called_once()
    mock_publish.assert_called_once_with(
        mock_channel,
        match_id="match-1",
        status="FAILED",
        rank_time_ms=0,
        ranked_results=[],
        error_message=JOB_RANK_FAILED_MESSAGE,
    )

def test_async_handler_acknowledges_success():
    mock_channel = MagicMock()
    mock_connection = MagicMock()
    mock_channel.connection = mock_connection
    mock_method = MagicMock()
    mock_method.delivery_tag = 1
    mock_handle = MagicMock()

    callback = _async_handler(mock_handle)
    callback(mock_channel, mock_method, None, b"body")

    mock_handle.assert_called_once_with(mock_channel, b"body")
    mock_connection.add_callback_threadsafe.assert_called_once()
    ack_callback = mock_connection.add_callback_threadsafe.call_args[0][0]
    ack_callback()
    mock_channel.basic_ack.assert_called_once_with(delivery_tag=1)


def test_async_handler_nacks_failure():
    mock_channel = MagicMock()
    mock_connection = MagicMock()
    mock_channel.connection = mock_connection
    mock_method = MagicMock()
    mock_method.delivery_tag = 1
    mock_handle = MagicMock(side_effect=Exception("Error"))

    callback = _async_handler(mock_handle)
    callback(mock_channel, mock_method, None, b"body")

    mock_handle.assert_called_once_with(mock_channel, b"body")
    mock_connection.add_callback_threadsafe.assert_called_once()
    nack_callback = mock_connection.add_callback_threadsafe.call_args[0][0]
    nack_callback()
    mock_channel.basic_nack.assert_called_once_with(delivery_tag=1, requeue=False)

def test_start_all_consumers():
    mock_channel = MagicMock()
    start_all_consumers(mock_channel)
    
    mock_channel.basic_qos.assert_called_once_with(prefetch_count=1)
    assert mock_channel.basic_consume.call_count == 4
    for call in mock_channel.basic_consume.call_args_list:
        assert call.kwargs["auto_ack"] is False
        assert callable(call.kwargs["on_message_callback"])
    mock_channel.start_consuming.assert_called_once()
