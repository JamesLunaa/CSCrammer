package com.example.civora.domain

import com.example.civora.domain.model.Difficulty
import com.example.civora.domain.model.Subject

const val DEFAULT_CUSTOM_TOPIC = "My questions"

/** A user-written question as typed into the editor, before it is saved. */
data class QuestionDraft(
    val subject: Subject = Subject.Verbal,
    val topic: String = DEFAULT_CUSTOM_TOPIC,
    val difficulty: Difficulty = Difficulty.Medium,
    val text: String = "",
    val choices: List<String> = List(4) { "" },
    val answerIndex: Int? = null,
    val explanation: String = "",
) {
    fun trimmed() = copy(
        topic = topic.trim(),
        text = text.trim(),
        choices = choices.map { it.trim() },
        explanation = explanation.trim(),
    )
}

sealed interface DraftField {
    data object Topic : DraftField
    data object Text : DraftField
    data class Choice(val index: Int) : DraftField
    data object Answer : DraftField
}

/** Problems keyed by field, with the message to show under it. Empty means the draft can be saved. */
fun QuestionDraft.validate(): Map<DraftField, String> {
    val d = trimmed()
    val issues = linkedMapOf<DraftField, String>()
    if (d.topic.isEmpty()) issues[DraftField.Topic] = "Add a topic."
    if (d.text.isEmpty()) issues[DraftField.Text] = "Write the question."
    d.choices.forEachIndexed { i, choice ->
        when {
            choice.isEmpty() -> issues[DraftField.Choice(i)] = "Fill in this choice."
            d.choices.take(i).any { it.equals(choice, ignoreCase = true) } ->
                issues[DraftField.Choice(i)] = "Same as another choice."
        }
    }
    if (d.answerIndex == null) issues[DraftField.Answer] = "Mark the correct answer."
    return issues
}
