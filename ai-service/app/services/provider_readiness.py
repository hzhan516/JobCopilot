"""Cached provider readiness without exposing credentials or raw provider errors."""

from __future__ import annotations

import logging
import os
import threading
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path

import litellm

from app.config import (
    AI_PROVIDER_PROBE_ENABLED,
    AI_PROVIDER_PROBE_TIMEOUT_SECONDS,
    LLM_TEXT_MODEL,
    VERTEX_LOCATION,
    VERTEX_PROJECT_ID,
)

logger = logging.getLogger(__name__)

_PLACEHOLDERS = {
    "",
    "changeme",
    "change-me",
    "placeholder",
    "your-api-key",
    "your_api_key",
    "jobcopilot-ai-service",
}


def _now() -> str:
    return datetime.now(timezone.utc).isoformat()


def _is_real_value(value: str | None) -> bool:
    return bool(value and value.strip().lower() not in _PLACEHOLDERS)


def _provider_alias(model: str) -> str:
    return model.split("/", 1)[0].lower() if "/" in model else "unknown"


def _vertex_credentials_available() -> bool:
    credentials_path = os.getenv("GOOGLE_APPLICATION_CREDENTIALS")
    if credentials_path and Path(credentials_path).is_file():
        return True
    if _is_real_value(os.getenv("VERTEX_CREDENTIALS")):
        return True
    try:
        import google.auth

        google.auth.default()
        return True
    except Exception:
        return False


def validate_provider_configuration(
    model: str = LLM_TEXT_MODEL,
) -> tuple[bool, str | None]:
    provider = _provider_alias(model)
    if provider == "vertex_ai":
        if not _is_real_value(VERTEX_PROJECT_ID) or not _is_real_value(VERTEX_LOCATION):
            return False, "CONFIG_VERTEX_PROJECT"
        if not _vertex_credentials_available():
            return False, "CONFIG_VERTEX_CREDENTIALS"
        return True, None
    if provider == "gemini":
        return (
            (True, None)
            if _is_real_value(os.getenv("GEMINI_API_KEY"))
            else (False, "CONFIG_GEMINI_KEY")
        )
    if provider == "openai":
        return (
            (True, None)
            if _is_real_value(os.getenv("OPENAI_API_KEY"))
            else (False, "CONFIG_OPENAI_KEY")
        )
    if provider == "anthropic":
        return (
            (True, None)
            if _is_real_value(os.getenv("ANTHROPIC_API_KEY"))
            else (False, "CONFIG_ANTHROPIC_KEY")
        )
    if provider in {"mock", "ollama"}:
        return True, None
    return False, "CONFIG_UNSUPPORTED_PROVIDER"


def classify_provider_error(exc: Exception) -> str:
    if isinstance(exc, litellm.exceptions.AuthenticationError):
        return "AUTH"
    if isinstance(exc, litellm.exceptions.RateLimitError):
        return "RATE_LIMIT"
    if isinstance(exc, litellm.exceptions.Timeout):
        return "TIMEOUT"
    if isinstance(exc, litellm.exceptions.NotFoundError):
        return "MODEL_NOT_FOUND"
    if isinstance(exc, litellm.exceptions.APIConnectionError):
        return "CONNECTION"
    return "PROVIDER_UNAVAILABLE"


@dataclass(frozen=True)
class ProviderSnapshot:
    provider: str
    modelAlias: str
    configured: bool
    probeEnabled: bool
    ready: bool
    lastProbeAt: str | None
    lastSuccessAt: str | None
    errorCode: str | None


class ProviderReadiness:
    def __init__(self) -> None:
        configured, error_code = validate_provider_configuration()
        self._lock = threading.Lock()
        self._snapshot = ProviderSnapshot(
            provider=_provider_alias(LLM_TEXT_MODEL),
            modelAlias=LLM_TEXT_MODEL,
            configured=configured,
            probeEnabled=AI_PROVIDER_PROBE_ENABLED,
            ready=configured and not AI_PROVIDER_PROBE_ENABLED,
            lastProbeAt=None,
            lastSuccessAt=None,
            errorCode=error_code,
        )

    def snapshot(self) -> ProviderSnapshot:
        with self._lock:
            return self._snapshot

    def public_status(self) -> dict[str, object]:
        return asdict(self.snapshot())

    def record_result(
        self, *, success: bool, error_code: str | None = None, probed: bool = False
    ) -> None:
        now = _now()
        with self._lock:
            current = self._snapshot
            self._snapshot = ProviderSnapshot(
                provider=current.provider,
                modelAlias=current.modelAlias,
                configured=current.configured,
                probeEnabled=current.probeEnabled,
                ready=current.configured and success,
                lastProbeAt=now if probed else current.lastProbeAt,
                lastSuccessAt=now if success else current.lastSuccessAt,
                errorCode=None if success else (error_code or "PROVIDER_UNAVAILABLE"),
            )

    def probe(self) -> ProviderSnapshot:
        current = self.snapshot()
        if not current.configured:
            self.record_result(success=False, error_code=current.errorCode, probed=True)
            return self.snapshot()
        if not current.probeEnabled:
            return current
        try:
            litellm.completion(
                model=LLM_TEXT_MODEL,
                messages=[{"role": "user", "content": "Reply with OK."}],
                max_tokens=2,
                temperature=0,
                timeout=AI_PROVIDER_PROBE_TIMEOUT_SECONDS,
            )
            self.record_result(success=True, probed=True)
        except Exception as exc:
            error_code = classify_provider_error(exc)
            logger.warning(
                "Provider readiness probe failed: provider=%s error_code=%s",
                current.provider,
                error_code,
            )
            self.record_result(success=False, error_code=error_code, probed=True)
        return self.snapshot()


provider_readiness = ProviderReadiness()
