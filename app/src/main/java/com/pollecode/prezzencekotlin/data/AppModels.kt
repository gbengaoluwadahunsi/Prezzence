package com.pollecode.prezzencekotlin.data

import java.util.Locale

enum class InterviewMode {
    SINGLE,
    PANEL
}

enum class InterviewPhase {
    INTRO,
    ASKING,
    ANSWERING,
    RESULT
}

data class Interviewer(
    val id: String,
    val name: String,
    val title: String,
    val modelName: String,
)

data class InterviewQuestion(
    val id: Int,
    val text: String,
    val role: String,
    val interviewerId: String,
    val type: String = "behavioral",
    val learnMoreTopic: String = "",
    val learnMoreUrl: String = "",
) {
    fun resolvedLearnMoreTopic(): String =
        learnMoreTopic.ifBlank {
            role.trim().ifBlank { type.trim() }.ifBlank { "Interview question prep" }
        }

    fun resolvedLearnMoreUrl(): String {
        if (learnMoreUrl.isNotBlank()) return learnMoreUrl
        val query = java.net.URLEncoder.encode("${text.take(90)} STAR behavioral interview", Charsets.UTF_8.name())
        return "https://www.google.com/search?q=$query"
    }
}

data class PresenceMetrics(
    val faceVisible: Boolean = false,
    val faceVisibility: Int = 0,
    val eyeContact: Int = 0,
    val headStability: Int = 0,
    val posture: Int = 0,
    val expressionEnergy: Int = 0,
)

data class AnswerResult(
    val transcript: String,
    val score: Int,
    val feedback: String,
    val improvedAnswer: String,
    val what: String,
    val how: String,
    val why: String,
    val coachingFeedback: String = "",
    val coachingMessage: String = "",
    val presenceMetrics: PresenceMetrics? = null,
    val retryRequired: Boolean = false,
)

data class SessionSummary(
    val id: String,
    val role: String,
    val score: Int,
    val answered: Int,
    val total: Int,
    val date: String,
    val status: String = "",
)

fun SessionSummary.resolvedPracticeStatus(): String {
    val backend = status.trim().lowercase()
    return when {
        total > 0 && answered >= total && score > 0 -> "Completed"
        backend == "completed" && answered > 0 && score <= 0 -> "In progress"
        answered > 0 -> "In progress"
        else -> "Started"
    }
}

data class ResumeProfile(
    val fileName: String,
    val summary: String,
    val skills: List<String>,
    val experience: List<String>,
)

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val createdAt: String,
    val isRead: Boolean,
)

data class NotificationPreferences(
    val pushNotificationsEnabled: Boolean = true,
    val emailSummariesEnabled: Boolean = false,
    val practiceRemindersEnabled: Boolean = true,
    val achievementAlertsEnabled: Boolean = true,
    val productUpdatesEnabled: Boolean = true,
)
object PrezzenceDefaults {
    const val FREE_SESSION_LIMIT = 3
    val interviewers = listOf(
        Interviewer("maya", "Maya", "People Lead", "Lily"),
        Interviewer("jonas", "Jonas", "Hiring Manager", "Oliver"),
        Interviewer("amina", "Sophia", "Domain Expert", "Sofia"),
    )

    fun panelIdsForStyle(style: String): List<String> = when (style.lowercase(Locale.US)) {
        "supportive", "friendly", "warm", "encouraging" -> listOf("amina", "maya", "jonas")
        "challenging", "tough", "direct", "skeptical" -> listOf("jonas", "maya", "amina")
        else -> listOf("maya", "jonas", "amina")
    }

    fun panelInterviewersForStyle(style: String): List<Interviewer> {
        val byId = interviewers.associateBy { it.id }
        return panelIdsForStyle(style).mapNotNull { byId[it] }
    }

    val roles = listOf(
        "Customer Support Representative",
        "Administrative Assistant",
        "Teacher",
        "Sales Representative",
        "Project Coordinator",
        "Software Engineer",
    )

    fun questionsFor(role: String, panelStyle: String = "Balanced"): List<InterviewQuestion> {
        val normalized = role.ifBlank { roles.first() }
        val base = when (normalized) {
            "Administrative Assistant" -> listOf(
                "Please introduce yourself and give me a quick overview of your background.",
                "How do you prioritize urgent requests from different people?",
                "Tell me about a time you kept an office or team organized under pressure.",
                "How do you handle confidential information?",
                "Describe a time you solved a scheduling or communication problem.",
            )
            "Teacher" -> listOf(
                "Please introduce yourself and give me a quick overview of your background.",
                "Tell me about a time you helped a struggling learner improve.",
                "How do you manage a classroom when attention is low?",
                "Describe how you plan lessons for different learning levels.",
                "How do you handle feedback from parents or school leadership?",
            )
            "Sales Representative" -> listOf(
                "Please introduce yourself and give me a quick overview of your background.",
                "Tell me about a time you handled a difficult objection.",
                "How do you qualify a new prospect?",
                "Describe a deal or customer conversation you moved forward.",
                "How do you recover after missing a sales target?",
            )
            "Project Coordinator" -> listOf(
                "Please introduce yourself and give me a quick overview of your background.",
                "Tell me about a time you coordinated work across multiple people.",
                "How do you handle a project that is falling behind?",
                "Describe a time you communicated risk early.",
                "How do you keep stakeholders aligned?",
            )
            "Software Engineer" -> listOf(
                "Please introduce yourself and give me a quick overview of your background.",
                "Tell me about a time you debugged a difficult issue.",
                "How do you balance speed and code quality?",
                "Describe a technical tradeoff you made.",
                "How do you explain technical work to non-technical people?",
            )

            else -> listOf(
                "Please introduce yourself and give me a quick overview of your background.",
                "Tell me about a time you handled a frustrated customer.",
                "How do you decide when to escalate a customer issue?",
                "Describe a time you stayed calm under pressure.",
                "How would you manage a high volume of customer requests?",
            )
        }

        val standard = if (base.size >= 8) {
            base
        } else {
            base + listOf(
                "Tell me about feedback you received and how you used it.",
                "Describe a time you had to learn something quickly.",
                "Why are you interested in this opportunity now?",
            ).take(8 - base.size)
        }

        val panel = panelIdsForStyle(panelStyle)
        return standard.mapIndexed { index, text ->
            InterviewQuestion(
                id = index + 1,
                text = text,
                role = normalized,
                interviewerId = panel[index % panel.size],
                type = if (index == 0) "introduction" else "behavioral",
            )
        }
    }
}

