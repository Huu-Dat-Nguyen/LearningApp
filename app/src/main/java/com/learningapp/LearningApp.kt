package com.learningapp

import android.app.Application
import android.util.Log
import com.learningapp.data.content.ContentSeeder
import com.learningapp.data.repo.UserPrefs
import com.learningapp.reminder.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LearningApp : Application() {

    @Inject lateinit var seeder: ContentSeeder
    @Inject lateinit var prefs: UserPrefs
    @Inject lateinit var reminders: ReminderScheduler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createChannel(this)
        // Load the bundled word list for the current target language (no-op if already imported).
        appScope.launch {
            runCatching { seeder.seed(prefs.targetLang.first()) }
                .onFailure { Log.e("LearningApp", "Content seeding failed", it) }
        }
        // Keep the reminder job in line with the settings (onboarding and Settings both just change prefs).
        appScope.launch {
            combine(prefs.onboardingDone, prefs.reminderEnabled, prefs.reminderMinuteOfDay) { done, on, minute ->
                (done == true && on) to minute
            }.distinctUntilChanged().collect { (on, minute) -> reminders.apply(on, minute) }
        }
    }
}
