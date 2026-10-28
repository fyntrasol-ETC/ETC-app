package com.evolvingtheconversation.app.ui.screens.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evolvingtheconversation.app.ui.components.BackButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.getValue
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme
import com.evolvingtheconversation.app.R

// ── Palette — matches the rest of the app (HomeScreen's gradient header etc.) ─
private val SurfaceBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFF4F6FB)
private val CardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val TextPrimary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)
private val TextSecondary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)
private val BlueStart     = Color(0xFF1E5EF3)
private val BlueEnd       = Color(0xFF1E3A8A)
private val BlueBar       = Color(0xFF1E5EF3)
private val GreenBar      = Color(0xFF0CBD85)
private val OrangeBar     = Color(0xFFFFA800)
private val PurpleBar     = Color(0xFF7C3AED)
private val BorderGray: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color(0xFFE5E7EB)

private val ToggleTrackBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF232326) else Color(0xFFEEF0F5)

private const val SKILL_POINTS_FOR_100_PERCENT = 300.0

// label changed from raw String to labelResId — this enum is defined
// OUTSIDE a Composable scope, so it can't call stringResource() itself.
// Resolved wherever it's rendered inside a Composable below.
private enum class ProgressPeriod(val labelResId: Int) {
    WEEK(R.string.progress_period_week),
    MONTH(R.string.progress_period_month),
    YEAR(R.string.progress_period_year)
}

private data class ActivityPoint(val label: String, val xp: Int)
private data class OverviewData(
    val xp: Int = 0,
    val streak: Int = 0,
    val totalHours: Double = 0.0,
    val totalLessonsCompleted: Int = 0,
    val skillBreakdown: Map<String, Int> = emptyMap()
)

private data class SkillBreakdownStats(
    val confidenceScore: Int = 0,
    val correctAnswers: Int = 0,
    val wrongAnswers: Int = 0,
    val totalDictations: Int = 0,
    val grammarQuizzesSolved: Int = 0,
    val wordsLearned: Int = 0,
    val savedWords: Int = 0
) {
    val isEmpty: Boolean
        get() = confidenceScore == 0 && correctAnswers == 0 && wrongAnswers == 0 &&
                grammarQuizzesSolved == 0
}

private fun dateKeyFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = java.util.TimeZone.getDefault()
}
private fun monthKeyFormat() = SimpleDateFormat("yyyy-MM", Locale.US).apply {
    timeZone = java.util.TimeZone.getDefault()
}
private fun dayLetter(date: Date) = SimpleDateFormat("EEEEE", Locale.US).format(date)

private fun fetchOverview(uid: String, onResult: (OverviewData) -> Unit) {
    FirebaseFirestore.getInstance().collection("users").document(uid).get()
        .addOnSuccessListener { doc ->
            val stats = doc.get("stats") as? Map<*, *>
            val skill = doc.get("skillBreakdown") as? Map<*, *>
            onResult(
                OverviewData(
                    xp = (doc.getLong("xp") ?: 0L).toInt(),
                    streak = (doc.getLong("streak") ?: 0L).toInt(),
                    totalHours = (stats?.get("totalHours") as? Number)?.toDouble() ?: 0.0,
                    totalLessonsCompleted = (stats?.get("totalLessonsCompleted") as? Number)?.toInt() ?: 0,
                    skillBreakdown = mapOf(
                        "speaking" to ((skill?.get("speaking") as? Number)?.toInt() ?: 0),
                        "listening" to ((skill?.get("listening") as? Number)?.toInt() ?: 0),
                        "grammar" to ((skill?.get("grammar") as? Number)?.toInt() ?: 0),
                        "vocabulary" to ((skill?.get("vocabulary") as? Number)?.toInt() ?: 0)
                    )
                )
            )
        }
        .addOnFailureListener { onResult(OverviewData()) }
}

