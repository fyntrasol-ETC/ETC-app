package com.evolvingtheconversation.app.ui.screens.learning

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.evolvingtheconversation.app.data.CourseCatalog
import kotlinx.coroutines.delay
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme

// ─── Colour tokens ────────────────────────────────────────────────────────────
private val BlueAccent      = Color(0xFF1E5EF3)
private val BlueMuted       = Color(0xFF5B87F5)
private val GreenAccent     = Color(0xFF0CBD85)
private val GreenDark       = Color(0xFF0A9E70)
private val GreenShadow     = Color(0x660CBD85)
private val DarkPlayer      = Color(0xFF111318)
private val SurfaceBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFF4F6FB)
private val CardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val TextPrimary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)
private val TextSecondary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)
private val BorderColor: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color(0xFFE5E7EB)

// New — for the transcript card's inline gradient
private val TranscriptCardTop: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val TranscriptCardBottom: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF232326) else Color(0xFFFAFBFF)

/** Same tinted-shadow trick used across the app's other "premium" screens. */
private fun Modifier.premiumLift(
    shape: RoundedCornerShape,
    elevation: androidx.compose.ui.unit.Dp = 10.dp,
    tint: Color = Color.Black
): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = tint.copy(alpha = 0.16f),
    spotColor = tint.copy(alpha = 0.24f)
)

