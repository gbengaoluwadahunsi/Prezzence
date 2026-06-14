package com.pollecode.prezzencekotlin.nativebridge

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.pollecode.prezzencekotlin.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

class NativeSpeechTranscriber(
    private val context: Context,
    private val onPartial: (String) -> Unit,
    private val onFinal: (String) -> Unit,
    private val onError: (String) -> Unit,
) {
    private var recognizer: SpeechRecognizer? = null
    private var latestText: String = ""
    private var recorder: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val recording = AtomicBoolean(false)
    private val pcmSamples = mutableListOf<Float>()
    private val pcmLock = Any()
    private var currentLanguageTag: String = "en-US"

    fun start(languageTag: String = "en-US") {
        currentLanguageTag = languageTag
        stop(languageTag)
        latestText = ""
        if (BuildConfig.PREZZENCE_ENABLE_WHISPER_CPP) {
            startWhisperCapture()
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition is not available on this device.")
            return
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit

                override fun onError(error: Int) {
                    if (latestText.isBlank()) onError(errorMessage(error)) else onFinal(latestText)
                }

                override fun onResults(results: Bundle?) {
                    val text = bestResult(results)
                    latestText = text.ifBlank { latestText }
                    onFinal(latestText)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val text = bestResult(partialResults)
                    if (text.isNotBlank()) {
                        latestText = text
                        onPartial(text)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        recognizer?.startListening(intent)
    }

    fun stop(languageTag: String = "en-US"): String {
        if (BuildConfig.PREZZENCE_ENABLE_WHISPER_CPP) return stopWhisperCapture(languageTag)
        val text = latestText
        runCatching { recognizer?.stopListening() }
        runCatching { recognizer?.cancel() }
        runCatching { recognizer?.destroy() }
        recognizer = null
        return text
    }

    private fun startWhisperCapture() {
        val minBuffer = AudioRecord.getMinBufferSize(
            WHISPER_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) {
            onError("Microphone capture is not available on this device.")
            return
        }

        val bufferSize = max(minBuffer, WHISPER_SAMPLE_RATE)
        val audioRecord = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                WHISPER_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
            )
        }.getOrElse {
            onError("Microphone capture failed to start.")
            return
        }

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            onError("Microphone capture failed to initialize.")
            return
        }

        synchronized(pcmLock) { pcmSamples.clear() }
        recorder = audioRecord
        recording.set(true)
        recordingThread = Thread({ readPcmLoop(audioRecord, bufferSize) }, "PrezzenceWhisperRecorder").also { it.start() }
        runCatching { audioRecord.startRecording() }.onFailure {
            recording.set(false)
            onError("Microphone capture failed to start.")
        }
    }

    private fun readPcmLoop(audioRecord: AudioRecord, bufferSize: Int) {
        val buffer = ShortArray(bufferSize / 2)
        while (recording.get()) {
            val read = audioRecord.read(buffer, 0, buffer.size)
            if (read > 0) {
                synchronized(pcmLock) {
                    repeat(read) { index ->
                        pcmSamples.add((buffer[index] / 32768.0f).coerceIn(-1.0f, 1.0f))
                    }
                }
            }
        }
    }

    private fun stopWhisperCapture(languageTag: String): String {
        recording.set(false)
        runCatching { recorder?.stop() }
        runCatching { recordingThread?.join(1500) }
        runCatching { recorder?.release() }
        recorder = null
        recordingThread = null

        val samples = synchronized(pcmLock) { pcmSamples.toFloatArray() }
        if (samples.size < WHISPER_SAMPLE_RATE / 2) {
            onError("No clear speech was captured.")
            return latestText
        }

        val modelPath = runCatching { ensureWhisperModel() }.getOrElse { error ->
            onError(error.message ?: "Whisper model is not available.")
            return latestText
        }

        val text = runCatching {
            NativeWhisperEngine.transcribePcm(modelPath.absolutePath, samples, languageTag).trim()
        }.getOrElse { error ->
            onError(error.message ?: "Native transcription failed.")
            ""
        }

        if (text.isNotBlank()) {
            latestText = text
            onFinal(text)
        } else if (latestText.isBlank()) {
            onError("No clear speech was captured.")
        }
        return latestText
    }

    private fun ensureWhisperModel(): File {
        val modelDir = File(context.filesDir, "whisper-models").apply { mkdirs() }
        val model = File(modelDir, BuildConfig.PREZZENCE_WHISPER_MODEL_NAME)
        if (model.exists() && model.length() > MIN_MODEL_BYTES) return model

        val temp = File(modelDir, "${model.name}.download")
        if (temp.exists()) temp.delete()
        val request = Request.Builder().url(BuildConfig.PREZZENCE_WHISPER_MODEL_URL).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("Whisper model download failed: HTTP ${response.code}")
            val body = response.body ?: throw IllegalStateException("Whisper model download returned no data.")
            temp.outputStream().use { output -> body.byteStream().copyTo(output) }
        }
        if (temp.length() <= MIN_MODEL_BYTES) {
            temp.delete()
            throw IllegalStateException("Whisper model download was incomplete.")
        }
        if (model.exists()) model.delete()
        if (!temp.renameTo(model)) throw IllegalStateException("Whisper model could not be cached.")
        return model
    }

    private fun bestResult(bundle: Bundle?): String {
        return bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim() ?: ""
    }

    private fun errorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording failed."
            SpeechRecognizer.ERROR_CLIENT -> "Speech client failed."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
            SpeechRecognizer.ERROR_NETWORK -> "Network speech recognition failed."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition timed out."
            SpeechRecognizer.ERROR_NO_MATCH -> "No clear speech was captured."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy."
            SpeechRecognizer.ERROR_SERVER -> "Speech service failed."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech was detected."
            else -> "Speech recognition failed."
        }
    }

    companion object {
        private const val WHISPER_SAMPLE_RATE = 16000
        private const val MIN_MODEL_BYTES = 1024 * 1024
        private val httpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .build()
    }
}
