package com.miqu.thinktwice.ui.quiz

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.miqu.thinktwice.AppContainer
import com.miqu.thinktwice.data.AnsweredQuestion
import com.miqu.thinktwice.data.content.Question
import com.miqu.thinktwice.domain.QuizKind
import com.miqu.thinktwice.domain.QuizPlan
import com.miqu.thinktwice.domain.QuizPlans
import com.miqu.thinktwice.domain.weekKey
import com.miqu.thinktwice.ui.navigation.QuizRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

data class QuizUi(
    val loading: Boolean = true,
    val plan: QuizPlan? = null,
    val seed: Long = 0,
    val index: Int = 0,
    /** Original answer index per revealed question, -1 for skipped. */
    val selections: List<Int> = emptyList(),
    val picked: Int? = null,
    val checked: Boolean = false,
    val secondsLeft: Int? = null,
    val timeUp: Boolean = false,
    val finishedAttemptId: Long? = null,
    val closed: Boolean = false,
    val silentIndex: Int = -1,
) {
    val questions: List<Question> get() = plan?.questions.orEmpty()
    val question: Question? get() = questions.getOrNull(index)
    val answerOrder: List<Int> get() = question?.let { QuizPlans.answerOrder(it, seed) }.orEmpty()

    private fun isCorrect(i: Int) = selections.getOrNull(i)?.let { it == questions[i].correctIndex } == true
    val correctCount: Int get() = selections.indices.count(::isCorrect)
    val wrongCount: Int get() = selections.size - correctCount
    val livesLeft: Int? get() = plan?.lives?.let { (it - wrongCount).coerceAtLeast(0) }
    val xp: Int get() = if (plan?.awardsXp == true) selections.indices.filter(::isCorrect).sumOf { questions[it].difficulty.xp } else 0

    val lastAnswerCorrect: Boolean get() = checked && isCorrect(index)
    val isOver: Boolean get() = timeUp || livesLeft == 0 || (checked && index >= questions.lastIndex)
}

