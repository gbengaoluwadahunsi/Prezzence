package com.pollecode.prezzencekotlin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pollecode.prezzencekotlin.data.NotificationItem
import com.pollecode.prezzencekotlin.qa.DeviceQaResult

@Composable
private fun SettingsShell(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    bottomPadding: Int = 32,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(PrezzenceColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomPadding.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PrezzenceNavButton(PrezzenceNavIcon.Back, onClick = onBack)
        }
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text(title, color = PrezzenceColors.TextPrimary, fontSize = 36.sp, fontWeight = FontWeight.Black, lineHeight = 40.sp)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(subtitle, color = PrezzenceColors.TextSecondary, fontSize = 15.sp, lineHeight = 22.sp)
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text,
        color = PrezzenceColors.Accent,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(bottom = 10.dp, top = 4.dp),
    )
}

@Composable
private fun SettingsGroupCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(PrezzenceColors.Card)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(28.dp))
            .padding(8.dp),
    ) {
        content()
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.06f)),
    )
}

@Composable
private fun SettingsNavRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = PrezzenceColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(subtitle, color = PrezzenceColors.TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
        Text("›", color = PrezzenceColors.TextSecondary, fontSize = 22.sp)
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = PrezzenceColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = PrezzenceColors.TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
        }
        Box(
            Modifier
                .size(width = 48.dp, height = 28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (checked) PrezzenceColors.Accent else Color.White.copy(alpha = 0.12f))
                .clickable { onCheckedChange(!checked) }
                .padding(3.dp),
        ) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (checked) 1f else 0.7f))
                    .then(if (checked) Modifier.align(Alignment.CenterEnd) else Modifier.align(Alignment.CenterStart)),
            )
        }
    }
}

@Composable
private fun SettingsInfoCard(title: String, body: String, accent: Color = PrezzenceColors.Success) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.24f), RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Text(title, color = accent, fontSize = 17.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text(body, color = PrezzenceColors.TextPrimary.copy(alpha = 0.88f), fontSize = 14.sp, lineHeight = 21.sp)
    }
}

@Composable
private fun SettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int = 1,
    enabled: Boolean = true,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        textStyle = TextStyle(color = PrezzenceColors.TextPrimary, fontSize = 15.sp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PrezzenceColors.Card)
            .border(1.dp, PrezzenceColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        minLines = minLines,
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(placeholder, color = PrezzenceColors.TextSecondary, fontSize = 15.sp)
            }
            inner()
        },
    )
}

@Composable
fun PrezzenceSimpleMessageScreen(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
) {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        SettingsShell(title = title, subtitle = subtitle, onBack = onBack) {}
    }
}

data class SettingsNavItem(
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
)

