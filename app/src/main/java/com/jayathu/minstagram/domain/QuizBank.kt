package com.jayathu.minstagram.domain

import kotlin.random.Random

data class QuizQuestion(
    val text: String,
    val options: List<String>,
    val answerIndex: Int,
    // a small, calm thing to read after getting it right; null falls back to
    // a short well-done, which is all the generated math questions get
    val fact: String? = null
)

// The kinds of question a user can turn on or off in settings. MATH is the
// generated, effectively endless set; the rest are fixed pools by subject.
enum class QuizTopic(val label: String) {
    MATH("Math"),
    GEOGRAPHY("Geography"),
    SCIENCE("Science"),
    SPACE("Space"),
    ANIMALS("Animals"),
    HUMAN_BODY("Human body"),
    NUMBERS("Numbers & shapes"),
    TIME("Time & calendar"),
    EVERYDAY("Everyday"),
    HISTORY("History"),
    WORDS("Words"),
    SPORTS("Sports");

    companion object {
        val DEFAULT_ENABLED: Set<String> = entries.map { it.name }.toSet()
    }
}

// Deliberately plain questions. The point is a small mental gear change
// between Reels, not entertainment. Variety matters because a question you
// can answer on autopilot stops interrupting anything, so there is a large
// fixed pool across many subjects plus generated math that never repeats.
object QuizBank {

    // Pick a random enabled topic, then a question from it. An empty set is
    // treated as everything on, so the user can never end up with no questions.
    fun next(topics: Set<QuizTopic> = QuizTopic.entries.toSet()): QuizQuestion {
        val enabled = topics.ifEmpty { QuizTopic.entries.toSet() }
        return when (val topic = enabled.random()) {
            QuizTopic.MATH -> generated()
            else -> pool(topic).random()
        }
    }

    // Short, warm confirmations for math and any question with no fact.
    private val affirmations = listOf(
        "Good job.", "Nice one.", "Well done.", "Correct.",
        "Nicely done.", "That's right.", "Spot on.", "Exactly."
    )

    fun affirmation(): String = affirmations.random()

    private fun pool(topic: QuizTopic): List<QuizQuestion> = when (topic) {
        QuizTopic.MATH -> emptyList() // handled by generated()
        QuizTopic.GEOGRAPHY -> geography
        QuizTopic.SCIENCE -> science
        QuizTopic.SPACE -> space
        QuizTopic.ANIMALS -> animals
        QuizTopic.HUMAN_BODY -> humanBody
        QuizTopic.NUMBERS -> numbers
        QuizTopic.TIME -> time
        QuizTopic.EVERYDAY -> everyday
        QuizTopic.HISTORY -> history
        QuizTopic.WORDS -> words
        QuizTopic.SPORTS -> sports
    }

    // all fixed questions, for tests and sizing
    internal val allFixed: List<QuizQuestion> by lazy {
        geography + science + space + animals + humanBody +
            numbers + time + everyday + history + words + sports
    }

    // --- Generated, effectively endless ---

