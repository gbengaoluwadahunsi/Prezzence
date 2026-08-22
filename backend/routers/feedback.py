from fastapi import APIRouter, Depends, HTTPException
from typing import Optional
from pydantic import BaseModel
from middleware.auth import get_current_user
from services.database import neon_db
from core.logging_config import get_logger

logger = get_logger("feedback")
router = APIRouter(prefix="/api/feedback", tags=["Feedback"])

class FeedbackRequest(BaseModel):
    session_id: Optional[str] = None
    rating: int
    comment: str

@router.post("", status_code=201)
async def submit_feedback(request: FeedbackRequest, user: dict = Depends(get_current_user)):
    try:
        feedback = await neon_db.create_feedback(
            user_id=str(user["id"]),
            session_id=request.session_id,
            rating=request.rating,
            comment=request.comment
        )
        return {"status": "success", "id": str(feedback["id"])}
    except Exception as e:
        logger.error("[Feedback] Error submitting feedback: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))
