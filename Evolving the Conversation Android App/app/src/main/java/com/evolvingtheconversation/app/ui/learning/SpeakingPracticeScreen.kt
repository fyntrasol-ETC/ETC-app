package com.evolvingtheconversation.app.ui.screens.learning

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.evolvingtheconversation.app.ai.AIService
import com.evolvingtheconversation.app.ai.AudioRecorder
import com.evolvingtheconversation.app.ai.SpeakingFeedback
import com.evolvingtheconversation.app.ui.components.BackButton
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evolvingtheconversation.viewmodel.AuthViewModel
import androidx.compose.runtime.getValue
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme

private val BlueStart     = Color(0xFF1E5EF3)
private val BlueEnd       = Color(0xFF1E3A8A)
private val GreenAccent   = Color(0xFF0CBD85)
private val ErrorRed      = Color(0xFFE53935)
private val SurfaceBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFF4F6FB)
private val CardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val SoftCardBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF23232A) else Color(0xFFEEF0F5)
private val TrackGrey: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF3A3D45) else Color(0xFFE5E7EB)
private val TextPrimary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)
private val TextSecondary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)

private enum class RecState { IDLE, RECORDING, ANALYZING }

/**
 * Pool of speaking prompts. A random prompt is picked each time the screen is
 * entered, so re-opening the screen shows a different prompt in shuffled order.
 */
private val SPEAKING_PROMPTS: List<String> = listOf(
    "Describe a place you'd love to visit and explain why it appeals to you.",
    "Talk about a book or movie that changed how you think.",
    "Describe your ideal weekend and what makes it relaxing for you.",
    "Talk about a challenge you overcame and what you learned from it.",
    "Describe a person who has influenced your life and explain how.",
    "Talk about a tradition from your culture that you find meaningful.",
    "Describe your dream job and why it appeals to you.",
    "Talk about a time you had to make a difficult decision."
)

@Composable
fun SpeakingPracticeScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recorder = remember { AudioRecorder(context) }
    val authViewModel: AuthViewModel = viewModel()

    // Picked once per screen visit so it changes every time the user backs
    // out and re-opens this screen.
    val prompt = remember { SPEAKING_PROMPTS.random() }

    var state by remember { mutableStateOf(RecState.IDLE) }
    var feedback by remember { mutableStateOf<SpeakingFeedback?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun beginRecording() {
        try {
            recorder.start()
            errorMessage = null
            feedback = null
            state = RecState.RECORDING
        } catch (_: Exception) {
            errorMessage = "Couldn't start the microphone — try again."
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) beginRecording()
        else errorMessage = "Microphone permission is needed for speaking practice."
    }

    fun stopAndAnalyze() {
        state = RecState.ANALYZING
        scope.launch {
            val bytes = recorder.stopAndGetBytes()
            if (bytes == null) {
                errorMessage = "Didn't catch that — try recording again."
                state = RecState.IDLE
                return@launch
            }
            try {
                feedback = AIService.judgePronunciation(bytes, prompt)
                errorMessage = null
                feedback?.let { authViewModel.recordSpeakingConfidence(it.confidenceScore) } // record daily skill stat
            } catch (e: Exception) {
                errorMessage = "Something went wrong analyzing your recording."
            } finally {
                state = RecState.IDLE
            }
        }
    }

    fun handleMicTap() {
        when (state) {
            RecState.IDLE -> {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) beginRecording()
                else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            RecState.RECORDING -> stopAndAnalyze()
            RecState.ANALYZING -> Unit
        }
    }

    DisposableEffect(Unit) { onDispose { recorder.cancel() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(onBack = onBack)
            Spacer(Modifier.width(4.dp))
            Column {
                Text("Speaking Practice", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
                Text("Talk, record & improve", fontSize = 12.sp, color = TextSecondary)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = BlueStart.copy(alpha = 0.18f),
                    spotColor = BlueStart.copy(alpha = 0.28f)
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Text(
                "\"$prompt\"",
                modifier = Modifier.padding(16.dp),
                fontSize = 14.sp, color = TextPrimary, lineHeight = 22.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            val bg = when (state) {
                RecState.RECORDING -> ErrorRed
                else -> BlueStart
            }
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .shadow(
                        elevation = 18.dp,
                        shape = CircleShape,
                        ambientColor = bg.copy(alpha = 0.35f),
                        spotColor = bg.copy(alpha = 0.45f)
                    )
                    .clip(CircleShape)
                    .background(bg)
                    .clickable(enabled = state != RecState.ANALYZING) { handleMicTap() },
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    RecState.ANALYZING -> CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                    RecState.RECORDING -> Icon(Icons.Filled.Stop, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(36.dp))
                    RecState.IDLE -> Icon(Icons.Outlined.Mic, contentDescription = "Record", tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                when (state) {
                    RecState.IDLE -> "Tap to record"
                    RecState.RECORDING -> "Tap to stop"
                    RecState.ANALYZING -> "Analyzing..."
                },
                fontSize = 13.sp, color = TextSecondary
            )
        }

        errorMessage?.let {
            Text(it, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
        }

        Spacer(Modifier.height(24.dp))

        feedback?.let { fb ->

            // ── WHAT YOU SAID ────────────────────────────────────────────────
            if (fb.transcript.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = Color.Black.copy(alpha = 0.06f),
                            spotColor = Color.Black.copy(alpha = 0.10f)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftCardBg),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "What you said",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "\"${fb.transcript}\"",
                            fontSize = 14.sp,
                            color = TextPrimary,
                            lineHeight = 21.sp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
            }

            // ── CORRECT LINE ─────────────────────────────────────────────────
            if (fb.correctedLine.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = Color.Black.copy(alpha = 0.06f),
                            spotColor = Color.Black.copy(alpha = 0.10f)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftCardBg),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Correct line",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "\"${fb.correctedLine}\"",
                            fontSize = 14.sp,
                            color = GreenAccent,
                            lineHeight = 21.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            // ── CONFIDENCE SCORE + MISTAKES ─────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = Color.Black.copy(alpha = 0.08f),
                        spotColor = Color.Black.copy(alpha = 0.14f)
                    ),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Confidence score",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { fb.confidenceScore / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50)),
                        color = GreenAccent,
                        trackColor = TrackGrey,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${fb.confidenceScore}", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = TextPrimary)
                        Text("/100", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(fb.summary, fontSize = 13.sp, color = TextSecondary)

                    if (fb.mistakes.isNotEmpty()) {
                        Spacer(Modifier.height(18.dp))
                        fb.mistakes.forEach { m ->
                            Column(Modifier.padding(vertical = 6.dp)) {
                                Text(m.issue, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                Spacer(Modifier.height(2.dp))
                                Text(m.tip, fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}