package com.evolvingtheconversation.app.ui.screens.core_tabs

import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.VideoCameraFront
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.evolvingtheconversation.app.data.CourseCatalog
import com.evolvingtheconversation.viewmodel.EnrollmentSummary
import com.evolvingtheconversation.viewmodel.LiveClass
import com.evolvingtheconversation.viewmodel.fetchAllLiveClasses
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evolvingtheconversation.app.dailychallenge.DailyChallengeService
import com.evolvingtheconversation.app.dailychallenge.XpCelebrationOverlay
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.getValue
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme
import com.evolvingtheconversation.app.R
private val BlueStart     = Color(0xFF1E5EF3)
private val BlueEnd       = Color(0xFF1E3A8A)
private val GreenAccent   = Color(0xFF0CBD85)
private val GreenBg       = Color(0xFF0DB87E)
private val OrangeStar    = Color(0xFFFFA800)
private val RedDate       = Color(0xFFE53935)
private val BluePill: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1C2A4A) else Color(0xFFE8EFFE)
private val BluePillText  = Color(0xFF4F8EF7)
private val SurfaceBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFF4F6FB)
private val CardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val TextPrimary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)
private val TextSecondary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)
private val DarkFooter    = Color(0xFF0F1B3C)   // already dark — unchanged
private val LiveGreenDot  = Color(0xFF12D18E)
// Must match LIVE_WINDOW_MILLIS in LiveClassesScreen.kt (private there, so it
// can't be shared across files) — keep both in sync if this ever changes.
private const val LIVE_WINDOW_MILLIS = 5 * 60 * 1000L

/**
 * What the "Continue Learning" card needs to render — derived from the real
 * catalog + this user's real progress. Null when the user hasn't started
 * (or has finished) every course.
 */
private data class FeaturedCourseProgress(
    val courseId: String,
    val subtitle: String,
    val progress: Float
)

/**
 * Home needs two things derived from all of the user's enrollments:
 *  - how many courses they've fully finished (any number, shown as "N course(s) completed")
 *  - which course to feature in "Continue learning" — the most recently
 *    studied course that isn't finished yet (or null if none is in progress)
 */
private data class HomeProgressSummary(
    val completedCourseCount: Int,
    val featured: FeaturedCourseProgress?
)

private fun buildHomeProgressSummary(
    enrollments: Map<String, EnrollmentSummary>
): HomeProgressSummary {
    var completedCount = 0
    var best: Pair<Long, FeaturedCourseProgress>? = null

    CourseCatalog.courses.forEach { catalogCourse ->
        val entry = enrollments[catalogCourse.id] ?: return@forEach
        val completed = entry.completedLessonIds
        if (completed.isEmpty()) return@forEach

        val total = catalogCourse.lessons.size
        if (total == 0) return@forEach

        val fraction = completed.size.toFloat() / total.toFloat()

        if (fraction >= 1f) {
            completedCount++
            return@forEach
        }

        val nextIndex = catalogCourse.lessons.indexOfFirst { it.id !in completed }
        val lessonNumber = if (nextIndex >= 0) nextIndex + 1 else total
        val candidate = FeaturedCourseProgress(
            courseId = catalogCourse.id,
            subtitle = "${catalogCourse.title} · Lesson $lessonNumber",
            progress = fraction
        )

        if (best == null || entry.updatedAtMillis > best!!.first) {
            best = entry.updatedAtMillis to candidate
        }
    }

    return HomeProgressSummary(completedCount, best?.second)
}

private fun isSameLocalDay(millisA: Long, millisB: Long): Boolean {
    val a = Calendar.getInstance().apply { timeInMillis = millisA }
    val b = Calendar.getInstance().apply { timeInMillis = millisB }
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}

