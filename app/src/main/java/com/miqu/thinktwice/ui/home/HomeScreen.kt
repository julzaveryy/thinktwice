package com.miqu.thinktwice.ui.home

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.miqu.thinktwice.ui.theme.staggeredEntrance
import androidx.compose.ui.res.stringResource
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miqu.thinktwice.R
import com.miqu.thinktwice.domain.QuizKind
import com.miqu.thinktwice.ui.common.appViewModel
import com.miqu.thinktwice.ui.components.AppCard
import com.miqu.thinktwice.ui.components.ArtImage
import com.miqu.thinktwice.ui.components.AvatarBubble
import com.miqu.thinktwice.ui.components.Overline
import com.miqu.thinktwice.ui.components.PillButton
import com.miqu.thinktwice.ui.components.ScreenGutter
import com.miqu.thinktwice.ui.components.SectionHeader
import com.miqu.thinktwice.ui.components.SegmentedProgress
import com.miqu.thinktwice.ui.components.StreakChip
import com.miqu.thinktwice.ui.components.topicArt
import com.miqu.thinktwice.ui.navigation.BottomBarClearance
import com.miqu.thinktwice.ui.theme.AppTheme
import com.miqu.thinktwice.ui.theme.Tint
import com.miqu.thinktwice.ui.theme.tintFor

/** Text colour used on top of the light pastel artwork, in both themes. */
private val ArtInk = Color(0xFF151A26)
private val ArtInkMuted = Color(0xFF3D4456)

@Composable
fun HomeScreen(
    onStartQuiz: (QuizKind, String?) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val vm = appViewModel { c, _ -> HomeViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    if (state.loading) return
    // Cards rise in one after another the first time Home appears, not on every return to it.
    var played by rememberSaveable { mutableStateOf(false) }
    val animateIn = !played
    LaunchedEffect(Unit) { played = true }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(
            start = ScreenGutter, end = ScreenGutter, top = 12.dp, bottom = BottomBarClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") { Stagger(0, animateIn) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(onClick = onOpenProfile, shape = CircleShape, color = Color.Transparent) {
                    AvatarBubble(state.profile.avatar, size = 44.dp)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.greeting, state.profile.displayName),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StreakChip(state.streak)
            }
        }}
        item(key = "headline") { Stagger(1, animateIn) {
            val headline = when {
                state.daily?.inProgress == true -> stringResource(R.string.headline_continue)
                !state.hasPlayed -> stringResource(R.string.headline_first)
                else -> stringResource(R.string.headline_again)
            }
            Text(
                headline,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp).semantics { heading() },
            )
        }}
        state.daily?.let { daily ->
            item(key = "daily") { Stagger(2, animateIn) { DailyHero(daily, onClick = { onStartQuiz(QuizKind.DAILY, null) }) }}
        }
        item(key = "tiles") { Stagger(3, animateIn) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureTile(
                    modifier = Modifier.weight(1f),
                    art = R.drawable.home_quick_mix,
                    title = stringResource(R.string.title_quick_mix),
                    status = stringResource(R.string.quick_mix_status),
                    action = stringResource(R.string.start_mix),
                    onClick = { onStartQuiz(QuizKind.QUICK_MIX, null) },
                )
                val weekly = state.weekly
                FeatureTile(
                    modifier = Modifier.weight(1f),
                    art = R.drawable.home_weekly,
                    title = stringResource(R.string.title_weekly),
                    status = when {
                        weekly == null -> ""
                        weekly.completed -> stringResource(R.string.weekly_collected)
                        else -> stringResource(R.string.weekly_progress, weekly.answered, weekly.total, weekly.bonus)
                    },
                    action = when {
                        weekly?.completed == true -> stringResource(R.string.replay)
                        weekly?.inProgress == true -> stringResource(R.string.continue_)
                        else -> stringResource(R.string.explore)
                    },
                    onClick = { onStartQuiz(QuizKind.WEEKLY, null) },
                )
            }
        }}
        item(key = "modes-title") { Stagger(4, animateIn) { SectionHeader(stringResource(R.string.game_modes), Modifier.padding(top = 8.dp)) }}
        item(key = "modes") { Stagger(5, animateIn) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModeCard(Modifier.weight(1f), Icons.Rounded.Timer, stringResource(R.string.title_time_attack), stringResource(R.string.seconds_90), Tint.SKY) {
                    onStartQuiz(QuizKind.TIME_ATTACK, null)
                }
                ModeCard(Modifier.weight(1f), Icons.Rounded.Favorite, stringResource(R.string.title_survival), stringResource(R.string.lives_3), Tint.PEACH) {
                    onStartQuiz(QuizKind.SURVIVAL, null)
                }
                ModeCard(Modifier.weight(1f), Icons.Rounded.AllInclusive, stringResource(R.string.title_endless), stringResource(R.string.no_limit), Tint.MINT) {
                    onStartQuiz(QuizKind.ENDLESS, null)
                }
            }
        }}
        item(key = "foryou-title") { Stagger(6, animateIn) {
            SectionHeader(stringResource(R.string.for_you), Modifier.padding(top = 8.dp), action = stringResource(R.string.see_all), onAction = onOpenLibrary)
        }}
        itemsIndexed(state.forYou, key = { _, it -> "topic_${it.id}" }) { i, topic ->
            Stagger(7 + i, animateIn) { TopicRow(topic, onClick = { onStartQuiz(QuizKind.CATEGORY, topic.id) }) }
        }
    }
}

