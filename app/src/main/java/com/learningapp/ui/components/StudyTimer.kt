package com.learningapp.ui.components

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import com.learningapp.data.repo.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StudyTimeViewModel @Inject constructor(private val progress: ProgressRepository) : ViewModel() {
    // Saved on the repository's app-wide scope: the screen (and this ViewModel) may be closing.
    fun add(seconds: Long) = progress.recordSeconds(seconds)
}

/**
 * Counts time while this screen is on screen and the app is in the foreground,
 * and adds it to today's study minutes when the screen pauses or closes.
 */
@Composable
fun TrackStudyTime(viewModel: StudyTimeViewModel = hiltViewModel()) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        var startedAt: Long? = null
        fun flush() {
            startedAt?.let { viewModel.add((SystemClock.elapsedRealtime() - it) / 1000) }
            startedAt = null
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> startedAt = SystemClock.elapsedRealtime()
                Lifecycle.Event.ON_PAUSE -> flush()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) startedAt = SystemClock.elapsedRealtime()
        onDispose {
            owner.lifecycle.removeObserver(observer)
            flush()
        }
    }
}
