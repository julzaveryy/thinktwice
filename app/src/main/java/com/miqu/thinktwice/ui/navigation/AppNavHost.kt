package com.miqu.thinktwice.ui.navigation

import androidx.annotation.StringRes
import com.miqu.thinktwice.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.miqu.thinktwice.domain.QuizKind
import com.miqu.thinktwice.ui.activity.ActivityScreen
import com.miqu.thinktwice.ui.home.HomeScreen
import com.miqu.thinktwice.ui.library.LibraryScreen
import com.miqu.thinktwice.ui.onboarding.OnboardingScreen
import com.miqu.thinktwice.ui.profile.BadgesScreen
import com.miqu.thinktwice.ui.profile.EditProfileScreen
import com.miqu.thinktwice.ui.profile.ProfileScreen
import com.miqu.thinktwice.ui.quiz.QuizScreen
import com.miqu.thinktwice.ui.result.ResultScreen
import com.miqu.thinktwice.ui.settings.SettingsScreen
import com.miqu.thinktwice.ui.settings.TopicsScreen
import com.miqu.thinktwice.ui.theme.AppTheme

private data class Tab(
    val route: Any,
    val matches: (NavDestination) -> Boolean,
    @StringRes val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

private val tabs = listOf(
    Tab(HomeRoute, { it.hasRoute<HomeRoute>() }, R.string.tab_home, Icons.Outlined.Home, Icons.Rounded.Home),
    Tab(LibraryRoute, { it.hasRoute<LibraryRoute>() }, R.string.tab_library, Icons.Outlined.AutoStories, Icons.Rounded.AutoStories),
    Tab(ActivityRoute, { it.hasRoute<ActivityRoute>() }, R.string.tab_activity, Icons.Outlined.Insights, Icons.Rounded.Insights),
    Tab(ProfileRoute, { it.hasRoute<ProfileRoute>() }, R.string.tab_profile, Icons.Outlined.AccountCircle, Icons.Rounded.AccountCircle),
)

/** Space reserved at the bottom of tab screens so content clears the floating bar. */
val BottomBarClearance: Dp
    @Composable get() = 104.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

fun NavHostController.startQuiz(kind: QuizKind, categoryId: String? = null) {
    navigate(QuizRoute(kind.name, categoryId)) { launchSingleTop = true }
}

@Composable
fun AppNavHost(onboarded: Boolean) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val destination = entry?.destination
    val showBar = destination != null && tabs.any { it.matches(destination) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        NavHost(
            navController = nav,
            startDestination = if (onboarded) HomeRoute else OnboardingRoute,
            // Tabs fade through (old fades out before the new fades in, so text never overlaps);
            // everything else slides. The quiz rises over the screen below, which stays put.
            enterTransition = { if (betweenTabs()) fadeThroughIn() else slideInHorizontally(tween(300)) { it } },
            exitTransition = {
                when {
                    betweenTabs() -> fadeThroughOut()
                    targetState.destination.hasRoute<QuizRoute>() -> ExitTransition.KeepUntilTransitionsFinished
                    else -> slideOutHorizontally(tween(300)) { -it / 4 }
                }
            },
            popEnterTransition = {
                when {
                    betweenTabs() -> fadeThroughIn()
                    initialState.destination.hasRoute<QuizRoute>() -> EnterTransition.None
                    else -> slideInHorizontally(tween(300)) { -it / 4 }
                }
            },
            popExitTransition = { if (betweenTabs()) fadeThroughOut() else slideOutHorizontally(tween(300)) { it } },
        ) {
            screen<OnboardingRoute> {
                OnboardingScreen(onDone = {
                    nav.navigate(HomeRoute) {
                        popUpTo<OnboardingRoute> { inclusive = true }
                        launchSingleTop = true
                    }
                })
            }
            screen<HomeRoute> {
                HomeScreen(
                    onStartQuiz = { kind, category -> nav.startQuiz(kind, category) },
                    onOpenLibrary = { nav.switchTab(LibraryRoute) },
                    onOpenProfile = { nav.switchTab(ProfileRoute) },
                )
            }
            screen<LibraryRoute> {
                LibraryScreen(onStartTopic = { nav.startQuiz(QuizKind.CATEGORY, it) })
            }
            screen<ActivityRoute> {
                ActivityScreen(
                    onOpenAttempt = { nav.navigate(ResultRoute(it)) },
                    onPractice = { nav.startQuiz(QuizKind.PRACTICE) },
                    onStartDaily = { nav.startQuiz(QuizKind.DAILY) },
                )
            }
            screen<ProfileRoute> {
                ProfileScreen(
                    onEdit = { nav.navigate(EditProfileRoute) },
                    onSettings = { nav.navigate(SettingsRoute) },
                    onBadges = { nav.navigate(BadgesRoute) },
                )
            }
            screen<QuizRoute>(
                enter = { slideInVertically(tween(320)) { it } },
                popExit = { slideOutVertically(tween(280)) { it } },
            ) { entry ->
                QuizScreen(
                    onClose = { nav.popIfCurrent(entry) },
                    onFinished = { attemptId ->
                        nav.navigate(ResultRoute(attemptId)) { popUpTo<QuizRoute> { inclusive = true } }
                    },
                )
            }
            screen<ResultRoute> { entry ->
                ResultScreen(
                    onDone = { nav.popIfCurrent(entry) },
                    onPlayAgain = { kind, category ->
                        if (nav.currentBackStackEntry?.id == entry.id) {
                            nav.navigate(QuizRoute(kind.name, category)) { popUpTo<ResultRoute> { inclusive = true } }
                        }
                    },
                )
            }
            screen<SettingsRoute> { entry ->
                SettingsScreen(
                    onBack = { nav.popIfCurrent(entry) },
                    onEditProfile = { nav.navigate(EditProfileRoute) },
                    onEditTopics = { nav.navigate(TopicsRoute) },
                )
            }
            screen<EditProfileRoute> { entry -> EditProfileScreen(onBack = { nav.popIfCurrent(entry) }) }
            screen<TopicsRoute> { entry -> TopicsScreen(onBack = { nav.popIfCurrent(entry) }) }
            screen<BadgesRoute> { entry -> BadgesScreen(onBack = { nav.popIfCurrent(entry) }) }
        }

        AnimatedVisibility(
            visible = showBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            FloatingTabBar(destination = destination, onSelect = { nav.switchTab(it) })
        }
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.betweenTabs(): Boolean =
    tabs.any { it.matches(initialState.destination) } && tabs.any { it.matches(targetState.destination) }

private fun fadeThroughIn(): EnterTransition = fadeIn(tween(durationMillis = 210, delayMillis = 90))
private fun fadeThroughOut(): ExitTransition = fadeOut(tween(durationMillis = 90))

/**
 * A destination drawn on an opaque background, so a screen sliding over another
 * (including during the predictive back gesture) never shows text through it.
 */
private inline fun <reified T : Any> NavGraphBuilder.screen(
    noinline enter: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition)? = null,
    noinline popExit: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition)? = null,
    crossinline content: @Composable (NavBackStackEntry) -> Unit,
) {
    composable<T>(enterTransition = enter, popExitTransition = popExit) { entry ->
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content(entry) }
    }
}

