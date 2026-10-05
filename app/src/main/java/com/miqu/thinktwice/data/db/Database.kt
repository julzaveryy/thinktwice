package com.miqu.thinktwice.data.db

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** One finished quiz round. Practice rounds are stored but excluded from stats. */
@Entity(tableName = "attempts", indices = [Index("quizId"), Index("epochDay")])
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val quizId: String,
    val kind: String,
    val title: String,
    val correct: Int,
    val total: Int,
    val xp: Int,
    val bonus: Int,
    val completedAt: Long,
    val epochDay: Long,
)

@Entity(
    tableName = "attempt_answers",
    foreignKeys = [
        ForeignKey(
            entity = AttemptEntity::class,
            parentColumns = ["id"],
            childColumns = ["attemptId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("attemptId"), Index("questionId")],
)
data class AttemptAnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val attemptId: Long,
    val questionId: String,
    /** Index into the question's original answer list, or -1 when skipped / timed out. */
    val selected: Int,
    val isCorrect: Boolean,
)

/** A question the player got wrong and has not yet answered correctly since. */
@Entity(tableName = "mistakes")
data class MistakeEntity(
    @PrimaryKey val questionId: String,
    val missedAt: Long,
)

/** In-progress Daily / Weekly challenge so it can be resumed later the same day or week. */
@Entity(tableName = "challenge_progress")
data class ChallengeProgressEntity(
    @PrimaryKey val quizId: String,
    /** Comma separated original answer indices, in question order (-1 = skipped). */
    val selections: String,
    val updatedAt: Long,
)

data class StatsRow(
    val quizzes: Int,
    val answers: Int,
    val correct: Int,
    val xp: Int,
    val perfect: Int,
)

data class CategoryAccuracyRow(
    @ColumnInfo(name = "quizId") val categoryId: String,
    val correct: Int,
    val total: Int,
)

@Dao
interface ProgressDao {
    @Insert
    suspend fun insertAttempt(attempt: AttemptEntity): Long

    @Insert
    suspend fun insertAnswers(answers: List<AttemptAnswerEntity>)

    @Upsert
    suspend fun upsertMistakes(items: List<MistakeEntity>)

    @Query("DELETE FROM mistakes WHERE questionId IN (:ids)")
    suspend fun deleteMistakes(ids: List<String>)

    @Transaction
    suspend fun recordAttempt(
        attempt: AttemptEntity,
        answers: List<AttemptAnswerEntity>,
        newMistakes: List<MistakeEntity>,
        clearedMistakes: List<String>,
    ): Long {
        val id = insertAttempt(attempt)
        insertAnswers(answers.map { it.copy(attemptId = id) })
        if (clearedMistakes.isNotEmpty()) deleteMistakes(clearedMistakes)
        if (newMistakes.isNotEmpty()) upsertMistakes(newMistakes)
        return id
    }

    @Query("SELECT * FROM attempts ORDER BY completedAt DESC, id DESC")
    fun observeAttempts(): Flow<List<AttemptEntity>>

    @Query("SELECT * FROM attempts WHERE id = :id")
    suspend fun attempt(id: Long): AttemptEntity?

    @Query("SELECT * FROM attempt_answers WHERE attemptId = :attemptId ORDER BY id")
    suspend fun answersFor(attemptId: Long): List<AttemptAnswerEntity>

    @Query(
        """
        SELECT COUNT(*) AS quizzes,
               COALESCE(SUM(total), 0) AS answers,
               COALESCE(SUM(correct), 0) AS correct,
               COALESCE(SUM(xp + bonus), 0) AS xp,
               COALESCE(SUM(CASE WHEN total > 0 AND correct = total THEN 1 ELSE 0 END), 0) AS perfect
        FROM attempts WHERE kind != 'PRACTICE'
        """,
    )
    fun observeStats(): Flow<StatsRow>

    @Query("SELECT DISTINCT epochDay FROM attempts ORDER BY epochDay DESC")
    fun observeActiveDays(): Flow<List<Long>>

    @Query("SELECT MAX(epochDay) FROM attempts")
    suspend fun lastActiveDay(): Long?

    @Query(
        """
        SELECT quizId, SUM(correct) AS correct, SUM(total) AS total
        FROM attempts WHERE kind = 'CATEGORY' GROUP BY quizId
        """,
    )
    fun observeCategoryAccuracy(): Flow<List<CategoryAccuracyRow>>

    @Query("SELECT EXISTS(SELECT 1 FROM attempts WHERE quizId = :quizId)")
    fun observeCompleted(quizId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM attempts WHERE quizId = :quizId)")
    suspend fun isCompleted(quizId: String): Boolean

    @Query("SELECT DISTINCT questionId FROM attempt_answers")
    suspend fun answeredQuestionIds(): List<String>

    @Query("SELECT * FROM mistakes ORDER BY missedAt DESC")
    fun observeMistakes(): Flow<List<MistakeEntity>>

    @Query("SELECT * FROM mistakes ORDER BY missedAt DESC")
    suspend fun mistakes(): List<MistakeEntity>

    @Query("SELECT * FROM challenge_progress")
    fun observeChallengeProgress(): Flow<List<ChallengeProgressEntity>>

    @Query("SELECT * FROM challenge_progress WHERE quizId = :quizId")
    suspend fun challengeProgress(quizId: String): ChallengeProgressEntity?

    @Upsert
    suspend fun upsertChallengeProgress(item: ChallengeProgressEntity)

    @Query("DELETE FROM challenge_progress WHERE quizId = :quizId")
    suspend fun deleteChallengeProgress(quizId: String)

    @Query("SELECT COUNT(*) FROM attempts")
    suspend fun attemptCount(): Int

    @Query("DELETE FROM attempts")
    suspend fun clearAttempts()

    @Query("DELETE FROM mistakes")
    suspend fun clearMistakes()

    @Query("DELETE FROM challenge_progress")
    suspend fun clearChallengeProgress()

    @Transaction
    suspend fun clearAll() {
        clearAttempts()
        clearMistakes()
        clearChallengeProgress()
    }
}

@Database(
    entities = [
        AttemptEntity::class,
        AttemptAnswerEntity::class,
        MistakeEntity::class,
        ChallengeProgressEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ThinkTwiceDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao

    companion object {
        const val NAME = "thinktwice.db"

        fun create(context: Context): ThinkTwiceDatabase =
            Room.databaseBuilder(context, ThinkTwiceDatabase::class.java, NAME).build()
    }
}
