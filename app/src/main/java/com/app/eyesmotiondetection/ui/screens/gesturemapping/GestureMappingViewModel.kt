package com.app.eyesmotiondetection.ui.screens.gesturemapping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.eyesmotiondetection.data.repository.GestureMappingRepository
import com.app.eyesmotiondetection.domain.model.AppAction
import com.app.eyesmotiondetection.domain.model.EyeGesture
import com.app.eyesmotiondetection.domain.model.GestureMapping
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GestureMappingViewModel @Inject constructor(
    private val repository: GestureMappingRepository
) : ViewModel() {

    val mappings = repository.getAllMappings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateMapping(gesture: EyeGesture, action: AppAction) {
        viewModelScope.launch {
            val existing = mappings.value.find { it.gesture == gesture }
            if (existing != null) {
                repository.saveMapping(existing.copy(action = action))
            } else {
                repository.saveMapping(GestureMapping(gesture = gesture, action = action))
            }
        }
    }

    fun toggleMapping(gesture: EyeGesture, enabled: Boolean) {
        viewModelScope.launch {
            val existing = mappings.value.find { it.gesture == gesture } ?: return@launch
            repository.saveMapping(existing.copy(isEnabled = enabled))
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repository.resetToDefaults()
        }
    }
}
