package com.example.civora.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.civora.AppContainer
import com.example.civora.data.db.PoolCount
import com.example.civora.domain.model.Difficulty
import com.example.civora.domain.model.Subject
import com.example.civora.ui.quiz.QuizSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class PracticeSetupState(
    val ready: Boolean = false,
    val subject: Subject = Subject.Verbal,
    val topics: List<String> = emptyList(),
    val selectedTopics: Set<String> = emptySet(),
    val difficulties: Set<Difficulty> = Difficulty.entries.toSet(),
    /** 0 means all matching questions. */
    val count: Int = 10,
    val available: Int = 0,
) {
    val startCount: Int get() = if (count == 0) available else minOf(count, available)
    val canStart: Boolean get() = available > 0

    fun toSource() = QuizSource.Practice(
        subject = subject,
        topics = topics.filter { it in selectedTopics },
        difficulties = Difficulty.entries.filter { it in difficulties },
        count = count,
    )
}

private data class Selection(
    val subject: Subject = Subject.Verbal,
    /** Topics the user switched off; new subject starts with none off. */
    val deselected: Set<String> = emptySet(),
    val difficulties: Set<Difficulty> = Difficulty.entries.toSet(),
    val count: Int = 10,
)

class PracticeSetupViewModel(container: AppContainer) : ViewModel() {
    private val selection = MutableStateFlow(Selection())

    val state: StateFlow<PracticeSetupState> = combine(container.questions.poolCounts, selection) { pool, sel ->
        val forSubject = pool.filter { it.subject == sel.subject.label }
        val topics = forSubject.map { it.topic }.distinct().sorted()
        val selected = topics.filter { it !in sel.deselected }.toSet()
        PracticeSetupState(
            ready = pool.isNotEmpty(),
            subject = sel.subject,
            topics = topics,
            selectedTopics = selected,
            difficulties = sel.difficulties,
            count = sel.count,
            available = forSubject.matching(selected, sel.difficulties),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PracticeSetupState())

    fun setSubject(subject: Subject) = selection.update { it.copy(subject = subject, deselected = emptySet()) }

    fun toggleTopic(topic: String) = selection.update {
        it.copy(deselected = if (topic in it.deselected) it.deselected - topic else it.deselected + topic)
    }

    fun setAllTopics(all: Boolean) {
        val topics = state.value.topics.toSet()
        selection.update { it.copy(deselected = if (all) emptySet() else topics) }
    }

    fun toggleDifficulty(d: Difficulty) = selection.update {
        it.copy(difficulties = if (d in it.difficulties) it.difficulties - d else it.difficulties + d)
    }

    fun setCount(count: Int) = selection.update { it.copy(count = count) }
}

private fun List<PoolCount>.matching(topics: Set<String>, difficulties: Set<Difficulty>): Int =
    filter { it.topic in topics && it.difficulty in difficulties.map(Difficulty::name) }.sumOf { it.count }
