import json
import sys
from pathlib import Path

import httpx
import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from services.gemini import GeminiService


class FakeResponse:
    def __init__(self, payload=None, status_code=200):
        self._payload = payload or {}
        self.status_code = status_code

    def raise_for_status(self):
        if self.status_code >= 400:
            request = httpx.Request("POST", "https://api.groq.com")
            response = httpx.Response(self.status_code, request=request)
            raise httpx.HTTPStatusError("request failed", request=request, response=response)

    def json(self):
        return self._payload


class FakeGroqChatClient:
    def __init__(self, *args, **kwargs):
        pass

    async def __aenter__(self):
        return self

    async def __aexit__(self, exc_type, exc, tb):
        return False

    async def post(self, url, **kwargs):
        assert url == "https://api.groq.com/openai/v1/chat/completions"
        body = {
            "transcript": "I handled an angry customer by listening, confirming the issue, fixing the billing error, and following up the next day.",
            "score": 82,
            "clarity_score": 84,
            "pacing_score": 80,
            "impact_score": 78,
            "confidence_score": 83,
            "knowledge_score": 81,
            "feedback": "Clear customer-service answer with a concrete action path.",
            "follow_up": "What metric showed the customer experience improved?",
            "tips": ["Add a measurable result"],
            "improved_answer": "I handled an angry customer by listening first, confirming the issue, fixing the billing error, and following up the next day.",
            "answer_structure": "Situation -> Action -> Result",
            "missing_evidence": ["Measured customer result"],
            "stronger_phrasing": ["The measurable result was..."],
            "coaching_breakdown": {
                "what_to_include": "Add the result.",
                "how_to_structure": "Use situation, action, result.",
                "why_it_works": "It proves ownership.",
            },
            "quality_label": "valid_answer",
            "question_relevance_score": 90,
        }
        return FakeResponse({
            "choices": [
                {"message": {"content": json.dumps(body)}}
            ]
        })


@pytest.mark.asyncio
async def test_groq_provider_scores_only_after_real_transcript(monkeypatch):
    service = GeminiService()
    service.scoring_provider = "groq"
    service.groq_api_key = "test-groq-key"

    async def fake_transcribe(audio_base64, audio_mime_type=None):
        return "I handled an angry customer by listening, confirming the issue, fixing the billing error, and following up the next day."

    monkeypatch.setattr(service, "_transcribe_with_groq", fake_transcribe)
    monkeypatch.setattr("services.gemini.httpx.AsyncClient", FakeGroqChatClient)

    result = await service.analyze_answer(
        "Tell me about a time you helped a frustrated customer.",
        audio_base64="data:audio/m4a;base64,AAAA",
        audio_duration_seconds=35,
        audio_mime_type="audio/m4a",
    )

    assert result["analysis_source"] == "groq"
    assert result["score"] > 0
    assert "angry customer" in result["transcript"]


@pytest.mark.asyncio
async def test_groq_provider_uses_supplied_transcript_without_transcribing(monkeypatch):
    service = GeminiService()
    service.scoring_provider = "groq"
    service.groq_api_key = "test-groq-key"

    async def fail_if_transcribed(audio_base64, audio_mime_type=None):
        raise AssertionError("audio transcription should not run when transcript is supplied")

    monkeypatch.setattr(service, "_transcribe_with_groq", fail_if_transcribed)
    monkeypatch.setattr("services.gemini.httpx.AsyncClient", FakeGroqChatClient)

    result = await service.analyze_answer(
        "Tell me about a time you helped a frustrated customer.",
        audio_base64="data:audio/m4a;base64,AAAA",
        transcript="I handled an angry customer by listening, confirming the issue, fixing the billing error, and following up the next day.",
        audio_duration_seconds=35,
        audio_mime_type="audio/m4a",
    )

    assert result["analysis_source"] == "groq"
    assert result["score"] > 0
    assert "angry customer" in result["transcript"]


