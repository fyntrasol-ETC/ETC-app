package com.evolvingtheconversation.app.ui.screens.core_tabs

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VideoCameraFront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.evolvingtheconversation.viewmodel.LiveClass
import com.evolvingtheconversation.viewmodel.fetchAllLiveClasses
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme
import com.evolvingtheconversation.app.R

// ── Colour tokens ─────────────────────────────────────────────────────────────
private val BlueGradientStart = Color(0xFF3366FF)
private val BlueGradientEnd   = Color(0xFF2255EE)
private val LiveRedDot        = Color(0xFFFF3B30)
private val LiveGreenDot      = Color(0xFF12D18E)
private val CardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val SurfaceBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFF4F6FB)
private val QTextPri: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)
private val TextPrimary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)
private val TextSecondary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)
private val SelectedDayBg     = Color(0xFF3366FF)
private val DayHasClassBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF232B45) else Color(0xFFEDF0FF)
private val RemindBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF232B45) else Color(0xFFEEF3FF)
private val RemindText        = Color(0xFF3366FF)

// New — the two HorizontalDivider() calls in CalendarOverlay
private val DividerColor: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color(0xFFF0F0F5)

// A class counts as "live" from its start time until this many millis after it.
// NOTE: HomeScreen.kt's live-class card duplicates this same 5-minute window
// (it can't share this private constant across files) — keep both in sync if
// this ever changes.
private const val LIVE_WINDOW_MILLIS = 5 * 60 * 1000L

private fun isSameLocalDay(millisA: Long, millisB: Long): Boolean {
    val a = Calendar.getInstance().apply { timeInMillis = millisA }
    val b = Calendar.getInstance().apply { timeInMillis = millisB }
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}

