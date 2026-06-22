"""
Neon PostgreSQL Database Service.
Handles all application data: sessions, answers, personas, progress.
"""
import os
import json
import asyncpg
from dotenv import load_dotenv
from typing import Optional, List, Dict, Any
from datetime import datetime, timezone

from services.entitlements import has_unlimited_access

load_dotenv()

DATABASE_URL = os.getenv("DATABASE_URL")


def _json_list(value: Any) -> List[str]:
    if value is None:
        return []
    if isinstance(value, list):
        return [str(item) for item in value if item is not None]
    if isinstance(value, tuple):
        return [str(item) for item in value if item is not None]
    if isinstance(value, str):
        text = value.strip()
        if not text:
            return []
        try:
            decoded = json.loads(text)
            if isinstance(decoded, list):
                return [str(item) for item in decoded if item is not None]
            if decoded is None:
                return []
            return [str(decoded)]
        except json.JSONDecodeError:
            return [text]
    return [str(value)]


def _coverage_adjusted_score(total_score: float, usable_answers: int, expected_questions: int) -> float:
    expected = max(int(expected_questions or 0), int(usable_answers or 0), 1)
    return float(total_score or 0) / expected


def _expected_question_count_from_session(session: Dict, observed_answers: int = 0) -> int:
    configured = int(session.get("question_count") or 0)
    length = str(session.get("length") or "").lower()
    fallback = 9
    if length == "quick":
        fallback = 4
    elif length == "deep":
        fallback = 16
    return max(configured, int(observed_answers or 0), fallback, 1)