@Composable
private fun Stagger(index: Int, enabled: Boolean, content: @Composable () -> Unit) {
    Box(Modifier.staggeredEntrance(index, enabled)) { content() }
}

@Composable
private fun DailyHero(card: ChallengeCard, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFFDDEBFF),
        modifier = Modifier.fillMaxWidth(),
    ) {
        // The card grows with its text (large font sizes, long titles) instead of clipping the button.
        Box(Modifier.heightIn(min = 240.dp)) {
            ArtImage(R.drawable.home_daily, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            // Soft wash so text stays readable whatever the crop.
            Box(
                Modifier.matchParentSize().background(
                    Brush.horizontalGradient(0f to Color.White.copy(alpha = 0.8f), 0.62f to Color.White.copy(alpha = 0f)),
                ),
            )
            Column(Modifier.fillMaxWidth(0.62f).padding(20.dp)) {
                Overline(stringResource(R.string.daily_challenge), color = ArtInkMuted)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.title_daily), style = MaterialTheme.typography.headlineMedium, color = ArtInk)
                Spacer(Modifier.height(6.dp))
                Text(
                    when {
                        card.completed -> stringResource(R.string.daily_done)
                        else -> stringResource(R.string.daily_progress, card.answered, card.total, card.bonus)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = ArtInkMuted,
                )
                Spacer(Modifier.height(18.dp))
                SegmentedProgress(
                    colors = List(card.total) { i -> if (i < card.answered) MaterialTheme.colorScheme.primary else ArtInk.copy(alpha = 0.14f) },
                    gap = 3.dp,
                    height = 5.dp,
                )
                Spacer(Modifier.height(14.dp))
                PillButton(
                    text = when {
                        card.completed -> stringResource(R.string.play_again)
                        card.inProgress -> stringResource(R.string.continue_)
                        else -> stringResource(R.string.start_quiz)
                    },
                    onClick = onClick,
                    containerColor = ArtInk,
                    contentColor = Color.White,
                )
            }
        }
    }
}

@Composable
private fun FeatureTile(
    modifier: Modifier,
    @DrawableRes art: Int,
    title: String,
    status: String,
    action: String,
    onClick: () -> Unit,
) {
    AppCard(modifier = modifier, onClick = onClick, contentPadding = PaddingValues(0.dp)) {
        ArtImage(
            art,
            Modifier.fillMaxWidth().aspectRatio(1.25f).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
            contentScale = ContentScale.Crop,
        )
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(action, style = MaterialTheme.typography.labelLarge, color = AppTheme.extra.link, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = AppTheme.extra.link, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ModeCard(modifier: Modifier, icon: ImageVector, title: String, detail: String, tint: Tint, onClick: () -> Unit) {
    AppCard(modifier = modifier, onClick = onClick, contentPadding = PaddingValues(14.dp)) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(tint.container()),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = tint.content(), modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
fun TopicRow(topic: TopicCard, onClick: () -> Unit) {
    val tint = tintFor(topic.id)
    AppCard(onClick = onClick, contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(tint.container()),
                contentAlignment = Alignment.Center,
            ) { ArtImage(topicArt(topic.id), Modifier.size(46.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(topic.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    topic.accuracy?.let { stringResource(R.string.topic_accuracy, topic.questionCount, it) }
                        ?: stringResource(R.string.questions_count, topic.questionCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(0.dp, Color.Transparent),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.play_topic, topic.title), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
