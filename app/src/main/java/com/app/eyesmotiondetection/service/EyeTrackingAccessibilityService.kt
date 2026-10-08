package com.app.eyesmotiondetection.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.app.eyesmotiondetection.data.datastore.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class EyeTrackingAccessibilityService : AccessibilityService() {

    @Inject lateinit var actionDispatcher: ActionDispatcher
    @Inject lateinit var appPreferences: AppPreferences

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var tapInFlight = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "onServiceConnected — accessibility service is ACTIVE")
        actionDispatcher.setAccessibilityService(this)
        scope.launch {
            appPreferences.detectionSettings.collect { actionDispatcher.updateSettings(it) }
        }
        scope.launch {
            GestureEventBus.events.collect { event ->
                actionDispatcher.dispatch(event.action)
            }
        }
        scope.launch {
            Log.d(TAG, "Subscribing to PointerClickBus")
            PointerClickBus.clicks.collect { point ->
                Log.d(TAG, "PointerClickBus received: (${point.x}, ${point.y}), tapInFlight=$tapInFlight")
                if (!tapInFlight) tapAt(point.x, point.y)
                else Log.w(TAG, "Tap skipped — previous tap still in flight")
            }
        }
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        Log.w(TAG, "onUnbind — accessibility service disconnected")
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        actionDispatcher.release()
        scope.cancel()
        super.onDestroy()
    }

    private fun tapAt(x: Float, y: Float) {
        tapInFlight = true
        // Safety net: reset after 600ms regardless of callback (covers crash/disconnect cases)
        scope.launch {
            delay(600L)
            if (tapInFlight) {
                tapInFlight = false
                Log.w(TAG, "tapInFlight safety-reset after timeout")
            }
        }
        Log.d(TAG, "tapAt($x, $y) — dispatching gesture")
        val path = Path().apply { moveTo(x, y) }
        val dispatched = dispatchGesture(
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0L, 100L))
                .build(),
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    tapInFlight = false
                    Log.d(TAG, "tapAt COMPLETED at ($x, $y)")
                }
                override fun onCancelled(gestureDescription: GestureDescription) {
                    tapInFlight = false
                    Log.w(TAG, "tapAt cancelled")
                }
            },
            null
        )
        if (!dispatched) {
            tapInFlight = false
            Log.e(TAG, "dispatchGesture returned FALSE — gesture rejected by system")
        } else {
            Log.d(TAG, "dispatchGesture returned: true")
        }
    }

    companion object {
        private const val TAG = "EyeAccessibility"
    }
}