class NeonDatabase:
    def __init__(self):
        self.pool: Optional[asyncpg.Pool] = None

    async def connect(self):
        """Create a connection pool to Neon PostgreSQL."""
        if not DATABASE_URL:
            print("WARNING: DATABASE_URL not set. Neon DB will not be available.")
            return
        try:
            self.pool = await asyncpg.create_pool(
                DATABASE_URL,
                min_size=2,
                max_size=10,
                ssl="require"
            )
            print("[Neon] Connected to PostgreSQL database")
            await self._run_migrations()
        except Exception as e:
            print(f"[Neon] Failed to connect: {e}")

    async def _run_migrations(self):
        """Add any missing columns to existing tables."""
        if not self.pool:
            return
        try:
            await self.pool.execute("""
                ALTER TABLE sessions ADD COLUMN IF NOT EXISTS company_name VARCHAR(200);
                ALTER TABLE sessions ADD COLUMN IF NOT EXISTS company_website TEXT;
                ALTER TABLE sessions ADD COLUMN IF NOT EXISTS company_context TEXT;
                ALTER TABLE sessions ADD COLUMN IF NOT EXISTS language VARCHAR(16) DEFAULT 'en';
                ALTER TABLE sessions ADD COLUMN IF NOT EXISTS question_count INT DEFAULT 0;
                ALTER TABLE answers ADD COLUMN IF NOT EXISTS improved_answer TEXT;
                ALTER TABLE answers ADD COLUMN IF NOT EXISTS answer_structure TEXT;
                ALTER TABLE answers ADD COLUMN IF NOT EXISTS missing_evidence JSONB NOT NULL DEFAULT '[]'::jsonb;
                ALTER TABLE answers ADD COLUMN IF NOT EXISTS stronger_phrasing JSONB NOT NULL DEFAULT '[]'::jsonb;
                ALTER TABLE answers ADD COLUMN IF NOT EXISTS coaching_breakdown JSONB NOT NULL DEFAULT '{}'::jsonb;

                -- Remove duplicate answers before adding the unique constraint
                DELETE FROM answers
                WHERE id IN (
                    SELECT id FROM (
                        SELECT id,
                               ROW_NUMBER() OVER (
                                   PARTITION BY session_id, question_id
                                   ORDER BY created_at DESC NULLS LAST, id DESC
                               ) AS rn
                        FROM answers
                    ) ranked
                    WHERE rn > 1
                );

                DO $$ BEGIN
                    IF NOT EXISTS (
                        SELECT 1 FROM pg_constraint
                        WHERE conname = 'answers_session_question_unique'
                    ) THEN
                        ALTER TABLE answers ADD CONSTRAINT answers_session_question_unique
                            UNIQUE (session_id, question_id);
                    END IF;
                END $$;

                CREATE TABLE IF NOT EXISTS user_entitlements (
                    user_id UUID PRIMARY KEY,
                    plan VARCHAR(40) NOT NULL DEFAULT 'free',
                    is_premium BOOLEAN NOT NULL DEFAULT FALSE,
                    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                );

                CREATE TABLE IF NOT EXISTS user_devices (
                    user_id UUID NOT NULL,
                    device_id TEXT NOT NULL,
                    first_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    PRIMARY KEY (user_id, device_id)
                );

                CREATE INDEX IF NOT EXISTS idx_user_devices_user_last_seen
                    ON user_devices(user_id, last_seen_at DESC);

                CREATE TABLE IF NOT EXISTS app_events (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    user_id UUID,
                    session_id UUID NULL,
                    name VARCHAR(80) NOT NULL,
                    properties JSONB NOT NULL DEFAULT '{}'::jsonb,
                    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                );

                CREATE INDEX IF NOT EXISTS idx_app_events_user_created
                    ON app_events(user_id, created_at DESC);
                CREATE INDEX IF NOT EXISTS idx_app_events_name_created
                    ON app_events(name, created_at DESC);
                CREATE INDEX IF NOT EXISTS idx_sessions_user_created
                    ON sessions(user_id, created_at DESC);
                CREATE INDEX IF NOT EXISTS idx_sessions_user_status_created
                    ON sessions(user_id, status, created_at DESC);
                CREATE INDEX IF NOT EXISTS idx_answers_session_question
                    ON answers(session_id, question_id ASC);

                CREATE TABLE IF NOT EXISTS user_practice_goals (
                    user_id UUID PRIMARY KEY,
                    daily_minutes INTEGER NOT NULL DEFAULT 10,
                    interview_date DATE,
                    target_role TEXT,
                    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                );

                CREATE TABLE IF NOT EXISTS user_resume_profiles (
                    user_id UUID PRIMARY KEY,
                    file_name TEXT,
                    source_type TEXT NOT NULL DEFAULT 'file',
                    summary TEXT NOT NULL DEFAULT '',
                    skills JSONB NOT NULL DEFAULT '[]'::jsonb,
                    experience JSONB NOT NULL DEFAULT '[]'::jsonb,
                    education JSONB NOT NULL DEFAULT '[]'::jsonb,
                    raw_text_excerpt TEXT,
                    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                );
            """)
            print("[Neon] Migrations applied successfully")
        except Exception as e:
            print(f"[Neon] Migration warning (may be safe to ignore): {e}")

    async def disconnect(self):
        if self.pool:
            await self.pool.close()

    # ─── Personas ───────────────────────────────────────────────

    async def get_personas(self) -> List[Dict]:
        if not self.pool:
            return []
        rows = await self.pool.fetch("SELECT * FROM personas ORDER BY name")
        return [dict(r) for r in rows]

    async def get_persona(self, persona_id: str) -> Optional[Dict]:
        if not self.pool:
            return None
        row = await self.pool.fetchrow("SELECT * FROM personas WHERE id = $1", persona_id)
        return dict(row) if row else None

    # ─── Sessions ───────────────────────────────────────────────

    async def create_session(self, session_data: dict) -> Dict:
        if not self.pool:
            raise RuntimeError("Database is unavailable")
        
        import uuid as uuid_mod
        # Cast user_id string to UUID for asyncpg compatibility
        user_id = session_data["user_id"]
        if isinstance(user_id, str):
            user_id = uuid_mod.UUID(user_id)
        
        try:
            row = await self.pool.fetchrow(
                """
                INSERT INTO sessions (
                    user_id, role_title, industry, seniority, interview_type, 
                    difficulty, length, panel_config, status,
                    company_name, company_website, company_context, language, question_count
                )
                VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14)
                RETURNING id, user_id, role_title, status, created_at
                """,
                user_id,
                session_data["role_title"],
                session_data["industry"],
                session_data["seniority"],
                session_data["interview_type"],
                session_data["difficulty"],
                session_data["length"],
                json.dumps(session_data.get("panel_config", [])),
                session_data.get("status", "in_progress"),
                session_data.get("company_name"),
                session_data.get("company_website"),
                session_data.get("company_context"),
                session_data.get("language", "en"),
                int(session_data.get("question_count") or 0),
            )
            return dict(row)
        except Exception as e:
            print(f"[Neon] Error creating session: {e}")
            raise

    async def get_session(self, session_id: str) -> Optional[Dict]:
        if not self.pool:
            return None
        row = await self.pool.fetchrow("SELECT * FROM sessions WHERE id = $1", session_id)
        return dict(row) if row else None

    async def add_notification(self, user_id: str, title: str, message: str, type: str = 'info'):
        if not self.pool:
            return
        await self.pool.execute(
            "INSERT INTO notifications (user_id, title, message, type) VALUES ($1, $2, $3, $4)",
            user_id, title, message, type
        )

    async def refresh_session_score(self, session_id: str) -> int:
        if not self.pool:
            return 0

        avg_score = await self.pool.fetchval(
            """
            SELECT COALESCE(
                SUM(CASE
                    WHEN a.score > 0 AND NULLIF(TRIM(COALESCE(a.transcript, '')), '') IS NOT NULL
                    THEN a.score ELSE 0
                END)::float
                / GREATEST(
                    COALESCE(NULLIF(s.question_count, 0), 0),
                    CASE
                        WHEN s.length = 'quick' THEN 4
                        WHEN s.length = 'deep' THEN 16
                        ELSE 9
                    END,
                    COUNT(a.id),
                    1
                ),
                0
            )
            FROM sessions s
            LEFT JOIN answers a ON a.session_id = s.id
            WHERE s.id = $1
            GROUP BY s.id, s.question_count, s.length
            """,
            session_id,
        )
        score = round(avg_score or 0)
        await self.pool.execute(
            "UPDATE sessions SET score = $1 WHERE id = $2",
            score,
            session_id,
        )
        return score

    async def get_user_sessions_for_list(self, user_id: str, limit: int = 20) -> List[Dict]:
        if not self.pool:
            return []

        rows = await self.pool.fetch(
            """
            SELECT
                s.id,
                s.role_title,
                s.interview_type,
                s.status,
                s.created_at,
                s.question_count,
                s.length,
                COALESCE(
                    SUM(
                        CASE
                            WHEN a.score > 0
                                 AND NULLIF(TRIM(COALESCE(a.transcript, '')), '') IS NOT NULL
                            THEN a.score
                            ELSE 0
                        END
                    ),
                    0
                ) AS total_score,
                COUNT(a.id) FILTER (
                    WHERE a.score > 0
                      AND NULLIF(TRIM(COALESCE(a.transcript, '')), '') IS NOT NULL
                ) AS usable_answers,
                COUNT(a.id) AS answer_count
            FROM sessions s
            LEFT JOIN answers a ON a.session_id = s.id
            WHERE s.user_id = $1
            GROUP BY s.id
            ORDER BY s.created_at DESC
            LIMIT $2
            """,
            user_id,
            limit,
        )

        results: List[Dict] = []
        for row in rows:
            item = dict(row)
            expected = _expected_question_count_from_session(item, int(item.get("answer_count") or 0))
            score = round(
                _coverage_adjusted_score(
                    float(item.get("total_score") or 0),
                    int(item.get("usable_answers") or 0),
                    expected,
                )
            )
            item["computed_score"] = score
            item["answered"] = int(item.get("usable_answers") or 0)
            item["total"] = expected
            results.append(item)
        return results

    async def complete_session(self, session_id: str):
        if not self.pool:
            return

        avg_score = await self.refresh_session_score(session_id)
        
        await self.pool.execute(
            "UPDATE sessions SET status = 'completed', completed_at = NOW() WHERE id = $1",
            session_id,
        )

        # Trigger Notification if it's a high score
        if avg_score and avg_score >= 85:
            user_id = await self.pool.fetchval("SELECT user_id FROM sessions WHERE id = $1", session_id)
            if user_id:
                await self.add_notification(
                    str(user_id),
                    "High Performance Alert",
                    f"Incredible! You achieved a score of {round(avg_score)}% in your last session.",
                    "score"
                )

    async def get_user_sessions(self, user_id: str, limit: int = 20) -> List[Dict]:
        if not self.pool:
            return []
        rows = await self.pool.fetch(
            "SELECT *, score FROM sessions WHERE user_id = $1 ORDER BY created_at DESC LIMIT $2",
            user_id, limit
        )
        return [dict(r) for r in rows]

    async def delete_user_session(self, user_id: str, session_id: str) -> bool:
        if not self.pool:
            return False

        row = await self.pool.fetchrow(
            "SELECT id FROM sessions WHERE id = $1 AND user_id = $2",
            session_id,
            user_id,
        )
        if not row:
            return False

        await self.pool.execute("DELETE FROM answers WHERE session_id = $1", session_id)
        await self.pool.execute("DELETE FROM sessions WHERE id = $1 AND user_id = $2", session_id, user_id)
        return True

    async def is_user_premium(self, user_id: str, email: str | None = None) -> bool:
        if has_unlimited_access(email):
            return True
        if not self.pool:
            return False
        try:
            row = await self.pool.fetchrow(
                """
                SELECT is_premium, plan
                FROM user_entitlements
                WHERE user_id = $1
                """,
                user_id
            )
            if not row:
                return False
            return bool(row["is_premium"]) or str(row["plan"] or "").lower() in {"premium", "pro", "paid"}
        except Exception as exc:
            print(f"[Neon] Premium entitlement check failed: {exc}")
            return False

    async def verify_device_allowance(self, user_id: str, device_id: str, max_devices: int = 2) -> bool:
        if not self.pool or not device_id:
            return True

        import uuid as uuid_mod
        try:
            user_uuid = uuid_mod.UUID(str(user_id))
        except (TypeError, ValueError):
            return False

        existing = await self.pool.fetchval(
            """
            SELECT 1
            FROM user_devices
            WHERE user_id = $1 AND device_id = $2
            """,
            user_uuid,
            device_id,
        )
        if existing:
            await self.pool.execute(
                """
                UPDATE user_devices
                SET last_seen_at = NOW()
                WHERE user_id = $1 AND device_id = $2
                """,
                user_uuid,
                device_id,
            )
            return True

        device_count = await self.pool.fetchval(
            "SELECT COUNT(*) FROM user_devices WHERE user_id = $1",
            user_uuid,
        )
        if int(device_count or 0) >= max_devices:
            # Replace the least recently seen device so reinstalls on the same
            # physical phone (new local app identity) don't deadlock the user.
            await self.pool.execute(
                """
                DELETE FROM user_devices
                WHERE user_id = $1
                  AND device_id IN (
                    SELECT device_id
                    FROM user_devices
                    WHERE user_id = $1
                    ORDER BY last_seen_at ASC
                    LIMIT 1
                  )
                """,
                user_uuid,
            )

        await self.pool.execute(
            """
            INSERT INTO user_devices (user_id, device_id)
            VALUES ($1, $2)
            ON CONFLICT (user_id, device_id)
            DO UPDATE SET last_seen_at = NOW()
            """,
            user_uuid,
            device_id,
        )
        return True

    # ─── Answers ────────────────────────────────────────────────

    async def save_answer(self, answer_data: dict) -> Dict:
        if not self.pool:
            raise RuntimeError("Database is unavailable")
        
        # Defensive defaults for missing intelligence data
        transcript = answer_data.get("transcript", "")
        score = answer_data.get("score", 0)
        
        # Handle "Silent" or "Technical Issue" case
        if not transcript and score == 0:
            feedback = "No voice intelligence detected. Please check your tactical audio link configuration for the next question."
        else:
            feedback = answer_data.get("feedback", "")

        row = await self.pool.fetchrow(
            """
            WITH deleted AS (
                DELETE FROM answers
                WHERE session_id = $1 AND question_id = $2
                RETURNING id
            )
            INSERT INTO answers (
                session_id, question_id, transcript, score, clarity, pacing, impact,
                confidence, knowledge, feedback, tips, improved_answer, answer_structure,
                missing_evidence, stronger_phrasing, coaching_breakdown
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16)
            RETURNING id
            """,
            answer_data["session_id"],
            answer_data["question_id"],
            transcript,
            score,
            answer_data.get("clarity", 0),
            answer_data.get("pacing", 0),
            answer_data.get("impact", 0),
            answer_data.get("confidence", 0),
            answer_data.get("knowledge", 0),
            feedback,
            json.dumps(_json_list(answer_data.get("tips", []))),
            answer_data.get("improved_answer", ""),
            answer_data.get("answer_structure", ""),
            json.dumps(_json_list(answer_data.get("missing_evidence", []))),
            json.dumps(_json_list(answer_data.get("stronger_phrasing", []))),
            json.dumps(answer_data.get("coaching_breakdown") or {})
        )
        return dict(row)

    def _normalize_answer_row(self, row) -> Dict:
        answer = dict(row)
        answer["tips"] = _json_list(answer.get("tips"))
        answer["missing_evidence"] = _json_list(answer.get("missing_evidence"))
        answer["stronger_phrasing"] = _json_list(answer.get("stronger_phrasing"))
        coaching_breakdown = answer.get("coaching_breakdown")
        if isinstance(coaching_breakdown, str):
            try:
                coaching_breakdown = json.loads(coaching_breakdown)
            except json.JSONDecodeError:
                coaching_breakdown = {}
        answer["coaching_breakdown"] = coaching_breakdown if isinstance(coaching_breakdown, dict) else {}
        return answer

    async def get_saved_answers(self, user_id: str, limit: int = 30) -> List[Dict]:
        if not self.pool:
            return []
        rows = await self.pool.fetch(
            """
            SELECT
                a.id,
                a.question_id,
                a.transcript,
                a.score,
                a.feedback,
                a.improved_answer,
                a.answer_structure,
                a.missing_evidence,
                a.stronger_phrasing,
                a.coaching_breakdown,
                s.role_title,
                s.industry,
                s.created_at
            FROM answers a
            JOIN sessions s ON a.session_id = s.id
            WHERE s.user_id = $1 AND COALESCE(a.improved_answer, '') <> ''
            ORDER BY s.created_at DESC, a.question_id ASC
            LIMIT $2
            """,
            user_id,
            limit,
        )
        return [self._normalize_answer_row(r) for r in rows]

    async def get_practice_goal(self, user_id: str) -> Dict:
        if not self.pool:
            return {"daily_minutes": 10, "interview_date": None, "target_role": None}
        row = await self.pool.fetchrow(
            "SELECT daily_minutes, interview_date, target_role FROM user_practice_goals WHERE user_id = $1",
            user_id,
        )
        if not row:
            return {"daily_minutes": 10, "interview_date": None, "target_role": None}
        return {
            "daily_minutes": row["daily_minutes"],
            "interview_date": row["interview_date"].isoformat() if row["interview_date"] else None,
            "target_role": row["target_role"],
        }

    async def update_practice_goal(self, user_id: str, goal: dict) -> Dict:
        if not self.pool:
            return goal
        from datetime import date
        interview_date = goal.get("interview_date")
        if isinstance(interview_date, str) and interview_date:
            interview_date = date.fromisoformat(interview_date[:10])
        elif not interview_date:
            interview_date = None
        row = await self.pool.fetchrow(
            """
            INSERT INTO user_practice_goals (user_id, daily_minutes, interview_date, target_role)
            VALUES ($1, $2, $3, $4)
            ON CONFLICT (user_id)
            DO UPDATE SET
                daily_minutes = EXCLUDED.daily_minutes,
                interview_date = EXCLUDED.interview_date,
                target_role = EXCLUDED.target_role,
                updated_at = NOW()
            RETURNING daily_minutes, interview_date, target_role
            """,
            user_id,
            int(goal.get("daily_minutes") or 10),
            interview_date,
            goal.get("target_role"),
        )
        return {
            "daily_minutes": row["daily_minutes"],
            "interview_date": row["interview_date"].isoformat() if row["interview_date"] else None,
            "target_role": row["target_role"],
        }

    # ─── Progress Aggregation ───────────────────────────────────

    async def get_resume_profile(self, user_id: str) -> Optional[Dict]:
        if not self.pool:
            return None
        import uuid as uuid_mod
        user_uuid = uuid_mod.UUID(str(user_id))
        row = await self.pool.fetchrow(
            """
            SELECT user_id, file_name, source_type, summary, skills, experience,
                   education, raw_text_excerpt, updated_at
            FROM user_resume_profiles
            WHERE user_id = $1
            """,
            user_uuid,
        )
        if not row:
            return None
        profile = dict(row)
        profile["user_id"] = str(profile["user_id"])
        profile["skills"] = _json_list(profile.get("skills"))
        profile["experience"] = _json_list(profile.get("experience"))
        profile["education"] = _json_list(profile.get("education"))
        profile["updated_at"] = profile["updated_at"].isoformat() if profile.get("updated_at") else None
        return profile

    async def upsert_resume_profile(self, user_id: str, profile: dict) -> Dict:
        if not self.pool:
            raise RuntimeError("Database is unavailable")
        import uuid as uuid_mod
        user_uuid = uuid_mod.UUID(str(user_id))
        row = await self.pool.fetchrow(
            """
            INSERT INTO user_resume_profiles (
                user_id, file_name, source_type, summary, skills, experience,
                education, raw_text_excerpt, updated_at
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, NOW())
            ON CONFLICT (user_id)
            DO UPDATE SET
                file_name = EXCLUDED.file_name,
                source_type = EXCLUDED.source_type,
                summary = EXCLUDED.summary,
                skills = EXCLUDED.skills,
                experience = EXCLUDED.experience,
                education = EXCLUDED.education,
                raw_text_excerpt = EXCLUDED.raw_text_excerpt,
                updated_at = NOW()
            RETURNING user_id, file_name, source_type, summary, skills, experience,
                      education, raw_text_excerpt, updated_at
            """,
            user_uuid,
            profile.get("file_name"),
            profile.get("source_type", "file"),
            profile.get("summary", ""),
            json.dumps(_json_list(profile.get("skills"))),
            json.dumps(_json_list(profile.get("experience"))),
            json.dumps(_json_list(profile.get("education"))),
            profile.get("raw_text_excerpt"),
        )
        saved = dict(row)
        saved["user_id"] = str(saved["user_id"])
        saved["skills"] = _json_list(saved.get("skills"))
        saved["experience"] = _json_list(saved.get("experience"))
        saved["education"] = _json_list(saved.get("education"))
        saved["updated_at"] = saved["updated_at"].isoformat() if saved.get("updated_at") else None
        return saved

    async def delete_resume_profile(self, user_id: str) -> bool:
        if not self.pool:
            return False
        import uuid as uuid_mod
        user_uuid = uuid_mod.UUID(str(user_id))
        result = await self.pool.execute(
            "DELETE FROM user_resume_profiles WHERE user_id = $1",
            user_uuid,
        )
        return result.endswith(" 1")

    async def get_user_progress(self, user_id: str) -> Optional[Dict]:
        if not self.pool:
            return None

        try:
            # Score progress from completed sessions, with skipped questions
            # counted as zero signal. Skill dimensions still use only answers
            # that have a real transcript and score.
            row = await self.pool.fetchrow(
                """
                WITH session_scores AS (
                    SELECT
                        s.id,
                        GREATEST(
                            COALESCE(NULLIF(s.question_count, 0), 0),
                            CASE
                                WHEN s.length = 'quick' THEN 4
                                WHEN s.length = 'deep' THEN 16
                                ELSE 9
                            END,
                            COUNT(a.id),
                            1
                        ) AS expected_questions,
                        COUNT(a.id) FILTER (
                            WHERE a.score > 0
                              AND NULLIF(TRIM(COALESCE(a.transcript, '')), '') IS NOT NULL
                        ) AS usable_answers,
                        COALESCE(SUM(a.score) FILTER (
                            WHERE a.score > 0
                              AND NULLIF(TRIM(COALESCE(a.transcript, '')), '') IS NOT NULL
                        ), 0) AS total_score,
                        AVG(a.clarity) FILTER (
                            WHERE a.score > 0
                              AND NULLIF(TRIM(COALESCE(a.transcript, '')), '') IS NOT NULL
                        ) AS avg_clarity,
                        AVG(a.pacing) FILTER (
                            WHERE a.score > 0
                              AND NULLIF(TRIM(COALESCE(a.transcript, '')), '') IS NOT NULL
                        ) AS avg_pacing,
                        AVG(a.impact) FILTER (
                            WHERE a.score > 0
                              AND NULLIF(TRIM(COALESCE(a.transcript, '')), '') IS NOT NULL
                        ) AS avg_impact,
                        AVG(a.confidence) FILTER (
                            WHERE a.score > 0
                              AND NULLIF(TRIM(COALESCE(a.transcript, '')), '') IS NOT NULL
                        ) AS avg_confidence,
                        AVG(a.knowledge) FILTER (
                            WHERE a.score > 0
                              AND NULLIF(TRIM(COALESCE(a.transcript, '')), '') IS NOT NULL
                        ) AS avg_knowledge
                    FROM sessions s
                    LEFT JOIN answers a ON a.session_id = s.id
                    WHERE s.user_id = $1 AND s.status = 'completed'
                    GROUP BY s.id, s.question_count, s.length
                )
                SELECT 
                    COALESCE(SUM(usable_answers), 0) as total_answers,
                    COALESCE(AVG(total_score::float / expected_questions), 0) as avg_score,
                    COALESCE(AVG(avg_clarity) FILTER (WHERE usable_answers > 0), 0) as avg_clarity,
                    COALESCE(AVG(avg_pacing) FILTER (WHERE usable_answers > 0), 0) as avg_pacing,
                    COALESCE(AVG(avg_impact) FILTER (WHERE usable_answers > 0), 0) as avg_impact,
                    COALESCE(AVG(avg_confidence) FILTER (WHERE usable_answers > 0), 0) as avg_confidence,
                    COALESCE(AVG(avg_knowledge) FILTER (WHERE usable_answers > 0), 0) as avg_knowledge,
                    COUNT(id) as sessions_count
                FROM session_scores
                """,
                user_id
            )

            if not row or row["sessions_count"] == 0:
                return None

            # Get recent completed sessions with their avg scores
            recent_rows = await self.pool.fetch(
                """
                SELECT
                    s.id,
                    s.role_title,
                    s.created_at,
                    COALESCE(
                        SUM(CASE
                            WHEN a.score > 0 AND NULLIF(TRIM(COALESCE(a.transcript, '')), '') IS NOT NULL
                            THEN a.score ELSE 0
                        END)::float / GREATEST(
                            COALESCE(NULLIF(s.question_count, 0), 0),
                            CASE
                                WHEN s.length = 'quick' THEN 4
                                WHEN s.length = 'deep' THEN 16
                                ELSE 9
                            END,
                            COUNT(a.id),
                            1
                        ),
                        0
                    ) as avg_score
                FROM sessions s
                LEFT JOIN answers a ON a.session_id = s.id
                WHERE s.user_id = $1 AND s.status = 'completed'
                GROUP BY s.id, s.role_title, s.created_at, s.question_count, s.length
                ORDER BY s.created_at DESC
                LIMIT 5
                """,
                user_id
            )

            recent = [{
                "id": str(r["id"]),
                "score": round(float(r["avg_score"])),
                "role": r["role_title"] or "Interview",
                "date": r["created_at"].strftime("%Y-%m-%d") if r["created_at"] else ""
            } for r in recent_rows]

            trend_rows = await self.pool.fetch(
                """
                SELECT
                    s.id,
                    s.role_title,
                    s.interview_type,
                    s.created_at,
                    COALESCE(
                        SUM(CASE
                            WHEN a.score > 0 AND NULLIF(TRIM(COALESCE(a.transcript, '')), '') IS NOT NULL
                            THEN a.score ELSE 0
                        END)::float / GREATEST(
                            COALESCE(NULLIF(s.question_count, 0), 0),
                            CASE
                                WHEN s.length = 'quick' THEN 4
                                WHEN s.length = 'deep' THEN 16
                                ELSE 9
                            END,
                            COUNT(a.id),
                            1
                        ),
                        0
                    ) as avg_score,
                    COALESCE(AVG(a.clarity) FILTER (
                        WHERE a.score > 0 AND NULLIF(TRIM(COALESCE(a.transcript, '')), '') IS NOT NULL
                    ), 0) as clarity,
                    COALESCE(AVG(a.pacing) FILTER (
                        WHERE a.score > 0 AND NULLIF(TRIM(COALESCE(a.transcript, '')), '') IS NOT NULL
                    ), 0) as pacing,
                    COALESCE(AVG(a.impact) FILTER (
                        WHERE a.score > 0 AND NULLIF(TRIM(COALESCE(a.transcript, '')), '') IS NOT NULL
                    ), 0) as impact,
                    COALESCE(AVG(a.confidence) FILTER (
                        WHERE a.score > 0 AND NULLIF(TRIM(COALESCE(a.transcript, '')), '') IS NOT NULL
                    ), 0) as confidence,
                    COALESCE(AVG(a.knowledge) FILTER (
                        WHERE a.score > 0 AND NULLIF(TRIM(COALESCE(a.transcript, '')), '') IS NOT NULL
                    ), 0) as knowledge,
                    COUNT(a.id) FILTER (
                        WHERE a.score > 0 AND NULLIF(TRIM(COALESCE(a.transcript, '')), '') IS NOT NULL
                    ) as answer_count
                FROM sessions s
                LEFT JOIN answers a ON a.session_id = s.id
                WHERE s.user_id = $1 AND s.status = 'completed'
                GROUP BY s.id, s.role_title, s.interview_type, s.created_at, s.question_count, s.length
                ORDER BY s.created_at ASC
                LIMIT 20
                """,
                user_id
            )

            session_trend = [{
                "id": str(r["id"]),
                "date": r["created_at"].strftime("%Y-%m-%d") if r["created_at"] else "",
                "role": r["role_title"] or "Interview",
                "type": r["interview_type"] or "interview",
                "score": round(float(r["avg_score"])),
                "skills": {
                    "clarity": round(float(r["clarity"])),
                    "pacing": round(float(r["pacing"])),
                    "impact": round(float(r["impact"])),
                    "confidence": round(float(r["confidence"])),
                    "knowledge": round(float(r["knowledge"])),
                },
                "answer_count": int(r["answer_count"] or 0),
            } for r in trend_rows]

            first_score = session_trend[0]["score"] if session_trend else round(float(row["avg_score"]))
            latest_score = session_trend[-1]["score"] if session_trend else round(float(row["avg_score"]))
            previous_score = session_trend[-2]["score"] if len(session_trend) > 1 else first_score
            best_score = max([s["score"] for s in session_trend], default=latest_score)
            improvement_from_first = latest_score - first_score
            improvement_from_previous = latest_score - previous_score

            dimensions = {
                "clarity": round(float(row["avg_clarity"])),
                "pacing": round(float(row["avg_pacing"])),
                "impact": round(float(row["avg_impact"])),
                "confidence": round(float(row["avg_confidence"])),
                "knowledge": round(float(row["avg_knowledge"])),
            }
            weakest_key = min(dimensions, key=dimensions.get)
            strongest_key = max(dimensions, key=dimensions.get)

            weak_answer_rows = await self.pool.fetch(
                """
                SELECT a.score, a.feedback, a.tips, s.role_title, s.created_at
                FROM answers a
                JOIN sessions s ON a.session_id = s.id
                WHERE s.user_id = $1
                  AND s.status = 'completed'
                  AND a.score < 60
                  AND a.score > 0
                  AND NULLIF(TRIM(COALESCE(a.transcript, '')), '') IS NOT NULL
                ORDER BY s.created_at DESC
                LIMIT 8
                """,
                user_id
            )
            repeated_weaknesses = []
            if dimensions["impact"] < 65:
                repeated_weaknesses.append("Add measurable outcomes and business impact.")
            if dimensions["clarity"] < 65:
                repeated_weaknesses.append("Use a clearer STAR structure with a concise ending.")
            if dimensions["knowledge"] < 65:
                repeated_weaknesses.append("Show deeper role-specific tradeoffs and domain context.")
            if dimensions["confidence"] < 65:
                repeated_weaknesses.append("Answer directly before explaining details.")
            if dimensions["pacing"] < 65:
                repeated_weaknesses.append("Keep answers tighter and avoid rambling.")
            for weak in weak_answer_rows[:3]:
                feedback = str(weak["feedback"] or "").strip()
                if feedback and feedback not in repeated_weaknesses:
                    repeated_weaknesses.append(feedback[:140])

            coaching_plan = [
                f"Next session focus: {weakest_key}.",
                "Retry one weak question and improve the same answer before moving on.",
                "Include one concrete metric, tradeoff, or outcome in every answer.",
            ]
            if weakest_key == "pacing":
                coaching_plan[2] = "Keep each answer under 90 seconds with one clear example."
            elif weakest_key == "knowledge":
                coaching_plan[2] = "Explain why your approach fits the role, company, and constraints."
            elif weakest_key == "confidence":
                coaching_plan[2] = "Start with a direct answer, then support it with evidence."

            if latest_score >= 90:
                readiness = "Elite"
            elif latest_score >= 82:
                readiness = "Strong Candidate"
            elif latest_score >= 72:
                readiness = "Interview Ready"
            elif latest_score >= 45:
                readiness = "Needs Practice"
            elif latest_score > 0:
                readiness = "Not Ready"
            else:
                readiness = "No Baseline"

            progress = {
                "avg_score": round(float(row["avg_score"])),
                "clarity": dimensions["clarity"],
                "pacing": dimensions["pacing"],
                "impact": dimensions["impact"],
                "confidence": dimensions["confidence"],
                "knowledge": dimensions["knowledge"],
                "sessions_count": row["sessions_count"],
                "answers_count": row["total_answers"],
                "recent_sessions": recent,
                "session_trend": session_trend,
                "improvement": {
                    "first_score": first_score,
                    "latest_score": latest_score,
                    "best_score": best_score,
                    "from_first": improvement_from_first,
                    "from_previous": improvement_from_previous,
                    "is_best_session": latest_score >= best_score and len(session_trend) > 1,
                },
                "readiness": {
                    "label": readiness,
                    "score": latest_score,
                },
                "skill_focus": {
                    "strongest": strongest_key,
                    "strongest_score": dimensions[strongest_key],
                    "weakest": weakest_key,
                    "weakest_score": dimensions[weakest_key],
                },
                "repeated_weaknesses": repeated_weaknesses[:5],
                "coaching_plan": coaching_plan,
            }

            # Check for streak milestone
            if progress["sessions_count"] > 0 and progress["sessions_count"] % 3 == 0:
                await self.add_notification(
                    user_id,
                    "Streak Milestone",
                    f"You've completed {progress['sessions_count']} sessions! Your tactical consistency is improving.",
                    "streak"
                )
                
            return progress
        except Exception as e:
            print(f"[Neon] Error aggregating progress: {e}")
            return None


    async def get_session_detail(self, session_id: str) -> Optional[Dict]:
        if not self.pool:
            return None

        # 1. Get session info
        session = await self.get_session(session_id)
        if not session:
            return None

        # 2. Get answers with scores
        answer_rows = await self.pool.fetch(
            "SELECT * FROM answers WHERE session_id = $1 ORDER BY question_id ASC",
            session_id
        )
        answers = [self._normalize_answer_row(row) for row in answer_rows]

        # 3. Calculate session analytics. Ignore unusable zero-score rows from
        # older builds so the report does not present placeholder strengths.
        total_answers = len(answers)
        scorable_answers = [
            r for r in answers
            if r.get("score", 0) > 0 and str(r.get("transcript") or "").strip()
        ]
        scorable_count = len(scorable_answers)
        analytics_answers = scorable_answers or answers
        expected_questions = _expected_question_count_from_session(session, total_answers)

        if scorable_count > 0:
            avg_score = _coverage_adjusted_score(
                sum(r["score"] for r in scorable_answers),
                scorable_count,
                expected_questions,
            )
            avg_clarity = sum(r["clarity"] for r in analytics_answers) / scorable_count
            avg_pacing = sum(r["pacing"] for r in analytics_answers) / scorable_count
            avg_impact = sum(r["impact"] for r in analytics_answers) / scorable_count
            avg_confidence = sum(r["confidence"] for r in analytics_answers) / scorable_count
            avg_knowledge = sum(r["knowledge"] for r in analytics_answers) / scorable_count
        else:
            avg_score = avg_clarity = avg_pacing = avg_impact = avg_confidence = avg_knowledge = 0

        metric_scores = {
            "Clarity": round(avg_clarity),
            "Pacing": round(avg_pacing),
            "Impact": round(avg_impact),
            "Confidence": round(avg_confidence),
            "Knowledge": round(avg_knowledge),
        }
        has_signal = scorable_count > 0
        weakest_metric = min(metric_scores, key=metric_scores.get) if has_signal else None
        strongest_metric = max(metric_scores, key=metric_scores.get) if has_signal else None
        low_answers = [r for r in scorable_answers if r["score"] < 60]
        strong_answers = [r for r in scorable_answers if r["score"] >= 75]

        coaching_plan = []
        if not has_signal:
            coaching_plan = [
                "Retry this interview with the microphone close to your mouth.",
                "Speak for at least 30 seconds before tapping Finish.",
                "Check that the transcript appears after each answer before continuing.",
            ]
        else:
            coaching_plan = [
                f"Protect your {strongest_metric.lower()} strength by keeping answers structured and concise.",
                f"Focus the next session on {weakest_metric.lower()}; this is the lowest skill in this report.",
                "For every answer, state the problem, your decision, the tradeoff, and the measurable result.",
            ]
            if low_answers:
                coaching_plan.append(f"Retry question {low_answers[0]['question_id']} first; it had the clearest improvement signal.")

        return {
            "id": str(session["id"]),
            "user_id": str(session["user_id"]),
            "role_title": session["role_title"],
            "industry": session["industry"],
            "status": session["status"],
            "date": session["created_at"].strftime("%Y-%m-%d") if session["created_at"] else "",
            "time": session["created_at"].strftime("%H:%M") if session["created_at"] else "",
            "duration": session.get("length", "15"),
            "analytics": {
                "radar_data": [
                    {"label": "Clarity", "value": round(avg_clarity)},
                    {"label": "Pacing", "value": round(avg_pacing)},
                    {"label": "Impact", "value": round(avg_impact)},
                    {"label": "Confidence", "value": round(avg_confidence)},
                    {"label": "Knowledge", "value": round(avg_knowledge)},
                ],
                "metrics": {
                    "clarity": round(avg_clarity),
                    "pace": round(avg_pacing),
                    "impact": round(avg_impact),
                    "confidence": round(avg_confidence),
                    "knowledge": round(avg_knowledge),
                }
            },
            "answers": answers,
            "feedback": {
                "summary": (
                    f"Average score {round(avg_score)}. Strongest skill: {strongest_metric}. Highest-leverage focus: {weakest_metric}."
                    if has_signal
                    else "Not enough usable speech was captured to produce a real coaching summary. This report is showing setup guidance instead of skill rankings."
                ),
                "strengths": [r["feedback"] for r in strong_answers][:3],
                "growth_areas": [r["feedback"] for r in low_answers][:3],
                "coaching_plan": coaching_plan,
                "strongest_metric": strongest_metric,
                "weakest_metric": weakest_metric,
                "has_signal": has_signal,
                "scorable_answers": scorable_count,
            },
            "score_band": self._score_band(round(avg_score)),
        }

    def _score_band(self, score: int) -> Dict:
        if score >= 85:
            return {"label": "Interview-ready", "description": "Strong evidence, structure, and role fit."}
        if score >= 70:
            return {"label": "Competitive", "description": "Good base, but tighten evidence and tradeoffs."}
        if score >= 55:
            return {"label": "Developing", "description": "Understandable answers, but not enough proof yet."}
        if score >= 35:
            return {"label": "Needs practice", "description": "Answers are too shallow, vague, or inconsistent."}
        return {"label": "Not enough signal", "description": "The answer quality or recording was insufficient to score confidently."}

    async def create_feedback(self, user_id: str, session_id: Optional[str], rating: int, comment: str):
        if not self.pool:
            return None
        
        return await self.pool.fetchrow(
            "INSERT INTO feedback (user_id, session_id, rating, comment) VALUES ($1, $2, $3, $4) RETURNING *",
            user_id, session_id, rating, comment
        )

    async def track_event(self, event_data: dict):
        if not self.pool:
            print(f"[Analytics] {event_data}")
            return None

        import uuid as uuid_mod
        user_id = event_data.get("user_id")
        session_id = event_data.get("session_id")
        occurred_at = event_data.get("occurred_at")

        try:
            user_uuid = uuid_mod.UUID(user_id) if user_id else None
            session_uuid = uuid_mod.UUID(session_id) if session_id else None
        except (TypeError, ValueError):
            user_uuid = None
            session_uuid = None

        if isinstance(occurred_at, str):
            try:
                occurred_at = datetime.fromisoformat(occurred_at.replace("Z", "+00:00"))
            except ValueError:
                occurred_at = None
        if occurred_at is None:
            occurred_at = datetime.now(timezone.utc)

        return await self.pool.fetchrow(
            """
            INSERT INTO app_events (user_id, session_id, name, properties, occurred_at)
            VALUES ($1, $2, $3, $4, COALESCE($5::timestamptz, NOW()))
            RETURNING id
            """,
            user_uuid,
            session_uuid,
            event_data["name"],
            json.dumps(event_data.get("properties", {})),
            occurred_at,
        )

    async def get_operational_metrics(self, hours: int = 24) -> Dict:
        if not self.pool:
            return {
                "window_hours": hours,
                "events": {},
                "api_errors": 0,
                "tts_failures": 0,
                "ai_provider_errors": 0,
                "client_crashes": 0,
                "estimated_ai_calls": 0,
                "estimated_tts_chars": 0,
            }

        rows = await self.pool.fetch(
            """
            SELECT name, COUNT(*)::int AS count
            FROM app_events
            WHERE created_at >= NOW() - ($1::int * INTERVAL '1 hour')
            GROUP BY name
            ORDER BY count DESC
            """,
            hours,
        )
        tts_chars = await self.pool.fetchval(
            """
            SELECT COALESCE(SUM((properties->>'chars')::int), 0)::int
            FROM app_events
            WHERE name = 'api_tts_synthesized'
              AND properties ? 'chars'
              AND created_at >= NOW() - ($1::int * INTERVAL '1 hour')
            """,
            hours,
        )
        provider_errors = await self.pool.fetchval(
            """
            SELECT COUNT(*)::int
            FROM app_events
            WHERE name IN ('api_ai_provider_error', 'api_tts_failed')
              AND created_at >= NOW() - ($1::int * INTERVAL '1 hour')
            """,
            hours,
        )
        events = {row["name"]: row["count"] for row in rows}
        return {
            "window_hours": hours,
            "events": events,
            "api_errors": events.get("api_error", 0),
            "tts_failures": events.get("api_tts_failed", 0),
            "ai_provider_errors": provider_errors or 0,
            "client_crashes": events.get("app_crash", 0) + events.get("app_render_error", 0),
            "estimated_ai_calls": events.get("api_answer_analyzed", 0) + events.get("api_questions_generated", 0),
            "estimated_tts_chars": tts_chars or 0,
        }

    async def get_user_notifications(self, user_id: str) -> List[Dict]:
        if not self.pool:
            return []
        
        rows = await self.pool.fetch(
            "SELECT * FROM notifications WHERE user_id = $1 ORDER BY created_at DESC LIMIT 50",
            user_id
        )
        return [dict(r) for r in rows]

    async def mark_notification_read(self, notification_id: str, user_id: str = None):
        if not self.pool:
            return

        if user_id:
            await self.pool.execute(
                "UPDATE notifications SET is_read = TRUE WHERE id = $1 AND user_id = $2",
                notification_id,
                user_id,
            )
        else:
            await self.pool.execute(
                "UPDATE notifications SET is_read = TRUE WHERE id = $1",
                notification_id,
            )

    async def delete_notification(self, notification_id: str, user_id: str):
        if not self.pool:
            return False

        result = await self.pool.execute(
            "DELETE FROM notifications WHERE id = $1 AND user_id = $2",
            notification_id,
            user_id,
        )
        return result.endswith(" 1")

    async def delete_user_data(self, user_id: str) -> Dict:
        """
        Permanently removes application-owned data for a user.
        Supabase Auth deletion is handled separately by the auth service.
        """
        if not self.pool:
            return {"deleted": False, "reason": "database_unavailable"}

        async with self.pool.acquire() as conn:
            async with conn.transaction():
                deleted_answers = await conn.fetchval(
                    """
                    WITH deleted AS (
                        DELETE FROM answers
                        WHERE session_id IN (SELECT id FROM sessions WHERE user_id = $1)
                        RETURNING 1
                    )
                    SELECT COUNT(*) FROM deleted
                    """,
                    user_id,
                )

                deleted_feedback = await conn.fetchval(
                    "WITH deleted AS (DELETE FROM feedback WHERE user_id = $1 RETURNING 1) SELECT COUNT(*) FROM deleted",
                    user_id,
                )
                deleted_notifications = await conn.fetchval(
                    "WITH deleted AS (DELETE FROM notifications WHERE user_id = $1 RETURNING 1) SELECT COUNT(*) FROM deleted",
                    user_id,
                )
                deleted_events = await conn.fetchval(
                    "WITH deleted AS (DELETE FROM app_events WHERE user_id = $1 RETURNING 1) SELECT COUNT(*) FROM deleted",
                    user_id,
                )
                deleted_devices = await conn.fetchval(
                    "WITH deleted AS (DELETE FROM user_devices WHERE user_id = $1 RETURNING 1) SELECT COUNT(*) FROM deleted",
                    user_id,
                )
                deleted_entitlements = await conn.fetchval(
                    "WITH deleted AS (DELETE FROM user_entitlements WHERE user_id = $1 RETURNING 1) SELECT COUNT(*) FROM deleted",
                    user_id,
                )
                deleted_resume_profiles = await conn.fetchval(
                    "WITH deleted AS (DELETE FROM user_resume_profiles WHERE user_id = $1 RETURNING 1) SELECT COUNT(*) FROM deleted",
                    user_id,
                )
                deleted_sessions = await conn.fetchval(
                    "WITH deleted AS (DELETE FROM sessions WHERE user_id = $1 RETURNING 1) SELECT COUNT(*) FROM deleted",
                    user_id,
                )

        return {
            "deleted": True,
            "answers": deleted_answers or 0,
            "feedback": deleted_feedback or 0,
            "notifications": deleted_notifications or 0,
            "events": deleted_events or 0,
            "devices": deleted_devices or 0,
            "entitlements": deleted_entitlements or 0,
            "resume_profiles": deleted_resume_profiles or 0,
            "sessions": deleted_sessions or 0,
        }

# Singleton
neon_db = NeonDatabase()