@Composable
fun PrezzenceSettingsScreen(
    language: String,
    subscriptionActive: Boolean,
    userEmail: String,
    showDeviceQa: Boolean = false,
    onBack: () -> Unit,
    onInterviewerSetup: () -> Unit,
    onLanguage: () -> Unit,
    onAccount: () -> Unit,
    onSubscription: () -> Unit,
    onPrivacy: () -> Unit,
    onNotificationPreferences: () -> Unit,
    onFeedback: () -> Unit,
    onDeviceQa: () -> Unit,
    onAccessibility: () -> Unit,
    onAppSettings: () -> Unit,
    onResumeProfile: () -> Unit,
    onHelp: () -> Unit,
    onSignOut: () -> Unit,
    onSignIn: () -> Unit,
) {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        SettingsShell(
            title = "Settings",
            subtitle = "Manage account access, language, subscription, privacy, and interview preferences.",
            onBack = onBack,
        ) {
            SettingsSectionLabel("INTERVIEW SETUP")
            SettingsGroupCard {
                SettingsNavRow("Start new practice", "Choose a track, role, and interviewer style", onInterviewerSetup)
                SettingsDivider()
                SettingsNavRow("App language", "Current: ${language.uppercase()}", onLanguage)
            }
            Spacer(Modifier.height(12.dp))
            SettingsSectionLabel("ACCOUNT")
            SettingsGroupCard {
                listOf(
                    SettingsNavItem("Account details", "Name, email, password reset, and your practice profile", onAccount),
                    SettingsNavItem("Subscription", if (subscriptionActive) "Active" else "Free plan, upgrade", onSubscription),
                    SettingsNavItem("Privacy and deletion", "Data, devices, and deletion controls", onPrivacy),
                    SettingsNavItem("Notification preferences", "Push, reminders, and milestone alerts", onNotificationPreferences),
                    SettingsNavItem("Feedback", "Send product feedback", onFeedback),
                ).forEachIndexed { index, item ->
                    if (index > 0) SettingsDivider()
                    SettingsNavRow(item.title, item.subtitle, item.onClick)
                }
            }
            Spacer(Modifier.height(12.dp))
            SettingsSectionLabel("TOOLS")
            SettingsGroupCard {
                buildList {
                    if (showDeviceQa) add(SettingsNavItem("Device QA", "Mic, backend, camera, and Duix checks", onDeviceQa))
                    add(SettingsNavItem("Accessibility", "Text size, contrast, and motion", onAccessibility))
                    add(SettingsNavItem("App Settings", "Wi-Fi only downloads", onAppSettings))
                    add(SettingsNavItem("Resume / CV profile", "Tune questions with your resume", onResumeProfile))
                    add(SettingsNavItem("Help & FAQ", "Common questions and contact support", onHelp))
                }.forEachIndexed { index, item ->
                    if (index > 0) SettingsDivider()
                    SettingsNavRow(item.title, item.subtitle, item.onClick)
                }
            }
            Spacer(Modifier.height(12.dp))
            SettingsSectionLabel("SIGN OUT")
            if (userEmail.isNotBlank()) {
                SettingsGroupCard {
                    SettingsNavRow("Sign out", "Signed in as $userEmail", onSignOut)
                }
            } else {
                SettingsGroupCard {
                    SettingsNavRow("Sign in", "Not signed in", onSignIn)
                }
            }
        }
    }
}

@Composable
fun PrezzenceSettingsLanguageScreen(
    selectedLanguage: String,
    languages: List<String>,
    onBack: () -> Unit,
    onLanguageSelected: (String) -> Unit,
) {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        SettingsShell(
            title = "Language",
            subtitle = "Choose the speech and session language used by the native interview flow.",
            onBack = onBack,
        ) {
            SettingsGroupCard {
                languages.forEachIndexed { index, tag ->
                    if (index > 0) SettingsDivider()
                    SettingsNavRow(tag, if (selectedLanguage == tag) "Selected" else "Tap to select") {
                        onLanguageSelected(tag)
                    }
                }
            }
        }
    }
}

@Composable
fun PrezzenceAccountScreen(
    fullName: String,
    email: String,
    focus: String,
    signedIn: Boolean,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onDeleteAccount: () -> Unit,
    onSignOut: () -> Unit,
    onSignIn: () -> Unit,
) {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        SettingsShell(title = "Account details", onBack = onBack) {
            if (signedIn) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(PrezzenceColors.Card)
                            .border(1.dp, PrezzenceColors.Accent.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            fullName.take(2).uppercase().ifBlank { "U" },
                            color = PrezzenceColors.Accent,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(fullName.ifBlank { "Prezzence user" }, color = PrezzenceColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text(email.uppercase(), color = PrezzenceColors.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                }
                Spacer(Modifier.height(8.dp))
                SettingsSectionLabel("PROFILE DETAILS")
                SettingsGroupCard {
                    SettingsNavRow("Display name", fullName.ifBlank { "Set your name" }, onEditProfile)
                    SettingsDivider()
                    SettingsNavRow("Email address", email) {}
                    SettingsDivider()
                    SettingsNavRow("Interview focus", focus.ifBlank { "Set your focus" }, onEditProfile)
                }
                Spacer(Modifier.height(16.dp))
                SettingsSectionLabel("ACCOUNT ACTIONS")
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(PrezzenceShape.Pill)
                        .background(PrezzenceColors.Danger.copy(alpha = 0.14f))
                        .border(1.dp, PrezzenceColors.Danger.copy(alpha = 0.28f), PrezzenceShape.Pill)
                        .clickable(onClick = onDeleteAccount)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Delete account", color = PrezzenceColors.Danger, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                PrezzenceSecondaryButton("Sign out", onClick = onSignOut)
            } else {
                Text("You are not signed in.", color = PrezzenceColors.TextSecondary, fontSize = 15.sp)
                Spacer(Modifier.height(16.dp))
                PrezzencePrimaryButton("Sign in", onClick = onSignIn)
            }
        }
    }
}