private fun fetchSkillBreakdown(uid: String, onResult: (SkillBreakdownStats) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val today = dateKeyFormat().format(Date())
    val userRef = db.collection("users").document(uid)

    var dailyStats: SkillBreakdownStats? = null
    var savedCount: Int? = null

    fun maybeEmit() {
        val daily = dailyStats ?: return
        val saved = savedCount ?: return
        onResult(daily.copy(savedWords = saved))
    }

    userRef.collection("skillStats").document(today).get()
        .addOnSuccessListener { doc ->
            val correct = (doc.getLong("dictationCorrect") ?: 0L).toInt()
            val wrong = (doc.getLong("dictationWrong") ?: 0L).toInt()
            dailyStats = SkillBreakdownStats(
                confidenceScore = (doc.getLong("speakingConfidence") ?: 0L).toInt(),
                correctAnswers = correct,
                wrongAnswers = wrong,
                totalDictations = correct + wrong,
                grammarQuizzesSolved = (doc.getLong("grammarQuizzesSolved") ?: 0L).toInt(),
                wordsLearned = (doc.getLong("wordsLearned") ?: 0L).toInt()
            )
            maybeEmit()
        }
        .addOnFailureListener { dailyStats = SkillBreakdownStats(); maybeEmit() }

    userRef.collection("savedVocabulary").get()
        .addOnSuccessListener { snapshot -> savedCount = snapshot.size(); maybeEmit() }
        .addOnFailureListener { savedCount = 0; maybeEmit() }
}

private fun fetchWeekActivity(uid: String, onResult: (List<ActivityPoint>) -> Unit) {
    val fmt = dateKeyFormat()
    val cal = Calendar.getInstance()
    val dates = (0..6).map { offset ->
        val c = cal.clone() as Calendar
        c.add(Calendar.DAY_OF_YEAR, -offset)
        c.time
    }.reversed()
    val keys = dates.map { fmt.format(it) }

    FirebaseFirestore.getInstance().collection("users").document(uid)
        .collection("activityLog")
        .whereGreaterThanOrEqualTo(FieldPath.documentId(), keys.first())
        .get()
        .addOnSuccessListener { snapshot ->
            val byKey = snapshot.documents.associateBy { it.id }
            onResult(
                dates.mapIndexed { i, date ->
                    val xp = (byKey[keys[i]]?.getLong("xpEarned") ?: 0L).toInt()
                    ActivityPoint(dayLetter(date), xp)
                }
            )
        }
        .addOnFailureListener { onResult(emptyList()) }
}

private fun fetchMonthActivity(uid: String, onResult: (List<ActivityPoint>) -> Unit) {
    val fmt = dateKeyFormat()
    val cal = Calendar.getInstance()
    val dates = (0..27).map { offset ->
        val c = cal.clone() as Calendar
        c.add(Calendar.DAY_OF_YEAR, -offset)
        c.time
    }.reversed()
    val keys = dates.map { fmt.format(it) }

    FirebaseFirestore.getInstance().collection("users").document(uid)
        .collection("activityLog")
        .whereGreaterThanOrEqualTo(FieldPath.documentId(), keys.first())
        .get()
        .addOnSuccessListener { snapshot ->
            val byKey = snapshot.documents.associateBy { it.id }
            val weekly = (0..3).map { weekIndex ->
                val weekKeys = keys.subList(weekIndex * 7, weekIndex * 7 + 7)
                val xpSum = weekKeys.sumOf { (byKey[it]?.getLong("xpEarned") ?: 0L).toInt() }
                // "Wk N" label — plain data-layer function, not a Composable,
                // so it can't call stringResource(). Left as-is; see note below.
                ActivityPoint("Wk ${weekIndex + 1}", xpSum)
            }
            onResult(weekly)
        }
        .addOnFailureListener { onResult(emptyList()) }
}

private fun fetchYearActivity(uid: String, onResult: (List<ActivityPoint>) -> Unit) {
    val fmt = monthKeyFormat()
    val labelFmt = SimpleDateFormat("MMM", Locale.US)
    val cal = Calendar.getInstance()
    val months = (0..11).map { offset ->
        val c = cal.clone() as Calendar
        c.add(Calendar.MONTH, -offset)
        c.time
    }.reversed()
    val keys = months.map { fmt.format(it) }

    FirebaseFirestore.getInstance().collection("users").document(uid)
        .collection("monthlyStats")
        .whereGreaterThanOrEqualTo(FieldPath.documentId(), keys.first())
        .get()
        .addOnSuccessListener { snapshot ->
            val byKey = snapshot.documents.associateBy { it.id }
            onResult(
                months.mapIndexed { i, date ->
                    val xp = (byKey[keys[i]]?.getLong("xpEarned") ?: 0L).toInt()
                    ActivityPoint(labelFmt.format(date), xp)
                }
            )
        }
        .addOnFailureListener { onResult(emptyList()) }
}

