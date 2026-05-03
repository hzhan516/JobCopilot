from app.services.resume_parser import _normalize_skills, _normalize_experience

def test_normalize_skills():
    assert _normalize_skills([" Python ", "Java", "", "  "]) == ["Python", "Java"]
    assert _normalize_skills("Python") == ["Python"]
    assert _normalize_skills("  ") == []
    assert _normalize_skills(None) == []

def test_normalize_experience():
    assert _normalize_experience([{"title": "Engineer"}, "Wrote code", None]) == [{"title": "Engineer"}, {"summary": "Wrote code"}]
    assert _normalize_experience({"title": "Engineer"}) == [{"title": "Engineer"}]
    assert _normalize_experience("Wrote code") == [{"summary": "Wrote code"}]
    assert _normalize_experience("  ") == []
    assert _normalize_experience(None) == []
