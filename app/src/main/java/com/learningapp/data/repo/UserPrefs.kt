package com.learningapp.data.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** The user's choices: language pair, daily goal, reminder, and whether onboarding is done. */
@Singleton
class UserPrefs @Inject constructor(private val prefs: DataStore<Preferences>) {
    private val mainKey = stringPreferencesKey("main_lang")
    private val targetKey = stringPreferencesKey("target_lang")
    private val goalKey = intPreferencesKey("daily_goal_minutes")
    private val onboardedKey = booleanPreferencesKey("onboarding_done")
    private val reminderOnKey = booleanPreferencesKey("reminder_enabled")
    private val reminderTimeKey = intPreferencesKey("reminder_minute_of_day")

    val mainLang: Flow<String> = prefs.data.map { it[mainKey] ?: "en" }
    val targetLang: Flow<String> = prefs.data.map { it[targetKey] ?: "es" }

    /** Daily goal in minutes: 5, 10 or 20. */
    val dailyGoalMinutes: Flow<Int> = prefs.data.map { it[goalKey] ?: 10 }

    /** Null until set: onboarding decides, or existing installs are migrated to true. */
    val onboardingDone: Flow<Boolean?> = prefs.data.map { it[onboardedKey] }

    val reminderEnabled: Flow<Boolean> = prefs.data.map { it[reminderOnKey] ?: false }

    /** Minutes after midnight; default 19:00. */
    val reminderMinuteOfDay: Flow<Int> = prefs.data.map { it[reminderTimeKey] ?: DEFAULT_REMINDER }

    suspend fun setDailyGoal(minutes: Int) {
        prefs.edit { it[goalKey] = minutes }
    }

    suspend fun setLanguages(main: String, target: String) {
        prefs.edit { it[mainKey] = main; it[targetKey] = target }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        prefs.edit { it[onboardedKey] = done }
    }

    suspend fun setReminder(enabled: Boolean, minuteOfDay: Int) {
        prefs.edit { it[reminderOnKey] = enabled; it[reminderTimeKey] = minuteOfDay }
    }

    companion object {
        const val DEFAULT_REMINDER = 19 * 60
        val GOALS = listOf(5, 10, 20)
    }
}
