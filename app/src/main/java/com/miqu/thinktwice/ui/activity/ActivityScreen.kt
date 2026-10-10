package com.miqu.thinktwice.ui.activity

import com.miqu.thinktwice.ui.common.KeepAppLanguage
import com.miqu.thinktwice.ui.theme.Motion
import com.miqu.thinktwice.ui.common.quizTitle
import com.miqu.thinktwice.ui.common.labelRes
import androidx.annotation.StringRes
import com.miqu.thinktwice.R
import androidx.compose.ui.res.stringResource
import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.miqu.thinktwice.AppContainer
import com.miqu.thinktwice.data.ProgressRepository
import com.miqu.thinktwice.data.content.Question
import com.miqu.thinktwice.data.db.AttemptEntity
import com.miqu.thinktwice.domain.Stats
import com.miqu.thinktwice.domain.dayOfWeek
import com.miqu.thinktwice.ui.common.appViewModel
import com.miqu.thinktwice.ui.common.contentFlow
import com.miqu.thinktwice.ui.components.AppCard
import com.miqu.thinktwice.ui.components.CircleIconButton
import com.miqu.thinktwice.ui.components.EmptyMessage
import com.miqu.thinktwice.ui.components.Overline
import com.miqu.thinktwice.ui.components.PillButton
import com.miqu.thinktwice.ui.components.PrimaryButton
import com.miqu.thinktwice.ui.components.ScreenGutter
import com.miqu.thinktwice.ui.components.SectionHeader
import com.miqu.thinktwice.ui.components.SegmentedTabs
import com.miqu.thinktwice.ui.navigation.BottomBarClearance
import com.miqu.thinktwice.ui.theme.AppTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class HistorySort(@StringRes val label: Int) { RECENT(R.string.sort_recent), LOWEST(R.string.sort_lowest), HIGHEST(R.string.sort_highest) }

data class MistakeItem(val question: Question, val categoryTitle: String)

data class ActivityState(
    val loading: Boolean = true,
    val stats: Stats = Stats(),
    val today: Long = 0,
    val activeDays: Set<Long> = emptySet(),
    val attempts: List<AttemptEntity> = emptyList(),
    val mistakes: List<MistakeItem> = emptyList(),
    val topicTitles: Map<String, String> = emptyMap(),
)

class ActivityViewModel(container: AppContainer) : ViewModel() {
    val state: StateFlow<ActivityState> = combine(
        container.contentFlow(),
        container.progress.stats,
        container.progress.attempts,
        container.progress.mistakes,
        combine(container.progress.activeDays, container.clock.today) { days, today -> days.toSet() to today },
    ) { content, stats, attempts, mistakes, (days, today) ->
        ActivityState(
            loading = false,
            stats = stats,
            today = today,
            activeDays = days,
            attempts = attempts,
            topicTitles = content.categories.associate { it.id to it.title },
            mistakes = mistakes.mapNotNull { m ->
                content.questionsById[m.questionId]?.let { MistakeItem(it, content.categoryTitle(it.categoryId)) }
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityState())
}

@Composable
fun ActivityScreen(
    onOpenAttempt: (Long) -> Unit,
    onPractice: () -> Unit,
    onStartDaily: () -> Unit,
) {
    val vm = appViewModel { c, _ -> ActivityViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(0) }
    var sort by rememberSaveable { mutableStateOf(HistorySort.RECENT) }
    var sortMenu by remember { mutableStateOf(false) }
    if (state.loading) return

    val history = when (sort) {
        HistorySort.RECENT -> state.attempts
        HistorySort.LOWEST -> state.attempts.sortedBy { it.correct.toFloat() / it.total.coerceAtLeast(1) }
        HistorySort.HIGHEST -> state.attempts.sortedByDescending { it.correct.toFloat() / it.total.coerceAtLeast(1) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = ScreenGutter, end = ScreenGutter, top = 12.dp, bottom = BottomBarClearance),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "tabs") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SegmentedTabs(
                    options = listOf(stringResource(R.string.tab_overview), stringResource(R.string.tab_history), stringResource(R.string.tab_mistakes)),
                    selected = tab,
                    onSelect = { tab = it },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                Box {
                    CircleIconButton(Icons.AutoMirrored.Rounded.Sort, stringResource(R.string.sort_history), onClick = { sortMenu = true }, size = 48.dp)
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        KeepAppLanguage {
                        HistorySort.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(stringResource(option.label), style = MaterialTheme.typography.bodyLarge) },
                                onClick = { sort = option; tab = 1; sortMenu = false },
                                trailingIcon = { if (option == sort) Icon(Icons.Rounded.Check, null) },
                            )
                        }
                    }
                    }
                }
            }
        }

        when (tab) {
            0 -> {
                item(key = "week") { WeekStrip(state.today, state.activeDays, state.stats.streak.current) }
                if (state.stats.quizzes == 0) {
                    item(key = "start") { JourneyCard(onStartDaily) }
                } else {
                    item(key = "stats") { StatGrid(state.stats) }
                    if (state.mistakes.isNotEmpty()) {
                        item(key = "practice") { PracticeCard(state.mistakes.size, onPractice) }
                    }
                    item(key = "recent-title") { SectionHeader(stringResource(R.string.recent), action = stringResource(R.string.see_all), onAction = { tab = 1 }) }
                    items(state.attempts.take(3), key = { "recent_${it.id}" }) { AttemptRow(it, state.topicTitles) { onOpenAttempt(it.id) } }
                }
            }
            1 -> {
                if (history.isEmpty()) {
                    item(key = "history-empty") { EmptyMessage(stringResource(R.string.no_quizzes), stringResource(R.string.no_quizzes_body)) }
                }
                items(history, key = { "history_${it.id}" }) {
                    Box(Modifier.animateItem(placementSpec = Motion.spatial())) { AttemptRow(it, state.topicTitles) { onOpenAttempt(it.id) } }
                }
            }
            else -> {
                if (state.mistakes.isEmpty()) {
                    item(key = "mistakes-empty") {
                        EmptyMessage(stringResource(R.string.no_mistakes), stringResource(R.string.no_mistakes_body))
                    }
                } else {
                    item(key = "practice-button") {
                        PrimaryButton(
                            if (state.mistakes.size == 1) stringResource(R.string.practice_one)
                            else stringResource(R.string.practice_n, state.mistakes.size),
                            onPractice,
                        )
                    }
                    items(state.mistakes, key = { "mistake_${it.question.id}" }) {
                        Box(Modifier.animateItem(placementSpec = Motion.spatial())) { MistakeRow(it) }
                    }
                }
            }
        }
    }
}


