package com.evolvingtheconversation.app.navigation

sealed class Screen(val route: String) {
    // Auth
    object Splash          : Screen("splash")
    object Onboard         : Screen("onboard")
    object Login           : Screen("login")
    object Register        : Screen("register")
    object ForgotPassword  : Screen("forgot_password")

    // Main Tabs
    object Home        : Screen("home")
    object Courses     : Screen("courses")
    object Practice    : Screen("practice")
    object Community   : Screen("community")
    object Profile     : Screen("profile")

    // Sub-screens
    object CourseDetails : Screen("course_details/{courseId}") {
        fun createRoute(courseId: String) = "course_details/$courseId"
    }
    object Video : Screen("video/{courseId}/{lessonId}") {
        fun createRoute(courseId: String, lessonId: String) = "video/$courseId/$lessonId"
    }
    object Quiz : Screen("quiz/{topic}") {
        fun createRoute(topic: String) = "quiz/$topic"
    }
    object Settings        : Screen("settings")
    object ChangePassword  : Screen("change_password")
    object DeleteAccount   : Screen("delete_account")

    // Learning
    object Grammar       : Screen("grammar")
    object Vocabulary    : Screen("vocabulary")
    object Speaking      : Screen("speaking")
    object Listening     : Screen("listening")
    object Writing       : Screen("writing")
    object TutorChat     : Screen("tutor_chat")

    // Profile sub-screens
    object MyProgress    : Screen("my_progress")
    object Certificates  : Screen("certificates")

    object PremiumPlans : Screen("premium_plans?courseId={courseId}") {
        fun createRoute(courseId: String? = null): String =
            if (courseId != null) "premium_plans?courseId=$courseId" else "premium_plans"
    }
    object HelpCenter    : Screen("help_center")

    object TeacherAuth : Screen("teacherAuth")
    object TeacherDashboard : Screen("teacherDashboard")
    object AddClass : Screen("addClass")

    // Home overlays
    object Search        : Screen("search")
    object Notifications : Screen("notifications")
    object LiveClasses   : Screen("live_classes")

    object DailyChallenge : Screen("daily_challenge")
}