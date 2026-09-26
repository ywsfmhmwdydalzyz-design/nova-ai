package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class VoiceState(
    val isListening: Boolean = false,
    val text: String = "",
    val partialText: String = "",
    val soundLevel: Float = 0f,
    val errorMessage: String? = null
)

class VoiceInputManager(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _voiceState = MutableStateFlow(VoiceState())
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(
        language: String = Locale.getDefault().toLanguageTag(),
        onPartialResult: ((String) -> Unit)? = null,
        onFinalResult: ((String) -> Unit)? = null
    ) {
        mainHandler.post {
            try {
                stopListening()

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _voiceState.value = _voiceState.value.copy(
                                isListening = true,
                                errorMessage = null,
                                soundLevel = 0f
                            )
                        }

                        override fun onBeginningOfSpeech() {
                            _voiceState.value = _voiceState.value.copy(isListening = true)
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            // rmsdB usually varies from -2.0 to 10.0+
                            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                            _voiceState.value = _voiceState.value.copy(soundLevel = normalized)
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _voiceState.value = _voiceState.value.copy(isListening = false, soundLevel = 0f)
                        }

                        override fun onError(error: Int) {
                            val message = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "خطأ في تسجيل الصوت من الميكروفون"
                                SpeechRecognizer.ERROR_CLIENT -> "خطأ داخلي في خدمة الصوت"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "يتطلب الإملاء الصوتي إذن الميكروفون"
                                SpeechRecognizer.ERROR_NETWORK -> "خطأ في الاتصال بالشبكة لخدمة الصوت"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "انتهت مهلة الاتصال بالشبكة"
                                SpeechRecognizer.ERROR_NO_MATCH -> "لم يتم التعرف على الصوت، يرجى إعادة المحاولة"
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "خدمة التعرف على الصوت مشغولة حالياً"
                                SpeechRecognizer.ERROR_SERVER -> "خطأ في خادم التعرف على الصوت"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "لم يتم التقاط أي صوت، يرجى التحدث بوضوح"
                                else -> "تعذر معالجة الصوت، يرجى المحاولة مجدداً"
                            }
                            _voiceState.value = _voiceState.value.copy(
                                isListening = false,
                                soundLevel = 0f,
                                errorMessage = message
                            )
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val recognized = matches?.firstOrNull().orEmpty()
                            _voiceState.value = _voiceState.value.copy(
                                isListening = false,
                                text = recognized,
                                partialText = recognized,
                                soundLevel = 0f
                            )
                            if (recognized.isNotBlank()) {
                                onFinalResult?.invoke(recognized)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partial = matches?.firstOrNull().orEmpty()
                            if (partial.isNotBlank()) {
                                _voiceState.value = _voiceState.value.copy(partialText = partial)
                                onPartialResult?.invoke(partial)
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                _voiceState.value = _voiceState.value.copy(
                    isListening = false,
                    errorMessage = "فشل بدء الميكروفون: ${e.message}"
                )
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Exception) {}
            _voiceState.value = _voiceState.value.copy(isListening = false, soundLevel = 0f)
        }
    }

    fun clearError() {
        _voiceState.value = _voiceState.value.copy(errorMessage = null)
    }

    fun resetState() {
        _voiceState.value = VoiceState()
    }

    companion object {
        fun createRecognizerIntent(context: Context, promptText: String = "تحدث الآن لتسجيل استفسارك..."): Intent {
            return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PROMPT, promptText)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
        }
    }
}
