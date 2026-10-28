package com.evolvingtheconversation.app.ui.screens.core_tabs

import androidx.compose.material.icons.filled.ArrowBackIosNew
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evolvingtheconversation.app.data.CourseCatalog
import kotlinx.coroutines.delay
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme
import com.evolvingtheconversation.app.R
private val BlueStart     = Color(0xFF1E5EF3)
private val BlueEnd       = Color(0xFF1E3A8A)
private val GreenAccent   = Color(0xFF0CBD85)
private val QTextPri: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)
private val SurfaceBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFF4F6FB)
private val CardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val TextPrimary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)
private val TextSecondary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)
private val OrangeStar    = Color(0xFFFFA800)
private val GreenLabel    = Color(0xFF0CBD85)

// New — needed for the inline gradient/border fixes below
private val CardBorder: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color(0xFFF0F0F5)
private val CardGradientTop: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val CardGradientBottom: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF232326) else Color(0xFFFAFBFF)
private val LockedCircleBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color(0xFFF3F4F6)
private val DurationTextColor: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF6B7280) else Color(0xFFB0B7C3)
private val FrostedOverlay: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color.Black.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.35f)

/** Same tinted-shadow trick used across the app's other "premium" screens —
 *  pushed a bit further here so the whole detail screen reads as premium. */
private fun Modifier.premiumLift(
    shape: RoundedCornerShape,
    elevation: androidx.compose.ui.unit.Dp = 14.dp,
    tint: Color = Color.Black
): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = tint.copy(alpha = 0.20f),
    spotColor = tint.copy(alpha = 0.30f)
)

// ─── Curriculum data ──────────────────────────────────────────────────────────
private enum class LessonState { DONE, ACTIVE, LOCKED }

private data class Lesson(
    val id       : String,
    val title    : String,
    val duration : String,
    val state    : LessonState,
    val number   : Int = 0
)

// ─── Screen ───────────────────────────────────────────────────────────────────
@Composable
fun CourseDetailScreen(
    courseId: String,
    completedLessonIds: Set<String> = emptySet(),     // from Firestore
    isEnrolled: Boolean = false,                       // has the user "paid" for this course
    showPaymentSuccess: Boolean = false,               // show the success banner once, right after purchase
    onPaymentSuccessDismiss: () -> Unit = {},
    onBack: () -> Unit = {},
    onLessonClick: (lessonId: String) -> Unit = {},
    onEnrollClick: () -> Unit = {}                     // navigates to the plans screen — no payment happens here
) {
    val scrollState = rememberScrollState()
    val course = CourseCatalog.getCourse(courseId) ?: return
    val firstIncompleteId = course.lessons.firstOrNull { it.id !in completedLessonIds }?.id

    val curriculum = course.lessons.map { lesson ->
        val state = when {
            !isEnrolled                      -> LessonState.LOCKED
            lesson.id in completedLessonIds  -> LessonState.DONE
            lesson.id == firstIncompleteId    -> LessonState.ACTIVE
            else                              -> LessonState.LOCKED
        }
        Lesson(lesson.id, lesson.title, lesson.duration, state, lesson.number)
    }

    Box(modifier = Modifier.fillMaxSize().background(SurfaceBg)) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = if (!isEnrolled) 88.dp else 24.dp)
        ) {

            // ── HERO BANNER ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .shadow(
                        elevation = 18.dp,
                        ambientColor = BlueStart.copy(alpha = 0.45f),
                        spotColor = BlueStart.copy(alpha = 0.45f)
                    )
                    .background(brush = Brush.linearGradient(listOf(Color(0xFF4F8EF7), BlueStart, BlueEnd))),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 44.dp, start = 12.dp)
                        .size(38.dp)
                        .shadow(10.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.4f))
                        .background(
                            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.14f))),
                            CircleShape
                        )
                        .border(
                            width = 1.2.dp,
                            brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.15f))),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBackIosNew,
                        contentDescription = stringResource(R.string.course_detail_back_description),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .shadow(18.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.4f))
                        .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF3F6FF))), CircleShape)
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFDCE6FF))),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.course_detail_play_preview_description),
                        tint = BlueStart, modifier = Modifier.size(36.dp)
                    )
                }
            }

            // ── WHITE MAIN CARD (rounded top corners overlapping hero) ────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-24).dp)          // pulls sheet up over blue hero
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        ambientColor = Color.Black.copy(alpha = 0.22f),
                        spotColor = Color.Black.copy(alpha = 0.22f)
                    )
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(CardWhite)
                    .padding(horizontal = 20.dp)
                    .padding(top = 28.dp, bottom = 16.dp)
            ) {

                // Category label
                Text(
                    stringResource(R.string.course_detail_category_label),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenLabel,
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    course.title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = TextPrimary,
                    lineHeight = 30.sp
                )

                Spacer(Modifier.height(10.dp))

                // Stats row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.Star, contentDescription = null,
                            tint = OrangeStar, modifier = Modifier.size(14.dp)
                        )
                        Text(
                            stringResource(R.string.course_detail_rating_label), fontSize = 12.sp,
                            color = TextSecondary, fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.AccessTime, contentDescription = null,
                            tint = TextSecondary, modifier = Modifier.size(13.dp)
                        )
                        Text(stringResource(R.string.course_detail_duration_label), fontSize = 12.sp, color = TextSecondary)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.LibraryBooks, contentDescription = null,
                            tint = TextSecondary, modifier = Modifier.size(13.dp)
                        )
                        Text(
                            pluralStringResource(
                                R.plurals.course_detail_lessons_count,
                                course.lessons.size,
                                course.lessons.size
                            ),
                            fontSize = 12.sp, color = TextSecondary
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    stringResource(R.string.course_detail_description),
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(20.dp))

                // ── INSTRUCTOR CARD ───────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumLift(RoundedCornerShape(14.dp), elevation = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(listOf(CardGradientTop, CardGradientBottom)))
                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(8.dp, CircleShape, spotColor = BlueStart.copy(alpha = 0.45f))
                            .background(
                                brush = Brush.linearGradient(listOf(Color(0xFF4F8EF7), BlueStart, BlueEnd)),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.course_detail_instructor_initials), fontWeight = FontWeight.Bold,
                            fontSize = 14.sp, color = Color.White
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.course_detail_instructor_name), fontWeight = FontWeight.Bold,
                            fontSize = 14.sp, color = TextPrimary
                        )
                        Text(
                            stringResource(R.string.course_detail_instructor_title),
                            fontSize = 12.sp, color = TextSecondary
                        )
                    }

                    Icon(
                        Icons.Filled.Verified, contentDescription = stringResource(R.string.course_detail_verified_description),
                        tint = BlueStart, modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.height(28.dp))

                // ── CURRICULUM ────────────────────────────────────────────────
                Text(
                    stringResource(R.string.course_detail_curriculum_title), fontWeight = FontWeight.Bold,
                    fontSize = 20.sp, color = TextPrimary
                )

                Spacer(Modifier.height(12.dp))

                curriculum.forEach { lesson ->
                    LessonRow(
                        lesson = lesson,
                        blurred = !isEnrolled,
                        onClick = { id -> if (isEnrolled) onLessonClick(id) else onEnrollClick() }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }

        // ── STICKY FOOTER (only shown before purchase) ─────────────────────────
        if (!isEnrolled) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                shadowElevation = 20.dp,
                color = CardWhite
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(stringResource(R.string.course_detail_lifetime_access), fontSize = 18.sp, color = TextSecondary)
                        Text(
                            stringResource(R.string.course_detail_price_label), fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp, color = TextPrimary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .shadow(
                                elevation = 18.dp,
                                shape = RoundedCornerShape(50.dp),
                                ambientColor = BlueStart.copy(alpha = 0.55f),
                                spotColor = BlueStart.copy(alpha = 0.65f)
                            )
                            .clip(RoundedCornerShape(50.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFF4F8EF7), BlueStart, BlueEnd)))
                            .border(
                                width = 1.5.dp,
                                brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent)),
                                shape = RoundedCornerShape(50.dp)
                            )
                            .clickable { onEnrollClick() }
                            .height(50.dp)
                            .width(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.course_detail_enroll_button), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // ── PAYMENT SUCCESS BANNER (shown once, right after purchase) ──────────
        if (showPaymentSuccess) {
            LaunchedEffect(showPaymentSuccess) {
                delay(3000)
                onPaymentSuccessDismiss()
            }
            PaymentSuccessBanner(modifier = Modifier.align(Alignment.TopCenter))
        }
    }
}

