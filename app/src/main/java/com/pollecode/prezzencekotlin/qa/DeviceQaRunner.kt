package com.pollecode.prezzencekotlin.qa

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.pollecode.prezzencekotlin.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.max

class DeviceQaRunner(
    private val context: Context,
    private val authToken: String = "",
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.MINUTES)
        .build()

    suspend fun runAll(languageTag: String, onStatus: suspend (String) -> Unit): List<DeviceQaResult> {
        val results = mutableListOf<DeviceQaResult>()
        results += microphoneCheck(onStatus)
        results += backendTranscriptionCheck(onStatus)
        results += duixEndpointCheck(onStatus)
        return results
    }

    private suspend fun microphoneCheck(onStatus: suspend (String) -> Unit): DeviceQaResult = withContext(Dispatchers.IO) {
        onStatus("Checking microphone")
        if (!hasPermission(Manifest.permission.RECORD_AUDIO)) {
            return@withContext DeviceQaResult("Microphone", false, "Record audio permission is not granted.")
        }
        val minBuffer = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuffer <= 0) return@withContext DeviceQaResult("Microphone", false, "AudioRecord minimum buffer is unavailable.")
        val recorder = runCatching {
            AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, max(minBuffer, 16000))
        }.getOrNull() ?: return@withContext DeviceQaResult("Microphone", false, "AudioRecord could not be created.")
        try {
            recorder.startRecording()
            val buffer = ShortArray(2048)
            val read = recorder.read(buffer, 0, buffer.size)
            val hasSignal = read > 0 && buffer.take(read).any { it.toInt() != 0 }
            DeviceQaResult("Microphone", read > 0, if (hasSignal) "Captured PCM signal." else "Captured silence; check input level on device.")
        } catch (error: Throwable) {
            DeviceQaResult("Microphone", false, error.message ?: "Microphone capture failed.")
        } finally {
            runCatching { recorder.stop() }
            recorder.release()
        }
    }

    private suspend fun backendTranscriptionCheck(onStatus: suspend (String) -> Unit): DeviceQaResult = withContext(Dispatchers.IO) {
        onStatus("Checking backend transcription API")
        val url = BuildConfig.PREZZENCE_API_URL.trimEnd('/') + "/healthz"
        try {
            val request = Request.Builder().url(url).get().build()
            http.newCall(request).execute().use { response ->
                DeviceQaResult(
                    "Backend transcription",
                    response.isSuccessful,
                    if (response.isSuccessful) "Backend API is reachable for server-side transcription."
                    else "Backend returned HTTP ${response.code}.",
                )
            }
        } catch (error: Throwable) {
            DeviceQaResult("Backend transcription", false, error.message ?: "Backend API check failed.")
        }
    }


    private suspend fun duixEndpointCheck(onStatus: suspend (String) -> Unit): DeviceQaResult = withContext(Dispatchers.IO) {
        onStatus("Checking Duix model endpoint")
        val url = BuildConfig.PREZZENCE_API_URL.trimEnd('/') + "/api/duix/models"
        try {
            val requestBuilder = Request.Builder().url(url).get()
            if (authToken.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer $authToken")
            }
            http.newCall(requestBuilder.build()).execute().use { response ->
                DeviceQaResult("Duix timing", response.isSuccessful, if (response.isSuccessful) "Duix model endpoint is reachable." else "Endpoint returned HTTP ${response.code}.")
            }
        } catch (error: Throwable) {
            DeviceQaResult("Duix timing", false, error.message ?: "Duix endpoint check failed.")
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}

data class DeviceQaResult(
    val name: String,
    val passed: Boolean,
    val detail: String,
)