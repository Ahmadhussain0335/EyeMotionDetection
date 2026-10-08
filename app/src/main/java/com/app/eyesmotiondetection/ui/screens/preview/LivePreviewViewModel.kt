package com.app.eyesmotiondetection.ui.screens.preview

import android.content.Context
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.eyesmotiondetection.detection.EyeGestureDetector
import com.app.eyesmotiondetection.detection.FaceAnalyzer
import com.app.eyesmotiondetection.detection.FrontCameraSession
import com.app.eyesmotiondetection.detection.model.FaceData
import com.app.eyesmotiondetection.detection.model.GestureResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LivePreviewViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val gestureDetector: EyeGestureDetector,
    faceAnalyzer: FaceAnalyzer
) : ViewModel() {

    val latestGesture: StateFlow<GestureResult?> = gestureDetector.gestureFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val faceData: StateFlow<FaceData?> = gestureDetector.faceDataFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _recentGestures = MutableStateFlow<List<GestureResult>>(emptyList())
    val recentGestures: StateFlow<List<GestureResult>> = _recentGestures.asStateFlow()

    private val cameraSession = FrontCameraSession(context, faceAnalyzer)

    init {
        viewModelScope.launch {
            gestureDetector.gestureFlow.collect { result ->
                _recentGestures.value = (_recentGestures.value + result).takeLast(20)
            }
        }
    }

    fun startCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) =
        cameraSession.bind(lifecycleOwner, surfaceProvider)

    fun clearHistory() {
        _recentGestures.value = emptyList()
    }

    override fun onCleared() {
        cameraSession.release()
        super.onCleared()
    }
}
