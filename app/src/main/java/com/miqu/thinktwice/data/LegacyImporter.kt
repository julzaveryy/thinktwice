package com.miqu.thinktwice.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.miqu.thinktwice.data.content.Content
import com.miqu.thinktwice.data.db.AttemptAnswerEntity
import com.miqu.thinktwice.data.db.AttemptEntity
import com.miqu.thinktwice.data.db.MistakeEntity
import com.miqu.thinktwice.data.db.ProgressDao
import com.miqu.thinktwice.data.prefs.Profile
import com.miqu.thinktwice.data.prefs.SettingsStore
import com.miqu.thinktwice.domain.QuizKind
import com.miqu.thinktwice.domain.QuizPlans
import com.miqu.thinktwice.domain.localEpochDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One-time, read-only import of progress saved by version 1 of the app
 * (database `think_twice.db` and preferences `think_twice_settings`).
 * The old files are left untouched, so nothing is lost if anything fails.
 */
class LegacyImporter(
    private val context: Context,
    private val dao: ProgressDao,
    private val settings: SettingsStore,
) {
    suspend fun importIfNeeded(content: Content) = withContext(Dispatchers.IO) {
        if (settings.legacyImported()) return@withContext
        runCatching { import(content) }.onFailure { Log.w(TAG, "Legacy import skipped", it) }
        settings.markLegacyImported()
    }

    private suspend fun import(content: Content) {
        val prefs = context.getSharedPreferences("think_twice_settings", Context.MODE_PRIVATE)
        val dbFile = context.getDatabasePath("think_twice.db")
        var profile: Profile? = null

        if (dbFile.exists() && dao.attemptCount() == 0) {
            SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                profile = readProfile(db)
                importAttempts(db, content)
            }
        }

        val onboarded = prefs.getBoolean("onboarding_completed", false)
        if (onboarded || profile != null) {
            settings.importLegacy(
                onboarded = onboarded,
                profile = profile,
                favorites = prefs.getStringSet("favorite_category_ids", emptySet()).orEmpty()
                    .filter { content.category(it) != null }.toSet(),
                sound = prefs.getBoolean("sound", true),
                haptics = prefs.getBoolean("haptic", true),
            )
        }
    }

    private fun readProfile(db: SQLiteDatabase): Profile? =
        db.rawQuery("SELECT displayName, handle, avatarEmoji FROM profile WHERE id = 1", null).use { c ->
            if (!c.moveToFirst()) return null
            val name = c.getString(0).orEmpty().takeUnless { it == "Dummy User" }.orEmpty()
            val handle = c.getString(1).orEmpty().removePrefix("@").takeUnless { it == "dummy" }.orEmpty()
            if (name.isBlank() && handle.isBlank()) null
            else Profile(name = name, handle = handle, avatar = Profile.DEFAULT_AVATAR)
        }

    private suspend fun importAttempts(db: SQLiteDatabase, content: Content) {
        data class Old(val id: Long, val categoryId: String, val title: String, val correct: Int, val total: Int, val xp: Int, val at: Long)

        val attempts = db.rawQuery(
            "SELECT id, categoryId, categoryTitle, correctAnswers, totalAnswers, pointsEarned, completedAt " +
                "FROM quiz_attempts WHERE totalAnswers > 0 ORDER BY completedAt",
            null,
        ).use { c ->
            buildList {
                while (c.moveToNext()) add(Old(c.getLong(0), c.getString(1), c.getString(2), c.getInt(3), c.getInt(4), c.getInt(5), c.getLong(6)))
            }
        }
        val latestByQuestion = mutableMapOf<String, Pair<Long, Boolean>>()

        for (old in attempts) {
            val (quizId, kind) = mapQuiz(old.categoryId)
            val answers = db.rawQuery(
                "SELECT questionId, selectedAnswer, isCorrect FROM attempt_answers WHERE attemptId = ? ORDER BY id",
                arrayOf(old.id.toString()),
            ).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        val questionId = c.getString(0)
                        val question = content.questionsById[questionId] ?: continue
                        val correct = c.getInt(2) != 0
                        add(AttemptAnswerEntity(attemptId = 0, questionId = questionId, selected = question.answers.indexOf(c.getString(1)), isCorrect = correct))
                        latestByQuestion[questionId] = old.at to correct
                    }
                }
            }
            dao.recordAttempt(
                attempt = AttemptEntity(
                    quizId = quizId,
                    kind = kind.name,
                    title = old.title,
                    correct = old.correct,
                    total = old.total,
                    xp = old.xp,
                    bonus = 0,
                    completedAt = old.at,
                    epochDay = localEpochDay(old.at),
                ),
                answers = answers,
                newMistakes = emptyList(),
                clearedMistakes = emptyList(),
            )
        }
        val mistakes = latestByQuestion.filterValues { !it.second }.map { (id, value) -> MistakeEntity(id, value.first) }
        if (mistakes.isNotEmpty()) dao.upsertMistakes(mistakes)
    }

    private fun mapQuiz(old: String): Pair<String, QuizKind> = when {
        old.startsWith("daily_mix_") -> QuizPlans.dailyId(old.removePrefix("daily_mix_").toLongOrNull() ?: 0) to QuizKind.DAILY
        old.startsWith("weekly_spotlight_") -> QuizPlans.weeklyId(old.removePrefix("weekly_spotlight_").toLongOrNull() ?: 0) to QuizKind.WEEKLY
        old == "mode_time" -> "time_attack" to QuizKind.TIME_ATTACK
        old == "mode_survival" -> "survival" to QuizKind.SURVIVAL
        old == "mode_endless" -> "endless" to QuizKind.ENDLESS
        old.startsWith("quick_mix") -> "quick_mix" to QuizKind.QUICK_MIX
        else -> old to QuizKind.CATEGORY
    }

    private companion object {
        const val TAG = "LegacyImporter"
    }
}
