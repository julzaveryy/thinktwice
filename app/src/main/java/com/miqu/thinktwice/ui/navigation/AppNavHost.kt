package com.miqu.thinktwice.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.navigation.NavGraph.Companion.findStartDestination
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
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

private val tabs = listOf(
    Tab(HomeRoute, { it.hasRoute<HomeRoute>() }, "Home", Icons.Outlined.Home, Icons.Rounded.Home),
    Tab(LibraryRoute, { it.hasRoute<LibraryRoute>() }, "Library", Icons.Outlined.AutoStories, Icons.Rounded.AutoStories),
    Tab(ActivityRoute, { it.hasRoute<ActivityRoute>() }, "Activity", Icons.Outlined.Insights, Icons.Rounded.Insights),
    Tab(ProfileRoute, { it.hasRoute<ProfileRoute>() }, "Profile", Icons.Outlined.AccountCircle, Icons.Rounded.AccountCircle),
)

/** Space reserved at the bottom of tab screens so content clears the floating bar. */
val BottomBarClearance = 104.dp

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
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() },
        ) {
            composable<OnboardingRoute> {
                OnboardingScreen(onDone = {
                    nav.navigate(HomeRoute) { popUpTo<OnboardingRoute> { inclusive = true } }
                })
            }
            composable<HomeRoute> {
                HomeScreen(
                    onStartQuiz = { kind, category -> nav.startQuiz(kind, category) },
                    onOpenLibrary = { nav.switchTab(LibraryRoute) },
                    onOpenProfile = { nav.switchTab(ProfileRoute) },
                )
            }
            composable<LibraryRoute> {
                LibraryScreen(onStartTopic = { nav.startQuiz(QuizKind.CATEGORY, it) })
            }
            composable<ActivityRoute> {
                ActivityScreen(
                    onOpenAttempt = { nav.navigate(ResultRoute(it)) },
                    onPractice = { nav.startQuiz(QuizKind.PRACTICE) },
                    onStartDaily = { nav.startQuiz(QuizKind.DAILY) },
                )
            }
            composable<ProfileRoute> {
                ProfileScreen(
                    onEdit = { nav.navigate(EditProfileRoute) },
                    onSettings = { nav.navigate(SettingsRoute) },
                    onBadges = { nav.navigate(BadgesRoute) },
                )
            }
            composable<QuizRoute>(
                enterTransition = { slideInVertically { it / 6 } + fadeIn() },
                exitTransition = { ExitTransition.None },
                popExitTransition = { slideOutVertically { it / 6 } + fadeOut() },
                popEnterTransition = { EnterTransition.None },
            ) {
                QuizScreen(
                    onClose = { nav.popBackStack() },
                    onFinished = { attemptId ->
                        nav.navigate(ResultRoute(attemptId)) { popUpTo<QuizRoute> { inclusive = true } }
                    },
                )
            }
            composable<ResultRoute> {
                ResultScreen(
                    onDone = { nav.popBackStack() },
                    onPlayAgain = { kind, category ->
                        nav.navigate(QuizRoute(kind.name, category)) { popUpTo<ResultRoute> { inclusive = true } }
                    },
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(
                    onBack = { nav.popBackStack() },
                    onEditProfile = { nav.navigate(EditProfileRoute) },
                    onEditTopics = { nav.navigate(TopicsRoute) },
                )
            }
            composable<EditProfileRoute> { EditProfileScreen(onBack = { nav.popBackStack() }) }
            composable<TopicsRoute> { TopicsScreen(onBack = { nav.popBackStack() }) }
            composable<BadgesRoute> { BadgesScreen(onBack = { nav.popBackStack() }) }
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

private fun NavHostController.switchTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
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
        shadowElevation = 12.dp,
    ) {
        Row(
            Modifier.padding(6.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val selected = destination != null && tab.matches(destination)
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
                        contentDescription = if (selected) null else tab.label,
                        tint = if (selected) Color.White else Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.size(22.dp),
                    )
                    if (selected) {
                        Spacer(Modifier.width(6.dp))
                        Text(tab.label, style = MaterialTheme.typography.labelLarge, color = Color.White, maxLines = 1)
                    }
                }
            }
        }
    }
}
