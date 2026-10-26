package com.example.civora.ui.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.civora.domain.model.Question
import com.example.civora.domain.model.choiceLetter
import com.example.civora.ui.theme.CivoraTheme
import com.example.civora.ui.theme.TotalNumber
import com.example.civora.ui.theme.subjectColor

@Composable
fun QuizScreen(viewModel: QuizViewModel, onExit: () -> Unit, onFinished: (Long) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val question = state.current

    state.finishedSessionId?.let { id -> LaunchedEffect(id) { onFinished(id) } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (question != null && !state.finished) {
                QuizTopBar(
                    question = question,
                    position = state.index + 1,
                    total = state.questions.size,
                    bookmarked = question.id in state.bookmarks,
                    onExit = onExit,
                    onBookmark = viewModel::toggleBookmark,
                )
            }
        },
        bottomBar = {
            if (question != null && !state.finished) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    // The single lamp action for this state.
                    Button(
                        onClick = if (state.revealed) viewModel::next else viewModel::check,
                        enabled = state.revealed || state.selected != null,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(
                            when {
                                !state.revealed -> "Check answer"
                                state.isLast -> "Finish"
                                else -> "Next"
                            },
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.questions.isEmpty() -> Message("No questions available.", "Back", onExit)
                state.finished -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                question != null -> QuestionBody(
                    question = question,
                    selected = state.selected,
                    revealed = state.revealed,
                    onSelect = viewModel::select,
                )
            }
        }
    }
}

@Composable
private fun QuizTopBar(
    question: Question,
    position: Int,
    total: Int,
    bookmarked: Boolean,
    onExit: () -> Unit,
    onBookmark: () -> Unit,
) {
    val subjectTint = subjectColor(question.subject)
    Column(Modifier.statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onExit) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Leave quiz")
            }
            Text(
                "$position / $total",
                style = TotalNumber,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onBookmark) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = if (bookmarked) "Remove bookmark" else "Bookmark question",
                    tint = if (bookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                )
            }
        }
        LinearProgressIndicator(
            progress = { position / total.toFloat() },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(4.dp),
            color = subjectTint,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun QuestionBody(
    question: Question,
    selected: Int?,
    revealed: Boolean,
    onSelect: (Int) -> Unit,
) {
    val scroll = rememberScrollState()
    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(10.dp).background(subjectColor(question.subject), CircleShape))
            Text(
                "${question.subject.shortLabel} · ${question.topic} · ${question.difficulty.name}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(question.text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            question.choices.forEachIndexed { i, text ->
                ChoiceRow(
                    index = i,
                    text = text,
                    state = choiceState(i, selected, revealed, question.answerIndex),
                    enabled = !revealed,
                    onClick = { onSelect(i) },
                )
            }
        }
        if (revealed) {
            Feedback(question, correct = selected == question.answerIndex)
        }
    }
}

private enum class ChoiceState { Idle, Selected, Correct, Wrong, Dimmed }

private fun choiceState(index: Int, selected: Int?, revealed: Boolean, answer: Int): ChoiceState = when {
    !revealed -> if (index == selected) ChoiceState.Selected else ChoiceState.Idle
    index == answer -> ChoiceState.Correct
    index == selected -> ChoiceState.Wrong
    else -> ChoiceState.Dimmed
}

@Composable
private fun ChoiceRow(index: Int, text: String, state: ChoiceState, enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val extras = CivoraTheme.colors
    val accent = when (state) {
        ChoiceState.Selected -> scheme.primary
        ChoiceState.Correct -> extras.correct
        ChoiceState.Wrong -> extras.wrong
        else -> scheme.outlineVariant
    }
    val container = when (state) {
        ChoiceState.Selected -> scheme.primaryContainer
        ChoiceState.Correct -> extras.correct.copy(alpha = 0.14f)
        ChoiceState.Wrong -> extras.wrong.copy(alpha = 0.14f)
        else -> scheme.surfaceContainerHigh
    }
    val alpha = if (state == ChoiceState.Dimmed) 0.55f else 1f
    val status = when (state) {
        ChoiceState.Correct -> "Correct answer"
        ChoiceState.Wrong -> "Your answer, incorrect"
        else -> null
    }
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .background(container, shape)
            .border(BorderStroke(if (state == ChoiceState.Idle || state == ChoiceState.Dimmed) 1.dp else 2.dp, accent), shape)
            .selectable(
                selected = state == ChoiceState.Selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics { status?.let { stateDescription = it } }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            choiceLetter(index),
            style = TotalNumber.copy(fontSize = 20.sp),
            color = if (state == ChoiceState.Idle || state == ChoiceState.Dimmed) scheme.onSurfaceVariant else accent,
            modifier = Modifier.padding(top = 1.dp),
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurface.copy(alpha = alpha),
            modifier = Modifier.weight(1f),
        )
        if (status != null) {
            Text(
                if (state == ChoiceState.Correct) "Correct" else "Wrong",
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun Feedback(question: Question, correct: Boolean) {
    val extras = CivoraTheme.colors
    val tint = if (correct) extras.correct else extras.wrong
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            if (correct) "Correct" else "Not quite · answer is ${choiceLetter(question.answerIndex)}",
            style = MaterialTheme.typography.titleMedium,
            color = tint,
        )
        Text(
            question.explanation,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun Message(text: String, action: String, onAction: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.height(16.dp))
        Button(onClick = onAction) { Text(action) }
    }
}
