from fastapi import APIRouter, Depends, File, HTTPException, UploadFile
from pydantic import BaseModel
from datetime import date
import asyncio
import os
from services.database import neon_db
from services.resume_parser import build_resume_profile, extract_resume_text
from middleware.auth import get_current_user
from services.supabase import db as supabase_auth

router = APIRouter(prefix="/api/users", tags=["Users"])
PROGRESS_QUERY_TIMEOUT_SECONDS = float(os.getenv("PROGRESS_QUERY_TIMEOUT_SECONDS", "12"))


class PracticeGoalRequest(BaseModel):
    daily_minutes: int = 10
    interview_date: date | None = None
    target_role: str | None = None


class ResumeTextRequest(BaseModel):
    text: str
    file_name: str | None = "Pasted resume"


def _localized_coaching_tip(language: str, strongest: str, strongest_score: int, weakest: str, weakest_score: int) -> str:
    weakest_l = weakest.lower()
    strongest_l = strongest.lower()

    if strongest_score >= 70:
        return (
            f"Your strongest current area is '{strongest}' at {strongest_score}%, while '{weakest}' "
            f"at {weakest_score}% needs attention. Focus on improving your {weakest_l} "
            "in your next session."
        )

    if strongest_score >= 45:
        return (
            f"Your best current area is '{strongest}' at {strongest_score}%, but it still "
            f"needs practice. Focus next on '{weakest}' at {weakest_score}% to build a "
            "stronger interview baseline."
        )

    return (
        f"Your highest current signal is '{strongest}' at {strongest_score}%, so it is "
        f"not a strength yet. Start by improving your {weakest_l}, then build clearer "
        f"evidence for {strongest_l}."
    )

    templates = {
        "en": "Your strongest current area is '{strongest}' at {strongest_score}%, while '{weakest}' at {weakest_score}% needs attention. Focus on improving your {weakest_l} in your next session.",
        "es": "Tu '{strongest}' es fuerte con {strongest_score}%, pero '{weakest}' con {weakest_score}% necesita atención. Enfócate en mejorar {weakest_l} en tu próxima sesión.",
        "fr": "Votre compétence '{strongest}' est solide à {strongest_score}%, mais '{weakest}' à {weakest_score}% demande du travail. Concentrez-vous sur {weakest_l} lors de votre prochaine session.",
        "de": "'{strongest}' ist mit {strongest_score}% stark, aber '{weakest}' mit {weakest_score}% braucht Aufmerksamkeit. Arbeiten Sie in der nächsten Sitzung an {weakest_l}.",
        "it": "La tua area '{strongest}' è forte al {strongest_score}%, ma '{weakest}' al {weakest_score}% richiede attenzione. Concentrati su {weakest_l} nella prossima sessione.",
        "pt": "Sua área '{strongest}' está forte em {strongest_score}%, mas '{weakest}' em {weakest_score}% precisa de atenção. Foque em melhorar {weakest_l} na próxima sessão.",
        "zh": "你的“{strongest}”较强，为 {strongest_score}%，但“{weakest}”为 {weakest_score}%，需要加强。下一次请重点提升{weakest_l}。",
        "ja": "「{strongest}」は {strongest_score}% と強みですが、「{weakest}」は {weakest_score}% で改善が必要です。次回は{weakest_l}に集中しましょう。",
        "ko": "'{strongest}'은 {strongest_score}%로 강점이지만, '{weakest}'은 {weakest_score}%로 개선이 필요합니다. 다음 세션에서는 {weakest_l}에 집중하세요.",
        "ar": "مجال '{strongest}' قوي بنسبة {strongest_score}%، لكن '{weakest}' بنسبة {weakest_score}% يحتاج إلى اهتمام. ركز على تحسين {weakest_l} في جلستك القادمة.",
        "hi": "आपका '{strongest}' {strongest_score}% पर मजबूत है, लेकिन '{weakest}' {weakest_score}% पर ध्यान चाहता है। अगले सत्र में {weakest_l} सुधारने पर ध्यान दें।",
    }
    template = templates.get((language or "en").lower(), templates["en"])
    return template.format(
        strongest=strongest,
        strongest_score=strongest_score,
        weakest=weakest,
        weakest_score=weakest_score,
        weakest_l=weakest.lower(),
    )