class QuizViewModel(
    private val container: AppContainer,
    private val saved: SavedStateHandle,
) : ViewModel() {
    private val route = saved.toRoute<QuizRoute>()
    private val kind = runCatching { QuizKind.valueOf(route.kind) }.getOrDefault(QuizKind.QUICK_MIX)

    private val _ui = MutableStateFlow(QuizUi())
    val ui: StateFlow<QuizUi> = _ui.asStateFlow()

    val feedback = container.settings.settings.map { it.sound to it.haptics }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true to true)

    private var timerJob: Job? = null
    private var finishing = false

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val content = container.content.content()
        val today = container.clock.today.value
        val seed: Long = saved[KEY_SEED] ?: when (kind) {
            QuizKind.DAILY -> today
            QuizKind.WEEKLY -> weekKey(today)
            else -> Random.nextLong()
        }.also { saved[KEY_SEED] = it }

        val plan = when (kind) {
            // Seed holds the day / week the round started on, so a round restored after midnight
            // keeps its own questions instead of mixing answers into the new day's set.
            QuizKind.DAILY -> QuizPlans.daily(content, seed)
            QuizKind.WEEKLY -> QuizPlans.weekly(content, seed)
            QuizKind.CATEGORY -> QuizPlans.category(content, route.categoryId.orEmpty(), container.progress.answeredQuestionIds(), seed)
            QuizKind.QUICK_MIX -> QuizPlans.quickMix(content, container.settings.current().favorites, container.progress.answeredQuestionIds(), seed)
            QuizKind.TIME_ATTACK -> QuizPlans.timeAttack(content, seed)
            QuizKind.SURVIVAL -> QuizPlans.survival(content, seed)
            QuizKind.ENDLESS -> QuizPlans.endless(content, seed)
            QuizKind.PRACTICE -> QuizPlans.practice(content, container.progress.mistakeIds())
        }
        if (plan == null || plan.questions.isEmpty()) {
            _ui.value = QuizUi(loading = false, closed = true)
            return
        }

        val restored = saved.get<IntArray>(KEY_SELECTIONS)?.toList()
            ?: if (plan.resumable) container.progress.loadChallenge(plan.quizId)?.take(plan.questions.size).orEmpty() else emptyList()
        val restoredChecked = saved.get<Boolean>(KEY_CHECKED) ?: (restored.size == plan.questions.size && restored.isNotEmpty())
        val index = if (restoredChecked) (restored.size - 1).coerceAtLeast(0) else restored.size
        _ui.value = QuizUi(
            loading = false,
            plan = plan,
            seed = seed,
            index = index.coerceAtMost(plan.questions.lastIndex),
            selections = restored,
            picked = if (restoredChecked) restored.lastOrNull() else saved.get<Int>(KEY_PICKED)?.takeIf { it >= 0 },
            checked = restoredChecked,
            // Don't replay sound/haptics for an answer revealed before the screen was recreated.
            silentIndex = if (restoredChecked) index else -1,
        )
        plan.timeLimitSeconds?.let { startTimer(it) }
    }

    private fun startTimer(limitSeconds: Int) {
        val deadline: Long = saved[KEY_DEADLINE] ?: (SystemClock.elapsedRealtime() + limitSeconds * 1_000L)
            .also { saved[KEY_DEADLINE] = it }
        timerJob = viewModelScope.launch {
            while (isActive) {
                val remainingMs = deadline - SystemClock.elapsedRealtime()
                val seconds = ((remainingMs + 999) / 1_000).toInt().coerceAtLeast(0)
                if (_ui.value.secondsLeft != seconds) {
                    _ui.update { it.copy(secondsLeft = seconds, timeUp = seconds == 0) }
                }
                if (seconds == 0) break
                delay(250)
            }
        }
    }

    fun pick(originalIndex: Int) {
        val current = _ui.value
        if (current.checked || current.isOver) return
        _ui.value = current.copy(picked = originalIndex)
        saved[KEY_PICKED] = originalIndex
    }

    fun check() = reveal { it.picked }

    fun skip() = reveal { -1 }

    private fun reveal(choice: (QuizUi) -> Int?) {
        val current = _ui.value
        if (current.checked || current.isOver || current.plan == null) return
        val selection = choice(current) ?: return
        val next = current.copy(selections = current.selections + selection, checked = true, picked = selection)
        _ui.value = next
        persist(next)
    }

    fun next() {
        val current = _ui.value
        if (!current.checked && !current.timeUp) return
        if (current.isOver) {
            finish()
            return
        }
        val next = current.copy(index = current.index + 1, picked = null, checked = false)
        _ui.value = next
        persist(next)
    }

    /** Ends the round now and records whatever has been answered. */
    fun finish() {
        if (finishing) return
        val current = _ui.value
        val plan = current.plan ?: return
        finishing = true
        timerJob?.cancel()
        viewModelScope.launch {
            val answers = current.selections.mapIndexed { i, selection ->
                val question = plan.questions[i]
                AnsweredQuestion(question.id, selection, selection == question.correctIndex)
            }
            if (answers.isEmpty()) {
                _ui.update { it.copy(closed = true) }
                return@launch
            }
            val id = container.progress.record(plan, answers, current.xp)
            _ui.update { it.copy(finishedAttemptId = id) }
        }
    }

    private fun persist(state: QuizUi) {
        saved[KEY_SELECTIONS] = state.selections.toIntArray()
        saved[KEY_CHECKED] = state.checked
        saved[KEY_PICKED] = state.picked ?: -1
        val plan = state.plan ?: return
        if (plan.resumable) {
            viewModelScope.launch { container.progress.saveChallenge(plan.quizId, state.selections) }
        }
    }

    private companion object {
        const val KEY_SEED = "seed"
        const val KEY_SELECTIONS = "selections"
        const val KEY_CHECKED = "checked"
        const val KEY_PICKED = "picked"
        const val KEY_DEADLINE = "deadline"
    }
}
