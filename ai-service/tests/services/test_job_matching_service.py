from unittest.mock import MagicMock, patch

from app.schemas import JobMatchRequest
from app.services.job_matching_service import (
    _clip_score,
    _extract_search_results,
    _to_float,
    _truncate_description,
    find_job_matches,
)


def test_clip_score():
    assert _clip_score(0.5) == 0.5
    assert _clip_score(1.5) == 1.0
    assert _clip_score(-0.5) == 0.0
    assert _clip_score(1.0) == 1.0
    assert _clip_score(0.0) == 0.0


def test_truncate_description():
    assert _truncate_description("Short description") == "Short description"
    assert _truncate_description("   Padded description   ") == "Padded description"
    assert _truncate_description("Line 1\nLine 2") == "Line 1 Line 2"

    long_desc = "A" * 300
    truncated = _truncate_description(long_desc)
    assert len(truncated) == 283  # 280 + 3 for "..."
    assert truncated.endswith("...")
    assert truncated.startswith("A" * 280)


def test_extract_search_results():
    # Valid items with required matchFactors fields
    valid_item = {
        "jobId": "test-1",
        "matchFactors": {"skillMatch": 0.0, "experienceMatch": 0.0, "locationMatch": 0.0},
    }
    assert len(_extract_search_results([valid_item, valid_item])) == 2
    # Dict payload with data
    assert len(_extract_search_results({"data": [valid_item]})) == 1
    # Invalid payloads
    assert _extract_search_results(None) == []
    assert _extract_search_results("string") == []
    assert _extract_search_results({"other": []}) == []
    # Non-dict items are filtered out; invalid-dict items raise ValidationError
    # Items 1 and 2 (int) are filtered; {missing_fields} raises ValidationError uncaught
    with pytest.raises(Exception):
        _extract_search_results([1, 2, {"missing": "fields"}])


def test_to_float():
    assert _to_float(1.5) == 1.5
    assert _to_float("2.5") == 2.5
    assert _to_float(1) == 1.0
    assert _to_float("invalid") == 0.0
    assert _to_float(None) == 0.0
    assert _to_float("invalid", default=1.0) == 1.0


@patch("app.services.job_matching_service.requests.post")
@patch("app.services.job_matching_service.generate_embedding")
def test_find_job_matches_calls_backend_vector_search(
    mock_generate_embedding, mock_post
):
    mock_generate_embedding.return_value = [0.1, 0.2, 0.3]

    mock_response = MagicMock()
    mock_response.raise_for_status.return_value = None
    mock_response.json.return_value = [
        {
            "jobId": "job-1",
            "title": "Python Developer",
            "company": "Tech Inc",
            "similarity": 0.87654,
            "matchFactors": {
                "skillMatch": 0.9,
                "experienceMatch": 0.8,
                "locationMatch": 0.7,
            },
            "description": "Build APIs with Python.",
        }
    ]
    mock_post.return_value = mock_response

    request = JobMatchRequest(
        userId="user1",
        query="Python Developer",
        topK=5,
        filters={"location": "Tempe"},
    )

    response = find_job_matches(request)

    assert response.total == 1
    assert len(response.matches) == 1
    assert response.matches[0].job_id == "job-1"
    assert response.matches[0].match_score == 0.8765
    assert response.matches[0].match_factors.skill_match == 0.9

    mock_generate_embedding.assert_called_once_with("Python Developer")
    mock_post.assert_called_once()
    _, kwargs = mock_post.call_args
    assert kwargs["json"] == {
        "queryText": "Python Developer",
        "queryEmbedding": [0.1, 0.2, 0.3],
        "limit": 5,
        "filters": {"location": "Tempe"},
    }


@patch("app.services.job_matching_service.requests.post")
@patch("app.services.job_matching_service.generate_embedding")
def test_find_job_matches_handles_embedding_failure(mock_generate_embedding, mock_post):
    mock_generate_embedding.side_effect = RuntimeError("embedding unavailable")

    mock_response = MagicMock()
    mock_response.raise_for_status.return_value = None
    mock_response.json.return_value = []
    mock_post.return_value = mock_response

    request = JobMatchRequest(userId="user1", query="Python Developer", topK=5)

    response = find_job_matches(request)

    assert response.total == 0
    assert response.matches == []
    assert mock_post.call_args.kwargs["json"]["queryEmbedding"] is None


@patch("app.services.job_matching_service.requests.post")
@patch("app.services.job_matching_service.generate_embedding")
def test_find_job_matches_handles_backend_failure(mock_generate_embedding, mock_post):
    mock_generate_embedding.return_value = [0.1, 0.2, 0.3]
    mock_post.side_effect = RuntimeError("backend unavailable")

    request = JobMatchRequest(userId="user1", query="Python Developer", topK=5)

    response = find_job_matches(request)

    assert response.total == 0
    assert response.matches == []
    assert response.recall_time == 0
    assert response.rank_time == 0


@patch("app.services.job_matching_service.requests.post")
@patch("app.services.job_matching_service.generate_embedding")
def test_find_job_matches_data_assembly_and_clipping(
    mock_generate_embedding, mock_post
):
    mock_generate_embedding.return_value = [0.1, 0.2]

    mock_response = MagicMock()
    mock_response.raise_for_status.return_value = None
    mock_response.json.return_value = [
        {
            "jobId": "job-1",
            "title": "Python Developer",
            "company": "Tech Inc",
            "similarity": 1.5,  # Should be clipped to 1.0
            "matchFactors": {
                "skillMatch": 1.2,  # Should be clipped to 1.0
                "experienceMatch": -0.5,  # Should be clipped to 0.0
                "locationMatch": 0.0,
            },
            "description": "A" * 300,  # Should be truncated
        },
        {
            "jobId": "job-2",
            "matchFactors": {"skillMatch": 0.0, "experienceMatch": 0.0, "locationMatch": 0.0},
        },
    ]
    mock_post.return_value = mock_response

    request = JobMatchRequest(userId="user1", query="Python", topK=2)
    response = find_job_matches(request)

    assert response.total == 2
    assert len(response.matches) == 2

    match1 = response.matches[0]
    assert match1.job_id == "job-1"
    assert match1.match_score == 1.0
    assert match1.match_factors.skill_match == 1.0
    assert match1.match_factors.experience_match == 0.0
    assert match1.match_factors.location_match == 0.0
    assert len(match1.description) == 283
    assert match1.description.endswith("...")

    match2 = response.matches[1]
    assert match2.job_id == "job-2"
    assert match2.title == ""
    assert match2.company == ""
    assert match2.match_score == 0.0
    assert match2.match_factors.skill_match == 0.0
    assert match2.match_factors.experience_match == 0.0
    assert match2.match_factors.location_match == 0.0
    assert match2.description == ""


def test_find_job_matches_empty_query():
    request = JobMatchRequest(userId="user1", query="", topK=5)

    response = find_job_matches(request)

    assert response.total == 0
    assert response.matches == []