private fun Modifier.premiumLift(
    shape: RoundedCornerShape,
    elevation: androidx.compose.ui.unit.Dp = 14.dp,
    tint: Color = Color.Black
): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = tint.copy(alpha = 0.18f),
    spotColor = tint.copy(alpha = 0.28f)
)

@Composable
fun MyProgressScreen(onBack: () -> Unit = {}) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    var overview by remember { mutableStateOf<OverviewData?>(null) }
    var skillStats by remember { mutableStateOf(SkillBreakdownStats()) }
    var period by remember { mutableStateOf(ProgressPeriod.WEEK) }
    var points by remember { mutableStateOf<List<ActivityPoint>>(emptyList()) }
    var chartLoading by remember { mutableStateOf(true) }

    LaunchedEffect(uid) {
        if (uid != null) {
            fetchOverview(uid) { overview = it }
            fetchSkillBreakdown(uid) { skillStats = it }
        }
    }

    LaunchedEffect(uid, period) {
        if (uid == null) return@LaunchedEffect
        chartLoading = true
        val callback: (List<ActivityPoint>) -> Unit = { points = it; chartLoading = false }
        when (period) {
            ProgressPeriod.WEEK -> fetchWeekActivity(uid, callback)
            ProgressPeriod.MONTH -> fetchMonthActivity(uid, callback)
            ProgressPeriod.YEAR -> fetchYearActivity(uid, callback)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp)
    ) {

        // ── GRADIENT HEADER BAND ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                    ambientColor = BlueStart.copy(alpha = 0.45f),
                    spotColor = BlueStart.copy(alpha = 0.45f)
                )
                .background(
                    brush = Brush.linearGradient(listOf(BlueStart, BlueEnd)),
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                )
                .statusBarsPadding()
                .padding(start = 12.dp, end = 20.dp, top = 12.dp, bottom = 64.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BackButton(onBack = onBack)
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.progress_title),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    color = Color.White
                )
            }
        }

        if (uid == null) {
            Text(
                stringResource(R.string.progress_sign_in_prompt),
                fontSize = 16.sp, color = TextSecondary,
                modifier = Modifier.padding(start = 20.dp, top = 24.dp, end = 20.dp)
            )
            return@Column
        }

        val ov = overview

        // ── STATS CARD (glass, overlapping the header like HomeScreen does) ─
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .offset(y = (-40).dp)
                .premiumLift(RoundedCornerShape(24.dp), elevation = 18.dp, tint = BlueStart),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(Modifier.padding(vertical = 26.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCell(formatHours(ov?.totalHours ?: 0.0), stringResource(R.string.progress_stat_hours))
                    Divider(modifier = Modifier.height(52.dp).width(1.dp), color = BorderGray)
                    StatCell("${ov?.totalLessonsCompleted ?: 0}", stringResource(R.string.progress_stat_lessons))
                    Divider(modifier = Modifier.height(52.dp).width(1.dp), color = BorderGray)
                    StatCell("${ov?.xp ?: 0}", stringResource(R.string.progress_stat_xp))
                }

                Spacer(Modifier.height(18.dp))

                // Streak pill — gradient, glossy, like the rest of the app's badges
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(50.dp),
                            ambientColor = OrangeBar.copy(alpha = 0.4f),
                            spotColor = OrangeBar.copy(alpha = 0.5f)
                        )
                        .clip(RoundedCornerShape(50.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFFB238), OrangeBar)))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        stringResource(R.string.progress_streak_label, ov?.streak ?: 0),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // ── PERIOD TOGGLE + CHART ────────────────────────────────────────
        Column(modifier = Modifier.offset(y = (-16).dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.progress_activity_title), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = TextPrimary)

                Row(
                    modifier = Modifier
                        .premiumLift(RoundedCornerShape(50.dp), elevation = 6.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(ToggleTrackBg)
                        .padding(4.dp)
                ) {
                    ProgressPeriod.entries.forEach { p ->
                        val selected = period == p
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .then(
                                    if (selected) Modifier
                                        .shadow(6.dp, RoundedCornerShape(50.dp), spotColor = BlueStart.copy(alpha = 0.5f))
                                        .background(Brush.verticalGradient(listOf(Color(0xFF4F8EF7), BlueStart, BlueEnd)))
                                    else Modifier
                                )
                                .clickable { period = p }
                                .padding(horizontal = 16.dp, vertical = 9.dp)
                        ) {
                            Text(
                                stringResource(p.labelResId),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .premiumLift(RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 20.dp)
                        .height(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        chartLoading -> CircularProgressIndicator(color = BlueStart, modifier = Modifier.size(26.dp))
                        points.all { it.xp == 0 } -> Text(
                            // Full sentence per period instead of label.lowercase()
                            // concatenation — Spanish grammar doesn't map cleanly
                            // onto that trick.
                            when (period) {
                                ProgressPeriod.WEEK -> stringResource(R.string.progress_no_activity_week)
                                ProgressPeriod.MONTH -> stringResource(R.string.progress_no_activity_month)
                                ProgressPeriod.YEAR -> stringResource(R.string.progress_no_activity_year)
                            },
                            fontSize = 14.sp, color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        else -> ActivityBarChart(points)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── SKILL BREAKDOWN ───────────────────────────────────────────
            Text(
                stringResource(R.string.progress_skill_breakdown_title), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(14.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .premiumLift(RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    // Speaking — AI confidence score (0-100, not a percentage)
                    SkillBreakdownHeader(
                        icon = Icons.Filled.RecordVoiceOver,
                        tint = BlueBar,
                        title = stringResource(R.string.progress_skill_speaking_title),
                        subtitle = stringResource(R.string.progress_skill_speaking_subtitle)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        StatCell("${skillStats.confidenceScore}", stringResource(R.string.progress_stat_confidence_score))
                    }

                    HorizontalDivider(color = BorderGray, thickness = 1.dp)

                    // Listening — today's dictation results
                    SkillBreakdownHeader(
                        icon = Icons.Filled.Headphones,
                        tint = GreenBar,
                        title = stringResource(R.string.progress_skill_listening_title),
                        subtitle = stringResource(R.string.progress_skill_listening_subtitle)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatCell("${skillStats.correctAnswers}", stringResource(R.string.progress_stat_correct))
                        StatCell("${skillStats.wrongAnswers}", stringResource(R.string.progress_stat_wrong))
                        StatCell("${skillStats.totalDictations}", stringResource(R.string.progress_stat_total))
                    }

                    HorizontalDivider(color = BorderGray, thickness = 1.dp)

                    // Grammar — quizzes completed
                    SkillBreakdownHeader(
                        icon = Icons.Filled.MenuBook,
                        tint = OrangeBar,
                        title = stringResource(R.string.progress_skill_grammar_title),
                        subtitle = stringResource(R.string.progress_skill_grammar_subtitle)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        StatCell("${skillStats.grammarQuizzesSolved}", stringResource(R.string.progress_stat_quizzes_solved))
                    }

                    if (skillStats.isEmpty) {
                        Text(
                            stringResource(R.string.progress_stats_empty_message),
                            fontSize = 13.sp, color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillBreakdownHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(subtitle, fontSize = 12.sp, color = TextSecondary)
        }
    }
}

private fun formatHours(hours: Double): String =
    if (hours >= 10) hours.toInt().toString() else String.format(Locale.US, "%.1f", hours)

@Composable
private fun ActivityBarChart(points: List<ActivityPoint>) {
    val maxXp = (points.maxOfOrNull { it.xp } ?: 1).coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        points.forEach { point ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                val fraction = point.xp.toFloat() / maxXp.toFloat()
                Box(modifier = Modifier.width(20.dp).height(84.dp), contentAlignment = Alignment.BottomCenter) {
                    if (point.xp > 0) {
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height((fraction * 84).dp.coerceAtLeast(6.dp))
                                .shadow(3.dp, RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp), spotColor = BlueStart.copy(alpha = 0.5f))
                                .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF4F8EF7), BlueStart, BlueEnd)
                                    )
                                )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(point.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun StatCell(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, color = TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
    }
}