@Composable
fun PrezzenceDeleteAccountScreen(
    onBack: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmText by remember { mutableStateOf("") }
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        SettingsShell(
            title = "Delete Account",
            subtitle = "Type DELETE to permanently remove data.",
            onBack = onBack,
        ) {
            SettingsInfoCard(
                "This deletes all sessions",
                "Scores, recordings, progress history, and saved interview settings will be removed.",
                PrezzenceColors.Danger,
            )
            Spacer(Modifier.height(16.dp))
            SettingsSectionLabel("CONFIRM")
            SettingsTextField(confirmText, { confirmText = it }, "Type DELETE")
            Spacer(Modifier.height(20.dp))
            PrezzencePrimaryButton("Delete Account", enabled = confirmText.trim() == "DELETE", onClick = onDelete)
        }
    }
}

@Composable
fun PrezzenceEditProfileScreen(
    initialName: String,
    email: String,
    initialFocus: String,
    onBack: () -> Unit,
    onSave: (name: String, focus: String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var focus by remember { mutableStateOf(initialFocus) }
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        SettingsShell(title = "Edit profile", onBack = onBack) {
            Text(email.ifBlank { "Signed in account" }, color = PrezzenceColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))
            SettingsSectionLabel("NAME")
            SettingsTextField(name, { name = it }, "Name")
            Spacer(Modifier.height(14.dp))
            SettingsSectionLabel("EMAIL")
            SettingsTextField(email, {}, "Email", enabled = false)
            Spacer(Modifier.height(14.dp))
            SettingsSectionLabel("INTERVIEW FOCUS")
            SettingsTextField(focus, { focus = it }, "Interview Focus")
            Spacer(Modifier.height(20.dp))
            PrezzencePrimaryButton("Save profile") { onSave(name.trim(), focus.trim()) }
        }
    }
}