def _empty_coaching_tip(language: str) -> str:
    return {
        "en": "Complete your first interview session to start receiving personalized coaching insights.",
        "es": "Completa tu primera entrevista para empezar a recibir coaching personalizado.",
        "fr": "Terminez votre première session d'entretien pour recevoir des conseils personnalisés.",
        "de": "Schließen Sie Ihre erste Interviewsitzung ab, um personalisierte Coaching-Hinweise zu erhalten.",
        "it": "Completa la tua prima sessione di colloquio per ricevere consigli personalizzati.",
        "pt": "Conclua sua primeira entrevista para receber insights personalizados de coaching.",
        "zh": "完成第一次面试后，你将开始收到个性化辅导建议。",
        "ja": "最初の面接セッションを完了すると、個別のコーチング提案が表示されます。",
        "ko": "첫 인터뷰 세션을 완료하면 개인화된 코칭 인사이트를 받을 수 있습니다.",
        "ar": "أكمل أول جلسة مقابلة للحصول على إرشادات تدريبية مخصصة.",
        "hi": "व्यक्तिगत कोचिंग सुझाव पाने के लिए अपना पहला इंटरव्यू सत्र पूरा करें।",
    }.get((language or "en").lower(), "Complete your first interview session to start receiving personalized coaching insights.")


@router.get("/me/entitlement", status_code=200)
async def get_entitlement(current_user: dict = Depends(get_current_user)):
    """Returns the server-authoritative entitlement status for the authenticated user."""
    try:
        is_premium = await asyncio.wait_for(
            neon_db.is_user_premium(str(current_user["id"]), current_user.get("email")),
            timeout=4.0,
        )
    except Exception:
        is_premium = False
    return {"is_premium": is_premium, "plan": "premium" if is_premium else "free"}


@router.get("/{user_id}/progress", status_code=200)
async def get_user_progress(user_id: str, language: str = "en", current_user: dict = Depends(get_current_user)):
    """
    Fetches aggregate score data for the user's progress dashboard.
    Uses real Neon DB data when available, falls back to defaults for new users.
    """
    if str(current_user["id"]) != str(user_id):
        raise HTTPException(status_code=403, detail="Cannot access another user's progress")

    try:
        progress = await asyncio.wait_for(
            neon_db.get_user_progress(user_id),
            timeout=PROGRESS_QUERY_TIMEOUT_SECONDS,
        )
    except asyncio.TimeoutError:
        print(f"[Progress] Timed out after {PROGRESS_QUERY_TIMEOUT_SECONDS}s for user={user_id}")
        progress = None
    except Exception as e:
        print(f"[Progress] Failed for user={user_id}: {e}")
        progress = None

    if progress:
        # Find the weakest dimension for coaching tip
        dimensions = {
            "Clarity": progress["clarity"],
            "Pacing": progress["pacing"],
            "Impact": progress["impact"],
            "Confidence": progress["confidence"],
            "Knowledge": progress["knowledge"],
        }
        weakest = min(dimensions, key=dimensions.get)
        strongest = max(dimensions, key=dimensions.get)

        coaching_tip = _localized_coaching_tip(language, strongest, dimensions[strongest], weakest, dimensions[weakest])

        return {
            "radar_data": [
                {"label": k, "value": v} for k, v in dimensions.items()
            ],
            "stats": {
                "streak": 0,
                "avg_score": progress["avg_score"],
                "sessions": progress["sessions_count"],
                "practice_hours": round(progress["answers_count"] * 2 / 60, 1)
            },
            "growth": progress.get("improvement", {}).get("from_previous", 0),
            "coaching_tip": coaching_tip,
            "recent_sessions": progress["recent_sessions"],
            "session_trend": progress.get("session_trend", []),
            "improvement": progress.get("improvement", {
                "first_score": progress["avg_score"],
                "latest_score": progress["avg_score"],
                "best_score": progress["avg_score"],
                "from_first": 0,
                "from_previous": 0,
                "is_best_session": False,
            }),
            "readiness": progress.get("readiness", {"label": "No Baseline", "score": progress["avg_score"]}),
            "skill_focus": progress.get("skill_focus", {
                "strongest": strongest.lower(),
                "strongest_score": dimensions[strongest],
                "weakest": weakest.lower(),
                "weakest_score": dimensions[weakest],
            }),
            "repeated_weaknesses": progress.get("repeated_weaknesses", []),
            "coaching_plan": progress.get("coaching_plan", []),
            "drills": _build_drills(progress.get("skill_focus", {}).get("weakest", weakest), progress.get("repeated_weaknesses", [])),
            "question_bank": _role_question_bank(),
        }

    # Fallback for new users with no data
    return {
        "radar_data": [
            {"label": "Clarity", "value": 0},
            {"label": "Pacing", "value": 0},
            {"label": "Impact", "value": 0},
            {"label": "Confidence", "value": 0},
            {"label": "Knowledge", "value": 0},
        ],
        "stats": {
            "streak": 0,
            "avg_score": 0,
            "sessions": 0,
            "practice_hours": 0
        },
        "growth": 0,
        "coaching_tip": _empty_coaching_tip(language),
        "recent_sessions": [],
        "session_trend": [],
        "improvement": {
            "first_score": 0,
            "latest_score": 0,
            "best_score": 0,
            "from_first": 0,
            "from_previous": 0,
            "is_best_session": False,
        },
        "readiness": {"label": "No Baseline", "score": 0},
        "skill_focus": {
            "strongest": "clarity",
            "strongest_score": 0,
            "weakest": "clarity",
            "weakest_score": 0,
        },
        "repeated_weaknesses": [],
        "coaching_plan": [],
        "drills": [],
        "question_bank": _role_question_bank(),
    }


