package com.learningapp.data.repo

import com.learningapp.data.db.DailyActivityEntity
import com.learningapp.data.db.LessonRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ProgressMathTest {

    private val today = LocalDate.of(2026, 10, 21) // a Wednesday
    private val t = today.toEpochDay()

    @Test
    fun streakCountsBackFromToday() {
        assertEquals(3, streak(listOf(t, t - 1, t - 2, t - 4), t))
    }

    @Test
    fun streakStillCountsWhenTodayNotStartedYet() {
        assertEquals(2, streak(listOf(t - 1, t - 2), t))
    }

    @Test
    fun streakBrokenAfterMissedDay() {
        assertEquals(0, streak(listOf(t - 2, t - 3), t))
        assertEquals(0, streak(emptyList(), t))
    }

    @Test
    fun minutesRounding() {
        assertEquals(0, minutes(29))
        assertEquals(1, minutes(30))
        assertEquals(10, minutes(600))
    }

    @Test
    fun summaryBuildsWeekGoalAndAccuracy() {
        val rows = listOf(
            DailyActivityEntity(t, "es", xp = 20, activeSeconds = 300, answered = 10, correct = 8),
            DailyActivityEntity(t - 1, "es", xp = 5, activeSeconds = 120, answered = 10, correct = 6),
            DailyActivityEntity(t - 9, "es", xp = 50, activeSeconds = 900, answered = 10, correct = 0), // outside 7 days
        )
        val s = buildSummary(today, rows, listOf(t, t - 1), totalXp = 75, wordsStarted = 12, goalMinutes = 10)
        assertEquals(5, s.todayMinutes)
        assertEquals(0.5f, s.goalProgress)
        assertEquals(2, s.streak)
        assertEquals(0.7, s.accuracy7d!!, 1e-9)
        assertEquals(LocalDate.of(2026, 10, 19), s.week.first().date) // Monday
        assertEquals(listOf(false, true, true, false, false, false, false), s.week.map { it.active })
        assertEquals(today, s.last7.last().date)
        assertEquals(20, s.last7.last().xp)
    }

    @Test
    fun noAnswersMeansNoAccuracy() {
        assertNull(buildSummary(today, emptyList(), emptyList(), 0, 0, 10).accuracy7d)
    }

    @Test
    fun lessonsUnlockInOrder() {
        fun row(id: String, unit: String, order: Int, done: Boolean) =
            LessonRow(id, unit, """{"en":"U $unit"}""", "A1", """{"en":"L $id"}""", order, done, if (done) 1.0 else null)
        val units = toUnits(
            listOf(row("a", "u1", 0, true), row("b", "u1", 1, false), row("c", "u2", 2, false), row("d", "u2", 3, false)),
            "en",
        )
        assertEquals(listOf("U u1", "U u2"), units.map { it.title })
        assertEquals(
            listOf(LessonState.Done, LessonState.Next, LessonState.Locked, LessonState.Locked),
            units.flatMap { it.lessons }.map { it.state },
        )
    }
}
