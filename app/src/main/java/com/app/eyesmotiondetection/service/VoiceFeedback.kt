package com.app.eyesmotiondetection.service

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.annotation.MainThread
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Speaks dispatched action names. The TTS engine is only held while the setting is on. */
@Singleton
class VoiceFeedback @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    @Volatile private var ready = false

    @MainThread
    fun setEnabled(enabled: Boolean) {
        if (enabled && tts == null) {
            tts = TextToSpeech(context) { status -> ready = status == TextToSpeech.SUCCESS }
        } else if (!enabled) {
            shutdown()
        }
    }

    /** Dropped silently until the engine has finished initialising. */
    @MainThread
    fun speak(text: String) {
        if (ready) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    @MainThread
    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }

    private companion object {
        const val UTTERANCE_ID = "eye_action"
    }
}
