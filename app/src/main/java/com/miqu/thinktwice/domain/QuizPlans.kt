package com.miqu.thinktwice.domain

import com.miqu.thinktwice.data.content.Content
import com.miqu.thinktwice.data.content.Question
import kotlin.random.Random

enum class QuizKind(val label: String) {
    CATEGORY("Topic"),
    DAILY("Daily challenge"),
    WEEKLY("Weekly spotlight"),
    QUICK_MIX("Quick mix"),
    TIME_ATTACK("Time attack"),
    SURVIVAL("Survival"),
    ENDLESS("Endless"),
    PRACTICE("Practice"),
}

/** Everything the quiz screen needs to run one round. */
data class QuizPlan(
    val quizId: String,
    val kind: QuizKind,
    val title: String,
    val subtitle: String,
    val questions: List<Question>,
    /** One-off bonus for finishing this exact quiz for the first time. */
    val firstClearBonus: Int = 0,
    val timeLimitSeconds: Int? = null,
    val lives: Int? = null,
    val awardsXp: Boolean = true,
    /** Progress is saved after every answer so the round can be continued later. */
    val resumable: Boolean = false,
) {
    val isOpenEnded: Boolean get() = kind == QuizKind.TIME_ATTACK || kind == QuizKind.SURVIVAL || kind == QuizKind.ENDLESS
}

object QuizPlans {
    const val ROUND_SIZE = 10
    const val DAILY_SIZE = 10
    const val WEEKLY_SIZE = 12
    const val DAILY_BONUS = 120
    const val WEEKLY_BONUS = 250
    const val TIME_ATTACK_SECONDS = 90
    const val SURVIVAL_LIVES = 3
    const val PRACTICE_SIZE = 15
    const val DAILY_CATEGORY = "landmarks"

    fun dailyId(day: Long) = "daily_$day"
    fun weeklyId(week: Long) = "weekly_$week"

    fun daily(content: Content, day: Long): QuizPlan {
        val questions = content.category(DAILY_CATEGORY)?.questions.orEmpty()
            .shuffled(Random(day * 7919L + 17L))
            .take(DAILY_SIZE)
        return QuizPlan(
            quizId = dailyId(day),
            kind = QuizKind.DAILY,
            title = "Landmark Hunt",
            subtitle = "Daily challenge",
            questions = questions,
            firstClearBonus = DAILY_BONUS,
            resumable = true,
        )
    }

    fun weeklyTopics(content: Content, week: Long) =
        content.categories.filter { it.id != "quick" && it.id != DAILY_CATEGORY }
            .shuffled(Random(week * 31L + 7L))
            .take(3)

    fun weekly(content: Content, week: Long): QuizPlan {
        val topics = weeklyTopics(content, week)
        val random = Random(week * 104_729L + 3L)
        val questions = topics.flatMap { it.questions.shuffled(random).take(WEEKLY_SIZE / topics.size.coerceAtLeast(1)) }
            .shuffled(random)
        return QuizPlan(
            quizId = weeklyId(week),
            kind = QuizKind.WEEKLY,
            title = "Weekly Spotlight",
            subtitle = topics.joinToString(" · ") { it.title },
            questions = questions,
            firstClearBonus = WEEKLY_BONUS,
            resumable = true,
        )
    }

    fun category(content: Content, categoryId: String, answered: Set<String>, seed: Long): QuizPlan? {
        val category = content.category(categoryId) ?: return null
        return QuizPlan(
            quizId = category.id,
            kind = QuizKind.CATEGORY,
            title = category.title,
            subtitle = "${category.questions.size} questions",
            questions = freshFirst(category.questions, answered, Random(seed)).take(ROUND_SIZE),
        )
    }

    fun quickMix(content: Content, favorites: Set<String>, answered: Set<String>, seed: Long): QuizPlan {
        val source = content.categories.filter { favorites.isEmpty() || it.id in favorites }
            .ifEmpty { content.categories }
        val random = Random(seed)
        // Round-robin across topics so a mix never collapses into one subject.
        val perTopic = source.map { freshFirst(it.questions, answered, random) }
        val mixed = buildList {
            var i = 0
            while (size < ROUND_SIZE && perTopic.any { i < it.size }) {
                perTopic.shuffled(random).forEach { list -> if (i < list.size && size < ROUND_SIZE) add(list[i]) }
                i++
            }
        }
        return QuizPlan(
            quizId = "quick_mix",
            kind = QuizKind.QUICK_MIX,
            title = "Quick Mix",
            subtitle = "$ROUND_SIZE questions from your topics",
            questions = mixed,
        )
    }

    fun timeAttack(content: Content, seed: Long) = QuizPlan(
        quizId = "time_attack",
        kind = QuizKind.TIME_ATTACK,
        title = "Time Attack",
        subtitle = "$TIME_ATTACK_SECONDS seconds",
        questions = content.allQuestions.shuffled(Random(seed)),
        timeLimitSeconds = TIME_ATTACK_SECONDS,
    )

    fun survival(content: Content, seed: Long) = QuizPlan(
        quizId = "survival",
        kind = QuizKind.SURVIVAL,
        title = "Survival",
        subtitle = "$SURVIVAL_LIVES lives",
        questions = content.allQuestions.shuffled(Random(seed)),
        lives = SURVIVAL_LIVES,
    )

    fun endless(content: Content, seed: Long) = QuizPlan(
        quizId = "endless",
        kind = QuizKind.ENDLESS,
        title = "Endless",
        subtitle = "Play as long as you like",
        questions = content.allQuestions.shuffled(Random(seed)),
    )

    fun practice(content: Content, mistakeIds: List<String>) = QuizPlan(
        quizId = "practice",
        kind = QuizKind.PRACTICE,
        title = "Practice mistakes",
        subtitle = "No XP · clears what you get right",
        questions = mistakeIds.mapNotNull(content.questionsById::get).take(PRACTICE_SIZE),
        awardsXp = false,
    )

    /** Unseen questions first, each group in seeded random order. */
    private fun freshFirst(questions: List<Question>, answered: Set<String>, random: Random): List<Question> {
        val (seen, fresh) = questions.shuffled(random).partition { it.id in answered }
        return fresh + seen
    }

    /** Stable display order of the four answers for one question in one round. */
    fun answerOrder(question: Question, seed: Long): List<Int> =
        question.answers.indices.shuffled(Random(seed xor question.id.hashCode().toLong()))
}
