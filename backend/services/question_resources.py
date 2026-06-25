"""Learn-more links and topic labels for interview questions."""

from __future__ import annotations

import re
from urllib.parse import quote_plus

STAR_GUIDE_URL = "https://en.wikipedia.org/wiki/Situation,_task,_action,_result"

_TOPIC_LINKS: list[tuple[tuple[str, ...], str, str]] = [
    (
        ("introduce yourself", "overview of your background", "tell me about yourself"),
        "Tell me about yourself",
        "https://www.indeed.com/career-advice/interviewing/how-to-answer-tell-me-about-yourself",
    ),
    (
        ("conflict", "disagreement", "difficult colleague", "pushback"),
        "Handling workplace conflict",
        "https://www.indeed.com/career-advice/interviewing/interview-question-tell-me-about-a-time-you-handled-a-conflict",
    ),
    (
        ("priorit", "urgent", "deadline", "multiple requests"),
        "Prioritization under pressure",
        "https://www.indeed.com/career-advice/interviewing/interview-question-how-do-you-prioritize-your-work",
    ),
    (
        ("lead", "managed a team", "led a team", "leadership"),
        "Leadership examples",
        "https://www.indeed.com/career-advice/interviewing/leadership-interview-questions",
    ),
    (
        ("mistake", "failure", "went wrong", "setback"),
        "Learning from failure",
        "https://www.indeed.com/career-advice/interviewing/interview-question-tell-me-about-a-time-you-made-a-mistake",
    ),
    (
        ("customer", "client", "frustrated", "complaint", "stakeholder"),
        "Customer & stakeholder handling",
        "https://www.indeed.com/career-advice/interviewing/customer-service-interview-questions",
    ),
    (
        ("confiden", "uncertain", "unclear", "ambig", "motivat", "reassur", "morale", "calm"),
        "Leading through uncertainty",
        "https://www.indeed.com/career-advice/interviewing/how-to-deal-with-ambiguity",
    ),
    (
        ("communicat", "explain", "present", "audience"),
        "Communicating clearly",
        "https://www.indeed.com/career-advice/interviewing/communication-interview-questions",
    ),
    (
        ("technical", "debug", "system design", "architecture", "tradeoff", "trade-off"),
        "Technical decision-making",
        "https://www.indeed.com/career-advice/interviewing/technical-interview-questions",
    ),
    (
        ("feedback", "criticism", "coaching"),
        "Receiving feedback",
        "https://www.indeed.com/career-advice/interviewing/interview-question-tell-me-about-a-time-you-received-feedback",
    ),
    (
        ("change", "adapt", "pivot", "unexpected"),
        "Adapting to change",
        "https://www.indeed.com/career-advice/interviewing/adaptability-interview-questions",
    ),
]


def _short_topic(question_text: str, question_type: str = "") -> str:
    text = (question_text or "").strip()
    if not text:
        return "Interview question prep"
    cleaned = re.sub(r"\s+", " ", text).strip(" ?.")
    if len(cleaned) <= 72:
        return cleaned
    return cleaned[:69].rstrip() + "..."


def build_question_learn_more(question_text: str, question_type: str = "", role_title: str = "") -> dict[str, str]:
    text = (question_text or "").lower()
    qtype = (question_type or "").lower()

    for keywords, topic, url in _TOPIC_LINKS:
        if any(keyword in text for keyword in keywords):
            return {"learn_more_topic": topic, "learn_more_url": url}

    if qtype in {"behavioral", "behavioural", "situational"} or "tell me about a time" in text:
        topic = _short_topic(question_text, question_type)
        query = quote_plus(f"{topic} STAR method behavioral interview")
        return {
            "learn_more_topic": topic,
            "learn_more_url": f"https://www.google.com/search?q={query}",
        }

    if qtype == "introduction" or "introduce" in text:
        return {
            "learn_more_topic": "Tell me about yourself",
            "learn_more_url": _TOPIC_LINKS[0][2],
        }

    topic = _short_topic(question_text, question_type)
    role = (role_title or "interview").strip()
    query = quote_plus(f"{topic} {role} interview guide")
    return {
        "learn_more_topic": topic,
        "learn_more_url": f"https://www.google.com/search?q={query}",
    }


def enrich_question(question: dict, role_title: str = "") -> dict:
    if not isinstance(question, dict):
        return question
    resources = build_question_learn_more(
        str(question.get("text") or ""),
        str(question.get("type") or ""),
        role_title,
    )
    question.update(resources)
    return question
