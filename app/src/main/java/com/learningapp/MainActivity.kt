package com.learningapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.learningapp.data.repo.UserPrefs
import com.learningapp.ui.nav.AppRoot
import com.learningapp.ui.theme.LearningAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefs: UserPrefs

    /** The UI language this activity was created with. */
    private var appliedLang: String? = null

    override fun attachBaseContext(newBase: Context) {
        appliedLang = AppLocale.saved(newBase)
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Follow the main language: when it changes (onboarding, Settings), remember it and rebuild the screen in it.
        lifecycleScope.launch {
            prefs.mainLang.distinctUntilChanged().collect { lang ->
                if (lang != appliedLang) {
                    AppLocale.save(applicationContext, lang)
                    recreate()
                }
            }
        }
        setContent {
            LearningAppTheme { AppRoot() }
        }
    }
}
