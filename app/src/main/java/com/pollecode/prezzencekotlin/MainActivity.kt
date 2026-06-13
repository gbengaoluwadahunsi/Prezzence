package com.pollecode.prezzencekotlin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pollecode.prezzencekotlin.billing.BillingUiState
import com.pollecode.prezzencekotlin.billing.PrezzenceBillingManager
import com.pollecode.prezzencekotlin.data.AnswerResult
import com.pollecode.prezzencekotlin.data.AppState
import com.pollecode.prezzencekotlin.data.InterviewMode
import com.pollecode.prezzencekotlin.data.Interviewer
import com.pollecode.prezzencekotlin.data.NotificationItem
import com.pollecode.prezzencekotlin.data.PrezzenceBackendClient
import com.pollecode.prezzencekotlin.data.ResumeProfile
import com.pollecode.prezzencekotlin.data.PrezzenceDefaults
import com.pollecode.prezzencekotlin.data.SessionCreateException
import com.pollecode.prezzencekotlin.data.SessionErrorReason
import com.pollecode.prezzencekotlin.data.SessionSummary
import com.pollecode.prezzencekotlin.nativebridge.NativeDuixAvatarView
import com.pollecode.prezzencekotlin.nativebridge.NativePresenceCameraView
import com.pollecode.prezzencekotlin.nativebridge.NativeSpeechTranscriber
import com.pollecode.prezzencekotlin.qa.DeviceQaResult
import com.pollecode.prezzencekotlin.qa.DeviceQaRunner
import com.pollecode.prezzencekotlin.ui.PrezzenceEnteringRoomScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceHomeScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceInterviewRoomScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceLandingScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceMicPermissionScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceOnboardingRoleScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceOnboardingTypeScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceSignInScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceForgotPasswordScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceVerifyScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceLanguageScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceResetPasswordScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceSignUpScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceProgressScreen
import com.pollecode.prezzencekotlin.ui.PrezzencePracticeScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceProfileScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceBottomTabBar
import com.pollecode.prezzencekotlin.ui.PrezzenceTab
import com.pollecode.prezzencekotlin.ui.SessionHistoryItem
import com.pollecode.prezzencekotlin.ui.PracticeSessionItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    private lateinit var root: FrameLayout
    private lateinit var appState: AppState

    private val backend = PrezzenceBackendClient()
    private val bg = Color.rgb(10, 10, 15)
    private val surface = Color.rgb(18, 18, 26)
    private val panel = Color.rgb(28, 28, 46)
    private val inputBg = Color.rgb(28, 28, 46)
    private val border = Color.rgb(42, 42, 62)
    private val borderLight = Color.rgb(58, 58, 94)
    private val muted = Color.rgb(138, 138, 154)
    private val textMuted = Color.rgb(85, 85, 112)
    private val accent = Color.rgb(108, 99, 255)
    private val green = Color.rgb(0, 214, 143)
    private val warning = Color.rgb(255, 179, 71)
    private val danger = Color.rgb(255, 71, 87)
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var billingManager: PrezzenceBillingManager

    private var activeAvatar: NativeDuixAvatarView? = null
    private var activeCamera: NativePresenceCameraView? = null
    private var activeTranscriber: NativeSpeechTranscriber? = null
    private var speechGenerationToken = 0
    private var cachedQuestionSpeechIndex = -1
    private var cachedQuestionSpeechSource: String = ""
    private val questionSpeechCache = mutableMapOf<Int, String>()
    private var openingIntroductionSpoken = false
    private var activeTranscript: String = ""
    private var speechError: String = ""
    private var currentAnswerResult: AnswerResult? = null
    private val sessionAnswers = mutableListOf<AnswerResult>()
    private var startAnswerAfterPermission = false
    private var remoteHistory: List<SessionSummary> = emptyList()
    private var oauthCodeVerifier: String = ""
    private var passwordResetAccessToken: String = ""
    private var onboardingTrack: String = "job"
    private var onboardingIndustry: String = "Customer Service"
    private var onboardingSeniority: String = "Mid"
    private var onboardingDifficulty: String = "Realistic"
    private var onboardingCompanyName: String = ""
    private var onboardingCompanyWebsite: String = ""
    private var onboardingCompanyContext: String = ""
    private var onboardingInterviewerStyle: String = "Balanced"
    private var onboardingPreviewGender: String = "Female"
    private var onboardingIncludeTechnical: Boolean = false
    private var onboardingEnableWebResearch: Boolean = false
    private var onboardingConsentAccepted: Boolean = false
    private var onboardingMicLoading: Boolean = false
    private var onboardingMicError: String? = null
    private var continueToRoomAfterMicPermission = false
    private var suppressNativeAvatarForEntry = false
    private var appToastView: View? = null
    private var activeTab: PrezzenceTab = PrezzenceTab.HOME
    private var unreadNotifications: Int = 0
    private var practiceRemoteSessions: List<PracticeSessionItem> = emptyList()
    private var coachingMessage: String = ""

    private val resumeDocumentPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) uploadResumeDocument(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = FrameLayout(this)
        root.fitsSystemWindows = false
        appState = AppState(this)
        onboardingInterviewerStyle = appState.interviewerStyle
        onboardingPreviewGender = appState.previewGender
        billingManager = PrezzenceBillingManager(this) { entitled, status ->
            appState.subscriptionEntitled = entitled
            appState.subscriptionStatus = status
            appState.subscriptionProductId = BuildConfig.PREZZENCE_SUBSCRIPTION_PRODUCT_ID
        }
        setContentView(root)
        if (!handleAuthCallback(intent?.data)) showSplash()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthCallback(intent.data)
    }

    override fun onDestroy() {
        releaseNativeSurfaces()
        if (::billingManager.isInitialized) billingManager.endConnection()
        super.onDestroy()
    }

    private fun saveAuthSession(session: com.pollecode.prezzencekotlin.data.AuthSession) {
        appState.authToken = session.accessToken
        appState.authRefreshToken = session.refreshToken
        appState.userId = session.userId
        if (session.email.isNotBlank()) appState.userEmail = session.email
        if (session.fullName.isNotBlank()) appState.userFullName = session.fullName
        if (session.focus.isNotBlank()) appState.userFocus = session.focus
        appState.onboardingComplete = true
    }

    private fun handleAuthCallback(uri: Uri?): Boolean {
        if (uri == null) return false
        if (uri.scheme == "https" && uri.host == "prezzence-backend.onrender.com" && uri.path == "/auth/verified") {
            showVerify()
            return true
        }
        if (uri.scheme != "prezzence") return false
        val isAuthCallback = uri.host == "auth" && (uri.path.orEmpty().contains("callback") || uri.path.orEmpty().contains("reset-password"))
        val isVerify = uri.host == "verify" || uri.toString().contains("verify")
        if (!isAuthCallback && !isVerify) return false

        val params = authParams(uri)
        val error = params["error_description"] ?: params["error"] ?: params["error_code"]
        if (!error.isNullOrBlank()) {
            showSignIn(error = error)
            return true
        }
        val authCode = params["code"].orEmpty()
        val accessToken = params["access_token"].orEmpty()
        val isPasswordRecovery = uri.path.orEmpty().contains("reset-password") || params["type"] == "recovery"
        if (authCode.isBlank() && accessToken.isBlank()) {
            if (isVerify) showVerify() else showSignIn()
            return true
        }
        if (isPasswordRecovery && accessToken.isNotBlank()) {
            passwordResetAccessToken = accessToken
            showResetPassword(accessToken)
            return true
        }
        showSignIn(loading = true)
        scope.launch {
            val session = if (authCode.isNotBlank()) {
                backend.exchangePkceCode(authCode, oauthCodeVerifier)
            } else {
                backend.sessionFromAccessToken(accessToken)
            }
            oauthCodeVerifier = ""
            if (session == null) {
                showSignIn(error = "Google sign-in finished, but the session could not be loaded.")
                return@launch
            }
            saveAuthSession(session)
            if (isVerify) showVerify() else showHome()
        }
        return true
    }

    private fun authParams(uri: Uri): Map<String, String> {
        fun parse(raw: String?): Map<String, String> {
            if (raw.isNullOrBlank()) return emptyMap()
            return raw.split('&').mapNotNull { pair ->
                val parts = pair.split('=', limit = 2)
                if (parts.isEmpty() || parts[0].isBlank()) null else {
                    val key = URLDecoder.decode(parts[0], "UTF-8")
                    val value = URLDecoder.decode(parts.getOrElse(1) { "" }, "UTF-8")
                    key to value
                }
            }.toMap()
        }
        return parse(uri.encodedQuery) + parse(uri.encodedFragment)
    }

    private fun startGoogleAuth() {
        runCatching {
            oauthCodeVerifier = backend.newPkceVerifier()
            val url = backend.googleOAuthUrl("prezzence://auth/callback", oauthCodeVerifier)
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            showAppToast("Could not open Google sign-in.", ToastKind.ERROR)
        }
    }

    private fun setScreen(view: View) {
        releaseNativeSurfaces()
        root.removeAllViews()
        root.setBackgroundColor(bg)
        view.alpha = 0f
        root.addView(
            view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        view.animate().alpha(1f).setDuration(300).start()
    }

    private fun showSplash() {
        if (BuildConfig.DEBUG && intent.getBooleanExtra("forceLanding", false)) {
            showLanding()
            return
        }
        if (BuildConfig.DEBUG && intent.getBooleanExtra("forceSignIn", false)) {
            showSignIn()
            return
        }
        if (BuildConfig.DEBUG && intent.getBooleanExtra("forceSignUp", false)) {
            showSignUp()
            return
        }
        if (BuildConfig.DEBUG && intent.getBooleanExtra("forceResetPassword", false)) {
            showResetPassword("debug-token")
            return
        }
        if (BuildConfig.DEBUG && intent.getBooleanExtra("forceLanguage", false)) {
            showAuthLanguage()
            return
        }
        if (BuildConfig.DEBUG && intent.getBooleanExtra("forceVerify", false)) {
            showVerify()
            return
        }
        if (BuildConfig.DEBUG && intent.getBooleanExtra("forceOnboardingType", false)) {
            showOnboardingType()
            return
        }
        if (BuildConfig.DEBUG && intent.getBooleanExtra("forceOnboardingRole", false)) {
            showOnboardingRole()
            return
        }
        if (BuildConfig.DEBUG && intent.getBooleanExtra("forceMicPermission", false)) {
            showMicPermission()
            return
        }
        if (BuildConfig.DEBUG && intent.getBooleanExtra("forceForgotPassword", false)) {
            showForgotPassword()
            return
        }
        if (appState.onboardingComplete) showHome() else showLanding()
    }

    private fun showLanding() {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceLandingScreen(
                    onStart = { showSignUp() },
                    onSignIn = { showSignIn() },
                )
            }
        })
    }
    private fun landingAtmosphere() = object : View(this) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat().coerceAtLeast(1f)
            val h = height.toFloat().coerceAtLeast(1f)
            paint.shader = LinearGradient(0f, 0f, 0f, h, intArrayOf(
                Color.rgb(6, 7, 17),
                Color.rgb(9, 13, 30),
                Color.rgb(8, 7, 16),
                Color.rgb(3, 3, 9),
            ), floatArrayOf(0f, 0.36f, 0.72f, 1f), Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w, h, paint)

            paint.shader = LinearGradient(0f, h * 0.22f, w, h * 0.78f, intArrayOf(
                Color.argb(0, 55, 65, 255),
                Color.argb(78, 48, 76, 160),
                Color.argb(88, 12, 130, 170),
                Color.argb(0, 17, 20, 50),
            ), null, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w, h, paint)

            paint.shader = RadialGradient(w * 0.42f, h * 0.46f, w * 0.62f, intArrayOf(
                Color.argb(95, 45, 84, 140),
                Color.argb(42, 31, 40, 82),
                Color.TRANSPARENT,
            ), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w, h, paint)

            paint.shader = LinearGradient(0f, h * 0.74f, 0f, h, Color.argb(0, 0, 0, 0), Color.argb(235, 4, 4, 10), Shader.TileMode.CLAMP)
            canvas.drawRect(0f, h * 0.70f, w, h, paint)
            paint.shader = null
            paint.color = Color.argb(46, 0, 0, 0)
            canvas.drawRect(0f, 0f, w, h, paint)
        }
    }

    private fun landingLogoRow() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(ImageView(this@MainActivity).apply {
            setImageResource(R.drawable.prezzence_icon)
            layoutParams = LinearLayout.LayoutParams(dp(42), dp(42)).apply { setMargins(0, 0, dp(16), 0) }
        })
        addView(TextView(this@MainActivity).apply {
            text = "Prezzen..."
            textSize = 27f
            setTextColor(Color.WHITE)
            includeFontPadding = false
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        })
    }

    private fun landingKicker(text: String) = TextView(this).apply {
        this.text = text
        textSize = 16f
        letterSpacing = 0.24f
        setTextColor(accent)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        includeFontPadding = false
        setPadding(0, 0, 0, dp(34))
    }

    private fun landingTitle(text: String, gradient: Boolean) = TextView(this).apply {
        this.text = text
        textSize = 70f
        setLineSpacing((-4).toFloat(), 0.92f)
        includeFontPadding = false
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        setTextColor(Color.rgb(244, 244, 255))
        setPadding(0, 0, 0, if (gradient) dp(40) else dp(22))
        if (gradient) {
            post {
                paint.shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), intArrayOf(
                    Color.rgb(108, 99, 255),
                    Color.rgb(67, 157, 255),
                    Color.rgb(30, 210, 181),
                ), null, Shader.TileMode.CLAMP)
                invalidate()
            }
        }
    }

    private fun landingSubtitle(text: String) = TextView(this).apply {
        this.text = text
        textSize = 22f
        setTextColor(Color.rgb(170, 166, 184))
        setLineSpacing(dp(7).toFloat(), 1.04f)
        includeFontPadding = true
        setPadding(0, 0, 0, dp(18))
    }

    private fun landingStartButton(action: () -> Unit) = TextView(this).apply {
        text = "Start                                      >"
        gravity = Gravity.CENTER
        textSize = 22f
        includeFontPadding = false
        setTextColor(Color.WHITE)
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        background = rounded(accent, radius = 40, strokeColor = Color.TRANSPARENT)
        minHeight = dp(92)
        elevation = dp(12).toFloat()
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(92)).apply {
            setMargins(0, 0, 0, dp(22))
        }
        setOnClickListener { action() }
    }

    private fun landingAccountLink(action: () -> Unit) = TextView(this).apply {
        text = "I already have an account ->"
        gravity = Gravity.CENTER
        textSize = 18f
        includeFontPadding = false
        setTextColor(Color.rgb(158, 154, 170))
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setPadding(0, dp(2), 0, dp(24))
        setOnClickListener { action() }
    }

    private fun showSignIn(error: String? = null, loading: Boolean = false, successMessage: String? = null) {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceSignInScreen(
                    loading = loading,
                    error = error,
                    successMessage = successMessage,
                    onClose = { showLanding() },
                    onForgotPassword = { showForgotPassword() },
                    onSignIn = { email, password ->
                        showSignIn(loading = true)
                        scope.launch {
                            val session = backend.signInWithPassword(email, password)
                            if (session == null) {
                                showSignIn(error = "Sign in failed. Check your details.")
                                return@launch
                            }
                            saveAuthSession(session)
                            if (appState.userEmail.isBlank()) appState.userEmail = email.trim()
                            showHome()
                        }
                    },
                    onGoogleSignIn = { startGoogleAuth() },
                    onSignUp = { showSignUp() },
                )
            }
        })
    }

    private fun showAuthCallback(token: String? = null, error: String? = null) {
        if (error != null) {
            showSignIn(error = error)
            return
        }
        setScreen(LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            background = rounded(bg)
            addView(LinearLayout(this@MainActivity).apply {
                gravity = Gravity.CENTER
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(dp(210), dp(136))
                background = rounded(panel, radius = 24, strokeColor = border)
                setPadding(dp(24), dp(24), dp(24), dp(24))
                addView(ProgressBar(this@MainActivity).apply {
                    isIndeterminate = true
                    layoutParams = LinearLayout.LayoutParams(dp(32), dp(32))
                })
                addView(spacer(16))
                addView(TextView(this@MainActivity).apply {
                    text = "Signing you in"
                    textSize = 16f
                    setTextColor(Color.WHITE)
                    typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    gravity = Gravity.CENTER
                })
            })
        })
        if (!token.isNullOrBlank()) {
            scope.launch {
                val session = backend.sessionFromAccessToken(token!!)
                if (session != null) {
                    saveAuthSession(session)
                    showHome()
                } else {
                    showSignIn(error = "Unable to complete sign in.")
                }
            }
        }
    }

    private fun showSignUp(error: String? = null, loading: Boolean = false, successEmail: String? = null) {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceSignUpScreen(
                    loading = loading,
                    error = error,
                    successEmail = successEmail,
                    onBack = { showLanding() },
                    onSignIn = { showSignIn() },
                    onSubmit = { name, email, password ->
                        showSignUp(loading = true)
                        scope.launch {
                            val created = backend.signUpWithPassword(email, password, name)
                            if (created) {
                                appState.onboardingComplete = false
                                showSignUp(successEmail = email)
                            } else {
                                showSignUp(error = "Could not create account. Check your email or try again.")
                            }
                        }
                    },
                    onGoogleSignUp = { startGoogleAuth() },
                    onTerms = { showLegal() },
                    onPrivacy = { showLegal() },
                )
            }
        })
    }

    private fun showForgotPassword(error: String? = null, loading: Boolean = false, sent: Boolean = false) {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceForgotPasswordScreen(
                    loading = loading,
                    error = error,
                    sent = sent,
                    onBack = { showSignIn() },
                    onSubmit = { rawEmail ->
                        showForgotPassword(loading = true)
                        scope.launch {
                            val sentReset = backend.requestPasswordReset(rawEmail, "prezzence://auth/reset-password")
                            if (sentReset) {
                                showForgotPassword(sent = true)
                            } else {
                                showForgotPassword(error = "Could not send reset email. Try again.")
                            }
                        }
                    },
                    onSignIn = { showSignIn() },
                )
            }
        })
    }
    private fun showResetPassword(accessToken: String = passwordResetAccessToken, error: String? = null, loading: Boolean = false) {
        if (accessToken.isNotBlank()) passwordResetAccessToken = accessToken
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceResetPasswordScreen(
                    loading = loading,
                    error = error,
                    onBack = { showSignIn() },
                    onSubmit = { password ->
                        if (passwordResetAccessToken == "debug-token") {
                            showResetPassword(error = "Open the latest reset email to reset your real password.")
                            return@PrezzenceResetPasswordScreen
                        }
                        showResetPassword(loading = true)
                        scope.launch {
                            val updated = backend.updatePassword(passwordResetAccessToken, password)
                            if (updated) {
                                passwordResetAccessToken = ""
                                showSignIn(successMessage = "Password updated. Sign in to continue.")
                            } else {
                                showResetPassword(error = "Unable to reset password. Open the latest reset email and try again.")
                            }
                        }
                    },
                    onSignIn = { showSignIn() },
                )
            }
        })
    }

    private fun showAuthLanguage() {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceLanguageScreen(
                    selectedLanguage = appState.language,
                    onBack = { showSignUp() },
                    onLanguageSelected = { language -> appState.language = language },
                    onContinue = { showSignUp() },
                )
            }
        })
    }

    private fun showVerify() {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceVerifyScreen(
                    onEnter = {
                        appState.onboardingComplete = false
                        showOnboardingType()
                    },
                )
            }
        })
    }

    private fun showOnboardingType() {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceOnboardingTypeScreen(
                    selectedTrack = onboardingTrack,
                    onBack = { showLanding() },
                    onSelectTrack = { track ->
                        onboardingTrack = track
                        appState.interviewMode = if (track == "leadership") InterviewMode.PANEL else InterviewMode.SINGLE
                        applyTrackDefaults(track)
                        showOnboardingType()
                    },
                    onContinue = { showOnboardingRole() },
                )
            }
        })
    }

    private fun applyTrackDefaults(track: String) {
        // Always apply role defaults when track changes (not just for stock roles)
        appState.selectedRole = when (track.lowercase()) {
            "promotion" -> "Senior Manager"
            "pitch" -> "Product or startup pitch"
            "leadership" -> "Team leadership scenario"
            "behavioral" -> "Behavioral interview stories"
            "technical" -> "Software Engineer"
            else -> "Software Engineer"  // Default role for job track
        }
        
        // Always apply industry defaults when track changes
        onboardingIndustry = when (track.lowercase()) {
            "promotion" -> "Current function or team"
            "pitch" -> "Investors or customers"
            "leadership" -> "Operations or people leadership"
            "behavioral" -> "Leadership, conflict, ownership"
            "technical" -> "Kotlin, cloud, data, ML"
            else -> "Tech"  // Default industry for job track
        }
        
        // Set technical flag for technical track
        onboardingIncludeTechnical = track.equals("technical", ignoreCase = true)
    }

    private fun showOnboardingRole() {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceOnboardingRoleScreen(
                    track = onboardingTrack,
                    role = appState.selectedRole,
                    industry = onboardingIndustry,
                    seniority = onboardingSeniority,
                    interviewMode = if (appState.interviewMode == InterviewMode.PANEL) "Panel" else "Single",
                    difficulty = onboardingDifficulty,
                    companyName = onboardingCompanyName,
                    companyWebsite = onboardingCompanyWebsite,
                    companyContext = onboardingCompanyContext,
                    interviewerStyle = onboardingInterviewerStyle,
                    previewGender = onboardingPreviewGender,
                    includeTechnical = onboardingIncludeTechnical || onboardingTrack.equals("technical", ignoreCase = true),
                    enableWebResearch = onboardingEnableWebResearch,
                    onBack = { showOnboardingType() },
                    onRoleChange = { role -> appState.selectedRole = role },
                    onIndustryChange = { industry -> onboardingIndustry = industry },
                    onSeniorityChange = { seniority -> onboardingSeniority = seniority },
                    onInterviewModeChange = { mode ->
                        appState.interviewMode = if (mode.equals("Panel", ignoreCase = true)) InterviewMode.PANEL else InterviewMode.SINGLE
                    },
                    onDifficultyChange = { difficulty -> onboardingDifficulty = difficulty },
                    onCompanyNameChange = { companyName -> onboardingCompanyName = companyName },
                    onCompanyWebsiteChange = { companyWebsite -> onboardingCompanyWebsite = companyWebsite },
                    onCompanyContextChange = { companyContext -> onboardingCompanyContext = companyContext },
                    onInterviewerStyleChange = { style ->
                        onboardingInterviewerStyle = style
                        appState.interviewerStyle = style
                    },
                    onPreviewGenderChange = { gender ->
                        onboardingPreviewGender = gender
                        appState.previewGender = gender
                    },
                    onIncludeTechnicalChange = { enabled -> onboardingIncludeTechnical = enabled },
                    onEnableWebResearchChange = { enabled -> onboardingEnableWebResearch = enabled },
                    onContinue = { showMicPermission() },
                )
            }
        })
    }

    private fun showMicPermission(error: String? = onboardingMicError, loading: Boolean = onboardingMicLoading) {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceMicPermissionScreen(
                    consentAccepted = onboardingConsentAccepted,
                    loading = loading,
                    error = error,
                    onBack = { showOnboardingRole() },
                    onConsentChange = { accepted ->
                        onboardingConsentAccepted = accepted
                        onboardingMicError = null
                        showMicPermission(error = null, loading = false)
                    },
                    onGrantAccess = { grantMicAndBegin() },
                )
            }
        })
    }

    private fun grantMicAndBegin() {
        if (!onboardingConsentAccepted) {
            onboardingMicError = "Accept audio and AI consent before starting."
            showMicPermission()
            return
        }
        val micGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!micGranted) {
            continueToRoomAfterMicPermission = true
            onboardingMicLoading = true
            showMicPermission(error = null, loading = true)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            return
        }
        appState.onboardingComplete = true
        appState.activeSessionId = ""
        showEnteringRoom()
    }

    private fun showHome(tab: PrezzenceTab = PrezzenceTab.HOME) {
        activeTab = tab
        val firstName = appState.userFullName.ifBlank { appState.userEmail.substringBefore('@') }.takeIf { it.isNotBlank() }
        val history = if (remoteHistory.isNotEmpty()) remoteHistory else appState.sessionHistory()
        val questions = appState.questions()
        val hasIncomplete = appState.activeSessionId.isNotBlank() && questions.isNotEmpty() &&
            appState.currentQuestionIndex < questions.size

        // Preload DUIX models for all personas on first app launch
        // This downloads models (~50MB) to device storage when user first signs in
        if (appState.duixModelsPreloaded) {
            // Models already preloaded, skip
        } else {
            appState.duixModelsPreloaded = true
            scope.launch {
                try {
                    // Preload all 3 personas: Sofia, Lily, Oliver
                    val preloadNames = listOf("Sofia", "Lily", "Oliver")
                    com.pollecode.prezzencekotlin.nativebridge.NativeDuixAvatarView.preloadModelFiles(this@MainActivity, preloadNames)
                } catch (e: Exception) {
                    // Model preload failed - they'll be downloaded on-demand when needed
                }
            }
        }
        
        setScreen(ComposeView(this).apply {
            setContent {
                androidx.compose.material3.MaterialTheme {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (tab) {
                            PrezzenceTab.HOME -> PrezzenceHomeScreen(
                                completedSessions = appState.completedSessions,
                                readinessScore = appState.readinessScore,
                                signedInAs = appState.userEmail.ifBlank { null },
                                firstName = firstName,
                                hasIncompleteSession = hasIncomplete,
                                currentQuestionIndex = appState.currentQuestionIndex,
                                totalQuestions = questions.size.coerceAtLeast(1),
                                unreadNotifications = unreadNotifications,
                                onStart = { showOnboardingType() },
                                onContinueSession = { showInterview(false) },
                                onNewSession = {
                                    appState.resetActiveSession()
                                    showOnboardingType()
                                },
                                onQuestions = { showQuestions() },
                                onProgress = { showHome(PrezzenceTab.PROGRESS) },
                                onSettings = { showSettings() },
                                onQa = { showDeviceQa() },
                                onNotifications = { showNotifications() },
                            )
                            PrezzenceTab.PRACTICE -> {
                                val practiceSessions = practiceRemoteSessions.ifEmpty {
                                    history.map { s ->
                                        PracticeSessionItem(
                                            id = s.id, title = s.role, type = "interview",
                                            date = s.date, score = s.score, status = "completed"
                                        )
                                    }
                                }
                                PrezzencePracticeScreen(
                                    recentSessions = practiceSessions,
                                    onSessionTap = { sessionId -> showSessionReport(sessionId) },
                                    onDeleteSession = { sessionId ->
                                        scope.launch {
                                            val deleted = backend.deleteSession(appState.authToken.ifBlank { null }, sessionId)
                                            if (!deleted) appState.deleteSession(sessionId)
                                            remoteHistory = remoteHistory.filterNot { it.id == sessionId }
                                            practiceRemoteSessions = practiceRemoteSessions.filterNot { it.id == sessionId }
                                            showHome(PrezzenceTab.PRACTICE)
                                        }
                                    },
                                    onSelectMode = { modeId ->
                                        appState.interviewMode = if (modeId == "full") InterviewMode.PANEL else InterviewMode.SINGLE
                                        showOnboardingRole()
                                    },
                                )
                            }
                            PrezzenceTab.PROGRESS -> {
                                val historyItems = (if (remoteHistory.isNotEmpty()) remoteHistory else appState.sessionHistory())
                                    .map { s ->
                                        SessionHistoryItem(id = s.id, role = s.role, score = s.score, date = s.date, answered = s.answered, total = s.total)
                                    }
                                PrezzenceProgressScreen(
                                    avgScore = if (historyItems.isEmpty()) 0 else historyItems.map { it.score }.average().toInt().coerceIn(0, 100),
                                    sessions = historyItems.size,
                                    practiceHours = 0f,
                                    readinessLabel = when {
                                        appState.readinessScore >= 75 -> "Interview Ready"
                                        appState.readinessScore >= 45 -> "Building Confidence"
                                        appState.readinessScore > 0 -> "Building Momentum"
                                        else -> "No Baseline"
                                    },
                                    recentSessions = historyItems,
                                    coachingTip = "",
                                    onSettings = { showSettings() },
                                    onSessionTap = { /* session detail */ },
                                    onNewSession = { showOnboardingType() },
                                    onDeleteSession = { sessionId ->
                                        scope.launch {
                                            val deleted = backend.deleteSession(appState.authToken.ifBlank { null }, sessionId)
                                            if (!deleted) appState.deleteSession(sessionId)
                                            remoteHistory = remoteHistory.filterNot { it.id == sessionId }
                                            showHome(PrezzenceTab.PROGRESS)
                                        }
                                    },
                                )
                            }
                            PrezzenceTab.PROFILE -> {
                                val name = appState.userEmail.substringBefore('@').ifBlank { "Candidate" }
                                val initials = name.split(' ').filter { it.isNotBlank() }.take(2)
                                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                    .joinToString("").ifBlank { "C" }
                                PrezzenceProfileScreen(
                                    fullName = name,
                                    email = appState.userEmail.ifBlank { "No email connected" },
                                    initials = initials,
                                    avgScore = appState.readinessScore,
                                    sessions = appState.completedSessions,
                                    coachingTip = "",
                                    bestSkillLabel = null,
                                    bestSkillValue = null,
                                    cameraCoachEnabled = appState.cameraCoachEnabled,
                                    resumeFileName = null,
                                    language = appState.language,
                                    goalValue = appState.weeklyGoal,
                                    onToggleCameraCoach = {
                                        appState.cameraCoachEnabled = !appState.cameraCoachEnabled
                                        showHome(PrezzenceTab.PROFILE)
                                    },
                                    onUploadResume = { pickResumeDocument() },
                                    onDeleteResume = {
                                        scope.launch {
                                            backend.deleteResumeProfile(appState.authToken.ifBlank { null })
                                            showHome(PrezzenceTab.PROFILE)
                                        }
                                    },
                                    onAccount = { showAccount() },
                                    onLanguage = { showLanguage() },
                                    onPrivacy = { showLegal() },
                                    onHelp = { showAppToast("Help coming soon.", ToastKind.INFO) },
                                    onSignOut = {
                                        appState.signOut()
                                        showLanding()
                                    },
                                    onSettings = { showSettings() },
                                    onNotifications = { showAppToast("Notifications coming soon.", ToastKind.INFO) },
                                    onGoalChange = { newGoal ->
                                        appState.weeklyGoal = newGoal
                                        showHome(PrezzenceTab.PROFILE)
                                    },
                                )
                            }
                        }
                        // Bottom tab bar always visible
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                        ) {
                            PrezzenceBottomTabBar(
                                activeTab = tab,
                                onTabSelected = { newTab -> showHome(newTab) },
                            )
                        }
                    }
                }
            }
        })

        // Lazy-fetch remote history for progress tab
        if (appState.authToken.isNotBlank() && remoteHistory.isEmpty() && (tab == PrezzenceTab.PROGRESS || tab == PrezzenceTab.PRACTICE)) {
            scope.launch {
                val fetched = backend.listSessions(appState.authToken)
                if (fetched.isNotEmpty()) {
                    remoteHistory = fetched
                    showHome(tab)
                }
            }
        }
        // Fetch unread notification count
        if (appState.authToken.isNotBlank() && tab == PrezzenceTab.HOME) {
            scope.launch {
                val notifications = backend.getNotifications(appState.authToken)
                val unread = notifications.count { !it.isRead }
                if (unread != unreadNotifications) {
                    unreadNotifications = unread
                    showHome(PrezzenceTab.HOME)
                }
            }
        }
    }
    private fun homeHeader() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(logoRow(23).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        addView(TextView(this@MainActivity).apply {
            text = "QA"
            gravity = Gravity.CENTER
            textSize = 13f
            setTextColor(accent)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            background = rounded(Color.argb(36, 108, 99, 255), radius = 22, strokeColor = borderLight)
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
            setOnClickListener { showDeviceQa() }
        })
    }

    private fun homeReadinessCard() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = rounded(panel, radius = 18, strokeColor = border)
        layoutParams = blockParams().apply { setMargins(0, dp(18), 0, dp(12)) }
        val score = appState.readinessScore.coerceIn(0, 100)
        val labelText = when {
            score >= 75 -> "Interview Ready"
            score >= 45 -> "Building"
            score > 0 -> "Needs Practice"
            else -> "Baseline in progress"
        }
        val row = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(label("CURRENT READINESS"))
                addView(title(labelText, 24).apply { setPadding(0, 0, 0, dp(8)) })
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(this@MainActivity).apply {
                text = "$score%"
                textSize = 34f
                setTextColor(accent)
                includeFontPadding = false
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
            })
        }
        addView(row)
        addView(progressBar(score))
        addView(body(if (score == 0) "Complete one realistic session to capture your baseline." else "Keep practicing to improve your interview signal."))
    }

    private fun progressBar(score: Int) = FrameLayout(this).apply {
        background = rounded(surface, radius = 8, strokeColor = Color.TRANSPARENT)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(10)).apply {
            setMargins(0, dp(10), 0, dp(12))
        }
        post {
            addView(View(this@MainActivity).apply {
                background = rounded(accent, radius = 8, strokeColor = Color.TRANSPARENT)
                layoutParams = FrameLayout.LayoutParams((width * (score / 100f)).toInt().coerceAtLeast(if (score > 0) dp(8) else 0), dp(10))
            })
        }
    }

    private fun homeActionGrid() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        layoutParams = blockParams()
        addView(homeNavButton("Questions") { showQuestions() })
        addView(homeNavButton("Progress") { showProgress() })
        addView(homeNavButton("Settings") { showSettings() })
    }

    private fun homeNavButton(text: String, action: () -> Unit) = TextView(this).apply {
        this.text = text
        gravity = Gravity.CENTER
        textSize = 14f
        setTextColor(accent)
        includeFontPadding = false
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        background = rounded(Color.TRANSPARENT, radius = 16, strokeColor = accent)
        layoutParams = LinearLayout.LayoutParams(0, dp(64), 1f).apply { setMargins(dp(5), 0, dp(5), 0) }
        setOnClickListener { action() }
    }

    private fun statusPill(text: String) = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(Color.rgb(190, 187, 204))
        includeFontPadding = false
        setPadding(dp(12), dp(8), dp(12), dp(8))
        background = rounded(surface, radius = 20, strokeColor = border)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, dp(2), 0, dp(8))
        }
    }
    private fun showQuestions() {
        val column = baseColumn()
        column.addView(backButton { showHome() })
        column.addView(title("Questions", 36))
        column.addView(body("Pick a role, choose one question, and start practicing."))
        column.addView(roleScroller())
        appState.questions().forEachIndexed { index, question ->
            column.addView(card(question.role, question.text).apply {
                setOnClickListener {
                    appState.currentQuestionIndex = index
                    showRoomSetup()
                }
            })
        }
        setScreen(scroll(column))
    }

    private fun settingsCard(items: List<View>, padding: Int = 8) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(padding, padding, padding, padding)
        background = rounded(panel, radius = 32, strokeColor = border)
        layoutParams = blockParams()
        items.forEachIndexed { idx, view ->
            if (idx > 0) {
                addView(View(this@MainActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply { setMargins(dp(20), 0, dp(20), 0) }
                    setBackgroundColor(Color.argb(8, 255, 255, 255))
                })
            }
            addView(view)
        }
    }

    private fun showSettings() {
        val column = baseColumn()
        column.addView(backButton { showHome() })
        column.addView(title("Settings", 40))
        column.addView(body("Manage account access, language, subscription, privacy, and interview preferences."))
        column.addView(spacer(8))

        column.addView(label("INTERVIEW SETUP"))
        column.addView(settingsCard(listOf(
            settingsRow("Camera presence coach", if (appState.cameraCoachEnabled) "On \u00B7 Uses your camera during answers only" else "Off. Turn on for face, eyes, posture feedback", icon = SettingsIcon.Camera) {
                appState.cameraCoachEnabled = !appState.cameraCoachEnabled; showSettings()
            },
            settingsRow("Interviewer setup", "Choose one interviewer or a panel, plus the style", icon = SettingsIcon.Sound) { showQuestions() },
            settingsRow("App language", "Current: ${appState.language.uppercase()}", icon = SettingsIcon.Globe) { showLanguage() },
        )))
        column.addView(spacer(8))

        column.addView(label("ACCOUNT"))
        column.addView(settingsCard(listOf(
            settingsRow("Account details", "Name, email, password reset, and your practice profile", icon = SettingsIcon.User) { showAccount() },
            settingsRow("Subscription", if (appState.subscriptionEntitled) "Active" else "Free plan, upgrade", icon = SettingsIcon.Sub) { showPaywall() },
            settingsRow("Privacy and deletion", "Data, devices, and deletion controls", icon = SettingsIcon.Shield) { showPrivacySettings() },
            settingsRow("Notification preferences", "Push, reminders, and milestone alerts", icon = SettingsIcon.Bell) { showNotificationSettings() },
            settingsRow("Notification inbox", "View past notifications", icon = SettingsIcon.Notif) { showNotifications() },
            settingsRow("Feedback", "Send product feedback", icon = SettingsIcon.Feedback) { showFeedback() },
        )))
        column.addView(spacer(8))

        column.addView(label("TOOLS"))
        column.addView(settingsCard(listOf(
            settingsRow("Device QA", "Mic, Whisper, camera, and Duix checks", icon = SettingsIcon.QA) { showDeviceQa() },
            settingsRow("Accessibility", "Text size, contrast, and motion", icon = SettingsIcon.A11y) { showAccessibility() },
            settingsRow("App Settings", "Wi-Fi only downloads", icon = SettingsIcon.Settings) { showAppSettings() },
            settingsRow("Resume / CV profile", "Tune questions with your resume", icon = SettingsIcon.File) { showResumeProfile() },
            settingsRow("Help & FAQ", "Search support topics and tutorials", icon = SettingsIcon.Help) { showHelp() },
            settingsRow("Visual parity", "React Native route coverage", icon = SettingsIcon.Eye) { showVisualParity() },
        )))
        column.addView(spacer(8))

        column.addView(label("SIGN OUT"))
        if (appState.userEmail.isNotBlank()) {
            column.addView(settingsRow("Sign out", "Signed in as ${appState.userEmail}", icon = SettingsIcon.LogOut) {
                appState.signOut(); showLanding()
            })
        } else {
            column.addView(settingsRow("Sign in", "Not signed in", icon = SettingsIcon.User) { showSignIn() })
        }
        setScreen(scroll(column))
    }

    private fun showLanguage() {
        val column = baseColumn()
        column.addView(backButton { showSettings() })
        column.addView(title("Language", 34))
        column.addView(body("Choose the speech and session language used by the native interview flow."))
        listOf("en-US", "en-GB", "es-ES", "fr-FR", "de-DE", "pt-BR", "ja-JP", "ko-KR", "zh-CN").forEach { tag ->
            column.addView(settingsRow(tag, if (appState.language == tag) "Selected" else "Tap to select") {
                appState.language = tag
                showLanguage()
            })
        }
        setScreen(scroll(column))
    }

    private fun showAccount() {
        val column = baseColumn()
        column.addView(backButton { showSettings() })
        column.addView(title("Account details", 34))
        if (appState.userEmail.isNotBlank()) {
            column.addView(LinearLayout(this).apply {
                gravity = Gravity.CENTER
                orientation = LinearLayout.VERTICAL
                setPadding(dp(24), dp(24), dp(24), dp(24))
                layoutParams = blockParams()
                addView(LinearLayout(this@MainActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(120), dp(120))
                    gravity = Gravity.CENTER
                    background = rounded(Color.TRANSPARENT, radius = 60).apply { setStroke(dp(1), accent) }
                    setPadding(dp(4), dp(4), dp(4), dp(4))
                    addView(LinearLayout(this@MainActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(dp(110), dp(110))
                        gravity = Gravity.CENTER
                        background = rounded(panel, radius = 55, strokeColor = border)
                        addView(TextView(this@MainActivity).apply {
                            text = appState.userFullName.take(2).uppercase().ifBlank { "U" }
                            textSize = 32f
                            setTextColor(accent)
                            typeface = Typeface.create("sans-serif", Typeface.BOLD)
                            gravity = Gravity.CENTER
                        })
                    })
                })
                addView(spacer(20))
                addView(TextView(this@MainActivity).apply {
                    text = appState.userFullName.ifBlank { "Prezzence user" }
                    textSize = 24f
                    setTextColor(Color.WHITE)
                    typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    gravity = Gravity.CENTER
                })
                addView(TextView(this@MainActivity).apply {
                    text = appState.userEmail.uppercase()
                    textSize = 14f
                    setTextColor(accent)
                    typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    gravity = Gravity.CENTER
                    setPadding(0, dp(4), 0, 0)
                })
            })
            column.addView(label("PROFILE DETAILS"))
            column.addView(settingsRow("Display name", appState.userFullName.ifBlank { "Set your name" }, icon = SettingsIcon.User) { showEditProfile() })
            column.addView(settingsRow("Email address", appState.userEmail, icon = SettingsIcon.User) { })
            column.addView(settingsRow("Interview focus", appState.userFocus.ifBlank { "Set your focus" }, icon = SettingsIcon.Globe) { showEditProfile() })
            column.addView(spacer(16))
            column.addView(label("ACCOUNT ACTIONS"))
            column.addView(dangerButton("Delete account") {
                showDeleteAccountConfirmation()
            })
            column.addView(spacer(12))
            column.addView(secondaryButton("Sign out") {
                appState.signOut()
                showLanding()
            })
        } else {
            column.addView(body("You are not signed in."))
            column.addView(primaryButton("Sign in") { showSignIn() })
        }
        setScreen(scroll(column))
    }

    private fun showDeleteAccountConfirmation() {
        val column = baseColumn()
        column.addView(backButton { showAccount() })
        column.addView(title("Delete Account", 48))
        column.addView(body("Type DELETE to permanently remove data."))
        column.addView(card("This deletes all sessions", "Scores, recordings, progress history, and saved interview settings will be removed."))
        column.addView(label("CONFIRM"))
        val confirmInput = input("Type DELETE")
        column.addView(confirmInput)
        column.addView(spacer(24))
        column.addView(primaryButton("Delete Account") {
            if (confirmInput.text.toString().trim() == "DELETE") {
                scope.launch {
                    val deleted = backend.deleteAccount(appState.authToken.ifBlank { null })
                    showAppToast(
                        if (deleted) "Account data deleted." else "Could not delete account.",
                        if (deleted) ToastKind.green else ToastKind.ERROR,
                    )
                    if (deleted) {
                        appState.signOut()
                        showLanding()
                    }
                }
            } else {
                showAppToast("Type DELETE to confirm.", ToastKind.WARNING)
            }
        })
        setScreen(scroll(column))
    }

    private fun showEditProfile() {
        val column = baseColumn()
        column.addView(backButton { showAccount() })
        column.addView(title("Edit profile", 34))
        
        // Avatar section with halo
        column.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(24), 0, dp(40))
            layoutParams = blockParams().apply { setMargins(0, dp(8), 0, 0) }
            addView(LinearLayout(this@MainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(120), dp(120))
                gravity = Gravity.CENTER
                background = rounded(Color.TRANSPARENT, radius = 60).apply { setStroke(dp(1), accent) }
                setPadding(dp(4), dp(4), dp(4), dp(4))
                addView(LinearLayout(this@MainActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(110), dp(110))
                    gravity = Gravity.CENTER
                    background = rounded(panel, radius = 55)
                    addView(TextView(this@MainActivity).apply {
                        textSize = 48f; setTextColor(accent)
                        val userIcon = object : android.graphics.drawable.Drawable() {
                            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = accent; style = Paint.Style.STROKE
                                strokeWidth = 3f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
                            }
                            override fun draw(canvas: Canvas) {
                                val w = bounds.width().toFloat(); val h = bounds.height().toFloat()
                                val cx = w / 2f; val cy = h * 0.32f
                                canvas.drawCircle(cx, cy, w * 0.18f, p)
                                val body = android.graphics.Path()
                                body.addArc(android.graphics.RectF(cx - w * 0.22f, cy + w * 0.08f, cx + w * 0.22f, h + w * 0.05f), 0f, -180f)
                                canvas.drawPath(body, p)
                            }
                            override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
                            override fun setAlpha(alpha: Int) {}
                            override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
                        }
                        userIcon.setBounds(0, 0, dp(48), dp(48))
                        setCompoundDrawables(userIcon, null, null, null)
                    })
                })
            })
            addView(TextView(this@MainActivity).apply {
                text = appState.userEmail.ifBlank { "Signed in account" }
                textSize = 12f; setTextColor(muted); typeface = Typeface.create("sans-serif", Typeface.BOLD)
                setPadding(0, dp(16), 0, 0)
            })
        })
        
        val nameInput = input("Name").apply { setText(appState.userFullName) }
        val emailInput = input("Email").apply { setText(appState.userEmail); isEnabled = false }
        val focusInput = input("Interview Focus").apply { setText(appState.userFocus) }
        
        column.addView(label("NAME"))
        column.addView(nameInput)
        column.addView(spacer(16))
        column.addView(label("EMAIL"))
        column.addView(emailInput)
        column.addView(spacer(16))
        column.addView(label("INTERVIEW FOCUS"))
        column.addView(focusInput)
        column.addView(spacer(24))
        
        column.addView(primaryButton("Save profile") {
            val name = nameInput.text.toString()
            val focus = focusInput.text.toString()
            scope.launch {
                val updated = backend.updateUserProfile(appState.authToken, name, focus)
                if (updated) {
                    appState.userFullName = name
                    appState.userFocus = focus
                    showAppToast("Profile updated.", ToastKind.green)
                    showAccount()
                } else {
                    showAppToast("Failed to update profile.", ToastKind.ERROR)
                }
            }
        })
        setScreen(scroll(column))
    }

    private fun showLowStorage() {
        val column = baseColumn()
        column.addView(backButton { showSettings() })
        column.addView(title("Low Storage Warning", 34))
        column.addView(body("Clear space for best recording quality."))
        column.addView(spacer(16))
        // Storage items
        column.addView(errorItemRow("R", "Recording may fail", "Recommended", green))
        column.addView(errorItemRow("F", "Free up storage", "Ready", accent))
        column.addView(errorItemRow("T", "Try again", "Ready", accent))
        column.addView(spacer(16))
        column.addView(primaryButton("Manage storage") {
            runCatching {
                startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION))
            }
        })
        setScreen(scroll(column))
    }

    private fun showMicDenied() {
        val column = baseColumn()
        column.addView(backButton { showSettings() })
        column.addView(title("Microphone access", 34))
        column.addView(body("Prezzence needs microphone access to record your answer and give feedback."))
        // Mic blocked indicator
        column.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(24), 0, dp(24))
            layoutParams = blockParams()
            addView(TextView(this@MainActivity).apply {
                text = "ðŸŽ¤"
                textSize = 48f
                gravity = Gravity.CENTER
            })
            addView(pill("MIC BLOCKED", danger))
        })
        column.addView(label("HOW TO FIX IT"))
        column.addView(settingsRow("Phone settings", "Allow microphone access in your device settings", icon = SettingsIcon.Shield) {
            runCatching { startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = android.net.Uri.parse("package:$packageName") }) }
        })
        column.addView(settingsRow("Privacy", "Recording starts only when you answer a question", icon = SettingsIcon.Shield) { showLegal() })
        column.addView(settingsRow("Microphone check", "Make sure your microphone works in other apps", icon = SettingsIcon.Sound) { showDeviceQa() })
        column.addView(spacer(8))
        column.addView(primaryButton("Open Device Settings") {
            runCatching { startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = android.net.Uri.parse("package:$packageName") }) }
        })
        setScreen(scroll(column))
    }

    private fun showNetworkError(returnToHome: Boolean = false) {
        val column = baseColumn()
        column.addView(backButton { if (returnToHome) showHome() else finish() })
        column.addView(title("Connection problem", 34))
        column.addView(body("We could not connect. Check your connection and try again."))
        // Glitch orb
        column.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(24), 0, dp(24))
            layoutParams = blockParams()
            addView(TextView(this@MainActivity).apply {
                text = "\uD83D\uDCF6"
                textSize = 48f
                gravity = Gravity.CENTER
            })
            addView(pill("CONNECTION UNAVAILABLE", accent))
        })
        column.addView(label("WHAT YOU CAN DO"))
        column.addView(settingsRow("Try again", "Retry the request when your connection is stable", icon = SettingsIcon.QA) {
            if (returnToHome) showHome() else finish()
        })
        column.addView(settingsRow("Keep progress on this device", "Your unfinished session can be continued later", icon = SettingsIcon.File) { showHome() })
        column.addView(settingsRow("Check your network", "Switch Wi-Fi or mobile data, then try again", icon = SettingsIcon.Globe) {
            runCatching { startActivity(android.content.Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)) }
        })
        column.addView(spacer(8))
        column.addView(primaryButton("Go back") { if (returnToHome) showHome() else finish() })
        setScreen(scroll(column))
    }

    private data class SessionErrorInfo(val title: String, val subtitle: String, val badgeLabel: String, val tips: List<String>)

    private fun showSessionCreateError(reason: SessionErrorReason) {
        val errorInfo = when (reason) {
            SessionErrorReason.AUTH_FAILED -> SessionErrorInfo(
                "Session expired",
                "Your sign-in has expired or is invalid. Please sign in again.",
                "AUTH EXPIRED",
                listOf("Sign in again to continue", "Your progress is saved on this device"),
            )
            SessionErrorReason.SERVER_TIMEOUT -> SessionErrorInfo(
                "Taking too long",
                "The server is not responding. Please try again in a moment.",
                "SERVER TIMEOUT",
                listOf("Try again when your connection is stable", "Your unfinished session can be continued later"),
            )
            SessionErrorReason.NETWORK_UNAVAILABLE -> SessionErrorInfo(
                "Connection problem",
                "We could not connect. Check your connection and try again.",
                "NETWORK UNAVAILABLE",
                listOf("Switch Wi-Fi or mobile data, then try again", "Your unfinished session can be continued later"),
            )
            SessionErrorReason.SERVER_ERROR -> SessionErrorInfo(
                "Something went wrong",
                "Our server encountered an error. Please try again.",
                "SERVER ERROR",
                listOf("Try again in a few minutes", "Your progress is saved on this device"),
            )
            SessionErrorReason.UNKNOWN -> SessionErrorInfo(
                "Something went wrong",
                "An unexpected error occurred. Please try again.",
                "ERROR",
                listOf("Try again", "Your progress is saved on this device"),
            )
        }
        val column = baseColumn()
        column.addView(backButton { showHome() })
        column.addView(title(errorInfo.title, 34))
        column.addView(body(errorInfo.subtitle))
        column.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(24), 0, dp(24))
            layoutParams = blockParams()
            addView(pill(errorInfo.badgeLabel, accent))
        })
        column.addView(label("WHAT YOU CAN DO"))
        for (tip in errorInfo.tips) {
            column.addView(settingsRow(tip, "", icon = SettingsIcon.QA) { showHome() })
        }
        column.addView(spacer(8))
        column.addView(primaryButton("Go back") { showHome() })
        setScreen(scroll(column))
    }

    private fun showProcessingTimeout() {
        val column = baseColumn()
        column.addView(headerWithHome("Processing Timeout") { showProcessingTimeout() })
        column.addView(progressSegments(3, 7))
        column.addView(title("Still processing.", 34))
        column.addView(body("This is taking longer than usual."))
        // Avatar stage
        column.addView(avatarStageHero())
        // Action card
        column.addView(label("WHAT YOU CAN DO"))
        column.addView(errorItemRow("R", "Retry analysis", "Recommended", green))
        column.addView(errorItemRow("S", "Skip scoring", "Ready", accent))
        column.addView(spacer(8))
        column.addView(primaryButton("Continue Waiting") { showHome() })
        setScreen(scroll(column))
    }

    private fun showSessionInterrupted() {
        val column = baseColumn()
        column.addView(headerWithHome("Session Interrupted") { showSessionInterrupted() })
        column.addView(progressSegments(3, 7))
        column.addView(title("Session paused.", 34))
        column.addView(body("You were away for 12 minutes."))
        column.addView(avatarStageHero())
        column.addView(label("WHAT YOU CAN DO"))
        column.addView(errorItemRow("R", "Resume", "Recommended", green))
        column.addView(errorItemRow("R", "Restart from this question", "Ready", accent))
        column.addView(spacer(8))
        column.addView(primaryButton("Resume Practice") { showHome() })
        setScreen(scroll(column))
    }

    private fun showSessionSaveError() {
        val column = baseColumn()
        column.addView(headerWithHome("Session Failed to Save") { showSessionSaveError() })
        column.addView(progressSegments(3, 7))
        column.addView(title("Session couldn't be saved.", 34))
        column.addView(body("We'll keep retrying before you lose anything."))
        column.addView(avatarStageHero())
        column.addView(label("CAREFUL ACTION"))
        column.addView(errorItemRow("M", "Manual retry", "Recommended", green))
        column.addView(errorItemRow("E", "Export audio", "Recommended", green))
        column.addView(spacer(8))
        column.addView(primaryButton("Manual Retry") { showHome() })
        setScreen(scroll(column))
    }

    private fun showNotFound(returnTo: () -> Unit = { showHome() }) {
        setScreen(LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.VERTICAL
            setPadding(dp(32), dp(32), dp(32), dp(32))
            background = rounded(bg)
            addView(LinearLayout(this@MainActivity).apply {
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(160), dp(160)).apply { setMargins(0, 0, 0, dp(48)) }
                background = rounded(Color.argb(12, 108, 99, 255), radius = 80)
                addView(TextView(this@MainActivity).apply {
                    text = "?"
                    textSize = 48f
                    setTextColor(accent)
                    gravity = Gravity.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                })
                addView(View(this@MainActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(180), dp(180))
                    background = rounded(Color.TRANSPARENT, radius = 90).apply { setStroke(dp(1), Color.argb(25, 108, 99, 255)) }
                })
            })
            addView(title("Page not found", 32))
            addView(body("This page does not exist or the link is no longer available."))
            addView(spacer(48))
            addView(primaryButton("Return home") { returnTo() })
        })
    }

    private fun showHelp() {
        val column = baseColumn()
        column.addView(backButton { showSettings() })
        column.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(24), dp(8), dp(24), 0)
            (0 until 7).forEach { i ->
                addView(View(this@MainActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(0, dp(4), 1f).apply { setMargins(dp(4), 0, dp(4), 0) }
                    background = rounded(if (i < 3) accent else Color.argb(25, 255, 255, 255), radius = 2)
                })
            }
        })
        column.addView(spacer(24))
        column.addView(TextView(this).apply {
            text = "Help"; textSize = 48f; setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        })
        column.addView(body("Search support topics and tutorials."))
        column.addView(spacer(8))
        column.addView(label("SEARCH HELP"))
        column.addView(input("Scoring, subscriptions, microphone..."))
        column.addView(spacer(8))
        column.addView(errorItemRow("G", "Getting Started", "Recommended", green))
        column.addView(errorItemRow("I", "Interview Modes", "Ready", accent))
        column.addView(errorItemRow("S", "Scoring & Feedback", "Ready", accent))
        column.addView(errorItemRow("A", "Avatar Interviews", "Animated preview", accent))
        column.addView(spacer(8))
        column.addView(primaryButton("Contact Support") { showFeedback() })
        setScreen(scroll(column))
    }

    private fun showPaymentSuccess() {
        val column = baseColumn()
        column.addView(backButton { showHome() })
        column.addView(spacer(24))
        column.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.VERTICAL
            layoutParams = blockParams()
            addView(TextView(this@MainActivity).apply {
                text = "\u2713"
                textSize = 36f; setTextColor(green); gravity = Gravity.CENTER
                setPadding(dp(24), dp(24), dp(24), dp(24))
                background = rounded(green, radius = 36)
            })
        })
        column.addView(TextView(this).apply {
            text = "PAYMENT SUCCESS"
            textSize = 12f; setTextColor(accent); typeface = Typeface.create("sans-serif", Typeface.BOLD)
            letterSpacing = 0.06f; gravity = Gravity.CENTER
        })
        column.addView(title("Prezzence Pro unlocked", 34))
        column.addView(body("Your account is ready for deeper practice."))
        column.addView(spacer(8))
        column.addView(card("You're ready for deeper practice", "You now have access to more sessions, deeper reports, and shareable score cards."))
        column.addView(spacer(16))
        column.addView(primaryButton("Go to Profile") { showHome(PrezzenceTab.PROFILE) })
        setScreen(scroll(column))
    }

    private fun showNotificationSettings() {
        val column = baseColumn()
        column.addView(backButton { showSettings() })
        column.addView(title("Notifications", 40))
        column.addView(body("Choose when Prezzence reminds you to practice or review progress."))
        column.addView(spacer(8))
        column.addView(label("CHANNELS"))
        val pushEnabled = booleanArrayOf(appState.pushNotificationsEnabled)
        val emailEnabled = booleanArrayOf(appState.emailSummariesEnabled)
        column.addView(settingsCard(listOf(
            toggleRow("Push notifications", "Practice reminders on this device", pushEnabled) { enabled ->
                appState.pushNotificationsEnabled = enabled
            },
            toggleRow("Email summaries", "Weekly progress and coaching summaries", emailEnabled) { enabled ->
                appState.emailSummariesEnabled = enabled
            },
        )))
        column.addView(spacer(8))
        column.addView(label("ACTIVITY ALERTS"))
        val remindersEnabled = booleanArrayOf(appState.practiceRemindersEnabled)
        val achievementsEnabled = booleanArrayOf(appState.achievementAlertsEnabled)
        column.addView(settingsCard(listOf(
            toggleRow("Practice reminders", "Get reminded before your interview date", remindersEnabled) { enabled ->
                appState.practiceRemindersEnabled = enabled
            },
            toggleRow("Milestone alerts", "Get alerts when your score or streak improves", achievementsEnabled) { enabled ->
                appState.achievementAlertsEnabled = enabled
            },
            toggleRow("Product updates", "New features and useful interview tips", booleanArrayOf(appState.productUpdatesEnabled)) { enabled ->
                appState.productUpdatesEnabled = enabled
            },
        )))
        column.addView(spacer(16))
        column.addView(primaryButton("Done") { showSettings() })
        setScreen(scroll(column))
    }

    private fun showAppSettings() {
        val column = baseColumn()
        column.addView(backButton { showSettings() })
        column.addView(title("App Settings", 34))
        column.addView(body("Manage accessibility and app settings."))
        // Already have accessibility, so focus on WiFi-only
        column.addView(toggleRow("Download over Wi-Fi only", "Only download model files on Wi-Fi", booleanArrayOf(appState.wifiOnlyDownloads)) { enabled ->
            appState.wifiOnlyDownloads = enabled
        })
        column.addView(spacer(8))
        column.addView(primaryButton("Save Settings") { showSettings() })
        setScreen(scroll(column))
    }

    // --- Helpers for the new screens ---

    private fun errorItemRow(letter: String, title: String, subtitle: String, badgeColor: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = rounded(panel)
        layoutParams = blockParams()
        // Letter icon
        addView(TextView(this@MainActivity).apply {
            text = letter
            textSize = 18f
            setTextColor(accent)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(11), dp(11), dp(11), dp(11))
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            background = rounded(surface, radius = 12, strokeColor = border)
        })
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(12), 0, dp(8), 0) }
            addView(TextView(this@MainActivity).apply {
                text = title; textSize = 16f; setTextColor(Color.WHITE)
                typeface = Typeface.create("sans-serif", Typeface.BOLD); includeFontPadding = false
            })
            addView(TextView(this@MainActivity).apply {
                text = subtitle; textSize = 13f; setTextColor(muted); includeFontPadding = false; setPadding(0, dp(3), 0, 0)
            })
        })
        // Status badge
        addView(TextView(this@MainActivity).apply {
            text = "On"
            textSize = 11f
            setTextColor(badgeColor)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(dp(12), dp(5), dp(12), dp(5))
            background = rounded(Color.argb(32, Color.red(badgeColor), Color.green(badgeColor), Color.blue(badgeColor)), radius = 8)
        })
    }

    private fun toggleRow(label: String, subtitle: String, state: BooleanArray, onToggle: (Boolean) -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = rounded(panel)
        layoutParams = blockParams()
        setOnClickListener {
            state[0] = !state[0]
            onToggle(state[0])
            // Toggle visual â€” rebuild by calling refresh
        }
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(TextView(this@MainActivity).apply {
                text = label; textSize = 16f; setTextColor(Color.WHITE)
                typeface = Typeface.create("sans-serif", Typeface.BOLD); includeFontPadding = false
            })
            addView(TextView(this@MainActivity).apply {
                text = subtitle; textSize = 13f; setTextColor(muted); includeFontPadding = false; setPadding(0, dp(3), 0, 0)
            })
        })
        // Custom toggle switch
        val thumb = View(this@MainActivity)
        addView(LinearLayout(this@MainActivity).apply {
            val isOn = state[0]
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(28)).apply { setMargins(dp(12), 0, 0, 0) }
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(3), dp(3), dp(3), dp(3))
            background = rounded(if (isOn) accent else Color.argb(25, 255, 255, 255), radius = 14)
            setOnClickListener {
                state[0] = !state[0]
                onToggle(state[0])
                // Toggle visual
                setBackgroundDrawable(rounded(if (state[0]) accent else Color.argb(25, 255, 255, 255), radius = 14))
                thumb.layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply {
                    gravity = if (state[0]) Gravity.END else Gravity.START
                }
                thumb.requestLayout()
            }
            addView(thumb.apply {
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply {
                    gravity = if (isOn) Gravity.END else Gravity.START
                }
                background = rounded(Color.WHITE, radius = 11)
            })
        })
    }

    private fun headerWithHome(titleText: String, onHome: () -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(4), dp(16), dp(4), dp(8))
        addView(ghostButton("< Back") { onHome() })
        addView(TextView(this@MainActivity).apply {
            text = titleText
            textSize = 18f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        addView(ghostButton("Home") { showHome() })
    }

    private fun progressSegments(active: Int, total: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(dp(4), dp(4), dp(4), dp(16))
        for (i in 0 until total) {
            addView(View(this@MainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(4), 1f).apply { setMargins(dp(2), 0, dp(2), 0) }
                background = rounded(if (i < active) accent else Color.argb(30, 255, 255, 255), radius = 2)
            })
        }
    }

    private fun avatarStageHero() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(dp(16), dp(24), dp(16), dp(24))
        layoutParams = blockParams()
        background = rounded(Color.argb(15, Color.red(accent), Color.green(accent), Color.blue(accent)), radius = 16)

        listOf(
            Triple("Sarah", green, false),
            Triple("Marcus", danger, true),
            Triple("Priya", warning, false),
        ).forEach { (name, color, dimmed) ->
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                alpha = if (dimmed) 0.4f else 1f
                addView(TextView(this@MainActivity).apply {
                    text = "\uD83D\uDC64"
                    textSize = 32f
                    gravity = Gravity.CENTER
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    layoutParams = LinearLayout.LayoutParams(dp(56), dp(56)).apply { setMargins(0, 0, 0, dp(8)) }
                    background = rounded(surface, radius = 28, strokeColor = color)
                })
                addView(TextView(this@MainActivity).apply {
                    text = name; textSize = 14f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
                    typeface = Typeface.create("sans-serif", Typeface.BOLD)
                })
                addView(pill(if (color == green) "green" else if (color == danger) "danger" else "warning", color))
            })
        }
    }

    private fun showAccessibility() {
        val column = baseColumn()
        column.addView(backButton { showSettings() })
        column.addView(title("Accessibility", 40))
        column.addView(body("Adjust text, motion, and audio settings to make the app easier to use."))
        
        column.addView(spacer(8))
        column.addView(label("VISION & DISPLAY"))
        val largeText = booleanArrayOf(appState.a11yLargeText)
        column.addView(toggleRow("Large text", "200% font scale across all screens", largeText) { enabled ->
            appState.a11yLargeText = enabled; showAccessibility()
        })
        val highContrast = booleanArrayOf(appState.a11yHighContrast)
        column.addView(toggleRow("High contrast", "Enhanced edge definition for UI elements", highContrast) { enabled ->
            appState.a11yHighContrast = enabled; showAccessibility()
        })
        val reducedMotion = booleanArrayOf(appState.a11yReducedMotion)
        column.addView(toggleRow("Reduced Motion", "Minimize animations and motion effects", reducedMotion) { enabled ->
            appState.a11yReducedMotion = enabled; showAccessibility()
        })
        
        column.addView(spacer(8))
        column.addView(label("AUDIO & FEEDBACK"))
        column.addView(toggleRow("Screen reader support", "Clear voice and label support", booleanArrayOf(false)) {})
        column.addView(toggleRow("Mono Audio", "Combine channels for single-ear focus", booleanArrayOf(false)) {})
        
        column.addView(spacer(8))
        column.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(32), dp(32), dp(32), dp(32))
            background = rounded(Color.argb(5, 255, 255, 255), radius = 32, strokeColor = border)
            layoutParams = blockParams()
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(TextView(this@MainActivity).apply {
                    val checkDrawable = object : android.graphics.drawable.Drawable() {
                        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = green; style = Paint.Style.STROKE; strokeWidth = 2.5f; strokeCap = Paint.Cap.ROUND
                        }
                        override fun draw(canvas: Canvas) {
                            val w = bounds.width().toFloat(); val h = bounds.height().toFloat()
                            canvas.drawLine(w*0.22f, h*0.52f, w*0.42f, h*0.72f, p)
                            canvas.drawLine(w*0.42f, h*0.72f, w*0.78f, h*0.28f, p)
                        }
                        override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
                        override fun setAlpha(alpha: Int) {}
                        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
                    }
                    checkDrawable.setBounds(0, 0, dp(18), dp(18))
                    setCompoundDrawables(checkDrawable, null, null, null)
                    setPadding(dp(4), 0, 0, 0)
                    text = " PREVIEW"
                    textSize = 18f; setTextColor(green)
                    typeface = Typeface.create("sans-serif", Typeface.BOLD)
                })
            })
            addView(TextView(this@MainActivity).apply {
                text = "Easy to read"
                textSize = 28f; setTextColor(Color.WHITE); typeface = Typeface.create("sans-serif", Typeface.BOLD)
                setPadding(0, dp(16), 0, dp(12))
            })
            addView(TextView(this@MainActivity).apply {
                text = "\"Tell me about a time you solved a difficult problem.\" Text should stay readable without clipping."
                textSize = 20f; setTextColor(Color.argb(180, 255, 255, 255)); setLineSpacing(0f, 1.5f)
            })
        })
        
        setScreen(scroll(column))
    }

    private fun showSessionReport(sessionId: String) {
        val session = appState.sessionHistory().find { it.id == sessionId }
        if (session == null) {
            showHome(PrezzenceTab.PRACTICE)
            return
        }

        val score = session.score
        val scoreColorVal = when {
            score >= 75 -> green
            score >= 55 -> warning
            else -> danger
        }
        val hasSignal = score > 0
        val summary = if (hasSignal) {
            if (score >= 75) "Strong performance! Your answers were structured and relevant to the role."
            else if (score >= 50) "Good effort. Focus on using the STAR method to structure your examples more clearly."
            else "Keep practicing. Focus on directly answering the prompt and providing concrete evidence."
        } else {
            "Not enough usable speech was captured to produce a real coaching summary. This report is showing setup guidance instead of skill rankings."
        }
        val coachingPlan = listOf(
            if (hasSignal) "Use situation, action, tradeoff, and measurable result in your next answer."
            else "Retry the interview with the microphone close to your mouth.",
            if (hasSignal) "Practice more behavioral questions using the STAR method."
            else "Speak for at least 30 seconds before tapping Finish.",
            if (hasSignal) "Make sure to include quantifiable results in your examples."
            else "Make sure a transcript appears after each answer before continuing.",
        )
        val growthAreas = listOf(
            if (hasSignal) "Provide more concrete evidence in your answers."
            else "No weak area detected yet.",
            if (hasSignal) "Structure responses with clear beginning, middle, and end."
            else "Complete more questions to build a stronger report.",
        )

        val column = baseColumn()
        column.addView(backButton { showHome(PrezzenceTab.PRACTICE) })
        column.addView(title("Session Report", 34))
        column.addView(spacer(8))

        // Hero card
        column.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(22), dp(22), dp(22), dp(22))
            background = rounded(panel, radius = 28, strokeColor = border)
            layoutParams = blockParams()
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(this@MainActivity).apply {
                    text = (if (hasSignal) "COMPLETED" else "INCOMPLETE")
                    textSize = 12f; setTextColor(accent); typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    letterSpacing = 0.05f
                })
                addView(TextView(this@MainActivity).apply {
                    text = session.role.ifBlank { "Interview Assessment" }
                    textSize = 28f; setTextColor(Color.WHITE); typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    setPadding(0, dp(8), 0, 0)
                })
                addView(TextView(this@MainActivity).apply {
                    val detail = listOfNotNull(session.date, "${session.answered}/${session.total} answered").joinToString(" | ")
                    text = detail
                    textSize = 13f; setTextColor(muted); typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    setPadding(0, dp(8), 0, 0)
                })
                if (hasSignal) {
                    addView(TextView(this@MainActivity).apply {
                        text = "1 scored answer"
                        textSize = 12f; setTextColor(muted); typeface = Typeface.create("sans-serif", Typeface.BOLD)
                        setPadding(0, dp(8), 0, 0)
                    })
                }
            })
            addView(LinearLayout(this@MainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(86), dp(86))
                gravity = Gravity.CENTER
                orientation = LinearLayout.VERTICAL
                background = rounded(Color.argb(8, 255, 255, 255), radius = 28).apply { setStroke(dp(2), scoreColorVal) }
                addView(TextView(this@MainActivity).apply {
                    text = "$score"
                    textSize = 30f; setTextColor(Color.WHITE); typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    gravity = Gravity.CENTER
                })
                addView(TextView(this@MainActivity).apply {
                    text = "score"
                    textSize = 10f; setTextColor(muted); typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    gravity = Gravity.CENTER; letterSpacing = 0.05f
                })
            })
        })
        column.addView(spacer(14))

        // Export PDF button
        column.addView(LinearLayout(this).apply {
            layoutParams = blockParams()
            setPadding(dp(20), 0, dp(20), 0)
            addView(LinearLayout(this@MainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50))
                gravity = Gravity.CENTER
                background = rounded(accent, radius = 25)
                addView(TextView(this@MainActivity).apply {
                    text = "Export PDF report"
                    textSize = 14f; setTextColor(Color.WHITE); typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    setPadding(0, 0, dp(8), 0)
                })
                addView(TextView(this@MainActivity).apply {
                    text = if (appState.subscriptionEntitled) "" else "Pro"
                    textSize = 11f; setTextColor(Color.argb(180, 255, 255, 255))
                    typeface = Typeface.create("sans-serif", Typeface.BOLD)
                })
                setOnClickListener { exportSessionPdf(sessionId) }
            })
        })
        column.addView(spacer(14))

        // Coach Summary card
        column.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = rounded(panel, radius = 26, strokeColor = border)
            layoutParams = blockParams()
            addView(TextView(this@MainActivity).apply {
                text = "Coach Summary"
                textSize = 20f; setTextColor(Color.WHITE); typeface = Typeface.create("sans-serif", Typeface.BOLD)
            })
            addView(TextView(this@MainActivity).apply {
                text = summary
                textSize = 15f; setTextColor(muted); setPadding(0, dp(14), 0, 0)
            })
            if (hasSignal) {
                addView(spacer(16))
                addView(LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    val focusPill = { label: String, value: String ->
                        LinearLayout(this@MainActivity).apply {
                            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                            orientation = LinearLayout.VERTICAL
                            setPadding(dp(14), dp(14), dp(14), dp(14))
                            background = rounded(Color.argb(30, 108, 99, 255), radius = 18).apply { setStroke(dp(1), Color.argb(45, 108, 99, 255)) }
                            addView(TextView(this@MainActivity).apply {
                                text = label
                                textSize = 10f; setTextColor(muted); typeface = Typeface.create("sans-serif", Typeface.BOLD)
                                letterSpacing = 0.04f
                            })
                            addView(TextView(this@MainActivity).apply {
                                text = value
                                textSize = 15f; setTextColor(Color.WHITE); typeface = Typeface.create("sans-serif", Typeface.BOLD)
                                setPadding(0, dp(6), 0, 0)
                            })
                        }
                    }
                    addView(focusPill("Strongest", "Measured after more answers"))
                    addView(spacer(10))
                    addView(focusPill("Focus", "Measured after more answers"))
                })
            } else {
                addView(spacer(16))
                addView(LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(dp(14), dp(14), dp(14), dp(14))
                    background = rounded(Color.argb(25, 255, 176, 32), radius = 18).apply { setStroke(dp(1), Color.argb(55, 255, 176, 32)) }
                    addView(TextView(this@MainActivity).apply {
                        val micOff = object : android.graphics.drawable.Drawable() {
                            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = Color.argb(200, 255, 176, 32); style = Paint.Style.STROKE; strokeWidth = 2.5f; strokeCap = Paint.Cap.ROUND
                            }
                            override fun draw(canvas: Canvas) {
                                val w = bounds.width().toFloat(); val h = bounds.height().toFloat(); val cx = w / 2f; val cy = h * 0.35f
                                canvas.drawArc(android.graphics.RectF(cx - w*0.18f, cy - h*0.18f, cx + w*0.18f, cy + h*0.18f), 200f, 140f, false, p)
                                canvas.drawLine(cx - w*0.10f, cy + h*0.28f, cx + w*0.10f, cy + h*0.28f, p)
                            }
                            override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
                            override fun setAlpha(alpha: Int) {}
                            override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
                        }
                        micOff.setBounds(0, 0, dp(18), dp(18))
                        setCompoundDrawables(micOff, null, null, null)
                        textSize = 18f
                    })
                    addView(LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(12), 0, 0, 0) }
                        addView(TextView(this@MainActivity).apply {
                            text = "Report needs clearer audio"
                            textSize = 14f; setTextColor(Color.WHITE); typeface = Typeface.create("sans-serif", Typeface.BOLD)
                        })
                        addView(TextView(this@MainActivity).apply {
                            text = "Skill rankings are hidden until at least one answer has a usable transcript and score."
                            textSize = 13f; setTextColor(muted); setPadding(0, dp(4), 0, 0)
                        })
                    })
                })
            }
        })
        column.addView(spacer(14))

        // Strengths and weak spots
        if (hasSignal) {
            val radarData = listOf("Clarity" to 72, "Relevance" to 85, "Structure" to 60, "Evidence" to 45, "Conciseness" to 78)
            column.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(20), dp(20), dp(20), dp(20))
                background = rounded(panel, radius = 26, strokeColor = border)
                layoutParams = blockParams()
                addView(TextView(this@MainActivity).apply {
                    text = "Strengths and weak spots"
                    textSize = 20f; setTextColor(Color.WHITE); typeface = Typeface.create("sans-serif", Typeface.BOLD)
                })
                addView(TextView(this@MainActivity).apply {
                    text = "A simple view of where this interview was strong and what needs more practice."
                    textSize = 14f; setTextColor(muted); setPadding(0, dp(8), 0, dp(18))
                })
                // Radar chart via Canvas
                addView(ImageView(this@MainActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220))
                    setPadding(dp(16), dp(16), dp(16), dp(16))
                    setImageDrawable(object : android.graphics.drawable.Drawable() {
                        override fun draw(c: Canvas) {
                            val w = bounds.width().toFloat(); val h = bounds.height().toFloat()
                            val cx = w / 2f; val cy = h / 2f; val r = minOf(cx, cy) * 0.75f
                            val n = radarData.size; val angleStep = Math.PI * 2 / n
                            val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = Color.argb(20, 255, 255, 255); style = Paint.Style.STROKE; strokeWidth = 1f
                            }
                            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = Color.argb(35, 108, 99, 255); style = Paint.Style.FILL
                            }
                            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = accent; style = Paint.Style.STROKE; strokeWidth = 2f; strokeCap = Paint.Cap.ROUND
                            }
                            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = Color.argb(180, 255, 255, 255); textSize = 28f; textAlign = Paint.Align.CENTER
                            }
                            // Grid rings
                            for (ring in 1..3) {
                                val gr = r * ring / 3
                                val path = android.graphics.Path()
                                for (i in 0 until n) {
                                    val a = -Math.PI / 2 + i * angleStep
                                    val x = cx + gr * Math.cos(a).toFloat()
                                    val y = cy + gr * Math.sin(a).toFloat()
                                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                path.close()
                                c.drawPath(path, gridPaint)
                            }
                            // Data polygon
                            val dataPath = android.graphics.Path()
                            val dataPoints = radarData.mapIndexed { i, (_, value) ->
                                val a = -Math.PI / 2 + i * angleStep
                                val vr = r * value / 100f
                                cx + vr * Math.cos(a).toFloat() to cy + vr * Math.sin(a).toFloat()
                            }
                            dataPoints.forEachIndexed { i, (x, y) ->
                                if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
                            }
                            dataPath.close()
                            c.drawPath(dataPath, fillPaint)
                            c.drawPath(dataPath, linePaint)
                            // Labels and axis lines
                            radarData.forEachIndexed { i, (label, _) ->
                                val a = -Math.PI / 2 + i * angleStep
                                val lx = cx + r * Math.cos(a).toFloat()
                                val ly = cy + r * Math.sin(a).toFloat()
                                c.drawLine(cx, cy, lx, ly, gridPaint)
                                val lr = r * 1.25f
                                val lrx = cx + lr * Math.cos(a).toFloat()
                                val lry = cy + lr * Math.sin(a).toFloat()
                                c.drawText(label, lrx, lry + 10f, labelPaint)
                            }
                            // Dot on each data point
                            dataPoints.forEach { (x, y) ->
                                c.drawCircle(x, y, 5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; style = Paint.Style.FILL })
                            }
                        }
                        override fun setAlpha(a: Int) {}
                        override fun setColorFilter(cf: ColorFilter?) {}
                        override fun getOpacity() = PixelFormat.TRANSLUCENT
                    })
                })
                // Legend below chart
                radarData.forEach { (label, value) ->
                    addView(LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(0, dp(4), 0, dp(4))
                        addView(TextView(this@MainActivity).apply {
                            text = label; textSize = 13f; setTextColor(Color.WHITE)
                            typeface = Typeface.create("sans-serif", Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        })
                        addView(TextView(this@MainActivity).apply {
                            text = "$value%"; textSize = 13f; setTextColor(when { value >= 70 -> green; value >= 50 -> warning; else -> danger })
                            typeface = Typeface.create("sans-serif", Typeface.BOLD)
                        })
                    })
                }
            })
            column.addView(spacer(14))
        }

        // Coaching plan
        column.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = rounded(panel, radius = 26, strokeColor = border)
            layoutParams = blockParams()
            addView(TextView(this@MainActivity).apply {
                text = "Next Coaching Plan"
                textSize = 20f; setTextColor(Color.WHITE); typeface = Typeface.create("sans-serif", Typeface.BOLD)
            })
            coachingPlan.forEach { plan ->
                addView(TextView(this@MainActivity).apply {
                    text = "- $plan"
                    textSize = 15f; setTextColor(muted); setPadding(0, dp(10), 0, 0)
                })
            }
        })
        column.addView(spacer(14))

        // What to improve
        column.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = rounded(panel, radius = 26, strokeColor = border)
            layoutParams = blockParams()
            addView(TextView(this@MainActivity).apply {
                text = "What to improve"
                textSize = 20f; setTextColor(Color.WHITE); typeface = Typeface.create("sans-serif", Typeface.BOLD)
            })
            growthAreas.forEach { area ->
                addView(TextView(this@MainActivity).apply {
                    text = "- $area"
                    textSize = 15f; setTextColor(muted); setPadding(0, dp(10), 0, 0)
                })
            }
        })
        column.addView(spacer(14))

        // Answer Review
        val sessionAnswers = appState.getSessionAnswers(sessionId)
        if (sessionAnswers.isNotEmpty()) {
            column.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(20), dp(20), dp(20), dp(20))
                background = rounded(panel, radius = 26, strokeColor = border)
                layoutParams = blockParams()
                addView(TextView(this@MainActivity).apply {
                    text = "Answer Review"
                    textSize = 20f; setTextColor(Color.WHITE); typeface = Typeface.create("sans-serif", Typeface.BOLD)
                })
                addView(TextView(this@MainActivity).apply {
                    text = "${sessionAnswers.size} answer(s)"
                    textSize = 12f; setTextColor(muted); typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    setPadding(0, dp(4), 0, dp(12))
                })
                sessionAnswers.forEachIndexed { i, ans ->
                    val ansScoreColor = when { ans.score >= 70 -> green; ans.score >= 50 -> warning; else -> danger }
                    addView(LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(14), dp(12), dp(14), dp(12))
                        background = rounded(Color.argb(12, 255, 255, 255), radius = 18)
                        layoutParams = blockParams().apply { setMargins(0, 0, 0, dp(8)) }
                        addView(LinearLayout(this@MainActivity).apply {
                            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                            addView(TextView(this@MainActivity).apply {
                                text = "Q${i + 1}"; textSize = 13f; setTextColor(muted)
                                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                            })
                            addView(spacer(10))
                            addView(TextView(this@MainActivity).apply {
                                text = "Score: ${ans.score}"; textSize = 14f; setTextColor(ansScoreColor)
                                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                            })
                            addView(TextView(this@MainActivity).apply {
                                text = if (ans.feedback.length > 30) ans.feedback.take(28) + ".." else ans.feedback
                                textSize = 12f; setTextColor(muted)
                            })
                        })
                        addView(TextView(this@MainActivity).apply {
                            text = ans.transcript.take(120)
                            textSize = 13f; setTextColor(Color.WHITE); setPadding(0, dp(6), 0, 0)
                            maxLines = 3
                        })
                        addView(TextView(this@MainActivity).apply {
                            text = "Improved: ${ans.improvedAnswer.take(80)}"
                            textSize = 12f; setTextColor(accent); setPadding(0, dp(4), 0, 0)
                            maxLines = 2
                        })
                    })
                    column.addView(spacer(6))
                }
            })
            column.addView(spacer(14))
        }

        column.addView(primaryButton("Practice Again") {
            appState.resetActiveSession()
            showOnboardingRole()
        })

        setScreen(scroll(column))
    }

    private fun exportSessionPdf(sessionId: String) {
        scope.launch {
            try {
                val session = appState.sessionHistory().find { it.id == sessionId } ?: return@launch
                val answers = appState.getSessionAnswers(sessionId)
                val document = android.graphics.pdf.PdfDocument()
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
                val page = document.startPage(pageInfo)
                val c = page.canvas
                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 36f; typeface = Typeface.create("sans-serif", Typeface.BOLD) }
                val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 22f; typeface = Typeface.create("sans-serif", Typeface.BOLD) }
                val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 14f }
                val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(108, 99, 255); textSize = 28f; typeface = Typeface.create("sans-serif", Typeface.BOLD) }
                var y = 60f
                c.drawText("Session Report", 40f, y, titlePaint)
                y += 50f
                c.drawText("Role: ${session.role}", 40f, y, headingPaint)
                y += 30f
                c.drawText("Date: ${session.date}", 40f, y, bodyPaint)
                y += 24f
                c.drawText("Score: ${session.score}/100", 40f, y, scorePaint)
                y += 24f
                c.drawText("Questions: ${session.answered}/${session.total} answered", 40f, y, bodyPaint)
                y += 40f
                if (answers.isNotEmpty()) {
                    c.drawText("Answer Details", 40f, y, headingPaint)
                    y += 30f
                    for ((i, ans) in answers.withIndex()) {
                        if (y > 780f) break
                        c.drawText("${i + 1}. Score: ${ans.score} - ${ans.transcript.take(80)}", 40f, y, bodyPaint)
                        y += 20f
                        c.drawText("   Feedback: ${ans.feedback.take(100)}", 40f, y, bodyPaint)
                        y += 24f
                    }
                }
                document.finishPage(page)
                val file = java.io.File(cacheDir, "session-${sessionId.take(8)}.pdf")
                java.io.FileOutputStream(file).use { document.writeTo(it) }
                document.close()
                val uri = androidx.core.content.FileProvider.getUriForFile(this@MainActivity, "${packageName}.fileprovider", file)
                startActivity(Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                })
            } catch (e: Exception) {
                showAppToast("Export failed: ${e.message}", ToastKind.ERROR)
            }
        }
    }

    private fun showFeedback() {
        val column = baseColumn()
        var rating = 5
        var submitted = false
        val comment = EditText(this).apply {
            hint = "What should we improve?"
            minLines = 5
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            setPadding(dp(18), dp(14), dp(18), dp(14))
            background = rounded(panel)
            layoutParams = blockParams()
        }
        val progressRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(dp(300), dp(4)).apply { gravity = Gravity.CENTER }
            for (i in 0 until 5) {
                val segment = View(this@MainActivity)
                val color = if (i < rating) green else panel
                segment.setBackgroundColor(color)
                segment.layoutParams = LinearLayout.LayoutParams(0, dp(4), 1f).apply { setMargins(dp(2), 0, dp(2), 0) }
                val radius = android.graphics.Outline().apply { setRoundRect(0, 0, 1, 1, dp(2).toFloat()) }
                segment.clipToOutline = true
                segment.outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(v: View, outline: android.graphics.Outline) {
                        outline.setRoundRect(0, 0, v.width, v.height, dp(2).toFloat())
                    }
                }
                addView(segment)
            }
        }
        val ratingContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = blockParams()
            gravity = Gravity.CENTER_HORIZONTAL
        }
        ratingContainer.addView(spacer(24))
        // Circular rating indicator
        ratingContainer.addView(object : android.view.View(this) {
            var currentRating = 5
            override fun onDraw(canvas: Canvas) {
                val cx = width / 2f
                val cy = height / 2f
                val r = minOf(cx, cy) - dp(8).toFloat()
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = panel; style = Paint.Style.STROKE; strokeWidth = dp(6).toFloat(); strokeCap = Paint.Cap.ROUND }
                val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = green; style = Paint.Style.STROKE; strokeWidth = dp(6).toFloat(); strokeCap = Paint.Cap.ROUND }
                canvas.drawCircle(cx, cy, r, bgPaint)
                val sweep = 360f * currentRating / 5f
                canvas.drawArc(android.graphics.RectF(cx - r, cy - r, cx + r, cy + r), -90f, sweep, false, fgPaint)
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = dp(48).toFloat(); textAlign = Paint.Align.CENTER; typeface = Typeface.create("sans-serif", Typeface.BOLD) }
                canvas.drawText("$currentRating", cx, cy + dp(16).toFloat(), textPaint)
            }
        }.apply {
            layoutParams = LinearLayout.LayoutParams(dp(150), dp(150))
        })
        // Stars row
        val starsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, dp(50)).apply { gravity = Gravity.CENTER; topMargin = dp(28) }
        }
        for (s in 1..5) {
            val starIndex = s
            val star = object : android.view.View(this) {
                var filled = starIndex <= rating
                override fun onDraw(canvas: Canvas) {
                    val c = if (filled) green else panel
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c; style = Paint.Style.FILL }
                    val path = starPath(width / 2f, height / 2f, minOf(width, height) / 2.2f)
                    canvas.drawPath(path, paint)
                    if (!filled) {
                        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = border; style = Paint.Style.STROKE; strokeWidth = 2f })
                    }
                }
            }.apply {
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply { setMargins(dp(6), 0, dp(6), 0) }
                setOnClickListener {
                    rating = starIndex
                    progressRow.removeAllViews()
                    for (i in 0 until 5) {
                        val seg = View(this@MainActivity)
                        seg.setBackgroundColor(if (i < rating) green else panel)
                        seg.layoutParams = LinearLayout.LayoutParams(0, dp(4), 1f).apply { setMargins(dp(2), 0, dp(2), 0) }
                        seg.clipToOutline = true
                        seg.outlineProvider = object : android.view.ViewOutlineProvider() {
                            override fun getOutline(v: View, outline: android.graphics.Outline) {
                                outline.setRoundRect(0, 0, v.width, v.height, dp(2).toFloat())
                            }
                        }
                        progressRow.addView(seg)
                    }
                    for (i in 0 until starsRow.childCount) {
                        (starsRow.getChildAt(i) as? android.view.View)?.invalidate()
                    }
                    (ratingContainer.getChildAt(1) as? android.view.View)?.let {
                        (it as? android.view.View)?.invalidate()
                    }
                }
                setTag(starIndex)
            }
            starsRow.addView(star)
        }
        ratingContainer.addView(starsRow)
        ratingContainer.addView(spacer(24))

        column.addView(backButton { showSettings() })
        column.addView(title("Feedback", 34))
        column.addView(spacer(8))
        column.addView(progressRow)
        column.addView(ratingContainer)
        column.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = blockParams()
            addView(TextView(this@MainActivity).apply {
                text = "How was it?"
                textSize = 32f
                setTextColor(Color.WHITE)
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                gravity = Gravity.CENTER
                layoutParams = blockParams()
            })
            addView(TextView(this@MainActivity).apply {
                text = "Rate your interview practice experience."
                textSize = 15f
                setTextColor(muted)
                gravity = Gravity.CENTER
                layoutParams = blockParams()
            })
        })
        column.addView(spacer(16))
        column.addView(TextView(this@MainActivity).apply {
            text = "COMMENT (OPTIONAL)"
            textSize = 11f
            setTextColor(muted)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        })
        column.addView(comment)
        column.addView(primaryButton("Submit feedback") {
            val text = comment.text.toString().trim()
            if (text.length < 4 && rating == 5) {
                showAppToast("Add a comment or lower your rating.", ToastKind.WARNING)
                return@primaryButton
            }
            submitted = true
            column.removeAllViews()
            column.addView(backButton { showSettings() })
            column.addView(spacer(60))
            column.addView(object : android.view.View(this) {
                override fun onDraw(canvas: Canvas) {
                    val cx = width / 2f
                    val cy = height / 2f
                    val r = minOf(cx, cy) - dp(4).toFloat()
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = green; style = Paint.Style.STROKE; strokeWidth = dp(5).toFloat(); strokeCap = Paint.Cap.ROUND }
                    canvas.drawArc(android.graphics.RectF(cx - r, cy - r, cx + r, cy + r), 0f, 360f, false, paint)
                    val check = android.graphics.Path().apply {
                        moveTo(cx - r * 0.35f, cy)
                        lineTo(cx - r * 0.08f, cy + r * 0.30f)
                        lineTo(cx + r * 0.40f, cy - r * 0.25f)
                    }
                    canvas.drawPath(check, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = green; style = Paint.Style.STROKE; strokeWidth = dp(5).toFloat(); strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND })
                }
            }.apply {
                layoutParams = LinearLayout.LayoutParams(dp(100), dp(100)).apply { gravity = Gravity.CENTER }
            })
            column.addView(spacer(24))
            column.addView(TextView(this@MainActivity).apply {
                this.text = "Thank You!"
                textSize = 32f
                setTextColor(Color.WHITE)
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                gravity = Gravity.CENTER
            })
            column.addView(TextView(this@MainActivity).apply {
                this.text = "Your feedback helps us improve the app."
                textSize = 15f
                setTextColor(muted)
                gravity = Gravity.CENTER
            })
            setScreen(scroll(column))
            scope.launch {
                val sent = backend.submitFeedback(appState.authToken.ifBlank { null }, appState.activeSessionId.ifBlank { null }, rating, text)
                if (!sent) showAppToast("Could not send feedback. Sign in and try again.", ToastKind.ERROR)
            }
        })
        setScreen(scroll(column))
    }

    private fun showPaywall() {
        val column = baseColumn()
        column.addView(backButton { showSettings() })
        column.addView(title("Practice more deeply", 36))
        column.addView(body("Pro is for candidates who want more sessions, stronger reports, company research, and advanced practice options."))
        column.addView(card("Pro", "Final pricing appears through Google Play or App Store checkout."))
        val benefits = listOf(
            "Unlimited sessions",
            "Role skills and leadership practice",
            "More interviewer styles",
            "Interview countdown plan",
            "PDF export for session reports",
            "Online company research",
            "Deeper session reports and trend coaching",
            "Higher usage limits for scoring and voice",
        )
        benefits.forEach { b -> column.addView(pill(b, accent)) }
        column.addView(spacer(12))
        column.addView(card("Beta access", "Payments are switched off while testers use the app. You can explore Pro features and send feedback before pricing goes live."))
        column.addView(primaryButton("Start Pro") { purchaseSubscription() })
        column.addView(secondaryButton("Restore purchases") { restoreSubscription() })
        setScreen(scroll(column))
    }

    private fun showSubscription() {
        val column = baseColumn()
        column.addView(backButton { showPaywall() })
        column.addView(title("Subscription", 34))
        column.addView(spacer(8))
        column.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            background = rounded(panel, radius = 28, strokeColor = border)
            layoutParams = blockParams()
            addView(TextView(this@MainActivity).apply {
                text = "PLAN"
                textSize = 10f; setTextColor(accent); typeface = Typeface.create("sans-serif", Typeface.BOLD)
                letterSpacing = 0.15f
            })
            addView(TextView(this@MainActivity).apply {
                text = "Prezzence Pro"
                textSize = 24f; setTextColor(Color.WHITE); typeface = Typeface.create("sans-serif", Typeface.BOLD)
                setPadding(0, dp(8), 0, 0)
            })
            addView(spacer(16))
            listOf("Unlimited practice sessions", "Deep coaching reports", "Shareable score cards", "Export to PDF").forEach { benefit ->
                addView(LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dp(6), 0, dp(6))
                    addView(TextView(this@MainActivity).apply {
                        text = "\u2713"; textSize = 16f; setTextColor(green)
                    })
                    addView(TextView(this@MainActivity).apply {
                        text = "  $benefit"; textSize = 15f; setTextColor(Color.WHITE)
                    })
                })
            }
            addView(spacer(16))
            addView(TextView(this@MainActivity).apply {
                text = "Status: ${if (appState.subscriptionEntitled) "Active" else "Inactive"}"
                textSize = 13f; setTextColor(muted)
            })
        })
        column.addView(spacer(16))
        column.addView(primaryButton("Subscribe") { purchaseSubscription() })
        column.addView(spacer(8))
        column.addView(secondaryButton("Check plan") { refreshSubscription() })
        column.addView(spacer(8))
        column.addView(secondaryButton("Restore purchases") { restoreSubscription() })
        setScreen(scroll(column))
        refreshSubscription(silent = true)
    }

    private fun refreshSubscription(silent: Boolean = false) {
        scope.launch {
            val state = billingManager.refresh()
            showBillingToast(state, silent)
            if (!silent) showSubscription()
        }
    }

    private fun purchaseSubscription() {
        scope.launch {
            val state = billingManager.purchase(this@MainActivity)
            showBillingToast(state, silent = false)
        }
    }

    private fun restoreSubscription() {
        scope.launch {
            val state = billingManager.restore()
            showBillingToast(state, silent = false)
            showSubscription()
        }
    }

    private fun showBillingToast(state: BillingUiState, silent: Boolean) {
        appState.subscriptionEntitled = state.entitled
        appState.subscriptionStatus = state.status
        appState.subscriptionProductId = state.productId
        if (!silent) showAppToast(state.status, if (state.entitled) ToastKind.green else ToastKind.INFO)
    }

    private fun showDeviceQa(results: List<DeviceQaResult> = emptyList(), running: Boolean = false, status: String = "Ready") {
        val column = baseColumn()
        column.addView(backButton { showSettings() })
        column.addView(title("Device QA", 34))
        column.addView(body(status))
        column.addView(spacer(8))
        column.addView(rowOf(
            secondaryButton("Allow mic/camera") {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA), 200)
            },
            primaryButton(if (running) "Running" else "Run checks") { if (!running) runDeviceQa() }
        ))
        column.addView(spacer(8))
        if (results.isEmpty()) {
            column.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(20), dp(20), dp(20), dp(20))
                background = rounded(panel, radius = 26, strokeColor = border)
                layoutParams = blockParams()
                addView(TextView(this@MainActivity).apply {
                    text = "Checks"; textSize = 20f; setTextColor(Color.WHITE)
                    typeface = Typeface.create("sans-serif", Typeface.BOLD)
                })
                addView(spacer(8))
                listOf("Microphone PCM capture", "First-run Whisper model download", "Native Whisper JNI load", "CameraX provider open", "Duix model endpoint reachability").forEach { check ->
                    addView(LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(0, dp(6), 0, dp(6))
                        addView(TextView(this@MainActivity).apply {
                            text = "..."; textSize = 14f; setTextColor(muted)
                        })
                        addView(TextView(this@MainActivity).apply {
                            text = "  $check"; textSize = 14f; setTextColor(Color.WHITE)
                        })
                    })
                }
            })
        } else {
            results.forEach { result ->
                column.addView(LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(20), dp(20), dp(20), dp(20))
                    background = rounded(panel, radius = 26, strokeColor = if (result.passed) Color.argb(25, 0, 214, 143) else Color.argb(25, 255, 71, 87))
                    layoutParams = blockParams()
                    addView(LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        addView(TextView(this@MainActivity).apply {
                            text = if (result.passed) "\u2713" else "\u2717"
                            textSize = 20f; setTextColor(if (result.passed) green else danger)
                        })
                        addView(TextView(this@MainActivity).apply {
                            text = "  ${result.name}"
                            textSize = 18f; setTextColor(Color.WHITE); typeface = Typeface.create("sans-serif", Typeface.BOLD)
                        })
                    })
                    addView(spacer(6))
                    addView(TextView(this@MainActivity).apply {
                        text = result.detail; textSize = 13f; setTextColor(muted)
                    })
                })
                column.addView(spacer(8))
            }
            column.addView(secondaryButton("Run again") { runDeviceQa() })
        }
        setScreen(scroll(column))
    }

    private fun runDeviceQa() {
        showDeviceQa(running = true, status = "Starting checks")
        scope.launch {
            val runner = DeviceQaRunner(this@MainActivity)
            val results = runner.runAll(appState.language) { status ->
                withContext(Dispatchers.Main) { showDeviceQa(running = true, status = status) }
            }
            showDeviceQa(results = results, status = "Checks complete")
        }
    }


    private fun showVisualParity() {
        val column = baseColumn()
        column.addView(backButton { showSettings() })
        column.addView(title("Visual parity", 34))
        column.addView(body("Route-by-route coverage against the React Native app. Close/wired means the UI exists and actions are connected, but final 100% status still requires screenshot comparison on this device."))
        visualParityRows().forEach { row ->
            column.addView(card(row.first, "React Native: ${row.second}\nKotlin: ${row.third}"))
        }
        setScreen(scroll(column))
    }

    private fun visualParityRows(): List<Triple<String, String, String>> = listOf(
        Triple("Splash / landing", "/", "Near replica / wired: Compose landing and auth navigation"),
        Triple("Language", "/auth/language", "Functional / close: language selection saved locally"),
        Triple("Sign in", "/auth/sign-in", "Near replica / wired: email/password, Google, forgot password"),
        Triple("Sign up", "/auth/sign-up", "Near replica / wired: email signup, Google, verify panel, legal links"),
        Triple("Forgot password", "/auth/forgot-password", "Near replica / wired: Supabase reset email"),
        Triple("Reset password", "/auth/reset-password", "Close / wired: recovery deep link updates password"),
        Triple("Verify / email confirmed", "/verify", "Close / wired: Android App Link opens verify screen"),
        Triple("Home", "/(tabs)/home", "Functional / not full RN replica: start, questions, progress, settings, QA"),
        Triple("Question selection", "/(tabs)/questions and /onboarding/role", "Functional / partial RN replica: role and question selection"),
        Triple("Onboarding type", "/onboarding/type", "Close / wired: track and mode selection"),
        Triple("Mic permission", "/onboarding/mic-permission", "Close / wired: Android permission flow"),
        Triple("Interview setup", "/interview/entering", "Close / wired: creates backend session, uses RN persona images"),
        Triple("Interview room", "/interview/speaking", "Functional / closer: avatar, cached TTS, mic, camera coach, panel support"),
        Triple("Answer result", "/interview/speaking result state", "Functional / partial RN replica: scoring and continue/retry"),
        Triple("Progress", "/(tabs)/progress", "Functional / not visually matched: local and remote history"),
        Triple("Practice history", "/(tabs)/practice and /sessions", "Partial: history exists through progress screen"),
        Triple("Session report", "/sessions/[sessionId]", "Partial: no dedicated report route"),
        Triple("Profile", "/(tabs)/profile", "Functional / not visually matched: combined settings/profile surfaces"),
        Triple("Settings", "/settings", "Functional / not visually matched"),
        Triple("Account", "/settings/account", "Functional / not visually matched: sign out and delete"),
        Triple("Notifications", "/profile/notifications and /settings/notifications", "Functional / not visually matched: fetch, mark read, delete"),
        Triple("Resume / CV", "Profile resume card", "Functional / not visually matched: upload, text save, fetch, delete"),
        Triple("Subscription", "/profile/subscription and /(modals)/paywall", "Functional / not visually matched: Google Play Billing wired"),
        Triple("Privacy / Terms", "/profile/privacy, /settings/privacy, /legal/privacy, /legal/terms", "Functional / not visually matched: combined legal page"),
        Triple("Feedback", "/feedback", "Functional / not visually matched: backend submit"),
        Triple("Profile edit", "/profile/edit", "Missing dedicated route"),
        Triple("Accessibility", "/settings/accessibility", "Missing dedicated route"),
        Triple("Payment green", "/profile/payment-green", "Missing dedicated route"),
        Triple("Help", "/profile/help", "Missing dedicated route"),
        Triple("Device failure states", "/errors/*", "Partial: toasts, QA, and fallbacks; no dedicated RN error screens")
    )
    private var privacyImprovementEnabled = true
    private var privacyRetentionEnabled = false

    private fun showPrivacySettings() {
        val column = baseColumn()
        column.addView(backButton { showSettings() })
        column.addView(title("Privacy", 40))
        column.addView(body("Control how your interview recordings, transcripts, scores, and account data are handled."))
        column.addView(spacer(12))
        column.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            background = rounded(Color.argb(12, 0, 214, 143), radius = 32, strokeColor = Color.argb(38, 0, 214, 143))
            layoutParams = blockParams()
            addView(TextView(this@MainActivity).apply {
                text = "Protected data"
                textSize = 18f
                setTextColor(Color.rgb(0, 214, 143))
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
            })
            addView(body("Your voice data, transcripts, and scores are stored privately and used to power your own coaching."))
        })
        column.addView(label("PRIVACY CONTROLS"))
        val improvementToggle = toggleRow("Product improvement", "Allow anonymous use of feedback to improve scoring quality", privacyImprovementEnabled) {
            privacyImprovementEnabled = !privacyImprovementEnabled
            showPrivacySettings()
        }
        column.addView(improvementToggle)
        val retentionToggle = toggleRow("Data retention", "Choose how long practice data stays in your account", privacyRetentionEnabled) {
            privacyRetentionEnabled = !privacyRetentionEnabled
            showPrivacySettings()
        }
        column.addView(retentionToggle)
        column.addView(settingsRow("Export your data", "Download your session history", icon = SettingsIcon.File) {
            showAppToast("Export feature coming soon.", ToastKind.INFO)
        })
        column.addView(spacer(12))
        column.addView(primaryButton("Done") { showSettings() })
        setScreen(scroll(column))
    }

    private fun showLegal() {
        val column = baseColumn()
        column.addView(backButton { showSettings() })
        column.addView(title("Privacy & Terms", 34))
        column.addView(card("Privacy", "Prezzence stores account, session, transcript, score, feedback, notification, and optional resume profile data to provide interview coaching. You can remove your resume profile or delete account data from the app."))
        column.addView(card("Terms", "Scores and AI feedback are coaching signals only. Prezzence does not guarantee job offers, admissions, hiring outcomes, or interview green."))
        column.addView(card("Avatar attribution", "Prezzence uses Duix Mobile avatar technology for on-device interviewer animation. Powered by Duix.com."))
        setScreen(scroll(column))
    }

    private fun showPaused() {
        val column = baseColumn().apply { gravity = Gravity.CENTER }
        column.addView(title("Interview paused", 30))
        column.addView(body("Resume when you are ready, or end this answer and review coaching."))
        column.addView(rowOf(
            primaryButton("Resume") { showInterview(activeTranscriber != null) },
            secondaryButton("Back to home") { showHome() }
        ))
        setScreen(scroll(column))
    }
    private fun pickResumeDocument() {
        if (appState.authToken.isBlank()) {
            showAppToast("Sign in before uploading a CV.", ToastKind.WARNING)
            return
        }
        resumeDocumentPicker.launch(arrayOf(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "text/plain",
            "text/markdown",
            "application/octet-stream",
        ))
    }

    private fun uploadResumeDocument(uri: Uri) {
        val column = baseColumn()
        column.addView(backButton { showResumeProfile() })
        column.addView(title("Uploading CV", 32))
        column.addView(body("Reading your file and building your practice profile..."))
        setScreen(scroll(column))

        scope.launch {
            val fileName = displayNameFor(uri)
            val mimeType = contentResolver.getType(uri)
            val bytes = withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            if (bytes == null || bytes.isEmpty()) {
                showAppToast("Could not read that file.", ToastKind.ERROR)
                showResumeProfile()
                return@launch
            }
            val saved = backend.uploadResumeFile(appState.authToken.ifBlank { null }, fileName, mimeType, bytes)
            showAppToast(
                if (saved != null) "CV profile saved." else "Could not process CV. Try PDF, DOCX, TXT, or MD.",
                if (saved != null) ToastKind.green else ToastKind.ERROR,
            )
            showResumeProfile()
        }
    }

    private fun displayNameFor(uri: Uri): String {
        return runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "resume"
    }
    private fun showResumeProfile() {
        val column = baseColumn()
        column.addView(backButton { showSettings() })
        column.addView(title("Resume / CV", 34))
        column.addView(body("Upload a PDF, DOCX, TXT, or MD resume, or paste resume text. Prezzence uses it to tune interview questions and coaching."))
        val input = EditText(this).apply {
            hint = "Paste resume text here"
            minLines = 8
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            setPadding(dp(18), dp(14), dp(18), dp(14))
            background = rounded(panel)
            layoutParams = blockParams()
        }
        column.addView(input)
        val status = body("Loading saved profile...")
        column.addView(status)
        column.addView(rowOf(
            primaryButton("Upload CV") { pickResumeDocument() },
            secondaryButton("Save text") {
                val resumeText = input.text.toString().trim()
                if (resumeText.length < 40) {
                    showAppToast("Paste more resume detail first.", ToastKind.WARNING)
                    return@secondaryButton
                }
                status.text = "Saving profile..."
                scope.launch {
                    val saved = backend.saveResumeText(appState.authToken.ifBlank { null }, resumeText)
                    status.text = if (saved != null) "Saved: ${saved.fileName}\n${saved.summary.take(180)}" else "Could not save profile. Sign in and try again."
                }
            },
            secondaryButton("Delete") {
                status.text = "Deleting profile..."
                scope.launch {
                    val deleted = backend.deleteResumeProfile(appState.authToken.ifBlank { null })
                    input.setText("")
                    status.text = if (deleted) "Resume profile deleted." else "No profile deleted. Sign in and try again."
                }
            }
        ))
        setScreen(scroll(column))
        scope.launch {
            val profile = backend.getResumeProfile(appState.authToken.ifBlank { null })
            if (profile != null) {
                input.setText(profile.summary)
                status.text = resumeProfileSummary(profile)
            } else {
                status.text = "No saved resume profile yet."
            }
        }
    }

    private fun showNotifications() {
        val column = baseColumn()
        column.addView(backButton { showHome() })
        column.addView(title("Notifications", 34))
        column.addView(body("Inbox updates and reminders."))
        val status = body("Loading notifications...")
        column.addView(status)
        setScreen(scroll(column))
        scope.launch {
            val items = backend.getNotifications(appState.authToken.ifBlank { null })
            column.removeView(status)
            if (items.isEmpty()) {
                column.addView(body("No notifications yet."))
            } else {
                items.forEach { item -> column.addView(notificationRow(item)) }
            }
        }
    }

    private fun resumeProfileSummary(profile: ResumeProfile): String {
        val skills = profile.skills.take(5).joinToString(", ").ifBlank { "No skills extracted yet" }
        return "Saved: ${profile.fileName}\nSkills: $skills\n${profile.summary.take(220)}"
    }

    private fun showProgress() {
        val column = baseColumn()
        val history = if (remoteHistory.isNotEmpty()) remoteHistory else appState.sessionHistory()
        column.addView(backButton { showHome() })
        column.addView(title("Progress", 36))
        column.addView(body(if (history.isEmpty()) {
            "Complete an interview to build your baseline."
        } else {
            "${history.size} completed session${if (history.size == 1) "" else "s"}. Use this page to review and delete old records."
        }))
        column.addView(readinessCard())
        if (history.isEmpty()) {
            column.addView(LinearLayout(this).apply {
                gravity = Gravity.CENTER
                orientation = LinearLayout.VERTICAL
                setPadding(dp(32), dp(32), dp(32), dp(32))
                background = rounded(panel, radius = 28, strokeColor = border)
                layoutParams = blockParams()
                addView(LinearLayout(this@MainActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(58), dp(58))
                    gravity = Gravity.CENTER
                    background = rounded(Color.argb(30, 108, 99, 255), radius = 20)
                    addView(TextView(this@MainActivity).apply {
                        text = "…"
                        textSize = 26f
                        setTextColor(accent)
                        gravity = Gravity.CENTER
                        typeface = Typeface.DEFAULT_BOLD
                    })
                })
                addView(spacer(14))
                addView(TextView(this@MainActivity).apply {
                    text = "No sessions yet"
                    textSize = 17f
                    setTextColor(Color.WHITE)
                    typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    gravity = Gravity.CENTER
                })
                addView(TextView(this@MainActivity).apply {
                    text = "Complete your first interview to see reports and trend history here."
                    textSize = 13f
                    setTextColor(muted)
                    gravity = Gravity.CENTER
                    setPadding(dp(8), dp(8), dp(8), 0)
                })
                addView(spacer(18))
                addView(primaryButton("Start interview") { showRoomSetup() })
            })
        } else {
            column.addView(rowOf(
                secondaryButton("Clear history") {
                    appState.clearHistory()
                    showProgress()
                },
                primaryButton("New session") { showRoomSetup() }
            ))
            history.forEach { session ->
                column.addView(LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(14), dp(14), dp(14), dp(14))
                    background = rounded(panel, radius = 24, strokeColor = border)
                    layoutParams = blockParams()
                    setOnClickListener { showSessionReport(session.id) }
                    addView(LinearLayout(this@MainActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(dp(58), dp(58))
                        gravity = Gravity.CENTER
                        orientation = LinearLayout.VERTICAL
                        background = rounded(Color.argb(35, 108, 99, 255), radius = 18)
                        addView(TextView(this@MainActivity).apply {
                            text = "${session.score}"
                            textSize = 20f
                            setTextColor(Color.WHITE)
                            typeface = Typeface.create("sans-serif", Typeface.BOLD)
                            gravity = Gravity.CENTER
                        })
                        addView(TextView(this@MainActivity).apply {
                            text = "score"
                            textSize = 9f
                            setTextColor(muted)
                            typeface = Typeface.create("sans-serif", Typeface.BOLD)
                            gravity = Gravity.CENTER
                        })
                    })
                    addView(LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(14), 0, dp(8), 0) }
                        addView(TextView(this@MainActivity).apply {
                            text = session.role.ifBlank { "Interview Assessment" }
                            textSize = 16f
                            setTextColor(Color.WHITE)
                            typeface = Typeface.create("sans-serif", Typeface.BOLD)
                            includeFontPadding = false
                            maxLines = 2
                        })
                        addView(TextView(this@MainActivity).apply {
                            text = "${session.answered}/${session.total} answered \u00B7 ${session.date}"
                            textSize = 12f
                            setTextColor(muted)
                            typeface = Typeface.create("sans-serif", Typeface.BOLD)
                            setPadding(0, dp(5), 0, 0)
                        })
                    })
                    addView(LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        addView(ImageView(this@MainActivity).apply {
                            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
                            setPadding(dp(10), dp(10), dp(10), dp(10))
                            background = rounded(Color.argb(30, 255, 71, 87), radius = 20, strokeColor = Color.argb(50, 255, 71, 87))
                            setImageDrawable(object : android.graphics.drawable.Drawable() {
                                override fun draw(c: Canvas) {
                                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 122, 138); style = Paint.Style.STROKE; strokeWidth = 2.5f; strokeCap = Paint.Cap.ROUND }
                                    val w = bounds.width().toFloat(); val h = bounds.height().toFloat(); val cx = w / 2f; val cy = h / 2f
                                    c.drawLine(cx - w*0.18f, cy - h*0.18f, cx + w*0.18f, cy + h*0.18f, p)
                                    c.drawLine(cx + w*0.18f, cy - h*0.18f, cx - w*0.18f, cy + h*0.18f, p)
                                }
                                override fun setAlpha(a: Int) {}
                                override fun setColorFilter(cf: ColorFilter?) {}
                                override fun getOpacity() = PixelFormat.TRANSLUCENT
                            })
                            setOnClickListener {
                                android.app.AlertDialog.Builder(this@MainActivity).apply {
                                    setTitle("Delete session?")
                                    setMessage("Remove \"${session.role}\" from your interview record? This cannot be undone.")
                                    setPositiveButton("Delete") { _, _ ->
                                        scope.launch {
                                            backend.deleteSession(appState.authToken, session.id)
                                            remoteHistory = remoteHistory.filter { it.id != session.id }
                                            showProgress()
                                        }
                                    }
                                    setNegativeButton("Cancel", null)
                                    show()
                                }
                            }
                        })
                        addView(chevronView())
                    })
                })
            }
        }
        setScreen(refreshableScroll(column) {
            scope.launch {
                val fetched = backend.listSessions(appState.authToken)
                if (fetched.isNotEmpty()) {
                    remoteHistory = fetched
                } else {
                    remoteHistory = appState.sessionHistory()
                }
                showProgress()
            }
        })
        if (appState.authToken.isNotBlank() && remoteHistory.isEmpty()) {
            scope.launch {
                val fetched = backend.listSessions(appState.authToken)
                if (fetched.isNotEmpty()) {
                    remoteHistory = fetched
                    showProgress()
                }
            }
        }
    }

    private fun showAllHistory() {
        val column = baseColumn()
        val history = if (remoteHistory.isNotEmpty()) remoteHistory else appState.sessionHistory()
        column.addView(backButton { showHome(PrezzenceTab.PRACTICE) })
        column.addView(TextView(this).apply {
            text = "${history.size} SESSIONS"
            textSize = 11f; setTextColor(accent); typeface = Typeface.create("sans-serif", Typeface.BOLD)
            letterSpacing = 0.06f
        })
        column.addView(title("Every interview", 38))
        column.addView(body("Open any completed or in-progress session report from your account history."))
        column.addView(spacer(8))
        if (history.isEmpty()) {
            column.addView(LinearLayout(this).apply {
                gravity = Gravity.CENTER
                orientation = LinearLayout.VERTICAL
                setPadding(dp(24), dp(24), dp(24), dp(24))
                background = rounded(panel, radius = 28, strokeColor = border)
                layoutParams = blockParams()
                addView(TextView(this@MainActivity).apply {
                    textSize = 26f; setTextColor(accent)
                    val p = Paint(Paint.ANTI_ALIAS_FLAG); p.color = accent; p.style = Paint.Style.STROKE; p.strokeWidth = 2.5f
                    val archiveIcon = object : android.graphics.drawable.Drawable() {
                        override fun draw(c: Canvas) {
                            val w = bounds.width().toFloat(); val h = bounds.height().toFloat()
                            c.drawRoundRect(w*0.18f, h*0.22f, w*0.82f, h*0.82f, 4f, 4f, p)
                            c.drawLine(w*0.10f, h*0.22f, w*0.90f, h*0.22f, p)
                        }
                        override fun setAlpha(a: Int) {}
                        override fun setColorFilter(cf: ColorFilter?) {}
                        override fun getOpacity() = PixelFormat.TRANSLUCENT
                    }
                    archiveIcon.setBounds(0, 0, dp(26), dp(26))
                    setCompoundDrawables(archiveIcon, null, null, null)
                })
                addView(TextView(this@MainActivity).apply {
                    text = "No sessions yet"
                    textSize = 17f; setTextColor(Color.WHITE); typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    setPadding(0, dp(14), 0, 0)
                })
                addView(TextView(this@MainActivity).apply {
                    text = "Complete your first interview to see reports and trend history here."
                    textSize = 13f; setTextColor(muted); gravity = Gravity.CENTER
                    setPadding(dp(8), dp(8), dp(8), 0)
                })
                addView(spacer(18))
                addView(primaryButton("Start interview") { showRoomSetup() })
            })
        } else {
            history.forEach { session ->
                column.addView(LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(14), dp(14), dp(14), dp(14))
                    background = rounded(panel, radius = 24, strokeColor = border)
                    layoutParams = blockParams()
                    setOnClickListener { showSessionReport(session.id) }
                    addView(LinearLayout(this@MainActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(dp(58), dp(58))
                        gravity = Gravity.CENTER
                        orientation = LinearLayout.VERTICAL
                        background = rounded(Color.argb(35, 108, 99, 255), radius = 18)
                        addView(TextView(this@MainActivity).apply {
                            text = "${session.score}"; textSize = 20f; setTextColor(Color.WHITE)
                            typeface = Typeface.create("sans-serif", Typeface.BOLD); gravity = Gravity.CENTER
                        })
                        addView(TextView(this@MainActivity).apply {
                            text = "score"; textSize = 9f; setTextColor(muted)
                            typeface = Typeface.create("sans-serif", Typeface.BOLD); gravity = Gravity.CENTER
                        })
                    })
                    addView(LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(14), 0, dp(8), 0) }
                        addView(TextView(this@MainActivity).apply {
                            text = session.role.ifBlank { "Interview Assessment" }
                            textSize = 16f; setTextColor(Color.WHITE)
                            typeface = Typeface.create("sans-serif", Typeface.BOLD); maxLines = 2
                        })
                        addView(TextView(this@MainActivity).apply {
                            text = "${session.answered}/${session.total} answered \u00B7 ${session.date}"
                            textSize = 12f; setTextColor(muted); typeface = Typeface.create("sans-serif", Typeface.BOLD)
                            setPadding(0, dp(5), 0, 0)
                        })
                    })
                    addView(LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        addView(ImageView(this@MainActivity).apply {
                            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
                            setPadding(dp(10), dp(10), dp(10), dp(10))
                            background = rounded(Color.argb(30, 255, 71, 87), radius = 20, strokeColor = Color.argb(50, 255, 71, 87))
                            setImageDrawable(object : android.graphics.drawable.Drawable() {
                                override fun draw(c: Canvas) {
                                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 122, 138); style = Paint.Style.STROKE; strokeWidth = 2.5f; strokeCap = Paint.Cap.ROUND }
                                    val w = bounds.width().toFloat(); val h = bounds.height().toFloat(); val cx = w / 2f; val cy = h / 2f
                                    c.drawLine(cx - w*0.18f, cy - h*0.18f, cx + w*0.18f, cy + h*0.18f, p)
                                    c.drawLine(cx + w*0.18f, cy - h*0.18f, cx - w*0.18f, cy + h*0.18f, p)
                                }
                                override fun setAlpha(a: Int) {}
                                override fun setColorFilter(cf: ColorFilter?) {}
                                override fun getOpacity() = PixelFormat.TRANSLUCENT
                            })
                            setOnClickListener {
                                android.app.AlertDialog.Builder(this@MainActivity).apply {
                                    setTitle("Delete session?")
                                    setMessage("Remove \"${session.role}\" from your interview record? This cannot be undone.")
                                    setPositiveButton("Delete") { _, _ ->
                                        scope.launch {
                                            backend.deleteSession(appState.authToken, session.id)
                                            remoteHistory = remoteHistory.filter { it.id != session.id }
                                            showAllHistory()
                                        }
                                    }
                                    setNegativeButton("Cancel", null)
                                    show()
                                }
                            }
                        })
                        addView(chevronView())
                    })
                })
                column.addView(spacer(8))
            }
        }
        setScreen(refreshableScroll(column) {
            scope.launch {
                val fetched = backend.listSessions(appState.authToken)
                if (fetched.isNotEmpty()) {
                    remoteHistory = fetched
                } else {
                    remoteHistory = appState.sessionHistory()
                }
                showAllHistory()
            }
        })
    }

    private fun showCoachingFeedback(question: String, answer: AnswerResult, onContinue: () -> Unit) {
        val scoreColor = when {
            answer.score >= 75 -> green
            answer.score >= 55 -> warning
            else -> danger
        }
        setScreen(scroll(baseColumn().apply {
            addView(backButton { onContinue() })
            addView(title("Answer Result", 34))
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(0, dp(16), 0, dp(16))
                addView(TextView(this@MainActivity).apply {
                    text = "${answer.score}"
                    textSize = 56f; setTextColor(scoreColor); typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    gravity = Gravity.CENTER
                })
                addView(TextView(this@MainActivity).apply {
                    text = "out of 100"
                    textSize = 14f; setTextColor(muted); gravity = Gravity.CENTER
                })
            })
            addView(card("Question", question))
            addView(spacer(8))
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(20), dp(20), dp(20), dp(20))
                background = rounded(panel, radius = 26, strokeColor = border)
                addView(TextView(this@MainActivity).apply {
                    text = "Feedback"
                    textSize = 18f; setTextColor(Color.WHITE); typeface = Typeface.create("sans-serif", Typeface.BOLD)
                })
                addView(TextView(this@MainActivity).apply {
                    text = answer.feedback; textSize = 15f; setTextColor(muted); setPadding(0, dp(12), 0, 0)
                })
                addView(spacer(16))
                addView(TextView(this@MainActivity).apply {
                    text = "Improved answer"
                    textSize = 16f; setTextColor(Color.WHITE); typeface = Typeface.create("sans-serif", Typeface.BOLD)
                })
                addView(TextView(this@MainActivity).apply {
                    text = answer.improvedAnswer; textSize = 15f; setTextColor(muted); setPadding(0, dp(8), 0, 0)
                })
                addView(spacer(16))
                listOf("What" to answer.what, "How" to answer.how, "Why" to answer.why).forEach { (label, value) ->
                    addView(TextView(this@MainActivity).apply {
                        text = "$label: $value"
                        textSize = 14f; setTextColor(Color.WHITE)
                        typeface = Typeface.create("sans-serif", Typeface.BOLD)
                        setPadding(0, dp(6), 0, 0)
                    })
                }
            })
            addView(spacer(24))
            addView(primaryButton("Continue") { onContinue() })
        }))
    }

    private fun showRoomSetup() {
        showEnteringRoom()
    }

    private fun showEnteringRoom(preparing: Boolean = true, setupStatus: String = "Preparing your questions and interview room.") {
        val interviewers = if (appState.interviewMode == InterviewMode.PANEL) {
            PrezzenceDefaults.panelInterviewersForStyle(appState.interviewerStyle)
        } else {
            listOf(appState.interviewerFor())
        }
        
        // Don't wait for models - they load on-demand during interview
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceEnteringRoomScreen(
                    isPanel = appState.interviewMode == InterviewMode.PANEL,
                    interviewers = interviewers.map { it.name to it.title },
                    preparing = preparing,
                    setupStatus = setupStatus,
                    onBack = { showHome() },
                    onJoin = { beginInterviewFromEntering() },
                )
            }
        })
        
        // Start backend session preparation if needed
        if (preparing) {
            prepareBackendSessionForEntering()
        }
    }

    private fun beginInterviewFromEntering() {
        oauthCodeVerifier = ""
        activeTranscript = ""
        speechError = ""
        currentAnswerResult = null
        sessionAnswers.clear()
        activeTranscriber = null
        startAnswerAfterPermission = false
        continueToRoomAfterMicPermission = false
        appState.currentQuestionIndex = 0
        cachedQuestionSpeechIndex = -1
        cachedQuestionSpeechSource = ""
        questionSpeechCache.clear()
        openingIntroductionSpoken = false
        suppressNativeAvatarForEntry = false
        
        // Go directly to interview - models will load on-demand if not cached
        showInterview(false)
        scope.launch { prepareCurrentQuestionSpeech() }
    }

    /** Shows network error screen when a backend call fails during critical flows. */
    private fun withNetworkGuard(result: Any?, onSuccess: () -> Unit) {
        if (result == null || result == false || (result is String && result.isBlank())) {
            showNetworkError(returnToHome = true)
        } else {
            onSuccess()
        }
    }

    private fun prepareBackendSessionForEntering() {
        if (appState.activeSessionId.isNotBlank() || appState.authToken.isBlank()) {
            showEnteringRoom(preparing = false, setupStatus = "Interview room prepared with the standard setup.")
            return
        }
        scope.launch {
            try {
                val remote = tryCreateSession()
                appState.activeSessionId = remote.sessionId.orEmpty()
                if (!remote.questions.isNullOrEmpty()) {
                    appState.setGeneratedQuestions(remote.questions)
                }
                showEnteringRoom(
                    preparing = false,
                    setupStatus = "Camera and audio staged.",
                )
            } catch (e: SessionCreateException) {
                if (e.reason == SessionErrorReason.AUTH_FAILED && appState.authRefreshToken.isNotBlank()) {
                    val refreshed = backend.refreshSession(appState.authRefreshToken)
                    if (refreshed != null) {
                        appState.authToken = refreshed.accessToken
                        appState.authRefreshToken = refreshed.refreshToken
                        try {
                            val remote = tryCreateSession()
                            appState.activeSessionId = remote.sessionId.orEmpty()
                            if (!remote.questions.isNullOrEmpty()) {
                                appState.setGeneratedQuestions(remote.questions)
                            }
                            showEnteringRoom(preparing = false, setupStatus = "Camera and audio staged.")
                            return@launch
                        } catch (_: SessionCreateException) { }
                    }
                }
                showSessionCreateError(e.reason)
            }
        }
    }

    private suspend fun tryCreateSession(): com.pollecode.prezzencekotlin.data.BackendSession {
        return backend.createSession(
            bearerToken = appState.authToken,
            role = appState.selectedRole,
            mode = appState.interviewMode,
            language = appState.language,
            interviewTrack = onboardingTrack,
            industry = onboardingIndustry,
            seniority = onboardingSeniority,
            difficulty = onboardingDifficulty,
            companyName = onboardingCompanyName,
            companyWebsite = onboardingCompanyWebsite,
            companyContext = onboardingCompanyContext,
            enableWebResearch = onboardingEnableWebResearch,
            includeTechnical = onboardingIncludeTechnical || onboardingTrack.equals("technical", ignoreCase = true),
            interviewerStyle = onboardingInterviewerStyle,
            previewGender = onboardingPreviewGender,
        )
    }

    private fun showLegacyRoomSetup() {
        val interviewer = appState.interviewerFor()
        val column = baseColumn()
        column.addView(backButton { showHome() })
        column.addView(label("LIVE INTERVIEW"))
        column.addView(title(if (appState.interviewMode == InterviewMode.PANEL) "Your interviewers are ready." else "Your interviewer is ready.", 30))
        column.addView(body("Preparing your questions and interview room."))
        if (appState.interviewMode == InterviewMode.PANEL) {
            column.addView(panelPreview())
        } else {
            column.addView(interviewerReadyCard(interviewer))
        }
        column.addView(primaryButton("Begin Interview") { prepareBackendSessionThenStart() })
        setScreen(scroll(column))
    }

    private fun prepareBackendSessionThenStart() {
        if (appState.activeSessionId.isNotBlank() || appState.authToken.isBlank()) {
            showInterview(false)
            return
        }
        val column = baseColumn().apply { gravity = Gravity.CENTER }
        column.addView(title("Preparing interview", 28))
        column.addView(body("Setting up your questions."))
        setScreen(column)
        scope.launch {
            try {
                val remote = tryCreateSession()
                appState.activeSessionId = remote.sessionId.orEmpty()
                if (!remote.questions.isNullOrEmpty()) {
                    appState.setGeneratedQuestions(remote.questions)
                }
                showInterview(false)
            } catch (e: SessionCreateException) {
                if (e.reason == SessionErrorReason.AUTH_FAILED && appState.authRefreshToken.isNotBlank()) {
                    val refreshed = backend.refreshSession(appState.authRefreshToken)
                    if (refreshed != null) {
                        appState.authToken = refreshed.accessToken
                        appState.authRefreshToken = refreshed.refreshToken
                        try {
                            val remote = tryCreateSession()
                            appState.activeSessionId = remote.sessionId.orEmpty()
                            if (!remote.questions.isNullOrEmpty()) {
                                appState.setGeneratedQuestions(remote.questions)
                            }
                            showInterview(false)
                            return@launch
                        } catch (_: SessionCreateException) { }
                    }
                }
                showSessionCreateError(e.reason)
            }
        }
    }

    private fun showInterview(answering: Boolean) {
        val question = appState.currentQuestion()
        val interviewer = appState.interviewerFor(question)
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceInterviewRoomScreen(
                    currentStep = appState.currentQuestionIndex + 1,
                    totalSteps = appState.questions().size,
                    interviewerName = interviewer.name,
                    interviewerTitle = interviewer.title,
                    isPanel = appState.interviewMode == InterviewMode.PANEL,
                    panelInterviewers = if (appState.interviewMode == InterviewMode.PANEL) {
                        PrezzenceDefaults.panelInterviewersForStyle(appState.interviewerStyle)
                            .map { it.name to it.title }
                    } else {
                        emptyList()
                    },
                    questionText = question.text,
                    answering = answering,
                    transcript = activeTranscript,
                    error = speechError,
                    cameraCoachEnabled = appState.cameraCoachEnabled,
                    createAvatarView = {
                        if (suppressNativeAvatarForEntry) {
                            suppressNativeAvatarForEntry = false
                            interviewerReadyCard(interviewer)
                        } else {
                            runCatching { duixAvatarCard(interviewer, "speaking", true) }
                                .getOrElse { interviewerReadyCard(interviewer) }
                        }
                    },
                    createCameraView = { cameraCoachCard(interviewer) },
                    onExit = { showHome() },
                    onPause = { pauseInterview() },
                    onRepeat = { replayCurrentQuestion() },
                    onClarify = { clarifyCurrentQuestion() },
                    onAnswerNow = { ensurePermissionsThenAnswer() },
                    onFinish = { finishAnswer(question.text) },
                    coachingMessage = coachingMessage,
                )
            }
        })
        if (answering) {
            startSpeechCapture()
            scope.launch { prepareQuestionSpeech(appState.currentQuestionIndex + 1) }
        }
    }

    private fun pauseInterview() {
        activeAvatar?.stopSpeaking()
        val transcriber = activeTranscriber
        activeTranscriber = null
        if (transcriber != null) {
            scope.launch(Dispatchers.IO) {
                transcriber.stop(appState.language)
            }
        }
        showPaused()
    }

    private fun replayCurrentQuestion() {
        val avatar = activeAvatar ?: return
        val question = appState.currentQuestion()
        val interviewer = appState.interviewerFor(question)
        val token = speechGenerationToken
        avatar.stopSpeaking()
        speakQuestionThroughAvatar(
            avatar = avatar,
            questionText = question.text,
            interviewer = interviewer,
            token = token,
            forceRefresh = false,
        )
    }

    private enum class ToastKind { INFO, green, WARNING, ERROR }

    private fun showAppToast(message: String, kind: ToastKind = ToastKind.INFO, durationMs: Long = 2600L) {
        if (!::root.isInitialized || message.isBlank()) return
        appToastView?.let { root.removeView(it) }
        val accentColor = when (kind) {
            ToastKind.green -> green
            ToastKind.WARNING -> warning
            ToastKind.ERROR -> danger
            ToastKind.INFO -> accent
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.rgb(18, 18, 26), radius = 14, strokeColor = Color.argb(56, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)))
            elevation = dp(18).toFloat()
            alpha = 0f
            addView(TextView(this@MainActivity).apply {
                text = if (kind == ToastKind.green) "OK" else "!"
                gravity = Gravity.CENTER
                setTextColor(accentColor)
                textSize = 16f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                background = rounded(Color.argb(24, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)), radius = 999, strokeColor = Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).apply { setMargins(0, 0, dp(10), 0) }
            })
            addView(TextView(this@MainActivity).apply {
                text = message
                setTextColor(Color.WHITE)
                textSize = 14f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                setLineSpacing(dp(2).toFloat(), 1.0f)
                maxLines = 3
            })
        }
        val params = FrameLayout.LayoutParams(
            (resources.displayMetrics.widthPixels - dp(44)).coerceAtMost(dp(420)),
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER,
        )
        root.addView(card, params)
        appToastView = card
        card.animate().alpha(1f).translationY(0f).setDuration(160).start()
        root.postDelayed({
            if (appToastView === card) {
                card.animate().alpha(0f).setDuration(180).withEndAction {
                    if (appToastView === card) appToastView = null
                    runCatching { root.removeView(card) }
                }.start()
            }
        }, durationMs)
    }

    private fun clarifyCurrentQuestion() {
        val avatar = activeAvatar
        if (avatar == null) {
            showAppToast("Clarify is ready once the interviewer is visible.", ToastKind.INFO)
            return
        }
        val question = appState.currentQuestion()
        val interviewer = appState.interviewerFor(question)
        val token = speechGenerationToken
        val clarification = "Let me rephrase that: ${clarifiedQuestionText(question.text)}"
        avatar.stopSpeaking()
        scope.launch {
            val backendSpeech = backend.synthesizeSpeechUrl(
                bearerToken = appState.authToken.ifBlank { null },
                text = clarification,
                language = appState.language,
                personality = interviewer.id,
            )
            if (!backendSpeech.isNullOrBlank()) {
                root.post {
                    if (token == speechGenerationToken && activeAvatar === avatar) {
                        avatar.speakAudioUri(backendSpeech, "clarify-${appState.currentQuestionIndex + 1}")
                    }
                }
                return@launch
            }
            showAppToast("Voice could not start. Try Clarify again.", ToastKind.WARNING)
        }
    }

    private fun clarifiedQuestionText(questionText: String): String {
        val cleaned = questionText
            .replace(Regex("\\s+"), " ")
            .trim()
            .trimEnd('.', '?', '!')
        if (cleaned.isBlank()) return "Could you answer the current question in your own words?"
        val lower = cleaned.lowercase(Locale.US)
        return when {
            lower.startsWith("please introduce yourself") ->
                "Could you briefly introduce yourself and summarize your background?"
            lower.startsWith("tell me about a time you") ->
                "Can you share a specific time when you ${removePrefixIgnoreCase(cleaned, "Tell me about a time you")}?"
            lower.startsWith("describe a time you") ->
                "Can you describe a specific time when you ${removePrefixIgnoreCase(cleaned, "Describe a time you")}?"
            lower.startsWith("describe how you") ->
                "Could you explain how you ${removePrefixIgnoreCase(cleaned, "Describe how you")}?"
            lower.startsWith("how would you") ->
                "What would you do to ${removePrefixIgnoreCase(cleaned, "How would you")}?"
            lower.startsWith("how do you") ->
                "Can you explain how you ${removePrefixIgnoreCase(cleaned, "How do you")}?"
            else -> "$cleaned?"
        }
    }

    private fun removePrefixIgnoreCase(text: String, prefix: String): String {
        return if (text.regionMatches(0, prefix, 0, prefix.length, ignoreCase = true)) {
            text.drop(prefix.length).trim().replaceFirstChar { it.lowercase(Locale.US) }
        } else {
            text
        }
    }

    private fun speakQuestionThroughAvatar(
        avatar: NativeDuixAvatarView,
        questionText: String,
        interviewer: Interviewer,
        token: Int,
        forceRefresh: Boolean,
    ) {
        val questionIndex = appState.currentQuestionIndex
        val source = "question-${questionIndex + 1}"
        val cached = if (forceRefresh) null else questionSpeechCache[questionIndex]
        if (cached != null) {
            if (questionIndex == 0) openingIntroductionSpoken = true
            if (token == speechGenerationToken && activeAvatar === avatar) {
                avatar.speakAudioUri(cached, source)
            }
            return
        }
        scope.launch {
            val speechText = openingQuestionSpeechText(questionText, interviewer)
            val backendSpeech = backend.synthesizeSpeechUrl(
                bearerToken = appState.authToken.ifBlank { null },
                text = speechText,
                language = appState.language,
                personality = interviewer.id,
            )
            if (!backendSpeech.isNullOrBlank()) {
                questionSpeechCache[questionIndex] = backendSpeech
                cachedQuestionSpeechIndex = questionIndex
                cachedQuestionSpeechSource = backendSpeech
                if (questionIndex == 0) openingIntroductionSpoken = true
                root.post {
                    if (token == speechGenerationToken && activeAvatar === avatar) {
                        avatar.speakAudioUri(backendSpeech, source)
                    }
                }
                return@launch
            }
            showAppToast("Voice could not start. Tap Repeat to try again.", ToastKind.WARNING)
        }
    }

    private suspend fun prepareCurrentQuestionSpeech(forceRefresh: Boolean = false): Boolean {
        return prepareQuestionSpeech(appState.currentQuestionIndex, forceRefresh)
    }

    private suspend fun prepareQuestionSpeech(questionIndex: Int, forceRefresh: Boolean = false): Boolean {
        if (questionIndex !in appState.questions().indices) return false
        if (!forceRefresh && questionSpeechCache[questionIndex]?.isNotBlank() == true) {
            return true
        }
        val question = appState.questions()[questionIndex]
        val interviewer = appState.interviewerFor(question)
        val speechText = questionSpeechText(questionIndex, question.text, interviewer)
        val backendSpeech = backend.synthesizeSpeechUrl(
            bearerToken = appState.authToken.ifBlank { null },
            text = speechText,
            language = appState.language,
            personality = interviewer.id,
        )
        if (backendSpeech.isNullOrBlank()) return false
        questionSpeechCache[questionIndex] = backendSpeech
        cachedQuestionSpeechIndex = questionIndex
        cachedQuestionSpeechSource = backendSpeech
        return true
    }

    private fun openingQuestionSpeechText(questionText: String, interviewer: Interviewer): String {
        return questionSpeechText(appState.currentQuestionIndex, questionText, interviewer)
    }

    private fun questionSpeechText(questionIndex: Int, questionText: String, interviewer: Interviewer): String {
        if (questionIndex != 0 || openingIntroductionSpoken) return questionText
        val lower = questionText.lowercase(Locale.US)
        if (
            lower.startsWith("hi, i'm") ||
            lower.startsWith("hi, i am") ||
            lower.startsWith("hello, i'm") ||
            lower.startsWith("hello, i am") ||
            lower.startsWith("hi, we're") ||
            lower.startsWith("hi, we are") ||
            lower.startsWith("welcome")
        ) {
            return questionText
        }
        return if (appState.interviewMode == InterviewMode.PANEL) {
            val panel = PrezzenceDefaults.panelInterviewersForStyle(appState.interviewerStyle)
                .joinToString("; ") { "${it.name}, ${it.title}" }
            "Welcome to Prezzence. Your interview panel today is $panel. I'm ${interviewer.name}, and I'll start. $questionText"
        } else {
            "Welcome to Prezzence. I'm ${interviewer.name}, your ${interviewer.title}. I'll guide this interview. $questionText"
        }
    }

    private fun ensurePermissionsThenAnswer() {
        val required = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (appState.cameraCoachEnabled) required.add(Manifest.permission.CAMERA)
        val needed = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            startAnswerAfterPermission = true
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 100)
            return
        }
        showInterview(true)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && continueToRoomAfterMicPermission) {
            continueToRoomAfterMicPermission = false
            onboardingMicLoading = false
            val micGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
            if (micGranted) {
                appState.onboardingComplete = true
                appState.activeSessionId = ""
                showEnteringRoom()
            } else {
                showMicDenied()
            }
            return
        }
        if (requestCode != 100 || !startAnswerAfterPermission) return
        startAnswerAfterPermission = false

        val micGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        val cameraGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED

        if (!micGranted) {
            showMicDenied()
            return
        }
        if (appState.cameraCoachEnabled && !cameraGranted) {
            showAppToast("Answering without camera coach until camera access is allowed.", ToastKind.WARNING)
        }
        showInterview(true)
    }

    private fun finishAnswer(questionText: String) {
        val transcriber = activeTranscriber
        activeTranscriber = null
        currentAnswerResult = null
        val column = baseColumn().apply { gravity = Gravity.CENTER }
        column.addView(title("Transcribing", 28))
        column.addView(body("Transcribing"))
        setScreen(column)

        scope.launch {
            val capturedTranscript = withContext(Dispatchers.IO) {
                transcriber?.stop(appState.language).orEmpty()
            }
            activeTranscript = capturedTranscript.ifBlank { activeTranscript }
            val transcript = activeTranscript.ifBlank { speechError }.ifBlank { "No clear speech was captured." }
            val localResult = backend.scoreLocalTranscript(questionText, transcript)
            val remoteResult = backend.scoreWithBackend(
                bearerToken = appState.authToken.ifBlank { null },
                sessionId = appState.activeSessionId,
                questionId = appState.currentQuestionIndex + 1,
                questionText = questionText,
                transcript = transcript,
            )
            val result = if (localResult.score <= 15 && (remoteResult?.score ?: 0) > 20) {
                localResult.copy(feedback = "This answer did not clearly address the question. Try again with one relevant example, your action, and the result.")
            } else if (remoteResult != null) {
                // Use backend result, but fallback to local coaching if backend didn't provide one
                remoteResult.copy(
                    coachingMessage = remoteResult.coachingMessage.ifBlank { localResult.coachingMessage }
                )
            } else {
                localResult
            }
            // If both local and remote scoring failed, show timeout / save error
            if (result.score <= 5 && result.transcript.isBlank()) {
                showProcessingTimeout()
                return@launch
            }
            appState.markAnswered(result.score, result.transcript)
            currentAnswerResult = result
            sessionAnswers.add(result)
            activeTranscript = ""
            speechError = ""
            showResult()
        }
    }

    private fun startSpeechCapture() {
        // Check storage before recording
        val storageStats = runCatching {
            val stats = android.os.StatFs(android.os.Environment.getDataDirectory().path)
            val freeBytes = stats.availableBytes
            val freeMb = freeBytes / (1024 * 1024)
            freeMb
        }.getOrDefault(100L)
        if (storageStats < 100) {
            showLowStorage()
            return
        }
        if (activeTranscriber != null) return
        activeTranscriber = NativeSpeechTranscriber(
            context = this,
            onPartial = { text ->
                activeTranscript = text
            },
            onFinal = { text ->
                activeTranscript = text
            },
            onError = { message ->
                speechError = message
            },
        ).also { it.start(appState.language) }
    }

    private fun showResult() {
        val result = currentAnswerResult ?: AnswerResult(
            transcript = appState.lastTranscript,
            score = appState.lastScore,
            feedback = "Answer coaching is ready.",
            improvedAnswer = "Use one specific example, your action, and the result.",
            what = "Situation, action, and result.",
            how = "Keep it short and concrete.",
            why = "Specific proof makes the answer easier to trust.",
            coachingMessage = "Let's review how you did and find ways to make your answer even stronger.",
        )
        
        // Update coaching message for display
        coachingMessage = result.coachingMessage
        
        val column = baseColumn()
        column.addView(title("Answer result", 30))
        column.addView(scoreCard(result))
        column.addView(card("Your answer", result.transcript.ifBlank { "No transcript captured." }))
        column.addView(card("Stronger answer", result.improvedAnswer))
        column.addView(coachingCard(result))
        column.addView(rowOf(
            secondaryButton("Retry question") { showInterview(false) },
            primaryButton("Continue") {
                val sessionId = appState.activeSessionId.ifBlank { "session-${System.currentTimeMillis()}" }
                appState.saveSessionAnswers(sessionId, sessionAnswers.toList())
                sessionAnswers.clear()
                val done = appState.advanceOrComplete()
                if (done) {
                    if (appState.authToken.isNotBlank() && appState.activeSessionId.isNotBlank()) {
                        showHome()
                    } else {
                        showSessionSaveError()
                    }
                } else {
                    val column = baseColumn().apply { gravity = Gravity.CENTER }
                    column.addView(title("Preparing next question", 28))
                    column.addView(body("Loading the interviewer voice before the room opens."))
                    setScreen(column)
                    scope.launch {
                        prepareCurrentQuestionSpeech()
                        showInterview(false)
                    }
                }
            }
        ))
        setScreen(scroll(column))
    }

    private fun readinessCard(): View {
        val score = appState.readinessScore
        val label = when {
            score >= 75 -> "Interview Ready"
            score >= 45 -> "Building"
            score > 0 -> "Needs Practice"
            else -> "No Baseline"
        }
        return card("Readiness", "$label\n$score%\nComplete realistic sessions to improve your baseline.")
    }

    private fun scoreCard(result: AnswerResult) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(20), dp(20), dp(20), dp(20))
        background = rounded(panel)
        layoutParams = blockParams()
        addView(TextView(this@MainActivity).apply {
            text = result.score.toString()
            textSize = 46f
            setTextColor(accent)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER
            background = rounded(surface)
            layoutParams = LinearLayout.LayoutParams(dp(100), dp(100)).apply { setMargins(0, 0, dp(18), 0) }
        })
        addView(body(result.feedback).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
    }

    private fun coachingCard(result: AnswerResult) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(22), dp(20), dp(22), dp(20))
        background = rounded(Color.rgb(8, 47, 45))
        layoutParams = blockParams()
        addView(label("ANSWER COACHING"))
        addView(metricText("WHAT", result.what))
        addView(metricText("HOW", result.how))
        addView(metricText("WHY", result.why))
    }

    private fun roleScroller() = HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        val row = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL }
        PrezzenceDefaults.roles.forEach { role ->
            row.addView(secondaryButton(role) {
                appState.selectedRole = role
                showQuestions()
            }.apply {
                minWidth = dp(190)
                if (role == appState.selectedRole) setBackgroundColor(accent)
            })
        }
        addView(row)
    }

    private fun interviewerReadyCard(interviewer: Interviewer) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(22), dp(28), dp(22), dp(28))
        background = rounded(Color.rgb(24, 23, 39))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(280)).apply {
            setMargins(0, dp(10), 0, dp(10))
        }
        addView(pill("LIVE INTERVIEW", green))
        addView(title(interviewer.name, 30).apply { gravity = Gravity.CENTER })
        addView(body(interviewer.title).apply { gravity = Gravity.CENTER })
        addView(body("Camera and audio staged").apply { gravity = Gravity.CENTER })
    }

    private fun panelPreview() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        layoutParams = blockParams()
        PrezzenceDefaults.interviewers.forEach { interviewer ->
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(10), dp(14), dp(10), dp(14))
                background = rounded(Color.rgb(25, 24, 40))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dp(4), 0, dp(4), 0)
                }
                addView(title(interviewer.name, 16))
                addView(body(interviewer.title).apply { gravity = Gravity.CENTER })
            })
        }
    }

    private fun compactQuestionHeader(interviewer: Interviewer) = card("Asked by ${interviewer.name}", appState.currentQuestion().text)

    private fun duixAvatarCard(interviewer: Interviewer, state: String, live: Boolean) = FrameLayout(this).apply {
        background = rounded(Color.rgb(24, 23, 39))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(if (live) 330 else 300)).apply {
            setMargins(0, dp(10), 0, dp(10))
        }
        val questionText = appState.currentQuestion().text
        val token = speechGenerationToken
        var speechQueued = false
        val avatar = NativeDuixAvatarView(this@MainActivity).apply avatarView@{
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            listener = object : NativeDuixAvatarView.Listener {
                override fun onModelReady(modelName: String) {
                    if (!live || speechQueued || token != speechGenerationToken) return
                    speechQueued = true
                    speakQuestionThroughAvatar(this@avatarView, questionText, interviewer, token, forceRefresh = false)
                }
                override fun onModelError(modelName: String, message: String?) {
                    showAppToast("Avatar failed to load: ${message ?: "tap Repeat"}", ToastKind.WARNING)
                }
                override fun onSpeechError(source: String?, modelName: String?, message: String?) {
                    showAppToast("Voice failed: ${message ?: "tap Repeat"}", ToastKind.WARNING)
                }
            }
            setModelName(interviewer.modelName)
        }
        activeAvatar = avatar
        addView(avatar)
        addView(pill(if (live) "LIVE" else state.uppercase(), if (live) green else accent).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.TOP or Gravity.START
                setMargins(dp(16), dp(16), 0, 0)
            }
        })
    }

    private fun cameraCoachCard(interviewer: Interviewer) = FrameLayout(this).apply {
        background = rounded(Color.BLACK)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(310)).apply {
            setMargins(0, dp(10), 0, dp(10))
        }
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCameraPermission) {
            val camera = NativePresenceCameraView(this@MainActivity).apply {
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }
            activeCamera = camera
            addView(camera)
        } else {
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(22), dp(22), dp(22), dp(22))
                addView(title("Camera access needed", 22).apply { gravity = Gravity.CENTER })
                addView(body("Allow camera access to capture presence coaching during your answer.").apply { gravity = Gravity.CENTER })
                addView(primaryButton("Allow camera") {
                    startAnswerAfterPermission = true
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA), 100)
                })
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            })
        }
        addView(TextView(this@MainActivity).apply {
            text = "ASKED BY\n${interviewer.name}"
            textSize = 13f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(dp(18), dp(18), dp(18), dp(18))
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                setMargins(dp(14), 0, 0, dp(48))
            }
        })
    }

    private fun transcriptPreview() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(14), dp(18), dp(14))
        background = rounded(inputBg)
        layoutParams = blockParams()
        addView(label("TRANSCRIPT"))
        addView(body(activeTranscript.ifBlank { speechError }))
    }

    private fun timerCard() = TextView(this).apply {
        text = "00:08     ||||||"
        textSize = 22f
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        setPadding(dp(18), dp(12), dp(18), dp(12))
        background = rounded(Color.rgb(27, 26, 35))
        layoutParams = blockParams()
    }

    enum class SettingsIcon { Camera, Sound, Globe, User, Card, Shield, Bell, File, Notif, Feedback, Sub, QA, A11y, Eye, Legal, LogOut, Chevron, Settings, Help }

    private fun settingsRow(title: String, value: String, icon: SettingsIcon = SettingsIcon.Chevron, onClick: () -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = rounded(panel)
        layoutParams = blockParams()
        // Leading icon
        addView(settingsIconView(icon))
        // Title + subtitle column
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(12), 0, dp(8), 0) }
            addView(TextView(this@MainActivity).apply {
                text = title
                textSize = 16f
                setTextColor(Color.WHITE)
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                includeFontPadding = false
            })
            addView(TextView(this@MainActivity).apply {
                text = value
                textSize = 13f
                setTextColor(muted)
                includeFontPadding = false
                setPadding(0, dp(3), 0, 0)
                maxLines = 2
            })
        })
        // Trailing chevron
        addView(chevronView())
        setOnClickListener { onClick() }
    }

    private fun settingsIconView(icon: SettingsIcon) = ImageView(this@MainActivity).apply {
        layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply { setMargins(0, 0, 0, 0) }
        setPadding(dp(10), dp(10), dp(10), dp(10))
        background = rounded(surface, radius = 12, strokeColor = border)
        setImageDrawable(object : android.graphics.drawable.Drawable() {
            override fun draw(canvas: Canvas) {
                val w = bounds.width().toFloat()
                val h = bounds.height().toFloat()
                val cx = w / 2f
                val cy = h / 2f
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accent; style = Paint.Style.STROKE; strokeWidth = 2.5f
                    strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
                }
                val pf = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accent; style = Paint.Style.FILL
                }
                when (icon) {
                    SettingsIcon.Camera -> {
                        // Camera body
                        canvas.drawRoundRect(cx - w*0.30f, cy - h*0.20f, cx + w*0.30f, cy + h*0.30f, 6f, 6f, p)
                        // Lens
                        canvas.drawCircle(cx, cy + h*0.05f, w*0.14f, p)
                        // Flash
                        canvas.drawPoint(cx + w*0.22f, cy - h*0.12f, pf)
                        // Top mount
                        canvas.drawLine(cx - w*0.08f, cy - h*0.20f, cx + w*0.08f, cy - h*0.20f, p)
                        canvas.drawLine(cx - w*0.04f, cy - h*0.20f, cx - w*0.04f, cy - h*0.28f, p)
                        canvas.drawLine(cx + w*0.04f, cy - h*0.20f, cx + w*0.04f, cy - h*0.28f, p)
                        canvas.drawLine(cx - w*0.04f, cy - h*0.28f, cx + w*0.04f, cy - h*0.28f, p)
                    }
                    SettingsIcon.Sound -> {
                        // Speaker
                        canvas.drawRect(cx - w*0.28f, cy - h*0.15f, cx - w*0.10f, cy + h*0.15f, pf)
                        // Cone
                        val path = android.graphics.Path().apply {
                            moveTo(cx - w*0.10f, cy - h*0.28f); lineTo(cx + w*0.22f, cy - h*0.15f)
                            lineTo(cx + w*0.22f, cy + h*0.15f); lineTo(cx - w*0.10f, cy + h*0.28f); close()
                        }
                        canvas.drawPath(path, pf)
                        // Sound waves
                        p.strokeWidth = 2f
                        canvas.drawArc(cx + w*0.10f, cy - h*0.35f, cx + w*0.38f, cy + h*0.02f, -60f, 120f, false, p)
                        canvas.drawArc(cx + w*0.10f, cy - h*0.02f, cx + w*0.38f, cy + h*0.35f, -60f, 120f, false, p)
                    }
                    SettingsIcon.Globe -> {
                        // Globe circle
                        canvas.drawCircle(cx, cy, w*0.32f, p)
                        // Horizontal lines
                        canvas.drawLine(cx - w*0.32f, cy, cx + w*0.32f, cy, p)
                        canvas.drawLine(cx - w*0.28f, cy - h*0.14f, cx + w*0.28f, cy - h*0.14f, p)
                        canvas.drawLine(cx - w*0.28f, cy + h*0.14f, cx + w*0.28f, cy + h*0.14f, p)
                        // Vertical arc
                        canvas.drawArc(cx - w*0.06f, cy - h*0.32f, cx + w*0.06f, cy + h*0.32f, 0f, 360f, false, p)
                    }
                    SettingsIcon.User -> {
                        // Head
                        canvas.drawCircle(cx, cy - h*0.10f, w*0.18f, p)
                        // Body
                        val bodyPath = android.graphics.Path().apply {
                            moveTo(cx - w*0.24f, cy + h*0.40f)
                            cubicTo(cx - w*0.20f, cy + h*0.14f, cx + w*0.20f, cy + h*0.14f, cx + w*0.24f, cy + h*0.40f)
                            close()
                        }
                        canvas.drawPath(bodyPath, p)
                    }
                    SettingsIcon.Card -> {
                        // Card outline
                        canvas.drawRoundRect(cx - w*0.34f, cy - h*0.26f, cx + w*0.34f, cy + h*0.26f, 6f, 6f, p)
                        // Chip lines
                        canvas.drawLine(cx - w*0.20f, cy - h*0.10f, cx + w*0.06f, cy - h*0.10f, p)
                        canvas.drawLine(cx - w*0.20f, cy + h*0.02f, cx + w*0.20f, cy + h*0.02f, p)
                        canvas.drawLine(cx - w*0.20f, cy + h*0.14f, cx + w*0.14f, cy + h*0.14f, p)
                    }
                    SettingsIcon.Shield -> {
                        // Shield outline
                        val shieldPath = android.graphics.Path().apply {
                            moveTo(cx, cy - h*0.34f)
                            lineTo(cx + w*0.30f, cy - h*0.22f)
                            lineTo(cx + w*0.30f, cy + h*0.08f)
                            cubicTo(cx + w*0.30f, cy + h*0.26f, cx, cy + h*0.36f, cx, cy + h*0.36f)
                            cubicTo(cx, cy + h*0.36f, cx - w*0.30f, cy + h*0.26f, cx - w*0.30f, cy + h*0.08f)
                            lineTo(cx - w*0.30f, cy - h*0.22f)
                            close()
                        }
                        canvas.drawPath(shieldPath, p)
                        // Checkmark
                        p.strokeWidth = 2.8f
                        canvas.drawLine(cx - w*0.10f, cy + h*0.02f, cx - w*0.02f, cy + h*0.14f, p)
                        canvas.drawLine(cx - w*0.02f, cy + h*0.14f, cx + w*0.14f, cy - h*0.06f, p)
                    }
                    SettingsIcon.Bell -> {
                        // Bell body
                        val bellPath = android.graphics.Path().apply {
                            moveTo(cx - w*0.12f, cy + h*0.26f)
                            lineTo(cx - w*0.22f, cy - h*0.04f)
                            cubicTo(cx - w*0.24f, cy - h*0.14f, cx - w*0.14f, cy - h*0.22f, cx, cy - h*0.22f)
                            cubicTo(cx + w*0.14f, cy - h*0.22f, cx + w*0.24f, cy - h*0.14f, cx + w*0.22f, cy - h*0.04f)
                            lineTo(cx + w*0.12f, cy + h*0.26f)
                            close()
                        }
                        canvas.drawPath(bellPath, p)
                        // Clapper
                        canvas.drawCircle(cx, cy + h*0.32f, w*0.06f, pf)
                        // Top nub
                        canvas.drawCircle(cx, cy - h*0.26f, w*0.04f, pf)
                    }
                    SettingsIcon.File -> {
                        // Document
                        val docPath = android.graphics.Path().apply {
                            moveTo(cx - w*0.28f, cy + h*0.34f)
                            lineTo(cx - w*0.28f, cy - h*0.28f)
                            lineTo(cx + w*0.04f, cy - h*0.28f)
                            lineTo(cx + w*0.28f, cy - h*0.04f)
                            lineTo(cx + w*0.28f, cy + h*0.34f)
                            close()
                        }
                        canvas.drawPath(docPath, p)
                        // Lines
                        canvas.drawLine(cx - w*0.14f, cy - h*0.04f, cx + w*0.14f, cy - h*0.04f, p)
                        canvas.drawLine(cx - w*0.14f, cy + h*0.06f, cx + w*0.14f, cy + h*0.06f, p)
                        canvas.drawLine(cx - w*0.14f, cy + h*0.16f, cx + w*0.06f, cy + h*0.16f, p)
                    }
                    SettingsIcon.Feedback -> {
                        // Chat bubble
                        val bubblePath = android.graphics.Path().apply {
                            moveTo(cx - w*0.30f, cy - h*0.14f)
                            lineTo(cx - w*0.30f, cy + h*0.20f)
                            lineTo(cx - w*0.10f, cy + h*0.10f)
                            lineTo(cx + w*0.30f, cy + h*0.10f)
                            lineTo(cx + w*0.30f, cy - h*0.14f)
                            close()
                        }
                        canvas.drawPath(bubblePath, p)
                        // Dots
                        canvas.drawCircle(cx - w*0.10f, cy - w*0.02f, 2.5f, pf)
                        canvas.drawCircle(cx, cy - w*0.02f, 2.5f, pf)
                        canvas.drawCircle(cx + w*0.10f, cy - w*0.02f, 2.5f, pf)
                    }
                    SettingsIcon.A11y -> {
                        // Circle (person)
                        canvas.drawCircle(cx, cy - h*0.06f, w*0.14f, p)
                        // Body line
                        canvas.drawLine(cx, cy + h*0.08f, cx, cy + h*0.30f, p)
                        // Arms
                        canvas.drawLine(cx - w*0.18f, cy + h*0.14f, cx + w*0.18f, cy + h*0.14f, p)
                        // Accessibility arc
                        p.strokeWidth = 2f
                        canvas.drawArc(cx - w*0.32f, cy - h*0.40f, cx + w*0.32f, cy + h*0.40f, -30f, 60f, false, p)
                    }
                    SettingsIcon.QA -> {
                        // Checkmark in circle
                        canvas.drawCircle(cx, cy, w*0.30f, p)
                        p.strokeWidth = 3f
                        canvas.drawLine(cx - w*0.12f, cy, cx - w*0.02f, cy + w*0.12f, p)
                        canvas.drawLine(cx - w*0.02f, cy + w*0.12f, cx + w*0.14f, cy - w*0.10f, p)
                    }
                    SettingsIcon.Eye -> {
                        // Eye shape
                        val eyePath = android.graphics.Path().apply {
                            moveTo(cx - w*0.30f, cy)
                            cubicTo(cx - w*0.20f, cy - h*0.22f, cx + w*0.20f, cy - h*0.22f, cx + w*0.30f, cy)
                            cubicTo(cx + w*0.20f, cy + h*0.22f, cx - w*0.20f, cy + h*0.22f, cx - w*0.30f, cy)
                            close()
                        }
                        canvas.drawPath(eyePath, p)
                        canvas.drawCircle(cx, cy, w*0.10f, p)
                    }
                    SettingsIcon.Legal -> {
                        // Scale icon (balanced justice)
                        canvas.drawLine(cx, cy - h*0.34f, cx, cy - h*0.10f, p)
                        canvas.drawLine(cx - w*0.28f, cy - h*0.10f, cx + w*0.28f, cy - h*0.10f, p)
                        // Left pan
                        val leftArc = android.graphics.Path().apply {
                            moveTo(cx - w*0.28f, cy - h*0.10f)
                            cubicTo(cx - w*0.22f, cy + h*0.04f, cx - w*0.18f, cy + h*0.04f, cx - w*0.12f, cy - h*0.10f)
                        }
                        canvas.drawPath(leftArc, p)
                        // Right pan
                        val rightArc = android.graphics.Path().apply {
                            moveTo(cx + w*0.12f, cy - h*0.10f)
                            cubicTo(cx + w*0.18f, cy + h*0.04f, cx + w*0.22f, cy + h*0.04f, cx + w*0.28f, cy - h*0.10f)
                        }
                        canvas.drawPath(rightArc, p)
                    }
                    SettingsIcon.Sub -> {
                        // Star / sparkle for subscription
                        val starPath = android.graphics.Path().apply {
                            for (i in 0..9) {
                                val angle = Math.toRadians((i * 36 - 90).toDouble())
                                val r = if (i % 2 == 0) (w * 0.30f) else (w * 0.14f)
                                val x = cx + r * cos(angle).toFloat()
                                val y = cy + r * sin(angle).toFloat()
                                if (i == 0) moveTo(x, y) else lineTo(x, y)
                            }
                            close()
                        }
                        pf.style = Paint.Style.FILL
                        pf.color = Color.argb(60, 108, 99, 255)
                        canvas.drawPath(starPath, pf)
                        pf.style = Paint.Style.STROKE
                        canvas.drawPath(starPath, p)
                    }
                    SettingsIcon.Notif -> {
                        // Notification bell (same as Bell but simpler)
                        val nPath = android.graphics.Path().apply {
                            moveTo(cx - w*0.10f, cy + h*0.24f)
                            lineTo(cx - w*0.20f, cy - h*0.06f)
                            cubicTo(cx - w*0.22f, cy - h*0.16f, cx - w*0.12f, cy - h*0.24f, cx, cy - h*0.24f)
                            cubicTo(cx + w*0.12f, cy - h*0.24f, cx + w*0.22f, cy - h*0.16f, cx + w*0.20f, cy - h*0.06f)
                            lineTo(cx + w*0.10f, cy + h*0.24f)
                            close()
                        }
                        canvas.drawPath(nPath, p)
                        canvas.drawCircle(cx, cy + h*0.30f, w*0.05f, pf)
                        // Notification badge
                        pf.color = Color.argb(255, 255, 71, 87)
                        canvas.drawCircle(cx + w*0.18f, cy - h*0.20f, w*0.06f, pf)
                    }
                    SettingsIcon.LogOut -> {
                        // Power icon
                        val powerPath = android.graphics.Path().apply {
                            moveTo(cx, cy - h*0.30f)
                            lineTo(cx, cy)
                            moveTo(cx - w*0.16f, cy - h*0.16f)
                            cubicTo(cx - w*0.28f, cy - h*0.04f, cx - w*0.24f, cy + h*0.16f, cx - w*0.10f, cy + h*0.26f)
                            cubicTo(cx + w*0.10f, cy + h*0.26f, cx + w*0.28f, cy - h*0.04f, cx + w*0.16f, cy - h*0.16f)
                        }
                        canvas.drawPath(powerPath, p)
                    }
                    SettingsIcon.Settings -> {
                        // Gear / cog icon
                        p.strokeWidth = 2.2f
                        canvas.drawCircle(cx, cy, w*0.22f, p)
                        canvas.drawCircle(cx, cy, w*0.08f, pf)
                        listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f).forEach { angle ->
                            val rad = Math.toRadians(angle.toDouble())
                            val rx = cx + w*0.32f * cos(rad).toFloat()
                            val ry = cy + w*0.32f * sin(rad).toFloat()
                            canvas.drawLine(rx, ry, cx + w*0.40f * cos(rad).toFloat(), cy + w*0.40f * sin(rad).toFloat(), p)
                        }
                    }
                    SettingsIcon.Help -> {
                        // Question mark in circle
                        p.strokeWidth = 2.5f
                        canvas.drawCircle(cx, cy, w*0.32f, p)
                        p.strokeWidth = 2.2f
                        val qPath = android.graphics.Path().apply {
                            moveTo(cx + w*0.08f, cy - h*0.06f)
                            cubicTo(cx + w*0.16f, cy - h*0.20f, cx - w*0.10f, cy - h*0.28f, cx - w*0.14f, cy - h*0.06f)
                            cubicTo(cx - w*0.18f, cy + h*0.08f, cx, cy + h*0.04f, cx, cy + h*0.14f)
                        }
                        canvas.drawPath(qPath, p)
                        pf.style = Paint.Style.FILL
                        canvas.drawCircle(cx, cy + h*0.22f, 1.8f, pf)
                    }
                    SettingsIcon.Chevron -> {
                        // Arrow right
                        p.strokeWidth = 3f
                        val ax = cx - w*0.06f
                        canvas.drawLine(ax + w*0.10f, cy, ax, cy - h*0.12f, p)
                        canvas.drawLine(ax + w*0.10f, cy, ax, cy + h*0.12f, p)
                    }
                }
            }
            override fun setAlpha(a: Int) {}
            override fun setColorFilter(cf: ColorFilter?) {}
            override fun getOpacity() = PixelFormat.TRANSLUCENT
        })
    }

    private fun toggleRow(title: String, subtitle: String, enabled: Boolean, onToggle: () -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = rounded(panel, radius = 12, strokeColor = border)
        layoutParams = blockParams()
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(TextView(this@MainActivity).apply {
                text = title
                textSize = 16f
                setTextColor(Color.WHITE)
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                includeFontPadding = false
            })
            addView(TextView(this@MainActivity).apply {
                text = subtitle
                textSize = 13f
                setTextColor(muted)
                setPadding(0, dp(3), 0, 0)
                maxLines = 2
            })
        })
        addView(LinearLayout(this@MainActivity).apply {
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(24))
            background = rounded(if (enabled) accent else Color.argb(25, 255, 255, 255), radius = 12)
            setPadding(dp(3), dp(3), dp(3), dp(3))
            addView(View(this@MainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(18), dp(18))
                background = rounded(Color.argb(if (enabled) 255 else 185, 255, 255, 255), radius = 9)
                (layoutParams as LinearLayout.LayoutParams).gravity = if (enabled) Gravity.END else Gravity.START
            })
        })
        setOnClickListener { onToggle() }
    }

    private fun chevronView() = ImageView(this@MainActivity).apply {
        layoutParams = LinearLayout.LayoutParams(dp(24), dp(24))
        setImageDrawable(object : android.graphics.drawable.Drawable() {
            override fun draw(canvas: Canvas) {
                val w = bounds.width().toFloat()
                val h = bounds.height().toFloat()
                val cx = w / 2f
                val cy = h / 2f
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textMuted; style = Paint.Style.STROKE; strokeWidth = 2.5f
                    strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
                }
                canvas.drawLine(cx + w*0.08f, cy, cx - w*0.10f, cy - h*0.12f, p)
                canvas.drawLine(cx + w*0.08f, cy, cx - w*0.10f, cy + h*0.12f, p)
            }
            override fun setAlpha(a: Int) {}
            override fun setColorFilter(cf: ColorFilter?) {}
            override fun getOpacity() = PixelFormat.TRANSLUCENT
        })
    }

    private fun sessionRow(session: SessionSummary) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(22), dp(18), dp(22), dp(18))
        background = rounded(panel)
        layoutParams = blockParams()
        addView(title(session.role, 20))
        addView(body("${session.date}  |  ${session.score}/100  |  ${session.answered}/${session.total} answered"))
        addView(rowOf(
            secondaryButton("Delete") {
                scope.launch {
                    val deletedRemote = backend.deleteSession(appState.authToken.ifBlank { null }, session.id)
                    if (!deletedRemote) appState.deleteSession(session.id)
                    remoteHistory = remoteHistory.filterNot { it.id == session.id }
                    showProgress()
                }
            },
            primaryButton("Practice again") {
                appState.selectedRole = session.role
                appState.resetActiveSession()
                showRoomSetup()
            }
        ))
    }

    private fun notificationRow(item: NotificationItem) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(22), dp(18), dp(22), dp(18))
        background = rounded(if (item.isRead) surface else panel)
        layoutParams = blockParams()
        addView(title(item.title, 20))
        addView(body(item.message.ifBlank { "No message" }))
        if (item.createdAt.isNotBlank()) addView(body(item.createdAt))
        addView(rowOf(
            secondaryButton(if (item.isRead) "Read" else "Mark read") {
                scope.launch {
                    backend.markNotificationRead(appState.authToken.ifBlank { null }, item.id)
                    showNotifications()
                }
            },
            secondaryButton("Delete") {
                scope.launch {
                    backend.deleteNotification(appState.authToken.ifBlank { null }, item.id)
                    showNotifications()
                }
            }
        ))
    }

    private fun metricText(k: String, v: String) = TextView(this).apply {
        text = "$k\n$v"
        textSize = 15f
        setTextColor(Color.WHITE)
        setLineSpacing(4f, 1.05f)
        setPadding(0, dp(8), 0, dp(8))
    }

    private fun spacer(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(height))
    }

    private fun rowOf(vararg views: View) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        layoutParams = blockParams()
        views.forEach { child ->
            addView(child.apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dp(4), 0, dp(4), 0)
                }
            })
        }
    }

    private fun baseColumn() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(22), dp(34), dp(22), dp(64))
        setBackgroundColor(bg)
    }

    private fun scroll(view: View) = ScrollView(this).apply {
        setBackgroundColor(bg)
        clipToPadding = false
        addView(view)
        ViewCompat.setOnApplyWindowInsetsListener(this) { target, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            target.setPadding(0, 0, 0, bars.bottom + dp(28))
            insets
        }
        requestApplyInsets()
    }

    private fun refreshableScroll(view: View, onRefresh: () -> Unit): SwipeRefreshLayout = SwipeRefreshLayout(this).apply {
        setBackgroundColor(bg)
        addView(scroll(view))
        setOnRefreshListener {
            onRefresh()
            isRefreshing = false
        }
        setColorSchemeColors(accent, green, warning)
        setProgressBackgroundColorSchemeColor(surface)
    }

    private fun logoRow(logoTextSize: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, 0, 0, dp(22))
        addView(ImageView(this@MainActivity).apply {
            setImageResource(R.drawable.prezzence_icon)
            layoutParams = LinearLayout.LayoutParams(dp(34), dp(34)).apply {
                setMargins(0, 0, dp(10), 0)
            }
        })
        addView(TextView(this@MainActivity).apply {
            text = "Prezzence"
            textSize = logoTextSize.toFloat()
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        })
    }

    private fun title(text: String, size: Int) = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(Color.WHITE)
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        setLineSpacing(0f, 0.98f)
        includeFontPadding = false
        setPadding(0, dp(6), 0, dp(12))
    }

    private fun body(text: String) = TextView(this).apply {
        this.text = text
        textSize = 15f
        setTextColor(muted)
        setLineSpacing(dp(3).toFloat(), 1.08f)
        includeFontPadding = true
        setPadding(0, dp(2), 0, dp(8))
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text.uppercase()
        textSize = 12f
        letterSpacing = 0.14f
        setTextColor(accent)
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        setPadding(0, dp(12), 0, dp(6))
    }

    private fun card(title: String, detail: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(16), dp(18), dp(16))
        background = rounded(panel, radius = 12, strokeColor = border)
        elevation = dp(1).toFloat()
        layoutParams = blockParams()
        addView(title(title, 20))
        addView(body(detail))
    }

    private fun pill(text: String, color: Int) = TextView(this).apply {
        this.text = text
        textSize = 12f
        setTextColor(Color.WHITE)
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        setPadding(dp(14), dp(7), dp(14), dp(7))
        background = rounded(Color.argb(220, 0, 0, 0)).apply { setStroke(0, color) }
    }

    private fun status(text: String) = TextView(this).apply {
        this.text = text
        gravity = Gravity.CENTER
        textSize = 11f
        letterSpacing = 0.20f
        setTextColor(Color.rgb(170, 164, 191))
        setPadding(0, dp(6), 0, dp(8))
    }

    private fun stepHeader() = TextView(this).apply {
        text = "STEP ${appState.currentQuestionIndex + 1} / ${appState.questions().size}"
        textSize = 17f
        setTextColor(muted)
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        setPadding(0, 0, 0, dp(10))
    }

    private fun backButton(action: () -> Unit) = ghostButton("< Back", action)

    private fun primaryButton(text: String, action: () -> Unit) = button(text, accent, Color.WHITE, action, borderColor = accent)

    private fun secondaryButton(text: String, action: () -> Unit) = button(text, Color.TRANSPARENT, accent, action, borderColor = accent)

    private fun dangerButton(text: String, action: () -> Unit) = button(text, danger, Color.WHITE, action, borderColor = danger)

    private fun ghostButton(text: String, action: () -> Unit) = button(text, Color.TRANSPARENT, muted, action, borderColor = Color.TRANSPARENT)

    private fun button(text: String, bgColor: Int, fgColor: Int, action: () -> Unit, borderColor: Int = Color.TRANSPARENT) = Button(this).apply {
        this.text = text
        isAllCaps = false
        includeFontPadding = false
        textSize = 15f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setTextColor(fgColor)
        background = rounded(bgColor, radius = 12, strokeColor = borderColor)
        minHeight = dp(52)
        minWidth = 0
        maxLines = 1
        isSingleLine = true
        stateListAnimator = null
        setPadding(dp(16), 0, dp(16), 0)
        setOnClickListener { action() }
    }

    private fun input(hint: String) = EditText(this).apply {
        this.hint = hint
        textSize = 15f
        setTextColor(Color.WHITE)
        setHintTextColor(textMuted)
        setSingleLine(true)
        setPadding(dp(16), 0, dp(16), 0)
        minHeight = dp(52)
        background = rounded(inputBg, radius = 12, strokeColor = border)
        layoutParams = blockParams()
    }

    private fun blockParams() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        setMargins(0, dp(7), 0, dp(9))
    }

    private fun rounded(color: Int, radius: Int = 12, strokeColor: Int = border) = android.graphics.drawable.GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (strokeColor != Color.TRANSPARENT) setStroke(dp(1), strokeColor)
    }

    private fun releaseNativeSurfaces() {
        speechGenerationToken += 1
        activeAvatar?.release()
        activeAvatar = null
        activeCamera?.stop()
        activeCamera = null
        activeTranscriber?.stop(appState.language)
        activeTranscriber = null
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun starPath(cx: Float, cy: Float, r: Float): android.graphics.Path {
        val path = android.graphics.Path()
        for (i in 0 until 10) {
            val angle = Math.toRadians((-90 + i * 36).toDouble())
            val radius = if (i % 2 == 0) r else r * 0.45f
            val x = cx + (radius * kotlin.math.cos(angle)).toFloat()
            val y = cy + (radius * kotlin.math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }
}








