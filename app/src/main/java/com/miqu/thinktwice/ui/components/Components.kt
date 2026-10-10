package com.miqu.thinktwice.ui.components

import com.miqu.thinktwice.ui.theme.tintFor
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.miqu.thinktwice.ui.theme.pressScale
import com.miqu.thinktwice.ui.theme.Motion
import androidx.compose.ui.res.stringResource
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import android.util.LruCache
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miqu.thinktwice.R
import com.miqu.thinktwice.ui.theme.AppTheme

val ScreenGutter = 20.dp

/** White (or raised dark) card with a hairline border. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.surface,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    border: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val stroke = if (border) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    val inner: @Composable () -> Unit = { Column(Modifier.padding(contentPadding), content = content) }
    if (onClick != null) {
        val interaction = remember { MutableInteractionSource() }
        Surface(
            onClick = onClick,
            modifier = modifier.pressScale(interaction),
            shape = shape,
            color = color,
            border = stroke,
            interactionSource = interaction,
            content = inner,
        )
    } else {
        Surface(modifier = modifier, shape = shape, color = color, border = stroke, content = inner)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    trailingIcon: ImageVector? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val container by animateColorAsState(if (enabled) containerColor else MaterialTheme.colorScheme.surfaceVariant, Motion.effects(), label = "btn")
    val animatedContent by animateColorAsState(contentColor, Motion.effects(), label = "btnContent")
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        modifier = modifier.fillMaxWidth().height(56.dp).pressScale(interaction, 0.97f),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = animatedContent,
            disabledContainerColor = container,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        // Label changes slide up into place instead of snapping.
        AnimatedContent(
            targetState = text to trailingIcon,
            transitionSpec = {
                (slideInVertically(Motion.spatial()) { it / 2 } + fadeIn(tween(160, delayMillis = 40))) togetherWith
                    (slideOutVertically(Motion.spatial()) { -it / 2 } + fadeOut(tween(90)))
            },
            label = "buttonLabel",
        ) { (label, icon) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                if (icon != null) {
                    Spacer(Modifier.width(8.dp))
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/** Compact pill used on cards, e.g. "Start quiz". */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = AppTheme.extra.featureCard,
    contentColor: Color = AppTheme.extra.onFeatureCard,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp).pressScale(interaction, 0.94f),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interaction,
    ) {
        Box(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surface,
    content: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 44.dp,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(size).semantics { this.contentDescription = contentDescription },
        shape = CircleShape,
        color = container,
        contentColor = content,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (action != null && onAction != null) {
            TextButton(onClick = onAction, modifier = Modifier.heightIn(min = 44.dp)) {
                Text(action, style = MaterialTheme.typography.labelLarge, color = AppTheme.extra.link)
            }
        }
    }
}

@Composable
fun Overline(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text.uppercase(), modifier = modifier, style = MaterialTheme.typography.labelMedium, color = color)
}

