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
        learnMoreTopic.ifBlank { matchedResource()?.topic ?: shortQuestionTopic() }

    fun resolvedLearnMoreUrl(): String {
        // Always send users to a live web search for the topic. Hardcoded article URLs
        // go stale and 404, so we build a reliable query from the question's topic.
        val subject = resolvedLearnMoreTopic().trim()
            .ifBlank { text.trim() }
            .ifBlank { role.trim() }
            .ifBlank { "behavioral interview questions" }
        val query = java.net.URLEncoder.encode("$subject interview answer tips".take(140), Charsets.UTF_8.name())
        return "https://www.google.com/search?q=$query"
    }

    private fun matchedResource(): LearnMoreResource? {
        val haystack = text.lowercase(Locale.US)
        return LEARN_MORE_RESOURCES.firstOrNull { resource ->
            resource.keywords.any { haystack.contains(it) }
        }
    }

    private fun shortQuestionTopic(): String {
        val cleaned = text.trim()
            .replace(Regex("\\s+"), " ")
            .trim(' ', '?', '.', '!')
        if (cleaned.isBlank()) {
            return type.trim().replaceFirstChar { it.uppercase(Locale.US) }.ifBlank { "Interview question prep" }
        }
        return if (cleaned.length <= 72) cleaned else cleaned.take(69).trimEnd() + "..."
    }

    data class LearnMoreResource(val keywords: List<String>, val topic: String, val url: String)

    companion object {
        private val LEARN_MORE_RESOURCES = listOf(
            LearnMoreResource(
                listOf("introduce yourself", "overview of your background", "tell me about yourself"),
                "Tell me about yourself",
                "https://www.indeed.com/career-advice/interviewing/how-to-answer-tell-me-about-yourself",
            ),
            LearnMoreResource(
                listOf("conflict", "disagreement", "difficult colleague", "pushback", "disagree"),
                "Handling workplace conflict",
                "https://www.indeed.com/career-advice/interviewing/interview-question-tell-me-about-a-time-you-handled-a-conflict",
            ),
            LearnMoreResource(
                listOf("priorit", "urgent", "deadline", "multiple requests", "juggle", "competing"),
                "Prioritization under pressure",
                "https://www.indeed.com/career-advice/interviewing/interview-question-how-do-you-prioritize-your-work",
            ),
            LearnMoreResource(
                listOf("lead", "managed a team", "led a team", "leadership", "delegate", "manage a team"),
                "Leadership examples",
                "https://www.indeed.com/career-advice/interviewing/leadership-interview-questions",
            ),
            LearnMoreResource(
                listOf("mistake", "failure", "went wrong", "setback", "failed"),
                "Learning from failure",
                "https://www.indeed.com/career-advice/interviewing/interview-question-tell-me-about-a-time-you-made-a-mistake",
            ),
            LearnMoreResource(
                listOf("customer", "client", "frustrated", "complaint", "stakeholder"),
                "Customer & stakeholder handling",
                "https://www.indeed.com/career-advice/interviewing/customer-service-interview-questions",
            ),
            LearnMoreResource(
                listOf("confiden", "uncertain", "unclear", "ambig", "motivat", "reassur", "morale", "calm"),
                "Leading through uncertainty",
                "https://www.indeed.com/career-advice/interviewing/how-to-deal-with-ambiguity",
            ),
            LearnMoreResource(
                listOf("communicat", "explain", "present", "audience"),
                "Communicating clearly",
                "https://www.indeed.com/career-advice/interviewing/communication-interview-questions",
            ),
            LearnMoreResource(
                listOf("technical", "debug", "system design", "architecture", "tradeoff", "trade-off"),
                "Technical decision-making",
                "https://www.indeed.com/career-advice/interviewing/technical-interview-questions",
            ),
            LearnMoreResource(
                listOf("feedback", "criticism", "coaching"),
                "Receiving feedback",
                "https://www.indeed.com/career-advice/interviewing/interview-question-tell-me-about-a-time-you-received-feedback",
            ),
            LearnMoreResource(
                listOf("change", "adapt", "pivot", "unexpected"),
                "Adapting to change",
                "https://www.indeed.com/career-advice/interviewing/adaptability-interview-questions",
            ),
        )
    }
}

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

