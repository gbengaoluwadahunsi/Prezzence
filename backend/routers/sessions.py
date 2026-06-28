from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile
from models.schemas import SessionCreateRequest, SessionCreateResponse, Question, SessionPanelMember, AnswerSubmitRequest
from services.database import neon_db
from services.gemini import gemini
from services.web_research import web_research
from middleware.auth import get_current_user
from services.rate_limit import rate_limited
from typing import Dict
import traceback
import asyncio
import os
import base64
import time

router = APIRouter(prefix="/api/sessions", tags=["Sessions"])
print(">>> DEBUG: Sessions Router Loaded with /create and /{session_id}/answers <<<")

PREMIUM_INTERVIEW_TYPES = {"technical", "promotion"}
QUESTION_GENERATION_TIMEOUT_SECONDS = float(os.getenv("QUESTION_GENERATION_TIMEOUT_SECONDS", "3.5"))
SESSION_DB_TIMEOUT_SECONDS = float(os.getenv("SESSION_DB_TIMEOUT_SECONDS", "8"))
SESSION_META_TIMEOUT_SECONDS = float(os.getenv("SESSION_META_TIMEOUT_SECONDS", "4"))
SESSION_ANALYTICS_TIMEOUT_SECONDS = float(os.getenv("SESSION_ANALYTICS_TIMEOUT_SECONDS", "2"))
SESSION_WEB_RESEARCH_TIMEOUT_SECONDS = float(os.getenv("SESSION_WEB_RESEARCH_TIMEOUT_SECONDS", "6"))
from core.feature_flags import BETA_UNLOCK_ALL_FEATURES, FREE_SESSION_LIMIT
from services.entitlements import has_unlimited_access

DEFAULT_PERSONAS = {
    "maya": {"name": "Maya", "title": "People Lead", "personality": "friendly"},
    "jonas": {"name": "Jonas", "title": "Hiring Manager", "personality": "tough"},
    "amina": {"name": "Sophia", "title": "Domain Expert", "personality": "neutral"},
}


def _with_opening_introduction(questions: list, panel: list, difficulty: str) -> list:
    panel_people = [member.get("persona", {}) for member in panel if member.get("persona")]
    names = [person.get("name") for person in panel_people if person.get("name")]

    if len(names) > 1:
        introduction = f"Hi, we're {', '.join(names[:-1])} and {names[-1]}. We'll be your interview panel today."
        interviewer_name = names[0]
    elif names:
        person = panel_people[0]
        title = person.get("title")
        introduction = f"Hi, I'm {names[0]}{f', {title}' if title else ''}. I'll be leading your interview today."
        interviewer_name = names[0]
    else:
        introduction = "Hi, I'm your interviewer. I'll be leading your interview today."
        interviewer_name = "Interviewer"

    opening = {
        "number": 1,
        "text": (
            f"{introduction} Before we get into the role-specific questions, "
            "please introduce yourself and give me a quick overview of your background."
        ),
        "interviewer_name": interviewer_name,
        "type": "introduction",
        "difficulty": difficulty,
    }

    return [
        opening,
        *[
            {
                **question,
                "number": index + 2,
            }
            for index, question in enumerate(questions or [])
        ],
    ]


async def _track_event_background(payload: dict):
    try:
        await asyncio.wait_for(neon_db.track_event(payload), timeout=SESSION_ANALYTICS_TIMEOUT_SECONDS)
    except Exception as e:
        print(f"[Analytics] Background event skipped: {payload.get('name')} ({e})")


def _queue_event(payload: dict):
    try:
        asyncio.create_task(_track_event_background(payload))
    except RuntimeError:
        pass


def _format_resume_context(profile: dict | None) -> str:
    if not profile:
        return ""
    parts = []
    summary = str(profile.get("summary") or "").strip()
    if summary:
        parts.append(f"Candidate summary: {summary[:900]}")
    skills = profile.get("skills") or []
    if skills:
        parts.append(f"Candidate skills: {', '.join([str(skill) for skill in skills[:12]])}")
    experience = profile.get("experience") or []
    if experience:
        parts.append(f"Candidate experience signals: {'; '.join([str(item) for item in experience[:5]])}")
    education = profile.get("education") or []
    if education:
        parts.append(f"Candidate education/certifications: {'; '.join([str(item) for item in education[:3]])}")
    if not parts:
        return ""
    return (
        "\n".join(parts)
        + "\nUse this to make questions more relevant to the candidate's background. "
        "Do not mention private resume details unless they naturally support the interview question."
    )


