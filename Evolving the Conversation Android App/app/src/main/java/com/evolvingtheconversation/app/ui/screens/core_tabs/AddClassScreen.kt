package com.evolvingtheconversation.app.ui.screens.core_tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evolvingtheconversation.viewmodel.TeacherViewModel
import com.evolvingtheconversation.app.ui.components.BackButton
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme

private val BlueStart = Color(0xFF1E5EF3)
private val SurfaceBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFF4F6FB)
private val TextPrimary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)
private val TextSecondary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)
private val CardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val RedDate = Color(0xFFE53935)

@Composable
fun AddClassScreen(
    teacherViewModel: TeacherViewModel,
    onBack: () -> Unit = {},
    onSaved: () -> Unit = {}
) {
    var title by remember { mutableStateOf("") }
    var meetingLink by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val today = remember { startOfDay(System.currentTimeMillis()) }
    var displayedMonth by remember { mutableStateOf(monthStart(today)) }
    var selectedDayMillis by remember { mutableStateOf(today) }

    val nowCal = remember { Calendar.getInstance() }
    var hourIndex by remember { mutableStateOf(((nowCal.get(Calendar.HOUR).let { if (it == 0) 12 else it }) - 1)) } // 0..11 -> hour 1..12
    var minuteIndex by remember { mutableStateOf(nowCal.get(Calendar.MINUTE)) } // 0..59
    var periodIndex by remember { mutableStateOf(if (nowCal.get(Calendar.AM_PM) == Calendar.PM) 1 else 0) } // 0=AM,1=PM
    var showTimeWheel by remember { mutableStateOf(false) }

    val timeLabel = formatTimeLabel(hourIndex, minuteIndex, periodIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 40.dp)
    ) {
        // ── TOP BAR ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(onBack = onBack)
            Spacer(Modifier.width(16.dp))
            Text("Add Class", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextPrimary)
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column {
                Text("Class title", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    singleLine = true, shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Filled.Title, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column {
                Text("Meeting link", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = meetingLink, onValueChange = { meetingLink = it },
                    singleLine = true, shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text("Date & time", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

            MonthCalendar(
                displayedMonth = displayedMonth,
                today = today,
                selectedDayMillis = selectedDayMillis,
                onSelectDay = { selectedDayMillis = it },
                onPrevMonth = { displayedMonth = addMonths(displayedMonth, -1) },
                onNextMonth = { displayedMonth = addMonths(displayedMonth, 1) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Time", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Box(
                    modifier = Modifier
                        .shadow(3.dp, RoundedCornerShape(10.dp))
                        .background(CardWhite, RoundedCornerShape(10.dp))
                        .clickable { showTimeWheel = !showTimeWheel }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(timeLabel, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                }
            }

            if (showTimeWheel) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .background(CardWhite, RoundedCornerShape(16.dp))
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    WheelColumn(
                        items = (1..12).map { it.toString() },
                        selectedIndex = hourIndex,
                        onSelected = { hourIndex = it },
                        modifier = Modifier.width(60.dp)
                    )
                    WheelColumn(
                        items = (0..59).map { String.format(Locale.US, "%02d", it) },
                        selectedIndex = minuteIndex,
                        onSelected = { minuteIndex = it },
                        modifier = Modifier.width(60.dp)
                    )
                    WheelColumn(
                        items = listOf("AM", "PM"),
                        selectedIndex = periodIndex,
                        onSelected = { periodIndex = it },
                        modifier = Modifier.width(60.dp)
                    )
                }
                Text(
                    "Done",
                    color = BlueStart, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable { showTimeWheel = false }
                        .padding(6.dp)
                )
            }

            errorMsg?.let { Text(it, color = RedDate, fontSize = 13.sp) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(14.dp))
                    .background(BlueStart, RoundedCornerShape(14.dp))
                    .clickable(enabled = !saving) {
                        val finalMillis = combineDateAndTime(selectedDayMillis, hourIndex, minuteIndex, periodIndex)
                        saving = true
                        errorMsg = null
                        teacherViewModel.addLiveClass(title, meetingLink, finalMillis) { success, err ->
                            saving = false
                            if (success) onSaved() else errorMsg = err ?: "Failed to save class"
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (saving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save class", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

// ── Month calendar grid ─────────────────────────────────────────────────────
@Composable
private fun MonthCalendar(
    displayedMonth: Calendar,
    today: Long,
    selectedDayMillis: Long,
    onSelectDay: (Long) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(16.dp))
            .background(CardWhite, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                SimpleDateFormat("MMMM yyyy", Locale.US).format(displayedMonth.time),
                fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary
            )
            Row {
                Icon(
                    Icons.Filled.ChevronLeft, contentDescription = "Previous month", tint = TextSecondary,
                    modifier = Modifier.clickable { onPrevMonth() }
                )
                Spacer(Modifier.width(12.dp))
                Icon(
                    Icons.Filled.ChevronRight, contentDescription = "Next month", tint = BlueStart,
                    modifier = Modifier.clickable { onNextMonth() }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT").forEach { d ->
                Text(
                    d, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        val daysInMonth = displayedMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        val leading = (displayedMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }.get(Calendar.DAY_OF_WEEK) - 1 // Calendar.SUNDAY == 1 -> 0-based

        val cells: List<Int?> = List(leading) { null } + (1..daysInMonth).toList()
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (day != null) {
                            val cellMillis = (displayedMonth.clone() as Calendar).apply {
                                set(Calendar.DAY_OF_MONTH, day)
                                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            val isPast = cellMillis < today
                            val isSelected = cellMillis == selectedDayMillis
                            val isToday = cellMillis == today

                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(if (isSelected) BlueStart else Color.Transparent, CircleShape)
                                    .then(
                                        if (isToday && !isSelected)
                                            Modifier.background(Color.Transparent, CircleShape)
                                        else Modifier
                                    )
                                    .clickable(enabled = !isPast) { onSelectDay(cellMillis) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    day.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isSelected -> Color.White
                                        isPast -> TextSecondary.copy(alpha = 0.4f)
                                        else -> TextPrimary
                                    }
                                )
                            }
                        }
                    }
                }
                // pad the row out to 7 columns so weeks with fewer trailing days still align
                repeat(7 - week.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

// ── Scroll wheel column (hour / minute / AM-PM) ─────────────────────────────
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun WheelColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemHeight = 36.dp
    val visibleCount = 3
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }

    // Single effect, sequenced: first snap the list to the current value with
    // NO reporting, then only start listening for scroll-settle events that
    // happen AFTER that initial positioning is done. This is what prevents
    // the "opens and immediately overwrites your time" bug — two separate
    // LaunchedEffects racing each other let the settle-detector fire once on
    // mount before scrollToItem finished.
    LaunchedEffect(listState) {
        listState.scrollToItem(selectedIndex)

        snapshotFlow { listState.isScrollInProgress }
            .drop(1) // drop the initial "false" value emitted right after scrollToItem — not a real settle
            .filter { isScrolling -> !isScrolling } // only care about the moment scrolling actually stops
            .collect {
                // Exact centered index, not a guess: with contentPadding set to
                // one itemHeight on each side, the item that sits under the
                // highlight box is always firstVisibleItemIndex, offset by
                // however many whole itemHeights of residual scroll remain.
                // (The previous approach compared visible-item pixel offsets to
                // a measured viewport center, which was consistently off by one row.)
                val centeredIndex = (listState.firstVisibleItemIndex +
                        (listState.firstVisibleItemScrollOffset / itemHeightPx).roundToInt())
                    .coerceIn(0, items.lastIndex)
                if (centeredIndex != selectedIndex) onSelected(centeredIndex)
            }
    }

    Box(modifier = modifier.height(itemHeight * visibleCount), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(BlueStart.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
        )
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            contentPadding = PaddingValues(vertical = itemHeight * (visibleCount / 2)),
            modifier = Modifier.fillMaxHeight()
        ) {
            itemsIndexed(items) { index, label ->
                val isSelected = index == selectedIndex
                Box(modifier = Modifier.height(itemHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        label,
                        fontSize = if (isSelected) 17.sp else 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) TextPrimary else TextSecondary
                    )
                }
            }
        }
    }
}

// ── Date/time helpers ────────────────────────────────────────────────────────
private fun startOfDay(millis: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun monthStart(millis: Long): Calendar =
    Calendar.getInstance().apply { timeInMillis = millis; set(Calendar.DAY_OF_MONTH, 1) }

private fun addMonths(cal: Calendar, delta: Int): Calendar =
    (cal.clone() as Calendar).apply { add(Calendar.MONTH, delta) }

private fun formatTimeLabel(hourIndex: Int, minuteIndex: Int, periodIndex: Int): String {
    val hour = hourIndex + 1
    val period = if (periodIndex == 0) "AM" else "PM"
    return String.format(Locale.US, "%d:%02d %s", hour, minuteIndex, period)
}

private fun combineDateAndTime(dayMillis: Long, hourIndex: Int, minuteIndex: Int, periodIndex: Int): Long {
    val hour12 = hourIndex + 1
    val hour24 = when {
        periodIndex == 1 && hour12 != 12 -> hour12 + 12 // PM
        periodIndex == 0 && hour12 == 12 -> 0            // 12 AM -> 0
        else -> hour12
    }
    return Calendar.getInstance().apply {
        timeInMillis = dayMillis
        set(Calendar.HOUR_OF_DAY, hour24)
        set(Calendar.MINUTE, minuteIndex)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}