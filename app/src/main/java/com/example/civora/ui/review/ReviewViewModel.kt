package com.example.civora.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.civora.AppContainer
import com.example.civora.domain.model.Question
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReviewUiState(
    val loaded: Boolean = false,
    val mistakes: List<Question> = emptyList(),
    val bookmarks: List<Question> = emptyList(),
)

class ReviewViewModel(private val container: AppContainer) : ViewModel() {
    val state: StateFlow<ReviewUiState> = combine(
        container.questions.mistakes,
        container.questions.bookmarks,
    ) { mistakes, bookmarks -> ReviewUiState(true, mistakes, bookmarks) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewUiState())

    fun removeBookmark(questionId: Int) {
        viewModelScope.launch { container.questions.toggleBookmark(questionId) }
    }
}
