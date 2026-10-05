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
    private val content: Content = parseContent(File("src/main/assets/content.json").readText())

    @Test fun `bundled content is complete and valid`() {
        assertEquals(22, content.categories.size)
        assertEquals(440, content.questionCount)
        content.categories.forEach { assertEquals(it.id, 20, it.questions.size) }
        content.allQuestions.forEach { q ->
            assertEquals(q.id, 4, q.answers.size)
            assertEquals(q.id, 4, q.answers.toSet().size)
            assertTrue(q.id, q.correctIndex in 0..3)
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
