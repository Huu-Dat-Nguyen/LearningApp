package com.learningapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learningapp.data.repo.LessonItem
import com.learningapp.data.repo.LessonRepository
import com.learningapp.data.repo.LessonState
import com.learningapp.data.repo.WordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(repo: WordRepository, lessons: LessonRepository) : ViewModel() {
    val cardsDue: StateFlow<Int> = repo.dueCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** The lesson "Continue lesson" opens; null when every lesson is done (or still loading). */
    val nextLesson: StateFlow<LessonItem?> = lessons.units()
        .map { units -> units.flatMap { it.lessons }.firstOrNull { it.state == LessonState.Next } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
