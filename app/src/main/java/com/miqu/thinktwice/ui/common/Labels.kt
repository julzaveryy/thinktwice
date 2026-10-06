package com.miqu.thinktwice.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.miqu.thinktwice.R
import com.miqu.thinktwice.data.content.Difficulty
import com.miqu.thinktwice.domain.BadgeMetric
import com.miqu.thinktwice.domain.BadgeSpec
import com.miqu.thinktwice.domain.LEVELS
import com.miqu.thinktwice.domain.Level
import com.miqu.thinktwice.domain.QuizKind

/** Localised names for domain values, which stay language-neutral. */

@StringRes
fun QuizKind.labelRes(): Int = when (this) {
    QuizKind.CATEGORY -> R.string.kind_topic
    QuizKind.DAILY -> R.string.kind_daily
    QuizKind.WEEKLY -> R.string.kind_weekly
    QuizKind.QUICK_MIX -> R.string.kind_quick_mix
    QuizKind.TIME_ATTACK -> R.string.kind_time_attack
    QuizKind.SURVIVAL -> R.string.kind_survival
    QuizKind.ENDLESS -> R.string.kind_endless
    QuizKind.PRACTICE -> R.string.kind_practice
}

/** Display title of a round: the topic's title for topic quizzes, otherwise the mode's name. */
@Composable
fun quizTitle(kind: QuizKind, topicTitle: String?): String = when (kind) {
    QuizKind.CATEGORY -> topicTitle.orEmpty()
    QuizKind.DAILY -> stringResource(R.string.title_daily)
    QuizKind.WEEKLY -> stringResource(R.string.title_weekly)
    QuizKind.QUICK_MIX -> stringResource(R.string.title_quick_mix)
    QuizKind.TIME_ATTACK -> stringResource(R.string.title_time_attack)
    QuizKind.SURVIVAL -> stringResource(R.string.title_survival)
    QuizKind.ENDLESS -> stringResource(R.string.title_endless)
    QuizKind.PRACTICE -> stringResource(R.string.title_practice)
}

@StringRes
fun Difficulty.labelRes(): Int = when (this) {
    Difficulty.EASY -> R.string.diff_easy
    Difficulty.MEDIUM -> R.string.diff_medium
    Difficulty.HARD -> R.string.diff_hard
}

@Composable
fun levelName(level: Level): String = stringResource(
    when (LEVELS.indexOf(level)) {
        0 -> R.string.level_beginner
        1 -> R.string.level_bronze
        2 -> R.string.level_silver
        3 -> R.string.level_gold
        4 -> R.string.level_platinum
        else -> R.string.level_diamond
    },
)

@StringRes
fun BadgeMetric.unitRes(): Int = when (this) {
    BadgeMetric.QUIZZES -> R.string.unit_quizzes
    BadgeMetric.ANSWERS -> R.string.unit_answers
    BadgeMetric.CORRECT -> R.string.unit_correct
    BadgeMetric.STREAK -> R.string.unit_days
    BadgeMetric.CATEGORIES -> R.string.unit_topics
    BadgeMetric.PERFECT -> R.string.unit_perfect
    BadgeMetric.XP -> R.string.unit_xp
}

@Composable
fun badgeTitle(spec: BadgeSpec): String = stringResource(badgeRes(spec.id).first)

@Composable
fun badgeDescription(spec: BadgeSpec): String = stringResource(badgeRes(spec.id).second)

private fun badgeRes(id: String): Pair<Int, Int> = when (id) {
    "first_step" -> R.string.badge_first_step to R.string.badge_first_step_desc
    "curious_mind" -> R.string.badge_curious_mind to R.string.badge_curious_mind_desc
    "on_fire" -> R.string.badge_on_fire to R.string.badge_on_fire_desc
    "explorer" -> R.string.badge_explorer to R.string.badge_explorer_desc
    "quiz_regular" -> R.string.badge_quiz_regular to R.string.badge_quiz_regular_desc
    "sharp_shooter" -> R.string.badge_sharp_shooter to R.string.badge_sharp_shooter_desc
    "perfect_round" -> R.string.badge_perfect_round to R.string.badge_perfect_round_desc
    "century_club" -> R.string.badge_century_club to R.string.badge_century_club_desc
    "super_star" -> R.string.badge_super_star to R.string.badge_super_star_desc
    "xp_collector" -> R.string.badge_xp_collector to R.string.badge_xp_collector_desc
    "quiz_champion" -> R.string.badge_quiz_champion to R.string.badge_quiz_champion_desc
    else -> R.string.badge_master_mind to R.string.badge_master_mind_desc
}
