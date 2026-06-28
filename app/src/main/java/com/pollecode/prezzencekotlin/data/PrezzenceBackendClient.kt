package com.pollecode.prezzencekotlin.data

import com.pollecode.prezzencekotlin.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class PrezzenceBackendClient {
    private val baseUrl = BuildConfig.PREZZENCE_API_URL.trimEnd('/')
    private val supabaseUrl = BuildConfig.PREZZENCE_SUPABASE_URL.trimEnd('/')
    private val supabaseAnonKey = BuildConfig.PREZZENCE_SUPABASE_ANON_KEY
    private val emailVerificationRedirectUrl = "$baseUrl/auth/verified"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(600, TimeUnit.SECONDS) // 10 minutes - match React Native's indefinite wait
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun health(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url("$baseUrl/health").get().build()
            client.newCall(request).execute().use { response -> response.isSuccessful }
        }.getOrDefault(false)
    }

    fun newPkceVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    fun googleOAuthUrl(redirectUri: String, codeVerifier: String): String {
        val encodedRedirect = URLEncoder.encode(redirectUri, "UTF-8")
        val challengeBytes = MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray(Charsets.US_ASCII))
        val codeChallenge = Base64.encodeToString(challengeBytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        return "$supabaseUrl/auth/v1/authorize?provider=google&redirect_to=$encodedRedirect&code_challenge=$codeChallenge&code_challenge_method=s256"
    }

    suspend fun exchangePkceCode(
        authCode: String,
        codeVerifier: String,
        redirectUri: String = "prezzence://auth/callback",
    ): AuthSession? = withContext(Dispatchers.IO) {
        if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank() || authCode.isBlank() || codeVerifier.isBlank()) return@withContext null
        parsePkceTokenResponse(exchangePkceToken(authCode, codeVerifier, redirectUri, codeField = "auth_code"))
            ?: parsePkceTokenResponse(exchangePkceToken(authCode, codeVerifier, redirectUri, codeField = "code"))
    }

    private fun exchangePkceToken(
        authCode: String,
        codeVerifier: String,
        redirectUri: String,
        codeField: String,
    ): String? = runCatching {
        val body = JSONObject()
            .put(codeField, authCode)
            .put("code_verifier", codeVerifier)
            .put("redirect_to", redirectUri)
            .toString()
            .toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("$supabaseUrl/auth/v1/token?grant_type=pkce")
            .header("apikey", supabaseAnonKey)
            .header("Authorization", "Bearer $supabaseAnonKey")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.w("PrezzenceAuth", "PKCE exchange failed (${response.code}) with $codeField: $raw")
                return@use null
            }
            raw
        }
    }.getOrNull()

    private fun parsePkceTokenResponse(raw: String?): AuthSession? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val json = JSONObject(raw)
            val user = json.optJSONObject("user")
            AuthSession(
                accessToken = json.optString("access_token"),
                userId = user?.optString("id").orEmpty(),
                email = user?.optString("email").orEmpty(),
                fullName = user?.optJSONObject("user_metadata")?.optString("full_name").orEmpty(),
                focus = user?.optJSONObject("user_metadata")?.optString("focus").orEmpty(),
                refreshToken = json.optString("refresh_token", ""),
            ).takeIf { it.accessToken.isNotBlank() && it.userId.isNotBlank() }
        }.getOrNull()
    }

    suspend fun signInWithPassword(email: String, password: String): AuthSession? = withContext(Dispatchers.IO) {
        if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank()) return@withContext null
        runCatching {
            val body = JSONObject()
                .put("email", email.trim())
                .put("password", password)
                .toString()
                .toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/token?grant_type=password")
                .header("apikey", supabaseAnonKey)
                .header("Authorization", "Bearer $supabaseAnonKey")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val json = JSONObject(response.body?.string().orEmpty())
                val user = json.optJSONObject("user")
                AuthSession(
                    accessToken = json.optString("access_token"),
                    userId = user?.optString("id").orEmpty(),
                    email = user?.optString("email").orEmpty().ifBlank { email.trim() },
                    fullName = user?.optJSONObject("user_metadata")?.optString("full_name").orEmpty(),
                    focus = user?.optJSONObject("user_metadata")?.optString("focus").orEmpty(),
                    refreshToken = json.optString("refresh_token", ""),
                ).takeIf { it.accessToken.isNotBlank() }
            }
        }.getOrNull()
    }

    suspend fun signUpWithPassword(email: String, password: String, fullName: String = "", redirectUri: String = emailVerificationRedirectUrl): Boolean = withContext(Dispatchers.IO) {
        if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank()) return@withContext false
        runCatching {
            val data = JSONObject()
            if (fullName.isNotBlank()) data.put("full_name", fullName.trim())
            val body = JSONObject()
                .put("email", email.trim())
                .put("password", password)
                .put("data", data)
                .put("options", JSONObject().put("email_redirect_to", redirectUri))
                .toString()
                .toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/signup")
                .header("apikey", supabaseAnonKey)
                .header("Authorization", "Bearer $supabaseAnonKey")
                .post(body)
                .build()
            client.newCall(request).execute().use { response -> response.isSuccessful }
        }.getOrDefault(false)
    }

    suspend fun requestPasswordReset(email: String, redirectUri: String = "prezzence://auth/reset-password"): Boolean = withContext(Dispatchers.IO) {
        if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank()) return@withContext false
        runCatching {
            val body = JSONObject()
                .put("email", email.trim())
                .put("redirect_to", redirectUri)
                .toString()
                .toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/recover")
                .header("apikey", supabaseAnonKey)
                .header("Authorization", "Bearer $supabaseAnonKey")
                .post(body)
                .build()
            client.newCall(request).execute().use { response -> response.isSuccessful }
        }.getOrDefault(false)
    }

    suspend fun sessionFromAccessToken(accessToken: String): AuthSession? = withContext(Dispatchers.IO) {
        if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank() || accessToken.isBlank()) return@withContext null
        runCatching {
            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/user")
                .header("apikey", supabaseAnonKey)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val user = JSONObject(response.body?.string().orEmpty())
                AuthSession(
                    accessToken = accessToken,
                    userId = user.optString("id"),
                    email = user.optString("email"),
                    fullName = user.optJSONObject("user_metadata")?.optString("full_name").orEmpty(),
                    focus = user.optJSONObject("user_metadata")?.optString("focus").orEmpty(),
                ).takeIf { it.accessToken.isNotBlank() && it.userId.isNotBlank() }
            }
        }.getOrNull()
    }

    suspend fun refreshSession(refreshToken: String): AuthSession? = withContext(Dispatchers.IO) {
        if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank() || refreshToken.isBlank()) return@withContext null
        runCatching {
            val body = JSONObject()
                .put("refresh_token", refreshToken)
                .toString()
                .toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/token?grant_type=refresh_token")
                .header("apikey", supabaseAnonKey)
                .header("Authorization", "Bearer $supabaseAnonKey")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val json = JSONObject(response.body?.string().orEmpty())
                val user = json.optJSONObject("user")
                AuthSession(
                    accessToken = json.optString("access_token"),
                    userId = user?.optString("id").orEmpty(),
                    email = user?.optString("email").orEmpty(),
                    fullName = user?.optJSONObject("user_metadata")?.optString("full_name").orEmpty(),
                    focus = user?.optJSONObject("user_metadata")?.optString("focus").orEmpty(),
                    refreshToken = json.optString("refresh_token", refreshToken),
                ).takeIf { it.accessToken.isNotBlank() && it.userId.isNotBlank() }
            }
        }.getOrNull()
    }

    suspend fun updatePassword(accessToken: String, password: String): Boolean = withContext(Dispatchers.IO) {
        if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank() || accessToken.isBlank() || password.isBlank()) return@withContext false
        runCatching {
            val body = JSONObject()
                .put("password", password)
                .toString()
                .toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/user")
                .header("apikey", supabaseAnonKey)
                .header("Authorization", "Bearer $accessToken")
                .put(body)
                .build()
            client.newCall(request).execute().use { response -> response.isSuccessful }
        }.getOrDefault(false)
    }

    suspend fun updateUserProfile(accessToken: String, fullName: String, focus: String): Boolean = withContext(Dispatchers.IO) {
        if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank() || accessToken.isBlank()) return@withContext false
        runCatching {
            val data = JSONObject()
            data.put("full_name", fullName.trim())
            data.put("focus", focus.trim())
            
            val body = JSONObject()
                .put("data", data)
                .toString()
                .toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/user")
                .header("apikey", supabaseAnonKey)
                .header("Authorization", "Bearer $accessToken")
                .put(body)
                .build()
            client.newCall(request).execute().use { response -> response.isSuccessful }
        }.getOrDefault(false)
    }

    suspend fun createSession(
        bearerToken: String?,
        role: String,
        mode: InterviewMode,
        language: String = "en",
        interviewTrack: String = "job",
        industry: String = "General Business",
        seniority: String = "Mid",
        difficulty: String = "Realistic",
        companyName: String = "",
        companyWebsite: String = "",
        companyContext: String = "",
        enableWebResearch: Boolean = false,
        includeTechnical: Boolean = false,
        interviewerStyle: String = "Balanced",
        previewGender: String = "Female",
        length: String = "standard",
    ): BackendSession = withContext(Dispatchers.IO) {
        if (bearerToken.isNullOrBlank()) throw SessionCreateException(SessionErrorReason.AUTH_FAILED, "No auth token available")
        try {
            val panelIds = panelPersonaIds(mode, interviewerStyle, previewGender)
            val panel = JSONArray().apply {
                panelIds.forEachIndexed { index, id ->
                    put(JSONObject().put("persona_id", id).put("seat", listOf("left", "centre", "right").getOrElse(index) { "centre" }))
                }
            }
            val bodyJson = JSONObject()
                .put("role_title", role.ifBlank { "Candidate" })
                .put("industry", industry.ifBlank { "General Business" })
                .put("seniority", seniority.ifBlank { "Mid" }.lowercase())
                .put("interview_type", interviewTrack.ifBlank { "job" }.lowercase())
                .put("difficulty", difficulty.ifBlank { "Realistic" }.lowercase())
                .put("length", length.ifBlank { "standard" }.lowercase())
                .put("panel_config", panel)
                .put("enable_web_research", enableWebResearch)
                .put("include_technical", includeTechnical)
                .put("language", language)
            if (companyName.isNotBlank()) bodyJson.put("company_name", companyName.trim())
            if (companyWebsite.isNotBlank()) bodyJson.put("company_website", companyWebsite.trim())
            if (companyContext.isNotBlank()) bodyJson.put("company_context", companyContext.trim())
            val body = bodyJson
                .toString()
                .toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("$baseUrl/api/sessions/create")
                .header("Authorization", "Bearer $bearerToken")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val bodyText = response.body?.string().orEmpty()
                    throw when (code) {
                        401, 403 -> SessionCreateException(SessionErrorReason.AUTH_FAILED, "Auth rejected ($code): $bodyText")
                        402 -> SessionCreateException(SessionErrorReason.AUTH_FAILED, "Payment required: $bodyText")
                        408, 504 -> SessionCreateException(SessionErrorReason.SERVER_TIMEOUT, "Server timeout ($code)")
                        in 500..599 -> SessionCreateException(SessionErrorReason.SERVER_ERROR, "Server error ($code): $bodyText")
                        else -> SessionCreateException(SessionErrorReason.UNKNOWN, "Unexpected status $code: $bodyText")
                    }
                }
                val json = JSONObject(response.body?.string().orEmpty())
                val sessionId = json.optString("session_id")
                if (sessionId.isBlank()) throw SessionCreateException(SessionErrorReason.SERVER_ERROR, "Empty session_id in response")
                BackendSession(
                    sessionId = sessionId,
                    questions = json.optJSONArray("questions").toInterviewQuestions(role, panelIds),
                )
            }
        } catch (e: SessionCreateException) {
            throw e
        } catch (e: java.net.ConnectException) {
            throw SessionCreateException(SessionErrorReason.NETWORK_UNAVAILABLE, "Connection refused")
        } catch (e: java.net.SocketTimeoutException) {
            throw SessionCreateException(SessionErrorReason.SERVER_TIMEOUT, "Socket timeout")
        } catch (e: java.net.UnknownHostException) {
            throw SessionCreateException(SessionErrorReason.NETWORK_UNAVAILABLE, "DNS resolution failed")
        } catch (e: Exception) {
            throw SessionCreateException(SessionErrorReason.UNKNOWN, e.message ?: "Unknown error")
        }
    }

    private fun panelPersonaIds(
        mode: InterviewMode,
        interviewerStyle: String,
        previewGender: String,
    ): List<String> {
        if (mode == InterviewMode.PANEL) {
            return when (interviewerStyle.lowercase()) {
                "supportive" -> listOf("amina", "maya", "jonas")
                "challenging" -> listOf("jonas", "maya", "amina")
                else -> listOf("maya", "jonas", "amina")
            }
        }
        when (interviewerStyle.lowercase()) {
            "supportive", "friendly", "warm", "encouraging" -> return listOf("maya")
            "challenging", "tough", "direct", "skeptical" -> return listOf("jonas")
        }
        return if (previewGender.equals("Male", ignoreCase = true)) listOf("jonas") else listOf("amina")
    }

    suspend fun synthesizeSpeechUrl(
        bearerToken: String?,
        text: String,
        language: String,
        personality: String = "neutral",
    ): String? = withContext(Dispatchers.IO) {
        if (text.isBlank() || bearerToken.isNullOrBlank()) return@withContext null
        runCatching {
            val body = JSONObject()
                .put("text", text.trim())
                .put("personality", personality)
                .put("lang", language.ifBlank { "en" })
                .toString()
                .toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$baseUrl/api/tts/synthesize")
                .header("Authorization", "Bearer $bearerToken")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                val responseText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.w("PrezzenceTTS", "Backend TTS failed ${response.code}: ${responseText.take(180)}")
                    return@use null
                }
                val audioUrl = JSONObject(responseText).optString("audio_url")
                Log.i("PrezzenceTTS", "Backend TTS URL received format=${JSONObject(responseText).optString("format")} urlBlank=${audioUrl.isBlank()}")
                when {
                    audioUrl.startsWith("http://") || audioUrl.startsWith("https://") -> audioUrl
                    audioUrl.startsWith("/") -> "$baseUrl$audioUrl"
                    audioUrl.isNotBlank() -> "$baseUrl/$audioUrl"
                    else -> null
                }
            }
        }.getOrNull()
    }

    private suspend fun withTokenRetry(
        token: String,
        refreshToken: String?,
        onTokenRefreshed: ((AuthSession) -> Unit)? = null,
        block: suspend (String) -> Boolean,
    ): Boolean {
        if (block(token)) return true
        if (refreshToken.isNullOrBlank()) return false
        val refreshed = refreshSession(refreshToken) ?: return false
        onTokenRefreshed?.invoke(refreshed)
        return block(refreshed.accessToken)
    }

    suspend fun listSessions(
        bearerToken: String?,
        refreshToken: String? = null,
        onTokenRefreshed: ((AuthSession) -> Unit)? = null,
    ): List<SessionSummary> = withContext(Dispatchers.IO) {
        if (bearerToken.isNullOrBlank()) return@withContext emptyList()
        suspend fun fetch(token: String): List<SessionSummary>? {
            val request = Request.Builder()
                .url("$baseUrl/api/sessions/")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            return client.newCall(request).execute().use { response ->
                if (response.code == 401) return@use null
                if (!response.isSuccessful) return@use emptyList()
                val sessions = JSONObject(response.body?.string().orEmpty()).optJSONArray("sessions") ?: JSONArray()
                (0 until sessions.length()).map { index ->
                    val item = sessions.getJSONObject(index)
                    val title = item.optString("title", "Interview Assessment")
                    SessionSummary(
                        id = item.optString("id"),
                        role = title.removeSuffix(" Assessment").ifBlank { "Interview" },
                        score = item.optInt("score", 0),
                        answered = item.optInt("answered", 0),
                        total = item.optInt("total", 0),
                        date = item.optString("date", ""),
                        status = item.optString("status", ""),
                    )
                }
            }
        }
        runCatching {
            fetch(bearerToken) ?: run {
                if (refreshToken.isNullOrBlank()) return@runCatching emptyList()
                val refreshed = refreshSession(refreshToken) ?: return@runCatching emptyList()
                onTokenRefreshed?.invoke(refreshed)
                fetch(refreshed.accessToken) ?: emptyList()
            }
        }.getOrDefault(emptyList())
    }

    suspend fun completeSession(
        bearerToken: String?,
        sessionId: String,
        refreshToken: String? = null,
        onTokenRefreshed: ((AuthSession) -> Unit)? = null,
    ): Boolean = withContext(Dispatchers.IO) {
        if (bearerToken.isNullOrBlank() || sessionId.isBlank()) return@withContext false
        suspend fun doComplete(token: String): Boolean {
            val body = "{}".toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$baseUrl/api/sessions/$sessionId/complete")
                .header("Authorization", "Bearer $token")
                .patch(body)
                .build()
            return client.newCall(request).execute().use { it.isSuccessful }
        }
        runCatching {
            withTokenRetry(bearerToken, refreshToken, onTokenRefreshed) { doComplete(it) }
        }.getOrDefault(false)
    }

    suspend fun deleteSession(bearerToken: String?, sessionId: String): Boolean = withContext(Dispatchers.IO) {
        if (bearerToken.isNullOrBlank() || sessionId.isBlank()) return@withContext false
        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/api/sessions/$sessionId")
                .header("Authorization", "Bearer $bearerToken")
                .delete()
                .build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        }.getOrDefault(false)
    }

    suspend fun deleteSessionWithAuthRetry(
        bearerToken: String,
        refreshToken: String,
        sessionId: String,
        onTokenRefreshed: (AuthSession) -> Unit = {},
    ): Boolean {
        if (sessionId.isBlank()) return false
        if (deleteSession(bearerToken, sessionId)) return true
        if (refreshToken.isBlank()) return false
        val refreshed = refreshSession(refreshToken) ?: return false
        onTokenRefreshed(refreshed)
        return deleteSession(refreshed.accessToken, sessionId)
    }

    suspend fun getResumeProfile(bearerToken: String?): ResumeProfile? = withContext(Dispatchers.IO) {
        if (bearerToken.isNullOrBlank()) return@withContext null
        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/api/users/me/resume-profile")
                .header("Authorization", "Bearer $bearerToken")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val profile = JSONObject(response.body?.string().orEmpty()).optJSONObject("profile") ?: return@use null
                profile.toResumeProfile()
            }
        }.getOrNull()
    }

    suspend fun uploadResumeFile(
        bearerToken: String?,
        fileName: String,
        mimeType: String?,
        data: ByteArray,
    ): ResumeProfile? = withContext(Dispatchers.IO) {
        if (bearerToken.isNullOrBlank() || data.isEmpty()) return@withContext null
        runCatching {
            val mediaType = (mimeType ?: "application/octet-stream").toMediaTypeOrNull()
            val fileBody = data.toRequestBody(mediaType)
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName.ifBlank { "resume" }, fileBody)
                .build()
            val request = Request.Builder()
                .url("$baseUrl/api/users/me/resume-profile")
                .header("Authorization", "Bearer $bearerToken")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val profile = JSONObject(response.body?.string().orEmpty()).optJSONObject("profile") ?: return@use null
                profile.toResumeProfile()
            }
        }.getOrNull()
    }
    suspend fun saveResumeText(bearerToken: String?, text: String, fileName: String = "Pasted resume"): ResumeProfile? = withContext(Dispatchers.IO) {
        if (bearerToken.isNullOrBlank() || text.isBlank()) return@withContext null
        runCatching {
            val body = JSONObject()
                .put("text", text)
                .put("file_name", fileName)
                .toString()
                .toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$baseUrl/api/users/me/resume-profile/text")
                .header("Authorization", "Bearer $bearerToken")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val profile = JSONObject(response.body?.string().orEmpty()).optJSONObject("profile") ?: return@use null
                profile.toResumeProfile()
            }
        }.getOrNull()
    }

    suspend fun deleteResumeProfile(bearerToken: String?): Boolean = withContext(Dispatchers.IO) {
        if (bearerToken.isNullOrBlank()) return@withContext false
        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/api/users/me/resume-profile")
                .header("Authorization", "Bearer $bearerToken")
                .delete()
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    suspend fun getNotifications(bearerToken: String?): List<NotificationItem> = withContext(Dispatchers.IO) {
        if (bearerToken.isNullOrBlank()) return@withContext emptyList()
        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/api/users/me/notifications")
                .header("Authorization", "Bearer $bearerToken")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList()
                val raw = response.body?.string().orEmpty()
                val array = if (raw.trim().startsWith("[")) JSONArray(raw) else JSONObject(raw).optJSONArray("notifications") ?: JSONArray()
                (0 until array.length()).map { index -> array.getJSONObject(index).toNotificationItem() }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun fetchNotificationPreferences(bearerToken: String?): NotificationPreferences? = withContext(Dispatchers.IO) {
        if (bearerToken.isNullOrBlank()) return@withContext null
        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/api/users/me/notification-preferences")
                .header("Authorization", "Bearer $bearerToken")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val prefs = JSONObject(response.body?.string().orEmpty()).optJSONObject("preferences") ?: return@use null
                prefs.toNotificationPreferences()
            }
        }.getOrNull()
    }

    suspend fun updateNotificationPreferences(
        bearerToken: String?,
        preferences: NotificationPreferences,
    ): NotificationPreferences? = withContext(Dispatchers.IO) {
        if (bearerToken.isNullOrBlank()) return@withContext null
        runCatching {
            val body = JSONObject()
                .put("push_notifications_enabled", preferences.pushNotificationsEnabled)
                .put("email_summaries_enabled", preferences.emailSummariesEnabled)
                .put("practice_reminders_enabled", preferences.practiceRemindersEnabled)
                .put("achievement_alerts_enabled", preferences.achievementAlertsEnabled)
                .put("product_updates_enabled", preferences.productUpdatesEnabled)
                .toString()
                .toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$baseUrl/api/users/me/notification-preferences")
                .header("Authorization", "Bearer $bearerToken")
                .put(body)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val prefs = JSONObject(response.body?.string().orEmpty()).optJSONObject("preferences") ?: return@use null
                prefs.toNotificationPreferences()
            }
        }.getOrNull()
    }

    suspend fun markNotificationRead(bearerToken: String?, notificationId: String): Boolean = withContext(Dispatchers.IO) {
        if (bearerToken.isNullOrBlank() || notificationId.isBlank()) return@withContext false
        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/api/users/me/notifications/$notificationId/read")
                .header("Authorization", "Bearer $bearerToken")
                .post("{}".toRequestBody(jsonMediaType))
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    suspend fun deleteNotification(bearerToken: String?, notificationId: String): Boolean = withContext(Dispatchers.IO) {
        if (bearerToken.isNullOrBlank() || notificationId.isBlank()) return@withContext false
        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/api/users/me/notifications/$notificationId")
                .header("Authorization", "Bearer $bearerToken")
                .delete()
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    suspend fun submitFeedback(bearerToken: String?, sessionId: String?, rating: Int, comment: String): Boolean = withContext(Dispatchers.IO) {
        if (bearerToken.isNullOrBlank() || comment.isBlank()) return@withContext false
        runCatching {
            val body = JSONObject()
                .put("session_id", sessionId)
                .put("rating", rating.coerceIn(1, 5))
                .put("comment", comment.trim())
                .toString()
                .toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$baseUrl/api/feedback")
                .header("Authorization", "Bearer $bearerToken")
                .post(body)
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    suspend fun deleteAccount(bearerToken: String?): Boolean = withContext(Dispatchers.IO) {
        if (bearerToken.isNullOrBlank()) return@withContext false
        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/api/users/me")
                .header("Authorization", "Bearer $bearerToken")
                .delete()
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    suspend fun fetchModelAnswer(
        bearerToken: String,
        questionText: String,
        transcript: String,
        roleTitle: String = "",
        interviewerName: String = "",
        interviewerTitle: String = "",
        refreshToken: String? = null,
        onTokenRefreshed: ((AuthSession) -> Unit)? = null,
    ): AnswerResult? = withContext(Dispatchers.IO) {
        if (bearerToken.isBlank() || questionText.isBlank()) return@withContext null
        suspend fun doFetch(token: String): AnswerResult? {
            val body = JSONObject()
                .put("question_text", questionText)
                .put("transcript", transcript)
                .put("role_title", roleTitle)
                .put("interviewer_name", interviewerName)
                .put("interviewer_title", interviewerTitle)
                .toString()
                .toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$baseUrl/api/sessions/coaching/model-answer")
                .header("Authorization", "Bearer $token")
                .post(body)
                .build()
            return client.newCall(request).execute().use { response ->
                if (response.code == 401) return@use null
                if (!response.isSuccessful) return@use null
                val root = JSONObject(response.body?.string().orEmpty())
                val breakdown = root.optJSONObject("coaching_breakdown")
                AnswerResult(
                    transcript = transcript,
                    score = 0,
                    feedback = "",
                    improvedAnswer = root.optString("improved_answer", ""),
                    what = breakdown?.optString("what_to_include", "") ?: "",
                    how = breakdown?.optString("how_to_structure", "") ?: "",
                    why = breakdown?.optString("why_it_works", "") ?: "",
                    coachingMessage = root.optString("coaching_message", ""),
                ).takeIf { it.improvedAnswer.isNotBlank() }
            }
        }
        runCatching {
            doFetch(bearerToken) ?: run {
                if (refreshToken.isNullOrBlank()) return@runCatching null
                val refreshed = refreshSession(refreshToken) ?: return@runCatching null
                onTokenRefreshed?.invoke(refreshed)
                doFetch(refreshed.accessToken)
            }
        }.getOrNull()
    }

    suspend fun fetchTopicLesson(
        bearerToken: String,
        questionText: String,
        roleTitle: String = "",
        refreshToken: String? = null,
        onTokenRefreshed: ((AuthSession) -> Unit)? = null,
    ): TopicLesson? = withContext(Dispatchers.IO) {
        if (bearerToken.isBlank() || questionText.isBlank()) return@withContext null
        suspend fun doFetch(token: String): TopicLesson? {
            val body = JSONObject()
                .put("question_text", questionText)
                .put("role_title", roleTitle)
                .toString()
                .toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$baseUrl/api/sessions/coaching/learn-topic")
                .header("Authorization", "Bearer $token")
                .post(body)
                .build()
            return client.newCall(request).execute().use { response ->
                if (response.code == 401) return@use null
                if (!response.isSuccessful) return@use null
                val root = JSONObject(response.body?.string().orEmpty())
                TopicLesson(
                    topic = root.optString("topic", ""),
                    lesson = root.optString("lesson", ""),
                ).takeIf { it.lesson.isNotBlank() }
            }
        }
        runCatching {
            doFetch(bearerToken) ?: run {
                if (refreshToken.isNullOrBlank()) return@runCatching null
                val refreshed = refreshSession(refreshToken) ?: return@runCatching null
                onTokenRefreshed?.invoke(refreshed)
                doFetch(refreshed.accessToken)
            }
        }.getOrNull()
    }

    suspend fun scoreWithBackend(
        bearerToken: String?,
        sessionId: String,
        questionId: Int,
        questionText: String,
        transcript: String,
        audioBase64: String? = null,
        audioDurationSeconds: Int = 0,
        refreshToken: String? = null,
        onTokenRefreshed: ((newAccessToken: String) -> Unit)? = null,
    ): BackendScoreResult = withContext(Dispatchers.IO) {
        if (bearerToken.isNullOrBlank() || sessionId.isBlank()) {
            return@withContext BackendScoreResult(
                failureMessage = "Sign in and start a session to score your answer.",
            )
        }

        fun buildRequestBody(): RequestBody {
            val body = JSONObject()
                .put("question_id", questionId)
                .put("question_text", questionText)
                .put("transcript_source", "backend-audio")
            if (transcript.isNotBlank()) {
                body.put("transcript", transcript)
            }
            if (!audioBase64.isNullOrBlank()) {
                body.put("audio_base64", audioBase64)
                body.put("audio_mime_type", "audio/wav")
                body.put("audio_duration_seconds", audioDurationSeconds.coerceAtLeast(1))
            }
            return body.toString().toRequestBody(jsonMediaType)
        }

        suspend fun executeRequest(token: String): BackendScoreResult {
            val requestBody = buildRequestBody()
            Log.i("PrezzenceBackend", "Sending answer: audioLen=${audioBase64?.length ?: 0}, duration=$audioDurationSeconds, session=$sessionId, q=$questionId")

            val request = Request.Builder()
                .url("$baseUrl/api/sessions/$sessionId/answers")
                .header("Authorization", "Bearer $token")
                .post(requestBody)
                .build()

            return client.newCall(request).execute().use { response ->
                Log.i("PrezzenceBackend", "Answer response: code=${response.code}")
                if (!response.isSuccessful) {
                    val raw = response.body?.string()?.take(500).orEmpty()
                    Log.w("PrezzenceBackend", "Backend error: ${response.code} $raw")
                    val message = when (response.code) {
                        401 -> "Your session expired. Sign in again and try again."
                        in 500..599 -> "Our servers are busy. Please wait a moment and try again."
                        408, 429 -> "The request timed out. Try again with a shorter answer."
                        else -> "We could not score your answer. Check your connection and try again."
                    }
                    return@use BackendScoreResult(failureCode = response.code, failureMessage = message)
                }
                val responseBody = response.body?.string().orEmpty()
                val root = JSONObject(responseBody)
                val analysis = root.optJSONObject("analysis")
                if (analysis == null) {
                    Log.w("PrezzenceBackend", "No analysis in response: ${responseBody.take(500)}")
                    return@use BackendScoreResult(
                        failureMessage = "We could not analyze your answer. Please try again.",
                    )
                }
                val transcript_result = root.optString("transcript", transcript)
                val retry = root.optBoolean("retry_required", false)
                val score = analysis.optInt("score", 0)
                val source = root.optJSONObject("performance")?.optString("analysis_source", "")
                Log.i("PrezzenceBackend", "Result: transcript=${transcript_result.take(80)}, score=$score, retry=$retry, source=$source")
                val rawTranscript = root.optString("transcript", transcript)
                val backendScore = analysis.optInt("score", 0)
                val resolvedScore = if (SessionScoring.isBlankTranscript(rawTranscript)) 0 else backendScore
                return@use BackendScoreResult(
                    answer = AnswerResult(
                        transcript = rawTranscript,
                        score = resolvedScore,
                        feedback = analysis.optString("feedback", ""),
                        improvedAnswer = analysis.optString("improved_answer", ""),
                        what = analysis.optJSONObject("coaching_breakdown")?.optString("what_to_include", "") ?: "",
                        how = analysis.optJSONObject("coaching_breakdown")?.optString("how_to_structure", "") ?: "",
                        why = analysis.optJSONObject("coaching_breakdown")?.optString("why_it_works", "") ?: "",
                        coachingMessage = analysis.optString("coaching_message", ""),
                        retryRequired = root.optBoolean("retry_required", false),
                    ),
                )
            }
        }

        val firstResult = executeRequest(bearerToken)
        if (firstResult.answer != null || firstResult.failureCode == 401) return@withContext firstResult

        if (!refreshToken.isNullOrBlank()) {
            Log.i("PrezzenceBackend", "First attempt failed; attempting token refresh before retry...")
            val refreshed = refreshSession(refreshToken)
            if (refreshed != null) {
                Log.i("PrezzenceBackend", "Token refreshed successfully, retrying answer submission")
                onTokenRefreshed?.invoke(refreshed.accessToken)
                val retryResult = executeRequest(refreshed.accessToken)
                if (retryResult.answer != null || retryResult.failureCode == 401) return@withContext retryResult
            } else {
                Log.w("PrezzenceBackend", "Token refresh failed")
            }
        }
        BackendScoreResult(
            failureMessage = "We could not reach the server. Check your connection and try again.",
        )
    }

    suspend fun fetchUserProgress(
        bearerToken: String,
        userId: String,
        language: String = "en",
        refreshToken: String? = null,
        onTokenRefreshed: ((AuthSession) -> Unit)? = null,
    ): UserProgressSnapshot? = withContext(Dispatchers.IO) {
        if (bearerToken.isBlank() || userId.isBlank()) return@withContext null

        suspend fun doFetch(token: String): UserProgressSnapshot? {
            val encodedLanguage = URLEncoder.encode(language.ifBlank { "en" }, "UTF-8")
            val request = Request.Builder()
                .url("$baseUrl/api/users/$userId/progress?language=$encodedLanguage")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            return client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("PrezzenceBackend", "Progress fetch failed: ${response.code}")
                    return@use null
                }
                parseUserProgress(JSONObject(response.body?.string().orEmpty()))
            }
        }

        doFetch(bearerToken) ?: run {
            if (refreshToken.isNullOrBlank()) return@withContext null
            val refreshed = refreshSession(refreshToken) ?: return@withContext null
            onTokenRefreshed?.invoke(refreshed)
            doFetch(refreshed.accessToken)
        }
    }

    private fun parseUserProgress(json: JSONObject): UserProgressSnapshot {
        val stats = json.optJSONObject("stats")
        val improvement = json.optJSONObject("improvement")
        val readiness = json.optJSONObject("readiness")
        val skillFocus = json.optJSONObject("skill_focus")
        val coachingPlan = json.optJSONArray("coaching_plan")?.let { array ->
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optString(index).trim()
                    if (item.isNotBlank()) add(item)
                }
            }
        }.orEmpty()
        val radarData = json.optJSONArray("radar_data")?.let { array ->
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val label = item.optString("label").trim()
                    if (label.isNotBlank()) add(label to item.optInt("value", 0))
                }
            }
        }.orEmpty()

        return UserProgressSnapshot(
            avgScore = stats?.optInt("avg_score", 0) ?: 0,
            sessions = stats?.optInt("sessions", 0) ?: 0,
            practiceHours = stats?.optDouble("practice_hours", 0.0)?.toFloat() ?: 0f,
            growth = json.optInt("growth", improvement?.optInt("from_previous", 0) ?: 0),
            improvementFromFirst = improvement?.optInt("from_first", 0) ?: 0,
            coachingTip = json.optString("coaching_tip", ""),
            readinessLabel = readiness?.optString("label", "") ?: "",
            readinessScore = readiness?.optInt("score", stats?.optInt("avg_score", 0) ?: 0) ?: 0,
            strongestSkill = skillFocus?.optString("strongest", "").orEmpty(),
            strongestScore = skillFocus?.optInt("strongest_score", 0) ?: 0,
            weakestSkill = skillFocus?.optString("weakest", "").orEmpty(),
            weakestScore = skillFocus?.optInt("weakest_score", 0) ?: 0,
            coachingPlan = coachingPlan,
            radarData = radarData,
        )
    }

    suspend fun fetchEntitlement(bearerToken: String): EntitlementStatus = withContext(Dispatchers.IO) {
        if (bearerToken.isBlank()) return@withContext EntitlementStatus(false)
        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/api/users/me/entitlement")
                .header("Authorization", "Bearer $bearerToken")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use EntitlementStatus(false)
                val body = JSONObject(response.body?.string().orEmpty())
                EntitlementStatus(
                    isPremium = body.optBoolean("is_premium", false),
                    betaUnlockAllFeatures = body.optBoolean("beta_unlock_all_features", false),
                    adminAccess = body.optBoolean("admin_access", false),
                )
            }
        }.getOrDefault(EntitlementStatus(false))
    }

    suspend fun syncEntitlement(
        bearerToken: String,
        purchaseToken: String,
        productId: String,
        packageName: String,
        orderId: String? = null,
    ): Boolean = withContext(Dispatchers.IO) {
        if (bearerToken.isBlank() || purchaseToken.isBlank()) return@withContext false
        runCatching {
            val body = JSONObject()
                .put("purchase_token", purchaseToken)
                .put("product_id", productId)
                .put("package_name", packageName)
            if (!orderId.isNullOrBlank()) body.put("order_id", orderId)
            val request = Request.Builder()
                .url("$baseUrl/api/billing/google/sync")
                .header("Authorization", "Bearer $bearerToken")
                .header("Content-Type", "application/json")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful && JSONObject(response.body?.string().orEmpty()).optBoolean("is_premium", false)
            }
        }.getOrDefault(false)
    }

    private fun buildCoachingMessage(question: String, transcript: String, score: Int): String {
        val words = transcript.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        
        return when {
            score < 15 -> {
                "Let's try that again. Start by briefly stating your relevant experience, then share one specific example where you handled this situation. What did you do, and what was the outcome?"
            }
            score < 35 -> {
                "Good start! Now let's make it stronger. Add one concrete example with a clear situation, your specific action, and the result. This turns a general answer into a memorable one."
            }
            score < 55 -> {
                "You're on the right track. I noticed your answer could benefit from more specific details. Can you share the exact result or outcome? Numbers, metrics, or tangible impact make your answer stick."
            }
            score < 75 -> {
                "Nice work! Your answer has good structure. To take it to the next level, try tightening the beginning - jump straight into your example without too much setup. Keep the result memorable."
            }
            else -> {
                "Excellent answer! You've got a clear structure with specific details. For even more impact, consider ending with how this experience prepares you for the role you're applying for."
            }
        }
    }
}

