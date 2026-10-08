package com.app.eyesmotiondetection.detection

import com.app.eyesmotiondetection.domain.model.DetectionSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationCalculatorTest {

    private val current = DetectionSettings()

    private fun samples(
        open: Float = 0.95f,
        closed: Float = 0.05f,
        baseYaw: Float = 0f,
        basePitch: Float = 0f,
        left: Float = 30f,
        right: Float = -30f,
        up: Float = 20f,
        down: Float = -20f
    ) = CalibrationSamples(
        eyesOpen = List(10) { open },
        eyesClosed = List(10) { closed },
        neutralYaw = List(10) { baseYaw },
        neutralPitch = List(10) { basePitch },
        turnLeftYaw = List(10) { left },
        turnRightYaw = List(10) { right },
        tiltUpPitch = List(10) { up },
        tiltDownPitch = List(10) { down }
    )

    private fun success(s: CalibrationSamples) =
        (CalibrationCalculator.compute(current, s) as CalibrationResult.Success).settings

    @Test
    fun `blink threshold sits 40 percent between closed and open`() {
        assertEquals(0.41f, success(samples(open = 0.95f, closed = 0.05f)).blinkThreshold, 1e-4f)
    }

    @Test
    fun `gaze thresholds are 60 percent of range around neutral pose`() {
        val s = success(samples(baseYaw = 5f, basePitch = -3f, left = 35f, right = -25f, up = 17f, down = -23f))
        assertEquals(5f + 18f, s.gazeRightThreshold, 1e-4f)
        assertEquals(5f - 18f, s.gazeLeftThreshold, 1e-4f)
        assertEquals(-3f + 12f, s.gazeDownThreshold, 1e-4f)
        assertEquals(-3f - 12f, s.gazeUpThreshold, 1e-4f)
    }

    @Test
    fun `offsets are clamped to sane bounds`() {
        val s = success(samples(left = 80f, right = -80f, up = 8f, down = -8f))
        assertEquals(30f, s.gazeRightThreshold, 1e-4f)
        assertEquals(-30f, s.gazeLeftThreshold, 1e-4f)
        assertEquals(4.8f, s.gazeDownThreshold, 1e-4f)
    }

    @Test
    fun `unrelated settings are preserved`() {
        val s = success(samples())
        assertEquals(current.globalCooldownMs, s.globalCooldownMs)
        assertEquals(current.wideOpenThreshold, s.wideOpenThreshold, 0f)
    }

    @Test
    fun `fails when eyes were not closed`() {
        val r = CalibrationCalculator.compute(current, samples(open = 0.9f, closed = 0.8f))
        assertTrue(r is CalibrationResult.Failure)
    }

    @Test
    fun `fails when head barely moved`() {
        val r = CalibrationCalculator.compute(current, samples(left = 3f, right = -3f))
        assertTrue(r is CalibrationResult.Failure)
    }

    @Test
    fun `fails when both turns went the same way`() {
        val r = CalibrationCalculator.compute(current, samples(left = 20f, right = 25f))
        assertTrue(r is CalibrationResult.Failure)
    }

    @Test
    fun `extreme deviation ignores a single outlier frame`() {
        val values = List(19) { 20f } + 90f
        assertEquals(20f, CalibrationCalculator.extremeDeviation(values, 0f), 1e-4f)
    }
}
