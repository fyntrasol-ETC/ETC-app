package com.evolvingtheconversation.app.ui.screens.core_tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evolvingtheconversation.app.dailychallenge.DailyChallengeService
import com.evolvingtheconversation.app.data.CourseCatalog
import com.evolvingtheconversation.app.data.search.RecentSearchStore
import com.evolvingtheconversation.app.data.search.SearchItem
import com.evolvingtheconversation.app.data.search.globalSearchItems
import com.evolvingtheconversation.app.data.search.matchesQuery
import com.evolvingtheconversation.app.navigation.Screen
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme
import com.evolvingtheconversation.app.R

private val SBlue      = Color(0xFF1E5EF3)
private val SGray: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)
private val SBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFF4F6FB)
private val SCardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val SText: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)
private val SDivider: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color(0xFFE5E7EB)

private val ResultIconBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1B2540) else Color(0xFFEEF3FF)

// Search-suggestion chips shown in the empty-results state. Resource IDs
// instead of raw strings — resolved inside the Composable below.
private val searchSuggestionResIds = listOf(
    R.string.search_suggestion_grammar,
    R.string.search_suggestion_vocabulary,
    R.string.search_suggestion_speaking,
    R.string.search_suggestion_ielts,
    R.string.search_suggestion_quiz
)

@Composable
fun SearchScreen(
    onBack: () -> Unit = {},
    onNavigate: (route: String) -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(Unit) { DailyChallengeService.listen() }

    val dailyChallenge by DailyChallengeService.challenge.collectAsStateWithLifecycle()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val recentSearches by RecentSearchStore.recentSearchesFlow(context, uid)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val filteredResults by remember {
        derivedStateOf { globalSearchItems.filter { it.matchesQuery(query) } }
    }

    // Course results resolve to that course's first lesson (Screen.Video),
    // not the generic Courses screen. Matched by title against CourseCatalog
    // rather than a hardcoded id, so it stays correct even if course ids change.
    // NOTE: "Daily Challenges" and "Course" here are internal matching keys
    // against globalSearchItems data (defined elsewhere), not displayed UI
    // copy — left as-is per translation scope.
    fun resolveRoute(item: SearchItem): String {
        if (item.title == "Daily Challenges") {
            val isCompleted = dailyChallenge?.isCompleted == true
            return Screen.DailyChallenge.route
        }

        if (item.category == "Course") {
            val course = CourseCatalog.courses.firstOrNull { it.title == item.title }
            val firstLesson = course?.lessons?.firstOrNull()
            if (course != null && firstLesson != null) {
                return Screen.Video.createRoute(course.id, firstLesson.id)
            }
        }

        return item.route
    }

    fun executeResult(item: SearchItem) {
        val committedQuery = query
        scope.launch { RecentSearchStore.addSearch(context, uid, committedQuery) }
        onNavigate(resolveRoute(item))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SBg)
            .statusBarsPadding()
    ) {

        // ── SEARCH BAR ROW ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SCardWhite)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Back button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .shadow(4.dp, RoundedCornerShape(10.dp),
                        spotColor = Color.Black.copy(0.15f))
                    .background(SCardWhite, RoundedCornerShape(10.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = stringResource(R.string.search_back_description),
                    tint = SText, modifier = Modifier.size(18.dp))
            }

            // Search input box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .shadow(4.dp, RoundedCornerShape(14.dp),
                        spotColor = Color.Black.copy(0.10f))
                    .background(SCardWhite, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Search, contentDescription = null,
                        tint = SGray, modifier = Modifier.size(20.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(stringResource(R.string.search_placeholder),
                                fontSize = 14.sp, color = SGray)
                        }
                        BasicTextField(
                            value         = query,
                            onValueChange = { query = it },
                            textStyle     = TextStyle(fontSize = 14.sp, color = SText),
                            cursorBrush   = SolidColor(SBlue),
                            modifier      = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    }
                    if (query.isNotEmpty()) {
                        Icon(Icons.Outlined.Cancel, contentDescription = stringResource(R.string.search_clear_description),
                            tint     = SGray,
                            modifier = Modifier.size(18.dp).clickable { query = "" })
                    }
                }
            }
        }

        Divider(color = SDivider, thickness = 1.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 24.dp)
        ) {
            if (query.isEmpty()) {
                // ── RECENT ────────────────────────────────────────────────────
                if (recentSearches.isNotEmpty()) {
                    Text(
                        stringResource(R.string.search_recent_label),
                        fontSize      = 11.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = SGray,
                        letterSpacing = 1.2.sp,
                        modifier      = Modifier.padding(start = 20.dp, bottom = 10.dp)
                    )

                    recentSearches.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { query = item }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Icon(Icons.Outlined.History, contentDescription = null,
                                    tint = SGray, modifier = Modifier.size(20.dp))
                                Text(item, fontSize = 15.sp, color = SText,
                                    fontWeight = FontWeight.Medium)
                            }
                            Icon(Icons.Outlined.ArrowForwardIos, contentDescription = null,
                                tint = SGray, modifier = Modifier.size(14.dp))
                        }
                        Divider(color = SDivider, thickness = 0.8.dp,
                            modifier = Modifier.padding(horizontal = 20.dp))
                    }
                }

            } else if (filteredResults.isEmpty()) {
                // ── EMPTY STATE ──────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.SearchOff,
                        contentDescription = null,
                        tint = SGray,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.search_no_results_title), fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold, color = SText)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.search_try_searching), fontSize = 13.sp, color = SGray)
                    Spacer(Modifier.height(6.dp))
                    searchSuggestionResIds.forEach { resId ->
                        Text(
                            "• ${stringResource(resId)}",
                            fontSize = 13.sp,
                            color = SGray,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

            } else {
                // ── LIVE RESULTS ─────────────────────────────────────────────
                Text(
                    stringResource(R.string.search_results_for, query.uppercase()),
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = SGray,
                    letterSpacing = 1.2.sp,
                    modifier      = Modifier.padding(start = 20.dp, bottom = 10.dp)
                )

                filteredResults.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { executeResult(item) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .shadow(4.dp, RoundedCornerShape(12.dp),
                                    spotColor = SBlue.copy(0.2f))
                                .background(ResultIconBg, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(item.icon, contentDescription = null,
                                tint = SBlue, modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            // item.title / item.category come from globalSearchItems
                            // (a separate data file, not this screen) — left as dynamic
                            // data, not converted here.
                            Text(item.title, fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold, color = SText)
                            Text(item.category,
                                fontSize = 13.sp, color = SGray)
                        }
                        Icon(Icons.Outlined.ArrowForwardIos, contentDescription = null,
                            tint = SGray, modifier = Modifier.size(14.dp))
                    }
                    Divider(color = SDivider, thickness = 0.8.dp,
                        modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
        }
    }
}