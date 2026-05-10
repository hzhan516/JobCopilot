"""Redis client singleton for distributed state synchronization.

Provides a shared redis.Redis instance used by:
- IncrementalModelService (stats, dedup, model version)
- ModelCache (invalidation pub/sub)
- Startup sync lock (distributed embedding sync)

This module relies on Python 3.3+ implicit namespace packages;
no __init__.py is required.
"""

import os

import redis

_REDIS_HOST = os.getenv("REDIS_HOST", "redis")
_REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))
_REDIS_PASSWORD = os.getenv("REDIS_PASSWORD") or None

_redis_client: redis.Redis | None = None


def get_redis() -> redis.Redis:
    """Return the singleton Redis client (decode_responses=True)."""
    global _redis_client
    if _redis_client is None:
        _redis_client = redis.Redis(
            host=_REDIS_HOST,
            port=_REDIS_PORT,
            password=_REDIS_PASSWORD,
            decode_responses=True,
            socket_connect_timeout=5,
            socket_timeout=5,
            health_check_interval=30,
        )
    return _redis_client
