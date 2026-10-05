package com.miqu.thinktwice.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.selection.selectable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.miqu.thinktwice.AppContainer
import com.miqu.thinktwice.data.prefs.Profile
import com.miqu.thinktwice.data.prefs.SettingsStore
import com.miqu.thinktwice.domain.Badge
import com.miqu.thinktwice.domain.LEVELS
import com.miqu.thinktwice.domain.Stats
import com.miqu.thinktwice.domain.badgesFor
import com.miqu.thinktwice.domain.levelFor
import com.miqu.thinktwice.ui.activity.StatCard
import com.miqu.thinktwice.ui.common.appViewModel
import com.miqu.thinktwice.ui.components.AppCard
import com.miqu.thinktwice.ui.components.AvatarBubble
import com.miqu.thinktwice.ui.components.CircleIconButton
import com.miqu.thinktwice.ui.components.LinearMeter
import com.miqu.thinktwice.ui.components.Overline
import com.miqu.thinktwice.ui.components.PillButton
import com.miqu.thinktwice.ui.components.PrimaryButton
import com.miqu.thinktwice.ui.components.ScreenGutter
import com.miqu.thinktwice.ui.components.SectionHeader
import com.miqu.thinktwice.ui.components.badgeArt
import com.miqu.thinktwice.ui.components.rememberArt
import com.miqu.thinktwice.ui.navigation.BottomBarClearance
import com.miqu.thinktwice.ui.theme.AppTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileState(
    val loading: Boolean = true,
    val profile: Profile = Profile(),
    val stats: Stats = Stats(),
    val badges: List<Badge> = emptyList(),
)

class ProfileViewModel(container: AppContainer) : ViewModel() {
    val state: StateFlow<ProfileState> = combine(container.settings.settings, container.progress.stats) { settings, stats ->
        ProfileState(false, settings.profile, stats, badgesFor(stats))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileState())
}

@Composable
fun ProfileScreen(onEdit: () -> Unit, onSettings: () -> Unit, onBadges: () -> Unit) {
    val vm = appViewModel { c, _ -> ProfileViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    if (state.loading) return
    val stats = state.stats

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = ScreenGutter, end = ScreenGutter, top = 12.dp, bottom = BottomBarClearance),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "identity") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarBubble(state.profile.avatar, size = 56.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(state.profile.displayName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(state.profile.displayHandle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                PillButton("Edit", onEdit, containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(8.dp))
                CircleIconButton(Icons.Rounded.Settings, "Settings", onSettings)
            }
        }
        item(key = "level") { LevelCard(stats.xp) }
        item(key = "stats") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Quizzes", stats.quizzes.toString(), Modifier.weight(1f))
                    StatCard("Accuracy", if (stats.answers == 0) "–" else "${stats.accuracy}%", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Answers", stats.answers.toString(), Modifier.weight(1f))
                    StatCard("Streak", "${stats.streak.current}d", Modifier.weight(1f))
                }
                if (stats.quizzes == 0) {
                    Text(
                        "Play your first quiz to see your stats grow.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item(key = "badges-title") {
            SectionHeader("Badges", action = "See all", onAction = onBadges)
        }
        item(key = "badges") {
            val unlocked = state.badges.count { it.unlocked }
            val next = state.badges.firstOrNull { !it.unlocked }
            AppCard(onClick = onBadges) {
                Text("$unlocked of ${state.badges.size} unlocked", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.badges.sortedByDescending { it.unlocked }.take(4).forEach { badge ->
                        BadgeImage(badge, Modifier.weight(1f))
                    }
                }
                if (next != null) {
                    Spacer(Modifier.height(14.dp))
                    Overline("Next up")
                    Spacer(Modifier.height(4.dp))
                    Text("${next.spec.title} · ${next.spec.description}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    LinearMeter(next.progress, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.surfaceVariant, height = 6.dp)
                }
            }
        }
    }
}

@Composable
private fun LevelCard(xp: Int) {
    val extra = AppTheme.extra
    val level = levelFor(xp)
    Surface(shape = RoundedCornerShape(24.dp), color = extra.featureCard) {
        Column(Modifier.padding(20.dp)) {
            Overline("Your level", color = extra.featureMuted)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${level.level.name} level", style = MaterialTheme.typography.headlineMedium, color = extra.onFeatureCard, modifier = Modifier.weight(1f))
                Text("$xp XP", style = MaterialTheme.typography.titleMedium, color = extra.amber)
            }
            Spacer(Modifier.height(14.dp))
            LinearMeter(level.progress, extra.amber, extra.featureTrack)
            Spacer(Modifier.height(8.dp))
            Text(
                level.next?.let { "${level.xpToNext} XP to ${it.name}" } ?: "Top level reached",
                style = MaterialTheme.typography.bodySmall,
                color = extra.featureMuted,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LEVELS.forEachIndexed { i, l ->
                    val reached = i <= level.index
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (reached) extra.amber.copy(alpha = 0.22f) else extra.featureTrack)
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            l.name.take(3),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (reached) extra.amber else extra.featureMuted,
                        )
                    }
                }
            }
        }
    }
}

