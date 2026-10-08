package com.app.eyesmotiondetection.detection

import com.app.eyesmotiondetection.domain.model.AppAction
import com.app.eyesmotiondetection.domain.model.EyeGesture

data class ActionEvent(
    val gesture: EyeGesture,
    val action: AppAction,
    val timestamp: Long
)
