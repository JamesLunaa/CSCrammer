package com.example.civora.ui.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.civora.domain.model.Difficulty
import com.example.civora.domain.model.Subject
import com.example.civora.ui.quiz.QuizSource
import com.example.civora.ui.theme.TotalNumber
import com.example.civora.ui.theme.subjectColor

private val Counts = listOf(10, 20, 50, 0)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PracticeSetupScreen(viewModel: PracticeSetupViewModel, onBack: () -> Unit, onStart: (QuizSource.Practice) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Row(Modifier.statusBarsPadding().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Practice", style = MaterialTheme.typography.titleLarge)
            }
        },
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    when {
                        !state.ready -> "Preparing questions…"
                        state.canStart -> "${state.available} questions match"
                        else -> "No questions match these filters"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { onStart(state.toSource()) },
                    enabled = state.canStart,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        if (state.canStart) "Start · ${state.startCount} questions" else "Start",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Section("Subject") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Subject.entries.forEach { subject ->
                        FilterChip(
                            selected = subject == state.subject,
                            onClick = { viewModel.setSubject(subject) },
                            label = { Text(subject.shortLabel) },
                            leadingIcon = {
                                Box(Modifier.size(10.dp).background(subjectColor(subject), CircleShape))
                            },
                        )
                    }
                }
            }
            Section("Topics") {
                if (state.topics.isNotEmpty()) {
                    val allOn = state.selectedTopics.size == state.topics.size
                    TextButton(onClick = { viewModel.setAllTopics(!allOn) }) {
                        Text(if (allOn) "Clear all" else "Select all")
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.topics.forEach { topic ->
                        FilterChip(
                            selected = topic in state.selectedTopics,
                            onClick = { viewModel.toggleTopic(topic) },
                            label = { Text(topic) },
                        )
                    }
                }
            }
            Section("Difficulty") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Difficulty.entries.forEach { d ->
                        FilterChip(
                            selected = d in state.difficulties,
                            onClick = { viewModel.toggleDifficulty(d) },
                            label = { Text(d.name) },
                        )
                    }
                }
            }
            Section("Questions") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Counts.forEach { n ->
                        FilterChip(
                            selected = state.count == n,
                            onClick = { viewModel.setCount(n) },
                            label = { Text(if (n == 0) "All" else "$n", style = TotalNumber.copy(fontSize = 16.sp)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}
