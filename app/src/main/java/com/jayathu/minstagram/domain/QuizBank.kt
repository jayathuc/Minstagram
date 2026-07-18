package com.jayathu.minstagram.domain

import kotlin.random.Random

data class QuizQuestion(
    val text: String,
    val options: List<String>,
    val answerIndex: Int
)

enum class QuizCategory { MATH, GENERAL, MIXED }

// Deliberately plain questions. The point is a small mental gear change
// between Reels, not entertainment. Variety matters because a question
// you can answer on autopilot stops interrupting anything, so alongside a
// large fixed pool there are generated questions that never repeat.
object QuizBank {

    fun next(category: QuizCategory = QuizCategory.MIXED): QuizQuestion = when (category) {
        QuizCategory.MATH -> generated()
        QuizCategory.GENERAL -> general.random()
        QuizCategory.MIXED -> if (Random.nextBoolean()) generated() else general.random()
    }

    // --- Generated, effectively endless ---

    private fun generated(): QuizQuestion = when (Random.nextInt(4)) {
        0 -> arithmetic()
        1 -> sequence()
        2 -> largestOrSmallest()
        else -> nextInList()
    }

    private fun arithmetic(): QuizQuestion {
        val a = Random.nextInt(3, 13)
        val b = Random.nextInt(3, 13)
        val c = Random.nextInt(11, 90)
        val d = Random.nextInt(11, 90)
        val even = Random.nextInt(6, 50) * 2
        val (text, answer) = when (Random.nextInt(6)) {
            0 -> "$a + $b = ?" to (a + b)
            1 -> "${a + b} - $b = ?" to a
            2 -> "$a × $b = ?" to (a * b)
            3 -> "$c + $d = ?" to (c + d)
            4 -> "Double of $c = ?" to (c * 2)
            else -> "Half of $even = ?" to (even / 2)
        }
        return numericChoices(text, answer)
    }

    private fun sequence(): QuizQuestion {
        val start = Random.nextInt(1, 12)
        return if (Random.nextBoolean()) {
            val step = Random.nextInt(2, 9)
            val terms = (0..3).map { start + it * step }
            numericChoices(
                "${terms[0]}, ${terms[1]}, ${terms[2]}, ? ",
                terms[0] + 3 * step
            )
        } else {
            val ratio = 2
            val terms = (0..3).map { start * intPow(ratio, it) }
            numericChoices(
                "${terms[0]}, ${terms[1]}, ${terms[2]}, ? ",
                start * intPow(ratio, 3)
            )
        }
    }

    private fun largestOrSmallest(): QuizQuestion {
        val nums = generateSequence { Random.nextInt(10, 99) }.distinct().take(3).toList()
        val largest = Random.nextBoolean()
        val answer = if (largest) nums.max() else nums.min()
        val question = if (largest) "Largest of ${nums.joinToString(", ")}?"
        else "Smallest of ${nums.joinToString(", ")}?"
        val options = nums.shuffled().map { it.toString() }
        return QuizQuestion(question, options, options.indexOf(answer.toString()))
    }

    private fun nextInList(): QuizQuestion {
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val months = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val list = if (Random.nextBoolean()) days else months
        val i = Random.nextInt(list.size)
        val a = list[i]
        val b = list[(i + 1) % list.size]
        val answer = list[(i + 2) % list.size]
        val wrong = list.filter { it != answer && it != a && it != b }.shuffled().take(2)
        return shuffled("What comes next: $a, $b, ?", answer, wrong[0], wrong[1])
    }

    private fun intPow(base: Int, exp: Int): Int {
        var r = 1
        repeat(exp) { r *= base }
        return r
    }

    private fun numericChoices(text: String, answer: Int): QuizQuestion {
        val wrongs = mutableSetOf<Int>()
        while (wrongs.size < 2) {
            val delta = Random.nextInt(1, 6) * if (Random.nextBoolean()) 1 else -1
            val candidate = answer + delta
            if (candidate >= 0 && candidate != answer) wrongs.add(candidate)
        }
        return shuffled(text, answer.toString(), *wrongs.map { it.toString() }.toTypedArray())
    }

    private fun shuffled(text: String, correct: String, vararg wrong: String): QuizQuestion {
        val options = (wrong.toList() + correct).shuffled()
        return QuizQuestion(text, options, options.indexOf(correct))
    }

    private fun q(text: String, correct: String, vararg wrong: String): QuizQuestion =
        shuffled(text, correct, *wrong)

