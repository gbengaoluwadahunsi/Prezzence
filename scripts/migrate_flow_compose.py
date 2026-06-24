from pathlib import Path

p = Path("app/src/main/java/com/pollecode/prezzencekotlin/MainActivity.kt")
text = p.read_text(encoding="utf-8")


def replace_func(name: str, new_body: str) -> None:
    global text
    marker = f"    private fun {name}("
    start = text.find(marker)
    if start < 0:
        raise SystemExit(f"not found: {name}")
    search_from = start + len(marker)
    next_idx = text.find("\n    private fun ", search_from)
    if next_idx < 0:
        raise SystemExit(f"no next func after {name}")
    text = text[:start] + new_body + text[next_idx + 1 :]


replace_func(
    "showAuthCallback",
    """    private fun showAuthCallback(token: String? = null, error: String? = null) {
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
""",
)

replace_func(
    "showQuestions",
    """    private fun showQuestions() {
        val questions = appState.questions().mapIndexed { index, question ->
            QuestionPickerItem(index = index, role = question.role, text = question.text)
        }
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceQuestionsPickerScreen(
                    roles = PrezzenceDefaults.roles,
                    selectedRole = appState.selectedRole,
                    questions = questions,
                    onBack = { showHome() },
                    onRoleSelected = { role ->
                        appState.selectedRole = role
                        showQuestions()
                    },
                    onQuestionSelected = { index ->
                        appState.currentQuestionIndex = index
                        showRoomSetup()
                    },
                )
            }
        })
    }
""",
)

replace_func(
    "showNetworkError",
    """    private fun showNetworkError(returnToHome: Boolean = false) {
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
""",
)

replace_func(
    "showSessionInterrupted",
    """    private fun showSessionInterrupted() {
        setScreen(ComposeView(this).apply {
            setContent { PrezzenceSessionInterruptedScreen(onResume = { showHome() }) }
        })
    }
""",
)

replace_func(
    "showSessionSaveError",
    """    private fun showSessionSaveError() {
        setScreen(ComposeView(this).apply {
            setContent { PrezzenceSessionSaveErrorScreen(onRetry = { showHome() }) }
        })
    }
""",
)

replace_func(
    "showNotFound",
    """    private fun showNotFound(returnTo: () -> Unit = { showHome() }) {
        setScreen(ComposeView(this).apply {
            setContent { PrezzenceNotFoundScreen(onReturnHome = returnTo) }
        })
    }
""",
)

replace_func(
    "showPaymentSuccess",
    """    private fun showPaymentSuccess() {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzencePaymentSuccessScreen(onGoToProfile = { showHome(PrezzenceTab.PROFILE) })
            }
        })
    }
""",
)

replace_func(
    "showPaywall",
    """    private fun showPaywall() {
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
""",
)

replace_func(
    "showSubscription",
    """    private fun showSubscription() {
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
""",
)

replace_func(
    "showCoachingFeedback",
    """    private fun showCoachingFeedback(question: String, answer: AnswerResult, onContinue: () -> Unit) {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceCoachingFeedbackScreen(
                    question = question,
                    answer = answer,
                    onContinue = onContinue,
                )
            }
        })
    }
""",
)

# showSessionCreateError - replace body only (keep SessionErrorInfo data class before it)
old_session_error = """        val column = baseColumn()
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
        setScreen(scroll(column))"""

new_session_error = """        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceSessionErrorScreen(
                    title = errorInfo.title,
                    subtitle = errorInfo.subtitle,
                    badgeLabel = errorInfo.badgeLabel,
                    tips = errorInfo.tips,
                    onBack = { showHome() },
                )
            }
        })"""

if old_session_error not in text:
    raise SystemExit("session create error block not found")
text = text.replace(old_session_error, new_session_error)

p.write_text(text, encoding="utf-8")
print("done")
