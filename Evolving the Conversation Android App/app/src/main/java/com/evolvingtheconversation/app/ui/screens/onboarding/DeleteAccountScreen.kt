package com.evolvingtheconversation.app.ui.screens.learning

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evolvingtheconversation.viewmodel.AuthResult
import com.evolvingtheconversation.viewmodel.AuthViewModel
import com.evolvingtheconversation.app.ui.components.BackButton
import androidx.compose.runtime.getValue
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme
import com.evolvingtheconversation.app.R

private val Bg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFF4F4F8)
private val Red      = Color(0xFFE53935)   // status accent — unchanged
private val Blue     = Color(0xFF1E5EF3)   // brand accent — unchanged
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

// New tokens for inline colors caught in this file:
private val IconCircleBgRed: Color   // was inline Color(0xFFFFE8E8), the person-remove icon backdrop
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF3A1A1A) else Color(0xFFFFE8E8)
private val LightButtonBg: Color     // was inline Color(0xFFF0F0F0), the confirm-dialog's "Delete Account" tap surface
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2C) else Color(0xFFF0F0F0)
private val WarningBannerBg: Color   // was inline Color(0xFFFFF3E8), the amber warning banner
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF3A2E1A) else Color(0xFFFFF3E8)
private val DisabledBg: Color        // was inline Color(0xFFCCCCCC), the disabled "Delete My Account" button
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF3A3A3C) else Color(0xFFCCCCCC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountScreen(
    onBack: () -> Unit = {},
    onDeleted: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel()
) {
    var password      by remember { mutableStateOf("") }
    var showPassword  by remember { mutableStateOf(false) }
    var showDialog    by remember { mutableStateOf(false) }

    // Canvas draw lambdas are DrawScope, NOT @Composable — resolve the
    // theme-aware color here (composable context) and pass a plain Color in.
    val iconColor = IconGray

    val authState = authViewModel.authState
    val isLoading = authState is AuthResult.Loading
    val errorMessage = (authState as? AuthResult.Error)?.message

    // Reset any leftover state from a previous screen visit
    LaunchedEffect(Unit) {
        authViewModel.resetState()
    }

    // React to auth state changes
    LaunchedEffect(authState) {
        if (authState is AuthResult.Success) {
            showDialog = false
            authViewModel.resetState()
            onDeleted()
        }
    }

    // ── Confirmation Dialog ───────────────────────────────────────────────────
    if (showDialog) {
        Dialog(onDismissRequest = { if (!isLoading) showDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardWhite, RoundedCornerShape(20.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        stringResource(R.string.delete_account_confirm_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.delete_account_confirm_subtitle), fontSize = 14.sp, color = TextGray)

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(errorMessage, fontSize = 13.sp, color = Red)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LightButtonBg, RoundedCornerShape(14.dp))
                            .clickable(enabled = !isLoading) {
                                authViewModel.deleteAccount(password)
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Red
                            )
                        } else {
                            Text(
                                stringResource(R.string.delete_account_confirm_button), fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold, color = Red
                            )
                        }
                    }
                }
            }
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
            Text(stringResource(R.string.delete_account_title), fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp, color = TextDark)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Red person-remove icon in light red circle ────────────────────────
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(IconCircleBgRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(44.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val strokePx = 2.4.dp.toPx()

                // Person head circle
                drawCircle(
                    color = Red,
                    radius = 7.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(cx - 3.dp.toPx(), cy - 8.dp.toPx()),
                    style = Stroke(width = strokePx)
                )
                // Person body arc
                val bodyPath = Path().apply {
                    moveTo(cx - 14.dp.toPx(), cy + 7.dp.toPx())
                    cubicTo(cx - 14.dp.toPx(), cy - 1.dp.toPx(), cx + 8.dp.toPx(), cy - 1.dp.toPx(), cx + 8.dp.toPx(), cy + 7.dp.toPx())
                }
                drawPath(bodyPath, Red, style = Stroke(width = strokePx, cap = StrokeCap.Round))

                // Minus badge (remove indicator) — bottom right
                val badgeCx = cx + 10.dp.toPx()
                val badgeCy = cy + 6.dp.toPx()
                drawCircle(
                    color = Red,
                    radius = 7.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(badgeCx, badgeCy)
                )
                // Minus line inside badge
                drawLine(
                    color = Color.White,
                    start = androidx.compose.ui.geometry.Offset(badgeCx - 4.dp.toPx(), badgeCy),
                    end   = androidx.compose.ui.geometry.Offset(badgeCx + 4.dp.toPx(), badgeCy),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(stringResource(R.string.delete_account_title), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.delete_account_warning_subtitle),
            fontSize = 14.sp, color = TextGray, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 28.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── Password field ────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text(stringResource(R.string.delete_account_confirm_password_label), fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, color = TextDark)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value         = password,
                onValueChange = { password = it },
                placeholder   = { Text(stringResource(R.string.delete_account_password_placeholder), color = iconColor) },
                modifier      = Modifier.fillMaxWidth().height(56.dp),
                shape         = RoundedCornerShape(14.dp),
                singleLine    = true,
                isError       = errorMessage != null && !showDialog,
                leadingIcon   = {
                    Canvas(modifier = Modifier.size(20.dp)) {
                        val strokePx = 2.dp.toPx()
                        drawRoundRect(
                            color = iconColor,
                            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.08f, size.height * 0.42f),
                            size = androidx.compose.ui.geometry.Size(size.width * 0.84f, size.height * 0.54f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                            style = Stroke(width = strokePx)
                        )
                        val shackle = Path().apply {
                            moveTo(size.width * 0.25f, size.height * 0.42f)
                            lineTo(size.width * 0.25f, size.height * 0.2f)
                            cubicTo(size.width * 0.25f, 0f, size.width * 0.75f, 0f, size.width * 0.75f, size.height * 0.2f)
                            lineTo(size.width * 0.75f, size.height * 0.42f)
                        }
                        drawPath(shackle, iconColor, style = Stroke(width = strokePx, cap = StrokeCap.Round))
                    }
                },
                trailingIcon  = {
                    Canvas(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(remember { MutableInteractionSource() }, null) {
                                showPassword = !showPassword
                            }
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
                            drawLine(
                                color = iconColor,
                                start = androidx.compose.ui.geometry.Offset(size.width * 0.08f, size.height * 0.15f),
                                end   = androidx.compose.ui.geometry.Offset(size.width * 0.92f, size.height * 0.85f),
                                strokeWidth = strokeW,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors               = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor       = CardWhite,
                    unfocusedBorderColor = Border,
                    focusedBorderColor   = Red
                )
            )

            // Inline error shown on the main screen (e.g. if dialog was dismissed after a failure)
            if (errorMessage != null && !showDialog) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(errorMessage, fontSize = 12.sp, color = Red)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Warning banner ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .background(WarningBannerBg, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Outlined.Warning, contentDescription = null,
                tint = Color(0xFFF57C00), modifier = Modifier.size(20.dp))
            Text(
                stringResource(R.string.delete_account_warning_banner),
                fontSize = 13.sp, color = TextGray, lineHeight = 19.sp
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── Delete My Account button (dimmed when no password) ────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (password.isNotEmpty()) Red else DisabledBg)
                .clickable(enabled = password.isNotEmpty() && !isLoading) {
                    authViewModel.resetState()
                    showDialog = true
                },
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.delete_account_delete_button), fontSize = 16.sp,
                fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Cancel button ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(56.dp)
                .background(CardWhite, RoundedCornerShape(18.dp))
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.delete_account_cancel_button), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Blue)
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}