def _generate_coaching_message(score: int, transcript: str, question: str) -> str:
    """
    Generate personalized coaching feedback based on answer quality score.
    This message is spoken by the avatar to guide the user through their answer improvement journey.
    """
    if score < 20:
        return "Let me help you refocus. Tell me one specific example where you handled this situation, and what was the result."
    elif score < 40:
        return "Good start! Now add one concrete detail—what exactly did you do, and what happened as a result?"
    elif score < 60:
        return "You're on the right track. Make it even stronger by explaining the situation, your specific action, and the result you achieved."
    elif score < 80:
        return "Solid answer! To make it even more impactful, consider adding a specific metric or quantifiable outcome."
    else:
        return "Excellent answer! You've got a clear structure with specific details. That's exactly what interviewers want to hear."


async def _submit_answer_payload(
    session_id: str,
    current_user: dict,
    *,
    question_id: int,
    question_text: str,
    audio_base64: str | None = None,
    transcript: str | None = None,
    transcript_source: str | None = None,
    audio_mime_type: str | None = None,
    audio_duration_seconds: int | None = None,
    initial_timings_ms: dict | None = None,
):
    total_started_at = time.perf_counter()
    timings_ms = dict(initial_timings_ms or {})
    session_lookup_started_at = time.perf_counter()
    session = await neon_db.get_session(session_id)
    timings_ms["session_lookup"] = round((time.perf_counter() - session_lookup_started_at) * 1000)
    if not session:
        raise HTTPException(status_code=404, detail="Session not found")
    if str(session["user_id"]) != str(current_user["id"]):
        raise HTTPException(status_code=403, detail="Forbidden")

    provided_transcript = str(transcript or "").strip()
    audio_payload = audio_base64 or ""
    has_transcript_signal = bool(provided_transcript)
    has_recorded_audio = (
        (audio_duration_seconds or 0) >= 1
        or len(audio_payload) >= 8000
    )

    if not has_transcript_signal and not has_recorded_audio:
        timings_ms["total"] = round((time.perf_counter() - total_started_at) * 1000)
        model_answer = gemini._build_model_answer(
            question_text,
            "",
            role_title=str(session.get("role_title") or ""),
        )
        return {
            "transcript": "",
            "retry_required": True,
            "performance": {
                "timings_ms": timings_ms,
                "transcript_source": transcript_source or "none",
                "analysis_source": "no_answer_signal",
            },
            "analysis": {
                "analysis_source": "no_answer_signal",
                "quality_label": "empty_transcript",
                "score": 0,
                "clarity_score": 0,
                "pacing_score": 0,
                "impact_score": 0,
                "confidence_score": 0,
                "knowledge_score": 0,
                "feedback": "We could not detect a clear answer. Please retry this question and speak close to the microphone.",
                "transcript": "",
                "improved_answer": model_answer,
            },
        }

    analysis_started_at = time.perf_counter()
    analysis = await gemini.analyze_answer(
        question_text=question_text,
        audio_base64=audio_payload or None,
        transcript=provided_transcript or None,
        audio_duration_seconds=audio_duration_seconds,
        audio_mime_type=audio_mime_type,
        role_title=str(session.get("role_title") or ""),
    )
    timings_ms["analysis"] = round((time.perf_counter() - analysis_started_at) * 1000)
    if analysis.get("analysis_source") == "analysis_unavailable":
        _queue_event({
            "user_id": str(current_user["id"]),
            "session_id": session_id,
            "name": "api_ai_provider_error",
            "properties": {
                "stage": "answer_analysis",
                "fallback": True,
                "analysis_source": analysis.get("analysis_source"),
                "quality_label": analysis.get("quality_label"),
                "audio_base64_chars": len(audio_payload),
                "audio_duration_seconds": audio_duration_seconds,
                "audio_mime_type": audio_mime_type,
                "transcript_chars": len(provided_transcript),
                "transcript_source": transcript_source,
                "timings_ms": timings_ms,
            },
        })
    _queue_event({
        "user_id": str(current_user["id"]),
        "session_id": session_id,
        "name": "api_answer_analyzed",
        "properties": {
            "question_id": question_id,
            "question_chars": len(question_text),
            "audio_base64_chars": len(audio_payload),
            "audio_duration_seconds": audio_duration_seconds,
            "audio_mime_type": audio_mime_type,
            "transcript_chars": len(provided_transcript),
            "transcript_source": transcript_source,
            "used_provided_transcript": has_transcript_signal,
            "score": analysis.get("score", 0),
            "analysis_source": analysis.get("analysis_source"),
            "quality_label": analysis.get("quality_label"),
            "timings_ms": timings_ms,
        },
    })

    transcript_text = str(analysis.get("transcript") or provided_transcript or "").strip()
    score = int(analysis.get("score", 0) or 0)
    unusable_transcript = (
        not transcript_text
        or transcript_text.lower().startswith("unable to transcribe")
        or transcript_text.startswith("[Audio received")
        or transcript_text.startswith("[Audio captured")
        or analysis.get("quality_label") == "empty_transcript"
        or analysis.get("analysis_source") == "analysis_unavailable"
    )

    if unusable_transcript and score <= 10:
        timings_ms["total"] = round((time.perf_counter() - total_started_at) * 1000)
        retry_feedback = (
            "We could not turn this recording into a clear answer. Please retry this question and speak close to the microphone."
            if has_recorded_audio
            else "We could not detect a clear answer. Please retry this question and speak close to the microphone."
        )
        gemini._ensure_answer_coaching(analysis, question_text)
        return {
            "transcript": "",
            "retry_required": True,
            "performance": {
                "timings_ms": timings_ms,
                "transcript_source": transcript_source or "backend",
                "analysis_source": analysis.get("analysis_source"),
            },
            "analysis": {
                **analysis,
                "score": 0,
                "clarity_score": 0,
                "pacing_score": 0,
                "impact_score": 0,
                "confidence_score": 0,
                "knowledge_score": 0,
                "feedback": retry_feedback,
                "transcript": "",
            },
        }

    answer_data = {
        "session_id": session_id,
        "question_id": question_id,
        "transcript": transcript_text,
        "score": analysis.get("score", 0),
        "clarity": analysis.get("clarity_score", 0),
        "pacing": analysis.get("pacing_score", 0),
        "impact": analysis.get("impact_score", 0),
        "confidence": analysis.get("confidence_score", 0),
        "knowledge": analysis.get("knowledge_score", 0),
        "feedback": analysis.get("feedback", ""),
        "tips": analysis.get("tips", []),
        "improved_answer": analysis.get("improved_answer", ""),
        "answer_structure": analysis.get("answer_structure", ""),
        "missing_evidence": analysis.get("missing_evidence", []),
        "stronger_phrasing": analysis.get("stronger_phrasing", []),
        "coaching_breakdown": analysis.get("coaching_breakdown", {}),
    }

    save_started_at = time.perf_counter()
    await neon_db.save_answer(answer_data)
    await neon_db.refresh_session_score(session_id)
    timings_ms["save_answer"] = round((time.perf_counter() - save_started_at) * 1000)
    timings_ms["total"] = round((time.perf_counter() - total_started_at) * 1000)

    return {
        "transcript": transcript_text,
        "performance": {
            "timings_ms": timings_ms,
            "transcript_source": transcript_source or ("provided" if has_transcript_signal else "backend"),
            "analysis_source": analysis.get("analysis_source"),
        },
        "analysis": {
            **analysis,
            "transcript": transcript_text,
            "coaching_message": _generate_coaching_message(
                score=int(analysis.get("score", 0) or 0),
                transcript=transcript_text,
                question=question_text,
            ),
        },
    }


