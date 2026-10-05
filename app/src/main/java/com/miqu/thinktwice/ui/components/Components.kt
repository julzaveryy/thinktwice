package com.miqu.thinktwice.ui.components

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
import androidx.compose.ui.res.painterResource
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
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, border = stroke, content = inner)
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
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
        if (trailingIcon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
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
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
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
            val thumbOffset by animateDpAsState(segment * selected, label = "segment")
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
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "$days day streak"
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
            Text(
                if (days == 1) "1 day" else "$days days",
                style = MaterialTheme.typography.labelLarge,
                color = if (days > 0) extra.onAmberContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
            Box(Modifier.weight(1f).height(height).clip(CircleShape).background(color))
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
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "meter")
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

@Composable
fun ArtImage(@DrawableRes res: Int, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Fit) {
    Image(painterResource(res), contentDescription = null, modifier = modifier, contentScale = contentScale)
}

@DrawableRes
fun topicArt(categoryId: String): Int = when (categoryId) {
    "science" -> R.drawable.topic_science
    "world", "landmarks" -> R.drawable.topic_world
    "movies", "popculture", "animation" -> R.drawable.topic_movies
    "gaming", "tech" -> R.drawable.topic_tech
    "superheroes", "myths" -> R.drawable.topic_myths
    "music", "popmusic" -> R.drawable.topic_music
    "history" -> R.drawable.topic_history
    "brain" -> R.drawable.topic_brain
    "flags" -> R.drawable.topic_flags
    "animals" -> R.drawable.topic_animals
    "sports" -> R.drawable.topic_sports
    "food" -> R.drawable.topic_food
    "space" -> R.drawable.topic_space
    "nature" -> R.drawable.topic_nature
    "art" -> R.drawable.topic_art
    else -> R.drawable.topic_quick
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
