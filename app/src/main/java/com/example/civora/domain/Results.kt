package com.example.civora.domain

import com.example.civora.domain.model.Question
import com.example.civora.domain.model.Subject

data class ResultItem(val question: Question, val chosen: Int?, val isCorrect: Boolean)

data class SubjectScore(val subject: Subject, val correct: Int, val total: Int)

/** Per-subject scores in the fixed subject order; subjects with no items are left out. */
fun subjectBreakdown(items: List<ResultItem>): List<SubjectScore> =
    Subject.entries.mapNotNull { subject ->
        val mine = items.filter { it.question.subject == subject }
        if (mine.isEmpty()) null else SubjectScore(subject, mine.count { it.isCorrect }, mine.size)
    }
