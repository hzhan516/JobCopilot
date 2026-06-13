from unittest.mock import MagicMock, patch, AsyncMock
import asyncio
import pytest

from app.worker_main import start_mq_consumer, run_worker
from app.config import FEEDBACK_REQUEST_QUEUE


@patch("app.worker_main.create_worker_connection")
@patch("app.worker_main.setup_feedback_queue")
def test_start_mq_consumer_sets_up_and_consumes(mock_setup, mock_create_conn):
    """Should create connection, setup queue, configure QoS and start consuming / 应创建连接、设置队列、配置 QoS 并开始消费"""
    mock_channel = MagicMock()
    mock_connection = MagicMock()
    mock_connection.channel.return_value = mock_channel
    mock_create_conn.return_value = mock_connection

    # Simulate channel.start_consuming() raising SystemExit to break the infinite loop
    # SystemExit inherits from BaseException, NOT Exception, so it bypasses the
    # production code's `except Exception` retry block.
    mock_channel.start_consuming.side_effect = SystemExit()

    try:
        start_mq_consumer()
    except SystemExit:
        pass

    mock_create_conn.assert_called_once()
    mock_setup.assert_called_once_with(mock_channel)
    mock_channel.basic_qos.assert_called_once_with(prefetch_count=1)
    consume_call = mock_channel.basic_consume.call_args
    assert consume_call.kwargs["queue"] == FEEDBACK_REQUEST_QUEUE
    assert consume_call.kwargs["auto_ack"] is False
    assert callable(consume_call.kwargs["on_message_callback"])
    mock_channel.start_consuming.assert_called_once()


@patch("app.worker_main.time.sleep", return_value=None)
@patch("app.worker_main.create_worker_connection")
@patch("app.worker_main.setup_feedback_queue")
def test_start_mq_consumer_retries_on_failure(mock_setup, mock_create_conn, mock_sleep):
    """Should retry on connection failure with exponential-like delay / 连接失败时应重试"""
    mock_channel = MagicMock()
    mock_conn = MagicMock()
    mock_conn.channel.return_value = mock_channel
    mock_create_conn.side_effect = [Exception("conn failed"), mock_conn]
    # SystemExit bypasses except Exception in the retry loop
    mock_channel.start_consuming.side_effect = SystemExit()

    try:
        start_mq_consumer()
    except SystemExit:
        pass

    assert mock_create_conn.call_count == 2
    mock_sleep.assert_called_once_with(5)
    mock_setup.assert_called_once_with(mock_channel)


@pytest.mark.asyncio
@patch("app.worker_main.start_mq_consumer")
@patch("app.worker_main.AsyncIOScheduler")
@patch("app.worker_main.IncrementalTrainer")
async def test_run_worker_starts_scheduler_and_trains(
    mock_trainer_cls, mock_scheduler_cls, mock_start_mq
):
    """Should start MQ thread, scheduler and run initial training / 应启动 MQ 线程、调度器和首次训练"""
    mock_trainer = MagicMock()
    mock_trainer.try_retrain = AsyncMock()
    mock_trainer_cls.return_value = mock_trainer

    mock_scheduler = MagicMock()
    mock_scheduler_cls.return_value = mock_scheduler

    # Cancel the infinite sleep quickly
    async def cancel_after_short():
        await asyncio.sleep(0.01)
        for task in asyncio.all_tasks():
            if task is not asyncio.current_task():
                task.cancel()

    try:
        await asyncio.wait_for(
            asyncio.gather(
                run_worker(),
                cancel_after_short(),
            ),
            timeout=1,
        )
    except (asyncio.CancelledError, asyncio.TimeoutError):
        pass

    mock_start_mq.assert_called_once()
    mock_scheduler.add_job.assert_called_once()
    job_call = mock_scheduler.add_job.call_args
    assert job_call.args[0] == mock_trainer.try_retrain
    assert job_call.kwargs["hour"] == 2
    assert job_call.kwargs["minute"] == 0
    mock_scheduler.start.assert_called_once()
    mock_trainer.try_retrain.assert_awaited_once()
    # scheduler.shutdown is not called here because the task is cancelled
    # (CancelledError) rather than stopped via KeyboardInterrupt/SystemExit
