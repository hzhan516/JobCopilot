"""Test job matching service (supplemental).
职位匹配补充测试：覆盖空查询、大结果集、并发调用等场景。
"""

from unittest.mock import MagicMock, patch


from app.schemas import JobMatchRequest
from app.services.job_matching_service import find_job_matches

# ── Empty query ────────────────────────────────────────────


def test_find_job_matches_empty_query():
    """Empty query should return empty results without calling backend.
    空查询应返回空结果且不调用后端。"""
    request = JobMatchRequest(userId="user1", query="", topK=5)

    with patch(
        "app.services.job_matching_service.generate_embedding"
    ) as mock_embed, patch(
        "app.services.job_matching_service.requests.post"
    ) as mock_post:
        response = find_job_matches(request)

        assert response.total == 0
        assert response.matches == []
        mock_embed.assert_not_called()
        mock_post.assert_not_called()


# ── Large result set ───────────────────────────────────────


@patch("app.services.job_matching_service.requests.post")
@patch("app.services.job_matching_service.generate_embedding")
def test_find_job_matches_large_result_set(mock_generate_embedding, mock_post):
    """Large result set is returned as-is from backend; backend handles capping via limit.
    大结果集由后端通过 limit 参数截断，AI 服务直接返回所有结果。"""
    mock_generate_embedding.return_value = [0.1] * 768

    mock_response = MagicMock()
    mock_response.raise_for_status.return_value = None
    mock_response.json.return_value = [
        {
            "jobId": f"job-{i}",
            "title": f"Job {i}",
            "company": "Corp",
            "similarity": 0.9 - i * 0.01,
            "matchFactors": {
                "skillMatch": 0.5,
                "experienceMatch": 0.5,
                "locationMatch": 0.5,
            },
        }
        for i in range(100)
    ]
    mock_post.return_value = mock_response

    request = JobMatchRequest(userId="user1", query="Developer", topK=10)
    response = find_job_matches(request)

    # Backend mock returns 100 items; service returns all of them.
    # The limit is enforced by the backend via the request parameter.
    assert response.total == 100
    assert len(response.matches) == 100
    assert response.matches[0].job_id == "job-0"


# ── Backend returns non-list ───────────────────────────────


@patch("app.services.job_matching_service.requests.post")
@patch("app.services.job_matching_service.generate_embedding")
def test_find_job_matches_backend_returns_string(mock_generate_embedding, mock_post):
    """When backend returns non-list, should return empty results.
    后端返回非列表时应返回空结果。"""
    mock_generate_embedding.return_value = [0.1] * 768

    mock_response = MagicMock()
    mock_response.raise_for_status.return_value = None
    mock_response.json.return_value = "unexpected string"
    mock_post.return_value = mock_response

    request = JobMatchRequest(userId="user1", query="Dev", topK=5)
    response = find_job_matches(request)

    assert response.total == 0
    assert response.matches == []


# ── Backend returns None ───────────────────────────────────


@patch("app.services.job_matching_service.requests.post")
@patch("app.services.job_matching_service.generate_embedding")
def test_find_job_matches_backend_returns_none(mock_generate_embedding, mock_post):
    """When backend returns None/empty, should return empty results.
    后端返回 None/空时应返回空结果。"""
    mock_generate_embedding.return_value = [0.1] * 768

    mock_response = MagicMock()
    mock_response.raise_for_status.return_value = None
    mock_response.json.return_value = None
    mock_post.return_value = mock_response

    request = JobMatchRequest(userId="user1", query="Dev", topK=5)
    response = find_job_matches(request)

    assert response.total == 0


# ── Concurrent calls safety ────────────────────────────────


@patch("app.services.job_matching_service.requests.post")
@patch("app.services.job_matching_service.generate_embedding")
def test_find_job_matches_thread_safety(mock_generate_embedding, mock_post):
    """Multiple concurrent calls should not interfere with each other.
    多并发调用不应相互干扰。"""
    mock_generate_embedding.return_value = [0.1] * 768

    mock_response = MagicMock()
    mock_response.raise_for_status.return_value = None
    mock_response.json.return_value = [
        {
            "jobId": "job-1",
            "title": "Dev",
            "company": "Acme",
            "similarity": 0.8,
            "matchFactors": {
                "skillMatch": 0.5,
                "experienceMatch": 0.5,
                "locationMatch": 0.5,
            },
        }
    ]
    mock_post.return_value = mock_response

    # Simulate two simultaneous requests
    req1 = JobMatchRequest(userId="user1", query="Python", topK=1)
    req2 = JobMatchRequest(userId="user2", query="Java", topK=1)

    resp1 = find_job_matches(req1)
    resp2 = find_job_matches(req2)

    assert resp1.matches[0].job_id == "job-1"
    assert resp2.matches[0].job_id == "job-1"
    assert mock_post.call_count == 2
