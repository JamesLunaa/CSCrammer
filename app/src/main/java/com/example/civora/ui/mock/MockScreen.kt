package com.example.civora.ui.mock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.civora.domain.model.Question
import com.example.civora.domain.model.choiceLetter
import com.example.civora.ui.theme.Category
import com.example.civora.ui.theme.CivoraTheme
import com.example.civora.ui.theme.TotalNumber
import com.example.civora.ui.theme.categoryColor
import com.example.civora.ui.theme.subjectColor
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockScreen(viewModel: MockViewModel, onExit: () -> Unit, onFinished: (Long) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val question = state.current
    var showGrid by rememberSaveable { mutableStateOf(false) }
    var confirmSubmit by rememberSaveable { mutableStateOf(false) }
    var confirmLeave by rememberSaveable { mutableStateOf(false) }

    state.finishedSessionId?.let { id -> LaunchedEffect(id) { onFinished(id) } }
    BackHandler(enabled = question != null && state.finishedSessionId == null) { confirmLeave = true }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (question != null) {
                MockTopBar(
                    position = state.index + 1,
                    total = state.questions.size,
                    remainingMs = state.remainingMs,
                    lowTime = state.lowTime,
                    subject = question,
                    onLeave = { confirmLeave = true },
                )
            }
        },
        bottomBar = {
            if (question != null) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilledTonalButton(
                            onClick = viewModel::previous,
                            enabled = state.index > 0,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) { Text("Previous") }
                        OutlinedButton(
                            onClick = { showGrid = true },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) { Text("${state.answers.size} / ${state.questions.size}", style = TotalNumber.copy(fontSize = 18.sp)) }
                    }
                    // The single lamp action: advance, or submit on the last item.
                    Button(
                        onClick = if (state.isLast) ({ confirmSubmit = true }) else viewModel::next,
                        enabled = !state.submitting,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text(if (state.isLast) "Submit exam" else "Next", style = MaterialTheme.typography.labelLarge) }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading || state.submitting -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                question == null -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("No questions available.", style = MaterialTheme.typography.bodyLarge)
                    Box(Modifier.height(16.dp))
                    Button(onClick = onExit) { Text("Back") }
                }
                else -> MockQuestion(
                    question = question,
                    selected = state.answers[state.index],
                    flagged = state.index in state.flagged,
                    onSelect = viewModel::select,
                    onClear = viewModel::clearAnswer,
                    onFlag = viewModel::toggleFlag,
                )
            }
        }
    }

    if (showGrid) {
        ModalBottomSheet(onDismissRequest = { showGrid = false }, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
            Navigator(
                state = state,
                onPick = { viewModel.goTo(it); showGrid = false },
                onSubmit = { showGrid = false; confirmSubmit = true },
            )
        }
    }
    if (confirmSubmit) {
        AlertDialog(
            onDismissRequest = { confirmSubmit = false },
            title = { Text("Submit exam?") },
            text = {
                Text(
                    buildString {
                        append(if (state.unanswered == 0) "All items answered." else "${state.unanswered} unanswered, counted as wrong.")
                        if (state.flagged.isNotEmpty()) append(" ${state.flagged.size} flagged for review.")
                    },
                )
            },
            confirmButton = { TextButton(onClick = { confirmSubmit = false; viewModel.submit() }) { Text("Submit") } },
            dismissButton = { TextButton(onClick = { confirmSubmit = false }) { Text("Keep going") } },
        )
    }
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Leave the exam?") },
            text = { Text("Your answers will be lost and nothing is recorded.") },
            confirmButton = { TextButton(onClick = { confirmLeave = false; onExit() }) { Text("Leave") } },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Stay") } },
        )
    }
}

@Composable
private fun MockTopBar(
    position: Int,
    total: Int,
    remainingMs: Long,
    lowTime: Boolean,
    subject: Question,
    onLeave: () -> Unit,
) {
    Column(Modifier.statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onLeave) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Leave exam")
            }
            Text("$position / $total", style = TotalNumber, modifier = Modifier.weight(1f))
            Text(
                formatClock(remainingMs),
                style = TotalNumber,
                color = if (lowTime) CivoraTheme.colors.warning else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .semantics { contentDescription = "Time left ${formatClock(remainingMs)}" },
            )
        }
        LinearProgressIndicator(
            progress = { position / total.toFloat() },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(4.dp),
            color = subjectColor(subject.subject),
            trackColor = MaterialTheme.colorScheme.outlineVariant,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

private fun formatClock(ms: Long): String {
    val h = TimeUnit.MILLISECONDS.toHours(ms)
    val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return "%d:%02d:%02d".format(h, m, s)
}

@Composable
private fun MockQuestion(
    question: Question,
    selected: Int?,
    flagged: Boolean,
    onSelect: (Int) -> Unit,
    onClear: () -> Unit,
    onFlag: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(10.dp).background(subjectColor(question.subject), CircleShape))
            Text(
                "${question.subject.shortLabel} · ${question.topic}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onFlag) { Text(if (flagged) "Flagged" else "Flag for later") }
        }
        Text(question.text, style = MaterialTheme.typography.bodyLarge)
        question.choices.forEachIndexed { i, text ->
            val on = i == selected
            val scheme = MaterialTheme.colorScheme
            val shape = RoundedCornerShape(14.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (on) scheme.primaryContainer else scheme.surfaceContainerHigh, shape)
                    .border(BorderStroke(if (on) 2.dp else 1.dp, if (on) scheme.primary else scheme.outlineVariant), shape)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(i) })
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    choiceLetter(i),
                    style = TotalNumber.copy(fontSize = 20.sp),
                    color = if (on) scheme.primary else scheme.onSurfaceVariant,
                )
                Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            }
        }
        if (selected != null) {
            TextButton(onClick = onClear) { Text("Clear answer") }
        }
    }
}

@Composable
private fun Navigator(state: MockUiState, onPick: (Int) -> Unit, onSubmit: () -> Unit) {
    val flagTint = categoryColor(Category.Blue)
    Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "${state.answers.size} answered · ${state.unanswered} left · ${state.flagged.size} flagged",
            style = MaterialTheme.typography.titleMedium,
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(52.dp),
            modifier = Modifier.weight(1f, fill = false).height(320.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(state.questions) { i, q ->
                val answered = i in state.answers
                val flagged = i in state.flagged
                val current = i == state.index
                val scheme = MaterialTheme.colorScheme
                val shape = RoundedCornerShape(10.dp)
                val description = buildString {
                    append("Question ${i + 1}, ${q.subject.shortLabel}, ")
                    append(if (answered) "answered" else "unanswered")
                    if (flagged) append(", flagged")
                    if (current) append(", current")
                }
                Box(
                    Modifier
                        .aspectRatio(1f)
                        .background(if (answered) scheme.primaryContainer else scheme.surfaceContainerHigh, shape)
                        .border(
                            BorderStroke(if (current) 2.dp else 1.dp, if (current) scheme.primary else scheme.outlineVariant),
                            shape,
                        )
                        .clickable(onClickLabel = "Go to question") { onPick(i) }
                        .semantics { contentDescription = description },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${i + 1}", style = TotalNumber.copy(fontSize = 18.sp), textAlign = TextAlign.Center)
                    if (flagged) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(8.dp)
                                .background(flagTint, CircleShape),
                        )
                    }
                }
            }
        }
        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(14.dp),
        ) { Text("Submit exam", style = MaterialTheme.typography.labelLarge) }
    }
}
