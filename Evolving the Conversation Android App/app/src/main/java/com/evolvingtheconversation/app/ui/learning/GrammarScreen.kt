package com.evolvingtheconversation.app.ui.screens.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evolvingtheconversation.app.ui.components.BackButton
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme
import com.evolvingtheconversation.app.R

private val QSurface: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFF0F2F8)
private val QCardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val QBlue         = Color(0xFF1E5EF3)
private val QIconBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF23304A) else Color(0xFFE7ECFC)
private val QTextPri: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)

/**
 * One grammar topic shown as a tappable card on this screen.
 * `title` MUST exactly match a key in `grammarMCQMap` (see GrammarMCQData.kt),
 * since that's how GrammarQuizScreen looks up its question pool — it stays as a
 * stable, untranslated identifier. `titleResId` is the localized label actually
 * shown on screen.
 */
data class GrammarTopic(
    val title: String,
    val titleResId: Int,
    val description: String
)

/** Topic list, in the same order/spelling as the keys in grammarMCQMap. */
val grammarTopics: List<GrammarTopic> = listOf(
    GrammarTopic("Tenses", R.string.grammar_topic_tenses, "Past, present, future and their forms"),
    GrammarTopic("Articles", R.string.grammar_topic_articles, "A, an, and the"),
    GrammarTopic("Prepositions", R.string.grammar_topic_prepositions, "In, on, at, and more"),
    GrammarTopic("Passive Voice", R.string.grammar_topic_passive_voice, "When the subject receives the action"),
    GrammarTopic("Active Voice", R.string.grammar_topic_active_voice, "When the subject performs the action"),
    GrammarTopic("Modals", R.string.grammar_topic_modals, "Can, could, must, should, and more"),
    GrammarTopic("Conditionals", R.string.grammar_topic_conditionals, "If-clauses and their results"),
    GrammarTopic("Reported Speech", R.string.grammar_topic_reported_speech, "Turning direct speech into indirect speech"),
    GrammarTopic("Punctuation", R.string.grammar_topic_punctuation, "Commas, periods, apostrophes, and more")
)

@Composable
fun GrammarScreen(
    onBack: () -> Unit = {},
    onTopicClick: (GrammarTopic) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(onBack = onBack)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.grammar_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = QTextPri)
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
        ) {
            items(grammarTopics) { topic ->
                GrammarTopicCard(
                    topic = topic,
                    onClick = { onTopicClick(topic) }
                )
            }
        }
    }
}

@Composable
private fun GrammarTopicCard(
    topic: GrammarTopic,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(QCardWhite, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 24.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(QIconBg, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Description,
                contentDescription = null,
                tint = QBlue,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(topic.titleResId),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = QTextPri,
            textAlign = TextAlign.Center
        )
    }
}