@router.post("/coaching/model-answer", status_code=200)
async def coaching_model_answer(
    payload: dict,
    current_user: dict = Depends(rate_limited("model_answer")),
):
    question_text = str(payload.get("question_text") or "").strip()
    transcript = str(payload.get("transcript") or "").strip()
    role_title = str(payload.get("role_title") or "").strip()
    interviewer_name = str(payload.get("interviewer_name") or "").strip()
    interviewer_title = str(payload.get("interviewer_title") or "").strip()
    if not question_text:
        raise HTTPException(status_code=400, detail="question_text is required")
    coaching_transcript = transcript
    if transcript:
        quality = gemini._classify_answer_quality(question_text, transcript)
        if quality.get("label") != "valid_answer":
            coaching_transcript = ""
    improved_answer = gemini._build_model_answer(
        question_text,
        coaching_transcript,
        role_title=role_title,
        interviewer_name=interviewer_name,
        interviewer_title=interviewer_title,
    )
    breakdown = gemini._build_coaching_breakdown(question_text, coaching_transcript)
    return {
        "improved_answer": improved_answer,
        "coaching_breakdown": breakdown,
        "coaching_message": "Here is a STAR model answer you can adapt — focus on context, your decision, and the result.",
    }


@router.post("/coaching/learn-topic", status_code=200)
async def coaching_learn_topic(
    payload: dict,
    current_user: dict = Depends(rate_limited("model_answer")),
):
    """Return a short, focused lesson teaching the skill/topic behind a question."""
    question_text = str(payload.get("question_text") or "").strip()
    role_title = str(payload.get("role_title") or "").strip()
    if not question_text:
        raise HTTPException(status_code=400, detail="question_text is required")
    return gemini.build_topic_lesson(question_text, role_title=role_title)