    // --- Fixed general-knowledge pool ---

    internal val general: List<QuizQuestion> = listOf(
        // Geography
        q("How many continents are there?", "7", "5", "6"),
        q("Which ocean is the largest?", "Pacific", "Atlantic", "Indian"),
        q("Which is the smallest ocean?", "Arctic", "Indian", "Atlantic"),
        q("What is the capital of France?", "Paris", "Rome", "Berlin"),
        q("What is the capital of Japan?", "Tokyo", "Seoul", "Beijing"),
        q("What is the capital of Italy?", "Rome", "Milan", "Venice"),
        q("What is the capital of England?", "London", "Dublin", "Paris"),
        q("What is the capital of Spain?", "Madrid", "Barcelona", "Lisbon"),
        q("What is the capital of Germany?", "Berlin", "Munich", "Hamburg"),
        q("What is the capital of Russia?", "Moscow", "Kyiv", "Warsaw"),
        q("What is the capital of Egypt?", "Cairo", "Giza", "Luxor"),
        q("What is the capital of Canada?", "Ottawa", "Toronto", "Vancouver"),
        q("What is the capital of Australia?", "Canberra", "Sydney", "Melbourne"),
        q("What is the capital of China?", "Beijing", "Shanghai", "Hong Kong"),
        q("What is the capital of India?", "New Delhi", "Mumbai", "Kolkata"),
        q("What is the capital of Sri Lanka?", "Colombo", "Kandy", "Galle"),
        q("Which river runs through Egypt?", "Nile", "Amazon", "Danube"),
        q("Which is the longest river in the world?", "Nile", "Amazon", "Yangtze"),
        q("What is the tallest mountain on Earth?", "Everest", "K2", "Kilimanjaro"),
        q("Which desert is the largest hot desert?", "Sahara", "Gobi", "Kalahari"),
        q("On which continent is Egypt?", "Africa", "Asia", "Europe"),
        q("On which continent is Brazil?", "South America", "Africa", "Europe"),
        q("Which country has the most people?", "India", "China", "USA"),
        q("Which sea lies between Europe and Africa?", "Mediterranean", "Caribbean", "Baltic"),
        q("How many oceans are there?", "5", "4", "7"),

        // Space and Earth
        q("Which planet is closest to the sun?", "Mercury", "Venus", "Mars"),
        q("Which is the largest planet?", "Jupiter", "Saturn", "Neptune"),
        q("Which planet is known as the red planet?", "Mars", "Venus", "Jupiter"),
        q("How many planets are in our solar system?", "8", "9", "7"),
        q("What is at the center of our solar system?", "The sun", "The Earth", "The moon"),
        q("What orbits the Earth?", "The moon", "The sun", "Mars"),
        q("Which planet do we live on?", "Earth", "Mars", "Venus"),
        q("What is the closest star to Earth?", "The sun", "Sirius", "Polaris"),
        q("Which planet has famous rings?", "Saturn", "Mars", "Mercury"),
        q("How long does Earth take to orbit the sun?", "1 year", "1 month", "1 week"),

        // Science and the body
        q("At what temperature does water freeze?", "0°C", "10°C", "-10°C"),
        q("At what temperature does water boil?", "100°C", "90°C", "120°C"),
        q("What gas do humans breathe in to live?", "Oxygen", "Carbon dioxide", "Nitrogen"),
        q("What gas do plants take in from the air?", "Carbon dioxide", "Oxygen", "Helium"),
        q("How many chambers does the human heart have?", "4", "2", "3"),
        q("How many teeth does an adult human have?", "32", "28", "36"),
        q("How many bones are in the adult human body?", "206", "150", "300"),
        q("How many senses do humans commonly have?", "5", "4", "6"),
        q("Which organ pumps blood around the body?", "Heart", "Lungs", "Liver"),
        q("Which organ do you breathe with?", "Lungs", "Kidneys", "Stomach"),
        q("What is H2O commonly known as?", "Water", "Salt", "Air"),
        q("What is the largest organ of the human body?", "Skin", "Liver", "Heart"),
        q("What do plants need to make food from sunlight?", "Water", "Milk", "Oil"),
        q("What force pulls objects toward the Earth?", "Gravity", "Friction", "Magnetism"),
        q("What is frozen water called?", "Ice", "Steam", "Fog"),
        q("What is water vapor called when it rises?", "Steam", "Ice", "Rain"),

        // Animals
        q("What is the fastest land animal?", "Cheetah", "Lion", "Horse"),
        q("What is the largest animal on Earth?", "Blue whale", "Elephant", "Giraffe"),
        q("What is the tallest animal?", "Giraffe", "Elephant", "Horse"),
        q("How many legs does a spider have?", "8", "6", "10"),
        q("How many legs does an insect have?", "6", "8", "4"),
        q("How many arms does an octopus have?", "8", "6", "10"),
        q("What do bees make?", "Honey", "Milk", "Silk"),
        q("Which bird cannot fly?", "Penguin", "Eagle", "Sparrow"),
        q("What is a baby dog called?", "Puppy", "Kitten", "Cub"),
        q("What is a baby cat called?", "Kitten", "Puppy", "Foal"),
        q("Which animal is known as the king of the jungle?", "Lion", "Tiger", "Bear"),
        q("What do caterpillars turn into?", "Butterflies", "Bees", "Spiders"),
        q("Which sea creature has eight arms?", "Octopus", "Shark", "Dolphin"),
        q("What is the largest land animal?", "Elephant", "Rhino", "Hippo"),

        // Shapes and math facts
        q("How many sides does a triangle have?", "3", "4", "5"),
        q("How many sides does a square have?", "4", "3", "5"),
        q("How many sides does a hexagon have?", "6", "5", "8"),
        q("How many sides does a pentagon have?", "5", "6", "4"),
        q("How many degrees are in a right angle?", "90", "45", "180"),
        q("How many degrees are in a circle?", "360", "180", "270"),
        q("How many degrees are in a straight line?", "180", "90", "360"),
        q("Which of these is a prime number?", "7", "9", "15"),
        q("Which of these is an even number?", "8", "7", "5"),
        q("What is a quarter as a fraction?", "1/4", "1/2", "1/3"),
        q("How many zeros are in one thousand?", "3", "2", "4"),
        q("How many zeros are in one million?", "6", "5", "7"),

        // Time and calendar
        q("How many minutes are in an hour?", "60", "90", "100"),
        q("How many hours are in a day?", "24", "12", "48"),
        q("How many seconds are in a minute?", "60", "100", "30"),
        q("How many weeks are in a year?", "52", "48", "50"),
        q("How many months are in a year?", "12", "10", "11"),
        q("How many days does a week have?", "7", "5", "6"),
        q("How many days does September have?", "30", "31", "28"),
        q("How many days does a leap year have?", "366", "365", "364"),
        q("How many days does a normal year have?", "365", "366", "360"),
        q("How many years are in a century?", "100", "10", "1000"),
        q("How many years are in a decade?", "10", "100", "12"),
        q("How many items are in a dozen?", "12", "10", "6"),
        q("How many days are in a fortnight?", "14", "7", "10"),
        q("Which month has 28 or 29 days?", "February", "April", "June"),
        q("What day comes after Sunday?", "Monday", "Saturday", "Friday"),

        // Language and everyday
        q("How many letters are in the English alphabet?", "26", "24", "28"),
        q("How many colors are in a rainbow?", "7", "6", "8"),
        q("How many primary colors are there?", "3", "4", "5"),
        q("What color do you get mixing blue and yellow?", "Green", "Purple", "Orange"),
        q("What color do you get mixing red and blue?", "Purple", "Green", "Orange"),
        q("What color do you get mixing red and yellow?", "Orange", "Green", "Purple"),
        q("Which direction does the sun rise from?", "East", "West", "North"),
        q("Which direction does the sun set in?", "West", "East", "South"),
        q("How many sides does a standard dice have?", "6", "4", "8"),
        q("How many players are on a soccer team on the field?", "11", "9", "7"),
        q("How many strings does a standard guitar have?", "6", "4", "8"),
        q("How many keys of each color repeat on a piano octave?", "7 white", "5 white", "8 white"),
        q("What is the first month of the year?", "January", "December", "March"),
        q("What is the last month of the year?", "December", "November", "January"),
        q("What shape is a stop sign?", "Octagon", "Square", "Circle"),
        q("How many wheels does a standard car have?", "4", "3", "6"),
        q("How many cents are in a dollar?", "100", "50", "10"),
        q("How many sides does a cube have?", "6", "4", "8"),
        q("What do you call frozen rain?", "Hail", "Fog", "Dew"),
        q("What instrument has 88 keys?", "Piano", "Guitar", "Violin")
    )
}
