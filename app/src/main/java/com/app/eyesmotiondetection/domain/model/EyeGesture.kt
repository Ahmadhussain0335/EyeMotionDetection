package com.app.eyesmotiondetection.domain.model

/**
 * @property isSupported false for gestures [com.app.eyesmotiondetection.detection.EyeGestureDetector]
 * never emits. They stay in the enum so stored mappings keep resolving, but are hidden from the UI.
 */
enum class EyeGesture(
    val displayName: String,
    val description: String,
    val isSupported: Boolean = true
) {
    BLINK_BOTH("Both Eyes Blink", "Both eyes blink simultaneously"),
    BLINK_LEFT("Left Eye Blink", "Only left eye blinks"),
    BLINK_RIGHT("Right Eye Blink", "Only right eye blinks"),
    SLOW_BLINK("Slow Blink", "Both eyes close and open slowly (300-1500ms)"),
    FAST_BLINK_MULTI("Fast Multi-Blink", "3+ rapid blinks within 1 second"),
    EYES_WIDE_OPEN("Eyes Wide Open", "Both eyes opened wider than normal"),
    EYES_HALF_OPEN("Eyes Half Open", "Eyes partially closed"),
    WINK_LEFT("Left Wink", "Quick left eye wink only"),
    WINK_RIGHT("Right Wink", "Quick right eye wink only"),
    DOUBLE_WINK("Double Wink", "Two consecutive winks of same eye"),
    LOOK_LEFT("Look Left", "Eyes and/or head turned left"),
    LOOK_RIGHT("Look Right", "Eyes and/or head turned right"),
    LOOK_UP("Look Up", "Eyes and/or head tilted upward"),
    LOOK_DOWN("Look Down", "Eyes and/or head tilted downward"),
    EYE_ROLL("Eye Roll", "Eyes moving in a circular pattern", isSupported = false),
    EYES_SHAKING("Eyes Shaking", "Rapid left-right gaze oscillation"),
    SLEEPY_EYES("Sleepy Eyes", "Eyes slowly drooping halfway"),
    ANGRY_NARROW("Angry Narrow", "Eyes narrowed/squinted tightly"),
    SURPRISE_WIDE("Surprise Wide", "Eyes opened very wide"),
    RANDOM_BLINK_PATTERN("Random Blink", "Irregular blinking sequence", isSupported = false),
    SLOW_CLOSE_REOPEN("Slow Close and Reopen", "Eyes close slowly then open wide"),
    RAPID_NERVOUS_BLINK("Rapid Nervous Blink", "6+ blinks in 2 seconds"),
    EYE_TRACKING("Eye Tracking", "Sustained directional gaze hold", isSupported = false),
    LAZY_EYELID("Lazy Eyelid", "Eyelid gradually drooping"),
    ALTERNATE_BLINK("Alternate Blink", "Left then right eye alternating"),
    SQUINT("Squint", "Both eyes squinting"),
    EYE_POP("Eye Pop", "Exaggerated sudden eye widening", isSupported = false);

    companion object {
        val supported: List<EyeGesture> = entries.filter { it.isSupported }
    }
}