@router.post("/{session_id}/answers", status_code=200)
async def submit_answer(
    session_id: str, 
    request: AnswerSubmitRequest, 
    current_user: dict = Depends(rate_limited("answer_analysis"))
):
    """
    Handles answer scoring from JSON audio/transcript payloads.
    """
    return await _submit_answer_payload(
        session_id,
        current_user,
        question_id=request.question_id,
        question_text=request.question_text,
        audio_base64=request.audio_base64,
        transcript=request.transcript,
        transcript_source=request.transcript_source,
        audio_duration_seconds=request.audio_duration_seconds,
        audio_mime_type=request.audio_mime_type,
    )


@router.post("/{session_id}/answers/upload", status_code=200)
async def submit_answer_upload(
    session_id: str,
    question_id: int = Form(...),
    question_text: str = Form(...),
    audio_duration_seconds: int | None = Form(None),
    transcript: str | None = Form(None),
    transcript_source: str | None = Form(None),
    audio: UploadFile = File(...),
    current_user: dict = Depends(rate_limited("answer_analysis"))
):
    """
    Handles recorded answer uploads without forcing the phone to base64-encode the file first.
    """
    read_started_at = time.perf_counter()
    audio_bytes = await audio.read()
    audio_read_ms = round((time.perf_counter() - read_started_at) * 1000)
    audio_mime_type = audio.content_type or "audio/m4a"
    encode_started_at = time.perf_counter()
    audio_base64 = f"data:{audio_mime_type};base64,{base64.b64encode(audio_bytes).decode('ascii')}"
    audio_encode_ms = round((time.perf_counter() - encode_started_at) * 1000)
    return await _submit_answer_payload(
        session_id,
        current_user,
        question_id=question_id,
        question_text=question_text,
        audio_base64=audio_base64,
        transcript=transcript,
        transcript_source=transcript_source,
        audio_mime_type=audio_mime_type,
        audio_duration_seconds=audio_duration_seconds,
        initial_timings_ms={
            "audio_read": audio_read_ms,
            "audio_encode": audio_encode_ms,
            "audio_bytes": len(audio_bytes),
        },
    )


