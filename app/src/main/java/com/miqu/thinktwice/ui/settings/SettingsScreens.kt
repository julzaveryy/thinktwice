package com.miqu.thinktwice.ui.settings

import com.miqu.thinktwice.ui.common.KeepAppLanguage
import com.miqu.thinktwice.data.prefs.AppLanguage
import com.miqu.thinktwice.R
import androidx.compose.ui.res.stringResource
import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.miqu.thinktwice.AppContainer
import com.miqu.thinktwice.BuildConfig
import com.miqu.thinktwice.data.content.Category
import com.miqu.thinktwice.data.prefs.Settings
import com.miqu.thinktwice.data.prefs.ThemeMode
import com.miqu.thinktwice.reminder.Reminders
import com.miqu.thinktwice.ui.common.appViewModel
import com.miqu.thinktwice.ui.common.contentFlow
import com.miqu.thinktwice.ui.components.AppCard
import com.miqu.thinktwice.ui.components.ArtImage
import com.miqu.thinktwice.ui.components.Overline
import com.miqu.thinktwice.ui.components.PrimaryButton
import com.miqu.thinktwice.ui.components.ScreenGutter
import com.miqu.thinktwice.ui.components.SegmentedTabs
import com.miqu.thinktwice.ui.components.TopicIcon
import com.miqu.thinktwice.ui.profile.DetailTopBar
import com.miqu.thinktwice.ui.theme.AppTheme
import com.miqu.thinktwice.ui.theme.tintFor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    private val store = container.settings

    val settings: StateFlow<Settings?> = store.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { store.setTheme(mode) }
    }
    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { store.setLanguage(language) }
    }
    fun setSound(on: Boolean) {
        viewModelScope.launch { store.setSound(on) }
    }
    fun setHaptics(on: Boolean) {
        viewModelScope.launch { store.setHaptics(on) }
    }

    fun setReminders(on: Boolean, context: Context) {
        viewModelScope.launch {
            store.setReminders(on)
            if (on) Reminders.schedule(context) else Reminders.cancel(context)
        }
    }

    fun resetProgress() {
        viewModelScope.launch { container.progress.resetAll() }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit, onEditProfile: () -> Unit, onEditTopics: () -> Unit) {
    val vm = appViewModel { c, _ -> SettingsViewModel(c) }
    val loaded by vm.settings.collectAsStateWithLifecycle()
    val settings = loaded ?: return
    val context = LocalContext.current
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setReminders(granted, context.applicationContext)
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        DetailTopBar(stringResource(R.string.settings), onBack)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenGutter)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsGroup(stringResource(R.string.appearance)) {
                Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.size(10.dp))
                SegmentedTabs(
                    options = listOf(stringResource(R.string.theme_system), stringResource(R.string.theme_light), stringResource(R.string.theme_dark)),
                    selected = settings.theme.ordinal,
                    onSelect = { vm.setTheme(ThemeMode.entries[it]) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.size(16.dp))
                Text(stringResource(R.string.language), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.size(10.dp))
                SegmentedTabs(
                    options = listOf(stringResource(R.string.lang_system), stringResource(R.string.lang_en), stringResource(R.string.lang_id)),
                    selected = settings.language.ordinal,
                    onSelect = { vm.setLanguage(AppLanguage.entries[it]) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            SettingsGroup(stringResource(R.string.personalization)) {
                LinkRow(stringResource(R.string.edit_profile), settings.profile.displayName, onEditProfile)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LinkRow(
                    stringResource(R.string.favorite_topics),
                    if (settings.favorites.isEmpty()) stringResource(R.string.all_topics) else stringResource(R.string.n_selected, settings.favorites.size),
                    onEditTopics,
                )
            }
            SettingsGroup(stringResource(R.string.preferences)) {
                ToggleRow(stringResource(R.string.sound), stringResource(R.string.sound_body), settings.sound, vm::setSound)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ToggleRow(stringResource(R.string.haptics), stringResource(R.string.haptics_body), settings.haptics, vm::setHaptics)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ToggleRow(stringResource(R.string.daily_reminder), stringResource(R.string.daily_reminder_body), settings.reminders && Reminders.canNotify(context)) { on ->
                    if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Reminders.canNotify(context)) {
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        vm.setReminders(on, context.applicationContext)
                    }
                }
            }
            SettingsGroup(stringResource(R.string.data)) {
                Text(
                    stringResource(R.string.data_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(8.dp))
                TextButton(onClick = { confirmReset = true }, modifier = Modifier.heightIn(min = 44.dp)) {
                    Text(stringResource(R.string.reset_progress), style = MaterialTheme.typography.titleSmall, color = AppTheme.extra.danger)
                }
            }
            Text(
                stringResource(R.string.version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { KeepAppLanguage { Text(stringResource(R.string.reset_title)) } },
            text = { KeepAppLanguage { Text(stringResource(R.string.reset_body)) } },
            confirmButton = {
                TextButton(onClick = { confirmReset = false; vm.resetProgress() }) {
                    KeepAppLanguage { Text(stringResource(R.string.reset), color = AppTheme.extra.danger) }
                }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { KeepAppLanguage { Text(stringResource(R.string.cancel)) } } },
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Overline(title, modifier = Modifier.padding(start = 4.dp))
        AppCard(content = content)
    }
}

@Composable
private fun LinkRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

/* ---------- Favorite topics ---------- */

class TopicsViewModel(private val container: AppContainer) : ViewModel() {
    val state: StateFlow<Pair<List<Category>, Set<String>>?> =
        combine(container.contentFlow(), container.settings.settings) { content, settings -> content.categories to settings.favorites }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(ids: Set<String>, then: () -> Unit) {
        viewModelScope.launch {
            container.settings.setFavorites(ids)
            then()
        }
    }
}

@Composable
fun TopicsScreen(onBack: () -> Unit) {
    val vm = appViewModel { c, _ -> TopicsViewModel(c) }
    val loaded by vm.state.collectAsStateWithLifecycle()
    val (categories, favorites) = loaded ?: return
    var picks by rememberSaveable(favorites) { mutableStateOf(favorites.toList()) }
    val selected = picks.toSet()
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        DetailTopBar(stringResource(R.string.favorite_topics), onBack)
        TopicPicker(categories, selected, onToggle = { id -> picks = if (id in picks) picks - id else picks + id }, modifier = Modifier.weight(1f))
        Box(Modifier.padding(horizontal = ScreenGutter, vertical = 16.dp)) {
            PrimaryButton(if (selected.isEmpty()) stringResource(R.string.use_all_topics) else stringResource(R.string.save_n_topics, selected.size), onClick = { vm.save(selected, onBack) })
        }
    }
}

@Composable
fun TopicPicker(
    categories: List<Category>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = ScreenGutter, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (header != null) item(span = { GridItemSpan(maxLineSpan) }) { header() }
        items(categories, key = { it.id }) { category ->
            val isSelected = category.id in selected
            val tint = tintFor(category.id)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.toggleable(value = isSelected, role = Role.Checkbox, onValueChange = { onToggle(category.id) }),
            ) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tint.container()),
                        contentAlignment = Alignment.Center,
                    ) { TopicIcon(category.id, Modifier.size(22.dp)) }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        category.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isSelected) Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
