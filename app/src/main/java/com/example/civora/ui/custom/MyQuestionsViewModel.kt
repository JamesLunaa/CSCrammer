package com.example.civora.ui.custom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.civora.AppContainer
import com.example.civora.domain.model.Question
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MyQuestionsUiState(val loaded: Boolean = false, val questions: List<Question> = emptyList())

class MyQuestionsViewModel(container: AppContainer) : ViewModel() {
    val state: StateFlow<MyQuestionsUiState> = container.questions.custom
        .map { MyQuestionsUiState(true, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyQuestionsUiState())
}
