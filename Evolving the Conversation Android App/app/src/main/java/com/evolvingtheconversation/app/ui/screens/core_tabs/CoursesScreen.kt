package com.evolvingtheconversation.app.ui.screens.core_tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evolvingtheconversation.app.data.CourseCatalog
import androidx.compose.runtime.getValue
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme
import com.evolvingtheconversation.app.R
private val SurfaceCardGradientTop: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color.White
private val SurfaceCardGradientBottom: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color(0xFFF7F8FC)
private val SurfaceCardGradientBottom2: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF232328) else Color(0xFFEFF1F6)
private val CardBorderColor: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2C2C2E) else Color(0xFFF1F2F6)
private val ChevronCircleBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color(0xFFF7F8FC)
private val IconBoxGradientTop: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color.White
private val BlueStart      = Color(0xFF1E5EF3)
private val SurfaceBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFFBFBFD)
private val CardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val TextPrimary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)
private val TextSecondary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)
private val ChipBorder: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF3A3D45) else Color(0xFFD1D5DB)
private val ProgressBlue   = Color(0xFF1E5EF3)
private val ProgressGreen  = Color(0xFF0CBD85)
private val ProgressPurple = Color(0xFF7C3AED)

/** Same tinted-shadow trick used across the app's other "premium" screens. */
private fun Modifier.premiumLift(
    shape: RoundedCornerShape,
    elevation: androidx.compose.ui.unit.Dp = 10.dp,
    tint: Color = Color.Black
): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = tint.copy(alpha = 0.18f),
    spotColor = tint.copy(alpha = 0.28f)
)

// ─── Data ─────────────────────────────────────────────────────────────────────
// Resource IDs instead of raw strings — this list is built OUTSIDE a
// Composable scope, so it can't call stringResource() itself. Resolved in
// the filter chip Row below, which IS a Composable.
private val filterTabResIds = listOf(
    R.string.courses_filter_all,
    R.string.courses_filter_beginner,
    R.string.courses_filter_spoken,
    R.string.courses_filter_grammar,
    R.string.courses_filter_business,
    R.string.courses_filter_ielts,
    R.string.courses_filter_advanced
)

// Local display model for a course card. Built at runtime from CourseCatalog +
// real Firestore progress — NOT hardcoded anymore. (Old hardcoded `courses` list removed.)
// title/instructor/duration/students come straight from CourseCatalog (a
// separate data file) — dynamic catalog data, not translated here.
private data class Course(
    val id            : String,
    val title         : String,
    val instructor    : String,
    val duration      : String,
    val students      : String,
    val progressPercent : Int?,   // null when there's no progress yet
    val progress      : Float,
    val cardGradient  : List<Color>,
    val progressColor : Color = ProgressBlue
)

private val gradients = listOf(
    listOf(Color(0xFF1E5EF3), Color(0xFF1E3A8A)),
    listOf(Color(0xFF0CBD85), Color(0xFF059669)),
    listOf(Color(0xFFFFA800), Color(0xFFE67E00)),
    listOf(Color(0xFF7C3AED), Color(0xFF5B21B6))
)
private val progressColors = listOf(ProgressBlue, ProgressGreen, ProgressBlue, ProgressPurple)

