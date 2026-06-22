package com.pollecode.prezzencekotlin.data

import kotlin.math.roundToInt

data class SessionSkillBreakdown(
    val clarity: Int,
    val relevance: Int,
    val structure: Int,
    val evidence: Int,
    val conciseness: Int,
) {
    fun asList(): List<Pair<String, Int>> = listOf(
        "Clarity" to clarity,
        "Relevance" to relevance,
        "Structure" to structure,
        "Evidence" to evidence,
        "Conciseness" to conciseness,
    )
}

object SessionScoring {
    private val fillerWords = setOf(
        "okay", "ok", "yes", "no", "hello", "hi", "hey", "thanks", "thank", "fine", "good",
        "alright", "right", "well", "just", "like", "yeah", "um", "uh", "music", "shoot",
    )

    private val micCheckPhrases = listOf(
        "can you hear me",
        "how are you today",
        "hello there",
        "is it okay",
        "testing testing",
        "testing one two",
        "mic check",
        "audio test",
        "are you there",
        "i'm going to do my",
        "going to tell you",
    )

    fun isPlaceholderTranscript(transcript: String): Boolean {
        val normalized = transcript.trim().lowercase()
        if (normalized.isBlank()) return true
        if (normalized == "no clear speech was captured.") return true
        if (normalized.startsWith("[audio received") || normalized.startsWith("[audio captured")) return true
        if (normalized.contains("blank audio") || normalized == "[blank]" || normalized == "[silence]") return true
        if (normalized == "[music]" || normalized == "[noise]") return true
        if (normalized.matches(Regex("^\\[[^\\]]+\\]$"))) return true
        return false
    }

    fun isBlankTranscript(transcript: String): Boolean = isPlaceholderTranscript(transcript)

    fun formatTranscriptForDisplay(transcript: String): String =
        if (isPlaceholderTranscript(transcript)) "No response" else transcript.trim()

    fun isSubstantiveAnswer(transcript: String): Boolean {
        val raw = transcript.trim()
        if (isBlankTranscript(raw)) return false
        val lower = raw.lowercase()
        if (lower.contains("[music]") || lower.contains("[silence]") || lower.contains("[noise]")) return false

        val words = meaningfulWords(lower)

        // Only treat mic-check phrases as disqualifying when the answer is short
        // (a real answer can open with "can you hear me" before continuing)
        if (words.size < 14 && micCheckPhrases.any { lower.contains(it) }) return false

        if (words.size < 14) return false

        val interviewSignals = listOf(
            // Original signals
            "because", "result", "outcome", "customer", "client", "team", "project", "managed",
            "handled", "resolved", "improved", "example", "situation", "challenge", "delivered",
            "implemented", "led", "worked", "helped", "achieved", "reduced", "increased",
            // Common real-answer words that the original list missed
            "experience", "understand", "environment", "explain", "explain", "wanted", "needed",
            "believe", "decided", "applied", "started", "completed", "focused", "learned",
            "approached", "used", "tried", "created", "developed", "supported", "chose",
            "role", "company", "field", "background", "skill", "goal", "approach",
            "encouraged", "motivated", "interested", "responsible", "involved",
        )
        val signalHits = interviewSignals.count { lower.contains(it) }
        val personalActions = listOf("i ", "my ", "we ", "our ").count { lower.contains(it) }
        if (signalHits < 1 && personalActions < 2) return false

        val questionLikeRatio = words.count { it.length <= 3 }.toFloat() / words.size.toFloat()
        if (lower.count { it == '?' } >= 2 && questionLikeRatio > 0.35f) return false

        return true
    }

    fun sanitizeScore(transcript: String, score: Int): Int {
        if (!isSubstantiveAnswer(transcript)) return 0
        return score.coerceIn(0, 100)
    }

    fun analyzeAnswer(question: String, transcript: String): AnswerDimensions {
        if (!isSubstantiveAnswer(transcript)) {
            return AnswerDimensions(0, 0, 0, 0, 0, 0)
        }
        val lower = transcript.lowercase().trim()
        val words = meaningfulWords(lower)

        val stopWords = setOf(
            "about", "when", "where", "would", "could", "should", "your", "you", "with", "that", "this",
            "from", "into", "have", "were", "been", "will", "tell", "give", "describe", "please",
            "time", "role", "background", "question", "answer", "interview", "there", "their", "them",
        )
        val questionWords = question.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length > 3 && it !in stopWords }
            .toSet()
        val overlap = questionWords.count { it in words.toSet() }

