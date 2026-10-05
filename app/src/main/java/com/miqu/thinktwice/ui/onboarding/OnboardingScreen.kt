package com.miqu.thinktwice.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.miqu.thinktwice.AppContainer
import com.miqu.thinktwice.R
import com.miqu.thinktwice.data.content.Category
import com.miqu.thinktwice.data.prefs.Profile
import com.miqu.thinktwice.ui.common.appViewModel
import com.miqu.thinktwice.ui.common.contentFlow
import com.miqu.thinktwice.ui.components.ArtImage
import com.miqu.thinktwice.ui.components.Overline
import com.miqu.thinktwice.ui.components.PrimaryButton
import com.miqu.thinktwice.ui.components.ScreenGutter
import com.miqu.thinktwice.ui.components.SegmentedProgress
import com.miqu.thinktwice.ui.profile.ProfileForm
import com.miqu.thinktwice.ui.settings.TopicPicker
import com.miqu.thinktwice.ui.theme.AppTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OnboardingViewModel(private val container: AppContainer) : ViewModel() {
    val categories: StateFlow<List<Category>> = container.contentFlow().map { it.categories }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun finish(profile: Profile, favorites: Set<String>, then: () -> Unit) {
        viewModelScope.launch {
            container.settings.completeOnboarding(profile, favorites)
            then()
        }
    }
}

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val vm = appViewModel { c, _ -> OnboardingViewModel(c) }
    val categories by vm.categories.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var handle by rememberSaveable { mutableStateOf("") }
    var avatar by rememberSaveable { mutableStateOf(Profile.DEFAULT_AVATAR) }
    var favorites by rememberSaveable { mutableStateOf(emptyList<String>()) }

    BackHandler(enabled = step > 0) { step-- }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        if (step > 0) {
            val track = MaterialTheme.colorScheme.outlineVariant
            val active = MaterialTheme.colorScheme.primary
            SegmentedProgress(
                colors = listOf(active, if (step >= 2) active else track),
                modifier = Modifier.padding(horizontal = ScreenGutter, vertical = 16.dp),
            )
        }
        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "onboarding",
            modifier = Modifier.weight(1f),
        ) { current ->
            when (current) {
                0 -> Welcome(onNext = { step = 1 })
                1 -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = ScreenGutter)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Overline("Step 1 of 2")
                    Text("What should we call you?", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.semantics { heading() })
                    Spacer(Modifier.height(8.dp))
                    ProfileForm(
                        initial = Profile(name, handle, avatar),
                        actionLabel = "Continue",
                        onSubmit = {
                            name = it.name
                            handle = it.handle
                            avatar = it.avatar
                            step = 2
                        },
                    )
                }
                else -> Column(Modifier.fillMaxSize()) {
                    TopicPicker(
                        categories = categories,
                        selected = favorites.toSet(),
                        onToggle = { id -> favorites = if (id in favorites) favorites - id else favorites + id },
                        modifier = Modifier.weight(1f),
                        header = {
                            Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Overline("Step 2 of 2")
                                Text("Pick topics you love", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.semantics { heading() })
                                Text(
                                    "Quick Mix and your For you list will lean on these. You can change them any time.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                    Box(Modifier.padding(horizontal = ScreenGutter, vertical = 16.dp)) {
                        PrimaryButton(
                            if (favorites.isEmpty()) "Skip for now" else "Start playing",
                            onClick = { vm.finish(Profile(name, handle, avatar), favorites.toSet(), onDone) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Welcome(onNext: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = ScreenGutter, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        ArtImage(
            if (AppTheme.extra.isDark) R.drawable.welcome_dark else R.drawable.welcome_light,
            Modifier.fillMaxWidth().widthIn(max = 360.dp).aspectRatio(1f).clip(RoundedCornerShape(36.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.height(32.dp))
        Text(
            "A little quiz.\nA new discovery.",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth().semantics { heading() },
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "440 questions across 22 topics, a fresh Landmark Hunt every day, and a streak worth keeping.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Get started", onNext)
    }
}
