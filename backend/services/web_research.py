import os
from typing import Optional

import httpx


class WebResearchService:
    def __init__(self):
        self.enabled = os.getenv("WEB_RESEARCH_ENABLED", "true").lower() == "true"
        self.tavily_api_key = os.getenv("TAVILY_API_KEY")
        self.timeout_seconds = float(os.getenv("WEB_RESEARCH_TIMEOUT_SECONDS", "12"))
        self.max_results = int(os.getenv("WEB_RESEARCH_MAX_RESULTS", "5"))

    async def research_company(
        self,
        *,
        company_name: Optional[str],
        company_website: Optional[str],
        role_title: str,
        industry: str,
        user_context: Optional[str],
    ) -> dict:
        if not self.enabled:
            return {"enabled": False, "summary": "", "sources": [], "provider": None}

        query_parts = [
            company_name or company_website,
            industry,
            role_title,
            "company overview products business model recent priorities interview preparation",
        ]
        query = " ".join(part for part in query_parts if part).strip()
        if not query:
            return {"enabled": True, "summary": "", "sources": [], "provider": None}

        if self.tavily_api_key:
            return await self._search_tavily(query, user_context)

        return {
            "enabled": True,
            "summary": "",
            "sources": [],
            "provider": None,
            "warning": "WEB_RESEARCH_ENABLED is true but TAVILY_API_KEY is not configured.",
        }

    async def _search_tavily(self, query: str, user_context: Optional[str]) -> dict:
        payload = {
            "query": query,
            "search_depth": "basic",
            "include_answer": True,
            "max_results": self.max_results,
        }
        async with httpx.AsyncClient(timeout=self.timeout_seconds) as client:
            response = await client.post(
                "https://api.tavily.com/search",
                json=payload,
                headers={"Authorization": f"Bearer {self.tavily_api_key}"},
            )
        response.raise_for_status()
        data = response.json()
        sources = [
            {
                "title": item.get("title", ""),
                "url": item.get("url", ""),
                "content": (item.get("content") or "")[:500],
            }
            for item in data.get("results", [])
        ]
        return {
            "enabled": True,
            "provider": "tavily",
            "summary": self._format_summary(data.get("answer") or "", sources, user_context),
            "sources": sources,
        }

    def _format_summary(self, answer: str, sources: list[dict], user_context: Optional[str]) -> str:
        lines = []
        if user_context:
            lines.append("Candidate-provided context:")
            lines.append(user_context[:1800])
        if answer:
            lines.append("Web research summary:")
            lines.append(answer[:1200])
        if sources:
            lines.append("Research sources:")
            for source in sources[: self.max_results]:
                title = source.get("title") or "Untitled source"
                url = source.get("url") or ""
                content = source.get("content") or ""
                lines.append(f"- {title} ({url}): {content[:320]}")
        return "\n".join(lines).strip()


web_research = WebResearchService()