private fun formatTime(ms: Long): String {
    if (ms < 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@Composable
fun VideoLessonScreen(
    courseId            : String,
    lessonId            : String,
    isInitiallyCompleted: Boolean = false,   // pass in the persisted completion state (e.g. from Firestore)
    onBack              : () -> Unit = {},
    onMarkComplete      : () -> Unit = {}
) {
    val course = CourseCatalog.getCourse(courseId) ?: return
    val lessonIndex = course.lessons.indexOfFirst { it.id == lessonId }
    val lesson = course.lessons.getOrNull(lessonIndex) ?: return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isPlaying   by remember { mutableStateOf(false) }
    var progress    by remember { mutableStateOf(0f) }          // 0f..1f
    var positionMs  by remember { mutableStateOf(0L) }
    var durationMs  by remember { mutableStateOf(0L) }
    // Seeded from the persisted flag so re-opening an already-completed lesson
    // — no matter how many times — still shows it as complete.
    var isComplete  by remember(lesson.id, isInitiallyCompleted) { mutableStateOf(isInitiallyCompleted) }
    var isDragging  by remember { mutableStateOf(false) }       // pause position updates while user drags

    // ── ExoPlayer setup, tied to this lesson's asset file ──────────────────
    val exoPlayer = remember(lesson.id) {
        ExoPlayer.Builder(context).build().apply {
            val assetUri = "asset:///${lesson.videoAsset}"   // e.g. asset:///videos/spoken_english_intro_1.mp4
            setMediaItem(MediaItem.fromUri(assetUri))
            prepare()

            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            })
        }
    }

    // Release the player when this lesson changes or the screen leaves composition.
    DisposableEffect(lesson.id) {
        onDispose { exoPlayer.release() }
    }

    // Pause playback when the app backgrounds, resume awareness on foreground
    // (does NOT auto-resume playback — just stops it from playing silently in background).
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Poll playback position while playing, drive the scrubber + timestamp.
    LaunchedEffect(exoPlayer, isDragging) {
        while (true) {
            if (!isDragging) {
                durationMs = exoPlayer.duration.coerceAtLeast(0L)
                positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                progress = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
            }
            delay(250)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(CardWhite)) {

        // ── VIDEO PLAYER ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .shadow(elevation = 12.dp, ambientColor = Color.Black.copy(alpha = 0.4f), spotColor = Color.Black.copy(alpha = 0.4f))
                .background(DarkPlayer)
        ) {
            AndroidView(
                factory = {
                    PlayerView(context).apply {
                        player = exoPlayer
                        useController = false   // we're driving our own custom controls below
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Back button
            IconButton(
                onClick  = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 36.dp, start = 10.dp)
                    .size(32.dp)
                    .shadow(6.dp, RoundedCornerShape(9.dp), spotColor = Color.Black.copy(alpha = 0.5f))
                    .background(
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.1f))),
                        RoundedCornerShape(9.dp)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(9.dp))
            ) {
                Icon(
                    Icons.Outlined.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint     = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Play / pause button
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .align(Alignment.Center)
                    .shadow(14.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.6f))
                    .background(
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.12f))),
                        CircleShape
                    )
                    .border(1.2.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint     = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Progress bar + controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 8.dp)
            ) {
                Slider(
                    value         = progress,
                    onValueChange = { newValue ->
                        isDragging = true
                        progress = newValue
                    },
                    onValueChangeFinished = {
                        val target = (progress * durationMs).toLong()
                        exoPlayer.seekTo(target)
                        isDragging = false
                    },
                    modifier      = Modifier.fillMaxWidth().height(12.dp),
                    colors        = SliderDefaults.colors(
                        thumbColor                = BlueAccent,
                        activeTrackColor          = BlueAccent,
                        inactiveTrackColor        = Color.White.copy(alpha = 0.30f),
                        activeTickColor           = Color.Transparent,
                        inactiveTickColor         = Color.Transparent
                    )
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${formatTime(positionMs)} / ${formatTime(durationMs)}",
                        fontSize   = 10.sp,
                        color      = Color.White.copy(alpha = 0.70f),
                        fontWeight = FontWeight.Normal
                    )
                    Box(
                        modifier = Modifier
                            .shadow(3.dp, RoundedCornerShape(4.dp), spotColor = Color.Black.copy(alpha = 0.3f))
                            .border(1.dp, Color.White.copy(alpha = 0.50f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            "1.0×",
                            fontSize   = 10.sp,
                            color      = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // ── CONTENT AREA ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .background(CardWhite)
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 20.dp)
        ) {

            Text(
                lesson.title,
                fontWeight = FontWeight.Bold,
                fontSize   = 19.sp,
                color      = TextPrimary,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(3.dp))

            Text(
                "Lesson ${lessonIndex + 1} of ${course.lessons.size} · ${course.title}",
                fontSize   = 12.sp,
                fontWeight = FontWeight.Normal,
                color      = TextSecondary,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))

            // Only Transcript remains — no tab switcher needed for a single section.
            Box(
                modifier = Modifier
                    .premiumLift(RoundedCornerShape(50.dp), elevation = 6.dp, tint = BlueAccent)
                    .clip(RoundedCornerShape(50.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF4F8EF7), BlueAccent)))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    "Transcript",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White
                )
            }

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .premiumLift(RoundedCornerShape(14.dp), elevation = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                    .background(Brush.verticalGradient(listOf(TranscriptCardTop, TranscriptCardBottom)))
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Welcome back. In this lesson we focus on the " +
                            "rhythm of natural speech — linking words " +
                            "together, stressing the right syllables, and " +
                            "softening sounds the way fluent speakers do...",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color      = BlueMuted,
                    lineHeight = 22.sp,
                    textAlign  = TextAlign.Start,
                    modifier   = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── MARK COMPLETE (only action button) ─────────────────────────────
            val btnColor by animateColorAsState(
                targetValue = if (isComplete) GreenDark else GreenAccent,
                label       = "btnColor"
            )
            val btnGradientEnd by animateColorAsState(
                targetValue = if (isComplete) Color(0xFF076B4F) else Color(0xFF089468),
                label       = "btnGradientEnd"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation    = 18.dp,
                        shape        = RoundedCornerShape(50.dp),
                        ambientColor = GreenShadow,
                        spotColor    = GreenShadow
                    )
            ) {
                Button(
                    onClick = {
                        // Once completed it stays completed, even on repeated visits.
                        if (!isComplete) {
                            isComplete = true
                            onMarkComplete()
                        }
                    },
                    shape    = RoundedCornerShape(50.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .background(
                            Brush.verticalGradient(listOf(btnColor, btnGradientEnd)),
                            RoundedCornerShape(50.dp)
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.5f), Color.Transparent)),
                            shape = RoundedCornerShape(50.dp)
                        )
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint     = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isComplete) "Completed!" else "Mark complete",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp,
                        color      = Color.White,
                        textAlign  = TextAlign.Center
                    )
                }
            }
        }
    }
}