enum class SessionErrorReason { NETWORK_UNAVAILABLE, SERVER_TIMEOUT, AUTH_FAILED, SERVER_ERROR, UNKNOWN }

class SessionCreateException(val reason: SessionErrorReason, message: String? = null) : Exception(message)

data class EntitlementStatus(
    val isPremium: Boolean,
    val betaUnlockAllFeatures: Boolean = false,
    val adminAccess: Boolean = false,
)

data class BackendScoreResult(
    val answer: AnswerResult? = null,
    val failureCode: Int? = null,
    val failureMessage: String? = null,
)

data class TopicLesson(
    val topic: String,
    val lesson: String,
)

data class UserProgressSnapshot(
    val avgScore: Int,
    val sessions: Int,
    val practiceHours: Float,
    val growth: Int,
    val improvementFromFirst: Int,
    val coachingTip: String,
    val readinessLabel: String,
    val readinessScore: Int,
    val strongestSkill: String,
    val strongestScore: Int,
    val weakestSkill: String,
    val weakestScore: Int,
    val coachingPlan: List<String>,
    val radarData: List<Pair<String, Int>>,
)

data class AuthSession(
    val accessToken: String,
    val userId: String,
    val email: String,
    val fullName: String = "",
    val focus: String = "",
    val refreshToken: String = "",
)