private fun startOfDay(millis: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun monthStart(millis: Long): Calendar =
    Calendar.getInstance().apply { timeInMillis = millis; set(Calendar.DAY_OF_MONTH, 1) }

private fun addMonths(cal: Calendar, delta: Int): Calendar =
    (cal.clone() as Calendar).apply { add(Calendar.MONTH, delta) }

// ── Screen ────────────────────────────────────────────────────────────────────
@Composable
fun LiveClassesScreen(
    onBack: () -> Unit = {},
    onTeacherClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current

    var allClasses by remember { mutableStateOf<List<LiveClass>?>(null) } // null = still loading
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedDayMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    // Full-month calendar overlay + the "tap a date" preview overlay drawn on top of it.
    var showCalendarOverlay by remember { mutableStateOf(false) }
    var calendarMonth by remember { mutableStateOf(monthStart(System.currentTimeMillis())) }
    var previewDayMillis by remember { mutableStateOf<Long?>(null) }

    // Refetch every time this screen becomes visible — covers first load AND
    // coming back here after adding a class in teacher mode. Any signed-in
    // user (student or teacher) can read every scheduled class here.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                fetchAllLiveClasses { allClasses = it }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // Live ticking clock so the countdown + "today" + live/not-live status stay accurate.
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000)
        }
    }

    val classes = allClasses ?: emptyList()
    val hasAnyClasses = classes.isNotEmpty()

    // The hero card only ever reflects TODAY — never a future day, even if
    // that's the only class scheduled.
    val todayClasses = classes.filter { isSameLocalDay(it.dateMillis, nowMillis) }

    val liveClass = todayClasses
        .filter { it.dateMillis <= nowMillis && nowMillis - it.dateMillis < LIVE_WINDOW_MILLIS }
        .maxByOrNull { it.dateMillis }

    // Next class today only, strictly ordered by date/time.
    val nextTodayClass = todayClasses.filter { it.dateMillis > nowMillis }.minByOrNull { it.dateMillis }

    val selectedDayClasses = classes.filter { isSameLocalDay(it.dateMillis, selectedDayMillis) }.sortedBy { it.dateMillis }
    val isViewingToday = isSameLocalDay(selectedDayMillis, nowMillis)

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceBg)
                .verticalScroll(scrollState)
                .padding(bottom = 80.dp)
        ) {

            // ── TOP BAR ───────────────────────────────────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .shadow(5.dp, RoundedCornerShape(12.dp))
                            .background(CardWhite, RoundedCornerShape(12.dp))
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Filled.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.liveclasses_back_description),
                            tint               = QTextPri,
                            modifier           = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(stringResource(R.string.liveclasses_title), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextPrimary)
                        Text(stringResource(R.string.liveclasses_subtitle), fontSize = 14.sp, color = TextSecondary)
                    }
                }

                // ── TEACHER ENTRY POINT ─────────────────────────────────
                Row(
                    modifier = Modifier
                        .shadow(4.dp, RoundedCornerShape(20.dp))
                        .background(CardWhite, RoundedCornerShape(20.dp))
                        .clickable { onTeacherClick() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.School, contentDescription = stringResource(R.string.liveclasses_teacher_icon_description), tint = BlueGradientStart, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.liveclasses_teacher_label), color = BlueGradientStart, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }

            // ── HERO CARD (live class, next class TODAY, or "no meeting today") ─
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(
                        elevation    = 18.dp,
                        shape        = RoundedCornerShape(24.dp),
                        ambientColor = BlueGradientStart.copy(0.55f),
                        spotColor    = BlueGradientStart.copy(0.55f)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(brush = Brush.linearGradient(listOf(BlueGradientStart, BlueGradientEnd)))
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                            center = Offset(0.15f, 0.0f),
                            radius = 900f
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    when {
                        liveClass != null -> {
                            // ── LIVE NOW STATE ───────────────────────────
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                PulsingDot(color = LiveGreenDot, size = 10.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.liveclasses_live_now_label), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
                            }
                            Spacer(Modifier.height(18.dp))
                            Text(liveClass.title, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.White, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(R.string.liveclasses_with_teacher_format, liveClass.teacherName), fontSize = 14.sp, color = Color.White.copy(alpha = 0.82f), textAlign = TextAlign.Center)
                            Spacer(Modifier.height(22.dp))
                            JoinPill(BlueGradientStart) { uriHandler.openUri(liveClass.meetingLink) }
                        }

                        nextTodayClass != null -> {
                            // ── COUNTDOWN STATE (today only) ─────────────
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                PulsingDot(color = LiveRedDot, size = 9.dp)
                                Spacer(Modifier.width(7.dp))
                                Text(stringResource(R.string.liveclasses_starts_in_label), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                            }
                            Spacer(Modifier.height(18.dp))

                            val remaining = (nextTodayClass.dateMillis - nowMillis).coerceAtLeast(0)
                            val hrs = remaining / 3_600_000
                            val mins = (remaining / 60_000) % 60
                            val secs = (remaining / 1000) % 60

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                CountdownBox(value = String.format(Locale.US, "%02d", hrs), label = stringResource(R.string.liveclasses_hours_label))
                                Spacer(Modifier.width(12.dp))
                                CountdownBox(value = String.format(Locale.US, "%02d", mins), label = stringResource(R.string.liveclasses_minutes_label))
                                Spacer(Modifier.width(12.dp))
                                CountdownBox(value = String.format(Locale.US, "%02d", secs), label = stringResource(R.string.liveclasses_seconds_label))
                            }

                            Spacer(Modifier.height(22.dp))
                            Text(nextTodayClass.title, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.White, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(R.string.liveclasses_with_teacher_format, nextTodayClass.teacherName), fontSize = 14.sp, color = Color.White.copy(alpha = 0.82f), textAlign = TextAlign.Center)
                            Spacer(Modifier.height(22.dp))
                            JoinPill(BlueGradientStart) { uriHandler.openUri(nextTodayClass.meetingLink) }
                        }

                        else -> {
                            // ── NO MEETING TODAY ─────────────────────────
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .shadow(6.dp, RoundedCornerShape(14.dp), spotColor = Color.Black.copy(alpha = 0.3f))
                                    .background(Color.White, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = BlueGradientStart, modifier = Modifier.size(22.dp))
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                        .background(LiveRedDot, CircleShape)
                                        .border(2.dp, Color.White, CircleShape)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.liveclasses_no_meeting_title),
                                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                stringResource(R.string.liveclasses_no_meeting_subtitle),
                                color = Color.White.copy(alpha = 0.82f), fontSize = 14.sp, textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── LIVE CLASS CALENDAR ENTRY ROW ────────────────────────────────
            if (hasAnyClasses) {
                ElevatedCard(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape     = RoundedCornerShape(16.dp),
                    colors    = CardDefaults.elevatedCardColors(containerColor = CardWhite),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 5.dp, pressedElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                calendarMonth = monthStart(selectedDayMillis)
                                showCalendarOverlay = true
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).background(RemindBg, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = RemindText, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.liveclasses_calendar_title), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                            Text(
                                if (isViewingToday) stringResource(R.string.liveclasses_viewing_today)
                                else stringResource(R.string.liveclasses_viewing_format, SimpleDateFormat("EEE, d MMM", Locale.US).format(Date(selectedDayMillis))),
                                fontSize = 13.sp, color = TextSecondary
                            )
                        }
                        Text("›", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    }
                }

                Spacer(Modifier.height(28.dp))
            }

            // ── SESSIONS FOR SELECTED DAY ─────────────────────────────────────
            Text(
                if (isViewingToday) stringResource(R.string.liveclasses_todays_sessions)
                else stringResource(R.string.liveclasses_sessions_on_format, SimpleDateFormat("d MMM", Locale.US).format(Date(selectedDayMillis))),
                fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary,
                modifier   = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(14.dp))

            if (!hasAnyClasses) {
                Text(
                    if (allClasses == null) stringResource(R.string.liveclasses_loading) else stringResource(R.string.liveclasses_no_classes_scheduled),
                    fontSize = 14.sp, color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            } else if (selectedDayClasses.isEmpty()) {
                Text(
                    stringResource(R.string.liveclasses_no_classes_for_day),
                    fontSize = 14.sp, color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            } else {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    selectedDayClasses.forEach { cls ->
                        SessionCard(
                            session = cls,
                            isLive = liveClass?.id == cls.id,
                            onJoin = { uriHandler.openUri(cls.meetingLink) }
                        )
                    }
                }
            }
        }

        // ── FULL-MONTH CALENDAR OVERLAY ─────────────────────────────────────
        if (showCalendarOverlay) {
            CalendarOverlay(
                displayedMonth = calendarMonth,
                classes = classes,
                today = startOfDay(nowMillis),
                selectedDayMillis = selectedDayMillis,
                onPrevMonth = { calendarMonth = addMonths(calendarMonth, -1) },
                onNextMonth = { calendarMonth = addMonths(calendarMonth, 1) },
                onDayClick = { dayMillis ->
                    selectedDayMillis = dayMillis
                    previewDayMillis = dayMillis
                },
                onClose = {
                    showCalendarOverlay = false
                    previewDayMillis = null
                }
            )
        }

        // ── DAY PREVIEW OVERLAY (drawn on top of the calendar overlay) ──────
        previewDayMillis?.let { dayMillis ->
            DayPreviewOverlay(
                dayMillis = dayMillis,
                classesForDay = classes.filter { isSameLocalDay(it.dateMillis, dayMillis) }.sortedBy { it.dateMillis },
                onClose = { previewDayMillis = null }
            )
        }
    }
}

