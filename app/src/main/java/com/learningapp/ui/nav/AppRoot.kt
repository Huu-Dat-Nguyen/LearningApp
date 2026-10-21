package com.learningapp.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.learningapp.data.repo.OnboardingStore
import com.learningapp.ui.onboarding.OnboardingScreen
import com.learningapp.ui.theme.ScreenBg
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class Gate { Loading, Onboarding, App }

@HiltViewModel
class GateViewModel @Inject constructor(store: OnboardingStore) : ViewModel() {
    private val _gate = MutableStateFlow(Gate.Loading)
    val gate: StateFlow<Gate> = _gate.asStateFlow()

    init {
        viewModelScope.launch { _gate.value = if (store.needsOnboarding()) Gate.Onboarding else Gate.App }
    }

    fun onboardingFinished() {
        _gate.value = Gate.App
    }
}

/** First launch shows onboarding; after that, the main app. */
@Composable
fun AppRoot(viewModel: GateViewModel = hiltViewModel()) {
    val gate by viewModel.gate.collectAsStateWithLifecycle()
    when (gate) {
        Gate.Loading -> Box(Modifier.fillMaxSize().background(ScreenBg))
        Gate.Onboarding -> Box(Modifier.fillMaxSize().background(ScreenBg)) { OnboardingScreen(onFinished = viewModel::onboardingFinished) }
        Gate.App -> AppNavHost()
    }
}
