package com.pollecode.prezzencekotlin.nativebridge

import android.content.Context
import android.graphics.Color
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import android.widget.ProgressBar
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import ai.guiji.duix.sdk.client.Constant
import ai.guiji.duix.sdk.client.DUIX
import ai.guiji.duix.sdk.client.render.DUIXRenderer
import ai.guiji.duix.sdk.client.render.DUIXTextureView
import ai.guiji.duix.sdk.client.util.ZipUtil
import com.pollecode.prezzencekotlin.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

class NativeDuixAvatarView(context: Context) : FrameLayout(context) {
    interface Listener {
        fun onModelReady(modelName: String) {}
        fun onModelError(modelName: String, message: String?) {}
        fun onSpeechReady(source: String, modelName: String?, durationMs: Int?) {}
        fun onSpeechStart(source: String?, modelName: String?) {}
        fun onSpeechEnd(source: String?, modelName: String?) {}
        fun onSpeechError(source: String?, modelName: String?, message: String?) {}
    }

    var listener: Listener? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(600, TimeUnit.SECONDS)  // 10 minutes for large ~50MB model file downloads
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val textureView = DUIXTextureView(context)
    private var renderer: DUIXRenderer? = null
    private var duix: DUIX? = null
    private var preparedModelName: String? = null
    private var preparingModelName: String? = null
    private var duixInitReady = false
    private var currentModelName: String = "Sofia"
    private var currentSpeechSource: String? = null
    private var playToken = AtomicInteger(0)
    private val duixSampleRate = 16_000

    private val modelRoot = File(context.getExternalFilesDir("duix"), "model")
    private val cacheRoot = File(context.cacheDir, "duix-audio")
    // Try backend API first, fall back to GitHub for base config and Supabase for models
    private val apiBase = BuildConfig.PREZZENCE_API_URL.trimEnd('/') + "/api/duix/models/download/"
    // Direct GitHub URLs as fallback
    private val githubBase = "https://github.com/duixcom/Duix-Mobile/releases/download/v1.0.0/"

    init {
        setBackgroundColor(Color.rgb(12, 11, 18))
        textureView.setEGLContextClientVersion(2)
        textureView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        textureView.setPreserveEGLContextOnPause(true)
        textureView.isOpaque = false
        textureView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(textureView)
        renderer = DUIXRenderer(context, textureView)
        renderer?.setScaleType(0)
        textureView.setRenderer(renderer)
        textureView.renderMode = DUIXTextureView.RENDERMODE_WHEN_DIRTY
        cacheRoot.mkdirs()
        modelRoot.mkdirs()
    }

    fun setModelName(name: String) {
        currentModelName = normalizeModelName(name)
        if (preparedModelName != currentModelName && preparingModelName != currentModelName) {
            prepareModel(currentModelName)
        }
    }

    fun prepareModel(name: String = currentModelName) {
        val modelName = normalizeModelName(name)
        currentModelName = modelName
        if (preparedModelName == modelName || preparingModelName == modelName) return
        preparingModelName = modelName

        // Show loading overlay for first-time download (models may take 10-30s)
        if (!isModelCached(context, modelName)) {
            showOverlay(0)
        } else {
            hideOverlay()
        }

        scope.launch {
            try {
                val dirs = withContext(Dispatchers.IO) {
                    ensureModelAvailable(modelName)
                }
                bindDuix(modelName, dirs.first, dirs.second)
                preparedModelName = modelName
                preparingModelName = null
                // NOTE: onModelReady fires from CALLBACK_EVENT_INIT_READY inside bindDuix(),
                // not here — the render thread finishes asynchronously.
            } catch (error: Throwable) {
                preparingModelName = null
                hideOverlay()
                Log.e("PrezzenceDuix", "Model preparation failed for $modelName", error)
                listener?.onModelError(modelName, error.message)
            }
        }
    }

    fun speakAudioUri(uri: String, source: String = "question") {
        val modelName = currentModelName
        val token = playToken.incrementAndGet()
        scope.launch {
            try {
                if (preparedModelName != modelName || duix == null) {
                    if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", "Model not ready, waiting for availability for $modelName")
                    val startTime = System.currentTimeMillis()
                    val dirs = withContext(Dispatchers.IO) { ensureModelAvailable(modelName) }
                    val elapsed = System.currentTimeMillis() - startTime
                    if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", "Model loaded in ${elapsed}ms for $modelName")
                    bindDuix(modelName, dirs.first, dirs.second)
                    preparedModelName = modelName
                }
                if (!waitForDuixReady(modelName)) {
                    throw IllegalStateException("Avatar is still initializing")
                }
                val audioPath = withContext(Dispatchers.IO) {
                    preparePlayableWav(uri, source)
                }
                if (token != playToken.get()) return@launch
                currentSpeechSource = source

                listener?.onSpeechReady(source, modelName, null)
                textureView.renderMode = DUIXTextureView.RENDERMODE_CONTINUOUSLY
                if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", "playAudio model=$modelName source=$source path=$audioPath bytes=${File(audioPath).length()}")
                duix?.playAudio(audioPath)
            } catch (error: Throwable) {
                Log.e("PrezzenceDuix", "speakAudioUri failed model=$modelName source=$source", error)
                listener?.onSpeechError(source, modelName, error.message)
            }
        }
    }