@router.post("/create", response_model=SessionCreateResponse, status_code=201)
async def create_new_session(request: SessionCreateRequest, current_user: dict = Depends(rate_limited("session_create"))):
    """
    Creates a new interview session, generates questions via Gemini, and saves to Neon DB.
    """
    try:
        interview_type = (request.interview_type or "").strip().lower()
        admin_access = has_unlimited_access(current_user.get("email"))
        is_premium = admin_access
        if not is_premium:
            try:
                is_premium = await asyncio.wait_for(
                    neon_db.is_user_premium(str(current_user["id"]), current_user.get("email")),
                    timeout=SESSION_META_TIMEOUT_SECONDS,
                )
            except asyncio.TimeoutError:
                print("[Sessions] Premium lookup timed out; treating as free for this request.")
                is_premium = False
        if BETA_UNLOCK_ALL_FEATURES:
            is_premium = True

        if not is_premium and FREE_SESSION_LIMIT > 0:
            existing_sessions = await asyncio.wait_for(
                neon_db.get_user_sessions(str(current_user["id"]), limit=FREE_SESSION_LIMIT + 1),
                timeout=SESSION_META_TIMEOUT_SECONDS,
            )
            if len(existing_sessions) >= FREE_SESSION_LIMIT:
                raise HTTPException(
                    status_code=402,
                    detail={
                        "code": "premium_required",
                        "message": f"Free plan includes {FREE_SESSION_LIMIT} practice sessions. Upgrade for unlimited sessions."
                    }
                )
        if interview_type in PREMIUM_INTERVIEW_TYPES or request.enable_web_research:
            if not is_premium:
                raise HTTPException(
                    status_code=402,
                    detail={
                        "code": "premium_required",
                        "message": "This interview mode or web research requires a premium plan."
                    }
                )

        # 1. Fetch persona details for the panel descriptions
        try:
            personas_data = await asyncio.wait_for(neon_db.get_personas(), timeout=SESSION_META_TIMEOUT_SECONDS)
        except asyncio.TimeoutError:
            print("[Sessions] Persona lookup timed out; using default personas.")
            personas_data = []
        persona_map = {**DEFAULT_PERSONAS}
        if personas_data:
            persona_map.update({str(p["id"]): p for p in personas_data})
        
        panel_desc = []
        session_panel = []
        
        for cfg in request.panel_config:
            p = persona_map.get(cfg.persona_id, {"name": "Interviewer", "title": "", "personality": "neutral"})
            panel_desc.append(f"{p['name']} ({p['personality']} {p.get('title', '')})")
            session_panel.append({
                "seat": cfg.seat,
                "persona": {"id": cfg.persona_id, "name": p["name"], "title": p.get("title", ""), "personality": p["personality"]}
            })

        panel_desc_str = ", ".join(panel_desc)

        company_context = request.company_context or ""
        resume_context = ""
        try:
            resume_profile = await asyncio.wait_for(
                neon_db.get_resume_profile(str(current_user["id"])),
                timeout=SESSION_META_TIMEOUT_SECONDS,
            )
            resume_context = _format_resume_context(resume_profile)
            if resume_context:
                company_context = (
                    f"{company_context}\n\n{resume_context}"
                    if company_context
                    else resume_context
                )
        except asyncio.TimeoutError:
            print("[Sessions] Resume profile lookup timed out; continuing without resume context.")
        except Exception as resume_error:
            print(f"[Sessions] Resume profile lookup failed: {resume_error}")
        research = {"enabled": False, "summary": "", "sources": [], "provider": None}
        if request.enable_web_research:
            try:
                research = await asyncio.wait_for(
                    web_research.research_company(
                        company_name=request.company_name,
                        company_website=request.company_website,
                        role_title=request.role_title,
                        industry=request.industry,
                        user_context=company_context,
                    ),
                    timeout=SESSION_WEB_RESEARCH_TIMEOUT_SECONDS,
                )
                if research.get("summary"):
                    company_context = (
                        f"{research['summary']}\n\n{resume_context}"
                        if resume_context
                        else research["summary"]
                    )
            except Exception as research_error:
                print(f"[WebResearch] Company research failed: {research_error}")
                _queue_event({
                    "user_id": str(current_user["id"]),
                    "name": "api_web_research_error",
                    "properties": {
                        "company_name": request.company_name,
                        "company_website": request.company_website,
                        "error": str(research_error),
                    },
                })

        try:
            generated = await asyncio.wait_for(
                gemini.generate_questions(
                    role_title=request.role_title,
                    industry=request.industry,
                    seniority=request.seniority,
                    interview_type=request.interview_type,
                    difficulty=request.difficulty,
                    company_name=request.company_name,
                    company_website=request.company_website,
                    company_context=company_context or "General context",
                    panel_descriptions=panel_desc_str,
                    length=request.length,
                    include_technical=request.include_technical,
                    language=request.language
                ),
                timeout=QUESTION_GENERATION_TIMEOUT_SECONDS,
            )
        except asyncio.TimeoutError:
            print(f"[Sessions] Question generation timed out after {QUESTION_GENERATION_TIMEOUT_SECONDS}s; using fallback questions.")
            if request.length == "quick":
                question_count = 3
            elif request.length == "deep":
                question_count = 15
            else:
                question_count = 8
            generated = gemini._fallback_questions(
                question_count,
                request.role_title,
                request.industry,
                request.seniority,
                request.difficulty,
                request.company_name,
                request.language,
            )
        if generated.get("source") == "fallback":
            _queue_event({
                "user_id": str(current_user["id"]),
                "name": "api_ai_provider_error",
                "properties": {
                    "stage": "question_generation",
                    "fallback": True,
                    "language": request.language,
                    "interview_type": request.interview_type,
                },
            })

        generated["questions"] = _with_opening_introduction(
            generated.get("questions", []),
            session_panel,
            request.difficulty,
        )

        _queue_event({
            "user_id": str(current_user["id"]),
            "name": "api_questions_generated",
            "properties": {
                "language": request.language,
                "length": request.length,
                "difficulty": request.difficulty,
                "question_count": len(generated.get("questions", [])),
                "estimated_prompt_chars": len(panel_desc_str) + len(company_context or "") + len(request.role_title),
                "resume_context_used": bool(resume_context),
                "web_research_enabled": request.enable_web_research,
                "web_research_provider": research.get("provider"),
                "web_research_sources": len(research.get("sources", [])),
            },
        })

        # 3. Save Session to Neon DB
        session_data = {
            "user_id": current_user["id"],
            "role_title": request.role_title,
            "industry": request.industry,
            "seniority": request.seniority,
            "interview_type": request.interview_type,
            "difficulty": request.difficulty,
            "length": request.length,
            "include_technical": request.include_technical,
            "panel_config": [c.model_dump() for c in request.panel_config],
            "company_name": request.company_name,
            "company_website": request.company_website,
            "company_context": company_context or request.company_context,
            "language": request.language,
            "question_count": len(generated.get("questions", [])),
            "questions": generated.get("questions", []),
            "status": "in_progress"
        }
        
        try:
            saved_session = await asyncio.wait_for(
                neon_db.create_session(session_data),
                timeout=SESSION_DB_TIMEOUT_SECONDS,
            )
        except asyncio.TimeoutError:
            print(f"[Sessions] Session insert timed out after {SESSION_DB_TIMEOUT_SECONDS}s.")
            raise HTTPException(status_code=503, detail="Session service is warming up. Please try again.")
        
        return {
            "session_id": str(saved_session["id"]),
            "questions": generated["questions"],
            "panel": session_panel
        }
    except HTTPException:
        raise
    except Exception as e:
        print(f"[Sessions] Error creating session: {e}")
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))


