package com.miqu.thinktwice.data

import com.miqu.thinktwice.data.db.AttemptAnswerEntity
import com.miqu.thinktwice.data.db.AttemptEntity
import com.miqu.thinktwice.data.db.ChallengeProgressEntity
import com.miqu.thinktwice.data.db.MistakeEntity
import com.miqu.thinktwice.data.db.ProgressDao
import com.miqu.thinktwice.domain.QuizKind
import com.miqu.thinktwice.domain.QuizPlan
import com.miqu.thinktwice.domain.Stats
import com.miqu.thinktwice.domain.localEpochDay
import com.miqu.thinktwice.domain.streakFrom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

/** The current local day. Refreshed by the activity so streaks and daily challenges roll over at midnight. */
class DayClock {
    private val _today = MutableStateFlow(localEpochDay())
    val today: StateFlow<Long> = _today.asStateFlow()

    fun refresh() {
        _today.value = localEpochDay()
    }
}

data class AnsweredQuestion(val questionId: String, val selected: Int, val isCorrect: Boolean)

class ProgressRepository(
    private val dao: ProgressDao,
    private val clock: DayClock,
) {
    val attempts: Flow<List<AttemptEntity>> = dao.observeAttempts()
    val mistakes: Flow<List<MistakeEntity>> = dao.observeMistakes()
    val categoryAccuracy = dao.observeCategoryAccuracy()
    val challengeProgress: Flow<List<ChallengeProgressEntity>> = dao.observeChallengeProgress()
    val activeDays: Flow<List<Long>> = dao.observeActiveDays()

    val stats: Flow<Stats> = combine(
        dao.observeStats(),
        dao.observeActiveDays(),
        dao.observeCategoryAccuracy(),
        clock.today,
    ) { row, days, categories, today ->
        Stats(
            quizzes = row.quizzes,
            answers = row.answers,
            correct = row.correct,
            xp = row.xp,
            perfect = row.perfect,
            categoriesPlayed = categories.size,
            streak = streakFrom(days, today),
        )
    }

    fun isCompleted(quizId: String): Flow<Boolean> = dao.observeCompleted(quizId)

    suspend fun answeredQuestionIds(): Set<String> = dao.answeredQuestionIds().toSet()

    suspend fun mistakeIds(): List<String> = dao.mistakes().map { it.questionId }

    suspend fun loadChallenge(quizId: String): List<Int>? =
        dao.challengeProgress(quizId)?.selections?.takeIf { it.isNotBlank() }
            ?.split(',')?.mapNotNull { it.toIntOrNull() }

    suspend fun saveChallenge(quizId: String, selections: List<Int>) {
        dao.upsertChallengeProgress(
            ChallengeProgressEntity(quizId, selections.joinToString(","), System.currentTimeMillis()),
        )
    }

    /** Stores a finished round and returns its id. Bonus is only granted the first time. */
    suspend fun record(plan: QuizPlan, answers: List<AnsweredQuestion>, xp: Int): Long {
        val now = System.currentTimeMillis()
        val bonus = if (plan.firstClearBonus > 0 && !dao.isCompleted(plan.quizId)) plan.firstClearBonus else 0
        val attempt = AttemptEntity(
            quizId = plan.quizId,
            kind = plan.kind.name,
            title = plan.title,
            correct = answers.count { it.isCorrect },
            total = answers.size,
            xp = if (plan.awardsXp) xp else 0,
            bonus = if (plan.awardsXp) bonus else 0,
            completedAt = now,
            epochDay = localEpochDay(now),
        )
        val id = dao.recordAttempt(
            attempt = attempt,
            answers = answers.map { AttemptAnswerEntity(attemptId = 0, questionId = it.questionId, selected = it.selected, isCorrect = it.isCorrect) },
            newMistakes = answers.filter { !it.isCorrect }.map { MistakeEntity(it.questionId, now) },
            clearedMistakes = answers.filter { it.isCorrect }.map { it.questionId },
        )
        if (plan.resumable) dao.deleteChallengeProgress(plan.quizId)
        clock.refresh()
        return id
    }

    suspend fun attempt(id: Long) = dao.attempt(id)
    suspend fun answersFor(id: Long) = dao.answersFor(id)

    suspend fun resetAll() = dao.clearAll()

    suspend fun playedToday(): Boolean = dao.lastActiveDay() == localEpochDay()

    companion object {
        fun kindOf(attempt: AttemptEntity): QuizKind =
            runCatching { QuizKind.valueOf(attempt.kind) }.getOrDefault(QuizKind.CATEGORY)
    }
}
