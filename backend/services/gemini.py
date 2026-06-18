import os
import httpx
import time
import base64
import json
import re
from dotenv import load_dotenv

load_dotenv()

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-2.0-flash")
GROQ_API_KEY = os.getenv("GROQ_API_KEY")
GROQ_MODEL = os.getenv("GROQ_MODEL", "llama-3.1-8b-instant")
GROQ_TRANSCRIPTION_MODEL = os.getenv("GROQ_TRANSCRIPTION_MODEL", "whisper-large-v3-turbo")
AI_SCORING_PROVIDER = os.getenv("AI_SCORING_PROVIDER", "groq" if GROQ_API_KEY else "gemini").lower()
QUESTION_PROVIDER_TIMEOUT_SECONDS = float(os.getenv("QUESTION_PROVIDER_TIMEOUT_SECONDS", "12"))

class GeminiService:
    def __init__(self):
        self.api_key = GEMINI_API_KEY
        self.model = GEMINI_MODEL
        self.groq_api_key = GROQ_API_KEY
        self.groq_model = GROQ_MODEL
        self.groq_transcription_model = GROQ_TRANSCRIPTION_MODEL
        self.scoring_provider = AI_SCORING_PROVIDER
        self.base_url = f"https://generativelanguage.googleapis.com/v1beta/models/{self.model}:generateContent"
        self.headers = {
            "Content-Type": "application/json"
        }
        self.analysis_cooldown_until = 0.0
        self.groq_analysis_cooldown_until = 0.0
        print(
            f"[AI] Scoring provider={self.scoring_provider} "
            f"groq_configured={bool(self.groq_api_key)} "
            f"gemini_configured={bool(self.api_key)} "
            f"groq_model={self.groq_model}"
        )

    async def generate_questions(self, role_title: str, industry: str, seniority: str, interview_type: str, difficulty: str, company_name: str, company_website: str, company_context: str, panel_descriptions: str, length: str, include_technical: bool = True, language: str = "en"):
        technical_instruction = "Include practical role-specific depth only when the role clearly needs it. Avoid obscure trivia and ask one technical idea at a time." if include_technical else "Focus primarily on leadership, soft skills, and behavioral consistency, avoiding deep technical trivia."
        mode_instruction = self._question_mode_instruction(interview_type)
        
        if length == "quick":
            question_count = 3
        elif length == "deep":
            question_count = 15
        else:
            question_count = 8
        
        prompt = f"""
        You are an expert interview coach generating a realistic live panel interview.
        Calibrate the session like a clear practice interview for a real candidate, not a quiz.
        
        {technical_instruction}

        Output language: {language}. Write every interview question naturally in this language.
        Keep names, company names, and role titles in their original form unless a localized wording is more natural.

        Target Company: {company_name} ({company_website})
        Role: {role_title}
        Industry: {industry}
        Seniority: {seniority}
        Interview type: {interview_type}
        Difficulty: {difficulty}
        Company context: {company_context}
        Panel: {panel_descriptions}

        Generate exactly {question_count} interview questions.
        {mode_instruction}
        Assign each question to one of the panel members by name.

        Make the panel feel human and easy to follow:
        - Friendly interviewers should sound warm but still specific.
        - Tough interviewers should challenge vague claims without sounding hostile.
        - Neutral interviewers should sound calm, precise, and analytical.
        - Each question should include company or role context when available.
        - Later questions should build on the role/company context instead of repeating the same setup.
        - Ask for one concrete example, action, result, decision, or lesson.
        - Use plain everyday language. Avoid jargon unless the role requires it.
        - Ask one main thing at a time. Avoid compound questions with multiple asks.
        - Keep most questions under 22 words, and all questions under 32 words.
        - For non-technical roles, ask about common work situations and customer/team problems.
        - Do not ask generic textbook questions unless the role context truly calls for it.
        - Never put the panel member/interviewer name inside the question text.
        - Do not end questions with the candidate's name or the interviewer name.

        Calibration rubric:
        - Easy: one direct question that asks for one concrete example.
        - Medium: one clear question requiring ownership and a result.
        - Hard: one clear question with pressure or ambiguity, but still understandable.

        Return ONLY a JSON object (without markdown code blocks constraints) exactly matching this schema:
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

        url = f"{self.base_url}?key={self.api_key}"

        if self.groq_api_key:
            groq_questions = await self._generate_questions_with_groq(prompt)
            if groq_questions:
                return self._sanitize_question_payload(groq_questions)

        if self.scoring_provider == "groq":
            print("[AI] Groq question generation failed; Gemini fallback is disabled while provider=groq.")
            return self._fallback_questions(
                question_count,
                role_title,
                industry,
                seniority,
                difficulty,
                company_name,
                language,
            )
        
        async with httpx.AsyncClient() as client:
            try:
                response = await client.post(
                    url,
                    headers=self.headers,
                    json={
                        "contents": [{"parts": [{"text": prompt}]}],
                        "generationConfig": {
                            "responseMimeType": "application/json",
                            "temperature": 0.7
                        }
                    },
                    timeout=QUESTION_PROVIDER_TIMEOUT_SECONDS
                )
                response.raise_for_status()
                result = response.json()
                import json
                content = result["candidates"][0]["content"]["parts"][0]["text"]
                return self._sanitize_question_payload(json.loads(content))
            except Exception as e:
                print(f"Error calling Gemini: {e}")
                return self._fallback_questions(
                    question_count,
                    role_title,
                    industry,
                    seniority,
                    difficulty,
                    company_name,
                    language,
                )

    async def _generate_questions_with_groq(self, prompt: str):
        try:
            async with httpx.AsyncClient(timeout=QUESTION_PROVIDER_TIMEOUT_SECONDS) as client:
                response = await client.post(
                    "https://api.groq.com/openai/v1/chat/completions",
                    headers={
                        "Authorization": f"Bearer {self.groq_api_key}",
                        "Content-Type": "application/json",
                    },
                    json={
                        "model": self.groq_model,
                        "messages": [
                            {"role": "system", "content": "You generate strict JSON only."},
                            {"role": "user", "content": prompt},
                        ],
                        "temperature": 0.65,
                        "response_format": {"type": "json_object"},
                    },
                )
                response.raise_for_status()
                content = response.json()["choices"][0]["message"]["content"]
                parsed = self._parse_json_object(content)
                if isinstance(parsed.get("questions"), list) and parsed["questions"]:
                    return self._sanitize_question_payload(parsed)
        except Exception as e:
            print(f"Error calling Groq for questions: {e}")
        return None

    def _sanitize_question_payload(self, payload: dict) -> dict:
        questions = payload.get("questions")
        if not isinstance(questions, list):
            return payload

        interviewer_names = [
            str(question.get("interviewer_name") or "").strip()
            for question in questions
            if isinstance(question, dict)
        ]
        interviewer_names.extend(["Maya", "Jonas", "Sophia"])

        for question in questions:
            if not isinstance(question, dict):
                continue
            text = str(question.get("text") or "").strip()
            question["text"] = self._strip_trailing_person_name(text, interviewer_names)
        return payload

    def _strip_trailing_person_name(self, text: str, names: list[str]) -> str:
        cleaned = (text or "").strip()
        for name in sorted({n for n in names if n}, key=len, reverse=True):
            pattern = re.compile(rf"([\s,;:–—-]+){re.escape(name)}\s*([?.!])?$", re.IGNORECASE)
            cleaned = pattern.sub(lambda match: match.group(2) or "?", cleaned).strip()
        cleaned = re.sub(r"\s+([?.!,;:])", r"\1", cleaned)
        return cleaned

    def _fallback_questions(self, question_count: int, role_title: str, industry: str, seniority: str, difficulty: str, company_name: str, language: str):
        company = company_name or {
            "es": "la empresa objetivo",
            "fr": "l'entreprise cible",
            "de": "das Zielunternehmen",
            "it": "l'azienda target",
            "pt": "a empresa alvo",
            "zh": "目标公司",
            "ja": "対象企業",
            "ko": "대상 회사",
            "ar": "الشركة المستهدفة",
            "hi": "लक्षित कंपनी",
        }.get((language or "en").lower(), "the target company")

        templates_by_lang = {
            "en": [
                ("Maya", "behavioural", f"Tell me about one real experience that prepared you for this {role_title} role."),
                ("Jonas", "situational", f"What result from your past work shows you can succeed in {industry}?"),
                ("Sophia", "role-specific", f"What would you do first to understand what matters at {company}?"),
                ("Jonas", "pressure", "Tell me about a time your first approach failed. What did you change?"),
                ("Maya", "behavioural", "How do you keep people confident when the next step is unclear?"),
                ("Sophia", "analytical", f"What would you learn first in this {role_title} role?"),
                ("Jonas", "pressure", "What skill do you still need to improve for this role?"),
                ("Maya", "closing", "Why do you want this role now?"),
            ],
            "es": [
                ("Maya", "behavioural", f"Cuéntame el ejemplo más sólido de tu experiencia que demuestre que estás listo para el puesto de {role_title}."),
                ("Jonas", "situational", f"Quiero datos concretos. ¿Qué resultado medible demuestra que puedes rendir a nivel {seniority} en {industry}?"),
                ("Sophia", "role-specific", f"Con el contexto de {company}, ¿qué tradeoff esperarías enfrentar en este rol y cómo lo manejarías?"),
                ("Jonas", "pressure", "Háblame de una vez en la que tu primer enfoque falló. ¿Qué cambiaste y cuál fue el resultado?"),
                ("Maya", "behavioural", "¿Cómo mantienes la confianza de los stakeholders cuando el camino no está claro?"),
                ("Sophia", "analytical", f"¿Qué tendrías que aprender en tus primeros 30 días para crear impacto en este rol de {role_title}?"),
                ("Jonas", "pressure", "¿Cuál es la parte más débil de tu candidatura y qué evidencia reduciría mi preocupación?"),
                ("Maya", "closing", "¿Por qué este rol, por qué ahora y qué debería recordar este panel de ti?"),
            ],
            "fr": [
                ("Maya", "behavioural", f"Présentez l'exemple le plus solide qui montre que vous êtes prêt pour ce poste de {role_title}."),
                ("Jonas", "situational", f"Je veux du concret. Quel résultat mesurable prouve que vous pouvez réussir au niveau {seniority} dans {industry} ?"),
                ("Sophia", "role-specific", f"Dans le contexte de {company}, quel compromis pensez-vous devoir gérer dans ce rôle et comment ?"),
                ("Jonas", "pressure", "Parlez-moi d'une fois où votre première approche a échoué. Qu'avez-vous changé et quel a été le résultat ?"),
                ("Maya", "behavioural", "Comment gardez-vous la confiance des parties prenantes quand la voie à suivre est incertaine ?"),
                ("Sophia", "analytical", f"Que devriez-vous apprendre dans les 30 premiers jours pour créer de la valeur dans ce rôle de {role_title} ?"),
                ("Jonas", "pressure", "Quelle est la partie la plus faible de votre candidature, et quelle preuve réduirait mon inquiétude ?"),
                ("Maya", "closing", "Pourquoi ce rôle, pourquoi maintenant, et que doit retenir ce panel de vous ?"),
            ],
            "de": [
                ("Maya", "behavioural", f"Nennen Sie das stärkste Beispiel aus Ihrer Erfahrung, das zeigt, dass Sie für die Rolle {role_title} bereit sind."),
                ("Jonas", "situational", f"Ich möchte Konkretes hören. Welches messbare Ergebnis zeigt, dass Sie auf {seniority}-Niveau in {industry} arbeiten können?"),
                ("Sophia", "role-specific", f"Mit Blick auf {company}: Welchen Zielkonflikt erwarten Sie in dieser Rolle und wie würden Sie damit umgehen?"),
                ("Jonas", "pressure", "Erzählen Sie von einer Situation, in der Ihr erster Ansatz scheiterte. Was haben Sie geändert und was war das Ergebnis?"),
                ("Maya", "behavioural", "Wie halten Sie Stakeholder zuversichtlich, wenn der weitere Weg unsicher ist?"),
                ("Sophia", "analytical", f"Was müssten Sie in den ersten 30 Tagen lernen, um in dieser {role_title}-Rolle Wirkung zu erzielen?"),
                ("Jonas", "pressure", "Was ist der schwächste Teil Ihrer Kandidatur, und welche Evidenz würde meine Sorge reduzieren?"),
                ("Maya", "closing", "Warum diese Rolle, warum jetzt, und woran sollte sich dieses Panel bei Ihnen erinnern?"),
            ],
            "it": [
                ("Maya", "behavioural", f"Raccontami l'esempio più forte della tua esperienza che dimostra che sei pronto per il ruolo di {role_title}."),
                ("Jonas", "situational", f"Voglio dettagli concreti. Quale risultato misurabile dimostra che puoi operare a livello {seniority} in {industry}?"),
                ("Sophia", "role-specific", f"Nel contesto di {company}, quale compromesso ti aspetti di affrontare in questo ruolo e come lo gestiresti?"),
                ("Jonas", "pressure", "Parlami di una volta in cui il tuo primo approccio non ha funzionato. Cosa hai cambiato e qual è stato il risultato?"),
                ("Maya", "behavioural", "Come mantieni la fiducia degli stakeholder quando il percorso non è chiaro?"),
                ("Sophia", "analytical", f"Cosa dovresti imparare nei primi 30 giorni per creare leva nel ruolo di {role_title}?"),
                ("Jonas", "pressure", "Qual è la parte più debole della tua candidatura e quale evidenza ridurrebbe la mia preoccupazione?"),
                ("Maya", "closing", "Perché questo ruolo, perché ora, e cosa dovrebbe ricordare questo panel di te?"),
            ],
            "pt": [
                ("Maya", "behavioural", f"Conte o exemplo mais forte da sua experiência que mostra que você está pronto para o cargo de {role_title}."),
                ("Jonas", "situational", f"Quero detalhes. Que resultado mensurável prova que você consegue atuar no nível {seniority} em {industry}?"),
                ("Sophia", "role-specific", f"No contexto de {company}, que tradeoff você espera enfrentar neste cargo e como lidaria com ele?"),
                ("Jonas", "pressure", "Fale sobre uma vez em que sua primeira abordagem falhou. O que você mudou e qual foi o resultado?"),
                ("Maya", "behavioural", "Como você mantém stakeholders confiantes quando o caminho ainda é incerto?"),
                ("Sophia", "analytical", f"O que você precisaria aprender nos primeiros 30 dias para gerar impacto neste cargo de {role_title}?"),
                ("Jonas", "pressure", "Qual é a parte mais fraca da sua candidatura e que evidência reduziria minha preocupação?"),
                ("Maya", "closing", "Por que este cargo, por que agora, e o que este painel deve lembrar sobre você?"),
            ],
            "zh": [
                ("Maya", "behavioural", f"请讲一个最能证明你已经准备好胜任 {role_title} 岗位的真实经历。"),
                ("Jonas", "situational", f"我需要具体证据。什么可衡量结果证明你能在 {industry} 中达到 {seniority} 水平？"),
                ("Sophia", "role-specific", f"结合 {company} 的背景，你预计这个岗位会遇到什么取舍？你会如何处理？"),
                ("Jonas", "pressure", "请讲一次你的第一方案失败的经历。你改变了什么，结果如何？"),
                ("Maya", "behavioural", "当方向不确定时，你如何让利益相关者保持信心？"),
                ("Sophia", "analytical", f"入职前 30 天你需要学习什么，才能在 {role_title} 岗位上创造影响？"),
                ("Jonas", "pressure", "你候选资格中最薄弱的一点是什么？什么证据能降低我的担忧？"),
                ("Maya", "closing", "为什么是这个岗位，为什么是现在？这个面试小组应该记住你的什么？"),
            ],
            "ja": [
                ("Maya", "behavioural", f"{role_title} の役割に準備ができていることを示す、最も強い実体験を話してください。"),
                ("Jonas", "situational", f"具体的に聞きます。{industry} で {seniority} レベルで成果を出せることを示す測定可能な実績は何ですか？"),
                ("Sophia", "role-specific", f"{company} の文脈で、この役割ではどんなトレードオフに直面すると考えますか？どう対応しますか？"),
                ("Jonas", "pressure", "最初のアプローチが失敗した経験を教えてください。何を変え、結果はどうなりましたか？"),
                ("Maya", "behavioural", "進む道が不確かなとき、ステークホルダーの信頼をどう保ちますか？"),
                ("Sophia", "analytical", f"{role_title} として最初の30日で価値を出すために、何を学ぶ必要がありますか？"),
                ("Jonas", "pressure", "あなたの候補者としての一番弱い点は何ですか？どんな証拠があれば懸念が下がりますか？"),
                ("Maya", "closing", "なぜこの役割で、なぜ今なのか。この面接官たちはあなたについて何を覚えるべきですか？"),
            ],
            "ko": [
                ("Maya", "behavioural", f"{role_title} 역할을 맡을 준비가 되었음을 보여주는 가장 강한 실제 경험을 말해 주세요."),
                ("Jonas", "situational", f"구체적으로 말해 주세요. {industry}에서 {seniority} 수준으로 성과를 낼 수 있음을 보여주는 측정 가능한 결과는 무엇인가요?"),
                ("Sophia", "role-specific", f"{company}의 맥락에서 이 역할에서 어떤 트레이드오프를 예상하며 어떻게 다루겠습니까?"),
                ("Jonas", "pressure", "처음 접근이 실패했던 경험을 말해 주세요. 무엇을 바꿨고 결과는 어땠나요?"),
                ("Maya", "behavioural", "앞길이 불확실할 때 이해관계자들의 신뢰를 어떻게 유지하나요?"),
                ("Sophia", "analytical", f"{role_title} 역할에서 첫 30일 안에 영향력을 만들려면 무엇을 배워야 하나요?"),
                ("Jonas", "pressure", "본인 후보자로서 가장 약한 부분은 무엇이며, 어떤 증거가 제 우려를 줄일 수 있나요?"),
                ("Maya", "closing", "왜 이 역할이고 왜 지금인가요? 이 패널이 당신에 대해 무엇을 기억해야 하나요?"),
            ],
            "ar": [
                ("Maya", "behavioural", f"اذكر أقوى مثال من خبرتك يثبت أنك جاهز لدور {role_title}."),
                ("Jonas", "situational", f"أريد تفاصيل محددة. ما النتيجة القابلة للقياس التي تثبت قدرتك على الأداء بمستوى {seniority} في {industry}؟"),
                ("Sophia", "role-specific", f"في سياق {company}، ما المفاضلة التي تتوقع مواجهتها في هذا الدور وكيف ستتعامل معها؟"),
                ("Jonas", "pressure", "حدثني عن مرة فشل فيها نهجك الأول. ماذا غيرت وما النتيجة؟"),
                ("Maya", "behavioural", "كيف تحافظ على ثقة أصحاب المصلحة عندما يكون المسار غير واضح؟"),
                ("Sophia", "analytical", f"ما الذي تحتاج إلى تعلمه في أول 30 يوما لتخلق تأثيرا في دور {role_title}؟"),
                ("Jonas", "pressure", "ما أضعف جانب في ترشحك، وما الدليل الذي سيقلل قلقي؟"),
                ("Maya", "closing", "لماذا هذا الدور، ولماذا الآن، وما الذي يجب أن يتذكره هذا الفريق عنك؟"),
            ],
            "hi": [
                ("Maya", "behavioural", f"अपने अनुभव से वह सबसे मजबूत उदाहरण बताइए जो दिखाता है कि आप {role_title} भूमिका के लिए तैयार हैं।"),
                ("Jonas", "situational", f"मुझे ठोस बात चाहिए। कौन सा मापने योग्य परिणाम साबित करता है कि आप {industry} में {seniority} स्तर पर प्रदर्शन कर सकते हैं?"),
                ("Sophia", "role-specific", f"{company} के संदर्भ में, इस भूमिका में आपको कौन सा tradeoff दिखता है और आप उसे कैसे संभालेंगे?"),
                ("Jonas", "pressure", "ऐसे समय के बारे में बताइए जब आपका पहला तरीका असफल हुआ। आपने क्या बदला और परिणाम क्या रहा?"),
                ("Maya", "behavioural", "जब आगे का रास्ता अस्पष्ट हो, तब आप stakeholders का भरोसा कैसे बनाए रखते हैं?"),
                ("Sophia", "analytical", f"{role_title} भूमिका में प्रभाव बनाने के लिए पहले 30 दिनों में आपको क्या सीखना होगा?"),
                ("Jonas", "pressure", "आपकी उम्मीदवारी का सबसे कमजोर हिस्सा क्या है, और कौन सा प्रमाण मेरी चिंता कम करेगा?"),
                ("Maya", "closing", "यह भूमिका क्यों, अभी क्यों, और इस पैनल को आपके बारे में क्या याद रखना चाहिए?"),
            ],
        }
        base_questions = templates_by_lang.get((language or "en").lower(), templates_by_lang["en"])
        questions = []
        for index in range(question_count):
            interviewer_name, question_type, text = base_questions[index % len(base_questions)]
            questions.append({
                "number": index + 1,
                "text": text,
                "interviewer_name": interviewer_name,
                "type": question_type,
                "difficulty": difficulty
            })
        return {"questions": questions, "source": "fallback"}

    def _question_mode_instruction(self, interview_type: str) -> str:
        mode = (interview_type or "mixed").strip().lower()
        if mode == "behavioral":
            return """
            This is a BEHAVIORAL interview. At least 80% of questions must ask for a real past example.
            Use simple STAR-style prompts, but ask for one thing at a time: situation, action, result,
            conflict, ownership, feedback, or lesson.
            Avoid technical trivia, case prompts, and abstract hypotheticals unless used as a brief follow-up.
            """
        if mode == "technical":
            return """
            This is a TECHNICAL interview. Prioritize practical debugging, implementation decisions,
            reliability, data, product judgment, and role-specific depth.
            Ask one technical concept per question and keep the wording clear.
            Include behavioral follow-ups only when they reveal how the candidate made a technical decision.
            """
        if mode == "promotion":
            return """
            This is a PROMOTION or seniority-readiness interview. Focus on scope, ownership, influence,
            strategic judgment, cross-functional leadership, business impact, raising the bar, and readiness
            for the next level. Ask for concrete evidence of operating above the current level.
            """
        if mode == "quick":
            return """
            This is a QUICK DRILL. Ask concise questions that can be answered in 60-90 seconds.
            Each question should test one skill at a time and avoid compound prompts.
            """
        return """
        This is a MIXED full-panel interview. Mix behavioral, situational, role-specific,
        technical or analytical, and follow-up-style questions appropriate for the role.
        Keep the session realistic, clear, and not unnecessarily complex.
        """

    async def analyze_answer(self, question_text: str, audio_base64: str = None, transcript: str = None, audio_duration_seconds: int | None = None, audio_mime_type: str | None = None):
        audio_chars = len(audio_base64 or "")
        normalized_mime_type = self._normalize_audio_mime_type(audio_mime_type, audio_base64)
        if self.scoring_provider == "groq":
            if not self.groq_api_key:
                return self._analysis_unavailable(
                    question_text,
                    transcript,
                    audio_chars,
                    audio_duration_seconds,
                    "groq_not_configured",
                    "Groq is selected for scoring, but no Groq API key is configured.",
                )
            groq_analysis = await self._analyze_answer_with_groq(question_text, audio_base64, transcript, audio_duration_seconds, normalized_mime_type)
            if groq_analysis:
                return groq_analysis
            return self._analysis_unavailable(
                question_text,
                transcript,
                audio_chars,
                audio_duration_seconds,
                "groq_analysis_failed",
                "Groq did not return a real transcript and score.",
            )

        if self._should_use_groq_for_scoring():
            groq_analysis = await self._analyze_answer_with_groq(question_text, audio_base64, transcript, audio_duration_seconds, normalized_mime_type)
            if groq_analysis:
                return groq_analysis

        if audio_base64:
            audio_payload = audio_base64.split(",", 1)[-1]
            prompt = f"""
            You are an expert interview coach and hiring manager.
            Listen to the following audio clip of the candidate answering the interview question.

            Question: {question_text}

            Evaluate based on:
            1. Clarity: structured, specific, easy to follow.
            2. Pacing: concise enough for a live interview, without rushing.
            3. Impact: clear stakes, measurable outcomes, and business relevance.
            4. Confidence: direct ownership, calm delivery, and no evasive filler.
            5. Knowledge: role-specific depth, tradeoffs, and sound judgment.

            CRITICAL SCORING RULES — follow strictly to avoid grade inflation:
            - 85-100: RARE. Only when the answer has concrete metrics, clear personal ownership,
              and flawless structure. Do NOT award this to generic or vague answers.
            - 65-84: Solid but missing at least one key element (no metric, weak ownership, or
              shallow reasoning). This is where MOST decent answers should land.
            - 40-64: Vague, rambling, uses filler, or avoids directly answering the question.
            - 15-39: Mostly off-topic, incoherent, or just a few words with no substance.
            - 0-14: Empty, "I don't know" with no attempt, or technically unusable audio.
            DEFAULT assumption: start at 55 and move up ONLY when the answer proves it
            deserves a higher score with specific evidence. Be skeptical, not generous.

            First, precisely transcribe the audio into text.
            Then provide scores, feedback, and one sharp follow-up question that a real interviewer would ask next.
            Feedback must cite the strongest evidence and the highest-leverage improvement.
            CRITICAL — improved_answer must be a MODEL ANSWER, not a rewrite of what the candidate said:
            Put yourself in the candidate's shoes. Answer this interview question as if YOU are a top-tier
            candidate being interviewed for this role. Craft a robust, specific, first-person answer (45-75
            seconds spoken aloud). Think about what a strong candidate WOULD say — use realistic, plausible
            details: concrete metrics, specific actions, clear personal ownership, and measurable outcomes
            that are credible for this role and industry. Do NOT reference the candidate's transcript. Do NOT
            use bracketed placeholders like [specific project] — invent plausible, realistic specifics instead.
            The improved_answer must directly answer the question in full as a complete spoken response.
            Do not write "A stronger answer would..." or any coaching language inside improved_answer.
            Also teach the candidate how to improve: explain what information to include, how to structure it,
            and why that structure makes the answer stronger.

            Return ONLY a JSON object exactly matching this schema:
            {{
              "transcript": "Exact transcription of what the candidate said...",
              "score": 85,
              "clarity_score": 88,
              "pacing_score": 75,
              "impact_score": 82,
              "confidence_score": 90,
              "knowledge_score": 85,
              "feedback": "Evidence-backed evaluation with one high-leverage improvement.",
              "follow_up": "One sharp follow-up question the interviewer would ask next.",
              "tips": ["Use more specific numbers", "Slow down your delivery"],
              "improved_answer": "A complete first-person 45-75 second answer that directly answers the question.",
              "answer_structure": "Situation -> Action -> Result",
              "missing_evidence": ["Metric or outcome", "Specific personal action"],
              "stronger_phrasing": ["Replace vague phrase with stronger wording"],
              "coaching_breakdown": {{
                "what_to_include": "The specific evidence, example, metric, or tradeoff this answer needs.",
                "how_to_structure": "How to organize the answer step by step.",
                "why_it_works": "Why this stronger version builds interviewer confidence."
              }}
            }}
            """
            parts = [
                {"text": prompt},
                {"inlineData": {"mimeType": normalized_mime_type, "data": audio_payload}}
            ]
        else:
            prompt = f"""
            You are an expert interview coach and hiring manager.
            Analyze the following interview response and provide scores and feedback.

            Question: {question_text}
            Candidate Answer: {transcript}

            Evaluate based on:
            1. Clarity: structured, specific, easy to follow.
            2. Pacing: concise enough for a live interview, without rushing.
            3. Impact: clear stakes, measurable outcomes, and business relevance.
            4. Confidence: direct ownership, calm delivery, and no evasive filler.
            5. Knowledge: role-specific depth, tradeoffs, and sound judgment.

            CRITICAL SCORING RULES — follow strictly to avoid grade inflation:
            - 85-100: RARE. Only when the answer has concrete metrics, clear personal ownership,
              and flawless structure. Do NOT award this to generic or vague answers.
            - 65-84: Solid but missing at least one key element (no metric, weak ownership, or
              shallow reasoning). This is where MOST decent answers should land.
            - 40-64: Vague, rambling, uses filler, or avoids directly answering the question.
            - 15-39: Mostly off-topic, incoherent, or just a few words with no substance.
            - 0-14: Empty, "I don't know" with no attempt, or technically unusable audio.
            DEFAULT assumption: start at 55 and move up ONLY when the answer proves it
            deserves a higher score with specific evidence. Be skeptical, not generous.

            CRITICAL — improved_answer must be a MODEL ANSWER, not a rewrite of what the candidate said:
            Put yourself in the candidate's shoes. Answer this interview question as if YOU are a top-tier
            candidate being interviewed for this role. Craft a robust, specific, first-person answer (45-75
            seconds spoken aloud). Think about what a strong candidate WOULD say — use realistic, plausible
            details: concrete metrics, specific actions, clear personal ownership, and measurable outcomes
            that are credible for this role and industry. Do NOT reference or reuse the candidate's transcript.
            Do NOT use bracketed placeholders like [specific project] — invent plausible, realistic specifics
            instead. The improved_answer must directly answer the question in full as a complete spoken response.
            Do not write "A stronger answer would..." or any coaching language inside improved_answer.
            Also teach the candidate how to improve: explain what information to include, how to structure it,
            and why that structure makes the answer stronger.

            Return ONLY a JSON object exactly matching this schema:
            {{
              "transcript": "{transcript}",
              "score": 85,
              "clarity_score": 88,
              "pacing_score": 75,
              "impact_score": 82,
              "confidence_score": 90,
              "knowledge_score": 85,
              "feedback": "Overall strong answer, but you could spend more time on the specific actions you took.",
              "follow_up": "One sharp follow-up question the interviewer would ask next.",
              "tips": ["Use more specific numbers", "Slow down your delivery"],
              "improved_answer": "A complete first-person 45-75 second answer that directly answers the question.",
              "answer_structure": "Situation -> Action -> Result",
              "missing_evidence": ["Metric or outcome", "Specific personal action"],
              "stronger_phrasing": ["Replace vague phrase with stronger wording"],
              "coaching_breakdown": {{
                "what_to_include": "The specific evidence, example, metric, or tradeoff this answer needs.",
                "how_to_structure": "How to organize the answer step by step.",
                "why_it_works": "Why this stronger version builds interviewer confidence."
              }}
            }}
            """
            parts = [{"text": prompt}]

        url = f"{self.base_url}?key={self.api_key}"

        if time.time() < self.analysis_cooldown_until:
            return self._fallback_analysis(question_text, audio_chars, transcript, audio_duration_seconds)

        async with httpx.AsyncClient() as client:
            try:
                response = await client.post(
                    url,
                    headers=self.headers,
                    json={
                        "contents": [{"parts": parts}],
                        "generationConfig": {
                            "responseMimeType": "application/json",
                            "temperature": 0.3
                        }
                    },
                    timeout=40.0
                )
                response.raise_for_status()
                result = response.json()
                import json
                content = result["candidates"][0]["content"]["parts"][0]["text"]
                analysis = json.loads(content)
                analysis["analysis_source"] = "gemini"
                analysis["transcript"] = analysis.get("transcript") or (transcript or "")
                self._apply_answer_quality_guardrails(
                    analysis,
                    question_text,
                    analysis.get("transcript", ""),
                    audio_duration_seconds,
                )
                self._ensure_answer_coaching(analysis, question_text)
                return analysis
            except Exception as e:
                if isinstance(e, httpx.HTTPStatusError) and e.response.status_code == 429:
                    self.analysis_cooldown_until = time.time() + 60
                print(f"Error calling Gemini for analysis: {e}")
                return self._fallback_analysis(question_text, audio_chars, transcript, audio_duration_seconds)

    def _normalize_audio_mime_type(self, audio_mime_type: str | None = None, audio_base64: str | None = None) -> str:
        raw = (audio_mime_type or "").strip().lower()
        if audio_base64 and audio_base64.startswith("data:"):
            raw = audio_base64.split(";", 1)[0].replace("data:", "").strip().lower() or raw

        aliases = {
            "audio/x-m4a": "audio/m4a",
            "audio/mp4a-latm": "audio/mp4",
            "audio/aacp": "audio/aac",
            "video/mp4": "audio/mp4",
        }
        raw = aliases.get(raw, raw)
        supported = {"audio/m4a", "audio/mp4", "audio/aac", "audio/mpeg", "audio/mp3", "audio/wav", "audio/webm", "audio/ogg"}
        return raw if raw in supported else "audio/m4a"

    def _audio_filename_for_mime_type(self, mime_type: str) -> str:
        extensions = {
            "audio/m4a": "m4a",
            "audio/mp4": "m4a",
            "audio/aac": "aac",
            "audio/mpeg": "mp3",
            "audio/mp3": "mp3",
            "audio/wav": "wav",
            "audio/webm": "webm",
            "audio/ogg": "ogg",
        }
        return f"answer.{extensions.get(mime_type, 'm4a')}"

    def _should_use_groq_for_scoring(self) -> bool:
        if not self.groq_api_key:
            return False
        if self.scoring_provider in {"gemini", "google"}:
            return False
        if time.time() < self.groq_analysis_cooldown_until:
            return False
        return True

    async def _analyze_answer_with_groq(self, question_text: str, audio_base64: str = None, transcript: str = None, audio_duration_seconds: int | None = None, audio_mime_type: str | None = None):
        try:
            candidate_transcript = (transcript or "").strip()
            if not candidate_transcript and audio_base64:
                candidate_transcript = await self._transcribe_with_groq(audio_base64, audio_mime_type)

            if not candidate_transcript:
                print(
                    "Groq transcription returned empty text "
                    f"(mime={audio_mime_type}, duration={audio_duration_seconds}, audio_chars={len(audio_base64 or '')})"
                )
                return None

            prompt = f"""
            You are an expert interview coach and hiring manager.
            Score the candidate's interview answer with realistic hiring-loop rigor.

            Question: {question_text}
            Candidate answer transcript: {candidate_transcript}

            Evaluate:
            1. Clarity: structured, specific, easy to follow.
            2. Pacing: concise enough for a live interview, without rushing.
            3. Impact: clear stakes, measurable outcomes, and business relevance.
            4. Confidence: direct ownership, calm delivery, and no evasive filler.
            5. Knowledge: role-specific depth, tradeoffs, and sound judgment.

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

            Strict calibration:
            - If the answer does not directly answer the question, score below 30.
            - If the answer has no concrete example, no action, and no result, score below 40.
            - If the answer is under 15 words, score below 25 unless it contains clear quantified proof.
            - If the answer is nonsense, random words, empty filler, or unrelated to the question, score 0-14.
            - If the candidate says they do not know, cannot answer, or gives up, score 0-14.
            - Do not give 40+ merely because the audio was clear. Score content quality, not just delivery.

            CRITICAL — improved_answer must be a MODEL ANSWER, not a rewrite of what the candidate said:
            Put yourself in the candidate's shoes. Answer this interview question as if YOU are a top-tier
            candidate being interviewed for this role. Craft a robust, specific, first-person answer (45-75
            seconds spoken aloud). Think about what a strong candidate WOULD say — use realistic, plausible
            details: concrete metrics, specific actions, clear personal ownership, and measurable outcomes
            that are credible for this role and industry. Do NOT reference or reuse the candidate's transcript.
            Do NOT use bracketed placeholders like [specific project] — invent plausible, realistic specifics
            instead. The improved_answer must directly answer the question in full as a complete spoken response.
            Do not write "A stronger answer would..." or any coaching language inside improved_answer.
            Also teach the candidate how to improve: explain what information to include, how to structure it,
            and why that structure makes the answer stronger.

            Return ONLY JSON with this exact shape:
            {{
              "transcript": "candidate transcript",
              "score": 85,
              "clarity_score": 88,
              "pacing_score": 75,
              "impact_score": 82,
              "confidence_score": 90,
              "knowledge_score": 85,
              "feedback": "Evidence-backed evaluation with one high-leverage improvement.",
              "follow_up": "One sharp follow-up question the interviewer would ask next.",
              "tips": ["Use more specific numbers", "Slow down your delivery"],
              "improved_answer": "A complete first-person 45-75 second answer that directly answers the question.",
              "answer_structure": "Situation -> Action -> Result",
              "missing_evidence": ["Metric or outcome", "Specific personal action"],
              "stronger_phrasing": ["Replace vague phrase with stronger wording"],
              "coaching_breakdown": {{
                "what_to_include": "The specific evidence, example, metric, or tradeoff this answer needs.",
                "how_to_structure": "How to organize the answer step by step.",
                "why_it_works": "Why this stronger version builds interviewer confidence."
              }},
              "quality_label": "valid_answer",
              "question_relevance_score": 85
            }}
            """

            async with httpx.AsyncClient(timeout=35.0) as client:
                response = await client.post(
                    "https://api.groq.com/openai/v1/chat/completions",
                    headers={
                        "Authorization": f"Bearer {self.groq_api_key}",
                        "Content-Type": "application/json",
                    },
                    json={
                        "model": self.groq_model,
                        "messages": [
                            {"role": "system", "content": "You return strict JSON only."},
                            {"role": "user", "content": prompt},
                        ],
                        "temperature": 0.25,
                        "response_format": {"type": "json_object"},
                    },
                )
                response.raise_for_status()
                result = response.json()
                content = result["choices"][0]["message"]["content"]
                analysis = self._parse_json_object(content)
                analysis["transcript"] = analysis.get("transcript") or candidate_transcript
                analysis["analysis_source"] = "groq"
                self._apply_answer_quality_guardrails(analysis, question_text, candidate_transcript, audio_duration_seconds)
                self._ensure_answer_coaching(analysis, question_text)
                return analysis
        except Exception as e:
            if isinstance(e, httpx.HTTPStatusError) and e.response.status_code == 429:
                self.groq_analysis_cooldown_until = time.time() + 60
            print(f"Error calling Groq for analysis: {e}")
            return None

    async def _transcribe_with_groq(self, audio_base64: str, audio_mime_type: str | None = None) -> str:
        if not self.groq_api_key or not audio_base64:
            return ""
        audio_payload = audio_base64.split(",", 1)[-1]
        audio_bytes = base64.b64decode(audio_payload)
        normalized_mime_type = self._normalize_audio_mime_type(audio_mime_type, audio_base64)

        async def request_transcript(mime_type: str) -> str:
            async with httpx.AsyncClient(timeout=35.0) as client:
                response = await client.post(
                    "https://api.groq.com/openai/v1/audio/transcriptions",
                    headers={"Authorization": f"Bearer {self.groq_api_key}"},
                    data={"model": self.groq_transcription_model, "response_format": "json"},
                    files={"file": (self._audio_filename_for_mime_type(mime_type), audio_bytes, mime_type)},
                )
                response.raise_for_status()
                result = response.json()
                return (result.get("text") or "").strip()

        try:
            return await request_transcript(normalized_mime_type)
        except httpx.HTTPStatusError as error:
            if normalized_mime_type == "audio/m4a":
                print(f"Groq rejected audio/m4a; retrying as audio/mp4: {error}")
                return await request_transcript("audio/mp4")
            raise

    def _parse_json_object(self, content: str):
        raw = (content or "").strip()
        if raw.startswith("```"):
            raw = raw.strip("`")
            if raw.lower().startswith("json"):
                raw = raw[4:].strip()
        start = raw.find("{")
        end = raw.rfind("}")
        if start >= 0 and end >= start:
            raw = raw[start:end + 1]
        return json.loads(raw)

    def _apply_answer_quality_guardrails(self, analysis: dict, question_text: str, transcript: str, audio_duration_seconds: int | None = None):
        verdict = self._classify_answer_quality(question_text, transcript, audio_duration_seconds)
        analysis["quality_label"] = verdict["label"]
        analysis["quality_reason"] = verdict["reason"]
        analysis["audio_duration_seconds"] = audio_duration_seconds

        cap = verdict.get("max_score")
        if cap is not None:
            self._cap_scores(analysis, cap)
            if cap <= 35:
                analysis["feedback"] = verdict["feedback"]
                analysis["follow_up"] = verdict["follow_up"]
                analysis["tips"] = verdict["tips"]
                analysis["improved_answer"] = verdict["improved_answer"]
                analysis["answer_structure"] = verdict["answer_structure"]
                analysis["missing_evidence"] = verdict["missing_evidence"]
                analysis["stronger_phrasing"] = verdict["stronger_phrasing"]

        self._calibrate_shallow_answer_scores(analysis, transcript)
        self._ensure_answer_coaching(analysis, question_text)

    def _ensure_answer_coaching(self, analysis: dict, question_text: str):
        transcript = (analysis.get("transcript") or "").strip()
        if not analysis.get("answer_structure"):
            analysis["answer_structure"] = "Situation -> Action -> Result"
        if not analysis.get("missing_evidence"):
            analysis["missing_evidence"] = [
                "Specific situation or context",
                "Your direct action",
                "Measurable result or learning",
            ]
        if not analysis.get("stronger_phrasing"):
            analysis["stronger_phrasing"] = [
                "Lead with the result, then explain how you created it.",
                "Replace vague impact with a concrete metric or observed outcome.",
            ]
        if not analysis.get("coaching_breakdown"):
            analysis["coaching_breakdown"] = self._build_coaching_breakdown(
                question_text,
                transcript,
                analysis.get("missing_evidence") or [],
            )
        improved_answer = (analysis.get("improved_answer") or "").strip()
        if improved_answer and not self._is_instructional_improved_answer(improved_answer) and not self._contains_placeholder_text(improved_answer):
            return

        if not transcript or transcript.startswith("[Audio received") or transcript.startswith("[Audio captured"):
            analysis["improved_answer"] = self._build_model_answer(question_text, "")
            return

        analysis["improved_answer"] = self._build_model_answer(question_text, transcript)

    def _is_instructional_improved_answer(self, answer: str) -> bool:
        value = answer.strip().lower()
        instructional_starts = (
            "a stronger answer would",
            "a stronger answer should",
            "to make it",
            "here is a stronger version",
            "for this question",
            "start with",
            "you should",
            "the candidate should",
        )
        return value.startswith(instructional_starts) or " add [specific" in value

    def _contains_placeholder_text(self, answer: str) -> bool:
        value = (answer or "").lower()
        placeholder_terms = (
            "[specific",
            "[your ",
            "[key ",
            "[challenge",
            "[metric",
            "[project",
            "[role",
            "[context",
            "[result",
            "[action",
        )
        return any(term in value for term in placeholder_terms)

    def _build_model_answer(self, question_text: str, transcript: str = "") -> str:
        question = (question_text or "this question").strip()
        candidate_detail = (transcript or "").strip()
        if candidate_detail and len(candidate_detail) > 220:
            candidate_detail = candidate_detail[:220].rsplit(" ", 1)[0] + "..."

        if candidate_detail:
            return (
                "Absolutely. One example I would use is the situation I described in my answer: "
                f"{candidate_detail}. Framed more clearly, I would explain what I personally owned, the action I took, "
                "the tradeoff I considered, and the result I achieved. What I would bring to this role is the same pattern: "
                "clear ownership, practical judgment, and the discipline to explain outcomes in business terms."
            )

        return (
            f"Absolutely. For '{question}', I would answer with one real example and keep it structured. "
            "I would start with the situation, explain the challenge, describe what I personally did, and end with the result. "
            "That structure helps the interviewer hear ownership, judgment, and evidence instead of a general answer."
        )

    def _build_coaching_breakdown(self, question_text: str, transcript: str = "", missing_evidence: list | None = None) -> dict:
        missing = [str(item) for item in (missing_evidence or []) if str(item).strip()]
        missing_text = ", ".join(missing[:3]) if missing else "a concrete example, your action, and the measurable result"
        question = (question_text or "the question").strip()
        return {
            "what_to_include": f"Include {missing_text}. Tie every point back to the exact question instead of giving a general explanation.",
            "how_to_structure": "Use a simple flow: direct answer first, then situation, your action, tradeoff or decision, and measurable result.",
            "why_it_works": f"This helps the interviewer see proof, ownership, and judgment for: {question}",
        }

    def _classify_answer_quality(self, question_text: str, transcript: str, audio_duration_seconds: int | None = None):
        text = (transcript or "").strip().lower()
        words = [word.strip(".,!?;:\"'()[]{}") for word in text.split() if word.strip()]
        word_count = len(words)
        unique_ratio = len(set(words)) / word_count if word_count else 0
        question_words = {
            word.strip(".,!?;:\"'()[]{}").lower()
            for word in (question_text or "").split()
            if len(word.strip(".,!?;:\"'()[]{}")) >= 5
        }
        overlap = len(question_words.intersection(words))
        give_up_phrases = [
            "i don't know", "i dont know", "no idea", "i have no idea",
            "not sure", "i cannot answer", "i can't answer", "skip",
            "i skipped", "i did not answer", "i didn't answer", "i will skip",
            "pass this", "nothing", "no answer"
        ]
        short_non_answer_phrases = [
            "thank you", "thanks", "i walked", "walked right now",
            "i practically walked", "that's all", "that is all", "i am done",
            "i'm done", "finish", "finished"
        ]
        filler_phrases = [
            "blah blah", "testing testing", "hello hello", "asdf", "qwerty",
            "random words", "banana", "lorem ipsum"
        ]
        evidence_terms = {
            "increased", "decreased", "reduced", "improved", "launched", "built",
            "led", "owned", "measured", "result", "metric", "revenue", "cost",
            "customer", "users", "team", "deadline", "tradeoff", "because", "%",
            "$", "saved", "grew", "delivered", "implemented", "designed"
        }
        ownership_terms = {
            "i", "me", "my", "mine", "owned", "led", "handled", "managed",
            "resolved", "built", "created", "improved", "delivered", "followed"
        }
        action_terms = {
            "handled", "resolved", "fixed", "called", "listened", "explained",
            "prioritized", "planned", "organized", "trained", "supported",
            "followed", "escalated", "built", "created", "improved", "delivered",
            "checked", "reviewed", "coordinated", "communicated"
        }
        result_terms = {
            "result", "outcome", "improved", "reduced", "increased", "saved",
            "delivered", "completed", "resolved", "satisfied", "retained",
            "faster", "better", "within", "after"
        }
        has_evidence = any(term in text for term in evidence_terms)
        has_numbers = any(ch.isdigit() for ch in text)
        has_ownership = any(term in words for term in ownership_terms)
        has_action = any(term in words for term in action_terms)
        has_result = any(term in words for term in result_terms)

        if audio_duration_seconds is not None and audio_duration_seconds <= 2:
            return self._quality_verdict("too_short_audio", "The recording was too short to evaluate.", 8)
        if word_count == 0:
            return self._quality_verdict("empty_transcript", "No usable speech was detected in the answer.", 8)
        if any(phrase in text for phrase in give_up_phrases):
            return self._quality_verdict("gave_up", "The answer indicates the candidate could not answer the question.", 15)
        if word_count < 18 and any(phrase in text for phrase in short_non_answer_phrases):
            return self._quality_verdict("likely_off_topic", "The answer does not appear connected to the question.", 10)
        if any(phrase in text for phrase in filler_phrases):
            return self._quality_verdict("nonsense_or_test_audio", "The answer appears to be test audio, filler, or nonsense.", 12)
        if word_count < 5:
            return self._quality_verdict("too_short_answer", "The answer is too short to show hiring signal.", 10)
        if word_count >= 8 and unique_ratio < 0.35:
            return self._quality_verdict("repetitive_or_nonsense", "The answer appears repetitive or incoherent.", 20)
        if word_count < 15 and not (has_evidence or has_numbers or overlap > 0):
            return self._quality_verdict("likely_off_topic", "The answer is too short and does not connect to the question.", 10)
        if word_count < 15 and not (has_action and has_result):
            return self._quality_verdict("too_short_answer", "The answer is too short to show hiring signal.", 15)
        if word_count < 20 and overlap == 0:
            return self._quality_verdict("likely_off_topic", "The answer does not appear connected to the question.", 10)
        if word_count < 35 and not has_evidence and not has_numbers:
            return self._quality_verdict("shallow_answer", "The answer is understandable but too shallow to prove interview readiness.", 28)
        if word_count < 55 and not (has_ownership and has_action):
            return self._quality_verdict("generic_answer", "The answer is too generic and does not clearly show what the candidate personally did.", 42)
        if word_count < 80 and not (has_result or has_evidence or has_numbers):
            return self._quality_verdict("missing_result", "The answer needs a clearer result, outcome, or lesson to prove interview readiness.", 50)

        return {
            "label": "valid_answer",
            "reason": "Answer passed basic transcript, duration, repetition, and relevance checks.",
            "max_score": None,
        }

    def _quality_verdict(self, label: str, reason: str, max_score: int):
        return {
            "label": label,
            "reason": reason,
            "max_score": max_score,
            "feedback": f"{reason} Give a direct answer with a concrete example, your action, and the measurable result.",
            "follow_up": "Can you give a specific example that directly answers the question?",
            "tips": ["Answer the exact question", "Use a real example", "Include a measurable result"],
            "improved_answer": self._build_model_answer(reason, ""),
            "answer_structure": "Situation -> Action -> Result",
            "missing_evidence": ["Specific example", "Personal action", "Measurable result"],
            "stronger_phrasing": ["I owned...", "The measurable result was..."],
            "coaching_breakdown": self._build_coaching_breakdown(reason, "", ["Specific example", "Personal action", "Measurable result"]),
        }

    def _cap_scores(self, analysis: dict, cap: int):
        for key in ["score", "clarity_score", "pacing_score", "impact_score", "confidence_score", "knowledge_score"]:
            value = analysis.get(key)
            if isinstance(value, (int, float)):
                analysis[key] = int(max(0, min(cap, round(value))))

    def _calibrate_shallow_answer_scores(self, analysis: dict, transcript: str):
        text = (transcript or "").strip().lower()
        words = [word for word in text.split() if word.strip()]
        word_count = len(words)
        short_non_answer_phrases = [
            "thank you", "thanks", "i walked", "walked right now",
            "i practically walked", "that's all", "that is all", "i am done",
            "i'm done", "finish", "finished"
        ]
        evidence_terms = [
            "%", "$", "increased", "reduced", "improved", "launched", "built",
            "led", "owned", "result", "because", "metric", "revenue", "cost",
            "users", "customers", "latency", "deadline", "team"
        ]
        ownership_terms = {"i", "me", "my", "owned", "led", "handled", "managed", "resolved", "delivered"}
        action_terms = {
            "handled", "resolved", "fixed", "listened", "explained", "prioritized",
            "planned", "organized", "supported", "followed", "escalated", "built",
            "created", "improved", "delivered", "communicated"
        }
        result_terms = {"result", "outcome", "improved", "reduced", "increased", "saved", "resolved", "completed"}
        has_evidence = any(term in text for term in evidence_terms)
        normalized_words = [word.strip(".,!?;:\"'()[]{}").lower() for word in words]
        has_ownership = any(term in normalized_words for term in ownership_terms)
        has_action = any(term in normalized_words for term in action_terms)
        has_result = any(term in normalized_words for term in result_terms)
        has_numbers = any(ch.isdigit() for ch in text)

        cap = None
        if word_count == 0:
            cap = 0
        elif word_count < 25 and any(phrase in text for phrase in short_non_answer_phrases):
            cap = 8
        elif word_count < 8 and not has_evidence:
            cap = 8
        elif word_count < 15 and not has_evidence:
            cap = 12
        elif word_count < 25 and not has_evidence:
            cap = 24
        elif word_count < 35 and not (has_evidence or has_numbers):
            cap = 30
        elif word_count < 55 and not (has_ownership and has_action):
            cap = 42
        elif word_count < 80 and not (has_result or has_evidence or has_numbers):
            cap = 50

        if cap is None:
            return

        self._cap_scores(analysis, cap)

        if cap <= 58:
            analysis["feedback"] = "The answer was too shallow to show hiring signal. Give a specific situation, your action, and the measurable result."
            analysis["follow_up"] = "What concrete example proves you can do this in the role?"
            analysis["tips"] = ["Use a real example", "Explain your action", "End with a measurable result"]

    def _analysis_unavailable(
        self,
        question_text: str,
        transcript: str = None,
        audio_chars: int = 0,
        audio_duration_seconds: int | None = None,
        label: str = "analysis_unavailable",
        reason: str = "The selected AI provider did not return a usable result.",
    ):
        text = (transcript or "").strip()
        result = {
            "transcript": text,
            "score": 0,
            "clarity_score": 0,
            "pacing_score": 0,
            "impact_score": 0,
            "confidence_score": 0,
            "knowledge_score": 0,
            "feedback": "We could not produce a real transcript and score for this answer. Please retry this question.",
            "follow_up": "Please answer again with the phone close to your mouth.",
            "tips": ["Move closer to the microphone", "Speak clearly", "Retry when the connection is stable"],
            "improved_answer": "",
            "answer_structure": "Situation -> Action -> Result",
            "missing_evidence": ["Usable transcript and scoring response"],
            "stronger_phrasing": [],
            "coaching_breakdown": [],
            "analysis_source": "analysis_unavailable",
            "quality_label": label,
            "quality_reason": reason,
            "audio_base64_chars": audio_chars,
            "audio_duration_seconds": audio_duration_seconds,
        }
        self._ensure_answer_coaching(result, question_text)
        return result

    def _fallback_analysis(self, question_text: str, audio_chars: int = 0, transcript: str = None, audio_duration_seconds: int | None = None):
        text = (transcript or "").strip()
        return self._analysis_unavailable(
            question_text,
            text,
            audio_chars,
            audio_duration_seconds,
            "ai_provider_unavailable",
            "The AI provider did not return a real scored analysis.",
        )

gemini = GeminiService()


