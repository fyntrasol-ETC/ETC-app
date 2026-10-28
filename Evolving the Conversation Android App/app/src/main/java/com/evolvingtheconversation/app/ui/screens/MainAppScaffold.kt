package com.evolvingtheconversation.app.ui.screens

import com.evolvingtheconversation.app.data.ThemeManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import com.evolvingtheconversation.app.ui.screens.tutor.TutorConversationScreen
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.evolvingtheconversation.viewmodel.AuthViewModel
import com.evolvingtheconversation.viewmodel.TeacherViewModel
import com.evolvingtheconversation.app.data.CourseCatalog
import com.evolvingtheconversation.app.navigation.Screen
import com.evolvingtheconversation.app.components.AppBottomNavigation
import com.evolvingtheconversation.app.ui.components.CertificateEarnedBanner
import com.evolvingtheconversation.app.ui.screens.core_tabs.*
import com.evolvingtheconversation.app.ui.screens.learning.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.evolvingtheconversation.viewmodel.EnrollmentSummary
import com.evolvingtheconversation.app.dailychallenge.DailyChallengeScreen
import com.evolvingtheconversation.app.dailychallenge.DailyChallengeType
import com.evolvingtheconversation.app.dailychallenge.DailyChallengeService
import com.evolvingtheconversation.app.dailychallenge.findActiveCourseAndLesson
import com.evolvingtheconversation.app.data.LanguageManager

// ── NOTE: add these three routes to Screen.kt using whatever pattern your
// other simple, no-argument routes already follow (mirror Screen.LiveClasses),
// e.g.:
//   object TeacherAuth : Screen("teacherAuth")
//   object TeacherDashboard : Screen("teacherDashboard")   // "My Classes"
//   object AddClass : Screen("addClass")

private val bottomBarHiddenRoutes = setOf(
    Screen.DailyChallenge.route,
    Screen.CourseDetails.route,
    Screen.Video.route,
    Screen.Speaking.route,
    Screen.Listening.route,
    Screen.Writing.route,
    Screen.Grammar.route,
    Screen.Vocabulary.route,
    Screen.Settings.route,
    Screen.MyProgress.route,
    Screen.Certificates.route,
    Screen.PremiumPlans.route,
    Screen.HelpCenter.route,
    Screen.Search.route,
    Screen.Notifications.route,
    Screen.LiveClasses.route,
    Screen.ChangePassword.route,
    Screen.DeleteAccount.route,
    Screen.TutorChat.route,
    Screen.TeacherAuth.route,
    Screen.TeacherDashboard.route,
    Screen.AddClass.route,
)

/**
 * onLogout / onAccountDeleted are bubbled up to the OUTER NavGraph's navController.
 * This screen's own navController only manages authenticated in-app screens
 * (Home, Profile, Settings, etc.) — it must never own Login/Register/ForgotPassword,
 * since those already live in the outer graph and get destroyed on logout.
 */