// ── Join button pill (shared by live + countdown hero states) ───────────────
@Composable
private fun JoinPill(accentColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(50.dp))
            .background(Color.White, RoundedCornerShape(50.dp))
            .clickable { onClick() }
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Default.VideoCameraFront, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.liveclasses_join_now_button), color = accentColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// ── Pulsing status dot (used for both "starts in" red and "live now" green) ──
@Composable
private fun PulsingDot(color: Color, size: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "dotPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(700), repeatMode = RepeatMode.Reverse),
        label = "dotAlpha"
    )
    Box(modifier = Modifier.size(size).background(color.copy(alpha = alpha), CircleShape))
}

// ── Countdown box — premium glass/3D tile ─────────────────────────────────────
@Composable
private fun CountdownBox(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(width = 78.dp, height = 70.dp)
                .shadow(
                    elevation    = 10.dp,
                    shape        = RoundedCornerShape(16.dp),
                    ambientColor = Color.Black.copy(alpha = 0.55f),
                    spotColor    = Color.Black.copy(alpha = 0.55f)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.08f)))
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.80f), Color.White.copy(alpha = 0.05f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.20f)),
                            startY = 60f
                        ),
                        RoundedCornerShape(16.dp)
                    )
            )
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                color = Color.White,
                style = TextStyle(
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.40f), offset = Offset(0f, 3f), blurRadius = 6f)
                )
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(label, fontSize = 12.sp, color = Color.White.copy(0.78f), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

// ── Session card (real data) ──────────────────────────────────────────────────
@Composable
private fun SessionCard(session: LiveClass, isLive: Boolean = false, onJoin: () -> Unit) {
    ElevatedCard(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.elevatedCardColors(containerColor = CardWhite),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 5.dp, pressedElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(5.dp, RoundedCornerShape(12.dp), spotColor = BlueGradientStart.copy(0.3f))
                    .background(BlueGradientStart.copy(0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = BlueGradientStart, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(session.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    if (isLive) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(LiveGreenDot.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(stringResource(R.string.liveclasses_live_badge_short), color = LiveGreenDot, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp)
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    stringResource(
                        R.string.liveclasses_session_time_teacher_format,
                        SimpleDateFormat("h:mm a", Locale.US).format(Date(session.dateMillis)),
                        session.teacherName
                    ),
                    fontSize = 13.sp, color = TextSecondary
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .shadow(3.dp, RoundedCornerShape(10.dp), spotColor = RemindText.copy(0.2f))
                    .background(RemindBg, RoundedCornerShape(10.dp))
                    .clickable { onJoin() }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(stringResource(R.string.liveclasses_join_button), color = RemindText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── FULL MONTH CALENDAR OVERLAY ─────────────────────────────────────────────────
@Composable
private fun CalendarOverlay(
    displayedMonth: Calendar,
    classes: List<LiveClass>,
    today: Long,
    selectedDayMillis: Long,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (Long) -> Unit,
    onClose: () -> Unit
) {
    val weekdayInitials = stringArrayResource(R.array.liveclasses_weekday_initials)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CardWhite)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { /* consume clicks */ }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.liveclasses_calendar_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
                Icon(
                    Icons.Filled.Close, contentDescription = stringResource(R.string.liveclasses_close_description), tint = TextSecondary,
                    modifier = Modifier.size(24.dp).clickable { onClose() }
                )
            }

            // ── Month navigation ───────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.liveclasses_previous_month_description), tint = TextSecondary,
                    modifier = Modifier.size(26.dp).clickable { onPrevMonth() }
                )
                Text(
                    SimpleDateFormat("MMMM yyyy", Locale.US).format(displayedMonth.time),
                    fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary
                )
                Icon(
                    Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.liveclasses_next_month_description), tint = BlueGradientStart,
                    modifier = Modifier.size(26.dp).clickable { onNextMonth() }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Grid horizontal inset is shared by the weekday header row AND
            // the day rows below, so columns line up exactly.
            val gridInset = 18.dp

            // ── Weekday headers ─────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = gridInset)) {
                weekdayInitials.forEach { d ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(d, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = DividerColor, thickness = 1.dp, modifier = Modifier.padding(horizontal = gridInset))
            Spacer(Modifier.height(14.dp))

            // ── Day grid ─────────────────────────────────────────────────
            val daysInMonth = displayedMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
            val leading = (displayedMonth.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, 1)
            }.get(Calendar.DAY_OF_WEEK) - 1 // Calendar.SUNDAY == 1 -> 0-based

            val cells: List<Int?> = List(leading) { null } + (1..daysInMonth).toList()

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = gridInset),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cells.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        week.forEach { day ->
                            Box(modifier = Modifier.weight(1f)) {
                                if (day != null) {
                                    val cellMillis = (displayedMonth.clone() as Calendar).apply {
                                        set(Calendar.DAY_OF_MONTH, day)
                                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                    val dayClasses = classes.filter { isSameLocalDay(it.dateMillis, cellMillis) }
                                        .sortedBy { it.dateMillis }
                                    val isToday = cellMillis == today
                                    val isSelected = isSameLocalDay(cellMillis, selectedDayMillis)
                                    val isPast = cellMillis < today

                                    MonthDayCell(
                                        dayNum = day,
                                        dayClasses = dayClasses,
                                        isToday = isToday,
                                        isSelected = isSelected,
                                        isPast = isPast,
                                        onClick = { onDayClick(cellMillis) }
                                    )
                                } else {
                                    Spacer(Modifier.aspectRatio(0.82f))
                                }
                            }
                        }
                        repeat(7 - week.size) { Box(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthDayCell(
    dayNum: Int,
    dayClasses: List<LiveClass>,
    isToday: Boolean,
    isSelected: Boolean,
    isPast: Boolean,
    onClick: () -> Unit
) {
    val hasClasses = dayClasses.isNotEmpty()
    val extraCount = (dayClasses.size - 1).coerceAtLeast(0)

    val bg = when {
        isSelected -> SelectedDayBg
        hasClasses -> DayHasClassBg
        else -> Color.Transparent
    }
    val numberColor = when {
        isSelected -> Color.White
        isPast && !hasClasses -> TextSecondary.copy(alpha = 0.38f)
        else -> TextPrimary
    }
    val subLabelColor = if (isSelected) Color.White.copy(alpha = 0.92f) else BlueGradientStart

    // Outer box is intentionally NOT clipped, so the "+N" badge can sit
    // slightly outside the rounded cell without getting cut off.
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.82f)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(bg)
                .then(
                    if (isSelected)
                        Modifier.shadow(6.dp, RoundedCornerShape(14.dp), spotColor = SelectedDayBg.copy(alpha = 0.5f))
                    else Modifier
                )
                .then(
                    if (isToday && !isSelected)
                        Modifier.border(1.5.dp, BlueGradientStart, RoundedCornerShape(14.dp))
                    else Modifier
                )
                .clip(RoundedCornerShape(14.dp))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    dayNum.toString(),
                    fontSize = 15.sp,
                    fontWeight = if (isSelected || isToday || hasClasses) FontWeight.Bold else FontWeight.Medium,
                    color = numberColor
                )
                if (hasClasses) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        dayClasses.first().teacherName.substringBefore(" "),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = subLabelColor,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (extraCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 5.dp, y = (-5).dp)
                    .size(17.dp)
                    .shadow(3.dp, CircleShape)
                    .background(BlueGradientStart, CircleShape)
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("+$extraCount", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ── DAY PREVIEW OVERLAY ─────────────────────────────────────────────────────────
@Composable
private fun DayPreviewOverlay(
    dayMillis: Long,
    classesForDay: List<LiveClass>,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .shadow(20.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(CardWhite)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { /* absorb */ }
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        SimpleDateFormat("EEEE, MMM d", Locale.US).format(Date(dayMillis)),
                        fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary
                    )
                    Icon(
                        Icons.Filled.Close, contentDescription = stringResource(R.string.liveclasses_close_description), tint = TextSecondary,
                        modifier = Modifier.size(20.dp).clickable { onClose() }
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (classesForDay.isEmpty()) {
                    Text(
                        stringResource(R.string.liveclasses_no_classes_for_day),
                        fontSize = 14.sp, color = TextSecondary
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        classesForDay.forEach { cls ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(RemindBg, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = RemindText, modifier = Modifier.size(16.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(cls.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                    Text(
                                        stringResource(
                                            R.string.liveclasses_day_preview_teacher_time_format,
                                            cls.teacherName,
                                            SimpleDateFormat("h:mm a", Locale.US).format(Date(cls.dateMillis))
                                        ),
                                        fontSize = 12.sp, color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}