package com.learningapp.ui.review

import app.cash.turbine.test
import com.learningapp.data.db.CardRow
import com.learningapp.data.db.UserWordEntity
import com.learningapp.data.repo.ReviewRepository
import com.learningapp.domain.srs.Grade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModelTest {
    private val today = LocalDate.of(2026, 10, 20)
    private val clock = Clock.fixed(Instant.parse("2026-10-20T09:00:00Z"), ZoneOffset.UTC)

    private class Saved(val state: UserWordEntity, val grade: Int, val previousInterval: Int)

    private class FakeRepo(
        val due: List<CardRow>,
        val new: List<CardRow>,
        val newLeft: Int = 10,
    ) : ReviewRepository {
        val saved = mutableListOf<Saved>()
        var newLimitAsked = -1
        override suspend fun targetLang() = "es"
        override suspend fun dueCards() = due
        override suspend fun newCards(limit: Int): List<CardRow> = new.take(limit)
        override suspend fun newCardsLeftToday(dailyLimit: Int): Int {
            newLimitAsked = dailyLimit
            return newLeft
        }
        override suspend fun saveReview(newState: UserWordEntity, grade: Int, previousIntervalDays: Int) {
            saved += Saved(newState, grade, previousIntervalDays)
        }
    }

    private fun newCard(id: String) = CardRow(id, "w-$id", "ex", "t-$id", "tex", null, null, null, null, null)

    private fun dueCard(id: String) = CardRow(id, "w-$id", "ex", "t-$id", "tex", 2.5, 6, 2, 0, today.toEpochDay())

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun queue_dueCardsFirst_thenNewUpToLimit() = runTest {
        val repo = FakeRepo(due = listOf(dueCard("d1"), dueCard("d2")), new = (1..5).map { newCard("n$it") }, newLeft = 2)
        val vm = ReviewViewModel(repo, clock)

        assertEquals(10, repo.newLimitAsked)
        val order = mutableListOf<String>()
        while (vm.state.value.card != null) {
            order += vm.state.value.card!!.wordId
            vm.grade(Grade.Good)
        }
        assertEquals(listOf("d1", "d2", "n1", "n2"), order)
    }

    @Test
    fun position_andIntervals_forCurrentCard() = runTest {
        val vm = ReviewViewModel(FakeRepo(due = listOf(dueCard("d1")), new = listOf(newCard("n1"))), clock)
        val s = vm.state.value
        assertEquals("1 / 2", s.positionLabel)
        // Due card has interval 6, repetitions 2: Good -> 6 x 2.5 = 15 days.
        assertEquals(15, s.intervals[Grade.Good])
        assertEquals(1, s.intervals[Grade.Again])
    }

    @Test
    fun flip_togglesAndResetsOnNextCard() = runTest {
        val vm = ReviewViewModel(FakeRepo(due = emptyList(), new = listOf(newCard("n1"), newCard("n2"))), clock)
        assertFalse(vm.state.value.isFlipped)
        vm.flip()
        assertTrue(vm.state.value.isFlipped)
        vm.grade(Grade.Good)
        assertFalse(vm.state.value.isFlipped)
        assertEquals("n2", vm.state.value.card?.wordId)
        assertEquals("2 / 2", vm.state.value.positionLabel)
    }

    @Test
    fun grade_savesSchedulerResult() = runTest {
        val repo = FakeRepo(due = listOf(dueCard("d1")), new = emptyList())
        val vm = ReviewViewModel(repo, clock)
        vm.grade(Grade.Good)

        val saved = repo.saved.single()
        assertEquals("d1", saved.state.wordId)
        assertEquals("es", saved.state.targetLang)
        assertEquals(Grade.Good.value, saved.grade)
        assertEquals(6, saved.previousInterval)
        assertEquals(15, saved.state.intervalDays)
        assertEquals(today.plusDays(15).toEpochDay(), saved.state.dueEpochDay)
        assertEquals(clock.millis(), saved.state.lastReviewedAtMs)
    }

    @Test
    fun again_requeuesCardAtEndWithNewState() = runTest {
        val repo = FakeRepo(due = emptyList(), new = listOf(newCard("n1"), newCard("n2")))
        val vm = ReviewViewModel(repo, clock)

        vm.grade(Grade.Again) // n1
        assertEquals("n2", vm.state.value.card?.wordId)
        assertEquals("2 / 3", vm.state.value.positionLabel)
        vm.grade(Grade.Good) // n2
        assertEquals("n1", vm.state.value.card?.wordId)
        assertEquals("3 / 3", vm.state.value.positionLabel)
        vm.grade(Grade.Good) // n1 again

        assertTrue(vm.state.value.finished)
        assertEquals(listOf("n1", "n2", "n1"), repo.saved.map { it.state.wordId })
        // Second answer for n1 starts from the Again state (interval 1, lapse counted).
        assertEquals(1, repo.saved[2].previousInterval)
        assertEquals(1, repo.saved[2].state.lapses)
        assertEquals(2, vm.state.value.reviewed)
    }

    @Test
    fun finishedState_afterLastCard() = runTest {
        val vm = ReviewViewModel(FakeRepo(due = listOf(dueCard("d1")), new = emptyList()), clock)
        vm.state.test {
            assertFalse(awaitItem().finished)
            vm.grade(Grade.Easy)
            val done = awaitItem()
            assertTrue(done.finished)
            assertNull(done.card)
            assertEquals(1, done.reviewed)
            assertFalse(done.isEmpty)
        }
        // Grading after the end does nothing.
        vm.grade(Grade.Good)
        assertTrue(vm.state.value.finished)
    }

    @Test
    fun emptyState_whenNothingDue() = runTest {
        val vm = ReviewViewModel(FakeRepo(due = emptyList(), new = emptyList()), clock)
        val s = vm.state.value
        assertTrue(s.isEmpty)
        assertFalse(s.finished)
        assertNull(s.card)
    }
}
