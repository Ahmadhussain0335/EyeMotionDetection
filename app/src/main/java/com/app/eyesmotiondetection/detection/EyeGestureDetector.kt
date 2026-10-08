package com.app.eyesmotiondetection.detection

import com.app.eyesmotiondetection.detection.model.FaceData
import com.app.eyesmotiondetection.detection.model.GestureResult
import com.app.eyesmotiondetection.domain.model.DetectionSettings
import com.app.eyesmotiondetection.domain.model.EyeGesture
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EyeGestureDetector @Inject constructor() {

    @Volatile private var settings = DetectionSettings()
    private var prevData: FaceData? = null
    private var lastGestureTime = 0L

    // Blink tracking
    private val blinkTimes = mutableListOf<Long>()
    private var lastLeftBlinkTime = 0L
    private var lastRightBlinkTime = 0L
    private var minAvgDuringClose = 1f   // tracks deepest closure; squints stay > 0.15, full blinks drop near 0

    // Slow blink
    private var isSlowBlinkActive = false
    private var slowBlinkStart = 0L

    // Alternate blink
    private var altSide = 0 // 0=expect left, 1=expect right
    private var altCount = 0
    private var altWindowStart = 0L

    // Gaze history for shaking detection
    private val yawHistory = ArrayDeque<Float>(20)

    private val _gestureFlow = MutableSharedFlow<GestureResult>(extraBufferCapacity = 32)
    val gestureFlow: SharedFlow<GestureResult> = _gestureFlow

    private val _faceDataFlow = MutableSharedFlow<FaceData>(replay = 1, extraBufferCapacity = 64)
    val faceDataFlow: SharedFlow<FaceData> = _faceDataFlow

    fun updateSettings(s: DetectionSettings) { settings = s }

    fun processFaceData(data: FaceData) {
        _faceDataFlow.tryEmit(data)
        if (!data.faceDetected) { resetState(); prevData = null; return }
        val now = data.timestamp
        val lClosed = data.leftEyeOpenProbability < settings.blinkThreshold
        val rClosed = data.rightEyeOpenProbability < settings.blinkThreshold
        val bothClosed = lClosed && rClosed

        yawHistory.addLast(data.headEulerAngleY)
        if (yawHistory.size > 20) yawHistory.removeFirst()

        // Gaze gestures are consumed by face pointer when active — skip to avoid blocking blink cooldown
        if (!settings.isFacePointerEnabled) processGaze(data, now)
        processOpenness(data, now)
        processBlinks(data, lClosed, rClosed, bothClosed, now)
        processSlowBlink(bothClosed, data, now)
        processAlternateBlink(lClosed, rClosed, now)
        if (yawHistory.size >= 8) processEyeShaking(now)

        prevData = data
    }

    private fun processGaze(d: FaceData, now: Long) {
        // Front-camera angles are in camera space: positive yaw = camera-right = subject's LEFT.
        // Swap gesture names so LOOK_LEFT fires when the subject actually looks left.
        when {
            d.headEulerAngleY > settings.gazeRightThreshold -> emit(EyeGesture.LOOK_LEFT, 0.85f, now)
            d.headEulerAngleY < settings.gazeLeftThreshold -> emit(EyeGesture.LOOK_RIGHT, 0.85f, now)
            d.headEulerAngleX > settings.gazeDownThreshold -> emit(EyeGesture.LOOK_UP, 0.85f, now)
            d.headEulerAngleX < settings.gazeUpThreshold -> emit(EyeGesture.LOOK_DOWN, 0.85f, now)
        }
    }

    private fun processOpenness(d: FaceData, now: Long) {
        val avg = (d.leftEyeOpenProbability + d.rightEyeOpenProbability) / 2f
        val avgEAR = (d.leftEyeEAR + d.rightEyeEAR) / 2f
        when {
            avg > 0.90f -> emit(EyeGesture.SURPRISE_WIDE, 0.85f, now)
            avg > settings.wideOpenThreshold -> emit(EyeGesture.EYES_WIDE_OPEN, 0.75f, now)
            avgEAR < 0.18f && avg < 0.25f -> {
                emit(EyeGesture.SQUINT, 0.75f, now)
                emit(EyeGesture.ANGRY_NARROW, 0.65f, now)
            }
            avg in 0.25f..0.50f -> {
                emit(EyeGesture.EYES_HALF_OPEN, 0.70f, now)
                emit(EyeGesture.SLEEPY_EYES, 0.60f, now)
            }
        }
        val prev = prevData
        if (prev != null) {
            val prevAvg = (prev.leftEyeOpenProbability + prev.rightEyeOpenProbability) / 2f
            if (prevAvg - avg > 0.06f && avg in 0.3f..0.65f) emit(EyeGesture.LAZY_EYELID, 0.6f, now)
        }
    }

    private fun processBlinks(d: FaceData, lClosed: Boolean, rClosed: Boolean, bothClosed: Boolean, now: Long) {
        val prev = prevData ?: return
        val prevL = prev.leftEyeOpenProbability < settings.blinkThreshold
        val prevR = prev.rightEyeOpenProbability < settings.blinkThreshold
        val prevBoth = prevL && prevR
        val currentAvg = (d.leftEyeOpenProbability + d.rightEyeOpenProbability) / 2f

        // Track deepest closure to distinguish full blinks from squints
        if (bothClosed) {
            minAvgDuringClose = minOf(minAvgDuringClose, currentAvg)
        } else if (!prevBoth) {
            minAvgDuringClose = 1f
        }

        // Both-eye blink on closed→open transition
        if (prevBoth && !bothClosed) {
            val wasFullBlink = minAvgDuringClose < FULL_BLINK_THRESHOLD
            minAvgDuringClose = 1f
            if (wasFullBlink) {
                blinkTimes.add(now)
                blinkTimes.removeAll { now - it > settings.fastBlinkWindowMs }
                when {
                    blinkTimes.size >= settings.fastBlinkCountThreshold -> {
                        emit(EyeGesture.FAST_BLINK_MULTI, 0.90f, now, bypassCooldown = true); blinkTimes.clear()
                    }
                    // Bypass cooldown in pointer mode: processOpenness fires EYES_WIDE_OPEN every
                    // ~333ms which resets lastGestureTime, blocking BLINK_BOTH in the same frame.
                    else -> emit(EyeGesture.BLINK_BOTH, 0.85f, now, bypassCooldown = settings.isFacePointerEnabled)
                }
                val nervousCount = blinkTimes.count { now - it <= 2000L }
                if (nervousCount >= 6) { emit(EyeGesture.RAPID_NERVOUS_BLINK, 0.85f, now); blinkTimes.clear() }
            }
        }

        // Single-eye blinks
        if (prevL && !lClosed && !prevR) {
            emit(EyeGesture.BLINK_LEFT, 0.8f, now)
            if (now - lastLeftBlinkTime < 400L) emit(EyeGesture.DOUBLE_WINK, 0.85f, now)
            else emit(EyeGesture.WINK_LEFT, 0.90f, now)
            lastLeftBlinkTime = now
        }
        if (prevR && !rClosed && !prevL) {
            emit(EyeGesture.BLINK_RIGHT, 0.8f, now)
            if (now - lastRightBlinkTime < 400L) emit(EyeGesture.DOUBLE_WINK, 0.85f, now)
            else emit(EyeGesture.WINK_RIGHT, 0.90f, now)
            lastRightBlinkTime = now
        }
    }

    private fun processSlowBlink(bothClosed: Boolean, d: FaceData, now: Long) {
        if (bothClosed && !isSlowBlinkActive) { isSlowBlinkActive = true; slowBlinkStart = now }
        else if (!bothClosed && isSlowBlinkActive) {
            val dur = now - slowBlinkStart
            if (dur in 300L..1500L) {
                emit(EyeGesture.SLOW_BLINK, 0.80f, now)
                val avgOpen = (d.leftEyeOpenProbability + d.rightEyeOpenProbability) / 2f
                if (avgOpen > 0.7f) emit(EyeGesture.SLOW_CLOSE_REOPEN, 0.75f, now)
            }
            isSlowBlinkActive = false
        }
    }

    private fun processAlternateBlink(lClosed: Boolean, rClosed: Boolean, now: Long) {
        val prev = prevData ?: return
        val prevL = prev.leftEyeOpenProbability < settings.blinkThreshold
        val prevR = prev.rightEyeOpenProbability < settings.blinkThreshold
        if (now - altWindowStart > 3500L) { altCount = 0; altWindowStart = now }
        if (!prevL && lClosed && !rClosed && altSide == 0) {
            if (altWindowStart == 0L) altWindowStart = now
            altCount++; altSide = 1
        } else if (!prevR && rClosed && !lClosed && altSide == 1) {
            altCount++; altSide = 0
        }
        if (altCount >= 4) { emit(EyeGesture.ALTERNATE_BLINK, 0.85f, now); altCount = 0 }
    }

    private fun processEyeShaking(now: Long) {
        val recent = yawHistory.toList().takeLast(8)
        var changes = 0
        for (i in 1 until recent.size) {
            if ((recent[i - 1] > 8f && recent[i] < -8f) || (recent[i - 1] < -8f && recent[i] > 8f)) changes++
        }
        if (changes >= 3) emit(EyeGesture.EYES_SHAKING, 0.75f, now)
    }

    private fun emit(gesture: EyeGesture, confidence: Float, now: Long, bypassCooldown: Boolean = false) {
        if (!bypassCooldown && now - lastGestureTime < settings.globalCooldownMs) return
        lastGestureTime = now
        _gestureFlow.tryEmit(GestureResult(gesture, confidence, now))
    }

    private fun resetState() {
        isSlowBlinkActive = false; yawHistory.clear(); blinkTimes.clear(); minAvgDuringClose = 1f
    }

    fun reset() { resetState(); prevData = null }

    companion object {
        private const val FULL_BLINK_THRESHOLD = 0.15f  // avg must drop below this to count as a real blink (not a squint)
    }
}