/** Pill-shaped segmented control with a sliding thumb. */
@Composable
fun SegmentedTabs(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = AppTheme.extra
    Surface(
        modifier = modifier.height(48.dp),
        shape = CircleShape,
        color = if (extra.isDark) MaterialTheme.colorScheme.surface else extra.segmentTrack,
        border = if (extra.isDark) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
    ) {
        BoxWithConstraints(Modifier.padding(4.dp)) {
            val segment = maxWidth / options.size
            val thumbOffset by animateDpAsState(segment * selected, Motion.bouncy(), label = "segment")
            Surface(
                modifier = Modifier.offset(x = thumbOffset).width(segment).fillMaxHeight(),
                shape = CircleShape,
                color = extra.segmentThumb,
                shadowElevation = if (extra.isDark) 0.dp else 1.dp,
            ) {}
            Row(Modifier.fillMaxWidth().selectableGroup()) {
                options.forEachIndexed { index, label ->
                    val active = index == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .selectable(selected = active, role = Role.Tab, onClick = { onSelect(index) }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AvatarBubble(emoji: String, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = (size.value * 0.5f).sp)
    }
}

@Composable
fun StreakChip(days: Int, modifier: Modifier = Modifier) {
    val extra = AppTheme.extra
    val streakLabel = stringResource(R.string.streak_cd, days)
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = streakLabel
        },
        shape = CircleShape,
        color = if (days > 0) extra.amberContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Rounded.LocalFireDepartment,
                contentDescription = null,
                tint = if (days > 0) Color(0xFFE8772E) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            // The number rolls up when the streak grows.
            AnimatedContent(
                targetState = days,
                transitionSpec = { (slideInVertically(Motion.spatial()) { it } + fadeIn()) togetherWith (slideOutVertically(Motion.spatial()) { -it } + fadeOut()) },
                label = "streak",
            ) { value ->
                Text(
                    if (value == 1) stringResource(R.string.streak_one_day) else stringResource(R.string.streak_days, value),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (days > 0) extra.onAmberContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** A row of short bars, one per question. */
@Composable
fun SegmentedProgress(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    gap: Dp = 4.dp,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
        colors.forEach { color ->
            val animated by animateColorAsState(color, Motion.effects(), label = "segment")
            Box(Modifier.weight(1f).height(height).clip(CircleShape).background(animated))
        }
    }
}

/** Continuous progress bar with a visible starting dot even at 0%. */
@Composable
fun LinearMeter(
    progress: Float,
    color: Color,
    track: Color,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
) {
    // Fills from empty when it first appears, then springs to new values.
    val fill = remember { Animatable(0f) }
    LaunchedEffect(progress) { fill.animateTo(progress.coerceIn(0f, 1f), spring(dampingRatio = 0.85f, stiffness = 120f)) }
    val animated = fill.value
    BoxWithConstraints(modifier.fillMaxWidth().height(height).clip(CircleShape).background(track)) {
        Box(
            Modifier
                .fillMaxHeight()
                .width((maxWidth * animated).coerceAtLeast(height))
                .clip(CircleShape)
                .background(color),
        )
    }
}

/**
 * Decoded artwork shared across screens. painterResource decodes a bitmap again every time an item
 * enters composition (e.g. while scrolling a grid); caching keeps scrolling and tab switches smooth.
 */
private val artCache = object : LruCache<Int, ImageBitmap>(24 * 1024 * 1024) {
    override fun sizeOf(key: Int, value: ImageBitmap): Int = value.asAndroidBitmap().byteCount
}

@Composable
fun rememberArt(@DrawableRes res: Int): Painter {
    val resources = LocalContext.current.resources
    return remember(res) {
        val bitmap = artCache.get(res) ?: ImageBitmap.imageResource(resources, res).also {
            it.prepareToDraw()
            artCache.put(res, it)
        }
        BitmapPainter(bitmap)
    }
}

@Composable
fun ArtImage(@DrawableRes res: Int, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Fit) {
    Image(rememberArt(res), contentDescription = null, modifier = modifier, contentScale = contentScale)
}

@DrawableRes
fun topicIcon(categoryId: String): Int = when (categoryId) {
    "science" -> R.drawable.ic_topic_science
    "world" -> R.drawable.ic_topic_world
    "movies" -> R.drawable.ic_topic_movies
    "history" -> R.drawable.ic_topic_history
    "brain" -> R.drawable.ic_topic_brain
    "flags" -> R.drawable.ic_topic_flags
    "animals" -> R.drawable.ic_topic_animals
    "sports" -> R.drawable.ic_topic_sports
    "tech" -> R.drawable.ic_topic_tech
    "food" -> R.drawable.ic_topic_food
    "space" -> R.drawable.ic_topic_space
    "music" -> R.drawable.ic_topic_music
    "nature" -> R.drawable.ic_topic_nature
    "myths" -> R.drawable.ic_topic_myths
    "art" -> R.drawable.ic_topic_art
    "landmarks" -> R.drawable.ic_topic_landmarks
    "popculture" -> R.drawable.ic_topic_popculture
    "gaming" -> R.drawable.ic_topic_gaming
    "animation" -> R.drawable.ic_topic_animation
    "superheroes" -> R.drawable.ic_topic_superheroes
    "popmusic" -> R.drawable.ic_topic_popmusic
    else -> R.drawable.ic_topic_quick
}

/** A topic's duotone icon, tinted with the topic's colour. */
@Composable
fun TopicIcon(categoryId: String, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(topicIcon(categoryId)),
        contentDescription = null,
        tint = tintFor(categoryId).content(),
        modifier = modifier,
    )
}

@DrawableRes
fun badgeArt(id: String): Int = when (id) {
    "first_step" -> R.drawable.badge_first_step
    "curious_mind" -> R.drawable.badge_curious_mind
    "on_fire" -> R.drawable.badge_on_fire
    "explorer" -> R.drawable.badge_explorer
    "quiz_regular" -> R.drawable.badge_quiz_regular
    "sharp_shooter" -> R.drawable.badge_sharp_shooter
    "perfect_round" -> R.drawable.badge_perfect_round
    "century_club" -> R.drawable.badge_century_club
    "super_star" -> R.drawable.badge_super_star
    "xp_collector" -> R.drawable.badge_xp_collector
    "quiz_champion" -> R.drawable.badge_quiz_champion
    else -> R.drawable.badge_master_mind
}

@Composable
fun ScreenTitleRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = ScreenGutter),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun EmptyMessage(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().widthIn(max = 420.dp).padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
