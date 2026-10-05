package com.miqu.thinktwice.ui.quiz

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miqu.thinktwice.data.content.Question
import com.miqu.thinktwice.domain.QuizKind
import com.miqu.thinktwice.domain.QuizPlans
import com.miqu.thinktwice.ui.common.appViewModel
import com.miqu.thinktwice.ui.components.AppCard
import com.miqu.thinktwice.ui.components.CircleIconButton
import com.miqu.thinktwice.ui.components.PrimaryButton
import com.miqu.thinktwice.ui.components.ScreenGutter
import com.miqu.thinktwice.ui.components.SegmentedProgress
import com.miqu.thinktwice.ui.theme.AppTheme

@Composable
fun QuizScreen(onClose: () -> Unit, onFinished: (Long) -> Unit) {
    val vm = appViewModel { c, saved -> QuizViewModel(c, saved) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val feedback by vm.feedback.collectAsStateWithLifecycle()
    var confirmExit by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(ui.finishedAttemptId) { ui.finishedAttemptId?.let(onFinished) }
    LaunchedEffect(ui.closed) { if (ui.closed) onClose() }

    // Sound + haptic feedback when an answer is revealed.
    val haptics = LocalHapticFeedback.current
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 55) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tone?.release() } }
    // Remember which question already gave feedback so rotation doesn't replay it.
    var feedbackGiven by rememberSaveable { mutableStateOf(-1) }
    LaunchedEffect(ui.checked, ui.index) {
        if (!ui.checked || ui.loading || feedbackGiven == ui.index) return@LaunchedEffect
        feedbackGiven = ui.index
        val (sound, haptic) = feedback
        if (haptic) haptics.performHapticFeedback(if (ui.lastAnswerCorrect) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress)
        if (sound) tone?.startTone(if (ui.lastAnswerCorrect) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_NACK, 120)
    }

    val plan = ui.plan
    val wantsConfirm = plan != null && ui.selections.isNotEmpty() && !plan.resumable && ui.finishedAttemptId == null
    BackHandler(enabled = wantsConfirm) { confirmExit = true }

    if (ui.loading || plan == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }
    val question = ui.question ?: return

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = ScreenGutter, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(Icons.Rounded.Close, "Exit quiz", onClick = { if (wantsConfirm) confirmExit = true else onClose() })
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(plan.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (plan.isOpenEnded) "Question ${ui.index + 1}" else "Question ${ui.index + 1} of ${ui.questions.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (plan.isOpenEnded) {
                TextButton(onClick = vm::finish, enabled = ui.selections.isNotEmpty(), modifier = Modifier.heightIn(min = 44.dp)) {
                    Text("End", style = MaterialTheme.typography.titleSmall)
                }
            } else {
                TextButton(onClick = vm::skip, enabled = !ui.checked, modifier = Modifier.heightIn(min = 44.dp)) {
                    Text("Skip", style = MaterialTheme.typography.titleSmall)
                }
            }
        }

        // Progress
        Box(Modifier.padding(horizontal = ScreenGutter)) {
            if (plan.isOpenEnded) {
                ModeStatus(ui)
            } else {
                val extra = AppTheme.extra
                val track = MaterialTheme.colorScheme.outlineVariant
                val current = MaterialTheme.colorScheme.onBackground
                SegmentedProgress(
                    colors = ui.questions.indices.map { i ->
                        val selection = ui.selections.getOrNull(i)
                        when {
                            selection == null -> if (i == ui.index) current else track
                            selection == ui.questions[i].correctIndex -> extra.success
                            else -> extra.danger
                        }
                    },
                    gap = if (ui.questions.size > 12) 3.dp else 4.dp,
                )
            }
        }

        AnimatedContent(
            targetState = ui.index,
            transitionSpec = {
                (slideInHorizontally { it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 } + fadeOut())
            },
            label = "question",
            modifier = Modifier.weight(1f),
        ) { index ->
            val shown = ui.questions.getOrNull(index) ?: question
            QuestionBody(
                question = shown,
                order = QuizPlans.answerOrder(shown, ui.seed),
                isCurrent = index == ui.index,
                ui = ui,
                awardsXp = plan.awardsXp,
                practice = plan.kind == QuizKind.PRACTICE,
                onPick = vm::pick,
            )
        }

        // Action
        Box(Modifier.padding(horizontal = ScreenGutter, vertical = 16.dp)) {
            val extra = AppTheme.extra
            when {
                ui.timeUp -> PrimaryButton("Time’s up · See results", onClick = vm::finish)
                ui.checked && ui.isOver -> PrimaryButton("See results", onClick = vm::next, trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward)
                ui.checked -> PrimaryButton(
                    "Next question",
                    onClick = vm::next,
                    containerColor = extra.featureCard,
                    contentColor = extra.onFeatureCard,
                    trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                )
                ui.picked != null -> PrimaryButton("Check answer", onClick = vm::check)
                else -> PrimaryButton("Pick an answer", onClick = {}, enabled = false)
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Leave this quiz?") },
            text = {
                Text(
                    if (plan.isOpenEnded) "End now to save your score, or leave without saving."
                    else "Answers from this round won’t be saved.",
                )
            },
            confirmButton = {
                if (plan.isOpenEnded) {
                    TextButton(onClick = { confirmExit = false; vm.finish() }) { Text("End & save") }
                } else {
                    TextButton(onClick = { confirmExit = false; onClose() }) { Text("Leave") }
                }
            },
            dismissButton = {
                if (plan.isOpenEnded) {
                    TextButton(onClick = { confirmExit = false; onClose() }) { Text("Leave") }
                } else {
                    TextButton(onClick = { confirmExit = false }) { Text("Keep playing") }
                }
            },
        )
    }
}

