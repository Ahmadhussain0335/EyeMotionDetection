package com.app.eyesmotiondetection.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.eyesmotiondetection.data.datastore.AppPreferences
import com.app.eyesmotiondetection.data.repository.GestureLogRepository
import com.app.eyesmotiondetection.data.repository.GestureMappingRepository
import com.app.eyesmotiondetection.service.EyeTrackingForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val isServiceEnabled: Boolean = false,
    val activeMappingsCount: Int = 0,
    val todayGesturesCount: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
    private val mappingRepository: GestureMappingRepository,
    private val logRepository: GestureLogRepository
) : ViewModel() {

    val state: StateFlow<HomeState> = combine(
        appPreferences.isServiceEnabled,
        mappingRepository.getAllMappings(),
        logRepository.observeTodayCount()
    ) { enabled, mappings, todayCount ->
        HomeState(
            isServiceEnabled = enabled,
            activeMappingsCount = mappings.count { it.isEnabled && it.gesture.isSupported },
            todayGesturesCount = todayCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())

    private val _startError = MutableStateFlow<String?>(null)
    val startError: StateFlow<String?> = _startError.asStateFlow()

    fun toggleService() {
        viewModelScope.launch {
            val current = appPreferences.isServiceEnabled.first()
            if (current) {
                EyeTrackingForegroundService.stop(context)
                appPreferences.setServiceEnabled(false)
            } else if (EyeTrackingForegroundService.start(context)) {
                _startError.value = null
                appPreferences.setServiceEnabled(true)
            } else {
                _startError.value = "Camera permission is required. Grant it in system Settings → Apps."
            }
        }
    }
}
