package com.evolvingtheconversation.app.ui.screens.onboarding


import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Preview(
    name = "Splash Screen - Phone View",
    widthDp = 360,
    heightDp = 740,
    showBackground = true
)
@Composable
fun SplashScreenPreview() {
    SplashScreen(onFinish = {})
}

@Composable
fun SplashScreen(onFinish: () -> Unit) {

    LaunchedEffect(Unit) {
        delay(6000) //
        onFinish()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ripples")

    // 🌀 Ripple 1 Animation
    val rippleScale1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(animation = tween(2500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "r1"
    )
    val rippleAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(animation = tween(2500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "a1"
    )

    // 🌀 Ripple 2 Animation (Delayed)
    val rippleScale2 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(animation = tween(2500, delayMillis = 1250, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "r2"
    )
    val rippleAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(animation = tween(2500, delayMillis = 1250, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "a2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E5EF3), // Bright Royal Blue
                        Color(0xFF1646C1),
                        Color(0xFF0B2364)  // Deep Navy Bottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        // 🌀 PULSING RIPPLE RINGS
        Box(
            modifier = Modifier
                .size(360.dp)
                .scale(rippleScale1)
                .alpha(rippleAlpha1)
                .border(1.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(120.dp))
        )
        Box(
            modifier = Modifier
                .size(360.dp)
                .scale(rippleScale2)
                .alpha(rippleAlpha2)
                .border(1.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(120.dp))
        )

        // 🔲 VISIBLE OUTER SQUIRCLE CARD CONTAINER
        Box(
            modifier = Modifier
                .size(340.dp)
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(90.dp))
                .border(1.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(90.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {

                // 💬 WHITE SQUIRCLE LOGO WITH VECTOR CHAT BUBBLE
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color.White, RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(44.dp)) {
                        val path = Path().apply {
                            addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height * 0.9f))
                            moveTo(size.width * 0.2f, size.height * 0.8f)
                            lineTo(size.width * 0.05f, size.height * 0.98f)
                            lineTo(size.width * 0.4f, size.height * 0.85f)
                        }
                        drawPath(
                            path = path,
                            color = Color(0xFF1E5EF3),
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 🔤 TYPOGRAPHY
                Text(
                    text = "Evolving The\nConversation",
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Learn English with Confidence",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                // ⚪⚪⚪ BOUNCING DOTS
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val dotAnim = infiniteTransition.animateFloat(
                            initialValue = -5f,
                            targetValue = 5f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(450, delayMillis = index * 150, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "dot_$index"
                        )

                        Box(
                            modifier = Modifier
                                .offset(y = dotAnim.value.dp)
                                .size(8.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
                }
            }
        }
    }
}