package com.evolvingtheconversation.app.ui.screens.learning

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evolvingtheconversation.app.ui.components.BackButton
import kotlin.math.roundToInt
import androidx.compose.runtime.getValue
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme

private val QSurface: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFF2F3F7)
private val QCardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val QBlue        = Color(0xFF1E5EF3)
private val QBlueEnd     = Color(0xFF1E3A8A)
private val QGreen       = Color(0xFF0CBD85)
private val QRed         = Color(0xFFE53935)
private val QOrange      = Color(0xFFFFA800)
private val QPurple      = Color(0xFF7C3AED)
private val QTeal        = Color(0xFF0891B2)
private val QTextPri: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)
private val QTextSec: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)
private val QChipBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1C2A4A) else Color(0xFFEEF3FF)
private val QGreenBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF0F2E24) else Color(0xFFE6FAF4)
private val QRedBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF3A1414) else Color(0xFFFFEEEE)
private val QBorderGray: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF3A3D45) else Color(0xFFE5E7EB)

// Palette used to color-code matched pairs in "Match the Pairs" questions.
private val PairColors = listOf(QBlue, QGreen, QOrange, QPurple, QTeal, QRed)

// ─────────────────────────────────────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────────────────────────────────────

/** The three question formats supported by the Quiz Center. */
enum class QuestionType {
    MULTIPLE_CHOICE,
    FILL_IN_THE_BLANK,
    MATCH_THE_PAIRS
}

/**
 * Unified question model. Depending on [type], only the relevant fields are
 * populated — the rest are left at their defaults.
 *
 *  - MULTIPLE_CHOICE   -> options, correctOptionIndex
 *  - FILL_IN_THE_BLANK -> blankAnswer
 *  - MATCH_THE_PAIRS   -> leftItems, rightItems, correctPairs
 *                         (correctPairs maps a leftItems index -> the index
 *                         in rightItems that correctly pairs with it)
 */
data class QuizQuestion(
    val type: QuestionType,
    val question: String,
    val options: List<String> = emptyList(),
    val correctOptionIndex: Int = -1,
    val blankAnswer: String = "",
    val leftItems: List<String> = emptyList(),
    val rightItems: List<String> = emptyList(),
    val correctPairs: Map<Int, Int> = emptyMap()
)

/**
 * Per-question answer state. One sealed hierarchy covers all three question
 * types so the whole quiz can be driven off a single `List<QuizAnswer>`,
 * exactly like the original `List<Int>` did for MCQ-only answers.
 */
sealed class QuizAnswer {
    object Unanswered : QuizAnswer()
    data class Mcq(val selectedIndex: Int) : QuizAnswer()
    data class FillBlank(val text: String = "", val submitted: Boolean = false) : QuizAnswer()
    data class Match(val matches: Map<Int, Int> = emptyMap(), val submitted: Boolean = false) : QuizAnswer()
}

// ─────────────────────────────────────────────────────────────────────────────
// QUESTION BANK
// These are POOLS, not a fixed running order. Every time a quiz starts (or is
// retried), `generateQuizQuestions()` below picks 5 random questions from
// each pool (see QUESTIONS_PER_TYPE) in random order — so both WHICH
// questions show up and the order they appear in are different every
// attempt. Add as many more questions as you like to any of the three lists
// below; nothing else needs to change for them to be picked up.
// ─────────────────────────────────────────────────────────────────────────────

