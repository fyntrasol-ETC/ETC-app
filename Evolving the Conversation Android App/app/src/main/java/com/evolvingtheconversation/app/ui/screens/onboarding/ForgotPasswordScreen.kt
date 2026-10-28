package com.evolvingtheconversation.app.ui.screens.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evolvingtheconversation.app.ui.components.BackButton
import androidx.compose.runtime.getValue
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme
import com.evolvingtheconversation.app.R

private val Bg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFF4F4F8)
private val Blue      = Color(0xFF1E5EF3)   // brand accent — unchanged
private val TextDark: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF07112A)
private val TextGray: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA4AF) else Color(0xFF626B80)
private val IconGray: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF8A93A6) else Color(0xFF9CA4B6)
private val BorderCol: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2C2C2E) else Color(0xFFE2E4E9)
private val CardWhite: Color   // was inline Color.White on the email field
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val IconCircleBg: Color   // was inline Color(0xFFE8EDFF), the lock icon backdrop
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1A2740) else Color(0xFFE8EDFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit = {},
    onSendResetLink: (String) -> Unit = {},   // ✅ now passes the typed email back
    isLoading: Boolean = false,               // ✅ optional: show a spinner while Firebase call is in flight
    errorMessage: String? = null              // ✅ optional: show inline error (e.g. "invalid email")
) {
    var emailInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ── Top bar with back button ──────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth()) {
            BackButton(onBack = onBack, modifier = Modifier.align(Alignment.CenterStart))
        }

        Spacer(modifier = Modifier.height(48.dp))

        // ── Lock refresh icon in soft blue circle ─────────────────────────────
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(IconCircleBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(44.dp)) {
                val strokePx = 2.8.dp.toPx()
                val cx = size.width / 2f
                val cy = size.height / 2f

                // Lock body
                drawRoundRect(
                    color = Blue,
                    topLeft = androidx.compose.ui.geometry.Offset(cx - 9.dp.toPx(), cy - 2.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(18.dp.toPx(), 13.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                    style = Stroke(width = strokePx)
                )
                // Lock shackle
                val shackle = Path().apply {
                    moveTo(cx - 5.dp.toPx(), cy - 2.dp.toPx())
                    lineTo(cx - 5.dp.toPx(), cy - 7.dp.toPx())
                    cubicTo(cx - 5.dp.toPx(), cy - 11.dp.toPx(), cx + 5.dp.toPx(), cy - 11.dp.toPx(), cx + 5.dp.toPx(), cy - 7.dp.toPx())
                    lineTo(cx + 5.dp.toPx(), cy - 2.dp.toPx())
                }
                drawPath(shackle, Blue, style = Stroke(width = strokePx, cap = androidx.compose.ui.graphics.StrokeCap.Round))

                // Refresh arrow (arc) around the lock — clockwise bottom-right arc
                val arcPath = Path().apply {
                    val r = 15.dp.toPx()
                    // Draw arc from ~200° to ~340°
                    addArc(
                        oval = androidx.compose.ui.geometry.Rect(cx - r, cy - r, cx + r, cy + r),
                        startAngleDegrees = 150f,
                        sweepAngleDegrees = 210f
                    )
                }
                drawPath(arcPath, Blue, style = Stroke(width = strokePx, cap = androidx.compose.ui.graphics.StrokeCap.Round))

                // Arrow head at end of arc (bottom-right)
                val arrowTipX = cx + 14.dp.toPx()
                val arrowTipY = cy + 6.dp.toPx()
                val arrowPath = Path().apply {
                    moveTo(arrowTipX - 4.dp.toPx(), arrowTipY - 2.dp.toPx())
                    lineTo(arrowTipX, arrowTipY)
                    lineTo(arrowTipX - 2.dp.toPx(), arrowTipY + 4.dp.toPx())
                }
                drawPath(arrowPath, Blue, style = Stroke(width = strokePx, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Title + subtitle ──────────────────────────────────────────────────
        Text(
            text = stringResource(R.string.forgot_password_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.forgot_password_subtitle),
            fontSize = 15.sp,
            color = TextGray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── Email label ───────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.forgot_password_email_label),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Canvas draw lambdas are DrawScope, NOT @Composable — resolve the
        // theme-aware color here (composable context) and pass a plain Color in.
        val iconColor = IconGray

        // ── Email field ───────────────────────────────────────────────────────
        OutlinedTextField(
            value = emailInput,
            onValueChange = { emailInput = it },
            placeholder = { Text(stringResource(R.string.forgot_password_email_placeholder), color = iconColor) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            isError = errorMessage != null,
            leadingIcon = {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val strokePx = 2.dp.toPx()
                    drawRoundRect(color = iconColor, style = Stroke(width = strokePx))
                    val flap = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width * 0.5f, size.height * 0.5f)
                        lineTo(size.width, 0f)
                    }
                    drawPath(flap, iconColor, style = Stroke(width = strokePx))
                }
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = CardWhite,
                unfocusedBorderColor = BorderCol,
                focusedBorderColor = Blue
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        // ── Inline error message (optional) ─────────────────────────────────
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = errorMessage,
                    fontSize = 13.sp,
                    color = Color(0xFFDC2626)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Send Reset Link button ────────────────────────────────────────────
        Button(
            onClick = {
                val trimmedEmail = emailInput.trim()
                if (trimmedEmail.isNotEmpty()) {
                    onSendResetLink(trimmedEmail)   // ✅ email now actually gets passed up
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(4.dp, RoundedCornerShape(18.dp),
                    ambientColor = Blue.copy(alpha = 0.4f), spotColor = Blue),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(stringResource(R.string.forgot_password_send_button), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}