    fun stopSpeaking() {
        playToken.incrementAndGet()
        runCatching { duix?.stopAudio() }
        textureView.renderMode = DUIXTextureView.RENDERMODE_WHEN_DIRTY
    }

    fun release() {
        stopSpeaking()
        runCatching { duix?.release() }
        runCatching { renderer?.release() }
        scope.cancel()
    }

    private fun bindDuix(modelName: String, baseConfigDir: File, modelDir: File) {
        if (preparedModelName == modelName && duix != null) return
        val currentRenderer = renderer ?: DUIXRenderer(context, textureView).also { renderer = it }
        duix?.release()
        duixInitReady = false
        duix = DUIX(context, modelName, currentRenderer) { event, msg, info ->
            when (event) {
                Constant.CALLBACK_EVENT_INIT_READY -> {
                    duixInitReady = true
                    runCatching { duix?.setVolume(1.0f) }
                    runCatching { duix?.startRandomMotion(false) }
                    textureView.requestRender()
                    hideOverlay()
                    listener?.onModelReady(modelName)
                }
                Constant.CALLBACK_EVENT_INIT_ERROR -> {
                    duixInitReady = false
                    hideOverlay()
                    listener?.onModelError(modelName, msg)
                }
                Constant.CALLBACK_EVENT_AUDIO_PLAY_START -> listener?.onSpeechStart(currentSpeechSource, modelName)
                Constant.CALLBACK_EVENT_AUDIO_PLAY_END -> {
                    textureView.renderMode = DUIXTextureView.RENDERMODE_WHEN_DIRTY
                    listener?.onSpeechEnd(currentSpeechSource, modelName)
                    currentSpeechSource = null
                }
                Constant.CALLBACK_EVENT_AUDIO_PLAY_ERROR -> {
                    listener?.onSpeechError(currentSpeechSource, modelName, msg)
                    currentSpeechSource = null
                }
            }
        }
        duix?.init()
    }

    private suspend fun waitForDuixReady(modelName: String): Boolean {
        repeat(120) {
            if (preparedModelName == modelName && duix != null && duixInitReady) return true
            delay(50)
        }
        return false
    }

    private fun ensureModelAvailable(modelName: String): Pair<File, File> {
        return ensureModelFilesAvailable(context, modelName, client) { progress ->
            mainHandler.post { showOverlay(progress) }
        }
    }

    private fun baseConfigLooksReady(dir: File): Boolean =
        dir.exists() &&
            dir.isDirectory &&
            listOf("alpha_model.b", "alpha_model.p", "cacert.p", "weight_168u.b", "wenet.o")
                .all { File(dir, it).isFile }

    private fun avatarModelLooksReady(dir: File): Boolean =
        dir.exists() &&
            dir.isDirectory &&
            listOf("bbox.j", "config.j", "dh_model.b", "dh_model.p")
                .all { File(dir, it).isFile } &&
            (File(dir, "weight_168u.bin").isFile || File(dir, "weight_168u.b").isFile) &&
            File(dir, "raw_jpgs").isDirectory

