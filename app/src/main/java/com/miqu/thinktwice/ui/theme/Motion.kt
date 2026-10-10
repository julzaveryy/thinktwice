package com.miqu.thinktwice.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset

/**
 * Motion system. Movement uses springs (they keep velocity when interrupted, e.g. by a fast tap
 * or the predictive back gesture, which is what makes motion feel fluid rather than scripted).
 * Colour and opacity use quick, critically damped springs so they never overshoot.
 */
object Motion {
    /** Screen-sized movement: settles in ~350 ms with no visible bounce. */
    fun <T> spatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.9f, stiffness = 380f)

    /** Small, playful movement (selection pills, chips): a hint of overshoot. */
    fun <T> bouncy(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.62f, stiffness = 520f)

    /** Colour / alpha / small scale changes. */
    fun <T> effects(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 1400f)

    private val offsetSpring = spring(dampingRatio = 0.9f, stiffness = 380f, visibilityThreshold = IntOffset.VisibilityThreshold)
    private val riseSpring = spring(dampingRatio = 0.86f, stiffness = 300f, visibilityThreshold = IntOffset.VisibilityThreshold)

    // Shared-axis X (Material): the old screen fades out quickly while the new one slides in
    // and fades in after it, so the two are never visible on top of each other.
    fun sharedAxisIn(forward: Boolean): EnterTransition =
        slideInHorizontally(offsetSpring) { if (forward) it / 5 else -it / 5 } +
            fadeIn(tween(durationMillis = 220, delayMillis = 70))

    fun sharedAxisOut(forward: Boolean): ExitTransition =
        slideOutHorizontally(offsetSpring) { if (forward) -it / 5 else it / 5 } +
            fadeOut(tween(durationMillis = 90))

    // Fade-through for tabs: old content fades away, new content fades and grows in slightly.
    fun fadeThroughIn(): EnterTransition =
        fadeIn(tween(durationMillis = 210, delayMillis = 90)) +
            scaleIn(spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.94f)

    fun fadeThroughOut(): ExitTransition = fadeOut(tween(durationMillis = 90))

    // The quiz rises as a sheet and drops back down.
    fun sheetIn(): EnterTransition = slideInVertically(riseSpring) { it }
    fun sheetOut(): ExitTransition = slideOutVertically(offsetSpring) { it }
}

/** Counts up from 0 to [target] once, for scores and XP. */
@Composable
fun rememberCountUp(target: Int, durationMillis: Int = 900): Int {
    val value = remember { Animatable(0f) }
    LaunchedEffect(target) {
        value.animateTo(target.toFloat(), tween(durationMillis, easing = androidx.compose.animation.core.FastOutSlowInEasing))
    }
    return value.value.toInt()
}

/** Shrinks slightly while pressed and springs back on release. */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource, pressedScale: Float = 0.97f): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = if (pressed) Motion.effects() else Motion.bouncy(),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Entrance for the n-th item of a screen: fades in and rises a little, staggered by [index].
 * When [enabled] is false (e.g. returning to a screen) the item appears immediately.
 */
@Composable
fun Modifier.staggeredEntrance(index: Int, enabled: Boolean = true): Modifier {
    val progress = remember { Animatable(if (enabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            kotlinx.coroutines.delay(40L * index.coerceAtMost(8))
            progress.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 260f))
        }
    }
    return graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * 24f * density
    }
}