// ─── Screen ───────────────────────────────────────────────────────────────────
@Composable
fun CoursesScreen(
    completedLessonsByCourse: Map<String, Set<String>> = emptyMap(), // courseId -> completed lesson ids, from Firestore
    onCourseClick : (courseId: String) -> Unit = {},
    onSearchClick : () -> Unit = {},
    onNotifClick  : () -> Unit = {}
) {
    val verticalScroll = rememberScrollState()
    val chipScroll     = rememberScrollState()
    var selectedTab    by remember { mutableStateOf(0) }

    // Build display list from the real catalog + real per-user progress.
    // This replaces the old hardcoded `courses` val entirely.
    val displayCourses = CourseCatalog.courses.mapIndexed { index, catalogCourse ->
        val completedIds     = completedLessonsByCourse[catalogCourse.id] ?: emptySet()
        val totalLessons     = catalogCourse.lessons.size
        val progressFraction = if (totalLessons > 0) completedIds.size.toFloat() / totalLessons else 0f

        Course(
            id            = catalogCourse.id,
            title         = catalogCourse.title,
            instructor    = catalogCourse.instructor,
            duration      = catalogCourse.duration,
            students      = catalogCourse.students,
            progressPercent = if (progressFraction > 0f) (progressFraction * 100).toInt() else null,
            progress      = progressFraction,
            cardGradient  = gradients[index % gradients.size],
            progressColor = progressColors[index % progressColors.size]
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(SurfaceBg)) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScroll)
                .padding(bottom = 72.dp)
        ) {

            // ── TOP BAR ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 52.dp, start = 18.dp, end = 18.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        stringResource(R.string.courses_title),
                        fontWeight = FontWeight.Bold,
                        fontSize   = 22.sp,
                        color      = TextPrimary
                    )
                    Text(
                        stringResource(R.string.courses_subtitle),
                        fontSize = 13.sp,
                        color    = TextSecondary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // Search button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .premiumLift(RoundedCornerShape(12.dp), elevation = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.verticalGradient(listOf(SurfaceCardGradientTop, SurfaceCardGradientBottom)))
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(listOf(SurfaceCardGradientTop, SurfaceCardGradientBottom2)),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSearchClick() },
                        contentAlignment = Alignment.Center
                    )  {
                        Icon(
                            imageVector        = Icons.Outlined.Search,
                            contentDescription = stringResource(R.string.courses_search_description),
                            tint               = TextPrimary,
                            modifier           = Modifier.size(22.dp)
                        )
                    }

                    // Notification button with badge
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .premiumLift(RoundedCornerShape(12.dp), elevation = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.verticalGradient(listOf(SurfaceCardGradientTop, SurfaceCardGradientBottom)))
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(listOf(SurfaceCardGradientTop, SurfaceCardGradientBottom2)),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onNotifClick() },
                        contentAlignment = Alignment.Center
                    ){
                        Box {
                            Icon(
                                imageVector        = Icons.Outlined.Notifications,
                                contentDescription = stringResource(R.string.courses_notifications_description),
                                tint               = TextPrimary,
                                modifier           = Modifier.size(22.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .shadow(3.dp, CircleShape, spotColor = Color(0xFFE53935).copy(alpha = 0.6f))
                                    .background(Brush.radialGradient(listOf(Color(0xFFFF6259), Color(0xFFE53935))), CircleShape)
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                }
            }

            // ── SCROLLABLE FILTER CHIPS ───────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chipScroll)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                filterTabResIds.forEachIndexed { index, labelResId ->
                    val selected = index == selectedTab
                    Box(
                        modifier = Modifier
                            .then(
                                if (selected)
                                    Modifier
                                        .shadow(
                                            elevation = 8.dp,
                                            shape = RoundedCornerShape(50.dp),
                                            ambientColor = BlueStart.copy(alpha = 0.4f),
                                            spotColor = BlueStart.copy(alpha = 0.5f)
                                        )
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(Brush.horizontalGradient(listOf(Color(0xFF4F8EF7), BlueStart)))
                                else
                                    Modifier
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(CardWhite)
                                        .border(1.dp, ChipBorder, RoundedCornerShape(50.dp))
                            )
                            .clickable { selectedTab = index }
                    ) {
                        Text(
                            text       = stringResource(labelResId),
                            fontSize   = 13.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color      = if (selected) Color.White else TextSecondary,
                            modifier   = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // ── 2-COLUMN COURSE GRID ──────────────────────────────────────────
            Column(
                modifier            = Modifier.padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                displayCourses.chunked(2).forEach { rowCourses ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        rowCourses.forEach { course ->
                            CourseCard(
                                course   = course,
                                modifier = Modifier.weight(1f),
                                onClick  = { onCourseClick(course.id) }
                            )
                        }
                        if (rowCourses.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Course card ─────────────────────────────────────────────────────────────
@Composable
private fun CourseCard(
    course   : Course,
    modifier : Modifier = Modifier,
    onClick  : () -> Unit = {}
) {
    Card(
        modifier  = modifier
            .premiumLift(RoundedCornerShape(16.dp), elevation = 10.dp, tint = course.cardGradient.last())
            .clickable { onClick() },
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {

            // Gradient thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(course.cardGradient.first().copy(alpha = 1f), course.cardGradient.last())
                        ),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
            ) {
                if (course.progressPercent != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        shape = RoundedCornerShape(50.dp),
                        color = Color.White.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text       = stringResource(R.string.courses_progress_done, course.progressPercent),
                            fontSize   = 9.sp,
                            color      = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(56.dp)
                        .shadow(8.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.25f))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.10f))
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.MenuBook,
                        contentDescription = null,
                        tint               = Color.White.copy(alpha = 0.95f),
                        modifier           = Modifier.size(28.dp)
                    )
                }
            }

            // Card body
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    course.title,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 13.sp,
                    color      = TextPrimary,
                    lineHeight = 18.sp
                )
                Text(
                    course.instructor,
                    fontSize = 11.sp,
                    color    = TextSecondary
                )
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            tint               = TextSecondary,
                            modifier           = Modifier.size(11.dp)
                        )
                        Text(course.duration, fontSize = 10.sp, color = TextSecondary)
                    }
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.Person,
                            contentDescription = null,
                            tint               = TextSecondary,
                            modifier           = Modifier.size(11.dp)
                        )
                        Text(course.students, fontSize = 10.sp, color = TextSecondary)
                    }
                }

                if (course.progress > 0f) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFE5E7EB))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(course.progress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .shadow(2.dp, RoundedCornerShape(3.dp), spotColor = course.progressColor.copy(alpha = 0.6f))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(course.progressColor.copy(alpha = 0.75f), course.progressColor)
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}