package com.miqu.thinktwice.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** A context whose resources use [tag] ("en" / "id"), independent of the phone's language. */
fun Context.withLanguage(tag: String): Context {
    val locale = Locale.forLanguageTag(tag)
    val config = Configuration(resources.configuration).apply {
        setLocale(locale)
        setLayoutDirection(locale)
    }
    val localized = createConfigurationContext(config).resources
    return object : ContextWrapper(this) {
        override fun getResources(): Resources = localized
    }
}

/** The app language tag, so popups and dialogs (which get a fresh Context) can re-apply it. */
val LocalAppLanguage = staticCompositionLocalOf<String?> { null }

/** Re-applies the app language inside a popup / dialog window. */
@Composable
fun KeepAppLanguage(content: @Composable () -> Unit) {
    val tag = LocalAppLanguage.current
    if (tag == null) content() else ProvideAppLanguage(tag, content)
}

/** Makes every stringResource() below use the app's chosen language. */
@Composable
fun ProvideAppLanguage(tag: String, content: @Composable () -> Unit) {
    val base = LocalContext.current
    val configuration = LocalConfiguration.current
    val localized = remember(base, configuration, tag) { base.withLanguage(tag) }
    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides localized.resources.configuration,
        LocalAppLanguage provides tag,
        content = content,
    )
}
