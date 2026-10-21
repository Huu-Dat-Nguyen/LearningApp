package com.learningapp.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.learningapp.MainActivity
import com.learningapp.R
import com.learningapp.data.content.languageName
import com.learningapp.data.db.ActivityDao
import com.learningapp.data.repo.UserPrefs
import com.learningapp.data.repo.WordRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Time from [now] until the next [minuteOfDay] (today if still ahead, else tomorrow). */
fun delayUntilNext(now: ZonedDateTime, minuteOfDay: Int): Duration {
    val today = now.toLocalDate().atStartOfDay(now.zone).plusMinutes(minuteOfDay.toLong())
    val next = if (today.isAfter(now)) today else today.plusDays(1)
    return Duration.between(now, next)
}

/** The reminder text, or null when the user already studied today. */
fun reminderText(studiedToday: Boolean, targetLanguage: String, cardsDue: Int, goalMinutes: Int): String? = when {
    studiedToday -> null
    cardsDue > 0 -> "$goalMinutes minutes of $targetLanguage today? $cardsDue ${if (cardsDue == 1) "card is" else "cards are"} due."
    else -> "$goalMinutes minutes of $targetLanguage today? Keep your streak going."
}

/** Schedules one WorkManager job for the next reminder time; the job schedules the next one. */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) {
    fun apply(enabled: Boolean, minuteOfDay: Int) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(WORK_NAME)
            return
        }
        val delay = delayUntilNext(ZonedDateTime.now(clock), minuteOfDay)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        // REPLACE so a new time takes over from the old one.
        wm.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        const val WORK_NAME = "daily_reminder"
        const val CHANNEL_ID = "daily_reminder"
        const val NOTIFICATION_ID = 1001

        fun createChannel(context: Context) {
            val channel = NotificationChannel(CHANNEL_ID, "Daily reminder", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "A daily nudge to practise"
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        fun canNotify(context: Context): Boolean =
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderEntryPoint {
    fun prefs(): UserPrefs
    fun activityDao(): ActivityDao
    fun words(): WordRepository
    fun scheduler(): ReminderScheduler
    fun clock(): Clock
}

/** Shows the reminder unless the user already studied today, then schedules tomorrow's. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, ReminderEntryPoint::class.java)
        val prefs = deps.prefs()
        if (!prefs.reminderEnabled.first()) return Result.success()
        val target = prefs.targetLang.first()
        val today = LocalDate.now(deps.clock()).toEpochDay()
        val studied = (deps.activityDao().day(today, target)?.xp ?: 0) > 0
        val text = reminderText(studied, languageName(target), deps.words().dueCount().first(), prefs.dailyGoalMinutes.first())
        if (text != null && ReminderScheduler.canNotify(applicationContext)) show(text)
        deps.scheduler().apply(true, prefs.reminderMinuteOfDay.first())
        return Result.success()
    }

    private fun show(text: String) {
        val open = PendingIntent.getActivity(
            applicationContext, 0,
            Intent(applicationContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to practise")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(applicationContext).notify(ReminderScheduler.NOTIFICATION_ID, n)
        } catch (_: SecurityException) {
            // Permission was revoked between the check and now; skip this one.
        }
    }
}
