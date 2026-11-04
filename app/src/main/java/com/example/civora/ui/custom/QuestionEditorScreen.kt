package com.example.civora.ui.custom

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.civora.domain.DraftField
import com.example.civora.domain.model.Difficulty
import com.example.civora.domain.model.Subject
import com.example.civora.domain.model.choiceLetter
import com.example.civora.domain.validate
import com.example.civora.ui.theme.subjectColor

private val Sentences = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)

@Composable
fun QuestionEditorScreen(viewModel: QuestionEditorViewModel, onClose: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val draft = state.draft
    val issues = state.issues
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    // Where each field sits, so a failed save can scroll to the first problem.
    var viewport by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val anchors = remember { mutableStateMapOf<DraftField, LayoutCoordinates>() }
    fun Modifier.anchor(field: DraftField) = onGloballyPositioned { anchors[field] = it }
    val save = {
        val first = draft.validate().keys.firstOrNull()
        if (!viewModel.save() && first != null) {
            val target = anchors[first]
            val top = viewport
            if (target != null && top != null && target.isAttached && top.isAttached) {
                val offset = target.positionInRoot().y - top.positionInRoot().y
                scope.launch { scroll.animateScrollTo((scroll.value + offset - 120).toInt().coerceAtLeast(0)) }
            }
        }
    }

    LaunchedEffect(state.done) { if (state.done) onClose() }
    val back = { if (state.dirty) { confirmDiscard = true } else { onClose() } }
    BackHandler(enabled = state.dirty && !state.done) { confirmDiscard = true }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Row(Modifier.statusBarsPadding().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = back) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    if (state.isNew) "New question" else "Edit question",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                if (!state.isNew) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete question")
                    }
                }
            }
        },
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (issues.isNotEmpty()) {
                    Text(
                        "Fix the highlighted fields to save.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Button(
                    onClick = save,
                    enabled = !state.loading && !state.saving,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(if (state.isNew) "Add question" else "Save changes", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
    ) { padding ->
        if (state.loading) return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .onGloballyPositioned { viewport = it }
                .verticalScroll(scroll)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Section("Subject") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Subject.entries.forEach { subject ->
                        FilterChip(
                            selected = subject == draft.subject,
                            onClick = { viewModel.setSubject(subject) },
                            label = { Text(subject.shortLabel) },
                            leadingIcon = { Box(Modifier.size(10.dp).background(subjectColor(subject), CircleShape)) },
                        )
                    }
                }
            }
            Section("Topic") {
                OutlinedTextField(
                    value = draft.topic,
                    onValueChange = viewModel::setTopic,
                    label = { Text("Topic") },
                    singleLine = true,
                    isError = DraftField.Topic in issues,
                    supportingText = issues[DraftField.Topic]?.let { { Text(it) } },
                    keyboardOptions = Sentences,
                    modifier = Modifier.fillMaxWidth().anchor(DraftField.Topic),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.topicSuggestions.forEach { topic ->
                        FilterChip(
                            selected = draft.topic.trim() == topic,
                            onClick = { viewModel.setTopic(topic) },
                            label = { Text(topic) },
                        )
                    }
                }
            }
            Section("Difficulty") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Difficulty.entries.forEach { d ->
                        FilterChip(
                            selected = d == draft.difficulty,
                            onClick = { viewModel.setDifficulty(d) },
                            label = { Text(d.name) },
                        )
                    }
                }
            }
            Section("Question") {
                OutlinedTextField(
                    value = draft.text,
                    onValueChange = viewModel::setText,
                    label = { Text("Question") },
                    minLines = 3,
                    isError = DraftField.Text in issues,
                    supportingText = issues[DraftField.Text]?.let { { Text(it) } },
                    keyboardOptions = Sentences,
                    modifier = Modifier.fillMaxWidth().anchor(DraftField.Text),
                )
            }
            Section("Choices") {
                Text(
                    "Tap the circle next to the correct answer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = issues[DraftField.Answer]?.let { MaterialTheme.colorScheme.error }
                        ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.anchor(DraftField.Answer),
                )
                draft.choices.forEachIndexed { i, choice ->
                    val letter = choiceLetter(i)
                    val field = DraftField.Choice(i)
                    Row(verticalAlignment = Alignment.Top) {
                        RadioButton(
                            selected = draft.answerIndex == i,
                            onClick = { viewModel.setAnswer(i) },
                            modifier = Modifier.padding(top = 8.dp).semantics { contentDescription = "Mark $letter as the correct answer" },
                        )
                        OutlinedTextField(
                            value = choice,
                            onValueChange = { viewModel.setChoice(i, it) },
                            label = { Text(if (draft.answerIndex == i) "Choice $letter · correct" else "Choice $letter") },
                            isError = field in issues,
                            supportingText = issues[field]?.let { { Text(it) } },
                            keyboardOptions = Sentences,
                            modifier = Modifier.weight(1f).anchor(field),
                        )
                    }
                }
                issues[DraftField.Answer]?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            }
            Section("Explanation") {
                OutlinedTextField(
                    value = draft.explanation,
                    onValueChange = viewModel::setExplanation,
                    label = { Text("Explanation") },
                    minLines = 3,
                    supportingText = { Text("Optional. Shown after you answer.") },
                    keyboardOptions = Sentences,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard changes?") },
            text = { Text("What you typed on this question will be lost.") },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; onClose() }) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this question?") },
            text = { Text("It leaves your question list, practice pool and bookmarks. This can't be undone.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.delete() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}