def _build_drills(weakest: str, weaknesses: list[str]):
    focus = (weakest or "clarity").lower()
    drill_map = {
        "impact": [
            "Rewrite one answer with a number, business outcome, and before/after result.",
            "Answer one question using: problem, action, measurable impact.",
        ],
        "clarity": [
            "Give a 60-second STAR answer with one sentence per section.",
            "Record the same answer again and remove one unnecessary detail.",
        ],
        "knowledge": [
            "Explain one role-specific tradeoff and why your choice fits the company.",
            "Prepare two domain terms you can use naturally in your next answer.",
        ],
        "confidence": [
            "Start the next answer with the direct conclusion before context.",
            "Replace passive wording with ownership language: I led, I decided, I delivered.",
        ],
        "pacing": [
            "Answer in under 90 seconds with one example only.",
            "Pause after the result instead of adding extra context.",
        ],
    }
    drills = drill_map.get(focus, drill_map["clarity"])
    items = [
        {"title": f"{focus.title()} drill", "description": drills[0]},
        {"title": "Retry drill", "description": drills[1]},
    ]
    if weaknesses:
        items.append({"title": "Weakness memory", "description": weaknesses[0]})
    return items


def _role_question_bank():
    return [
        {"role": "Product Manager", "questions": ["Tell me about a product tradeoff you owned.", "How would you prioritize a roadmap with limited engineering capacity?"]},
        {"role": "Software Engineer", "questions": ["Describe a system you improved and the measurable result.", "How do you handle reliability versus speed of delivery?"]},
        {"role": "Finance Analyst", "questions": ["Walk me through a financial model decision you defended.", "How would you evaluate risk in a new market?"]},
        {"role": "Nursing", "questions": ["Tell me about a high-pressure patient-care decision.", "How do you communicate with a difficult patient or family?"]},
        {"role": "Law", "questions": ["Describe a case or argument where your reasoning changed the outcome.", "How do you manage client risk under ambiguity?"]},
        {"role": "Sales", "questions": ["Tell me about a deal you rescued.", "How do you qualify a prospect and handle objections?"]},
    ]


@router.get("/me/stats")
async def get_my_stats(user: dict = Depends(get_current_user)):
    progress = await neon_db.get_user_progress(str(user["id"]))
    if not progress:
        return {"avg_score": 0, "sessions": 0, "practice_hours": 0}
    return {
        "avg_score": progress["avg_score"],
        "sessions": progress["sessions_count"],
        "practice_hours": round(progress["answers_count"] * 2 / 60, 1),
    }


