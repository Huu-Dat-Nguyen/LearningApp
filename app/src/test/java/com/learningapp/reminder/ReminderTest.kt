package com.learningapp.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderTest {
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")
    private fun at(h: Int, m: Int) = ZonedDateTime.of(2026, 10, 21, h, m, 0, 0, zone)

    // English wording, same as values/strings.xml.
    private val english = object : ReminderStrings {
        override fun withDueCards(goalMinutes: Int, language: String, cardsDue: Int) =
            "$goalMinutes minutes of $language today? $cardsDue ${if (cardsDue == 1) "card is" else "cards are"} due."

        override fun keepStreak(goalMinutes: Int, language: String) =
            "$goalMinutes minutes of $language today? Keep your streak going."
    }

    @Test
    fun laterTodayWhenStillAhead() {
        assertEquals(Duration.ofHours(10), delayUntilNext(at(9, 0), 19 * 60))
    }

    @Test
    fun tomorrowWhenPassed() {
        assertEquals(Duration.ofHours(23), delayUntilNext(at(20, 0), 19 * 60))
    }

    @Test
    fun exactlyNowMeansTomorrow() {
        assertEquals(Duration.ofDays(1), delayUntilNext(at(19, 0), 19 * 60))
    }

    @Test
    fun aroundMidnight() {
        assertEquals(Duration.ofMinutes(10), delayUntilNext(at(23, 55), 5))
    }

    @Test
    fun dstChangeKeepsWallClockTime() {
        val ny = ZoneId.of("America/New_York")
        // Clocks go back on 2026-11-01: that day is 25 hours long.
        val now = ZonedDateTime.of(2026, 10, 31, 20, 0, 0, 0, ny)
        assertEquals(Duration.ofHours(24), delayUntilNext(now, 19 * 60))
    }

    @Test
    fun noReminderAfterStudying() {
        assertNull(reminderText(studiedToday = true, targetLanguage = "Spanish", cardsDue = 5, goalMinutes = 10, strings = english))
    }

    @Test
    fun textMentionsDueCards() {
        assertEquals("10 minutes of Spanish today? 12 cards are due.", reminderText(false, "Spanish", 12, 10, english))
        assertEquals("5 minutes of Spanish today? 1 card is due.", reminderText(false, "Spanish", 1, 5, english))
        assertEquals("10 minutes of Spanish today? Keep your streak going.", reminderText(false, "Spanish", 0, 10, english))
    }
}
