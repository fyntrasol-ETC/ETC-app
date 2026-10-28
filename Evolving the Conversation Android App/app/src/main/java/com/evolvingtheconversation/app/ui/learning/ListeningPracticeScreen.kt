package com.evolvingtheconversation.app.ui.screens.learning

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.evolvingtheconversation.app.ui.components.BackButton
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evolvingtheconversation.viewmodel.AuthViewModel
import androidx.compose.runtime.getValue
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme


private val BlueStart      = Color(0xFF1E5EF3)
private val BlueDeep       = Color(0xFF1447D1)
private val BlueLight      = Color(0xFF3B82F6)
private val GreenAccent    = Color(0xFF0CBD85)
private val OrangeAccent   = Color(0xFFFFA800)
private val ErrorRed       = Color(0xFFE53935)
private val SurfaceBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFF4F6FB)
private val CardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val TextPrimary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)
private val TextSecondary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)
private val ShadowAmbient: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0x33000000) else Color(0x14000000)
private val ShadowSpot: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0x40000000) else Color(0x1F000000)

// Fixed (non-theme-aware) text colors used specifically inside the level accordion header.
// The header background (see levelHeaderStyle) is always a light pastel color, in both
// light and dark mode, so the text sitting on top of it must stay dark in both modes too —
// using the theme-aware TextPrimary/TextSecondary here caused near-invisible text in dark mode.
private val LevelHeaderTitleColor    = Color(0xFF111827)
private val LevelHeaderSubtitleColor = Color(0xFF374151)

/** Simple in-screen navigation. No extra nav graph needed for this one screen. */
private sealed class InternalScreen {
    object Main : InternalScreen()
    data class PodcastEpisodes(val seriesId: String) : InternalScreen()
}

/** Formats milliseconds as m:ss, e.g. 68_000L -> "1:08". */
private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

/**
 * Compares the typed answer against the original sentence, word by word.
 * Returns the list of (originalWord, wasTypedCorrectly) plus an accuracy percentage.
 */
private fun compareAnswer(typed: String, original: String): Pair<List<Pair<String, Boolean>>, Int> {
    val typedWords = typed.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val originalWords = original.trim().split(Regex("\\s+"))
    fun clean(word: String) = word.trim('.', ',', '!', '?').lowercase()

    val results = originalWords.mapIndexed { index, word ->
        val typedWord = typedWords.getOrNull(index) ?: ""
        word to (clean(typedWord) == clean(word))
    }
    val correctCount = results.count { it.second }
    val accuracy = if (originalWords.isNotEmpty()) (correctCount * 100) / originalWords.size else 0
    return results to accuracy
}

/** Pastel background + accent color + icon used for each level's accordion header. */
private fun levelHeaderStyle(level: Level): Triple<Color, Color, ImageVector> = when (level) {
    Level.BEGINNER -> Triple(Color(0xFFE3F6EA), Color(0xFF16A34A), Icons.Filled.Eco)
    Level.INTERMEDIATE -> Triple(Color(0xFFFDEAD6), Color(0xFFF97316), Icons.Filled.LocalFireDepartment)
    Level.ADVANCED -> Triple(Color(0xFFFCE4EC), Color(0xFFE11D48), Icons.Filled.Bolt)
}

/** Icon shown in each dialogue card, matched by id. */
private fun dialogueIcon(id: String): ImageVector = when (id) {
    "dlg_airport" -> Icons.Filled.Flight
    "dlg_restaurant" -> Icons.Filled.Restaurant
    "dlg_hotel" -> Icons.Filled.Hotel
    "dlg_shopping" -> Icons.Filled.ShoppingBag
    "dlg_hospital" -> Icons.Filled.LocalHospital
    "dlg_office" -> Icons.Filled.Business
    "dlg_interview" -> Icons.Filled.Person
    "dlg_bank" -> Icons.Filled.AccountBalance
    "dlg_pharmacy" -> Icons.Filled.LocalPharmacy
    "dlg_taxi" -> Icons.Filled.LocalTaxi
    "dlg_gym" -> Icons.Filled.FitnessCenter
    "dlg_school" -> Icons.Filled.School
    "dlg_post_office" -> Icons.Filled.LocalPostOffice
    "dlg_library" -> Icons.Filled.LocalLibrary
    "dlg_salon" -> Icons.Filled.ContentCut
    "dlg_bakery" -> Icons.Filled.BakeryDining
    "dlg_gas_station" -> Icons.Filled.LocalGasStation
    else -> Icons.Outlined.Headphones
}

