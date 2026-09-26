package com.example.util

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class TextToSpeechHelper(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var onStartCallback: (() -> Unit)? = null
    private var onDoneCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            // Configure Arabic language with natural human voice parameters
            val arLocale = Locale.forLanguageTag("ar")
            val arResult = tts?.setLanguage(arLocale)
            if (arResult == TextToSpeech.LANG_MISSING_DATA || arResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }

            // Select best high-quality Arabic voice if available
            try {
                val voices = tts?.voices
                if (voices != null) {
                    val bestVoice = voices.firstOrNull { voice ->
                        voice.locale.language == "ar" && !voice.isNetworkConnectionRequired
                    } ?: voices.firstOrNull { voice ->
                        voice.locale.language == "ar"
                    }
                    if (bestVoice != null) {
                        tts?.voice = bestVoice
                    }
                }
            } catch (_: Exception) {}

            // Set natural human conversational pitch & speed
            tts?.setPitch(1.02f)
            tts?.setSpeechRate(0.98f)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    onStartCallback?.invoke()
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    onDoneCallback?.invoke()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    onDoneCallback?.invoke()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    onDoneCallback?.invoke()
                }
            })
        }
    }

    fun speak(
        rawText: String,
        onStart: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null
    ) {
        if (!isInitialized || tts == null) return

        onStartCallback = onStart
        onDoneCallback = onDone

        // Clean markdown, symbols, and formatting for natural speech
        val cleaned = rawText
            .replace(Regex("[*#`_~>]"), "")
            .replace(Regex("```[a-zA-Z]*"), "")
            .replace(Regex("https?://\\S+"), "رابط مرفق")
            .trim()

        if (cleaned.isEmpty()) return

        // Auto switch language based on text script
        val isArabic = cleaned.any { it in '\u0600'..'\u06FF' }
        if (isArabic) {
            tts?.setLanguage(Locale.forLanguageTag("ar"))
        } else {
            tts?.setLanguage(Locale.ENGLISH)
        }

        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle()
        tts?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        _isSpeaking.value = false
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (_: Exception) {}
        _isSpeaking.value = false
    }
}
