package com.pacepilot.app.service

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceNavigationManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false
    private var lastSpokenMessage = ""
    private var lastSpokenTime = 0L

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val idLocale = Locale("id", "ID")
            val result = tts?.setLanguage(idLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to English if Indonesian language data is not present
                tts?.language = Locale.US
            }
            tts?.setSpeechRate(1.05f)
            tts?.setPitch(1.0f)
            isInitialized = true
        }
    }

    /**
     * Membacakan instruksi navigasi suara atau peringatan pacing
     */
    fun speak(text: String, isUrgent: Boolean = false) {
        if (!isInitialized || text.isBlank()) return

        val now = System.currentTimeMillis()
        // Mencegah spam kalimat yang sama berulang-ulang dalam 10 detik
        if (!isUrgent && text == lastSpokenMessage && (now - lastSpokenTime < 10000L)) {
            return
        }

        lastSpokenMessage = text
        lastSpokenTime = now

        val queueMode = if (isUrgent) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.speak(text, queueMode, null, "PacePilotVoice_${System.currentTimeMillis()}")
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            // Ignored on shutdown
        }
    }
}