// ═════════════════════════════════════════════════════════════════════════
// PREMIUM 3D CONTROLS — gradient fill + drop shadow + press animation.
// Used for every tappable "button" so they pop off the flat background
// instead of blending into it.
// ═════════════════════════════════════════════════════════════════════════

/** A round gradient button used for play/pause controls of every size. */
@Composable
private fun GradientCircleButton(
    icon: ImageVector,
    size: Dp,
    iconSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.90f else 1f, label = "circleScale")
    val shadowElevation by animateFloatAsState(if (isPressed) 2f else 10f, label = "circleShadow")

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(
                elevation = shadowElevation.dp,
                shape = CircleShape,
                ambientColor = BlueStart.copy(alpha = 0.35f),
                spotColor = BlueStart.copy(alpha = 0.55f)
            )
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(BlueLight, BlueStart, BlueDeep)))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(iconSize))
    }
}

/** A pill-shaped gradient button used for Start / Check / Next / Finish / Try Again. */
@Composable
private fun PremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "btnScale")
    val shadowElevation by animateFloatAsState(if (isPressed || !enabled) 2f else 10f, label = "btnShadow")
    val shape = RoundedCornerShape(50.dp)

    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(
                elevation = shadowElevation.dp,
                shape = shape,
                ambientColor = BlueStart.copy(alpha = 0.35f),
                spotColor = BlueStart.copy(alpha = 0.55f)
            )
            .clip(shape)
            .background(
                if (enabled) Brush.verticalGradient(listOf(BlueLight, BlueStart, BlueDeep))
                else Brush.verticalGradient(listOf(Color(0xFFCBD5E1), Color(0xFFA6B0C3)))
            )
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick)
            .height(46.dp)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

/** Drop shadow applied to flat white surfaces (cards, accordions) so they lift off the background. */
@Composable
private fun Modifier.cardLift(shape: RoundedCornerShape, elevation: Dp = 8.dp): Modifier =
    this.shadow(elevation = elevation, shape = shape, ambientColor = ShadowAmbient, spotColor = ShadowSpot)

