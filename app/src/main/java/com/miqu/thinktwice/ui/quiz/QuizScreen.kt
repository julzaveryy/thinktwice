package com.miqu.thinktwice.ui.quiz

import androidx.compose.foundation.layout.widthIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.ripple
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import com.miqu.thinktwice.ui.theme.pressScale
import com.miqu.thinktwice.ui.theme.Motion
import com.miqu.thinktwice.ui.common.quizTitle
import com.miqu.thinktwice.ui.common.labelRes
import com.miqu.thinktwice.R
import androidx.compose.ui.res.stringResource
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
        if (!ui.checked || ui.loading || feedbackGiven == ui.index || ui.silentIndex == ui.index) return@LaunchedEffect
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
            CircleIconButton(Icons.Rounded.Close, stringResource(R.string.exit_quiz), onClick = { if (wantsConfirm) confirmExit = true else onClose() })
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (plan.kind == QuizKind.CATEGORY) plan.title else quizTitle(plan.kind, null), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (plan.isOpenEnded) stringResource(R.string.question_n, ui.index + 1)
                    else stringResource(R.string.question_n_of, ui.index + 1, ui.questions.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (plan.isOpenEnded) {
                TextButton(onClick = vm::finish, enabled = ui.selections.isNotEmpty(), modifier = Modifier.heightIn(min = 44.dp)) {
                    Text(stringResource(R.string.end), style = MaterialTheme.typography.titleSmall)
                }
            } else {
                TextButton(onClick = vm::skip, enabled = !ui.checked, modifier = Modifier.heightIn(min = 44.dp)) {
                    Text(stringResource(R.string.skip), style = MaterialTheme.typography.titleSmall)
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
                // Shared-axis: the old question leaves before the new one arrives, so text never overlaps.
                Motion.sharedAxisIn(forward = true) togetherWith Motion.sharedAxisOut(forward = true)
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
            // One button whose label and colour morph between states (instead of swapping buttons).
            val label: String
            val action: () -> Unit
            var enabled = true
            var dark = false
            var arrow = false
            when {
                ui.timeUp -> { label = stringResource(R.string.times_up); action = vm::finish }
                ui.checked && ui.isOver -> { label = stringResource(R.string.see_results); action = vm::next; arrow = true }
                ui.checked -> { label = stringResource(R.string.next_question); action = vm::next; dark = true; arrow = true }
                ui.picked != null -> { label = stringResource(R.string.check_answer); action = vm::check }
                else -> { label = stringResource(R.string.pick_answer); action = {}; enabled = false }
            }
            PrimaryButton(
                text = label,
                onClick = action,
                enabled = enabled,
                containerColor = if (dark) extra.featureCard else MaterialTheme.colorScheme.primary,
                contentColor = if (dark) extra.onFeatureCard else MaterialTheme.colorScheme.onPrimary,
                trailingIcon = if (arrow) Icons.AutoMirrored.Rounded.ArrowForward else null,
            )
        }
    }

    LeaveSheet(
        visible = confirmExit,
        openEnded = plan.isOpenEnded,
        onKeepPlaying = { confirmExit = false },
        onLeave = { confirmExit = false; onClose() },
        onEndAndSave = { confirmExit = false; vm.finish() },
    )
}

/**
 * Leave confirmation: a small card floating in the middle of the screen. Drawn inside the quiz
 * screen (not a separate dialog window) so it follows the app language and the app's motion.
 */
