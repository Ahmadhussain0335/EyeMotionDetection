package com.app.eyesmotiondetection.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.eyesmotiondetection.data.datastore.AppPreferences
import com.app.eyesmotiondetection.domain.model.DetectionSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {

    val settings = appPreferences.detectionSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetectionSettings())

    val isDarkMode = appPreferences.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setDarkMode(value: Boolean) = viewModelScope.launch { appPreferences.setDarkMode(value) }

    fun setVibration(value: Boolean) = viewModelScope.launch {
        appPreferences.updateDetectionSettings(settings.value.copy(isVibrationEnabled = value))
    }

    fun setVoiceFeedback(value: Boolean) = viewModelScope.launch {
        appPreferences.updateDetectionSettings(settings.value.copy(isVoiceFeedbackEnabled = value))
    }

    fun setOverlay(value: Boolean) = viewModelScope.launch {
        appPreferences.updateDetectionSettings(settings.value.copy(isOverlayEnabled = value))
    }

    fun setLogging(value: Boolean) = viewModelScope.launch {
        appPreferences.updateDetectionSettings(settings.value.copy(isLoggingEnabled = value))
    }

    fun updateSettings(s: DetectionSettings) = viewModelScope.launch {
        appPreferences.updateDetectionSettings(s)
    }

    fun setFacePointerEnabled(value: Boolean) = viewModelScope.launch {
        appPreferences.updateDetectionSettings(settings.value.copy(isFacePointerEnabled = value))
    }

    fun setPointerSpeed(value: Float) = viewModelScope.launch {
        appPreferences.updateDetectionSettings(settings.value.copy(pointerSpeed = value))
    }

    fun setPointerSensitivity(value: Float) = viewModelScope.launch {
        appPreferences.updateDetectionSettings(settings.value.copy(pointerSensitivity = value))
    }
}
