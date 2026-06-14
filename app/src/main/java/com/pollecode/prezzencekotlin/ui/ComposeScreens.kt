package com.pollecode.prezzencekotlin.ui

import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.viewinterop.AndroidView
import com.pollecode.prezzencekotlin.R

private val Bg = Color(0xFF0A0A0F)
private val Surface = Color(0xFF12121A)
private val Card = Color(0xFF1C1C2E)
private val Accent = Color(0xFF6C63FF)
private val Cyan = Color(0xFF24C8F2)
private val Mint = Color(0xFF19D5B2)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF8A8A9A)
private val Border = Color(0xFF2A2A3E)

private val PrezzenceTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 58.sp, lineHeight = 58.sp, letterSpacing = 0.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = 0.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 23.sp, letterSpacing = 0.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 26.sp, letterSpacing = 0.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.sp),
)

@Composable
fun PrezzenceLandingScreen(
    onStart: () -> Unit,
    onSignIn: () -> Unit,
    authLoading: Boolean = false,
    checking: Boolean = false,
) {
    PrezzenceTheme {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF0A0911))) {
            val screenH = maxHeight
            val compactHeight = screenH < 760.dp
            // RN scaling: BASE_WIDTH=430, cap at 1.12x
            val scale = (maxWidth / 430.dp).coerceAtMost(1.12f)
            fun sz(v: Int) = (v * scale).dp
            fun fs(v: Int) = (v * scale).sp

            // Full-screen background image + gradient layers
            LandingBackdrop()

            // Decorative geometric overlay
            LandingBackgroundDecor()

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top content area
                Column(
                    modifier = Modifier.padding(
                        start = sz(32),
                        end = sz(32),
                        top = if (compactHeight) sz(32) else sz(56),
                        bottom = 0.dp,
                    ),
                ) {
                    BrandRow(compactText = true)
                    Spacer(Modifier.height(if (compactHeight) sz(36) else sz(48)))
                    Text(
                        text = "V 1.0 \u00B7 SEASON ONE",
                        color = Color(0xFF675EFB),
                        fontSize = fs(16),
                        letterSpacing = (5.2f * scale).sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(sz(30)))
                    Text(
                        text = "Practice\nthe room.",
                        color = Color(0xFFF0ECFF),
                        fontSize = fs(76),
                        lineHeight = fs(74),
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-3.2f * scale).sp,
                    )
                    Spacer(Modifier.height(sz(8)))
                    GradientHeadline(fontSize = fs(68))
                    Spacer(Modifier.height(sz(34)))
                    Text(
                        text = "Practice with voice-led AI interviewers that ask, react, score, and coach your answers before the real interview.",
                        color = Color(0xFF8E8AA7),
                        fontSize = fs(18),
                        lineHeight = fs(28),
                    )
                }

                // Footer with button
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(start = sz(28), end = sz(28), bottom = if (compactHeight) sz(24) else sz(56)),
                ) {
                    // Purple halo glow behind button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(sz(40))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0x606B61FF),
                                        Color.Transparent,
                                    )
                                )
                            )
                    )
                    LandingPrimaryButton("Start Practicing", onClick = onStart, height = sz(74))
                    Spacer(Modifier.height(sz(28)))
                    Text(
                        text = "I already have an account \u2192",
                        color = Color(0xFF858198),
                        fontSize = fs(17),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onSignIn),
                    )

                    // Loading overlay
                    if (checking || authLoading) {
                        Spacer(Modifier.height(sz(18)))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().heightIn(min = sz(34)),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(sz(16)),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(sz(10)))
                            Text(
                                "Opening Prezzence...",
                                color = Color(0xFFC8C4DD),
                                fontSize = fs(13),
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun PrezzenceSignUpScreen(
    loading: Boolean,
    error: String?,
    successEmail: String?,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onSubmit: (String, String, String) -> Unit,
    onGoogleSignUp: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(successEmail ?: "") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val visibleError = localError ?: error

    PrezzenceTheme {
        Box(Modifier.fillMaxSize().background(Bg)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 44.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    AuthCircleButton("<", onBack)
                    Spacer(Modifier.weight(1f))
                    PrezzenceMark(Modifier.size(32.dp))
                }

                Spacer(Modifier.height(26.dp))

                if (successEmail != null) {
                    VerifyEmailPanel(successEmail = successEmail, onSignIn = onSignIn)
                    return@Column
                }

                Text("Join Prezzence.", color = TextPrimary, fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text("Start your path to interview readiness.", color = TextSecondary, fontSize = 16.sp, lineHeight = 24.sp, maxLines = 1)

                Spacer(Modifier.height(24.dp))

                if (visibleError != null) {
                    ErrorBox(visibleError)
                    Spacer(Modifier.height(16.dp))
                }

                AuthField("Full Name", name, { name = it; localError = null }, "Enter your name", AuthIcon.User, enabled = !loading)
                Spacer(Modifier.height(16.dp))
                AuthField("Email Address", email, { email = it; localError = null }, "name@example.com", AuthIcon.Mail, keyboardType = KeyboardType.Email, enabled = !loading)
                Spacer(Modifier.height(16.dp))
                AuthField(
                    label = "Password",
                    value = password,
                    onValueChange = { password = it; localError = null },
                    placeholder = "Create a password",
                    leading = AuthIcon.Lock,
                    trailingIcon = AuthIcon.Eye,
                    onTrailingClick = { showPassword = !showPassword },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardType = KeyboardType.Password,
                    enabled = !loading,
                )
                Spacer(Modifier.height(12.dp))
                PasswordRequirements(password)

                Spacer(Modifier.height(22.dp))
                AuthPrimaryButton(if (loading) "Loading..." else "Create Account", !loading) {
                    when {
                        name.isBlank() || email.isBlank() || password.isBlank() -> localError = "Please fill in all fields"
                        password.length < 8 -> localError = "Password must be at least 8 characters"
                        else -> onSubmit(name.trim(), email.trim(), password)
                    }
                }

                Spacer(Modifier.height(14.dp))
                SocialAuthButton("Continue with Google", onGoogleSignUp)

                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Already have an account? ", color = TextSecondary, fontSize = 16.sp)
                    Text("Sign In", color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onSignIn))
                }

                Spacer(Modifier.height(34.dp))
                Text(
                    text = "By creating an account, you agree to Prezzence's",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Text("Terms of Service", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onTerms))
                    Text(" and ", color = TextSecondary, fontSize = 12.sp)
                    Text("Privacy Policy", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onPrivacy))
                }
            }
        }
    }
}

@Composable
fun PrezzenceSignInScreen(
    loading: Boolean,
    error: String?,
    successMessage: String?,
    onClose: () -> Unit,
    onForgotPassword: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onSignUp: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val visibleError = localError ?: error

    PrezzenceTheme {
        Box(Modifier.fillMaxSize().background(Bg)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 44.dp),
            ) {
                AuthHeader(onLeft = onClose, leftLabel = "x")
                Spacer(Modifier.height(26.dp))

                Text("Welcome Back", color = TextPrimary, fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text("Sign in to continue your journey.", color = TextSecondary, fontSize = 16.sp, lineHeight = 24.sp, maxLines = 1)
                Spacer(Modifier.height(24.dp))

                if (successMessage != null) {
                    SuccessBox(successMessage)
                    Spacer(Modifier.height(16.dp))
                }
                if (visibleError != null) {
                    ErrorBox(visibleError)
                    Spacer(Modifier.height(16.dp))
                }

                AuthField("Email Address", email, { email = it; localError = null }, "name@example.com", AuthIcon.Mail, keyboardType = KeyboardType.Email, enabled = !loading)
                Spacer(Modifier.height(16.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Password", color = TextPrimary.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("Forgot Password?", color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onForgotPassword))
                    }
                    Spacer(Modifier.height(12.dp))
                    AuthFieldBody(
                        value = password,
                        onValueChange = { password = it; localError = null },
                        placeholder = "Enter your password",
                        leading = AuthIcon.Lock,
                        trailingIcon = AuthIcon.Eye,
                        onTrailingClick = { showPassword = !showPassword },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardType = KeyboardType.Password,
                        enabled = !loading,
                    )
                }

                Spacer(Modifier.height(24.dp))
                AuthPrimaryButton(if (loading) "Loading..." else "Sign In", !loading) {
                    when {
                        email.isBlank() || password.isBlank() -> localError = "Please fill in all fields"
                        else -> onSignIn(email.trim(), password)
                    }
                }

                Spacer(Modifier.height(24.dp))
                AuthDivider("or continue with")
                Spacer(Modifier.height(20.dp))
                SocialAuthButton("Google", onGoogleSignIn)

                Spacer(Modifier.height(40.dp))
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Don't have an account? ", color = TextSecondary, fontSize = 16.sp)
                    Text("Sign Up", color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onSignUp))
                }
            }
        }
    }
}

@Composable
fun PrezzenceForgotPasswordScreen(
    loading: Boolean,
    error: String?,
    sent: Boolean,
    onBack: () -> Unit,
    onSubmit: (String) -> Unit,
    onSignIn: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val visibleError = localError ?: error

    PrezzenceTheme {
        Box(Modifier.fillMaxSize().background(Bg)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 44.dp),
            ) {
                AuthHeader(onLeft = onBack, leftLabel = "<")
                Spacer(Modifier.height(26.dp))

                Text("Forgot Password", color = TextPrimary, fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (sent) "Reset instructions sent to your inbox." else "Enter your email and we'll send reset instructions.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 20.sp,
                    maxLines = 1,
                )
                Spacer(Modifier.height(20.dp))

                RecoveryHero()
                Spacer(Modifier.height(24.dp))

                if (!sent) {
                    AuthField("Email Address", email, { email = it; localError = null }, "name@example.com", AuthIcon.Mail, keyboardType = KeyboardType.Email, enabled = !loading)
                    Spacer(Modifier.height(24.dp))
                    AuthPrimaryButton(if (loading) "Loading..." else "Send Instructions", !loading) {
                        val normalized = email.trim().lowercase()
                        if (normalized.isBlank()) localError = "Enter the email address on your account." else onSubmit(normalized)
                    }
                    if (visibleError != null) {
                        Spacer(Modifier.height(14.dp))
                        Text(visibleError, color = Color(0xFFFF6B7A), fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0x1A00D68F))
                            .border(1.dp, Color(0x3300D68F), RoundedCornerShape(24.dp))
                            .padding(24.dp),
                    ) {
                        Text("Check your inbox", color = Color(0xFF00D68F), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(8.dp))
                        Text("We sent password reset instructions to your email address.", color = TextSecondary, fontSize = 15.sp, lineHeight = 22.sp)
                    }
                    Spacer(Modifier.height(24.dp))
                    AuthSecondaryButton("Back to Sign In", onSignIn)
                }

                Spacer(Modifier.height(40.dp))
                Text("Back to Sign In", color = Accent, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().clickable(onClick = onSignIn))
            }
        }
    }
}


@Composable
fun PrezzenceResetPasswordScreen(
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSubmit: (String) -> Unit,
    onSignIn: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val visibleError = localError ?: error

    PrezzenceTheme {
        Box(Modifier.fillMaxSize().background(Bg)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 44.dp),
            ) {
                AuthHeader(onLeft = onBack, leftLabel = "<")
                Spacer(Modifier.height(26.dp))
                Text("Reset password.", color = TextPrimary, fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text("Choose a strong new password for your account.", color = TextSecondary, fontSize = 12.sp, lineHeight = 20.sp, maxLines = 1)
                Spacer(Modifier.height(24.dp))

                AuthField(
                    label = "New Password",
                    value = password,
                    onValueChange = { password = it; localError = null },
                    placeholder = "Enter new password",
                    leading = AuthIcon.Lock,
                    trailingIcon = AuthIcon.Eye,
                    onTrailingClick = { showPassword = !showPassword },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardType = KeyboardType.Password,
                    enabled = !loading,
                )
                Spacer(Modifier.height(16.dp))
                AuthField(
                    label = "Confirm New Password",
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; localError = null },
                    placeholder = "Repeat your password",
                    leading = AuthIcon.Lock,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardType = KeyboardType.Password,
                    enabled = !loading,
                )
                Spacer(Modifier.height(24.dp))
                AuthPrimaryButton(if (loading) "Loading..." else "Reset Password", !loading) {
                    when {
                        password.length < 8 -> localError = "Password must be at least 8 characters."
                        password != confirmPassword -> localError = "Passwords do not match."
                        else -> onSubmit(password)
                    }
                }
                if (visibleError != null) {
                    Spacer(Modifier.height(14.dp))
                    Text(visibleError, color = Color(0xFFFF6B7A), fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(40.dp))
                Text("Cancel and return to Sign In", color = TextSecondary.copy(alpha = 0.8f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().clickable(onClick = onSignIn))
            }
        }
    }
}

