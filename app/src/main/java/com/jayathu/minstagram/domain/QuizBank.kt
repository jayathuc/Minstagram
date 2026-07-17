package com.jayathu.minstagram.domain

import kotlin.random.Random

data class QuizQuestion(
    val text: String,
    val options: List<String>,
    val answerIndex: Int
)

// Deliberately dry questions. The point is a small mental gear change
// between Reels, not entertainment.
object QuizBank {

    fun next(): QuizQuestion =
        if (Random.nextBoolean()) arithmetic() else general.random()

    private fun arithmetic(): QuizQuestion {
        val a = Random.nextInt(3, 13)
        val b = Random.nextInt(3, 13)
        val (text, answer) = when (Random.nextInt(3)) {
            0 -> "$a + $b = ?" to (a + b)
            1 -> "${a + b} - $b = ?" to a
            else -> "$a × $b = ?" to (a * b)
        }
        val wrong1 = answer + Random.nextInt(1, 4)
        val wrong2 = answer - Random.nextInt(1, 4)
        return shuffled(text, answer.toString(), wrong1.toString(), wrong2.toString())
    }

    private fun shuffled(text: String, correct: String, vararg wrong: String): QuizQuestion {
        val options = (wrong.toList() + correct).shuffled()
        return QuizQuestion(text, options, options.indexOf(correct))
    }

    private val general = listOf(
        shuffledFixed("How many continents are there?", "7", "5", "6"),
        shuffledFixed("How many days are in a leap year?", "366", "365", "364"),
        shuffledFixed("What is the capital of France?", "Paris", "Rome", "Berlin"),
        shuffledFixed("How many minutes are in an hour?", "60", "90", "100"),
        shuffledFixed("Which planet is closest to the sun?", "Mercury", "Venus", "Mars"),
        shuffledFixed("How many sides does a hexagon have?", "6", "5", "8"),
        shuffledFixed("How many legs does a spider have?", "8", "6", "10"),
        shuffledFixed("Which ocean is the largest?", "Pacific", "Atlantic", "Indian"),
        shuffledFixed("How many colors are in a rainbow?", "7", "6", "8"),
        shuffledFixed("What is the largest planet in our solar system?", "Jupiter", "Saturn", "Neptune"),
        shuffledFixed("How many hours are in two days?", "48", "36", "24"),
        shuffledFixed("Which direction does the sun rise from?", "East", "West", "North"),
        shuffledFixed("How many weeks are in a year?", "52", "48", "50"),
        shuffledFixed("What do bees make?", "Honey", "Milk", "Silk"),
        shuffledFixed("How many seconds are in a minute?", "60", "100", "30")
    )

    private fun shuffledFixed(text: String, correct: String, vararg wrong: String): QuizQuestion =
        shuffled(text, correct, *wrong)
}
