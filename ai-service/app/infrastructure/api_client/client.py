import logging
import httpx
from app.config import BACKEND_SERVICE_URL, INTERNAL_API_KEY, BACKEND_QUERY_TIMEOUT
from app.schemas import BaselineFeature

logger = logging.getLogger(__name__)


class InternalApiClient:
    def __init__(self):
        self.base_url = BACKEND_SERVICE_URL
        self.headers = (
            {"X-Internal-API-Key": INTERNAL_API_KEY} if INTERNAL_API_KEY else {}
        )

    async def get_baseline_features_async(self) -> list[BaselineFeature]:
        try:
            url = f"{self.base_url}/api/internal/ai/baseline-features"
            async with httpx.AsyncClient(timeout=BACKEND_QUERY_TIMEOUT * 10) as client:
                response = await client.get(url, headers=self.headers)
                response.raise_for_status()
                data = response.json()
                if isinstance(data, dict) and "data" in data:
                    data = data["data"]
                if not isinstance(data, list):
                    logger.warning("Backend returned non-list for baseline features.")
                    return []
                logger.info(f"Fetched {len(data)} baseline records from backend.")
                return [BaselineFeature.model_validate(item) for item in data]
        except Exception as e:
            logger.error(f"Failed to fetch baseline features from backend: {e}")
            return []
