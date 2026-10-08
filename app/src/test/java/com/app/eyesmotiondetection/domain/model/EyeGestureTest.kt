package com.app.eyesmotiondetection.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EyeGestureTest {

    @Test
    fun `gestures the detector never emits are not supported`() {
        val unsupported = EyeGesture.entries.filterNot { it.isSupported }.toSet()
        assertEquals(
            setOf(EyeGesture.EYE_ROLL, EyeGesture.RANDOM_BLINK_PATTERN, EyeGesture.EYE_TRACKING, EyeGesture.EYE_POP),
            unsupported
        )
        assertEquals(EyeGesture.entries.size - 4, EyeGesture.supported.size)
    }

    @Test
    fun `every default mapping uses a supported gesture`() {
        assertTrue(
            com.app.eyesmotiondetection.data.repository.GestureMappingRepository.defaultMappings
                .all { it.gesture.isSupported }
        )
    }
}
