import asyncio
from fastapi import FastAPI, Request
from fastapi.responses import HTMLResponse
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
from dotenv import load_dotenv
import os
import time
import traceback
from core.feature_flags import BETA_UNLOCK_ALL_FEATURES
from core.production_config import (
    assetlinks_fingerprints,
    assetlinks_package_names,
    is_production,
    production_status,
    production_warnings,
)
from routers import analytics, personas, sessions, tts, users, feedback, legal, duix, billing, integrity
from services.database import neon_db
from services.supabase import db as supabase_service
from services.tts import tts_service
from core.logging_config import setup_logging, get_logger
from fastapi.staticfiles import StaticFiles
from pathlib import Path

# Initialize Logging
setup_logging()
logger = get_logger("prezzence_api")

# Load environment variables
load_dotenv()

app = FastAPI(
    title="Prezzence API",
    description="Elite AI Interview Preparation Backend",
    version="1.0.0"
)

def _csv_env(name: str, default: str = "") -> list[str]:
    return [item.strip() for item in os.getenv(name, default).split(",") if item.strip()]


# CORS configuration. Credentialed browser requests cannot use "*" origins.
allowed_origins = _csv_env(
    "CORS_ALLOWED_ORIGINS",
    "http://localhost:19006,http://localhost:8081,http://localhost:3000,http://127.0.0.1:19006,http://127.0.0.1:8081,http://127.0.0.1:3000",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=allowed_origins,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"],
    allow_headers=["*"],
)

# Mount static folder
static_dir = Path(__file__).resolve().parent / "static"
app.mount("/static", StaticFiles(directory=static_dir), name="static")

# Request Logging Middleware
@app.middleware("http")
async def log_requests(request: Request, call_next):
    print(f"DEBUG: Incoming Request -> {request.method} {request.url.path}")
    start_time = time.time()
    try:
        response = await call_next(request)
    except Exception as exc:
        process_time = (time.time() - start_time) * 1000
        logger.error(
            f"RID={request.scope.get('root_id', 'N/A')} "
            f"METHOD={request.method} PATH={request.url.path} "
            f"STATUS=500 DURATION={process_time:.2f}ms ERROR={exc}"
        )
        try:
            await neon_db.track_event({
                "name": "api_unhandled_exception",
                "properties": {
                    "method": request.method,
                    "path": request.url.path,
                    "duration_ms": round(process_time, 2),
                    "error": str(exc)[:300],
                    "traceback": traceback.format_exc(limit=4)[:1200],
                },
            })
        except Exception:
            pass
        raise
    process_time = (time.time() - start_time) * 1000
    formatted_process_time = "{0:.2f}".format(process_time)
    
    logger.info(
        f"RID={request.scope.get('root_id', 'N/A')} "
        f"METHOD={request.method} PATH={request.url.path} "
        f"STATUS={response.status_code} "
        f"DURATION={formatted_process_time}ms"
    )
    if response.status_code >= 500:
        try:
            await neon_db.track_event({
                "name": "api_error",
                "properties": {
                    "method": request.method,
                    "path": request.url.path,
                    "status": response.status_code,
                    "duration_ms": round(process_time, 2),
                },
            })
        except Exception:
            pass
    return response

# Include routers (prefixes are now defined inside the router files)
app.include_router(personas.router)
app.include_router(sessions.router)
app.include_router(tts.router)
app.include_router(feedback.router)
app.include_router(users.router)
app.include_router(billing.router)
app.include_router(analytics.router)
app.include_router(legal.router)
app.include_router(duix.router)
app.include_router(integrity.router)

@app.get("/api/version")
async def api_version():
    """Returns current API version and deployment info."""
    return {
        "version": "1.0.1",
        "build": "2026-06-13-avatar-fix",
        "environment": os.getenv("ENVIRONMENT", "production"),
        "beta_unlock_all_features": BETA_UNLOCK_ALL_FEATURES,
        "features": [
            "coaching_messages",
            "backend_scoring",
            "avatar_models",
            "tts_synthesis"
        ]
    }

@app.get("/healthz")
async def healthz():
    return {
        "status": "ok",
        "service": "prezzence-api",
        "environment": os.getenv("ENVIRONMENT", "development"),
    }

