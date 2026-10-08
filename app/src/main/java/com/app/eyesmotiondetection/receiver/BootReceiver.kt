package com.app.eyesmotiondetection.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.app.eyesmotiondetection.data.datastore.AppPreferences
import com.app.eyesmotiondetection.service.ResumeTrackingNotification
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Camera foreground services may not start from BOOT_COMPLETED (Android 15+), so instead of
 * starting tracking this posts a notification the user taps to resume it.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var appPreferences: AppPreferences

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (appPreferences.isServiceEnabled.first()) {
                    ResumeTrackingNotification.post(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
