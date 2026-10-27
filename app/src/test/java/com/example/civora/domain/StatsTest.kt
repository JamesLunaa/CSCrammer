package com.example.civora.domain

import com.example.civora.data.db.TopicAccuracy
import java.time.LocalDate
import org.junit.Test
import org.junit.Assert.assertEquals

class StatsTest {
    private val today = LocalDate.parse("2026-10-26")

    @Test
    fun emptyDaysGiveNoStreak() {
        assertEquals(Streak(0, 0), computeStreak(emptyList(), today))
    }

    @Test
    fun currentStreakCountsBackFromToday() {
        val s = computeStreak(listOf("2026-10-26", "2026-10-25", "2026-10-24", "2026-10-20"), today)
        assertEquals(Streak(3, 3), s)
    }

    @Test
    fun streakSurvivesUntilEndOfToday() {
        val s = computeStreak(listOf("2026-10-25", "2026-10-24"), today)
        assertEquals(2, s.current)
    }

    @Test
    fun missedDayBreaksCurrentButKeepsBest() {
        val s = computeStreak(listOf("2026-10-23", "2026-10-10", "2026-10-11", "2026-10-12", "2026-10-13"), today)
        assertEquals(Streak(0, 4), s)
    }

    @Test
    fun weakestTopicsIgnoresThinSamplesAndSortsByAccuracy() {
        val list = listOf(
            TopicAccuracy("S", "A", 10, 9),
            TopicAccuracy("S", "B", 10, 3),
            TopicAccuracy("S", "C", 2, 0),
            TopicAccuracy("S", "D", 20, 3),
        )
        assertEquals(listOf("D", "B", "A"), weakestTopics(list).map { it.topic })
    }
}
