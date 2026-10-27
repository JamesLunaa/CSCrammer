package com.example.civora.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.civora.AppContainer
import com.example.civora.data.db.SessionEntity
import com.example.civora.domain.ResultItem
import com.example.civora.domain.model.choiceIndex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResultsUiState(
    val loading: Boolean = true,
    val session: SessionEntity? = null,
    val items: List<ResultItem> = emptyList(),
)

class ResultsViewModel(private val container: AppContainer, private val sessionId: Long) : ViewModel() {
    private val _state = MutableStateFlow(ResultsUiState())
    val state: StateFlow<ResultsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val session = container.progress.session(sessionId)
            val attempts = container.progress.attempts(sessionId)
            val questions = container.questions.byIds(attempts.map { it.questionId }).associateBy { it.id }
            val items = attempts.mapNotNull { a ->
                questions[a.questionId]?.let { ResultItem(it, a.chosen?.let(::choiceIndex), a.isCorrect) }
            }
            _state.update { ResultsUiState(loading = false, session = session, items = items) }
        }
    }
}
