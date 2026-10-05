package com.miqu.thinktwice

import com.miqu.thinktwice.domain.BADGES
import com.miqu.thinktwice.domain.Stats
import com.miqu.thinktwice.domain.Streak
import com.miqu.thinktwice.domain.badgesFor
import com.miqu.thinktwice.domain.dayOfWeek
import com.miqu.thinktwice.domain.levelFor
import com.miqu.thinktwice.domain.streakFrom
import com.miqu.thinktwice.domain.weekKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesTest {
    @Test fun `streak counts back from today`() {
        assertEquals(Streak(3, 3), streakFrom(listOf(10L, 9L, 8L), today = 10))
    }

    @Test fun `streak survives until the end of the next day`() {
        assertEquals(Streak(2, 2), streakFrom(listOf(9L, 8L), today = 10))
    }

    @Test fun `streak breaks after a missed day but keeps the best run`() {
        assertEquals(Streak(0, 4), streakFrom(listOf(1L, 2L, 3L, 4L, 7L), today = 9))
        assertEquals(Streak(1, 4), streakFrom(listOf(1L, 2L, 3L, 4L, 9L), today = 9))
    }

    @Test fun `no activity means no streak`() {
        assertEquals(Streak(0, 0), streakFrom(emptyList(), today = 5))
    }

    @Test fun `levels progress between thresholds`() {
        assertEquals("Beginner", levelFor(0).level.name)
        assertEquals(100, levelFor(0).xpToNext)
        val silver = levelFor(375)
        assertEquals("Silver", silver.level.name)
        assertEquals(0.5f, silver.progress, 0.001f)
        val top = levelFor(10_000)
        assertNull(top.next)
        assertEquals(1f, top.progress, 0f)
    }

    @Test fun `weeks start on monday`() {
        // 1970-01-05 (epoch day 4) was a Monday.
        assertEquals(0, dayOfWeek(4))
        assertEquals(6, dayOfWeek(10))
        assertEquals(weekKey(4), weekKey(10))
        assertTrue(weekKey(11) == weekKey(4) + 1)
    }

    @Test fun `badges unlock at their targets`() {
        val badges = badgesFor(Stats(quizzes = 5, answers = 30, correct = 10, xp = 50, perfect = 0, categoriesPlayed = 1, streak = Streak(1, 3)))
        val unlocked = badges.filter { it.unlocked }.map { it.spec.id }.toSet()
        assertEquals(setOf("first_step", "quiz_regular", "curious_mind", "on_fire"), unlocked)
        assertEquals(BADGES.size, badges.size)
    }
}