@Composable
fun HomeScreen(
    enrollments        : Map<String, EnrollmentSummary> = emptyMap(),
    onMenuClick        : () -> Unit = {},
    onSearchClick      : () -> Unit = {},
    onNotifClick       : () -> Unit = {},
    onLogout           : () -> Unit = {},
    onCourseClick      : (courseId: String) -> Unit = {},
    onBrowseCoursesClick : () -> Unit = {},
    onCompletedCoursesClick : () -> Unit = onBrowseCoursesClick,
    onGrammarClick     : () -> Unit = {},
    onVocabularyClick  : () -> Unit = {},
    onSpeakingClick    : () -> Unit = {},
    onListeningClick   : () -> Unit = {},
    onLiveClassesClick : () -> Unit = {},
    onDailyChallengeClick: () -> Unit = {},
    xp                 : Int = 0,
    streak              : Int = 0
) {
    val scrollState = rememberScrollState()
    val summary = remember(enrollments) { buildHomeProgressSummary(enrollments) }
    val featured = summary.featured
    val dailyChallenge by DailyChallengeService.challenge.collectAsStateWithLifecycle()
    val celebrationXp by DailyChallengeService.celebrationXp.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { DailyChallengeService.listen() }


    // (user, including the iOS app, reads from /liveClasses) ────────────────
    val uriHandler = LocalUriHandler.current
    var liveClasses by remember { mutableStateOf<List<LiveClass>>(emptyList()) }
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                fetchAllLiveClasses { liveClasses = it }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000)
        }
    }

    // isLive is true only while now falls within LIVE_WINDOW_MILLIS after the
    // class's start time — exactly matches LiveClassesScreen's rule. Once that
    // window passes, this naturally becomes null again and todayUpcomingClass
    // takes over, showing the next scheduled class TODAY with a fresh countdown.
    val liveClass = liveClasses
        .filter { it.dateMillis <= nowMillis && nowMillis - it.dateMillis < LIVE_WINDOW_MILLIS }
        .maxByOrNull { it.dateMillis }

    // FIX: only consider classes scheduled for TODAY when looking for the
    // next upcoming one — mirrors LiveClassesScreen's "todayClasses" filter.
    // Previously this picked the earliest future class across ALL days, so a
    // class scheduled for tomorrow (or later) would incorrectly show up here
    // even when nothing is scheduled today.
    val todayUpcomingClass = liveClasses
        .filter { isSameLocalDay(it.dateMillis, nowMillis) && it.dateMillis > nowMillis }
        .minByOrNull { it.dateMillis }

    val featuredClass = liveClass ?: todayUpcomingClass

    // Wraps the whole screen so the confetti/XP popup can float on top of everything.
    Box(Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceBg)
                .verticalScroll(scrollState)
                .padding(bottom = 72.dp)
        ) {

            // ── HEADER BAND ───────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation    = 12.dp,
                        shape        = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                        ambientColor = BlueStart.copy(alpha = 0.4f),
                        spotColor    = BlueStart.copy(alpha = 0.4f)
                    )
                    .background(
                        brush = Brush.linearGradient(listOf(BlueStart, BlueEnd)),
                        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                    )
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 56.dp)
            ) {
                Column {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        HeaderIconButton(
                            icon        = Icons.Default.Menu,
                            description = stringResource(R.string.home_menu_description),
                            onClick     = onMenuClick
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            HeaderIconButton(
                                icon        = Icons.Default.Search,
                                description = stringResource(R.string.home_search_description),
                                onClick     = onSearchClick
                            )

                            Box(
                                modifier = Modifier
                                    .size(45.dp)
                                    .shadow(
                                        elevation    = 10.dp,
                                        shape        = RoundedCornerShape(14.dp),
                                        ambientColor = Color.Black.copy(alpha = 0.45f),
                                        spotColor    = Color.Black.copy(alpha = 0.45f)
                                    )
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.White.copy(alpha = 0.32f), Color.White.copy(alpha = 0.14f))
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.verticalGradient(
                                            listOf(Color.White.copy(alpha = 0.6f), Color.Transparent)
                                        ),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onNotifClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                // FIX: badge was aligning to this OUTER 45.dp button box,
                                // not to the bell icon (which is only 23.dp and sits
                                // centered inside it) — that's why the dot drifted out
                                // near the button's border instead of sitting on the bell.
                                // Wrapping icon + badge in a Box sized to the icon itself
                                // fixes the anchor, and a small offset tucks it right onto
                                // the bell's top-right corner.
                                Box(modifier = Modifier.size(23.dp)) {
                                    Icon(
                                        imageVector        = Icons.Outlined.Notifications,
                                        contentDescription = stringResource(R.string.home_notifications_description),
                                        tint               = Color.White,
                                        modifier           = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .align(Alignment.TopEnd)
                                            .offset(x = 2.dp, y = (-2).dp)
                                            .background(Color(0xFFE53935), CircleShape)
                                            .border(1.2.dp, BlueStart, CircleShape)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        stringResource(R.string.home_greeting),
                        color     = Color.White.copy(alpha = 0.85f),
                        fontSize  = 19.sp,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth()
                    )
                    val currentUserName = FirebaseAuth.getInstance().currentUser?.displayName
                        ?: stringResource(R.string.home_default_name)
                    Text(
                        stringResource(R.string.home_greeting_name, currentUserName),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 33.sp,
                        color      = Color.White,
                        textAlign  = TextAlign.Center,
                        modifier   = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(22.dp))

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatPill("🔥", streak.toString(), stringResource(R.string.home_day_streak), Modifier.weight(1f))
                        StatPill("🏆", String.format(Locale.US, "%,d", xp), stringResource(R.string.home_xp_earned), Modifier.weight(1f))
                    }
                }
            }

            // ── CONTINUE LEARNING CARD (real progress) OR EMPTY STATE ────────────
            ElevatedCard(
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-30).dp),
                shape     = RoundedCornerShape(20.dp),
                colors    = CardDefaults.elevatedCardColors(containerColor = CardWhite),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = 10.dp,
                    pressedElevation = 14.dp
                )
            ) {
                Column {
                    // "N course(s) completed" — only shown once the user has finished at least one.
                    if (summary.completedCourseCount > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCompletedCoursesClick() }
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector        = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint               = GreenAccent,
                                modifier           = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text       = pluralStringResource(
                                    R.plurals.home_courses_completed,
                                    summary.completedCourseCount,
                                    summary.completedCourseCount
                                ),
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 17.sp,
                                color      = TextPrimary,
                                modifier   = Modifier.weight(1f)
                            )
                            Text(
                                "›",
                                fontSize   = 23.sp,
                                fontWeight = FontWeight.Bold,
                                color      = TextSecondary
                            )
                        }
                        HorizontalDivider(color = Color(0xFFF0F0F5), thickness = 1.dp)
                    }

                    Row(
                        modifier          = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (featured != null) {
                            CircularProgressRing(
                                progress = featured.progress,
                                label    = "${(featured.progress * 100).toInt()}%",
                                sub      = stringResource(R.string.home_progress_ring_done)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    stringResource(R.string.home_continue_learning),
                                    color         = GreenAccent,
                                    fontWeight    = FontWeight.Bold,
                                    fontSize      = 14.sp,
                                    letterSpacing = 4.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    featured.subtitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 19.sp,
                                    color      = TextPrimary,
                                    lineHeight = 25.sp
                                )
                                Spacer(Modifier.height(12.dp))
                                Premium3DButton(
                                    text    = stringResource(R.string.home_resume_button),
                                    onClick = { onCourseClick(featured.courseId) },
                                    gradientColors = listOf(Color(0xFF4F8EF7), BlueStart, BlueEnd)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color(0xFFF3F4F6), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector        = Icons.Outlined.MenuBook,
                                    contentDescription = null,
                                    tint               = TextSecondary,
                                    modifier           = Modifier.size(28.dp)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    stringResource(R.string.home_no_course_in_progress),
                                    color         = TextSecondary,
                                    fontWeight    = FontWeight.Bold,
                                    fontSize      = 14.sp,
                                    letterSpacing = 4.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    stringResource(R.string.home_enroll_prompt),
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 19.sp,
                                    color      = TextPrimary,
                                    lineHeight = 25.sp
                                )
                                Spacer(Modifier.height(12.dp))
                                Premium3DButton(
                                    text    = stringResource(R.string.home_enroll_now_button),
                                    onClick = onBrowseCoursesClick,
                                    gradientColors = listOf(Color(0xFF4F8EF7), BlueStart, BlueEnd)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height((-10).dp))

            // ── DAILY CHALLENGE ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .shadow(
                        elevation    = 10.dp,
                        shape        = RoundedCornerShape(16.dp),
                        ambientColor = GreenBg.copy(alpha = 0.5f),
                        spotColor    = GreenBg.copy(alpha = 0.5f)
                    )
                    .background(GreenBg, RoundedCornerShape(16.dp))
                    .clickable { onDailyChallengeClick() }
            ) {
                Row(
                    modifier          = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color.White.copy(0.22f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text("🎯", fontSize = 25.sp) }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.home_daily_challenge_title),
                            fontWeight = FontWeight.Bold,
                            color      = Color.White,
                            fontSize   = 19.sp
                        )
                        Text(
                            if (dailyChallenge?.isCompleted == true)
                                stringResource(R.string.home_daily_challenge_completed)
                            else
                                stringResource(R.string.home_daily_challenge_prompt),
                            color    = Color.White.copy(0.85f),
                            fontSize = 16.sp
                        )
                    }
                    Text("›", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ── CATEGORIES ────────────────────────────────────────────────────────
            Text(
                stringResource(R.string.home_categories_title),
                fontWeight = FontWeight.Bold,
                fontSize   = 23.sp,
                color      = TextPrimary,
                modifier   = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
            )

            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CategoryChip(
                    icon    = Icons.Outlined.LibraryBooks,
                    label   = stringResource(R.string.home_category_grammar),
                    bg      = Color(0xFFEEF3FF),
                    tint    = Color(0xFF1E5EF3),
                    onClick = onGrammarClick
                )
                CategoryChip(
                    icon    = Icons.Outlined.Style,
                    label   = stringResource(R.string.home_category_vocabulary),
                    bg      = Color(0xFFEAFFF7),
                    tint    = Color(0xFF0CBD85),
                    onClick = onVocabularyClick
                )
                CategoryChip(
                    icon    = Icons.Outlined.Mic,
                    label   = stringResource(R.string.home_category_speaking),
                    bg      = Color(0xFFFFF4E5),
                    tint    = Color(0xFFFFA800),
                    onClick = onSpeakingClick
                )
                CategoryChip(
                    icon    = Icons.Outlined.Headphones,
                    label   = stringResource(R.string.home_category_listening),
                    bg      = Color(0xFFF3EEFF),
                    tint    = Color(0xFF7C3AED),
                    onClick = onListeningClick
                )
            }

            // ── LIVE CLASS (real Firestore data, synced with iOS) ─────────────────
            Text(
                stringResource(R.string.home_live_class_title),
                fontWeight = FontWeight.Bold,
                fontSize   = 23.sp,
                color      = TextPrimary,
                modifier   = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp)
            )

            if (featuredClass != null) {
                LiveClassHomeCard(
                    cls       = featuredClass,
                    isLive    = liveClass != null,
                    nowMillis = nowMillis,
                    onJoin    = { uriHandler.openUri(featuredClass.meetingLink) },
                    onOpen    = onLiveClassesClick
                )
            } else {
                ElevatedCard(
                    modifier  = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 22.dp)
                        .clickable { onLiveClassesClick() },
                    shape     = RoundedCornerShape(16.dp),
                    colors    = CardDefaults.elevatedCardColors(containerColor = CardWhite),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp, pressedElevation = 10.dp)
                ) {
                    Row(
                        modifier          = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.home_no_live_classes),
                            fontSize = 15.sp, color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Text("›", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    }
                }
            }

            // ── QUOTE FOOTER ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(
                        8.dp, RoundedCornerShape(16.dp),
                        ambientColor = DarkFooter.copy(alpha = 0.3f),
                        spotColor    = DarkFooter.copy(alpha = 0.3f)
                    )
                    .background(DarkFooter, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier          = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "✦",
                        color    = GreenAccent,
                        fontSize = 23.sp,
                        modifier = Modifier.padding(end = 12.dp, top = 2.dp)
                    )
                    Text(
                        stringResource(R.string.home_footer_quote),
                        color      = Color.White,
                        fontSize   = 17.sp,
                        lineHeight = 25.sp,
                        fontStyle  = FontStyle.Italic
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        // ── XP CELEBRATION POPUP (confetti) ───────────────────────────────────
        // Floats above everything else in the Box. Shows automatically whenever
        // DailyChallengeService.markComplete() fires from anywhere in the app.
        celebrationXp?.let { xp ->
            XpCelebrationOverlay(
                xpAmount = xp,
                onFinished = { DailyChallengeService.consumeCelebration() }
            )
        }
    }
}

