package com.learningapp.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learningapp.data.repo.LessonRepository
import com.learningapp.data.repo.UnitItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LearnViewModel @Inject constructor(repo: LessonRepository) : ViewModel() {
    /** Null while loading. */
    val units: StateFlow<List<UnitItem>?> = repo.units()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
