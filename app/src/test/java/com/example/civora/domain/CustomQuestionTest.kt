package com.example.civora.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomQuestionTest {
    private val valid = QuestionDraft(
        text = "What is 2 + 2?",
        choices = listOf("3", "4", "5", "6"),
        answerIndex = 1,
        explanation = "Basic addition.",
    )

    @Test
    fun validDraftHasNoIssues() {
        assertTrue(valid.validate().isEmpty())
    }

    @Test
    fun explanationIsOptional() {
        assertTrue(valid.copy(explanation = "  ").validate().isEmpty())
    }

    @Test
    fun blankFieldsAreReported() {
        val issues = QuestionDraft(topic = " ").validate()
        assertEquals(
            setOf(
                DraftField.Topic, DraftField.Text, DraftField.Answer,
                DraftField.Choice(0), DraftField.Choice(1), DraftField.Choice(2), DraftField.Choice(3),
            ),
            issues.keys,
        )
    }

    @Test
    fun duplicateChoiceIsFlaggedOnTheLaterOne() {
        val issues = valid.copy(choices = listOf("Yes", "No", " yes ", "Maybe")).validate()
        assertEquals(setOf(DraftField.Choice(2)), issues.keys)
    }

    @Test
    fun trimmedStripsWhitespace() {
        val d = valid.copy(topic = "  Math ", choices = listOf(" 3", "4 ", "5", "6")).trimmed()
        assertEquals("Math", d.topic)
        assertEquals(listOf("3", "4", "5", "6"), d.choices)
    }
}
