package com.evolvingtheconversation.app.ui.screens.learning

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evolvingtheconversation.viewmodel.AuthResult
import com.evolvingtheconversation.viewmodel.AuthViewModel
import com.evolvingtheconversation.app.ui.components.BackButton
import androidx.compose.runtime.getValue
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme
import com.evolvingtheconversation.app.R

private val Bg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFF4F4F8)
private val Blue     = Color(0xFF1E5EF3)   // brand accent — unchanged
private val Red      = Color(0xFFE53935)   // status accent — unchanged
private val TextDark: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF07112A)
private val TextGray: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA4AF) else Color(0xFF626B80)
private val IconGray: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF8A93A6) else Color(0xFF9CA4B6)
private val Border: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2C2C2E) else Color(0xFFE2E4E9)
private val CardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val IconCircleBg: Color   // was inline Color(0xFFE8EDFF), the lock-icon circle backdrop
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1A2740) else Color(0xFFE8EDFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel()
) {
    var current        by remember { mutableStateOf("") }
    var newPass        by remember { mutableStateOf("") }
    var confirmPass    by remember { mutableStateOf("") }
    var showCurrent    by remember { mutableStateOf(false) }
    var showNew        by remember { mutableStateOf(false) }
    var showConfirm    by remember { mutableStateOf(false) }

    // Local, client-side validation error (e.g. mismatch, too short) — separate from Firebase errors
    var localError by remember { mutableStateOf<String?>(null) }

    val authState = authViewModel.authState
    val isLoading = authState is AuthResult.Loading
    val serverError = (authState as? AuthResult.Error)?.message
    val errorMessage = localError ?: serverError

    val allFieldsRequiredError = stringResource(R.string.change_password_error_all_fields_required)
    val minLengthError = stringResource(R.string.change_password_error_min_length)
    val mismatchError = stringResource(R.string.change_password_error_mismatch)

    // Reset any leftover state from a previous screen visit
    LaunchedEffect(Unit) {
        authViewModel.resetState()
    }

    // React to a successful password change
    LaunchedEffect(authState) {
        if (authState is AuthResult.Success) {
            authViewModel.resetState()
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            BackButton(onBack = onBack)
            Text(stringResource(R.string.change_password_title), fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp, color = TextDark)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Lock refresh icon ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(IconCircleBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(40.dp)) {
                val strokePx = 2.6.dp.toPx()
                val cx = size.width / 2f
                val cy = size.height / 2f

                drawRoundRect(
                    color = Blue,
                    topLeft = androidx.compose.ui.geometry.Offset(cx - 8.dp.toPx(), cy - 1.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(16.dp.toPx(), 12.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.5.dp.toPx()),
                    style = Stroke(width = strokePx)
                )
                val shackle = Path().apply {
                    moveTo(cx - 4.5.dp.toPx(), cy - 1.dp.toPx())
                    lineTo(cx - 4.5.dp.toPx(), cy - 6.dp.toPx())
                    cubicTo(cx - 4.5.dp.toPx(), cy - 10.dp.toPx(), cx + 4.5.dp.toPx(), cy - 10.dp.toPx(), cx + 4.5.dp.toPx(), cy - 6.dp.toPx())
                    lineTo(cx + 4.5.dp.toPx(), cy - 1.dp.toPx())
                }
                drawPath(shackle, Blue, style = Stroke(width = strokePx, cap = StrokeCap.Round))

                val arcPath = Path().apply {
                    val r = 14.dp.toPx()
                    addArc(
                        oval = androidx.compose.ui.geometry.Rect(cx - r, cy - r, cx + r, cy + r),
                        startAngleDegrees = 150f,
                        sweepAngleDegrees = 210f
                    )
                }
                drawPath(arcPath, Blue, style = Stroke(width = strokePx, cap = StrokeCap.Round))

                val arrowTipX = cx + 13.dp.toPx()
                val arrowTipY = cy + 5.5.dp.toPx()
                val arrowPath = Path().apply {
                    moveTo(arrowTipX - 3.5.dp.toPx(), arrowTipY - 2.dp.toPx())
                    lineTo(arrowTipX, arrowTipY)
                    lineTo(arrowTipX - 2.dp.toPx(), arrowTipY + 3.5.dp.toPx())
                }
                drawPath(arrowPath, Blue, style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(stringResource(R.string.change_password_title), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            stringResource(R.string.change_password_subtitle),
            fontSize = 14.sp, color = TextGray, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── Fields ────────────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            PasswordFieldBlock(
                label       = stringResource(R.string.change_password_current_label),
                value       = current,
                onValueChange = {
                    current = it
                    localError = null
                },
                placeholder = stringResource(R.string.change_password_current_placeholder),
                showPassword = showCurrent,
                onToggle    = { showCurrent = !showCurrent },
                isUnlocked  = false
            )

            PasswordFieldBlock(
                label       = stringResource(R.string.change_password_new_label),
                value       = newPass,
                onValueChange = {
                    newPass = it
                    localError = null
                },
                placeholder = stringResource(R.string.change_password_new_placeholder),
                showPassword = showNew,
                onToggle    = { showNew = !showNew },
                isUnlocked  = true
            )

            PasswordFieldBlock(
                label       = stringResource(R.string.change_password_confirm_label),
                value       = confirmPass,
                onValueChange = {
                    confirmPass = it
                    localError = null
                },
                placeholder = stringResource(R.string.change_password_confirm_placeholder),
                showPassword = showConfirm,
                onToggle    = { showConfirm = !showConfirm },
                isUnlocked  = true
            )
        }

        // ── Error message (validation or Firebase) ──────────────────────────────
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                errorMessage,
                fontSize = 13.sp,
                color = Red,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Update Password button ────────────────────────────────────────────
        Button(
            onClick = {
                when {
                    current.isBlank() || newPass.isBlank() || confirmPass.isBlank() -> {
                        localError = allFieldsRequiredError
                    }
                    newPass.length < 6 -> {
                        localError = minLengthError
                    }
                    newPass != confirmPass -> {
                        localError = mismatchError
                    }
                    else -> {
                        localError = null
                        authViewModel.changePassword(current, newPass)
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(56.dp)
                .shadow(4.dp, RoundedCornerShape(18.dp),
                    ambientColor = Blue.copy(alpha = 0.4f), spotColor = Blue),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text(stringResource(R.string.change_password_update_button), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Cancel button ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(56.dp)
                .background(CardWhite, RoundedCornerShape(18.dp))
                .clickable(enabled = !isLoading) { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.change_password_cancel_button), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Blue)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordFieldBlock(
    label        : String,
    value        : String,
    onValueChange: (String) -> Unit,
    placeholder  : String,
    showPassword : Boolean,
    onToggle     : () -> Unit,
    isUnlocked   : Boolean
) {
    // Canvas draw lambdas are DrawScope, NOT @Composable — resolve the
    // theme-aware color here (composable context) and pass a plain Color in.
    val iconColor = IconGray

    Column {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = { Text(placeholder, color = iconColor) },
            modifier      = Modifier.fillMaxWidth().height(56.dp),
            shape         = RoundedCornerShape(14.dp),
            singleLine    = true,
            leadingIcon   = {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val strokePx = 2.dp.toPx()
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    // Lock body
                    drawRoundRect(
                        color = iconColor,
                        topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.08f, size.height * 0.42f),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.84f, size.height * 0.54f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                        style = Stroke(width = strokePx)
                    )
                    // Shackle — unlocked = right side open
                    val shackle = Path().apply {
                        if (isUnlocked) {
                            // open shackle — only left side drawn, right side floating
                            moveTo(size.width * 0.25f, size.height * 0.42f)
                            lineTo(size.width * 0.25f, size.height * 0.2f)
                            cubicTo(size.width * 0.25f, 0f, size.width * 0.75f, 0f, size.width * 0.75f, size.height * 0.2f)
                            // open right: only go part-way up
                            lineTo(size.width * 0.75f, size.height * 0.05f)
                        } else {
                            moveTo(size.width * 0.25f, size.height * 0.42f)
                            lineTo(size.width * 0.25f, size.height * 0.2f)
                            cubicTo(size.width * 0.25f, 0f, size.width * 0.75f, 0f, size.width * 0.75f, size.height * 0.2f)
                            lineTo(size.width * 0.75f, size.height * 0.42f)
                        }
                    }
                    drawPath(shackle, iconColor, style = Stroke(width = strokePx, cap = StrokeCap.Round))
                }
            },
            trailingIcon  = {
                Canvas(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null
                        ) { onToggle() }
                ) {
                    val strokeW = 2.dp.toPx()
                    val eyePath = Path().apply {
                        moveTo(size.width * 0.1f, size.height * 0.5f)
                        cubicTo(size.width * 0.3f, size.height * 0.18f, size.width * 0.7f, size.height * 0.18f, size.width * 0.9f, size.height * 0.5f)
                        cubicTo(size.width * 0.7f, size.height * 0.82f, size.width * 0.3f, size.height * 0.82f, size.width * 0.1f, size.height * 0.5f)
                    }
                    drawPath(eyePath, iconColor, style = Stroke(width = strokeW, cap = StrokeCap.Round))
                    drawCircle(iconColor, radius = 4.dp.toPx())
                    if (!showPassword) {
                        val slashPath = Path().apply {
                            moveTo(size.width * 0.15f, size.height * 0.85f)
                            lineTo(size.width * 0.85f, size.height * 0.15f)
                        }
                        drawPath(slashPath, iconColor, style = Stroke(width = strokeW, cap = StrokeCap.Round))
                    }
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors               = TextFieldDefaults.outlinedTextFieldColors(
                containerColor       = CardWhite,
                unfocusedBorderColor = Border,
                focusedBorderColor   = Blue
            )
        )
    }
}