data class BackendSession(
    val sessionId: String,
    val questions: List<InterviewQuestion> = emptyList(),
)

private fun JSONArray?.toInterviewQuestions(role: String, panelIds: List<String> = PrezzenceDefaults.panelIdsForStyle("Balanced")): List<InterviewQuestion> {
    if (this == null) return emptyList()
    val fallbackPanel = panelIds.ifEmpty { PrezzenceDefaults.panelIdsForStyle("Balanced") }
    return (0 until length()).mapNotNull { index ->
        val item = optJSONObject(index) ?: return@mapNotNull null
        val text = item.optString("text").trim()
        if (text.isBlank()) return@mapNotNull null
        val interviewerName = item.optString(
            "interviewerId",
            item.optString(
                "persona_id",
                item.optString("interviewer_name", item.optString("interviewer", "")),
            ),
        ).lowercase()
        val interviewerId = when {
            "maya" in interviewerName -> "maya"
            "jonas" in interviewerName || "oliver" in interviewerName -> "jonas"
            "amina" in interviewerName || "sophia" in interviewerName -> "amina"
            else -> fallbackPanel[index % fallbackPanel.size]
        }
        InterviewQuestion(
            id = item.optInt("id", item.optInt("number", index + 1)).coerceAtLeast(1),
            text = text,
            role = role.ifBlank { "Interview" },
            interviewerId = interviewerId,
            type = item.optString("type", if (index == 0) "introduction" else "behavioral"),
            learnMoreTopic = item.optString("learn_more_topic", item.optString("learnMoreTopic", "")),
            learnMoreUrl = item.optString("learn_more_url", item.optString("learnMoreUrl", "")),
        )
    }
}

