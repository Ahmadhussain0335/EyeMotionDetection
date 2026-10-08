package com.app.eyesmotiondetection.service

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import com.app.eyesmotiondetection.data.datastore.AppPreferences
import com.app.eyesmotiondetection.data.repository.GestureMappingRepository
import com.app.eyesmotiondetection.detection.EyeGestureDetector
import com.app.eyesmotiondetection.domain.model.AppAction
import com.app.eyesmotiondetection.domain.model.EyeGesture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FacePointerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gestureDetector: EyeGestureDetector,
    private val appPreferences: AppPreferences,
    private val mappingRepository: GestureMappingRepository
) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var cursorView: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var scope: CoroutineScope? = null

    // Smoothed cursor center (screen coords, may exceed bounds before clamping)
    private var smoothX = 0f
    private var smoothY = 0f
    private var initialized = false

    // Actual on-screen cursor center in valid screen coords — use this for taps
    private var displayCenterX = 0f
    private var displayCenterY = 0f

    // Last position written to WindowManager — used for deadzone check
    private var lastLaidX = -1
    private var lastLaidY = -1

    // Pre-smoothing: rolling average of raw angles to kill frame-to-frame noise
    private val yawBuf = ArrayDeque<Float>(ANGLE_BUF)
    private val pitchBuf = ArrayDeque<Float>(ANGLE_BUF)

    // Cached screen dimensions — refreshed once per cursor session
    private var screenW = 0
    private var screenH = 0

    private val cursorDiameterPx: Int by lazy {
        (36 * context.resources.displayMetrics.density).toInt()
    }

    // True while the cursor is visible — used to suppress conflicting regular tap actions
    var isActive: Boolean = false
        private set

    // Set of gestures the user has mapped to TAP — refreshed live from the DB
    private var tapGestures: Set<EyeGesture> = emptySet()

    fun start() {
        scope?.cancel()
        scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        // Keep tapGestures in sync with the gesture-map DB
        scope?.launch {
            mappingRepository.getAllMappings().collect { mappings ->
                tapGestures = mappings
                    .filter { it.isEnabled && it.action == AppAction.TAP }
                    .map { it.gesture }
                    .toSet()
                Log.d(TAG, "Tap gestures updated: $tapGestures")
            }
        }

        // Position tracking
        scope?.launch {
            combine(gestureDetector.faceDataFlow, appPreferences.detectionSettings) { face, settings ->
                Pair(face, settings)
            }.collect { (face, settings) ->
                if (!settings.isFacePointerEnabled || !face.faceDetected) {
                    removeCursor()
                    return@collect
                }
                ensureCursor()
                updatePosition(face.headEulerAngleY, face.headEulerAngleX, settings.pointerSpeed, settings.pointerSensitivity)
            }
        }

        // Click whenever the detected gesture is mapped to TAP in the gesture-map screen
        scope?.launch {
            gestureDetector.gestureFlow.collect { result ->
                Log.d(TAG, "gestureFlow received: ${result.gesture}, isActive=$isActive, tapGestures=$tapGestures")
                if (!isActive) return@collect
                if (tapGestures.isEmpty()) {
                    Log.w(TAG, "No TAP gestures configured in gesture map — pointer click ignored")
                    return@collect
                }
                if (result.gesture in tapGestures) {
                    val cx = displayCenterX
                    val cy = displayCenterY
                    Log.d(TAG, "Pointer click triggered: center=($cx, $cy)")
                    if (cx > 0f && cy > 0f) {
                        if (!isAccessibilityServiceConnected()) {
                            Log.e(TAG, "CLICK FAILED — Accessibility Service is NOT enabled! " +
                                "Go to Settings → Accessibility → Downloaded apps → enable Eye Gesture Control")
                        } else {
                            val ok = PointerClickBus.tryEmit(ClickPoint(cx, cy))
                            Log.d(TAG, "PointerClickBus.tryEmit=${if (ok) "SUCCESS" else "DROPPED"} at ($cx, $cy)")
                        }
                    } else {
                        Log.w(TAG, "Click skipped — displayCenter not set yet")
                    }
                }
            }
        }
    }

    fun stop() {
        scope?.cancel()
        scope = null
        removeCursor()
        resetState()
    }

    private fun resetState() {
        initialized = false
        lastLaidX = -1
        lastLaidY = -1
        displayCenterX = 0f
        displayCenterY = 0f
        yawBuf.clear()
        pitchBuf.clear()
    }

    private fun ensureCursor() {
        if (cursorView != null) return

        val (sw, sh) = fetchScreenSize()
        screenW = sw
        screenH = sh
        smoothX = sw / 2f
        smoothY = sh / 2f
        resetState()

        val diameter = cursorDiameterPx
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0x8000E5FF.toInt())
            setStroke((2 * context.resources.displayMetrics.density).toInt(), 0xCCFFFFFF.toInt())
        }

        val view = View(context).apply { background = drawable }

        val lp = WindowManager.LayoutParams(
            diameter, diameter,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (sw / 2 - diameter / 2).coerceAtLeast(0)
            y = (sh / 2 - diameter / 2).coerceAtLeast(0)
        }

        runCatching {
            wm.addView(view, lp)
            cursorView = view
            params = lp
            isActive = true
        }.onFailure { android.util.Log.e("FacePointerManager", "addView failed", it) }
    }

    private fun removeCursor() {
        runCatching { cursorView?.let { wm.removeView(it) } }
        cursorView = null
        params = null
        isActive = false
        resetState()
    }

    private fun updatePosition(yaw: Float, pitch: Float, speed: Float, sensitivity: Float) {
        val lp = params ?: return
        val sw = screenW.takeIf { it > 0 } ?: return
        val sh = screenH.takeIf { it > 0 } ?: return
        val diameter = cursorDiameterPx

        // Step 1: rolling average on raw angles (kills per-frame noise)
        yawBuf.addLast(yaw)
        if (yawBuf.size > ANGLE_BUF) yawBuf.removeFirst()
        pitchBuf.addLast(pitch)
        if (pitchBuf.size > ANGLE_BUF) pitchBuf.removeFirst()
        val avgYaw = yawBuf.average().toFloat()
        val avgPitch = pitchBuf.average().toFloat()

        // Step 2: map averaged angles to screen coords
        // Negate yaw and pitch: front camera reports angles in camera space (mirror of subject).
        // Positive yaw = face toward camera-right = subject's LEFT → without negation the cursor
        // moves right when the user looks left.  Pitch has the same mirror effect vertically.
        val rawX = sw / 2f - avgYaw * sensitivity * speed * (sw / 60f)
        val rawY = sh / 2f - avgPitch * sensitivity * speed * (sh / 40f)

        // Step 3: EMA with heavy smoothing (α=0.12 — much calmer than 0.25)
        if (!initialized) {
            smoothX = rawX
            smoothY = rawY
            initialized = true
        } else {
            smoothX = smoothX * (1f - ALPHA) + rawX * ALPHA
            smoothY = smoothY * (1f - ALPHA) + rawY * ALPHA
        }

        // Step 4: deadzone — skip layout update for sub-pixel jitter
        val newX = (smoothX - diameter / 2f).coerceIn(0f, (sw - diameter).toFloat()).toInt()
        val newY = (smoothY - diameter / 2f).coerceIn(0f, (sh - diameter).toFloat()).toInt()
        if (Math.abs(newX - lastLaidX) < DEADZONE_PX && Math.abs(newY - lastLaidY) < DEADZONE_PX) return

        lp.x = newX
        lp.y = newY
        lastLaidX = newX
        lastLaidY = newY
        // Record clamped center for tap dispatch — this is always a valid screen coordinate
        displayCenterX = newX + diameter / 2f
        displayCenterY = newY + diameter / 2f

        runCatching { wm.updateViewLayout(cursorView, lp) }
    }

    private fun isAccessibilityServiceConnected(): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName == context.packageName }
    }

    @Suppress("DEPRECATION")
    private fun fetchScreenSize(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            Pair(bounds.width(), bounds.height())
        } else {
            val dm = DisplayMetrics()
            wm.defaultDisplay.getRealMetrics(dm)
            Pair(dm.widthPixels, dm.heightPixels)
        }
    }

    companion object {
        private const val TAG = "FacePointerManager"
        private const val ALPHA = 0.12f      // EMA weight for new data (lower = smoother)
        private const val ANGLE_BUF = 5      // frames to average before EMA
        private const val DEADZONE_PX = 3    // skip update if cursor moves < 3px
    }
}