private val grammarMcqQuestions = listOf(
    QuizQuestion(
        type = QuestionType.MULTIPLE_CHOICE,
        question = "Choose the correct form: \"She ___ learning English for two years.\"",
        options = listOf("have been", "has been", "had being", "have being"),
        correctOptionIndex = 1
    ),
    QuizQuestion(
        type = QuestionType.MULTIPLE_CHOICE,
        question = "Which sentence uses the passive voice correctly?",
        options = listOf(
            "She was given the award.",
            "She gave the award.",
            "The award was giving to her.",
            "They have given she the award."
        ),
        correctOptionIndex = 0
    ),
    QuizQuestion(
        type = QuestionType.MULTIPLE_CHOICE,
        question = "Choose the correct article: \"___ Eiffel Tower is in Paris.\"",
        options = listOf("A", "An", "The", "No article"),
        correctOptionIndex = 2
    ),
    QuizQuestion(
        type = QuestionType.MULTIPLE_CHOICE,
        question = "Select the correct conditional: \"If I ___ you, I would study harder.\"",
        options = listOf("am", "was", "were", "will be"),
        correctOptionIndex = 2
    ),
    QuizQuestion(
        type = QuestionType.MULTIPLE_CHOICE,
        question = "Which modal expresses strong obligation?",
        options = listOf("might", "could", "must", "would"),
        correctOptionIndex = 2
    ),
    QuizQuestion(
        type = QuestionType.MULTIPLE_CHOICE,
        question = "Choose the correct sentence.",
        options = listOf(
            "She don't like coffee.",
            "She doesn't like coffee.",
            "She doesn't likes coffee.",
            "She not like coffee."
        ),
        correctOptionIndex = 1
    ),
    QuizQuestion(
        type = QuestionType.MULTIPLE_CHOICE,
        question = "Choose the correct verb: \"My brother ___ to school by bus every day.\"",
        options = listOf("go", "goes", "going", "gone"),
        correctOptionIndex = 1
    ),
    QuizQuestion(
        type = QuestionType.MULTIPLE_CHOICE,
        question = "Choose the correct preposition: \"The keys are ___ the table.\"",
        options = listOf("in", "on", "at", "between"),
        correctOptionIndex = 1
    ),
    QuizQuestion(
        type = QuestionType.MULTIPLE_CHOICE,
        question = "Which word is the synonym of \"rapid\"?",
        options = listOf("Slow", "Quick", "Heavy", "Bright"),
        correctOptionIndex = 1
    ),
    QuizQuestion(
        type = QuestionType.MULTIPLE_CHOICE,
        question = "Choose the correct article: \"I saw ___ elephant at the zoo.\"",
        options = listOf("a", "an", "the", "no article"),
        correctOptionIndex = 1
    ),
    QuizQuestion(
        type = QuestionType.MULTIPLE_CHOICE,
        question = "Which sentence is in the Present Continuous tense?",
        options = listOf(
            "She reads every night.",
            "She is reading a book.",
            "She read a book.",
            "She has read a book."
        ),
        correctOptionIndex = 1
    ),
    QuizQuestion(
        type = QuestionType.MULTIPLE_CHOICE,
        question = "Choose the correct modal verb: \"You ___ wear a seatbelt while driving.\"",
        options = listOf("can", "must", "may", "might"),
        correctOptionIndex = 1
    ),
    QuizQuestion(
        type = QuestionType.MULTIPLE_CHOICE,
        question = "Choose the opposite of \"expensive\".",
        options = listOf("Cheap", "Costly", "Large", "Valuable"),
        correctOptionIndex = 0
    ),
    QuizQuestion(
        type = QuestionType.MULTIPLE_CHOICE,
        question = "Complete the sentence: \"If it rains tomorrow, we ___ at home.\"",
        options = listOf("stay", "stayed", "will stay", "staying"),
        correctOptionIndex = 2
    ),
    QuizQuestion(
        type = QuestionType.MULTIPLE_CHOICE,
        question = "Choose the correct question.",
        options = listOf(
            "Where you live?",
            "Where do you live?",
            "Where does you live?",
            "Where living you?"
        ),
        correctOptionIndex = 1
    ),
)

private val fillBlankQuestions = listOf(
    QuizQuestion(
        type = QuestionType.FILL_IN_THE_BLANK,
        question = "Complete the sentence: \"I ___ (go) to the market every Sunday.\"",
        blankAnswer = "go"
    ),
    QuizQuestion(
        type = QuestionType.FILL_IN_THE_BLANK,
        question = "Fill in the blank: \"Water boils at 100 degrees ___.\"",
        blankAnswer = "celsius"
    ),
    QuizQuestion(
        type = QuestionType.FILL_IN_THE_BLANK,
        question = "Complete: \"The opposite of 'happy' is ___.\"",
        blankAnswer = "sad"
    ),
    QuizQuestion(
        type = QuestionType.FILL_IN_THE_BLANK,
        question = "Fill in the blank: \"She has been living here ___ 2019.\"",
        blankAnswer = "since"
    ),
    QuizQuestion(
        type = QuestionType.FILL_IN_THE_BLANK,
        question = "Complete: \"An animal that meows is called a ___.\"",
        blankAnswer = "cat"
    ),
    QuizQuestion(
        type = QuestionType.FILL_IN_THE_BLANK,
        question = "Complete the sentence: \"She ___ (study) English every evening.\"",
        blankAnswer = "studies"
    ),
    QuizQuestion(
        type = QuestionType.FILL_IN_THE_BLANK,
        question = "Fill in the blank: \"We arrived ___ the airport early.\"",
        blankAnswer = "at"
    ),
    QuizQuestion(
        type = QuestionType.FILL_IN_THE_BLANK,
        question = "Complete the sentence: \"I have lived here ___ five years.\"",
        blankAnswer = "for"
    ),
    QuizQuestion(
        type = QuestionType.FILL_IN_THE_BLANK,
        question = "Fill in the blank: \"The sun ___ in the east.\"",
        blankAnswer = "rises"
    ),
    QuizQuestion(
        type = QuestionType.FILL_IN_THE_BLANK,
        question = "Complete the sentence: \"Yesterday, they ___ (watch) a movie.\"",
        blankAnswer = "watched"
    ),
    QuizQuestion(
        type = QuestionType.FILL_IN_THE_BLANK,
        question = "Fill in the blank: \"Please turn ___ the lights before leaving.\"",
        blankAnswer = "off"
    ),
    QuizQuestion(
        type = QuestionType.FILL_IN_THE_BLANK,
        question = "Complete the sentence: \"My sister is ___ than me.\"",
        blankAnswer = "taller"
    ),
    QuizQuestion(
        type = QuestionType.FILL_IN_THE_BLANK,
        question = "Fill in the blank: \"He has ___ his homework already.\"",
        blankAnswer = "finished"
    ),
    QuizQuestion(
        type = QuestionType.FILL_IN_THE_BLANK,
        question = "Complete the sentence: \"There ___ three apples on the table.\"",
        blankAnswer = "are"
    ),
    QuizQuestion(
        type = QuestionType.FILL_IN_THE_BLANK,
        question = "Fill in the blank: \"The opposite of 'early' is ___.\"",
        blankAnswer = "late"
    ),
)

