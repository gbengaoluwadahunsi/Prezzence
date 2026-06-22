import os
from pathlib import Path

from fastapi import APIRouter, HTTPException, Request, Depends
from fastapi.responses import FileResponse, RedirectResponse
from middleware.auth import get_current_user

router = APIRouter(prefix="/api/duix", tags=["duix"])

MODEL_NAMES = ("gj_dh_res", "Sofia", "Oliver", "Lily")
DEFAULT_BASE_CONFIG_URL = (
    "https://github.com/duixcom/Duix-Mobile/releases/download/v1.0.0/gj_dh_res.zip"
)


@router.get("/models")
async def duix_models(request: Request, current_user: dict = Depends(get_current_user)):
    configured_base_url = os.getenv("DUIX_MODEL_BASE_URL", "").strip().rstrip("/")
    if configured_base_url:
        base_url = configured_base_url
    else:
        base_url = str(request.base_url).rstrip("/") + "/static/duix-models"

    return {
        "baseUrl": base_url,
        "models": [
            {"name": name, "url": f"{str(request.base_url).rstrip('/')}/api/duix/models/download/{name}.zip"}
            for name in MODEL_NAMES
        ],
    }


@router.get("/models/download/{zip_name}")
async def download_duix_model(zip_name: str, current_user: dict = Depends(get_current_user)):
    model_name = zip_name.removesuffix(".zip")
    if model_name not in MODEL_NAMES or zip_name != f"{model_name}.zip":
        raise HTTPException(status_code=404, detail="Unknown Duix model")

    model_path = Path(__file__).resolve().parent.parent / "static" / "duix-models" / zip_name
    if model_path.exists() and model_path.stat().st_size > 0:
        return FileResponse(
            model_path,
            media_type="application/zip",
            filename=zip_name,
        )

    if zip_name == "gj_dh_res.zip":
        base_config_url = os.getenv("DUIX_BASE_CONFIG_URL", "").strip() or DEFAULT_BASE_CONFIG_URL
        return RedirectResponse(base_config_url, status_code=307)

    configured_base_url = os.getenv("DUIX_MODEL_BASE_URL", "").strip().rstrip("/")
    if configured_base_url:
        return RedirectResponse(f"{configured_base_url}/{zip_name}", status_code=307)

    raise HTTPException(status_code=404, detail="Duix model file is not available")


@router.get("/models/{model_name}/status")
async def duix_model_status(model_name: str, current_user: dict = Depends(get_current_user)):
    if model_name not in MODEL_NAMES:
        raise HTTPException(status_code=404, detail="Unknown Duix model")

    model_path = Path(__file__).resolve().parent.parent / "static" / "duix-models" / f"{model_name}.zip"
    return {
        "name": model_name,
        "available": model_path.exists() and model_path.stat().st_size > 0,
        "sizeBytes": model_path.stat().st_size if model_path.exists() else 0,
    }