@Composable
fun MainAppScaffold(
    onLogout: () -> Unit = {},
    onAccountDeleted: () -> Unit = {}
) {
    val navController  = rememberNavController()
    val navBackStack   by navController.currentBackStackEntryAsState()
    val currentRoute   = navBackStack?.destination?.route ?: ""
    val drawerState    = androidx.compose.material3.rememberDrawerState(DrawerValue.Closed)
    val scope          = rememberCoroutineScope()
    val authViewModel: AuthViewModel = viewModel()
    // Shared across TeacherAuthScreen -> TeacherDashboardScreen -> AddClassScreen
    // so the signed-in teacher profile survives that navigation chain.
    val teacherViewModel: TeacherViewModel = viewModel()

    // Holds the title of a course whose certificate was just earned.
    // Non-null shows the floating banner; set back to null to dismiss.
    var certificateEarnedTitle by remember { mutableStateOf<String?>(null) }

    val showBottomBar = bottomBarHiddenRoutes.none { currentRoute == it } &&
            !currentRoute.startsWith("quiz/")

    ModalNavigationDrawer(
        drawerState     = drawerState,
        gesturesEnabled = showBottomBar,
        drawerContent   = {
            HomeDrawerContent(
                onClose  = { scope.launch { drawerState.close() } },

                onLogout = {
                    scope.launch { drawerState.close() }
                    authViewModel.logout()
                    ThemeManager.resetToDefault()
                    LanguageManager.resetToDefault()
                    onLogout()   // ← bubble up to the outer NavGraph, don't touch this inner navController
                },

                onItemClick = { route ->
                    scope.launch { drawerState.close() }
                    navController.navigate(route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
    ) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) AppBottomNavigation(navController, currentRoute)
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController    = navController,
                    startDestination = Screen.Home.route,
                    modifier         = Modifier.padding(innerPadding)
                ) {
                    // ── CORE TABS ──────────────────────────────────────────────
                    composable(Screen.Home.route) {
                        var enrollments by remember { mutableStateOf<Map<String, EnrollmentSummary>>(emptyMap()) }
                        var xp by remember { mutableStateOf(0) }
                        var streak by remember { mutableStateOf(0) }

                        LaunchedEffect(Unit) {
                            authViewModel.fetchAllEnrollmentsDetailed { data -> enrollments = data }

                            // +5 XP once per day just for opening the app, then load
                            // the real (possibly just-updated) xp/streak to display.
                            authViewModel.awardDailyLoginXpIfNeeded {
                                authViewModel.fetchUserStats { fetchedXp, fetchedStreak ->
                                    xp = fetchedXp
                                    streak = fetchedStreak
                                }
                            }
                        }

                        HomeScreen(
                            enrollments        = enrollments,
                            xp                  = xp,
                            streak              = streak,
                            onCourseClick      = { courseId -> navController.navigate(Screen.CourseDetails.createRoute(courseId)) },
                            onBrowseCoursesClick = { navController.navigate(Screen.Courses.route) },
                            onGrammarClick     = { navController.navigate(Screen.Grammar.route) },
                            onVocabularyClick  = { navController.navigate(Screen.Vocabulary.route) },
                            onSpeakingClick    = { navController.navigate(Screen.Speaking.route) },
                            onListeningClick   = { navController.navigate(Screen.Listening.route) },
                            onMenuClick        = { scope.launch { drawerState.open() } },
                            onSearchClick      = { navController.navigate(Screen.Search.route) },
                            onNotifClick       = { navController.navigate(Screen.Notifications.route) },
                            onLiveClassesClick = { navController.navigate(Screen.LiveClasses.route) },
                            onDailyChallengeClick = { navController.navigate(Screen.DailyChallenge.route) }
                        )
                    }

                    // ── DAILY CHALLENGE ──────────────────────────────────────────
                    composable(Screen.DailyChallenge.route) {
                        var enrollments by remember { mutableStateOf<Map<String, EnrollmentSummary>>(emptyMap()) }
                        LaunchedEffect(Unit) {
                            authViewModel.fetchAllEnrollmentsDetailed { data -> enrollments = data }
                        }

                        DailyChallengeScreen(
                            enrollments = enrollments,
                            onBack = { navController.popBackStack() },
                            onStartChallenge = { type ->
                                when (type) {
                                    DailyChallengeType.WATCH_LESSON -> {
                                        val active = findActiveCourseAndLesson(enrollments)
                                        active?.let { (courseId, lessonId) ->
                                            navController.navigate(Screen.Video.createRoute(courseId, lessonId))
                                        }
                                    }
                                    DailyChallengeType.GRAMMAR_QUIZ -> navController.navigate(Screen.Grammar.route)
                                    DailyChallengeType.GENERAL_QUIZ -> navController.navigate(Screen.Quiz.createRoute("General"))
                                    DailyChallengeType.ESSAY_WRITING -> navController.navigate(Screen.Writing.route)
                                    DailyChallengeType.AI_SPEAKING -> navController.navigate(Screen.TutorChat.route)
                                }
                            }
                        )
                    }

                    composable(Screen.Courses.route) {
                        var completedLessonsByCourse by remember { mutableStateOf<Map<String, Set<String>>>(emptyMap()) }
                        LaunchedEffect(Unit) {
                            authViewModel.fetchAllEnrollments { data -> completedLessonsByCourse = data }
                        }

                        CoursesScreen(
                            completedLessonsByCourse = completedLessonsByCourse,
                            onCourseClick = { courseId -> navController.navigate(Screen.CourseDetails.createRoute(courseId)) },
                            onSearchClick = { navController.navigate(Screen.Search.route) },
                            onNotifClick  = { navController.navigate(Screen.Notifications.route) }
                        )
                    }
                    composable(Screen.Practice.route) {
                        PracticeScreen(
                            onSpeakingClick   = { navController.navigate(Screen.Speaking.route) },
                            onListeningClick  = { navController.navigate(Screen.Listening.route) },
                            onWritingClick    = { navController.navigate(Screen.Writing.route) },
                            onGrammarClick    = { navController.navigate(Screen.Grammar.route) },
                            onVocabularyClick = { navController.navigate(Screen.Vocabulary.route) },
                            onSearchClick     = { navController.navigate(Screen.Search.route) },
                            onNotifClick      = { navController.navigate(Screen.Notifications.route) },
                            onQuizClick       = { navController.navigate(Screen.Quiz.createRoute("General")) },
                            onTutorClick      = { navController.navigate(Screen.TutorChat.route) }
                        )
                    }

                    composable(Screen.Community.route) {
                        CommunityScreen(
                            onSearchClick = { navController.navigate(Screen.Search.route) },
                            onNotifClick  = { navController.navigate(Screen.Notifications.route) }
                        )
                    }

                    composable(Screen.Profile.route) {
                        ProfileScreen(
                            onProgressClick     = { navController.navigate(Screen.MyProgress.route) },
                            onCertificatesClick = { navController.navigate(Screen.Certificates.route) },
                            onPremiumClick      = { navController.navigate(Screen.PremiumPlans.createRoute()) },
                            onSettingsClick     = { navController.navigate(Screen.Settings.route) },
                            onHelpClick         = { navController.navigate(Screen.HelpCenter.route) },
                            onMenuClick         = { scope.launch { drawerState.open() } },
                            onNotifClick        = { navController.navigate(Screen.Notifications.route) }
                        )
                    }

                    // ── HOME OVERLAYS ──────────────────────────────────────────
                    composable(Screen.Search.route) {
                        SearchScreen(
                            onBack = { navController.popBackStack() },
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(Screen.Notifications.route) {
                        NotificationsScreen(onBack = { navController.popBackStack() })
                    }

                    composable(Screen.LiveClasses.route) {
                        LiveClassesScreen(
                            onBack = { navController.popBackStack() },
                            onTeacherClick = { navController.navigate(Screen.TeacherAuth.route) }
                        )
                    }

                    // ── TEACHER FLOW ────────────────────────────────────────────
                    composable(Screen.TeacherAuth.route) {
                        TeacherAuthScreen(
                            teacherViewModel = teacherViewModel,
                            onBack = { navController.popBackStack() },
                            onAuthSuccess = {
                                navController.navigate(Screen.TeacherDashboard.route) {
                                    popUpTo(Screen.TeacherAuth.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.TeacherDashboard.route) {
                        TeacherDashboardScreen(
                            teacherViewModel = teacherViewModel,
                            onAddClassClick = { navController.navigate(Screen.AddClass.route) },
                            onDone = {
                                // exitTeacherMode() just clears local state — no
                                // FirebaseAuth sign-out happens (see TeacherViewModel).
                                navController.popBackStack(Screen.LiveClasses.route, inclusive = false)
                            }
                        )
                    }

                    composable(Screen.AddClass.route) {
                        AddClassScreen(
                            teacherViewModel = teacherViewModel,
                            onBack = { navController.popBackStack() },
                            onSaved = { navController.popBackStack() }
                        )
                    }

                    // ── COURSE FLOW ────────────────────────────────────────────
                    composable(
                        route     = Screen.CourseDetails.route,
                        arguments = listOf(navArgument("courseId") { type = NavType.StringType })
                    ) { backStack ->
                        val courseId = backStack.arguments?.getString("courseId") ?: return@composable

                        var completedIds by remember(courseId) { mutableStateOf<Set<String>>(emptySet()) }
                        var isEnrolled by remember(courseId) { mutableStateOf(false) }
                        var showPaymentSuccess by remember(courseId) { mutableStateOf(false) }

                        LaunchedEffect(courseId) {
                            authViewModel.fetchEnrollment(courseId) { ids, _, enrolled ->
                                completedIds = ids
                                isEnrolled = enrolled
                            }

                            // If we're arriving back here right after a "purchase" on the
                            // Premium Plans screen, PremiumPlans left a flag on our
                            // savedStateHandle before popping back — pick it up once.
                            val savedStateHandle = backStack.savedStateHandle
                            val justPurchasedId = savedStateHandle.get<String>("justPurchasedCourseId")
                            if (justPurchasedId == courseId) {
                                isEnrolled = true
                                showPaymentSuccess = true
                                savedStateHandle.remove<String>("justPurchasedCourseId")
                            }
                        }

                        CourseDetailScreen(
                            courseId               = courseId,
                            completedLessonIds     = completedIds,
                            isEnrolled             = isEnrolled,
                            showPaymentSuccess      = showPaymentSuccess,
                            onPaymentSuccessDismiss = { showPaymentSuccess = false },
                            onBack                 = { navController.popBackStack() },
                            onLessonClick           = { lessonId ->
                                navController.navigate(Screen.Video.createRoute(courseId, lessonId))
                            },
                            onEnrollClick = {
                                // No payment happens here — just hand off to the plans screen.
                                navController.navigate(Screen.PremiumPlans.createRoute(courseId))
                            }
                        )
                    }

                    composable(
                        route     = Screen.Video.route,
                        arguments = listOf(
                            navArgument("courseId") { type = NavType.StringType },
                            navArgument("lessonId") { type = NavType.StringType }
                        )
                    ) { backStack ->
                        val courseId = backStack.arguments?.getString("courseId") ?: return@composable
                        val lessonId = backStack.arguments?.getString("lessonId") ?: return@composable

                        VideoLessonScreen(
                            courseId   = courseId,
                            lessonId   = lessonId,
                            onBack     = { navController.popBackStack() },
                            onMarkComplete = {
                                authViewModel.markLessonComplete(
                                    courseId = courseId,
                                    lessonId = lessonId,
                                    onDone = { navController.popBackStack() },
                                    onCertificateEarned = { title -> certificateEarnedTitle = title }
                                )
                                DailyChallengeService.markComplete(DailyChallengeType.WATCH_LESSON)
                            }
                        )
                    }

                    // ── PREMIUM PLANS (general, or tied to a specific course) ────
                    composable(
                        route = Screen.PremiumPlans.route,
                        arguments = listOf(navArgument("courseId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        })
                    ) { backStack ->
                        val courseId = backStack.arguments?.getString("courseId")

                        PremiumPlansScreen(
                            onBack = { navController.popBackStack() },
                            onUpgrade = { _ ->
                                // Payments aren't wired up yet — treat plan selection as a
                                // successful purchase for now, and unlock the course it came from.
                                if (courseId != null) {
                                    val firstLessonId = CourseCatalog.getCourse(courseId)
                                        ?.lessons?.firstOrNull()?.id ?: ""
                                    authViewModel.enrollInCourse(courseId, firstLessonId) { success ->
                                        if (success) {
                                            navController.previousBackStackEntry
                                                ?.savedStateHandle
                                                ?.set("justPurchasedCourseId", courseId)
                                        }
                                        navController.popBackStack()
                                    }
                                } else {
                                    // Opened from Profile with no specific course — nothing to unlock.
                                    navController.popBackStack()
                                }
                            }
                        )
                    }

                    // ── PRACTICE FLOW ──────────────────────────────────────────
                    composable(Screen.TutorChat.route) {
                        TutorConversationScreen(
                            onBack = {
                                navController.popBackStack()
                                DailyChallengeService.markComplete(DailyChallengeType.AI_SPEAKING)
                            },
                            onTurnComplete = { authViewModel.recordActivity(15) }
                        )
                    }
                    composable(Screen.Speaking.route) {
                        SpeakingPracticeScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Listening.route) {
                        ListeningPracticeScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Writing.route) {
                        WritingPracticeScreen(onBack = {
                            navController.popBackStack()
                            DailyChallengeService.markComplete(DailyChallengeType.ESSAY_WRITING)
                        })
                    }
                    composable(Screen.Grammar.route) {
                        GrammarScreen(
                            onBack = {
                                navController.popBackStack()
                                DailyChallengeService.markComplete(DailyChallengeType.GRAMMAR_QUIZ)
                            },
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
                            onBack     = { navController.popBackStack() },
                            onFinished = { _, _ -> authViewModel.recordActivity(10) }
                        )
                    }
                    composable(Screen.Vocabulary.route) {
                        VocabularyScreen(onBack = { navController.popBackStack() })
                    }
                    composable(
                        route     = Screen.Quiz.route,
                        arguments = listOf(navArgument("topic") { type = NavType.StringType })
                    ) { backStack ->
                        val topic = backStack.arguments?.getString("topic") ?: "General"
                        QuizScreen(
                            topic = topic,
                            onBack = {
                                navController.popBackStack()
                                DailyChallengeService.markComplete(DailyChallengeType.GENERAL_QUIZ)
                            },
                            onQuizComplete = { _, _, _ -> authViewModel.recordActivity(10) }
                        )
                    }

                    // ── PROFILE FLOW ───────────────────────────────────────────
                    composable(Screen.MyProgress.route) {
                        MyProgressScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Certificates.route) {
                        var earnedCertificates by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
                        LaunchedEffect(Unit) {
                            authViewModel.fetchAllCertificates { data -> earnedCertificates = data }
                        }
                        CertificatesScreen(
                            earnedCertificates = earnedCertificates,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            onBack = {
                                navController.popBackStack()
                            },
                            onChangePassword = {
                                navController.navigate(Screen.ChangePassword.route)
                            },
                            onDeleteAccount = {
                                navController.navigate(Screen.DeleteAccount.route)
                            }
                        )
                    }
                    composable(Screen.ChangePassword.route) {
                        ChangePasswordScreen(
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(Screen.DeleteAccount.route) {
                        DeleteAccountScreen(
                            onBack = {
                                navController.popBackStack()
                            },
                            onDeleted = {
                                ThemeManager.resetToDefault()
                                onAccountDeleted()
                            }
                        )
                    }
                    composable(Screen.HelpCenter.route) {
                        HelpCenterScreen(onBack = { navController.popBackStack() })
                    }

                    // ── NOTE ─────────────────────────────────────────────────────
                    // Login, Register, and ForgotPassword are intentionally NOT
                    // defined here. They belong only to the outer NavGraph, since
                    // this whole scaffold is destroyed and recreated on logout.
                }

                // ── CERTIFICATE-EARNED FLOATING BANNER ──────────────────────────
                // Shown over whatever screen is currently active, dismisses itself
                // after a few seconds, or navigates to Certificates on tap.
                certificateEarnedTitle?.let { title ->
                    CertificateEarnedBanner(
                        courseTitle = title,
                        onClick = {
                            certificateEarnedTitle = null
                            navController.navigate(Screen.Certificates.route) { launchSingleTop = true }
                        },
                        onDismiss = { certificateEarnedTitle = null },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}