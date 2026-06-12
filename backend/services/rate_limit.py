import os
import time
from collections import defaultdict, deque
from dataclasses import dataclass
from fastapi import Depends, HTTPException, Request
from middleware.auth import get_current_user

try:
    from upstash_redis.asyncio import Redis
except Exception:
    Redis = None


@dataclass
class LimitRule:
    max_requests: int
    window_seconds: int


class InMemoryRateLimiter:
    def __init__(self):
        self.requests = defaultdict(deque)
        self.allowed = defaultdict(int)
        self.blocked = defaultdict(int)

    async def check(self, key: str, rule: LimitRule):
        now = time.time()
        bucket = self.requests[key]

        while bucket and bucket[0] <= now - rule.window_seconds:
            bucket.popleft()

        if len(bucket) >= rule.max_requests:
            retry_after = max(1, int(rule.window_seconds - (now - bucket[0])))
            self.blocked[key.split(":", 1)[0]] += 1
            raise_rate_limited(retry_after)

        bucket.append(now)
        self.allowed[key.split(":", 1)[0]] += 1

    def stats(self):
        return {
            "backend": "memory",
            "allowed": dict(self.allowed),
            "blocked": dict(self.blocked),
        }


class RedisRateLimiter:
    def __init__(self, redis: Redis):
        self.redis = redis
        self.allowed = defaultdict(int)
        self.blocked = defaultdict(int)

    async def check(self, key: str, rule: LimitRule):
        redis_key = f"rate:{key}"
        count = await self.redis.incr(redis_key)
        if count == 1:
            await self.redis.expire(redis_key, rule.window_seconds)

        if count > rule.max_requests:
            ttl = await self.redis.ttl(redis_key)
            retry_after = ttl if isinstance(ttl, int) and ttl > 0 else rule.window_seconds
            self.blocked[key.split(":", 1)[0]] += 1
            raise_rate_limited(retry_after)
        self.allowed[key.split(":", 1)[0]] += 1

    def stats(self):
        return {
            "backend": "redis",
            "allowed": dict(self.allowed),
            "blocked": dict(self.blocked),
        }


def raise_rate_limited(retry_after: int):
    raise HTTPException(
        status_code=429,
        detail={
            "code": "RATE_LIMITED",
            "message": "Too many requests. Please try again shortly.",
            "retry_after_seconds": retry_after,
        },
    )


def create_rate_limiter():
    redis_url = os.getenv("UPSTASH_REDIS_REST_URL") or os.getenv("REDIS_URL")
    redis_token = os.getenv("UPSTASH_REDIS_REST_TOKEN") or os.getenv("REDIS_TOKEN")
    if Redis and redis_url and redis_token:
        return RedisRateLimiter(Redis(url=redis_url, token=redis_token))

    print("[RateLimit] Using in-memory rate limits. Configure Upstash Redis for multi-instance production.")
    return InMemoryRateLimiter()


rate_limiter = create_rate_limiter()

RULES = {
    "session_create": LimitRule(
        max_requests=int(os.getenv("RATE_LIMIT_SESSION_CREATE_MAX", "12")),
        window_seconds=int(os.getenv("RATE_LIMIT_SESSION_CREATE_WINDOW", "3600")),
    ),
    "answer_analysis": LimitRule(
        max_requests=int(os.getenv("RATE_LIMIT_ANSWER_ANALYSIS_MAX", "80")),
        window_seconds=int(os.getenv("RATE_LIMIT_ANSWER_ANALYSIS_WINDOW", "3600")),
    ),
    "tts": LimitRule(
        max_requests=int(os.getenv("RATE_LIMIT_TTS_MAX", "180")),
        window_seconds=int(os.getenv("RATE_LIMIT_TTS_WINDOW", "3600")),
    ),
    "analytics": LimitRule(
        max_requests=int(os.getenv("RATE_LIMIT_ANALYTICS_MAX", "600")),
        window_seconds=int(os.getenv("RATE_LIMIT_ANALYTICS_WINDOW", "3600")),
    ),
}


def rate_limited(action: str):
    async def dependency(current_user: dict = Depends(get_current_user)):
        user_id = str(current_user.get("id", "anonymous"))
        rule = RULES[action]
        await rate_limiter.check(f"{action}:{user_id}", rule)
        return current_user

    return dependency


def public_rate_limited(action: str):
    async def dependency(request: Request):
        device_id = request.headers.get("X-Device-Identity")
        client_ip = request.client.host if request.client else "unknown"
        key = device_id or client_ip
        rule = RULES[action]
        await rate_limiter.check(f"{action}:public:{key}", rule)

    return dependency