    internal fun generated(): QuizQuestion = when (Random.nextInt(4)) {
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

    // correct answer first, then the wrong options, then an optional fact
    private fun q(text: String, correct: String, vararg wrong: String, fact: String? = null): QuizQuestion {
        val options = (wrong.toList() + correct).shuffled()
        return QuizQuestion(text, options, options.indexOf(correct), fact)
    }

    // --- Fixed pools by subject ---

    internal val geography: List<QuizQuestion> = listOf(
        q("How many continents are there?", "7", "5", "6", fact = "Some countries teach 5 or 6, grouping the Americas or Eurasia together."),
        q("Which ocean is the largest?", "Pacific", "Atlantic", "Indian", fact = "The Pacific is larger than all the land on Earth put together."),
        q("Which is the smallest ocean?", "Arctic", "Indian", "Atlantic", fact = "The Arctic Ocean is mostly covered by drifting sea ice."),
        q("How many oceans are there?", "5", "4", "7", fact = "The Southern Ocean around Antarctica was officially added in 2000."),
        q("What is the capital of France?", "Paris", "Rome", "Berlin", fact = "Paris grew out of a small island in the middle of the Seine river."),
        q("What is the capital of Japan?", "Tokyo", "Seoul", "Beijing", fact = "Tokyo was once a quiet fishing village called Edo."),
        q("What is the capital of Italy?", "Rome", "Milan", "Venice", fact = "Legend says Rome was founded in 753 BC by Romulus."),
        q("What is the capital of England?", "London", "Dublin", "Paris", fact = "London has been a major city since Roman times, nearly 2,000 years."),
        q("What is the capital of Spain?", "Madrid", "Barcelona", "Lisbon", fact = "Madrid sits almost exactly in the geographic center of Spain."),
        q("What is the capital of Germany?", "Berlin", "Munich", "Hamburg", fact = "Berlin is about nine times the size of Paris by area."),
        q("What is the capital of Russia?", "Moscow", "Kyiv", "Warsaw", fact = "Some Moscow metro stations were built to look like underground palaces."),
        q("What is the capital of Egypt?", "Cairo", "Giza", "Luxor", fact = "Cairo is the largest city in the Arab world."),
        q("What is the capital of Canada?", "Ottawa", "Toronto", "Vancouver", fact = "Ottawa was chosen partly because it sat between English and French regions."),
        q("What is the capital of Australia?", "Canberra", "Sydney", "Melbourne", fact = "Canberra was built as a compromise between rivals Sydney and Melbourne."),
        q("What is the capital of China?", "Beijing", "Shanghai", "Hong Kong", fact = "Beijing has been China's capital, on and off, for over 800 years."),
        q("What is the capital of India?", "New Delhi", "Mumbai", "Kolkata", fact = "New Delhi was designed and built by the British in the early 1900s."),
        q("What is the capital of the USA?", "Washington DC", "New York", "Los Angeles", fact = "It was built on land given by Maryland and Virginia just to be the capital."),
        q("What is the capital of Brazil?", "Brasília", "Rio de Janeiro", "São Paulo", fact = "Brasília was built from nothing in about four years and opened in 1960."),
        q("What is the capital of Greece?", "Athens", "Rome", "Cairo", fact = "Athens has been lived in for over 3,000 years."),
        q("What is the capital of Portugal?", "Lisbon", "Madrid", "Porto", fact = "Lisbon is one of the oldest cities in western Europe."),
        q("What is the capital of Thailand?", "Bangkok", "Hanoi", "Manila", fact = "Bangkok's full ceremonial name is the longest place name in the world."),
        q("What is the capital of South Korea?", "Seoul", "Tokyo", "Osaka", fact = "Roughly half of all South Koreans live in the greater Seoul area."),
        q("What is the capital of Sri Lanka?", "Colombo", "Kandy", "Galle", fact = "The official capital is nearby Sri Jayawardenepura Kotte; Colombo is the largest city."),
        q("Which river runs through Egypt?", "Nile", "Amazon", "Danube", fact = "Ancient Egypt thrived because the Nile flooded and left rich soil each year."),
        q("Which is the longest river in the world?", "Nile", "Amazon", "Yangtze", fact = "The Nile flows north, which surprises people who expect rivers to flow south."),
        q("What is the tallest mountain on Earth?", "Everest", "K2", "Kilimanjaro", fact = "Everest grows a few millimeters taller each year as plates push together."),
        q("Which desert is the largest hot desert?", "Sahara", "Gobi", "Kalahari", fact = "The Sahara is roughly the size of the whole United States."),
        q("On which continent is Egypt?", "Africa", "Asia", "Europe", fact = "A small part of Egypt, the Sinai, actually sits in Asia."),
        q("On which continent is Brazil?", "South America", "Africa", "Europe", fact = "Brazil covers nearly half of the whole continent."),
        q("Which country has the most people?", "India", "China", "USA", fact = "India passed China as the most populous country in 2023."),
        q("Which is the largest country by area?", "Russia", "Canada", "China", fact = "Russia is so wide it spans eleven time zones."),
        q("Which is the smallest continent?", "Australia", "Europe", "Antarctica", fact = "Australia is the only continent that is also a single country."),
        q("Which continent is the coldest?", "Antarctica", "Asia", "Europe", fact = "Antarctica is technically the largest desert, since almost no rain falls."),
        q("Which continent has the most countries?", "Africa", "Asia", "Europe", fact = "Africa has 54 countries, more than any other continent."),
        q("Which sea lies between Europe and Africa?", "Mediterranean", "Caribbean", "Baltic", fact = "'Mediterranean' means 'middle of the land'."),
        q("Which country is shaped like a boot?", "Italy", "Spain", "Greece", fact = "The 'boot' even looks like it's kicking the island of Sicily."),
        q("The Eiffel Tower is in which city?", "Paris", "London", "Rome", fact = "It was meant to be temporary and was almost torn down."),
        q("The Great Wall is in which country?", "China", "Japan", "India", fact = "It's not one wall but many, built over centuries."),
        q("The pyramids of Giza are in which country?", "Egypt", "Mexico", "Iraq", fact = "The Great Pyramid was the tallest human-made thing for nearly 4,000 years."),
        q("Mount Fuji is in which country?", "Japan", "China", "Nepal", fact = "Fuji is an active volcano, though it hasn't erupted since 1707."),
        q("The Amazon rainforest is mostly in which country?", "Brazil", "Peru", "Mexico", fact = "The Amazon holds more than half of the world's rainforest."),
        q("Which US state is the largest by area?", "Alaska", "Texas", "California", fact = "Alaska is more than twice the size of Texas.")
    )

    internal val science: List<QuizQuestion> = listOf(
        q("At what temperature does water freeze?", "0°C", "10°C", "-10°C", fact = "Oddly, hot water can sometimes freeze faster than cold."),
        q("At what temperature does water boil?", "100°C", "90°C", "120°C", fact = "Water boils at a lower temperature high up a mountain."),
        q("What gas do humans breathe in to live?", "Oxygen", "Carbon dioxide", "Nitrogen", fact = "A lot of your oxygen comes from ocean plankton, not trees."),
        q("What gas do plants take in from the air?", "Carbon dioxide", "Oxygen", "Helium", fact = "Plants pull carbon out of the air and build their bodies from it."),
        q("What gas do plants release?", "Oxygen", "Carbon dioxide", "Nitrogen", fact = "One large tree can supply a day's oxygen for several people."),
        q("What is H2O commonly known as?", "Water", "Salt", "Air", fact = "About 60% of your body is water."),
        q("What do plants need to make food from sunlight?", "Water", "Milk", "Oil", fact = "They also need carbon dioxide from the air."),
        q("What is the process plants use to make food?", "Photosynthesis", "Digestion", "Respiration", fact = "It's powered directly by sunlight."),
        q("What force pulls objects toward the Earth?", "Gravity", "Friction", "Magnetism", fact = "The same gravity keeps the moon circling Earth."),
        q("What force slows a sliding object?", "Friction", "Gravity", "Magnetism", fact = "Without friction you couldn't walk, or even hold anything."),
        q("What is frozen water called?", "Ice", "Steam", "Fog", fact = "Ice floats because it's lighter than liquid water, which is unusual."),
        q("What is water vapor called when it rises?", "Steam", "Ice", "Rain", fact = "Steam itself is invisible; the cloud you see is tiny droplets."),
        q("What do we call water falling from clouds?", "Rain", "Wind", "Fog", fact = "Raindrops are round, not tear-shaped."),
        q("What do you call frozen rain?", "Hail", "Fog", "Dew", fact = "Some hailstones have grown as big as grapefruits."),
        q("What state of matter is steam?", "Gas", "Liquid", "Solid", fact = "Water is one of the few things we meet as solid, liquid and gas."),
        q("What state of matter is ice?", "Solid", "Liquid", "Gas"),
        q("What tool measures temperature?", "Thermometer", "Barometer", "Ruler"),
        q("What metal is a magnet attracted to?", "Iron", "Wood", "Plastic", fact = "Earth itself acts like a giant magnet, which is why compasses work."),
        q("What is the center of an atom called?", "Nucleus", "Electron", "Orbit", fact = "Atoms are mostly empty space around a tiny nucleus."),
        q("Which of these is a renewable energy source?", "Solar", "Coal", "Oil", fact = "Enough sunlight hits Earth in an hour to power the world for a year."),
        q("What do you call animals that eat only plants?", "Herbivores", "Carnivores", "Omnivores", fact = "The largest animals on Earth, like elephants, are herbivores."),
        q("What do you call animals that eat only meat?", "Carnivores", "Herbivores", "Omnivores", fact = "A cat is a true carnivore and can't stay healthy without meat."),
        q("Sound travels through the air as what?", "Waves", "Light", "Heat", fact = "Sound can't travel through empty space, so space is silent."),
        q("What makes plants green?", "Chlorophyll", "Water", "Sunlight", fact = "Chlorophyll soaks up red and blue light and reflects green.")
    )

    internal val space: List<QuizQuestion> = listOf(
        q("Which planet is closest to the sun?", "Mercury", "Venus", "Mars", fact = "A year on Mercury lasts just 88 Earth days."),
        q("Which is the largest planet?", "Jupiter", "Saturn", "Neptune", fact = "All the other planets could fit inside Jupiter."),
        q("Which planet is known as the red planet?", "Mars", "Venus", "Jupiter", fact = "Mars looks red because its soil is full of rust."),
        q("Which planet is farthest from the sun?", "Neptune", "Uranus", "Saturn", fact = "Neptune takes 165 Earth years to orbit the sun once."),
        q("How many planets are in our solar system?", "8", "9", "7", fact = "Pluto was reclassified as a dwarf planet in 2006."),
        q("What is at the center of our solar system?", "The sun", "The Earth", "The moon", fact = "The sun holds 99.8% of all the mass in the solar system."),
        q("What orbits the Earth?", "The moon", "The sun", "Mars", fact = "The moon is slowly drifting away, a few centimeters each year."),
        q("Which planet do we live on?", "Earth", "Mars", "Venus", fact = "Earth is the only planet not named after a god."),
        q("What is the closest star to Earth?", "The sun", "Sirius", "Polaris", fact = "Its light takes about eight minutes to reach us."),
        q("Which planet has famous rings?", "Saturn", "Mars", "Mercury", fact = "Saturn's rings are made mostly of ice and rock."),
        q("How long does Earth take to orbit the sun?", "1 year", "1 month", "1 week", fact = "Earth moves around the sun at about 30 kilometers every second."),
        q("What galaxy do we live in?", "Milky Way", "Andromeda", "Whirlpool", fact = "Our galaxy has well over 100 billion stars."),
        q("The sun is mostly made of which gas?", "Hydrogen", "Oxygen", "Carbon", fact = "The sun fuses 600 million tons of hydrogen every second."),
        q("What causes day and night?", "Earth spinning", "The moon", "Clouds", fact = "Earth spins at about 1,600 km/h at the equator."),
        q("What do we call a group of stars forming a pattern?", "Constellation", "Galaxy", "Comet", fact = "Different cultures saw completely different pictures in the same stars."),
        q("A space rock that lands on Earth is called a?", "Meteorite", "Comet", "Star", fact = "Most meteorites are older than any rock on Earth."),
        q("Roughly how long does the moon take to orbit Earth?", "About a month", "About a week", "About a year", fact = "The word 'month' comes from 'moon'."),
        q("What is the moon called when it is fully lit?", "Full moon", "New moon", "Half moon", fact = "A full moon is about 14 times brighter than a half moon.")
    )

    internal val animals: List<QuizQuestion> = listOf(
        q("What is the fastest land animal?", "Cheetah", "Lion", "Horse", fact = "A cheetah can hit 100 km/h in about three seconds."),
        q("What is the largest animal on Earth?", "Blue whale", "Elephant", "Giraffe", fact = "A blue whale's heart is about the size of a small car."),
        q("What is the tallest animal?", "Giraffe", "Elephant", "Horse", fact = "A giraffe's neck has the same number of bones as yours, just longer."),
        q("What is the largest land animal?", "Elephant", "Rhino", "Hippo", fact = "Elephants are the only animals that can't jump."),
        q("Which is the largest big cat?", "Tiger", "Lion", "Leopard", fact = "No two tigers share the same stripe pattern."),
        q("How many legs does a spider have?", "8", "6", "10", fact = "Spiders aren't insects; insects have six legs."),
        q("How many legs does an insect have?", "6", "8", "4", fact = "Insects make up more than half of all known living things."),
        q("How many arms does an octopus have?", "8", "6", "10", fact = "An octopus has three hearts and blue blood."),
        q("What do bees make?", "Honey", "Milk", "Silk", fact = "Bees visit millions of flowers to fill a single jar of honey."),
        q("What do bees live in?", "Hive", "Nest", "Den"),
        q("Which bird cannot fly?", "Penguin", "Eagle", "Sparrow", fact = "Penguins 'fly' underwater instead, using their wings as flippers."),
        q("Which bird is known for copying speech?", "Parrot", "Owl", "Eagle", fact = "Some parrots can learn hundreds of words."),
        q("What is a baby dog called?", "Puppy", "Kitten", "Cub"),
        q("What is a baby cat called?", "Kitten", "Puppy", "Foal"),
        q("What is a baby cow called?", "Calf", "Foal", "Kid"),
        q("What is a baby horse called?", "Foal", "Calf", "Cub"),
        q("What is a baby sheep called?", "Lamb", "Kid", "Calf"),
        q("What is a young frog called?", "Tadpole", "Chick", "Cub", fact = "Tadpoles slowly lose their tails as they turn into frogs."),
        q("Which animal is known as the king of the jungle?", "Lion", "Tiger", "Bear", fact = "Lions actually live on grasslands, not jungles."),
        q("What do caterpillars turn into?", "Butterflies", "Bees", "Spiders", fact = "Inside the cocoon, a caterpillar almost completely dissolves first."),
        q("Which sea creature has eight arms?", "Octopus", "Shark", "Dolphin"),
        q("Which of these sea animals is a mammal?", "Dolphin", "Shark", "Tuna", fact = "Dolphins must come up for air; they can't breathe underwater."),
        q("What is a group of lions called?", "Pride", "Pack", "Flock"),
        q("What is a group of wolves called?", "Pack", "Herd", "School"),
        q("How many humps does a dromedary camel have?", "1", "2", "3", fact = "The hump stores fat, not water."),
        q("What do we call an animal active at night?", "Nocturnal", "Diurnal", "Extinct", fact = "Many nocturnal animals have huge eyes to gather what little light there is."),
        q("Which animal can regrow its tail?", "Lizard", "Dog", "Frog", fact = "A lizard can drop its tail to escape and grow a new one."),
        q("Which is the fastest bird in a dive?", "Peregrine falcon", "Ostrich", "Eagle", fact = "A diving peregrine is the fastest animal alive, over 300 km/h.")
    )

    internal val humanBody: List<QuizQuestion> = listOf(
        q("How many chambers does the human heart have?", "4", "2", "3", fact = "Your heart beats about 100,000 times a day."),
        q("How many teeth does an adult human have?", "32", "28", "36", fact = "That count includes four wisdom teeth many people have removed."),
        q("How many bones are in the adult human body?", "206", "150", "300", fact = "Babies are born with around 300 bones that fuse as they grow."),
        q("How many senses do humans commonly have?", "5", "4", "6", fact = "Some scientists count more, like balance and temperature."),
        q("How many lungs do humans have?", "2", "1", "3", fact = "Your right lung is a bit bigger to make room for the heart."),
        q("Which organ pumps blood around the body?", "Heart", "Lungs", "Liver", fact = "Your blood travels roughly 19,000 km through your body each day."),
        q("Which organ do you breathe with?", "Lungs", "Kidneys", "Stomach", fact = "You breathe around 20,000 times a day without thinking."),
        q("Which organ helps you think?", "Brain", "Stomach", "Liver", fact = "Your brain uses about a fifth of all the energy your body burns."),
        q("Which organs filter the blood to make urine?", "Kidneys", "Lungs", "Heart", fact = "Your kidneys filter all your blood about 30 times a day."),
        q("What is the largest organ of the human body?", "Skin", "Liver", "Heart", fact = "Your skin completely renews itself about once a month."),
        q("Which body part do you hear with?", "Ears", "Nose", "Eyes", fact = "Your ears also keep you balanced."),
        q("Which body part do you smell with?", "Nose", "Ears", "Tongue", fact = "Smell is closely tied to memory and emotion."),
        q("What protects the brain?", "Skull", "Ribs", "Spine", fact = "The adult skull is made of 22 fused bones."),
        q("What protects the heart and lungs?", "Ribs", "Skull", "Hips", fact = "You have 12 pairs of ribs."),
        q("What is the hardest substance in the body?", "Tooth enamel", "Bone", "Skin", fact = "Enamel is very hard, but unlike bone it can't heal itself."),
        q("How many fingers are on one hand?", "5", "4", "6"),
        q("What do we call the full frame of bones in the body?", "Skeleton", "Muscle", "Joint", fact = "Gram for gram, bone is stronger than steel."),
        q("What carries oxygen around in your blood?", "Red blood cells", "White blood cells", "Plasma", fact = "You make millions of new red blood cells every second.")
    )

    internal val numbers: List<QuizQuestion> = listOf(
        q("How many sides does a triangle have?", "3", "4", "5"),
        q("How many sides does a square have?", "4", "3", "5"),
        q("How many sides does a rectangle have?", "4", "3", "5"),
        q("How many sides does a pentagon have?", "5", "6", "4"),
        q("How many sides does a hexagon have?", "6", "5", "8", fact = "Bees build their honeycomb out of hexagons."),
        q("How many sides does an octagon have?", "8", "6", "7", fact = "'Octo' means eight, like an octopus's arms."),
        q("How many faces does a cube have?", "6", "4", "8"),
        q("How many degrees are in a right angle?", "90", "45", "180"),
        q("How many degrees are in a straight line?", "180", "90", "360"),
        q("How many degrees are in a circle?", "360", "180", "270", fact = "The 360 may come from ancient astronomers counting days in a year."),
        q("An angle less than 90 degrees is called?", "Acute", "Obtuse", "Right", fact = "'Acute' also means sharp, like the angle."),
        q("An angle more than 90 degrees is called?", "Obtuse", "Acute", "Straight"),
        q("Which of these is a prime number?", "7", "9", "15", fact = "A prime number divides evenly only by 1 and itself."),
        q("Which of these is an even number?", "8", "7", "5"),
        q("Is 9 an odd or even number?", "Odd", "Even", "Neither"),
        q("What is a quarter as a fraction?", "1/4", "1/2", "1/3"),
        q("Which is bigger?", "1/2", "1/4", "1/8"),
        q("How many zeros are in one thousand?", "3", "2", "4"),
        q("How many zeros are in one million?", "6", "5", "7", fact = "A billion has nine zeros."),
        q("What is half of 100?", "50", "25", "40"),
        q("What is the Roman numeral for 5?", "V", "X", "I", fact = "The Romans had no symbol at all for zero."),
        q("What is the Roman numeral for 10?", "X", "V", "L"),
        q("What is pi rounded to two decimals?", "3.14", "3.41", "2.14", fact = "Pi's digits go on forever without ever repeating."),
        q("How many corners does a triangle have?", "3", "4", "2")
    )

    internal val time: List<QuizQuestion> = listOf(
        q("How many minutes are in an hour?", "60", "90", "100", fact = "We count time in 60s thanks to the ancient Babylonians."),
        q("How many hours are in a day?", "24", "12", "48"),
        q("How many seconds are in a minute?", "60", "100", "30"),
        q("How many minutes are in half an hour?", "30", "15", "45"),
        q("How many hours are in half a day?", "12", "6", "24"),
        q("How many weeks are in a year?", "52", "48", "50"),
        q("How many months are in a year?", "12", "10", "11"),
        q("How many days does a week have?", "7", "5", "6"),
        q("How many months have 31 days?", "7", "5", "6"),
        q("How many days does September have?", "30", "31", "28"),
        q("How many days does April have?", "30", "31", "28"),
        q("How many days does December have?", "31", "30", "28"),
        q("How many days does a leap year have?", "366", "365", "364", fact = "The extra day keeps our calendar in step with Earth's orbit."),
        q("How many days does a normal year have?", "365", "366", "360"),
        q("How many years are in a century?", "100", "10", "1000"),
        q("How many years are in a decade?", "10", "100", "12"),
        q("How many items are in a dozen?", "12", "10", "6"),
        q("How many days are in a fortnight?", "14", "7", "10", fact = "'Fortnight' is short for 'fourteen nights'."),
        q("How many seasons are there?", "4", "3", "5", fact = "Near the equator there are really just wet and dry seasons."),
        q("Which season comes after winter?", "Spring", "Summer", "Autumn"),
        q("Which season comes after summer?", "Autumn", "Spring", "Winter"),
        q("Which month has 28 or 29 days?", "February", "April", "June", fact = "February gets an extra day almost every four years."),
        q("What day comes after Sunday?", "Monday", "Saturday", "Friday"),
        q("What day comes before Monday?", "Sunday", "Tuesday", "Saturday")
    )

    internal val everyday: List<QuizQuestion> = listOf(
        q("How many colors are in a rainbow?", "7", "6", "8", fact = "You never quite reach a rainbow; it moves as you do."),
        q("How many primary colors are there?", "3", "4", "5"),
        q("What color do you get mixing blue and yellow?", "Green", "Purple", "Orange"),
        q("What color do you get mixing red and blue?", "Purple", "Green", "Orange"),
        q("What color do you get mixing red and yellow?", "Orange", "Green", "Purple"),
        q("What color is a banana?", "Yellow", "Blue", "Purple"),
        q("What color is the sky on a clear day?", "Blue", "Green", "Red", fact = "The sky is blue because air scatters blue light the most."),
        q("What color is grass?", "Green", "Blue", "Orange"),
        q("Which direction does the sun rise from?", "East", "West", "North", fact = "The sun only rises due east twice a year, at the equinoxes."),
        q("Which direction does the sun set in?", "West", "East", "South"),
        q("Which direction is opposite to north?", "South", "East", "West"),
        q("Which direction is opposite to east?", "West", "North", "South"),
        q("How many sides does a standard dice have?", "6", "4", "8", fact = "Opposite sides of a dice always add up to seven."),
        q("How many wheels does a standard car have?", "4", "3", "6"),
        q("How many wheels does a bicycle have?", "2", "3", "4"),
        q("How many wheels does a tricycle have?", "3", "2", "4"),
        q("How many colors are on a traffic light?", "3", "2", "4"),
        q("What color means stop on a traffic light?", "Red", "Green", "Yellow"),
        q("What color means go on a traffic light?", "Green", "Red", "Yellow"),
        q("What do you use to unlock a door?", "Key", "Spoon", "Pen"),
        q("How many cents are in a dollar?", "100", "50", "10"),
        q("What shape is a stop sign?", "Octagon", "Square", "Circle", fact = "The octagon shape lets drivers know it even from behind."),
        q("How many strings does a standard guitar have?", "6", "4", "8"),
        q("What instrument has 88 keys?", "Piano", "Guitar", "Violin", fact = "A piano has 52 white keys and 36 black ones."),
        q("What is the first month of the year?", "January", "December", "March"),
        q("What is the last month of the year?", "December", "November", "January")
    )

    internal val history: List<QuizQuestion> = listOf(
        q("Who was the first President of the United States?", "George Washington", "Abraham Lincoln", "Thomas Jefferson", fact = "Washington turned down the chance to be made king."),
        q("In which country did the Olympic Games begin?", "Greece", "Italy", "Egypt", fact = "The ancient games paused wars so athletes could travel safely."),
        q("Who painted the Mona Lisa?", "Leonardo da Vinci", "Pablo Picasso", "Vincent van Gogh", fact = "The Mona Lisa has no visible eyebrows."),
        q("The Great Pyramids were built by which civilization?", "Egyptians", "Romans", "Greeks", fact = "The builders were paid workers, not slaves."),
        q("Who is famous for a theory of gravity after an apple fell?", "Isaac Newton", "Albert Einstein", "Galileo", fact = "Newton also helped invent a new branch of math, calculus."),
        q("Who led India's nonviolent independence movement?", "Mahatma Gandhi", "Nelson Mandela", "Winston Churchill", fact = "Gandhi led a 380 km march to the sea to protest a salt tax."),
        q("The Titanic sank after hitting what?", "An iceberg", "A whale", "A reef", fact = "The Titanic was called 'unsinkable' before its first voyage."),
        q("Who is credited with inventing the light bulb?", "Thomas Edison", "Nikola Tesla", "Henry Ford", fact = "Edison held over 1,000 patents."),
        q("Who invented the telephone?", "Alexander Graham Bell", "Thomas Edison", "Samuel Morse", fact = "His first phone words were reportedly 'Mr. Watson, come here'."),
        q("The ancient Colosseum is found in which city?", "Rome", "Athens", "Cairo", fact = "The Colosseum could hold around 50,000 spectators."),
        q("Who was the nurse known as 'the Lady with the Lamp'?", "Florence Nightingale", "Marie Curie", "Joan of Arc", fact = "She used graphs to prove that hygiene saved soldiers' lives."),
        q("Who won two Nobel Prizes for work on radioactivity?", "Marie Curie", "Rosalind Franklin", "Ada Lovelace", fact = "She's the only person to win Nobel Prizes in two different sciences."),
        q("Who was the first person to walk on the moon?", "Neil Armstrong", "Buzz Aldrin", "Yuri Gagarin", fact = "His footprints are still up there; there's no wind to erase them."),
        q("Who was the first person in space?", "Yuri Gagarin", "Neil Armstrong", "John Glenn", fact = "Gagarin's whole flight lasted just 108 minutes."),
        q("Which wall famously fell in 1989?", "Berlin Wall", "Great Wall", "Hadrian's Wall", fact = "People chipped off pieces of the wall to keep as souvenirs."),
        q("Who wrote Romeo and Juliet?", "William Shakespeare", "Charles Dickens", "Mark Twain", fact = "Shakespeare coined hundreds of words we still use today."),
        q("What did ancient Egyptians write with?", "Hieroglyphics", "Latin letters", "Numbers", fact = "They stayed a mystery until the Rosetta Stone was found."),
        q("The Wright brothers are famous for inventing the?", "Airplane", "Car", "Telephone", fact = "Their first flight was shorter than a jumbo jet's wingspan.")
    )

    internal val words: List<QuizQuestion> = listOf(
        q("What is the opposite of 'hot'?", "Cold", "Dry", "Loud"),
        q("What is the opposite of 'up'?", "Down", "Left", "Over"),
        q("What is the opposite of 'big'?", "Small", "Tall", "Wide"),
        q("What is the opposite of 'fast'?", "Slow", "Quick", "Early"),
        q("What is the opposite of 'happy'?", "Sad", "Angry", "Tired"),
        q("What is the opposite of 'open'?", "Closed", "Empty", "Broken"),
        q("What is the opposite of 'day'?", "Night", "Noon", "Week"),
        q("What is the opposite of 'full'?", "Empty", "Heavy", "Wide"),
        q("What is the opposite of 'left'?", "Right", "Up", "Back"),
        q("What is the plural of 'child'?", "Children", "Childs", "Childes", fact = "English keeps many old plurals that don't just add an 's'."),
        q("What is the plural of 'mouse'?", "Mice", "Mouses", "Meese"),
        q("What is the plural of 'foot'?", "Feet", "Foots", "Feets"),
        q("What is the plural of 'tooth'?", "Teeth", "Tooths", "Teeths"),
        q("How many letters are in the English alphabet?", "26", "24", "28", fact = "The letter 'e' is the most used letter in English."),
        q("How many vowels are in the English alphabet?", "5", "6", "4"),
        q("Which of these is a vowel?", "A", "B", "C"),
        q("The first letter of the alphabet is?", "A", "B", "Z"),
        q("The last letter of the English alphabet is?", "Z", "Y", "X"),
        q("What punctuation mark ends a question?", "Question mark", "Period", "Comma"),
        q("A word meaning the same as another is a?", "Synonym", "Antonym", "Pronoun"),
        q("A word meaning the opposite of another is an?", "Antonym", "Synonym", "Homonym"),
        q("Which word is spelled correctly?", "Necessary", "Neccessary", "Necesary")
    )

    internal val sports: List<QuizQuestion> = listOf(
        q("How many players are on a soccer team on the field?", "11", "9", "7"),
        q("How many players are on a cricket team?", "11", "9", "7"),
        q("How many players are on a basketball team on court?", "5", "6", "7"),
        q("In tennis, what is a score of zero called?", "Love", "Duck", "Nil", fact = "'Love' may come from the French 'l'oeuf', meaning egg."),
        q("How many rings are on the Olympic flag?", "5", "4", "6", fact = "The five rings stand for five continents, joined together."),
        q("Which sport uses a racket and a shuttlecock?", "Badminton", "Tennis", "Squash", fact = "A shuttlecock can leave the racket at over 400 km/h."),
        q("In which sport do you score a 'touchdown'?", "American football", "Basketball", "Baseball"),
        q("Which sport is played at Wimbledon?", "Tennis", "Golf", "Cricket"),
        q("In golf, one under par on a hole is called a?", "Birdie", "Eagle", "Bogey"),
        q("How many bases are on a baseball field?", "4", "3", "5"),
        q("Which sport uses a puck?", "Ice hockey", "Soccer", "Rugby", fact = "Pucks are frozen before games so they slide better."),
        q("How long is a soccer match in regular time?", "90 minutes", "60 minutes", "120 minutes"),
        q("Three goals by one player in soccer is a?", "Hat-trick", "Triple", "Turkey", fact = "The term 'hat-trick' actually comes from cricket."),
        q("What ball is used in table tennis?", "Ping pong ball", "Tennis ball", "Golf ball")
    )
}