// ─── Live class card — real data, synced via Firestore ────────────────────────
// Mirrors LiveClassesScreen's hero: red pulsing "STARTS IN" + countdown before
// the class starts, green "LIVE NOW" once it's within LIVE_WINDOW_MILLIS.
// Tapping the Join button opens the meeting link directly; tapping anywhere
// else on the card opens the full Live Classes screen. Join is only enabled
// while live — see JoinButton3D below.
@Composable
private fun LiveClassHomeCard(
    cls: LiveClass,
    isLive: Boolean,
    nowMillis: Long,
    onJoin: () -> Unit,
    onOpen: () -> Unit
) {
    val classDate = remember(cls.dateMillis) { Date(cls.dateMillis) }
    val isToday = remember(cls.dateMillis, nowMillis) { isSameLocalDay(cls.dateMillis, nowMillis) }
    val dayLabel = if (isToday) stringResource(R.string.home_today_label)
    else SimpleDateFormat("EEE, d MMM", Locale.US).format(classDate)

    ElevatedCard(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 22.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.elevatedCardColors(containerColor = CardWhite),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp, pressedElevation = 10.dp)
    ) {
        Column(modifier = Modifier.clickable { onOpen() }) {
            // ── Status row: dot + LIVE NOW/STARTS IN pinned left, countdown pinned right ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(if (isLive) LiveGreenDot else RedDate, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isLive) stringResource(R.string.home_live_now) else stringResource(R.string.home_starts_in),
                        color = if (isLive) LiveGreenDot else RedDate,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp
                    )
                }

                if (!isLive) {
                    val remaining = (cls.dateMillis - nowMillis).coerceAtLeast(0)
                    val hrs = remaining / 3_600_000
                    val mins = (remaining / 60_000) % 60
                    val secs = (remaining / 1000) % 60
                    Text(
                        String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs),
                        color = RedDate, fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                }
            }

            // ── Body: date badge, title/subtitle, premium 3D Join button ────
            Row(
                modifier          = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier
                        .background(Color(0xFFFFE8E8), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        SimpleDateFormat("d", Locale.US).format(classDate),
                        fontWeight = FontWeight.ExtraBold, color = RedDate, fontSize = 18.sp
                    )
                    Text(
                        SimpleDateFormat("MMM", Locale.US).format(classDate).uppercase(Locale.US),
                        color = RedDate, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(cls.title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        stringResource(
                            R.string.home_live_class_subtitle,
                            dayLabel,
                            SimpleDateFormat("h:mm a", Locale.US).format(classDate),
                            cls.teacherName
                        ),
                        fontSize = 13.sp, color = TextSecondary
                    )
                }
                Spacer(Modifier.width(8.dp))
                JoinButton3D(isLive = isLive, onJoin = onJoin)
            }
        }
    }
}

