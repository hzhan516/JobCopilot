from app.services.suitability_service import (
    _normalize_items,
    _calculate_experience_score,
    _clamp_score,
)

def test_normalize_items():
    items = [" Python ", "JAVA", "", "  ", "python"]
    normalized = _normalize_items(items)
    assert normalized == {"python", "java"}

def test_calculate_experience_score():
    assert _calculate_experience_score([]) == 0.2
    assert _calculate_experience_score([{}]) == 0.4
    assert _calculate_experience_score([{}, {}]) == 0.6
    assert _calculate_experience_score([{}, {}, {}]) == 0.8
    assert _calculate_experience_score([{}, {}, {}, {}]) == 1.0

def test_clamp_score():
    assert _clamp_score(-0.5) == 0.0
    assert _clamp_score(0.5) == 0.5
    assert _clamp_score(1.5) == 1.0
