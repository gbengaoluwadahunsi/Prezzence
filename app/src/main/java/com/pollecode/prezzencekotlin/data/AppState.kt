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

    private val unlimitedAccessEmails = setOf(
        "gbengaoluwadahunsicodes@gmail.com",
        "gbengaoluwadahunsicode@gmail.com",
        "alabiolusola399@gmail.com",
    )

    var onboardingComplete: Boolean
        get() = prefs.getBoolean("onboardingComplete", false)
        set(value) = prefs.edit().putBoolean("onboardingComplete", value).apply()

    var duixModelsPreloaded: Boolean
        get() = prefs.getBoolean("duixModelsPreloaded", false)
        set(value) = prefs.edit().putBoolean("duixModelsPreloaded", value).apply()

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
            val mode = InterviewMode.valueOf(prefs.getString("interviewMode", InterviewMode.SINGLE.name) ?: InterviewMode.SINGLE.name)
            // Free tier: single mode only (Sophia)
            if (!subscriptionEntitled && mode == InterviewMode.PANEL) InterviewMode.SINGLE else mode
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

    fun addPracticeSeconds(seconds: Int) {
        if (seconds <= 0) return
        totalPracticeSeconds = totalPracticeSeconds + seconds
    }

    val practiceMinutes: Int
        get() {
            val seconds = totalPracticeSeconds
            if (seconds <= 0) return 0
            return maxOf(1, (seconds + 59) / 60)
        }

    private var totalPracticeSeconds: Int
        get() = prefs.getInt("totalPracticeSeconds", 0)
        set(value) = prefs.edit().putInt("totalPracticeSeconds", value.coerceAtLeast(0)).apply()

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

    var authRefreshToken: String
        get() = prefs.getString("authRefreshToken", "") ?: ""
        set(value) = prefs.edit().putString("authRefreshToken", value).apply()

    var oauthPkceVerifier: String
        get() = prefs.getString("oauthPkceVerifier", "") ?: ""
        set(value) = prefs.edit().putString("oauthPkceVerifier", value).apply()

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
        get() {
            val email = userEmail.lowercase().trim()
            if (email in unlimitedAccessEmails) return true
            return prefs.getBoolean("subscriptionEntitled", false)
        }
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
        // Free tier: only Sophia (amina) is available
        if (!subscriptionEntitled) {
            return PrezzenceDefaults.interviewers.firstOrNull { it.id == "amina" }
                ?: PrezzenceDefaults.interviewers.last()
        }
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
        lastScore = SessionScoring.sanitizeScore(transcript, score)
        lastTranscript = transcript
        if (lastAnsweredQuestionIndex != currentQuestionIndex) {
            sessionScoreTotal += lastScore
            sessionAnsweredCount += 1
            lastAnsweredQuestionIndex = currentQuestionIndex
        }
        readinessScore = activeSessionScore()
        upsertActiveSessionProgress()
    }

    private fun activeSessionScore(): Int {
        if (activeSessionId.isBlank()) return 0
        val answers = getSessionAnswers(activeSessionId)
        if (answers.isEmpty()) return 0
        return SessionScoring.sessionScore(answers, questions())
    }

    private fun upsertActiveSessionProgress() {
        if (activeSessionId.isBlank()) return
        val answers = getSessionAnswers(activeSessionId)
        val summary = SessionSummary(
            id = activeSessionId,
            role = selectedRole,
            score = SessionScoring.sessionScore(answers, questions()),
            answered = answers.size,
            total = questions().size,
            date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
        )
        writeHistory(listOf(summary) + sessionHistory().filterNot { it.id == summary.id })
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
            val obj = JSONObject()
                .put("transcript", ans.transcript)
                .put("score", ans.score)
                .put("feedback", ans.feedback)
                .put("improvedAnswer", ans.improvedAnswer)
                .put("what", ans.what)
                .put("how", ans.how)
                .put("why", ans.why)
                .put("coachingMessage", ans.coachingMessage)
            ans.presenceMetrics?.let { pm ->
                obj.put("presenceMetrics", JSONObject()
                    .put("faceVisible", pm.faceVisible)
                    .put("faceVisibility", pm.faceVisibility)
                    .put("eyeContact", pm.eyeContact)
                    .put("headStability", pm.headStability)
                    .put("posture", pm.posture)
                    .put("expressionEnergy", pm.expressionEnergy))
            }
            array.put(obj)
        }
        prefs.edit().putString("sessionAnswers_$sessionId", array.toString()).apply()
        saveSessionQuestionsSnapshot(sessionId)
    }

    private fun saveSessionQuestionsSnapshot(sessionId: String) {
        val current = questions()
        if (current.isEmpty()) return
        val array = JSONArray()
        current.forEach { question ->
            array.put(JSONObject()
                .put("id", question.id)
                .put("text", question.text)
                .put("role", question.role)
                .put("interviewerId", question.interviewerId)
                .put("type", question.type))
        }
        prefs.edit().putString("sessionQuestions_$sessionId", array.toString()).apply()
    }

    fun questionsForSession(sessionId: String): List<InterviewQuestion> {
        val raw = prefs.getString("sessionQuestions_$sessionId", "") ?: ""
        val stored = runCatching {
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
        return stored.ifEmpty { questions() }
    }

    fun getSessionAnswers(sessionId: String): List<AnswerResult> {
        val raw = prefs.getString("sessionAnswers_$sessionId", "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                val pmObj = item.optJSONObject("presenceMetrics")
                val pm = pmObj?.let {
                    PresenceMetrics(
                        faceVisible = it.optBoolean("faceVisible", false),
                        faceVisibility = it.optInt("faceVisibility", 0),
                        eyeContact = it.optInt("eyeContact", 0),
                        headStability = it.optInt("headStability", 0),
                        posture = it.optInt("posture", 0),
                        expressionEnergy = it.optInt("expressionEnergy", 0),
                    )
                }
                AnswerResult(
                    transcript = SessionScoring.normalizeStoredTranscript(item.optString("transcript", "")),
                    score = SessionScoring.sanitizeScore(
                        item.optString("transcript", ""),
                        item.optInt("score", 0),
                    ),
                    feedback = item.optString("feedback", ""),
                    improvedAnswer = item.optString("improvedAnswer", ""),
                    what = item.optString("what", ""),
                    how = item.optString("how", ""),
                    why = item.optString("why", ""),
                    coachingMessage = item.optString("coachingMessage", ""),
                    presenceMetrics = pm,
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
        authRefreshToken = ""
        oauthPkceVerifier = ""
        userId = ""
        userEmail = ""
        userFullName = ""
        userFocus = ""
        onboardingComplete = false
        resetActiveSession()
    }

    fun sessionHistory(): List<SessionSummary> {
        val raw = prefs.getString("sessionHistory", "[]") ?: "[]"
        val all = runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                SessionSummary(
                    id = item.optString("id"),
                    role = item.optString("role", PrezzenceDefaults.roles.first()),
                    score = item.optInt("score", 0),
                    answered = item.optInt("answered", 0),
                    total = item.optInt("total", 0),
                    date = item.optString("date", ""),
                    status = item.optString("status", ""),
                )
            }
        }.getOrDefault(emptyList())
            .filterNot { isSessionDeleted(it.id) }
        // Free tier: only show last 5 sessions
        return if (!subscriptionEntitled) all.takeLast(5) else all
    }

    fun isSessionDeleted(id: String): Boolean = id.isNotBlank() && id in deletedSessionIds()

    private fun deletedSessionIds(): Set<String> {
        val raw = prefs.getString("deletedSessionIds", "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                array.optString(index).takeIf { it.isNotBlank() }
            }.toSet()
        }.getOrDefault(emptySet())
    }

    private fun markSessionDeleted(id: String) {
        if (id.isBlank()) return
        val updated = deletedSessionIds().toMutableSet()
        updated.add(id)
        val array = JSONArray()
        updated.take(100).forEach { array.put(it) }
        prefs.edit().putString("deletedSessionIds", array.toString()).apply()
    }

    fun deleteSession(id: String) {
        if (id.isBlank()) return
        markSessionDeleted(id)
        val kept = runCatching {
            val array = JSONArray(prefs.getString("sessionHistory", "[]") ?: "[]")
            (0 until array.length()).mapNotNull { index ->
                val item = array.getJSONObject(index)
                if (item.optString("id") == id) return@mapNotNull null
                SessionSummary(
                    id = item.optString("id"),
                    role = item.optString("role", PrezzenceDefaults.roles.first()),
                    score = item.optInt("score", 0),
                    answered = item.optInt("answered", 0),
                    total = item.optInt("total", 0),
                    date = item.optString("date", ""),
                    status = item.optString("status", ""),
                )
            }
        }.getOrDefault(emptyList())
        writeHistory(kept)
        prefs.edit()
            .remove("sessionAnswers_$id")
            .remove("sessionQuestions_$id")
            .apply()
        completedSessions = kept.count { it.resolvedPracticeStatus() == "Completed" }
        readinessScore = if (kept.isEmpty()) 0 else kept.filter { it.score > 0 }.map { it.score }.average().toInt().coerceIn(0, 100)
    }

    fun clearHistory() {
        writeHistory(emptyList())
        completedSessions = 0
        readinessScore = 0
    }

    private fun saveCurrentSessionSummary() {
        val sessionId = activeSessionId.ifBlank { "session-${System.currentTimeMillis()}" }
        val answers = getSessionAnswers(sessionId)
        saveSessionSummary(
            sessionId = sessionId,
            answered = answers.size,
            score = SessionScoring.sessionScore(answers, questions()),
            total = questions().size,
        )
    }

    fun finalizeSessionForId(sessionId: String, answers: List<AnswerResult>) {
        if (sessionId.isBlank()) return
        saveSessionQuestionsSnapshot(sessionId)
        val sessionQuestions = questionsForSession(sessionId)
        val total = sessionQuestions.size.coerceAtLeast(answers.size.coerceAtLeast(1))
        val score = SessionScoring.sessionScore(answers, sessionQuestions)
        val substantiveCount = SessionScoring.substantiveAnswerCount(answers)
        saveSessionSummary(
            sessionId = sessionId,
            answered = answers.size,
            score = score,
            total = total,
            substantiveCount = substantiveCount,
        )
    }

    private fun saveSessionSummary(
        sessionId: String,
        answered: Int,
        score: Int,
        total: Int,
        substantiveCount: Int = SessionScoring.substantiveAnswerCount(getSessionAnswers(sessionId)),
    ) {
        val summary = SessionSummary(
            id = sessionId,
            role = selectedRole,
            score = score,
            answered = answered,
            total = total,
            date = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date()),
            status = when {
                total > 0 && answered >= total && substantiveCount > 0 && score > 0 -> "completed"
                answered > 0 -> "in_progress"
                else -> "started"
            },
        )
        writeHistory(listOf(summary) + sessionHistory().filterNot { it.id == sessionId })
    }

    fun mergeSessionHistory(remote: List<SessionSummary>) {
        if (remote.isEmpty()) return
        val localById = sessionHistory().associateBy { it.id }
        val mergedRemote = remote.filterNot { isSessionDeleted(it.id) }.map { remoteItem ->
            val local = localById[remoteItem.id]
            val answers = getSessionAnswers(remoteItem.id)
            val recomputedScore = if (answers.isNotEmpty()) {
                SessionScoring.sessionScore(answers, questionsForSession(remoteItem.id))
            } else {
                null
            }
            val answered = if (answers.isNotEmpty()) answers.size else maxOf(remoteItem.answered, local?.answered ?: 0)
            remoteItem.copy(
                score = recomputedScore ?: local?.score?.takeIf { it > 0 } ?: remoteItem.score,
                answered = answered,
                total = listOf(remoteItem.total, local?.total ?: 0, answers.size).max(),
                role = remoteItem.role.ifBlank { local?.role.orEmpty() }.ifBlank { "Interview" },
                status = remoteItem.status.ifBlank { local?.status.orEmpty() },
            )
        }
        val remoteIds = mergedRemote.map { it.id }.toSet()
        val extras = sessionHistory().filterNot { it.id in remoteIds }
        writeHistory((mergedRemote + extras).take(30))
        completedSessions = (mergedRemote + extras).size
        val scored = (mergedRemote + extras).filter { it.score > 0 }
        readinessScore = if (scored.isEmpty()) readinessScore else scored.map { it.score }.average().toInt().coerceIn(0, 100)
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
                .put("date", item.date)
                .put("status", item.status))
        }
        prefs.edit().putString("sessionHistory", array.toString()).apply()
    }
}

