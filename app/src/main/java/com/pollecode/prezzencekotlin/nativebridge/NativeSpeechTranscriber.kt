package com.pollecode.prezzencekotlin.nativebridge

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import com.pollecode.prezzencekotlin.data.SessionScoring
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

data class SpeechCaptureResult(
    val transcript: String,
    val audioBase64: String? = null,
    val audioDurationSeconds: Int = 0,
)

/**
 * Records interview answer audio on-device. Transcription is performed on the backend only.
 */
class NativeSpeechTranscriber(
    private val context: Context,
    private val onPartial: (String) -> Unit,
    private val onFinal: (String) -> Unit,
    private val onError: (String) -> Unit,
) {
    private var recorder: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val recording = AtomicBoolean(false)
    private val pcmSamples = mutableListOf<Float>()
    private val pcmLock = Any()

    fun start(languageTag: String = "en-US") {
        stop(languageTag)
        startAudioCapture()
    }

    fun stop(languageTag: String = "en-US"): SpeechCaptureResult {
        return stopAudioCapture()
    }

    private fun startAudioCapture() {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) {
            onError("Microphone capture is not available on this device.")
            return
        }

        val bufferSize = max(minBuffer, SAMPLE_RATE)
        val audioRecord = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
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
        recordingThread = Thread({ readPcmLoop(audioRecord, bufferSize) }, "PrezzenceAnswerRecorder").also { it.start() }
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

    private fun stopAudioCapture(): SpeechCaptureResult {
        recording.set(false)
        runCatching { recorder?.stop() }
        runCatching { recordingThread?.join(1500) }
        runCatching { recorder?.release() }
        recorder = null
        recordingThread = null

        val samples = synchronized(pcmLock) { pcmSamples.toFloatArray() }
        val durationSeconds = (samples.size / SAMPLE_RATE.toFloat()).toInt().coerceAtLeast(0)
        val audioBase64 = if (samples.size >= SAMPLE_RATE / 4) {
            encodePcmToWavBase64(samples, SAMPLE_RATE)
        } else {
            null
        }

        if (samples.size < SAMPLE_RATE / 4) {
            onError("No clear speech was captured.")
        }

        return SpeechCaptureResult(
            transcript = "",
            audioBase64 = audioBase64,
            audioDurationSeconds = durationSeconds,
        )
    }

    private fun encodePcmToWavBase64(samples: FloatArray, sampleRate: Int): String {
        val pcmBytes = ByteArray(samples.size * 2)
        samples.forEachIndexed { index, sample ->
            val clipped = (sample * 32767f).toInt().coerceIn(-32768, 32767).toShort()
            pcmBytes[index * 2] = (clipped.toInt() and 0xFF).toByte()
            pcmBytes[index * 2 + 1] = ((clipped.toInt() shr 8) and 0xFF).toByte()
        }

        val header = ByteArrayOutputStream(44)
        val dataSize = pcmBytes.size
        val riffSize = 36 + dataSize
        header.write("RIFF".toByteArray())
        header.write(intToLittleEndian(riffSize))
        header.write("WAVE".toByteArray())
        header.write("fmt ".toByteArray())
        header.write(intToLittleEndian(16))
        header.write(shortToLittleEndian(1))
        header.write(shortToLittleEndian(1))
        header.write(intToLittleEndian(sampleRate))
        header.write(intToLittleEndian(sampleRate * 2))
        header.write(shortToLittleEndian(2))
        header.write(shortToLittleEndian(16))
        header.write("data".toByteArray())
        header.write(intToLittleEndian(dataSize))

        val wavBytes = header.toByteArray() + pcmBytes
        val encoded = Base64.encodeToString(wavBytes, Base64.NO_WRAP)
        return "data:audio/wav;base64,$encoded"
    }

    private fun intToLittleEndian(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte(),
        )
    }

    private fun shortToLittleEndian(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
        )
    }

    companion object {
        private const val SAMPLE_RATE = 16000

        fun isPlaceholderTranscript(text: String): Boolean =
            SessionScoring.isBlankTranscript(text)
    }
}
