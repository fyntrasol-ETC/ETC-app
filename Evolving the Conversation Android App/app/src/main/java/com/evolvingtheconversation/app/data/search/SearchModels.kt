package com.evolvingtheconversation.app.data.search

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.evolvingtheconversation.app.navigation.Screen

/**
 * One globally searchable entry. This list is the single source of truth
 * for global search — to make a new screen searchable later, just add
 * another SearchItem here. No other code needs to change.
 */
data class SearchItem(
    val title: String,
    val category: String,
    val keywords: List<String>,
    val icon: ImageVector,
    val route: String
)

val globalSearchItems: List<SearchItem> = listOf(
    // ── Courses — route here is a placeholder; SearchScreen.resolveRoute()
    // overrides it at tap-time to jump straight into that course's first
    // lesson (Screen.Video) instead of the generic Courses screen. ──────
    SearchItem(
        title = "Spoken English Mastery",
        category = "Course",
        keywords = listOf("spoken english mastery", "spoken english", "spoken", "course", "courses"),
        icon = Icons.Outlined.MenuBook,
        route = Screen.Courses.route
    ),
    SearchItem(
        title = "English Grammar Pro",
        category = "Course",
        keywords = listOf("english grammar pro", "grammar course", "grammar", "course", "courses"),
        icon = Icons.Outlined.MenuBook,
        route = Screen.Courses.route
    ),
    SearchItem(
        title = "Business English",
        category = "Course",
        keywords = listOf("business english", "business", "course", "courses"),
        icon = Icons.Outlined.MenuBook,
        route = Screen.Courses.route
    ),
    SearchItem(
        title = "IELTS Band 8 Prep",
        category = "Course",
        keywords = listOf("ielts band 8 prep", "ielts band 8", "ielts prep", "ielts", "course", "courses"),
        icon = Icons.Outlined.MenuBook,
        route = Screen.Courses.route
    ),

    // ── Practice features ─────────────────────────────────────────────────
    SearchItem("Grammar Practice", "Practice", listOf("grammar", "grammar practice"), Icons.Outlined.LibraryBooks, Screen.Grammar.route),
    SearchItem("Vocabulary Practice", "Practice", listOf("vocabulary", "vocab", "words"), Icons.Outlined.Style, Screen.Vocabulary.route),
    SearchItem("Speaking Practice", "Practice", listOf("speaking", "speaking practice", "speaking lessons"), Icons.Outlined.Mic, Screen.Speaking.route),
    SearchItem("Listening Practice", "Practice", listOf("listening", "listening practice"), Icons.Outlined.Headphones, Screen.Listening.route),
    SearchItem("Writing Practice", "Practice", listOf("writing", "writing practice"), Icons.Outlined.Create, Screen.Writing.route),
    SearchItem("AI Tutor", "AI", listOf("ai", "ai tutor", "tutor", "chatbot"), Icons.Outlined.AutoAwesome, Screen.TutorChat.route),
    SearchItem("Quiz Centre", "Practice", listOf("quiz", "quizzes", "quiz center", "quiz centre"), Icons.Outlined.Adjust, Screen.Quiz.createRoute("General")),

    // ── Live classes ──────────────────────────────────────────────────────
    SearchItem("Live Classes", "Classes", listOf("live", "live classes", "classes"), Icons.Outlined.VideoCameraFront, Screen.LiveClasses.route),

    // ── Daily challenge — destination is resolved at tap-time based on the
    // real completion state read from DailyChallengeService (see SearchScreen) ──
    SearchItem("Daily Challenges", "Challenges", listOf("challenge", "challenges", "daily challenge", "daily challenges"), Icons.Outlined.EmojiEvents, Screen.DailyChallenge.route),

    // ── Profile tab — ported over from the iOS AppScreenDestination list,
    // these were missing from Android search entirely ───────────────────
    SearchItem("My Progress", "Profile", listOf("my progress", "progress", "stats", "activity"), Icons.Outlined.BarChart, Screen.MyProgress.route),
    SearchItem("Certificates", "Profile", listOf("certificate", "certificates", "certification"), Icons.Outlined.CardMembership, Screen.Certificates.route),
    SearchItem("Premium Plans", "Profile", listOf("premium", "premium plans", "upgrade", "subscription"), Icons.Outlined.EmojiEvents, Screen.PremiumPlans.createRoute()),
    SearchItem("Settings", "Profile", listOf("settings", "preferences"), Icons.Outlined.Settings, Screen.Settings.route),
    SearchItem("Help Center", "Profile", listOf("help", "help center", "support", "faq"), Icons.Outlined.HelpOutline, Screen.HelpCenter.route),
    SearchItem("Change Password", "Profile", listOf("change password", "password", "reset password"), Icons.Outlined.Lock, Screen.ChangePassword.route),
    SearchItem("Delete Account", "Profile", listOf("delete account", "delete", "remove account"), Icons.Outlined.PersonRemove, Screen.DeleteAccount.route)
)

/**
 * Case-insensitive match: true if [query] is a substring of the title or any
 * keyword, or a prefix of any individual word in the title/keywords. Covers
 * partial matches, prefix matches, and full matches from the spec.
 */
fun SearchItem.matchesQuery(query: String): Boolean {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return false

    val haystacks = keywords + title
    return haystacks.any { text ->
        val lower = text.lowercase()
        lower.contains(q) || lower.split(" ").any { word -> word.startsWith(q) }
    }
}