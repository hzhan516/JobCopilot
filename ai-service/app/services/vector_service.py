import litellm
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type

from app.config import (
    LLM_EMBEDDING_MODEL,
    LLM_EMBEDDING_MODEL_DIMENSION,
    LLM_REQUEST_TIMEOUT_SECONDS,
)

EMBEDDING_MODEL_CAPABILITIES = {
    "gemini/gemini-embedding-001": {"supports_dimensions": False},
    "gemini/gemini-embedding-2": {"supports_dimensions": False},
    "text-embedding-3-small": {"supports_dimensions": True},
    "text-embedding-3-large": {"supports_dimensions": True},
}


# Exponential backoff for transient embedding API failures.
# 指数退避重试：embedding 服务同样可能遇到限流或网络抖动，复用与 LLM 相同的重试策略。
RETRY_STRATEGY = retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential(multiplier=1, min=2, max=10),
    retry=retry_if_exception_type((
        litellm.exceptions.RateLimitError,
        litellm.exceptions.APIConnectionError,
        litellm.exceptions.Timeout
    ))
)


def _model_supports_dimensions(model: str) -> bool:
    normalized = model.strip().lower()
    capability = EMBEDDING_MODEL_CAPABILITIES.get(normalized)
    if capability is not None:
        return bool(capability["supports_dimensions"])

    if "text-embedding-3" in normalized:
        return True

    # Be conservative for unknown providers. Passing unsupported parameters
    # turns recoverable vector generation into a hard 502 for the backend.
    return False


@RETRY_STRATEGY
def generate_embedding(text: str) -> list[float]:
    """Generate an embedding vector with dimension validation to catch model config mismatches early.
    生成 embedding 向量：调用完成后校验输出维度是否与配置一致，
    在模型版本切换或维度配置错误时尽早暴露问题，避免脏数据写入向量库。"""
    cleaned_text = text.strip()

    if not cleaned_text:
        raise ValueError("Input text for vector generation is empty.")

    # dimensions=LLM_EMBEDDING_MODEL_DIMENSION,  # Remove this for gemini-embedding-001/textembedding-gecko compatibility
    # https://docs.litellm.ai/docs/providers/vertex_embedding
    # LiteLLM supports 'dimensions' for Vertex models that support the `output_dimensionality` parameter.
    # Google's `text-embedding-004` and `text-multilingual-embedding-002` support this, as does the latest 
    # `gemini-embedding-001` (from the gemini SDK, replacing the older text-embedding-gecko models).
    # OpenAI's `text-embedding-3-*` models also support the `dimensions` parameter.
    kwargs = {
        "model": LLM_EMBEDDING_MODEL,
        "input": [cleaned_text],
        "timeout": LLM_REQUEST_TIMEOUT_SECONDS,
    }
    
    if _model_supports_dimensions(LLM_EMBEDDING_MODEL):
        kwargs["dimensions"] = LLM_EMBEDDING_MODEL_DIMENSION

    response = litellm.embedding(**kwargs)

    if not response.data:
        raise ValueError("LiteLLM returned no embeddings.")

    emb_item = response.data[0]
    emb = emb_item["embedding"] if isinstance(emb_item, dict) else emb_item.embedding

    if not emb:
        raise ValueError("LiteLLM returned no embeddings.")

    if len(emb) != LLM_EMBEDDING_MODEL_DIMENSION:
        raise ValueError(
            "Embedding dimension mismatch: "
            f"expected {LLM_EMBEDDING_MODEL_DIMENSION}, got {len(emb)}"
        )

    return emb
