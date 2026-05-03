from app.services.job_parser import _normalize_requirements, _build_job_content

def test_normalize_requirements():
    assert _normalize_requirements([" Python ", "Java", "", "  "]) == ["Python", "Java"]
    assert _normalize_requirements("Python") == ["Python"]
    assert _normalize_requirements("  ") == []
    assert _normalize_requirements(None) == []

def test_build_job_content():
    data = {
        "title": " Software Engineer ",
        "company": " Google ",
        "description": " Write code. ",
        "requirements": ["Python", "Java"]
    }
    content = _build_job_content(data)
    assert content.title == "Software Engineer"
    assert content.company == "Google"
    assert content.description == "Write code."
    assert content.requirements == ["Python", "Java"]
