package com.app.eyesmotiondetection.ui.screens.splash

import androidx.lifecycle.ViewModel
import com.app.eyesmotiondetection.data.datastore.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    appPreferences: AppPreferences
) : ViewModel() {
    val isOnboarded = appPreferences.isOnboarded
}