@Composable
private fun LeaveSheet(
    visible: Boolean,
    openEnded: Boolean,
    onKeepPlaying: () -> Unit,
    onLeave: () -> Unit,
    onEndAndSave: () -> Unit,
) {
    BackHandler(enabled = visible, onBack = onKeepPlaying)
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = visible, enter = fadeIn(tween(180)), exit = fadeOut(tween(160))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onKeepPlaying),
            )
        }
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 28.dp),
            enter = scaleIn(Motion.bouncy(), initialScale = 0.9f) + fadeIn(tween(140)),
            exit = scaleOut(Motion.effects(), targetScale = 0.95f) + fadeOut(tween(120)),
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 24.dp,
                modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp),
            ) {
                Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 18.dp)) {
                    Text(
                        stringResource(R.string.leave_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(if (openEnded) R.string.leave_open else R.string.leave_round),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(22.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryButton(
                            stringResource(if (openEnded) R.string.end_short else R.string.leave_short),
                            onClick = if (openEnded) onEndAndSave else onLeave,
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (openEnded) MaterialTheme.colorScheme.onSurface else AppTheme.extra.danger,
                        )
                        PrimaryButton(
                            stringResource(R.string.keep_playing),
                            onClick = onKeepPlaying,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeStatus(ui: QuizUi) {
    val extra = AppTheme.extra
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        ui.secondsLeft?.let { seconds ->
            val urgent = seconds <= 10
            StatusChip(
                text = stringResource(R.string.seconds_short, seconds),
                container = if (urgent) extra.dangerContainer else MaterialTheme.colorScheme.surface,
                content = if (urgent) extra.danger else MaterialTheme.colorScheme.onSurface,
            ) { Icon(Icons.Rounded.Timer, null, Modifier.size(16.dp)) }
        }
        ui.livesLeft?.let { lives ->
            val total = ui.plan?.lives ?: 0
            val livesLabel = stringResource(R.string.lives_left, lives, total)
            Row(
                Modifier.semantics(mergeDescendants = true) {}.padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                repeat(total) { i ->
                    Icon(
                        if (i < lives) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (i == 0) livesLabel else null,
                        tint = Color(0xFFE5484D),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        StatusChip(
            text = stringResource(R.string.n_correct, ui.correctCount),
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
            Chip(stringResource(question.difficulty.labelRes()), MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurfaceVariant, border = true)
            when {
                practice -> Chip(stringResource(R.string.practice_no_xp), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                awardsXp -> Chip(stringResource(R.string.plus_xp, question.difficulty.xp), extra.amberContainer, extra.onAmberContainer)
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
        AnimatedVisibility(
            visible = ui.checked && isCurrent,
            enter = slideInVertically(Motion.spatial()) { it / 3 } + fadeIn(tween(200)),
            exit = fadeOut(tween(90)),
        ) {
            val correct = ui.lastAnswerCorrect
            AppCard(
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                contentPadding = PaddingValues(16.dp),
            ) {
                Text(
                    when {
                        correct && awardsXp -> stringResource(R.string.fb_correct_xp, question.difficulty.xp)
                        correct -> stringResource(R.string.fb_correct_cleared)
                        ui.picked == -1 -> stringResource(R.string.fb_skipped)
                        else -> stringResource(R.string.fb_wrong)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = if (correct) extra.success else extra.danger,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    question.explanation ?: stringResource(R.string.fb_fallback, question.correctAnswer),
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
    val isReveal = state == AnswerState.Correct || state == AnswerState.Wrong
    val container by animateColorAsState(
        when (state) {
            AnswerState.Idle, AnswerState.Dimmed -> scheme.surface
            AnswerState.Selected -> scheme.primaryContainer
            AnswerState.Correct -> extra.successContainer
            AnswerState.Wrong -> extra.dangerContainer
        },
        Motion.effects(), label = "answerBg",
    )
    val borderColor by animateColorAsState(
        when (state) {
            AnswerState.Idle, AnswerState.Dimmed -> scheme.outlineVariant
            AnswerState.Selected -> scheme.primary
            AnswerState.Correct -> extra.success
            AnswerState.Wrong -> extra.danger
        },
        Motion.effects(), label = "answerBorder",
    )
    val borderWidth by animateDpAsState(if (state == AnswerState.Idle || state == AnswerState.Dimmed) 1.dp else 2.dp, Motion.effects(), label = "answerBorderW")
    val badge by animateColorAsState(
        when (state) {
            AnswerState.Idle, AnswerState.Dimmed -> scheme.surfaceVariant
            AnswerState.Selected -> scheme.primary
            AnswerState.Correct -> extra.success
            AnswerState.Wrong -> extra.danger
        },
        Motion.effects(), label = "answerBadge",
    )
    val dim by animateFloatAsState(if (state == AnswerState.Dimmed) 0.5f else 1f, Motion.effects(), label = "answerDim")
    val badgeContent = if (state == AnswerState.Idle || state == AnswerState.Dimmed) scheme.onSurface else Color.White

    // Correct answers pop, wrong answers give a short shake.
    val pop = remember { Animatable(1f) }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(state) {
        when (state) {
            AnswerState.Correct -> {
                pop.animateTo(1.035f, spring(dampingRatio = 0.5f, stiffness = 900f))
                pop.animateTo(1f, Motion.bouncy())
            }
            AnswerState.Wrong -> shake.animateTo(
                0f,
                keyframes {
                    durationMillis = 380
                    -10f at 50; 9f at 110; -6f at 180; 4f at 250; -2f at 310
                },
            )
            else -> Unit
        }
    }
    val interaction = remember { MutableInteractionSource() }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = container,
        border = BorderStroke(borderWidth, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .graphicsLayer {
                alpha = dim
                scaleX = pop.value
                scaleY = pop.value
                translationX = shake.value * density
            }
            .pressScale(interaction)
            .selectable(
                selected = state == AnswerState.Selected,
                enabled = state == AnswerState.Idle || state == AnswerState.Selected,
                interactionSource = interaction,
                indication = ripple(),
                role = Role.RadioButton,
                onClick = onClick,
            ),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).background(badge, CircleShape), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = state,
                    transitionSpec = { (scaleIn(Motion.bouncy(), initialScale = 0.4f) + fadeIn()) togetherWith fadeOut(tween(80)) },
                    contentKey = { it == AnswerState.Correct || it == AnswerState.Wrong },
                    label = "badge",
                ) { s ->
                    when (s) {
                        AnswerState.Correct -> Icon(Icons.Rounded.Check, null, tint = badgeContent, modifier = Modifier.size(18.dp))
                        AnswerState.Wrong -> Icon(Icons.Rounded.Close, null, tint = badgeContent, modifier = Modifier.size(18.dp))
                        else -> Text(letter, style = MaterialTheme.typography.labelLarge, color = badgeContent)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface, modifier = Modifier.weight(1f))
            AnimatedVisibility(
                visible = isReveal,
                enter = fadeIn(tween(180, delayMillis = 80)) + slideInHorizontally(Motion.spatial()) { it / 2 },
                exit = fadeOut(tween(80)),
            ) {
                Text(
                    stringResource(if (state == AnswerState.Correct) R.string.correct else R.string.your_answer),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (state == AnswerState.Correct) extra.success else extra.danger,
                )
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
