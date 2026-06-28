package com.pollecode.prezzencekotlin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pollecode.prezzencekotlin.data.AnswerResult

@Composable
private fun FlowShell(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(PrezzenceColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
    ) {
        if (onBack != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                PrezzenceNavButton(PrezzenceNavIcon.Back, onClick = onBack)
            }
        }
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text(title, color = PrezzenceColors.TextPrimary, fontSize = 34.sp, fontWeight = FontWeight.Black, lineHeight = 38.sp)
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
fun PrezzenceAuthCallbackScreen() {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        Box(Modifier.fillMaxSize().background(PrezzenceColors.Background), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(PrezzenceColors.Card)
                    .border(1.dp, PrezzenceColors.Border, RoundedCornerShape(24.dp))
                    .padding(24.dp),
            ) {
                CircularProgressIndicator(color = PrezzenceColors.Accent, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(16.dp))
                Text("Signing you in", color = PrezzenceColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

data class QuestionPickerItem(val index: Int, val role: String, val text: String)

@Composable
fun PrezzenceQuestionsPickerScreen(
    roles: List<String>,
    selectedRole: String,
    questions: List<QuestionPickerItem>,
    onBack: () -> Unit,
    onRoleSelected: (String) -> Unit,
    onQuestionSelected: (Int) -> Unit,
) {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        FlowShell(
            title = "Questions",
            subtitle = "Pick a role, choose one question, and start practicing.",
            onBack = onBack,
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                roles.forEach { role ->
                    Box(
                        Modifier
                            .clip(PrezzenceShape.Pill)
                            .background(if (role == selectedRole) PrezzenceColors.Accent else PrezzenceColors.Card)
                            .clickable { onRoleSelected(role) }
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                    ) {
                        Text(role, color = PrezzenceColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
            questions.forEach { item ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(PrezzenceColors.Card)
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(22.dp))
                        .clickable { onQuestionSelected(item.index) }
                        .padding(18.dp),
                ) {
                    Text(item.role, color = PrezzenceColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(6.dp))
                    Text(item.text, color = PrezzenceColors.TextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
                }
            }
        }
    }
}

@Composable
fun PrezzenceNetworkErrorScreen(
    returnToHome: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onKeepProgress: () -> Unit,
    onOpenWifiSettings: () -> Unit,
) {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        FlowShell(
            title = "Connection problem",
            subtitle = "We could not connect. Check your connection and try again.",
            onBack = onBack,
        ) {
            Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📶", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(999.dp)).background(PrezzenceColors.Accent.copy(alpha = 0.16f)).padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text("CONNECTION UNAVAILABLE", color = PrezzenceColors.Accent, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            Text("WHAT YOU CAN DO", color = PrezzenceColors.Accent, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            listOf(
                "Try again" to "Retry when your connection is stable",
                "Keep progress on this device" to "Your unfinished session can be continued later",
                "Check your network" to "Switch Wi-Fi or mobile data, then try again",
            ).forEach { (title, subtitle) ->
                Column(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(18.dp)).background(PrezzenceColors.Card).padding(14.dp),
                ) {
                    Text(title, color = PrezzenceColors.TextPrimary, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = PrezzenceColors.TextSecondary, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            PrezzencePrimaryButton(if (returnToHome) "Go back" else "Go back", onClick = onRetry)
        }
    }
}

@Composable
fun PrezzenceSessionErrorScreen(
    title: String,
    subtitle: String,
    badgeLabel: String,
    tips: List<String>,
    onBack: () -> Unit,
) {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        FlowShell(title = title, subtitle = subtitle, onBack = onBack) {
            Box(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.clip(RoundedCornerShape(999.dp)).background(PrezzenceColors.Accent.copy(alpha = 0.16f)).padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(badgeLabel, color = PrezzenceColors.Accent, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
            Text("WHAT YOU CAN DO", color = PrezzenceColors.Accent, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            tips.forEach { tip ->
                Text("• $tip", color = PrezzenceColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
            Spacer(Modifier.height(8.dp))
            PrezzencePrimaryButton("Go back", onClick = onBack)
        }
    }
}

@Composable
fun PrezzenceSessionInterruptedScreen(awayMinutes: Int = 0, onResume: () -> Unit) {
    val subtitle = when {
        awayMinutes >= 60 -> "You were away for a while. Pick up where you left off."
        awayMinutes > 1 -> "You were away for $awayMinutes minutes."
        awayMinutes == 1 -> "You were away for a minute."
        else -> "You stepped away from your session."
    }
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        FlowShell(title = "Session paused.", subtitle = subtitle, onBack = onResume) {
            PrezzencePrimaryButton("Resume Practice", onClick = onResume)
        }
    }
}

@Composable
fun PrezzenceSessionSaveErrorScreen(onRetry: () -> Unit) {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        FlowShell(title = "Session couldn't be saved.", subtitle = "We'll keep retrying before you lose anything.", onBack = onRetry) {
            PrezzencePrimaryButton("Manual Retry", onClick = onRetry)
        }
    }
}

@Composable
fun PrezzenceNotFoundScreen(onReturnHome: () -> Unit) {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        Box(Modifier.fillMaxSize().background(PrezzenceColors.Background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Box(
                    Modifier.size(120.dp).clip(RoundedCornerShape(60.dp)).background(PrezzenceColors.Accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("?", color = PrezzenceColors.Accent, fontSize = 48.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(24.dp))
                Text("Page not found", color = PrezzenceColors.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text(
                    "This page does not exist or the link is no longer available.",
                    color = PrezzenceColors.TextSecondary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                PrezzencePrimaryButton("Return home", onClick = onReturnHome)
            }
        }
    }
}

@Composable
fun PrezzencePaymentSuccessScreen(onGoToProfile: () -> Unit) {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        FlowShell(title = "Prezzence Pro unlocked", subtitle = "Your account is ready for deeper practice.", onBack = onGoToProfile) {
            Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                Text("✓", color = PrezzenceColors.Success, fontSize = 48.sp, fontWeight = FontWeight.Black)
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(PrezzenceColors.Card).padding(18.dp),
            ) {
                Text("You're ready for deeper practice", color = PrezzenceColors.TextPrimary, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "You now have access to more sessions, deeper reports, and shareable score cards.",
                    color = PrezzenceColors.TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                )
            }
            Spacer(Modifier.height(16.dp))
            PrezzencePrimaryButton("Go to Profile", onClick = onGoToProfile)
        }
    }
}

@Composable
fun PrezzencePaywallScreen(
    hasPremium: Boolean,
    priceLabel: String?,
    onBack: () -> Unit,
    onSubscribe: () -> Unit,
    onRestore: () -> Unit,
) {
    val displayPrice = priceLabel ?: "…"
    val benefits = listOf(
        "Unlimited interview sessions" to "Practice as much as you need",
        "All 3 AI interviewers" to "Maya, Jonas & Sophia",
        "AI-powered scoring" to "Deep coaching with improved answers",
        "Panel mode" to "Multiple interviewers at once",
        "Full session reports" to "Radar charts & PDF export",
        "Company web research" to "Tailored questions for your target role",
    )
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        FlowShell(
            title = "Upgrade to Pro",
            subtitle = "Unlock unlimited interview practice",
            onBack = onBack,
        ) {
            if (hasPremium) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(PrezzenceColors.Success.copy(alpha = 0.10f))
                        .border(1.dp, PrezzenceColors.Success.copy(alpha = 0.24f), RoundedCornerShape(24.dp)).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("✓", color = PrezzenceColors.Success, fontSize = 36.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("You're a Pro member", color = PrezzenceColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("All features are unlocked.", color = PrezzenceColors.TextSecondary, fontSize = 14.sp)
                }
            } else {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(PrezzenceColors.Accent.copy(alpha = 0.10f))
                        .border(1.dp, PrezzenceColors.Accent.copy(alpha = 0.24f), RoundedCornerShape(24.dp)).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(displayPrice, color = PrezzenceColors.TextPrimary, fontSize = 42.sp, fontWeight = FontWeight.Black)
                    Text("per month · Cancel anytime", color = PrezzenceColors.TextSecondary, fontSize = 13.sp)
                }
                Spacer(Modifier.height(16.dp))
                Text("WHAT YOU GET", color = PrezzenceColors.Accent, fontSize = 11.sp, fontWeight = FontWeight.Black)
                benefits.forEach { (title, desc) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.04f)).padding(12.dp),
                    ) {
                        Text("✓", color = PrezzenceColors.Success, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 10.dp))
                        Column {
                            Text(title, color = PrezzenceColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(desc, color = PrezzenceColors.TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Free plan: 3 sessions/month with Sophia",
                    color = PrezzenceColors.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(16.dp))
            PrezzencePrimaryButton(
                if (hasPremium) "Manage Subscription" else "Start Pro - $displayPrice / month",
                onClick = onSubscribe,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Restore purchases",
                color = PrezzenceColors.TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onRestore).padding(vertical = 12.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun PrezzenceSubscriptionScreen(
    hasPremium: Boolean,
    onBack: () -> Unit,
    onSubscribe: () -> Unit,
    onCheckPlan: () -> Unit,
    onRestore: () -> Unit,
) {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        FlowShell(title = "Subscription", onBack = onBack) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(PrezzenceColors.Card).padding(20.dp),
            ) {
                Text("PLAN", color = PrezzenceColors.Accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text("Prezzence Pro", color = PrezzenceColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(12.dp))
                listOf("Unlimited practice sessions", "Deep coaching reports", "Shareable score cards", "Export to PDF").forEach { benefit ->
                    Text("✓  $benefit", color = PrezzenceColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("Status: ${if (hasPremium) "Active" else "Inactive"}", color = PrezzenceColors.TextSecondary, fontSize = 13.sp)
            }
            Spacer(Modifier.height(16.dp))
            PrezzencePrimaryButton("Subscribe", onClick = onSubscribe)
            Spacer(Modifier.height(10.dp))
            PrezzenceSecondaryButton("Check plan", onClick = onCheckPlan)
            Spacer(Modifier.height(10.dp))
            PrezzenceSecondaryButton("Restore purchases", onClick = onRestore)
        }
    }
}

@Composable
fun PrezzenceCoachingFeedbackScreen(
    question: String,
    answer: AnswerResult,
    onContinue: () -> Unit,
) {
    val scoreColor = when {
        answer.score >= 75 -> PrezzenceColors.Success
        answer.score >= 55 -> Color(0xFFFFB020)
        else -> PrezzenceColors.Danger
    }
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = PrezzenceColors.Background)) {
        FlowShell(title = "Answer Result", onBack = onContinue) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${answer.score}", color = scoreColor, fontSize = 56.sp, fontWeight = FontWeight.Black)
                Text("out of 100", color = PrezzenceColors.TextSecondary, fontSize = 14.sp)
            }
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(PrezzenceColors.Card).padding(16.dp)) {
                Text("Question", color = PrezzenceColors.TextPrimary, fontWeight = FontWeight.Black)
                Text(question, color = PrezzenceColors.TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
            }
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(PrezzenceColors.Card).padding(16.dp)) {
                Text("Feedback", color = PrezzenceColors.TextPrimary, fontWeight = FontWeight.Black)
                Text(answer.feedback, color = PrezzenceColors.TextSecondary, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(12.dp))
                if (answer.improvedAnswer.isNotBlank()) {
                    Text("Model answer", color = PrezzenceColors.TextPrimary, fontWeight = FontWeight.Black)
                    Text(answer.improvedAnswer, color = PrezzenceColors.TextSecondary, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp))
                }
                listOf("What" to answer.what, "How" to answer.how, "Why" to answer.why).forEach { (label, value) ->
                    Text("$label: $value", color = PrezzenceColors.TextPrimary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            PrezzencePrimaryButton("Continue", onClick = onContinue)
        }
    }
}
