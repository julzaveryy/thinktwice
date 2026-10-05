package com.miqu.thinktwice.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miqu.thinktwice.AppContainer
import com.miqu.thinktwice.data.db.ChallengeProgressEntity
import com.miqu.thinktwice.data.db.AttemptEntity
import com.miqu.thinktwice.domain.Stats
import com.miqu.thinktwice.data.prefs.Settings
import com.miqu.thinktwice.data.content.Content
import com.miqu.thinktwice.domain.QuizPlan
import com.miqu.thinktwice.data.prefs.Profile
import com.miqu.thinktwice.domain.QuizPlans
import com.miqu.thinktwice.domain.weekKey
import com.miqu.thinktwice.ui.common.contentFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ChallengeCard(
    val title: String,
    val subtitle: String,
    val answered: Int,
    val total: Int,
    val completed: Boolean,
    val bonus: Int,
) {
    val inProgress: Boolean get() = !completed && answered > 0
}

data class TopicCard(
    val id: String,
    val title: String,
    val questionCount: Int,
    val accuracy: Int?,
)

data class HomeState(
    val loading: Boolean = true,
    val profile: Profile = Profile(),
    val streak: Int = 0,
    val hasPlayed: Boolean = false,
    val daily: ChallengeCard? = null,
    val weekly: ChallengeCard? = null,
    val forYou: List<TopicCard> = emptyList(),
)

class HomeViewModel(container: AppContainer) : ViewModel() {
    private val progress = container.progress

    private val base = combine(
        container.contentFlow(),
        container.settings.settings,
        progress.stats,
        progress.attempts,
        progress.challengeProgress,
    ) { content, settings, stats, attempts, saved -> Base(content, settings, stats, attempts, saved) }

    val state: StateFlow<HomeState> = combine(base, container.clock.today, progress.categoryAccuracy) { b, today, accuracy ->
        val completedIds = b.attempts.mapTo(HashSet()) { it.quizId }
        val savedById = b.saved.associateBy { it.quizId }

        fun card(plan: QuizPlan): ChallengeCard {
            val completed = plan.quizId in completedIds
            val answered = if (completed) plan.questions.size
            else savedById[plan.quizId]?.selections?.split(',')?.count { it.isNotBlank() } ?: 0
            return ChallengeCard(plan.title, plan.subtitle, answered, plan.questions.size, completed, plan.firstClearBonus)
        }

        val accuracyById = accuracy.associate { it.categoryId to if (it.total == 0) 0 else it.correct * 100 / it.total }
        val favorites = b.settings.favorites
        val topics = b.content.categories
            .sortedByDescending { it.id in favorites }
            .take(4)
            .map { TopicCard(it.id, it.title, it.questions.size, accuracyById[it.id]) }

        HomeState(
            loading = false,
            profile = b.settings.profile,
            streak = b.stats.streak.current,
            hasPlayed = b.stats.quizzes > 0,
            daily = card(QuizPlans.daily(b.content, today)),
            weekly = card(QuizPlans.weekly(b.content, weekKey(today))),
            forYou = topics,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    private data class Base(
        val content: Content,
        val settings: Settings,
        val stats: Stats,
        val attempts: List<AttemptEntity>,
        val saved: List<ChallengeProgressEntity>,
    )
}
