package com.learningapp.domain.srs

import com.learningapp.data.db.UserWordEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class Sm2SchedulerTest {
    private val today = LocalDate.of(2026, 10, 20)
    private val now = 1_000L

    private fun review(state: UserWordEntity?, grade: Grade, day: LocalDate = today) =
        Sm2Scheduler.review(state, "w1", "es", grade, day, now)

    /** A card already reviewed twice (interval 6 days, default ease). */
    private val mature = UserWordEntity(
        wordId = "w1", targetLang = "es", easeFactor = 2.5, intervalDays = 6,
        repetitions = 2, lapses = 0, dueEpochDay = today.toEpochDay(), lastReviewedAtMs = 0,
    )

    @Test
    fun newCard_good_dueTomorrow() {
        val s = review(null, Grade.Good)
        assertEquals("w1", s.wordId)
        assertEquals("es", s.targetLang)
        assertEquals(1, s.intervalDays)
        assertEquals(1, s.repetitions)
        assertEquals(0, s.lapses)
        assertEquals(2.5, s.easeFactor, 1e-9)
        assertEquals(today.plusDays(1).toEpochDay(), s.dueEpochDay)
        assertEquals(now, s.lastReviewedAtMs)
    }

    @Test
    fun secondReview_good_sixDays() {
        val s = review(review(null, Grade.Good), Grade.Good)
        assertEquals(6, s.intervalDays)
        assertEquals(2, s.repetitions)
    }

    @Test
    fun good_multipliesByEase() {
        val s = review(mature, Grade.Good)
        assertEquals(15, s.intervalDays)
        assertEquals(2.5, s.easeFactor, 1e-9)
        assertEquals(3, s.repetitions)
    }

    @Test
    fun hard_multipliesBy1_2_andLowersEase() {
        val s = review(mature, Grade.Hard)
        assertEquals(7, s.intervalDays)
        assertEquals(2.36, s.easeFactor, 1e-9)
        assertEquals(3, s.repetitions)
    }

    @Test
    fun easy_addsBonus_andRaisesEase() {
        val s = review(mature, Grade.Easy)
        assertEquals(20, s.intervalDays) // 6 * 2.5 * 1.3 = 19.5
        assertEquals(2.6, s.easeFactor, 1e-9)
    }

    @Test
    fun again_resetsProgress() {
        val s = review(mature.copy(repetitions = 5, intervalDays = 40, lapses = 2), Grade.Again)
        assertEquals(0, s.repetitions)
        assertEquals(1, s.intervalDays)
        assertEquals(3, s.lapses)
        assertEquals(1.96, s.easeFactor, 1e-9)
        assertEquals(today.plusDays(1).toEpochDay(), s.dueEpochDay)
        // Next success starts the ladder again: 1 day, then 6 days.
        val again1 = review(s, Grade.Good)
        assertEquals(1, again1.intervalDays)
        assertEquals(6, review(again1, Grade.Good).intervalDays)
    }

    @Test
    fun ease_neverBelowFloor() {
        var s: UserWordEntity? = null
        repeat(10) { s = review(s, Grade.Again) }
        assertEquals(Sm2Scheduler.MIN_EASE, s!!.easeFactor, 1e-9)
        assertEquals(Sm2Scheduler.MIN_EASE, Sm2Scheduler.nextEase(1.4, Grade.Hard), 1e-9)
    }

    @Test
    fun fiveGoodReviews_intervalGrowth() {
        var s: UserWordEntity? = null
        var day = today
        val intervals = mutableListOf<Int>()
        repeat(5) {
            val next = review(s, Grade.Good, day)
            intervals += next.intervalDays
            day = LocalDate.ofEpochDay(next.dueEpochDay)
            s = next
        }
        assertEquals(listOf(1, 6, 15, 38, 95), intervals)
        assertEquals(today.plusDays(1 + 6 + 15 + 38 + 95L).toEpochDay(), s!!.dueEpochDay)
    }

    @Test
    fun preview_matchesReview() {
        for (state in listOf(null, mature)) {
            val preview = Sm2Scheduler.previewIntervals(state)
            Grade.entries.forEach { g -> assertEquals(review(state, g).intervalDays, preview[g]) }
        }
        assertEquals(
            mapOf(Grade.Again to 1, Grade.Hard to 7, Grade.Good to 15, Grade.Easy to 20),
            Sm2Scheduler.previewIntervals(mature),
        )
    }

    @Test
    fun formatInterval() {
        assertEquals("1d", Sm2Scheduler.formatInterval(1))
        assertEquals("6d", Sm2Scheduler.formatInterval(6))
        assertEquals("3mo", Sm2Scheduler.formatInterval(95))
        assertEquals("1y", Sm2Scheduler.formatInterval(365))
        assertEquals("1.5y", Sm2Scheduler.formatInterval(548))
    }

    @Test
    fun grade_valuesAndQuality() {
        assertEquals(listOf(0, 1, 2, 3), Grade.entries.map { it.value })
        assertEquals(listOf(1, 3, 4, 5), Grade.entries.map { it.quality })
        assertEquals(Grade.Easy, Grade.fromValue(3))
    }
}
