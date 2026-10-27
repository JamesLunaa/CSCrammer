package com.example.civora.data.seed

import java.io.Reader

/**
 * Minimal RFC 4180 parser: quoted fields, embedded commas, doubled quotes,
 * line breaks inside quotes, and LF or CRLF row endings.
 */
object CsvParser {

    fun parse(reader: Reader): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var fieldStarted = false
        val text = reader.readText().removePrefix("\uFEFF")
        var i = 0

        fun endField() {
            row.add(field.toString())
            field.clear()
            fieldStarted = false
        }

        fun endRow() {
            endField()
            if (!(row.size == 1 && row[0].isEmpty())) rows.add(row)
            row = mutableListOf()
        }

        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                when {
                    c == '"' && i + 1 < text.length && text[i + 1] == '"' -> { field.append('"'); i++ }
                    c == '"' -> inQuotes = false
                    c == '\r' && i + 1 < text.length && text[i + 1] == '\n' -> { field.append('\n'); i++ }
                    else -> field.append(c)
                }
            } else {
                when {
                    c == '"' && !fieldStarted && field.isEmpty() -> { inQuotes = true; fieldStarted = true }
                    c == ',' -> endField()
                    c == '\r' -> { if (i + 1 < text.length && text[i + 1] == '\n') i++; endRow() }
                    c == '\n' -> endRow()
                    else -> field.append(c)
                }
            }
            i++
        }
        require(!inQuotes) { "Unterminated quoted field" }
        if (field.isNotEmpty() || row.isNotEmpty()) endRow()
        return rows
    }
}
