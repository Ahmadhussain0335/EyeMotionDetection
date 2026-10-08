package com.app.eyesmotiondetection.domain.model

data class GestureMapping(
    val id: Long = 0,
    val gesture: EyeGesture,
    val action: AppAction,
    val isEnabled: Boolean = true,
    val sensitivity: Float = 0.5f,
    val cooldownMs: Long = 1000L
)