@Composable
private fun WeekStrip(today: Long, activeDays: Set<Long>, streak: Int) {
    val monday = today - dayOfWeek(today)
    val dayLetters = stringResource(R.string.day_letters)
    val playedLabel = stringResource(R.string.played)
    val notPlayedLabel = stringResource(R.string.not_played)
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Overline(stringResource(R.string.this_week))
                Text(
                    if (streak > 0) stringResource(R.string.streak_n_day, streak) else stringResource(R.string.start_streak),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            dayLetters.forEachIndexed { i, letter ->
                val day = monday + i
                val active = day in activeDays
                val isToday = day == today
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(letter.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(if (active) AppTheme.extra.amber else MaterialTheme.colorScheme.surfaceVariant)
                            .then(
                                if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier,
                            )
                            .semantics { contentDescription = if (active) playedLabel else notPlayedLabel },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (active) Icon(Icons.Rounded.Check, null, tint = Color(0xFF3A2A00), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun JourneyCard(onStart: () -> Unit) {
    val extra = AppTheme.extra
    Surface(shape = RoundedCornerShape(24.dp), color = extra.featureCard) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Overline(stringResource(R.string.activity_overline), color = extra.featureMuted)
            Text(stringResource(R.string.journey_title), style = MaterialTheme.typography.titleLarge, color = extra.onFeatureCard)
            Text(
                stringResource(R.string.journey_body),
                style = MaterialTheme.typography.bodyMedium,
                color = extra.featureMuted,
            )
            Spacer(Modifier.height(4.dp))
            PillButton(stringResource(R.string.play_first_quiz), onStart, containerColor = Color.White, contentColor = Color(0xFF151A26))
        }
    }
}

@Composable
private fun StatGrid(stats: Stats) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.stat_quizzes), stats.quizzes.toString(), Modifier.weight(1f))
            StatCard(stringResource(R.string.stat_accuracy), stringResource(R.string.percent, stats.accuracy), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.stat_total_xp), stats.xp.toString(), Modifier.weight(1f))
            StatCard(stringResource(R.string.stat_best_streak), stringResource(R.string.days_short, stats.streak.best), Modifier.weight(1f))
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Overline(label)
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun PracticeCard(count: Int, onPractice: () -> Unit) {
    AppCard(onClick = onPractice, color = MaterialTheme.colorScheme.primaryContainer, border = false) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (count == 1) stringResource(R.string.review_one) else stringResource(R.string.review_n, count),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(stringResource(R.string.review_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            PillButton(stringResource(R.string.practice), onPractice, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun AttemptRow(attempt: AttemptEntity, topicTitles: Map<String, String>, onClick: () -> Unit) {
    val kind = ProgressRepository.kindOf(attempt)
    val accuracy = attempt.correct * 100 / attempt.total.coerceAtLeast(1)
    val extra = AppTheme.extra
    AppCard(onClick = onClick, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(quizTitle(kind, topicTitles[attempt.quizId] ?: attempt.title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(kind.labelRes()) + " · " +
                        DateUtils.getRelativeTimeSpanString(attempt.completedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.score, attempt.correct, attempt.total), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    stringResource(R.string.percent, accuracy),
                    style = MaterialTheme.typography.labelLarge,
                    color = when {
                        accuracy >= 80 -> extra.success
                        accuracy >= 50 -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> extra.danger
                    },
                )
            }
        }
    }
}

@Composable
private fun MistakeRow(item: MistakeItem) {
    AppCard(contentPadding = PaddingValues(16.dp)) {
        Overline(item.categoryTitle)
        Spacer(Modifier.height(4.dp))
        Text(item.question.text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Surface(shape = CircleShape, color = AppTheme.extra.successContainer, border = BorderStroke(0.dp, Color.Transparent)) {
            Text(
                stringResource(R.string.answer_is, item.question.correctAnswer),
                style = MaterialTheme.typography.labelLarge,
                color = AppTheme.extra.success,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}
