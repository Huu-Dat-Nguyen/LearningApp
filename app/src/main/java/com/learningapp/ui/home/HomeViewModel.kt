package com.learningapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learningapp.data.repo.WordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(repo: WordRepository) : ViewModel() {
    val cardsDue: StateFlow<Int> = repo.dueCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
