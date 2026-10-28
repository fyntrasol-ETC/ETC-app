package com.evolvingtheconversation.app.dailychallenge

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evolvingtheconversation.app.data.CourseCatalog
import com.evolvingtheconversation.viewmodel.EnrollmentSummary
import kotlinx.coroutines.delay
import kotlin.random.Random
import java.util.Calendar
import com.evolvingtheconversation.app.ui.components.BackButton
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme

private val CardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White

private val NextChallengeBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2438) else Color(0xFFEDEBFB)
private fun DailyChallengeType.icon(): ImageVector = when (this) {
    DailyChallengeType.WATCH_LESSON -> Icons.Filled.PlayCircleFilled
    DailyChallengeType.GRAMMAR_QUIZ -> Icons.Outlined.LibraryBooks
    DailyChallengeType.GENERAL_QUIZ -> Icons.Filled.Quiz
    DailyChallengeType.ESSAY_WRITING -> Icons.Filled.Edit
    DailyChallengeType.AI_SPEAKING -> Icons.Filled.Mic
}

/** Finds (courseId, lessonId) of the first course the user has started but not finished. */
fun findActiveCourseAndLesson(enrollments: Map<String, EnrollmentSummary>): Pair<String, String>? {
    for (course in CourseCatalog.courses) {
        val entry = enrollments[course.id] ?: continue
        val nextLesson = course.lessons.firstOrNull { it.id !in entry.completedLessonIds } ?: continue
        return course.id to nextLesson.id
    }
    return null
}

/** Milliseconds remaining until local midnight — when the next challenge unlocks. */
private fun millisUntilNextChallenge(): Long {
    val now = Calendar.getInstance()
    val midnight = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return (midnight.timeInMillis - now.timeInMillis).coerceAtLeast(0)
}

@Composable
fun DailyChallengeScreen(
    enrollments: Map<String, EnrollmentSummary>,
    onStartChallenge: (DailyChallengeType) -> Unit,
    onBack: () -> Unit
) {
    val challenge by DailyChallengeService.challenge.collectAsStateWithLifecycle()
    val activeLesson = remember(enrollments) { findActiveCourseAndLesson(enrollments) }

    LaunchedEffect(Unit) { DailyChallengeService.listen() }

    Column(Modifier.fillMaxSize()) {
        // ── Header (matches the rest of your screens) ───────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(onBack = onBack)
            Spacer(Modifier.width(14.dp))
            Text("Daily Challenge", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Box(Modifier.fillMaxWidth().weight(1f)) {
            when (val current = challenge) {
                null -> Column(
                    Modifier.fillMaxWidth().padding(top = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) { CircularProgressIndicator() }

                else -> if (current.isCompleted) {
                    CompletedCard(onBack, Modifier.align(Alignment.TopCenter))
                } else {
                    val blockedNoCourse = current.type == DailyChallengeType.WATCH_LESSON && activeLesson == null
                    ChallengeCard(
                        challenge = current,
                        blockedNoCourse = blockedNoCourse,
                        onStart = { onStartChallenge(current.type) },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChallengeCard(
    challenge: DailyChallenge,
    blockedNoCourse: Boolean,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val type = challenge.type
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().background(CardWhite, RoundedCornerShape(20.dp)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(88.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(type.icon(), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
            }
            Text("TODAY'S CHALLENGE", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = MaterialTheme.colorScheme.primary)
            Text(type.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(type.subtitle, fontSize = 15.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Bolt, null, tint = Color(0xFFFF9500), modifier = Modifier.size(16.dp))
                Text("+${type.xpReward} XP on completion", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Button(onClick = onStart, enabled = !blockedNoCourse, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Do it now")
        }

        if (blockedNoCourse) {
            Text(
                "You're not enrolled in a course yet — enroll first to unlock this challenge.",
                fontSize = 13.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompletedCard(onBack: () -> Unit, modifier: Modifier = Modifier) {
    // Ticks every second so the countdown stays live while this screen is open.
    var remainingMs by remember { mutableStateOf(millisUntilNextChallenge()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            remainingMs = millisUntilNextChallenge()
        }
    }
    val totalSeconds = remainingMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val countdownText = "%02d:%02d:%02d".format(hours, minutes, seconds)

    Column(
        modifier.fillMaxWidth().padding(top = 60.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            Modifier.size(100.dp).background(Color(0xFF2DBD7E).copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Verified, null, tint = Color(0xFF2DBD7E), modifier = Modifier.size(48.dp))
        }

        Text("🎉 Congrats!", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Text(
            "You've completed today's challenge. Come back tomorrow for a new one!",
            fontSize = 15.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NextChallengeBg, RoundedCornerShape(14.dp))
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "NEXT CHALLENGE IN",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                countdownText,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF5B4FE5)
            )
        }

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Back to Home")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Confetti + XP popup — drop this in HomeScreen (see patch notes).
// ─────────────────────────────────────────────────────────────────────────

private data class ConfettiPiece(
    val startX: Float,       // 0f..1f fraction of width
    val delay: Float,        // 0f..0.3f fraction of animation to wait before falling
    val color: Color,
    val size: Float,         // px
    val drift: Float,        // horizontal sway amplitude
    val spinSpeed: Float
)

private val confettiColors = listOf(
    Color(0xFFFF6B6B), Color(0xFFFFD93D), Color(0xFF6BCB77),
    Color(0xFF4D96FF), Color(0xFFB983FF), Color(0xFFFF9F45)
)

/**
 * Call from HomeScreen: pass `xpAmount` from DailyChallengeService.celebrationXp.
 * Shows for ~2.5s then calls onFinished() — caller should null out the state there.
 */
@Composable
fun XpCelebrationOverlay(xpAmount: Int, onFinished: () -> Unit) {
    val pieces = remember {
        List(70) {
            ConfettiPiece(
                startX = Random.nextFloat(),
                delay = Random.nextFloat() * 0.3f,
                color = confettiColors[Random.nextInt(confettiColors.size)],
                size = Random.nextFloat() * 6f + 5f,
                drift = Random.nextFloat() * 40f - 20f,
                spinSpeed = Random.nextFloat() * 6f + 2f
            )
        }
    }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(2500, easing = LinearEasing))
        delay(150)
        onFinished()
    }

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            pieces.forEach { p ->
                val local = ((progress.value - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
                if (local <= 0f) return@forEach
                val fallY = local * (h + 40f) - 20f
                val sway = kotlin.math.sin(local * p.spinSpeed * Math.PI).toFloat() * p.drift
                val alpha = if (local > 0.85f) (1f - local) / 0.15f else 1f
                drawCircle(
                    color = p.color.copy(alpha = alpha.coerceIn(0f, 1f)),
                    radius = p.size,
                    center = Offset(p.startX * w + sway, fallY)
                )
            }
        }

        Card(
            modifier = Modifier.align(Alignment.Center).padding(32.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                Modifier.padding(horizontal = 32.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🎉", fontSize = 40.sp)
                Text("Congratulations!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "You've earned +$xpAmount XP",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}