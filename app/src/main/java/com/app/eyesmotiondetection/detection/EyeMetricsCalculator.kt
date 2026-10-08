package com.app.eyesmotiondetection.detection

import com.app.eyesmotiondetection.detection.model.FaceData
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import kotlin.math.sqrt

object EyeMetricsCalculator {

    fun calculate(face: Face): FaceData = FaceData(
        leftEyeOpenProbability = face.leftEyeOpenProbability ?: 0.5f,
        rightEyeOpenProbability = face.rightEyeOpenProbability ?: 0.5f,
        headEulerAngleX = face.headEulerAngleX,
        headEulerAngleY = face.headEulerAngleY,
        headEulerAngleZ = face.headEulerAngleZ,
        leftEyeEAR = calculateEAR(face, FaceContour.LEFT_EYE),
        rightEyeEAR = calculateEAR(face, FaceContour.RIGHT_EYE),
        faceDetected = true
    )

    // Eye Aspect Ratio computed from ML Kit contour (16 points around eye)
    private fun calculateEAR(face: Face, contourType: Int): Float {
        val pts = face.getContour(contourType)?.points ?: return 0.5f
        if (pts.size < 16) return 0.5f
        // Pick left(0), right(8), top-left(3), top-right(5), bot-right(11), bot-left(13)
        val v1 = dist(pts[3].x, pts[3].y, pts[13].x, pts[13].y)
        val v2 = dist(pts[5].x, pts[5].y, pts[11].x, pts[11].y)
        val h = dist(pts[0].x, pts[0].y, pts[8].x, pts[8].y)
        return if (h > 0f) (v1 + v2) / (2f * h) else 0.5f
    }

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1; val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }
}
