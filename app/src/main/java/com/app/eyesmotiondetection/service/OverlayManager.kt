package com.app.eyesmotiondetection.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverlayManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var view: TextView? = null

    fun show() {
        handler.post {
            // Checked on the main thread: settings emissions can call show() repeatedly
            if (view != null) return@post
            runCatching {
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT
                ).apply { gravity = Gravity.TOP or Gravity.END; x = 20; y = 120 }

                val tv = TextView(context).apply {
                    text = "👁 ON"
                    textSize = 11f
                    setBackgroundColor(0xCC1A1A2E.toInt())
                    setTextColor(0xFF00E5FF.toInt())
                    setPadding(12, 6, 12, 6)
                }
                wm.addView(tv, params)
                view = tv
            }.onFailure { android.util.Log.e("OverlayManager", "show failed", it) }
        }
    }

    fun hide() {
        handler.post {
            runCatching { view?.let { wm.removeView(it) } }
            view = null
        }
    }

    fun showGestureIndicator(label: String) {
        handler.post {
            view?.text = "👁 $label"
            handler.postDelayed({ view?.text = "👁 ON" }, 1500)
        }
    }
}
