package com.example.civora.ui.mock

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.civora.AppContainer
import com.example.civora.data.db.AttemptEntity
import com.example.civora.domain.MockKind
import com.example.civora.domain.model.Question
import com.example.civora.domain.model.Subject
import com.example.civora.domain.model.choiceLetter
import com.example.civora.domain.splitEvenly
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MockUiState(
    val loading: Boolean = true,
    val kind: MockKind = MockKind.Full,
    val questions: List<Question> = emptyList(),
    /** Question index -> chosen choice index. */
    val answers: Map<Int, Int> = emptyMap(),
    val flagged: Set<Int> = emptySet(),
    val index: Int = 0,
    val remainingMs: Long = 0,
    val submitting: Boolean = false,
    val finishedSessionId: Long? = null,
) {
    val current: Question? get() = questions.getOrNull(index)
    val isLast: Boolean get() = index == questions.lastIndex
    val unanswered: Int get() = questions.size - answers.size

    /** Amber for the final five minutes. */
    val lowTime: Boolean get() = remainingMs <= LOW_TIME_MS

    companion object {
        const val LOW_TIME_MS = 5 * 60_000L
    }
}

/** Timed exam player: no per-answer feedback, free navigation, auto-submit when time runs out. */
class MockViewModel(
    private val container: AppContainer,
    kind: MockKind,
    private val now: () -> Long = System::currentTimeMillis,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) : ViewModel() {
    private val _state = MutableStateFlow(MockUiState(kind = kind))
    val state: StateFlow<MockUiState> = _state.asStateFlow()

    private var sessionId = 0L
    private var startedAt = 0L
    private var deadline = 0L
    private var shownAt = 0L
    private val spentMs = HashMap<Int, Long>()

    init {
        viewModelScope.launch { load(kind) }
    }

    private suspend fun load(kind: MockKind) {
        container.seeder.ensureSeeded()
        // Grouped by subject, in the fixed subject order, like the sections of the real exam.
        val counts = splitEvenly(kind.items, Subject.entries.size)
        val questions = Subject.entries.zip(counts).flatMap { (subject, n) ->
            container.questions.randomBySubject(subject, n)
        }
        if (questions.isEmpty()) {
            _state.update { it.copy(loading = false) }
            return
        }
        sessionId = container.progress.startSession(kind.sessionMode, "{}", questions.size)
        startedAt = now()
        deadline = clock() + kind.durationMs
        shownAt = clock()
        _state.update { it.copy(loading = false, questions = questions, remainingMs = kind.durationMs) }
        tick()
    }

    private suspend fun tick() {
        while (_state.value.finishedSessionId == null) {
            val left = (deadline - clock()).coerceAtLeast(0)
            _state.update { it.copy(remainingMs = left) }
            if (left == 0L) {
                submit()
                return
            }
            delay(1_000)
        }
    }

    fun select(choice: Int) {
        _state.update { s ->
            if (s.submitting) s else s.copy(answers = if (s.answers[s.index] == choice) s.answers - s.index else s.answers + (s.index to choice))
        }
    }

    fun clearAnswer() = _state.update { it.copy(answers = it.answers - it.index) }

    fun toggleFlag() = _state.update { s ->
        s.copy(flagged = if (s.index in s.flagged) s.flagged - s.index else s.flagged + s.index)
    }

    fun goTo(index: Int) {
        val s = _state.value
        if (s.submitting || index !in s.questions.indices) return
        val t = clock()
        spentMs[s.index] = (spentMs[s.index] ?: 0) + (t - shownAt)
        shownAt = t
        _state.update { it.copy(index = index) }
    }

    fun next() = goTo(_state.value.index + 1)

    fun previous() = goTo(_state.value.index - 1)

    /** Idempotent: the button, the dialog and the timer may all call it. */
    fun submit() {
        val s = _state.value
        if (s.submitting || s.finishedSessionId != null || s.questions.isEmpty()) return
        _state.update { it.copy(submitting = true) }
        val t = clock()
        spentMs[s.index] = (spentMs[s.index] ?: 0) + (t - shownAt)
        val answeredAt = now()
        val attempts = s.questions.mapIndexed { i, q ->
            val chosen = s.answers[i]
            AttemptEntity(
                sessionId = sessionId,
                questionId = q.id,
                chosen = chosen?.let(::choiceLetter),
                isCorrect = chosen == q.answerIndex,
                answeredAt = answeredAt,
                timeMs = spentMs[i] ?: 0,
            )
        }
        val correct = attempts.count { it.isCorrect }
        val duration = answeredAt - startedAt
        viewModelScope.launch {
            container.progress.recordAttempts(attempts)
            container.progress.finishSession(sessionId, correct, duration)
            _state.update { it.copy(finishedSessionId = sessionId) }
        }
    }
}
