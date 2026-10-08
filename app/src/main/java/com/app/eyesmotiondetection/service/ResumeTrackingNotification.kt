package com.app.eyesmotiondetection.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.app.eyesmotiondetection.MainActivity
import com.app.eyesmotiondetection.R

/**
 * Asks the user to resume eye tracking when the camera service cannot start on its own
 * (after boot, or a STICKY restart while the app is in the background). Android 15+ blocks
 * camera foreground services started from BOOT_COMPLETED, so the restart must come from the UI.
 */
object ResumeTrackingNotification {
    private const val CHANNEL_ID = "eye_tracking_resume"
    const val NOTIFICATION_ID = 1002

    fun post(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Resume eye tracking", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Shown when eye tracking stopped and needs a tap to restart" }
        )

        // Targets a non-exported alias so other apps cannot trigger a camera start via MainActivity
        val intent = Intent()
            .setComponent(ComponentName(context, MainActivity.RESUME_TRACKING_ALIAS))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pi = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_eye_notification)
            .setContentTitle("Eye tracking is paused")
            .setContentText("Tap to resume hands-free control")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) = NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
}
