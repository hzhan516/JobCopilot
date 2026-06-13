from app.schemas import ExperienceItem
from app.services.resume_parser import _normalize_skills, _normalize_experience


def test_normalize_skills():
    assert _normalize_skills([" Python ", "Java", "", "  "]) == ["Python", "Java"]
    assert _normalize_skills("Python") == ["Python"]
    assert _normalize_skills("  ") == []
    assert _normalize_skills(None) == []


def test_normalize_experience():
    assert _normalize_experience([{"title": "Engineer"}, "Wrote code", None]) == [
        ExperienceItem(title="Engineer"),
        ExperienceItem(summary="Wrote code"),
    ]
    assert _normalize_experience({"title": "Engineer"}) == [
        ExperienceItem(title="Engineer")
    ]
    assert _normalize_experience("Wrote code") == [
        ExperienceItem(summary="Wrote code")
    ]
    assert _normalize_experience("  ") == []
    assert _normalize_experience(None) == []
