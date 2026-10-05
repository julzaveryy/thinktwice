package com.miqu.thinktwice.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.miqu.thinktwice.data.prefs.ThemeMode

/** Colours Material 3 has no slot for. */
@Immutable
data class ExtraColors(
    val featureCard: Color,
    val onFeatureCard: Color,
    val featureMuted: Color,
    val featureTrack: Color,
    val amber: Color,
    val amberContainer: Color,
    val onAmberContainer: Color,
    val success: Color,
    val successContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val segmentTrack: Color,
    val segmentThumb: Color,
    val navBar: Color,
    val link: Color,
    val isDark: Boolean,
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6ECFD),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF2F80ED),
    onSecondary = Color.White,
    background = Color(0xFFEEF3FF),
    onBackground = Color(0xFF151A26),
    surface = Color.White,
    onSurface = Color(0xFF151A26),
    surfaceVariant = Color(0xFFE6ECFA),
    onSurfaceVariant = Color(0xFF596070),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF5F8FF),
    outline = Color(0xFFB9C2D6),
    outlineVariant = Color(0xFFDCE3F2),
    error = Color(0xFFC8493A),
    onError = Color.White,
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1F2A4D),
    onPrimaryContainer = Color(0xFFD6E2FF),
    secondary = Color(0xFF7AA7FF),
    onSecondary = Color(0xFF0D1117),
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF1E2430),
    onSurfaceVariant = Color(0xFF9DA7B3),
    surfaceContainer = Color(0xFF161B22),
    surfaceContainerHigh = Color(0xFF1E2430),
    outline = Color(0xFF3D4657),
    outlineVariant = Color(0xFF2D3545),
    error = Color(0xFFFF8A7A),
    onError = Color(0xFF0D1117),
)

private val LightExtra = ExtraColors(
    featureCard = Color(0xFF151A26),
    onFeatureCard = Color.White,
    featureMuted = Color(0xFFC9CEDA),
    featureTrack = Color(0xFF3A4152),
    amber = Color(0xFFF5B53D),
    amberContainer = Color(0xFFFCEFD2),
    onAmberContainer = Color(0xFF7A4E00),
    success = Color(0xFF1E7A4C),
    successContainer = Color(0xFFE3F3EA),
    danger = Color(0xFFB23B2E),
    dangerContainer = Color(0xFFFBE7E3),
    segmentTrack = Color(0xFFDDE5F7),
    segmentThumb = Color.White,
    navBar = Color(0xFF151A26),
    link = Color(0xFF2350E0),
    isDark = false,
)

private val DarkExtra = ExtraColors(
    featureCard = Color(0xFF252C3A),
    onFeatureCard = Color.White,
    featureMuted = Color(0xFFC9CEDA),
    featureTrack = Color(0xFF3A4152),
    amber = Color(0xFFF5B53D),
    amberContainer = Color(0xFF3A2E14),
    onAmberContainer = Color(0xFFFFD98A),
    success = Color(0xFF5FD39A),
    successContainer = Color(0xFF16302A),
    danger = Color(0xFFFF8A7A),
    dangerContainer = Color(0xFF3A1E1E),
    segmentTrack = Color(0xFF161B22),
    segmentThumb = Color(0xFF353F52),
    navBar = Color(0xFF1E2430),
    link = Color(0xFF7AA7FF),
    isDark = true,
)

private val LocalExtraColors = staticCompositionLocalOf { LightExtra }

object AppTheme {
    val extra: ExtraColors
        @Composable @ReadOnlyComposable get() = LocalExtraColors.current
}

@Composable
fun ThinkTwiceTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalExtraColors provides if (dark) DarkExtra else LightExtra) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

/** Soft pastel tints used for topic tiles and game-mode icons. */
enum class Tint(val light: Color, val dark: Color, val onLight: Color, val onDark: Color) {
    SKY(Color(0xFFDDEBFF), Color(0xFF1C2B45), Color(0xFF1D4ED8), Color(0xFF9CC2FF)),
    PEACH(Color(0xFFFFE4D9), Color(0xFF3A2620), Color(0xFFB4441F), Color(0xFFFFB49A)),
    MINT(Color(0xFFDDF5E8), Color(0xFF18322A), Color(0xFF1E7A4C), Color(0xFF7FDDB0)),
    LILAC(Color(0xFFECE3FF), Color(0xFF2A2342), Color(0xFF6D3FD1), Color(0xFFC6AEFF)),
    BUTTER(Color(0xFFFFF1CC), Color(0xFF362D15), Color(0xFF8A5A00), Color(0xFFFFD77A)),
    ROSE(Color(0xFFFFE1EA), Color(0xFF3A1F2A), Color(0xFFB4235A), Color(0xFFFFA3C2));

    @Composable @ReadOnlyComposable
    fun container(): Color = if (AppTheme.extra.isDark) dark else light

    @Composable @ReadOnlyComposable
    fun content(): Color = if (AppTheme.extra.isDark) onDark else onLight
}

fun tintFor(categoryId: String): Tint = when (categoryId) {
    "science", "space", "tech", "world" -> Tint.SKY
    "food", "sports", "history", "superheroes" -> Tint.PEACH
    "animals", "nature", "flags", "landmarks" -> Tint.MINT
    "myths", "art", "gaming", "brain" -> Tint.LILAC
    "quick", "music", "animation" -> Tint.BUTTER
    else -> Tint.ROSE
}
