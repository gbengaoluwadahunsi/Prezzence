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
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.os.Bundle
import android.util.Log
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pollecode.prezzencekotlin.billing.BillingUiState
import com.pollecode.prezzencekotlin.billing.PrezzenceBillingManager
import com.pollecode.prezzencekotlin.data.AnswerResult
import com.pollecode.prezzencekotlin.data.AuthSession
import com.pollecode.prezzencekotlin.data.AppState
import com.pollecode.prezzencekotlin.data.InterviewMode
import com.pollecode.prezzencekotlin.data.Interviewer
import com.pollecode.prezzencekotlin.data.NotificationItem
import com.pollecode.prezzencekotlin.data.PrezzenceBackendClient
import com.pollecode.prezzencekotlin.data.UserProgressSnapshot
import com.pollecode.prezzencekotlin.data.ResumeProfile
import com.pollecode.prezzencekotlin.data.PrezzenceDefaults
import com.pollecode.prezzencekotlin.data.SessionCreateException
import com.pollecode.prezzencekotlin.data.SessionErrorReason
import com.pollecode.prezzencekotlin.data.SessionSummary
import com.pollecode.prezzencekotlin.data.SessionScoring
import com.pollecode.prezzencekotlin.data.resolvedPracticeStatus
import com.pollecode.prezzencekotlin.nativebridge.NativeDuixAvatarView
import com.pollecode.prezzencekotlin.nativebridge.NativeSpeechTranscriber
import com.pollecode.prezzencekotlin.nativebridge.SpeechCaptureResult
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
import com.pollecode.prezzencekotlin.ui.PrezzenceSessionReportScreen
import com.pollecode.prezzencekotlin.ui.SessionReportAnswerItem
import com.pollecode.prezzencekotlin.ui.SessionHistoryItem
import com.pollecode.prezzencekotlin.ui.PrezzenceSessionHistoryScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceAnswerResultOverlay
import com.pollecode.prezzencekotlin.ui.PrezzenceModelAnswerOverlay
import com.pollecode.prezzencekotlin.ui.PrezzenceInterviewPausedOverlay
import com.pollecode.prezzencekotlin.data.NotificationPreferences
import com.pollecode.prezzencekotlin.ui.PracticeSessionItem
import com.pollecode.prezzencekotlin.ui.PrezzenceSettingsScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceSettingsLanguageScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceAccountScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceDeleteAccountScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceEditProfileScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceHelpScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceNotificationSettingsScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceAppSettingsScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceAccessibilitySettingsScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceFeedbackScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceDeviceQaScreen
import com.pollecode.prezzencekotlin.ui.PrezzencePrivacySettingsScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceLegalScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceResumeProfileScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceNotificationsInboxScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceMicDeniedScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceLowStorageScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceSimpleMessageScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceAuthCallbackScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceNetworkErrorScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceSessionErrorScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceSessionInterruptedScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceSessionSaveErrorScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceNotFoundScreen
import com.pollecode.prezzencekotlin.ui.PrezzencePaymentSuccessScreen
import com.pollecode.prezzencekotlin.ui.PrezzencePaywallScreen
import com.pollecode.prezzencekotlin.ui.PrezzenceSubscriptionScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private var subscriptionDisplayPrice: String? = null
    private val subscriptionPriceState = androidx.compose.runtime.mutableStateOf<String?>(null)

    private var activeAvatar: NativeDuixAvatarView? = null
    private var coachingMediaPlayer: MediaPlayer? = null
    private var resultOverlay: FrameLayout? = null
    private var teachingOverlay: FrameLayout? = null
    private val answerReviewVisibleState = androidx.compose.runtime.mutableStateOf(false)
    private val modelAnswerVisibleState = androidx.compose.runtime.mutableStateOf(false)
    private val interviewPausedState = androidx.compose.runtime.mutableStateOf(false)
    private var confirmOverlay: FrameLayout? = null
    private var activeTranscriber: NativeSpeechTranscriber? = null
    private var speechGenerationToken = 0
    private var cachedQuestionSpeechIndex = -1
    private var cachedQuestionSpeechSource: String = ""
    private val questionSpeechCache = mutableMapOf<Int, String>()
    private var openingIntroductionSpoken = false
    private var activeTranscript: String = ""
    private val speechErrorState = androidx.compose.runtime.mutableStateOf("")
    private var speechError: String
        get() = speechErrorState.value
        set(value) { speechErrorState.value = value }
    private var recordingStartTime: Long = 0L
    private val recordingDurationState = androidx.compose.runtime.mutableIntStateOf(0)
    private var recordingTimer: android.os.CountDownTimer? = null
    private val processingAnswerState = androidx.compose.runtime.mutableStateOf(false)
    private val interviewAnsweringState = androidx.compose.runtime.mutableStateOf(false)
    private val processingStageState = androidx.compose.runtime.mutableStateOf("")
    private val processingProgressState = androidx.compose.runtime.mutableIntStateOf(0)
    private val avatarReadyState = androidx.compose.runtime.mutableStateOf(false)
    private val interviewerSpeakingState = androidx.compose.runtime.mutableStateOf(false)
    private var interviewComposeView: ComposeView? = null
    private var currentAnswerResult: AnswerResult? = null
    private val sessionAnswers = mutableListOf<AnswerResult>()
    private var startAnswerAfterPermission = false
    private val remoteHistoryState = androidx.compose.runtime.mutableStateOf<List<SessionSummary>>(emptyList())
    private val userProgressState = androidx.compose.runtime.mutableStateOf<UserProgressSnapshot?>(null)
    private val unreadNotificationsState = androidx.compose.runtime.mutableIntStateOf(0)
    private val resumeFileNameState = androidx.compose.runtime.mutableStateOf<String?>(null)
    private val deviceQaResultsState = androidx.compose.runtime.mutableStateOf<List<DeviceQaResult>>(emptyList())
    private val deviceQaRunningState = androidx.compose.runtime.mutableStateOf(false)
    private val deviceQaStatusState = androidx.compose.runtime.mutableStateOf("Ready")
    private val resumeProfileTextState = androidx.compose.runtime.mutableStateOf("")
    private val resumeProfileStatusState = androidx.compose.runtime.mutableStateOf("Loading saved profile...")
    private val resumeProfileLoadingState = androidx.compose.runtime.mutableStateOf(true)
    private val notificationsInboxState = androidx.compose.runtime.mutableStateOf<List<NotificationItem>>(emptyList())
    private val notificationsLoadingState = androidx.compose.runtime.mutableStateOf(true)
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
    private var onboardingSessionLength: String = "standard"
    private var onboardingConsentAccepted: Boolean = false
    private var onboardingMicLoading: Boolean = false
    private var onboardingMicError: String? = null
    private var continueToRoomAfterMicPermission = false
    private var suppressNativeAvatarForEntry = false
    
    // Inter font typefaces for consistent cross-device typography
    private val interRegular: android.graphics.Typeface by lazy { androidx.core.content.res.ResourcesCompat.getFont(this, R.font.inter_regular)!! }
    private val interMedium: android.graphics.Typeface by lazy { androidx.core.content.res.ResourcesCompat.getFont(this, R.font.inter_medium)!! }
    private val interSemiBold: android.graphics.Typeface by lazy { androidx.core.content.res.ResourcesCompat.getFont(this, R.font.inter_semibold)!! }
    private val interBold: android.graphics.Typeface by lazy { androidx.core.content.res.ResourcesCompat.getFont(this, R.font.inter_bold)!! }
    private val interBlack: android.graphics.Typeface by lazy { androidx.core.content.res.ResourcesCompat.getFont(this, R.font.inter_black)!! }
    private var appToastView: View? = null
    private var activeTab: PrezzenceTab = PrezzenceTab.HOME
    private val homeTabState = androidx.compose.runtime.mutableStateOf(PrezzenceTab.HOME)
    private var homeComposeView: ComposeView? = null
    private var homeShellAttached = false
    private val homeUiRefreshState = androidx.compose.runtime.mutableIntStateOf(0)
    private var coachingMessage: String = ""

    private data class DashboardMetrics(
        val sessions: Int,
        val avgScore: Int,
        val improvementDelta: Int,
        val readinessLabel: String,
        val coachingTip: String,
        val bestSkillLabel: String?,
        val bestSkillValue: Int?,
        val weakestSkillLabel: String?,
    )

    private val resumeDocumentPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) uploadResumeDocument(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase Crashlytics for crash reporting
        runCatching {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().apply {
                setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
                log("Prezzence Kotlin v${BuildConfig.PREZZENCE_VERSION_NAME}")
            }
        }
        
        root = FrameLayout(this)
        root.fitsSystemWindows = false
        appState = AppState(this)
        onboardingInterviewerStyle = appState.interviewerStyle
        onboardingPreviewGender = appState.previewGender
        updateDuixDownloadAuth()
        billingManager = PrezzenceBillingManager(this) { entitled, status, purchaseToken, orderId ->
            appState.subscriptionEntitled = entitled
            appState.subscriptionStatus = status
            appState.subscriptionProductId = BuildConfig.PREZZENCE_SUBSCRIPTION_PRODUCT_ID
            if (entitled) syncPlayPurchase(purchaseToken, orderId)
        }
        setContentView(root)
        
        if (!handleAuthCallback(intent?.data)) showSplash()
    }

    private fun updateDuixDownloadAuth() {
        NativeDuixAvatarView.downloadAuthToken = appState.authToken.takeIf { it.isNotBlank() }
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
        updateDuixDownloadAuth()
        scope.launch {
            refreshNotificationPreferencesFromServer()
            refreshServerEntitlement()
        }
    }

    private suspend fun refreshServerEntitlement() {
        if (appState.authToken.isBlank()) return
        val entitlement = backend.fetchEntitlement(appState.authToken)
        appState.betaUnlockAllFeatures = entitlement.betaUnlockAllFeatures
        appState.adminAccess = entitlement.adminAccess
        var entitled = entitlement.isPremium || entitlement.adminAccess
        if (!entitled) {
            val playState = billingManager.refresh()
            entitled = playState.entitled
            if (entitled && !playState.purchaseToken.isNullOrBlank()) {
                backend.syncEntitlement(
                    bearerToken = appState.authToken,
                    purchaseToken = playState.purchaseToken,
                    productId = playState.productId,
                    packageName = packageName,
                    orderId = playState.orderId,
                )
            }
            if (!playState.price.isNullOrBlank()) {
                subscriptionDisplayPrice = playState.price
                subscriptionPriceState.value = playState.price
            }
        }
        appState.subscriptionEntitled = entitled
        updateDuixDownloadAuth()
    }

    private fun currentNotificationPreferences(): NotificationPreferences = NotificationPreferences(
        pushNotificationsEnabled = appState.pushNotificationsEnabled,
        emailSummariesEnabled = appState.emailSummariesEnabled,
        practiceRemindersEnabled = appState.practiceRemindersEnabled,
        achievementAlertsEnabled = appState.achievementAlertsEnabled,
        productUpdatesEnabled = appState.productUpdatesEnabled,
    )

    private fun applyNotificationPreferences(preferences: NotificationPreferences) {
        appState.pushNotificationsEnabled = preferences.pushNotificationsEnabled
        appState.emailSummariesEnabled = preferences.emailSummariesEnabled
        appState.practiceRemindersEnabled = preferences.practiceRemindersEnabled
        appState.achievementAlertsEnabled = preferences.achievementAlertsEnabled
        appState.productUpdatesEnabled = preferences.productUpdatesEnabled
    }

    private suspend fun refreshNotificationPreferencesFromServer() {
        if (appState.authToken.isBlank()) return
        backend.fetchNotificationPreferences(appState.authToken)?.let { applyNotificationPreferences(it) }
    }

    private suspend fun syncNotificationPreferencesToServer() {
        if (appState.authToken.isBlank()) return
        backend.updateNotificationPreferences(appState.authToken, currentNotificationPreferences())
    }

    private fun updateNotificationPreference(onUpdated: () -> Unit = {}, block: () -> Unit) {
        block()
        scope.launch {
            syncNotificationPreferencesToServer()
            withContext(Dispatchers.Main) { onUpdated() }
        }
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
        val refreshToken = params["refresh_token"].orEmpty()
        val alreadySignedIn = appState.authToken.isNotBlank()
        if (!alreadySignedIn) {
            showSignIn(loading = true)
        }
        scope.launch {
            completeOAuthCallback(authCode, accessToken, refreshToken, isVerify)
        }
        return true
    }

    private fun routeAfterAuth(isVerify: Boolean) {
        if (isVerify) {
            showVerify()
        } else if (appState.onboardingComplete) {
            showHome()
        } else {
            showOnboardingType()
        }
    }

    private suspend fun resolveOAuthSession(authCode: String, accessToken: String, refreshToken: String): AuthSession? {
        val codeVerifier = appState.oauthPkceVerifier.ifBlank { oauthCodeVerifier }
        if (authCode.isNotBlank() && codeVerifier.isNotBlank()) {
            backend.exchangePkceCode(authCode, codeVerifier)?.let { return it }
        }
        if (accessToken.isNotBlank()) {
            val session = backend.sessionFromAccessToken(accessToken) ?: return null
            return if (refreshToken.isNotBlank()) session.copy(refreshToken = refreshToken) else session
        }
        return null
    }

    private suspend fun completeOAuthCallback(authCode: String, accessToken: String, refreshToken: String, isVerify: Boolean) {
        val session = resolveOAuthSession(authCode, accessToken, refreshToken)
        appState.oauthPkceVerifier = ""
        oauthCodeVerifier = ""

        if (session != null) {
            saveAuthSession(session)
            refreshServerEntitlement()
            routeAfterAuth(isVerify)
            return
        }

        if (appState.authToken.isNotBlank()) {
            val existing = backend.sessionFromAccessToken(appState.authToken)
            if (existing != null) {
                saveAuthSession(existing)
                refreshServerEntitlement()
                routeAfterAuth(isVerify)
                return
            }
            val storedRefresh = appState.authRefreshToken
            if (storedRefresh.isNotBlank()) {
                backend.refreshSession(storedRefresh)?.let {
                    saveAuthSession(it)
                    refreshServerEntitlement()
                    routeAfterAuth(isVerify)
                    return
                }
            }
        }

        showSignIn(error = "Google sign-in finished, but the session could not be loaded.")
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
            appState.oauthPkceVerifier = oauthCodeVerifier
            val url = backend.googleOAuthUrl("prezzence://auth/callback", oauthCodeVerifier)
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            appState.oauthPkceVerifier = ""
            oauthCodeVerifier = ""
            showAppToast("Could not open Google sign-in.", ToastKind.ERROR)
        }
    }

    private var suppressInterviewSpeech = false

    private fun muteInterviewRoomSpeech() {
        speechGenerationToken += 1
        suppressInterviewSpeech = true
        activeAvatar?.stopSpeaking()
        stopCoachingAudioFallback()
        interviewerSpeakingState.value = false
    }

    private fun resumeInterviewRoomSpeech() {
        suppressInterviewSpeech = false
    }

    private fun dismissTeachingOverlay() {
        activeAvatar?.stopSpeaking()
        stopCoachingAudioFallback()
        modelAnswerVisibleState.value = false
        teachingOverlay?.let { runCatching { root.removeView(it) } }
        teachingOverlay = null
    }

    private fun dismissResultOverlay() {
        dismissTeachingOverlay()
        activeAvatar?.stopSpeaking()
        answerReviewVisibleState.value = false
        resultOverlay?.let { runCatching { root.removeView(it) } }
        resultOverlay = null
    }

    private fun setScreen(view: View, animate: Boolean = true) {
        if (root.childCount == 1 && root.getChildAt(0) === view) {
            return
        }
        dismissResultOverlay()
        dismissConfirmOverlay()
        if (view !== interviewComposeView) {
            interviewComposeView = null
        }
        releaseNativeSurfaces()
        root.removeAllViews()
        root.setBackgroundColor(bg)
        view.alpha = if (animate) 0f else 1f
        root.addView(
            view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        if (animate) {
            view.animate().alpha(1f).setDuration(300).start()
        }
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
        if (appState.authToken.isNotBlank()) {
            if (appState.onboardingComplete) showHome() else showOnboardingType()
        } else if (appState.onboardingComplete) {
            showHome()
        } else {
            showLanding()
        }
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
            typeface = interBold
        })
    }

    private fun landingKicker(text: String) = TextView(this).apply {
        this.text = text
        textSize = 16f
        letterSpacing = 0.24f
        setTextColor(accent)
        typeface = interMedium
        includeFontPadding = false
        setPadding(0, 0, 0, dp(34))
    }

    private fun landingTitle(text: String, gradient: Boolean) = TextView(this).apply {
        this.text = text
        textSize = 70f
        setLineSpacing((-4).toFloat(), 0.92f)
        includeFontPadding = false
        typeface = interBold
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
        typeface = interBold
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
        typeface = interMedium
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
                            refreshServerEntitlement()
                            withContext(Dispatchers.Main) {
                                if (appState.onboardingComplete) showHome() else showOnboardingType()
                            }
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
        setScreen(ComposeView(this).apply {
            setContent { PrezzenceAuthCallbackScreen() }
        })
        if (!token.isNullOrBlank()) {
            scope.launch {
                val session = backend.sessionFromAccessToken(token)
                if (session != null) {
                    saveAuthSession(session)
                    refreshServerEntitlement()
                    withContext(Dispatchers.Main) {
                        if (appState.onboardingComplete) showHome() else showOnboardingType()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showSignIn(error = "Unable to complete sign in.")
                    }
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
                    onBack = {
                        if (appState.authToken.isNotBlank() || appState.onboardingComplete) {
                            showHome()
                        } else {
                            showLanding()
                        }
                    },
                    onSelectTrack = { track ->
                        scope.launch {
                            refreshServerEntitlement()
                            withContext(Dispatchers.Main) {
                                if (!appState.hasPremiumAccess() && !track.equals("job", ignoreCase = true)) {
                                    showAppToast("Only the Job Interview track is available on the Free plan. Upgrade to Pro for all tracks.", ToastKind.WARNING)
                                    showPaywall()
                                    return@withContext
                                }
                                onboardingTrack = track
                                appState.interviewMode = if (track == "leadership") InterviewMode.PANEL else InterviewMode.SINGLE
                                applyTrackDefaults(track)
                                showOnboardingType()
                            }
                        }
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
                        if (mode.equals("Panel", ignoreCase = true) && !appState.hasPremiumAccess()) {
                            showAppToast("Panel interviews are a Pro feature. Subscribe to unlock all interviewers.", ToastKind.WARNING)
                            return@PrezzenceOnboardingRoleScreen
                        }
                        appState.interviewMode = if (mode.equals("Panel", ignoreCase = true)) InterviewMode.PANEL else InterviewMode.SINGLE
                    },
                    onDifficultyChange = { difficulty -> onboardingDifficulty = difficulty },
                    onCompanyNameChange = { companyName -> onboardingCompanyName = companyName },
                    onCompanyWebsiteChange = { companyWebsite -> onboardingCompanyWebsite = companyWebsite },
                    onCompanyContextChange = { companyContext -> onboardingCompanyContext = companyContext },
                    onInterviewerStyleChange = { style ->
                        if (!appState.hasPremiumAccess() && !style.equals("Balanced", ignoreCase = true)) {
                            showAppToast("Only Balanced style is available on the Free plan. Upgrade to Pro for Supportive and Challenging styles.", ToastKind.WARNING)
                            return@PrezzenceOnboardingRoleScreen
                        }
                        onboardingInterviewerStyle = style
                        appState.interviewerStyle = style
                    },
                    onPreviewGenderChange = { gender ->
                        onboardingPreviewGender = gender
                        appState.previewGender = gender
                    },
                    onIncludeTechnicalChange = { enabled -> onboardingIncludeTechnical = enabled },
                    onEnableWebResearchChange = { enabled ->
                        if (enabled && !appState.hasPremiumAccess()) {
                            showAppToast("Company web research is a Pro feature.", ToastKind.WARNING)
                            return@PrezzenceOnboardingRoleScreen
                        }
                        onboardingEnableWebResearch = enabled
                    },
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

    private fun refreshHomeData(tab: PrezzenceTab) {
        if (appState.authToken.isBlank()) return
        scope.launch {
            if (tab == PrezzenceTab.PROGRESS || tab == PrezzenceTab.PRACTICE) {
                refreshRemoteHistory()
            }
            if (tab == PrezzenceTab.HOME || tab == PrezzenceTab.PROGRESS || tab == PrezzenceTab.PROFILE) {
                refreshUserProgress()
            }
            if (tab == PrezzenceTab.PROFILE) {
                refreshResumeProfileName()
            }
            refreshUnreadNotifications()
        }
    }

    private fun ensureHomeShell() {
        if (homeComposeView != null) return
        homeComposeView = ComposeView(this).apply {
            setContent {
                val currentTab by homeTabState
                val remoteHistory by remoteHistoryState
                val progressSnapshot by userProgressState
                val unreadCount by unreadNotificationsState
                val metrics = remember(remoteHistory, progressSnapshot) {
                    buildDashboardMetrics(sessionHistoryItems(), progressSnapshot)
                }
                val historyItems = remember(remoteHistory, progressSnapshot) { historyItemsForUi() }
                val firstName = appState.userFullName.ifBlank { appState.userEmail.substringBefore('@') }
                    .takeIf { it.isNotBlank() }
                val questions = appState.questions()
                val hasIncomplete = appState.activeSessionId.isNotBlank() && questions.isNotEmpty() &&
                    appState.currentQuestionIndex < questions.size
                androidx.compose.material3.MaterialTheme {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (currentTab) {
                            PrezzenceTab.HOME -> PrezzenceHomeScreen(
                                completedSessions = metrics.sessions,
                                readinessScore = metrics.avgScore,
                                signedInAs = appState.userEmail.ifBlank { null },
                                firstName = firstName,
                                hasIncompleteSession = hasIncomplete,
                                currentQuestionIndex = appState.currentQuestionIndex,
                                totalQuestions = questions.size.coerceAtLeast(1),
                                unreadNotifications = unreadCount,
                                improvementDelta = metrics.improvementDelta,
                                homeCoachingTip = metrics.coachingTip,
                                onStart = { showOnboardingType() },
                                onContinueSession = { resumeActiveSession() },
                                onNewSession = {
                                    appState.resetActiveSession()
                                    showOnboardingType()
                                },
                                onProgress = {
                                    homeTabState.value = PrezzenceTab.PROGRESS
                                    activeTab = PrezzenceTab.PROGRESS
                                    refreshHomeData(PrezzenceTab.PROGRESS)
                                },
                                onSettings = { showSettings() },
                                onNotifications = { showNotifications() },
                            )
                            PrezzenceTab.PRACTICE -> {
                                val allHistory = sessionHistoryItems()
                                val practiceSessions = allHistory.take(5).map { s ->
                                    PracticeSessionItem(
                                        id = s.id,
                                        title = s.role,
                                        type = "interview",
                                        date = s.date,
                                        score = s.score,
                                        answered = s.answered,
                                        total = s.total,
                                        status = s.resolvedPracticeStatus(),
                                    )
                                }
                                PrezzencePracticeScreen(
                                    recentSessions = practiceSessions,
                                    totalSessionCount = allHistory.size,
                                    onSessionTap = { sessionId -> showSessionReport(sessionId, PrezzenceTab.PRACTICE) },
                                    onDeleteSession = { sessionId ->
                                        val sessionTitle = practiceSessions.firstOrNull { it.id == sessionId }?.title
                                            ?: allHistory.firstOrNull { it.id == sessionId }?.role
                                            ?: "this session"
                                        confirmDeleteSession(sessionId, sessionTitle, PrezzenceTab.PRACTICE)
                                    },
                                    onViewAllHistory = { showAllHistory(PrezzenceTab.PRACTICE) },
                                    onSelectMode = { modeId ->
                                        when (modeId) {
                                            "full" -> {
                                                onboardingTrack = "job"
                                                onboardingSessionLength = "standard"
                                                appState.interviewMode = InterviewMode.PANEL
                                            }
                                            "quick" -> {
                                                onboardingTrack = "job"
                                                onboardingSessionLength = "quick"
                                                appState.interviewMode = InterviewMode.SINGLE
                                            }
                                            "behavioral" -> {
                                                onboardingTrack = "behavioral"
                                                onboardingSessionLength = "standard"
                                                appState.interviewMode = InterviewMode.SINGLE
                                            }
                                            "technical" -> {
                                                onboardingTrack = "technical"
                                                onboardingSessionLength = "standard"
                                                appState.interviewMode = InterviewMode.SINGLE
                                            }
                                            "promotion" -> {
                                                onboardingTrack = "promotion"
                                                onboardingSessionLength = "standard"
                                                appState.interviewMode = InterviewMode.SINGLE
                                            }
                                            else -> {
                                                onboardingTrack = "job"
                                                onboardingSessionLength = "standard"
                                                appState.interviewMode = InterviewMode.SINGLE
                                            }
                                        }
                                        applyTrackDefaults(onboardingTrack)
                                        showOnboardingRole()
                                    },
                                )
                            }
                            PrezzenceTab.PROGRESS -> {
                                PrezzenceProgressScreen(
                                    avgScore = metrics.avgScore,
                                    sessions = metrics.sessions,
                                    practiceMinutes = appState.practiceMinutes,
                                    readinessLabel = metrics.readinessLabel,
                                    recentSessions = historyItems,
                                    coachingTip = metrics.coachingTip,
                                    onSettings = { showSettings() },
                                    onSessionTap = { sessionId -> showSessionReport(sessionId, PrezzenceTab.PROGRESS) },
                                    onViewAllHistory = { showAllHistory(PrezzenceTab.PROGRESS) },
                                )
                            }
                            PrezzenceTab.PROFILE -> {
                                homeUiRefreshState.intValue
                                val displayName = appState.userFullName.ifBlank {
                                    appState.userEmail.substringBefore('@').ifBlank { "Prezzence user" }
                                }
                                val initials = displayName.split(' ').filter { it.isNotBlank() }.take(2)
                                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                    .joinToString("").ifBlank { "C" }
                                PrezzenceProfileScreen(
                                    fullName = displayName,
                                    email = appState.userEmail.ifBlank { "No email connected" },
                                    initials = initials,
                                    planLabel = accountPlanLabel(),
                                    avgScore = metrics.avgScore,
                                    sessions = metrics.sessions,
                                    practiceMinutes = appState.practiceMinutes,
                                    coachingTip = metrics.coachingTip,
                                    bestSkillLabel = metrics.bestSkillLabel,
                                    bestSkillValue = metrics.bestSkillValue,
                                    focusSkillLabel = metrics.weakestSkillLabel,
                                    resumeFileName = resumeFileNameState.value,
                                    language = appState.language,
                                    goalValue = appState.weeklyGoal,
                                    onUploadResume = { pickResumeDocument() },
                                    onDeleteResume = {
                                        scope.launch {
                                            backend.deleteResumeProfile(appState.authToken.ifBlank { null })
                                            resumeFileNameState.value = null
                                        }
                                    },
                                    onAccount = { showAccount() },
                                    onLanguage = { showLanguage() },
                                    onPrivacy = { showLegal() },
                                    onHelp = { showHelp { showHome(PrezzenceTab.PROFILE) } },
                                    onSignOut = {
                                        appState.signOut()
                                        showLanding()
                                    },
                                    onSettings = { showSettings() },
                                    onNotifications = { showNotifications() },
                                    unreadNotifications = unreadCount,
                                    onGoalChange = { newGoal ->
                                        appState.weeklyGoal = newGoal
                                    },
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                        ) {
                            PrezzenceBottomTabBar(
                                activeTab = currentTab,
                                onTabSelected = { newTab ->
                                    if (newTab != homeTabState.value) {
                                        homeTabState.value = newTab
                                        activeTab = newTab
                                        refreshHomeData(newTab)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun showHome(tab: PrezzenceTab = PrezzenceTab.HOME, skipDataRefresh: Boolean = false) {
        activeTab = tab
        homeTabState.value = tab

        if (skipDataRefresh) return

        if (appState.authToken.isNotBlank()) {
            scope.launch { refreshServerEntitlement() }
        }

        if (!appState.duixModelsPreloaded) {
            appState.duixModelsPreloaded = true
            scope.launch {
                try {
                    val preloadNames = listOf("Sofia", "Lily", "Oliver")
                    com.pollecode.prezzencekotlin.nativebridge.NativeDuixAvatarView.preloadModelFiles(this@MainActivity, preloadNames)
                } catch (_: Exception) {
                }
            }
        }

        val alreadyOnHome = homeComposeView?.parent == root
        ensureHomeShell()
        if (!alreadyOnHome) {
            val animate = !homeShellAttached
            setScreen(homeComposeView!!, animate = animate)
            homeShellAttached = true
        }
        refreshHomeData(tab)
    }

    private fun resumeActiveSession() {
        val sessionId = appState.activeSessionId
        sessionAnswers.clear()
        if (sessionId.isNotBlank()) {
            sessionAnswers.addAll(appState.getSessionAnswers(sessionId))
        }
        if (appState.questions().isEmpty()) {
            showAppToast("This session could not be resumed. Start a new interview.", ToastKind.WARNING)
            showOnboardingType()
            return
        }
        showInterview(false)
    }

    private suspend fun refreshResumeProfileName() {
        if (appState.authToken.isBlank()) {
            resumeFileNameState.value = null
            return
        }
        val profile = backend.getResumeProfile(appState.authToken)
        resumeFileNameState.value = profile?.fileName?.takeIf { it.isNotBlank() }
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
            typeface = interBold
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
                typeface = interBold
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
    private fun accountPlanLabel(): String = when {
        appState.adminAccess -> "Admin"
        appState.subscriptionEntitled -> "Pro"
        appState.betaUnlockAllFeatures -> "Beta"
        else -> "Free"
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
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceSettingsScreen(
                    language = appState.language,
                    subscriptionActive = appState.hasPremiumAccess(),
                    userEmail = appState.userEmail,
                    showDeviceQa = BuildConfig.DEBUG,
                    onBack = { showHome() },
                    onInterviewerSetup = { showOnboardingType() },
                    onLanguage = { showLanguage() },
                    onAccount = { showAccount() },
                    onSubscription = { showPaywall() },
                    onPrivacy = { showPrivacySettings() },
                    onNotificationPreferences = { showNotificationSettings() },
                    onFeedback = { showFeedback() },
                    onDeviceQa = { showDeviceQa() },
                    onAccessibility = { showAccessibility() },
                    onAppSettings = { showAppSettings() },
                    onResumeProfile = { showResumeProfile() },
                    onHelp = { showHelp() },
                    onSignOut = { appState.signOut(); showLanding() },
                    onSignIn = { showSignIn() },
                )
            }
        })
    }

    private fun showLanguage() {
        val languages = listOf("en-US", "en-GB", "es-ES", "fr-FR", "de-DE", "pt-BR", "ja-JP", "ko-KR", "zh-CN")
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceSettingsLanguageScreen(
                    selectedLanguage = appState.language,
                    languages = languages,
                    onBack = { showSettings() },
                    onLanguageSelected = { tag ->
                        appState.language = tag
                        showLanguage()
                    },
                )
            }
        })
    }

    private fun showAccount() {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceAccountScreen(
                    fullName = appState.userFullName,
                    email = appState.userEmail,
                    focus = appState.userFocus,
                    signedIn = appState.userEmail.isNotBlank(),
                    onBack = { showSettings() },
                    onEditProfile = { showEditProfile() },
                    onDeleteAccount = { showDeleteAccountConfirmation() },
                    onSignOut = { appState.signOut(); showLanding() },
                    onSignIn = { showSignIn() },
                )
            }
        })
    }

    private fun showDeleteAccountConfirmation() {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceDeleteAccountScreen(
                    onBack = { showAccount() },
                    onDelete = {
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
                    },
                )
            }
        })
    }

    private fun showEditProfile() {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceEditProfileScreen(
                    initialName = appState.userFullName,
                    email = appState.userEmail,
                    initialFocus = appState.userFocus,
                    onBack = { showAccount() },
                    onSave = { name, focus ->
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
                    },
                )
            }
        })
    }

    private fun showLowStorage() {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceLowStorageScreen(
                    onBack = { showSettings() },
                    onManageStorage = {
                        runCatching {
                            startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION))
                        }
                    },
                )
            }
        })
    }

    private fun showMicDenied() {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceMicDeniedScreen(
                    onBack = { showSettings() },
                    onOpenSettings = {
                        runCatching {
                            startActivity(
                                android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .apply { data = android.net.Uri.parse("package:$packageName") },
                            )
                        }
                    },
                    onPrivacy = { showLegal() },
                    onDeviceQa = { showDeviceQa() },
                )
            }
        })
    }

    private fun showNetworkError(returnToHome: Boolean = false) {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceNetworkErrorScreen(
                    returnToHome = returnToHome,
                    onBack = { if (returnToHome) showHome() else finish() },
                    onRetry = { if (returnToHome) showHome() else finish() },
                    onKeepProgress = { showHome() },
                    onOpenWifiSettings = {
                        runCatching { startActivity(android.content.Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)) }
                    },
                )
            }
        })
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
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceSessionErrorScreen(
                    title = errorInfo.title,
                    subtitle = errorInfo.subtitle,
                    badgeLabel = errorInfo.badgeLabel,
                    tips = errorInfo.tips,
                    onBack = { showHome() },
                )
            }
        })
    }

    private fun showAnswerRetryRequired(message: String) {
        speechError = message
        activeTranscript = ""
        processingAnswerState.value = false
        interviewAnsweringState.value = false
        processingStageState.value = ""
        processingProgressState.intValue = 0
        showAppToast(message, ToastKind.WARNING)
        if (!isInterviewRoomVisible()) {
            showInterview(answering = false, forceRebuild = true)
        }
    }

    private fun isInterviewRoomVisible(): Boolean = interviewComposeView?.parent == root

    private fun showSessionInterrupted() {
        setScreen(ComposeView(this).apply {
            setContent { PrezzenceSessionInterruptedScreen(onResume = { showHome() }) }
        })
    }
    private fun showSessionSaveError() {
        setScreen(ComposeView(this).apply {
            setContent { PrezzenceSessionSaveErrorScreen(onRetry = { showHome() }) }
        })
    }
    private fun showNotFound(returnTo: () -> Unit = { showHome() }) {
        setScreen(ComposeView(this).apply {
            setContent { PrezzenceNotFoundScreen(onReturnHome = returnTo) }
        })
    }
    private fun showHelp(onBack: () -> Unit = { showSettings() }) {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceHelpScreen(
                    onBack = onBack,
                    onContactSupport = { contactSupportEmail() },
                )
            }
        })
    }

    private fun contactSupportEmail() {
        val email = "support@prezzence.app"
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:$email")
            putExtra(Intent.EXTRA_SUBJECT, "Prezzence support")
            putExtra(Intent.EXTRA_TEXT, "Describe what you need help with:\n\n")
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            showAppToast("Email us at $email", ToastKind.INFO)
        }
    }

    private fun showPaymentSuccess() {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzencePaymentSuccessScreen(onGoToProfile = { showHome(PrezzenceTab.PROFILE) })
            }
        })
    }
    private fun showNotificationSettings() {
        scope.launch {
            refreshNotificationPreferencesFromServer()
            withContext(Dispatchers.Main) {
                setScreen(ComposeView(this@MainActivity).apply {
                    setContent {
                        PrezzenceNotificationSettingsScreen(
                            pushEnabled = appState.pushNotificationsEnabled,
                            emailEnabled = appState.emailSummariesEnabled,
                            remindersEnabled = appState.practiceRemindersEnabled,
                            achievementsEnabled = appState.achievementAlertsEnabled,
                            productUpdatesEnabled = appState.productUpdatesEnabled,
                            onBack = { showSettings() },
                            onDone = { showSettings() },
                            onTogglePush = { enabled ->
                                updateNotificationPreference {
                                    appState.pushNotificationsEnabled = enabled
                                }
                            },
                            onToggleEmail = { enabled ->
                                updateNotificationPreference {
                                    appState.emailSummariesEnabled = enabled
                                }
                            },
                            onToggleReminders = { enabled ->
                                updateNotificationPreference {
                                    appState.practiceRemindersEnabled = enabled
                                }
                            },
                            onToggleAchievements = { enabled ->
                                updateNotificationPreference {
                                    appState.achievementAlertsEnabled = enabled
                                }
                            },
                            onToggleProductUpdates = { enabled ->
                                updateNotificationPreference {
                                    appState.productUpdatesEnabled = enabled
                                }
                            },
                        )
                    }
                })
            }
        }
    }

    private fun showAppSettings() {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceAppSettingsScreen(
                    wifiOnlyDownloads = appState.wifiOnlyDownloads,
                    onBack = { showSettings() },
                    onToggleWifiOnly = { enabled -> appState.wifiOnlyDownloads = enabled },
                    onSave = { showSettings() },
                )
            }
        })
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
            typeface = interBold
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
                typeface = interBold; includeFontPadding = false
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
            typeface = interBold
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
            // Toggle visual Ã¢â‚¬â€ rebuild by calling refresh
        }
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(TextView(this@MainActivity).apply {
                text = label; textSize = 16f; setTextColor(Color.WHITE)
                typeface = interBold; includeFontPadding = false
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
            typeface = interBold
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
                    typeface = interBold
                })
                addView(pill(if (color == green) "green" else if (color == danger) "danger" else "warning", color))
            })
        }
    }

    private fun showAccessibility() {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceAccessibilitySettingsScreen(
                    largeText = appState.a11yLargeText,
                    highContrast = appState.a11yHighContrast,
                    reducedMotion = appState.a11yReducedMotion,
                    onBack = { showSettings() },
                    onToggleLargeText = { enabled ->
                        appState.a11yLargeText = enabled
                        showAccessibility()
                    },
                    onToggleHighContrast = { enabled ->
                        appState.a11yHighContrast = enabled
                        showAccessibility()
                    },
                    onToggleReducedMotion = { enabled ->
                        appState.a11yReducedMotion = enabled
                        showAccessibility()
                    },
                )
            }
        })
    }

    private fun sessionHistoryItems(): List<SessionSummary> {
        val local = appState.sessionHistory()
        val remote = remoteHistoryState.value.filterNot { appState.isSessionDeleted(it.id) }
        if (remote.isEmpty()) return local
        val localById = local.associateBy { it.id }
        val mergedRemote = remote.map { remoteItem ->
            val localItem = localById[remoteItem.id]
            val answers = appState.getSessionAnswers(remoteItem.id)
            val sessionQuestions = appState.questionsForSession(remoteItem.id)
            val recomputedScore = if (answers.isNotEmpty()) {
                SessionScoring.sessionScore(answers, sessionQuestions)
            } else {
                null
            }
            remoteItem.copy(
                score = recomputedScore ?: localItem?.score?.takeIf { it > 0 } ?: remoteItem.score,
                answered = if (answers.isNotEmpty()) answers.size else maxOf(remoteItem.answered, localItem?.answered ?: 0),
                total = listOf(remoteItem.total, localItem?.total ?: 0, answers.size, sessionQuestions.size).max(),
                role = remoteItem.role.ifBlank { localItem?.role.orEmpty() }.ifBlank { "Interview" },
                status = remoteItem.status.ifBlank { localItem?.status.orEmpty() },
            )
        }
        val remoteIds = mergedRemote.map { it.id }.toSet()
        return mergedRemote + local.filterNot { it.id in remoteIds }
    }

    private fun computeLocalImprovementDelta(history: List<SessionSummary>): Int {
        val dateFormat = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US)
        val chronologicalScores = history
            .sortedBy { runCatching { dateFormat.parse(it.date)?.time }.getOrNull() ?: 0L }
            .map { it.score }
            .filter { it > 0 }
        return if (chronologicalScores.size >= 2) {
            chronologicalScores.last() - chronologicalScores.first()
        } else {
            0
        }
    }

    private fun localReadinessLabel(avgScore: Int, historySize: Int): String = when {
        avgScore >= 75 -> "Interview Ready"
        avgScore >= 45 -> "Building Confidence"
        avgScore >= 25 -> "Building Momentum"
        avgScore > 0 -> "Foundation Built"
        historySize > 0 -> "Sessions Started"
        else -> "No Baseline"
    }

    private fun formatSkillLabel(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return null
        return value.split(Regex("\\s+")).joinToString(" ") { word ->
            word.replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase() else ch.toString() }
        }
    }

    private fun buildDashboardMetrics(
        history: List<SessionSummary>,
        progress: UserProgressSnapshot?,
    ): DashboardMetrics {
        val scored = history.filter { it.score > 0 }
        val sessions = maxOf(progress?.sessions ?: 0, history.size)
        val localAvg = if (scored.isNotEmpty()) {
            scored.map { it.score }.average().toInt().coerceIn(0, 100)
        } else {
            0
        }
        val avgScore = when {
            (progress?.avgScore ?: 0) > 0 -> progress!!.avgScore
            localAvg > 0 -> localAvg
            else -> 0
        }
        val improvementDelta = when {
            (progress?.improvementFromFirst ?: 0) != 0 -> progress!!.improvementFromFirst
            else -> computeLocalImprovementDelta(history)
        }
        val readinessLabel = progress?.readinessLabel?.takeIf { it.isNotBlank() }
            ?: localReadinessLabel(avgScore, history.size)
        val coachingTip = progress?.coachingTip.orEmpty()
        val strongest = formatSkillLabel(progress?.strongestSkill)
        val weakest = formatSkillLabel(progress?.weakestSkill)

        return DashboardMetrics(
            sessions = sessions,
            avgScore = avgScore,
            improvementDelta = improvementDelta,
            readinessLabel = readinessLabel,
            coachingTip = coachingTip,
            bestSkillLabel = strongest,
            bestSkillValue = progress?.strongestScore?.takeIf { it > 0 },
            weakestSkillLabel = weakest,
        )
    }

    private suspend fun refreshUserProgress() {
        val userId = appState.userId
        if (appState.authToken.isBlank() || userId.isBlank()) {
            userProgressState.value = null
            return
        }
        val progress = backend.fetchUserProgress(
            bearerToken = appState.authToken,
            userId = userId,
            language = appState.language,
            refreshToken = appState.authRefreshToken,
            onTokenRefreshed = { refreshed ->
                appState.authToken = refreshed.accessToken
                appState.authRefreshToken = refreshed.refreshToken
            },
        )
        userProgressState.value = progress
        if (progress != null) {
            if (progress.avgScore > 0) appState.readinessScore = progress.avgScore
            if (progress.sessions > 0) appState.completedSessions = progress.sessions
        }
    }

    private suspend fun refreshUnreadNotifications() {
        if (appState.authToken.isBlank()) {
            unreadNotificationsState.intValue = 0
            return
        }
        val notifications = backend.getNotifications(appState.authToken)
        unreadNotificationsState.intValue = notifications.count { !it.isRead }
    }

    private fun historyItemsForUi(): List<SessionHistoryItem> {
        return sessionHistoryItems().map { session ->
            SessionHistoryItem(
                id = session.id,
                role = session.role,
                score = session.score,
                date = session.date,
                answered = session.answered,
                total = session.total,
            )
        }
    }

    private suspend fun deleteSessionEverywhere(sessionId: String): Boolean {
        appState.deleteSession(sessionId)
        remoteHistoryState.value = remoteHistoryState.value.filterNot { it.id == sessionId }
        if (sessionId.startsWith("session-") || appState.authToken.isBlank()) {
            return true
        }
        return backend.deleteSessionWithAuthRetry(
            bearerToken = appState.authToken,
            refreshToken = appState.authRefreshToken,
            sessionId = sessionId,
            onTokenRefreshed = { refreshed ->
                appState.authToken = refreshed.accessToken
                appState.authRefreshToken = refreshed.refreshToken
            },
        )
    }

    private fun confirmDeleteSession(
        sessionId: String,
        sessionTitle: String,
        onTab: PrezzenceTab = PrezzenceTab.PRACTICE,
        onDeleted: (() -> Unit)? = null,
    ) {
        showConfirmDialog(
            title = "Delete session?",
            message = "Remove \"$sessionTitle\" from your history? This clears scores, answers, and coaching for this session.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                scope.launch {
                    val remoteDeleted = deleteSessionEverywhere(sessionId)
                    showAppToast(
                        if (remoteDeleted) "Session deleted." else "Session removed from this device.",
                        ToastKind.green,
                    )
                    onDeleted?.invoke() ?: showHome(onTab)
                }
            },
        )
    }

    private suspend fun refreshRemoteHistory() {
        if (appState.authToken.isBlank()) {
            remoteHistoryState.value = appState.sessionHistory()
            return
        }
        val fetched = backend.listSessions(appState.authToken)
            .filterNot { appState.isSessionDeleted(it.id) }
        if (fetched.isNotEmpty()) {
            remoteHistoryState.value = fetched
            appState.mergeSessionHistory(fetched)
        } else {
            remoteHistoryState.value = appState.sessionHistory()
        }
    }

    private fun resolveSessionSummary(sessionId: String): SessionSummary? {
        val answers = appState.getSessionAnswers(sessionId)
        val sessionQuestions = appState.questionsForSession(sessionId)
        val cached = sessionHistoryItems().find { it.id == sessionId }
        if (cached == null && answers.isEmpty()) return null
        val total = listOf(
            cached?.total ?: 0,
            sessionQuestions.size,
            answers.size,
        ).max().coerceAtLeast(1)
        val score = SessionScoring.sessionScore(answers, sessionQuestions)
        val substantiveCount = SessionScoring.substantiveAnswerCount(answers)
        val recordedCount = answers.size
        return SessionSummary(
            id = sessionId,
            role = cached?.role?.ifBlank { appState.selectedRole } ?: appState.selectedRole,
            score = score,
            answered = recordedCount,
            total = total,
            date = cached?.date ?: java.text.SimpleDateFormat("MMM d, yyyy", Locale.US).format(java.util.Date()),
            status = when {
                total > 0 && recordedCount >= total && substantiveCount > 0 && score > 0 -> "completed"
                recordedCount > 0 -> "in_progress"
                else -> cached?.status ?: "started"
            },
        )
    }

    private data class SessionReportBundle(
        val session: SessionSummary,
        val recordedCount: Int,
        val substantiveCount: Int,
        val hasSignal: Boolean,
        val displayScore: Int,
        val summary: String,
        val coachingTips: List<String>,
        val skillBreakdown: List<Pair<String, Int>>,
        val answers: List<SessionReportAnswerItem>,
    )

    private fun buildSessionReportAnswers(sessionId: String, total: Int): List<SessionReportAnswerItem> {
        val saved = appState.getSessionAnswers(sessionId)
        val questionCount = total.coerceAtLeast(saved.size).coerceAtLeast(1)
        return (0 until questionCount).map { index ->
            val answer = saved.getOrNull(index)
            val rawTranscript = answer?.transcript.orEmpty()
            val displayTranscript = SessionScoring.formatTranscriptForDisplay(rawTranscript)
            val storedScore = when {
                answer == null || SessionScoring.isBlankTranscript(rawTranscript) -> 0
                else -> answer.score.coerceIn(0, 100)
            }
            SessionReportAnswerItem(
                index = index + 1,
                score = storedScore,
                feedback = if (storedScore > 0) answer?.feedback.orEmpty() else "",
                transcript = displayTranscript,
            )
        }
    }

    private fun buildSessionReportBundle(sessionId: String): SessionReportBundle? {
        val session = resolveSessionSummary(sessionId) ?: return null
        val storedAnswers = appState.getSessionAnswers(sessionId)
        val sessionQuestions = appState.questionsForSession(sessionId)
        val answerItems = buildSessionReportAnswers(sessionId, session.total)
        val substantiveCount = SessionScoring.substantiveAnswerCount(storedAnswers)
        val recordedCount = storedAnswers.size
        val score = session.score
        val hasSignal = substantiveCount > 0 && score > 0
        val skillBreakdown = SessionScoring.sessionSkillBreakdown(storedAnswers, sessionQuestions)?.asList().orEmpty()
        val summary = if (hasSignal) {
            when {
                score >= 75 -> "Strong session. Your answers were relevant and well structured."
                score >= 50 -> "Solid effort. Tighten your examples with clearer actions and results."
                else -> "Keep practicing. Answer the prompt directly and back it up with one concrete example."
            }
        } else {
            "Most answers in this session were not scored as real interview responses. Retry with direct answers that include one example, your action, and a result."
        }
        val progressPlan = userProgressState.value?.coachingPlan.orEmpty()
        val weakestSkill = formatSkillLabel(userProgressState.value?.weakestSkill)
        val coachingTips = when {
            progressPlan.isNotEmpty() -> progressPlan.take(3)
            hasSignal && weakestSkill != null -> listOf(
                "Your next focus area is $weakestSkill. Use one STAR example with a clear result.",
                "Lead with your direct answer, then one situation-action-result example.",
                "Practice one more behavioral question before your next session.",
            )
            hasSignal -> listOf(
                "Lead with your direct answer, then one situation-action-result example.",
                "Include a measurable result or outcome in every answer.",
                "Practice one more behavioral question before your next session.",
            )
            else -> listOf(
                "Hold the phone close and speak until a transcript appears after each answer.",
                "Finish each question before tapping Continue.",
            )
        }
        return SessionReportBundle(
            session = session,
            recordedCount = recordedCount,
            substantiveCount = substantiveCount,
            hasSignal = hasSignal,
            displayScore = if (hasSignal) score else 0,
            summary = summary,
            coachingTips = coachingTips,
            skillBreakdown = skillBreakdown,
            answers = answerItems,
        )
    }

    private fun showSessionReport(sessionId: String, returnTab: PrezzenceTab = PrezzenceTab.HOME) {
        val bundle = buildSessionReportBundle(sessionId)
        if (bundle == null) {
            showAppToast("Session saved. You can review it from Progress.", ToastKind.INFO)
            showHome(returnTab)
            return
        }

        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceSessionReportScreen(
                    role = bundle.session.role,
                    date = bundle.session.date,
                    recordedCount = bundle.recordedCount,
                    total = bundle.session.total,
                    score = bundle.displayScore,
                    substantiveCount = bundle.substantiveCount,
                    hasSignal = bundle.hasSignal,
                    summary = bundle.summary,
                    coachingTips = bundle.coachingTips,
                    skillBreakdown = bundle.skillBreakdown,
                    answers = bundle.answers,
                    isPro = appState.hasPremiumAccess(),
                    onBack = { showHome(returnTab) },
                    onShareScore = { shareSessionScore(bundle) },
                    onExportPdf = { exportSessionPdf(sessionId) },
                    onPracticeAgain = {
                        appState.resetActiveSession()
                        showOnboardingRole()
                    },
                    onViewProgress = { showHome(PrezzenceTab.PROGRESS) },
                )
            }
        })
    }

    private fun shareSessionScore(bundle: SessionReportBundle) {
        if (!bundle.hasSignal) {
            showAppToast("Complete a scored answer before sharing your score.", ToastKind.INFO)
            return
        }
        val text = buildString {
            append("My Prezzence interview score: ${bundle.displayScore}%")
            append('\n')
            append(bundle.session.role.ifBlank { "Interview session" })
            if (bundle.session.date.isNotBlank()) {
                append(" · ")
                append(bundle.session.date)
            }
            append('\n')
            append("Practiced with Prezzence — AI interview coaching.")
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(shareIntent, "Share score"))
    }

    private fun exportSessionPdf(sessionId: String) {
        if (!appState.hasPremiumAccess()) {
            showAppToast("PDF export is a Pro feature. Subscribe to unlock.", ToastKind.WARNING)
            showPaywall()
            return
        }
        scope.launch {
            try {
                val bundle = withContext(Dispatchers.IO) {
                    buildSessionReportBundle(sessionId)
                } ?: run {
                    showAppToast("Session not found on this device.", ToastKind.ERROR)
                    return@launch
                }
                val file = withContext(Dispatchers.IO) {
                    writeSessionReportPdf(bundle, sessionId)
                }
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    this@MainActivity,
                    "${packageName}.fileprovider",
                    file,
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Share session report"))
            } catch (e: Exception) {
                showAppToast("Export failed. Try again.", ToastKind.ERROR)
            }
        }
    }

    private fun exportUserData() {
        scope.launch {
            try {
                val exportJson = withContext(Dispatchers.IO) { buildUserDataExportJson() }
                val file = java.io.File(cacheDir, "prezzence-export-${System.currentTimeMillis()}.json")
                withContext(Dispatchers.IO) { file.writeText(exportJson) }
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    this@MainActivity,
                    "${packageName}.fileprovider",
                    file,
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Prezzence data export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Export your data"))
            } catch (e: Exception) {
                showAppToast("Export failed. Try again.", ToastKind.ERROR)
            }
        }
    }

    private fun buildUserDataExportJson(): String {
        val root = org.json.JSONObject()
        root.put("exported_at", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date()))
        root.put("email", appState.userEmail)
        root.put("display_name", appState.userFullName)
        root.put("language", appState.language)
        root.put("completed_sessions", appState.completedSessions)
        root.put("readiness_score", appState.readinessScore)
        root.put("practice_minutes", appState.practiceMinutes)

        val sessionsArray = org.json.JSONArray()
        sessionHistoryItems().forEach { session ->
            val sessionJson = org.json.JSONObject()
                .put("id", session.id)
                .put("role", session.role)
                .put("score", session.score)
                .put("date", session.date)
                .put("answered", session.answered)
                .put("total", session.total)
                .put("status", session.status)
            val answers = appState.getSessionAnswers(session.id)
            if (answers.isNotEmpty()) {
                val answersArray = org.json.JSONArray()
                answers.forEachIndexed { index, answer ->
                    answersArray.put(
                        org.json.JSONObject()
                            .put("question_number", index + 1)
                            .put("score", answer.score)
                            .put("transcript", answer.transcript)
                            .put("feedback", answer.feedback),
                    )
                }
                sessionJson.put("answers", answersArray)
            }
            sessionsArray.put(sessionJson)
        }
        root.put("sessions", sessionsArray)
        return root.toString(2)
    }

    private fun writeSessionReportPdf(bundle: SessionReportBundle, sessionId: String): java.io.File {
        val session = bundle.session
        val document = android.graphics.pdf.PdfDocument()
        val writer = SessionPdfWriter(document)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 36f
            typeface = interBold
        }
        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 22f
            typeface = interBold
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 14f
        }
        val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(108, 99, 255)
            textSize = 28f
            typeface = interBold
        }

        writer.drawTextLine("Session Report", titlePaint, 44f)
        writer.gap(12f)
        writer.drawTextLine("Role: ${session.role}", headingPaint, 28f)
        writer.drawTextLine("Date: ${session.date}", bodyPaint)
        writer.drawTextLine(
            if (bundle.hasSignal) "Score: ${bundle.displayScore}/100" else "Score: --",
            scorePaint,
            32f,
        )
        writer.drawTextLine(
            "Questions: ${session.answered}/${session.total} recorded · ${bundle.substantiveCount} scored",
            bodyPaint,
        )

        if (bundle.skillBreakdown.isNotEmpty()) {
            writer.drawHeading("Strengths and gaps", headingPaint)
            writer.drawWrapped(
                "From ${bundle.substantiveCount} substantive ${if (bundle.substantiveCount == 1) "answer" else "answers"} only.",
                bodyPaint,
            )
            bundle.skillBreakdown.forEach { (label, value) ->
                writer.drawTextLine("$label: $value/100", bodyPaint)
            }
        }

        writer.drawHeading("Coach summary", headingPaint)
        writer.drawWrapped(bundle.summary, bodyPaint)
        bundle.coachingTips.forEach { tip ->
            writer.drawWrapped("• $tip", bodyPaint)
        }

        if (bundle.answers.isNotEmpty()) {
            writer.drawHeading("Answer details", headingPaint)
            bundle.answers.forEach { item ->
                val scoreLabel = when {
                    item.score > 0 -> "Score: ${item.score}"
                    item.transcript.isNotBlank() -> "Low signal"
                    else -> "No response"
                }
                writer.drawTextLine("Q${item.index} · $scoreLabel", headingPaint, 24f)
                if (item.transcript.isNotBlank()) {
                    writer.drawWrapped(item.transcript, bodyPaint)
                }
                if (item.feedback.isNotBlank()) {
                    writer.drawWrapped("Feedback: ${item.feedback}", bodyPaint)
                }
                writer.gap(8f)
            }
        }

        writer.finish()
        val file = java.io.File(cacheDir, "session-${sessionId.take(8)}.pdf")
        java.io.FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private class SessionPdfWriter(
        private val document: android.graphics.pdf.PdfDocument,
        private val pageWidth: Int = 595,
        private val pageHeight: Int = 842,
        private val margin: Float = 40f,
    ) {
        private var pageIndex = 0
        private var currentPage: android.graphics.pdf.PdfDocument.Page? = null
        var canvas: Canvas = Canvas()
            private set
        var y: Float = margin
            private set

        val contentWidth: Int get() = (pageWidth - 2 * margin).toInt()
        private val bottomLimit get() = pageHeight - margin

        init {
            startNewPage()
        }

        fun startNewPage() {
            currentPage?.let { document.finishPage(it) }
            pageIndex++
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
            currentPage = document.startPage(pageInfo)
            canvas = currentPage!!.canvas
            y = margin + 16f
        }

        fun finish() {
            currentPage?.let { document.finishPage(it) }
            currentPage = null
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > bottomLimit) startNewPage()
        }

        fun gap(amount: Float) {
            y += amount
        }

        fun drawTextLine(text: String, paint: Paint, lineHeight: Float = paint.textSize * 1.4f) {
            ensureSpace(lineHeight)
            canvas.drawText(text, margin, y, paint)
            y += lineHeight
        }

        fun drawWrapped(text: String, paint: Paint, spacingMultiplier: Float = 1.25f) {
            val textPaint = android.text.TextPaint(paint)
            var offset = 0
            while (offset < text.length) {
                if (y + textPaint.textSize * spacingMultiplier > bottomLimit) {
                    startNewPage()
                }
                val availableHeight = bottomLimit - y
                val lineHeight = textPaint.textSize * spacingMultiplier
                val maxLines = (availableHeight / lineHeight).toInt().coerceAtLeast(1)
                val layout = android.text.StaticLayout.Builder
                    .obtain(text, offset, text.length, textPaint, contentWidth)
                    .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, spacingMultiplier)
                    .setMaxLines(maxLines)
                    .build()
                if (layout.lineCount == 0) {
                    startNewPage()
                    continue
                }
                canvas.save()
                canvas.translate(margin, y)
                layout.draw(canvas)
                canvas.restore()
                y += layout.height + 4f
                val newOffset = layout.getLineEnd(layout.lineCount - 1)
                if (newOffset <= offset) {
                    startNewPage()
                    offset = (offset + 1).coerceAtMost(text.length)
                } else {
                    offset = newOffset
                    while (offset < text.length && text[offset].isWhitespace()) offset++
                }
            }
        }

        fun drawHeading(text: String, paint: Paint) {
            gap(12f)
            drawTextLine(text, paint, paint.textSize * 1.35f)
        }
    }

    private fun showFeedback() {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceFeedbackScreen(
                    onBack = { showSettings() },
                    onSubmit = { rating, comment ->
                        if (comment.length < 4 && rating == 5) {
                            showAppToast("Add a comment or lower your rating.", ToastKind.WARNING)
                            false
                        } else {
                            scope.launch {
                                val sent = backend.submitFeedback(
                                    appState.authToken.ifBlank { null },
                                    appState.activeSessionId.ifBlank { null },
                                    rating,
                                    comment,
                                )
                                if (!sent) showAppToast("Could not send feedback. Sign in and try again.", ToastKind.ERROR)
                            }
                            true
                        }
                    },
                )
            }
        })
    }
    private fun syncPlayPurchase(purchaseToken: String?, orderId: String? = null) {
        if (purchaseToken.isNullOrBlank() || appState.authToken.isBlank()) return
        scope.launch {
            backend.syncEntitlement(
                bearerToken = appState.authToken,
                purchaseToken = purchaseToken,
                productId = BuildConfig.PREZZENCE_SUBSCRIPTION_PRODUCT_ID,
                packageName = packageName,
                orderId = orderId,
            )
        }
    }

    private fun showPaywall() {
        setScreen(ComposeView(this).apply {
            setContent {
                val price by subscriptionPriceState
                PrezzencePaywallScreen(
                    hasPremium = appState.hasPremiumAccess(),
                    priceLabel = price ?: subscriptionDisplayPrice,
                    onBack = { showSettings() },
                    onSubscribe = { purchaseSubscription() },
                    onRestore = { restoreSubscription() },
                )
            }
        })
        if (subscriptionDisplayPrice == null && subscriptionPriceState.value == null) {
            scope.launch {
                val price = billingManager.refresh().price
                if (!price.isNullOrBlank()) {
                    subscriptionDisplayPrice = price
                    subscriptionPriceState.value = price
                }
            }
        }
    }
    private fun showSubscription() {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceSubscriptionScreen(
                    hasPremium = appState.hasPremiumAccess(),
                    onBack = { showPaywall() },
                    onSubscribe = { purchaseSubscription() },
                    onCheckPlan = { refreshSubscription() },
                    onRestore = { restoreSubscription() },
                )
            }
        })
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
        if (state.entitled || !appState.hasPremiumAccess()) {
            appState.subscriptionEntitled = state.entitled || appState.adminAccess || appState.betaUnlockAllFeatures
        }
        appState.subscriptionStatus = state.status
        appState.subscriptionProductId = state.productId
        if (!state.price.isNullOrBlank()) {
            subscriptionDisplayPrice = state.price
            subscriptionPriceState.value = state.price
        }
        if (state.entitled) syncPlayPurchase(state.purchaseToken, state.orderId)
        if (!silent) showAppToast(state.status, if (state.entitled) ToastKind.green else ToastKind.INFO)
    }

    private fun showDeviceQa(
        results: List<DeviceQaResult> = deviceQaResultsState.value,
        running: Boolean = deviceQaRunningState.value,
        status: String = deviceQaStatusState.value,
    ) {
        deviceQaResultsState.value = results
        deviceQaRunningState.value = running
        deviceQaStatusState.value = status
        setScreen(ComposeView(this).apply {
            setContent {
                val qaResults by deviceQaResultsState
                val qaRunning by deviceQaRunningState
                val qaStatus by deviceQaStatusState
                PrezzenceDeviceQaScreen(
                    status = qaStatus,
                    running = qaRunning,
                    results = qaResults,
                    onBack = { showSettings() },
                    onRequestPermissions = {
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA),
                            200,
                        )
                    },
                    onRunChecks = { if (!qaRunning) runDeviceQa() },
                    onClearModelCache = {
                        try {
                            NativeDuixAvatarView.clearModelCache(this@MainActivity)
                            showAppToast("Model cache cleared. Models will re-download on next interview.", ToastKind.green)
                        } catch (e: Exception) {
                            showAppToast("Failed to clear cache: ${e.message}", ToastKind.ERROR)
                        }
                    },
                    onRunAgain = { runDeviceQa() },
                )
            }
        })
    }
    private fun runDeviceQa() {
        showDeviceQa(running = true, status = "Starting checks")
        scope.launch {
            val runner = DeviceQaRunner(this@MainActivity, appState.authToken)
            val results = runner.runAll(appState.language) { status ->
                withContext(Dispatchers.Main) { showDeviceQa(running = true, status = status) }
            }
            showDeviceQa(results = results, status = "Checks complete")
        }
    }


    private fun showPrivacySettings() {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzencePrivacySettingsScreen(
                    onBack = { showSettings() },
                    onExportData = { exportUserData() },
                    onDone = { showSettings() },
                )
            }
        })
    }
    private fun showLegal() {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceLegalScreen(onBack = { showSettings() })
            }
        })
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
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceSimpleMessageScreen(
                    title = "Uploading CV",
                    subtitle = "Reading your file and building your practice profile...",
                    onBack = { showResumeProfile() },
                )
            }
        })

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
            if (saved != null) {
                resumeFileNameState.value = saved.fileName
                showHome(PrezzenceTab.PROFILE)
            } else {
                showResumeProfile()
            }
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
        resumeProfileLoadingState.value = true
        resumeProfileStatusState.value = "Loading saved profile..."
        setScreen(ComposeView(this).apply {
            setContent {
                val resumeText by resumeProfileTextState
                val statusText by resumeProfileStatusState
                val loading by resumeProfileLoadingState
                PrezzenceResumeProfileScreen(
                    initialText = resumeText,
                    statusText = statusText,
                    loading = loading,
                    onBack = { showSettings() },
                    onTextChange = { resumeProfileTextState.value = it },
                    onUploadFile = { pickResumeDocument() },
                    onSaveText = { resumeText ->
                        if (resumeText.length < 40) {
                            showAppToast("Paste more resume detail first.", ToastKind.WARNING)
                            false
                        } else {
                            resumeProfileStatusState.value = "Saving profile..."
                            scope.launch {
                                val saved = backend.saveResumeText(appState.authToken.ifBlank { null }, resumeText)
                                resumeProfileStatusState.value = if (saved != null) {
                                    "Saved: ${saved.fileName}\n${saved.summary.take(180)}"
                                } else {
                                    "Could not save profile. Sign in and try again."
                                }
                            }
                            true
                        }
                    },
                    onDelete = {
                        resumeProfileStatusState.value = "Deleting profile..."
                        scope.launch {
                            val deleted = backend.deleteResumeProfile(appState.authToken.ifBlank { null })
                            if (deleted) resumeProfileTextState.value = ""
                            resumeProfileStatusState.value = if (deleted) {
                                "Resume profile deleted."
                            } else {
                                "No profile deleted. Sign in and try again."
                            }
                        }
                    },
                )
            }
        })
        scope.launch {
            val profile = backend.getResumeProfile(appState.authToken.ifBlank { null })
            if (profile != null) {
                resumeProfileTextState.value = profile.summary
                resumeProfileStatusState.value = resumeProfileSummary(profile)
            } else {
                resumeProfileStatusState.value = "No saved resume profile yet."
            }
            resumeProfileLoadingState.value = false
        }
    }
    private fun showNotifications() {
        notificationsLoadingState.value = true
        setScreen(ComposeView(this).apply {
            setContent {
                val items by notificationsInboxState
                val loading by notificationsLoadingState
                PrezzenceNotificationsInboxScreen(
                    items = items,
                    loading = loading,
                    onBack = { showHome() },
                    onMarkRead = { item ->
                        scope.launch {
                            backend.markNotificationRead(appState.authToken.ifBlank { null }, item.id)
                            showNotifications()
                        }
                    },
                    onDelete = { item ->
                        scope.launch {
                            backend.deleteNotification(appState.authToken.ifBlank { null }, item.id)
                            showNotifications()
                        }
                    },
                )
            }
        })
        scope.launch {
            notificationsInboxState.value = backend.getNotifications(appState.authToken.ifBlank { null })
            notificationsLoadingState.value = false
        }
    }
    private fun resumeProfileSummary(profile: ResumeProfile): String {
        val skills = profile.skills.take(5).joinToString(", ").ifBlank { "No skills extracted yet" }
        return "Saved: ${profile.fileName}\nSkills: $skills\n${profile.summary.take(220)}"
    }

    private fun showProgress() = showHome(PrezzenceTab.PROGRESS)

    private fun showAllHistory(returnTab: PrezzenceTab = PrezzenceTab.PROGRESS) {
        val sessions = historyItemsForUi()
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceSessionHistoryScreen(
                    sessions = sessions,
                    onBack = { showHome(returnTab) },
                    onSessionTap = { sessionId -> showSessionReport(sessionId, returnTab) },
                    onDeleteSession = { sessionId ->
                        val sessionTitle = sessions.firstOrNull { it.id == sessionId }?.role ?: "this session"
                        confirmDeleteSession(sessionId, sessionTitle, returnTab) {
                            showAllHistory(returnTab)
                        }
                    },
                )
            }
        })
    }

    private fun showRoomSetup() {
        showEnteringRoom()
    }

    private fun showEnteringRoom(preparing: Boolean = true, setupStatus: String = "Preparing your questions and interview room.") {
        if (!appState.hasPremiumAccess() && sessionHistoryItems().size >= PrezzenceDefaults.FREE_SESSION_LIMIT) {
            showAppToast(
                "Free plan includes ${PrezzenceDefaults.FREE_SESSION_LIMIT} practice sessions. Upgrade to Pro for unlimited practice.",
                ToastKind.WARNING,
            )
            showPaywall()
            return
        }
        val interviewers = if (appState.interviewMode == InterviewMode.PANEL) {
            PrezzenceDefaults.panelInterviewersForStyle(appState.interviewerStyle)
        } else {
            listOf(appState.interviewerFor())
        }
        
        // Check if backend session is ready (questions loaded)
        val isSessionReady = appState.activeSessionId.isNotBlank() && appState.questions().isNotEmpty()
        
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceEnteringRoomScreen(
                    isPanel = appState.interviewMode == InterviewMode.PANEL,
                    interviewers = interviewers.map { it.name to it.title },
                    preparing = preparing && !isSessionReady, // Only show spinner while session/questions load
                    setupStatus = setupStatus,
                    onBack = { showHome() },
                    onJoin = { beginInterviewFromEntering() },
                )
            }
        })
        
        // Prepare backend session (fetch questions) - avatars will load on-demand during interview
        if (preparing && !isSessionReady) {
            prepareBackendSessionForEntering()
        }
    }

    private fun beginInterviewFromEntering() {
        oauthCodeVerifier = ""
        appState.oauthPkceVerifier = ""
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

    private suspend fun ensureActiveBackendSession(): Boolean {
        if (appState.authToken.isBlank()) return false
        if (appState.activeSessionId.isNotBlank()) return true
        return runCatching {
            val remote = tryCreateSession()
            appState.activeSessionId = remote.sessionId.orEmpty()
            if (!remote.questions.isNullOrEmpty() && appState.questions().isEmpty()) {
                appState.setGeneratedQuestions(remote.questions)
            }
            appState.activeSessionId.isNotBlank()
        }.getOrDefault(false)
    }

    private suspend fun enrichWithModelAnswer(questionText: String, result: AnswerResult): AnswerResult {
        val substantive = SessionScoring.isSubstantiveAnswer(result.transcript)
        val existing = sanitizeModelAnswer(result.improvedAnswer, result.transcript, substantive)
        if (existing.isNotBlank()) {
            return result.copy(improvedAnswer = existing)
        }

        val interviewer = appState.interviewerFor()
        val coachingTranscript = if (substantive) result.transcript else ""
        if (appState.authToken.isNotBlank()) {
            runCatching { ensureActiveBackendSession() }
            val fetched = runCatching {
                backend.fetchModelAnswer(
                    bearerToken = appState.authToken,
                    questionText = questionText,
                    transcript = coachingTranscript,
                    roleTitle = appState.selectedRole,
                    interviewerName = interviewer.name,
                    interviewerTitle = interviewer.title,
                )
            }.getOrNull()
            val fetchedAnswer = fetched?.improvedAnswer?.let {
                sanitizeModelAnswer(it, result.transcript, substantive)
            }.orEmpty()
            if (fetched != null && fetchedAnswer.isNotBlank()) {
                return result.copy(
                    improvedAnswer = fetchedAnswer,
                    what = fetched.what.ifBlank { result.what },
                    how = fetched.how.ifBlank { result.how },
                    why = fetched.why.ifBlank { result.why },
                    coachingMessage = fetched.coachingMessage.ifBlank { result.coachingMessage },
                )
            }
        }

        val fallback = buildLocalModelAnswer(questionText, appState.selectedRole)
        return result.copy(improvedAnswer = fallback)
    }

    private fun buildLocalModelAnswer(questionText: String, role: String): String {
        val q = questionText.trim().lowercase(Locale.US)
        val r = role.ifBlank { "professional" }
        if (q.contains("introduce yourself") || q.contains("tell me about yourself") || q.contains("overview of your background")) {
            return "Situation: I've spent the last several years building my expertise as a $r, " +
                "working across teams of varying sizes and tackling increasingly complex challenges.\n\n" +
                "Task: In my most recent role, I was brought on specifically to improve how our team delivered results " +
                "and to close gaps that were impacting our outcomes.\n\n" +
                "Action: I took ownership of our core workflow, introduced structured planning sessions, " +
                "and built relationships with stakeholders to align priorities. " +
                "I also mentored two junior team members who later took on leadership responsibilities.\n\n" +
                "Result: Within the first year, our team's output improved by 35% and client satisfaction scores " +
                "rose from 72% to 91%. I'm now looking for a role where I can bring that same impact at a larger scale.\n\n" +
                "Why this worked: Leading with concrete results and showing personal ownership demonstrates readiness for the next challenge."
        }
        return "Situation: In my role as a $r, I encountered a significant challenge " +
            "that required both strategic thinking and hands-on execution. The team was facing pressure to deliver " +
            "and existing approaches were falling short.\n\n" +
            "Task: I was responsible for diagnosing the root cause, proposing a solution, " +
            "and driving execution within a tight timeline. Leadership expected measurable improvement.\n\n" +
            "Action: I started by gathering data from all stakeholders to understand the full picture. " +
            "Then I designed a new approach, breaking it into phases so we could show early wins. " +
            "I personally led the first phase, set up weekly check-ins to maintain momentum, " +
            "and adjusted the plan twice based on feedback from the team.\n\n" +
            "Result: We completed the initiative two weeks ahead of schedule. " +
            "The measurable outcome was a 40% improvement in our key metric, and the approach " +
            "was adopted as the standard process going forward.\n\n" +
            "Why this worked: Showing that you can diagnose, plan, execute, and adapt under pressure " +
            "gives the interviewer confidence in your problem-solving ability and leadership."
    }

    private fun sanitizeModelAnswer(
        answer: String,
        userTranscript: String,
        substantive: Boolean,
    ): String {
        val trimmed = answer.trim()
        if (trimmed.isBlank()) return ""
        if (isGenericModelAnswer(trimmed)) return ""
        if (!substantive && quotesUserTranscript(trimmed, userTranscript)) return ""
        return trimmed
    }

    private fun quotesUserTranscript(modelAnswer: String, userTranscript: String): Boolean {
        val transcript = userTranscript.trim()
        if (transcript.isBlank()) return false
        if (modelAnswer.length > 200) return false
        val modelLower = modelAnswer.lowercase(Locale.US)
        val words = transcript.lowercase(Locale.US)
            .split(Regex("[^a-z0-9']+"))
            .filter { it.length > 5 }
        if (words.size < 6) return false
        val hits = words.count { modelLower.contains(it) }
        return hits >= (words.size * 0.7f).toInt().coerceAtLeast(5)
    }

    private fun isGenericModelAnswer(text: String): Boolean {
        val lower = text.trim().lowercase(Locale.US)
        if (lower.isBlank()) return true
        if (lower.length < 60) return true
        return (lower.startsWith("absolutely. for '") && lower.contains("keep it structured"))
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
            length = onboardingSessionLength,
        )
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

    private fun startRecordingTimer() {
        recordingStartTime = System.currentTimeMillis()
        recordingDurationState.intValue = 0
        recordingTimer?.cancel()
        recordingTimer = object : android.os.CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                recordingDurationState.intValue = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
            }
            override fun onFinish() {}
        }.start()
    }

    private fun stopRecordingTimer() {
        recordingTimer?.cancel()
        recordingTimer = null
        recordingDurationState.intValue = 0
    }

    private fun showInterview(answering: Boolean, processing: Boolean = false, forceRebuild: Boolean = false) {
        val wasAnswering = interviewAnsweringState.value
        interviewAnsweringState.value = answering
        processingAnswerState.value = processing

        if (!processing && forceRebuild) {
            interviewPausedState.value = false
            answerReviewVisibleState.value = false
            modelAnswerVisibleState.value = false
            coachingMessage = ""
            avatarReadyState.value = false
            interviewerSpeakingState.value = false
        }

        if (!forceRebuild && isInterviewRoomVisible()) {
            if (answering && !processing && !wasAnswering) {
                speechError = ""
                startRecordingTimer()
                startSpeechCapture()
                scope.launch { prepareQuestionSpeech(appState.currentQuestionIndex + 1) }
            } else if (!answering && wasAnswering) {
                stopRecordingTimer()
            }
            return
        }

        val question = appState.currentQuestion()
        val interviewer = appState.interviewerFor(question)

        if (answering && !processing) {
            speechError = ""
            startRecordingTimer()
        } else if (!answering) {
            stopRecordingTimer()
        }

        val composeView = ComposeView(this)
        interviewComposeView = composeView
        composeView.setContent {
            val answeringNow by interviewAnsweringState
            val processingNow by processingAnswerState
            val stage by processingStageState
            val progress by processingProgressState
            val recordingDuration by recordingDurationState
            val answerReviewVisible by answerReviewVisibleState
            val modelAnswerVisible by modelAnswerVisibleState
            val interviewPaused by interviewPausedState
            val avatarReady by avatarReadyState
            val interviewerSpeaking by interviewerSpeakingState
            val currentQuestion = appState.currentQuestion()
            val currentInterviewer = appState.interviewerFor(currentQuestion)
            val reviewResult = currentAnswerResult
            val nextStep = appState.currentQuestionIndex + 1
            val totalQs = appState.questions().size
            val continueLabel = if (nextStep >= totalQs) "Finish session" else "Continue ${nextStep + 1}/$totalQs"

            Box(Modifier.fillMaxSize()) {
                PrezzenceInterviewRoomScreen(
                    currentStep = appState.currentQuestionIndex + 1,
                    totalSteps = appState.questions().size,
                    interviewerName = currentInterviewer.name,
                    interviewerTitle = currentInterviewer.title,
                    isPanel = appState.interviewMode == InterviewMode.PANEL,
                    panelInterviewers = if (appState.interviewMode == InterviewMode.PANEL) {
                        PrezzenceDefaults.panelInterviewersForStyle(appState.interviewerStyle)
                            .map { it.name to it.title }
                    } else {
                        emptyList()
                    },
                    questionText = currentQuestion.text,
                    learnMoreTopic = currentQuestion.resolvedLearnMoreTopic(),
                    onLearnMore = { openLearnMoreUrl(currentQuestion.resolvedLearnMoreUrl()) },
                    answering = answeringNow,
                    processing = processingNow,
                    processingStage = stage,
                    processingProgress = progress,
                    transcript = activeTranscript,
                    error = speechError,
                    recordingDuration = recordingDuration,
                    isRecording = answeringNow && !processingNow && activeTranscriber != null,
                    createAvatarView = {
                        if (suppressNativeAvatarForEntry) {
                            suppressNativeAvatarForEntry = false
                            interviewerReadyCard(currentInterviewer)
                        } else {
                            try {
                                duixAvatarCard(currentInterviewer, "speaking", true)
                            } catch (e: Exception) {
                                interviewerReadyCard(currentInterviewer)
                            }
                        }
                    },
                    onExit = { showHome() },
                    onPause = { pauseInterview() },
                    onRepeat = { replayCurrentQuestion() },
                    onClarify = { clarifyCurrentQuestion() },
                    onAnswerNow = { ensurePermissionsThenAnswer() },
                    onFinish = { finishAnswer(currentQuestion.text) },
                    coachingMessage = coachingMessage,
                    avatarReady = avatarReady,
                    interviewerSpeaking = interviewerSpeaking,
                )

                if (interviewPaused) {
                    PrezzenceInterviewPausedOverlay(
                        onResume = {
                            interviewPausedState.value = false
                            showInterview(activeTranscriber != null)
                        },
                        onBackHome = {
                            interviewPausedState.value = false
                            showHome()
                        },
                    )
                }

                if (answerReviewVisible && reviewResult != null && !modelAnswerVisible) {
                    PrezzenceAnswerResultOverlay(
                        score = reviewResult.score,
                        feedback = reviewResult.feedback,
                        questionText = currentQuestion.text,
                        learnMoreTopic = currentQuestion.resolvedLearnMoreTopic(),
                        displayTranscript = SessionScoring.formatTranscriptForDisplay(reviewResult.transcript),
                        hasModelAnswer = reviewResult.improvedAnswer.isNotBlank(),
                        continueLabel = continueLabel,
                        onLearnMore = { openLearnMoreUrl(currentQuestion.resolvedLearnMoreUrl()) },
                        onTryAgain = { handleAnswerTryAgain() },
                        onContinue = {
                            resumeInterviewRoomSpeech()
                            dismissResultOverlay()
                            advanceAfterAnswerReview()
                        },
                        onOpenModelAnswer = { openModelAnswerOverlay(reviewResult) },
                    )
                }

                if (modelAnswerVisible && reviewResult != null) {
                    PrezzenceModelAnswerOverlay(
                        score = reviewResult.score,
                        modelAnswer = reviewResult.improvedAnswer.trim(),
                        learnMoreTopic = currentQuestion.resolvedLearnMoreTopic(),
                        continueLabel = continueLabel,
                        onBack = { dismissTeachingOverlay() },
                        onLearnMore = { openLearnMoreUrl(currentQuestion.resolvedLearnMoreUrl()) },
                        onPlayAgain = { playCoachingAudio(reviewResult.improvedAnswer.trim()) },
                        onTryAgain = { handleAnswerTryAgain() },
                        onContinue = {
                            resumeInterviewRoomSpeech()
                            dismissResultOverlay()
                            advanceAfterAnswerReview()
                        },
                    )
                }
            }
        }
        setScreen(composeView)
        interviewComposeView = composeView
        if (answering && !processing) {
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
        interviewPausedState.value = true
        if (!isInterviewRoomVisible()) {
            showInterview(answering = false)
        }
    }

    private fun handleAnswerTryAgain() {
        val retryIndex = appState.currentQuestionIndex
        if (retryIndex < sessionAnswers.size) {
            sessionAnswers.removeAt(retryIndex)
            val sid = appState.activeSessionId.ifBlank { "session-${System.currentTimeMillis()}" }
            appState.saveSessionAnswers(sid, sessionAnswers.toList())
        }
        currentAnswerResult = null
        coachingMessage = ""
        speechError = ""
        resumeInterviewRoomSpeech()
        dismissResultOverlay()
        showInterview(false)
    }

    private fun openModelAnswerOverlay(result: AnswerResult) {
        val modelAnswer = result.improvedAnswer.trim()
        modelAnswerVisibleState.value = true
        if (modelAnswer.isNotBlank()) {
            scope.launch {
                delay(400)
                playCoachingAudio(modelAnswer)
            }
        }
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
                typeface = interBold
                background = rounded(Color.argb(24, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)), radius = 999, strokeColor = Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).apply { setMargins(0, 0, dp(10), 0) }
            })
            addView(TextView(this@MainActivity).apply {
                text = message
                setTextColor(Color.WHITE)
                textSize = 14f
                typeface = interBold
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

    private fun dismissConfirmOverlay() {
        confirmOverlay?.let { runCatching { root.removeView(it) } }
        confirmOverlay = null
    }

    private fun showConfirmDialog(
        title: String,
        message: String,
        confirmLabel: String,
        destructive: Boolean = false,
        onConfirm: () -> Unit,
    ) {
        if (!::root.isInitialized) return
        dismissConfirmOverlay()
        val accentColor = if (destructive) danger else accent
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(Color.argb(150, 0, 0, 0))
            setOnClickListener { dismissConfirmOverlay() }
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.rgb(18, 18, 29), radius = 22, strokeColor = Color.argb(76, 108, 99, 255))
            setPadding(dp(20), dp(20), dp(20), dp(18))
            layoutParams = FrameLayout.LayoutParams(
                (resources.displayMetrics.widthPixels - dp(48)).coerceAtMost(dp(420)),
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            )
            setOnClickListener { }
        }
        panel.addView(TextView(this).apply {
            text = title
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = interBold
        })
        panel.addView(TextView(this).apply {
            text = message
            textSize = 14f
            setTextColor(Color.argb(210, 255, 255, 255))
            typeface = interRegular
            setLineSpacing(dp(3).toFloat(), 1f)
            setPadding(0, dp(10), 0, dp(18))
        })
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        actions.addView(TextView(this).apply {
            text = "Cancel"
            textSize = 14f
            setTextColor(muted)
            typeface = interBold
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setOnClickListener { dismissConfirmOverlay() }
        })
        actions.addView(TextView(this).apply {
            text = confirmLabel
            textSize = 14f
            setTextColor(accentColor)
            typeface = interBold
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setOnClickListener {
                dismissConfirmOverlay()
                onConfirm()
            }
        })
        panel.addView(actions)
        overlay.addView(panel)
        root.addView(
            overlay,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        confirmOverlay = overlay
        panel.alpha = 0f
        panel.translationY = dp(12).toFloat()
        panel.animate().alpha(1f).translationY(0f).setDuration(180).start()
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
        if (suppressInterviewSpeech) return
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

    private fun playCoachingAudio(improvedAnswer: String) {
        val modelAnswer = improvedAnswer.trim()
        if (modelAnswer.isBlank()) {
            showAppToast("No model answer is available yet.", ToastKind.WARNING)
            return
        }
        scope.launch {
            try {
                val currentInterviewer = appState.interviewerFor(appState.currentQuestion())
                val backendSpeech = backend.synthesizeSpeechUrl(
                    bearerToken = appState.authToken.ifBlank { null },
                    text = modelAnswer,
                    language = appState.language,
                    personality = currentInterviewer.id,
                )
                if (backendSpeech.isNullOrBlank()) {
                    showAppToast("Could not generate coaching audio.", ToastKind.WARNING)
                    return@launch
                }
                val avatar = waitForActiveAvatar()
                root.post {
                    if (avatar != null) {
                        avatar.speakAudioUri(backendSpeech, "coaching")
                    } else {
                        playCoachingAudioFallback(backendSpeech)
                    }
                }
            } catch (e: Exception) {
                showAppToast("Error playing coaching audio: ${e.message}", ToastKind.ERROR)
            }
        }
    }

    private suspend fun waitForActiveAvatar(maxWaitMs: Long = 8000): NativeDuixAvatarView? {
        val deadline = System.currentTimeMillis() + maxWaitMs
        while (System.currentTimeMillis() < deadline) {
            activeAvatar?.let { return it }
            delay(250)
        }
        return activeAvatar
    }

    private fun playCoachingAudioFallback(audioUrl: String) {
        stopCoachingAudioFallback()
        runCatching {
            coachingMediaPlayer = MediaPlayer().apply {
                setDataSource(audioUrl)
                setOnPreparedListener { player ->
                    player.start()
                }
                setOnCompletionListener {
                    stopCoachingAudioFallback()
                }
                setOnErrorListener { _, _, _ ->
                    stopCoachingAudioFallback()
                    showAppToast("Could not play coaching audio.", ToastKind.WARNING)
                    true
                }
                prepareAsync()
            }
        }.onFailure {
            showAppToast("Could not play coaching audio.", ToastKind.WARNING)
        }
    }

    private fun stopCoachingAudioFallback() {
        runCatching {
            coachingMediaPlayer?.stop()
            coachingMediaPlayer?.release()
        }
        coachingMediaPlayer = null
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

        if (!micGranted) {
            showMicDenied()
            return
        }
        showInterview(true)
    }

    private fun refreshInterviewForProcessing() {
        interviewAnsweringState.value = true
        processingAnswerState.value = true
        if (!isInterviewRoomVisible()) {
            showInterview(answering = true, processing = true, forceRebuild = true)
        }
    }

    private fun finishAnswer(questionText: String) {
        if (processingAnswerState.value) return
        val transcriber = activeTranscriber
        activeTranscriber = null
        currentAnswerResult = null
        val capturedSpeechError = speechError
        val answerPracticeSeconds = recordingDurationState.intValue.coerceAtLeast(0)
        
        // Stay on the interview screen and show processing state
        recordingTimer?.cancel()
        recordingTimer = null
        processingAnswerState.value = true
        processingStageState.value = "Transcribing audio"
        processingProgressState.intValue = 5
        refreshInterviewForProcessing()

        scope.launch {
            // Animate progress during transcription
            val progressJob = launch {
                var p = 5
                while (p < 95) {
                    delay(120)
                    p = (p + 1 + (Math.random() * 2).toInt()).coerceAtMost(95)
                    processingProgressState.intValue = p
                }
            }
            val capture = withContext(Dispatchers.IO) {
                transcriber?.stop(appState.language) ?: SpeechCaptureResult("")
            }
            Log.i("PrezzenceFinish", "Audio capture: base64Len=${capture.audioBase64?.length ?: 0}, duration=${capture.audioDurationSeconds}s, hasAudio=${!capture.audioBase64.isNullOrBlank()}")
            val practiceSeconds = when {
                answerPracticeSeconds > 0 -> answerPracticeSeconds
                capture.audioDurationSeconds > 0 -> capture.audioDurationSeconds
                else -> 0
            }
            if (practiceSeconds > 0) {
                appState.addPracticeSeconds(practiceSeconds)
            }
            progressJob.cancel()
            processingProgressState.intValue = 95
            activeTranscript = ""
            speechError = capturedSpeechError

            if (capture.audioBase64.isNullOrBlank()) {
                processingAnswerState.value = false
                processingStageState.value = ""
                processingProgressState.intValue = 0
                val message = capturedSpeechError.ifBlank {
                    "We could not capture your answer audio. Please speak closer to the microphone and try again."
                }
                showAnswerRetryRequired(message)
                return@launch
            }

            processingStageState.value = "Transcribing audio"
            processingProgressState.intValue = 97
            ensureActiveBackendSession()

            if (appState.authToken.isBlank() || appState.activeSessionId.isBlank()) {
                processingAnswerState.value = false
                processingStageState.value = ""
                processingProgressState.intValue = 0
                showAnswerRetryRequired("Sign in and start a session to transcribe and score your answer.")
                return@launch
            }

            val remoteResult = backend.scoreWithBackend(
                bearerToken = appState.authToken,
                sessionId = appState.activeSessionId,
                questionId = appState.currentQuestionIndex + 1,
                questionText = questionText,
                transcript = "",
                audioBase64 = capture.audioBase64,
                audioDurationSeconds = capture.audioDurationSeconds,
                refreshToken = appState.authRefreshToken,
                onTokenRefreshed = { newToken ->
                    appState.authToken = newToken
                },
            )

            val scoredAnswer = remoteResult.answer
            if (scoredAnswer == null) {
                processingAnswerState.value = false
                interviewAnsweringState.value = false
                processingStageState.value = ""
                processingProgressState.intValue = 0
                showAnswerRetryRequired(
                    remoteResult.failureMessage ?: "We could not score your answer. Check your connection and try again.",
                )
                return@launch
            }

            val displayTranscript = scoredAnswer.transcript.trim()
            val isBlank = SessionScoring.isBlankTranscript(displayTranscript)
            val feedback = when {
                scoredAnswer.retryRequired && isBlank ->
                    scoredAnswer.feedback.ifBlank {
                        "We could not turn this recording into a clear answer. Please retry and speak close to the microphone."
                    }
                else -> scoredAnswer.feedback
            }

            if (scoredAnswer.retryRequired || isBlank) {
                processingAnswerState.value = false
                interviewAnsweringState.value = false
                processingStageState.value = ""
                processingProgressState.intValue = 0
                showAnswerRetryRequired(
                    feedback.ifBlank {
                        "We could not turn this recording into a clear answer. Please retry and speak close to the microphone."
                    },
                )
                return@launch
            }

            // Trust the backend score; only sanitize the model answer display
            val result = scoredAnswer.copy(
                transcript = displayTranscript,
                score = scoredAnswer.score.coerceIn(0, 100),
                feedback = feedback,
                improvedAnswer = sanitizeModelAnswer(
                    scoredAnswer.improvedAnswer.trim(),
                    displayTranscript,
                    true,
                ),
                retryRequired = scoredAnswer.retryRequired,
            )
            var finalResult = enrichWithModelAnswer(questionText, result)
                .let { answer ->
                    val normalizedTranscript = answer.transcript.trim().ifBlank { displayTranscript }
                    answer.copy(
                        transcript = normalizedTranscript,
                        score = if (SessionScoring.isBlankTranscript(normalizedTranscript)) 0 else answer.score,
                    )
                }

            currentAnswerResult = finalResult
            sessionAnswers.add(finalResult)
            val sessionId = appState.activeSessionId.ifBlank { "session-${System.currentTimeMillis()}" }
            appState.saveSessionAnswers(sessionId, sessionAnswers.toList())
            appState.markAnswered(finalResult.score, finalResult.transcript)
            activeTranscript = ""
            speechError = ""
            processingProgressState.intValue = 100
            processingAnswerState.value = false
            interviewAnsweringState.value = false
            processingStageState.value = ""
            muteInterviewRoomSpeech()
            showResult()
            if (appState.authToken.isNotBlank()) {
                refreshRemoteHistory()
            }
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
        speechError = ""
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
        muteInterviewRoomSpeech()
        val result = currentAnswerResult ?: AnswerResult(
            transcript = appState.lastTranscript,
            score = appState.lastScore,
            feedback = "Answer coaching is ready.",
            improvedAnswer = "",
            what = "Situation, action, and result.",
            how = "Keep it short and concrete.",
            why = "Specific proof makes the answer easier to trust.",
            coachingMessage = "Let's review how you did and find ways to make your answer even stronger.",
        )
        coachingMessage = result.coachingMessage
        answerReviewVisibleState.value = true
        modelAnswerVisibleState.value = false
        if (!isInterviewRoomVisible()) {
            showInterview(answering = false)
        }
    }

    private fun showModelAnswerTeaching(
        result: AnswerResult,
        onContinue: () -> Unit,
    ) {
        currentAnswerResult = result
        answerReviewVisibleState.value = true
        openModelAnswerOverlay(result)
        if (!isInterviewRoomVisible()) {
            showInterview(answering = false)
        }
    }

    private fun advanceAfterAnswerReview() {
        dismissResultOverlay()
        resumeInterviewRoomSpeech()
        val sessionId = appState.activeSessionId.ifBlank { "session-${System.currentTimeMillis()}" }
        val answersSnapshot = sessionAnswers.toList()
        appState.saveSessionAnswers(sessionId, answersSnapshot)
        val done = appState.advanceOrComplete()
        if (done) {
            appState.finalizeSessionForId(sessionId, answersSnapshot)
            sessionAnswers.clear()
            scope.launch {
                if (appState.authToken.isNotBlank() && !sessionId.startsWith("session-")) {
                    backend.completeSession(appState.authToken, sessionId)
                }
                refreshRemoteHistory()
                showSessionReport(sessionId)
            }
        } else {
            scope.launch { prepareCurrentQuestionSpeech() }
            showInterview(answering = false, forceRebuild = true)
        }
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
            typeface = interBold
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
        addView(label("STAR COACHING"))
        addView(metricText("SITUATION & TASK", result.what))
        addView(metricText("ACTION", result.how))
        addView(metricText("RESULT & WHY", result.why))
    }

    private fun openLearnMoreUrl(url: String) {
        if (url.isBlank()) return
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            showAppToast("Could not open link.", ToastKind.WARNING)
        }
    }

    private fun learnMoreLinkView(question: com.pollecode.prezzencekotlin.data.InterviewQuestion): TextView {
        return TextView(this).apply {
            text = "Learn more: ${question.resolvedLearnMoreTopic()}"
            textSize = 13f
            setTextColor(accent)
            typeface = interBold
            setPadding(0, dp(8), 0, 0)
            setOnClickListener { openLearnMoreUrl(question.resolvedLearnMoreUrl()) }
        }
    }

    private fun roleScroller() = HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        val row = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL }
        PrezzenceDefaults.roles.forEach { role ->
            row.addView(secondaryButton(role) {
                appState.selectedRole = role
                showOnboardingType()
            }.apply {
                minWidth = dp(190)
                if (role == appState.selectedRole) setBackgroundColor(accent)
            })
        }
        addView(row)
    }

    private fun getInterviewerDrawableId(name: String): Int {
        return when (name.lowercase()) {
            "sophia" -> R.drawable.interviewer_sophia
            "maya" -> R.drawable.interviewer_maya
            "jonas" -> R.drawable.interviewer_jonas
            else -> R.drawable.interviewer_sophia
        }
    }

    private fun staticAvatarCard(interviewer: Interviewer) = FrameLayout(this).apply {
        background = rounded(Color.rgb(24, 23, 39))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(330)).apply {
            setMargins(0, dp(10), 0, dp(10))
        }
        
        // Show static interviewer image
        addView(android.widget.ImageView(this@MainActivity).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            setImageResource(getInterviewerDrawableId(interviewer.name))
        })
        
        // Add overlay with interviewer info
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.argb(100, 0, 0, 0))
            
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                setBackgroundColor(Color.argb(220, 30, 30, 40))
                
                addView(android.widget.ImageView(this@MainActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
                    scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    setImageResource(getInterviewerDrawableId(interviewer.name))
                })
                
                addView(LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        leftMargin = dp(12)
                    }
                    
                    addView(TextView(this@MainActivity).apply {
                        text = "ASKED BY"
                        textSize = 10f
                        setTextColor(Color.rgb(160, 160, 160))
                        typeface = interBold
                    })
                    
                    addView(TextView(this@MainActivity).apply {
                        text = interviewer.name
                        textSize = 16f
                        setTextColor(Color.WHITE)
                        typeface = interBold
                    })
                    
                    addView(TextView(this@MainActivity).apply {
                        text = interviewer.title
                        textSize = 11f
                        setTextColor(Color.rgb(160, 160, 160))
                    })
                })
            })
        })
        
        addView(pill("LIVE", green).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.TOP or Gravity.START
                setMargins(dp(16), dp(16), 0, 0)
            }
        })
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
        
        // Add static image as fallback background
        addView(android.widget.ImageView(this@MainActivity).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            setImageResource(getInterviewerDrawableId(interviewer.name))
            alpha = 0.3f  // Faded background
        })
        
        val avatar = NativeDuixAvatarView(this@MainActivity).apply avatarView@{
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            listener = object : NativeDuixAvatarView.Listener {
                override fun onModelReady(modelName: String) {
                    avatarReadyState.value = true
                    interviewerSpeakingState.value = false
                    if (suppressInterviewSpeech || !live || speechQueued || token != speechGenerationToken) return
                    speechQueued = true
                    speakQuestionThroughAvatar(this@avatarView, questionText, interviewer, token, forceRefresh = false)
                }
                override fun onModelError(modelName: String, message: String?) {
                    avatarReadyState.value = true
                    interviewerSpeakingState.value = false
                    val errorMsg = message ?: "Avatar unavailable"
                    showAppToast("Avatar info: $errorMsg", ToastKind.INFO)
                }
                override fun onSpeechStart(source: String?, modelName: String?) {
                    interviewerSpeakingState.value = true
                }
                override fun onSpeechEnd(source: String?, modelName: String?) {
                    interviewerSpeakingState.value = false
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
                typeface = interBold
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
                typeface = interBold
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
                confirmDeleteSession(session.id, session.role, PrezzenceTab.PROGRESS)
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
            typeface = interBold
        })
    }

    private fun title(text: String, size: Int) = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(Color.WHITE)
        typeface = interBold
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
        typeface = interBold
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
        typeface = interBold
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
        typeface = interBold
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
        typeface = interMedium
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
        stopCoachingAudioFallback()
        activeAvatar?.release()
        activeAvatar = null
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








