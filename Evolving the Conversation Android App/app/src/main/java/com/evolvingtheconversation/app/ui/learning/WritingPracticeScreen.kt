package com.evolvingtheconversation.app.ui.screens.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evolvingtheconversation.app.ai.AIService
import com.evolvingtheconversation.app.ai.SuggestionDTO
import com.evolvingtheconversation.app.ai.WritingFeedbackResponse
import com.evolvingtheconversation.app.ui.components.BackButton
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme

private val BlueStart     = Color(0xFF1E5EF3)
private val BlueEnd       = Color(0xFF3B7CFB)
private val GreenAccent   = Color(0xFF0CBD85)
private val OrangeAccent  = Color(0xFFFFA800)
private val ErrorRed      = Color(0xFFE53935)
private val SurfaceBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFF4F6FB)
private val CardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val TextPrimary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)
private val TextSecondary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)
private val DarkInput     = Color(0xFF1C1C2E)   // already dark — unchanged in both themes

/**
 * Pool of writing prompts. A random prompt is picked each time the screen is
 * entered (see `remember { WRITING_PROMPTS.random() }` below), so re-opening
 * the screen shows a different prompt in shuffled order.
 */
private val WRITING_PROMPTS: List<String> = listOf(
    "Some people prefer to study alone, others in groups. Discuss both views and give your opinion.",
    "Technology has made communication easier but relationships weaker. Do you agree or disagree?",
    "Should university education be free for everyone? Discuss.",
    "Describe a skill you'd like to learn and explain why it's important to you.",
    "Many people believe social media does more harm than good. What is your view?",
    "Discuss the advantages and disadvantages of working from home.",
    "Some people think success comes from hard work, others believe it comes from luck. Discuss both views.",
    "Should governments invest more in public transport than roads? Give your opinion."
)

@Composable
fun WritingPracticeScreen(onBack: () -> Unit = {}) {
    // Picked once per composition (i.e. once per screen visit) so it changes
    // every time the user navigates back in and re-opens this screen.
    val writingPrompt = remember { WRITING_PROMPTS.random() }

    var essayText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<WritingFeedbackResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val wordCount = remember(essayText) {
        essayText.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    }

    fun submitEssay() {
        if (essayText.isBlank()) return
        isSubmitting = true
        errorMessage = null
        scope.launch {
            try {
                result = AIService.analyzeWriting(essayText, writingPrompt)
            } catch (e: Exception) {
                errorMessage = "Couldn't analyze your essay — please try again."
            } finally {
                isSubmitting = false
            }
        }
    }

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
            BackButton(onBack = onBack)
            Spacer(Modifier.width(4.dp))
            Column {
                Text("Writing Practice", fontWeight = FontWeight.Bold,
                    fontSize = 20.sp, color = TextPrimary)
                Text("Write & get AI feedback", fontSize = 12.sp, color = TextSecondary)
            }
        }

        // ── WRITING PROMPT (premium elevated card) ──────────────────────────────
        Card(
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = BlueStart.copy(alpha = 0.18f),
                    spotColor = BlueStart.copy(alpha = 0.28f)
                ),
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("WRITING PROMPT", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = BlueStart, letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text(
                    "\"$writingPrompt\"",
                    fontSize   = 14.sp,
                    color      = TextPrimary,
                    lineHeight = 22.sp,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── YOUR ESSAY ────────────────────────────────────────────────────────
        Text("Your essay", fontWeight = FontWeight.Bold,
            fontSize = 17.sp, color = TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp))

        Spacer(Modifier.height(10.dp))

        Card(
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = Color.Black.copy(alpha = 0.25f),
                    spotColor = Color.Black.copy(alpha = 0.35f)
                ),
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = DarkInput),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            TextField(
                value         = essayText,
                onValueChange = { essayText = it },
                modifier      = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 160.dp),
                placeholder   = {
                    Text("Start writing here...", color = Color.White.copy(alpha = 0.35f),
                        fontSize = 14.sp)
                },
                colors        = TextFieldDefaults.colors(
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor        = Color.White,
                    unfocusedTextColor      = Color.White,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("$wordCount / 250 words", fontSize = 12.sp, color = TextSecondary)
            Text("Auto-saved", fontSize = 12.sp,
                color = GreenAccent, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(14.dp))

        // ── SUBMIT BUTTON (premium elevated gradient pill) ──────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(50.dp),
                    ambientColor = BlueStart.copy(alpha = 0.35f),
                    spotColor = BlueStart.copy(alpha = 0.45f)
                )
                .clip(RoundedCornerShape(50.dp))
                .background(Brush.verticalGradient(listOf(BlueEnd, BlueStart)))
        ) {
            Button(
                onClick  = { submitEssay() },
                enabled  = !isSubmitting && essayText.isNotBlank(),
                shape    = RoundedCornerShape(50.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.Outlined.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Submit", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                }
            }
        }

        errorMessage?.let {
            Text(it, color = ErrorRed, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }

        Spacer(Modifier.height(24.dp))

        // ── AI FEEDBACK ───────────────────────────────────────────────────────
        result?.let { r ->
            Text("AI feedback", fontWeight = FontWeight.Bold,
                fontSize = 17.sp, color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp))

            Spacer(Modifier.height(10.dp))

            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = GreenAccent.copy(alpha = 0.20f),
                        spotColor = GreenAccent.copy(alpha = 0.30f)
                    ),
                shape     = RoundedCornerShape(18.dp),
                colors    = CardDefaults.cardColors(containerColor = Color(0xFFEAFFF7)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier          = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .shadow(6.dp, RoundedCornerShape(10.dp), spotColor = GreenAccent.copy(alpha = 0.4f))
                            .clip(RoundedCornerShape(10.dp))
                            .background(GreenAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null,
                            tint = GreenAccent, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(r.feedbackTitle, fontWeight = FontWeight.Bold,
                            fontSize = 14.sp, color = TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            r.feedbackMessage,
                            fontSize   = 13.sp,
                            color      = TextSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            if (r.suggestions.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))

                Text("Grammar suggestions", fontWeight = FontWeight.Bold,
                    fontSize = 17.sp, color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp))

                Spacer(Modifier.height(10.dp))

                Column(
                    modifier            = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    r.suggestions.forEach { s ->
                        SuggestionRow(s)
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(s: SuggestionDTO) {
    val accentColor = if (s.severity == "error") ErrorRed else OrangeAccent

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.14f)
            ),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text("!", fontSize = 14.sp,
                    color = accentColor, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("\"${s.original}\" → \"${s.corrected}\"", fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp, color = TextPrimary)
                Text("${s.type} · line ${s.line}", fontSize = 12.sp, color = TextSecondary)
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Check,
                    contentDescription = "Accept",
                    tint               = TextSecondary,
                    modifier           = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
fun WritingPracticeScreenPreview() { WritingPracticeScreen() }