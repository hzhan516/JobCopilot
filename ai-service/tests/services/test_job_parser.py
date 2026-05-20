from app.services import job_parser
from app.services.job_parser import (
    _build_job_content,
    _normalize_requirements,
    parse_job_from_image,
    parse_job_text,
)

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


def test_parse_job_text_prompt_allows_json_schema_braces(monkeypatch):
    def fake_generate_json(prompt: str):
        assert '"title": "string"' in prompt
        return {
            "title": "Backend Engineer",
            "company": "Acme",
            "description": "Build APIs.",
            "requirements": ["Java"],
        }

    monkeypatch.setattr(job_parser, "generate_json_from_text_prompt", fake_generate_json)

    content = parse_job_text("Backend Engineer at Acme. Build APIs with Java.")

    assert content.title == "Backend Engineer"
    assert content.company == "Acme"


def test_parse_job_from_image_prompt_allows_json_schema_braces(monkeypatch):
    def fake_load_image_bytes(image_url: str):
        assert image_url == "data:image/png;base64,abc"
        return b"image-bytes", "image/png"

    def fake_generate_json(prompt: str, image_bytes: bytes, mime_type: str):
        assert '"requirements": ["string", "string"]' in prompt
        assert image_bytes == b"image-bytes"
        assert mime_type == "image/png"
        return {
            "title": "Frontend Engineer",
            "company": "Acme",
            "description": "Build UI.",
            "requirements": ["React"],
        }

    monkeypatch.setattr(job_parser, "_load_image_bytes", fake_load_image_bytes)
    monkeypatch.setattr(job_parser, "generate_json_from_image_prompt", fake_generate_json)

    content = parse_job_from_image("data:image/png;base64,abc", "Frontend Engineer")

    assert content.title == "Frontend Engineer"
    assert content.company == "Acme"
