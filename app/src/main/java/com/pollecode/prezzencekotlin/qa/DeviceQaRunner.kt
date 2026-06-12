package com.pollecode.prezzencekotlin.qa

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.pollecode.prezzencekotlin.BuildConfig
import com.pollecode.prezzencekotlin.nativebridge.NativeWhisperEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.max

class DeviceQaRunner(private val context: Context) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.MINUTES)
        .build()

    suspend fun runAll(languageTag: String, onStatus: suspend (String) -> Unit): List<DeviceQaResult> {
        val results = mutableListOf<DeviceQaResult>()
        results += microphoneCheck(onStatus)
        results += whisperModelCheck(onStatus)
        results += whisperEngineCheck(languageTag, onStatus)
        results += cameraProviderCheck(onStatus)
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

    private suspend fun whisperModelCheck(onStatus: suspend (String) -> Unit): DeviceQaResult = withContext(Dispatchers.IO) {
        onStatus("Checking Whisper model")
        val model = whisperModelFile()
        if (model.exists() && model.length() > MIN_MODEL_BYTES) {
            return@withContext DeviceQaResult("Whisper model", true, "Cached ${model.name} (${model.length() / 1024 / 1024} MB).")
        }
        val temp = File(model.parentFile, "${model.name}.download")
        if (temp.exists()) temp.delete()
        try {
            val request = Request.Builder().url(BuildConfig.PREZZENCE_WHISPER_MODEL_URL).build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext DeviceQaResult("Whisper model", false, "Download failed: HTTP ${response.code}.")
                val body = response.body ?: return@withContext DeviceQaResult("Whisper model", false, "Download returned no body.")
                temp.outputStream().use { output -> body.byteStream().copyTo(output) }
            }
            if (temp.length() <= MIN_MODEL_BYTES) {
                temp.delete()
                return@withContext DeviceQaResult("Whisper model", false, "Downloaded model is too small.")
            }
            if (model.exists()) model.delete()
            if (!temp.renameTo(model)) return@withContext DeviceQaResult("Whisper model", false, "Could not move model into cache.")
            DeviceQaResult("Whisper model", true, "Downloaded ${model.name} (${model.length() / 1024 / 1024} MB).")
        } catch (error: Throwable) {
            DeviceQaResult("Whisper model", false, error.message ?: "Model download failed.")
        }
    }

    private suspend fun whisperEngineCheck(languageTag: String, onStatus: suspend (String) -> Unit): DeviceQaResult = withContext(Dispatchers.IO) {
        onStatus("Checking Whisper JNI")
        val model = whisperModelFile()
        if (!model.exists() || model.length() <= MIN_MODEL_BYTES) return@withContext DeviceQaResult("Whisper JNI", false, "Model is not cached.")
        val silence = FloatArray(16000) { 0f }
        val loaded = runCatching { NativeWhisperEngine.transcribePcm(model.absolutePath, silence, languageTag) }
        if (loaded.isSuccess) DeviceQaResult("Whisper JNI", true, "Native library loaded and accepted ${languageTag} input.")
        else DeviceQaResult("Whisper JNI", false, loaded.exceptionOrNull()?.message ?: "Native transcription call failed.")
    }

    private suspend fun cameraProviderCheck(onStatus: suspend (String) -> Unit): DeviceQaResult {
        onStatus("Checking camera coach")
        if (!hasPermission(Manifest.permission.CAMERA)) {
            return DeviceQaResult("Camera coach", false, "Camera permission is not granted.")
        }
        return suspendCancellableCoroutine { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                val result = runCatching {
                    val provider = future.get()
                    provider.unbindAll()
                    DeviceQaResult("Camera coach", true, "CameraX provider opened successfully.")
                }.getOrElse { error ->
                    DeviceQaResult("Camera coach", false, error.message ?: "CameraX provider failed.")
                }
                cont.resume(result)
            }, ContextCompat.getMainExecutor(context))
        }
    }

    private suspend fun duixEndpointCheck(onStatus: suspend (String) -> Unit): DeviceQaResult = withContext(Dispatchers.IO) {
        onStatus("Checking Duix model endpoint")
        val url = BuildConfig.PREZZENCE_API_URL.trimEnd('/') + "/api/duix/models"
        try {
            val request = Request.Builder().url(url).get().build()
            http.newCall(request).execute().use { response ->
                DeviceQaResult("Duix timing", response.isSuccessful, if (response.isSuccessful) "Duix model endpoint is reachable." else "Endpoint returned HTTP ${response.code}.")
            }
        } catch (error: Throwable) {
            DeviceQaResult("Duix timing", false, error.message ?: "Duix endpoint check failed.")
        }
    }

    private fun whisperModelFile(): File {
        val dir = File(context.filesDir, "whisper-models").apply { mkdirs() }
        return File(dir, BuildConfig.PREZZENCE_WHISPER_MODEL_NAME)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val MIN_MODEL_BYTES = 1024 * 1024
    }
}

data class DeviceQaResult(
    val name: String,
    val passed: Boolean,
    val detail: String,
)