@Composable
fun PrezzenceHelpScreen(
    onBack: () -> Unit,
    onContactSupport: () -> Unit,
) {
    val faqs = listOf(
        "How do I start practicing?" to "Tap Practice tracks on Home or open the Practice tab, pick a track, then follow role and interviewer setup.",
        "Which interviewers are included?" to "Prezzence includes Maya (peer reviewer), Jonas (hiring manager), and Sophia (domain expert). Pro unlocks all three plus panel mode.",
        "Why is my score low or missing?" to "Scores need a clear spoken answer. Silence, mic checks, and very short replies are not scored.",
        "How do subscriptions work?" to "Free includes limited sessions with Sophia. Pro unlocks unlimited practice, all interviewers, panel mode, and PDF export.",
        "How do I delete my data?" to "Open Settings → Privacy and deletion to export your history or delete your account.",
    )
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        SettingsShell(
            title = "Help",
            subtitle = "Answers to common questions about practice, scoring, and subscriptions.",
            onBack = onBack,
        ) {
            SettingsSectionLabel("FREQUENTLY ASKED")
            faqs.forEach { (question, answer) ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(PrezzenceColors.Card)
                        .padding(14.dp),
                ) {
                    Text(question, color = PrezzenceColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(answer, color = PrezzenceColors.TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            PrezzencePrimaryButton("Email support", onClick = onContactSupport)
        }
    }
}

@Composable
fun PrezzenceNotificationSettingsScreen(
    pushEnabled: Boolean,
    emailEnabled: Boolean,
    remindersEnabled: Boolean,
    achievementsEnabled: Boolean,
    productUpdatesEnabled: Boolean,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onTogglePush: (Boolean) -> Unit,
    onToggleEmail: (Boolean) -> Unit,
    onToggleReminders: (Boolean) -> Unit,
    onToggleAchievements: (Boolean) -> Unit,
    onToggleProductUpdates: (Boolean) -> Unit,
) {
    var push by remember { mutableStateOf(pushEnabled) }
    var email by remember { mutableStateOf(emailEnabled) }
    var reminders by remember { mutableStateOf(remindersEnabled) }
    var achievements by remember { mutableStateOf(achievementsEnabled) }
    var productUpdates by remember { mutableStateOf(productUpdatesEnabled) }

    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        SettingsShell(
            title = "Notifications",
            subtitle = "Choose when Prezzence reminds you to practice or review progress. Preferences sync to your account.",
            onBack = onBack,
        ) {
            SettingsSectionLabel("CHANNELS")
            SettingsGroupCard {
                SettingsToggleRow("Push notifications", "Practice reminders on this device", push) {
                    push = it; onTogglePush(it)
                }
                SettingsDivider()
                SettingsToggleRow("Email summaries", "Weekly progress and coaching summaries", email) {
                    email = it; onToggleEmail(it)
                }
            }
            Spacer(Modifier.height(12.dp))
            SettingsSectionLabel("ACTIVITY ALERTS")
            SettingsGroupCard {
                SettingsToggleRow("Practice reminders", "Get reminded before your interview date", reminders) {
                    reminders = it; onToggleReminders(it)
                }
                SettingsDivider()
                SettingsToggleRow("Milestone alerts", "Get alerts when your score or streak improves", achievements) {
                    achievements = it; onToggleAchievements(it)
                }
                SettingsDivider()
                SettingsToggleRow("Product updates", "New features and useful interview tips", productUpdates) {
                    productUpdates = it; onToggleProductUpdates(it)
                }
            }
            Spacer(Modifier.height(16.dp))
            PrezzencePrimaryButton("Done", onClick = onDone)
        }
    }
}

@Composable
fun PrezzenceAppSettingsScreen(
    wifiOnlyDownloads: Boolean,
    onBack: () -> Unit,
    onToggleWifiOnly: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    var wifiOnly by remember { mutableStateOf(wifiOnlyDownloads) }
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        SettingsShell(
            title = "App Settings",
            subtitle = "Manage accessibility and app settings.",
            onBack = onBack,
        ) {
            SettingsGroupCard {
                SettingsToggleRow("Download over Wi-Fi only", "Only download model files on Wi-Fi", wifiOnly) {
                    wifiOnly = it; onToggleWifiOnly(it)
                }
            }
            Spacer(Modifier.height(16.dp))
            PrezzencePrimaryButton("Save Settings", onClick = onSave)
        }
    }
}

