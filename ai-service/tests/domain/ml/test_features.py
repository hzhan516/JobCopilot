from typing import Any
from app.domain.ml.features import tokenize_text, normalize_items, extract_features, FEATURE_COLUMNS

def test_feature_columns():
    assert isinstance(FEATURE_COLUMNS, list)
    assert len(FEATURE_COLUMNS) == 8
    assert "semantic_match" in FEATURE_COLUMNS
    assert "skill_overlap_ratio" in FEATURE_COLUMNS
    assert "experience_overlap_ratio" in FEATURE_COLUMNS
    assert "title_keyword_overlap" in FEATURE_COLUMNS
    assert "query_title_similarity" in FEATURE_COLUMNS
    assert "years_of_experience_diff" in FEATURE_COLUMNS
    assert "location_match" in FEATURE_COLUMNS
    assert "salary_range_overlap" in FEATURE_COLUMNS

def test_tokenize_text_empty():
    assert tokenize_text("") == set()

def test_tokenize_text_basic():
    assert tokenize_text("Hello World") == {"hello", "world"}

def test_tokenize_text_short_words():
    assert tokenize_text("A an the cat") == {"the", "cat"}

def test_tokenize_text_punctuation():
    assert tokenize_text("Hello, world! This is a test.") == {"hello", "world", "this", "test"}

def test_tokenize_text_numbers():
    assert tokenize_text("Python 3.9 is great") == {"python", "great"}

def test_normalize_items_empty():
    assert normalize_items([]) == set()

def test_normalize_items_basic():
    assert normalize_items([" Python ", "JAVA", "c++"]) == {"python", "java", "c++"}

def test_normalize_items_empty_strings():
    assert normalize_items(["", "  ", "Python"]) == {"python"}

def test_extract_features_empty():
    job_details: dict[str, Any] = {}
    query = ""
    resume_text = ""
    features = extract_features(job_details, query, resume_text)
    
    assert features["semantic_match"] == 0.0
    assert features["skill_overlap_ratio"] == 0.0
    assert features["experience_overlap_ratio"] == 0.0
    assert features["title_keyword_overlap"] == 0.0
    assert features["query_title_similarity"] == 0.0
    assert features["years_of_experience_diff"] == 0.0
    assert features["location_match"] == 0.0
    assert features["salary_range_overlap"] == 0.0

def test_extract_features_basic():
    job_details: dict[str, Any] = {
        "title": "Software Engineer",
        "description": "Looking for a software engineer with Python experience.",
        "semanticMatch": 0.85
    }
    query = "software engineer"
    resume_text = "I am a software engineer with Python skills."
    
    features = extract_features(job_details, query, resume_text)
    
    assert features["semantic_match"] == 0.85
    
    # query_text = "software engineer I am a software engineer with Python skills."
    # query_tokens = {"software", "engineer", "with", "python", "skills"} (len 5)
    # title_tokens = {"software", "engineer"}
    # description_tokens = {"looking", "for", "software", "engineer", "with", "python", "experience"}
    
    # query_tokens & title_tokens = {"software", "engineer"} (len 2)
    # query_tokens & description_tokens = {"software", "engineer", "with", "python"} (len 4)
    
    assert features["skill_overlap_ratio"] == 2 / 5
    assert features["experience_overlap_ratio"] == 4 / 5
    assert features["title_keyword_overlap"] == 2.0

def test_extract_features_no_query_tokens():
    job_details: dict[str, Any] = {
        "title": "Software Engineer",
        "description": "Looking for a software engineer with Python experience.",
        "semanticMatch": 0.85
    }
    query = "a"
    resume_text = "an"
    
    features = extract_features(job_details, query, resume_text)
    
    assert features["semantic_match"] == 0.85
    assert features["skill_overlap_ratio"] == 0.0
    assert features["experience_overlap_ratio"] == 0.0
    assert features["title_keyword_overlap"] == 0.0