@Composable
private fun ModeStatus(ui: QuizUi) {
    val extra = AppTheme.extra
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        ui.secondsLeft?.let { seconds ->
            val urgent = seconds <= 10
            StatusChip(
                text = "${seconds}s",
                container = if (urgent) extra.dangerContainer else MaterialTheme.colorScheme.surface,
                content = if (urgent) extra.danger else MaterialTheme.colorScheme.onSurface,
            ) { Icon(Icons.Rounded.Timer, null, Modifier.size(16.dp)) }
        }
        ui.livesLeft?.let { lives ->
            val total = ui.plan?.lives ?: 0
            Row(
                Modifier.semantics(mergeDescendants = true) {}.padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                repeat(total) { i ->
                    Icon(
                        if (i < lives) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (i == 0) "$lives of $total lives left" else null,
                        tint = Color(0xFFE5484D),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        StatusChip(
            text = "${ui.correctCount} correct",
            container = extra.successContainer,
            content = extra.success,
        ) { Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) }
    }
}

@Composable
private fun StatusChip(text: String, container: Color, content: Color, icon: @Composable () -> Unit) {
    Surface(shape = CircleShape, color = container, contentColor = content, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.width(4.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun QuestionBody(
    question: Question,
    order: List<Int>,
    isCurrent: Boolean,
    ui: QuizUi,
    awardsXp: Boolean,
    practice: Boolean,
    onPick: (Int) -> Unit,
) {
    val extra = AppTheme.extra
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenGutter)
            .padding(top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(question.difficulty.label, MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurfaceVariant, border = true)
            when {
                practice -> Chip("PRACTICE · NO XP", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                awardsXp -> Chip("+${question.difficulty.xp} XP", extra.amberContainer, extra.onAmberContainer)
            }
        }
        Text(
            question.text,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            order.forEachIndexed { position, original ->
                AnswerOption(
                    letter = "ABCD".getOrElse(position) { '•' }.toString(),
                    text = question.answers[original],
                    state = when {
                        !isCurrent -> AnswerState.Idle
                        !ui.checked -> if (ui.picked == original) AnswerState.Selected else AnswerState.Idle
                        original == question.correctIndex -> AnswerState.Correct
                        original == ui.picked -> AnswerState.Wrong
                        else -> AnswerState.Dimmed
                    },
                    onClick = { onPick(original) },
                )
            }
        }
        AnimatedVisibility(visible = ui.checked && isCurrent, enter = fadeIn(), exit = fadeOut()) {
            val correct = ui.lastAnswerCorrect
            AppCard(
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                contentPadding = PaddingValues(16.dp),
            ) {
                Text(
                    when {
                        correct && awardsXp -> "Nice one · +${question.difficulty.xp} XP"
                        correct -> "Nice one · cleared from Mistakes"
                        ui.picked == -1 -> "Skipped · saved to Mistakes"
                        else -> "Not quite · saved to Mistakes"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = if (correct) extra.success else extra.danger,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    question.explanation ?: "The correct answer is ${question.correctAnswer}.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private enum class AnswerState { Idle, Selected, Correct, Wrong, Dimmed }

@Composable
private fun AnswerOption(letter: String, text: String, state: AnswerState, onClick: () -> Unit) {
    val extra = AppTheme.extra
    val scheme = MaterialTheme.colorScheme
    val (container, border, badge) = when (state) {
        AnswerState.Idle, AnswerState.Dimmed -> Triple(scheme.surface, BorderStroke(1.dp, scheme.outlineVariant), scheme.surfaceVariant)
        AnswerState.Selected -> Triple(scheme.primaryContainer, BorderStroke(2.dp, scheme.primary), scheme.primary)
        AnswerState.Correct -> Triple(extra.successContainer, BorderStroke(2.dp, extra.success), extra.success)
        AnswerState.Wrong -> Triple(extra.dangerContainer, BorderStroke(2.dp, extra.danger), extra.danger)
    }
    val badgeContent = if (state == AnswerState.Idle || state == AnswerState.Dimmed) scheme.onSurface else Color.White
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = container,
        border = border,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .alpha(if (state == AnswerState.Dimmed) 0.55f else 1f)
            .selectable(
                selected = state == AnswerState.Selected,
                enabled = state == AnswerState.Idle || state == AnswerState.Selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).background(badge, CircleShape), contentAlignment = Alignment.Center) {
                when (state) {
                    AnswerState.Correct -> Icon(Icons.Rounded.Check, null, tint = badgeContent, modifier = Modifier.size(18.dp))
                    AnswerState.Wrong -> Icon(Icons.Rounded.Close, null, tint = badgeContent, modifier = Modifier.size(18.dp))
                    else -> Text(letter, style = MaterialTheme.typography.labelLarge, color = badgeContent)
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface, modifier = Modifier.weight(1f))
            when (state) {
                AnswerState.Correct -> Text("Correct", style = MaterialTheme.typography.labelLarge, color = extra.success)
                AnswerState.Wrong -> Text("Your answer", style = MaterialTheme.typography.labelLarge, color = extra.danger)
                else -> Unit
            }
        }
    }
}

@Composable
private fun Chip(text: String, container: Color, content: Color, border: Boolean = false) {
    Surface(
        shape = CircleShape,
        color = container,
        contentColor = content,
        border = if (border) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}
