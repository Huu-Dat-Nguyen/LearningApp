package com.learningapp.ui.speak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learningapp.data.repo.PracticeSentence
import com.learningapp.data.repo.SpeakStore
import com.learningapp.domain.speech.PronunciationScorer
import com.learningapp.domain.speech.SpeechError
import com.learningapp.domain.speech.SpeechEvent
import com.learningapp.domain.speech.SpeechInput
import com.learningapp.domain.speech.SpeechScore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SpeakPhase { Idle, Listening, Result, Error }

data class SpeakUiState(
    val loading: Boolean = true,
    /** False when the phone has no speech recognition: listen-only practice. */
    val recognitionAvailable: Boolean = true,
    val targetLang: String = "",
    val sentence: PracticeSentence? = null,
    val position: Int = 0,
    val total: Int = 0,
    val phase: SpeakPhase = SpeakPhase.Idle,
    val level: Float = 0f,
    val partial: String = "",
    val score: SpeechScore? = null,
    val xpGained: Int = 0,
    val error: SpeechError? = null,
)

/** Speech recognizer language for a target language code. */
fun recognizerTag(lang: String): String = when (lang) {
    "es" -> "es-ES"
    "fr" -> "fr-FR"
    "en" -> "en-US"
    "ja" -> "ja-JP"
    "vi" -> "vi-VN"
    else -> lang
}

@HiltViewModel
class SpeakViewModel @Inject constructor(
    private val store: SpeakStore,
    private val speech: SpeechInput,
) : ViewModel() {

    private var sentences: List<PracticeSentence> = emptyList()
    private var index = 0

    private val _state = MutableStateFlow(SpeakUiState())
    val state: StateFlow<SpeakUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            sentences = store.sentences()
            _state.value = SpeakUiState(
                loading = false,
                recognitionAvailable = speech.isAvailable,
                targetLang = store.targetLang(),
            ).withSentence()
        }
    }

    /** Mic button: start listening, or stop and wait for the result. */
    fun micTapped() {
        val s = _state.value
        if (s.sentence == null) return
        if (s.phase == SpeakPhase.Listening) {
            speech.stop()
            return
        }
        _state.update { it.copy(phase = SpeakPhase.Listening, partial = "", level = 0f, score = null, error = null, xpGained = 0) }
        speech.start(recognizerTag(s.targetLang), ::onEvent)
    }

    /** The user said no to the microphone permission. */
    fun permissionDenied() = _state.update { it.copy(phase = SpeakPhase.Error, error = SpeechError.Permission) }

    fun tryAgain() {
        speech.cancel()
        _state.update { it.copy(phase = SpeakPhase.Idle, partial = "", score = null, error = null, xpGained = 0, level = 0f) }
    }

    fun next() {
        speech.cancel()
        if (sentences.isNotEmpty()) index = (index + 1) % sentences.size
        _state.update { it.withSentence().copy(phase = SpeakPhase.Idle, partial = "", score = null, error = null, xpGained = 0, level = 0f) }
    }

    private fun onEvent(event: SpeechEvent) {
        when (event) {
            SpeechEvent.Ready -> Unit
            is SpeechEvent.Level -> _state.update { if (it.phase == SpeakPhase.Listening) it.copy(level = event.level) else it }
            is SpeechEvent.Partial -> _state.update { it.copy(partial = event.text) }
            is SpeechEvent.Results -> {
                val sentence = _state.value.sentence ?: return
                val score = PronunciationScorer.best(sentence.text, event.guesses, _state.value.targetLang)
                _state.update { it.copy(phase = SpeakPhase.Result, score = score, level = 0f) }
                viewModelScope.launch {
                    val xp = store.recordAttempt(sentence.text, score.score)
                    _state.update { if (it.score == score) it.copy(xpGained = xp) else it }
                }
            }
            is SpeechEvent.Error -> _state.update { it.copy(phase = SpeakPhase.Error, error = event.error, level = 0f) }
        }
    }

    private fun SpeakUiState.withSentence() = copy(
        sentence = sentences.getOrNull(index),
        position = if (sentences.isEmpty()) 0 else index + 1,
        total = sentences.size,
    )

    override fun onCleared() {
        speech.release()
    }
}
