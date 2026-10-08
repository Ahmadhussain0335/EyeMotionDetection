package com.app.eyesmotiondetection.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.media.AudioManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.WindowManager
import com.app.eyesmotiondetection.domain.model.AppAction
import com.app.eyesmotiondetection.domain.model.DetectionSettings
import com.app.eyesmotiondetection.util.vibrateOnce
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val voiceFeedback: VoiceFeedback
) {
    private var service: AccessibilityService? = null
    private var settings = DetectionSettings()

    fun setAccessibilityService(svc: AccessibilityService?) { service = svc }

    fun updateSettings(s: DetectionSettings) {
        settings = s
        voiceFeedback.setEnabled(s.isVoiceFeedbackEnabled)
    }

    fun release() {
        service = null
        voiceFeedback.shutdown()
    }

    fun dispatch(action: AppAction) {
        if (settings.isVibrationEnabled) context.vibrateOnce()
        if (settings.isVoiceFeedbackEnabled && action != AppAction.NONE) voiceFeedback.speak(action.displayName)
        val svc = service
        when (action) {
            AppAction.SCROLL_UP -> svc?.scroll(up = true)
            AppAction.SCROLL_DOWN -> svc?.scroll(up = false)
            AppAction.TAP -> svc?.tap()
            AppAction.DOUBLE_TAP -> svc?.doubleTap()
            AppAction.LONG_PRESS -> svc?.longPress()
            AppAction.BACK -> svc?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            AppAction.HOME -> svc?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            AppAction.RECENT_APPS -> svc?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            AppAction.OPEN_NOTIFICATIONS -> svc?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            AppAction.LOCK_SCREEN -> svc?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            AppAction.VOLUME_UP -> adjustVolume(AudioManager.ADJUST_RAISE)
            AppAction.VOLUME_DOWN -> adjustVolume(AudioManager.ADJUST_LOWER)
            AppAction.PLAY_PAUSE_MEDIA -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            AppAction.NEXT_SONG -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            AppAction.PREVIOUS_SONG -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            AppAction.SWIPE_LEFT -> svc?.swipe(left = true)
            AppAction.SWIPE_RIGHT -> svc?.swipe(left = false)
            AppAction.ZOOM_IN -> svc?.pinch(zoomIn = true)
            AppAction.ZOOM_OUT -> svc?.pinch(zoomIn = false)
            AppAction.OPEN_APP, AppAction.NONE -> Unit
        }
    }

    private fun screenSize(): Pair<Int, Int> {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val b = wm.currentWindowMetrics.bounds; Pair(b.width(), b.height())
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION") wm.defaultDisplay.getMetrics(dm)
            Pair(dm.widthPixels, dm.heightPixels)
        }
    }

    private fun AccessibilityService.scroll(up: Boolean) {
        val (w, h) = screenSize()
        val sY = if (up) h * 0.7f else h * 0.3f
        val eY = if (up) h * 0.3f else h * 0.7f
        gesture(Path().apply { moveTo(w / 2f, sY); lineTo(w / 2f, eY) }, 300)
    }

    private fun AccessibilityService.tap() {
        val (w, h) = screenSize()
        gesture(Path().apply { moveTo(w / 2f, h / 2f) }, 50)
    }

    private fun AccessibilityService.doubleTap() {
        val (w, h) = screenSize()
        val p = Path().apply { moveTo(w / 2f, h / 2f) }
        dispatchGesture(GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p, 0, 50))
            .addStroke(GestureDescription.StrokeDescription(p, 150, 50)).build(), null, null)
    }

    private fun AccessibilityService.longPress() {
        val (w, h) = screenSize()
        gesture(Path().apply { moveTo(w / 2f, h / 2f) }, 800)
    }

    private fun AccessibilityService.swipe(left: Boolean) {
        val (w, h) = screenSize()
        val sX = if (left) w * 0.8f else w * 0.2f
        val eX = if (left) w * 0.2f else w * 0.8f
        gesture(Path().apply { moveTo(sX, h / 2f); lineTo(eX, h / 2f) }, 300)
    }

    private fun AccessibilityService.pinch(zoomIn: Boolean) {
        val (w, h) = screenSize()
        val cx = w / 2f; val cy = h / 2f
        val s = if (zoomIn) 60f else 150f; val e = if (zoomIn) 150f else 60f
        dispatchGesture(GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(
                Path().apply { moveTo(cx - s, cy); lineTo(cx - e, cy) }, 0, 300))
            .addStroke(GestureDescription.StrokeDescription(
                Path().apply { moveTo(cx + s, cy); lineTo(cx + e, cy) }, 0, 300))
            .build(), null, null)
    }

    private fun AccessibilityService.gesture(path: Path, durationMs: Long) {
        dispatchGesture(GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs)).build(), null, null)
    }

    private fun adjustVolume(dir: Int) {
        (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager)
            .adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, AudioManager.FLAG_SHOW_UI)
    }

    private fun sendMediaKey(keyCode: Int) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }
}