private fun JSONObject.toResumeProfile(): ResumeProfile {
    fun stringList(key: String): List<String> {
        val array = optJSONArray(key) ?: JSONArray()
        return (0 until array.length()).mapNotNull { index -> array.optString(index).takeIf { it.isNotBlank() } }
    }
    val summary = optString("summary").ifBlank { optString("headline").ifBlank { optString("raw_text", "") } }
    return ResumeProfile(
        fileName = optString("file_name", optString("source_file", "Resume profile")),
        summary = summary,
        skills = stringList("skills"),
        experience = stringList("experience"),
    )
}

private fun JSONObject.toNotificationItem(): NotificationItem {
    return NotificationItem(
        id = optString("id", optString("notification_id")),
        title = optString("title", "Notification"),
        message = optString("message", optString("body", "")),
        createdAt = optString("created_at", optString("date", "")),
        isRead = optBoolean("is_read", optBoolean("read", false)),
    )
}

private fun JSONObject.toNotificationPreferences(): NotificationPreferences {
    return NotificationPreferences(
        pushNotificationsEnabled = optBoolean("push_notifications_enabled", true),
        emailSummariesEnabled = optBoolean("email_summaries_enabled", false),
        practiceRemindersEnabled = optBoolean("practice_reminders_enabled", true),
        achievementAlertsEnabled = optBoolean("achievement_alerts_enabled", true),
        productUpdatesEnabled = optBoolean("product_updates_enabled", true),
    )
}

