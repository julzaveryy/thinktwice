package com.miqu.thinktwice.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** App language. SYSTEM follows the phone: Indonesian if the phone is set to Indonesian, otherwise English. */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null), ENGLISH("en"), INDONESIAN("id");

    /** The concrete language to use right now: "en" or "id". */
    fun resolve(): String = tag ?: when (java.util.Locale.getDefault().language) {
        "in", "id" -> "id"
        else -> "en"
    }
}

data class Profile(
    val name: String = "",
    val handle: String = "",
    val avatar: String = DEFAULT_AVATAR,
) {
    val displayName: String get() = name.ifBlank { "Explorer" }
    val displayHandle: String get() = "@" + handle.ifBlank { "explorer" }

    companion object {
        const val DEFAULT_AVATAR = "🦊"
        val AVATARS = listOf("🦊", "🐼", "🐯", "🐨", "🐸", "🐙", "🦄", "🐧", "🦉", "🐳", "🐝", "🦁")
    }
}

data class Settings(
    val onboarded: Boolean = false,
    val profile: Profile = Profile(),
    val favorites: Set<String> = emptySet(),
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val reminders: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val onboarded = booleanPreferencesKey("onboarded")
        val name = stringPreferencesKey("name")
        val handle = stringPreferencesKey("handle")
        val avatar = stringPreferencesKey("avatar")
        val favorites = stringSetPreferencesKey("favorites")
        val theme = stringPreferencesKey("theme")
        val sound = booleanPreferencesKey("sound")
        val haptics = booleanPreferencesKey("haptics")
        val reminders = booleanPreferencesKey("reminders")
        val legacyImported = booleanPreferencesKey("legacy_imported")
        val language = stringPreferencesKey("language")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            onboarded = p[Keys.onboarded] ?: false,
            profile = Profile(
                name = p[Keys.name].orEmpty(),
                handle = p[Keys.handle].orEmpty(),
                avatar = p[Keys.avatar] ?: Profile.DEFAULT_AVATAR,
            ),
            favorites = p[Keys.favorites].orEmpty(),
            theme = p[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            sound = p[Keys.sound] ?: true,
            haptics = p[Keys.haptics] ?: true,
            reminders = p[Keys.reminders] ?: false,
            language = p[Keys.language]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: AppLanguage.SYSTEM,
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun completeOnboarding(profile: Profile, favorites: Set<String>) = context.dataStore.edit {
        it[Keys.name] = profile.name.trim()
        it[Keys.handle] = profile.handle.trim()
        it[Keys.avatar] = profile.avatar
        it[Keys.favorites] = favorites
        it[Keys.onboarded] = true
    }

    suspend fun setProfile(profile: Profile) = context.dataStore.edit {
        it[Keys.name] = profile.name.trim()
        it[Keys.handle] = profile.handle.trim()
        it[Keys.avatar] = profile.avatar
    }

    suspend fun setFavorites(ids: Set<String>) = context.dataStore.edit { it[Keys.favorites] = ids }
    suspend fun setTheme(mode: ThemeMode) = context.dataStore.edit { it[Keys.theme] = mode.name }
    suspend fun setSound(on: Boolean) = context.dataStore.edit { it[Keys.sound] = on }
    suspend fun setHaptics(on: Boolean) = context.dataStore.edit { it[Keys.haptics] = on }
    suspend fun setReminders(on: Boolean) = context.dataStore.edit { it[Keys.reminders] = on }
    suspend fun setLanguage(language: AppLanguage) = context.dataStore.edit { it[Keys.language] = language.name }

    suspend fun legacyImported(): Boolean = context.dataStore.data.first()[Keys.legacyImported] ?: false
    suspend fun markLegacyImported() = context.dataStore.edit { it[Keys.legacyImported] = true }

    /** Used by the one-time import from version 1. */
    suspend fun importLegacy(onboarded: Boolean, profile: Profile?, favorites: Set<String>, sound: Boolean, haptics: Boolean) =
        context.dataStore.edit {
            if (profile != null) {
                it[Keys.name] = profile.name
                it[Keys.handle] = profile.handle
                it[Keys.avatar] = profile.avatar
            }
            if (favorites.isNotEmpty()) it[Keys.favorites] = favorites
            it[Keys.sound] = sound
            it[Keys.haptics] = haptics
            if (onboarded) it[Keys.onboarded] = true
        }
}
