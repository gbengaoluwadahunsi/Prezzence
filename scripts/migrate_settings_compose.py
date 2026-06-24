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
    "showFeedback",
    """    private fun showFeedback() {
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
""",
)

replace_func(
    "showDeviceQa",
    """    private fun showDeviceQa(
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
""",
)

replace_func(
    "showPrivacySettings",
    """    private fun showPrivacySettings() {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzencePrivacySettingsScreen(
                    productImprovementEnabled = privacyImprovementEnabled,
                    dataRetentionEnabled = privacyRetentionEnabled,
                    onBack = { showSettings() },
                    onToggleProductImprovement = { enabled ->
                        privacyImprovementEnabled = enabled
                    },
                    onToggleDataRetention = { enabled ->
                        privacyRetentionEnabled = enabled
                    },
                    onExportData = { exportUserData() },
                    onDone = { showSettings() },
                )
            }
        })
    }
""",
)

replace_func(
    "showLegal",
    """    private fun showLegal() {
        setScreen(ComposeView(this).apply {
            setContent {
                PrezzenceLegalScreen(onBack = { showSettings() })
            }
        })
    }
""",
)

replace_func(
    "showResumeProfile",
    """    private fun showResumeProfile() {
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
                            return@PrezzenceResumeProfileScreen
                        }
                        resumeProfileStatusState.value = "Saving profile..."
                        scope.launch {
                            val saved = backend.saveResumeText(appState.authToken.ifBlank { null }, resumeText)
                            resumeProfileStatusState.value = if (saved != null) {
                                "Saved: ${saved.fileName}\\n${saved.summary.take(180)}"
                            } else {
                                "Could not save profile. Sign in and try again."
                            }
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
""",
)

replace_func(
    "showNotifications",
    """    private fun showNotifications() {
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
""",
)

text = text.replace(
    "\n\n    private var privacyImprovementEnabled = true\n    private var privacyRetentionEnabled = false\n\n    private fun showPrivacySettings()",
    "\n\n    private fun showPrivacySettings()",
)

p.write_text(text, encoding="utf-8")
print("done")
