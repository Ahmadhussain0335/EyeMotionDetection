package com.app.eyesmotiondetection.detection

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.app.eyesmotiondetection.detection.model.FaceData
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Runs ML Kit face detection on CameraX frames and hands the resulting [FaceData] to [onFaceData].
 * The app-wide instance feeding [EyeGestureDetector] is provided by DetectionModule.
 */
class FaceAnalyzer(
    private val onFaceData: (FaceData) -> Unit
) : ImageAnalysis.Analyzer {

    // Single lane: consumers such as EyeGestureDetector keep unsynchronized frame-to-frame state
    private val scope = CoroutineScope(Dispatchers.Default.limitedParallelism(1) + SupervisorJob())

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()
    )

    private val noFace = FaceData(
        leftEyeOpenProbability = 0.5f, rightEyeOpenProbability = 0.5f,
        headEulerAngleX = 0f, headEulerAngleY = 0f, headEulerAngleZ = 0f,
        leftEyeEAR = 0.5f, rightEyeEAR = 0.5f, faceDetected = false
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image ?: run { image.close(); return }
        val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                val data = if (faces.isNotEmpty()) EyeMetricsCalculator.calculate(faces[0]) else noFace
                scope.launch { onFaceData(data) }
            }
            .addOnFailureListener { Log.w("FaceAnalyzer", "Detection error: ${it.message}") }
            .addOnCompleteListener { image.close() }
    }

    fun close() {
        scope.cancel()
        detector.close()
    }
}
