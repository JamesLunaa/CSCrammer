package com.example.civora.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.civora.data.db.SessionEntity
import com.example.civora.data.db.SubjectAccuracy
import com.example.civora.data.db.TopicAccuracy
import com.example.civora.domain.Scoring
import com.example.civora.domain.isMockMode
import com.example.civora.domain.model.Subject
import com.example.civora.ui.theme.CivoraTheme
import com.example.civora.ui.theme.HeroNumber
import com.example.civora.ui.theme.TotalNumber
import com.example.civora.ui.theme.subjectColor
import java.text.DateFormat
import java.util.Date

@Composable
fun StatsScreen(viewModel: StatsViewModel, onBack: () -> Unit, onOpenSession: (Long) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val extras = CivoraTheme.colors
    val answered = state.overall.answered

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(extras.field)
                .statusBarsPadding()
                .padding(start = 4.dp, end = 24.dp, bottom = 24.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = extras.onField)
            }
            Column(Modifier.padding(start = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Progress", style = MaterialTheme.typography.titleMedium, color = extras.fieldLilac)
                Text(
                    if (answered > 0) "${Scoring.percent(state.overall.correct, answered)}%" else "—",
                    style = HeroNumber,
                    color = extras.onField,
                )
                Text(
                    if (answered > 0) "$answered answered · ${state.overall.correct} correct" else "Answer a few questions to see your stats.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = extras.fieldLilac,
                )
                Text(
                    "Streak ${state.streak.current} ${if (state.streak.current == 1) "day" else "days"} · best ${state.streak.best}",
                    style = TotalNumber.copy(fontSize = 18.sp),
                    color = extras.onField,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        Column(
            Modifier.navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            if (state.subjects.isNotEmpty()) SubjectBars(state.subjects)
            if (state.weakest.isNotEmpty()) Weakest(state.weakest)
            if (state.history.isNotEmpty()) History(state.history, onOpenSession)
        }
    }
}

@Composable
private fun SubjectBars(rows: List<SubjectAccuracy>) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("By subject", style = MaterialTheme.typography.titleLarge)
        Subject.entries.forEach { subject ->
            val row = rows.firstOrNull { it.subject == subject.label }
            val tint = subjectColor(subject)
            val percent = row?.let { Scoring.percent(it.correct, it.answered) }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(10.dp).background(tint, CircleShape))
                    Text(subject.shortLabel, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(
                        if (percent != null) "$percent% · ${row.answered}" else "Not started",
                        style = TotalNumber.copy(fontSize = 18.sp),
                        color = if (percent != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LinearProgressIndicator(
                    progress = { if (row == null) 0f else row.correct / row.answered.toFloat() },
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
private fun Weakest(rows: List<TopicAccuracy>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Weakest topics", style = MaterialTheme.typography.titleLarge)
        rows.forEach { row ->
            val subject = Subject.entries.firstOrNull { it.label == row.subject }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (subject != null) Box(Modifier.size(10.dp).background(subjectColor(subject), CircleShape))
                Column(Modifier.weight(1f)) {
                    Text(row.topic, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${subject?.shortLabel.orEmpty()} · ${row.answered} answered",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text("${Scoring.percent(row.correct, row.answered)}%", style = TotalNumber.copy(fontSize = 18.sp))
            }
        }
    }
}

private fun sessionLabel(mode: String): String = when {
    mode == "quick" -> "Quick review"
    mode == "practice" -> "Practice"
    mode == "mistakes" -> "Mistakes drill"
    mode == "bookmarks" -> "Bookmarks drill"
    isMockMode(mode) -> if (mode.endsWith("mini")) "Mini mock" else "Full mock"
    else -> "Session"
}

@Composable
private fun History(sessions: List<SessionEntity>, onOpen: (Long) -> Unit) {
    val fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Recent sessions", style = MaterialTheme.typography.titleLarge)
        sessions.forEach { s ->
            val shape = RoundedCornerShape(14.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape)
                    .clickable(role = Role.Button, onClickLabel = "Open results") { onOpen(s.id) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(sessionLabel(s.mode), style = MaterialTheme.typography.titleMedium)
                    Text(
                        fmt.format(Date(s.startedAt)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text("${s.correct} / ${s.total}", style = TotalNumber.copy(fontSize = 18.sp))
            }
        }
    }
}
