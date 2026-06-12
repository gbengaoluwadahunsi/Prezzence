"""
Resume parsing helpers.

The app only keeps a compact profile summary for interview personalization.
Raw resume files are parsed in-memory and discarded after upload.
"""
from __future__ import annotations

import re
import zipfile
from io import BytesIO
from pathlib import Path
from typing import Any, Dict, List
from xml.etree import ElementTree


MAX_RESUME_BYTES = 5 * 1024 * 1024
MAX_TEXT_CHARS = 20000
SUPPORTED_EXTENSIONS = {".txt", ".md", ".docx", ".pdf"}


def _decode_text(data: bytes) -> str:
    for encoding in ("utf-8", "utf-16", "latin-1"):
        try:
            return data.decode(encoding)
        except UnicodeDecodeError:
            continue
    return data.decode("utf-8", errors="ignore")


def _clean_text(text: str) -> str:
    text = text.replace("\x00", " ")
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()[:MAX_TEXT_CHARS]


def _extract_docx(data: bytes) -> str:
    try:
        with zipfile.ZipFile(BytesIO(data)) as archive:
            xml = archive.read("word/document.xml")
    except (KeyError, zipfile.BadZipFile) as exc:
        raise ValueError("This DOCX file could not be read. Please upload a valid DOCX, PDF, or TXT resume.") from exc

    root = ElementTree.fromstring(xml)
    paragraphs: List[str] = []
    namespace = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}
    for paragraph in root.findall(".//w:p", namespace):
        parts = [node.text or "" for node in paragraph.findall(".//w:t", namespace)]
        line = "".join(parts).strip()
        if line:
            paragraphs.append(line)
    return "\n".join(paragraphs)


def _extract_pdf(data: bytes) -> str:
    try:
        from pypdf import PdfReader
    except ImportError as exc:
        raise ValueError("PDF parsing is not enabled on the server yet. Please upload DOCX or TXT.") from exc

    try:
        reader = PdfReader(BytesIO(data))
        text = "\n".join((page.extract_text() or "") for page in reader.pages[:8])
    except Exception as exc:
        raise ValueError("This PDF could not be read. Please upload a text-based PDF, DOCX, or TXT resume.") from exc
    if not text.strip():
        raise ValueError("No readable text was found in this PDF. Please upload DOCX or TXT.")
    return text


def extract_resume_text(file_name: str, data: bytes) -> str:
    if not data:
        raise ValueError("The uploaded resume file is empty.")
    if len(data) > MAX_RESUME_BYTES:
        raise ValueError("Resume file is too large. Please upload a file under 5 MB.")

    extension = Path(file_name or "").suffix.lower()
    if extension not in SUPPORTED_EXTENSIONS:
        raise ValueError("Unsupported resume format. Please upload PDF, DOCX, TXT, or MD.")

    if extension in {".txt", ".md"}:
        text = _decode_text(data)
    elif extension == ".docx":
        text = _extract_docx(data)
    else:
        text = _extract_pdf(data)

    text = _clean_text(text)
    if len(text) < 40:
        raise ValueError("Resume text is too short to personalize interview questions.")
    return text


def _lines(text: str) -> List[str]:
    return [line.strip(" -•\t") for line in text.splitlines() if line.strip(" -•\t")]


def _first_nonempty_lines(lines: List[str], limit: int = 4) -> str:
    ignored = {"resume", "curriculum vitae", "cv"}
    selected = [line for line in lines if line.lower() not in ignored][:limit]
    return " ".join(selected)[:700]


def _collect_after_headings(lines: List[str], headings: set[str], limit: int = 12) -> List[str]:
    collected: List[str] = []
    active = False
    stop_words = {
        "experience",
        "work experience",
        "employment",
        "education",
        "projects",
        "certifications",
        "skills",
        "summary",
        "profile",
    }
    for line in lines:
        key = re.sub(r"[^a-z ]", "", line.lower()).strip()
        if key in headings:
            active = True
            continue
        if active and key in stop_words and key not in headings:
            break
        if active:
            collected.extend(_split_items(line))
        if len(collected) >= limit:
            break
    return _dedupe(collected)[:limit]


def _split_items(line: str) -> List[str]:
    parts = re.split(r"[,;|•]+", line)
    return [part.strip() for part in parts if 2 < len(part.strip()) <= 70]


def _dedupe(items: List[str]) -> List[str]:
    seen = set()
    result = []
    for item in items:
        key = item.lower()
        if key not in seen:
            seen.add(key)
            result.append(item)
    return result


def _infer_skills(lines: List[str]) -> List[str]:
    explicit = _collect_after_headings(lines, {"skills", "technical skills", "core skills", "competencies"}, 18)
    if explicit:
        return explicit[:18]

    joined = " ".join(lines).lower()
    common = [
        "customer service",
        "project management",
        "leadership",
        "sales",
        "communication",
        "data analysis",
        "operations",
        "python",
        "javascript",
        "react",
        "sql",
        "excel",
        "stakeholder management",
        "training",
        "problem solving",
    ]
    return [skill.title() for skill in common if skill in joined][:12]


def _infer_experience(lines: List[str]) -> List[str]:
    explicit = _collect_after_headings(lines, {"experience", "work experience", "employment"}, 8)
    if explicit:
        return explicit[:8]
    experience = []
    pattern = re.compile(r"(manager|lead|engineer|analyst|assistant|representative|specialist|director|teacher|nurse|developer|coach)", re.I)
    for line in lines:
        if pattern.search(line) or re.search(r"\b(20\d{2}|19\d{2})\b", line):
            experience.append(line[:140])
        if len(experience) >= 8:
            break
    return _dedupe(experience)


def _infer_education(lines: List[str]) -> List[str]:
    education = _collect_after_headings(lines, {"education", "academic background"}, 6)
    if education:
        return education[:6]
    pattern = re.compile(r"(university|college|bachelor|master|mba|degree|diploma|certification)", re.I)
    return _dedupe([line[:140] for line in lines if pattern.search(line)])[:6]


def build_resume_profile(text: str, file_name: str, source_type: str = "file") -> Dict[str, Any]:
    lines = _lines(text)
    skills = _infer_skills(lines)
    experience = _infer_experience(lines)
    education = _infer_education(lines)

    summary_parts = [_first_nonempty_lines(lines)]
    if skills:
        summary_parts.append(f"Key skills: {', '.join(skills[:8])}.")
    if experience:
        summary_parts.append(f"Relevant background: {'; '.join(experience[:3])}.")

    return {
        "file_name": file_name,
        "source_type": source_type,
        "summary": " ".join(part for part in summary_parts if part).strip()[:1200],
        "skills": skills,
        "experience": experience,
        "education": education,
        "raw_text_excerpt": text[:4000],
    }
