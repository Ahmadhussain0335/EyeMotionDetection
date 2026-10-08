package com.app.eyesmotiondetection.detection

import com.app.eyesmotiondetection.domain.model.DetectionSettings
import kotlin.math.abs
import kotlin.math.sign

/** Raw per-step readings collected by the calibration wizard (face-detected frames only). */
data class CalibrationSamples(
    val eyesOpen: List<Float>,
    val eyesClosed: List<Float>,
    val neutralYaw: List<Float>,
    val neutralPitch: List<Float>,
    val turnLeftYaw: List<Float>,
    val turnRightYaw: List<Float>,
    val tiltUpPitch: List<Float>,
    val tiltDownPitch: List<Float>
)

sealed interface CalibrationResult {
    data class Success(val settings: DetectionSettings) : CalibrationResult
    data class Failure(val reason: String) : CalibrationResult
}

/**
 * Derives personal thresholds from calibration samples.
 *
 * - Blink: 40% of the way from the user's closed-eye to open-eye probability.
 * - Head turn/tilt: 60% of the user's comfortable range, measured from their neutral pose, so a
 *   phone held off-centre does not bias one direction. Thresholds stay absolute angles because
 *   [EyeGestureDetector] compares raw Euler angles.
 */
object CalibrationCalculator {
    const val MIN_EYE_GAP = 0.3f
    const val MIN_MOVEMENT_DEG = 6f
    private const val BLINK_POSITION = 0.4f
    private const val RANGE_FRACTION = 0.6f
    private val YAW_OFFSET = 5f..30f
    private val PITCH_OFFSET = 4f..25f

    fun compute(current: DetectionSettings, s: CalibrationSamples): CalibrationResult {
        val open = s.eyesOpen.median()
        val closed = s.eyesClosed.median()
        if (open - closed < MIN_EYE_GAP) {
            return CalibrationResult.Failure(
                "Closed-eye reading was too close to open-eye. Close your eyes fully in that step."
            )
        }

        val baseYaw = s.neutralYaw.median()
        val basePitch = s.neutralPitch.median()
        val leftDev = extremeDeviation(s.turnLeftYaw, baseYaw)
        val rightDev = extremeDeviation(s.turnRightYaw, baseYaw)
        val upDev = extremeDeviation(s.tiltUpPitch, basePitch)
        val downDev = extremeDeviation(s.tiltDownPitch, basePitch)

        if (!isOpposingMovement(leftDev, rightDev)) {
            return CalibrationResult.Failure("Head turn was too small. Turn further left and right.")
        }
        if (!isOpposingMovement(upDev, downDev)) {
            return CalibrationResult.Failure("Head tilt was too small. Tilt further up and down.")
        }

        val yawPos = maxOf(leftDev, rightDev)
        val yawNeg = minOf(leftDev, rightDev)
        val pitchPos = maxOf(upDev, downDev)
        val pitchNeg = minOf(upDev, downDev)

        return CalibrationResult.Success(
            current.copy(
                blinkThreshold = (closed + (open - closed) * BLINK_POSITION).coerceIn(0.1f, 0.6f),
                gazeRightThreshold = baseYaw + (yawPos * RANGE_FRACTION).coerceIn(YAW_OFFSET),
                gazeLeftThreshold = baseYaw - (-yawNeg * RANGE_FRACTION).coerceIn(YAW_OFFSET),
                gazeDownThreshold = basePitch + (pitchPos * RANGE_FRACTION).coerceIn(PITCH_OFFSET),
                gazeUpThreshold = basePitch - (-pitchNeg * RANGE_FRACTION).coerceIn(PITCH_OFFSET)
            )
        )
    }

    private fun isOpposingMovement(a: Float, b: Float) =
        abs(a) >= MIN_MOVEMENT_DEG && abs(b) >= MIN_MOVEMENT_DEG && a.sign != b.sign

    /** Signed deviation from [base] at the 90th percentile, robust to single noisy frames. */
    internal fun extremeDeviation(values: List<Float>, base: Float): Float {
        val devs = values.map { it - base }
        val direction = devs.sum().sign
        val magnitudes = devs.map { abs(it) }.sorted()
        val p90 = magnitudes[((magnitudes.size - 1) * 0.9f).toInt()]
        return direction * p90
    }

    internal fun List<Float>.median(): Float {
        val sorted = sorted()
        val mid = size / 2
        return if (size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2f else sorted[mid]
    }
}
