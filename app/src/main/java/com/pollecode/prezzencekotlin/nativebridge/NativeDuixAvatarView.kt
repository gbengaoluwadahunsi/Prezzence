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
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val textureView = DUIXTextureView(context)
    private var renderer: DUIXRenderer? = null
    private var duix: DUIX? = null
    private var preparedModelName: String? = null
    private var preparingModelName: String? = null
    private var duixInitReady = false
    private var currentModelName: String = "Sofia"
    private var currentSpeechId = 0
    private var currentSpeechSource: String? = null
    private var playToken = AtomicInteger(0)
    private val duixSampleRate = 16_000

    private val modelRoot = File(context.getExternalFilesDir("duix"), "model")
    private val cacheRoot = File(context.cacheDir, "duix-audio")
    private val apiBase = BuildConfig.PREZZENCE_API_URL.trimEnd('/') + "/api/duix/models/download/"

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
        if (!isModelCached(context, modelName)) {
            showOverlay("Preparing")
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
            } catch (error: Throwable) {
                preparingModelName = null
                hideOverlay()
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
                    val dirs = withContext(Dispatchers.IO) { ensureModelAvailable(modelName) }
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
                Log.i("PrezzenceDuix", "playAudio model=$modelName source=$source path=$audioPath bytes=${File(audioPath).length()}")
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
        return ensureModelFilesAvailable(context, modelName, client)
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
            File(dir, "raw_jpgs").isDirectory &&
            File(dir, "gh").isDirectory

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
        val id = ++currentSpeechId
        val extension = uri.substringBefore("?").substringAfterLast(".", "audio").take(8)
        val input = File(cacheRoot, "speech-$id-input.$extension")
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
        val wav = ensureDuixWav(input, id, source)
        Log.i("PrezzenceDuix", "Prepared WAV source=$source inputExt=$extension bytes=${wav.length()}")
        return wav.absolutePath
    }

    private fun ensureDuixWav(input: File, id: Int, source: String): File {
        if (input.extension.equals("wav", ignoreCase = true) && isDuixCompatibleWav(input)) return input
        val output = File(cacheRoot, "speech-$id-$source.wav")
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
        val minBytes = duixSampleRate * 2
        if (pcmData.size >= minBytes) return pcmData
        return pcmData + ByteArray(minBytes - pcmData.size)
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

    private fun showOverlay(text: String) {
        mainHandler.post {
            val existing = findViewWithTag<LinearLayout>("duixOverlay")
            if (existing == null) {
                val accent = Color.rgb(108, 99, 255)
                addView(LinearLayout(context).apply {
                    tag = "duixOverlay"
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(24), dp(24), dp(24), dp(24))
                    setBackgroundColor(Color.rgb(8, 8, 14))
                    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                    addView(TextView(context).apply {
                        this.text = displayInitialForModel()
                        gravity = Gravity.CENTER
                        textSize = 34f
                        setTextColor(Color.WHITE)
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        background = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.OVAL
                            setColor(Color.rgb(30, 29, 48))
                            setStroke(dp(2), accent)
                        }
                        layoutParams = LinearLayout.LayoutParams(dp(92), dp(92)).apply {
                            bottomMargin = dp(16)
                        }
                    })
                    addView(TextView(context).apply {
                        this.text = text.ifBlank { "Preparing avatar" }
                        gravity = Gravity.CENTER
                        textSize = 15f
                        setTextColor(Color.WHITE)
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    })
                    addView(TextView(context).apply {
                        this.text = "Loading interviewer model"
                        gravity = Gravity.CENTER
                        textSize = 12f
                        setTextColor(Color.rgb(150, 150, 168))
                        layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                            topMargin = dp(6)
                        }
                    })
                })
            } else {
                (existing.getChildAt(0) as? TextView)?.text = displayInitialForModel()
                (existing.getChildAt(1) as? TextView)?.text = text.ifBlank { "Preparing avatar" }
            }
        }
    }

    private fun hideOverlay() {
        mainHandler.post { findViewWithTag<LinearLayout>("duixOverlay")?.let { removeView(it) } }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val BASE_MODEL_NAME = "gj_dh_res"

        fun isModelCached(context: Context, name: String): Boolean {
            val modelName = normalizeModelNameStatic(name)
            val root = modelRootFor(context)
            return baseConfigLooksReadyStatic(File(root, BASE_MODEL_NAME)) &&
                avatarModelLooksReadyStatic(File(root, modelName))
        }

        fun preloadModelFiles(context: Context, names: List<String>) {
            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .build()
            names.map { normalizeModelNameStatic(it) }
                .distinct()
                .forEach { ensureModelFilesAvailable(context, it, client) }
        }

        private fun ensureModelFilesAvailable(
            context: Context,
            modelName: String,
            client: OkHttpClient,
        ): Pair<File, File> {
            val normalized = normalizeModelNameStatic(modelName)
            val root = modelRootFor(context)
            val baseDir = File(root, BASE_MODEL_NAME)
            val modelDir = File(root, normalized)
            if (!baseConfigLooksReadyStatic(baseDir)) {
                downloadAndUnzipStatic(client, root, BASE_MODEL_NAME, baseDir)
            }
            if (!avatarModelLooksReadyStatic(modelDir)) {
                downloadAndUnzipStatic(client, root, normalized, modelDir)
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
                File(dir, "raw_jpgs").isDirectory &&
                File(dir, "gh").isDirectory

        private fun downloadAndUnzipStatic(client: OkHttpClient, root: File, name: String, destination: File) {
            destination.parentFile?.mkdirs()
            val zip = File(root, "$name.zip")
            
            // Download with validation
            val request = Request.Builder().url(apiBaseStatic() + "$name.zip").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IllegalStateException("Model download failed: ${response.code}")
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(zip).use { output -> input.copyTo(output) }
                } ?: throw IllegalStateException("Model download returned empty body")
            }
            
            // Validate downloaded file
            if (!zip.exists() || zip.length() < 1000) {
                zip.delete()
                throw IllegalStateException("Model download incomplete: ${zip.length()} bytes")
            }
            
            // Clear destination before unzip
            destination.deleteRecursively()
            destination.mkdirs()
            
            // Unzip with error handling
            try {
                val ok = ZipUtil.unzip(zip.absolutePath, root.absolutePath, null)
                if (!ok) {
                    throw IllegalStateException("ZipUtil.unzip returned false")
                }
            } catch (e: Exception) {
                zip.delete()
                destination.deleteRecursively()
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
                destination.deleteRecursively()
                throw IllegalStateException("Model unzip validation failed - missing expected files in $name")
            }
        }

        private fun repairSingleNestedDirectoryStatic(destination: File, expectedName: String) {
            val files = destination.listFiles() ?: return
            if (files.size == 1 && files[0].isDirectory && files[0].name.equals(expectedName, ignoreCase = true)) {
                files[0].listFiles()?.forEach { child -> child.renameTo(File(destination, child.name)) }
                files[0].deleteRecursively()
            }
        }

        private fun normalizeModelNameStatic(name: String): String = when (name.lowercase()) {
            "maya", "lily" -> "Lily"
            "jonas", "oliver" -> "Oliver"
            else -> "Sofia"
        }
    }
}


