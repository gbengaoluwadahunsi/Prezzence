package com.pollecode.prezzencekotlin.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppState(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("prezzence_kotlin_state", Context.MODE_PRIVATE)

    var onboardingComplete: Boolean
        get() = prefs.getBoolean("onboardingComplete", false)
        set(value) = prefs.edit().putBoolean("onboardingComplete", value).apply()

    var cameraCoachEnabled: Boolean
        get() = prefs.getBoolean("cameraCoachEnabled", true)
        set(value) = prefs.edit().putBoolean("cameraCoachEnabled", value).apply()

    var a11yLargeText: Boolean
        get() = prefs.getBoolean("a11yLargeText", false)
        set(value) = prefs.edit().putBoolean("a11yLargeText", value).apply()

    var a11yHighContrast: Boolean
        get() = prefs.getBoolean("a11yHighContrast", false)
        set(value) = prefs.edit().putBoolean("a11yHighContrast", value).apply()

    var a11yReducedMotion: Boolean
        get() = prefs.getBoolean("a11yReducedMotion", false)
        set(value) = prefs.edit().putBoolean("a11yReducedMotion", value).apply()

    var pushNotificationsEnabled: Boolean
        get() = prefs.getBoolean("pushNotificationsEnabled", true)
        set(value) = prefs.edit().putBoolean("pushNotificationsEnabled", value).apply()

    var practiceRemindersEnabled: Boolean
        get() = prefs.getBoolean("practiceRemindersEnabled", true)
        set(value) = prefs.edit().putBoolean("practiceRemindersEnabled", value).apply()

    var achievementAlertsEnabled: Boolean
        get() = prefs.getBoolean("achievementAlertsEnabled", true)
        set(value) = prefs.edit().putBoolean("achievementAlertsEnabled", value).apply()

    var emailSummariesEnabled: Boolean
        get() = prefs.getBoolean("emailSummariesEnabled", false)
        set(value) = prefs.edit().putBoolean("emailSummariesEnabled", value).apply()

    var productUpdatesEnabled: Boolean
        get() = prefs.getBoolean("productUpdatesEnabled", true)
        set(value) = prefs.edit().putBoolean("productUpdatesEnabled", value).apply()

    var wifiOnlyDownloads: Boolean
        get() = prefs.getBoolean("wifiOnlyDownloads", false)
        set(value) = prefs.edit().putBoolean("wifiOnlyDownloads", value).apply()

    var selectedRole: String
        get() = prefs.getString("selectedRole", PrezzenceDefaults.roles.first()) ?: PrezzenceDefaults.roles.first()
        set(value) = prefs.edit().putString("selectedRole", value).apply()

    var interviewMode: InterviewMode
        get() = runCatching {
            InterviewMode.valueOf(prefs.getString("interviewMode", InterviewMode.SINGLE.name) ?: InterviewMode.SINGLE.name)
        }.getOrDefault(InterviewMode.SINGLE)
        set(value) = prefs.edit().putString("interviewMode", value.name).apply()

    var interviewerStyle: String
        get() = prefs.getString("interviewerStyle", "Balanced") ?: "Balanced"
        set(value) = prefs.edit().putString("interviewerStyle", value.ifBlank { "Balanced" }).apply()

    var previewGender: String
        get() = prefs.getString("previewGender", "Female") ?: "Female"
        set(value) = prefs.edit().putString("previewGender", value.ifBlank { "Female" }).apply()

    var language: String
        get() = prefs.getString("language", "en-US") ?: "en-US"
        set(value) = prefs.edit().putString("language", value.ifBlank { "en-US" }).apply()

    var currentQuestionIndex: Int
        get() = prefs.getInt("currentQuestionIndex", 0)
        set(value) = prefs.edit().putInt("currentQuestionIndex", value.coerceAtLeast(0)).apply()

    var completedSessions: Int
        get() = prefs.getInt("completedSessions", 0)
        set(value) = prefs.edit().putInt("completedSessions", value.coerceAtLeast(0)).apply()

    var readinessScore: Int
        get() = prefs.getInt("readinessScore", 0)
        set(value) = prefs.edit().putInt("readinessScore", value.coerceIn(0, 100)).apply()

    var lastTranscript: String
        get() = prefs.getString("lastTranscript", "") ?: ""
        set(value) = prefs.edit().putString("lastTranscript", value).apply()

    var lastScore: Int
        get() = prefs.getInt("lastScore", 0)
        set(value) = prefs.edit().putInt("lastScore", value.coerceIn(0, 100)).apply()

    var authToken: String
        get() = prefs.getString("authToken", "") ?: ""
        set(value) = prefs.edit().putString("authToken", value).apply()

    var userId: String
        get() = prefs.getString("userId", "") ?: ""
        set(value) = prefs.edit().putString("userId", value).apply()

    var userEmail: String
        get() = prefs.getString("userEmail", "") ?: ""
        set(value) = prefs.edit().putString("userEmail", value).apply()

    var userFullName: String
        get() = prefs.getString("userFullName", "") ?: ""
        set(value) = prefs.edit().putString("userFullName", value).apply()

    var userFocus: String
        get() = prefs.getString("userFocus", "") ?: ""
        set(value) = prefs.edit().putString("userFocus", value).apply()

    var weeklyGoal: String
        get() = prefs.getString("weeklyGoal", "") ?: ""
        set(value) = prefs.edit().putString("weeklyGoal", value).apply()

    var activeSessionId: String
        get() = prefs.getString("activeSessionId", "") ?: ""
        set(value) = prefs.edit().putString("activeSessionId", value).apply()

    var subscriptionEntitled: Boolean
        get() = prefs.getBoolean("subscriptionEntitled", false)
        set(value) = prefs.edit().putBoolean("subscriptionEntitled", value).apply()

    var subscriptionStatus: String
        get() = prefs.getString("subscriptionStatus", "Not checked") ?: "Not checked"
        set(value) = prefs.edit().putString("subscriptionStatus", value).apply()

    var subscriptionProductId: String
        get() = prefs.getString("subscriptionProductId", "") ?: ""
        set(value) = prefs.edit().putString("subscriptionProductId", value).apply()

    private var sessionScoreTotal: Int
        get() = prefs.getInt("sessionScoreTotal", 0)
        set(value) = prefs.edit().putInt("sessionScoreTotal", value.coerceAtLeast(0)).apply()

    private var sessionAnsweredCount: Int
        get() = prefs.getInt("sessionAnsweredCount", 0)
        set(value) = prefs.edit().putInt("sessionAnsweredCount", value.coerceAtLeast(0)).apply()

    private var lastAnsweredQuestionIndex: Int
        get() = prefs.getInt("lastAnsweredQuestionIndex", -1)
        set(value) = prefs.edit().putInt("lastAnsweredQuestionIndex", value).apply()

    fun setGeneratedQuestions(questions: List<InterviewQuestion>) {
        val array = JSONArray()
        questions.forEach { question ->
            array.put(JSONObject()
                .put("id", question.id)
                .put("text", question.text)
                .put("role", question.role)
                .put("interviewerId", question.interviewerId)
                .put("type", question.type))
        }
        prefs.edit().putString("generatedQuestions", array.toString()).apply()
    }

    fun clearGeneratedQuestions() {
        prefs.edit().remove("generatedQuestions").apply()
    }

    fun questions(): List<InterviewQuestion> {
        val raw = prefs.getString("generatedQuestions", "") ?: ""
        val generated = runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val text = item.optString("text").trim()
                if (text.isBlank()) return@mapNotNull null
                InterviewQuestion(
                    id = item.optInt("id", index + 1),
                    text = text,
                    role = item.optString("role", selectedRole),
                    interviewerId = item.optString("interviewerId", PrezzenceDefaults.panelIdsForStyle(interviewerStyle)[index % 3]),
                    type = item.optString("type", if (index == 0) "introduction" else "behavioral"),
                )
            }
        }.getOrDefault(emptyList())
        return generated.ifEmpty { PrezzenceDefaults.questionsFor(selectedRole, interviewerStyle) }
    }

    fun currentQuestion(): InterviewQuestion = questions().let { list ->
        list[currentQuestionIndex.coerceIn(0, list.lastIndex)]
    }

    fun interviewerFor(question: InterviewQuestion = currentQuestion()): Interviewer {
        val selected = if (interviewMode == InterviewMode.SINGLE) {
            when (interviewerStyle.lowercase(Locale.US)) {
                "supportive", "friendly", "warm", "encouraging" -> "maya"
                "challenging", "tough", "direct", "skeptical" -> "jonas"
                else -> if (previewGender.equals("Male", ignoreCase = true)) "jonas" else "amina"
            }
        } else {
            question.interviewerId
        }
        return PrezzenceDefaults.interviewers.firstOrNull { it.id == selected }
            ?: PrezzenceDefaults.interviewers.last()
    }

    fun markAnswered(score: Int, transcript: String) {
        lastScore = score
        lastTranscript = transcript
        if (lastAnsweredQuestionIndex != currentQuestionIndex) {
            sessionScoreTotal += score.coerceIn(0, 100)
            sessionAnsweredCount += 1
            lastAnsweredQuestionIndex = currentQuestionIndex
        }
        readinessScore = sessionWeightedScore()
    }

    fun advanceOrComplete(): Boolean {
        val next = currentQuestionIndex + 1
        val done = next >= questions().size
        if (done) {
            saveCurrentSessionSummary()
            completedSessions += 1
            currentQuestionIndex = 0
            activeSessionId = ""
            sessionScoreTotal = 0
            sessionAnsweredCount = 0
            lastAnsweredQuestionIndex = -1
        } else {
            currentQuestionIndex = next
        }
        return done
    }

    fun saveSessionAnswers(sessionId: String, answers: List<AnswerResult>) {
        val array = JSONArray()
        answers.forEach { ans ->
            array.put(JSONObject()
                .put("transcript", ans.transcript)
                .put("score", ans.score)
                .put("feedback", ans.feedback)
                .put("improvedAnswer", ans.improvedAnswer)
                .put("what", ans.what)
                .put("how", ans.how)
                .put("why", ans.why))
        }
        prefs.edit().putString("sessionAnswers_$sessionId", array.toString()).apply()
    }

    fun getSessionAnswers(sessionId: String): List<AnswerResult> {
        val raw = prefs.getString("sessionAnswers_$sessionId", "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                AnswerResult(
                    transcript = item.optString("transcript", ""),
                    score = item.optInt("score", 0),
                    feedback = item.optString("feedback", ""),
                    improvedAnswer = item.optString("improvedAnswer", ""),
                    what = item.optString("what", ""),
                    how = item.optString("how", ""),
                    why = item.optString("why", ""),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun resetActiveSession() {
        currentQuestionIndex = 0
        activeSessionId = ""
        clearGeneratedQuestions()
        lastTranscript = ""
        lastScore = 0
        sessionScoreTotal = 0
        sessionAnsweredCount = 0
        lastAnsweredQuestionIndex = -1
    }

    fun signOut() {
        authToken = ""
        userId = ""
        userEmail = ""
        userFullName = ""
        userFocus = ""
        onboardingComplete = false
        resetActiveSession()
    }

    fun sessionHistory(): List<SessionSummary> {
        val raw = prefs.getString("sessionHistory", "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                SessionSummary(
                    id = item.optString("id"),
                    role = item.optString("role", PrezzenceDefaults.roles.first()),
                    score = item.optInt("score", 0),
                    answered = item.optInt("answered", 0),
                    total = item.optInt("total", questions().size),
                    date = item.optString("date", ""),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun deleteSession(id: String) {
        val kept = sessionHistory().filterNot { it.id == id }
        writeHistory(kept)
        completedSessions = kept.size
        readinessScore = if (kept.isEmpty()) 0 else kept.map { it.score }.average().toInt().coerceIn(0, 100)
    }

    fun clearHistory() {
        writeHistory(emptyList())
        completedSessions = 0
        readinessScore = 0
    }

    private fun sessionWeightedScore(): Int {
        val totalQuestions = questions().size.coerceAtLeast(1)
        if (sessionAnsweredCount == 0) return 0
        val averageAnsweredScore = sessionScoreTotal / sessionAnsweredCount
        val completionRatio = sessionAnsweredCount.toDouble() / totalQuestions.toDouble()
        return (averageAnsweredScore * completionRatio).toInt().coerceIn(0, 100)
    }

    private fun saveCurrentSessionSummary() {
        val answered = sessionAnsweredCount.coerceAtLeast(if (lastTranscript.isNotBlank()) 1 else 0)
        val score = sessionWeightedScore()
        val summary = SessionSummary(
            id = "session-${System.currentTimeMillis()}",
            role = selectedRole,
            score = score,
            answered = answered,
            total = questions().size,
            date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
        )
        writeHistory(listOf(summary) + sessionHistory())
    }

    private fun writeHistory(items: List<SessionSummary>) {
        val array = JSONArray()
        items.take(30).forEach { item ->
            array.put(JSONObject()
                .put("id", item.id)
                .put("role", item.role)
                .put("score", item.score)
                .put("answered", item.answered)
                .put("total", item.total)
                .put("date", item.date))
        }
        prefs.edit().putString("sessionHistory", array.toString()).apply()
    }
}