@pytest.mark.asyncio
async def test_groq_provider_does_not_create_provisional_score_without_transcript(monkeypatch):
    service = GeminiService()
    service.scoring_provider = "groq"
    service.groq_api_key = "test-groq-key"

    async def fake_transcribe(audio_base64, audio_mime_type=None):
        return ""

    monkeypatch.setattr(service, "_transcribe_with_groq", fake_transcribe)
    monkeypatch.setattr("services.gemini.httpx.AsyncClient", FakeGroqChatClient)

    result = await service.analyze_answer(
        "Tell me about a time you helped a frustrated customer.",
        audio_base64="data:audio/m4a;base64,AAAA",
        audio_duration_seconds=35,
        audio_mime_type="audio/m4a",
    )

    assert result["analysis_source"] == "analysis_unavailable"
    assert result["score"] == 0
    assert result["transcript"] == ""
    assert "provisional" not in result["feedback"].lower()


@pytest.mark.asyncio
async def test_groq_transcription_retries_m4a_as_mp4(monkeypatch):
    service = GeminiService()
    service.groq_api_key = "test-groq-key"
    seen_mime_types = []

    class FakeGroqTranscriptionClient:
        def __init__(self, *args, **kwargs):
            pass

        async def __aenter__(self):
            return self

        async def __aexit__(self, exc_type, exc, tb):
            return False

        async def post(self, url, **kwargs):
            _filename, _bytes, mime_type = kwargs["files"]["file"]
            seen_mime_types.append(mime_type)
            if mime_type == "audio/m4a":
                return FakeResponse(status_code=400)
            return FakeResponse({"text": "This is the real transcript."})

    monkeypatch.setattr("services.gemini.httpx.AsyncClient", FakeGroqTranscriptionClient)

    transcript = await service._transcribe_with_groq(
        "data:audio/m4a;base64,QUJDRA==",
        "audio/m4a",
    )

    assert transcript == "This is the real transcript."
    assert seen_mime_types == ["audio/m4a", "audio/mp4"]


def test_placeholder_improved_answer_is_replaced():
    service = GeminiService()
    analysis = {
        "transcript": "I helped a customer by listening and fixing the issue.",
        "improved_answer": "I worked on [specific project] and achieved [specific metric].",
        "coaching_breakdown": {
            "what_to_include": "Add the exact customer issue.",
            "how_to_structure": "Use situation, action, result.",
            "why_it_works": "It proves ownership.",
        },
    }

    service._ensure_answer_coaching(
        analysis,
        "Tell me about a time you helped a frustrated customer.",
    )

    assert "[specific" not in analysis["improved_answer"].lower()
    assert "[your " not in analysis["improved_answer"].lower()


def test_give_up_answer_cannot_receive_high_score():
    service = GeminiService()
    analysis = {
        "transcript": "I skipped this question because I do not know the answer.",
        "score": 82,
        "clarity_score": 80,
        "pacing_score": 80,
        "impact_score": 80,
        "confidence_score": 80,
        "knowledge_score": 80,
    }

    service._apply_answer_quality_guardrails(
        analysis,
        "Tell me about a time you handled a difficult customer.",
        analysis["transcript"],
        audio_duration_seconds=12,
    )

    assert analysis["quality_label"] == "gave_up"
    assert analysis["score"] <= 15
    assert analysis["impact_score"] <= 15


def test_generic_short_answer_is_capped_below_ready_score():
    service = GeminiService()
    analysis = {
        "transcript": "I am hardworking and I always try my best. I believe I can do this job well because I am focused and ready to learn.",
        "score": 74,
        "clarity_score": 74,
        "pacing_score": 74,
        "impact_score": 74,
        "confidence_score": 74,
        "knowledge_score": 74,
    }

    service._apply_answer_quality_guardrails(
        analysis,
        "Tell me about one real experience that prepared you for this customer support role.",
        analysis["transcript"],
        audio_duration_seconds=24,
    )

    assert analysis["score"] <= 58
    assert analysis["quality_label"] in {"generic_answer", "missing_result", "shallow_answer", "valid_answer"}


def test_short_off_topic_answer_is_capped_near_zero():
    service = GeminiService()
    analysis = {
        "transcript": "What I did is I practically walked right now. Thank you.",
        "score": 72,
        "clarity_score": 72,
        "pacing_score": 72,
        "impact_score": 72,
        "confidence_score": 72,
        "knowledge_score": 72,
    }

    service._apply_answer_quality_guardrails(
        analysis,
        "Can you describe a time you handled a difficult customer in a high-pressure situation?",
        analysis["transcript"],
        audio_duration_seconds=14,
    )

    assert analysis["quality_label"] == "likely_off_topic"
    assert analysis["score"] <= 10
    assert analysis["impact_score"] <= 10
