package com.learningapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learningapp.data.content.ContentCatalog
import com.learningapp.data.content.ContentSeeder
import com.learningapp.data.repo.UserPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val mainLang: String,
    val targetLang: String,
    val goalMinutes: Int,
    val reminderEnabled: Boolean,
    val reminderMinuteOfDay: Int,
    val pairs: Map<String, Set<String>>,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPrefs,
    private val catalog: ContentCatalog,
    private val seeder: ContentSeeder,
) : ViewModel() {

    val state: StateFlow<SettingsUiState?> = combine(
        prefs.mainLang, prefs.targetLang, prefs.dailyGoalMinutes, prefs.reminderEnabled, prefs.reminderMinuteOfDay,
    ) { main, target, goal, on, time -> SettingsUiState(main, target, goal, on, time, catalog.pairs) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Changes the language pair if it has content; progress is kept per target language. */
    fun setLanguages(main: String, target: String) {
        if (!catalog.isAvailable(main, target)) return
        viewModelScope.launch {
            seeder.seed(target)
            prefs.setLanguages(main, target)
        }
    }

    fun setGoal(minutes: Int) = viewModelScope.launch { prefs.setDailyGoal(minutes) }

    fun setReminder(enabled: Boolean, minuteOfDay: Int) = viewModelScope.launch { prefs.setReminder(enabled, minuteOfDay) }
}
