package com.jayathu.minstagram.domain

import kotlin.random.Random

data class QuizQuestion(
    val text: String,
    val options: List<String>,
    val answerIndex: Int
)

enum class QuizCategory { MATH, GENERAL, MIXED }

// Deliberately dry questions. The point is a small mental gear change
// between Reels, not entertainment.
object QuizBank {

    fun next(category: QuizCategory = QuizCategory.MIXED): QuizQuestion = when (category) {
        QuizCategory.MATH -> arithmetic()
        QuizCategory.GENERAL -> general.random()
        QuizCategory.MIXED -> if (Random.nextBoolean()) arithmetic() else general.random()
    }

    private fun arithmetic(): QuizQuestion {
        val a = Random.nextInt(3, 13)
        val b = Random.nextInt(3, 13)
        val c = Random.nextInt(11, 90)
        val even = Random.nextInt(6, 50) * 2
        val d = Random.nextInt(11, 90)
        val (text, answer) = when (Random.nextInt(6)) {
            0 -> "$a + $b = ?" to (a + b)
            1 -> "${a + b} - $b = ?" to a
            2 -> "$a × $b = ?" to (a * b)
            3 -> "$c + $d = ?" to (c + d)
            4 -> "Double of $c = ?" to (c * 2)
            else -> "Half of $even = ?" to (even / 2)
        }
        val wrong1 = answer + Random.nextInt(1, 4)
        val wrong2 = (answer - Random.nextInt(1, 4)).coerceAtLeast(0)
            .let { if (it == wrong1 || it == answer) answer + Random.nextInt(4, 7) else it }
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
        shuffledFixed("What is the capital of Japan?", "Tokyo", "Seoul", "Beijing"),
        shuffledFixed("What is the capital of Italy?", "Rome", "Milan", "Madrid"),
        shuffledFixed("What is the capital of England?", "London", "Dublin", "Paris"),
        shuffledFixed("How many minutes are in an hour?", "60", "90", "100"),
        shuffledFixed("How many hours are in a day?", "24", "12", "48"),
        shuffledFixed("How many seconds are in a minute?", "60", "100", "30"),
        shuffledFixed("How many weeks are in a year?", "52", "48", "50"),
        shuffledFixed("How many days does September have?", "30", "31", "28"),
        shuffledFixed("How many years are in a century?", "100", "10", "1000"),
        shuffledFixed("How many years are in a decade?", "10", "100", "12"),
        shuffledFixed("How many items are in a dozen?", "12", "10", "6"),
        shuffledFixed("Which planet is closest to the sun?", "Mercury", "Venus", "Mars"),
        shuffledFixed("What is the largest planet in our solar system?", "Jupiter", "Saturn", "Neptune"),
        shuffledFixed("How many planets are in our solar system?", "8", "9", "7"),
        shuffledFixed("Which direction does the sun rise from?", "East", "West", "North"),
        shuffledFixed("At what temperature does water freeze?", "0°C", "10°C", "-10°C"),
        shuffledFixed("At what temperature does water boil?", "100°C", "90°C", "120°C"),
        shuffledFixed("How many degrees are in a right angle?", "90", "45", "180"),
        shuffledFixed("How many sides does a hexagon have?", "6", "5", "8"),
        shuffledFixed("How many sides does a triangle have?", "3", "4", "5"),
        shuffledFixed("How many letters are in the English alphabet?", "26", "24", "28"),
        shuffledFixed("How many colors are in a rainbow?", "7", "6", "8"),
        shuffledFixed("How many legs does a spider have?", "8", "6", "10"),
        shuffledFixed("How many legs does an insect have?", "6", "8", "4"),
        shuffledFixed("How many arms does an octopus have?", "8", "6", "10"),
        shuffledFixed("What is the fastest land animal?", "Cheetah", "Lion", "Horse"),
        shuffledFixed("What is the largest animal on Earth?", "Blue whale", "Elephant", "Giraffe"),
        shuffledFixed("What do bees make?", "Honey", "Milk", "Silk"),
        shuffledFixed("Which ocean is the largest?", "Pacific", "Atlantic", "Indian"),
        shuffledFixed("Which river runs through Egypt?", "Nile", "Amazon", "Danube"),
        shuffledFixed("What is the tallest mountain on Earth?", "Everest", "K2", "Kilimanjaro"),
        shuffledFixed("How many chambers does the human heart have?", "4", "2", "3"),
        shuffledFixed("How many teeth does an adult human have?", "32", "28", "36"),
        shuffledFixed("How many bones are in the adult human body?", "206", "150", "300"),
        shuffledFixed("How many senses do humans commonly have?", "5", "4", "6"),
        shuffledFixed("How many primary colors are there?", "3", "4", "5"),
        shuffledFixed("How many days are in a fortnight?", "14", "7", "10")
    )

    private fun shuffledFixed(text: String, correct: String, vararg wrong: String): QuizQuestion =
        shuffled(text, correct, *wrong)
}
