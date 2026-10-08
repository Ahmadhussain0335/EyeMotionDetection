package com.app.eyesmotiondetection

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.app.eyesmotiondetection.data.datastore.AppPreferences
import com.app.eyesmotiondetection.service.EyeTrackingForegroundService
import com.app.eyesmotiondetection.ui.navigation.AppNavigation
import com.app.eyesmotiondetection.ui.theme.EyesMotionDetectionTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleResumeTracking(intent)
        setContent {
            val isDark by appPreferences.isDarkMode.collectAsState(initial = false)
            EyesMotionDetectionTheme(darkTheme = isDark) {
                AppNavigation(navController = rememberNavController())
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleResumeTracking(intent)
    }

    private fun handleResumeTracking(intent: Intent) {
        if (intent.component?.className != RESUME_TRACKING_ALIAS) return
        EyeTrackingForegroundService.start(this)
    }

    companion object {
        /** Non-exported activity-alias used only by ResumeTrackingNotification. */
        const val RESUME_TRACKING_ALIAS = "com.app.eyesmotiondetection.ResumeTrackingAlias"
    }
}
