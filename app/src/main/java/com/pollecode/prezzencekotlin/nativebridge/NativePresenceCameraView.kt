package com.pollecode.prezzencekotlin.nativebridge

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class NativePresenceCameraView(private val activity: ComponentActivity) : FrameLayout(activity) {
    data class Metrics(
        val faceVisible: Boolean,
        val faceVisibility: Int,
        val eyeContact: Int,
        val headStability: Int,
        val posture: Int,
        val expressionEnergy: Int,
    )

    interface Listener {
        fun onMetrics(metrics: Metrics) {}
        fun onStatus(message: String) {}
        fun onError(message: String) {}
    }

    var listener: Listener? = null
    private var analyzerExecutor: ExecutorService? = null
    private var faceLandmarker: FaceLandmarker? = null
    private var poseLandmarker: PoseLandmarker? = null
    private var lastAnalyzeAt = 0L
    private var lastFrameSeenAt = 0L
    private var lastCenterX: Float? = null
    private var lastCenterY: Float? = null
    private var lastFaceSize: Float? = null
    private var lastGoodMetrics: Metrics? = null
    private var isStopped = false
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val previewView = PreviewView(activity).apply {
        scaleType = PreviewView.ScaleType.FILL_CENTER
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
    }

    private val status = TextView(activity).apply {
        text = "Starting camera. Position your face in frame"
        textSize = 15f
        setTextColor(Color.WHITE)
        setPadding(dp(18), dp(16), dp(18), dp(8))
    }

    private val metricsRow = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(dp(8), dp(8), dp(8), dp(12))
    }

    init {
        setBackgroundColor(Color.BLACK)
        addView(previewView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        setMetrics(Metrics(false, 0, 0, 0, 0, 0))
        start()
    }

    fun start() {
        lastAnalyzeAt = 0L
        lastFrameSeenAt = 0L
        lastCenterX = null
        lastCenterY = null
        lastFaceSize = null
        lastGoodMetrics = null
        isStopped = false
        status.text = "Starting camera. Position your face in frame"
        setMetrics(Metrics(false, 0, 0, 0, 0, 0))
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            status.text = "Camera access needed"
            listener?.onError("Camera permission is not granted")
            return
        }
        initializeLandmarkers()
        val future = ProcessCameraProvider.getInstance(activity)
        future.addListener({
            try {
                val provider = future.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                analyzerExecutor?.shutdownNow()
                analyzerExecutor = Executors.newSingleThreadExecutor()
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()
                    .also { imageAnalysis ->
                        imageAnalysis.setAnalyzer(analyzerExecutor!!) { image ->
                            analyzeFrame(image)
                        }
                    }
                provider.unbindAll()
                provider.bindToLifecycle(activity, CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build(), preview, analysis)
                status.text = "Keep your face in frame"
                listener?.onStatus("Camera ready")
                postDelayed({
                    if (lastFrameSeenAt == 0L && isAttachedToWindow) {
                        status.text = "Camera is starting. Keep your face in frame"
                        listener?.onStatus("Camera starting")
                    }
                }, 2500)
                postDelayed({
                    if (lastFrameSeenAt == 0L && isAttachedToWindow) {
                        status.text = "Camera preview is not responding. Reopen the coach or check camera permission."
                        listener?.onError("Camera preview is not responding")
                    }
                }, 6500)
            } catch (error: Throwable) {
                status.text = "Camera preview is not responding"
                listener?.onError(error.message ?: "Camera preview failed")
            }
        }, ContextCompat.getMainExecutor(activity))
    }

    fun stop() {
        isStopped = true
        runCatching { ProcessCameraProvider.getInstance(activity).get().unbindAll() }
        analyzerExecutor?.shutdownNow()
        analyzerExecutor = null
        runCatching { faceLandmarker?.close() }
        runCatching { poseLandmarker?.close() }
        faceLandmarker = null
        poseLandmarker = null
    }

    private fun initializeLandmarkers() {
        if (faceLandmarker != null) return
        status.text = "Preparing camera coach models..."
        listener?.onStatus("Preparing camera coach models...")
        Executors.newSingleThreadExecutor().execute {
            try {
                activity.runOnUiThread {
                    status.text = "Downloading camera coach models..."
                    listener?.onStatus("Downloading camera coach models...")
                }
                val faceModel = ensureModel(
                    fileName = FACE_MODEL_NAME,
                    url = FACE_MODEL_URL,
                    minBytes = MIN_FACE_MODEL_BYTES,
                )
                val poseModel = ensureModel(
                    fileName = POSE_MODEL_NAME,
                    url = POSE_MODEL_URL,
                    minBytes = MIN_POSE_MODEL_BYTES,
                )
                val faceOptions = FaceLandmarker.FaceLandmarkerOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetBuffer(faceModel.toDirectByteBuffer()).build())
                    .setRunningMode(RunningMode.IMAGE)
                    .setNumFaces(1)
                    .setMinFaceDetectionConfidence(0.35f)
                    .setMinFacePresenceConfidence(0.35f)
                    .setMinTrackingConfidence(0.35f)
                    .build()
                val fLandmarker = FaceLandmarker.createFromOptions(activity, faceOptions)
                val poseOptions = PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetBuffer(poseModel.toDirectByteBuffer()).build())
                    .setRunningMode(RunningMode.IMAGE)
                    .setNumPoses(1)
                    .setMinPoseDetectionConfidence(0.30f)
                    .setMinPosePresenceConfidence(0.30f)
                    .setMinTrackingConfidence(0.35f)
                    .build()
                val pLandmarker = PoseLandmarker.createFromOptions(activity, poseOptions)
                activity.runOnUiThread {
                    if (isStopped) {
                        runCatching { fLandmarker.close() }
                        runCatching { pLandmarker.close() }
                    } else {
                        faceLandmarker = fLandmarker
                        poseLandmarker = pLandmarker
                        status.text = "Camera presence captured"
                        listener?.onStatus("Camera ready")
                    }
                }
            } catch (error: Throwable) {
                activity.runOnUiThread {
                    status.text = "Camera coach error: ${error.message ?: "unknown error"}"
                    listener?.onError("MediaPipe unavailable: ${error.message ?: "unknown error"}")
                }
            }
        }
    }

    private fun ensureModel(fileName: String, url: String, minBytes: Long): File {
        val dir = File(activity.filesDir, "mediapipe-models").apply { mkdirs() }
        val model = File(dir, fileName)
        if (model.exists() && model.length() > minBytes) return model

        val temp = File(dir, "$fileName.download")
        if (temp.exists()) temp.delete()
        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("$fileName download failed: HTTP ${response.code}")
            val body = response.body ?: throw IllegalStateException("$fileName download returned no data")
            temp.outputStream().use { output -> body.byteStream().copyTo(output) }
        }
        if (temp.length() <= minBytes) {
            temp.delete()
            throw IllegalStateException("$fileName download was incomplete")
        }
        if (model.exists()) model.delete()
        if (!temp.renameTo(model)) throw IllegalStateException("$fileName could not be cached")
        return model
    }

    private fun File.toDirectByteBuffer(): ByteBuffer {
        val bytes = readBytes()
        return ByteBuffer.allocateDirect(bytes.size)
            .order(ByteOrder.nativeOrder())
            .apply {
                put(bytes)
                rewind()
            }
    }

    private fun analyzeFrame(image: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastAnalyzeAt < 450) {
            image.close()
            return
        }
        val fLandmarker = faceLandmarker
        if (fLandmarker == null) {
            image.close()
            return
        }
        lastAnalyzeAt = now
        lastFrameSeenAt = now
        try {
            val bitmap = image.toBitmapForLandmarks()
            val mpImage = BitmapImageBuilder(bitmap).build()
            val faceResult = fLandmarker.detect(mpImage)
            val faceLandmarks = faceResult?.faceLandmarks()?.firstOrNull()
            if (faceLandmarks.isNullOrEmpty()) {
                activity.runOnUiThread {
                    status.text = "Position your face in frame"
                    setMetrics(lastGoodMetrics ?: Metrics(false, 0, 0, 0, 0, 0))
                    listener?.onStatus("No face detected yet")
                }
                bitmap.recycle()
                return
            }

            val poseLandmarks = runCatching { poseLandmarker?.detect(mpImage)?.landmarks()?.firstOrNull() }.getOrNull()
            val metrics = computeMetrics(faceLandmarks, poseLandmarks)
            lastGoodMetrics = metrics
            activity.runOnUiThread {
                status.text = "Camera presence captured"
                setMetrics(metrics)
            }
            bitmap.recycle()
        } catch (error: Throwable) {
            activity.runOnUiThread {
                status.text = "Camera preview is active"
                listener?.onError(error.message ?: "Camera analysis failed")
            }
        } finally {
            image.close()
        }
    }

    private fun computeMetrics(face: List<NormalizedLandmark>, pose: List<NormalizedLandmark>?): Metrics {
        var minX = 1f
        var maxX = 0f
        var minY = 1f
        var maxY = 0f
        face.forEach { landmark ->
            minX = min(minX, landmark.x())
            maxX = max(maxX, landmark.x())
            minY = min(minY, landmark.y())
            maxY = max(maxY, landmark.y())
        }
        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f
        val faceSize = max(maxX - minX, maxY - minY).coerceAtLeast(0.01f)
        
        // Face visibility: how well face is positioned and sized
        val facePositioned = (100 - (abs(centerX - 0.5f) * 170 + abs(centerY - 0.43f) * 135)).roundToInt().coerceIn(0, 100)
        val faceSized = (100 - abs(faceSize - 0.46f) * 170).roundToInt().coerceIn(0, 100)
        val faceVisibility = ((facePositioned + faceSized) / 2).coerceIn(0, 100)
        val faceVisible = faceVisibility > 50

        // Eye contact: based on yaw/head turn
        val leftEyeOuter = face.safe(33)
        val leftEyeInner = face.safe(133)
        val rightEyeInner = face.safe(362)
        val rightEyeOuter = face.safe(263)
        val nose = face.safe(1) ?: face.safe(4)
        val eyeMidX = listOfNotNull(leftEyeOuter, leftEyeInner, rightEyeInner, rightEyeOuter).map { it.x() }.averageOrNull()?.toFloat() ?: centerX
        val yawScore = if (nose != null) (100 - abs(nose.x() - eyeMidX) * 520).roundToInt().coerceIn(0, 100) else facePositioned
        val eyeContact = yawScore

        // Head stability: based on tilt/roll
        val rollScore = if (leftEyeOuter != null && rightEyeOuter != null) {
            (100 - abs(leftEyeOuter.y() - rightEyeOuter.y()) * 760).roundToInt().coerceIn(0, 100)
        } else facePositioned
        val headStability = rollScore

        // Posture: from pose landmarks
        val postureScore = pose?.let { landmarks ->
            val leftShoulder = landmarks.safe(11)
            val rightShoulder = landmarks.safe(12)
            val poseNose = landmarks.safe(0)
            if (leftShoulder != null && rightShoulder != null && poseNose != null) {
                val shoulderTilt = abs(leftShoulder.y() - rightShoulder.y())
                val shoulderCenterX = (leftShoulder.x() + rightShoulder.x()) / 2f
                val upright = 100 - shoulderTilt * 820 - abs(poseNose.x() - shoulderCenterX) * 160
                upright.roundToInt().coerceIn(0, 100)
            } else null
        } ?: ((facePositioned + faceSized) / 2).coerceIn(0, 100)

        // Expression Energy: from movement/stillness
        val prevX = lastCenterX
        val prevY = lastCenterY
        val prevSize = lastFaceSize
        val stillness = if (prevX == null || prevY == null || prevSize == null) {
            72
        } else {
            (100 - ((abs(centerX - prevX) + abs(centerY - prevY)) * 330 + abs(faceSize - prevSize) * 220)).roundToInt().coerceIn(0, 100)
        }
        lastCenterX = centerX
        lastCenterY = centerY
        lastFaceSize = faceSize
        val expressionEnergy = (100 - stillness).coerceIn(12, 88)

        return Metrics(
            faceVisible = faceVisible,
            faceVisibility = faceVisibility,
            eyeContact = eyeContact,
            headStability = headStability,
            posture = postureScore,
            expressionEnergy = expressionEnergy,
        )
    }

    private fun List<NormalizedLandmark>.safe(index: Int): NormalizedLandmark? = getOrNull(index)

    private fun List<Float>.averageOrNull(): Double? = if (isEmpty()) null else sum().toDouble() / size

    private fun ImageProxy.toBitmapForLandmarks(): Bitmap {
        val nv21 = yuv420ToNv21()
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 78, out)
        val decoded = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
        val matrix = Matrix().apply {
            val rotation = imageInfo.rotationDegrees
            if (rotation != 0) postRotate(rotation.toFloat())
            // Front-camera frames are mirrored for preview. Mirroring the analysis bitmap keeps
            // face and pose coordinates aligned with what the user sees on screen.
            postScale(-1f, 1f)
        }
        val oriented = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        val targetWidth = 360
        val scaledHeight = (oriented.height * (targetWidth.toFloat() / oriented.width)).roundToInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(oriented, targetWidth, scaledHeight, true)
        val argb = scaled.copy(Bitmap.Config.ARGB_8888, false)
        if (scaled !== oriented) scaled.recycle()
        if (oriented !== decoded) oriented.recycle()
        decoded.recycle()
        return argb
    }

    private fun ImageProxy.yuv420ToNv21(): ByteArray {
        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]
        val ySize = width * height
        val nv21 = ByteArray(ySize + width * height / 2)
        val yBuffer = yPlane.buffer
        var outputOffset = 0
        for (row in 0 until height) {
            yBuffer.position(row * yPlane.rowStride)
            yBuffer.get(nv21, outputOffset, width)
            outputOffset += width
        }
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer
        val chromaHeight = height / 2
        val chromaWidth = width / 2
        var chromaOffset = ySize
        for (row in 0 until chromaHeight) {
            for (col in 0 until chromaWidth) {
                val vuIndex = row * vPlane.rowStride + col * vPlane.pixelStride
                val uuIndex = row * uPlane.rowStride + col * uPlane.pixelStride
                nv21[chromaOffset++] = vBuffer.get(vuIndex)
                nv21[chromaOffset++] = uBuffer.get(uuIndex)
            }
        }
        return nv21
    }

    fun setMetrics(metrics: Metrics) {
        metricsRow.removeAllViews()
        metricsRow.addView(metric("FACE", metrics.faceVisibility))
        metricsRow.addView(metric("EYES", metrics.eyeContact))
        metricsRow.addView(metric("HEAD", metrics.headStability))
        metricsRow.addView(metric("POSTURE", metrics.posture))
        metricsRow.addView(metric("ENERGY", metrics.expressionEnergy))
        listener?.onMetrics(metrics)
    }

    private fun label(text: String) = TextView(activity).apply {
        this.text = text
        textSize = 13f
        letterSpacing = 0.18f
        setTextColor(Color.rgb(25, 220, 164))
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setPadding(dp(18), dp(18), dp(18), 0)
    }

    private fun metric(name: String, value: Int) = TextView(activity).apply {
        text = if (value > 0) "$name\n$value" else "$name\n--"
        gravity = Gravity.CENTER
        textSize = 10f
        setTextColor(Color.WHITE)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setPadding(dp(8), dp(8), dp(8), dp(8))
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.argb(180, 18, 18, 24))
            cornerRadius = dp(12).toFloat()
        }
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(dp(4), 0, dp(4), 0)
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private companion object {
        private const val FACE_MODEL_NAME = "face_landmarker.task"
        private const val POSE_MODEL_NAME = "pose_landmarker_lite.task"
        private const val FACE_MODEL_URL = "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/latest/face_landmarker.task"
        private const val POSE_MODEL_URL = "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task"
        private const val MIN_FACE_MODEL_BYTES = 1024L * 1024L
        private const val MIN_POSE_MODEL_BYTES = 1024L * 1024L
    }
}

