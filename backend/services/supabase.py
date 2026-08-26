"""
Supabase Service — AUTH ONLY.
Handles user authentication, profile lookups, and device verification.
All application data (sessions, answers, personas) lives in Neon PostgreSQL.
"""
import os
from supabase import create_client, Client
from dotenv import load_dotenv

load_dotenv()

SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_KEY = os.getenv("SUPABASE_SERVICE_ROLE_KEY")


class SupabaseAuthService:
    def __init__(self):
        if not SUPABASE_URL or not SUPABASE_KEY:
            self.client = None
            print("WARNING: Supabase credentials not found. Auth operations will fail.")
        else:
            self.client: Client = create_client(SUPABASE_URL, SUPABASE_KEY)

    def get_user_profile(self, user_id: str):
        if not self.client:
            return None
        response = self.client.table("profiles").select("*").eq("id", user_id).single().execute()
        return response.data

    def verify_device_allowance(self, user_id: str, device_id: str) -> bool:
        if not self.client:
            return True
        try:
            res = self.client.table("profiles").select("registered_devices").eq("id", user_id).single().execute()
            devices = res.data.get("registered_devices")
            if devices is None:
                devices = []
        except Exception as e:
            print(f"Warning: Device schema not initialized. {e}")
            return True

        if device_id in devices:
            return True

        if len(devices) >= 2:
            return False

        devices.append(device_id)
        self.client.table("profiles").update({"registered_devices": devices}).eq("id", user_id).execute()
        return True

    def delete_auth_user(self, user_id: str) -> bool:
        if not self.client:
            return False
        self.client.auth.admin.delete_user(user_id)
        return True

    def ensure_storage_bucket(self, bucket_name: str, public: bool = True) -> bool:
        """
        Ensures an object storage bucket exists for generated media.
        This keeps media files out of Neon while Neon remains the source of truth
        for sessions, scores, answers, progress, and analytics.
        """
        if not self.client:
            print("[Storage] Supabase client unavailable; cannot ensure bucket.")
            return False

        try:
            self.client.storage.get_bucket(bucket_name)
            return True
        except Exception:
            pass

        try:
            self.client.storage.create_bucket(
                bucket_name,
                options={
                    "public": public,
                    "file_size_limit": 10 * 1024 * 1024,
                    "allowed_mime_types": ["audio/mpeg", "audio/mp3"],
                },
            )
            print(f"[Storage] Created Supabase bucket '{bucket_name}'.")
            return True
        except Exception as exc:
            message = str(exc)
            if "already exists" in message.lower() or "duplicate" in message.lower():
                return True
            print(f"[Storage] Failed to create Supabase bucket '{bucket_name}': {exc}")
            return False

    def ping(self) -> bool:
        """Pings Supabase to prevent project auto-pausing/sleeping."""
        if not self.client:
            return False
        try:
            self.client.table("profiles").select("id").limit(1).execute()
            return True
        except Exception as e:
            print(f"[Supabase Keepalive] Ping error: {e}")
            return False


# Singleton — used by auth middleware only
db = SupabaseAuthService()