@Composable
fun PrezzenceLanguageScreen(
    selectedLanguage: String,
    onBack: () -> Unit,
    onLanguageSelected: (String) -> Unit,
    onContinue: () -> Unit,
) {
    var search by remember { mutableStateOf("") }
    val languages = listOf(
        LanguageOption("en-US", "English", "English", "US"),
        LanguageOption("es", "Spanish", "Espanol", "ES"),
        LanguageOption("fr", "French", "Francais", "FR"),
        LanguageOption("de", "German", "Deutsch", "DE"),
        LanguageOption("it", "Italian", "Italiano", "IT"),
        LanguageOption("pt", "Portuguese", "Portugues", "BR"),
        LanguageOption("zh", "Chinese", "Zhongwen", "CN"),
        LanguageOption("ja", "Japanese", "Nihongo", "JP"),
        LanguageOption("ko", "Korean", "Hangugeo", "KR"),
        LanguageOption("ar", "Arabic", "Al-Arabiyyah", "SA"),
        LanguageOption("hi", "Hindi", "Hindi", "IN"),
    )
    val filtered = languages.filter { item ->
        item.name.contains(search, ignoreCase = true) || item.native.contains(search, ignoreCase = true)
    }

    PrezzenceTheme {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 16.dp),
                ) {
                    AuthCircleButton("<", onBack)
                    Text("Language", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(44.dp))
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp, end = 24.dp, bottom = 120.dp),
                ) {
                    Spacer(Modifier.height(12.dp))
                    Text("Choose your interview language", color = TextPrimary, fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Text("Sign in to continue your journey.", color = TextSecondary, fontSize = 16.sp, lineHeight = 24.sp)
                    Spacer(Modifier.height(20.dp))
                    SearchField(search, { search = it })
                    Spacer(Modifier.height(24.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        filtered.forEach { item ->
                            LanguageCard(
                                option = item,
                                selected = selectedLanguage == item.id || (selectedLanguage.startsWith(item.id) && item.id.length == 2),
                                onClick = { onLanguageSelected(item.id) },
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(start = 24.dp, end = 24.dp, bottom = 34.dp, top = 14.dp),
            ) {
                PrimaryPillButton("Continue", onContinue)
            }
        }
    }
}

@Composable
fun PrezzenceVerifyScreen(
    onEnter: () -> Unit,
) {
    PrezzenceTheme {
        Box(Modifier.fillMaxSize().background(Bg)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF0A0A0F), Color(0xFF131321), Color(0xFF0A0A0F))))
            )
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 88.dp)
                    .size(220.dp)
                    .clip(CircleShape)
                    .background(Accent.copy(alpha = 0.05f))
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 32.dp, end = 32.dp, top = 92.dp, bottom = 36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(146.dp)
                            .clip(CircleShape)
                            .background(Color(0x2219D5B2))
                            .border(2.dp, Mint, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .matchParentSize()
                                .background(Brush.radialGradient(listOf(Mint.copy(alpha = 0.20f), Color.Transparent)))
                        )
                        ShieldIcon(Modifier.size(72.dp))
                    }
                }
                Spacer(Modifier.height(34.dp))
                Text("Email Confirmed", color = TextPrimary, fontSize = 40.sp, lineHeight = 42.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                Text(
                    "Your email is confirmed. Continue to set up your interview practice profile.",
                    color = TextSecondary,
                    fontSize = 20.sp,
                    lineHeight = 30.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Spacer(Modifier.height(30.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0x0FFFFFFF))
                        .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(24.dp))
                        .padding(16.dp),
                ) {
                    VerifyPerk("Account email confirmed")
                    Spacer(Modifier.height(12.dp))
                    VerifyPerk("Interview practice profile ready")
                }
                Spacer(Modifier.height(30.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .clip(RoundedCornerShape(35.dp))
                        .background(Brush.horizontalGradient(listOf(Accent, Color(0xFF8E7DFF))))
                        .clickable(onClick = onEnter),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Text("Enter Prezzence", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.width(12.dp))
                        Text(">", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PrezzenceHomeScreen(
    completedSessions: Int,
    readinessScore: Int,
    signedInAs: String?,
    firstName: String?,
    hasIncompleteSession: Boolean,
    currentQuestionIndex: Int,
    totalQuestions: Int,
    unreadNotifications: Int,
    improvementDelta: Int = 0,
    onStart: () -> Unit,
    onContinueSession: () -> Unit,
    onNewSession: () -> Unit,
    onQuestions: () -> Unit,
    onProgress: () -> Unit,
    onSettings: () -> Unit,
    onQa: () -> Unit,
    onNotifications: () -> Unit,
) {
    val score = readinessScore.coerceIn(0, 100)
    val readinessLabel = when {
        score >= 75 -> "Interview Ready"
        score >= 45 -> "Building Confidence"
        score >= 25 -> "Building Momentum"
        score > 0 -> "Foundation Built"
        else -> "Ready to Practice"
    }
    val milestoneTargets = listOf(1, 3, 5, 10, 20, 50)
    val nextMilestoneTarget = milestoneTargets.firstOrNull { completedSessions < it }
    val milestoneDesc = when {
        nextMilestoneTarget == null -> "Keep your streak alive with another practice session."
        completedSessions == 0 -> "Complete your first practice session to start your streak."
        else -> "${nextMilestoneTarget - completedSessions} more ${if (nextMilestoneTarget - completedSessions == 1) "session" else "sessions"} to reach $nextMilestoneTarget completed sessions."
    }
    val displayName = firstName?.takeIf { it.isNotBlank() } ?: "!"

    val fadeIn by animateFloatAsState(targetValue = 1f, animationSpec = tween(600))
    LaunchedEffect(Unit) { }
    PrezzenceTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Bg)
                .alpha(fadeIn)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 96.dp),
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 20.dp),
            ) {
                BrandRow(compactText = true)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Card.copy(alpha = 0.6f))
                        .clickable(onClick = onNotifications),
                    contentAlignment = Alignment.Center,
                ) {
                    // Bell icon via canvas
                    Canvas(Modifier.size(22.dp)) {
                        val c = Accent
                        val stroke = Stroke(width = 2.4f, cap = StrokeCap.Round)
                        // Bell body arc
                        drawArc(c, startAngle = 200f, sweepAngle = 140f, useCenter = false,
                            topLeft = Offset(size.width * 0.16f, size.height * 0.04f),
                            size = Size(size.width * 0.68f, size.height * 0.64f), style = stroke)
                        // Bell sides
                        drawLine(c, Offset(size.width * 0.16f, size.height * 0.55f), Offset(size.width * 0.10f, size.height * 0.78f), strokeWidth = 2.4f, cap = StrokeCap.Round)
                        drawLine(c, Offset(size.width * 0.84f, size.height * 0.55f), Offset(size.width * 0.90f, size.height * 0.78f), strokeWidth = 2.4f, cap = StrokeCap.Round)
                        // Bell bottom line
                        drawLine(c, Offset(size.width * 0.10f, size.height * 0.78f), Offset(size.width * 0.90f, size.height * 0.78f), strokeWidth = 2.4f, cap = StrokeCap.Round)
                        // Clapper
                        drawCircle(c, radius = size.minDimension * 0.07f, center = Offset(size.width * 0.5f, size.height * 0.92f))
                    }
                    if (unreadNotifications > 0) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF4757))
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (unreadNotifications > 9) "9+" else unreadNotifications.toString(),
                                color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
            }

            // Hero
            Column(
                modifier = Modifier.padding(horizontal = 24.dp).padding(top = 20.dp, bottom = 32.dp),
            ) {
                Text(
                    text = "Welcome, $displayName",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 34.sp,
                    letterSpacing = (-1).sp,
                    maxLines = 1,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (signedInAs != null) {
                        if (completedSessions > 0)
                            "You've completed $completedSessions ${if (completedSessions == 1) "session" else "sessions"}. Ready for the next one?"
                        else
                            "Experience a bespoke interview experience."
                    } else {
                        "Sign in to continue your journey."
                    },
                    color = TextSecondary,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                )
                Spacer(Modifier.height(20.dp))
                if (hasIncompleteSession) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(Accent.copy(alpha = 0.3f))
                                .border(2.dp, Bg, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("P", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Box(
                            modifier = Modifier.offset(x = (-14).dp).size(36.dp).clip(CircleShape)
                                .background(Color(0xFF00D68F).copy(alpha = 0.3f))
                                .border(2.dp, Bg, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("A", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("2 interviewers ready", color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("0 interviewers ready", color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Resume incomplete session card
            if (hasIncompleteSession) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 22.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Card.copy(alpha = 0.82f))
                        .border(1.dp, Accent.copy(alpha = 0.34f), RoundedCornerShape(26.dp))
                        .padding(18.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(44.dp).clip(RoundedCornerShape(16.dp))
                                .background(Accent.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Canvas(Modifier.size(22.dp)) {
                                val c = Accent
                                // Play circle
                                drawCircle(c, radius = size.minDimension * 0.44f, center = Offset(size.width / 2f, size.height / 2f), style = Stroke(width = 2.2f))
                                // Play triangle
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(size.width * 0.40f, size.height * 0.30f)
                                    lineTo(size.width * 0.72f, size.height * 0.50f)
                                    lineTo(size.width * 0.40f, size.height * 0.70f)
                                    close()
                                }
                                drawPath(path, c)
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Continue unfinished interview?", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
                            Text(
                                "Question ${minOf(currentQuestionIndex + 1, totalQuestions)} of $totalQuestions",
                                color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(22.dp))
                                .background(Accent).clickable(onClick = onContinueSession),
                            contentAlignment = Alignment.Center,
                        ) { Text("Continue", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black) }
                        Box(
                            Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(22.dp))
                                .background(Color.White.copy(alpha = 0.08f)).clickable(onClick = onNewSession),
                            contentAlignment = Alignment.Center,
                        ) { Text("Start over", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold) }
                    }
                }
            }

            // Readiness card
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Card.copy(alpha = 0.4f))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(32.dp))
            ) {
                Box(Modifier.matchParentSize().clip(RoundedCornerShape(32.dp)).background(
                    Brush.verticalGradient(listOf(Accent.copy(alpha = 0.08f), Color.Transparent))
                ))
                Column(Modifier.padding(24.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Column {
                            Text("CURRENT READINESS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(readinessLabel, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                        Text("$score%", color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(16.dp))
                    // Progress bar
                    Box(
                        Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        if (score > 0) {
                            Box(
                                Modifier
                                    .fillMaxWidth(score / 100f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Accent)
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            if (completedSessions == 0) "Baseline in progress"
                            else if (improvementDelta > 0) "+$improvementDelta since first session"
                            else if (improvementDelta < 0) "$improvementDelta since first session"
                            else "Baseline captured",
                            color = Accent, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (score == 0) "Complete one interview with clear audio to unlock personalized coaching."
                        else "Keep practicing to improve your interview signal.",
                        color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp,
                    )
                }
            }

            // Start new session CTA
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp)
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(RoundedCornerShape(34.dp))
                    .background(Accent)
                    .clickable(onClick = onStart),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text("Start New Session", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(12.dp))
                    Canvas(Modifier.size(24.dp)) {
                        drawLine(Color.White, Offset(size.width * 0.22f, size.height * 0.5f), Offset(size.width * 0.72f, size.height * 0.5f), strokeWidth = 3f, cap = StrokeCap.Round)
                        drawLine(Color.White, Offset(size.width * 0.52f, size.height * 0.28f), Offset(size.width * 0.72f, size.height * 0.5f), strokeWidth = 3f, cap = StrokeCap.Round)
                        drawLine(Color.White, Offset(size.width * 0.52f, size.height * 0.72f), Offset(size.width * 0.72f, size.height * 0.5f), strokeWidth = 3f, cap = StrokeCap.Round)
                    }
                }
            }

            // Action grid
            Row(
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // New interview card
                Column(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp))
                        .background(Card)
                        .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(24.dp))
                        .clickable(onClick = onStart)
                        .padding(20.dp),
                ) {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF00D68F).copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.size(24.dp)) {
                            val c = Color(0xFF00D68F)
                            drawCircle(c, radius = size.minDimension * 0.44f, center = Offset(size.width / 2f, size.height / 2f), style = Stroke(width = 2.2f))
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(size.width * 0.40f, size.height * 0.30f)
                                lineTo(size.width * 0.72f, size.height * 0.50f)
                                lineTo(size.width * 0.40f, size.height * 0.70f)
                                close()
                            }
                            drawPath(path, c)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("New interview", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text("General", color = TextSecondary, fontSize = 12.sp)
                }
                // Questions card
                Column(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp))
                        .background(Card)
                        .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(24.dp))
                        .clickable(onClick = onQuestions)
                        .padding(20.dp),
                ) {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                            .background(Accent.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.size(24.dp)) {
                            val c = Accent
                            // Target/crosshair icon
                            drawCircle(c, radius = size.minDimension * 0.44f, center = Offset(size.width / 2f, size.height / 2f), style = Stroke(width = 2.2f))
                            drawCircle(c, radius = size.minDimension * 0.22f, center = Offset(size.width / 2f, size.height / 2f), style = Stroke(width = 2.2f))
                            drawCircle(c, radius = size.minDimension * 0.07f, center = Offset(size.width / 2f, size.height / 2f))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Questions", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text("5 modes available", color = TextSecondary, fontSize = 12.sp)
                }
            }

            // Milestone card
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFFFB020).copy(alpha = 0.05f))
                    .border(1.dp, Color(0xFFFFB020).copy(alpha = 0.10f), RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Canvas(Modifier.size(24.dp)) {
                    val c = Color(0xFFFFB020)
                    // Trophy cup outline
                    drawRoundRect(c, Offset(size.width * 0.22f, size.height * 0.12f), Size(size.width * 0.56f, size.height * 0.52f), CornerRadius(4f), style = Stroke(width = 2.4f, cap = StrokeCap.Round))
                    drawLine(c, Offset(size.width * 0.10f, size.height * 0.20f), Offset(size.width * 0.22f, size.height * 0.20f), strokeWidth = 2.4f, cap = StrokeCap.Round)
                    drawLine(c, Offset(size.width * 0.78f, size.height * 0.20f), Offset(size.width * 0.90f, size.height * 0.20f), strokeWidth = 2.4f, cap = StrokeCap.Round)
                    drawLine(c, Offset(size.width * 0.50f, size.height * 0.64f), Offset(size.width * 0.50f, size.height * 0.80f), strokeWidth = 2.4f, cap = StrokeCap.Round)
                    drawLine(c, Offset(size.width * 0.32f, size.height * 0.86f), Offset(size.width * 0.68f, size.height * 0.86f), strokeWidth = 2.4f, cap = StrokeCap.Round)
                }
                Column(Modifier.weight(1f)) {
                    Text("Next milestone", color = Color(0xFFFFB020), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(2.dp))
                    Text(milestoneDesc, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
        }
    }
}

@Composable
private fun PrezzenceTheme(content: @Composable () -> Unit) {
    val currentDensity = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(currentDensity.density, fontScale = 1f)) {
        MaterialTheme(
            colorScheme = androidx.compose.material3.darkColorScheme(
                primary = Accent,
                background = Bg,
                surface = Surface,
            ),
            typography = PrezzenceTypography,
            content = content,
        )
    }
}

@Composable
private fun LandingBackdrop() {
    Box(Modifier.fillMaxSize().background(Color(0xFF0A0911))) {
        Image(
            painter = painterResource(R.drawable.interview_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.65f),
        )
        // Dark gradient overlay - heavier at bottom so content is readable
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color(0x220A0911),
                            0.45f to Color(0x440A0911),
                            0.75f to Color(0xCC05050B),
                            1f to Color(0xFF0A0911),
                        ),
                    ),
                ),
        )
        // Subtle center radial teal glow
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(Color(0x1524C8F2), Color.Transparent),
                        center = Offset.Unspecified,
                        radius = 800f,
                    ),
                ),
        )
    }
}

@Composable
private fun LandingBackgroundDecor() {
    // All decorative non-interactive overlays drawn on top of backdrop
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Horizon lines across full width
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineColor = Color(0xFF58548F).copy(alpha = 0.10f)
            listOf(0.14f, 0.29f, 0.53f).forEach { yFraction ->
                drawLine(
                    color = lineColor,
                    start = Offset(0f, size.height * yFraction),
                    end = Offset(size.width, size.height * yFraction),
                    strokeWidth = 1f,
                )
            }

            // Floating top preview card — subtle glassmorphic box
            val cardW = size.width * 0.35f
            val cardH = size.height * 0.11f
            val cardLeft = (size.width - cardW) / 2f
            val cardTop = size.height * 0.19f
            val cardRadius = CornerRadius(16.dp.toPx())
            drawRoundRect(
                color = Color(0xFF605AA8).copy(alpha = 0.14f),
                topLeft = Offset(cardLeft, cardTop),
                size = Size(cardW, cardH),
                cornerRadius = cardRadius,
                style = Stroke(width = 1f),
            )
            // Inner card shimmer lines
            val lineInset = cardLeft + 18.dp.toPx()
            listOf(0.24f, 0.49f, 0.74f).forEach { fraction ->
                val lineWidth = if (fraction == 0.24f) cardW * 0.68f
                    else if (fraction == 0.49f) cardW * 0.54f else cardW * 0.62f
                drawLine(
                    color = Color(0xFF6E68C2).copy(alpha = 0.12f),
                    start = Offset(lineInset, cardTop + cardH * fraction),
                    end = Offset(lineInset + lineWidth, cardTop + cardH * fraction),
                    strokeWidth = 1f,
                )
            }

            // Center vertical glow strip
            val glowW = size.width * 0.58f
            val glowH = size.height * 0.72f
            val glowLeft = (size.width - glowW) / 2f
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color(0x33493AA8),
                        Color.Transparent,
                    ),
                    startY = size.height * 0.22f,
                    endY = size.height * 0.78f,
                ),
                topLeft = Offset(glowLeft, size.height * 0.22f),
                size = Size(glowW, glowH),
            )

            // Center phone device frame outline
            val frameW = size.width * 0.40f
            val frameH = size.height * 0.50f
            val frameLeft = (size.width - frameW) / 2f
            val frameTop = size.height * 0.40f
            drawRoundRect(
                color = Color(0xFF605AA8).copy(alpha = 0.14f),
                topLeft = Offset(frameLeft, frameTop),
                size = Size(frameW, frameH),
                cornerRadius = CornerRadius(28.dp.toPx()),
                style = Stroke(width = 1f),
            )
        }

        // Left tilted side frame (rotate 8 degrees)
        Box(
            modifier = Modifier
                .width(108.dp)
                .fillMaxHeight(0.65f)
                .align(Alignment.BottomStart)
                .offset(x = (-4).dp, y = (-24).dp)
                .rotate(8f)
                .border(1.dp, Color(0xFF4C4783).copy(alpha = 0.15f), RoundedCornerShape(26.dp))
        )

        // Right tilted side frame (rotate -8 degrees)
        Box(
            modifier = Modifier
                .width(114.dp)
                .fillMaxHeight(0.67f)
                .align(Alignment.BottomEnd)
                .offset(x = 8.dp, y = (-26).dp)
                .rotate(-8f)
                .border(1.dp, Color(0xFF4C4783).copy(alpha = 0.15f), RoundedCornerShape(26.dp))
        )

        // Bottom left corner mask (hides where rotated frame bleeds past screen)
        Box(
            modifier = Modifier
                .size(width = 110.dp, height = 110.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-12).dp, y = 12.dp)
                .background(Color(0xFF0A0911))
        )

        // Bottom right corner mask
        Box(
            modifier = Modifier
                .size(width = 110.dp, height = 110.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 12.dp, y = 12.dp)
                .background(Color(0xFF0A0911))
        )
    }
}

@Composable
private fun BrandRow(modifier: Modifier = Modifier, compactText: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Image(
            painter = painterResource(R.drawable.prezzence_icon),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(if (compactText) 34.dp else 40.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = "Prezzence",
            color = TextPrimary,
            fontSize = if (compactText) 23.sp else 32.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun GradientHeadline(fontSize: androidx.compose.ui.unit.TextUnit = 68.sp) {
    val topBrush = Brush.horizontalGradient(listOf(Color(0xFF6A67FF), Color(0xFF5488FF), Color(0xFF36B8E8)))
    val bottomBrush = Brush.horizontalGradient(listOf(Color(0xFF31B9E7), Color(0xFF13D3A6)))
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(brush = topBrush)) { append("Own the\n") }
            withStyle(SpanStyle(brush = bottomBrush)) { append("room.") }
        },
        fontSize = fontSize,
        lineHeight = fontSize,
        fontWeight = FontWeight.Black,
    )
}

