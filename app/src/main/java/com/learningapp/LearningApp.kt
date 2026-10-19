package com.learningapp

import android.app.Application
import android.util.Log
import com.learningapp.data.content.ContentSeeder
import com.learningapp.data.repo.UserPrefs
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LearningApp : Application() {

    @Inject lateinit var seeder: ContentSeeder
    @Inject lateinit var prefs: UserPrefs

    private val appScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        // Load the bundled word list for the current target language (no-op if already imported).
        appScope.launch {
            runCatching { seeder.seed(prefs.targetLang.first()) }
                .onFailure { Log.e("LearningApp", "Content seeding failed", it) }
        }
    }
}
