from pathlib import Path

path = Path(r"app/src/main/java/com/pollecode/prezzencekotlin/MainActivity.kt")
text = path.read_text(encoding="utf-8")
start_marker = "    private fun enhancedPresenceSummaryLegacyRemoved"
end_marker = "    private fun summarizePresenceSamples"
start = text.index(start_marker)
end = text.index(end_marker)
text = text[:start] + text[end:]
# Remove legacy showPaused View screen
legacy_paused = """    private fun showPaused() {
        val column = baseColumn().apply { gravity = Gravity.CENTER }
        column.addView(title("Interview paused", 30))
        column.addView(body("Resume when you are ready, or end this answer and review coaching."))
        column.addView(rowOf(
            primaryButton("Resume") { showInterview(activeTranscriber != null) },
            secondaryButton("Back to home") { showHome() }
        ))
        setScreen(scroll(column))
    }
"""
text = text.replace(legacy_paused, "")
path.write_text(text, encoding="utf-8")
print("cleanup done")
