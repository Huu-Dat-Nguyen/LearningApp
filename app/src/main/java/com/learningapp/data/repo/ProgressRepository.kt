package com.learningapp.data.repo

import com.learningapp.data.db.ActivityDao
import com.learningapp.data.db.DailyActivityEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** One day in the 7-day chart / week strip. */
data class DayStat(val date: LocalDate, val xp: Int, val minutes: Int, val active: Boolean)

data class ProgressSummary(
    val today: LocalDate,
    val todayMinutes: Int,
    val dailyGoalMinutes: Int,
    val streak: Int,
    val totalXp: Int,
    val wordsStarted: Int,
    /** Correct / answered over the last 7 days; null when nothing was answered. */
    val accuracy7d: Double?,
    /** Monday..Sunday of the current week. */
    val week: List<DayStat>,
    /** The last 7 days ending today, for the chart. */
    val last7: List<DayStat>,
) {
    val goalProgress: Float get() = if (dailyGoalMinutes <= 0) 0f else (todayMinutes.toFloat() / dailyGoalMinutes).coerceIn(0f, 1f)
}

/**
 * Consecutive active days ending today, or ending yesterday if today has no activity yet
 * (so the streak doesn't look broken first thing in the morning).
 */
fun streak(activeDaysDesc: List<Long>, today: Long): Int {
    val days = activeDaysDesc.toSortedSet().reversed()
    var cursor = if (today in days) today else today - 1
    var count = 0
    while (cursor in days) { count++; cursor-- }
    return count
}

/** Rounds seconds to whole minutes, counting any started minute after the first 30 s. */
fun minutes(seconds: Long): Int = ((seconds + 30) / 60).toInt()

@Singleton
class ProgressRepository @Inject constructor(
    private val activityDao: ActivityDao,
    private val prefs: UserPrefs,
    private val clock: Clock,
) {
    fun today(): LocalDate = LocalDate.now(clock)

    suspend fun addActivity(xp: Int = 0, seconds: Long = 0, answered: Int = 0, correct: Int = 0) {
        activityDao.add(today().toEpochDay(), prefs.targetLang.first(), xp, seconds, answered, correct)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun summary(): Flow<ProgressSummary> = prefs.targetLang.flatMapLatest { lang ->
        val today = today()
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val from = minOf(monday, today.minusDays(6)).toEpochDay()
        combine(
            activityDao.range(lang, from, today.plusDays(7).toEpochDay()),
            activityDao.activeDays(lang),
            activityDao.totalXp(lang),
            activityDao.wordsStarted(lang),
            prefs.dailyGoalMinutes,
        ) { range, active, total, words, goal ->
            buildSummary(today, range, active, total, words, goal)
        }
    }
}

internal fun buildSummary(
    today: LocalDate,
    range: List<DailyActivityEntity>,
    activeDaysDesc: List<Long>,
    totalXp: Int,
    wordsStarted: Int,
    goalMinutes: Int,
): ProgressSummary {
    val byDay = range.associateBy { it.epochDay }
    fun stat(d: LocalDate): DayStat {
        val a = byDay[d.toEpochDay()]
        return DayStat(d, a?.xp ?: 0, minutes(a?.activeSeconds ?: 0), (a?.xp ?: 0) > 0)
    }
    val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val last7 = (6 downTo 0).map { stat(today.minusDays(it.toLong())) }
    val last7Rows = last7.mapNotNull { byDay[it.date.toEpochDay()] }
    val answered = last7Rows.sumOf { it.answered }
    return ProgressSummary(
        today = today,
        todayMinutes = minutes(byDay[today.toEpochDay()]?.activeSeconds ?: 0),
        dailyGoalMinutes = goalMinutes,
        streak = streak(activeDaysDesc, today.toEpochDay()),
        totalXp = totalXp,
        wordsStarted = wordsStarted,
        accuracy7d = if (answered == 0) null else last7Rows.sumOf { it.correct }.toDouble() / answered,
        week = (0..6).map { stat(monday.plusDays(it.toLong())) },
        last7 = last7,
    )
}
