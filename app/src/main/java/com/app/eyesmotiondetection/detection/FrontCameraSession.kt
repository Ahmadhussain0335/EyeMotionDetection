package com.app.eyesmotiondetection.detection

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors

/**
 * Binds front-camera analysis (and optionally a preview) to a lifecycle.
 *
 * ProcessCameraProvider is process-wide and shared by the foreground service and the UI, so this
 * only ever unbinds its own use cases. CameraX hands the camera to the most recently started
 * lifecycle and gives it back to the previous one when that lifecycle stops.
 */
class FrontCameraSession(
    private val context: Context,
    private val analyzer: ImageAnalysis.Analyzer
) {
    private val executor = Executors.newSingleThreadExecutor()
    private var provider: ProcessCameraProvider? = null
    private var useCases: Array<UseCase> = emptyArray()

    fun bind(owner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider? = null) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            runCatching {
                val p = future.get().also { provider = it }
                if (useCases.isNotEmpty()) p.unbind(*useCases)
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(executor, analyzer) }
                val preview = surfaceProvider?.let { sp ->
                    Preview.Builder().build().also { it.surfaceProvider = sp }
                }
                useCases = listOfNotNull(preview, analysis).toTypedArray()
                p.bindToLifecycle(owner, CameraSelector.DEFAULT_FRONT_CAMERA, *useCases)
            }.onFailure { Log.e(TAG, "Camera bind failed", it) }
        }, ContextCompat.getMainExecutor(context))
    }

    @MainThread
    fun unbind() {
        if (useCases.isNotEmpty()) provider?.unbind(*useCases)
        useCases = emptyArray()
    }

    @MainThread
    fun release() {
        unbind()
        executor.shutdown()
    }

    private companion object {
        const val TAG = "FrontCameraSession"
    }
}
