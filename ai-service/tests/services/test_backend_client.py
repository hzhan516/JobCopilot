"""Test backend client module.
后端客户端测试：覆盖向量 upsert、批量操作及异常降级。
"""

from unittest.mock import MagicMock, patch

import httpx

from app.services.backend_client import (
    _build_job_vector_item,
    _build_resume_vector_item,
    batch_upsert_job_vectors,
    batch_upsert_resume_vectors,
)
from app.schemas import JobVectorItem, ResumeVectorItem

# ── _build_job_vector_item ─────────────────────────────────


def test_build_job_vector_item():
    """Should construct a JobVectorItem with provided fields.
    应构造 JobVectorItem 并填充提供的字段。"""
    result = _build_job_vector_item(
        reference_id="job-1",
        embedding=[0.1] * 768,
        title="Engineer",
        description="Build stuff",
        requirements=["Python"],
    )
    assert result.job_id == "job-1"
    assert result.title == "Engineer"
    assert result.requirements == ["Python"]
    assert result.embedding == [0.1] * 768


# ── _build_resume_vector_item ──────────────────────────────


def test_build_resume_vector_item():
    """Should construct a ResumeVectorItem with minimal fields.
    应构造 ResumeVectorItem 并仅填充最小必要字段。"""
    result = _build_resume_vector_item(
        reference_id="res-1",
        embedding=[0.2] * 768,
    )
    assert result.resume_version_id == "res-1"
    assert result.embedding == [0.2] * 768


# ── batch_upsert_job_vectors ───────────────────────────────


@patch("app.services.backend_client.requests.post")
def test_batch_upsert_job_vectors_success(mock_post):
    """Successful batch upsert should return parsed response.
    成功的批量 upsert 应返回解析后的响应。"""
    mock_response = MagicMock()
    mock_response.raise_for_status.return_value = None
    mock_response.json.return_value = {"total": 2, "failed": 0, "failed_ids": []}
    mock_post.return_value = mock_response

    items = [
        JobVectorItem(
            job_id="job-1",
            embedding=[0.1] * 768,
            title="Dev",
            description="Build",
            requirements=["Python"],
            raw_content="",
            source_file="",
            model_version="",
        ),
        JobVectorItem(
            job_id="job-2",
            embedding=[0.2] * 768,
            title="Dev",
            description="Build",
            requirements=["Java"],
            raw_content="",
            source_file="",
            model_version="",
        ),
    ]
    result = batch_upsert_job_vectors(items)

    assert result.total == 2
    assert result.failed == 0
    mock_post.assert_called_once()


@patch("app.services.backend_client.requests.post")
def test_batch_upsert_job_vectors_http_error(mock_post):
    """HTTP error should return all items as failed.
    HTTP 错误应返回所有项为失败。"""
    mock_post.side_effect = httpx.HTTPStatusError(
        "500 Server Error",
        request=httpx.Request("POST", "http://test"),
        response=httpx.Response(500),
    )

    items = [
        JobVectorItem(
            job_id="job-1",
            embedding=[0.1] * 768,
            title="Dev",
            description="Build",
            requirements=["Python"],
            raw_content="",
            source_file="",
            model_version="",
        ),
    ]
    result = batch_upsert_job_vectors(items)

    assert result.total == 1
    assert result.failed == 1
    assert "job-1" in result.failed_ids


@patch("app.services.backend_client.requests.post")
def test_batch_upsert_job_vectors_empty_list(mock_post):
    """Empty list should return empty response without calling backend.
    空列表应返回空响应且不调用后端。"""
    result = batch_upsert_job_vectors([])

    assert result.total == 0
    assert result.failed == 0
    mock_post.assert_not_called()


# ── batch_upsert_resume_vectors ────────────────────────────


@patch("app.services.backend_client.requests.post")
def test_batch_upsert_resume_vectors_success(mock_post):
    """Successful batch upsert for resumes should return parsed response.
    成功的简历批量 upsert 应返回解析后的响应。"""
    mock_response = MagicMock()
    mock_response.raise_for_status.return_value = None
    mock_response.json.return_value = {"total": 1, "failed": 0, "failed_ids": []}
    mock_post.return_value = mock_response

    items = [
        ResumeVectorItem(resume_version_id="res-1", embedding=[0.1] * 768),
    ]
    result = batch_upsert_resume_vectors(items)

    assert result.total == 1
    assert result.failed == 0


@patch("app.services.backend_client.requests.post")
def test_batch_upsert_resume_vectors_empty_list(mock_post):
    """Empty list should return empty response without calling backend.
    空列表应返回空响应且不调用后端。"""
    result = batch_upsert_resume_vectors([])

    assert result.total == 0
    assert result.failed == 0
    mock_post.assert_not_called()
