from app.services.suitability_service import (
    _normalize_items,
    _calculate_experience_score,
    _tokenize_text,
    _build_experience_text,
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

def test_tokenize_text():
    text = "Hello world! This is a test 123."
    tokens = _tokenize_text(text)
    assert tokens == {"hello", "world", "this", "test"}

def test_build_experience_text():
    experience_items = [
        {"title": "Software Engineer", "company": "Google", "summary": "Wrote code."},
        {"title": "Intern", "company": "Microsoft"},
        "Not a dict",
        {"summary": "Did things."}
    ]
    text = _build_experience_text(experience_items)
    assert "Software Engineer" in text
    assert "Google" in text
    assert "Wrote code." in text
    assert "Intern" in text
    assert "Microsoft" in text
    assert "Did things." in text
    assert "Not a dict" not in text

def test_clamp_score():
    assert _clamp_score(-0.5) == 0.0
    assert _clamp_score(0.5) == 0.5
    assert _clamp_score(1.5) == 1.0
