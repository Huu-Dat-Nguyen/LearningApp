package com.learningapp.domain.srs

import com.learningapp.data.db.UserWordEntity
import java.time.LocalDate
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * SM-2 spaced-repetition scheduler. Pure Kotlin, no Android dependencies.
 *
 * - Again: repetitions = 0, interval = 1 day, lapses + 1.
 * - Otherwise: repetition 1 -> 1 day, 2 -> 6 days, then interval x ease
 *   (Hard uses x1.2 instead of ease, Easy adds a 1.3x bonus).
 * - Ease: ease + (0.1 - (5 - q)(0.08 + (5 - q)0.02)), minimum 1.3.
 */
object Sm2Scheduler {
    const val DEFAULT_EASE = 2.5
    const val MIN_EASE = 1.3
    const val HARD_MULTIPLIER = 1.2
    const val EASY_BONUS = 1.3

    /** Returns the new state after answering [grade]. [state] is null for a new card. */
    fun review(
        state: UserWordEntity?,
        wordId: String,
        targetLang: String,
        grade: Grade,
        today: LocalDate,
        nowMs: Long,
    ): UserWordEntity {
        val ease = state?.easeFactor ?: DEFAULT_EASE
        val repetitions = state?.repetitions ?: 0
        val lapses = state?.lapses ?: 0
        val interval = nextInterval(state, grade)
        return UserWordEntity(
            wordId = state?.wordId ?: wordId,
            targetLang = state?.targetLang ?: targetLang,
            easeFactor = nextEase(ease, grade),
            intervalDays = interval,
            repetitions = if (grade == Grade.Again) 0 else repetitions + 1,
            lapses = if (grade == Grade.Again) lapses + 1 else lapses,
            dueEpochDay = today.plusDays(interval.toLong()).toEpochDay(),
            lastReviewedAtMs = nowMs,
        )
    }

    /** Interval in days the card would get for [grade]. */
    fun nextInterval(state: UserWordEntity?, grade: Grade): Int {
        if (grade == Grade.Again) return 1
        val ease = state?.easeFactor ?: DEFAULT_EASE
        val previous = state?.intervalDays ?: 0
        return when ((state?.repetitions ?: 0) + 1) {
            1 -> 1
            2 -> 6
            else -> {
                val factor = when (grade) {
                    Grade.Hard -> HARD_MULTIPLIER
                    Grade.Easy -> ease * EASY_BONUS
                    else -> ease
                }
                // Always move forward at least one day.
                max(previous + 1, (previous * factor).roundToInt())
            }
        }
    }

    fun nextEase(ease: Double, grade: Grade): Double {
        val d = 5 - grade.quality
        return max(MIN_EASE, ease + (0.1 - d * (0.08 + d * 0.02)))
    }

    /** Next interval for every grade, for the review buttons ("Good · 6d"). */
    fun previewIntervals(state: UserWordEntity?): Map<Grade, Int> =
        Grade.entries.associateWith { nextInterval(state, it) }

    /** Short label for an interval: "1d", "6d", "3mo", "1y". */
    fun formatInterval(days: Int): String = when {
        days < 30 -> "${days}d"
        days < 365 -> "${(days / 30.0).roundToInt()}mo"
        else -> "%.1f".format(Locale.ROOT, days / 365.0).removeSuffix(".0") + "y"
    }
}
