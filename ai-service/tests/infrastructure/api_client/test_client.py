import pytest
import respx
import httpx
from app.infrastructure.api_client.client import InternalApiClient
from app.config import BACKEND_SERVICE_URL

@pytest.fixture
def api_client() -> InternalApiClient:
    return InternalApiClient()

@respx.mock
def test_get_baseline_features_success(api_client: InternalApiClient):
    url = f"{BACKEND_SERVICE_URL}/api/internal/ai/baseline-features"
    mock_data = {"data": [{"feature": "test1"}, {"feature": "test2"}]}
    respx.get(url).mock(return_value=httpx.Response(200, json=mock_data))

    result = api_client.get_baseline_features()
    assert len(result) == 2
    assert result[0]["feature"] == "test1"

@respx.mock
def test_get_baseline_features_direct_list(api_client: InternalApiClient):
    url = f"{BACKEND_SERVICE_URL}/api/internal/ai/baseline-features"
    mock_data = [{"feature": "test1"}, {"feature": "test2"}]
    respx.get(url).mock(return_value=httpx.Response(200, json=mock_data))

    result = api_client.get_baseline_features()
    assert len(result) == 2
    assert result[0]["feature"] == "test1"

@respx.mock
def test_get_baseline_features_non_list(api_client: InternalApiClient):
    url = f"{BACKEND_SERVICE_URL}/api/internal/ai/baseline-features"
    mock_data = {"data": "not a list"}
    respx.get(url).mock(return_value=httpx.Response(200, json=mock_data))

    result = api_client.get_baseline_features()
    assert result == []

@respx.mock
def test_get_baseline_features_http_error(api_client: InternalApiClient):
    url = f"{BACKEND_SERVICE_URL}/api/internal/ai/baseline-features"
    respx.get(url).mock(return_value=httpx.Response(500))

    result = api_client.get_baseline_features()
    assert result == []

@respx.mock
def test_get_baseline_features_exception(api_client: InternalApiClient):
    url = f"{BACKEND_SERVICE_URL}/api/internal/ai/baseline-features"
    respx.get(url).mock(side_effect=httpx.RequestError("Network error", request=httpx.Request("GET", url)))

    result = api_client.get_baseline_features()
    assert result == []

@pytest.mark.asyncio
@respx.mock
async def test_get_baseline_features_async_success(api_client: InternalApiClient):
    url = f"{BACKEND_SERVICE_URL}/api/internal/ai/baseline-features"
    mock_data = {"data": [{"feature": "test1"}, {"feature": "test2"}]}
    respx.get(url).mock(return_value=httpx.Response(200, json=mock_data))

    result = await api_client.get_baseline_features_async()
    assert len(result) == 2
    assert result[0]["feature"] == "test1"

@pytest.mark.asyncio
@respx.mock
async def test_get_baseline_features_async_direct_list(api_client: InternalApiClient):
    url = f"{BACKEND_SERVICE_URL}/api/internal/ai/baseline-features"
    mock_data = [{"feature": "test1"}, {"feature": "test2"}]
    respx.get(url).mock(return_value=httpx.Response(200, json=mock_data))

    result = await api_client.get_baseline_features_async()
    assert len(result) == 2
    assert result[0]["feature"] == "test1"

@pytest.mark.asyncio
@respx.mock
async def test_get_baseline_features_async_non_list(api_client: InternalApiClient):
    url = f"{BACKEND_SERVICE_URL}/api/internal/ai/baseline-features"
    mock_data = {"data": "not a list"}
    respx.get(url).mock(return_value=httpx.Response(200, json=mock_data))

    result = await api_client.get_baseline_features_async()
    assert result == []

@pytest.mark.asyncio
@respx.mock
async def test_get_baseline_features_async_http_error(api_client: InternalApiClient):
    url = f"{BACKEND_SERVICE_URL}/api/internal/ai/baseline-features"
    respx.get(url).mock(return_value=httpx.Response(500))

    result = await api_client.get_baseline_features_async()
    assert result == []

@pytest.mark.asyncio
@respx.mock
async def test_get_baseline_features_async_exception(api_client: InternalApiClient):
    url = f"{BACKEND_SERVICE_URL}/api/internal/ai/baseline-features"
    respx.get(url).mock(side_effect=httpx.RequestError("Network error", request=httpx.Request("GET", url)))

    result = await api_client.get_baseline_features_async()
    assert result == []
