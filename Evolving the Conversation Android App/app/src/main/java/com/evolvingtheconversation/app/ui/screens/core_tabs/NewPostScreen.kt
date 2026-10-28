package com.evolvingtheconversation.app.ui.screens.core_tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evolvingtheconversation.app.ui.components.BackButton
import com.evolvingtheconversation.app.viewmodel.CurrentUserColor
import com.evolvingtheconversation.app.viewmodel.PostCategory
import com.evolvingtheconversation.app.viewmodel.currentUserDisplayName
import com.evolvingtheconversation.app.viewmodel.currentUserInitial
import androidx.compose.runtime.getValue
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme
import com.evolvingtheconversation.app.R

private const val MAX_POST_LENGTH = 500

private val ScreenBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFF4F4F8)
private val TextPrimary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF07112A)
private val TextSecondary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF8E8E93)
private val LabelSecondary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF626B80)
private val CardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val InputBorder: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color(0xFFE2E4E9)
private val PlaceholderGray: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF6B7280) else Color(0xFF9CA4B6)
private val DisabledButtonBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color(0xFFCCD3E0)
private val ChipBorder: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color(0xFFDDE1E7)
private val ChipIconUnselected: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF8E8E93)
private val ChipTextUnselected: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)

@Composable
fun NewPostScreen(
    onBack: () -> Unit = {},
    onSubmit: (text: String, category: PostCategory) -> Unit = { _, _ -> }
) {
    var text by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(PostCategory.QUESTION) }

    // Same name shown on Home/Profile — pulled from Firebase Auth's
    // displayName, which was set at sign-up (see AuthViewModel.signUp).
    val authorName = currentUserDisplayName()
    val authorInitial = currentUserInitial()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .statusBarsPadding()
    ) {
        // ── Top bar ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            BackButton(onBack = onBack)
            Text(
                stringResource(R.string.newpost_title),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = TextPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Author row ────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(CurrentUserColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(authorInitial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(authorName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Text(stringResource(R.string.newpost_posting_to_community), fontSize = 13.sp, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                stringResource(R.string.newpost_prompt),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                stringResource(R.string.newpost_type_label),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = LabelSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Post type selector — responsive: each chip takes equal
            // width via weight(1f), so it scales cleanly across screen sizes
            // instead of relying on fixed widths.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PostCategory.values().forEach { category ->
                    PostTypeChip(
                        category = category,
                        isSelected = category == selectedCategory,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedCategory = category }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Text input with inline character counter ────────────────────
            // Bounded with a max height (not just min) so long posts scroll
            // *inside* the box instead of pushing everything below it off
            // the visible screen.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp, max = 220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardWhite)
                    .border(1.dp, InputBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TextField(
                        value = text,
                        onValueChange = { if (it.length <= MAX_POST_LENGTH) text = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        placeholder = { Text(stringResource(R.string.newpost_placeholder), color = PlaceholderGray) },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            cursorColor = Color(0xFF1E5EF3)
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = TextPrimary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                    )
                    Text(
                        // Just a numeric counter ("123/500") — no translatable
                        // words in it, so left as a plain interpolated string.
                        text = "${text.length}/$MAX_POST_LENGTH",
                        fontSize = 12.sp,
                        color = PlaceholderGray,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // ── Post button — pinned outside the scrollable area, so it's
        // always visible regardless of content length or keyboard state ───
        val canPost = text.isNotBlank()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (canPost) Color(0xFF1E5EF3) else DisabledButtonBg)
                .clickable(enabled = canPost) {
                    onSubmit(text.trim(), selectedCategory)
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.newpost_post_button), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun PostTypeChip(
    category: PostCategory,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val (selectedColor, icon) = when (category) {
        PostCategory.QUESTION -> Color(0xFFF5A623) to Icons.Filled.Info
        PostCategory.TIP -> Color(0xFF34A853) to Icons.Filled.Lightbulb
        PostCategory.OTHER -> Color(0xFF7C4DFF) to Icons.Outlined.ChatBubbleOutline
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(if (isSelected) selectedColor else CardWhite)
            .border(
                width = 1.dp,
                color = if (isSelected) selectedColor else ChipBorder,
                shape = RoundedCornerShape(50.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            // category.label comes from the PostCategory enum in the
            // viewmodel package — out of scope for this file's conversion.
            contentDescription = category.label,
            tint = if (isSelected) Color.White else ChipIconUnselected,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            category.label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) Color.White else ChipTextUnselected
        )
    }
}