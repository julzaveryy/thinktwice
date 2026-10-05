package com.miqu.thinktwice.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object OnboardingRoute
@Serializable data object HomeRoute
@Serializable data object LibraryRoute
@Serializable data object ActivityRoute
@Serializable data object ProfileRoute
@Serializable data object SettingsRoute
@Serializable data object EditProfileRoute
@Serializable data object TopicsRoute
@Serializable data object BadgesRoute

/** [kind] is a [com.miqu.thinktwice.domain.QuizKind] name; [categoryId] is set for topic quizzes. */
@Serializable data class QuizRoute(val kind: String, val categoryId: String? = null)

@Serializable data class ResultRoute(val attemptId: Long)
