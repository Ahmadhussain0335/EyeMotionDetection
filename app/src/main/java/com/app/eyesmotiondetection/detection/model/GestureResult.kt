package com.app.eyesmotiondetection.detection.model

import com.app.eyesmotiondetection.domain.model.EyeGesture

data class GestureResult(
    val gesture: EyeGesture,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)