@Composable
private fun PrimaryPillButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Accent, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun LandingPrimaryButton(
    label: String,
    onClick: () -> Unit,
    height: Dp = 64.dp,
) {
    val shape = RoundedCornerShape(height / 2)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .shadow(
                elevation = 18.dp,
                shape = shape,
                ambientColor = Color(0xFF685FFF),
                spotColor = Color(0xFF685FFF),
            )
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF7062FF), Color(0xFF564FF8))
                )
            )
            .border(
                width = 1.dp,
                color = Color(0xFFAFA5FF).copy(alpha = 0.55f),
                shape = shape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.width(12.dp))
            // Chevron right icon
            Canvas(Modifier.size(24.dp)) {
                drawLine(
                    Color.White,
                    Offset(size.width * 0.72f, size.height * 0.50f),
                    Offset(size.width * 0.38f, size.height * 0.28f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    Color.White,
                    Offset(size.width * 0.72f, size.height * 0.50f),
                    Offset(size.width * 0.38f, size.height * 0.72f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun VerifyEmailPanel(successEmail: String, onSignIn: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(118.dp).clip(CircleShape).background(Color(0x1400D68F)))
            Box(
                modifier = Modifier
                    .size(98.dp)
                    .clip(CircleShape)
                    .background(Color(0x1F00D68F))
                    .border(2.dp, Color(0xFF00D68F), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AuthIconView(AuthIcon.Mail, Modifier.size(42.dp))
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            "Verify Your Email",
            color = TextPrimary,
            fontSize = 32.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Open the confirmation email we sent you. Once confirmed, return here to continue.",
            color = TextSecondary,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        Spacer(Modifier.height(12.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Sent to",
                color = TextSecondary.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x0FFFFFFF))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    successEmail,
                    color = Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "Still waiting for email confirmation",
            color = TextPrimary.copy(alpha = 0.86f),
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(14.dp))
        AuthPrimaryButton("I Verified My Email", true, onSignIn)
        Spacer(Modifier.height(12.dp))
        AuthSecondaryButton("Go to Sign In", onSignIn)
        Spacer(Modifier.height(14.dp))
        Text(
            "Didn't get the email? Check spam or resend from your inbox flow.",
            color = TextSecondary.copy(alpha = 0.68f),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun ErrorBox(message: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x1AFF4757))
            .border(1.dp, Color(0x33FF4757), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text("!", color = Color(0xFFFF4757), fontSize = 16.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.width(8.dp))
        Text(message, color = Color(0xFFFF4757), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leading: AuthIcon,
    trailingIcon: AuthIcon? = null,
    onTrailingClick: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
) {
    Column {
        Text(label, color = TextPrimary.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        AuthFieldBody(value, onValueChange, placeholder, leading, trailingIcon, onTrailingClick, visualTransformation, keyboardType, enabled)
    }
}

@Composable
private fun AuthFieldBody(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leading: AuthIcon,
    trailingIcon: AuthIcon? = null,
    onTrailingClick: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x08FFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp),
    ) {
        AuthIconView(leading, Modifier.size(24.dp))
        Spacer(Modifier.width(18.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) Text(placeholder, color = Color.White.copy(alpha = 0.2f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                innerTextField()
            },
        )
        if (trailingIcon != null && onTrailingClick != null) {
            Box(Modifier.size(30.dp).clickable(onClick = onTrailingClick), contentAlignment = Alignment.Center) {
                AuthIconView(trailingIcon, Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun PasswordRequirements(password: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PasswordRequirement("At least 8 characters", password.length >= 8, Modifier.weight(1f))
            PasswordRequirement("One uppercase letter", password.any { it.isUpperCase() }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PasswordRequirement("One number", password.any { it.isDigit() }, Modifier.weight(1f))
            PasswordRequirement("One special character", password.any { "!@#$%^&*".contains(it) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PasswordRequirement(label: String, met: Boolean, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        RequirementCircle(met, Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            color = if (met) TextPrimary else TextSecondary.copy(alpha = 0.6f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            softWrap = false,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AuthPrimaryButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    PrezzencePrimaryButton(label = label, enabled = enabled, onClick = onClick)
}
@Composable
private fun SocialAuthButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(Color.Transparent)
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(27.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text("G", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(12.dp))
            Text(label, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AuthHeader(onLeft: () -> Unit, leftLabel: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        AuthCircleButton(leftLabel, onLeft)
        Spacer(Modifier.weight(1f))
        PrezzenceMark(Modifier.size(32.dp))
    }
}

@Composable
private fun AuthCircleButton(label: String, onClick: () -> Unit) {
    PrezzenceNavButton(
        icon = if (label == "x") PrezzenceNavIcon.Close else PrezzenceNavIcon.Back,
        onClick = onClick,
    )
}

@Composable
private fun AuthDivider(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.weight(1f).height(1.dp).background(Color(0x1AFFFFFF)))
        Text(label, color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 20.dp))
        Box(Modifier.weight(1f).height(1.dp).background(Color(0x1AFFFFFF)))
    }
}

@Composable
private fun AuthSecondaryButton(label: String, onClick: () -> Unit) {
    PrezzenceSecondaryButton(label = label, onClick = onClick)
}

private data class TrackOption(val id: String, val title: String, val subtitle: String, val accent: Color)

@Composable
private fun OnboardingTopBar(label: String, onBack: () -> Unit, height: Int = 60) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().height(height.dp).padding(horizontal = 16.dp),
    ) {
        PrezzenceNavButton(PrezzenceNavIcon.Back, modifier = Modifier.size(40.dp), onClick = onBack)
        Text(label, color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.size(40.dp))
    }
}

@Composable
private fun TrackCard(option: TrackOption, selected: Boolean, disabled: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(164.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (selected) Color(0xFF24243A) else if (disabled) Color.Transparent else Card)
            .border(1.dp, if (selected) Accent else if (disabled) Border else Color(0x0DFFFFFF), RoundedCornerShape(28.dp))
            .alpha(if (disabled) 0.6f else 1f)
            .clickable(enabled = !disabled, onClick = onClick)
            .padding(16.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) Accent.copy(alpha = 0.10f) else option.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                TrackIcon(option.id, option.accent, selected)
            }
            Spacer(Modifier.weight(1f))
            Text(option.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(option.subtitle, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 2)
        }
        if (selected) {
            Box(Modifier.align(Alignment.TopEnd).size(20.dp).clip(CircleShape).background(Accent), contentAlignment = Alignment.Center) {
                CheckIcon(Modifier.size(12.dp), TextPrimary)
            }
        }
    }
}

@Composable
private fun TrackIcon(id: String, accent: Color, selected: Boolean) {
    val color = if (selected) Accent else accent
    Canvas(Modifier.size(22.dp)) {
        val stroke = Stroke(width = 2.4f, cap = StrokeCap.Round)
        when (id) {
            "job" -> {
                drawRoundRect(color, Offset(size.width * 0.16f, size.height * 0.30f), Size(size.width * 0.68f, size.height * 0.50f), CornerRadius(3f, 3f), style = stroke)
                drawLine(color, Offset(size.width * 0.38f, size.height * 0.30f), Offset(size.width * 0.38f, size.height * 0.20f), strokeWidth = 2.4f, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.62f, size.height * 0.30f), Offset(size.width * 0.62f, size.height * 0.20f), strokeWidth = 2.4f, cap = StrokeCap.Round)
            }
            "promotion" -> {
                drawLine(color, Offset(size.width * 0.18f, size.height * 0.76f), Offset(size.width * 0.42f, size.height * 0.52f), strokeWidth = 2.4f, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.42f, size.height * 0.52f), Offset(size.width * 0.58f, size.height * 0.62f), strokeWidth = 2.4f, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.58f, size.height * 0.62f), Offset(size.width * 0.82f, size.height * 0.26f), strokeWidth = 2.4f, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.68f, size.height * 0.26f), Offset(size.width * 0.82f, size.height * 0.26f), strokeWidth = 2.4f, cap = StrokeCap.Round)
            }
            "pitch" -> drawCircle(color, size.minDimension * 0.34f, Offset(size.width / 2f, size.height / 2f), style = stroke)
            "leadership" -> {
                drawCircle(color, size.minDimension * 0.13f, Offset(size.width * 0.36f, size.height * 0.36f), style = stroke)
                drawCircle(color, size.minDimension * 0.13f, Offset(size.width * 0.66f, size.height * 0.36f), style = stroke)
                drawLine(color, Offset(size.width * 0.18f, size.height * 0.76f), Offset(size.width * 0.82f, size.height * 0.76f), strokeWidth = 2.4f, cap = StrokeCap.Round)
            }
            "technical" -> {
                drawRoundRect(color, Offset(size.width * 0.25f, size.height * 0.25f), Size(size.width * 0.50f, size.height * 0.50f), CornerRadius(4f, 4f), style = stroke)
                drawLine(color, Offset(size.width * 0.10f, size.height * 0.40f), Offset(size.width * 0.25f, size.height * 0.40f), strokeWidth = 2.2f)
                drawLine(color, Offset(size.width * 0.75f, size.height * 0.60f), Offset(size.width * 0.90f, size.height * 0.60f), strokeWidth = 2.2f)
            }
            else -> {
                drawCircle(color, size.minDimension * 0.35f, Offset(size.width / 2f, size.height / 2f), style = stroke)
                drawLine(color, Offset(size.width * 0.32f, size.height * 0.50f), Offset(size.width * 0.68f, size.height * 0.50f), strokeWidth = 2.4f, cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun OnboardingContinueButton(label: String, enabled: Boolean, icon: String = "›", onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Accent.copy(alpha = if (enabled) 1f else 0.45f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(label, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(icon, color = TextPrimary, fontSize = if (icon == "›") 28.sp else 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OnboardingField(label: String, value: String, placeholder: String, onValueChange: (String) -> Unit) {
    Column {
        Text(label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, modifier = Modifier.padding(start = 4.dp))
        Spacer(Modifier.height(12.dp))
        CompactOnboardingInput(value, placeholder, onValueChange)
    }
}

@Composable
private fun CompactOnboardingInput(value: String, placeholder: String, onValueChange: (String) -> Unit, minHeight: Int = 64) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold),
        singleLine = minHeight <= 70,
        modifier = Modifier
            .fillMaxWidth()
            .height(minHeight.dp)
            .clip(RoundedCornerShape(if (minHeight > 70) 18.dp else 24.dp))
            .background(if (minHeight > 70) Surface else Card)
            .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(if (minHeight > 70) 18.dp else 24.dp))
            .padding(horizontal = if (minHeight > 70) 16.dp else 24.dp, vertical = 18.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) Text(placeholder, color = TextPrimary.copy(alpha = 0.30f), fontSize = if (minHeight > 70) 15.sp else 17.sp, fontWeight = FontWeight.Bold)
            inner()
        },
    )
}

@Composable
private fun OnboardingSegment(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column {
        Text(label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, modifier = Modifier.padding(start = 4.dp))
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Surface)
                .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(24.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { option ->
                val active = selected.equals(option, ignoreCase = true)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (active) Accent else Color.Transparent)
                        .clickable { onSelect(option) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(option, color = if (active) TextPrimary else TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun MicHero() {
    Box(Modifier.size(156.dp), contentAlignment = Alignment.Center) {
        listOf(156.dp, 136.dp, 116.dp).forEachIndexed { index, size ->
            Box(
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .border(1.dp, Accent.copy(alpha = listOf(0.05f, 0.10f, 0.20f)[index]), CircleShape)
            )
        }
        Box(
            Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Card)
                .border(1.dp, Color(0x1AFFFFFF), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            MicIcon(Modifier.size(56.dp))
        }
    }
}

@Composable
private fun MicIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = Stroke(width = 3.2f, cap = StrokeCap.Round)
        drawRoundRect(Accent, Offset(size.width * 0.34f, size.height * 0.14f), Size(size.width * 0.32f, size.height * 0.48f), CornerRadius(18f, 18f), style = stroke)
        drawArc(Accent, 25f, 130f, false, Offset(size.width * 0.20f, size.height * 0.36f), Size(size.width * 0.60f, size.height * 0.46f), style = stroke)
        drawLine(Accent, Offset(size.width * 0.50f, size.height * 0.82f), Offset(size.width * 0.50f, size.height * 0.94f), strokeWidth = 3.2f, cap = StrokeCap.Round)
        drawLine(Accent, Offset(size.width * 0.36f, size.height * 0.94f), Offset(size.width * 0.64f, size.height * 0.94f), strokeWidth = 3.2f, cap = StrokeCap.Round)
    }
}

@Composable
private fun InfoPanel() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Card)
            .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(32.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        InfoRow("Session Privacy", "Recording starts only when you answer an interview question.", "shield")
        InfoRow("Protected Processing", "Your practice data is used to create your own interview feedback.", "lock")
    }
}

@Composable
private fun InfoRow(title: String, body: String, icon: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(Accent.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
            if (icon == "shield") ShieldIcon(Modifier.size(18.dp)) else AuthIconView(AuthIcon.Lock, Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(2.dp))
            Text(body, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun ConsentPanel() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Card)
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Audio and AI consent", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text("Prezzence uses your microphone audio to transcribe answers, score interview quality, and generate coaching feedback.", color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
        ConsentBullet("Your voice is recorded only during active answer time.")
        ConsentBullet("Audio may be sent to trusted AI services for transcription and scoring.")
        ConsentBullet("Transcripts, scores, and session feedback are saved to your account history.")
    }
}

@Composable
private fun ConsentBullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        CheckIcon(Modifier.size(15.dp), Accent)
        Text(text, color = TextPrimary, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SuccessBox(message: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x1A00D68F))
            .border(1.dp, Color(0x3300D68F), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        RequirementCircle(true)
        Spacer(Modifier.width(8.dp))
        Text(message, color = Color(0xFF00D68F), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun RecoveryHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(
                color = Color(0x1AFFFFFF),
                topLeft = Offset(1.5f, 1.5f),
                size = Size(size.width - 3f, size.height - 3f),
                cornerRadius = CornerRadius(32.dp.toPx(), 32.dp.toPx()),
                style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f), 0f)),
            )
        }
        PrezzenceMark(Modifier.size(100.dp))
    }
}

@Composable
private fun BackCircle(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Card.copy(alpha = 0.72f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("<", color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    }
}

private data class LanguageOption(val id: String, val name: String, val native: String, val flag: String)

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF171721))
            .padding(horizontal = 16.dp),
    ) {
        SearchIcon(Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isEmpty()) Text("Search", color = TextSecondary, fontSize = 16.sp)
                inner()
            },
        )
    }
}

@Composable
private fun LanguageCard(option: LanguageOption, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Accent.copy(alpha = 0.05f) else Color(0xFF171721))
            .border(2.dp, if (selected) Color(0xFF5C61FF) else Color.Transparent, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(option.flag, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.width(32.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(option.name, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(option.native, color = TextSecondary, fontSize = 12.sp)
        }
        if (selected) {
            Box(Modifier.size(20.dp).clip(CircleShape).background(Color(0xFF5C61FF)), contentAlignment = Alignment.Center) {
                CheckIcon(Modifier.size(12.dp), TextPrimary)
            }
        }
    }
}

@Composable
private fun SearchIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = TextSecondary
        drawCircle(c, radius = size.minDimension * 0.30f, center = Offset(size.width * 0.42f, size.height * 0.42f), style = Stroke(width = 2.3f, cap = StrokeCap.Round))
        drawLine(c, Offset(size.width * 0.64f, size.height * 0.64f), Offset(size.width * 0.86f, size.height * 0.86f), strokeWidth = 2.3f, cap = StrokeCap.Round)
    }
}

@Composable
private fun CheckIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF00D68F)) {
    Canvas(modifier) {
        drawLine(color, Offset(size.width * 0.20f, size.height * 0.52f), Offset(size.width * 0.42f, size.height * 0.74f), strokeWidth = 2.4f, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.42f, size.height * 0.74f), Offset(size.width * 0.82f, size.height * 0.26f), strokeWidth = 2.4f, cap = StrokeCap.Round)
    }
}