@router.get("/me/saved-answers")
async def get_saved_answers(user: dict = Depends(get_current_user)):
    rows = await neon_db.get_saved_answers(str(user["id"]))
    return {"answers": rows}


@router.get("/me/practice-goal")
async def get_practice_goal(user: dict = Depends(get_current_user)):
    return await neon_db.get_practice_goal(str(user["id"]))


@router.patch("/me/practice-goal")
async def update_practice_goal(goal: PracticeGoalRequest, user: dict = Depends(get_current_user)):
    return await neon_db.update_practice_goal(str(user["id"]), goal.model_dump())


@router.get("/me/resume-profile")
async def get_resume_profile(user: dict = Depends(get_current_user)):
    profile = await neon_db.get_resume_profile(str(user["id"]))
    return {"profile": profile}


@router.post("/me/resume-profile")
async def upload_resume_profile(
    file: UploadFile = File(...),
    user: dict = Depends(get_current_user),
):
    try:
        data = await file.read()
        text = extract_resume_text(file.filename or "resume", data)
        profile = build_resume_profile(text, file.filename or "resume", "file")
        saved = await neon_db.upsert_resume_profile(str(user["id"]), profile)
        await neon_db.track_event({
            "user_id": str(user["id"]),
            "name": "resume_profile_uploaded",
            "properties": {
                "file_name": file.filename,
                "chars": len(text),
                "skills": len(saved.get("skills", [])),
                "experience": len(saved.get("experience", [])),
            },
        })
        return {"profile": saved}
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc))
    except Exception as exc:
        print(f"[Resume] Upload failed: {exc}")
        raise HTTPException(status_code=500, detail="Unable to process resume")


@router.post("/me/resume-profile/text")
async def save_resume_text(request: ResumeTextRequest, user: dict = Depends(get_current_user)):
    try:
        text = (request.text or "").strip()
        if len(text) < 40:
            raise ValueError("Resume text is too short to personalize interview questions.")
        profile = build_resume_profile(text[:20000], request.file_name or "Pasted resume", "text")
        saved = await neon_db.upsert_resume_profile(str(user["id"]), profile)
        return {"profile": saved}
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc))
    except Exception as exc:
        print(f"[Resume] Text save failed: {exc}")
        raise HTTPException(status_code=500, detail="Unable to save resume profile")


@router.delete("/me/resume-profile")
async def delete_resume_profile(user: dict = Depends(get_current_user)):
    await neon_db.delete_resume_profile(str(user["id"]))
    return {"status": "deleted"}


@router.get("/me/notifications")
async def get_notifications(user: dict = Depends(get_current_user)):
    try:
        return await neon_db.get_user_notifications(str(user["id"]))
    except Exception as e:
        print(f"Error getting notifications: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/me/notifications/{notification_id}/read")
async def mark_notification_read(notification_id: str, user: dict = Depends(get_current_user)):
    try:
        await neon_db.mark_notification_read(notification_id, str(user["id"]))
        return {"status": "success"}
    except Exception as e:
        print(f"Error marking notification read: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.delete("/me/notifications/{notification_id}")
async def delete_notification(notification_id: str, user: dict = Depends(get_current_user)):
    try:
        deleted = await neon_db.delete_notification(notification_id, str(user["id"]))
        if not deleted:
            raise HTTPException(status_code=404, detail="Notification not found")
        return {"status": "success"}
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error deleting notification: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.delete("/me", status_code=200)
async def delete_my_account(user: dict = Depends(get_current_user)):
    """
    Permanently deletes the user's app data and removes the Supabase Auth account
    when the service-role key is configured.
    """
    user_id = str(user["id"])
    try:
        deleted_data = await neon_db.delete_user_data(user_id)
    except Exception as e:
        print(f"Error deleting app data for user {user_id}: {e}")
        raise HTTPException(status_code=500, detail="Unable to delete account data")

    auth_deleted = False
    auth_warning = None
    try:
        auth_deleted = supabase_auth.delete_auth_user(user_id)
    except Exception as e:
        auth_warning = str(e)
        print(f"Warning: app data deleted but Supabase Auth user deletion failed for {user_id}: {e}")

    return {
        "status": "deleted",
        "app_data": deleted_data,
        "auth_deleted": auth_deleted,
        "auth_warning": auth_warning,
    }
