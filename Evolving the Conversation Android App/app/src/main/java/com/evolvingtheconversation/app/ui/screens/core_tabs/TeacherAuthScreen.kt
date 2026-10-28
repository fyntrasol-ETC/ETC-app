package com.evolvingtheconversation.app.ui.screens.core_tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evolvingtheconversation.viewmodel.TeacherAuthResult
import com.evolvingtheconversation.viewmodel.TeacherViewModel
import com.evolvingtheconversation.app.ui.components.BackButton
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme

private val BlueStart = Color(0xFF1E5EF3)
private val SurfaceBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFF4F6FB)
private val TextPrimary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)
private val TextSecondary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)
private val CardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White

/**
 * Reached from the "Teacher" button on LiveClassesScreen. Re-checks the
 * SAME credentials used to log into the app (no separate teacher account).
 * On success, hands off to "My Classes".
 */
@Composable
fun TeacherAuthScreen(
    teacherViewModel: TeacherViewModel,
    onBack: () -> Unit = {},
    onAuthSuccess: () -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val authState = teacherViewModel.authState

    LaunchedEffect(authState) {
        if (authState is TeacherAuthResult.Success) {
            teacherViewModel.resetState()
            onAuthSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 40.dp)
    ) {
        // ── TOP BAR ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(onBack = onBack)
            Spacer(Modifier.width(16.dp))
            Text("Sign in as Teacher", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextPrimary)
        }

        Text(
            "Use the same email and password you use to log in to this app.",
            fontSize = 14.sp, color = TextSecondary,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            LabeledField("Full name", name, leadingIcon = Icons.Filled.Person) { name = it }
            LabeledField("Email", email, leadingIcon = Icons.Filled.Email, keyboardType = KeyboardType.Email) { email = it }

            Column {
                Text("Password", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                )
            }

            if (authState is TeacherAuthResult.Error) {
                Text(authState.message, color = Color(0xFFE53935), fontSize = 13.sp)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(14.dp))
                    .background(BlueStart, RoundedCornerShape(14.dp))
                    .clickable(enabled = authState !is TeacherAuthResult.Loading) {
                        teacherViewModel.signInAsTeacher(name, email, password)
                    }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                if (authState is TeacherAuthResult.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Sign in as Teacher", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    Column {
        Text(label, fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null) } }
        )
    }
}