package com.example.civora.domain

import com.example.civora.domain.model.Difficulty
import com.example.civora.domain.model.Question
import com.example.civora.domain.model.Subject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MockTest {
    @Test
    fun fullAndMiniSplitEvenlyAcrossFiveSubjects() {
        assertEquals(List(5) { 34 }, splitEvenly(MockKind.Full.items, 5))
        assertEquals(List(5) { 10 }, splitEvenly(MockKind.Mini.items, 5))
    }

    @Test
    fun splitKeepsTotalAndSpreadsRemainder() {
        val parts = splitEvenly(52, 5)
        assertEquals(52, parts.sum())
        assertEquals(listOf(11, 11, 10, 10, 10), parts)
    }

    @Test
    fun passMarkIsEightyPercentExact() {
        assertTrue(Scoring.passed(136, 170))
        assertFalse(Scoring.passed(135, 170))
        assertEquals(79, Scoring.percent(135, 170))
        assertEquals(80, Scoring.percent(136, 170))
        assertFalse(Scoring.passed(0, 0))
    }

    @Test
    fun breakdownFollowsSubjectOrderAndSkipsEmpty() {
        fun item(subject: Subject, ok: Boolean) = ResultItem(
            Question(1, subject, "t", Difficulty.Easy, "q", listOf("a", "b", "c", "d"), 0, "e"),
            chosen = if (ok) 0 else 1,
            isCorrect = ok,
        )
        val scores = subjectBreakdown(
            listOf(item(Subject.Numerical, true), item(Subject.Verbal, false), item(Subject.Numerical, false)),
        )
        assertEquals(listOf(Subject.Verbal, Subject.Numerical), scores.map { it.subject })
        assertEquals(SubjectScore(Subject.Numerical, 1, 2), scores[1])
    }

    @Test
    fun mockModesAreDetected() {
        assertTrue(isMockMode(MockKind.Mini.sessionMode))
        assertFalse(isMockMode("practice"))
        assertEquals(MockKind.Mini, MockKind.fromKey("mini"))
    }
}
