package com.app.eyesmotiondetection.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.app.eyesmotiondetection.ui.screens.calibration.CalibrationScreen
import com.app.eyesmotiondetection.ui.screens.gesturemapping.GestureMappingScreen
import com.app.eyesmotiondetection.ui.screens.home.HomeScreen
import com.app.eyesmotiondetection.ui.screens.logs.LogsScreen
import com.app.eyesmotiondetection.ui.screens.onboarding.OnboardingScreen
import com.app.eyesmotiondetection.ui.screens.permission.PermissionScreen
import com.app.eyesmotiondetection.ui.screens.preview.LivePreviewScreen
import com.app.eyesmotiondetection.ui.screens.settings.SettingsScreen
import com.app.eyesmotiondetection.ui.screens.splash.SplashScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Permission.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Permission.route) {
            PermissionScreen(
                onPermissionsGranted = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Permission.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToGestureMapping = { navController.navigate(Screen.GestureMapping.route) },
                onNavigateToCalibration = { navController.navigate(Screen.Calibration.route) },
                onNavigateToLivePreview = { navController.navigate(Screen.LivePreview.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToLogs = { navController.navigate(Screen.Logs.route) }
            )
        }
        composable(Screen.GestureMapping.route) { GestureMappingScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Calibration.route) { CalibrationScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.LivePreview.route) { LivePreviewScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Settings.route) { SettingsScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Logs.route) { LogsScreen(onBack = { navController.popBackStack() }) }
    }
}