        val evidenceWords = listOf(
            "customer", "client", "student", "team", "manager", "project", "issue", "problem",
            "resolved", "improved", "reduced", "increased", "handled", "measured", "result", "outcome",
        ).count { lower.contains(it) }
        val structureWords = listOf("situation", "action", "result", "first", "then", "because", "after", "finally")
            .count { lower.contains(it) }
        val personalActionWords = listOf("i did", "i worked", "i handled", "i called", "i helped", "i asked", "i followed")
            .count { lower.contains(it) }

        val clarity = (
            (words.size.coerceAtMost(80) / 80.0 * 35) +
                (structureWords.coerceAtMost(3) / 3.0 * 35) +
                (if (lower.contains("because") || lower.contains("so that")) 30.0 else 10.0)
            ).roundToInt().coerceIn(0, 100)

        val relevance = ((overlap.coerceAtMost(5) / 5.0 * 70) + (if (questionWords.isEmpty()) 30 else 0))
            .roundToInt().coerceIn(0, 100)

        val structure = ((structureWords.coerceAtMost(4) / 4.0 * 55) + (personalActionWords.coerceAtMost(2) / 2.0 * 45))
            .roundToInt().coerceIn(0, 100)

        val evidence = ((evidenceWords.coerceAtMost(5) / 5.0 * 70) +
            (if (Regex("\\b\\d+[%x]?\\b").containsMatchIn(transcript)) 30 else 0))
            .roundToInt().coerceIn(0, 100)

        val conciseness = when {
            words.size < 18 -> 35
            words.size <= 75 -> 88
            words.size <= 110 -> 68
            else -> 45
        }

        val overall = (
            clarity * 0.22 +
                relevance * 0.26 +
                structure * 0.22 +
                evidence * 0.20 +
                conciseness * 0.10
            ).roundToInt().coerceIn(0, 100)

        return AnswerDimensions(clarity, relevance, structure, evidence, conciseness, overall)
    }

    fun sessionSkillBreakdown(answers: List<AnswerResult>, questions: List<InterviewQuestion>): SessionSkillBreakdown? {
        val dimensions = answers.mapIndexedNotNull { index, answer ->
            if (isBlankTranscript(answer.transcript) || answer.score <= 0) return@mapIndexedNotNull null
            val question = questions.getOrNull(index)?.text.orEmpty()
            analyzeAnswer(question, answer.transcript)
        }
        if (dimensions.isEmpty()) return null
        return SessionSkillBreakdown(
            clarity = dimensions.map { it.clarity }.average().roundToInt(),
            relevance = dimensions.map { it.relevance }.average().roundToInt(),
            structure = dimensions.map { it.structure }.average().roundToInt(),
            evidence = dimensions.map { it.evidence }.average().roundToInt(),
            conciseness = dimensions.map { it.conciseness }.average().roundToInt(),
        )
    }

    fun sessionScore(answers: List<AnswerResult>, questions: List<InterviewQuestion>): Int {
        val scored = answers.mapNotNull { answer ->
            if (isBlankTranscript(answer.transcript)) null
            else answer.score.coerceIn(0, 100)
        }.filter { it > 0 }
        if (scored.isEmpty()) return 0
        return scored.average().roundToInt().coerceIn(0, 100)
    }

    fun substantiveAnswerCount(answers: List<AnswerResult>): Int =
        answers.count { !isBlankTranscript(it.transcript) && it.score > 0 }

    fun normalizeStoredTranscript(transcript: String): String {
        val trimmed = transcript.trim()
        return if (isPlaceholderTranscript(trimmed)) "" else trimmed
    }

    private fun meaningfulWords(lower: String): List<String> =
        lower.split(Regex("[^a-z0-9]+"))
            .filter { it.length > 2 && it !in fillerWords }
}

data class AnswerDimensions(
    val clarity: Int,
    val relevance: Int,
    val structure: Int,
    val evidence: Int,
    val conciseness: Int,
    val overall: Int,
)
