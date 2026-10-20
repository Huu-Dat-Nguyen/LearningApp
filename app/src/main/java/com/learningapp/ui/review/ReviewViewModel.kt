package com.learningapp.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learningapp.data.db.CardRow
import com.learningapp.data.db.UserWordEntity
import com.learningapp.data.repo.ReviewRepository
import com.learningapp.domain.srs.Grade
import com.learningapp.domain.srs.Sm2Scheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class ReviewUiState(
    val loading: Boolean = true,
    val targetLang: String = "",
    val card: CardRow? = null,
    val isFlipped: Boolean = false,
    /** 1-based position of the current card; [total] grows when a card is re-queued with Again. */
    val position: Int = 0,
    val total: Int = 0,
    /** Next interval in days for each grade button. */
    val intervals: Map<Grade, Int> = emptyMap(),
    /** Distinct cards answered this session. */
    val reviewed: Int = 0,
    val finished: Boolean = false,
) {
    /** Nothing due and no new cards left today. */
    val isEmpty: Boolean get() = !loading && total == 0
    val positionLabel: String get() = "$position / $total"
}

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val repo: ReviewRepository,
    private val clock: Clock,
) : ViewModel() {

    private class Item(val card: CardRow, val state: UserWordEntity?)

    private val queue = mutableListOf<Item>()
    private var index = 0
    private val reviewedIds = mutableSetOf<String>()
    private var targetLang = ""
    private val saveLock = Mutex()

    private val _state = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            targetLang = repo.targetLang()
            val due = repo.dueCards()
            val new = repo.newCards(repo.newCardsLeftToday(NEW_CARDS_PER_DAY))
            (due + new).forEach { queue += Item(it, it.toState(targetLang)) }
            publish(isFlipped = false)
        }
    }

    fun flip() {
        val s = _state.value
        if (s.card != null) _state.value = s.copy(isFlipped = !s.isFlipped)
    }

    fun grade(grade: Grade) {
        val item = queue.getOrNull(index) ?: return
        val newState = Sm2Scheduler.review(
            item.state, item.card.wordId, targetLang, grade, LocalDate.now(clock), clock.millis(),
        )
        val previous = item.state?.intervalDays ?: 0
        reviewedIds += item.card.wordId
        // Again: see the card once more at the end of this session, with its new state.
        if (grade == Grade.Again) queue += Item(item.card, newState)
        index++
        publish(isFlipped = false)
        viewModelScope.launch {
            saveLock.withLock { repo.saveReview(newState, grade.value, previous) }
        }
    }

    private fun publish(isFlipped: Boolean) {
        val item = queue.getOrNull(index)
        _state.value = ReviewUiState(
            loading = false,
            targetLang = targetLang,
            card = item?.card,
            isFlipped = isFlipped,
            position = if (item != null) index + 1 else queue.size,
            total = queue.size,
            intervals = item?.let { Sm2Scheduler.previewIntervals(it.state) } ?: emptyMap(),
            reviewed = reviewedIds.size,
            finished = item == null && queue.isNotEmpty(),
        )
    }

    companion object {
        const val NEW_CARDS_PER_DAY = 10
    }
}

/** Review state stored for this card, or null when it is new. */
private fun CardRow.toState(targetLang: String): UserWordEntity? =
    if (isNew) null else UserWordEntity(
        wordId = wordId,
        targetLang = targetLang,
        easeFactor = easeFactor ?: Sm2Scheduler.DEFAULT_EASE,
        intervalDays = intervalDays ?: 0,
        repetitions = repetitions ?: 0,
        lapses = lapses ?: 0,
        dueEpochDay = dueEpochDay!!,
        lastReviewedAtMs = 0,
    )
