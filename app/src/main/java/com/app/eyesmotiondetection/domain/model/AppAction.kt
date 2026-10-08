package com.app.eyesmotiondetection.domain.model

enum class AppAction(val displayName: String) {
    SCROLL_UP("Scroll Up"),
    SCROLL_DOWN("Scroll Down"),
    TAP("Tap"),
    DOUBLE_TAP("Double Tap"),
    LONG_PRESS("Long Press"),
    BACK("Back"),
    HOME("Home"),
    RECENT_APPS("Recent Apps"),
    OPEN_NOTIFICATIONS("Open Notifications"),
    VOLUME_UP("Volume Up"),
    VOLUME_DOWN("Volume Down"),
    LOCK_SCREEN("Lock Screen"),
    PLAY_PAUSE_MEDIA("Play / Pause"),
    NEXT_SONG("Next Song"),
    PREVIOUS_SONG("Previous Song"),
    OPEN_APP("Open App"),
    SWIPE_LEFT("Swipe Left"),
    SWIPE_RIGHT("Swipe Right"),
    ZOOM_IN("Zoom In"),
    ZOOM_OUT("Zoom Out"),
    NONE("None")
}
