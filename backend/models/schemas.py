from pydantic import BaseModel, Field
from typing import List, Optional, Dict
from uuid import UUID

# --- Persona Schemas ---

class Persona(BaseModel):
    id: str
    name: str
    title: str
    personality: str
    backstory: str
    traits: List[str]
    is_default: bool = True
    is_premium: bool = False

class PersonaList(BaseModel):
    personas: List[Persona]

# --- Session Schemas ---

class PanelMemberConfig(BaseModel):
    persona_id: str
    seat: str # left, centre, right

class SessionCreateRequest(BaseModel):
    role_title: str
    industry: str
    seniority: str
    interview_type: str
    difficulty: str
    length: str
    panel_config: List[PanelMemberConfig]
    company_name: Optional[str] = None
    company_website: Optional[str] = None
    company_context: Optional[str] = None
    enable_web_research: bool = False
    include_technical: bool = True
    language: str = "en"

class Question(BaseModel):
    number: int
    text: str
    interviewer_name: str
    type: str
    difficulty: str

class SessionPanelMember(BaseModel):
    seat: str
    persona: Dict[str, str] # Minimal persona info

class SessionCreateResponse(BaseModel):
    session_id: str
    questions: List[Question]
    panel: List[SessionPanelMember]

# --- Error Schema ---

class ErrorDetail(BaseModel):
    code: str
    message: str
    detail: Optional[str] = None

class ErrorResponse(BaseModel):
    error: ErrorDetail

class AnswerSubmitRequest(BaseModel):
    question_id: int
    audio_base64: Optional[str] = None
    question_text: str
    transcript: Optional[str] = None
    transcript_source: Optional[str] = None
    audio_mime_type: Optional[str] = None
    audio_duration_seconds: Optional[int] = None
    audio_base64_chars: Optional[int] = None
