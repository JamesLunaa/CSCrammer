package com.example.civora.domain

import com.example.civora.data.db.TopicAccuracy
import java.time.LocalDate

data class Streak(val current: Int, val best: Int)

/**
 * [days] are local calendar days ("yyyy-MM-dd") with at least one attempt, in any order.
 * The current streak counts back from [today], or from yesterday if today has no attempt yet.
 */
fun computeStreak(days: Collection<String>, today: LocalDate): Streak {
    val set = days.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSortedSet()
    if (set.isEmpty()) return Streak(0, 0)

    var best = 0
    var run = 0
    var prev: LocalDate? = null
    for (d in set) {
        run = if (prev != null && prev.plusDays(1) == d) run + 1 else 1
        if (run > best) best = run
        prev = d
    }

    var cursor = if (today in set) today else today.minusDays(1)
    var current = 0
    while (cursor in set) {
        current++
        cursor = cursor.minusDays(1)
    }
    return Streak(current, best)
}

/** Topics with at least [minAnswered] attempts, lowest accuracy first (ties: more attempts first). */
fun weakestTopics(topics: List<TopicAccuracy>, minAnswered: Int = 5, limit: Int = 5): List<TopicAccuracy> =
    topics.filter { it.answered >= minAnswered }
        .sortedWith(
            compareBy<TopicAccuracy> { it.correct.toDouble() / it.answered }.thenByDescending { it.answered },
        )
        .take(limit)