@Composable
fun PrezzenceOnboardingTypeScreen(
    selectedTrack: String,
    onBack: () -> Unit,
    onSelectTrack: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val tracks = remember {
        listOf(
            TrackOption("job", "Job Interview", "Practice for a role, company, or hiring panel.", Color(0xFF6C63FF)),
            TrackOption("promotion", "Promotion", "Prepare to show impact, readiness, and leadership.", Color(0xFF00D68F)),
            TrackOption("pitch", "Pitch", "Sharpen investor, sales, or presentation answers.", Color(0xFFFFB020)),
            TrackOption("leadership", "Leadership", "Practice executive presence and people decisions.", Color(0xFF24C8F2)),
            TrackOption("behavioral", "Behavioral", "Build stronger STAR stories and examples.", Color(0xFFFF5A7A)),
            TrackOption("technical", "Technical", "Answer specialist and role-specific questions.", Color(0xFF8E7DFF)),
        )
    }
    PrezzenceTheme {
        Box(Modifier.fillMaxSize().background(Bg)) {
            Column(Modifier.fillMaxSize()) {
                OnboardingTopBar("STEP 1 / 2", onBack)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp, end = 24.dp, bottom = 140.dp),
                ) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "What kind of interview\nare you preparing for?",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Choose a track. You can change this later.", color = TextSecondary, fontSize = 16.sp)
                    Spacer(Modifier.height(20.dp))
                    tracks.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                            row.forEach { track ->
                                TrackCard(
                                    option = track,
                                    selected = selectedTrack == track.id,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onSelectTrack(track.id) },
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                        TrackCard(
                            option = TrackOption("more", "More options", "Coming later", TextSecondary),
                            selected = false,
                            disabled = true,
                            modifier = Modifier.weight(1f),
                            onClick = {},
                        )
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 34.dp),
            ) {
                OnboardingContinueButton("Continue", enabled = selectedTrack.isNotBlank(), onClick = onContinue)
            }
        }
    }
}

private data class OnboardingSetupCopy(
    val title: String,
    val subtitle: String,
    val primaryLabel: String,
    val primaryPlaceholder: String,
    val secondaryLabel: String,
    val secondaryPlaceholder: String,
    val levelLabel: String = "EXPERIENCE LEVEL",
    val levelOptions: List<String> = listOf("Junior", "Mid", "Senior", "Executive"),
    val detailsTitle: String,
    val detailsSubtitle: String,
    val detailNamePlaceholder: String,
    val detailLinkPlaceholder: String,
    val detailContextPlaceholder: String,
    val detailsHelp: String,
    val researchLabel: String,
    val askerLabel: String,
    val askerOptions: List<String> = listOf("Single", "Panel"),
    val askerHelp: String,
    val coachStyleLabel: String = "INTERVIEWER STYLE",
    val coachStyleOptions: List<String> = listOf("Supportive", "Balanced", "Challenging"),
    val coachStyleHelp: String,
    val difficultyLabel: String,
    val difficultyOptions: List<String> = listOf("Beginner", "Realistic", "Pressure"),
    val skillQuestionsLabel: String,
    val skillQuestionsHelp: String,
)

private fun onboardingSetupForTrack(track: String): OnboardingSetupCopy = when (track.lowercase()) {
    "promotion" -> OnboardingSetupCopy(
        title = "Tell us about\nthe next level.",
        subtitle = "We will tune practice around promotion readiness.",
        primaryLabel = "TARGET ROLE OR LEVEL",
        primaryPlaceholder = "Senior Product Manager",
        secondaryLabel = "FUNCTION OR TEAM",
        secondaryPlaceholder = "Product, Engineering, Sales",
        detailsTitle = "Promotion context",
        detailsSubtitle = "Optional. Add scope, wins, and manager expectations.",
        detailNamePlaceholder = "Company or team name",
        detailLinkPlaceholder = "Promotion packet, review link, or notes URL",
        detailContextPlaceholder = "Paste accomplishments, current scope, target level, feedback, or promotion criteria.",
        detailsHelp = "Add the strongest context you have. Research can add public company context when enabled.",
        researchLabel = "ONLINE COMPANY RESEARCH",
        askerLabel = "WHO SHOULD CHALLENGE YOU?",
        askerHelp = "Use single mode for direct manager practice, or panel mode for cross-functional promotion review.",
        coachStyleHelp = "Choose how direct the promotion committee should feel.",
        difficultyLabel = "READINESS BAR",
        skillQuestionsLabel = "ASK LEADERSHIP-SPECIFIC QUESTIONS",
        skillQuestionsHelp = "Useful for scope, judgment, ownership, influence, and strategic impact.",
    )
    "pitch" -> OnboardingSetupCopy(
        title = "Tell us about\nthe pitch.",
        subtitle = "We will tune objections, follow-ups, and clarity.",
        primaryLabel = "WHAT ARE YOU PITCHING?",
        primaryPlaceholder = "Seed round for Prezzence",
        secondaryLabel = "AUDIENCE",
        secondaryPlaceholder = "Investors, customers, partners",
        levelLabel = "PITCH STAGE",
        levelOptions = listOf("Draft", "Ready", "High stakes", "Executive"),
        detailsTitle = "Pitch details",
        detailsSubtitle = "Optional. Add deck context, ask, audience, and objections.",
        detailNamePlaceholder = "Company, product, or idea name",
        detailLinkPlaceholder = "Website, deck link, or product URL",
        detailContextPlaceholder = "Paste your pitch, target audience, pricing, traction, risks, or objections you expect.",
        detailsHelp = "Add the concrete pitch details. Research can add public market or company context when enabled.",
        researchLabel = "ONLINE CONTEXT RESEARCH",
        askerLabel = "WHO SHOULD ASK FOLLOW-UPS?",
        askerHelp = "Use single mode for one decision-maker, or panel mode for multiple stakeholder angles.",
        coachStyleLabel = "AUDIENCE STYLE",
        coachStyleHelp = "Choose whether the audience feels encouraging, balanced, or skeptical.",
        difficultyLabel = "OBJECTION LEVEL",
        skillQuestionsLabel = "ASK DOMAIN-SPECIFIC OBJECTIONS",
        skillQuestionsHelp = "Useful for investor, sales, product, and partnership pitches.",
    )
    "leadership" -> OnboardingSetupCopy(
        title = "Tell us about\nyour leadership scenario.",
        subtitle = "We will tune questions around judgment and executive presence.",
        primaryLabel = "LEADERSHIP SCOPE",
        primaryPlaceholder = "Head of Customer Success",
        secondaryLabel = "ORG OR FUNCTION",
        secondaryPlaceholder = "Operations, Product, People",
        detailsTitle = "Scenario context",
        detailsSubtitle = "Optional. Add team size, stakes, and decision context.",
        detailNamePlaceholder = "Company, team, or business unit",
        detailLinkPlaceholder = "Company site or context link",
        detailContextPlaceholder = "Paste the leadership challenge, team context, metrics, conflicts, or strategic decision.",
        detailsHelp = "Add enough context for realistic leadership follow-ups. Research can add public company context.",
        researchLabel = "ONLINE ORG RESEARCH",
        askerLabel = "WHO SHOULD ASK QUESTIONS?",
        askerHelp = "Panel mode is useful for leadership because it creates multiple executive perspectives.",
        coachStyleHelp = "Choose how direct and senior the leadership conversation should feel.",
        difficultyLabel = "PRESSURE LEVEL",
        skillQuestionsLabel = "ASK STRATEGIC JUDGMENT QUESTIONS",
        skillQuestionsHelp = "Useful for conflict, prioritization, people leadership, strategy, and tradeoffs.",
    )
    "behavioral" -> OnboardingSetupCopy(
        title = "Tell us about\nyour stories.",
        subtitle = "We will tune STAR practice around your examples.",
        primaryLabel = "TARGET ROLE OR THEME",
        primaryPlaceholder = "Customer Success Manager",
        secondaryLabel = "STORY AREA",
        secondaryPlaceholder = "Leadership, conflict, ownership",
        detailsTitle = "Story context",
        detailsSubtitle = "Optional. Add examples, achievements, and weak spots.",
        detailNamePlaceholder = "Company, team, or project",
        detailLinkPlaceholder = "Resume, portfolio, or context link",
        detailContextPlaceholder = "Paste story bullets, wins, failures, conflicts, metrics, or examples you want to practice.",
        detailsHelp = "Add real examples for better follow-ups. Research can add public company context when enabled.",
        researchLabel = "ONLINE CONTEXT RESEARCH",
        askerLabel = "WHO SHOULD ASK FOLLOW-UPS?",
        askerHelp = "Use single mode for focused STAR practice, or panel mode for varied follow-up styles.",
        coachStyleHelp = "Choose how supportive or challenging the behavioral follow-ups should be.",
        difficultyLabel = "FOLLOW-UP DEPTH",
        skillQuestionsLabel = "ASK ROLE-SPECIFIC BEHAVIORAL QUESTIONS",
        skillQuestionsHelp = "Useful when your stories need to connect to a specific role or industry.",
    )
    "technical" -> OnboardingSetupCopy(
        title = "Tell us about\nthe technical role.",
        subtitle = "We will tune specialist and role-specific questions.",
        primaryLabel = "TECHNICAL ROLE",
        primaryPlaceholder = "Backend Engineer",
        secondaryLabel = "DOMAIN OR STACK",
        secondaryPlaceholder = "Kotlin, cloud, data, ML",
        detailsTitle = "Technical context",
        detailsSubtitle = "Optional. Add stack, job description, and focus areas.",
        detailNamePlaceholder = "Company or team name",
        detailLinkPlaceholder = "Job link, repo, portfolio, or product URL",
        detailContextPlaceholder = "Paste job requirements, stack, architecture topics, system design focus, or project notes.",
        detailsHelp = "Add technical context for sharper specialist questions. Research can add public company context.",
        researchLabel = "ONLINE COMPANY RESEARCH",
        askerLabel = "WHO SHOULD ASK QUESTIONS?",
        askerHelp = "Use single mode for one interviewer, or panel mode for recruiter, hiring manager, and technical depth.",
        coachStyleHelp = "Choose how direct the technical interviewer should be.",
        difficultyLabel = "TECHNICAL DIFFICULTY",
        skillQuestionsLabel = "ASK DEEP TECHNICAL QUESTIONS",
        skillQuestionsHelp = "Useful for coding, architecture, systems, domain knowledge, and tradeoffs.",
    )
    else -> OnboardingSetupCopy(
        title = "Tell us about\nthe role.",
        subtitle = "We will tune your questions to match.",
        primaryLabel = "ROLE YOU WANT",
        primaryPlaceholder = "Senior Product Manager",
        secondaryLabel = "INDUSTRY",
        secondaryPlaceholder = "Customer Service",
        detailsTitle = "Company or job details",
        detailsSubtitle = "Optional. Helps the app ask better questions.",
        detailNamePlaceholder = "Company name, e.g. Stripe or Goldman Sachs",
        detailLinkPlaceholder = "Website or job link",
        detailContextPlaceholder = "Paste job description, company notes, products, market, or interview focus.",
        detailsHelp = "Paste details for the best results. Online research can add public company context when enabled.",
        researchLabel = "ONLINE COMPANY RESEARCH",
        askerLabel = "WHO SHOULD ASK QUESTIONS?",
        askerHelp = "Start with one interviewer for speed, or use panel mode when you want multiple interviewer styles.",
        coachStyleHelp = "The interviewer look follows the industry: tech is more casual, law and finance are more formal.",
        difficultyLabel = "QUESTION DIFFICULTY",
        skillQuestionsLabel = "ASK ROLE-SPECIFIC SKILL QUESTIONS",
        skillQuestionsHelp = "Useful for software, finance, healthcare, law, sales, and other specialist roles.",
    )
}

