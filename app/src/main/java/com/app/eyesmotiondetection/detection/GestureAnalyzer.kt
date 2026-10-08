package com.app.eyesmotiondetection.detection

import com.app.eyesmotiondetection.data.repository.GestureLogRepository
import com.app.eyesmotiondetection.data.repository.GestureMappingRepository
import com.app.eyesmotiondetection.domain.model.AppAction
import com.app.eyesmotiondetection.domain.model.DetectionSettings
import com.app.eyesmotiondetection.domain.model.EyeGesture
import com.app.eyesmotiondetection.domain.model.GestureLog
import com.app.eyesmotiondetection.service.FacePointerManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GestureAnalyzer @Inject constructor(
    private val detector: EyeGestureDetector,
    private val mappingRepository: GestureMappingRepository,
    private val logRepository: GestureLogRepository,
    private val facePointerManager: FacePointerManager
) {
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _actionFlow = MutableSharedFlow<ActionEvent>(extraBufferCapacity = 32)
    val actionFlow: SharedFlow<ActionEvent> = _actionFlow
    private val gestureCooldowns = mutableMapOf<EyeGesture, Long>()
    @Volatile private var settings = DetectionSettings()

    fun start() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            detector.gestureFlow.collect { result ->
                val now = System.currentTimeMillis()
                val mapping = mappingRepository.getMappingForGesture(result.gesture) ?: return@collect
                if (!mapping.isEnabled) return@collect
                // When the face pointer is visible, gestures mapped to TAP are consumed as pointer
                // clicks — suppress the regular mapped action to avoid a double tap
                if (facePointerManager.isActive && mapping.action == AppAction.TAP) return@collect
                val lastTime = gestureCooldowns[result.gesture] ?: 0L
                if (now - lastTime < mapping.cooldownMs) return@collect
                gestureCooldowns[result.gesture] = now
                _actionFlow.emit(ActionEvent(result.gesture, mapping.action, now))
                if (settings.isLoggingEnabled) {
                    logRepository.logGesture(
                        GestureLog(gesture = result.gesture, action = mapping.action,
                            timestamp = now, confidence = result.confidence)
                    )
                }
            }
        }
    }

    fun stop() { scope.cancel() }

    fun updateSettings(s: DetectionSettings) { settings = s; detector.updateSettings(s) }
}
