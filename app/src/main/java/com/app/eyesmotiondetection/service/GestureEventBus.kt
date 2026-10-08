package com.app.eyesmotiondetection.service

import com.app.eyesmotiondetection.detection.ActionEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object GestureEventBus {
    private val _events = MutableSharedFlow<ActionEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<ActionEvent> = _events

    fun tryEmit(event: ActionEvent) { _events.tryEmit(event) }
    suspend fun emit(event: ActionEvent) { _events.emit(event) }
}
