package com.learningapp.data.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** The user's language pair. Onboarding sets these later; defaults are English → Spanish. */
@Singleton
class UserPrefs @Inject constructor(private val prefs: DataStore<Preferences>) {
    private val mainKey = stringPreferencesKey("main_lang")
    private val targetKey = stringPreferencesKey("target_lang")

    val mainLang: Flow<String> = prefs.data.map { it[mainKey] ?: "en" }
    val targetLang: Flow<String> = prefs.data.map { it[targetKey] ?: "es" }

    private val goalKey = intPreferencesKey("daily_goal_minutes")
    /** Daily goal in minutes; 10 until onboarding/settings let the user pick 5, 10 or 20. */
    val dailyGoalMinutes: Flow<Int> = prefs.data.map { it[goalKey] ?: 10 }

    suspend fun setDailyGoal(minutes: Int) {
        prefs.edit { it[goalKey] = minutes }
    }

    suspend fun setLanguages(main: String, target: String) {
        prefs.edit { it[mainKey] = main; it[targetKey] = target }
    }
}