@app.get("/auth/verified", response_class=HTMLResponse)
async def auth_verified():
    return """<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Prezzence Email Verified</title>
  <style>
    html, body {
      margin: 0;
      padding: 0;
      background: #05050a;
      color: #f6f5ff;
      font-family: Arial, Helvetica, sans-serif;
    }
    main {
      max-width: 520px;
      margin: 0 auto;
      padding: 72px 28px 36px;
      text-align: center;
    }
    .mark {
      width: 108px;
      height: 108px;
      margin: 0 auto 34px;
      display: block;
      border: 2px solid #12d69b;
      border-radius: 999px;
      background: rgba(18, 214, 155, 0.12);
      box-shadow: 0 0 42px rgba(18, 214, 155, 0.18);
      color: #12d69b;
      font-size: 46px;
      line-height: 1;
    }
    .mark span {
      display: block;
      padding-top: 28px;
    }
    h1 {
      margin: 0 0 18px;
      color: #ffffff;
      font-size: 44px;
      line-height: 1.04;
      letter-spacing: 0;
    }
    p {
      margin: 0 auto 30px;
      max-width: 440px;
      color: #aaa6b8;
      font-size: 20px;
      line-height: 1.38;
    }
    a {
      display: inline-block;
      padding: 0 34px;
      min-height: 60px;
      line-height: 60px;
      border-radius: 999px;
      background: #6c63ff;
      color: white;
      font-size: 18px;
      font-weight: 800;
      text-decoration: none;
      box-shadow: 0 14px 38px rgba(108, 99, 255, 0.28);
    }
    small {
      display: block;
      margin-top: 24px;
      color: #747086;
      font-size: 14px;
      line-height: 1.45;
    }
  </style>
  <script>
    window.setTimeout(function () {
      window.location.href = "prezzence://verify";
    }, 800);
  </script>
</head>
<body>
  <main>
    <div class="mark" aria-hidden="true"><span>&#10003;</span></div>
    <h1>Email Verified</h1>
    <p>Your Prezzence account is confirmed. Open the app and sign in with your email and password.</p>
    <a href="prezzence://verify">Open Prezzence</a>
    <small>If you are on a laptop, continue using Prezzence from the app on your phone.</small>
  </main>
</body>
</html>"""

@app.get("/.well-known/assetlinks.json")
async def android_assetlinks():
    # Fingerprints must be the Play App Signing SHA-256 (Play Console → App integrity → App signing),
    # not the upload key. Set ANDROID_APP_SHA256_FINGERPRINTS on Render before production rollout.
    package_names = assetlinks_package_names()
    fingerprints = assetlinks_fingerprints()
    return [
        {
            "relation": ["delegate_permission/common.handle_all_urls"],
            "target": {
                "namespace": "android_app",
                "package_name": package_name,
                "sha256_cert_fingerprints": fingerprints,
            },
        }
        for package_name in package_names
    ]

@app.get("/api/health/production")
async def production_health():
    """Non-secret checklist for Render / Play production configuration."""
    return production_status()

async def keep_supabase_alive():
    """Background task that pings Supabase every 10 minutes to prevent auto-pausing/sleeping."""
    while True:
        try:
            await asyncio.sleep(600)  # Ping every 10 minutes (600 seconds)
            if supabase_service.client:
                success = await asyncio.to_thread(supabase_service.ping)
                if success:
                    logger.info("[Supabase Keepalive] Successfully pinged Supabase (Keepalive Active)")
        except asyncio.CancelledError:
            break
        except Exception as e:
            logger.warning("[Supabase Keepalive] Ping error: %s", e)


@app.on_event("startup")
async def startup():
    print(">>> STARTING PREZZENCE API <<<")
    if is_production():
        for warning in production_warnings():
            logger.warning("[Production] %s", warning)
    print(">>> REGISTERED ROUTES:")
    for route in app.routes:
        route_name = getattr(route, 'name', None) or route.__class__.__name__
        print(f"  {getattr(route, 'path', 'N/A')} -> {route_name}")
    print(">>> END REGISTERED ROUTES <<<")
    await neon_db.connect()
    tts_service.ensure_storage_ready()
    asyncio.create_task(keep_supabase_alive())


@app.on_event("shutdown")
async def shutdown():
    await neon_db.disconnect()

@app.get("/")
async def root():
    return {
        "status": "online",
        "service": "Prezzence API"
    }

if __name__ == "__main__":
    port = int(os.getenv("PORT", 8000))
    uvicorn.run("main:app", host="0.0.0.0", port=port, reload=True)
