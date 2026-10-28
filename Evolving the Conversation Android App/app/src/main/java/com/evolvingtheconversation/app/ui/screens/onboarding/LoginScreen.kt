package com.evolvingtheconversation.app.ui.screens.onboarding

import android.widget.Toast
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evolvingtheconversation.viewmodel.AuthResult
import com.evolvingtheconversation.viewmodel.AuthViewModel
import com.evolvingtheconversation.app.data.LanguageManager
import com.evolvingtheconversation.app.data.ThemeManager
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.getValue
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme
import com.evolvingtheconversation.app.R

// This file had no top color-token block at all — everything was inline.
// Added one to follow the same pattern as the rest of the app.
private val ScreenBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFFBFBFD)
private val TextDark: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF07112A)
private val TextGray: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA4AF) else Color(0xFF626B80)
private val IconGray: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF8A93A6) else Color(0xFF9CA4B6)
private val CanvasIconGray: Color   // the 0xFF7D8396 stroke color used inside Canvas drawings
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF8A93A6) else Color(0xFF7D8396)
private val BorderCol: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2C2C2E) else Color(0xFFE2E4E9)
private val CardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
// Blue (0xFF1E5EF3) and Green (0xFF0CBD85) stay inline/unchanged — brand accents
// (gradient logo badge, focused borders, links). Google/Apple logo colors and the
// white bubble-icon stroke drawn ON the gradient badge also stay unchanged — brand
// marks sitting on a colored surface, not app surface colors.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit = {},
    onCreateAccountClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {}
) {
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val authState = authViewModel.authState
    val webClientId = stringResource(id = R.string.default_web_client_id)


    LaunchedEffect(authState) {
        when (authState) {
            is AuthResult.Success -> {
                Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                // Runtime login (no Activity recreate) never hits MainActivity's
                // cold-start LaunchedEffect(Unit) — reload this user's saved
                // settings here instead, right as their session starts.
                FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                    ThemeManager.loadThemeForUser(context, uid)
                    LanguageManager.loadLanguageForUser(context, uid)
                }
                authViewModel.resetState()
                onLoginSuccess()
            }
            is AuthResult.Error -> {
                Toast.makeText(context, authState.message, Toast.LENGTH_LONG).show()
                authViewModel.resetState()
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .systemBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // 1. TOP-LEFT GRADIENT LOGO BADGE
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color(0xFF0CBD85).copy(alpha = 0.2f),
                    spotColor = Color(0xFF1E5EF3)
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF1E5EF3), Color(0xFF0CBD85)),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(28.dp)) {
                val strokeW = 2.2.dp.toPx()

                val centerX = size.width / 2f
                val centerY = size.height / 2f - 1.dp.toPx()

                val bubbleW = 10.dp.toPx()
                val bubbleH = 9.dp.toPx()
                val cornerR = 10.dp.toPx()

                val bubblePath = Path().apply {
                    val left   = centerX - bubbleW
                    val right  = centerX + bubbleW
                    val top    = centerY - bubbleH
                    val bottom = centerY + bubbleH

                    moveTo(left + cornerR, top)
                    lineTo(right - cornerR, top)
                    quadraticBezierTo(right, top, right, top + cornerR)
                    lineTo(right, bottom - cornerR)
                    quadraticBezierTo(right, bottom, right - cornerR, bottom)
                    val tailStartX = left + cornerR + 1.dp.toPx()
                    lineTo(tailStartX, bottom)

                    val tipX = left - 3.dp.toPx()
                    val tipY = bottom + 4.dp.toPx()
                    quadraticBezierTo(left - 1.dp.toPx(), bottom + 1.dp.toPx(), tipX, tipY)
                    quadraticBezierTo(left + 0.5.dp.toPx(), bottom - 0.5.dp.toPx(), left, bottom - cornerR)

                    lineTo(left, top + cornerR)
                    quadraticBezierTo(left, top, left + cornerR, top)
                    close()
                }

                drawPath(
                    path = bubblePath,
                    color = Color.White,
                    style = Stroke(
                        width = strokeW,
                        join = StrokeJoin.Round,
                        cap = StrokeCap.Round
                    )
                )

                val dotRadius = 3.dp.toPx()
                val dotX = centerX + bubbleW + 2.9.dp.toPx()
                val dotY = centerY - bubbleH - 1.5.dp.toPx()
                drawCircle(
                    color = Color.White,
                    radius = dotRadius,
                    center = androidx.compose.ui.geometry.Offset(dotX, dotY)
                )
            }
        }
        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "Welcome back",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Log in to continue your learning journey.",
            fontSize = 16.sp,
            color = TextGray
        )

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "Email",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Canvas draw lambdas are DrawScope, NOT @Composable — resolve the
        // theme-aware colors here (composable context) and pass plain Colors in.
        val iconColor = IconGray
        val canvasIconColor = CanvasIconGray

        OutlinedTextField(
            value = emailInput,
            onValueChange = { emailInput = it },
            placeholder = { Text("you@example.com", color = iconColor) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            leadingIcon = {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val strokePx = 2.dp.toPx()
                    drawRoundRect(
                        color = canvasIconColor,
                        style = Stroke(width = strokePx)
                    )
                    val envelopeFlap = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width * 0.5f, size.height * 0.5f)
                        lineTo(size.width, 0f)
                    }
                    drawPath(path = envelopeFlap, color = canvasIconColor, style = Stroke(width = strokePx))
                }
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = CardWhite,
                unfocusedBorderColor = BorderCol,
                focusedBorderColor = Color(0xFF1E5EF3)
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Password",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = passwordInput,
            onValueChange = { passwordInput = it },
            placeholder = { Text("Enter your password", color = iconColor) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            leadingIcon = {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val strokePx = 2.dp.toPx()
                    drawRoundRect(
                        color = canvasIconColor,
                        topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * 0.4f),
                        size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.6f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )
                    val shacklePath = Path().apply {
                        moveTo(size.width * 0.25f, size.height * 0.4f)
                        lineTo(size.width * 0.25f, size.height * 0.22f)
                        cubicTo(size.width * 0.25f, 0f, size.width * 0.75f, 0f, size.width * 0.75f, size.height * 0.22f)
                        lineTo(size.width * 0.75f, size.height * 0.4f)
                    }
                    drawPath(path = shacklePath, color = canvasIconColor, style = Stroke(width = strokePx))
                }
            },
            trailingIcon = {
                Canvas(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isPasswordVisible = !isPasswordVisible }
                ) {
                    val strokeW = 2.dp.toPx()
                    val eyePath = Path().apply {
                        moveTo(size.width * 0.15f, size.height * 0.5f)
                        cubicTo(size.width * 0.35f, size.height * 0.2f, size.width * 0.65f, size.height * 0.2f, size.width * 0.85f, size.height * 0.5f)
                        cubicTo(size.width * 0.65f, size.height * 0.8f, size.width * 0.35f, size.height * 0.8f, size.width * 0.15f, size.height * 0.5f)
                    }
                    drawPath(path = eyePath, color = canvasIconColor, style = Stroke(width = strokeW))
                    drawCircle(color = canvasIconColor, radius = 4.5.dp.toPx())


                    if (!isPasswordVisible) {
                        drawLine(
                            color = canvasIconColor,
                            start = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.15f),
                            end = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.85f),
                            strokeWidth = strokeW
                        )
                    }
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = CardWhite,
                unfocusedBorderColor = BorderCol,
                focusedBorderColor = Color(0xFF1E5EF3)
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Text(
                text = "Forgot password?",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E5EF3),
                modifier = Modifier.clickable { onForgotPasswordClick() }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 6. LOG IN BUTTON
        // FIX: Material3's Button() applies a default translucent
        // disabledContainerColor/disabledContentColor when `enabled = false`.
        // Since this button goes disabled during the Loading state, that
        // default gray-alpha overlay was painting on top of the blue
        // containerColor + shadow, producing the "bar inside a bar" look
        // in the screenshot instead of a clean blue button with a spinner.
        // Fix: explicitly pin the disabled colors to the same blue / white
        // so the button looks identical whether enabled or loading.
        Button(
            onClick = { authViewModel.login(emailInput, passwordInput) },
            enabled = authState !is AuthResult.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = Color(0xFF1E5EF3).copy(alpha = 0.4f),
                    spotColor = Color(0xFF1E5EF3)
                ),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E5EF3),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF1E5EF3),
                disabledContentColor = Color.White
            )
        ) {
            if (authState is AuthResult.Loading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp
                )
            } else {
                Text(
                    text = "Log in",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.weight(1f).height(1.dp).background(BorderCol))
            Text(
                text = "or continue with",
                fontSize = 13.sp,
                color = TextGray,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Box(modifier = Modifier.weight(1f).height(1.dp).background(BorderCol))
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 8. OAUTH SOCIAL SIGN IN ROW
        // 8. GOOGLE SIGN IN BUTTON
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .border(1.dp, BorderCol, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(CardWhite)
                .clickable { authViewModel.signInWithGoogle(context, webClientId) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scaleX = size.width / 24f
                    val scaleY = size.height / 24f

                    withTransform({
                        scale(scaleX, scaleY, androidx.compose.ui.geometry.Offset.Zero)
                    }) {
                        val redPath = PathParser().parsePathString(
                            "M12.24,5a7.11,7.11,0,0,1,5,1.94l3.71,-3.71A12,12,0,0,0,2.33,7.18L6.2,10.18A7.15,7.15,0,0,1,12.24,5Z"
                        ).toPath()
                        drawPath(path = redPath, color = Color(0xFFEA4335))

                        val greenPath = PathParser().parsePathString(
                            "M12.24,19a7.17,7.17,0,0,1,-6,-3.14L2.3,18.84A12,12,0,0,0,22,15.56l-3.8,-2.95A7.1,7.1,0,0,1,12.24,19Z"
                        ).toPath()
                        drawPath(path = greenPath, color = Color(0xFF34A853))

                        val yellowPath = PathParser().parsePathString(
                            "M6.2,10.18a7.06,7.06,0,0,1,0,3.64l-3.9,3A12,12,0,0,1,2.3,7.18Z"
                        ).toPath()
                        drawPath(path = yellowPath, color = Color(0xFFFBBC05))

                        val bluePath = PathParser().parsePathString(
                            "M22.5,12.27a10.42,10.42,0,0,0,-0.17,-2H12.24v4h5.83A5,5,0,0,1,15.9,17.61l3.8,3A12,12,0,0,0,22.5,12.27Z"
                        ).toPath()
                        drawPath(path = bluePath, color = Color(0xFF4285F4))
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "Google",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "New here? ", fontSize = 15.sp, color = TextGray)
            Text(
                text = "Create account",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E5EF3),
                modifier = Modifier.clickable { onCreateAccountClick() }
            )
        }
    }
}

@Preview(name = "Login View screen", widthDp = 360, heightDp = 740, showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen()
}