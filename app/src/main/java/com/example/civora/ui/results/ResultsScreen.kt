package com.example.civora.ui.results

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.civora.domain.ResultItem
import com.example.civora.domain.Scoring
import com.example.civora.domain.SubjectScore
import com.example.civora.domain.isMockMode
import com.example.civora.domain.model.choiceLetter
import com.example.civora.domain.subjectBreakdown
import com.example.civora.ui.theme.CivoraTheme
import com.example.civora.ui.theme.HeroNumber
import com.example.civora.ui.theme.TotalNumber
import com.example.civora.ui.theme.subjectColor
import java.util.concurrent.TimeUnit

@Composable
fun ResultsScreen(viewModel: ResultsViewModel, onDone: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val session = state.session
    var wrongOnly by rememberSaveable { mutableStateOf(false) }
    var expandedId by rememberSaveable { mutableStateOf<Int?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Back to home", style = MaterialTheme.typography.labelLarge) }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading || session == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                val shown = if (wrongOnly) state.items.filterNot { it.isCorrect } else state.items
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { ScoreBand(session.mode, session.correct, session.total, session.durationMs) }
                    item { Breakdown(subjectBreakdown(state.items)) }
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Review answers", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                            FilterChip(
                                selected = wrongOnly,
                                onClick = { wrongOnly = !wrongOnly },
                                label = { Text("Wrong only") },
                            )
                        }
                    }
                    if (shown.isEmpty()) {
                        item {
                            Text(
                                "Nothing wrong. Clean sheet.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                    items(shown, key = { it.question.id }) { item ->
                        AnswerRow(
                            item = item,
                            number = state.items.indexOf(item) + 1,
                            expanded = expandedId == item.question.id,
                            onToggle = { expandedId = if (expandedId == item.question.id) null else item.question.id },
                        )
                    }
                    item { Box(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

private fun modeLabel(mode: String): String = when {
    mode == "quick" -> "Quick review"
    mode == "practice" -> "Practice"
    mode == "mistakes" -> "Mistakes drill"
    mode == "bookmarks" -> "Bookmarks drill"
    isMockMode(mode) -> if (mode.endsWith("mini")) "Mini mock exam" else "Full mock exam"
    else -> "Session"
}

@Composable
private fun ScoreBand(mode: String, correct: Int, total: Int, durationMs: Long) {
    val extras = CivoraTheme.colors
    val mock = isMockMode(mode)
    val percent = Scoring.percent(correct, total)
    Column(
        Modifier
            .fillMaxWidth()
            .background(extras.field)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(modeLabel(mode), style = MaterialTheme.typography.titleMedium, color = extras.fieldLilac)
        Text(
            if (mock) "$percent%" else "$correct / $total",
            style = HeroNumber,
            color = extras.onField,
        )
        if (mock) {
            val passed = Scoring.passed(correct, total)
            Text(
                if (passed) "Passed" else "Below the ${Scoring.PASS_PERCENT}% pass mark",
                style = MaterialTheme.typography.titleLarge,
                color = if (passed) extras.fieldCorrect else extras.fieldWrong,
            )
            Text("$correct of $total correct", style = MaterialTheme.typography.bodyLarge, color = extras.fieldLilac)
        } else {
            Text("$percent% correct", style = MaterialTheme.typography.bodyLarge, color = extras.fieldLilac)
        }
        Text(formatDuration(durationMs), style = TotalNumber.copy(fontSize = 18.sp), color = extras.fieldLilac)
    }
}

private fun formatDuration(ms: Long): String {
    val h = TimeUnit.MILLISECONDS.toHours(ms)
    val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

@Composable
private fun Breakdown(scores: List<SubjectScore>) {
    if (scores.size < 2) return
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("By subject", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        scores.forEach { score ->
            val tint = subjectColor(score.subject)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(10.dp).background(tint, CircleShape))
                    Text(
                        score.subject.shortLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text("${score.correct} / ${score.total}", style = TotalNumber.copy(fontSize = 18.sp))
                }
                LinearProgressIndicator(
                    progress = { score.correct / score.total.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = tint,
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
            }
        }
    }
}

@Composable
private fun AnswerRow(item: ResultItem, number: Int, expanded: Boolean, onToggle: () -> Unit) {
    val extras = CivoraTheme.colors
    val q = item.question
    val tint = if (item.isCorrect) extras.correct else extras.wrong
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape)
            .clickable(role = Role.Button, onClickLabel = if (expanded) "Collapse" else "Show explanation", onClick = onToggle)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Text("$number", style = TotalNumber.copy(fontSize = 18.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                q.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                modifier = Modifier.weight(1f),
            )
        }
        val yours = item.chosen?.let { choiceLetter(it) } ?: "none"
        Text(
            if (item.isCorrect) "Correct · $yours" else "Wrong · you chose $yours · answer ${choiceLetter(q.answerIndex)}",
            style = MaterialTheme.typography.labelMedium,
            color = tint,
        )
        if (expanded) {
            q.choices.forEachIndexed { i, text ->
                val mark = when (i) {
                    q.answerIndex -> extras.correct
                    item.chosen -> extras.wrong
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text("${choiceLetter(i)}.  $text", style = MaterialTheme.typography.bodyMedium, color = mark)
            }
            Text(q.explanation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
