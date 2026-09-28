package com.smartkhata.app.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class SpeechRecognizerHelper(
    private val context: Context,
    private val onReady: () -> Unit = {},
    private val onRmsChanged: (Float) -> Unit = {},
    private val onPartialResult: (String) -> Unit = {},
    private val onFinalResult: (String) -> Unit = {},
    private val onError: (String) -> Unit = {}
) {
    private var speechRecognizer: SpeechRecognizer? = null
    var isListening = false
        private set

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(languageCode: String = "hi-IN") {
        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        onReady()
                    }

                    override fun onBeginningOfSpeech() {
                        isListening = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        onRmsChanged(rmsdB)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListening = false
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        val message = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please speak clearly into mic."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Please try speaking again."
                            SpeechRecognizer.ERROR_NETWORK -> "Network issue encountered."
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition connection timed out."
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check microphone."
                            SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy. Please wait a moment."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                            else -> "Voice recognition issue (Code $error)"
                        }
                        Log.w("SpeechRecognizerHelper", "onError: $error - $message")
                        onError(message)
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val best = matches?.firstOrNull() ?: ""
                        if (best.isNotBlank()) {
                            onFinalResult(best)
                        } else {
                            onError("No words detected. Try speaking again.")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val best = matches?.firstOrNull() ?: ""
                        if (best.isNotBlank()) {
                            onPartialResult(best)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            speechRecognizer?.startListening(intent)
            isListening = true
        } catch (e: Exception) {
            isListening = false
            Log.e("SpeechRecognizerHelper", "Failed to start listening", e)
            onError("Could not initialize voice recognition: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        isListening = false
    }
}
