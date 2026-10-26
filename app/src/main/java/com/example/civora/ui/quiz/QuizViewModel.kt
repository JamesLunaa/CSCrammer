package com.example.civora.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.civora.AppContainer
import com.example.civora.domain.model.Question
import com.example.civora.domain.model.choiceLetter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuizUiState(
    val loading: Boolean = true,
    val questions: List<Question> = emptyList(),
    val index: Int = 0,
    val selected: Int? = null,
    val revealed: Boolean = false,
    val correct: Int = 0,
    val bookmarks: Set<Int> = emptySet(),
    /** Set once the session is written; the screen then moves on to Results. */
    val finishedSessionId: Long? = null,
) {
    val current: Question? get() = questions.getOrNull(index)
    val isLast: Boolean get() = index == questions.lastIndex
    val finished: Boolean get() = finishedSessionId != null
}

/** One question player shared by every mode that gives feedback after each answer. */
class QuizViewModel(
    private val container: AppContainer,
    private val source: QuizSource,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val _state = MutableStateFlow(QuizUiState())
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    private var sessionId = 0L
    private var startedAt = 0L
    private var shownAt = 0L
    private var pendingWrite: Job? = null

    init {
        viewModelScope.launch {
            container.questions.bookmarkIds.collect { ids -> _state.update { it.copy(bookmarks = ids) } }
        }
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        container.seeder.ensureSeeded()
        val questions = when (source) {
            QuizSource.Quick -> container.questions.random(QUICK_COUNT)
            is QuizSource.Practice -> container.questions.practice(
                source.subject,
                source.topics,
                source.difficulties,
                if (source.count > 0) source.count else Int.MAX_VALUE,
            )
        }
        if (questions.isNotEmpty()) {
            sessionId = container.progress.startSession(source.mode, source.filtersJson, questions.size)
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
        pendingWrite = viewModelScope.launch {
            container.progress.recordAttempt(sessionId, question.id, choiceLetter(chosen), isCorrect, timeMs)
        }
    }

    fun next() {
        val s = _state.value
        if (!s.revealed || s.finished) return
        if (s.isLast) {
            val correct = s.correct
            val duration = now() - startedAt
            // Results reads the session back, so publish the id only after the writes land.
            viewModelScope.launch {
                pendingWrite?.join()
                container.progress.finishSession(sessionId, correct, duration)
                _state.update { it.copy(finishedSessionId = sessionId) }
            }
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