private val matchPairQuestions = listOf(
    QuizQuestion(
        type = QuestionType.MATCH_THE_PAIRS,
        question = "Match each word with its correct meaning.",
        leftItems = listOf("Happy", "Big", "Fast"),
        rightItems = listOf("Quick", "Large", "Joyful"),
        correctPairs = mapOf(0 to 2, 1 to 1, 2 to 0)
    ),
    QuizQuestion(
        type = QuestionType.MATCH_THE_PAIRS,
        question = "Match each country with its capital.",
        leftItems = listOf("France", "Japan", "Egypt"),
        rightItems = listOf("Cairo", "Tokyo", "Paris"),
        correctPairs = mapOf(0 to 2, 1 to 1, 2 to 0)
    ),
    QuizQuestion(
        type = QuestionType.MATCH_THE_PAIRS,
        question = "Match each verb with its past tense form.",
        leftItems = listOf("Go", "Eat", "See"),
        rightItems = listOf("Saw", "Went", "Ate"),
        correctPairs = mapOf(0 to 1, 1 to 2, 2 to 0)
    ),
    QuizQuestion(
        type = QuestionType.MATCH_THE_PAIRS,
        question = "Match each word with its synonym.",
        leftItems = listOf("Smart", "Angry", "Tiny"),
        rightItems = listOf("Furious", "Small", "Clever"),
        correctPairs = mapOf(0 to 2, 1 to 0, 2 to 1)
    ),
    QuizQuestion(
        type = QuestionType.MATCH_THE_PAIRS,
        question = "Match each tool with what it's used for.",
        leftItems = listOf("Hammer", "Scissors", "Pencil"),
        rightItems = listOf("Writing", "Cutting", "Hammering nails"),
        correctPairs = mapOf(0 to 2, 1 to 1, 2 to 0)
    ),
    QuizQuestion(
        type = QuestionType.MATCH_THE_PAIRS,
        question = "Match the word with its meaning.",
        leftItems = listOf("Honest", "Brave", "Tiny"),
        rightItems = listOf("Truthful", "Courageous", "Very small"),
        correctPairs = mapOf(0 to 0, 1 to 1, 2 to 2)
    ),
    QuizQuestion(
        type = QuestionType.MATCH_THE_PAIRS,
        question = "Match the country with its capital.",
        leftItems = listOf("Canada", "Italy", "Australia"),
        rightItems = listOf("Ottawa", "Rome", "Canberra"),
        correctPairs = mapOf(0 to 0, 1 to 1, 2 to 2)
    ),
    QuizQuestion(
        type = QuestionType.MATCH_THE_PAIRS,
        question = "Match the verb with its past tense.",
        leftItems = listOf("Write", "Buy", "Speak"),
        rightItems = listOf("Wrote", "Bought", "Spoke"),
        correctPairs = mapOf(0 to 0, 1 to 1, 2 to 2)
    ),
    QuizQuestion(
        type = QuestionType.MATCH_THE_PAIRS,
        question = "Match the adjective with its opposite.",
        leftItems = listOf("Strong", "Empty", "Clean"),
        rightItems = listOf("Weak", "Full", "Dirty"),
        correctPairs = mapOf(0 to 0, 1 to 1, 2 to 2)
    ),
    QuizQuestion(
        type = QuestionType.MATCH_THE_PAIRS,
        question = "Match the word with its category.",
        leftItems = listOf("Apple", "Carrot", "Salmon"),
        rightItems = listOf("Fruit", "Vegetable", "Fish"),
        correctPairs = mapOf(0 to 0, 1 to 1, 2 to 2)
    ),
    QuizQuestion(
        type = QuestionType.MATCH_THE_PAIRS,
        question = "Match the occupation with the workplace.",
        leftItems = listOf("Doctor", "Teacher", "Chef"),
        rightItems = listOf("Hospital", "School", "Restaurant"),
        correctPairs = mapOf(0 to 0, 1 to 1, 2 to 2)
    ),
    QuizQuestion(
        type = QuestionType.MATCH_THE_PAIRS,
        question = "Match the sentence with its tense.",
        leftItems = listOf("I eat lunch.", "I ate lunch.", "I will eat lunch."),
        rightItems = listOf("Present Simple", "Past Simple", "Future Simple"),
        correctPairs = mapOf(0 to 0, 1 to 1, 2 to 2)
    ),
    QuizQuestion(
        type = QuestionType.MATCH_THE_PAIRS,
        question = "Match the phrasal verb with its meaning.",
        leftItems = listOf("Wake up", "Sit down", "Turn off"),
        rightItems = listOf("Stop sleeping", "Take a seat", "Switch off"),
        correctPairs = mapOf(0 to 0, 1 to 1, 2 to 2)
    ),
    QuizQuestion(
        type = QuestionType.MATCH_THE_PAIRS,
        question = "Match the word with its synonym.",
        leftItems = listOf("Begin", "End", "Silent"),
        rightItems = listOf("Start", "Finish", "Quiet"),
        correctPairs = mapOf(0 to 0, 1 to 1, 2 to 2)
    ),
    QuizQuestion(
        type = QuestionType.MATCH_THE_PAIRS,
        question = "Match the sentence with the correct punctuation.",
        leftItems = listOf("What is your name", "I love English", "Wow"),
        rightItems = listOf("?", ".", "!"),
        correctPairs = mapOf(0 to 0, 1 to 1, 2 to 2)
    ),
)

