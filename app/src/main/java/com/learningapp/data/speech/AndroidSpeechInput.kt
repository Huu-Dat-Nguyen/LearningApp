package com.learningapp.data.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.learningapp.domain.speech.SpeechError
import com.learningapp.domain.speech.SpeechEvent
import com.learningapp.domain.speech.SpeechInput
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** [SpeechInput] on Android's SpeechRecognizer. Must be used from the main thread. */
class AndroidSpeechInput @Inject constructor(
    @ApplicationContext private val context: Context,
) : SpeechInput {

    private var recognizer: SpeechRecognizer? = null

    override val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    override fun start(langTag: String, onEvent: (SpeechEvent) -> Unit) {
        if (!isAvailable) {
            onEvent(SpeechEvent.Error(SpeechError.NotAvailable))
            return
        }
        val r = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
        r.setRecognitionListener(Listener(onEvent))
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            // Use the on-device model when one is installed; free and works offline.
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        r.startListening(intent)
    }

    override fun stop() {
        recognizer?.stopListening()
    }

    override fun cancel() {
        recognizer?.cancel()
    }

    override fun release() {
        recognizer?.destroy()
        recognizer = null
    }

    private class Listener(private val onEvent: (SpeechEvent) -> Unit) : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = onEvent(SpeechEvent.Ready)
        override fun onBeginningOfSpeech() = Unit
        // rmsdB is roughly -2..10 on most phones.
        override fun onRmsChanged(rmsdB: Float) = onEvent(SpeechEvent.Level(((rmsdB + 2f) / 12f).coerceIn(0f, 1f)))
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onPartialResults(partialResults: Bundle?) {
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
                onEvent(SpeechEvent.Partial(it))
            }
        }

        override fun onResults(results: Bundle?) {
            val guesses = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            onEvent(if (guesses.isEmpty()) SpeechEvent.Error(SpeechError.NoMatch) else SpeechEvent.Results(guesses))
        }

        override fun onError(error: Int) = onEvent(
            SpeechEvent.Error(
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> SpeechError.NoMatch
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechError.NoSpeech
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                    SpeechRecognizer.ERROR_SERVER, SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> SpeechError.Network
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY, SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> SpeechError.Busy
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechError.Permission
                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> SpeechError.NotAvailable
                    else -> SpeechError.Other
                }
            )
        )
    }
}
