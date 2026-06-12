from fastapi import Request, HTTPException, Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from jose import jwt, JWTError
import httpx
import os
import json
import base64
import time
from dotenv import load_dotenv

load_dotenv()

# Security scheme
security = HTTPBearer()

JWT_SECRET = os.getenv("JWT_SECRET")
ALGORITHM = "HS256"
SUPABASE_URL = os.getenv("SUPABASE_URL", "").rstrip("/")
SUPABASE_ANON_KEY = os.getenv("SUPABASE_ANON_KEY")
SUPABASE_SERVICE_ROLE_KEY = os.getenv("SUPABASE_SERVICE_ROLE_KEY")
DEBUG = os.getenv("DEBUG", "").lower() == "true"
ALLOW_UNVERIFIED_JWT = os.getenv("ALLOW_UNVERIFIED_JWT", "").lower() == "true"
ENVIRONMENT = os.getenv("ENVIRONMENT", os.getenv("APP_ENV", "development")).lower()
IS_PRODUCTION = ENVIRONMENT in {"production", "prod"}
DEVICE_LIMIT_ENFORCED = os.getenv("DEVICE_LIMIT_ENFORCED", "false").lower() == "true"
DEVICE_LIMIT_HARD_FAIL = os.getenv("DEVICE_LIMIT_HARD_FAIL", "false").lower() == "true"
try:
    MAX_DEVICES_PER_USER = int(os.getenv("MAX_DEVICES_PER_USER", "2"))
except ValueError:
    MAX_DEVICES_PER_USER = 2

from services.database import neon_db

TOKEN_VALIDATION_CACHE: dict[str, dict] = {}
TOKEN_CACHE_TTL_SECONDS = int(os.getenv("TOKEN_CACHE_TTL_SECONDS", "300"))

print(
    f"[Auth] Device limit config enforced={DEVICE_LIMIT_ENFORCED} "
    f"hard_fail={DEVICE_LIMIT_HARD_FAIL} max_devices={MAX_DEVICES_PER_USER}"
)

if IS_PRODUCTION and ALLOW_UNVERIFIED_JWT:
    raise RuntimeError("ALLOW_UNVERIFIED_JWT must be false in production")

if IS_PRODUCTION and (
    (not JWT_SECRET or JWT_SECRET == "YOUR_JWT_SECRET")
    and not (SUPABASE_URL and (SUPABASE_ANON_KEY or SUPABASE_SERVICE_ROLE_KEY))
):
    raise RuntimeError("JWT_SECRET or Supabase Auth validation credentials must be set in production")


def _decode_jwt_payload(token: str) -> dict:
    """
    Manually decode the JWT payload without any signature or algorithm verification.
    This is safe for local development where Supabase handles auth on the frontend.
    """
    try:
        # JWT structure: header.payload.signature
        parts = token.split(".")
        if len(parts) != 3:
            raise ValueError("Invalid JWT format")
        
        # Decode the payload (second part)
        payload_b64 = parts[1]
        # Add padding if needed
        padding = 4 - len(payload_b64) % 4
        if padding != 4:
            payload_b64 += "=" * padding
        
        payload_bytes = base64.urlsafe_b64decode(payload_b64)
        return json.loads(payload_bytes)
    except Exception as e:
        raise JWTError(f"Failed to decode token: {str(e)}")


async def _validate_with_supabase_auth(token: str) -> dict:
    """
    Validate the access token against Supabase Auth. This covers Supabase projects
    that use asymmetric JWT algorithms instead of the legacy HS256 JWT secret.
    """
    api_key = SUPABASE_ANON_KEY or SUPABASE_SERVICE_ROLE_KEY
    if not SUPABASE_URL or not api_key:
        raise JWTError("Supabase Auth validation is not configured")

    now = time.time()
    cached = TOKEN_VALIDATION_CACHE.get(token)
    if cached and cached.get("expires_at", 0) > now:
        return cached["payload"]

    try:
        async with httpx.AsyncClient(timeout=10) as client:
            response = await client.get(
                f"{SUPABASE_URL}/auth/v1/user",
                headers={
                    "apikey": api_key,
                    "Authorization": f"Bearer {token}",
                },
            )
        if response.status_code != 200:
            raise JWTError(f"Supabase Auth rejected token with status {response.status_code}")

        user = response.json()
        payload = {
            "sub": user.get("id"),
            "email": user.get("email"),
            "role": user.get("role") or user.get("aud"),
        }
        try:
            token_payload = _decode_jwt_payload(token)
            token_exp = float(token_payload.get("exp") or 0)
        except Exception:
            token_exp = 0
        TOKEN_VALIDATION_CACHE[token] = {
            "payload": payload,
            "expires_at": min(token_exp, now + TOKEN_CACHE_TTL_SECONDS) if token_exp else now + TOKEN_CACHE_TTL_SECONDS,
        }
        return payload
    except httpx.HTTPError as e:
        raise JWTError(f"Supabase Auth validation failed: {str(e)}")


async def get_current_user(request: Request, auth: HTTPAuthorizationCredentials = Depends(security)):
    """
    Validates the Supabase JWT and verifies hardware telemetry quotas.
    """
    token = auth.credentials
    try:
        if JWT_SECRET and JWT_SECRET != "YOUR_JWT_SECRET":
            try:
                payload = jwt.decode(
                    token,
                    JWT_SECRET,
                    algorithms=[ALGORITHM],
                    options={"verify_aud": False},
                )
            except JWTError as local_error:
                if SUPABASE_URL and (SUPABASE_ANON_KEY or SUPABASE_SERVICE_ROLE_KEY):
                    payload = await _validate_with_supabase_auth(token)
                else:
                    raise local_error
        elif SUPABASE_URL and (SUPABASE_ANON_KEY or SUPABASE_SERVICE_ROLE_KEY):
            payload = await _validate_with_supabase_auth(token)
        elif not IS_PRODUCTION and (DEBUG or ALLOW_UNVERIFIED_JWT):
            print("WARNING: JWT signature verification is disabled. Set JWT_SECRET for production.")
            payload = _decode_jwt_payload(token)
        else:
            raise HTTPException(status_code=500, detail="JWT validation is not configured")

        user_id: str = payload.get("sub")
        if user_id is None:
            raise HTTPException(status_code=401, detail="Invalid authentication token")
            
        device_id = request.headers.get("x-device-identity")
        if device_id and DEVICE_LIMIT_ENFORCED:
            allowed = await neon_db.verify_device_allowance(
                user_id,
                device_id,
                max_devices=MAX_DEVICES_PER_USER,
            )
            if not allowed and DEVICE_LIMIT_HARD_FAIL:
                raise HTTPException(status_code=403, detail="Device limit reached. Sign out of another device.")
            if not allowed and not DEVICE_LIMIT_HARD_FAIL:
                print(f"[Auth] Device limit exceeded for user={user_id}, but hard-fail is disabled.")
        elif not device_id:
            print("Warning: Request missing X-Device-Identity header")
        return {
            "id": user_id,
            "email": payload.get("email"),
            "role": payload.get("role")
        }
    except JWTError as e:
        raise HTTPException(status_code=401, detail=f"Could not validate credentials: {str(e)}")

def require_auth(request: Request):
    # This can be used as a dependency in routes
    pass
