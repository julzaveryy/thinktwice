package com.miqu.thinktwice.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.withTransaction
import com.miqu.thinktwice.data.content.Content
import com.miqu.thinktwice.data.db.AttemptAnswerEntity
import com.miqu.thinktwice.data.db.AttemptEntity
import com.miqu.thinktwice.data.db.MistakeEntity
import com.miqu.thinktwice.data.db.ThinkTwiceDatabase
import com.miqu.thinktwice.data.prefs.Profile
import com.miqu.thinktwice.data.prefs.SettingsStore
import com.miqu.thinktwice.domain.QuizKind
import com.miqu.thinktwice.domain.QuizPlans
import com.miqu.thinktwice.domain.localEpochDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * One-time, read-only import of progress saved by version 1 of the app
 * (database `think_twice.db` and preferences `think_twice_settings`).
 *
 * - The old files are never modified.
 * - Settings, profile and history are imported independently, so one broken part
 *   doesn't lose the others.
 * - History is written in a single transaction: it is imported completely or not at all.
 * - Runs as [NonCancellable] so leaving the splash screen can't interrupt it halfway.
 */
class LegacyImporter(
    private val context: Context,
    private val database: ThinkTwiceDatabase,
    private val settings: SettingsStore,
) {
    private val dao = database.progressDao()

    suspend fun importIfNeeded(content: Content) = withContext(Dispatchers.IO + NonCancellable) {
        if (settings.legacyImported()) return@withContext

        val dbFile = context.getDatabasePath(LEGACY_DB)
        var profile: Profile? = null
        if (dbFile.exists()) {
            runCatching {
                SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                    profile = runCatching { readProfile(db) }.getOrNull()
                    if (dao.attemptCount() == 0) importHistory(db, content)
                }
            }.onFailure { Log.w(TAG, "Legacy history skipped", it) }
        }

        runCatching { importSettings(content, profile) }
            .onFailure { Log.w(TAG, "Legacy settings skipped", it) }

        settings.markLegacyImported()
    }

    private suspend fun importSettings(content: Content, profile: Profile?) {
        val prefs = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        val onboarded = prefs.getBoolean("onboarding_completed", false)
        if (!onboarded && profile == null) return
        settings.importLegacy(
            onboarded = onboarded,
            profile = profile,
            favorites = prefs.getStringSet("favorite_category_ids", emptySet()).orEmpty()
                .filter { content.category(it) != null }.toSet(),
            sound = prefs.getBoolean("sound", true),
            haptics = prefs.getBoolean("haptic", true),
        )
    }

    private fun readProfile(db: SQLiteDatabase): Profile? =
        db.rawQuery("SELECT displayName, handle FROM profile WHERE id = 1", null).use { c ->
            if (!c.moveToFirst()) return null
            val name = c.text(0).takeUnless { it == "Dummy User" }.orEmpty()
            val handle = c.text(1).removePrefix("@").takeUnless { it == "dummy" }.orEmpty()
            if (name.isBlank() && handle.isBlank()) null else Profile(name = name, handle = handle)
        }

    private data class OldAttempt(
        val id: Long,
        val categoryId: String,
        val title: String,
        val correct: Int,
        val total: Int,
        val xp: Int,
        val at: Long,
    )

    private suspend fun importHistory(db: SQLiteDatabase, content: Content) {
        val attempts = db.rawQuery(
            "SELECT id, categoryId, categoryTitle, correctAnswers, totalAnswers, pointsEarned, completedAt " +
                "FROM quiz_attempts WHERE totalAnswers > 0 ORDER BY completedAt",
            null,
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    val categoryId = c.text(1)
                    if (categoryId.isEmpty()) continue
                    add(OldAttempt(c.getLong(0), categoryId, c.text(2).ifEmpty { categoryId }, c.getInt(3), c.getInt(4), c.getInt(5), c.getLong(6)))
                }
            }
        }
        if (attempts.isEmpty()) return

        val latestByQuestion = mutableMapOf<String, Pair<Long, Boolean>>()
        val answersByAttempt = attempts.associate { old ->
            old.id to db.rawQuery(
                "SELECT questionId, selectedAnswer, isCorrect FROM attempt_answers WHERE attemptId = ? ORDER BY id",
                arrayOf(old.id.toString()),
            ).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        val question = content.questionsById[c.text(0)] ?: continue
                        val correct = c.getInt(2) != 0
                        add(AttemptAnswerEntity(attemptId = 0, questionId = question.id, selected = question.answers.indexOf(c.text(1)), isCorrect = correct))
                        latestByQuestion[question.id] = old.at to correct
                    }
                }
            }
        }

        database.withTransaction {
            for (old in attempts) {
                val (quizId, kind) = mapQuiz(old.categoryId)
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
                    answers = answersByAttempt[old.id].orEmpty(),
                    newMistakes = emptyList(),
                    clearedMistakes = emptyList(),
                )
            }
            val mistakes = latestByQuestion.filterValues { !it.second }.map { (id, value) -> MistakeEntity(id, value.first) }
            if (mistakes.isNotEmpty()) dao.upsertMistakes(mistakes)
        }
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

    /** Null-safe text read: v1 columns are NOT NULL, but a damaged file shouldn't crash the import. */
    private fun Cursor.text(index: Int): String = if (isNull(index)) "" else getString(index)

    private companion object {
        const val TAG = "LegacyImporter"
        const val LEGACY_DB = "think_twice.db"
        const val LEGACY_PREFS = "think_twice_settings"
    }
}
