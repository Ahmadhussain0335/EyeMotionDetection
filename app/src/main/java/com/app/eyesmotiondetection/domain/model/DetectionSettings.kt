package com.app.eyesmotiondetection.domain.model

data class DetectionSettings(
    val blinkThreshold: Float = 0.3f,
    val wideOpenThreshold: Float = 0.75f,
    val halfOpenThreshold: Float = 0.45f,
    val gazeLeftThreshold: Float = -15f,
    val gazeRightThreshold: Float = 15f,
    val gazeUpThreshold: Float = -10f,
    val gazeDownThreshold: Float = 10f,
    val fastBlinkCountThreshold: Int = 3,
    val fastBlinkWindowMs: Long = 1000L,
    val globalCooldownMs: Long = 500L,
    val debounceMs: Long = 150L,
    val isVibrationEnabled: Boolean = true,
    val isVoiceFeedbackEnabled: Boolean = false,
    val isOverlayEnabled: Boolean = true,
    val isLoggingEnabled: Boolean = true,
    val isFacePointerEnabled: Boolean = false,
    val pointerSpeed: Float = 1.5f,
    val pointerSensitivity: Float = 3.0f
)
