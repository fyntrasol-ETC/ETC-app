package com.evolvingtheconversation.app.navigation

import com.evolvingtheconversation.app.data.ThemeManager
import android.content.Context
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.evolvingtheconversation.app.data.UserPreferences
import com.evolvingtheconversation.app.ui.screens.MainAppScaffold
import com.evolvingtheconversation.app.ui.screens.onboarding.*
import com.evolvingtheconversation.app.ui.screens.learning.ChangePasswordScreen
import com.evolvingtheconversation.app.ui.screens.learning.DeleteAccountScreen
import com.evolvingtheconversation.app.ui.screens.learning.GrammarScreen
import com.evolvingtheconversation.app.ui.screens.learning.QuizScreen
import com.evolvingtheconversation.app.ui.screens.learning.SettingsScreen
import com.evolvingtheconversation.app.ui.screens.learning.VocabularyScreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.evolvingtheconversation.app.ui.screens.learning.GrammarQuizScreen


@Composable
fun NavGraph(context: Context) {
    val navController          = rememberNavController()
    val userPrefs              = remember { UserPreferences(context) }
    val hasCompletedOnboarding by userPrefs.hasCompletedOnboarding.collectAsState(initial = null)
    val scope                  = rememberCoroutineScope()

    if (hasCompletedOnboarding == null) return

    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        // ── Splash ────────────────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(onFinish = {
                val isLoggedIn = FirebaseAuth.getInstance().currentUser != null

                val next = when {
                    isLoggedIn -> Screen.Home.route
                    hasCompletedOnboarding == true -> Screen.Login.route
                    else -> Screen.Onboard.route
                }

                navController.navigate(next) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }

        // ── Onboarding ────────────────────────────────────────────────────────
        composable(Screen.Onboard.route) {
            OnboardScreen(
                onSkip = {
                    scope.launch { userPrefs.saveOnboardingCompleted(true) }
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboard.route) { inclusive = true }
                    }
                },
                onGetStarted = {
                    scope.launch { userPrefs.saveOnboardingCompleted(true) }
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboard.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Login ─────────────────────────────────────────────────────────────
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    // Load the signed-in user's saved theme BEFORE navigating home.
                    FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                        ThemeManager.loadThemeForUser(context, uid)
                    }
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onCreateAccountClick  = { navController.navigate(Screen.Register.route) },
                onForgotPasswordClick = { navController.navigate(Screen.ForgotPassword.route) }
            )
        }

        // ── Forgot Password ───────────────────────────────────────────────────
        composable(Screen.ForgotPassword.route) {
            var isSendingReset by remember { mutableStateOf(false) }
            var resetError     by remember { mutableStateOf<String?>(null) }

            ForgotPasswordScreen(
                onBack          = { navController.popBackStack() },
                isLoading       = isSendingReset,
                errorMessage    = resetError,
                onSendResetLink = { email ->
                    isSendingReset = true
                    resetError = null
                    FirebaseAuth.getInstance()
                        .sendPasswordResetEmail(email)
                        .addOnCompleteListener { task ->
                            isSendingReset = false
                            if (task.isSuccessful) {
                                navController.popBackStack()
                            } else {
                                resetError = task.exception?.message
                                    ?: "Failed to send reset email. Please try again."
                            }
                        }
                }
            )
        }

        // ── Register ──────────────────────────────────────────────────────────
        composable(Screen.Register.route) {
            CreateAccountScreen(
                onBackClick          = { navController.popBackStack() },
                onCreateAccountClick = {
                    // New account → settings.darkMode defaults to false in
                    // buildNewUserDoc(), so this just confirms it and caches it.
                    FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                        ThemeManager.loadThemeForUser(context, uid)
                    }
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Main App Scaffold (Home + bottom tabs) ────────────────────────────
        composable(Screen.Home.route) {
            MainAppScaffold(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onAccountDeleted = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Settings / Change Password / Delete Account ───────────────────────
        // NOTE: these three are also defined inside MainAppScaffold's own inner
        // NavHost for in-app access via the drawer/profile flow. Keeping duplicate
        // top-level entries here is harmless (unused unless something navigates to
        // them via THIS outer navController), but if nothing here ever does, they
        // can safely be deleted to avoid confusion.
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack           = { navController.popBackStack() },
                onChangePassword = { navController.navigate(Screen.ChangePassword.route) },
                onDeleteAccount  = { navController.navigate(Screen.DeleteAccount.route) }
            )
        }

        composable(Screen.ChangePassword.route) {
            ChangePasswordScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DeleteAccount.route) {
            DeleteAccountScreen(
                onBack    = { navController.popBackStack() },
                onDeleted = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }


        composable(Screen.Grammar.route) {
            GrammarScreen(
                onBack       = { navController.popBackStack() },
                onTopicClick = { topic ->
                    navController.navigate("grammarQuiz/${topic.title}")
                }
            )
        }

        composable(
            route     = "grammarQuiz/{topic}",
            arguments = listOf(navArgument("topic") { type = NavType.StringType })
        ) { backStack ->
            val topic = backStack.arguments?.getString("topic") ?: "Grammar"
            GrammarQuizScreen(
                topicTitle = topic,
                onBack     = { navController.popBackStack() }
            )
        }

        // ── Vocabulary ────────────────────────────────────────────────────────
        composable(Screen.Vocabulary.route) {
            VocabularyScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ── Quiz ──────────────────────────────────────────────────────────────
        composable(
            route     = Screen.Quiz.route,
            arguments = listOf(navArgument("topic") { type = NavType.StringType })
        ) { backStack ->
            val topic = backStack.arguments?.getString("topic") ?: "Grammar"
            QuizScreen(
                topic  = topic,
                onBack = { navController.popBackStack() }
            )
        }
    }
}