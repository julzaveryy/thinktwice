package com.miqu.thinktwice.data.content

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class Difficulty(val label: String, val xp: Int) {
    EASY("Easy", 10),
    MEDIUM("Medium", 15),
    HARD("Hard", 20);

    companion object {
        fun parse(value: String): Difficulty =
            entries.firstOrNull { it.label.equals(value, ignoreCase = true) } ?: MEDIUM
    }
}

data class Question(
    val id: String,
    val categoryId: String,
    val text: String,
    val answers: List<String>,
    val correctIndex: Int,
    val difficulty: Difficulty,
    val explanation: String?,
) {
    val correctAnswer: String get() = answers[correctIndex]
}

data class Category(
    val id: String,
    val title: String,
    val emoji: String,
    val questions: List<Question>,
)

/** Immutable quiz catalogue bundled with the app. */
class Content(val categories: List<Category>) {
    val questionsById: Map<String, Question> =
        categories.flatMap { it.questions }.associateBy { it.id }

    val allQuestions: List<Question> get() = questionsById.values.toList()

    fun category(id: String): Category? = categories.firstOrNull { it.id == id }

    fun categoryTitle(id: String): String = category(id)?.title ?: id

    val questionCount: Int get() = questionsById.size
}

@Serializable
private data class ContentDto(
    val version: Int,
    val categories: List<CategoryDto>,
    val questions: List<QuestionDto>,
)

@Serializable
private data class CategoryDto(val id: String, val title: String, val emoji: String, val color: String)

@Serializable
private data class QuestionDto(
    val id: String,
    val category: String,
    val text: String,
    val answers: List<String>,
    val correct: Int,
    val difficulty: String,
    val explanation: String? = null,
)

internal fun parseContent(json: String): Content {
    val dto = Json { ignoreUnknownKeys = true }.decodeFromString(ContentDto.serializer(), json)
    val byCategory = dto.questions.groupBy { it.category }
    return Content(
        dto.categories.map { category ->
            Category(
                id = category.id,
                title = category.title,
                emoji = category.emoji,
                questions = byCategory[category.id].orEmpty().map { q ->
                    Question(
                        id = q.id,
                        categoryId = q.category,
                        text = q.text,
                        answers = q.answers,
                        correctIndex = q.correct,
                        difficulty = Difficulty.parse(q.difficulty),
                        explanation = q.explanation,
                    )
                },
            )
        },
    )
}

/** Loads `assets/content.json` once, off the main thread. */
class ContentRepository(private val context: Context) {
    private val mutex = Mutex()
    @Volatile private var cached: Content? = null

    suspend fun content(): Content = cached ?: mutex.withLock {
        cached ?: withContext(Dispatchers.IO) {
            val json = context.assets.open("content.json").bufferedReader().use { it.readText() }
            parseContent(json)
        }.also { cached = it }
    }
}
