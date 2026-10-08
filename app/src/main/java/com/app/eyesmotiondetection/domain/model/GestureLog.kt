package com.app.eyesmotiondetection.domain.model

data class GestureLog(
    val id: Long = 0,
    val gesture: EyeGesture,
    val action: AppAction,
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float
)