@router.patch("/{session_id}/complete", status_code=200)
async def complete_session(
    session_id: str,
    current_user: dict = Depends(get_current_user)
):
    """
    Marks a session as completed after the last question is answered.
    """
    session = await neon_db.get_session(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="Session not found")
    if str(session["user_id"]) != str(current_user["id"]):
        raise HTTPException(status_code=403, detail="Forbidden")

    await neon_db.complete_session(session_id)
    return {"status": "completed", "session_id": session_id}


@router.get("/{session_id}")
async def get_session_detail(session_id: str, current_user: dict = Depends(get_current_user)):
    try:
        detail = await neon_db.get_session_detail(session_id)
        if not detail:
            raise HTTPException(status_code=404, detail="Session not found")
        
        # Security check: ensure session belongs to user
        if detail["user_id"] != str(current_user["id"]):
            raise HTTPException(status_code=403, detail="Forbidden")
            
        return detail
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error getting session detail: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/", status_code=200)
async def list_sessions(current_user: dict = Depends(get_current_user)):
    """
    Lists all sessions for the current user.
    """
    sessions = await neon_db.get_user_sessions_for_list(current_user["id"])
    return {"sessions": [
        {
            "id": str(s["id"]),
            "title": f"{s.get('role_title', 'Interview')} Assessment",
            "type": s.get("interview_type", "behavioral").capitalize(),
            "date": s["created_at"].strftime("%b %d, %Y") if s.get("created_at") else "Unknown",
            "score": int(s.get("computed_score", s.get("score", 0)) or 0),
            "answered": int(s.get("answered", 0) or 0),
            "total": int(s.get("total", 0) or 0),
            "status": s.get("status", ""),
        }
        for s in sessions
    ]}


@router.delete("/{session_id}", status_code=200)
async def delete_session(session_id: str, current_user: dict = Depends(get_current_user)):
    deleted = await neon_db.delete_user_session(str(current_user["id"]), session_id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Session not found")
    return {"status": "deleted", "session_id": session_id}

