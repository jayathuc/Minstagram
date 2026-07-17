package com.jayathu.minstagram

import com.jayathu.minstagram.domain.QuizBank
import com.jayathu.minstagram.domain.QuizCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizBankTest {

    @Test
    fun `every question is well formed`() {
        repeat(200) {
            val q = QuizBank.next(QuizCategory.MIXED)
            assertEquals(3, q.options.size)
            assertTrue(q.answerIndex in 0..2)
            assertEquals(3, q.options.distinct().size)
            assertTrue(q.text.isNotBlank())
        }
    }

    @Test
    fun `math questions have the right answer`() {
        repeat(200) {
            val q = QuizBank.next(QuizCategory.MATH)
            val answer = q.options[q.answerIndex].toInt()
            val text = q.text.removeSuffix(" = ?")
            val expected = when {
                text.startsWith("Double of ") -> text.removePrefix("Double of ").toInt() * 2
                text.startsWith("Half of ") -> text.removePrefix("Half of ").toInt() / 2
                "+" in text -> text.split("+").let { it[0].trim().toInt() + it[1].trim().toInt() }
                "×" in text -> text.split("×").let { it[0].trim().toInt() * it[1].trim().toInt() }
                "-" in text -> text.split("-").let { it[0].trim().toInt() - it[1].trim().toInt() }
                else -> error("unexpected question: ${q.text}")
            }
            assertEquals(q.text, expected, answer)
        }
    }

    @Test
    fun `general questions come from the fixed pool`() {
        repeat(50) {
            val q = QuizBank.next(QuizCategory.GENERAL)
            assertTrue(q.text.endsWith("?"))
            assertTrue(q.text.none { it == '=' })
        }
    }
}
