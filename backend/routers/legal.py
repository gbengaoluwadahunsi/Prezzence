from fastapi import APIRouter
from fastapi.responses import HTMLResponse
import os

router = APIRouter(tags=["legal"])

BASE_STYLE = """
  body { margin: 0; background: #0A0A0F; color: #F7F7FF; font-family: Arial, sans-serif; }
  main { max-width: 820px; margin: 0 auto; padding: 48px 20px 72px; }
  h1 { font-size: 40px; margin: 0 0 8px; }
  h2 { font-size: 22px; margin: 30px 0 10px; }
  p, li { color: #C7C7D8; font-size: 16px; line-height: 1.65; }
  .updated { color: #9B9BAA; font-weight: 700; margin-bottom: 28px; }
  a { color: #8E7DFF; }
"""


def page(title: str, body: str) -> HTMLResponse:
    return HTMLResponse(
        f"""<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>{title} | Prezzence</title>
  <style>{BASE_STYLE}</style>
</head>
<body>
  <main>{body}</main>
</body>
</html>"""
    )


@router.get("/privacy", response_class=HTMLResponse)
async def privacy_policy():
    support_email = os.getenv("SUPPORT_EMAIL", "support@prezzence.app")
    return page(
        "Privacy Policy",
        f"""
        <h1>Privacy Policy</h1>
        <p class="updated">Last updated: May 26, 2026</p>
        <p>Prezzence helps users practice interviews with generated questions, audio recording, transcription, scoring, and coaching feedback.</p>

        <h2>Information We Collect</h2>
        <ul>
          <li>Account information such as email address and basic profile details.</li>
          <li>Interview setup choices such as role, company, language, and practice mode.</li>
          <li>Audio answers you choose to record, transcripts, scores, feedback, and session history.</li>
          <li>Optional resume or CV content when you upload a document to personalize questions. Prezzence extracts a compact profile such as summary, skills, experience, and education; the original uploaded file is not retained as a stored document after parsing.</li>
          <li>Device, diagnostic, crash, and usage events needed to keep the app reliable.</li>
        </ul>

        <h2>How We Use Information</h2>
        <p>We use your data to provide interview practice, personalize questions, transcribe and score answers, generate coaching feedback, save progress, troubleshoot errors, prevent abuse, and improve app reliability.</p>

        <h2>Audio, Transcription, and AI Processing</h2>
        <p>When you answer a question, audio may be uploaded to speech and AI providers so Prezzence can transcribe your response, score it, and generate coaching feedback. Recording starts only when you choose to answer.</p>

        <h2>Resume and CV Uploads</h2>
        <p>Resume upload is optional. If you upload a PDF, DOCX, TXT, or Markdown resume, the file is sent to our backend so we can extract a compact practice profile. We store the extracted profile, including summary, skills, experience, education, filename, and a limited text excerpt, so future interview questions can better match your background. You can remove the stored resume profile from the app.</p>

        <h2>Third-Party Services</h2>
        <p>Prezzence may use Supabase for authentication, Neon for app data, Groq or Gemini for transcription and scoring, Edge TTS for generated voice, object storage for temporary generated audio files, and hosting providers for backend infrastructure.</p>

        <h2>Retention and Deletion</h2>
        <p>You can request account deletion from the app profile area. Deletion removes sessions, answers, stored resume profile, analytics tied to your account, devices, and entitlement records from Prezzence app storage, subject to legal, security, and fraud-prevention requirements.</p>

        <h2>Contact</h2>
        <p>For privacy requests, contact <a href="mailto:{support_email}">{support_email}</a>.</p>
        """,
    )


@router.get("/terms", response_class=HTMLResponse)
async def terms_of_service():
    return page(
        "Terms of Service",
        """
        <h1>Terms of Service</h1>
        <p class="updated">Last updated: May 26, 2026</p>
        <p>Prezzence provides interview practice, generated questions, voice-led practice, transcription, scoring, and coaching feedback.</p>

        <h2>No Hiring Guarantee</h2>
        <p>Prezzence does not guarantee job offers, admissions, hiring outcomes, or interview success. Scores and feedback are coaching signals only.</p>

        <h2>User Responsibilities</h2>
        <p>Use the app lawfully. Do not upload confidential employer, client, patient, financial, or regulated information unless you are authorized to do so.</p>

        <h2>AI Feedback</h2>
        <p>AI feedback may be incomplete or incorrect. Review important career decisions with human mentors, recruiters, or qualified advisors.</p>

        <h2>Payments</h2>
        <p>During beta, paid features may be unlocked for testing. If paid plans launch, Android purchases should be handled through Google Play Billing and subscription details must match the store listing.</p>

        <h2>Account Deletion</h2>
        <p>You can delete your account from the app profile area. Some records may be retained where required for legal, fraud-prevention, or security reasons.</p>
        """,
    )
