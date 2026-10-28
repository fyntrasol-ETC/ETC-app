package com.evolvingtheconversation.app.ui.screens.learning

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evolvingtheconversation.app.ui.components.BackButton
import kotlin.math.roundToInt
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evolvingtheconversation.viewmodel.AuthViewModel
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

private enum class GQState { QUESTIONS, RESULT }

/**
 * Quiz screen for a single grammar topic — same visual language as the
 * general QuizScreen (top bar, gradient progress bar, bordered MCQ cards
 * with check/× feedback icons, and the identical gradient-hero result
 * screen with stat cards + accuracy bar).
 *
 * `remember(topicTitle, quizKey)` runs its initializer once per time this
 * composable enters composition for a given topic/key, so every fresh entry
 * (tapping a topic card, or "Retry Quiz" bumping quizKey) reshuffles the
 * question pool. Recompositions from selecting an answer do NOT reshuffle,
 * since `remember` caches the result across those.
 */
@Composable
fun GrammarQuizScreen(
    topicTitle: String,
    onBack: () -> Unit = {},
    onFinished: (score: Int, total: Int) -> Unit = { _, _ -> }
) {
    val authViewModel: AuthViewModel = viewModel()
    var quizKey by remember { mutableIntStateOf(0) } // bump to force a manual reshuffle (Retry)

    val questions = remember(topicTitle, quizKey) {
        grammarMCQMap[topicTitle]?.shuffledQuiz() ?: emptyList()
    }

    if (questions.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(QSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(onBack = onBack)
                Spacer(Modifier.width(4.dp))
                Text(topicTitle, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = QTextPri)
            }
            Text(
                "No questions found for this topic.",
                color = QTextSec,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
            )
        }
        return
    }

    var state by remember(topicTitle, quizKey) { mutableStateOf(GQState.QUESTIONS) }
    var currentIndex by remember(topicTitle, quizKey) { mutableIntStateOf(0) }
    var selectedOption by remember(topicTitle, quizKey) { mutableStateOf<String?>(null) }
    var score by remember(topicTitle, quizKey) { mutableIntStateOf(0) }

    LaunchedEffect(state) {
        if (state == GQState.RESULT) {
            onFinished(score, questions.size)
            authViewModel.recordGrammarQuizSolved() // record daily skill stat
        }
    }

    AnimatedContent(
        targetState = state,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "grammar_quiz_state"
    ) { target ->
        when (target) {
            GQState.QUESTIONS -> {
                val question = questions[currentIndex]
                GrammarQuestionScreen(
                    topicTitle = topicTitle,
                    currentIndex = currentIndex,
                    total = questions.size,
                    questionText = question.question,
                    options = question.options,
                    correctAnswer = question.correctAnswer,
                    selectedOption = selectedOption,
                    onBack = onBack,
                    onSelect = { option ->
                        if (selectedOption == null) {
                            selectedOption = option
                            if (option == question.correctAnswer) score++
                        }
                    },
                    onNext = {
                        if (currentIndex < questions.lastIndex) {
                            currentIndex++
                            selectedOption = null
                        } else {
                            state = GQState.RESULT
                        }
                    }
                )
            }

            GQState.RESULT -> {
                GrammarQuizResultScreen(
                    score = score,
                    total = questions.size,
                    onBack = onBack,
                    onRetry = { quizKey++ } // remember(topicTitle, quizKey) blocks reset themselves
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// QUESTION SCREEN — top bar, gradient progress bar, badge, and bordered MCQ
// options with check/× feedback — mirrors QuizQuestionScreen in QuizScreen.kt.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GrammarQuestionScreen(
    topicTitle: String,
    currentIndex: Int,
    total: Int,
    questionText: String,
    options: List<String>,
    correctAnswer: String,
    selectedOption: String?,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onNext: () -> Unit
) {
    val progress = (currentIndex + 1).toFloat() / total.toFloat()
    val answered = selectedOption != null

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
            Text("Quiz · $topicTitle", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = QTextPri)
        }

        // Progress bar
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
            Surface(shape = RoundedCornerShape(6.dp), color = QChipBg) {
                Text(
                    text = "MULTIPLE CHOICE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = QBlue,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = questionText,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = QTextPri,
                lineHeight = 25.sp
            )

            Spacer(Modifier.height(24.dp))

            options.forEach { option ->
                val isSelected = option == selectedOption
                val isCorrectOption = option == correctAnswer
                val showCorrect = answered && isCorrectOption
                val showWrong = answered && isSelected && !isCorrectOption

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
                        .clickable(enabled = !answered) { onSelect(option) }
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

            Spacer(Modifier.height(12.dp))

            if (answered) {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = QBlue)
                ) {
                    Text(
                        text = if (currentIndex == total - 1) "See Results" else "Next Question",
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
// RESULT SCREEN — identical design to QuizResultScreen in QuizScreen.kt:
// gradient hero, trophy icon, percent + points, verdict pill, stat cards,
// accuracy bar, Retry / Back buttons.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GrammarQuizResultScreen(
    score: Int,
    total: Int,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    val percent = if (total == 0) 0 else ((score.toFloat() / total.toFloat()) * 100).roundToInt()
    val wrong = total - score

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QSurface)
    ) {
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

                    Text("$percent%", fontWeight = FontWeight.ExtraBold, fontSize = 56.sp, color = Color.White)
                    Text(
                        "$score out of $total points",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    Spacer(Modifier.height(14.dp))

                    val (pillColor, pillLabel) = when {
                        percent >= 80 -> QGreen to "🎉 Excellent!"
                        percent >= 50 -> QOrange to "👍 Good effort!"
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

            Text("Progress Breakdown", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = QTextPri, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GrammarResultStatCard("Correct", "$score", QGreen, QGreenBg, Modifier.weight(1f))
                GrammarResultStatCard("Missed", "$wrong", QRed, QRedBg, Modifier.weight(1f))
                GrammarResultStatCard("Score", "$percent%", QBlue, QChipBg, Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))

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
                        Text("$percent%", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = QTextPri)
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
                                .fillMaxWidth(percent / 100f)
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

            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = QBlue)
            ) {
                Icon(Icons.Outlined.Replay, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Retry Quiz", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, QBlue)
            ) {
                Text("Back to Topics", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = QBlue)
            }
        }
    }
}

@Composable
private fun GrammarResultStatCard(
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = color)
            Text(label, fontSize = 12.sp, color = color.copy(alpha = 0.75f), fontWeight = FontWeight.Medium)
        }
    }
}