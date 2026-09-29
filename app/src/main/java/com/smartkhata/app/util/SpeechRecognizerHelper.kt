package com.smartkhata.app.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class SpeechRecognizerHelper(
    context: Context,
    private val onReady: () -> Unit = {},
    private val onRmsChanged: (Float) -> Unit = {},
    private val onPartialResult: (String) -> Unit = {},
    private val onFinalResult: (String) -> Unit = {},
    private val onError: (String) -> Unit = {}
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    var isListening = false
        private set

    fun isAvailable(): Boolean {
        return try {
            SpeechRecognizer.isRecognitionAvailable(appContext)
        } catch (_: Exception) {
            false
        }
    }

    fun startListening(languageCode: String = "hi-IN") {
        mainHandler.post {
            stopListening()

            try {
                if (!isAvailable()) {
                    onError("Speech recognition service is not available on this device.")
                    return@post
                }

                val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
                speechRecognizer = recognizer

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        mainHandler.post {
                            isListening = true
                            onReady()
                        }
                    }

                    override fun onBeginningOfSpeech() {
                        mainHandler.post {
                            isListening = true
                        }
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        mainHandler.post {
                            onRmsChanged(rmsdB)
                        }
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        mainHandler.post {
                            isListening = false
                        }
                    }

                    override fun onError(error: Int) {
                        mainHandler.post {
                            isListening = false
                            val message = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Please speak closer to microphone."
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard. Tap mic to try again."
                                SpeechRecognizer.ERROR_NETWORK -> "Network issue encountered."
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition connection timed out."
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check microphone."
                                SpeechRecognizer.ERROR_CLIENT -> "Speech recognizer client error."
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy. Please wait a moment."
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                                else -> "Speech service error (Code $error)"
                            }
                            Log.w("SpeechRecognizerHelper", "onError: $error - $message")
                            onError(message)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        mainHandler.post {
                            isListening = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val best = matches?.firstOrNull() ?: ""
                            if (best.isNotBlank()) {
                                onFinalResult(best)
                            } else {
                                onError("No words recognized. Try again.")
                            }
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        mainHandler.post {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val best = matches?.firstOrNull() ?: ""
                            if (best.isNotBlank()) {
                                onPartialResult(best)
                            }
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
                }

                recognizer.startListening(intent)
                isListening = true
            } catch (e: Exception) {
                isListening = false
                Log.e("SpeechRecognizerHelper", "Failed to start listening", e)
                onError("Could not initialize voice recognition: ${e.message}")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.w("SpeechRecognizerHelper", "Error destroying speech recognizer: ${e.message}")
            } finally {
                speechRecognizer = null
                isListening = false
            }
        }
    }
}
