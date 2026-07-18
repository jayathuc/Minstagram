package com.jayathu.minstagram

import com.jayathu.minstagram.domain.QuizBank
import com.jayathu.minstagram.domain.QuizCategory
import com.jayathu.minstagram.domain.QuizQuestion
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
    fun `generated questions are well formed`() {
        repeat(1000) { assertWellFormed(QuizBank.next(QuizCategory.MATH)) }
    }

    @Test
    fun `mixed questions are well formed`() {
        repeat(1000) { assertWellFormed(QuizBank.next(QuizCategory.MIXED)) }
    }

    @Test
    fun `every general question is well formed`() {
        QuizBank.general.forEach { assertWellFormed(it) }
    }

    @Test
    fun `general pool has no duplicate questions`() {
        val texts = QuizBank.general.map { it.text }
        assertEquals(texts.size, texts.distinct().size)
    }

    @Test
    fun `general pool is large`() {
        assertTrue("pool should be sizable", QuizBank.general.size >= 100)
    }

    @Test
    fun `arithmetic questions have the right answer`() {
        repeat(2000) {
            val q = QuizBank.next(QuizCategory.MATH)
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
