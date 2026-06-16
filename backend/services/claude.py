import os
import httpx
from dotenv import load_dotenv

load_dotenv()

CLAUDE_API_KEY = os.getenv("CLAUDE_API_KEY")
CLAUDE_MODEL = "claude-3-5-sonnet-20241022" # Updated to latest stable sonnet

class ClaudeService:
    def __init__(self):
        self.api_key = CLAUDE_API_KEY
        self.base_url = "https://api.anthropic.com/v1/messages"
        self.headers = {
            "x-api-key": self.api_key,
            "anthropic-version": "2023-06-01",
            "content-type": "application/json"
        }

    async def generate_questions(self, role_title: str, industry: str, seniority: str, interview_type: str, difficulty: str, company_context: str, panel_descriptions: str, question_count: int = 8):
        prompt = f"""
        You are an expert interview coach generating realistic interview questions.

        Role: {role_title}
        Industry: {industry}
        Seniority: {seniority}
        Interview type: {interview_type}
        Difficulty: {difficulty}
        Company context: {company_context}
        Panel: {panel_descriptions}

        Generate exactly {question_count} interview questions. Mix behavioural, 
        situational, and role-specific questions appropriate for the difficulty level.
        Assign each question to one of the panel members by name.

        Return ONLY valid JSON, no markdown, no preamble:
        {{
          "questions": [
            {{
              "number": 1,
              "text": "question text here",
              "interviewer_name": "panel member name",
              "type": "behavioural",
              "difficulty": "{difficulty}"
            }}
          ]
        }}
        """

        async with httpx.AsyncClient() as client:
            try:
                response = await client.post(
                    self.base_url,
                    headers=self.headers,
                    json={
                        "model": CLAUDE_MODEL,
                        "max_tokens": 2048,
                        "messages": [{"role": "user", "content": prompt}]
                    },
                    timeout=30.0
                )
                response.raise_for_status()
                result = response.json()
                # Parse the JSON from Claude's response
                import json
                content = result["content"][0]["text"]
                return json.loads(content)
            except Exception as e:
                print(f"Error calling Claude: {e}")
                # Fallback mock questions
                return {
                    "questions": [
                        {"number": 1, "text": f"Tell me about your experience as a {role_title}.", "interviewer_name": "Sarah K.", "type": "behavioural", "difficulty": difficulty},
                        {"number": 2, "text": "What is your biggest professional challenge?", "interviewer_name": "Marcus T.", "type": "situational", "difficulty": difficulty}
                    ]
                }

    async def analyze_answer(self, question_text: str, transcript: str):
        prompt = f"""
        You are an expert interview coach and hiring manager.
        Analyze the following interview response and provide scores and feedback.

        Question: {question_text}
        Candidate Answer: {transcript}

        Evaluate based on:
        1. Clarity: How well-explained and structured is the answer?
        2. Pacing: Is it concise or does it ramble?
        3. Impact: Did they show clear results/impact?
        4. Confidence: Does the answer sound assured and professional?
        5. Knowledge: Does the candidate demonstrate subject matter expertise?

        CRITICAL SCORING RULES — follow strictly to avoid grade inflation:
        - 85-100: RARE. Only when the answer has concrete metrics, clear personal ownership,
          and flawless structure. Do NOT award this to generic or vague answers.
        - 65-84: Solid but missing at least one key element (no metric, weak ownership, or
          shallow reasoning). This is where MOST decent answers should land.
        - 40-64: Vague, rambling, uses filler, or avoids directly answering the question.
        - 15-39: Mostly off-topic, incoherent, or just a few words with no substance.
        - 0-14: Empty, "I don't know" with no attempt, or technically unusable.
        DEFAULT assumption: start at 55 and move up ONLY when the answer proves it
        deserves a higher score with specific evidence. Be skeptical, not generous.

        CRITICAL — improved_answer must be a MODEL ANSWER, not a rewrite of what the candidate said:
        Put yourself in the candidate's shoes. Answer this interview question as if YOU are a top-tier
        candidate being interviewed for this role. Craft a robust, specific, first-person answer (45-75
        seconds spoken aloud). Use realistic, plausible details: concrete metrics, specific actions, clear
        personal ownership, and measurable outcomes credible for this role and industry. Do NOT reference
        or reuse the candidate's transcript. Do NOT use bracketed placeholders — invent plausible, realistic
        specifics instead. The improved_answer must directly answer the question in full as a complete
        spoken response. Do not include coaching language inside improved_answer.
        Also teach the candidate how to improve: explain what to include, how to structure it, and why it works.

        Return ONLY valid JSON, no markdown, no preamble:
        {{
          "score": 85,
          "clarity_score": 88,
          "pacing_score": 75,
          "impact_score": 82,
          "confidence_score": 90,
          "knowledge_score": 85,
          "feedback": "Overall strong answer, but you could spend more time on the specific actions you took.",
          "tips": ["Use more specific numbers", "Slow down your delivery"],
          "improved_answer": "A complete first-person 45-75 second answer that directly answers the question.",
          "answer_structure": "Situation -> Action -> Result",
          "missing_evidence": ["Metric or outcome", "Specific personal action"],
          "stronger_phrasing": ["Replace vague phrase with stronger wording"],
          "coaching_breakdown": {
            "what_to_include": "The specific evidence, example, metric, or tradeoff this answer needs.",
            "how_to_structure": "How to organize the answer step by step.",
            "why_it_works": "Why this stronger version builds interviewer confidence."
          }
        }}
        """

        async with httpx.AsyncClient() as client:
            try:
                response = await client.post(
                    self.base_url,
                    headers=self.headers,
                    json={
                        "model": CLAUDE_MODEL,
                        "max_tokens": 1024,
                        "messages": [{"role": "user", "content": prompt}]
                    },
                    timeout=20.0
                )
                response.raise_for_status()
                result = response.json()
                import json
                content = result["content"][0]["text"]
                return json.loads(content)
            except Exception as e:
                print(f"Error calling Claude for analysis: {e}")
                return {
                    "score": 70,
                    "clarity_score": 70,
                    "pacing_score": 70,
                    "impact_score": 70,
                    "confidence_score": 70,
                    "knowledge_score": 70,
                    "feedback": "Unable to provide detailed AI feedback at this time.",
                    "tips": ["Review your answer structure"]
                }

claude = ClaudeService()
