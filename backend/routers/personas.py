from fastapi import APIRouter, Depends
from services.database import neon_db
from middleware.auth import get_current_user

router = APIRouter(prefix="/api/personas", tags=["Personas"])


@router.get("/")
async def get_personas(current_user: dict = Depends(get_current_user)):
    """
    Returns all available interviewer personas from Neon DB.
    """
    personas = await neon_db.get_personas()

    # Convert UUID and array fields for JSON serialization
    result = []
    for p in personas:
        result.append({
            "id": str(p["id"]),
            "name": p["name"],
            "title": p.get("title", ""),
            "personality": p.get("personality", "neutral"),
            "backstory": p.get("backstory", ""),
            "traits": list(p.get("traits", [])),
            "avatar_url": p.get("avatar_url", ""),
            "glb_url": p.get("glb_url", ""),
            "is_default": p.get("is_default", True),
            "is_premium": p.get("is_premium", False),
        })

    return {"personas": result}
