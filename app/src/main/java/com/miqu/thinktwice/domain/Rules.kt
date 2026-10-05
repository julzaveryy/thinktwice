package com.miqu.thinktwice.domain

import java.util.TimeZone

/** Days since 1970-01-01 in the device's local time zone. */
fun localEpochDay(now: Long = System.currentTimeMillis(), zone: TimeZone = TimeZone.getDefault()): Long =
    Math.floorDiv(now + zone.getOffset(now), 86_400_000L)

/** Weeks start on Monday. Epoch day 0 was a Thursday, hence the +3 shift. */
fun weekKey(epochDay: Long): Long = Math.floorDiv(epochDay + 3L, 7L)

/** 0 = Monday … 6 = Sunday. */
fun dayOfWeek(epochDay: Long): Int = Math.floorMod(epochDay + 3L, 7L).toInt()

data class Streak(val current: Int, val best: Int)

/**
 * Streak is derived from the days on which the player finished at least one round,
 * so it can never drift out of sync with the history.
 */
fun streakFrom(activeDays: Collection<Long>, today: Long): Streak {
    if (activeDays.isEmpty()) return Streak(0, 0)
    val days = activeDays.toSortedSet()
    var best = 0
    var run = 0
    var previous: Long? = null
    for (day in days) {
        run = if (previous != null && day == previous + 1) run + 1 else 1
        best = maxOf(best, run)
        previous = day
    }
    val anchor = when {
        today in days -> today
        (today - 1) in days -> today - 1
        else -> return Streak(0, best)
    }
    var current = 0
    var day = anchor
    while (day in days) {
        current++
        day--
    }
    return Streak(current, best)
}

data class Level(val name: String, val minXp: Int)

val LEVELS = listOf(
    Level("Beginner", 0),
    Level("Bronze", 100),
    Level("Silver", 250),
    Level("Gold", 500),
    Level("Platinum", 1_000),
    Level("Diamond", 2_000),
)

data class LevelProgress(
    val level: Level,
    val next: Level?,
    val progress: Float,
    val xpToNext: Int,
) {
    val index: Int get() = LEVELS.indexOf(level)
}

fun levelFor(xp: Int): LevelProgress {
    val safe = xp.coerceAtLeast(0)
    val index = LEVELS.indexOfLast { safe >= it.minXp }.coerceAtLeast(0)
    val level = LEVELS[index]
    val next = LEVELS.getOrNull(index + 1) ?: return LevelProgress(level, null, 1f, 0)
    val span = (next.minXp - level.minXp).toFloat()
    return LevelProgress(
        level = level,
        next = next,
        progress = ((safe - level.minXp) / span).coerceIn(0f, 1f),
        xpToNext = next.minXp - safe,
    )
}

data class Stats(
    val quizzes: Int = 0,
    val answers: Int = 0,
    val correct: Int = 0,
    val xp: Int = 0,
    val perfect: Int = 0,
    val categoriesPlayed: Int = 0,
    val streak: Streak = Streak(0, 0),
) {
    val accuracy: Int get() = if (answers == 0) 0 else correct * 100 / answers
}

enum class BadgeMetric(val unit: String) {
    QUIZZES("quizzes"), ANSWERS("answers"), CORRECT("correct"), STREAK("days"),
    CATEGORIES("topics"), PERFECT("perfect rounds"), XP("XP"),
}

data class BadgeSpec(
    val id: String,
    val title: String,
    val description: String,
    val metric: BadgeMetric,
    val target: Int,
)

data class Badge(val spec: BadgeSpec, val current: Int) {
    val unlocked: Boolean get() = current >= spec.target
    val progress: Float get() = (current.toFloat() / spec.target).coerceIn(0f, 1f)
}

val BADGES = listOf(
    BadgeSpec("first_step", "First Step", "Finish your first quiz.", BadgeMetric.QUIZZES, 1),
    BadgeSpec("curious_mind", "Curious Mind", "Answer 25 questions.", BadgeMetric.ANSWERS, 25),
    BadgeSpec("on_fire", "On Fire", "Play 3 days in a row.", BadgeMetric.STREAK, 3),
    BadgeSpec("explorer", "Explorer", "Finish quizzes in 3 topics.", BadgeMetric.CATEGORIES, 3),
    BadgeSpec("quiz_regular", "Quiz Regular", "Finish 5 quizzes.", BadgeMetric.QUIZZES, 5),
    BadgeSpec("sharp_shooter", "Sharp Shooter", "Get 25 answers right.", BadgeMetric.CORRECT, 25),
    BadgeSpec("perfect_round", "Perfect Round", "Finish a quiz without a mistake.", BadgeMetric.PERFECT, 1),
    BadgeSpec("century_club", "Century Club", "Answer 100 questions.", BadgeMetric.ANSWERS, 100),
    BadgeSpec("super_star", "Super Star", "Play 7 days in a row.", BadgeMetric.STREAK, 7),
    BadgeSpec("xp_collector", "XP Collector", "Earn 500 XP in total.", BadgeMetric.XP, 500),
    BadgeSpec("quiz_champion", "Quiz Champion", "Finish 20 quizzes.", BadgeMetric.QUIZZES, 20),
    BadgeSpec("master_mind", "Master Mind", "Get 100 answers right.", BadgeMetric.CORRECT, 100),
)

fun badgesFor(stats: Stats): List<Badge> = BADGES.map { spec ->
    val value = when (spec.metric) {
        BadgeMetric.QUIZZES -> stats.quizzes
        BadgeMetric.ANSWERS -> stats.answers
        BadgeMetric.CORRECT -> stats.correct
        BadgeMetric.STREAK -> stats.streak.best
        BadgeMetric.CATEGORIES -> stats.categoriesPlayed
        BadgeMetric.PERFECT -> stats.perfect
        BadgeMetric.XP -> stats.xp
    }
    Badge(spec, value)
}
