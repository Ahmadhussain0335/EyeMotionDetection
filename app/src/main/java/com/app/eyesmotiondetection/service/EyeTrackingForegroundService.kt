package com.app.eyesmotiondetection.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.app.eyesmotiondetection.MainActivity
import com.app.eyesmotiondetection.R
import com.app.eyesmotiondetection.data.datastore.AppPreferences
import com.app.eyesmotiondetection.detection.FaceAnalyzer
import com.app.eyesmotiondetection.detection.FrontCameraSession
import com.app.eyesmotiondetection.detection.GestureAnalyzer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class EyeTrackingForegroundService : LifecycleService() {

    @Inject lateinit var faceAnalyzer: FaceAnalyzer
    @Inject lateinit var gestureAnalyzer: GestureAnalyzer
    @Inject lateinit var overlayManager: OverlayManager
    @Inject lateinit var facePointerManager: FacePointerManager
    @Inject lateinit var appPreferences: AppPreferences

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var cameraSession: FrontCameraSession
    private var isRunning = false

    companion object {
        const val CHANNEL_ID = "eye_tracking_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.app.eyesmotiondetection.STOP"
        private const val TAG = "EyeService"

        /**
         * Must be called while the app is in the foreground. Returns false without starting when
         * camera permission is missing: a camera-type FGS throws SecurityException in that case.
         */
        fun start(context: Context): Boolean {
            val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            if (hasCamera) {
                ContextCompat.startForegroundService(
                    context, Intent(context, EyeTrackingForegroundService::class.java)
                )
            }
            return hasCamera
        }

        fun stop(context: Context) = context.startService(
            Intent(context, EyeTrackingForegroundService::class.java).apply { action = ACTION_STOP }
        )
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        cameraSession = FrontCameraSession(this, faceAnalyzer)
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (e: RuntimeException) {
            // A STICKY restart from the background is not allowed to use the camera
            // (ForegroundServiceStartNotAllowedException / SecurityException). Ask the user instead.
            Log.w(TAG, "Cannot start camera FGS from background", e)
            ResumeTrackingNotification.post(this)
            stopSelf()
            return
        }
        ResumeTrackingNotification.cancel(this)
        isRunning = true
        gestureAnalyzer.start()
        facePointerManager.start()
        scope.launch {
            appPreferences.detectionSettings.collect { s ->
                gestureAnalyzer.updateSettings(s)
                if (s.isOverlayEnabled) overlayManager.show() else overlayManager.hide()
            }
        }
        scope.launch {
            gestureAnalyzer.actionFlow.collect { event ->
                GestureEventBus.tryEmit(event)
                overlayManager.showGestureIndicator(event.gesture.displayName)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP || !isRunning) { stopSelf(); return START_NOT_STICKY }
        cameraSession.bind(this)
        return START_STICKY
    }

    override fun onDestroy() {
        if (isRunning) {
            cameraSession.release()
            gestureAnalyzer.stop()
            facePointerManager.stop()
            overlayManager.hide()
        }
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "Eye Tracking Service",
            NotificationManager.IMPORTANCE_LOW).apply {
            description = "Eye gesture control running in background"
        }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification {
        val stopPi = PendingIntent.getService(this, 0,
            Intent(this, EyeTrackingForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE)
        val openPi = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Eye Tracking Active")
            .setContentText("Controlling your phone with eye gestures")
            .setSmallIcon(R.drawable.ic_eye_notification)
            .setContentIntent(openPi)
            .addAction(0, "Stop", stopPi)
            .setOngoing(true).build()
    }
}
