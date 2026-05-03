import json
import pytest
from unittest.mock import MagicMock, patch

from app.mq.consumer import (
    create_connection,
    setup_all_queues,
    parse_job_command,
    parse_resume_command,
    parse_vector_command,
    parse_conversation_command,
    parse_job_rank_command,
    build_failed_event,
    handle_job_message,
    handle_resume_message,
    handle_vector_message,
    handle_conversation_message,
    handle_job_rank_message,
    job_message_callback,
    resume_message_callback,
    vector_message_callback,
    conversation_message_callback,
    job_rank_message_callback,
    start_all_consumers,
)
from app.schemas import (
    JobParseCommand,
    ResumeParseCommand,
    VectorGenCommand,
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
    
    mock_channel.exchange_declare.assert_called_once()
    assert mock_channel.queue_declare.call_count == 10
    assert mock_channel.queue_bind.call_count == 10

def test_parse_commands():
    job_body = json.dumps({"jobId": "job-1", "url": "http://test.com/job.pdf", "imageCheckEnabled": False}).encode("utf-8")
    job_cmd = parse_job_command(job_body)
    assert isinstance(job_cmd, JobParseCommand)
    assert job_cmd.job_id == "job-1"
    
    resume_body = json.dumps({"resumeId": "res-1", "fileUrl": "http://test.com/res.pdf", "fileType": "pdf"}).encode("utf-8")
    res_cmd = parse_resume_command(resume_body)
    assert isinstance(res_cmd, ResumeParseCommand)
    assert res_cmd.resume_id == "res-1"
    
    vector_body = json.dumps({"referenceId": "ref-1", "entityType": "JOB", "text": "test"}).encode("utf-8")
    vec_cmd = parse_vector_command(vector_body)
    assert isinstance(vec_cmd, VectorGenCommand)
    assert vec_cmd.reference_id == "ref-1"
    
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
    assert published_event.error_message == "Processing failed"

@patch("app.mq.consumer.process_resume")
@patch("app.mq.consumer.publish_ai_result")
def test_handle_resume_message_success(mock_publish, mock_process):
    mock_channel = MagicMock()
    body = json.dumps({"resumeId": "res-1", "fileUrl": "http://test.com/res.pdf", "fileType": "pdf"}).encode("utf-8")
    
    mock_result = AiResultEvent(referenceId="res-1", type="RESUME_PARSE", status="COMPLETED", data={})
    mock_process.return_value = mock_result
    
    handle_resume_message(mock_channel, body)
    
    mock_process.assert_called_once()
    mock_publish.assert_called_once_with(mock_channel, mock_result)

@patch("app.mq.consumer.process_resume")
@patch("app.mq.consumer.publish_ai_result")
def test_handle_resume_message_failure(mock_publish, mock_process):
    mock_channel = MagicMock()
    body = json.dumps({"resumeId": "res-1", "fileUrl": "http://test.com/res.pdf", "fileType": "pdf"}).encode("utf-8")
    
    mock_process.side_effect = Exception("Processing failed")
    
    handle_resume_message(mock_channel, body)
    
    mock_process.assert_called_once()
    mock_publish.assert_called_once()
    published_event = mock_publish.call_args[0][1]
    assert published_event.status == "FAILED"
    assert published_event.error_message == "Processing failed"

@patch("app.mq.consumer.process_vector")
@patch("app.mq.consumer.publish_ai_result")
def test_handle_vector_message_success(mock_publish, mock_process):
    mock_channel = MagicMock()
    body = json.dumps({"referenceId": "ref-1", "entityType": "JOB", "text": "test"}).encode("utf-8")
    
    mock_result = AiResultEvent(referenceId="ref-1", type="VECTOR_GEN", status="COMPLETED", data={})
    mock_process.return_value = mock_result
    
    handle_vector_message(mock_channel, body)
    
    mock_process.assert_called_once()
    mock_publish.assert_called_once_with(mock_channel, mock_result)

@patch("app.mq.consumer.process_vector")
@patch("app.mq.consumer.publish_ai_result")
def test_handle_vector_message_failure(mock_publish, mock_process):
    mock_channel = MagicMock()
    body = json.dumps({"referenceId": "ref-1", "entityType": "JOB", "text": "test"}).encode("utf-8")
    
    mock_process.side_effect = Exception("Processing failed")
    
    handle_vector_message(mock_channel, body)
    
    mock_process.assert_called_once()
    mock_publish.assert_called_once()
    published_event = mock_publish.call_args[0][1]
    assert published_event.status == "FAILED"
    assert published_event.error_message == "Processing failed"

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
    assert published_event.error_message == "Processing failed"

@patch("app.mq.consumer.rank_jobs")
@patch("app.mq.consumer.publish_job_rank_result")
def test_handle_job_rank_message_success(mock_publish, mock_rank):
    mock_channel = MagicMock()
    body = json.dumps({"matchId": "match-1", "userId": "user-1", "resumeVersionId": "res-1", "query": "test"}).encode("utf-8")
    
    mock_result = JobRankResultPayload(matchId="match-1", status="COMPLETED", rankTimeMs=100, rankedResults=[])
    mock_rank.return_value = mock_result
    
    handle_job_rank_message(mock_channel, body)
    
    mock_rank.assert_called_once()
    mock_publish.assert_called_once_with(mock_channel, mock_result.model_dump(by_alias=True))

@patch("app.mq.consumer.rank_jobs")
@patch("app.mq.consumer.publish_job_rank_result")
def test_handle_job_rank_message_failure(mock_publish, mock_rank):
    mock_channel = MagicMock()
    body = json.dumps({"matchId": "match-1", "userId": "user-1", "resumeVersionId": "res-1", "query": "test"}).encode("utf-8")
    
    mock_rank.side_effect = Exception("Processing failed")
    
    handle_job_rank_message(mock_channel, body)
    
    mock_rank.assert_called_once()
    mock_publish.assert_called_once()
    published_payload = mock_publish.call_args[0][1]
    assert published_payload["status"] == "FAILED"
    assert published_payload["errorMessage"] == "Processing failed"

@patch("app.mq.consumer.handle_job_message")
def test_job_message_callback_success(mock_handle):
    mock_channel = MagicMock()
    mock_method = MagicMock()
    mock_method.delivery_tag = 1
    
    job_message_callback(mock_channel, mock_method, None, b"body")
    
    mock_handle.assert_called_once_with(mock_channel, b"body")
    mock_channel.basic_ack.assert_called_once_with(delivery_tag=1)

@patch("app.mq.consumer.handle_job_message")
def test_job_message_callback_failure(mock_handle):
    mock_channel = MagicMock()
    mock_method = MagicMock()
    mock_method.delivery_tag = 1
    
    mock_handle.side_effect = Exception("Error")
    
    job_message_callback(mock_channel, mock_method, None, b"body")
    
    mock_handle.assert_called_once_with(mock_channel, b"body")
    mock_channel.basic_nack.assert_called_once_with(delivery_tag=1, requeue=False)

@patch("app.mq.consumer.handle_resume_message")
def test_resume_message_callback_success(mock_handle):
    mock_channel = MagicMock()
    mock_method = MagicMock()
    mock_method.delivery_tag = 1
    
    resume_message_callback(mock_channel, mock_method, None, b"body")
    
    mock_handle.assert_called_once_with(mock_channel, b"body")
    mock_channel.basic_ack.assert_called_once_with(delivery_tag=1)

@patch("app.mq.consumer.handle_resume_message")
def test_resume_message_callback_failure(mock_handle):
    mock_channel = MagicMock()
    mock_method = MagicMock()
    mock_method.delivery_tag = 1
    
    mock_handle.side_effect = Exception("Error")
    
    resume_message_callback(mock_channel, mock_method, None, b"body")
    
    mock_handle.assert_called_once_with(mock_channel, b"body")
    mock_channel.basic_nack.assert_called_once_with(delivery_tag=1, requeue=False)

@patch("app.mq.consumer.handle_vector_message")
def test_vector_message_callback_success(mock_handle):
    mock_channel = MagicMock()
    mock_method = MagicMock()
    mock_method.delivery_tag = 1
    
    vector_message_callback(mock_channel, mock_method, None, b"body")
    
    mock_handle.assert_called_once_with(mock_channel, b"body")
    mock_channel.basic_ack.assert_called_once_with(delivery_tag=1)

@patch("app.mq.consumer.handle_vector_message")
def test_vector_message_callback_failure(mock_handle):
    mock_channel = MagicMock()
    mock_method = MagicMock()
    mock_method.delivery_tag = 1
    
    mock_handle.side_effect = Exception("Error")
    
    vector_message_callback(mock_channel, mock_method, None, b"body")
    
    mock_handle.assert_called_once_with(mock_channel, b"body")
    mock_channel.basic_nack.assert_called_once_with(delivery_tag=1, requeue=False)

@patch("app.mq.consumer.handle_conversation_message")
def test_conversation_message_callback_success(mock_handle):
    mock_channel = MagicMock()
    mock_method = MagicMock()
    mock_method.delivery_tag = 1
    
    conversation_message_callback(mock_channel, mock_method, None, b"body")
    
    mock_handle.assert_called_once_with(mock_channel, b"body")
    mock_channel.basic_ack.assert_called_once_with(delivery_tag=1)

@patch("app.mq.consumer.handle_conversation_message")
def test_conversation_message_callback_failure(mock_handle):
    mock_channel = MagicMock()
    mock_method = MagicMock()
    mock_method.delivery_tag = 1
    
    mock_handle.side_effect = Exception("Error")
    
    conversation_message_callback(mock_channel, mock_method, None, b"body")
    
    mock_handle.assert_called_once_with(mock_channel, b"body")
    mock_channel.basic_nack.assert_called_once_with(delivery_tag=1, requeue=False)

@patch("app.mq.consumer.handle_job_rank_message")
def test_job_rank_message_callback_success(mock_handle):
    mock_channel = MagicMock()
    mock_method = MagicMock()
    mock_method.delivery_tag = 1
    
    job_rank_message_callback(mock_channel, mock_method, None, b"body")
    
    mock_handle.assert_called_once_with(mock_channel, b"body")
    mock_channel.basic_ack.assert_called_once_with(delivery_tag=1)

@patch("app.mq.consumer.handle_job_rank_message")
def test_job_rank_message_callback_failure(mock_handle):
    mock_channel = MagicMock()
    mock_method = MagicMock()
    mock_method.delivery_tag = 1
    
    mock_handle.side_effect = Exception("Error")
    
    job_rank_message_callback(mock_channel, mock_method, None, b"body")
    
    mock_handle.assert_called_once_with(mock_channel, b"body")
    mock_channel.basic_nack.assert_called_once_with(delivery_tag=1, requeue=False)

def test_start_all_consumers():
    mock_channel = MagicMock()
    start_all_consumers(mock_channel)
    
    mock_channel.basic_qos.assert_called_once_with(prefetch_count=1)
    assert mock_channel.basic_consume.call_count == 5
    mock_channel.start_consuming.assert_called_once()