@Composable
fun ListeningPracticeScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current

    // ── EXOPLAYER SETUP ─────────────────────────────────────────────────────
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    // ── NOW PLAYING STATE ────────────────────────────────────────────────────
    var nowPlayingId by remember { mutableStateOf<String?>(null) }
    var nowPlayingTitle by remember { mutableStateOf("Nothing playing yet") }
    var nowPlayingCategory by remember { mutableStateOf("Choose a lesson, podcast, or dialogue") }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var totalDurationMs by remember { mutableStateOf(0L) }

    // ── DEBUG LOGGING ─────────────────────────────────────────────────────
    // Temporary — added to diagnose the "icon flips to pause but no audio
    // plays" issue on podcast episodes. Filter Logcat by "AudioDebug".
    // Safe to remove once the root cause (volume stream / bad asset / stale
    // player state) is confirmed and fixed.
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playingNow: Boolean) {
                isPlaying = playingNow
                android.util.Log.d(
                    "AudioDebug",
                    "isPlaying=$playingNow, volume=${exoPlayer.volume}, currentPos=${exoPlayer.currentPosition}"
                )
            }
            override fun onPlaybackStateChanged(state: Int) {
                val stateName = when (state) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN"
                }
                android.util.Log.d("AudioDebug", "state=$stateName, duration=${exoPlayer.duration}")
                if (state == Player.STATE_READY) {
                    totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
                }
                if (state == Player.STATE_ENDED) {
                    currentPositionMs = totalDurationMs
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("AudioDebug", "Playback error: ${error.errorCodeName} — ${error.message}")
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // Poll the playback position while something is playing.
    LaunchedEffect(isPlaying, nowPlayingId) {
        while (isPlaying) {
            currentPositionMs = exoPlayer.currentPosition
            delay(500)
        }
    }

    /** Starts a new track, or toggles play/pause if the same track is tapped again. */
    fun playTrack(id: String, title: String, category: String, audioFile: String) {
        if (nowPlayingId == id) {
            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
            return
        }
        nowPlayingId = id
        nowPlayingTitle = title
        nowPlayingCategory = category
        currentPositionMs = 0L
        totalDurationMs = 0L
        android.util.Log.d("AudioDebug", "playTrack: id=$id, audioFile=$audioFile")
        val mediaItem = MediaItem.fromUri(Uri.parse("asset:///audio/$audioFile"))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun togglePlayPause() {
        if (nowPlayingId == null) return
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
    }

    // ── INTERNAL NAVIGATION ──────────────────────────────────────────────────
    var internalScreen by remember { mutableStateOf<InternalScreen>(InternalScreen.Main) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {

        // ── TOP BAR ───────────────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(onBack = {
                when (internalScreen) {
                    is InternalScreen.PodcastEpisodes -> internalScreen = InternalScreen.Main
                    InternalScreen.Main -> onBack()
                }
            })
            Spacer(Modifier.width(4.dp))
            Column {
                val title = when (val screen = internalScreen) {
                    is InternalScreen.PodcastEpisodes ->
                        podcastSeries.first { it.id == screen.seriesId }.title
                    InternalScreen.Main -> "Listening Practice"
                }
                Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
                Text("Train your ear", fontSize = 12.sp, color = TextSecondary)
            }
        }

        // ── NOW PLAYING CARD ──────────────────────────────────────────────────
        Card(
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .cardLift(RoundedCornerShape(16.dp), elevation = 10.dp),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GradientCircleButton(
                        icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        size = 48.dp,
                        iconSize = 26.dp,
                        onClick = { togglePlayPause() },
                        contentDescription = "Play or pause"
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("NOW PLAYING", fontSize = 9.sp, color = TextSecondary,
                            fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(nowPlayingTitle, fontWeight = FontWeight.Bold,
                            fontSize = 16.sp, color = TextPrimary)
                        Text(nowPlayingCategory, fontSize = 12.sp, color = TextSecondary)
                    }
                    Icon(Icons.Outlined.VolumeUp, contentDescription = null,
                        tint = TextSecondary, modifier = Modifier.size(20.dp))
                }

                Spacer(Modifier.height(14.dp))

                // Progress bar
                val progress = if (totalDurationMs > 0)
                    (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                else 0f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFF0F0F5))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Brush.horizontalGradient(listOf(BlueLight, BlueStart)))
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(currentPositionMs), fontSize = 11.sp, color = TextSecondary)
                    Text(formatTime(totalDurationMs), fontSize = 11.sp, color = TextSecondary)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        when (val screen = internalScreen) {
            InternalScreen.Main -> MainListeningContent(
                nowPlayingId  = nowPlayingId,
                isPlaying     = isPlaying,
                onPlayLesson  = { lesson ->
                    playTrack(lesson.id, lesson.title, "Audio lesson · ${lesson.level.label}", lesson.audioFile)
                },
                onOpenPodcast = { series -> internalScreen = InternalScreen.PodcastEpisodes(series.id) },
                onPlayDialogue = { dialogue ->
                    playTrack(dialogue.id, dialogue.title, "Dialogue", dialogue.audioFile)
                },
                onPlayDictationAudio = { exercise ->
                    playTrack("dictation_${exercise.id}", "Dictation exercise", "Dictation · ${exercise.level.label}", exercise.audioFile)
                }
            )

            is InternalScreen.PodcastEpisodes -> {
                val series = podcastSeries.first { it.id == screen.seriesId }
                PodcastEpisodesContent(
                    series       = series,
                    nowPlayingId = nowPlayingId,
                    isPlaying    = isPlaying,
                    onPlayEpisode = { episode ->
                        playTrack(episode.id, episode.title, "${series.title} · Podcast", episode.audioFile)
                    }
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// MAIN CONTENT (Audio lessons, Podcasts, Dialogues, Dictation)
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun MainListeningContent(
    nowPlayingId: String?,
    isPlaying: Boolean,
    onPlayLesson: (AudioLesson) -> Unit,
    onOpenPodcast: (PodcastSeries) -> Unit,
    onPlayDialogue: (DialogueItem) -> Unit,
    onPlayDictationAudio: (DictationExercise) -> Unit
) {
    // ── AUDIO LESSONS (accordion, one level open at a time) ─────────────────
    SectionTitle("Audio lessons")
    Spacer(Modifier.height(10.dp))

    var expandedLevel by remember { mutableStateOf<Level?>(Level.INTERMEDIATE) }

    Column(
        modifier            = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Level.entries.forEach { level ->
            LevelAccordion(
                level        = level,
                isExpanded   = expandedLevel == level,
                lessons      = audioLessons.filter { it.level == level },
                nowPlayingId = nowPlayingId,
                isPlaying    = isPlaying,
                onToggle     = { expandedLevel = if (expandedLevel == level) null else level },
                onPlayLesson = onPlayLesson
            )
        }
    }

    Spacer(Modifier.height(24.dp))

    // ── PODCASTS ──────────────────────────────────────────────────────────
    SectionTitle("Podcasts")
    Spacer(Modifier.height(10.dp))

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        podcastSeries.forEach { series ->
            Card(
                modifier  = Modifier
                    .weight(1f)
                    .cardLift(RoundedCornerShape(16.dp), elevation = 8.dp),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(0.dp),
                onClick   = { onOpenPodcast(series) }
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .background(Brush.linearGradient(series.gradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.VolumeUp, contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(series.title, fontWeight = FontWeight.Bold,
                            fontSize = 13.sp, color = TextPrimary)
                        Text("${series.episodes.size} episodes", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(24.dp))

    // ── DIALOGUES (2-column grid with show more / show less) ────────────────
    SectionTitle("Dialogues")
    Spacer(Modifier.height(10.dp))

    var showAllDialogues by remember { mutableStateOf(false) }
    val visibleDialogues = if (showAllDialogues) dialogues else dialogues.take(DIALOGUES_COLLAPSED_COUNT)

    Column(
        modifier            = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        visibleDialogues.chunked(2).forEach { rowItems ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { dialogue ->
                    DialogueCard(
                        dialogue   = dialogue,
                        isSelected = nowPlayingId == dialogue.id,
                        isPlaying  = isPlaying,
                        modifier   = Modifier.weight(1f),
                        onClick    = { onPlayDialogue(dialogue) }
                    )
                }
                // Keep the row's second column reserved when there's an odd item out.
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        Text(
            text = if (showAllDialogues) "Show less" else "Show more",
            color = BlueStart,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAllDialogues = !showAllDialogues }
                .padding(vertical = 6.dp)
        )
    }

    Spacer(Modifier.height(24.dp))

    // ── DICTATION EXERCISE ────────────────────────────────────────────────
    SectionTitle("Dictation exercise")
    Spacer(Modifier.height(10.dp))

    DictationCard(onPlayAudio = onPlayDictationAudio)
}

// ═════════════════════════════════════════════════════════════════════════
// AUDIO LESSONS — expandable "level" accordion
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun LevelAccordion(
    level: Level,
    isExpanded: Boolean,
    lessons: List<AudioLesson>,
    nowPlayingId: String?,
    isPlaying: Boolean,
    onToggle: () -> Unit,
    onPlayLesson: (AudioLesson) -> Unit
) {
    val (headerBg, accentColor, icon) = levelHeaderStyle(level)
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .cardLift(shape, elevation = 8.dp)
            .clip(shape)
    ) {
        Column {
            // Header row — always visible, tinted with the level's pastel color.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .clickable { onToggle() }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .cardLift(RoundedCornerShape(10.dp), elevation = 3.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardWhite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // NOTE: headerBg is always a light pastel color (in both light AND dark
                    // mode — see levelHeaderStyle above), so the text on top of it must stay
                    // dark in both modes too. Using the theme-aware TextPrimary/TextSecondary
                    // here made these labels nearly invisible in dark mode. Fixed dark colors
                    // are used instead so contrast holds regardless of app theme.
                    Text(level.label, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LevelHeaderTitleColor)
                    Text("${lessons.size} lessons", fontSize = 12.sp, color = LevelHeaderSubtitleColor)
                }
                Icon(
                    if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = LevelHeaderSubtitleColor
                )
            }

            // Body — lesson list, only shown when expanded.
            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardWhite)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    lessons.forEach { lesson ->
                        val selected = nowPlayingId == lesson.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onPlayLesson(lesson) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GradientCircleButton(
                                icon = if (selected && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                size = 30.dp,
                                iconSize = 15.dp,
                                onClick = { onPlayLesson(lesson) },
                                contentDescription = "Play ${lesson.title}"
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    lesson.title,
                                    fontSize = 14.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) BlueStart else TextPrimary
                                )
                                Text(lesson.duration, fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// DIALOGUES — 2-column grid card
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun DialogueCard(
    dialogue: DialogueItem,
    isSelected: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Card(
        modifier  = modifier.cardLift(shape, elevation = 6.dp),
        shape     = shape,
        colors    = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = if (isSelected) BorderStroke(1.5.dp, BlueStart) else null,
        onClick   = onClick
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // FIX: previously this only showed Pause while actively playing and fell
            // back to the category icon (Flight, Restaurant, etc.) the instant the
            // track was paused — so a paused dialogue looked like it had no play
            // control at all. Now: selected+playing -> Pause, selected+paused ->
            // PlayArrow (so you can resume), not selected -> category icon.
            GradientCircleButton(
                icon = when {
                    isSelected && isPlaying -> Icons.Filled.Pause
                    isSelected              -> Icons.Filled.PlayArrow
                    else                    -> dialogueIcon(dialogue.id)
                },
                size = 34.dp,
                iconSize = 16.dp,
                onClick = onClick,
                contentDescription = if (isSelected && isPlaying) "Pause ${dialogue.title}" else "Play ${dialogue.title}"
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(dialogue.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                Text(dialogue.duration, fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// PODCAST EPISODES SCREEN (opened when a podcast card is tapped)
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun PodcastEpisodesContent(
    series: PodcastSeries,
    nowPlayingId: String?,
    isPlaying: Boolean,
    onPlayEpisode: (PodcastEpisode) -> Unit
) {
    SectionTitle(series.title)
    Spacer(Modifier.height(10.dp))

    Column(
        modifier            = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        series.episodes.forEach { episode ->
            val selected = nowPlayingId == episode.id
            val shape = RoundedCornerShape(14.dp)
            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .cardLift(shape, elevation = 8.dp),
                shape     = shape,
                colors    = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(0.dp),
                border    = if (selected) BorderStroke(1.5.dp, BlueStart) else null,
                onClick   = { onPlayEpisode(episode) }
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GradientCircleButton(
                        icon = if (selected && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        size = 36.dp,
                        iconSize = 17.dp,
                        onClick = { onPlayEpisode(episode) },
                        contentDescription = "Play ${episode.title}"
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(episode.title, fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp, color = TextPrimary)
                        Text(episode.duration, fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// DICTATION EXERCISE (self-contained state machine inside one card)
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun DictationCard(onPlayAudio: (DictationExercise) -> Unit) {
    val authViewModel: AuthViewModel = viewModel()
    var selectedLevel by remember { mutableStateOf<Level?>(null) }
    var started by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableStateOf(0) }
    var typedAnswer by remember { mutableStateOf("") }
    var checked by remember { mutableStateOf(false) }
    var wordResults by remember { mutableStateOf<List<Pair<String, Boolean>>>(emptyList()) }
    var accuracyHistory by remember { mutableStateOf<List<Int>>(emptyList()) }

    fun reset() {
        selectedLevel = null
        started = false
        finished = false
        currentIndex = 0
        typedAnswer = ""
        checked = false
        wordResults = emptyList()
        accuracyHistory = emptyList()
    }

    val exercises = selectedLevel?.let { level -> dictationExercises.filter { it.level == level } } ?: emptyList()
    val cardShape = RoundedCornerShape(16.dp)

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .cardLift(cardShape, elevation = 10.dp),
        shape     = cardShape,
        colors    = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            when {
                // ── FINISHED: FINAL SCORE ────────────────────────────────
                finished -> {
                    val correctAnswers = accuracyHistory.count { it == 100 }
                    val incorrectAnswers = accuracyHistory.size - correctAnswers
                    val overallAccuracy = if (accuracyHistory.isNotEmpty())
                        accuracyHistory.sum() / accuracyHistory.size else 0

                    Text("Final Score", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                    Spacer(Modifier.height(10.dp))
                    Text("Accuracy: $overallAccuracy%", fontSize = 14.sp, color = TextSecondary)
                    Text("Correct answers: $correctAnswers", fontSize = 14.sp, color = GreenAccent)
                    Text("Incorrect answers: $incorrectAnswers", fontSize = 14.sp, color = ErrorRed)
                    Spacer(Modifier.height(14.dp))
                    PremiumButton(text = "Try Again", onClick = { reset() })
                }

                // ── IN PROGRESS: CURRENT SENTENCE ────────────────────────
                started && selectedLevel != null -> {
                    val exercise = exercises[currentIndex]

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        val (_, accentColor, icon) = levelHeaderStyle(exercise.level)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(exercise.level.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = accentColor)
                        }
                        Text(
                            "Sentence ${currentIndex + 1} of ${exercises.size}",
                            fontSize = 12.sp, color = TextSecondary
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GradientCircleButton(
                            icon = Icons.Outlined.VolumeUp,
                            size = 40.dp,
                            iconSize = 18.dp,
                            onClick = { onPlayAudio(exercise) },
                            contentDescription = "Play sentence"
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Listen, then type what you hear.", fontSize = 13.sp, color = TextSecondary)
                    }

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value         = typedAnswer,
                        onValueChange = { typedAnswer = it },
                        enabled       = !checked,
                        placeholder   = { Text("Type the sentence here") },
                        modifier      = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )

                    Spacer(Modifier.height(12.dp))

                    if (!checked) {
                        PremiumButton(
                            text = "Check",
                            onClick = {
                                val (results, accuracy) = compareAnswer(typedAnswer, exercise.sentence)
                                wordResults = results
                                accuracyHistory = accuracyHistory + accuracy
                                checked = true
                                authViewModel.recordDictationAnswer(accuracy == 100)
                            }
                        )
                    } else {
                        val accuracy = accuracyHistory.last()
                        Text("Accuracy: $accuracy%", fontWeight = FontWeight.Bold,
                            fontSize = 14.sp, color = if (accuracy == 100) GreenAccent else ErrorRed)
                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = buildAnnotatedString {
                                wordResults.forEachIndexed { i, (word, correct) ->
                                    withStyle(SpanStyle(color = if (correct) GreenAccent else ErrorRed)) {
                                        append(word)
                                    }
                                    if (i != wordResults.lastIndex) append(" ")
                                }
                            },
                            fontSize = 14.sp
                        )

                        Spacer(Modifier.height(6.dp))
                        Text("Correct answer: ${exercise.sentence}", fontSize = 12.sp, color = TextSecondary)

                        Spacer(Modifier.height(14.dp))
                        PremiumButton(
                            text = if (currentIndex == exercises.lastIndex) "Finish" else "Next",
                            onClick = {
                                if (currentIndex == exercises.lastIndex) {
                                    finished = true
                                } else {
                                    currentIndex += 1
                                    typedAnswer = ""
                                    checked = false
                                }
                            }
                        )
                    }
                }

                // ── NOT STARTED: LEVEL PICKER ────────────────────────────
                else -> {
                    Text("Listen and type what you hear. 4 sentences per level with instant scoring.",
                        fontSize = 13.sp, color = TextSecondary, lineHeight = 20.sp)
                    Spacer(Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Level.entries.forEach { level ->
                            val selected = selectedLevel == level
                            val chipShape = RoundedCornerShape(50.dp)
                            Box(
                                modifier = Modifier
                                    .let { if (selected) it.cardLift(chipShape, elevation = 5.dp) else it }
                                    .clip(chipShape)
                                    .background(if (selected) level.badgeColor else Color(0xFFF0F0F5))
                                    .clickable { selectedLevel = level }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    level.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) Color.White else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    PremiumButton(
                        text = "Start",
                        enabled = selectedLevel != null,
                        leadingIcon = Icons.Outlined.VolumeUp,
                        onClick = { started = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp,
        color = TextPrimary, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth())
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
fun ListeningPracticeScreenPreview() { ListeningPracticeScreen() }