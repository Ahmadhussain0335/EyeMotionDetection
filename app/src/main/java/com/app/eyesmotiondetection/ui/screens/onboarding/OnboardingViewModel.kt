package com.app.eyesmotiondetection.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.eyesmotiondetection.data.datastore.AppPreferences
import com.app.eyesmotiondetection.data.repository.GestureMappingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val mappingRepository: GestureMappingRepository
) : ViewModel() {

    fun completeOnboarding() {
        viewModelScope.launch {
            mappingRepository.insertDefaultMappings()
            appPreferences.setOnboarded(true)
        }
    }
}
