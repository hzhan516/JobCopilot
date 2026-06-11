import pytest
from app.schemas import EmbeddingRequest
from app.config import EMBEDDING_MAX_BATCH_SIZE, EMBEDDING_MAX_TEXT_LENGTH


def test_embedding_request_valid_batch():
    """Should accept batch at exact limit / 应接受等于限制的 batch"""
    texts = ["text"] * EMBEDDING_MAX_BATCH_SIZE
    req = EmbeddingRequest(texts=texts)
    assert len(req.texts) == EMBEDDING_MAX_BATCH_SIZE


def test_embedding_request_batch_size_exceeded():
    """Should reject batch exceeding max size / 应拒绝超出最大 batch size 的请求"""
    texts = ["text"] * (EMBEDDING_MAX_BATCH_SIZE + 1)
    with pytest.raises(
        ValueError, match=f"Batch size exceeds maximum of {EMBEDDING_MAX_BATCH_SIZE}"
    ):
        EmbeddingRequest(texts=texts)


def test_embedding_request_single_text_length_exceeded():
    """Should reject text exceeding max length / 应拒绝超出最大长度的文本"""
    texts = ["a" * (EMBEDDING_MAX_TEXT_LENGTH + 1)]
    with pytest.raises(
        ValueError,
        match=f"Text at index 0 exceeds maximum length of {EMBEDDING_MAX_TEXT_LENGTH} characters",
    ):
        EmbeddingRequest(texts=texts)


def test_embedding_request_text_at_exact_length_limit():
    """Should accept text at exact length limit / 应接受等于长度限制的文本"""
    texts = ["a" * EMBEDDING_MAX_TEXT_LENGTH]
    req = EmbeddingRequest(texts=texts)
    assert len(req.texts[0]) == EMBEDDING_MAX_TEXT_LENGTH


def test_embedding_request_multiple_texts_one_exceeded():
    """Should reject when any text exceeds limit / 当任一文本超出限制时应拒绝"""
    texts = ["short", "a" * (EMBEDDING_MAX_TEXT_LENGTH + 1), "also short"]
    with pytest.raises(ValueError, match="Text at index 1 exceeds maximum length"):
        EmbeddingRequest(texts=texts)


def test_embedding_request_empty_list():
    """Should accept empty text list / 应接受空文本列表"""
    req = EmbeddingRequest(texts=[])
    assert req.texts == []


def test_embedding_request_no_texts_field():
    """Should default to empty list when texts omitted / 省略 texts 时应默认为空列表"""
    req = EmbeddingRequest()
    assert req.texts == []


def test_embedding_request_model_field():
    """Should accept model parameter / 应接受 model 参数"""
    req = EmbeddingRequest(texts=["hello"], model="custom-model")
    assert req.model == "custom-model"


def test_embedding_request_boundary_batch_size_minus_one():
    """Should accept batch size one below limit / 应接受比限制小 1 的 batch size"""
    texts = ["t"] * (EMBEDDING_MAX_BATCH_SIZE - 1)
    req = EmbeddingRequest(texts=texts)
    assert len(req.texts) == EMBEDDING_MAX_BATCH_SIZE - 1