@Composable
fun PrezzenceAccessibilitySettingsScreen(
    largeText: Boolean,
    highContrast: Boolean,
    reducedMotion: Boolean,
    onBack: () -> Unit,
    onToggleLargeText: (Boolean) -> Unit,
    onToggleHighContrast: (Boolean) -> Unit,
    onToggleReducedMotion: (Boolean) -> Unit,
) {
    var large by remember { mutableStateOf(largeText) }
    var contrast by remember { mutableStateOf(highContrast) }
    var motion by remember { mutableStateOf(reducedMotion) }

    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        SettingsShell(
            title = "Accessibility",
            subtitle = "Adjust text size, contrast, and motion to make the app easier to use.",
            onBack = onBack,
        ) {
            SettingsSectionLabel("VISION & DISPLAY")
            SettingsGroupCard {
                SettingsToggleRow("Large text", "200% font scale across all screens", large) {
                    large = it; onToggleLargeText(it)
                }
                SettingsDivider()
                SettingsToggleRow("High contrast", "Enhanced edge definition for UI elements", contrast) {
                    contrast = it; onToggleHighContrast(it)
                }
                SettingsDivider()
                SettingsToggleRow("Reduced Motion", "Minimize animations and motion effects", motion) {
                    motion = it; onToggleReducedMotion(it)
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, PrezzenceColors.Border, RoundedCornerShape(28.dp))
                    .padding(24.dp),
            ) {
                Text("PREVIEW", color = PrezzenceColors.Success, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(12.dp))
                Text("Easy to read", color = PrezzenceColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text(
                    "\"Tell me about a time you solved a difficult problem.\" Text should stay readable without clipping.",
                    color = PrezzenceColors.TextPrimary.copy(alpha = 0.75f),
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                )
            }
        }
    }
}

