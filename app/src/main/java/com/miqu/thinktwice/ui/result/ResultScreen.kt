package com.miqu.thinktwice.ui.result

import com.miqu.thinktwice.ui.theme.rememberCountUp
import com.miqu.thinktwice.ui.common.levelName
import com.miqu.thinktwice.ui.common.quizTitle
import com.miqu.thinktwice.ui.common.labelRes
import com.miqu.thinktwice.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.miqu.thinktwice.AppContainer
import com.miqu.thinktwice.data.ProgressRepository
import com.miqu.thinktwice.domain.LevelProgress
import com.miqu.thinktwice.domain.QuizKind
import com.miqu.thinktwice.domain.levelFor
import com.miqu.thinktwice.ui.common.appViewModel
import com.miqu.thinktwice.ui.components.AppCard
import com.miqu.thinktwice.ui.components.CircleIconButton
import com.miqu.thinktwice.ui.components.LinearMeter
import com.miqu.thinktwice.ui.components.Overline
import com.miqu.thinktwice.ui.components.PrimaryButton
import com.miqu.thinktwice.ui.components.ScreenGutter
import com.miqu.thinktwice.ui.components.SectionHeader
import com.miqu.thinktwice.ui.navigation.ResultRoute
import com.miqu.thinktwice.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ReviewItem(
    val question: String,
    val yourAnswer: String?,
    val correctAnswer: String,
    val isCorrect: Boolean,
)

data class ResultState(
    val title: String,
    val kind: QuizKind,
    val categoryId: String?,
    val correct: Int,
    val total: Int,
    val xp: Int,
    val bonus: Int,
    val level: LevelProgress,
    val items: List<ReviewItem>,
) {
    val accuracy: Int get() = if (total == 0) 0 else correct * 100 / total
}

class ResultViewModel(private val container: AppContainer, saved: SavedStateHandle) : ViewModel() {
    private val attemptId = saved.toRoute<ResultRoute>().attemptId
    private val _state = MutableStateFlow<ResultState?>(null)
    val state: StateFlow<ResultState?> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val attempt = container.progress.attempt(attemptId) ?: return@launch
            val content = container.content.content()
            val stats = container.progress.stats.first()
            val kind = ProgressRepository.kindOf(attempt)
            _state.value = ResultState(
                title = content.category(attempt.quizId)?.title ?: attempt.title,
                kind = kind,
                categoryId = attempt.quizId.takeIf { kind == QuizKind.CATEGORY },
                correct = attempt.correct,
                total = attempt.total,
                xp = attempt.xp,
                bonus = attempt.bonus,
                level = levelFor(stats.xp),
                items = container.progress.answersFor(attemptId).mapNotNull { answer ->
                    val question = content.questionsById[answer.questionId] ?: return@mapNotNull null
                    ReviewItem(
                        question = question.text,
                        yourAnswer = question.answers.getOrNull(answer.selected),
                        correctAnswer = question.correctAnswer,
                        isCorrect = answer.isCorrect,
                    )
                },
            )
        }
    }
}

@Composable
fun ResultScreen(onDone: () -> Unit, onPlayAgain: (QuizKind, String?) -> Unit) {
    val vm = appViewModel { c, saved -> ResultViewModel(c, saved) }
    val loaded by vm.state.collectAsStateWithLifecycle()
    val state = loaded ?: return
    val extra = AppTheme.extra

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = ScreenGutter, end = ScreenGutter, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "close") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(Icons.Rounded.Close, stringResource(R.string.close_results), onDone)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(quizTitle(state.kind, state.title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                    Text(stringResource(state.kind.labelRes()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item(key = "score") {
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ScoreRing(state.accuracy, state.correct, state.total)
                Spacer(Modifier.height(16.dp))
                Text(
                    when {
                        state.accuracy == 100 -> stringResource(R.string.res_perfect)
                        state.accuracy >= 80 -> stringResource(R.string.res_brilliant)
                        state.accuracy >= 50 -> stringResource(R.string.res_nice)
                        else -> stringResource(R.string.res_lesson)
                    },
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        if (state.kind != QuizKind.PRACTICE) {
            item(key = "xp") {
                Surface(shape = RoundedCornerShape(24.dp), color = extra.featureCard) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Column(Modifier.weight(1f)) {
                                Overline(stringResource(R.string.xp_earned), color = extra.featureMuted)
                                Text(stringResource(R.string.plus_xp, rememberCountUp(state.xp + state.bonus)), style = MaterialTheme.typography.headlineMedium, color = extra.amber)
                            }
                            if (state.bonus > 0) {
                                Text(stringResource(R.string.bonus_incl, state.bonus), style = MaterialTheme.typography.bodySmall, color = extra.featureMuted)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        LinearMeter(state.level.progress, extra.amber, extra.featureTrack)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            state.level.next?.let { stringResource(R.string.level_progress, levelName(state.level.level), state.level.xpToNext, levelName(it)) }
                                ?: stringResource(R.string.level_top, levelName(state.level.level)),
                            style = MaterialTheme.typography.bodySmall,
                            color = extra.featureMuted,
                        )
                    }
                }
            }
        }
        item(key = "actions") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(stringResource(R.string.play_again), onClick = { onPlayAgain(state.kind, state.categoryId) })
                PrimaryButton(
                    stringResource(R.string.done),
                    onClick = onDone,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (state.items.isNotEmpty()) {
            item(key = "review-title") { SectionHeader(stringResource(R.string.review_answers), Modifier.padding(top = 8.dp)) }
            items(state.items) { item -> ReviewRow(item) }
        }
        item(key = "bottom") { Spacer(Modifier.navigationBarsPadding()) }
    }
}

@Composable
private fun ScoreRing(accuracy: Int, correct: Int, total: Int) {
    val extra = AppTheme.extra
    val track = MaterialTheme.colorScheme.surfaceVariant
    val color = when {
        accuracy >= 80 -> extra.success
        accuracy >= 50 -> MaterialTheme.colorScheme.primary
        else -> extra.danger
    }
    val sweep = remember { Animatable(0f) }
    val scoreLabel = stringResource(R.string.score_cd, correct, total, accuracy)
    LaunchedEffect(accuracy) { sweep.animateTo(accuracy / 100f, tween(900)) }
    Box(
        Modifier.size(168.dp).semantics(mergeDescendants = true) { contentDescription = scoreLabel },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            drawArc(track, -90f, 360f, useCenter = false, style = stroke)
            drawArc(color, -90f, 360f * sweep.value, useCenter = false, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.score, rememberCountUp(correct), total), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
            Text(stringResource(R.string.pct_correct, rememberCountUp(accuracy)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReviewRow(item: ReviewItem) {
    val extra = AppTheme.extra
    AppCard(contentPadding = PaddingValues(16.dp)) {
        Row {
            Box(
                Modifier.size(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(shape = CircleShape, color = if (item.isCorrect) extra.success else extra.danger, modifier = Modifier.size(28.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (item.isCorrect) Icons.Rounded.Check else Icons.Rounded.Close,
                            contentDescription = stringResource(if (item.isCorrect) R.string.correct else R.string.incorrect),
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.question, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                if (!item.isCorrect) {
                    Text(
                        stringResource(R.string.your_answer_is, item.yourAnswer ?: stringResource(R.string.skipped)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = extra.danger,
                    )
                }
                Text(stringResource(R.string.answer_is, item.correctAnswer), style = MaterialTheme.typography.bodyMedium, color = extra.success)
            }
        }
    }
}
