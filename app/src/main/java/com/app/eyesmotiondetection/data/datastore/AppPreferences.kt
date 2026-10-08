package com.app.eyesmotiondetection.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.app.eyesmotiondetection.domain.model.DetectionSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "eye_tracking_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val IS_ONBOARDED = booleanPreferencesKey("is_onboarded")
        val IS_SERVICE_ENABLED = booleanPreferencesKey("is_service_enabled")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val BLINK_THRESHOLD = floatPreferencesKey("blink_threshold")
        val WIDE_OPEN_THRESHOLD = floatPreferencesKey("wide_open_threshold")
        val HALF_OPEN_THRESHOLD = floatPreferencesKey("half_open_threshold")
        val GAZE_LEFT_THRESHOLD = floatPreferencesKey("gaze_left_threshold")
        val GAZE_RIGHT_THRESHOLD = floatPreferencesKey("gaze_right_threshold")
        val GAZE_UP_THRESHOLD = floatPreferencesKey("gaze_up_threshold")
        val GAZE_DOWN_THRESHOLD = floatPreferencesKey("gaze_down_threshold")
        val FAST_BLINK_COUNT = intPreferencesKey("fast_blink_count")
        val FAST_BLINK_WINDOW_MS = longPreferencesKey("fast_blink_window_ms")
        val GLOBAL_COOLDOWN_MS = longPreferencesKey("global_cooldown_ms")
        val DEBOUNCE_MS = longPreferencesKey("debounce_ms")
        val IS_VIBRATION_ENABLED = booleanPreferencesKey("is_vibration_enabled")
        val IS_VOICE_FEEDBACK_ENABLED = booleanPreferencesKey("is_voice_feedback_enabled")
        val IS_OVERLAY_ENABLED = booleanPreferencesKey("is_overlay_enabled")
        val IS_LOGGING_ENABLED = booleanPreferencesKey("is_logging_enabled")
        val IS_FACE_POINTER_ENABLED = booleanPreferencesKey("is_face_pointer_enabled")
        val POINTER_SPEED = floatPreferencesKey("pointer_speed")
        val POINTER_SENSITIVITY = floatPreferencesKey("pointer_sensitivity")
    }

    val isOnboarded: Flow<Boolean> = dataStore.data.map { it[IS_ONBOARDED] ?: false }
    val isServiceEnabled: Flow<Boolean> = dataStore.data.map { it[IS_SERVICE_ENABLED] ?: false }
    val isDarkMode: Flow<Boolean> = dataStore.data.map { it[IS_DARK_MODE] ?: false }

    val detectionSettings: Flow<DetectionSettings> = dataStore.data.map { prefs ->
        DetectionSettings(
            blinkThreshold = prefs[BLINK_THRESHOLD] ?: 0.3f,
            wideOpenThreshold = prefs[WIDE_OPEN_THRESHOLD] ?: 0.75f,
            halfOpenThreshold = prefs[HALF_OPEN_THRESHOLD] ?: 0.45f,
            gazeLeftThreshold = prefs[GAZE_LEFT_THRESHOLD] ?: -15f,
            gazeRightThreshold = prefs[GAZE_RIGHT_THRESHOLD] ?: 15f,
            gazeUpThreshold = prefs[GAZE_UP_THRESHOLD] ?: -10f,
            gazeDownThreshold = prefs[GAZE_DOWN_THRESHOLD] ?: 10f,
            fastBlinkCountThreshold = prefs[FAST_BLINK_COUNT] ?: 3,
            fastBlinkWindowMs = prefs[FAST_BLINK_WINDOW_MS] ?: 1000L,
            globalCooldownMs = prefs[GLOBAL_COOLDOWN_MS] ?: 500L,
            debounceMs = prefs[DEBOUNCE_MS] ?: 150L,
            isVibrationEnabled = prefs[IS_VIBRATION_ENABLED] ?: true,
            isVoiceFeedbackEnabled = prefs[IS_VOICE_FEEDBACK_ENABLED] ?: false,
            isOverlayEnabled = prefs[IS_OVERLAY_ENABLED] ?: true,
            isLoggingEnabled = prefs[IS_LOGGING_ENABLED] ?: true,
            isFacePointerEnabled = prefs[IS_FACE_POINTER_ENABLED] ?: false,
            pointerSpeed = prefs[POINTER_SPEED] ?: 1.5f,
            pointerSensitivity = prefs[POINTER_SENSITIVITY] ?: 3.0f
        )
    }

    suspend fun setOnboarded(value: Boolean) = dataStore.edit { it[IS_ONBOARDED] = value }
    suspend fun setServiceEnabled(value: Boolean) = dataStore.edit { it[IS_SERVICE_ENABLED] = value }
    suspend fun setDarkMode(value: Boolean) = dataStore.edit { it[IS_DARK_MODE] = value }

    suspend fun updateDetectionSettings(s: DetectionSettings) {
        dataStore.edit { p ->
            p[BLINK_THRESHOLD] = s.blinkThreshold
            p[WIDE_OPEN_THRESHOLD] = s.wideOpenThreshold
            p[HALF_OPEN_THRESHOLD] = s.halfOpenThreshold
            p[GAZE_LEFT_THRESHOLD] = s.gazeLeftThreshold
            p[GAZE_RIGHT_THRESHOLD] = s.gazeRightThreshold
            p[GAZE_UP_THRESHOLD] = s.gazeUpThreshold
            p[GAZE_DOWN_THRESHOLD] = s.gazeDownThreshold
            p[FAST_BLINK_COUNT] = s.fastBlinkCountThreshold
            p[FAST_BLINK_WINDOW_MS] = s.fastBlinkWindowMs
            p[GLOBAL_COOLDOWN_MS] = s.globalCooldownMs
            p[DEBOUNCE_MS] = s.debounceMs
            p[IS_VIBRATION_ENABLED] = s.isVibrationEnabled
            p[IS_VOICE_FEEDBACK_ENABLED] = s.isVoiceFeedbackEnabled
            p[IS_OVERLAY_ENABLED] = s.isOverlayEnabled
            p[IS_LOGGING_ENABLED] = s.isLoggingEnabled
            p[IS_FACE_POINTER_ENABLED] = s.isFacePointerEnabled
            p[POINTER_SPEED] = s.pointerSpeed
            p[POINTER_SENSITIVITY] = s.pointerSensitivity
        }
    }
}