/** How many questions of EACH type are pulled into a single quiz attempt. */
private const val QUESTIONS_PER_TYPE = 5

/**
 * Builds one full quiz attempt: from each 15-question pool (MCQ,
 * Fill-in-the-Blank, Match-the-Pairs), [QUESTIONS_PER_TYPE] questions are
 * picked at random with no repeats, then the three groups are concatenated
 * in that order (so the on-screen "MULTIPLE CHOICE" / "FILL IN THE BLANK" /
 * "MATCH THE PAIRS" badge grouping still reads sensibly).
 *
 * Because `shuffled()` randomizes the whole pool before `take()` grabs the
 * first few, both WHICH questions are chosen and the ORDER they appear in
 * change every time this is called — so call it fresh every time a quiz
 * starts or is retried.
 *
 * If you want every question inside "Match the Pairs" to also show its left
 * and right columns in a shuffled order (instead of just shuffling which
 * question comes up), that's a slightly bigger change — ask if you want that
 * added too.
 */
private fun generateQuizQuestions(): List<QuizQuestion> {
    return grammarMcqQuestions.shuffled().take(QUESTIONS_PER_TYPE) +
            fillBlankQuestions.shuffled().take(QUESTIONS_PER_TYPE) +
            matchPairQuestions.shuffled().take(QUESTIONS_PER_TYPE)
}

// ─────────────────────────────────────────────────────────────────────────────
// SCORING
// Each question contributes a fractional score in [0f, 1f]. Match-the-Pairs
// gets partial credit: (# correct pairs) / (total pairs in that question).
// ─────────────────────────────────────────────────────────────────────────────
private fun scoreFor(question: QuizQuestion, answer: QuizAnswer): Float {
    return when (question.type) {
        QuestionType.MULTIPLE_CHOICE -> {
            val a = answer as? QuizAnswer.Mcq ?: return 0f
            if (a.selectedIndex == question.correctOptionIndex) 1f else 0f
        }
        QuestionType.FILL_IN_THE_BLANK -> {
            val a = answer as? QuizAnswer.FillBlank ?: return 0f
            if (a.text.trim().equals(question.blankAnswer.trim(), ignoreCase = true)) 1f else 0f
        }
        QuestionType.MATCH_THE_PAIRS -> {
            val a = answer as? QuizAnswer.Match ?: return 0f
            if (question.correctPairs.isEmpty()) return 0f
            val correctCount = question.correctPairs.count { (left, right) -> a.matches[left] == right }
            correctCount.toFloat() / question.correctPairs.size.toFloat()
        }
    }
}