private val grayscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
private fun BadgeImage(badge: Badge, modifier: Modifier = Modifier) {
    Image(
        rememberArt(badgeArt(badge.spec.id)),
        contentDescription = badge.spec.title + if (badge.unlocked) ", unlocked" else ", locked",
        colorFilter = if (badge.unlocked) null else grayscale,
        modifier = modifier.aspectRatio(1f).alpha(if (badge.unlocked) 1f else 0.45f),
    )
}

class BadgesViewModel(container: AppContainer) : ViewModel() {
    val badges: StateFlow<List<Badge>?> = container.progress.stats.map { badgesFor(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun BadgesScreen(onBack: () -> Unit) {
    val vm = appViewModel { c, _ -> BadgesViewModel(c) }
    val badges by vm.badges.collectAsStateWithLifecycle()
    val list = badges ?: return
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        DetailTopBar("Badges", onBack)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(start = ScreenGutter, end = ScreenGutter, top = 4.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.navigationBarsPadding(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Overline("${list.count { it.unlocked }} of ${list.size} unlocked")
            }
            items(list, key = { it.spec.id }) { badge ->
                AppCard {
                    BadgeImage(badge, Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(badge.spec.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(badge.spec.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, minLines = 2)
                    Spacer(Modifier.height(10.dp))
                    LinearMeter(badge.progress, if (badge.unlocked) AppTheme.extra.success else MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.surfaceVariant, height = 6.dp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (badge.unlocked) "Unlocked" else "${badge.current.coerceAtMost(badge.spec.target)} / ${badge.spec.target} ${badge.spec.metric.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun DetailTopBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = ScreenGutter, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
    }
}

class EditProfileViewModel(private val store: SettingsStore) : ViewModel() {
    val profile: StateFlow<Profile?> = store.settings.map { it.profile }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun save(profile: Profile, then: () -> Unit) {
        viewModelScope.launch {
            store.setProfile(profile)
            then()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileForm(
    initial: Profile,
    actionLabel: String,
    onSubmit: (Profile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var handle by rememberSaveable { mutableStateOf(initial.handle) }
    var avatar by rememberSaveable { mutableStateOf(initial.avatar) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    )

    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { AvatarBubble(avatar, size = 88.dp) }
        Overline("Pick an avatar")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Profile.AVATARS.forEach { emoji ->
                val selected = emoji == avatar
                Surface(
                    shape = CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { avatar = emoji })
                        .semantics { contentDescription = "Avatar $emoji" },
                ) {
                    Box(contentAlignment = Alignment.Center) { Text(emoji, style = MaterialTheme.typography.headlineSmall) }
                }
            }
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(24) },
            label = { Text("Name") },
            singleLine = true,
            colors = fieldColors,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            supportingText = { Text("${name.length}/24", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = handle,
            onValueChange = { value -> handle = value.lowercase().filter { it.isLetterOrDigit() || it == '_' || it == '.' }.take(20) },
            label = { Text("Username") },
            prefix = { Text("@") },
            singleLine = true,
            colors = fieldColors,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton(
            text = actionLabel,
            onClick = { onSubmit(Profile(name.trim(), handle.trim(), avatar)) },
            enabled = name.isNotBlank(),
        )
    }
}

@Composable
fun EditProfileScreen(onBack: () -> Unit) {
    val vm = appViewModel { c, _ -> EditProfileViewModel(c.settings) }
    val profile by vm.profile.collectAsStateWithLifecycle()
    val initial = profile ?: return
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        DetailTopBar("Edit profile", onBack)
        ProfileForm(
            initial = initial,
            actionLabel = "Save",
            onSubmit = { vm.save(it, onBack) },
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenGutter)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
        )
    }
}