// ─── Premium 3D Join button — vibrant gradient + glow when live, muted and
// inert when not. Replaces the old flat white/light-blue pill. ────────────
@Composable
private fun JoinButton3D(
    isLive: Boolean,
    onJoin: () -> Unit
) {
    val gradientColors = if (isLive)
        listOf(Color(0xFF23E6A6), LiveGreenDot, Color(0xFF0A9E70))
    else
        listOf(Color(0xFFE4E7EE), Color(0xFFCDD2DC))

    val textColor = if (isLive) Color.White else Color(0xFF9AA1AC)
    val glowColor = if (isLive) LiveGreenDot else Color.Black

    Box(
        modifier = Modifier
            .shadow(
                elevation    = if (isLive) 12.dp else 2.dp,
                shape        = RoundedCornerShape(50.dp),
                ambientColor = glowColor.copy(alpha = if (isLive) 0.45f else 0.10f),
                spotColor    = glowColor.copy(alpha = if (isLive) 0.55f else 0.10f)
            )
            .clip(RoundedCornerShape(50.dp))
            .background(Brush.verticalGradient(gradientColors))
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = if (isLive) 0.6f else 0.4f), Color.Transparent)
                ),
                shape = RoundedCornerShape(50.dp)
            )
            .clickable(enabled = isLive) { onJoin() }
            .padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector        = Icons.Filled.VideoCameraFront,
                contentDescription = null,
                tint               = textColor,
                modifier           = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.home_join_button), color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