@Composable
fun PrezzenceFeedbackScreen(
    onBack: () -> Unit,
    onSubmit: (rating: Int, comment: String) -> Boolean,
) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        if (submitted) {
            SettingsShell(title = "Thank You!", subtitle = "Your feedback helps us improve the app.", onBack = onBack) {
                Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    Text("✓", color = PrezzenceColors.Success, fontSize = 64.sp, fontWeight = FontWeight.Black)
                }
            }
        } else {
            SettingsShell(title = "Feedback", onBack = onBack) {
                Text("How was it?", color = PrezzenceColors.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text("Rate your interview practice experience.", color = PrezzenceColors.TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    (1..5).forEach { star ->
                        Text(
                            if (star <= rating) "★" else "☆",
                            color = if (star <= rating) PrezzenceColors.Success else PrezzenceColors.TextSecondary,
                            fontSize = 36.sp,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clickable { rating = star },
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                SettingsSectionLabel("COMMENT (OPTIONAL)")
                SettingsTextField(comment, { comment = it }, "What should we improve?", minLines = 5)
                Spacer(Modifier.height(16.dp))
                PrezzencePrimaryButton("Submit feedback") {
                    if (onSubmit(rating, comment.trim())) {
                        submitted = true
                    }
                }
            }
        }
    }
}

@Composable
fun PrezzenceDeviceQaScreen(
    status: String,
    running: Boolean,
    results: List<DeviceQaResult>,
    onBack: () -> Unit,
    onRequestPermissions: () -> Unit,
    onRunChecks: () -> Unit,
    onClearModelCache: () -> Unit,
    onRunAgain: () -> Unit,
) {
    val pendingChecks = listOf(
        "Microphone PCM capture",
        "Backend transcription API reachability",
        "CameraX provider open",
        "Duix model endpoint reachability",
    )
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        SettingsShell(title = "Device QA", subtitle = status, onBack = onBack) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(PrezzenceShape.Pill)
                        .background(PrezzenceColors.Card)
                        .clickable(onClick = onRequestPermissions)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Allow mic/camera", color = PrezzenceColors.TextPrimary, fontWeight = FontWeight.Bold)
                }
                Box(
                    Modifier
                        .weight(1f)
                        .clip(PrezzenceShape.Pill)
                        .background(PrezzenceColors.Accent)
                        .clickable(enabled = !running, onClick = onRunChecks)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (running) "Running" else "Run checks", color = PrezzenceColors.TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(PrezzenceColors.Card)
                    .padding(18.dp),
            ) {
                Text("Avatar Model Cache", color = PrezzenceColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text(
                    "If avatar models fail to load or show corrupted, clear cache to force fresh download.",
                    color = PrezzenceColors.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
                Spacer(Modifier.height(12.dp))
                PrezzenceSecondaryButton("Clear Avatar Model Cache", onClick = onClearModelCache)
            }
            Spacer(Modifier.height(12.dp))
            if (results.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(PrezzenceColors.Card)
                        .padding(18.dp),
                ) {
                    Text("Checks", color = PrezzenceColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    pendingChecks.forEach { check ->
                        Text("…  $check", color = PrezzenceColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            } else {
                results.forEach { result ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (result.passed) PrezzenceColors.Success.copy(alpha = 0.08f) else PrezzenceColors.Danger.copy(alpha = 0.08f))
                            .border(
                                1.dp,
                                (if (result.passed) PrezzenceColors.Success else PrezzenceColors.Danger).copy(alpha = 0.22f),
                                RoundedCornerShape(24.dp),
                            )
                            .padding(18.dp),
                    ) {
                        Text(
                            "${if (result.passed) "✓" else "✗"}  ${result.name}",
                            color = PrezzenceColors.TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(result.detail, color = PrezzenceColors.TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
                    }
                }
                PrezzenceSecondaryButton("Run again", onClick = onRunAgain)
            }
        }
    }
}

@Composable
fun PrezzencePrivacySettingsScreen(
    onBack: () -> Unit,
    onExportData: () -> Unit,
    onDone: () -> Unit,
) {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        SettingsShell(
            title = "Privacy",
            subtitle = "Control how your interview recordings, transcripts, scores, and account data are handled.",
            onBack = onBack,
        ) {
            SettingsInfoCard(
                "Protected data",
                "Your voice data, transcripts, and scores are stored privately and used to power your own coaching.",
            )
            Spacer(Modifier.height(12.dp))
            SettingsInfoCard(
                "Your controls",
                "Export your session history anytime. Delete your account from Account details to remove stored practice data.",
            )
            Spacer(Modifier.height(16.dp))
            SettingsSectionLabel("YOUR DATA")
            SettingsGroupCard {
                SettingsNavRow("Export your data", "Download your session history as JSON", onExportData)
            }
            Spacer(Modifier.height(16.dp))
            PrezzencePrimaryButton("Done", onClick = onDone)
        }
    }
}

@Composable
fun PrezzenceLegalScreen(
    onBack: () -> Unit,
) {
    val sections = listOf(
        "Privacy" to "Prezzence stores account, session, transcript, score, feedback, notification, and optional resume profile data to provide interview coaching. You can remove your resume profile or delete account data from the app.",
        "Terms" to "Scores and AI feedback are coaching signals only. Prezzence does not guarantee job offers, admissions, hiring outcomes, or interview success.",
        "Avatar attribution" to "Prezzence uses Duix Mobile avatar technology for on-device interviewer animation. Powered by Duix.com.",
    )
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        SettingsShell(title = "Privacy & Terms", onBack = onBack) {
            sections.forEach { (heading, body) ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(PrezzenceColors.Card)
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
                        .padding(18.dp),
                ) {
                    Text(heading, color = PrezzenceColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Text(body, color = PrezzenceColors.TextSecondary, fontSize = 14.sp, lineHeight = 21.sp)
                }
            }
        }
    }
}

@Composable
fun PrezzenceResumeProfileScreen(
    initialText: String,
    statusText: String,
    loading: Boolean,
    onBack: () -> Unit,
    onTextChange: (String) -> Unit,
    onUploadFile: () -> Unit,
    onSaveText: (String) -> Boolean,
    onDelete: () -> Unit,
) {
    var resumeText by remember(initialText) { mutableStateOf(initialText) }
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        SettingsShell(
            title = "Resume / CV",
            subtitle = "Upload a PDF, DOCX, TXT, or MD resume, or paste resume text. Prezzence uses it to tune interview questions and coaching.",
            onBack = onBack,
        ) {
            SettingsTextField(resumeText, {
                resumeText = it
                onTextChange(it)
            }, "Paste resume text here", minLines = 8)
            Spacer(Modifier.height(12.dp))
            Text(
                if (loading) "Loading saved profile..." else statusText,
                color = PrezzenceColors.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(16.dp))
            PrezzencePrimaryButton("Upload CV", onClick = onUploadFile)
            Spacer(Modifier.height(10.dp))
            PrezzenceSecondaryButton("Save text") {
                if (onSaveText(resumeText.trim())) {
                    // parent updates status
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(PrezzenceShape.Pill)
                    .background(PrezzenceColors.Danger.copy(alpha = 0.12f))
                    .border(1.dp, PrezzenceColors.Danger.copy(alpha = 0.24f), PrezzenceShape.Pill)
                    .clickable(onClick = onDelete)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Delete", color = PrezzenceColors.Danger, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun PrezzenceNotificationsInboxScreen(
    items: List<NotificationItem>,
    loading: Boolean,
    onBack: () -> Unit,
    onMarkRead: (NotificationItem) -> Unit,
    onDelete: (NotificationItem) -> Unit,
) {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        SettingsShell(title = "Notifications", subtitle = "Inbox updates and reminders.", onBack = onBack) {
            when {
                loading -> Text("Loading notifications...", color = PrezzenceColors.TextSecondary, fontSize = 14.sp)
                items.isEmpty() -> Text("No notifications yet.", color = PrezzenceColors.TextSecondary, fontSize = 14.sp)
                else -> items.forEach { item ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (item.isRead) PrezzenceColors.Surface else PrezzenceColors.Card)
                            .padding(18.dp),
                    ) {
                        Text(item.title, color = PrezzenceColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(6.dp))
                        Text(item.message.ifBlank { "No message" }, color = PrezzenceColors.TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
                        if (item.createdAt.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(item.createdAt, color = PrezzenceColors.TextSecondary, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PrezzenceSecondaryButton(
                                if (item.isRead) "Read" else "Mark read",
                                modifier = Modifier.weight(1f),
                                onClick = { onMarkRead(item) },
                            )
                            PrezzenceSecondaryButton("Delete", modifier = Modifier.weight(1f), onClick = { onDelete(item) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrezzenceMicDeniedScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onPrivacy: () -> Unit,
    onDeviceQa: () -> Unit,
) {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        SettingsShell(
            title = "Microphone access",
            subtitle = "Prezzence needs microphone access to record your answer and give feedback.",
            onBack = onBack,
        ) {
            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎤", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(PrezzenceColors.Danger.copy(alpha = 0.16f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text("MIC BLOCKED", color = PrezzenceColors.Danger, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
            SettingsSectionLabel("HOW TO FIX IT")
            SettingsGroupCard {
                SettingsNavRow("Phone settings", "Allow microphone access in your device settings", onOpenSettings)
                SettingsDivider()
                SettingsNavRow("Privacy", "Recording starts only when you answer a question", onPrivacy)
                SettingsDivider()
                SettingsNavRow("Microphone check", "Make sure your microphone works in other apps", onDeviceQa)
            }
            Spacer(Modifier.height(12.dp))
            PrezzencePrimaryButton("Open Device Settings", onClick = onOpenSettings)
        }
    }
}

@Composable
fun PrezzenceLowStorageScreen(
    onBack: () -> Unit,
    onManageStorage: () -> Unit,
) {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        SettingsShell(
            title = "Low Storage Warning",
            subtitle = "Clear space for best recording quality.",
            onBack = onBack,
        ) {
            listOf(
                Triple("Recording may fail", "Recommended", PrezzenceColors.Success),
                Triple("Free up storage", "Ready", PrezzenceColors.Accent),
                Triple("Try again", "Ready", PrezzenceColors.Accent),
            ).forEach { (title, badge, color) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(PrezzenceColors.Card)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(title, color = PrezzenceColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(badge, color = PrezzenceColors.TextSecondary, fontSize = 12.sp)
                    }
                    Text("On", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            PrezzencePrimaryButton("Manage storage", onClick = onManageStorage)
        }
    }
}
