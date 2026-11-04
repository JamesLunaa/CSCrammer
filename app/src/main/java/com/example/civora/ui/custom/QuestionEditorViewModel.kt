package com.example.civora.ui.custom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.civora.AppContainer
import com.example.civora.domain.DEFAULT_CUSTOM_TOPIC
import com.example.civora.domain.DraftField
import com.example.civora.domain.QuestionDraft
import com.example.civora.domain.model.Difficulty
import com.example.civora.domain.model.Subject
import com.example.civora.domain.validate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val draft: QuestionDraft = QuestionDraft(),
    /** Errors only show after the first save attempt, so a blank form isn't covered in red. */
    val issues: Map<DraftField, String> = emptyMap(),
    val topicSuggestions: List<String> = emptyList(),
    val dirty: Boolean = false,
    val saving: Boolean = false,
    /** Saved or deleted; the screen should close. */
    val done: Boolean = false,
)

private data class Form(
    val loading: Boolean,
    val draft: QuestionDraft,
    val original: QuestionDraft,
    val showIssues: Boolean = false,
    val saving: Boolean = false,
    val done: Boolean = false,
)

/** Adds a custom question, or edits the one with [questionId]. */
class QuestionEditorViewModel(private val container: AppContainer, private val questionId: Int?) : ViewModel() {
    private val form = MutableStateFlow(Form(loading = questionId != null, draft = QuestionDraft(), original = QuestionDraft()))

    val state: StateFlow<EditorUiState> = combine(form, container.questions.poolCounts) { f, pool ->
        val topics = pool.filter { it.subject == f.draft.subject.label }.map { it.topic }
        EditorUiState(
            loading = f.loading,
            isNew = questionId == null,
            draft = f.draft,
            issues = if (f.showIssues) f.draft.validate() else emptyMap(),
            topicSuggestions = (listOf(DEFAULT_CUSTOM_TOPIC) + topics.sorted()).distinct(),
            dirty = f.draft != f.original,
            saving = f.saving,
            done = f.done,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditorUiState(loading = questionId != null))

    init {
        if (questionId != null) {
            viewModelScope.launch {
                val q = container.questions.question(questionId)
                if (q == null) {
                    form.update { it.copy(loading = false, done = true) }
                } else {
                    val draft = QuestionDraft(q.subject, q.topic, q.difficulty, q.text, q.choices, q.answerIndex, q.explanation)
                    form.value = Form(loading = false, draft = draft, original = draft)
                }
            }
        }
    }

    private fun edit(change: QuestionDraft.() -> QuestionDraft) = form.update { it.copy(draft = it.draft.change()) }

    fun setSubject(subject: Subject) = edit { copy(subject = subject) }
    fun setTopic(topic: String) = edit { copy(topic = topic) }
    fun setDifficulty(difficulty: Difficulty) = edit { copy(difficulty = difficulty) }
    fun setText(text: String) = edit { copy(text = text) }
    fun setChoice(index: Int, text: String) = edit { copy(choices = choices.toMutableList().also { it[index] = text }) }
    fun setAnswer(index: Int) = edit { copy(answerIndex = index) }
    fun setExplanation(text: String) = edit { copy(explanation = text) }

    /** Returns false (and turns on error display) when the draft isn't valid yet. */
    fun save(): Boolean {
        val f = form.value
        if (f.saving) return true
        if (f.draft.validate().isNotEmpty()) {
            form.update { it.copy(showIssues = true) }
            return false
        }
        form.update { it.copy(saving = true) }
        viewModelScope.launch {
            container.questions.saveCustom(f.draft, questionId)
            form.update { it.copy(saving = false, done = true) }
        }
        return true
    }

    fun delete() {
        val id = questionId ?: return
        viewModelScope.launch {
            container.questions.deleteCustom(id)
            form.update { it.copy(done = true) }
        }
    }
}
