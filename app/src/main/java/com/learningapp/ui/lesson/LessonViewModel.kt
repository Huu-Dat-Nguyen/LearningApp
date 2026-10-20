package com.learningapp.ui.lesson

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.learningapp.data.repo.LessonData
import com.learningapp.data.repo.LessonResult
import com.learningapp.data.repo.LessonStore
import com.learningapp.domain.lesson.Answer
import com.learningapp.domain.lesson.CheckResult
import com.learningapp.domain.lesson.Exercise
import com.learningapp.domain.lesson.ExerciseBuilder
import com.learningapp.domain.lesson.LessonSession
import com.learningapp.ui.nav.LessonRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

/** A word chip in a word-order exercise; [id] keeps repeated words apart. */
data class Chip(val id: Int, val text: String)

data class LessonUiState(
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val title: String = "",
    val targetLang: String = "",
    val exercise: Exercise? = null,
    val isRetry: Boolean = false,
    val progress: Float = 0f,
    // Answer being built
    val selected: Int? = null,
    val text: String = "",
    val pool: List<Chip> = emptyList(),
    val placed: List<Chip> = emptyList(),
    /** Set after Check; the screen shows the feedback panel and a Continue button. */
    val feedback: CheckResult? = null,
    /** Set when the lesson is over. */
    val result: LessonResult? = null,
) {
    val canCheck: Boolean
        get() = feedback == null && when (exercise) {
            is Exercise.MultipleChoice -> selected != null
            is Exercise.WordOrder -> placed.isNotEmpty()
            null -> false
            else -> text.isNotBlank()
        }
}

@HiltViewModel
class LessonViewModel internal constructor(
    private val lessonId: String,
    private val store: LessonStore,
    private val random: Random,
) : ViewModel() {

    @Inject
    constructor(handle: SavedStateHandle, store: LessonStore) :
        this(handle.toRoute<LessonRoute>().lessonId, store, Random.Default)

    private var data: LessonData? = null
    private var session: LessonSession? = null

    private val _state = MutableStateFlow(LessonUiState())
    val state: StateFlow<LessonUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val lesson = store.lesson(lessonId)
            if (lesson == null) {
                _state.value = LessonUiState(loading = false, notFound = true)
                return@launch
            }
            data = lesson
            val exercises = ExerciseBuilder(lesson.unitWords, lesson.mainLang, random).build(lesson.exercises)
            session = LessonSession(exercises)
            showCurrent()
        }
    }

    fun select(index: Int) = edit { copy(selected = index) }

    fun type(text: String) = edit { copy(text = text) }

    /** Moves a chip from the pool to the end of the answer line. */
    fun place(chip: Chip) = edit { copy(pool = pool - chip, placed = placed + chip) }

    /** Moves a chip from the answer line back to the pool. */
    fun unplace(chip: Chip) = edit { copy(placed = placed - chip, pool = pool + chip) }

    fun check() {
        val s = _state.value
        val session = session ?: return
        if (!s.canCheck) return
        val answer = when (s.exercise) {
            is Exercise.MultipleChoice -> Answer.Choice(s.selected!!)
            is Exercise.WordOrder -> Answer.Chips(s.placed.map { it.text })
            else -> Answer.Text(s.text)
        }
        _state.update { it.copy(feedback = session.submit(answer), progress = session.progress) }
    }

    /** After feedback: next exercise, or finish the lesson. */
    fun next() {
        val session = session ?: return
        if (_state.value.feedback == null) return
        session.next()
        if (session.finished) finish(session) else showCurrent()
    }

    private fun finish(session: LessonSession) {
        val lesson = data ?: return
        val result = LessonResult(lesson.id, session.accuracy, session.xp, session.answered, session.correct)
        _state.update { it.copy(result = result, feedback = null, exercise = null, progress = 1f) }
        viewModelScope.launch { store.completeLesson(result, lesson.lessonWordIds) }
    }

    private fun showCurrent() {
        val lesson = data ?: return
        val session = session ?: return
        val ex = session.exercise
        _state.value = LessonUiState(
            loading = false,
            title = lesson.title,
            targetLang = lesson.targetLang,
            exercise = ex,
            isRetry = session.isRetry,
            progress = session.progress,
            pool = (ex as? Exercise.WordOrder)?.chips?.mapIndexed { i, t -> Chip(i, t) }.orEmpty(),
        )
    }

    /** Changes the answer only while no feedback is shown. */
    private inline fun edit(crossinline change: LessonUiState.() -> LessonUiState) {
        _state.update { if (it.feedback == null) it.change() else it }
    }
}
