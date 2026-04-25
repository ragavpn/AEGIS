package com.aegis.app.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class VoiceOutputHandler(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var speed = 1.0f

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isReady = true
                tts?.setSpeechRate(speed)
            }
        }
    }

    fun setSpeed(newSpeed: Float) {
        speed = newSpeed
        tts?.setSpeechRate(speed)
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!isReady) return
        
        if (onDone != null) {
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    onDone()
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onDone()
                }
            })
        } else {
            tts?.setOnUtteranceProgressListener(null)
        }

        val utteranceId = "utterance_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
