package com.app.eyesmotiondetection.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

data class ClickPoint(val x: Float, val y: Float)

object PointerClickBus {
    private val _clicks = MutableSharedFlow<ClickPoint>(extraBufferCapacity = 8)
    val clicks: SharedFlow<ClickPoint> = _clicks

    fun tryEmit(point: ClickPoint): Boolean = _clicks.tryEmit(point)
}
