package com.learningapp.domain.speech

/** Why listening stopped without a result. */
enum class SpeechError { NoMatch, NoSpeech, Network, Busy, Permission, NotAvailable, Other }

sealed interface SpeechEvent {
    /** The microphone is open. */
    data object Ready : SpeechEvent
    /** Input level, roughly 0..1, for the pulsing mic. */
    data class Level(val level: Float) : SpeechEvent
    data class Partial(val text: String) : SpeechEvent
    /** Final guesses, best first. */
    data class Results(val guesses: List<String>) : SpeechEvent
    data class Error(val error: SpeechError) : SpeechEvent
}

/** Speech-to-text. The Android version wraps SpeechRecognizer; tests use a fake. */
interface SpeechInput {
    /** False when the phone has no speech recognition service. */
    val isAvailable: Boolean
    /** Starts listening in [langTag] (e.g. "es-ES"); events arrive on the main thread. */
    fun start(langTag: String, onEvent: (SpeechEvent) -> Unit)
    /** Stops listening and waits for the final result. */
    fun stop()
    fun cancel()
    fun release()
}
