package com.evolvingtheconversation.app.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.evolvingtheconversation.app.R

data class OnboardingData(
    val title: String,
    val description: String,
    val iconType: IconType,
    val tileBgColor: Color,
    val iconColor: Color
)

enum class IconType { BOOK, MICROPHONE, TRENDING_UP }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardScreen(onSkip: () -> Unit, onGetStarted: () -> Unit) {
    val items = remember {
        listOf(
            OnboardingData("Learn English Every Day", "Improve your communication skills through structured, bite-sized lessons that fit your routine.", IconType.BOOK, Color(0xFFEBF1FF), Color(0xFF1E5EF3)),
            OnboardingData("Practice with Confidence", "Enhance speaking, listening, grammar, and vocabulary with an AI tutor that listens and guides.", IconType.MICROPHONE, Color(0xFFE8F8F2), Color(0xFF0CBD85)),
            OnboardingData("Track Your Progress", "Monitor achievements, keep your streak alive, and stay motivated every step of the way.", IconType.TRENDING_UP, Color(0xFFFFF6E6), Color(0xFFFF9F0A))
        )
    }

    val pagerState = rememberPagerState(pageCount = { items.size })
    val scope = rememberCoroutineScope()

    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFBFBFD)).systemBarsPadding()) {
        // TOP HEADER
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            if (pagerState.currentPage > 0) {
                Text("Back", color = Color(0xFF7D8396), fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } })
            } else Spacer(Modifier.size(1.dp))

            if (pagerState.currentPage < items.size - 1) {
                Text("Skip", color = Color(0xFF7D8396), fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { onSkip() })
            }
        }

        // CAROUSEL
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().weight(1f)) { page ->
            val data = items[page]
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.size(190.dp).background(data.tileBgColor, RoundedCornerShape(54.dp)), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(120.dp).shadow(elevation = 8.dp, shape = RoundedCornerShape(38.dp), spotColor = data.iconColor).background(data.iconColor, RoundedCornerShape(38.dp)), contentAlignment = Alignment.Center) {
                        OnboardingVectorIcon(type = data.iconType)
                    }
                }
                Spacer(modifier = Modifier.height(48.dp))
                Text(text = data.title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF07112A), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(14.dp))
                Text(text = data.description, fontSize = 15.sp, color = Color(0xFF626B80), textAlign = TextAlign.Center)
            }
        }

        // FOOTER
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(items.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val widthAnim by animateDpAsState(if (isSelected) 22.dp else 7.dp, label = "")
                    Box(modifier = Modifier.size(height = 7.dp, width = widthAnim).background(if (isSelected) Color(0xFF1E5EF3) else Color(0xFFE2E4E9), CircleShape))
                }
            }

            val isLastPage = pagerState.currentPage == items.size - 1
            Button(
                onClick = { if (isLastPage) onGetStarted() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E5EF3))
            ) {
                Text(text = if (isLastPage) "Get Started" else "Next", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun OnboardingVectorIcon(type: IconType) {
    val iconRes = when (type) {
        IconType.BOOK -> R.drawable.baseline_menu_book_24
        IconType.MICROPHONE -> R.drawable.baseline_mic_24
        IconType.TRENDING_UP -> R.drawable.baseline_trending_up_24
    }

    Icon(
        painter = painterResource(id = iconRes),
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(32.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun OnboardScreenPreview() {
    OnboardScreen(onSkip = {}, onGetStarted = {})
}