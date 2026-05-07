import json
from unittest.mock import patch, MagicMock

import litellm
import pytest
from tenacity import RetryError

from app.schemas import JobRankCommand
from app.services.job_rank_service import (
    _tokenize, 
    _clip_score, 
    _safe_llm_call, 
    _generate_match_reason, 
    rank_jobs
)

def test_tokenize():
    text = "Hello, world! This is a test-case 123."
    tokens = _tokenize(text)
    assert "hello" in tokens
    assert "world" in tokens
    assert "this" in tokens
    assert "is" in tokens
    assert "test" in tokens
    assert "case" in tokens
    assert "123" in tokens
    assert "a" not in tokens

def test_clip_score():
    assert _clip_score(-1.0) == 0.0
    assert _clip_score(0.0) == 0.0
    assert _clip_score(0.5) == 0.5
    assert _clip_score(1.0) == 1.0
    assert _clip_score(2.0) == 1.0

@patch("litellm.completion")
def test_safe_llm_call_success(mock_completion):
    mock_response = MagicMock()
    mock_response.choices = [MagicMock()]
    mock_response.choices[0].message.content = "   Great fit!   "
    mock_completion.return_value = mock_response
    
    result = _safe_llm_call("Test prompt")
    
    assert result == "Great fit!"
    mock_completion.assert_called_once()
    kwargs = mock_completion.call_args.kwargs
    assert kwargs["temperature"] == 0.3
    assert kwargs["timeout"] == 10
    assert kwargs["messages"] == [{"role": "user", "content": "Test prompt"}]

@patch("litellm.completion")
def test_safe_llm_call_retry_success(mock_completion):
    mock_response = MagicMock()
    mock_response.choices = [MagicMock()]
    mock_response.choices[0].message.content = "Worked on retry"
    
    mock_completion.side_effect = [
        litellm.exceptions.Timeout("Timeout error", model="test", llm_provider="test"),
        mock_response
    ]
    
    result = _safe_llm_call("Test prompt")
    
    assert result == "Worked on retry"
    assert mock_completion.call_count == 2

@patch("litellm.completion")
def test_safe_llm_call_max_retries_exceeded(mock_completion):
    mock_completion.side_effect = litellm.exceptions.RateLimitError(
        "Too many requests", response=MagicMock(), model="test", llm_provider="test"
    )
    
    with pytest.raises(RetryError):
        _safe_llm_call("Test prompt")
    
    assert mock_completion.call_count == 2

@patch("app.services.job_rank_service._safe_llm_call")
def test_generate_match_reason_no_resume(mock_safe_call):
    command = JobRankCommand(
        matchId="123", userId="user", resumeVersionId="v1", 
        query="", recalledJobIds=["job1"], jobDetails={}
    )
    result = _generate_match_reason(command, MagicMock())
    assert result is None
    mock_safe_call.assert_not_called()

@patch("app.services.job_rank_service._safe_llm_call")
def test_generate_match_reason_success(mock_safe_call):
    command = JobRankCommand(
        matchId="123", userId="user", resumeVersionId="v1", 
        resumeText="Java developer with 5 years experience.",
        query="", recalledJobIds=["job1"], 
        jobDetails={"job1": {"description": "Looking for Java expert"}}
    )
    
    job_mock = MagicMock()
    job_mock.job_id = "job1"
    job_mock.title = "Backend Eng"
    job_mock.company = "TechCorp"
    
    mock_safe_call.return_value = "Candidate is perfect."
    
    result = _generate_match_reason(command, job_mock)
    
    assert result == "Candidate is perfect."
    mock_safe_call.assert_called_once()
    
    prompt_sent = mock_safe_call.call_args[0][0]
    assert "<candidate_resume>" in prompt_sent
    assert "</candidate_resume>" in prompt_sent
    assert "<job_description>" in prompt_sent
    assert "UNTRUSTED raw data" in prompt_sent

@patch("app.services.job_rank_service._safe_llm_call")
def test_generate_match_reason_fallback_on_error(mock_safe_call):
    command = JobRankCommand(
        matchId="123", userId="user", resumeVersionId="v1", 
        resumeText="Java developer", query="", recalledJobIds=["job1"], 
        jobDetails={"job1": {}}
    )
    job_mock = MagicMock()
    job_mock.job_id = "job1"
    
    mock_safe_call.side_effect = Exception("LLM totally down")
    
    result = _generate_match_reason(command, job_mock)
    
    assert result is None

@patch("app.services.job_rank_service._generate_match_reason")
def test_rank_jobs(mock_generate):
    job_details = {
        "job1": {"title": "Junior Java", "description": "Needs basic java", "semanticMatch": 0.4},
        "job2": {"title": "Senior Java", "description": "Needs expert java", "semanticMatch": 0.9},
        "job3": {"title": "Python Dev", "description": "Needs python", "semanticMatch": 0.1},
        "job4": {"title": "Mid Java", "description": "Needs some java", "semanticMatch": 0.7},
    }
    
    command = JobRankCommand(
        matchId="123", userId="user", resumeVersionId="v1", 
        resumeText="Java developer with spring boot experience",
        query="Java", recalledJobIds=["job1", "job2", "job3", "job4"], 
        jobDetails=job_details
    )
    
    def mock_reason_generator(cmd, job):
        return f"Reason for {job.job_id}"
    mock_generate.side_effect = mock_reason_generator
    
    result = rank_jobs(command)
    
    assert result.match_id == "123"
    assert result.status == "COMPLETED"
    assert len(result.ranked_results) == 4
    
    # Semantic match dominates weight (0.40), so job2 > job4 > job1 > job3
    assert result.ranked_results[0].job_id == "job2"
    assert result.ranked_results[1].job_id == "job4"
    
    # Only top-3 receive LLM match reasons to control cost
    assert result.ranked_results[0].match_reason == "Reason for job2"
    assert result.ranked_results[1].match_reason == "Reason for job4"
    assert result.ranked_results[2].match_reason is not None
    assert result.ranked_results[3].match_reason is None
    
    assert mock_generate.call_count == 3
