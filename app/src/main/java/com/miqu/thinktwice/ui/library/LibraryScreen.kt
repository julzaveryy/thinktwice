package com.miqu.thinktwice.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.miqu.thinktwice.AppContainer
import com.miqu.thinktwice.ui.common.appViewModel
import com.miqu.thinktwice.ui.common.contentFlow
import com.miqu.thinktwice.ui.components.ArtImage
import com.miqu.thinktwice.ui.components.CircleIconButton
import com.miqu.thinktwice.ui.components.EmptyMessage
import com.miqu.thinktwice.ui.components.Overline
import com.miqu.thinktwice.ui.components.ScreenGutter
import com.miqu.thinktwice.ui.components.SegmentedTabs
import com.miqu.thinktwice.ui.components.topicArt
import com.miqu.thinktwice.ui.navigation.BottomBarClearance
import com.miqu.thinktwice.ui.theme.AppTheme
import com.miqu.thinktwice.ui.theme.tintFor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class LibrarySort(val label: String) { DEFAULT("Featured"), NAME("A to Z"), ACCURACY_LOW("Needs practice"), ACCURACY_HIGH("Best accuracy") }

data class LibraryTopic(
    val id: String,
    val title: String,
    val emoji: String,
    val questionCount: Int,
    val accuracy: Int?,
    val favorite: Boolean,
)

data class LibraryState(
    val loading: Boolean = true,
    val topics: List<LibraryTopic> = emptyList(),
    val completed: List<LibraryTopic> = emptyList(),
    val totalTopics: Int = 0,
    val totalQuestions: Int = 0,
)

class LibraryViewModel(container: AppContainer) : ViewModel() {
    val query = MutableStateFlow("")
    val sort = MutableStateFlow(LibrarySort.DEFAULT)

    val state: StateFlow<LibraryState> = combine(
        container.contentFlow(),
        container.progress.categoryAccuracy,
        container.settings.settings,
        query,
        sort,
    ) { content, accuracy, settings, query, sort ->
        val accuracyById = accuracy.associate { it.categoryId to if (it.total == 0) 0 else it.correct * 100 / it.total }
        val all = content.categories.map {
            LibraryTopic(it.id, it.title, it.emoji, it.questions.size, accuracyById[it.id], it.id in settings.favorites)
        }
        val filtered = all.filter { query.isBlank() || it.title.contains(query.trim(), ignoreCase = true) }
        val sorted = when (sort) {
            LibrarySort.DEFAULT -> filtered.sortedByDescending { it.favorite }
            LibrarySort.NAME -> filtered.sortedBy { it.title }
            LibrarySort.ACCURACY_LOW -> filtered.sortedWith(compareBy<LibraryTopic, Int?>(nullsLast()) { it.accuracy })
            LibrarySort.ACCURACY_HIGH -> filtered.sortedWith(compareByDescending<LibraryTopic, Int?>(nullsFirst()) { it.accuracy })
        }
        LibraryState(
            loading = false,
            topics = sorted,
            completed = sorted.filter { it.accuracy != null },
            totalTopics = all.size,
            totalQuestions = content.questionCount,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryState())
}

@Composable
fun LibraryScreen(onStartTopic: (String) -> Unit) {
    val vm = appViewModel { c, _ -> LibraryViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val sort by vm.sort.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(0) }
    var sortMenu by remember { mutableStateOf(false) }
    if (state.loading) return

    val list = if (tab == 0) state.topics else state.completed

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = ScreenGutter, end = ScreenGutter, top = 12.dp, bottom = BottomBarClearance),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "search", span = { GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SearchField(query, { vm.query.value = it }, Modifier.weight(1f))
                Spacer(Modifier.size(10.dp))
                Box {
                    CircleIconButton(Icons.AutoMirrored.Rounded.Sort, "Sort topics", onClick = { sortMenu = true }, size = 52.dp)
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        LibrarySort.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label, style = MaterialTheme.typography.bodyLarge) },
                                onClick = { vm.sort.value = option; sortMenu = false },
                                trailingIcon = {
                                    if (option == sort) Text("✓", color = MaterialTheme.colorScheme.primary)
                                },
                            )
                        }
                    }
                }
            }
        }
        item(key = "tabs", span = { GridItemSpan(maxLineSpan) }) {
            SegmentedTabs(
                options = listOf("Topics", "Completed · ${state.completed.size}"),
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item(key = "count", span = { GridItemSpan(maxLineSpan) }) {
            Overline(
                if (tab == 0) "${state.totalTopics} topics · ${state.totalQuestions} questions"
                else "${state.completed.size} topics played",
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (list.isEmpty()) {
            item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                if (tab == 1 && query.isBlank()) {
                    EmptyMessage("Nothing completed yet", "Finish a topic quiz and it will show up here with your accuracy.")
                } else {
                    EmptyMessage("No topics found", "No match for “$query”. Try another word.")
                }
            }
        }
        items(list, key = { "${tab}_${it.id}" }) { topic ->
            TopicTile(topic, onClick = { onStartTopic(topic.id) })
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.heightIn(min = 52.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(10.dp))
            Box(Modifier.weight(1f).padding(vertical = 15.dp)) {
                if (value.isEmpty()) {
                    Text("Search topics", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search topics" },
                )
            }
            if (value.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun TopicTile(topic: LibraryTopic, onClick: () -> Unit) {
    val tint = tintFor(topic.id)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(10.dp)) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(1.3f).clip(RoundedCornerShape(18.dp)).background(tint.container()),
                contentAlignment = Alignment.Center,
            ) {
                ArtImage(topicArt(topic.id), Modifier.fillMaxSize().padding(10.dp))
                if (topic.accuracy != null) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    ) {
                        Text(
                            "${topic.accuracy}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = AppTheme.extra.success,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                topic.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Text(
                "${topic.questionCount} questions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}
