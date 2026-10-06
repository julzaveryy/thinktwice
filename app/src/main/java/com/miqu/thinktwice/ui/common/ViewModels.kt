package com.miqu.thinktwice.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miqu.thinktwice.AppContainer
import com.miqu.thinktwice.appContainer
import com.miqu.thinktwice.data.content.Content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Creates a screen ViewModel with access to the app container and its SavedStateHandle. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    crossinline create: (AppContainer, SavedStateHandle) -> VM,
): VM {
    val container = LocalContext.current.appContainer
    return viewModel { create(container, createSavedStateHandle()) }
}

/** Quiz content that follows the language setting. */
fun AppContainer.contentFlow(): Flow<Content> =
    settings.settings.map { it.language.resolve() }.distinctUntilChanged().map { content.content(it) }
