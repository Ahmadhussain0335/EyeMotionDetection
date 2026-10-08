package com.app.eyesmotiondetection.detection.model

data class FaceData(
    val leftEyeOpenProbability: Float,
    val rightEyeOpenProbability: Float,
    val headEulerAngleX: Float,
    val headEulerAngleY: Float,
    val headEulerAngleZ: Float,
    val leftEyeEAR: Float,
    val rightEyeEAR: Float,
    val faceDetected: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