    private fun downloadAndUnzip(name: String, destination: File) {
        destination.parentFile?.mkdirs()
        val zip = File(modelRoot, "$name.zip")
        val request = Request.Builder().url(apiBase + "$name.zip").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("Model download failed: ${response.code}")
            response.body?.byteStream()?.use { input ->
                FileOutputStream(zip).use { output -> input.copyTo(output) }
            } ?: throw IllegalStateException("Model download returned empty body")
        }
        destination.deleteRecursively()
        val ok = ZipUtil.unzip(zip.absolutePath, modelRoot.absolutePath, null)
        if (!ok) throw IllegalStateException("Model unzip failed")
        repairSingleNestedDirectory(destination, name)
        File(modelRoot, "tmp/$name").mkdirs()
        zip.delete()
    }

    private fun repairSingleNestedDirectory(destination: File, expectedName: String) {
        val files = destination.listFiles() ?: return
        if (files.size == 1 && files[0].isDirectory && files[0].name.equals(expectedName, ignoreCase = true)) {
            files[0].listFiles()?.forEach { child -> child.renameTo(File(destination, child.name)) }
            files[0].deleteRecursively()
        }
    }

    private fun preparePlayableWav(uri: String, source: String): String {
        // Key the cache file on the audio URL (unique per synthesis) so a new
        // answer's audio can never collide with a stale cached file. Using a
        // resettable counter caused old clips (e.g. question 1) to be replayed.
        val key = audioCacheKey(uri)
        val extension = uri.substringBefore("?").substringAfterLast(".", "audio").take(8)
        val input = File(cacheRoot, "speech-$key-input.$extension")
        if (uri.startsWith("http://") || uri.startsWith("https://")) {
            val request = Request.Builder().url(uri).build()
            client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("Audio download failed: ${response.code}")
            response.body?.byteStream()?.use { stream ->
                FileOutputStream(input).use { out -> stream.copyTo(out) }
            } ?: throw IllegalStateException("Audio download returned empty body")
            }
        } else {
            val parsed = Uri.parse(uri)
            if (parsed.scheme == "content") {
                context.contentResolver.openInputStream(parsed)?.use { stream ->
                    FileOutputStream(input).use { out -> stream.copyTo(out) }
                } ?: throw IllegalStateException("Unable to open audio content")
            } else {
                File(uri.removePrefix("file://")).copyTo(input, overwrite = true)
            }
        }
        val wav = ensureDuixWav(input, key, source)
        if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", "Prepared WAV source=$source inputExt=$extension bytes=${wav.length()}")
        return wav.absolutePath
    }

    private fun audioCacheKey(uri: String): String {
        val base = uri.substringBefore("?")
        return runCatching {
            val digest = java.security.MessageDigest.getInstance("SHA-1").digest(base.toByteArray())
            digest.joinToString("") { "%02x".format(it) }.take(20)
        }.getOrElse { (base.hashCode().toLong() and 0xffffffffL).toString(16) }
    }

    private fun ensureDuixWav(input: File, key: String, source: String): File {
        if (input.extension.equals("wav", ignoreCase = true) && isDuixCompatibleWav(input)) return input
        val output = File(cacheRoot, "speech-$key-$source.wav")
        if (output.exists() && output.length() > 44) return output
        decodeTo16kMonoWav(input, output)
        input.delete()
        return output
    }

    private fun isDuixCompatibleWav(input: File): Boolean {
        if (!input.exists() || input.length() <= 44) return false
        return try {
            RandomAccessFile(input, "r").use { file ->
                val riff = ByteArray(4)
                file.readFully(riff)
                if (String(riff) != "RIFF") return@use false
                file.seek(8)
                val wave = ByteArray(4)
                file.readFully(wave)
                if (String(wave) != "WAVE") return@use false

                var audioFormat: Int? = null
                var channels: Int? = null
                var sampleRate: Int? = null
                var bitsPerSample: Int? = null
                var hasData = false

                while (file.filePointer + 8 <= file.length()) {
                    val chunkIdBytes = ByteArray(4)
                    file.readFully(chunkIdBytes)
                    val chunkId = String(chunkIdBytes)
                    val chunkSize = readIntLe(file).toLong().coerceAtLeast(0)
                    val nextChunk = (file.filePointer + chunkSize + (chunkSize % 2)).coerceAtMost(file.length())
                    when (chunkId) {
                        "fmt " -> if (chunkSize >= 16) {
                            audioFormat = readShortLe(file)
                            channels = readShortLe(file)
                            sampleRate = readIntLe(file)
                            file.skipBytes(6)
                            bitsPerSample = readShortLe(file)
                        }
                        "data" -> hasData = chunkSize > 0
                    }
                    if (hasData && audioFormat == 1 && channels == 1 && sampleRate == duixSampleRate && bitsPerSample == 16) {
                        return@use true
                    }
                    file.seek(nextChunk)
                }
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    private fun decodeTo16kMonoWav(input: File, output: File) {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        try {
            extractor.setDataSource(input.absolutePath)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw IllegalStateException("No audio track found")
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: throw IllegalStateException("Missing audio mime")
            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(format, null, null, 0)
            decoder.start()

            var sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) format.getInteger(MediaFormat.KEY_SAMPLE_RATE) else duixSampleRate
            var channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1) else 1
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT
            val monoPcm = ByteArrayOutputStream()
            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            while (!outputDone) {
                if (!inputDone) {
                    val inputIndex = decoder.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val buffer = decoder.getInputBuffer(inputIndex)!!
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0) {
                            decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                when (val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 10_000)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outFormat = decoder.outputFormat
                        if (outFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) sampleRate = outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        if (outFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) channels = outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
                        if (outFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) pcmEncoding = outFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    else -> if (outputIndex >= 0) {
                        val buffer = decoder.getOutputBuffer(outputIndex)!!
                        if (bufferInfo.size > 0) {
                            buffer.position(bufferInfo.offset)
                            buffer.limit(bufferInfo.offset + bufferInfo.size)
                            appendMono16BitPcm(buffer, bufferInfo.size, channels, pcmEncoding, monoPcm)
                        }
                        outputDone = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        decoder.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }
            writeWav(output, padForDuix(resamplePcm16(monoPcm.toByteArray(), sampleRate, duixSampleRate)), duixSampleRate)
        } finally {
            runCatching { decoder?.stop() }
            decoder?.release()
            extractor.release()
        }
    }

    private fun appendMono16BitPcm(
        buffer: java.nio.ByteBuffer,
        size: Int,
        channelCount: Int,
        pcmEncoding: Int,
        out: ByteArrayOutputStream,
    ) {
        val data = ByteArray(size)
        buffer.get(data)

        if (pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT) {
            val floats = java.nio.ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
            while (floats.remaining() >= channelCount) {
                var sum = 0f
                repeat(channelCount) { sum += floats.get() }
                val sample = (sum / channelCount * Short.MAX_VALUE).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                writeShortLe(out, sample.toShort())
            }
            return
        }

        var index = 0
        val frameSize = channelCount * 2
        while (index + frameSize <= data.size) {
            var sum = 0
            repeat(channelCount) { channel ->
                val sampleIndex = index + channel * 2
                val sample = ((data[sampleIndex + 1].toInt() shl 8) or (data[sampleIndex].toInt() and 0xff)).toShort()
                sum += sample.toInt()
            }
            writeShortLe(out, (sum / channelCount).toShort())
            index += frameSize
        }
    }

    private fun resamplePcm16(input: ByteArray, srcRate: Int, dstRate: Int): ByteArray {
        if (srcRate == dstRate || input.isEmpty()) return input
        val inputSamples = input.size / 2
        val outputSamples = ((inputSamples.toDouble() * dstRate) / srcRate).roundToInt().coerceAtLeast(1)
        val output = ByteArrayOutputStream(outputSamples * 2)
        val sampleBuffer = java.nio.ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN)
        val samples = ShortArray(inputSamples) { sampleBuffer.getShort() }
        for (index in 0 until outputSamples) {
            val sourcePosition = index.toDouble() * srcRate / dstRate
            val leftIndex = sourcePosition.toInt().coerceIn(0, inputSamples - 1)
            val rightIndex = (leftIndex + 1).coerceAtMost(inputSamples - 1)
            val fraction = sourcePosition - leftIndex
            val interpolated = samples[leftIndex] + ((samples[rightIndex] - samples[leftIndex]) * fraction)
            writeShortLe(output, interpolated.roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
        }
        return output.toByteArray()
    }

    private fun writeWav(file: File, pcmData: ByteArray, sampleRate: Int) {
        FileOutputStream(file).use { out ->
            val dataSize = pcmData.size
            out.write("RIFF".toByteArray())
            out.writeIntLE(36 + dataSize)
            out.write("WAVEfmt ".toByteArray())
            out.writeIntLE(16)
            out.writeShortLE(1)
            out.writeShortLE(1)
            out.writeIntLE(sampleRate)
            out.writeIntLE(sampleRate * 2)
            out.writeShortLE(2)
            out.writeShortLE(16)
            out.write("data".toByteArray())
            out.writeIntLE(dataSize)
            out.write(pcmData)
        }
    }

    private fun padForDuix(pcmData: ByteArray): ByteArray {
        // Add 600ms of silence at the START so the DUIX render thread has time
        // to spin up and sync before real audio arrives — fixes initial choppy/stuttery speech.
        val leadInBytes = (duixSampleRate * 2 * 0.60).toInt() // 600ms @ 16kHz mono 16-bit = 19200 bytes
        val leadIn = ByteArray(leadInBytes)
        val withLeadIn = leadIn + pcmData

        // Ensure minimum total length (1 second) for DUIX compatibility
        val minBytes = duixSampleRate * 2
        if (withLeadIn.size >= minBytes) return withLeadIn
        return withLeadIn + ByteArray(minBytes - withLeadIn.size)
    }

    private fun writeShortLe(out: ByteArrayOutputStream, value: Short) {
        out.write(value.toInt() and 0xff)
        out.write((value.toInt() shr 8) and 0xff)
    }

    private fun readShortLe(file: RandomAccessFile): Int {
        val b0 = file.read()
        val b1 = file.read()
        if (b0 < 0 || b1 < 0) return 0
        return (b0 and 0xff) or ((b1 and 0xff) shl 8)
    }

    private fun readIntLe(file: RandomAccessFile): Int {
        val b0 = file.read()
        val b1 = file.read()
        val b2 = file.read()
        val b3 = file.read()
        if (b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0) return 0
        return (b0 and 0xff) or ((b1 and 0xff) shl 8) or ((b2 and 0xff) shl 16) or ((b3 and 0xff) shl 24)
    }

    private fun FileOutputStream.writeIntLE(value: Int) {
        write(byteArrayOf(value.toByte(), (value shr 8).toByte(), (value shr 16).toByte(), (value shr 24).toByte()))
    }

    private fun FileOutputStream.writeShortLE(value: Int) {
        write(byteArrayOf(value.toByte(), (value shr 8).toByte()))
    }

    private fun normalizeModelName(name: String): String = when (name.lowercase()) {
        "maya", "lily" -> "Lily"
        "jonas", "oliver" -> "Oliver"
        else -> "Sofia"
    }

    private fun displayInitialForModel(): String = when (currentModelName.lowercase()) {
        "lily" -> "M"
        "oliver" -> "J"
        else -> "S"
    }

    private fun showOverlay(progress: Int) {
        mainHandler.post {
            val existing = findViewWithTag<LinearLayout>("duixOverlay")
            val messageText = "One-time setup. Your 3D interviewer is downloading now — future interviews start instantly."
            val progressText = "$progress%"
            val phaseMessage = when (progress) {
                in 0..4 -> "Preparing one-time setup..."
                in 5..92 -> "Downloading interviewer engine..."
                in 93..99 -> "Installing & extracting..."
                else -> "Finalizing your interviewer..."
            }

            if (existing == null) {
                val accentColor = Color.rgb(108, 99, 255)
                val cyberCyan = Color.rgb(26, 216, 166)
                addView(LinearLayout(context).apply {
                    tag = "duixOverlay"
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(24), dp(24), dp(24), dp(24))
                    setBackgroundColor(Color.rgb(8, 8, 14))
                    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

                    // 3D Avatar Compiling HUD Image - downscaled to fit perfectly
                    val imageResId = context.resources.getIdentifier("avatar_compiling_hud", "drawable", context.packageName)
                    if (imageResId != 0) {
                        addView(ImageView(context).apply {
                            tag = "avatarHudImage"
                            setImageResource(imageResId)
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            layoutParams = LinearLayout.LayoutParams(dp(120), dp(120)).apply {
                                bottomMargin = dp(16)
                                topMargin = dp(16)
                            }
                            // Start pulsing micro-animation
                            ObjectAnimator.ofFloat(this, "alpha", 0.5f, 1.0f).apply {
                                duration = 1500
                                repeatMode = ValueAnimator.REVERSE
                                repeatCount = ValueAnimator.INFINITE
                                start()
                            }
                        })
                    }

                    // Progress Bar
                    addView(ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                        tag = "progressBar"
                        max = 100
                        setProgress(progress)
                        progressTintList = ColorStateList.valueOf(accentColor)
                        progressBackgroundTintList = ColorStateList.valueOf(Color.rgb(30, 29, 48))
                        layoutParams = LinearLayout.LayoutParams(dp(200), dp(6)).apply {
                            bottomMargin = dp(8)
                        }
                    })

                    // Progress Percentage Text
                    addView(TextView(context).apply {
                        tag = "progressPercentage"
                        this.text = progressText
                        gravity = Gravity.CENTER
                        textSize = 20f
                        setTextColor(Color.WHITE)
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    })

                    // Compilation Phase Message (Tech-Cyan)
                    addView(TextView(context).apply {
                        tag = "compilationPhase"
                        this.text = phaseMessage
                        gravity = Gravity.CENTER
                        textSize = 12f
                        setTextColor(cyberCyan)
                        typeface = android.graphics.Typeface.MONOSPACE
                        layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                            topMargin = dp(8)
                        }
                    })

                    // Disclaimer text
                    addView(TextView(context).apply {
                        tag = "disclaimerText"
                        this.text = messageText
                        gravity = Gravity.CENTER
                        textSize = 11f
                        setTextColor(Color.rgb(150, 150, 168))
                        setLineSpacing(dp(4).toFloat(), 1f)
                        layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                            topMargin = dp(8)
                        }
                    })
                })
            } else {
                val progressBar = existing.findViewWithTag<ProgressBar>("progressBar")
                val progressTextView = existing.findViewWithTag<TextView>("progressPercentage")
                val phaseTextView = existing.findViewWithTag<TextView>("compilationPhase")
                val disclaimerTextView = existing.findViewWithTag<TextView>("disclaimerText")

                progressBar?.progress = progress
                progressTextView?.text = progressText
                phaseTextView?.text = phaseMessage
                disclaimerTextView?.text = messageText
            }
        }
    }

    private fun hideOverlay() {
        mainHandler.post { findViewWithTag<LinearLayout>("duixOverlay")?.let { removeView(it) } }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val BASE_MODEL_NAME = "gj_dh_res"

        @Volatile
        var downloadAuthToken: String? = null

        @Volatile
        private var prefetchStarted = false

        // Per-model locks so a background prefetch and an interview-time load can
        // never download/unzip the same files at once (which would corrupt them).
        private val modelLocks = java.util.concurrent.ConcurrentHashMap<String, Any>()
        private fun lockFor(name: String): Any = modelLocks.getOrPut(name) { Any() }

        fun isModelCached(context: Context, name: String): Boolean {
            val modelName = normalizeModelNameStatic(name)
            val root = modelRootFor(context)
            return baseConfigLooksReadyStatic(File(root, BASE_MODEL_NAME)) &&
                avatarModelLooksReadyStatic(File(root, modelName))
        }

        /**
         * Downloads the shared base engine (~133MB) plus the given avatar(s) in a
         * low-priority background thread so the heavy first-time setup is finished
         * before the user ever reaches the interview room. Safe to call repeatedly;
         * it only runs once and skips anything already cached.
         */
        fun preloadModelFiles(context: Context, names: List<String>) {
            if (prefetchStarted) return
            prefetchStarted = true
            val appContext = context.applicationContext
            val toFetch = names.ifEmpty { listOf("Sofia") }
            Thread({
                val prefetchClient = OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(600, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build()
                for (name in toFetch) {
                    try {
                        if (!isModelCached(appContext, name)) {
                            if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", "Prefetching avatar model '$name' in background")
                            ensureModelFilesAvailable(appContext, name, prefetchClient, null)
                            if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", "Prefetch complete for '$name'")
                        }
                    } catch (e: Exception) {
                        Log.w("PrezzenceDuix", "Background prefetch failed for '$name': ${e.message}")
                    }
                }
            }, "duix-prefetch").apply {
                isDaemon = true
                priority = Thread.MIN_PRIORITY
            }.start()
        }
        
        fun clearModelCache(context: Context) {
            val root = modelRootFor(context)
            if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", "Clearing model cache at: ${root.absolutePath}")
            root.deleteRecursively()
            root.mkdirs()
            if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", "Model cache cleared successfully")
        }

        private fun ensureModelFilesAvailable(
            context: Context,
            modelName: String,
            client: OkHttpClient,
            onProgress: ((Int) -> Unit)? = null
        ): Pair<File, File> {
            val normalized = normalizeModelNameStatic(modelName)
            val root = modelRootFor(context)
            val baseDir = File(root, BASE_MODEL_NAME)
            val modelDir = File(root, normalized)
            
            // If base model needs download, we'll allocate 0-60% of progress to it
            // If avatar needs download, we'll allocate 60-100% of progress to it
            val needsBase = !baseConfigLooksReadyStatic(baseDir)
            val needsAvatar = !avatarModelLooksReadyStatic(modelDir)
            
            if (needsBase) {
                // Re-check inside the lock: another thread (e.g. the background
                // prefetch) may have just finished downloading the base engine.
                synchronized(lockFor(BASE_MODEL_NAME)) {
                    if (!baseConfigLooksReadyStatic(baseDir)) {
                        downloadAndUnzipStatic(client, root, BASE_MODEL_NAME, baseDir) { p ->
                            if (needsAvatar) onProgress?.invoke((p * 0.6).toInt())
                            else onProgress?.invoke(p)
                        }
                    }
                }
            }
            if (needsAvatar) {
                synchronized(lockFor(normalized)) {
                    if (!avatarModelLooksReadyStatic(modelDir)) {
                        downloadAndUnzipStatic(client, root, normalized, modelDir) { p ->
                            if (needsBase) onProgress?.invoke(60 + (p * 0.4).toInt())
                            else onProgress?.invoke(p)
                        }
                    }
                }
            }
            
            File(root, "tmp/$BASE_MODEL_NAME").mkdirs()
            File(root, "tmp/$normalized").mkdirs()
            return baseDir to modelDir
        }

        private fun modelRootFor(context: Context): File =
            File(context.getExternalFilesDir("duix"), "model").apply { mkdirs() }

        private fun apiBaseStatic(): String =
            BuildConfig.PREZZENCE_API_URL.trimEnd('/') + "/api/duix/models/download/"

        private fun baseConfigLooksReadyStatic(dir: File): Boolean =
            dir.exists() &&
                dir.isDirectory &&
                listOf("alpha_model.b", "alpha_model.p", "cacert.p", "weight_168u.b", "wenet.o")
                    .all { File(dir, it).isFile }

        private fun avatarModelLooksReadyStatic(dir: File): Boolean =
            dir.exists() &&
                dir.isDirectory &&
                listOf("bbox.j", "config.j", "dh_model.b", "dh_model.p")
                    .all { File(dir, it).isFile } &&
                (File(dir, "weight_168u.bin").isFile || File(dir, "weight_168u.b").isFile) &&
                File(dir, "raw_jpgs").isDirectory

        private fun downloadAndUnzipStatic(
            client: OkHttpClient, 
            root: File, 
            name: String, 
            destination: File,
            onProgress: ((Int) -> Unit)? = null
        ) {
            destination.parentFile?.mkdirs()
            val zip = File(root, "$name.zip")
            
            // Build list of URLs to try - START WITH DIRECT URLs FOR IMMEDIATE FIX
            val urlsToTry = mutableListOf<String>()
            
            // 1. Direct GitHub for base config (MOST RELIABLE)
            if (name == BASE_MODEL_NAME) {
                urlsToTry.add("https://github.com/duixcom/Duix-Mobile/releases/download/v1.0.0/gj_dh_res.zip")
            }
            
            // 2. Direct Supabase for models
            if (name != BASE_MODEL_NAME) {
                val baseUrl = BuildConfig.DEBUG_DUIX_MODEL_BASE_URL.takeIf { it.isNotEmpty() && !it.contains("your-project") }
                    ?: "https://djwtmlvorvuhgaksqirc.supabase.co/storage/v1/object/public/duix-models"
                urlsToTry.add("$baseUrl/$name.zip")
            }
            
            // 3. Backend API as fallback (with redirect following)
            urlsToTry.add(apiBaseStatic() + "$name.zip")
            
            var lastError: Exception? = null
            var downloadSuccessful = false
            
            if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", "Will try ${urlsToTry.size} URL(s) for model $name")
            urlsToTry.forEachIndexed { idx, url -> if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", "  URL ${idx + 1}: $url") }
            
            // Try each URL with retries
            for (urlIndex in urlsToTry.indices) {
                if (downloadSuccessful) break
                
                val url = urlsToTry[urlIndex]
                val maxRetries = 2
                
                for (attempt in 1..maxRetries) {
                    try {
                        if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", ">>> Downloading model $name (URL ${urlIndex + 1}/${urlsToTry.size}, attempt $attempt/$maxRetries)")
                        if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", ">>> From: $url")
                        
                        val requestBuilder = Request.Builder()
                            .url(url)
                            .header("User-Agent", "Prezzence-Android/${BuildConfig.PREZZENCE_VERSION_NAME}")
                        if (url.contains("/api/duix/models/download/")) {
                            downloadAuthToken?.takeIf { it.isNotBlank() }?.let { token ->
                                requestBuilder.header("Authorization", "Bearer $token")
                            }
                        }
                        val request = requestBuilder.build()
                        
                        // Create client that follows redirects
                        val downloadClient = client.newBuilder()
                            .followRedirects(true)
                            .followSslRedirects(true)
                            .build()
                        
                        downloadClient.newCall(request).execute().use { response ->
                            if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", ">>> HTTP Response: ${response.code} ${response.message}")
                            
                            if (!response.isSuccessful) {
                                Log.e("PrezzenceDuix", ">>> FAILED: HTTP ${response.code} from URL $url")
                                lastError = IllegalStateException("HTTP ${response.code}")
                                if (response.code == 404) {
                                    Log.w("PrezzenceDuix", ">>> 404 Not Found - skipping to next URL")
                                    return@use  // Don't retry 404s
                                }
                                throw lastError!!
                            }
                            
                            val contentLength = response.header("content-length")?.toLongOrNull() ?: -1L
                            val contentType = response.header("content-type") ?: "unknown"
                            if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", ">>> Downloading: $contentLength bytes, type: $contentType")
                            
                            val body = response.body ?: throw IllegalStateException("Empty response body")
                            
                            // Use buffered stream for reliability
                            zip.delete()
                            var bytesWritten = 0L
                            
                            // Initialize progress tracking
                            var lastReportedProgress = 0
                            
                            FileOutputStream(zip).use { output ->
                                body.byteStream().buffered(8192).use { input ->
                                    val buffer = ByteArray(8192)
                                    var count: Int
                                    while (input.read(buffer).also { count = it } != -1) {
                                        output.write(buffer, 0, count)
                                        bytesWritten += count
                                        
                                        if (contentLength > 0 && onProgress != null) {
                                            val progress = ((bytesWritten.toDouble() / contentLength) * 100).toInt().coerceIn(0, 100)
                                            // Only report on 1% increments to avoid spamming UI thread too much
                                            if (progress > lastReportedProgress) {
                                                lastReportedProgress = progress
                                                onProgress(progress)
                                            }
                                        }
                                    }
                                }
                            }
                            if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", ">>> Wrote $bytesWritten bytes to disk")
                        }
                        
                        // Validate downloaded file
                        val downloadedSize = zip.length()
                        if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", ">>> Final file size: $downloadedSize bytes")
                        
                        if (downloadedSize < 1000) {
                            Log.e("PrezzenceDuix", ">>> FAILED: File too small: $downloadedSize bytes")
                            zip.delete()
                            lastError = IllegalStateException("File too small: $downloadedSize bytes")
                            continue
                        }
                        
                        // Verify it's actually a valid ZIP
                        var isValidZip = false
                        try {
                            RandomAccessFile(zip, "r").use { raf ->
                                val header = ByteArray(4)
                                raf.readFully(header)
                                val signature = (header[0].toInt() and 0xFF) or
                                    ((header[1].toInt() and 0xFF) shl 8) or
                                    ((header[2].toInt() and 0xFF) shl 16) or
                                    ((header[3].toInt() and 0xFF) shl 24)
                                isValidZip = signature == 0x04034b50  // ZIP local file header signature
                                if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", ">>> ZIP signature: 0x${signature.toString(16)} (valid: $isValidZip)")
                                if (!isValidZip) {
                                    Log.e("PrezzenceDuix", ">>> FAILED: Invalid ZIP signature")
                                    zip.delete()
                                    lastError = IllegalStateException("Invalid ZIP file")
                                    throw lastError!!
                                }
                            }
                        } catch (e: Exception) {
                            if (!isValidZip) {
                                Log.e("PrezzenceDuix", ">>> ZIP validation failed: ${e.message}")
                                throw e
                            }
                            Log.w("PrezzenceDuix", ">>> Could not fully verify ZIP (but continuing): ${e.message}")
                        }
                        
                        if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", ">>> SUCCESS: Model $name downloaded: $downloadedSize bytes")
                        lastError = null
                        downloadSuccessful = true
                        break  // Success - exit retry loop
                        
                    } catch (e: Exception) {
                        lastError = e
                        Log.e("PrezzenceDuix", ">>> Download attempt $attempt/$maxRetries FAILED: ${e.javaClass.simpleName}: ${e.message}")
                        if (attempt < maxRetries) {
                            val backoffMs = 1000L * attempt * 2  // Exponential backoff: 2s, 4s
                            if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", ">>> Waiting ${backoffMs}ms before retry...")
                            Thread.sleep(backoffMs)
                        }
                    }
                }
            }
            
            // If no URL succeeded, throw error
            if (lastError != null || !zip.exists() || zip.length() < 1000) {
                zip.delete()
                Log.e("PrezzenceDuix", ">>> FINAL FAILURE: All ${urlsToTry.size} URLs failed for $name")
                throw lastError ?: IllegalStateException("Model download failed: no valid file obtained")
            }
            
            // Clear destination before unzip
            if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", ">>> Preparing to unzip to ${destination.absolutePath}")
            destination.deleteRecursively()
            destination.mkdirs()
            
            // Unzip with error handling
            try {
                if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", ">>> Calling ZipUtil.unzip...")
                val ok = ZipUtil.unzip(zip.absolutePath, root.absolutePath, null)
                if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", ">>> ZipUtil.unzip returned: $ok")
                if (!ok) {
                    throw IllegalStateException("ZipUtil.unzip returned false")
                }
            } catch (e: Exception) {
                zip.delete()
                destination.deleteRecursively()
                Log.e("PrezzenceDuix", ">>> Unzip FAILED: ${e.message}", e)
                throw IllegalStateException("Model unzip failed: ${e.message}", e)
            }
            
            repairSingleNestedDirectoryStatic(destination, name)
            File(root, "tmp/$name").mkdirs()
            zip.delete()
            
            // Validate unzipped content
            val isBase = name == BASE_MODEL_NAME
            val isValid = if (isBase) {
                baseConfigLooksReadyStatic(destination)
            } else {
                avatarModelLooksReadyStatic(destination)
            }
            
            if (!isValid) {
                // List what we actually have for debugging
                val filesInDest = destination.listFiles()?.map { it.name } ?: emptyList()
                val filesStr = filesInDest.joinToString(", ")
                val subdirFiles = filesInDest.mapNotNull { fname ->
                    val f = File(destination, fname)
                    if (f.isDirectory) {
                        "$fname/: " + (f.listFiles()?.map { it.name }?.take(5)?.joinToString(", ") ?: "empty")
                    } else null
                }
                val subdirStr = subdirFiles.joinToString("; ")
                
                Log.w("PrezzenceDuix", ">>> Model validation WARNING for $name")
                Log.w("PrezzenceDuix", ">>> Root files: [$filesStr]")
                Log.w("PrezzenceDuix", ">>> Subdirs: [$subdirStr]")
                Log.w("PrezzenceDuix", ">>> Attempting to use incomplete model anyway")
            } else {
                if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", ">>> Model $name validated successfully!")
            }
        }

        private fun repairSingleNestedDirectoryStatic(destination: File, expectedName: String) {
            val files = destination.listFiles() ?: return
            if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", "repairSingleNestedDirectory: checking $destination with ${files.size} items")
            files.forEach { f -> if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", "  - ${f.name} (isDir: ${f.isDirectory})") }
            
            // If we have exactly one nested directory, move its contents up
            if (files.size == 1 && files[0].isDirectory) {
                val nested = files[0]
                if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", "Found single nested dir: ${nested.name}, flattening...")
                nested.listFiles()?.forEach { child ->
                    val newPath = File(destination, child.name)
                    val success = child.renameTo(newPath)
                    if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", "  Moved ${child.name} to ${newPath.name}: $success")
                }
                nested.deleteRecursively()
                if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", "Nested directory flattening complete")
            }
            
            // Check if files are in a deeply nested structure
            var current = destination
            var depth = 0
            while (current.listFiles()?.size == 1 && current.listFiles()!![0].isDirectory && depth < 5) {
                current = current.listFiles()!![0]
                depth++
                if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", "Descending into nested dir at depth $depth: ${current.name}")
            }
            
            if (depth > 0 && current != destination) {
                if (BuildConfig.DEBUG) Log.i("PrezzenceDuix", "Moving contents from depth $depth back to root")
                current.listFiles()?.forEach { child ->
                    val newPath = File(destination, child.name)
                    child.renameTo(newPath)
                }
                // Clean up old directories
                var parent = current.parentFile
                while (parent != destination && parent != null) {
                    parent.deleteRecursively()
                    parent = parent.parentFile
                }
            }
        }

        private fun normalizeModelNameStatic(name: String): String = when (name.lowercase()) {
            "maya", "lily" -> "Lily"
            "jonas", "oliver" -> "Oliver"
            else -> "Sofia"
        }
    }
}