private fun isAnswered(answer: QuizAnswer): Boolean = when (answer) {
    is QuizAnswer.Unanswered -> false
    is QuizAnswer.Mcq -> answer.selectedIndex >= 0
    is QuizAnswer.FillBlank -> answer.submitted
    is QuizAnswer.Match -> answer.submitted
}

/** Formats a fractional score nicely: whole numbers show as "4", otherwise "3.7". */
private fun formatScore(value: Double): String {
    val rounded = (value * 10).roundToInt() / 10.0
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        "%.1f".format(rounded)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// INTERNAL FLOW STATE
// ─────────────────────────────────────────────────────────────────────────────
private enum class QuizState { QUESTIONS, RESULT }

// ─────────────────────────────────────────────────────────────────────────────
// ENTRY POINT — the only public composable in this file.
// [onQuizComplete] is an optional hook fired once the result screen is shown,
// with (scorePercent, pointsEarned, totalQuestions). Wire this to a
// ViewModel/repository to persist progress for the logged-in user without
// having to touch this screen's UI again.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun QuizScreen(
    topic: String = "Grammar",
    onBack: () -> Unit = {},
    onQuizComplete: (scorePercent: Int, pointsEarned: Double, total: Int) -> Unit = { _, _, _ -> }
) {
    // Freshly shuffled every time this composable enters composition (i.e.
    // every time the user opens/starts a quiz) — NOT a fixed top-level list
    // anymore, so the order differs each attempt.
    var questions by remember { mutableStateOf(generateQuizQuestions()) }

    var quizState by remember { mutableStateOf(QuizState.QUESTIONS) }
    var currentIndex by remember { mutableStateOf(0) }
    var answers by remember {
        mutableStateOf<List<QuizAnswer>>(questions.map { QuizAnswer.Unanswered })
    }

    // Derived results (only meaningful when quizState == RESULT)
    val pointsEarned = answers.zip(questions).sumOf { (ans, q) -> scoreFor(q, ans).toDouble() }
    val score = if (questions.isEmpty()) 0 else ((pointsEarned / questions.size) * 100).roundToInt()

    // Fire the completion callback exactly once per finished attempt.
    LaunchedEffect(quizState) {
        if (quizState == QuizState.RESULT) {
            onQuizComplete(score, pointsEarned, questions.size)
        }
    }

    AnimatedContent(
        targetState = quizState,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "quiz_state"
    ) { state ->
        when (state) {
            QuizState.QUESTIONS -> {
                QuizQuestionScreen(
                    topic = topic,
                    questions = questions,
                    currentIndex = currentIndex,
                    answer = answers[currentIndex],
                    onBack = onBack,
                    onAnswerChange = { newAnswer ->
                        answers = answers.toMutableList().also { it[currentIndex] = newAnswer }
                    },
                    onNext = {
                        if (currentIndex < questions.lastIndex) {
                            currentIndex++
                        } else {
                            quizState = QuizState.RESULT
                        }
                    }
                )
            }

            QuizState.RESULT -> {
                QuizResultScreen(
                    topic = topic,
                    score = score,
                    pointsEarned = pointsEarned,
                    total = questions.size,
                    onBack = onBack,          // back to Practice
                    onRetry = {                // reshuffle and start over
                        questions = generateQuizQuestions()
                        answers = questions.map { QuizAnswer.Unanswered }
                        currentIndex = 0
                        quizState = QuizState.QUESTIONS
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// QUESTION SCREEN — shared chrome (top bar + progress bar) for every type,
// dispatching the actual question body via when(question.type).
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun QuizQuestionScreen(
    topic: String,
    questions: List<QuizQuestion>,
    currentIndex: Int,
    answer: QuizAnswer,
    onBack: () -> Unit,
    onAnswerChange: (QuizAnswer) -> Unit,
    onNext: () -> Unit
) {
    val question = questions[currentIndex]
    val total = questions.size
    val progress = (currentIndex + 1).toFloat() / total.toFloat()
    val answered = isAnswered(answer)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QSurface)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(onBack = onBack)
            Spacer(Modifier.width(4.dp))
            Text("Quiz · $topic", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = QTextPri)
        }

        // Progress bar — numbering continues across all question types.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(QBorderGray)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(listOf(QGreen, QBlue)),
                            RoundedCornerShape(4.dp)
                        )
                )
            }
            Text(
                text = "${currentIndex + 1}/$total",
                fontSize = 12.sp,
                color = QTextSec,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp)
        ) {
            // Badge — label matches the current question type.
            val badgeLabel = when (question.type) {
                QuestionType.MULTIPLE_CHOICE -> "MULTIPLE CHOICE"
                QuestionType.FILL_IN_THE_BLANK -> "FILL IN THE BLANK"
                QuestionType.MATCH_THE_PAIRS -> "MATCH THE PAIRS"
            }
            Surface(shape = RoundedCornerShape(6.dp), color = QChipBg) {
                Text(
                    text = badgeLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = QBlue,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = question.question,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = QTextPri,
                lineHeight = 25.sp
            )

            Spacer(Modifier.height(24.dp))

            // Dispatch to the correct question body.
            when (question.type) {
                QuestionType.MULTIPLE_CHOICE -> {
                    val mcqAnswer = answer as? QuizAnswer.Mcq ?: QuizAnswer.Mcq(-1)
                    MultipleChoiceQuestionBody(
                        question = question,
                        selectedIndex = mcqAnswer.selectedIndex,
                        onSelect = { index -> onAnswerChange(QuizAnswer.Mcq(index)) }
                    )
                }
                QuestionType.FILL_IN_THE_BLANK -> {
                    val fillAnswer = answer as? QuizAnswer.FillBlank ?: QuizAnswer.FillBlank()
                    FillInTheBlankQuestionBody(
                        question = question,
                        answer = fillAnswer,
                        onTextChange = { text -> onAnswerChange(fillAnswer.copy(text = text)) },
                        onSubmit = { onAnswerChange(fillAnswer.copy(submitted = true)) }
                    )
                }
                QuestionType.MATCH_THE_PAIRS -> {
                    val matchAnswer = answer as? QuizAnswer.Match ?: QuizAnswer.Match()
                    MatchThePairsQuestionBody(
                        question = question,
                        answer = matchAnswer,
                        onMatchesChange = { matches -> onAnswerChange(matchAnswer.copy(matches = matches)) },
                        onSubmit = { onAnswerChange(matchAnswer.copy(submitted = true)) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Shared Next/See-Results control — identical across all question types.
            if (answered) {
                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = QBlue)
                ) {
                    Text(
                        text = if (currentIndex == questions.lastIndex) "See Results" else "Next Question",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MULTIPLE CHOICE — unchanged visual behavior, extracted into its own
// reusable composable so QuizQuestionScreen can dispatch to it.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MultipleChoiceQuestionBody(
    question: QuizQuestion,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val answered = selectedIndex >= 0

    question.options.forEachIndexed { index, option ->
        val isSelected = selectedIndex == index
        val isCorrect = index == question.correctOptionIndex
        val showCorrect = answered && isCorrect
        val showWrong = answered && isSelected && !isCorrect

        val borderColor = when {
            showCorrect -> QGreen
            showWrong -> QRed
            isSelected -> QBlue
            else -> QBorderGray
        }
        val bgColor = when {
            showCorrect -> QGreenBg
            showWrong -> QRedBg
            isSelected -> QChipBg
            else -> QCardWhite
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                .background(bgColor, RoundedCornerShape(14.dp))
                .clickable(enabled = !answered) { onSelect(index) }
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = option,
                fontSize = 14.sp,
                fontWeight = if (isSelected || showCorrect) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    showCorrect -> QGreen
                    showWrong -> QRed
                    isSelected -> QBlue
                    else -> QTextPri
                }
            )
            if (showCorrect) Icon(Icons.Filled.Check, null, tint = QGreen, modifier = Modifier.size(18.dp))
            if (showWrong) Icon(Icons.Filled.Close, null, tint = QRed, modifier = Modifier.size(18.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FILL IN THE BLANK
// TextField -> Submit -> locked feedback (correct/incorrect) -> Next (shared).
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FillInTheBlankQuestionBody(
    question: QuizQuestion,
    answer: QuizAnswer.FillBlank,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val submitted = answer.submitted
    val isCorrect = submitted &&
            answer.text.trim().equals(question.blankAnswer.trim(), ignoreCase = true)

    val borderColor = when {
        !submitted -> QBorderGray
        isCorrect -> QGreen
        else -> QRed
    }

    OutlinedTextField(
        value = answer.text,
        onValueChange = { if (!submitted) onTextChange(it) },
        enabled = !submitted,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Type your answer…", color = QTextSec) },
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            containerColor = QCardWhite,
            unfocusedBorderColor = borderColor,
            focusedBorderColor = if (submitted) borderColor else QBlue,
            disabledBorderColor = borderColor,
            disabledTextColor = if (isCorrect) QGreen else QRed
        ),
        trailingIcon = {
            if (submitted) {
                Icon(
                    imageVector = if (isCorrect) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = null,
                    tint = if (isCorrect) QGreen else QRed
                )
            }
        }
    )

    Spacer(Modifier.height(10.dp))

    if (submitted) {
        // Locked feedback banner.
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isCorrect) QGreenBg else QRedBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isCorrect) "Correct!" else "Correct answer: ${question.blankAnswer}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isCorrect) QGreen else QRed,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    } else {
        Button(
            onClick = onSubmit,
            enabled = answer.text.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = QBlue)
        ) {
            Text("Submit", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MATCH THE PAIRS
// Tap a left item, then a right item, to link them. Each linked pair gets its
// own color. Submit becomes available once every left item has a match; after
// submitting, each pair is marked correct/incorrect (partial credit supported).
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MatchThePairsQuestionBody(
    question: QuizQuestion,
    answer: QuizAnswer.Match,
    onMatchesChange: (Map<Int, Int>) -> Unit,
    onSubmit: () -> Unit
) {
    // Local "currently selected left item awaiting a right item" cursor.
    // Keyed on the question so it resets cleanly when moving to a new question.
    var pendingLeft by remember(question) { mutableStateOf<Int?>(null) }

    // Right column order is shuffled once per question (keyed on the question
    // object) so the correct answer position isn't always predictable either.
    val shuffledRightIndices = remember(question) { question.rightItems.indices.shuffled() }

    val matches = answer.matches
    val submitted = answer.submitted
    val allMatched = matches.size == question.leftItems.size

    // Stable color per left-item index so a pair keeps its color once matched.
    fun colorFor(leftIndex: Int): Color = PairColors[leftIndex % PairColors.size]

    // Reverse lookup: which left index currently claims this right index.
    val rightToLeft: Map<Int, Int> = matches.entries.associate { (l, r) -> r to l }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Left column ──────────────────────────────────────────────────
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            question.leftItems.forEachIndexed { leftIndex, label ->
                val isMatched = matches.containsKey(leftIndex)
                val isPending = pendingLeft == leftIndex
                val pairColor = if (isMatched) colorFor(leftIndex) else QBlue

                val isCorrectMatch = submitted && matches[leftIndex] == question.correctPairs[leftIndex]
                val isWrongMatch = submitted && isMatched && !isCorrectMatch

                val borderColor = when {
                    isWrongMatch -> QRed
                    submitted && isCorrectMatch -> QGreen
                    isMatched -> pairColor
                    isPending -> QBlue
                    else -> QBorderGray
                }
                val bgColor = when {
                    isWrongMatch -> QRedBg
                    submitted && isCorrectMatch -> QGreenBg
                    isMatched -> pairColor.copy(alpha = 0.10f)
                    isPending -> QChipBg
                    else -> QCardWhite
                }

                MatchItemChip(
                    label = label,
                    borderColor = borderColor,
                    bgColor = bgColor,
                    textColor = if (isMatched || isPending) pairColor.takeIf { !submitted } ?: QTextPri else QTextPri,
                    enabled = !submitted,
                    trailingIcon = when {
                        submitted && isCorrectMatch -> Icons.Filled.Check
                        isWrongMatch -> Icons.Filled.Close
                        else -> null
                    },
                    trailingIconTint = if (isWrongMatch) QRed else QGreen,
                    onClick = {
                        if (submitted) return@MatchItemChip
                        if (isMatched) {
                            // Tapping an already-matched left item lets the user re-pick it.
                            onMatchesChange(matches - leftIndex)
                            pendingLeft = leftIndex
                        } else {
                            pendingLeft = leftIndex
                        }
                    }
                )
            }
        }

        // ── Right column ─────────────────────────────────────────────────
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            shuffledRightIndices.forEach { rightIndex ->
                val label = question.rightItems[rightIndex]
                val matchedLeft = rightToLeft[rightIndex]
                val isMatched = matchedLeft != null
                val pairColor = if (isMatched) colorFor(matchedLeft!!) else QBlue

                val isCorrectMatch = submitted && isMatched &&
                        question.correctPairs[matchedLeft] == rightIndex
                val isWrongMatch = submitted && isMatched && !isCorrectMatch

                val borderColor = when {
                    isWrongMatch -> QRed
                    submitted && isCorrectMatch -> QGreen
                    isMatched -> pairColor
                    else -> QBorderGray
                }
                val bgColor = when {
                    isWrongMatch -> QRedBg
                    submitted && isCorrectMatch -> QGreenBg
                    isMatched -> pairColor.copy(alpha = 0.10f)
                    else -> QCardWhite
                }

                MatchItemChip(
                    label = label,
                    borderColor = borderColor,
                    bgColor = bgColor,
                    textColor = QTextPri,
                    enabled = !submitted,
                    trailingIcon = when {
                        submitted && isCorrectMatch -> Icons.Filled.Check
                        isWrongMatch -> Icons.Filled.Close
                        else -> null
                    },
                    trailingIconTint = if (isWrongMatch) QRed else QGreen,
                    onClick = {
                        if (submitted) return@MatchItemChip
                        val left = pendingLeft ?: return@MatchItemChip
                        // A right item can only belong to one pair at a time.
                        val updated = matches.filterValues { it != rightIndex } + (left to rightIndex)
                        onMatchesChange(updated)
                        pendingLeft = null
                    }
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    if (!submitted) {
        Button(
            onClick = onSubmit,
            enabled = allMatched,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = QBlue)
        ) {
            Text(
                text = if (allMatched) "Submit" else "Match all pairs to continue",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    } else {
        val correctPairCount = question.correctPairs.count { (l, r) -> matches[l] == r }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (correctPairCount == question.correctPairs.size) QGreenBg else QRedBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "$correctPairCount of ${question.correctPairs.size} pairs correct",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (correctPairCount == question.correctPairs.size) QGreen else QRed,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

/** A single tappable chip used by both columns in Match the Pairs. */
@Composable
private fun MatchItemChip(
    label: String,
    borderColor: Color,
    bgColor: Color,
    textColor: Color,
    enabled: Boolean,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector?,
    trailingIconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .background(bgColor, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
        if (trailingIcon != null) {
            Icon(trailingIcon, null, tint = trailingIconTint, modifier = Modifier.size(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RESULT SCREEN — same design as before. Now shows fractional points when
// Match-the-Pairs partial credit produces a non-whole score.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun QuizResultScreen(
    topic: String,
    score: Int,
    pointsEarned: Double,
    total: Int,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    val wrongPoints = total - pointsEarned

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QSurface)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(onBack = onBack)
            Spacer(Modifier.width(4.dp))
            Text("Quiz Results", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = QTextPri)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Score hero
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(QBlue, QBlueEnd)),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(vertical = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(Color.White.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEvents,
                            contentDescription = null,
                            tint = QOrange,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text("$score%", fontWeight = FontWeight.ExtraBold, fontSize = 56.sp, color = Color.White)
                    Text(
                        "${formatScore(pointsEarned)} out of $total points",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    Spacer(Modifier.height(14.dp))

                    val (pillColor, pillLabel) = when {
                        score >= 80 -> QGreen to "🎉 Excellent!"
                        score >= 50 -> QOrange to "👍 Good effort!"
                        else -> QRed to "💪 Keep practicing!"
                    }
                    Surface(shape = RoundedCornerShape(50.dp), color = pillColor) {
                        Text(
                            text = pillLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Stat cards
            Text("Progress Breakdown", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = QTextPri, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ResultStatCard("Correct", formatScore(pointsEarned), QGreen, Color(0xFFE6FAF4), Modifier.weight(1f))
                ResultStatCard("Missed", formatScore(wrongPoints), QRed, Color(0xFFFFEEEE), Modifier.weight(1f))
                ResultStatCard("Score", "$score%", QBlue, Color(0xFFEEF3FF), Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))

            // Accuracy bar card
            Text("Accuracy", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = QTextPri, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = QCardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Your score", fontSize = 13.sp, color = QTextSec)
                        Text("$score%", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = QTextPri)
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(QBorderGray, RoundedCornerShape(5.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(score / 100f)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(listOf(QBlue, QGreen)),
                                    RoundedCornerShape(5.dp)
                                )
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("0%", fontSize = 11.sp, color = QTextSec)
                        Text("Pass: 60%", fontSize = 11.sp, color = QTextSec)
                        Text("100%", fontSize = 11.sp, color = QTextSec)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // Retry
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = QBlue)
            ) {
                Icon(Icons.Outlined.Replay, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Retry Quiz", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }

            Spacer(Modifier.height(12.dp))

            // Back to Practice
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, QBlue)
            ) {
                Text("Back to Practice", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = QBlue)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STAT CARD — unchanged
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ResultStatCard(
    label: String,
    value: String,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = color)
            Text(label, fontSize = 12.sp, color = color.copy(alpha = 0.75f), fontWeight = FontWeight.Medium)
        }
    }
}