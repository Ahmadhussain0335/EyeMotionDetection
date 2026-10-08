package com.app.eyesmotiondetection.ui.screens.calibration

import android.content.Context
import android.os.SystemClock
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.eyesmotiondetection.data.datastore.AppPreferences
import com.app.eyesmotiondetection.detection.CalibrationCalculator
import com.app.eyesmotiondetection.detection.CalibrationResult
import com.app.eyesmotiondetection.detection.CalibrationSamples
import com.app.eyesmotiondetection.detection.FaceAnalyzer
import com.app.eyesmotiondetection.detection.FrontCameraSession
import com.app.eyesmotiondetection.detection.model.FaceData
import com.app.eyesmotiondetection.domain.model.DetectionSettings
import com.app.eyesmotiondetection.util.vibrateOnce
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

enum class CalibrationStep(val instruction: String, val settleMs: Long, val measureMs: Long) {
    IDLE("Hold the phone as you normally use it, then press Start.", 0, 0),
    EYES_OPEN("Look at the screen with your eyes open and keep your head still.", 1500, 3000),
    EYES_CLOSED("Close your eyes until you feel a vibration.", 2500, 2500),
    TURN_LEFT("Turn your head to the left as far as is comfortable and hold.", 2000, 2500),
    TURN_RIGHT("Turn your head to the right as far as is comfortable and hold.", 2000, 2500),
    TILT_UP("Tilt your head up as far as is comfortable and hold.", 2000, 2500),
    TILT_DOWN("Tilt your head down as far as is comfortable and hold.", 2000, 2500),
    DONE("Calibration saved.", 0, 0);

    companion object {
        val measured = entries.filter { it.measureMs > 0 }
    }
}

data class CalibrationState(
    val step: CalibrationStep = CalibrationStep.IDLE,
    val isSettling: Boolean = false,
    val stepProgress: Float = 0f,
    val faceDetected: Boolean = false,
    val error: String? = null,
    val before: DetectionSettings? = null,
    val after: DetectionSettings? = null
) {
    val isRunning get() = step != CalibrationStep.IDLE && step != CalibrationStep.DONE && error == null
    val stepNumber get() = CalibrationStep.measured.indexOf(step) + 1
}

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(CalibrationState())
    val state: StateFlow<CalibrationState> = _state.asStateFlow()

    val settings = appPreferences.detectionSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetectionSettings())

    // Own analyzer, not the app-wide one: calibration frames must not reach EyeGestureDetector
    // and trigger mapped actions while the user is closing eyes or turning their head.
    private val faceFrames = MutableSharedFlow<FaceData>(extraBufferCapacity = 64)
    private val faceAnalyzer = FaceAnalyzer { faceFrames.tryEmit(it) }
    private val cameraSession = FrontCameraSession(context, faceAnalyzer)
    private var calibrationJob: Job? = null

    init {
        viewModelScope.launch {
            faceFrames.collect { fd ->
                if (fd.faceDetected != _state.value.faceDetected) {
                    _state.update { it.copy(faceDetected = fd.faceDetected) }
                }
            }
        }
    }

    fun bindCamera(owner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) =
        cameraSession.bind(owner, surfaceProvider)

    fun startCalibration() {
        calibrationJob?.cancel()
        calibrationJob = viewModelScope.launch { runCalibration() }
    }

    fun cancelCalibration() {
        calibrationJob?.cancel()
        _state.update { CalibrationState(faceDetected = it.faceDetected) }
    }

    private suspend fun runCalibration() {
        _state.update { CalibrationState(faceDetected = it.faceDetected) }
        val samples = mutableMapOf<CalibrationStep, List<FaceData>>()
        for (step in CalibrationStep.measured) {
            samples[step] = measure(step) ?: return
            context.vibrateOnce(150L)
        }
        val before = settings.value
        val input = CalibrationSamples(
            eyesOpen = samples.getValue(CalibrationStep.EYES_OPEN).map { it.avgEyeOpen() },
            eyesClosed = samples.getValue(CalibrationStep.EYES_CLOSED).map { it.avgEyeOpen() },
            neutralYaw = samples.getValue(CalibrationStep.EYES_OPEN).map { it.headEulerAngleY },
            neutralPitch = samples.getValue(CalibrationStep.EYES_OPEN).map { it.headEulerAngleX },
            turnLeftYaw = samples.getValue(CalibrationStep.TURN_LEFT).map { it.headEulerAngleY },
            turnRightYaw = samples.getValue(CalibrationStep.TURN_RIGHT).map { it.headEulerAngleY },
            tiltUpPitch = samples.getValue(CalibrationStep.TILT_UP).map { it.headEulerAngleX },
            tiltDownPitch = samples.getValue(CalibrationStep.TILT_DOWN).map { it.headEulerAngleX }
        )
        when (val result = CalibrationCalculator.compute(before, input)) {
            is CalibrationResult.Success -> {
                appPreferences.updateDetectionSettings(result.settings)
                _state.update {
                    it.copy(step = CalibrationStep.DONE, stepProgress = 1f, before = before, after = result.settings)
                }
            }
            is CalibrationResult.Failure -> _state.update { it.copy(error = result.reason) }
        }
    }

    /** Returns face-detected frames for [step], or null (with an error set) if too few were seen. */
    private suspend fun measure(step: CalibrationStep): List<FaceData>? {
        _state.update { it.copy(step = step, isSettling = true, stepProgress = 0f, error = null) }
        delay(step.settleMs)
        _state.update { it.copy(isSettling = false) }

        val collected = mutableListOf<FaceData>()
        val start = SystemClock.elapsedRealtime()
        withTimeoutOrNull(step.measureMs) {
            faceFrames.collect { fd ->
                if (fd.faceDetected) collected += fd
                val elapsed = SystemClock.elapsedRealtime() - start
                _state.update { it.copy(stepProgress = (elapsed.toFloat() / step.measureMs).coerceAtMost(1f)) }
            }
        }
        if (collected.size < MIN_FRAMES) {
            _state.update {
                it.copy(error = "Face not detected clearly. Make sure your face is visible and well lit.")
            }
            return null
        }
        return collected
    }

    fun updateBlinkThreshold(value: Float) {
        viewModelScope.launch {
            appPreferences.updateDetectionSettings(settings.value.copy(blinkThreshold = value))
        }
    }

    fun updateGazeSensitivity(degrees: Float) {
        viewModelScope.launch {
            val s = settings.value
            appPreferences.updateDetectionSettings(s.copy(
                gazeLeftThreshold = -degrees,
                gazeRightThreshold = degrees,
                gazeUpThreshold = -(degrees * 0.7f),
                gazeDownThreshold = degrees * 0.7f
            ))
        }
    }

    fun updateCooldown(ms: Long) {
        viewModelScope.launch {
            appPreferences.updateDetectionSettings(settings.value.copy(globalCooldownMs = ms))
        }
    }

    override fun onCleared() {
        cameraSession.release()
        faceAnalyzer.close()
        super.onCleared()
    }

    private fun FaceData.avgEyeOpen() = (leftEyeOpenProbability + rightEyeOpenProbability) / 2f

    private companion object {
        const val MIN_FRAMES = 5
    }
}