/** Pops [entry] only if it is still on top, so a double tap on Close/Back can't pop the screen below. */
private fun NavHostController.popIfCurrent(entry: NavBackStackEntry) {
    if (currentBackStackEntry?.id == entry.id) popBackStack()
}

/** Home is always the root under the tabs, even when the graph started on onboarding. */
private fun NavHostController.switchTab(route: Any) {
    navigate(route) {
        popUpTo<HomeRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun FloatingTabBar(destination: NavDestination?, onSelect: (Any) -> Unit) {
    val extra = AppTheme.extra
    Surface(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(66.dp),
        shape = CircleShape,
        color = extra.navBar,
        border = if (extra.isDark) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 12.dp,
    ) {
        Row(
            Modifier.padding(6.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val selected = destination != null && tab.matches(destination)
                val label = stringResource(tab.label)
                Row(
                    modifier = Modifier
                        .weight(if (selected) 1.5f else 1f)
                        .height(54.dp)
                        .clip(CircleShape)
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .selectable(selected = selected, role = Role.Tab, onClick = { if (!selected) onSelect(tab.route) }),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (selected) tab.selectedIcon else tab.icon,
                        contentDescription = if (selected) null else label,
                        tint = if (selected) Color.White else extra.navContent,
                        modifier = Modifier.size(22.dp),
                    )
                    if (selected) {
                        Spacer(Modifier.width(6.dp))
                        Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White, maxLines = 1)
                    }
                }
            }
        }
    }
}
