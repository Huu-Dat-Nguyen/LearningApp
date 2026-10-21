package com.learningapp.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learningapp.data.repo.OnboardingChoices
import com.learningapp.data.repo.OnboardingStore
import com.learningapp.data.repo.Placement
import com.learningapp.data.repo.PlacementQuestion
import com.learningapp.data.repo.UserPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingStep { Welcome, Languages, Placement, Goal }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.Welcome,
    /** target -> main languages with content. */
    val pairs: Map<String, Set<String>> = emptyMap(),
    val mainLang: String = "en",
    val targetLang: String = "es",
    val questions: List<PlacementQuestion> = emptyList(),
    val questionIndex: Int = 0,
    val selected: Int? = null,
    val correct: Int = 0,
    val loadingQuestions: Boolean = false,
    val startUnitIndex: Int = 0,
    val goalMinutes: Int = 10,
    val reminderEnabled: Boolean = true,
    val reminderMinuteOfDay: Int = UserPrefs.DEFAULT_REMINDER,
    val finishing: Boolean = false,
    val finished: Boolean = false,
) {
    val pairAvailable: Boolean get() = mainLang != targetLang && mainLang in pairs[targetLang].orEmpty()
    val question: PlacementQuestion? get() = questions.getOrNull(questionIndex)
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val repo: OnboardingStore) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState(pairs = repo.pairs()))
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun start() = _state.update { it.copy(step = OnboardingStep.Languages) }

    fun pickMain(code: String) = _state.update { it.copy(mainLang = code) }

    fun pickTarget(code: String) = _state.update { it.copy(targetLang = code) }

    /** Languages chosen: load the placement quiz. */
    fun confirmLanguages() {
        val s = _state.value
        if (!s.pairAvailable) return
        _state.update { it.copy(step = OnboardingStep.Placement, loadingQuestions = true, questionIndex = 0, correct = 0, selected = null) }
        viewModelScope.launch {
            val qs = repo.placementQuestions(s.mainLang, s.targetLang)
            _state.update { it.copy(questions = qs, loadingQuestions = false) }
        }
    }

    fun select(index: Int) = _state.update { it.copy(selected = index) }

    /** Records the answer and moves on; after the last question, picks the start unit. */
    fun nextQuestion() {
        val s = _state.value
        val q = s.question ?: return
        val selected = s.selected ?: return
        val correct = s.correct + if (selected == q.exercise.correctIndex) 1 else 0
        if (s.questionIndex + 1 >= s.questions.size) {
            _state.update { it.copy(correct = correct, startUnitIndex = Placement.startUnit(correct), step = OnboardingStep.Goal, selected = null) }
        } else {
            _state.update { it.copy(correct = correct, questionIndex = it.questionIndex + 1, selected = null) }
        }
    }

    /** "I'm a beginner": start at unit 1. */
    fun skipPlacement() = _state.update { it.copy(startUnitIndex = 0, step = OnboardingStep.Goal) }

    fun pickGoal(minutes: Int) = _state.update { it.copy(goalMinutes = minutes) }

    fun setReminder(enabled: Boolean) = _state.update { it.copy(reminderEnabled = enabled) }

    fun setReminderTime(minuteOfDay: Int) = _state.update { it.copy(reminderMinuteOfDay = minuteOfDay) }

    fun back() = _state.update {
        when (it.step) {
            OnboardingStep.Welcome -> it
            OnboardingStep.Languages -> it.copy(step = OnboardingStep.Welcome)
            OnboardingStep.Placement -> it.copy(step = OnboardingStep.Languages)
            OnboardingStep.Goal -> it.copy(step = OnboardingStep.Languages)
        }
    }

    fun finish() {
        val s = _state.value
        if (s.finishing) return
        _state.update { it.copy(finishing = true) }
        viewModelScope.launch {
            repo.finish(
                OnboardingChoices(s.mainLang, s.targetLang, s.startUnitIndex, s.goalMinutes, s.reminderEnabled, s.reminderMinuteOfDay)
            )
            _state.update { it.copy(finished = true) }
        }
    }
}
