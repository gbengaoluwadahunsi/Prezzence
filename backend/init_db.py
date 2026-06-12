"""
Neon PostgreSQL Schema Initialization.
Run this script ONCE to create all tables in your Neon database.

Usage: python init_db.py
"""
import asyncio
import os
import asyncpg
from dotenv import load_dotenv

load_dotenv()

DATABASE_URL = os.getenv("DATABASE_URL")

SCHEMA = """
-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Personas table
CREATE TABLE IF NOT EXISTS personas (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL DEFAULT '',
    personality VARCHAR(50) NOT NULL DEFAULT 'neutral',
    backstory TEXT DEFAULT '',
    bio TEXT DEFAULT '',
    traits TEXT[] DEFAULT '{}',
    avatar_url TEXT DEFAULT '',
    glb_url TEXT DEFAULT '',
    is_default BOOLEAN DEFAULT TRUE,
    is_premium BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Sessions table
CREATE TABLE IF NOT EXISTS sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    role_title VARCHAR(200) NOT NULL,
    industry VARCHAR(100) DEFAULT 'Tech',
    seniority VARCHAR(50) DEFAULT 'Mid-level',
    interview_type VARCHAR(50) DEFAULT 'mixed',
    difficulty VARCHAR(50) DEFAULT 'medium',
    length VARCHAR(50) DEFAULT '15',
    panel_config JSONB DEFAULT '[]',
    status VARCHAR(20) DEFAULT 'in_progress',
    company_name VARCHAR(200),
    company_website TEXT,
    company_context TEXT,
    question_count INT DEFAULT 0,
    score INT DEFAULT 0,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- User entitlement table
CREATE TABLE IF NOT EXISTS user_entitlements (
    user_id UUID PRIMARY KEY,
    plan VARCHAR(40) NOT NULL DEFAULT 'free',
    is_premium BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Answers table
CREATE TABLE IF NOT EXISTS answers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    question_id INT NOT NULL,
    transcript TEXT DEFAULT '',
    score INT DEFAULT 0,
    clarity INT DEFAULT 0,
    pacing INT DEFAULT 0,
    impact INT DEFAULT 0,
    confidence INT DEFAULT 0,
    knowledge INT DEFAULT 0,
    feedback TEXT DEFAULT '',
    tips JSONB DEFAULT '[]',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Feedback table
CREATE TABLE IF NOT EXISTS feedback (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    session_id UUID REFERENCES sessions(id),
    rating INT DEFAULT 5,
    comment TEXT DEFAULT '',
    category VARCHAR(50) DEFAULT 'general',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) DEFAULT 'info',
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes for fast lookups
CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_sessions_status ON sessions(status);
CREATE INDEX IF NOT EXISTS idx_answers_session_id ON answers(session_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_is_read ON notifications(is_read);

-- Seed default personas (only if empty)
INSERT INTO personas (name, title, personality, backstory, traits, is_default)
SELECT * FROM (VALUES
    ('Sophia', 'Senior Technical Lead', 'friendly', 'Sophia is a warm but thorough interviewer who values structured thinking and collaboration.', ARRAY['empathetic', 'structured', 'encouraging'], TRUE),
    ('Marcus', 'VP of Engineering', 'tough', 'Marcus is a no-nonsense leader who pushes candidates to demonstrate real impact and depth.', ARRAY['direct', 'analytical', 'demanding'], TRUE),
    ('Priya', 'Product Director', 'neutral', 'Priya brings a balanced perspective, focusing on strategic thinking and communication clarity.', ARRAY['strategic', 'calm', 'insightful'], TRUE)
) AS v(name, title, personality, backstory, traits, is_default)
WHERE NOT EXISTS (SELECT 1 FROM personas LIMIT 1);
"""


async def main():
    if not DATABASE_URL:
        print("ERROR: DATABASE_URL not set in .env")
        return

    print(f"[init_db] Connecting to Neon PostgreSQL...")
    conn = await asyncpg.connect(DATABASE_URL, ssl="require")

    try:
        print("[init_db] Creating schema...")
        await conn.execute(SCHEMA)
        print("[init_db] Schema created successfully!")

        # Verify
        count = await conn.fetchval("SELECT COUNT(*) FROM personas")
        print(f"[init_db] Personas seeded: {count} entries")

        sessions_count = await conn.fetchval("SELECT COUNT(*) FROM sessions")
        print(f"[init_db] Sessions table ready ({sessions_count} existing)")

        answers_count = await conn.fetchval("SELECT COUNT(*) FROM answers")
        print(f"[init_db] Answers table ready ({answers_count} existing)")

    except Exception as e:
        print(f"[init_db] ERROR: {e}")
    finally:
        await conn.close()
        print("[init_db] Connection closed.")


if __name__ == "__main__":
    asyncio.run(main())
