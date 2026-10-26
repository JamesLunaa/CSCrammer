package com.example.civora.domain

enum class MockKind(val key: String, val label: String, val items: Int, val minutes: Int) {
    Full("full", "Full mock", 170, 190),
    Mini("mini", "Mini mock", 50, 55);

    val sessionMode: String get() = "$MOCK_PREFIX$key"
    val durationMs: Long get() = minutes * 60_000L

    companion object {
        const val MOCK_PREFIX = "mock-"

        fun fromKey(key: String): MockKind = entries.firstOrNull { it.key == key } ?: Full
    }
}

object Scoring {
    const val PASS_PERCENT = 80

    /** Whole percent, rounded down so it never shows 80 for a failing 79.6. */
    fun percent(correct: Int, total: Int): Int = if (total == 0) 0 else correct * 100 / total

    fun passed(correct: Int, total: Int): Boolean = total > 0 && correct * 100 >= PASS_PERCENT * total
}

/** Splits [total] into [parts] near-equal counts; the first ones take the remainder. */
fun splitEvenly(total: Int, parts: Int): List<Int> {
    require(parts > 0)
    return List(parts) { i -> total / parts + if (i < total % parts) 1 else 0 }
}

fun isMockMode(mode: String): Boolean = mode.startsWith(MockKind.MOCK_PREFIX)
