from unittest.mock import patch
from app.schemas import JobMatchRequest, MatchItem
from app.services.job_matching_service import (
    _cosine_similarity,
    _clip_score,
    _tokenize,
    _job_text,
    _get_job_embedding,
    _score_job,
    find_job_matches,
    _JOB_EMBEDDING_CACHE
)


def test_cosine_similarity():
    # Identical vectors
    assert _cosine_similarity([1.0, 0.0], [1.0, 0.0]) == 1.0
    # Orthogonal vectors
    assert _cosine_similarity([1.0, 0.0], [0.0, 1.0]) == 0.0
    # Opposite vectors
    assert _cosine_similarity([1.0, 0.0], [-1.0, 0.0]) == -1.0
    # Different lengths
    assert _cosine_similarity([1.0, 0.0], [1.0]) == 0.0
    # Empty vectors
    assert _cosine_similarity([], []) == 0.0
    # Zero vectors
    assert _cosine_similarity([0.0, 0.0], [1.0, 1.0]) == 0.0


def test_clip_score():
    assert _clip_score(0.5) == 0.5
    assert _clip_score(1.5) == 1.0
    assert _clip_score(-0.5) == 0.0
    assert _clip_score(1.0) == 1.0
    assert _clip_score(0.0) == 0.0


def test_tokenize():
    assert _tokenize("Hello World!") == {"hello", "world"}
    # C++ becomes c, which is < 2 chars, so it's dropped
    assert _tokenize("C++ Developer") == {"developer"}
    assert _tokenize("A B C") == set()  # Single chars are dropped
    assert _tokenize("Software Engineer, Backend") == {
        "software", "engineer", "backend"}


def test_job_text():
    job = {
        "title": "Software Engineer",
        "company": "Tech Corp",
        "description": "Build cool stuff.",
        "requirements": ["Python", "SQL"]
    }
    assert _job_text(
        job) == "Software Engineer Tech Corp Build cool stuff. Python SQL"


@patch("app.services.job_matching_service.generate_embedding")
def test_get_job_embedding(mock_generate_embedding):
    mock_generate_embedding.return_value = [0.1, 0.2, 0.3]

    # Clear cache
    _JOB_EMBEDDING_CACHE.clear()

    job = {"job_id": "123", "title": "Test Job"}

    # First call should generate embedding
    embedding = _get_job_embedding(job)
    assert embedding == [0.1, 0.2, 0.3]
    mock_generate_embedding.assert_called_once()

    # Second call should use cache
    embedding2 = _get_job_embedding(job)
    assert embedding2 == [0.1, 0.2, 0.3]
    assert mock_generate_embedding.call_count == 1

    # Missing job_id
    assert _get_job_embedding({"title": "No ID"}) is None


@patch("app.services.job_matching_service._get_job_embedding")
def test_score_job(mock_get_job_embedding):
    mock_get_job_embedding.return_value = [1.0, 0.0]

    job = {
        "job_id": "1",
        "title": "Python Developer",
        "company": "Tech Inc",
        "description": "Looking for a Python developer in New York.",
        "requirements": ["Python", "Django"]
    }

    query = "Python Developer"
    query_embedding = [1.0, 0.0]
    filters = {"location": "New York"}

    match_item = _score_job(query, query_embedding, job, filters)

    assert isinstance(match_item, MatchItem)
    assert match_item.job_id == "1"
    assert match_item.title == "Python Developer"
    assert match_item.company == "Tech Inc"

    # Semantic match: cosine_similarity([1,0], [1,0]) = 1.0
    # -> (1.0 + 1.0) / 2.0 = 1.0
    # Skill match: query tokens {"python", "developer"}
    # Title tokens {"python", "developer"}
    # Requirements tokens {"python", "django"}
    # Skill source {"python", "developer", "django"}
    # Intersection {"python", "developer"} -> 2/2 = 1.0
    # Experience match: description tokens
    # {"looking", "for", "python", "developer", "in", "new", "york"}
    # Intersection {"python", "developer"} -> 2/2 = 1.0
    # Location match: filter tokens {"new", "york"}
    # Intersection with description tokens -> True -> 1.0
    # Match score: (1.0 * 0.4) + (1.0 * 0.2) + (1.0 * 0.1) + (1.0 * 0.3) = 1.0

    assert match_item.match_score == 1.0
    assert match_item.match_factors.skill_match == 1.0
    assert match_item.match_factors.experience_match == 1.0
    assert match_item.match_factors.location_match == 1.0


@patch("app.services.job_matching_service._load_jobs")
@patch("app.services.job_matching_service.generate_embedding")
@patch("app.services.job_matching_service._get_job_embedding")
def test_find_job_matches(
        mock_get_job_embedding,
        mock_generate_embedding,
        mock_load_jobs):
    mock_load_jobs.return_value = [
        {
            "job_id": "1",
            "title": "Python Developer",
            "company": "Tech Inc",
            "description": "Looking for a Python developer in New York.",
            "requirements": ["Python", "Django"]
        },
        {
            "job_id": "2",
            "title": "Java Developer",
            "company": "Enterprise Corp",
            "description": "Looking for a Java developer in London.",
            "requirements": ["Java", "Spring"]
        }
    ]

    mock_generate_embedding.return_value = [1.0, 0.0]
    mock_get_job_embedding.side_effect = lambda job: [
        1.0, 0.0] if job["job_id"] == "1" else [0.0, 1.0]

    request = JobMatchRequest(
        userId="user1",
        query="Python Developer",
        topK=10,
        filters={"location": "New York"}
    )

    response = find_job_matches(request)

    assert response.total == 1
    assert len(response.matches) == 1
    assert response.matches[0].job_id == "1"

    # Test empty query
    empty_request = JobMatchRequest(
        userId="user1",
        query="",
        topK=10
    )
    empty_response = find_job_matches(empty_request)
    assert empty_response.total == 0
    assert len(empty_response.matches) == 0


@patch("app.services.job_matching_service._load_jobs")
@patch("app.services.job_matching_service.generate_embedding")
@patch("app.services.job_matching_service._get_job_embedding")
def test_find_job_matches_no_filters(
        mock_get_job_embedding,
        mock_generate_embedding,
        mock_load_jobs):
    mock_load_jobs.return_value = [
        {
            "job_id": "1",
            "title": "Python Developer",
            "company": "Tech Inc",
            "description": "Looking for a Python developer in New York.",
            "requirements": ["Python", "Django"]
        },
        {
            "job_id": "2",
            "title": "Java Developer",
            "company": "Enterprise Corp",
            "description": "Looking for a Java developer in London.",
            "requirements": ["Java", "Spring"]
        }
    ]

    mock_generate_embedding.return_value = [1.0, 0.0]
    mock_get_job_embedding.side_effect = lambda job: [
        1.0, 0.0] if job["job_id"] == "1" else [0.0, 1.0]

    request = JobMatchRequest(
        userId="user1",
        query="Developer",
        topK=10
    )

    response = find_job_matches(request)

    assert response.total == 2
    assert len(response.matches) == 2
    # Job 1 should be ranked higher because of semantic match (cosine
    # similarity 1.0 vs 0.0)
    assert response.matches[0].job_id == "1"
    assert response.matches[1].job_id == "2"
