import asyncio
import os
import asyncpg
from dotenv import load_dotenv

load_dotenv()

async def migrate():
    url = os.getenv("DATABASE_URL")
    if not url:
        print("DATABASE_URL not found")
        return

    conn = await asyncpg.connect(url, ssl="require")
    try:
        print("Running migrations...")
        await conn.execute("ALTER TABLE sessions ADD COLUMN IF NOT EXISTS score INT DEFAULT 0")
        await conn.execute("ALTER TABLE personas ADD COLUMN IF NOT EXISTS bio TEXT DEFAULT ''")
        print("Migration complete!")
    except Exception as e:
        print(f"Migration error: {e}")
    finally:
        await conn.close()

if __name__ == "__main__":
    asyncio.run(migrate())
