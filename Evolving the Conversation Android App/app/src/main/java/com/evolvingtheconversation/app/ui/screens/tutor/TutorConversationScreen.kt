package com.evolvingtheconversation.app.ui.screens.tutor

import android.Manifest
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.evolvingtheconversation.app.ai.AIConversationService
import com.evolvingtheconversation.app.ai.AIService
import com.evolvingtheconversation.app.ai.AudioRecorder
import com.evolvingtheconversation.app.ai.StoredMessage
import com.evolvingtheconversation.app.ui.components.BackButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.compose.runtime.getValue
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme

private val BlueStart: Color
    @Composable get() = Color(0xFF1E5EF3)
private val SurfaceBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFFBFBFD)
private val BubbleUser: Color
    @Composable get() = Color(0xFF1E5EF3)
private val BubbleAI: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color(0xFFF0F1F5)
private val TextPrimary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)
private val TextSecondary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)
private val ErrorRed: Color
    @Composable get() = Color(0xFFE53935)
private enum class TutorState { IDLE, RECORDING, THINKING }

@Composable
fun TutorConversationScreen(onBack: () -> Unit = {},
                            onTurnComplete: () -> Unit = {} ) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid }

    var state by remember { mutableStateOf(TutorState.IDLE) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Firestore is the single source of truth for the conversation — this list is never
    // mutated locally, only ever replaced by what fetchHistory()/listenToMessages() return.
    // That's what makes leaving the screen and coming back show exactly where you left off.
    var storedMessages by remember { mutableStateOf<List<StoredMessage>>(emptyList()) }
    var historyLoaded by remember { mutableStateOf(false) }

    val recorder = remember { AudioRecorder(context) }

    // --- Text-to-speech engine for the tutor's replies ---
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }

    // Load this user's saved conversation once, then keep listening for live updates
    // (covers this device writing new turns, and keeps things consistent if the same
    // account is ever open elsewhere).
    LaunchedEffect(uid) {
        val currentUid = uid ?: run {
            errorMessage = "You need to be signed in to talk with your AI tutor."
            return@LaunchedEffect
        }
        storedMessages = AIConversationService.fetchHistory(currentUid)
        historyLoaded = true
        AIConversationService.listenToMessages(currentUid).collect { updated ->
            storedMessages = updated
        }
    }

    fun beginRecording() {
        try {
            recorder.start()
            errorMessage = null
            state = TutorState.RECORDING
        } catch (_: Exception) {
            errorMessage = "Couldn't start the microphone — try again."
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) beginRecording()
        else errorMessage = "Microphone permission is needed to talk with your AI tutor."
    }

    fun stopAndSend() {
        val currentUid = uid
        if (currentUid == null) {
            errorMessage = "You need to be signed in to talk with your AI tutor."
            return
        }

        state = TutorState.THINKING
        scope.launch {
            val bytes = recorder.stopAndGetBytes()
            if (bytes == null) {
                errorMessage = "Didn't catch that — try recording again."
                state = TutorState.IDLE
                return@launch
            }
            try {
                val turn = AIService.converse(
                    uid = currentUid,
                    seedHistory = storedMessages,
                    audioData = bytes
                )

                // Persist both turns. We don't touch storedMessages directly here —
                // the listener from the LaunchedEffect above will pick these up and
                // update the UI, keeping Firestore as the one source of truth.
                AIConversationService.saveMessage(currentUid, turn.userTranscript, isUser = true)
                AIConversationService.saveMessage(currentUid, turn.aiReply, isUser = false)
                errorMessage = null
                onTurnComplete()   // ← +15 XP for this completed exchange

                // Speak the reply aloud
                if (ttsReady) {
                    tts?.speak(turn.aiReply, TextToSpeech.QUEUE_FLUSH, null, "tutorReply")
                }
            } catch (e: Exception) {
                android.util.Log.e("TutorConversation", "converse() failed", e)
                errorMessage = "Error: ${e.message}"
            } finally {
                state = TutorState.IDLE
            }
        }
    }

    fun handleMicTap() {
        when (state) {
            TutorState.IDLE -> {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) beginRecording()
                else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            TutorState.RECORDING -> stopAndSend()
            TutorState.THINKING -> Unit
        }
    }

    LaunchedEffect(storedMessages.size) {
        if (storedMessages.isNotEmpty()) listState.animateScrollToItem(storedMessages.size - 1)
    }

    // Init TTS once, tear everything down on dispose
    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                ttsReady = true
            }
        }
        tts = engine
        onDispose {
            recorder.cancel()
            tts?.stop()
            tts?.shutdown()
            tts = null
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(SurfaceBg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(onBack = onBack)
                Spacer(Modifier.width(14.dp))
                Text("AI Tutor", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            }

            // Conversation
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (historyLoaded && storedMessages.isEmpty()) {
                    item {
                        Text(
                            "Tap the mic and say hello — your tutor is listening.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 40.dp)
                        )
                    }
                }
                items(storedMessages, key = { it.id }) { msg -> ChatRow(msg) }
                if (state == TutorState.THINKING) {
                    item { ThinkingRow() }
                }
            }

            errorMessage?.let { msg ->
                Text(
                    msg,
                    color = ErrorRed,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                )
            }

            // Mic control
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                val bg = if (state == TutorState.RECORDING) ErrorRed else BlueStart
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(bg, CircleShape)
                        .clickable(enabled = state != TutorState.THINKING) { handleMicTap() },
                    contentAlignment = Alignment.Center
                ) {
                    when (state) {
                        TutorState.THINKING -> CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(28.dp)
                        )
                        TutorState.RECORDING -> Icon(
                            Icons.Filled.Stop,
                            contentDescription = "Stop recording",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                        TutorState.IDLE -> Icon(
                            Icons.Outlined.Mic,
                            contentDescription = "Start recording",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatRow(message: StoredMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .wrapContentWidth(if (message.isUser) Alignment.End else Alignment.Start)
                .background(
                    if (message.isUser) BubbleUser else BubbleAI,
                    RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                message.text,
                fontSize = 14.sp,
                color = if (message.isUser) Color.White else TextPrimary,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun ThinkingRow() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier
                .background(
                    BubbleAI,
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text("…thinking", fontSize = 13.sp, color = TextSecondary)
        }
    }
}