@Composable
fun PrezzenceOnboardingRoleScreen(
    track: String,
    role: String,
    industry: String,
    seniority: String,
    interviewMode: String,
    difficulty: String,
    companyName: String,
    companyWebsite: String,
    companyContext: String,
    interviewerStyle: String,
    previewGender: String,
    includeTechnical: Boolean,
    enableWebResearch: Boolean,
    onBack: () -> Unit,
    onRoleChange: (String) -> Unit,
    onIndustryChange: (String) -> Unit,
    onSeniorityChange: (String) -> Unit,
    onInterviewModeChange: (String) -> Unit,
    onDifficultyChange: (String) -> Unit,
    onCompanyNameChange: (String) -> Unit,
    onCompanyWebsiteChange: (String) -> Unit,
    onCompanyContextChange: (String) -> Unit,
    onInterviewerStyleChange: (String) -> Unit,
    onPreviewGenderChange: (String) -> Unit,
    onIncludeTechnicalChange: (Boolean) -> Unit,
    onEnableWebResearchChange: (Boolean) -> Unit,
    onContinue: () -> Unit,
) {
    val setup = remember(track) { onboardingSetupForTrack(track) }
    var roleValue by remember { mutableStateOf(role) }
    var industryValue by remember { mutableStateOf(industry) }
    var seniorityValue by remember { mutableStateOf(seniority) }
    var interviewModeValue by remember { mutableStateOf(interviewMode) }
    var difficultyValue by remember { mutableStateOf(difficulty) }
    var companyNameValue by remember { mutableStateOf(companyName) }
    var companyWebsiteValue by remember { mutableStateOf(companyWebsite) }
    var companyContextValue by remember { mutableStateOf(companyContext) }
    var interviewerStyleValue by remember { mutableStateOf(interviewerStyle) }
    var previewGenderValue by remember { mutableStateOf(previewGender) }
    var includeTechnicalValue by remember { mutableStateOf(includeTechnical) }
    var enableWebResearchValue by remember { mutableStateOf(enableWebResearch) }

    fun updateRole(value: String) {
        roleValue = value
        onRoleChange(value)
    }

    fun updateIndustry(value: String) {
        industryValue = value
        onIndustryChange(value)
    }

    fun updateSeniority(value: String) {
        seniorityValue = value
        onSeniorityChange(value)
    }

    fun updateInterviewMode(value: String) {
        interviewModeValue = value
        onInterviewModeChange(value)
    }

    fun updateDifficulty(value: String) {
        difficultyValue = value
        onDifficultyChange(value)
    }

    fun updateCompanyName(value: String) {
        companyNameValue = value
        onCompanyNameChange(value)
    }

    fun updateCompanyWebsite(value: String) {
        companyWebsiteValue = value
        onCompanyWebsiteChange(value)
    }

    fun updateCompanyContext(value: String) {
        companyContextValue = value
        onCompanyContextChange(value)
    }

    fun updateInterviewerStyle(value: String) {
        interviewerStyleValue = value
        onInterviewerStyleChange(value)
    }

    fun updatePreviewGender(value: String) {
        previewGenderValue = value
        onPreviewGenderChange(value)
    }

    fun updateIncludeTechnical(value: Boolean) {
        includeTechnicalValue = value
        onIncludeTechnicalChange(value)
    }

    fun updateEnableWebResearch(value: Boolean) {
        enableWebResearchValue = value
        onEnableWebResearchChange(value)
    }

    PrezzenceTheme {
        Box(Modifier.fillMaxSize().background(Bg)) {
            Column(Modifier.fillMaxSize()) {
                OnboardingTopBar("STEP 2 / 2", onBack, height = 80)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp, end = 24.dp, bottom = 130.dp),
                ) {
                    Spacer(Modifier.height(20.dp))
                    Text(setup.title, color = TextPrimary, fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Text(setup.subtitle, color = TextSecondary, fontSize = 16.sp, lineHeight = 24.sp)
                    Spacer(Modifier.height(32.dp))

                    OnboardingField(setup.primaryLabel, roleValue, setup.primaryPlaceholder, ::updateRole)
                    Spacer(Modifier.height(24.dp))
                    OnboardingField(setup.secondaryLabel, industryValue, setup.secondaryPlaceholder, ::updateIndustry)
                    Spacer(Modifier.height(24.dp))
                    OnboardingSegment(setup.levelLabel, setup.levelOptions, seniorityValue, ::updateSeniority)
                    Spacer(Modifier.height(24.dp))

                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(26.dp))
                            .background(Card)
                            .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(26.dp))
                            .padding(18.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(42.dp).clip(RoundedCornerShape(16.dp)).background(Accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                SearchIcon(Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(setup.detailsTitle, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
                                Text(setup.detailsSubtitle, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        CompactOnboardingInput(companyNameValue, setup.detailNamePlaceholder, ::updateCompanyName)
                        Spacer(Modifier.height(12.dp))
                        CompactOnboardingInput(companyWebsiteValue, setup.detailLinkPlaceholder, ::updateCompanyWebsite)
                        Spacer(Modifier.height(12.dp))
                        CompactOnboardingInput(companyContextValue, setup.detailContextPlaceholder, ::updateCompanyContext, minHeight = 118)
                        Spacer(Modifier.height(12.dp))
                        Text(setup.detailsHelp, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(16.dp))
                        OnboardingSegment(
                            setup.researchLabel,
                            listOf("Off", "On"),
                            if (enableWebResearchValue) "On" else "Off",
                            { choice -> updateEnableWebResearch(choice == "On") },
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    OnboardingSegment(setup.askerLabel, setup.askerOptions, interviewModeValue, ::updateInterviewMode)
                    Text(setup.askerHelp, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
                    Spacer(Modifier.height(24.dp))
                    OnboardingSegment(setup.coachStyleLabel, setup.coachStyleOptions, interviewerStyleValue, ::updateInterviewerStyle)
                    Text(setup.coachStyleHelp, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
                    Spacer(Modifier.height(24.dp))
                    OnboardingSegment(setup.difficultyLabel, setup.difficultyOptions, difficultyValue, ::updateDifficulty)
                    Spacer(Modifier.height(24.dp))
                    OnboardingSegment(
                        setup.skillQuestionsLabel,
                        listOf("Off", "On"),
                        if (includeTechnicalValue) "On" else "Off",
                        { choice -> updateIncludeTechnical(choice == "On") },
                    )
                    Text(setup.skillQuestionsHelp, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
                    Spacer(Modifier.height(24.dp))
                    OnboardingSegment("YOUR PREVIEW IMAGE", listOf("Female", "Male"), previewGenderValue, ::updatePreviewGender)
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.80f))
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 40.dp),
            ) {
                OnboardingContinueButton("Continue", enabled = roleValue.trim().length >= 2, icon = "⚡", onClick = onContinue)
            }
        }
    }
}

@Composable
fun PrezzenceMicPermissionScreen(
    consentAccepted: Boolean,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onConsentChange: (Boolean) -> Unit,
    onGrantAccess: () -> Unit,
) {
    PrezzenceTheme {
        Box(Modifier.fillMaxSize().background(Bg)) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 16.dp),
                ) {
                    PrezzenceNavButton(PrezzenceNavIcon.Back, onClick = onBack)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("READY TO BEGIN", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Permissions", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.size(44.dp))
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                ) {
                    MicHero()
                    Text("Microphone\nPermission", color = TextPrimary, fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "To analyze your voice and provide feedback, we need access to your microphone.",
                        color = TextSecondary,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(18.dp))
                    InfoPanel()
                    Spacer(Modifier.height(18.dp))
                    ConsentPanel()
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Bg)
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Card)
                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(18.dp))
                            .clickable { onConsentChange(!consentAccepted) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Box(
                            Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (consentAccepted) Accent else Bg)
                                .border(1.dp, if (consentAccepted) Accent else Color(0x1AFFFFFF), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (consentAccepted) CheckIcon(Modifier.size(16.dp), TextPrimary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "I agree to audio recording and AI analysis for interview practice.",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(error, color = Color(0xFFFF5C7A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    OnboardingContinueButton(if (loading) "Loading..." else "Grant Access & Begin", enabled = consentAccepted && !loading, icon = "⚡", onClick = onGrantAccess)
                    Spacer(Modifier.height(10.dp))
                    Text("Decline Review", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().height(42.dp).clickable(onClick = onBack))
                }
            }
        }
    }
}

@Composable
fun PrezzenceEnteringRoomScreen(
    isPanel: Boolean,
    interviewers: List<Pair<String, String>>,
    preparing: Boolean,
    setupStatus: String,
    onBack: () -> Unit,
    onJoin: () -> Unit,
) {
    val displayInterviewers = if (interviewers.isEmpty()) listOf("Sophia" to "Domain Expert") else interviewers
    PrezzenceTheme {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 16.dp),
                ) {
                    PrezzenceNavButton(PrezzenceNavIcon.Back, onClick = onBack)
                    Text("Entering the Room", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.size(44.dp))
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp, end = 24.dp, top = 30.dp, bottom = 120.dp),
                ) {
                    Text(
                        if (isPanel) "LIVE PANEL" else "LIVE INTERVIEW",
                        color = Accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (preparing) {
                            if (isPanel) "Your interviewers are getting ready." else "Getting ready"
                        } else {
                            if (isPanel) "Your interviewers are ready." else "Your interviewer is ready."
                        },
                        color = TextPrimary,
                        fontSize = 27.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        if (preparing) {
                            "Preparing your questions and interview room."
                        } else if (isPanel) {
                            "${displayInterviewers.size} interviewers are ready."
                        } else {
                            "1 interviewer is ready."
                        },
                        color = Color(0xFFA5A6BA),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(28.dp))

                    if (isPanel) {
                        EnteringPanelRow(displayInterviewers, preparing = false)
                    } else {
                        EnteringSingleCard(displayInterviewers.first(), preparing = false)
                    }

                    Spacer(Modifier.height(22.dp))
                    Spacer(Modifier.height(22.dp))

                    Spacer(Modifier.height(22.dp))
                    EnteringSetupPreview(setupStatus = setupStatus, preparing = preparing)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.98f))
                    .padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 34.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .clip(RoundedCornerShape(31.dp))
                        .background(Accent.copy(alpha = if (preparing) 0.45f else 1f))
                        .clickable(enabled = !preparing, onClick = onJoin),
                    contentAlignment = Alignment.Center,
                ) {
                    if (preparing) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = TextPrimary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Preparing...", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Join Interview", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(10.dp))
                            Text("→", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnteringSingleCard(interviewer: Pair<String, String>, preparing: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF111119))
            .border(1.dp, Accent.copy(alpha = 0.70f), RoundedCornerShape(22.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .background(Color(0xFF050509)),
        ) {
            EnteringInterviewerImage(interviewer.first, Modifier.fillMaxSize())
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x15000000), Color.Transparent, Color(0x55050509)),
                        ),
                    ),
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(14.dp),
            ) {
                EnteringStatusBadge("LIVE INTERVIEW", Color(0xFFFF3B6B))
                EnteringStatusBadge(if (preparing) "PREPARING" else "READY", Color(0xFF00D68F))
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF151526))
                .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(interviewer.first, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(interviewer.second, color = Color(0xFFD9DAE8), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                EnteringCallControl("mic")
                EnteringCallControl("video")
                Box(
                    Modifier
                        .weight(1f)
                        .height(30.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Accent.copy(alpha = 0.34f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("FIRST QUESTION", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun EnteringPanelRow(interviewers: List<Pair<String, String>>, preparing: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        interviewers.take(3).forEachIndexed { index, interviewer ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .offset(y = if (index == 1) (-8).dp else 0.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (index == 1) Color(0xFF151526) else Color(0xFF111119))
                    .border(1.dp, if (index == 1) Accent.copy(alpha = 0.55f) else Color(0x14FFFFFF), RoundedCornerShape(22.dp))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
            ) {
                Box(Modifier.size(72.dp).clip(CircleShape).background(Card).border(2.dp, Color(0x1AFFFFFF), CircleShape), contentAlignment = Alignment.Center) {
                    EnteringInterviewerImage(interviewer.first, Modifier.fillMaxSize())
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x20000000)))),
                    )
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(if (preparing) Accent else Color(0xFF00D68F))
                            .border(2.dp, Color(0xFF111119), CircleShape),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(interviewer.first, color = TextPrimary, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(interviewer.second, color = Color(0xFFA5A6BA), fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Accent.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (index == 0) "FIRST QUESTION" else if (preparing) "PREPARING" else "READY", color = Color(0xFFC7C9FF), fontSize = 8.sp, lineHeight = 10.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun EnteringInterviewerImage(name: String, modifier: Modifier = Modifier) {
    val image = when (name.lowercase()) {
        "maya" -> R.drawable.interviewer_maya
        "jonas" -> R.drawable.interviewer_jonas
        "sophia", "amina" -> R.drawable.interviewer_sophia
        else -> R.drawable.interviewer_sophia
    }
    Image(
        painter = painterResource(image),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = Alignment.TopCenter,
        modifier = modifier,
    )
}

@Composable
private fun EnteringSetupPreview(setupStatus: String, preparing: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF111119))
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(22.dp))
            .padding(12.dp),
    ) {
        Box(
            Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(Accent, Cyan))),
            contentAlignment = Alignment.Center,
        ) {
            PrezzenceMark(Modifier.size(34.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(if (preparing) "DEVICE SETUP" else "SETUP PREVIEW", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
            Spacer(Modifier.height(3.dp))
            Text(setupStatus, color = TextPrimary, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold)
        }
        if (!preparing) {
            CheckIcon(Modifier.size(22.dp), Color(0xFF00D68F))
        }
    }
}

@Composable
private fun EnteringStatusBadge(label: String, dotColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.58f))
            .padding(horizontal = 9.dp, vertical = 6.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor))
        Spacer(Modifier.width(7.dp))
        Text(label, color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
    }
}

@Composable
private fun EnteringCallControl(icon: String) {
    Box(Modifier.size(30.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
        if (icon == "video") CameraGlyph(Modifier.size(15.dp), TextPrimary) else MicIcon(Modifier.size(15.dp))
    }
}

@Composable
private fun CameraGlyph(modifier: Modifier = Modifier, color: Color = TextPrimary) {
    Canvas(modifier) {
        drawRoundRect(color, Offset(size.width * 0.10f, size.height * 0.25f), Size(size.width * 0.56f, size.height * 0.50f), CornerRadius(3f, 3f), style = Stroke(width = 2.2f, cap = StrokeCap.Round))
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.68f, size.height * 0.40f)
            lineTo(size.width * 0.92f, size.height * 0.25f)
            lineTo(size.width * 0.92f, size.height * 0.75f)
            lineTo(size.width * 0.68f, size.height * 0.60f)
            close()
        }
        drawPath(path, color, style = Stroke(width = 2.2f, cap = StrokeCap.Round))
    }
}

@Composable
private fun PrezzenceMark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.prezzence_icon),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

@Composable
private fun ShieldIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = Color(0xFF00D68F)
        val stroke = Stroke(width = 4f, cap = StrokeCap.Round)
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.50f, size.height * 0.08f)
            lineTo(size.width * 0.82f, size.height * 0.22f)
            lineTo(size.width * 0.76f, size.height * 0.62f)
            quadraticBezierTo(size.width * 0.68f, size.height * 0.84f, size.width * 0.50f, size.height * 0.94f)
            quadraticBezierTo(size.width * 0.32f, size.height * 0.84f, size.width * 0.24f, size.height * 0.62f)
            lineTo(size.width * 0.18f, size.height * 0.22f)
            close()
        }
        drawPath(path, c.copy(alpha = 0.16f))
        drawPath(path, c, style = stroke)
        drawLine(c, Offset(size.width * 0.34f, size.height * 0.52f), Offset(size.width * 0.46f, size.height * 0.64f), strokeWidth = 4f, cap = StrokeCap.Round)
        drawLine(c, Offset(size.width * 0.46f, size.height * 0.64f), Offset(size.width * 0.68f, size.height * 0.38f), strokeWidth = 4f, cap = StrokeCap.Round)
    }
}

