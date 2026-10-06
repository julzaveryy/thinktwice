package com.miqu.thinktwice

import com.miqu.thinktwice.data.content.Content
import com.miqu.thinktwice.data.content.parseContent
import com.miqu.thinktwice.domain.QuizPlans
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ContentAndPlansTest {
    private val content: Content = parseContent(File("src/main/assets/content.en.json").readText())
    private val indonesian: Content = parseContent(File("src/main/assets/content.id.json").readText())

    @Test fun `bundled content is complete and valid`() {
        assertEquals(22, content.categories.size)
        assertEquals(880, content.questionCount)
        content.categories.forEach { assertEquals(it.id, 40, it.questions.size) }
        content.allQuestions.forEach { q ->
            assertEquals(q.id, 4, q.answers.size)
            assertEquals(q.id, 4, q.answers.toSet().size)
            assertTrue(q.id, q.correctIndex in 0..3)
            assertTrue(q.id, !q.explanation.isNullOrBlank())
        }
    }

    @Test fun `indonesian content mirrors english ids and answer keys`() {
        assertEquals(content.categories.map { it.id }, indonesian.categories.map { it.id })
        assertEquals(content.questionsById.keys, indonesian.questionsById.keys)
        content.allQuestions.forEach { en ->
            val id = indonesian.questionsById.getValue(en.id)
            assertEquals(en.id, en.correctIndex, id.correctIndex)
            assertEquals(en.id, en.difficulty, id.difficulty)
            assertEquals(en.id, 4, id.answers.toSet().size)
            assertTrue(en.id, !id.explanation.isNullOrBlank())
        }
    }

    @Test fun `daily challenge is stable for a day and changes the next`() {
        val a = QuizPlans.daily(content, 20_000)
        val b = QuizPlans.daily(content, 20_000)
        val c = QuizPlans.daily(content, 20_001)
        assertEquals(a.questions.map { it.id }, b.questions.map { it.id })
        assertNotEquals(a.questions.map { it.id }, c.questions.map { it.id })
        assertEquals(QuizPlans.DAILY_SIZE, a.questions.size)
        assertTrue(a.questions.all { it.categoryId == QuizPlans.DAILY_CATEGORY })
    }

    @Test fun `weekly spotlight mixes three topics`() {
        val plan = QuizPlans.weekly(content, 2_900)
        assertEquals(QuizPlans.WEEKLY_SIZE, plan.questions.size)
        assertEquals(3, plan.questions.map { it.categoryId }.toSet().size)
    }

    @Test fun `topic rounds prefer unseen questions`() {
        val seen = content.category("science")!!.questions.take(15).map { it.id }.toSet()
        val plan = QuizPlans.category(content, "science", seen, seed = 1)!!
        assertEquals(QuizPlans.ROUND_SIZE, plan.questions.size)
        assertEquals(5, plan.questions.take(5).count { it.id !in seen })
    }

    @Test fun `quick mix spreads across favorite topics`() {
        val plan = QuizPlans.quickMix(content, setOf("art", "space"), emptySet(), seed = 3)
        assertEquals(QuizPlans.ROUND_SIZE, plan.questions.size)
        assertEquals(setOf("art", "space"), plan.questions.map { it.categoryId }.toSet())
        assertEquals(plan.questions.size, plan.questions.map { it.id }.toSet().size)
    }

    @Test fun `answer order is a stable permutation`() {
        val q = content.allQuestions.first()
        val order = QuizPlans.answerOrder(q, 42)
        assertEquals(listOf(0, 1, 2, 3), order.sorted())
        assertEquals(order, QuizPlans.answerOrder(q, 42))
    }
}