// ─── Premium 3D button — deep shadow, gradient fill, glossy top edge, big bold text ──
@Composable
private fun Premium3DButton(
    text            : String,
    onClick         : () -> Unit,
    modifier        : Modifier = Modifier,
    gradientColors  : List<Color> = listOf(Color(0xFF4F8EF7), BlueStart, BlueEnd),
    textColor       : Color = Color.White,
    fontSize        : androidx.compose.ui.unit.TextUnit = 18.sp
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .shadow(
                elevation    = if (pressed) 4.dp else 14.dp,
                shape        = RoundedCornerShape(16.dp),
                ambientColor = gradientColors.last().copy(alpha = 0.55f),
                spotColor    = gradientColors.last().copy(alpha = 0.65f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(gradientColors))
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.55f), Color.Transparent)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null
            ) {
                onClick()
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown(requireUnconsumed = false)
                        pressed = true
                        waitForUpOrCancellation()
                        pressed = false
                    }
                }
            }
            .padding(horizontal = 26.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = text,
            color      = textColor,
            fontSize   = fontSize,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

// ─── Header icon button (menu / search / bell) ────────────────────────────────
@Composable
private fun HeaderIconButton(
    icon        : ImageVector,
    description : String,
    onClick     : () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size(45.dp)
            .shadow(
                elevation    = 10.dp,
                shape        = RoundedCornerShape(14.dp),
                ambientColor = Color.Black.copy(alpha = 0.45f),
                spotColor    = Color.Black.copy(alpha = 0.45f)
            )
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.32f), Color.White.copy(alpha = 0.14f))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.6f), Color.Transparent)
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = description,
            tint               = Color.White,
            modifier           = Modifier.size(23.dp)
        )
    }
}

