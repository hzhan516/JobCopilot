from unittest.mock import MagicMock, patch

from app.services.provider_readiness import (
    ProviderReadiness,
    validate_provider_configuration,
)


def test_provider_configuration_requires_prefix_specific_key(monkeypatch):
    monkeypatch.delenv("GEMINI_API_KEY", raising=False)
    assert validate_provider_configuration("gemini/gemini-2.5-flash") == (
        False,
        "CONFIG_GEMINI_KEY",
    )

    monkeypatch.setenv("GEMINI_API_KEY", "real-test-key")
    assert validate_provider_configuration("gemini/gemini-2.5-flash") == (True, None)


def test_provider_configuration_rejects_unknown_provider():
    assert validate_provider_configuration("unknown/model") == (
        False,
        "CONFIG_UNSUPPORTED_PROVIDER",
    )


@patch(
    "app.services.provider_readiness.validate_provider_configuration",
    return_value=(True, None),
)
@patch("app.services.provider_readiness.litellm.completion")
def test_probe_caches_success_without_exposing_credentials(
    mock_completion, _mock_validate
):
    mock_completion.return_value = MagicMock()
    readiness = ProviderReadiness()

    snapshot = readiness.probe()
    public = readiness.public_status()

    assert snapshot.ready is True
    assert snapshot.lastProbeAt is not None
    assert snapshot.lastSuccessAt is not None
    assert public["errorCode"] is None
    assert all("key" not in name.lower() for name in public)
