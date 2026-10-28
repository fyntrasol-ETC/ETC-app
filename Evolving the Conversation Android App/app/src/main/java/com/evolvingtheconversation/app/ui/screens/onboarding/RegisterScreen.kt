package com.evolvingtheconversation.app.ui.screens.onboarding

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.evolvingtheconversation.app.ui.components.BackButton
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.getValue
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme

// No top color-token block existed in this file either — reusing the same
// tokens/values as LoginScreen for app-wide consistency.
private val ScreenBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFFBFBFD)
private val TextDark: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF07112A)
private val TextGray: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA4AF) else Color(0xFF626B80)
private val IconGray: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF8A93A6) else Color(0xFF9CA4B6)
private val BorderCol: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2C2C2E) else Color(0xFFE2E4E9)
private val CardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
// Color(0xFF1E5EF3) stays unchanged everywhere — brand accent (checkbox check
// color, "Terms"/"Privacy Policy" links, button background).

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountScreen(
    authViewModel: AuthViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onCreateAccountClick: () -> Unit = {}
) {
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var isChecked by remember { mutableStateOf(false) }


    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val authState = authViewModel.authState

    LaunchedEffect(authState) {
        when (authState) {
            is AuthResult.Success -> {
                Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                // Same reload MainActivity's cold-start effect can't reach for a
                // runtime signup (no Activity recreate happens here).
                FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                    ThemeManager.loadThemeForUser(context, uid)
                    LanguageManager.loadLanguageForUser(context, uid)
                }
                authViewModel.resetState()
                onCreateAccountClick()
            }
            is AuthResult.Error -> {
                Toast.makeText(context, authState.message, Toast.LENGTH_LONG).show()
                authViewModel.resetState()
            }
            else -> {}
        }
    }


    fun isPasswordStrong(password: String): Boolean {
        val hasMinLength = password.length >= 8
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        return hasMinLength && hasLetter && hasDigit
    }

    fun validateAll(): Boolean {
        nameError = if (nameInput.isBlank()) "Full name is required" else null

        emailError = when {
            emailInput.isBlank() -> "Email is required"
            !Patterns.EMAIL_ADDRESS.matcher(emailInput).matches() -> "Enter a valid email address"
            else -> null
        }

        passwordError = when {
            passwordInput.isBlank() -> "Password is required"
            !isPasswordStrong(passwordInput) -> "Min 8 characters, with a letter and a number"
            else -> null
        }

        confirmPasswordError = when {
            confirmPasswordInput.isBlank() -> "Please confirm your password"
            confirmPasswordInput != passwordInput -> "Passwords do not match"
            else -> null
        }

        return listOf(nameError, emailError, passwordError, confirmPasswordError).all { it == null }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .systemBarsPadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // BACK BUTTON
        BackButton(onBack = onBackClick)

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Create account", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Start learning English with confidence.", fontSize = 16.sp, color = TextGray)

        Spacer(modifier = Modifier.height(32.dp))

        val inputModifier = Modifier.fillMaxWidth()
        val textFieldColors = TextFieldDefaults.outlinedTextFieldColors(
            containerColor = CardWhite,
            unfocusedBorderColor = BorderCol,
            focusedBorderColor = Color(0xFF1E5EF3)
        )

        // Full Name
        Text(text = "Full name", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = nameInput,
            onValueChange = {
                nameInput = it
                if (nameError != null) nameError = null
            },
            modifier = inputModifier, shape = RoundedCornerShape(14.dp),
            placeholder = { Text("Jane Cooper", color = IconGray) },
            colors = textFieldColors, singleLine = true,
            isError = nameError != null,
            supportingText = {
                nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Email
        Text(text = "Email", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = emailInput,
            onValueChange = {
                emailInput = it
                if (emailError != null) emailError = null
            },
            modifier = inputModifier, shape = RoundedCornerShape(14.dp),
            placeholder = { Text("you@example.com", color = IconGray) },
            colors = textFieldColors, singleLine = true,
            isError = emailError != null,
            supportingText = {
                emailError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Password
        Text(text = "Password", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = passwordInput,
            onValueChange = {
                passwordInput = it
                if (passwordError != null) passwordError = null
            },
            modifier = inputModifier, shape = RoundedCornerShape(14.dp),
            placeholder = { Text("********", color = IconGray) },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val icon = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                Icon(
                    imageVector = icon,
                    contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                    modifier = Modifier.clickable { isPasswordVisible = !isPasswordVisible }
                )
            },
            colors = textFieldColors, singleLine = true,
            isError = passwordError != null,
            supportingText = {
                passwordError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Confirm Password
        Text(text = "Confirm password", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = confirmPasswordInput,
            onValueChange = {
                confirmPasswordInput = it
                if (confirmPasswordError != null) confirmPasswordError = null
            },
            modifier = inputModifier, shape = RoundedCornerShape(14.dp),
            placeholder = { Text("********", color = IconGray) },
            visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val icon = if (isConfirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                Icon(
                    imageVector = icon,
                    contentDescription = if (isConfirmPasswordVisible) "Hide password" else "Show password",
                    modifier = Modifier.clickable { isConfirmPasswordVisible = !isConfirmPasswordVisible }
                )
            },
            colors = textFieldColors, singleLine = true,
            isError = confirmPasswordError != null,
            supportingText = {
                confirmPasswordError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        )
        Spacer(modifier = Modifier.height(20.dp))

        // TERMS CHECKBOX
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { isChecked = it },
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1E5EF3))
            )
            Text(text = "I accept the ", fontSize = 14.sp, color = TextGray)
            Text(text = "Terms ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E5EF3))
            Text(text = "& ", fontSize = 14.sp, color = TextGray)
            Text(text = "Privacy Policy", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E5EF3))
        }

        Spacer(modifier = Modifier.height(28.dp))

        // CREATE ACCOUNT BUTTON
        Button(
            onClick = {
                if (!isChecked) {
                    Toast.makeText(context, "Please accept Terms & Privacy Policy", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (validateAll()) {
                    authViewModel.signUp(nameInput, emailInput, passwordInput)
                }
            },
            enabled = authState !is AuthResult.Loading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E5EF3))
        ) {
            if (authState is AuthResult.Loading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(text = "Create account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview(name = "Create Account Preview", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun CreateAccountScreenPreview() {
    CreateAccountScreen(
        onBackClick = {},
        onCreateAccountClick = {}
    )
}