// ─── Payment success pill ──────────────────────────────────────────────────────
@Composable
private fun PaymentSuccessBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .padding(top = 44.dp)
            .shadow(14.dp, RoundedCornerShape(50.dp), spotColor = GreenAccent.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(50.dp),
        color = GreenAccent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color.White.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                stringResource(R.string.course_detail_payment_successful),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

// ─── Lesson row — blurred+locked when the course isn't purchased yet ──────────
@Composable
private fun LessonRow(lesson: Lesson, blurred: Boolean, onClick: (lessonId: String) -> Unit) {
    val isActive = lesson.state == LessonState.ACTIVE
    val isDone = lesson.state == LessonState.DONE
    val isLocked = lesson.state == LessonState.LOCKED

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isActive) 14.dp else 6.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = if (isActive) BlueStart.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.12f),
                    spotColor = if (isActive) BlueStart.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.18f)
                )
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(CardGradientTop, CardGradientBottom)))
                .then(
                    when {
                        isActive -> Modifier.border(1.5.dp, BlueStart, RoundedCornerShape(14.dp))
                        isLocked -> Modifier.border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                        else -> Modifier.border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    }
                )
                .clickable { onClick(lesson.id) }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left icon / number
            when (lesson.state) {
                LessonState.DONE -> {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = stringResource(R.string.course_detail_lesson_completed_description),
                        tint = GreenAccent,
                        modifier = Modifier.size(30.dp)
                    )
                }

                LessonState.ACTIVE -> {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .shadow(8.dp, CircleShape, spotColor = BlueStart.copy(alpha = 0.55f))
                            .background(Brush.verticalGradient(listOf(Color(0xFF4F8EF7), BlueStart)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.course_detail_lesson_playing_description),
                            tint = Color.White, modifier = Modifier.size(17.dp)
                        )
                    }
                }

                LessonState.LOCKED -> {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(LockedCircleBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${lesson.number}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Title + duration
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    lesson.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (isLocked) TextSecondary else TextPrimary
                )
                Text(lesson.duration, fontSize = 12.sp, color = DurationTextColor)
            }

            // Resume badge (active only)
            if (isActive) {
                Text(
                    stringResource(R.string.course_detail_resume_label),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BlueStart
                )
            }
        }

        // Frosted lock overlay — only shown while the whole course is unpurchased.
        if (blurred) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(FrostedOverlay)
                    .clickable { onClick(lesson.id) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .shadow(4.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.4f))
                        .background(Color(0xFF2B2F38).copy(alpha = 0.85f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = stringResource(R.string.course_detail_locked_description),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}