package com.jayathu.minstagram

import com.jayathu.minstagram.domain.QuizBank
import com.jayathu.minstagram.domain.QuizQuestion
import com.jayathu.minstagram.domain.QuizTopic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizBankTest {

    private fun assertWellFormed(q: QuizQuestion) {
        assertEquals("options for: ${q.text}", 3, q.options.size)
        assertEquals("distinct options for: ${q.text}", 3, q.options.distinct().size)
        assertTrue("answerIndex for: ${q.text}", q.answerIndex in 0..2)
        assertTrue("text not blank", q.text.isNotBlank())
    }

    @Test
    fun `generated math questions are well formed`() {
        repeat(1000) { assertWellFormed(QuizBank.next(setOf(QuizTopic.MATH))) }
    }

    @Test
    fun `all topics enabled produces well formed questions`() {
        repeat(2000) { assertWellFormed(QuizBank.next()) }
    }

    @Test
    fun `an empty topic set falls back to all topics`() {
        repeat(500) { assertWellFormed(QuizBank.next(emptySet())) }
    }

    @Test
    fun `every fixed question is well formed`() {
        QuizBank.allFixed.forEach { assertWellFormed(it) }
    }

    @Test
    fun `each single topic yields well formed questions`() {
        QuizTopic.entries.forEach { topic ->
            repeat(200) { assertWellFormed(QuizBank.next(setOf(topic))) }
        }
    }

    @Test
    fun `fixed pool has no duplicate questions`() {
        val texts = QuizBank.allFixed.map { it.text }
        assertEquals(texts.size, texts.distinct().size)
    }

    @Test
    fun `fixed pool is large`() {
        assertTrue("pool should be sizable, was ${QuizBank.allFixed.size}", QuizBank.allFixed.size >= 200)
    }

    @Test
    fun `facts when present are non blank`() {
        QuizBank.allFixed.forEach {
            val fact = it.fact
            if (fact != null) assertTrue("blank fact for: ${it.text}", fact.isNotBlank())
        }
    }

    @Test
    fun `many questions carry a fact`() {
        val withFact = QuizBank.allFixed.count { it.fact != null }
        assertTrue("expected plenty of facts, was $withFact", withFact >= 100)
    }

    @Test
    fun `affirmation is never blank`() {
        repeat(50) { assertTrue(QuizBank.affirmation().isNotBlank()) }
    }

    @Test
    fun `arithmetic questions have the right answer`() {
        repeat(2000) {
            val q = QuizBank.next(setOf(QuizTopic.MATH))
            val answer = q.options[q.answerIndex].toIntOrNull() ?: return@repeat
            val text = q.text.trim().removeSuffix("= ?").removeSuffix("=?").trim()
            val expected = when {
                text.startsWith("Double of ") -> text.removePrefix("Double of ").trim().toInt() * 2
                text.startsWith("Half of ") -> text.removePrefix("Half of ").trim().toInt() / 2
                "+" in text -> text.split("+").let { it[0].trim().toInt() + it[1].trim().toInt() }
                "×" in text -> text.split("×").let { it[0].trim().toInt() * it[1].trim().toInt() }
                text.count { it == '-' } == 1 && !text.startsWith("-") ->
                    text.split("-").let { it[0].trim().toInt() - it[1].trim().toInt() }
                else -> return@repeat // sequences, comparisons, lists: skip arithmetic check
            }
            assertEquals(q.text, expected, answer)
        }
    }
}