// ─── Circular progress ring ───────────────────────────────────────────────────
@Composable
private fun CircularProgressRing(
    progress : Float,
    label    : String,
    sub      : String,
    modifier : Modifier = Modifier
) {
    Box(modifier = modifier.size(80.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke  = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
            val inset   = stroke.width / 2
            val arcSize = androidx.compose.ui.geometry.Size(
                size.width - stroke.width, size.height - stroke.width
            )
            drawArc(
                color      = Color(0xFFE0F2EE),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter  = false,
                topLeft    = androidx.compose.ui.geometry.Offset(inset, inset),
                size       = arcSize,
                style      = stroke
            )
            drawArc(
                color      = GreenAccent,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter  = false,
                topLeft    = androidx.compose.ui.geometry.Offset(inset, inset),
                size       = arcSize,
                style      = stroke
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = GreenAccent)
            Text(sub,   fontSize = 15.sp, color = TextSecondary)
        }
    }
}

// ─── Stat pill ────────────────────────────────────────────────────────────────
@Composable
private fun StatPill(
    emoji    : String,
    value    : String,
    label    : String,
    modifier : Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(emoji, fontSize = 27.sp)
        Column {
            Text(value, fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 21.sp)
            Text(label, fontSize = 15.sp, color = Color.White.copy(alpha = 0.80f))
        }
    }
}

// ─── Category chip — elevated 3D card style ───────────────────────────────────
@Composable
private fun CategoryChip(
    icon    : ImageVector,
    label   : String,
    bg      : Color,
    tint    : Color,
    onClick : () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier
            .clickable { onClick() }
            .padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(
                    elevation    = 10.dp,
                    shape        = RoundedCornerShape(20.dp),
                    ambientColor = tint.copy(alpha = 0.25f),
                    spotColor    = tint.copy(alpha = 0.45f)
                )
                .background(CardWhite, RoundedCornerShape(20.dp))
                .padding(4.dp)
                .background(bg, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = tint,
                modifier           = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            fontSize   = 15.sp,
            color      = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign  = TextAlign.Center
        )
    }
}