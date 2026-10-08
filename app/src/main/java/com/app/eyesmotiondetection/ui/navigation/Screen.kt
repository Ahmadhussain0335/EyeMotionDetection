package com.app.eyesmotiondetection.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Permission : Screen("permission")
    object Home : Screen("home")
    object GestureMapping : Screen("gesture_mapping")
    object Calibration : Screen("calibration")
    object LivePreview : Screen("live_preview")
    object Settings : Screen("settings")
    object Logs : Screen("logs")
}
