"""Test suitability service module.
适配度评估测试：覆盖主入口 evaluate_suitability_with_vertex 及工具函数。
"""
from unittest.mock import patch

import pytest

from app.schemas import (
    SuitabilityRequest,
    ResumeSnapshot,
    JobSnapshot,
    SuitabilityBreakdown,
)
from app.services.suitability_service import (
    _normalize_items,
    _calculate_experience_score,
    _clamp_score,
    evaluate_suitability_baseline,
    evaluate_suitability_with_vertex,
)


# ── Tool function tests ────────────────────────────────────

def test_normalize_items():
    """Should normalize and deduplicate skill items.
    应规范化并去重技能项。"""
    items = [" Python ", "JAVA", "", "  ", "python"]
    normalized = _normalize_items(items)
    assert normalized == {"python", "java"}


def test_calculate_experience_score():
    """Should map experience count to score using piecewise thresholds.
    应按分段阈值将经验条目数映射为分数。"""
    assert _calculate_experience_score([]) == 0.2
    assert _calculate_experience_score([{}]) == 0.4
    assert _calculate_experience_score([{}, {}]) == 0.6
    assert _calculate_experience_score([{}, {}, {}]) == 0.8
    assert _calculate_experience_score([{}, {}, {}, {}]) == 1.0


def test_clamp_score():
    """Should clamp score to [0.0, 1.0].
    应将分数限制在 [0.0, 1.0] 范围内。"""
    assert _clamp_score(-0.5) == 0.0
    assert _clamp_score(0.5) == 0.5
    assert _clamp_score(1.5) == 1.0


# ── Baseline evaluation ────────────────────────────────────

def test_evaluate_suitability_baseline_strong_match():
    """Strong skill overlap should yield high score and suitable=True.
    强技能重叠应产生高分且 suitable=True。"""
    request = SuitabilityRequest(
        resume=ResumeSnapshot(skills=["Python", "Django"], experience=[{"title": "Dev"}]),
        job=JobSnapshot(title="Python Dev", requirements=["Python", "Django"]),
    )
    result = evaluate_suitability_baseline(request)
    assert result.suitable is True
    assert result.final_score >= 0.6


def test_evaluate_suitability_baseline_no_match():
    """No skill overlap should yield low score and suitable=False.
    无技能重叠应产生低分且 suitable=False。"""
    request = SuitabilityRequest(
        resume=ResumeSnapshot(skills=["Java"], experience=[]),
        job=JobSnapshot(title="Python Dev", requirements=["Python", "Django"]),
    )
    result = evaluate_suitability_baseline(request)
    assert result.suitable is False
    assert result.final_score < 0.6


def test_evaluate_suitability_baseline_empty_requirements():
    """Empty job requirements should use default skill_score=0.5.
    空职位要求应使用默认 skill_score=0.5。"""
    request = SuitabilityRequest(
        resume=ResumeSnapshot(skills=["Python"], experience=[{}]),
        job=JobSnapshot(title="Dev", requirements=[]),
    )
    result = evaluate_suitability_baseline(request)
    assert result.breakdown.skillScore == 0.5


# ── Vertex evaluation (mocked LLM) ───────────────────────────

@patch("app.services.suitability_service.generate_json_from_text_prompt")
def test_evaluate_suitability_vertex_success(mock_generate):
    """LLM returns valid JSON should produce structured result.
    LLM 返回有效 JSON 时应产生结构化结果。"""
    mock_generate.return_value = {
        "suitable": True,
        "summary": "Strong match.",
        "skillScore": 0.85,
        "experienceScore": 0.90,
        "overallScore": 0.87,
    }

    request = SuitabilityRequest(
        resume=ResumeSnapshot(skills=["Python"], experience=[{"title": "Dev"}]),
        job=JobSnapshot(title="Python Dev", requirements=["Python"]),
    )
    result = evaluate_suitability_with_vertex(request)

    assert result.suitable is True
    assert result.final_score == 0.87
    assert result.vertexScore == 0.87
    assert result.llmModel is not None
    mock_generate.assert_called_once()


@patch("app.services.suitability_service.generate_json_from_text_prompt")
def test_evaluate_suitability_vertex_low_score(mock_generate):
    """Low LLM scores should result in suitable=False.
    低 LLM 分数应导致 suitable=False。"""
    mock_generate.return_value = {
        "suitable": False,
        "summary": "Missing skills.",
        "skillScore": 0.30,
        "experienceScore": 0.40,
        "overallScore": 0.35,
    }

    request = SuitabilityRequest(
        resume=ResumeSnapshot(skills=["Java"], experience=[]),
        job=JobSnapshot(title="Python Dev", requirements=["Python", "Django"]),
    )
    result = evaluate_suitability_with_vertex(request)

    assert result.suitable is False
    assert result.final_score == 0.35


@patch("app.services.suitability_service.generate_json_from_text_prompt")
def test_evaluate_suitability_vertex_llm_failure(mock_generate):
    """When LLM fails, should fall back to baseline evaluation.
    LLM 失败时应降级到基线评估。"""
    mock_generate.side_effect = Exception("LLM timeout")

    request = SuitabilityRequest(
        resume=ResumeSnapshot(skills=["Python"], experience=[]),
        job=JobSnapshot(title="Dev", requirements=["Python"]),
    )
    result = evaluate_suitability_with_vertex(request)

    assert result.suitable is not None  # Baseline returns something
    assert result.llmModel is None  # Fallback has no LLM model


@patch("app.services.suitability_service.generate_json_from_text_prompt")
def test_evaluate_suitability_vertex_clamps_scores(mock_generate):
    """LLM scores outside [0,1] should be clamped.
    超出 [0,1] 范围的 LLM 分数应被截断。"""
    mock_generate.return_value = {
        "suitable": True,
        "summary": "Overconfident.",
        "skillScore": 1.5,
        "experienceScore": -0.3,
        "overallScore": 2.0,
    }

    request = SuitabilityRequest(
        resume=ResumeSnapshot(skills=["Python"], experience=[{"title": "Dev"}]),
        job=JobSnapshot(title="Dev", requirements=["Python"]),
    )
    result = evaluate_suitability_with_vertex(request)

    assert result.breakdown.skillScore == 1.0
    assert result.breakdown.experienceScore == 0.0
    assert result.breakdown.overallScore == 1.0


@patch("app.services.suitability_service.generate_json_from_text_prompt")
def test_evaluate_suitability_vertex_missing_summary(mock_generate):
    """Missing summary in LLM response should use default text.
    LLM 响应缺少 summary 时应使用默认文本。"""
    mock_generate.return_value = {
        "suitable": True,
        "skillScore": 0.8,
        "experienceScore": 0.8,
        "overallScore": 0.8,
    }

    request = SuitabilityRequest(
        resume=ResumeSnapshot(skills=["Python"], experience=[{"title": "Dev"}]),
        job=JobSnapshot(title="Dev", requirements=["Python"]),
    )
    result = evaluate_suitability_with_vertex(request)

    assert "Vertex AI did not return a summary" in result.summary
