package com.example.civora.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.civora.AppContainer
import com.example.civora.data.db.Accuracy
import com.example.civora.data.db.SessionEntity
import com.example.civora.data.db.SubjectAccuracy
import com.example.civora.data.db.TopicAccuracy
import com.example.civora.domain.Streak
import com.example.civora.domain.computeStreak
import com.example.civora.domain.weakestTopics
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class StatsUiState(
    val loaded: Boolean = false,
    val overall: Accuracy = Accuracy(0, 0),
    val streak: Streak = Streak(0, 0),
    val subjects: List<SubjectAccuracy> = emptyList(),
    val weakest: List<TopicAccuracy> = emptyList(),
    val history: List<SessionEntity> = emptyList(),
)

class StatsViewModel(container: AppContainer) : ViewModel() {
    private val progress = container.progress

    val state: StateFlow<StatsUiState> = combine(
        progress.overall,
        progress.bySubject,
        progress.byTopic,
        progress.activeDays,
        progress.history,
    ) { overall, subjects, topics, days, history ->
        StatsUiState(
            loaded = true,
            overall = overall,
            streak = computeStreak(days, LocalDate.now()),
            subjects = subjects,
            weakest = weakestTopics(topics),
            history = history.take(HISTORY_LIMIT),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    private companion object {
        const val HISTORY_LIMIT = 20
    }
}
