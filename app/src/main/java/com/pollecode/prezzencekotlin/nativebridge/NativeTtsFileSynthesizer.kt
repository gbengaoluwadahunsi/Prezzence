package com.pollecode.prezzencekotlin.nativebridge

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.io.File
import java.util.Locale
import java.util.UUID

class NativeTtsFileSynthesizer(
    context: Context,
    private val onReadyStateChanged: (Boolean) -> Unit = {},
) {
    private val appContext = context.applicationContext
    private val outputDir = File(appContext.cacheDir, "prezzence-tts").apply { mkdirs() }
    private var ready = false
    private var tts: TextToSpeech? = null
    private val pending = ArrayDeque<() -> Unit>()

    init {
        tts = TextToSpeech(appContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts?.language = Locale.US
                drainPending()
            }
            onReadyStateChanged(ready)
        }
    }

    fun synthesize(
        text: String,
        source: String,
        onFile: (File) -> Unit,
        onError: (String) -> Unit,
    ) {
        val work = {
            val clean = text.trim()
            if (clean.isBlank()) {
                onError("No speech text provided.")
            } else {
                synthesizeNow(clean, source, onFile, onError)
            }
        }
        if (ready) work() else pending.add(work)
    }

    fun shutdown() {
        pending.clear()
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
        ready = false
    }

    private fun synthesizeNow(
        text: String,
        source: String,
        onFile: (File) -> Unit,
        onError: (String) -> Unit,
    ) {
        val engine = tts ?: run {
            onError("Text to speech is not available.")
            return
        }
        val utteranceId = "$source-${UUID.randomUUID()}"
        val file = File(outputDir, "$utteranceId.wav")
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(doneId: String?) {
                if (doneId == utteranceId && file.exists() && file.length() > 44) {
                    onFile(file)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(errorId: String?) {
                if (errorId == utteranceId) onError("Unable to generate interviewer speech.")
            }

            override fun onError(errorId: String?, errorCode: Int) {
                if (errorId == utteranceId) onError("Unable to generate interviewer speech.")
            }
        })
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        val result = engine.synthesizeToFile(text, params, file, utteranceId)
        if (result == TextToSpeech.ERROR) onError("Unable to start interviewer speech generation.")
    }

    private fun drainPending() {
        while (pending.isNotEmpty()) {
            pending.removeFirst().invoke()
        }
    }
}
