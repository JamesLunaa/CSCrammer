package com.example.civora.data.seed

import com.example.civora.domain.model.Difficulty
import com.example.civora.domain.model.Subject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CsvParserTest {

    private fun parse(text: String) = CsvParser.parse(text.reader())

    @Test
    fun quotedFieldsKeepCommasQuotesAndLineBreaks() {
        val rows = parse("a,\"b, c\",\"say \"\"hi\"\"\",\"line1\r\nline2\"\r\n1,2,3,4\r\n")
        assertEquals(listOf("a", "b, c", "say \"hi\"", "line1\nline2"), rows[0])
        assertEquals(listOf("1", "2", "3", "4"), rows[1])
        assertEquals(2, rows.size)
    }

    @Test
    fun handlesLfEndingsEmptyFieldsBomAndMissingTrailingNewline() {
        val rows = parse("﻿x,,z\n,,\nlast")
        assertEquals(listOf(listOf("x", "", "z"), listOf("", "", ""), listOf("last")), rows)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnterminatedQuote() {
        parse("a,\"oops\n")
    }

    @Test
    fun bundledQuestionsAreValid() {
        val file = File("src/main/assets/questions.csv")
        val questions = file.reader(Charsets.UTF_8).use { QuestionSeeder.toEntities(CsvParser.parse(it)) }

        assertEquals(2500, questions.size)
        assertEquals(2500, questions.map { it.id }.toSet().size)
        questions.forEach { q ->
            assertTrue("id ${q.id} answer ${q.answer}", q.answer in setOf("A", "B", "C", "D"))
            assertTrue("id ${q.id} has an empty field", listOf(q.question, q.a, q.b, q.c, q.d, q.topic, q.explanation).none { it.isBlank() })
            Subject.fromLabel(q.subject)
            Difficulty.fromLabel(q.difficulty)
        }
        Subject.entries.forEach { s -> assertEquals(s.label, 500, questions.count { it.subject == s.label }) }
        assertEquals(24, questions.map { it.subject to it.topic }.toSet().size)
    }
}
