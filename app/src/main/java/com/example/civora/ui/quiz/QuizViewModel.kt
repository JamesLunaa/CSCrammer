package com.example.civora.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.civora.AppContainer
import com.example.civora.domain.model.Question
import com.example.civora.domain.model.choiceLetter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object QuizModes {
    const val Quick = "quick"
}

data class QuizUiState(
    val loading: Boolean = true,
    val questions: List<Question> = emptyList(),
    val index: Int = 0,
    val selected: Int? = null,
    val revealed: Boolean = false,
    val correct: Int = 0,
    val bookmarks: Set<Int> = emptySet(),
    val finished: Boolean = false,
) {
    val current: Question? get() = questions.getOrNull(index)
    val isLast: Boolean get() = index == questions.lastIndex
}

/** One question player shared by every mode that gives feedback after each answer. */
class QuizViewModel(
    private val container: AppContainer,
    private val mode: String,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val _state = MutableStateFlow(QuizUiState())
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    private var sessionId = 0L
    private var startedAt = 0L
    private var shownAt = 0L

    init {
        viewModelScope.launch {
            container.questions.bookmarkIds.collect { ids -> _state.update { it.copy(bookmarks = ids) } }
        }
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        container.seeder.ensureSeeded()
        val questions = when (mode) {
            QuizModes.Quick -> container.questions.random(QUICK_COUNT)
            else -> emptyList()
        }
        if (questions.isNotEmpty()) {
            sessionId = container.progress.startSession(mode, "{}", questions.size)
        }
        startedAt = now()
        shownAt = startedAt
        _state.update { it.copy(loading = false, questions = questions) }
    }

    fun select(choice: Int) {
        _state.update { if (it.revealed || it.finished) it else it.copy(selected = choice) }
    }

    fun check() {
        val s = _state.value
        val question = s.current ?: return
        val chosen = s.selected ?: return
        if (s.revealed) return
        val isCorrect = chosen == question.answerIndex
        _state.update { it.copy(revealed = true, correct = it.correct + if (isCorrect) 1 else 0) }
        val timeMs = now() - shownAt
        viewModelScope.launch {
            container.progress.recordAttempt(sessionId, question.id, choiceLetter(chosen), isCorrect, timeMs)
        }
    }

    fun next() {
        val s = _state.value
        if (!s.revealed || s.finished) return
        if (s.isLast) {
            _state.update { it.copy(finished = true) }
            val correct = s.correct
            val duration = now() - startedAt
            viewModelScope.launch { container.progress.finishSession(sessionId, correct, duration) }
        } else {
            shownAt = now()
            _state.update { it.copy(index = it.index + 1, selected = null, revealed = false) }
        }
    }

    fun toggleBookmark() {
        val id = _state.value.current?.id ?: return
        viewModelScope.launch { container.questions.toggleBookmark(id) }
    }

    private companion object {
        const val QUICK_COUNT = 10
    }
}