@Composable
private fun VerifyPerk(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x08FFFFFF))
            .padding(12.dp),
    ) {
        CheckIcon(Modifier.size(16.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, color = TextPrimary.copy(alpha = 0.82f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}


private enum class AuthIcon { User, Mail, Lock, Eye }

@Composable
private fun AuthIconView(icon: AuthIcon, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = Stroke(width = 2.8f, cap = StrokeCap.Round)
        val c = TextSecondary.copy(alpha = 0.9f)
        when (icon) {
            AuthIcon.User -> {
                drawCircle(c, radius = size.minDimension * 0.17f, center = Offset(size.width * 0.5f, size.height * 0.33f), style = stroke)
                drawArc(c, startAngle = 205f, sweepAngle = 130f, useCenter = false, topLeft = Offset(size.width * 0.25f, size.height * 0.50f), size = Size(size.width * 0.5f, size.height * 0.38f), style = stroke)
            }
            AuthIcon.Mail -> {
                drawRoundRect(c, topLeft = Offset(size.width * 0.12f, size.height * 0.24f), size = Size(size.width * 0.76f, size.height * 0.54f), cornerRadius = CornerRadius(2.5f, 2.5f), style = stroke)
                drawLine(c, Offset(size.width * 0.15f, size.height * 0.30f), Offset(size.width * 0.50f, size.height * 0.56f), strokeWidth = 2.8f, cap = StrokeCap.Round)
                drawLine(c, Offset(size.width * 0.85f, size.height * 0.30f), Offset(size.width * 0.50f, size.height * 0.56f), strokeWidth = 2.8f, cap = StrokeCap.Round)
            }
            AuthIcon.Lock -> {
                drawRoundRect(c, topLeft = Offset(size.width * 0.20f, size.height * 0.45f), size = Size(size.width * 0.60f, size.height * 0.38f), cornerRadius = CornerRadius(3f, 3f), style = stroke)
                drawArc(c, startAngle = 200f, sweepAngle = 140f, useCenter = false, topLeft = Offset(size.width * 0.32f, size.height * 0.14f), size = Size(size.width * 0.36f, size.height * 0.46f), style = stroke)
            }
            AuthIcon.Eye -> {
                drawOval(c, topLeft = Offset(size.width * 0.12f, size.height * 0.28f), size = Size(size.width * 0.76f, size.height * 0.44f), style = stroke)
                drawCircle(c, radius = size.minDimension * 0.11f, center = Offset(size.width * 0.5f, size.height * 0.5f), style = stroke)
            }
        }
    }
}

@Composable
private fun RequirementCircle(met: Boolean, modifier: Modifier = Modifier.size(18.dp)) {
    Canvas(modifier) {
        val c = if (met) Color(0xFF00D68F) else TextSecondary.copy(alpha = 0.9f)
        drawCircle(c, radius = size.minDimension * 0.40f, center = Offset(size.width / 2f, size.height / 2f), style = Stroke(width = 2.4f))
        if (met) {
            drawLine(c, Offset(size.width * 0.30f, size.height * 0.52f), Offset(size.width * 0.45f, size.height * 0.67f), strokeWidth = 2.4f, cap = StrokeCap.Round)
            drawLine(c, Offset(size.width * 0.45f, size.height * 0.67f), Offset(size.width * 0.72f, size.height * 0.36f), strokeWidth = 2.4f, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun PrezzenceInterviewRoomScreen(
    currentStep: Int,
    totalSteps: Int,
    interviewerName: String,
    interviewerTitle: String,
    isPanel: Boolean = false,
    panelInterviewers: List<Pair<String, String>> = emptyList(),
    questionText: String,
    answering: Boolean,
    transcript: String,
    error: String,
    cameraCoachEnabled: Boolean,
    recordingDuration: Int = 0,
    isRecording: Boolean = false,
    faceMetric: Int = 0,
    eyesMetric: Int = 0,
    headMetric: Int = 0,
    postureMetric: Int = 0,
    energyMetric: Int = 0,
    isAvatarLoading: Boolean = false,
    isStartingAnswer: Boolean = false,
    createAvatarView: () -> View,
    createCameraView: () -> View,
    onExit: () -> Unit,
    onPause: () -> Unit,
    onRepeat: () -> Unit,
    onClarify: () -> Unit,
    onAnswerNow: () -> Unit,
    onFinish: () -> Unit,
    coachingMessage: String = "",
) {
    PrezzenceTheme {
        BoxWithConstraints(Modifier.fillMaxSize().background(Bg)) {
            val compactHeight = maxHeight < 720.dp
            val compactWidth = maxWidth < 380.dp
            val pageHorizontalPadding = if (compactWidth) 12.dp else 16.dp
            val contentGap = if (compactHeight) 8.dp else 10.dp
            val stageRadius = if (compactWidth) 24.dp else 28.dp
            val panelMode = isPanel && panelInterviewers.size > 1
            val stageMinHeight = if (compactHeight) 270.dp else 320.dp
            val stageMaxHeight = if (compactHeight) 392.dp else 520.dp
            // Use aspect ratio instead of fixed height for consistency with entering room
            val avatarTopCrop = if (compactHeight) 48.dp else 56.dp
            val interviewerChipBottom = if (compactHeight) 10.dp else 14.dp

            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Accent.copy(alpha = 0.05f), Color.Transparent))),
            )
            Column(Modifier.fillMaxSize()) {
                InterviewHeader(currentStep, totalSteps, onExit)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = pageHorizontalPadding, vertical = if (compactHeight) 6.dp else 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(contentGap),
                ) {
                    BoxWithConstraints(
                        Modifier
                            .fillMaxWidth()
                            .then(
                                if (answering) {
                                    Modifier.weight(1f).heightIn(min = stageMinHeight, max = stageMaxHeight)
                                } else {
                                    // Increased height: 5:4 aspect ratio (taller than 4:3)
                                    Modifier.aspectRatio(4f / 5f)
                                }
                            )
                            .clip(RoundedCornerShape(stageRadius))
                            .background(Color(0xFF050509))
                            .border(1.dp, Accent.copy(alpha = if (answering && cameraCoachEnabled) 0.72f else 0.28f), RoundedCornerShape(stageRadius)),
                    ) {
                        if (answering && cameraCoachEnabled) {
                            // Show camera coach for presence feedback
                            AndroidView(
                                factory = { createCameraView() },
                                modifier = Modifier.fillMaxSize(),
                                update = { /* Don't recreate on recomposition */ }
                            )
                            InterviewTopGlassLabel("Camera Presence Coach", "Starting camera. Position your face in frame")
                            InterviewerChip(interviewerName, interviewerTitle, Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 66.dp))
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                listOf(
                                    "Face" to faceMetric,
                                    "Eyes" to eyesMetric,
                                    "Head" to headMetric,
                                    "Posture" to postureMetric,
                                    "Energy" to energyMetric,
                                ).forEach { (label, value) ->
                                    PresenceMetricPill(
                                        label,
                                        if (value > 0) value.toString() else "--",
                                        Modifier.weight(1f),
                                    )
                                }
                            }
                        } else if (answering) {
                            // Show avatar during answering when camera coach disabled
                            if (isAvatarLoading) {
                                // Show minimal loading state - just dark background with subtle pulse
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF050509))
                                        .clip(RoundedCornerShape(stageRadius))
                                ) {
                                    // Subtle pulse animation in center
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .align(Alignment.Center)
                                            .clip(CircleShape)
                                            .background(Accent.copy(alpha = 0.2f))
                                    )
                                }
                            } else {
                                AndroidView(
                                    factory = { createAvatarView() },
                                    modifier = Modifier.fillMaxSize(),
                                    update = { /* Don't recreate on recomposition */ }
                                )
                            }
                            InterviewerChip(
                                interviewerName,
                                interviewerTitle,
                                Modifier.align(Alignment.BottomStart).padding(start = interviewerChipBottom, end = interviewerChipBottom, bottom = interviewerChipBottom),
                            )
                        } else {
                            // Show avatar while listening to question
                            if (isAvatarLoading) {
                                // Show minimal loading state
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF050509))
                                        .clip(RoundedCornerShape(stageRadius))
                                ) {
                                    // Subtle pulse animation in center
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .align(Alignment.Center)
                                            .clip(CircleShape)
                                            .background(Accent.copy(alpha = 0.2f))
                                    )
                                }
                            } else {
                                AndroidView(
                                    factory = { createAvatarView() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(maxHeight + avatarTopCrop)
                                        .offset(y = -avatarTopCrop),
                                    update = { /* Don't recreate on recomposition */ }
                                )
                            }
                            InterviewerChip(
                                interviewerName,
                                interviewerTitle,
                                Modifier.align(Alignment.BottomStart).padding(start = interviewerChipBottom, end = interviewerChipBottom, bottom = interviewerChipBottom),
                            )
                        }
                    }

                    if (panelMode && !answering) {
                        InterviewSupportingPanelRow(panelInterviewers, interviewerName, compact = compactWidth || compactHeight)
                    }

                    StatusStrip(if (answering) "YOU ARE SPEAKING" else "INTERVIEWER SPEAKING")

                    if (!answering) {
                        // No question text display - avatar speaks the question
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(if (compactHeight) 9.dp else 12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                InterviewSmallButton("Pause", "pause", Modifier.weight(1f), onPause, compact = compactWidth)
                                InterviewSmallButton("Repeat", "repeat", Modifier.weight(1f), onRepeat, compact = compactWidth)
                                InterviewSmallButton("Clarify", "clarify", Modifier.weight(1f), onClarify, compact = compactWidth)
                            }
                            InterviewPrimaryButton(
                                "Answer Now",
                                "mic",
                                onAnswerNow,
                                Modifier.fillMaxWidth(),
                                compact = compactWidth || compactHeight,
                                enabled = !isStartingAnswer
                            )

                            Spacer(Modifier.height(if (compactHeight) 8.dp else 14.dp))
                        }
                    }
                }

                if (answering) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = if (compactWidth) 16.dp else 22.dp, end = if (compactWidth) 16.dp else 22.dp, top = 2.dp, bottom = if (compactHeight) 12.dp else 18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RecorderBar(recordingDuration = recordingDuration, isRecording = isRecording)
                        
                        // Coaching feedback display
                        if (coachingMessage.isNotBlank()) {
                            Text(
                                coachingMessage,
                                color = Color(0xFF00D68F),
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF00D68F).copy(alpha = 0.12f))
                                    .border(1.dp, Color(0xFF00D68F).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                    .padding(14.dp),
                            )
                        }
                        
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            InterviewSmallButton("Pause", "pause", Modifier.weight(0.85f), onPause, height = 54, compact = compactWidth)
                            InterviewPrimaryButton("Finish", "check", onFinish, Modifier.weight(1.15f), Color(0xFFFF4757), compact = compactWidth)

                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InterviewHeader(currentStep: Int, totalSteps: Int, onExit: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(totalSteps.coerceAtLeast(1)) { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            when {
                                index < currentStep - 1 -> Color(0xFF00D68F)
                                index == currentStep - 1 -> Accent
                                else -> Color.White.copy(alpha = 0.10f)
                            },
                        ),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("STEP $currentStep / $totalSteps", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .clickable(onClick = onExit),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(14.dp)) {
                    val c = TextPrimary
                    drawLine(c, Offset(size.width * 0.10f, size.height * 0.10f), Offset(size.width * 0.90f, size.height * 0.90f), strokeWidth = 2.4f, cap = StrokeCap.Round)
                    drawLine(c, Offset(size.width * 0.90f, size.height * 0.10f), Offset(size.width * 0.10f, size.height * 0.90f), strokeWidth = 2.4f, cap = StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
private fun StatusStrip(text: String) {
    Box(
        Modifier
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun InterviewSupportingPanelRow(interviewers: List<Pair<String, String>>, activeName: String, compact: Boolean) {
    val supporting = interviewers.filterNot { (name, _) -> name.equals(activeName, ignoreCase = true) }.take(2)
    if (supporting.isEmpty()) return
    Row(
        Modifier
            .fillMaxWidth()
            .height(if (compact) 66.dp else 76.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        supporting.forEach { (name, title) ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.055f))
                    .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(18.dp))
                    .padding(horizontal = if (compact) 9.dp else 12.dp, vertical = if (compact) 8.dp else 10.dp),
            ) {
                Row(
                    Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(if (compact) 34.dp else 42.dp)
                            .clip(CircleShape)
                            .background(Card)
                            .border(1.dp, Accent.copy(alpha = 0.42f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(name.take(1), color = TextPrimary, fontSize = if (compact) 14.sp else 17.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(if (compact) 8.dp else 10.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                        Text(name, color = TextPrimary, fontSize = if (compact) 12.sp else 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(title, color = TextSecondary, fontSize = if (compact) 9.sp else 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("READY", color = Accent, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun InterviewSmallButton(label: String, icon: String, modifier: Modifier = Modifier, onClick: () -> Unit, height: Int = 42, compact: Boolean = false) {
    Row(
        modifier
            .height(height.dp)
            .clip(RoundedCornerShape((height / 2).dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape((height / 2).dp))
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 10.dp else 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(if (compact) 14.dp else 16.dp)) {
            val c = TextPrimary
            val s = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            when (icon) {
                "pause" -> {
                    drawRoundRect(c, Offset(size.width * 0.16f, size.height * 0.08f), Size(size.width * 0.26f, size.height * 0.84f), CornerRadius(2f))
                    drawRoundRect(c, Offset(size.width * 0.58f, size.height * 0.08f), Size(size.width * 0.26f, size.height * 0.84f), CornerRadius(2f))
                }
                "repeat" -> {
                    drawArc(c, startAngle = 120f, sweepAngle = 300f, useCenter = false, topLeft = Offset(size.width * 0.10f, size.height * 0.10f), size = Size(size.width * 0.76f, size.height * 0.76f), style = s)
                    val path = Path().apply {
                        moveTo(size.width * 0.68f, size.height * 0.18f)
                        lineTo(size.width * 0.86f, size.height * 0.32f)
                        lineTo(size.width * 0.68f, size.height * 0.46f)
                    }
                    drawPath(path, c, style = s)
                }
                "clarify" -> {
                    drawCircle(c, radius = size.minDimension * 0.44f, center = Offset(size.width / 2f, size.height / 2f), style = s)
                    drawLine(c, Offset(size.width * 0.50f, size.height * 0.50f), Offset(size.width * 0.50f, size.height * 0.68f), strokeWidth = 2.2f, cap = StrokeCap.Round)
                    drawCircle(c, radius = size.minDimension * 0.07f, center = Offset(size.width * 0.50f, size.height * 0.28f))
                }
            }
        }
        // Always show consistent spacing between icon and label
        Spacer(Modifier.width(if (compact) 5.dp else 7.dp))
        Text(
            label,
            color = TextPrimary,
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun InterviewPrimaryButton(label: String, icon: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Accent, compact: Boolean = false, enabled: Boolean = true) {
    Row(
        modifier
            .height(if (compact) 52.dp else 56.dp)
            .clip(RoundedCornerShape(if (compact) 26.dp else 28.dp))
            .background(if (enabled) color else color.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (compact) 20.dp else 28.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = if (enabled) TextPrimary else TextPrimary.copy(alpha = 0.6f), fontSize = if (compact) 15.sp else 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(if (compact) 8.dp else 12.dp))
        Canvas(Modifier.size(if (compact) 18.dp else 20.dp)) {
            val c = if (enabled) TextPrimary else TextPrimary.copy(alpha = 0.6f)
            val s = Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            when (icon) {
                "mic" -> {
                    drawRoundRect(c, Offset(size.width * 0.28f, size.height * 0.04f), Size(size.width * 0.44f, size.height * 0.52f), CornerRadius(4f), style = s)
                    drawArc(c, startAngle = 200f, sweepAngle = 140f, useCenter = false, topLeft = Offset(size.width * 0.20f, size.height * 0.48f), size = Size(size.width * 0.60f, size.height * 0.40f), style = s)
                    drawLine(c, Offset(size.width * 0.50f, size.height * 0.84f), Offset(size.width * 0.50f, size.height * 0.94f), strokeWidth = 2.4f, cap = StrokeCap.Round)
                    drawLine(c, Offset(size.width * 0.34f, size.height * 0.94f), Offset(size.width * 0.66f, size.height * 0.94f), strokeWidth = 2.4f, cap = StrokeCap.Round)
                }
                "check" -> {
                    val path = Path().apply {
                        moveTo(size.width * 0.22f, size.height * 0.50f)
                        lineTo(size.width * 0.44f, size.height * 0.72f)
                        lineTo(size.width * 0.78f, size.height * 0.28f)
                    }
                    drawPath(path, c, style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
    }
}

@Composable
private fun RecorderBar(recordingDuration: Int = 0, isRecording: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${(recordingDuration / 60).toString().padStart(2, '0')}:${(recordingDuration % 60).toString().padStart(2, '0')}",
            color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.width(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            val heights = if (isRecording) listOf(3, 5, 9, 5, 12, 7, 9, 5, 11) else listOf(1, 2, 4, 3, 5, 2, 4, 3, 1)
            heights.forEach { h ->
                Box(
                    Modifier
                        .width(3.dp)
                        .height((h * 3).dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isRecording) Color(0xFF00D68F) else Accent),
                )
            }
        }
    }
}

@Composable
private fun InterviewerChip(name: String, title: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.70f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(14.dp)).background(Accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Text(name.take(1), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text("ASKED BY", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Black)
            Text(name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(title, color = TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun InterviewTopGlassLabel(label: String, status: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label.uppercase(), color = Color(0xFF00D68F), fontSize = 10.sp, fontWeight = FontWeight.Black)
        Text(status, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun AvatarLoadingSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(Color(0xFF050509)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            // Animated shimmer circle
            Box(
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color.White.copy(alpha = 0.2f),
                                Color.White.copy(alpha = 0.05f),
                            ),
                        ),
                    ),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Loading avatar...",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun PresenceMetricPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            if (value == "--") "--" else value,
            color = if (value == "--") TextSecondary else TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun CircleAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun ReadinessCard(label: String, score: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Card)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Readiness Score",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = label,
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Accent.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$score%",
                    color = Accent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun NavChip(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─── Progress Screen ──────────────────────────────────────────────────────────

data class SessionHistoryItem(
    val id: String,
    val role: String,
    val score: Int,
    val date: String,
    val answered: Int,
    val total: Int,
)

@Composable
fun PrezzenceProgressScreen(
    avgScore: Int,
    sessions: Int,
    practiceHours: Float,
    readinessLabel: String,
    recentSessions: List<SessionHistoryItem>,
    coachingTip: String,
    onSettings: () -> Unit,
    onSessionTap: (String) -> Unit,
    onNewSession: () -> Unit,
    onDeleteSession: (String) -> Unit,
    onViewAllHistory: () -> Unit = {},
) {
    val performanceTitle = when {
        avgScore >= 85 -> "Interview Ready"
        avgScore >= 70 -> "Building Confidence"
        avgScore >= 55 -> "Building Momentum"
        avgScore > 0 -> "Foundation Built"
        else -> "Ready to Practice"
    }
    val firstScore = recentSessions.lastOrNull()?.score ?: 0
    val latestScore = recentSessions.firstOrNull()?.score ?: 0
    val bestScore = recentSessions.maxOfOrNull { it.score } ?: 0
    val delta = latestScore - firstScore
    val sortedScores = recentSessions.sortedBy { it.date }.map { it.score }
    val trendData = sortedScores.takeLast(8).ifEmpty { emptyList() }
    val hasSignal = sessions > 0 && avgScore > 0
    val strongest = recentSessions.maxByOrNull { it.score }
    val weakest = recentSessions.minByOrNull { it.score }
    val focusText = if (hasSignal && strongest != null && weakest != null)
        "${if (strongest.score >= 70) "Strongest" else "Best current signal"}: ${strongest.role} at ${strongest.score}%. Focus next on ${weakest.role} at ${weakest.score}%."
    else ""
    val growthText = when {
        sessions == 0 -> "Complete your first interview to build your baseline."
        delta > 0 -> "Your interview baseline moved +$delta% this session."
        delta < 0 -> "Your interview baseline moved $delta% this session."
        else -> "Keep practicing to improve your interview signal."
    }

    PrezzenceTheme {
        Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(bottom = 96.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Performance", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)).clickable(onClick = onSettings),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(20.dp)) {
                        val c = Color.White
                        val s = Stroke(width = 2.2f, cap = StrokeCap.Round)
                        drawCircle(c, radius = size.minDimension * 0.44f, style = s)
                        listOf(0f, 120f, 240f).forEach { angle ->
                            val rad = Math.toRadians(angle.toDouble())
                            val cx = size.width / 2f + (size.width * 0.3f * kotlin.math.cos(rad)).toFloat()
                            val cy = size.height / 2f + (size.height * 0.3f * kotlin.math.sin(rad)).toFloat()
                            drawCircle(c, radius = size.minDimension * 0.08f, center = Offset(cx, cy))
                        }
                    }
                }
            }

            // Summary
            Column(Modifier.padding(horizontal = 24.dp).padding(top = 32.dp, bottom = 40.dp)) {
                Text(performanceTitle, color = TextPrimary, fontSize = 42.sp, fontWeight = FontWeight.Black, lineHeight = 48.sp, letterSpacing = (-2).sp)
                Spacer(Modifier.height(10.dp))
                Text(growthText, color = TextSecondary, fontSize = 17.sp, lineHeight = 25.sp)
            }

            // Stat cards
            Row(
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard("AVG SCORE", "$avgScore", "%", Modifier.weight(1f))
                StatCard("SESSIONS", "$sessions", "", Modifier.weight(1f))
                StatCard("PRACTICE", "${practiceHours.toInt()}", "hrs", Modifier.weight(1f))
            }

            // Improvement card
            Column(
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Card)
                    .border(1.dp, Accent.copy(alpha = 0.22f), RoundedCornerShape(28.dp))
                    .padding(22.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("Improvement", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Interview readiness: $readinessLabel", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    if (hasSignal) {
                        Row(
                            Modifier.clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (delta > 0) Color(0xFF00D68F).copy(alpha = 0.12f)
                                    else if (delta < 0) Color(0xFFFF5C7A).copy(alpha = 0.12f)
                                    else Color.White.copy(alpha = 0.06f)
                                )
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Canvas(Modifier.size(14.dp)) {
                                val c = if (delta > 0) Color(0xFF00D68F) else if (delta < 0) Color(0xFFFF5C7A) else TextSecondary
                                val path = Path().apply {
                                    if (delta > 0) {
                                        moveTo(size.width * 0.50f, size.height * 0.16f)
                                        lineTo(size.width * 0.84f, size.height * 0.72f)
                                        lineTo(size.width * 0.16f, size.height * 0.72f)
                                        close()
                                    } else if (delta < 0) {
                                        moveTo(size.width * 0.50f, size.height * 0.84f)
                                        lineTo(size.width * 0.16f, size.height * 0.28f)
                                        lineTo(size.width * 0.84f, size.height * 0.28f)
                                        close()
                                    } else {
                                        moveTo(size.width * 0.16f, size.height * 0.50f)
                                        lineTo(size.width * 0.84f, size.height * 0.50f)
                                    }
                                }
                                drawPath(path, c, style = Stroke(width = 2.4f, cap = StrokeCap.Round))
                            }
                            Text(
                                "${if (delta > 0) "+" else ""}$delta",
                                color = if (delta > 0) Color(0xFF00D68F) else if (delta < 0) Color(0xFFFF5C7A) else TextSecondary,
                                fontSize = 13.sp, fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
                if (sessions == 0) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Complete an interview with a clear transcript. Once at least one answer is scored, this page will show real improvement.",
                        color = TextPrimary, fontSize = 14.sp, lineHeight = 20.sp,
                    )
                } else {
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        ScoreCompareBox("First", "$firstScore%")
                        Canvas(Modifier.size(18.dp)) {
                            drawLine(TextSecondary, Offset(size.width * 0.20f, size.height * 0.50f), Offset(size.width * 0.80f, size.height * 0.50f), strokeWidth = 2f, cap = StrokeCap.Round)
                            drawLine(TextSecondary, Offset(size.width * 0.58f, size.height * 0.30f), Offset(size.width * 0.80f, size.height * 0.50f), strokeWidth = 2f, cap = StrokeCap.Round)
                            drawLine(TextSecondary, Offset(size.width * 0.58f, size.height * 0.70f), Offset(size.width * 0.80f, size.height * 0.50f), strokeWidth = 2f, cap = StrokeCap.Round)
                        }
                        ScoreCompareBox("Latest", "$latestScore%")
                        ScoreCompareBox("Best", "$bestScore%")
                    }
                    if (focusText.isNotBlank()) {
                        Spacer(Modifier.height(14.dp))
                        Text(focusText, color = TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                    Spacer(Modifier.height(18.dp))
                    // Trend bars
                    Row(
                        modifier = Modifier.fillMaxWidth().height(118.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        val data = if (trendData.isNotEmpty()) trendData else listOf(avgScore.coerceAtLeast(10))
                        data.forEach { h ->
                            Column(
                                Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                            ) {
                                Box(
                                    Modifier.fillMaxWidth().height(h.dp.coerceAtLeast(8.dp)).clip(RoundedCornerShape(999.dp)).background(Accent)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text("$h", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            // Coaching tip
            Row(
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 40.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Card)
                    .border(1.dp, Accent.copy(alpha = 0.20f), RoundedCornerShape(28.dp))
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(24.dp)) {
                        val c = Accent
                        // Lightning bolt
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width * 0.60f, size.height * 0.05f)
                            lineTo(size.width * 0.30f, size.height * 0.52f)
                            lineTo(size.width * 0.52f, size.height * 0.52f)
                            lineTo(size.width * 0.38f, size.height * 0.95f)
                            lineTo(size.width * 0.70f, size.height * 0.44f)
                            lineTo(size.width * 0.48f, size.height * 0.44f)
                            close()
                        }
                        drawPath(path, c)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("COACHING TIP", color = Accent, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        coachingTip.ifBlank { "Complete one interview with clear audio to unlock personalized coaching." },
                        color = TextPrimary, fontSize = 15.sp, lineHeight = 22.sp,
                    )
                }
            }

            // Recent sessions
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 0.dp).padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text("Recent Sessions", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                if (recentSessions.isNotEmpty()) {
                    Text(
                        "View all",
                        color = Accent, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.clickable(onClick = onViewAllHistory),
                    )
                }
            }

            if (recentSessions.isEmpty()) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Card)
                        .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(24.dp))
                        .padding(20.dp),
                ) {
                    Text("No sessions yet", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(6.dp))
                    Text("Your completed interviews will appear here after your first practice session.", color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
                }
            } else {
                recentSessions.forEach { session ->
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 20.dp).padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Card)
                            .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(24.dp))
                            .clickable { onSessionTap(session.id) }
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                                .background(if (session.score >= 70) Color(0xFF00D68F).copy(alpha = 0.20f) else Color.White.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${session.score}",
                                color = if (session.score >= 70) Color(0xFF00D68F) else TextPrimary,
                                fontSize = 18.sp, fontWeight = FontWeight.Black,
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(session.role, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(4.dp))
                            Text(session.date, color = TextSecondary, fontSize = 14.sp)
                        }
                        // Chevron
                        Canvas(Modifier.size(20.dp)) {
                            val c = TextSecondary
                            drawLine(c, Offset(size.width * 0.36f, size.height * 0.24f), Offset(size.width * 0.64f, size.height * 0.50f), strokeWidth = 2.4f, cap = StrokeCap.Round)
                            drawLine(c, Offset(size.width * 0.36f, size.height * 0.76f), Offset(size.width * 0.64f, size.height * 0.50f), strokeWidth = 2.4f, cap = StrokeCap.Round)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Card)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(28.dp))
            .padding(20.dp),
    ) {
        Text(label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
            if (unit.isNotBlank()) {
                Spacer(Modifier.width(4.dp))
                Text(unit, color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ScoreCompareBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(12.dp),
    ) {
        Text(label.uppercase(), color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
    }
}

// ─── Practice Screen ──────────────────────────────────────────────────────────

data class PracticeSessionItem(
    val id: String,
    val title: String,
    val type: String,
    val date: String,
    val score: Int,
    val status: String,
)

@Composable
fun PrezzencePracticeScreen(
    recentSessions: List<PracticeSessionItem>,
    onSessionTap: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onSelectMode: (String) -> Unit,
) {
    data class PracticeMode(val id: String, val title: String, val desc: String, val color: Color)
    val modes = listOf(
        PracticeMode("full", "Full interview", "Practice a complete interview from start to finish.", Color(0xFF7C3AED)),
        PracticeMode("quick", "Quick practice", "Answer a few short questions when you have limited time.", Color(0xFF0EA5E9)),
        PracticeMode("behavioral", "Story questions", "Practice examples about teamwork, leadership, and problem solving.", Color(0xFF10B981)),
        PracticeMode("technical", "Role skills", "Practice questions about the real work in your target role.", Color(0xFFF59E0B)),
        PracticeMode("promotion", "Leadership questions", "Prepare for manager, senior, promotion, or impact questions.", Color(0xFFEF4444)),
    )

    PrezzenceTheme {
        Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(bottom = 96.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().height(70.dp).padding(horizontal = 20.dp).padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Practice", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
            }

            // Title section
            Column(Modifier.padding(horizontal = 24.dp, vertical = 0.dp).padding(top = 20.dp, bottom = 32.dp)) {
                Text("What do you want\nto practice?", color = TextPrimary, fontSize = 42.sp, fontWeight = FontWeight.Black, lineHeight = 48.sp, letterSpacing = (-1).sp)
                Spacer(Modifier.height(12.dp))
                Text("Choose the interview practice that matches your next step.", color = TextSecondary, fontSize = 16.sp, lineHeight = 24.sp)
            }

            // Practice options
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                Text("Practice options", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 16.dp))
                modes.forEach { mode ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
                            .clickable { onSelectMode(mode.id) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(54.dp).clip(CircleShape).background(mode.color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(Modifier.size(20.dp)) {
                                drawCircle(mode.color, size.width / 2f)
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(mode.title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(4.dp))
                            Text(mode.desc, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }

            // Recent sessions
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Recent sessions", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("${recentSessions.size}", color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
                if (recentSessions.isEmpty()) {
                    Column(
                        Modifier.clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp)).padding(20.dp)
                    ) {
                        Text("No sessions yet", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(6.dp))
                        Text("Your completed interviews will appear here.", color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                } else {
                    recentSessions.forEach { session ->
                        Row(
                            modifier = Modifier.padding(bottom = 12.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
                                .clickable { onSessionTap(session.id) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(Accent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text("${session.score}", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                                    Text("%", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(session.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(4.dp))
                                Text("${session.type} • ${session.date}", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    Modifier.clip(RoundedCornerShape(999.dp))
                                        .background(if (session.status == "completed") Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFF59E0B).copy(alpha = 0.12f))
                                        .padding(horizontal = 9.dp, vertical = 5.dp),
                                ) {
                                    Text(session.status, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                                Box(
                                    Modifier.size(34.dp).clip(CircleShape)
                                        .background(Color(0xFFFF4757).copy(alpha = 0.12f))
                                        .border(1.dp, Color(0xFFFF4757).copy(alpha = 0.22f), CircleShape)
                                        .clickable { onDeleteSession(session.id) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Canvas(Modifier.size(16.dp)) {
                                        val c = Color(0xFFFF8A9A)
                                        drawLine(c, Offset(size.width * 0.28f, size.height * 0.28f), Offset(size.width * 0.72f, size.height * 0.72f), strokeWidth = 2.2f, cap = StrokeCap.Round)
                                        drawLine(c, Offset(size.width * 0.72f, size.height * 0.28f), Offset(size.width * 0.28f, size.height * 0.72f), strokeWidth = 2.2f, cap = StrokeCap.Round)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Practice options
            Text("Practice options", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 24.dp, vertical = 0.dp).padding(bottom = 14.dp))
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                modes.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(mode.color.copy(alpha = 0.04f))
                            .border(1.dp, mode.color.copy(alpha = 0.40f), RoundedCornerShape(24.dp))
                            .clickable { onSelectMode(mode.id) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            Modifier.size(52.dp).clip(RoundedCornerShape(18.dp))
                                .background(mode.color.copy(alpha = 0.10f))
                                .border(1.dp, mode.color.copy(alpha = 0.20f), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Canvas(Modifier.size(23.dp)) {
                                val c = mode.color
                                when (mode.id) {
                                    "full" -> {
                                        drawCircle(c, size.minDimension * 0.30f, style = Stroke(2.2f))
                                        drawCircle(c, size.minDimension * 0.14f, center = Offset(size.width * 0.65f, size.height * 0.38f), style = Stroke(2.0f))
                                    }
                                    "quick" -> {
                                        drawCircle(c, size.minDimension * 0.44f, style = Stroke(2.2f))
                                        drawLine(c, Offset(size.width / 2f, size.height * 0.28f), Offset(size.width / 2f, size.height / 2f), strokeWidth = 2.4f, cap = StrokeCap.Round)
                                        drawLine(c, Offset(size.width / 2f, size.height / 2f), Offset(size.width * 0.65f, size.height * 0.60f), strokeWidth = 2.4f, cap = StrokeCap.Round)
                                    }
                                    "behavioral" -> {
                                        drawRoundRect(c, Offset(size.width * 0.12f, size.height * 0.18f), Size(size.width * 0.76f, size.height * 0.56f), CornerRadius(6f), style = Stroke(2.2f))
                                        drawLine(c, Offset(size.width * 0.28f, size.height * 0.42f), Offset(size.width * 0.72f, size.height * 0.42f), strokeWidth = 2f, cap = StrokeCap.Round)
                                        drawLine(c, Offset(size.width * 0.28f, size.height * 0.58f), Offset(size.width * 0.56f, size.height * 0.58f), strokeWidth = 2f, cap = StrokeCap.Round)
                                    }
                                    "technical" -> {
                                        drawRoundRect(c, Offset(size.width * 0.18f, size.height * 0.22f), Size(size.width * 0.64f, size.height * 0.56f), CornerRadius(4f), style = Stroke(2.2f))
                                    }
                                    "promotion" -> {
                                        drawLine(c, Offset(size.width * 0.18f, size.height * 0.76f), Offset(size.width * 0.42f, size.height * 0.52f), strokeWidth = 2.2f, cap = StrokeCap.Round)
                                        drawLine(c, Offset(size.width * 0.42f, size.height * 0.52f), Offset(size.width * 0.58f, size.height * 0.62f), strokeWidth = 2.2f, cap = StrokeCap.Round)
                                        drawLine(c, Offset(size.width * 0.58f, size.height * 0.62f), Offset(size.width * 0.82f, size.height * 0.26f), strokeWidth = 2.2f, cap = StrokeCap.Round)
                                    }
                                    else -> drawCircle(c, size.minDimension * 0.44f, style = Stroke(2.2f))
                                }
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(mode.title, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.3).sp)
                            Spacer(Modifier.height(4.dp))
                            Text(mode.desc, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Canvas(Modifier.size(20.dp)) {
                            drawLine(TextSecondary, Offset(size.width * 0.36f, size.height * 0.24f), Offset(size.width * 0.64f, size.height * 0.50f), strokeWidth = 2.2f, cap = StrokeCap.Round)
                            drawLine(TextSecondary, Offset(size.width * 0.36f, size.height * 0.76f), Offset(size.width * 0.64f, size.height * 0.50f), strokeWidth = 2.2f, cap = StrokeCap.Round)
                        }
                    }
                }
            }
        }
    }
}

// ─── Profile Screen ───────────────────────────────────────────────────────────

@Composable
fun PrezzenceProfileScreen(
    fullName: String,
    email: String,
    initials: String,
    avgScore: Int,
    sessions: Int,
    coachingTip: String,
    bestSkillLabel: String? = null,
    bestSkillValue: Int? = null,
    cameraCoachEnabled: Boolean,
    resumeFileName: String?,
    language: String,
    goalValue: String = "",
    onToggleCameraCoach: () -> Unit,
    onUploadResume: () -> Unit,
    onDeleteResume: () -> Unit,
    onAccount: () -> Unit,
    onLanguage: () -> Unit,
    onPrivacy: () -> Unit,
    onHelp: () -> Unit,
    onSignOut: () -> Unit,
    onSettings: () -> Unit,
    onNotifications: () -> Unit = {},
    onGoalChange: (String) -> Unit = {},
) {
    val langLabel = mapOf(
        "en" to "English", "en-US" to "English", "es" to "Spanish", "fr" to "French",
        "de" to "German", "it" to "Italian", "pt" to "Portuguese", "zh" to "Chinese",
        "ja" to "Japanese", "ko" to "Korean", "ar" to "Arabic", "hi" to "Hindi",
    )[language] ?: language.uppercase()

    PrezzenceTheme {
        Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(bottom = 96.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Profile", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)).clickable(onClick = onNotifications),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.size(22.dp)) {
                            val c = Color.White
                            val s = Stroke(width = 2.2f, cap = StrokeCap.Round)
                            val path = Path().apply {
                                moveTo(size.width * 0.50f, size.height * 0.72f)
                                lineTo(size.width * 0.28f, size.height * 0.72f)
                                cubicTo(size.width * 0.22f, size.height * 0.72f, size.width * 0.22f, size.height * 0.48f, size.width * 0.30f, size.height * 0.44f)
                                lineTo(size.width * 0.30f, size.height * 0.36f)
                                cubicTo(size.width * 0.30f, size.height * 0.20f, size.width * 0.40f, size.height * 0.14f, size.width * 0.50f, size.height * 0.14f)
                                cubicTo(size.width * 0.60f, size.height * 0.14f, size.width * 0.70f, size.height * 0.20f, size.width * 0.70f, size.height * 0.36f)
                                lineTo(size.width * 0.70f, size.height * 0.44f)
                                cubicTo(size.width * 0.78f, size.height * 0.48f, size.width * 0.78f, size.height * 0.72f, size.width * 0.72f, size.height * 0.72f)
                            }
                            drawPath(path, c, style = s)
                            drawCircle(c, radius = size.minDimension * 0.06f, center = Offset(size.width * 0.50f, size.height * 0.82f))
                            drawCircle(Color(0xFFFF4757), radius = size.minDimension * 0.07f, center = Offset(size.width * 0.62f, size.height * 0.20f))
                        }
                    }
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)).clickable(onClick = onSettings),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.size(22.dp)) {
                            val c = Color.White
                            val s = Stroke(width = 2.2f, cap = StrokeCap.Round)
                            drawCircle(c, radius = size.minDimension * 0.44f, style = s)
                            listOf(0f, 120f, 240f).forEach { angle ->
                                val rad = Math.toRadians(angle.toDouble())
                                val cx = size.width / 2f + (size.width * 0.3f * kotlin.math.cos(rad)).toFloat()
                                val cy = size.height / 2f + (size.height * 0.3f * kotlin.math.sin(rad)).toFloat()
                                drawCircle(c, radius = size.minDimension * 0.08f, center = Offset(cx, cy))
                            }
                        }
                    }
                }
            }

            // Identity card
            Row(
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 18.dp).padding(top = 8.dp)
                    .clip(RoundedCornerShape(24.dp)).background(Card)
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier.size(66.dp).clip(RoundedCornerShape(22.dp))
                        .background(Accent.copy(alpha = 0.16f))
                        .border(1.dp, Accent.copy(alpha = 0.34f), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(initials, color = TextPrimary, fontSize = 23.sp, fontWeight = FontWeight.Black)
                }
                Column(Modifier.weight(1f)) {
                    Text(fullName, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black, lineHeight = 25.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(email, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 17.sp)
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(999.dp)).background(Accent.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("Beta access", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            // Stats row
            Row(
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard("SESSIONS", "$sessions", "", Modifier.weight(1f))
                StatCard("AVG SCORE", "$avgScore", "%", Modifier.weight(1f))
                StatCard("PRACTICE", "0", "h", Modifier.weight(1f))
            }

            // Coaching focus
            Column(
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 18.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Accent.copy(alpha = 0.08f))
                    .border(1.dp, Accent.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
                    .padding(18.dp),
            ) {
                Text("COACHING FOCUS", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    coachingTip.ifBlank { if (sessions == 0) "Complete your first interview to build a coaching profile." else "Keep practicing to improve your interview signal." },
                    color = TextPrimary, fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold,
                )
                if (bestSkillLabel != null && bestSkillValue != null) {
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Best skill", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        Text("$bestSkillLabel $bestSkillValue%", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            // Camera coach toggle
            Row(
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 18.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF00D68F).copy(alpha = 0.08f))
                    .border(1.dp, Color(0xFF00D68F).copy(alpha = 0.20f), RoundedCornerShape(24.dp))
                    .clickable(onClick = onToggleCameraCoach)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF00D68F).copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(20.dp)) {
                        val c = Color(0xFF00D68F)
                        drawRoundRect(c, Offset(size.width * 0.10f, size.height * 0.25f), Size(size.width * 0.56f, size.height * 0.50f), CornerRadius(3f), style = Stroke(2.2f))
                        val p = androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width * 0.68f, size.height * 0.40f)
                            lineTo(size.width * 0.92f, size.height * 0.25f)
                            lineTo(size.width * 0.92f, size.height * 0.75f)
                            lineTo(size.width * 0.68f, size.height * 0.60f)
                            close()
                        }
                        drawPath(p, c, style = Stroke(2.0f))
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("Camera coach", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (cameraCoachEnabled) "Eye contact, head, posture, and expression feedback is enabled."
                        else "Turn on camera presence feedback before starting an interview.",
                        color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold,
                    )
                }
                // Toggle track
                Box(
                    Modifier.width(48.dp).height(28.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (cameraCoachEnabled) Color(0xFF00D68F) else Color.White.copy(alpha = 0.12f))
                        .padding(3.dp),
                ) {
                    Box(
                        Modifier.size(22.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha = if (cameraCoachEnabled) 1f else 0.62f))
                            .then(if (cameraCoachEnabled) Modifier.align(Alignment.CenterEnd) else Modifier.align(Alignment.CenterStart))
                    )
                }
            }

            // Resume section
            Column(
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 18.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF00D68F).copy(alpha = 0.08f))
                    .border(1.dp, Color(0xFF00D68F).copy(alpha = 0.20f), RoundedCornerShape(24.dp))
                    .padding(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF00D68F).copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.size(20.dp)) {
                            val c = Color(0xFF00D68F)
                            drawRoundRect(c, Offset(size.width * 0.18f, size.height * 0.08f), Size(size.width * 0.64f, size.height * 0.84f), CornerRadius(3f), style = Stroke(2.2f))
                            drawLine(c, Offset(size.width * 0.34f, size.height * 0.40f), Offset(size.width * 0.66f, size.height * 0.40f), strokeWidth = 2f, cap = StrokeCap.Round)
                            drawLine(c, Offset(size.width * 0.34f, size.height * 0.56f), Offset(size.width * 0.66f, size.height * 0.56f), strokeWidth = 2f, cap = StrokeCap.Round)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Resume context", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(3.dp))
                        Text("Personalizes future questions without storing the original file.", color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    resumeFileName?.let { "Uploaded: $it" } ?: "Upload a PDF, DOCX, or TXT resume so Prezzence can ask questions that match your real background.",
                    color = if (resumeFileName != null) Color(0xFF00D68F) else TextPrimary,
                    fontSize = if (resumeFileName != null) 13.sp else 13.sp,
                    fontWeight = if (resumeFileName != null) FontWeight.Black else FontWeight.Bold,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFF00D68F))
                            .clickable(onClick = onUploadResume).padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (resumeFileName != null) "Replace CV" else "Upload CV", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                    if (resumeFileName != null) {
                        Box(
                            Modifier.height(44.dp).clip(RoundedCornerShape(22.dp))
                                .background(Color(0xFFFF4757).copy(alpha = 0.10f))
                                .border(1.dp, Color(0xFFFF4757).copy(alpha = 0.22f), RoundedCornerShape(22.dp))
                                .clickable(onClick = onDeleteResume).padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Remove", color = Color(0xFFFF4757), fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // Goal setting
            Column(
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 18.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Card)
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
                    .padding(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).background(Accent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.size(20.dp)) {
                            val c = Accent
                            drawLine(c, Offset(size.width * 0.28f, size.height * 0.50f), Offset(size.width * 0.72f, size.height * 0.50f), strokeWidth = 2.5f, cap = StrokeCap.Round)
                            drawLine(c, Offset(size.width * 0.50f, size.height * 0.28f), Offset(size.width * 0.50f, size.height * 0.72f), strokeWidth = 2.5f, cap = StrokeCap.Round)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Weekly goal", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(3.dp))
                        Text("Set a target for how many interviews to complete each week.", color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.05f)).padding(horizontal = 16.dp).height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("My goal: ", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    var editingGoal by remember { mutableStateOf(false) }
                    if (editingGoal) {
                        var goalText by remember(goalValue) { mutableStateOf(goalValue) }
                        BasicTextField(
                            value = goalText,
                            onValueChange = { goalText = it },
                            textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier.clip(RoundedCornerShape(8.dp)).background(Accent).clickable {
                                onGoalChange(goalText)
                                editingGoal = false
                            }.padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Set", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    } else {
                        Text(goalValue.ifBlank { "Not set" }, color = if (goalValue.isNotBlank()) TextPrimary else TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier.clip(RoundedCornerShape(8.dp)).background(Accent.copy(alpha = 0.14f)).clickable { editingGoal = true }.padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Edit", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // Menu links
            val menuItems = listOf(
                Triple("Account", "Name, email, and practice profile", { onAccount() }),
                Triple("Language", langLabel, { onLanguage() }),
                Triple("Privacy & Security", "Data, devices, and deletion controls", { onPrivacy() }),
                Triple("Help", "Support, scoring, and troubleshooting", { onHelp() }),
            )
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                menuItems.forEachIndexed { index, (title, subtitle, action) ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Card)
                            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(20.dp))
                            .clickable { action() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                            Canvas(Modifier.size(22.dp)) {
                                val c = Accent
                                val s = Stroke(width = 2.2f, cap = StrokeCap.Round)
                                when (index) {
                                    0 -> {
                                        drawCircle(c, radius = size.minDimension * 0.20f, center = Offset(size.width / 2f, size.height * 0.32f), style = s)
                                        val body = Path().apply {
                                            moveTo(size.width * 0.16f, size.height * 0.78f)
                                            cubicTo(size.width * 0.26f, size.height * 0.54f, size.width * 0.74f, size.height * 0.54f, size.width * 0.84f, size.height * 0.78f)
                                        }
                                        drawPath(body, c, style = s)
                                    }
                                    1 -> {
                                        drawCircle(c, radius = size.minDimension * 0.38f, style = s)
                                        drawLine(c, Offset(size.width * 0.12f, size.height * 0.50f), Offset(size.width * 0.88f, size.height * 0.50f), strokeWidth = 2f, cap = StrokeCap.Round)
                                        drawLine(c, Offset(size.width * 0.16f, size.height * 0.30f), Offset(size.width * 0.84f, size.height * 0.30f), strokeWidth = 1.5f, cap = StrokeCap.Round)
                                        drawLine(c, Offset(size.width * 0.16f, size.height * 0.70f), Offset(size.width * 0.84f, size.height * 0.70f), strokeWidth = 1.5f, cap = StrokeCap.Round)
                                        drawArc(c, 0f, 360f, false, topLeft = Offset(size.width * 0.42f, size.height * 0.10f), size = Size(size.width * 0.16f, size.height * 0.80f), style = Stroke(width = 2.2f, cap = StrokeCap.Round))
                                    }
                                    2 -> {
                                        val path = Path().apply {
                                            moveTo(size.width * 0.50f, size.height * 0.12f)
                                            lineTo(size.width * 0.80f, size.height * 0.24f)
                                            lineTo(size.width * 0.80f, size.height * 0.52f)
                                            cubicTo(size.width * 0.80f, size.height * 0.72f, size.width * 0.50f, size.height * 0.86f, size.width * 0.50f, size.height * 0.86f)
                                            cubicTo(size.width * 0.50f, size.height * 0.86f, size.width * 0.20f, size.height * 0.72f, size.width * 0.20f, size.height * 0.52f)
                                            lineTo(size.width * 0.20f, size.height * 0.24f)
                                            close()
                                        }
                                        drawPath(path, c, style = s)
                                        drawLine(c, Offset(size.width * 0.38f, size.height * 0.50f), Offset(size.width * 0.46f, size.height * 0.62f), strokeWidth = 2.5f, cap = StrokeCap.Round)
                                        drawLine(c, Offset(size.width * 0.46f, size.height * 0.62f), Offset(size.width * 0.62f, size.height * 0.40f), strokeWidth = 2.5f, cap = StrokeCap.Round)
                                    }
                                    3 -> {
                                        drawCircle(c, radius = size.minDimension * 0.38f, style = s)
                                        val q = Path().apply {
                                            moveTo(size.width * 0.56f, size.height * 0.40f)
                                            cubicTo(size.width * 0.62f, size.height * 0.30f, size.width * 0.40f, size.height * 0.24f, size.width * 0.38f, size.height * 0.40f)
                                            cubicTo(size.width * 0.36f, size.height * 0.54f, size.width * 0.50f, size.height * 0.50f, size.width * 0.50f, size.height * 0.60f)
                                        }
                                        drawPath(q, c, style = s)
                                        drawCircle(c, radius = 1.5f, center = Offset(size.width * 0.50f, size.height * 0.72f))
                                    }
                                }
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(3.dp))
                            Text(subtitle, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Canvas(Modifier.size(18.dp)) {
                            drawLine(TextSecondary, Offset(size.width * 0.36f, size.height * 0.24f), Offset(size.width * 0.64f, size.height * 0.50f), strokeWidth = 2f, cap = StrokeCap.Round)
                            drawLine(TextSecondary, Offset(size.width * 0.36f, size.height * 0.76f), Offset(size.width * 0.64f, size.height * 0.50f), strokeWidth = 2f, cap = StrokeCap.Round)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                // Sign out
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp)).background(Card)
                        .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(20.dp))
                        .clickable { onSignOut() }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFFF4757).copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(22.dp)) {
                            val c = Color(0xFFFF4757)
                            val s = Stroke(width = 2.2f, cap = StrokeCap.Round)
                            val path = Path().apply {
                                moveTo(size.width * 0.50f, size.height * 0.16f)
                                lineTo(size.width * 0.50f, size.height * 0.50f)
                                moveTo(size.width * 0.32f, size.height * 0.30f)
                                cubicTo(size.width * 0.18f, size.height * 0.42f, size.width * 0.22f, size.height * 0.70f, size.width * 0.50f, size.height * 0.78f)
                                cubicTo(size.width * 0.78f, size.height * 0.70f, size.width * 0.82f, size.height * 0.42f, size.width * 0.68f, size.height * 0.30f)
                            }
                            drawPath(path, c, style = s)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Sign out", color = Color(0xFFFF4757), fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(3.dp))
                        Text("End this session on this device", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ─── Bottom Tab Bar ───────────────────────────────────────────────────────────

enum class PrezzenceTab { HOME, PRACTICE, PROGRESS, PROFILE }

@Composable
fun PrezzenceBottomTabBar(
    activeTab: PrezzenceTab,
    onTabSelected: (PrezzenceTab) -> Unit,
) {
    val tabs = listOf(
        PrezzenceTab.HOME to "Home",
        PrezzenceTab.PRACTICE to "Practice",
        PrezzenceTab.PROGRESS to "Progress",
        PrezzenceTab.PROFILE to "Profile",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Bg)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Bg.copy(alpha = 0.96f))
                .navigationBarsPadding()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { (tab, label) ->
                val isActive = tab == activeTab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clickable { onTabSelected(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    TabIcon(tab = tab, active = isActive)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        label,
                        color = if (isActive) TextPrimary else TextSecondary.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun TabIcon(tab: PrezzenceTab, active: Boolean) {
    val color = if (active) Accent else TextSecondary.copy(alpha = 0.7f)
    Canvas(Modifier.size(22.dp)) {
        val s = Stroke(width = 2.2f, cap = StrokeCap.Round)
        when (tab) {
            PrezzenceTab.HOME -> {
                // House icon
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.50f, size.height * 0.12f)
                    lineTo(size.width * 0.10f, size.height * 0.48f)
                    lineTo(size.width * 0.22f, size.height * 0.48f)
                    lineTo(size.width * 0.22f, size.height * 0.88f)
                    lineTo(size.width * 0.78f, size.height * 0.88f)
                    lineTo(size.width * 0.78f, size.height * 0.48f)
                    lineTo(size.width * 0.90f, size.height * 0.48f)
                    close()
                }
                drawPath(path, color, style = if (active) androidx.compose.ui.graphics.drawscope.Fill else s)
            }
            PrezzenceTab.PRACTICE -> {
                // Target icon
                drawCircle(color, size.minDimension * 0.44f, style = s)
                drawCircle(color, size.minDimension * 0.26f, style = s)
                if (active) drawCircle(color, size.minDimension * 0.10f)
            }
            PrezzenceTab.PROGRESS -> {
                // Bar chart
                drawRoundRect(color, Offset(size.width * 0.10f, size.height * 0.44f), Size(size.width * 0.22f, size.height * 0.46f), CornerRadius(2f), style = if (active) androidx.compose.ui.graphics.drawscope.Fill else s)
                drawRoundRect(color, Offset(size.width * 0.39f, size.height * 0.24f), Size(size.width * 0.22f, size.height * 0.66f), CornerRadius(2f), style = if (active) androidx.compose.ui.graphics.drawscope.Fill else s)
                drawRoundRect(color, Offset(size.width * 0.68f, size.height * 0.10f), Size(size.width * 0.22f, size.height * 0.80f), CornerRadius(2f), style = if (active) androidx.compose.ui.graphics.drawscope.Fill else s)
            }
            PrezzenceTab.PROFILE -> {
                // Person icon
                drawCircle(color, size.minDimension * 0.20f, center = Offset(size.width * 0.50f, size.height * 0.34f), style = if (active) androidx.compose.ui.graphics.drawscope.Fill else s)
                drawArc(color, 195f, 150f, false, Offset(size.width * 0.16f, size.height * 0.52f), Size(size.width * 0.68f, size.height * 0.44f), style = s)
            }
        }
    }
}
