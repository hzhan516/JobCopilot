import json
import logging
from typing import Any
import redis.asyncio as redis
from app.config import REDIS_HOST, REDIS_PORT, REDIS_PASSWORD, REDIS_KEY_PREFIX

logger = logging.getLogger(__name__)

def get_redis_client() -> redis.Redis:
    return redis.Redis(
        host=REDIS_HOST,
        port=REDIS_PORT,
        password=REDIS_PASSWORD or None,
        decode_responses=True
    )

class RedisBuffer:
    def __init__(self):
        self.redis = get_redis_client()
        self.buffer_key = REDIS_KEY_PREFIX + "feedback:buffer"
        self.lock_key = REDIS_KEY_PREFIX + "model:retrain:lock"

    async def append(self, item: dict[str, Any]):
        await self.redis.lpush(self.buffer_key, json.dumps(item, ensure_ascii=False))

    async def drain(self) -> list[dict[str, Any]]:
        pipe = self.redis.pipeline()
        pipe.lrange(self.buffer_key, 0, -1)
        pipe.delete(self.buffer_key)
        results, _ = await pipe.execute()
        
        samples = []
        for raw in results:
            try:
                samples.append(json.loads(raw))
            except json.JSONDecodeError:
                pass
        return samples

    async def acquire_lock(self, instance_id: str, ttl: int = 3600) -> bool:
        return await self.redis.set(self.lock_key, instance_id, nx=True, ex=ttl)

    async def release_lock(self, instance_id: str):
        lua_script = """
        if redis.call("get", KEYS[1]) == ARGV[1] then
            return redis.call("del", KEYS[1])
        else
            return 0
        end
        """
        await self.redis.eval(lua_script, 1, self.lock_key, instance_id)

    async def broadcast_reload(self, version: str, object_key: str):
        payload = {"version": version, "object_key": object_key}
        await self.redis.publish("ai.model.reload", json.dumps(payload))

    async def close(self):
        await self.redis.close()
