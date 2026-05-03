from app.services.job_rank_service import _tokenize, _clip_score

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
    assert "a" not in tokens # length < 2

def test_clip_score():
    assert _clip_score(-1.0) == 0.0
    assert _clip_score(0.0) == 0.0
    assert _clip_score(0.5) == 0.5
    assert _clip_score(1.0) == 1.0
    assert _clip_score(2.0) == 1.0
