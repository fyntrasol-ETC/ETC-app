package com.evolvingtheconversation.app.ui.screens.core_tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evolvingtheconversation.app.viewmodel.CommunityPost
import com.evolvingtheconversation.app.viewmodel.CommunityViewModel
import com.evolvingtheconversation.app.viewmodel.PostCategory
import com.evolvingtheconversation.app.viewmodel.formatTimeAgo
import androidx.compose.runtime.getValue
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme
import com.evolvingtheconversation.app.R

private val BgGray: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFF2F2F7)
private val NavyBlue: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF1A2A4A)
private val AccentBlue    = Color(0xFF2979FF)
private val TextGray: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF8E8E93)
private val CardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White

// Added for dark-mode pass — inline gradients/borders/text that used to be hardcoded.
private val IconBoxBorder: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color(0xFFEFF1F6)
private val IconBoxGradientBottom: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF232326) else Color(0xFFF7F8FC)
private val ChipBorderUnselected: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color(0xFFDDE1E7)
private val ChipTextUnselected: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)
private val PostCardGradientBottom: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF232326) else Color(0xFFFAFBFF)
private val PostCardBorder: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color(0xFFF1F2F6)
private val PostTextColor: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFD1D5DB) else Color(0xFF3C3C43)
private val DividerColor: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color(0xFFF0F0F0)
private val EditedLabelColor: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF6B7280) else Color(0xFFB0B6C3)

// CHANGED: filter identity used to be the raw display string itself
// ("For you" / "Questions" / "Tips" / "Featured"), which the filtering
// logic below compared directly. That breaks once the chip label is
// translated. Mirrors the DrawerItem/NavItem shape: hold a resource id,
// resolve it with stringResource() only where it's rendered, and switch
// on the stable enum value everywhere else.
private enum class CommunityFilter(val labelResId: Int) {
    FOR_YOU(R.string.community_filter_for_you),
    QUESTIONS(R.string.community_filter_questions),
    TIPS(R.string.community_filter_tips),
    FEATURED(R.string.community_filter_featured)
}

/** Same tinted-shadow trick used across the app's other "premium" screens. */
private fun Modifier.premiumLift(
    shape: RoundedCornerShape,
    elevation: androidx.compose.ui.unit.Dp = 10.dp,
    tint: Color = Color.Black
): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = tint.copy(alpha = 0.18f),
    spotColor = tint.copy(alpha = 0.26f)
)

/** Safely parses a "#RRGGBB" hex string into a Compose Color, falling back
 *  to the app's accent blue if the stored value is missing/malformed —
 *  guards against a bad write from either platform crashing the feed. */
private fun parseHexColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    AccentBlue
}

@Composable
fun CommunityScreen(
    communityViewModel: CommunityViewModel = viewModel(),
    onSearchClick: () -> Unit = {},
    onNotifClick: () -> Unit = {}
) {
    val filters = CommunityFilter.values().toList()
    var selectedFilter by remember { mutableStateOf(CommunityFilter.FOR_YOU) }

    var showNewPostScreen by remember { mutableStateOf(false) }
    var activeCommentsPostId by remember { mutableStateOf<String?>(null) }

    // Only listen to a post's comments subcollection while its sheet is
    // actually open — avoids paying for reads on every post up front.
    LaunchedEffect(activeCommentsPostId) {
        activeCommentsPostId?.let { communityViewModel.startListeningToComments(it) }
    }
    DisposableEffect(activeCommentsPostId) {
        val id = activeCommentsPostId
        onDispose { id?.let { communityViewModel.stopListeningToComments(it) } }
    }

    var editingPostId by remember { mutableStateOf<String?>(null) }
    var postPendingDelete by remember { mutableStateOf<String?>(null) }

    // category is now the raw Firestore string ("questions" / "tips" / "general"),
    // so filter against PostCategory.<X>.firestoreValue rather than the enum itself.
    val filteredPosts = when (selectedFilter) {
        CommunityFilter.QUESTIONS -> communityViewModel.posts.filter { it.category == PostCategory.QUESTION.firestoreValue }
        CommunityFilter.TIPS -> communityViewModel.posts.filter { it.category == PostCategory.TIP.firestoreValue }
        else -> communityViewModel.posts
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(modifier = Modifier.fillMaxSize().background(BgGray)) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardWhite)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                    Text(stringResource(R.string.community_title), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = NavyBlue)
                    Text(stringResource(R.string.community_subtitle), fontSize = 13.sp, color = TextGray)
                }
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .premiumLift(RoundedCornerShape(12.dp), elevation = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.verticalGradient(listOf(CardWhite, IconBoxGradientBottom)))
                            .border(1.dp, IconBoxBorder, RoundedCornerShape(12.dp))
                            .clickable { onSearchClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.community_search_description), tint = NavyBlue, modifier = Modifier.size(20.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .premiumLift(RoundedCornerShape(12.dp), elevation = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.verticalGradient(listOf(CardWhite, IconBoxGradientBottom)))
                            .border(1.dp, IconBoxBorder, RoundedCornerShape(12.dp))
                            .clickable { onNotifClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box {
                            Icon(Icons.Outlined.Notifications, contentDescription = stringResource(R.string.community_notifications_description), tint = NavyBlue, modifier = Modifier.size(20.dp))
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFFE53935), CircleShape).align(Alignment.TopEnd))
                        }
                    }
                }
            }

            // ── Filter Chips — horizontally scrollable so it never
            // overflows on narrow screens or larger font scales ───────────
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                filters.forEach { filter ->
                    val isSelected = filter == selectedFilter
                    Box(
                        modifier = Modifier
                            .then(
                                if (isSelected)
                                    Modifier.shadow(10.dp, RoundedCornerShape(50.dp), spotColor = AccentBlue.copy(alpha = 0.45f))
                                else
                                    Modifier.shadow(3.dp, RoundedCornerShape(50.dp), spotColor = Color.Black.copy(alpha = 0.1f))
                            )
                            .clip(RoundedCornerShape(50.dp))
                            .then(
                                if (isSelected) Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF4F8EF7), AccentBlue)))
                                else Modifier.background(CardWhite).border(1.dp, ChipBorderUnselected, RoundedCornerShape(50.dp))
                            )
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(filter.labelResId),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Color.White else ChipTextUnselected
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Feed — LazyColumn so it stays smooth as posts/comments grow ─
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp) // keeps last card clear of the FAB
            ) {
                items(filteredPosts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        // CHANGED: was post.commentCount (frozen field). Now read the
                        // live, query-derived count the ViewModel keeps in commentCounts.
                        // Falls back to 0 until the first count query resolves.
                        commentCount = communityViewModel.commentCounts[post.id] ?: 0,
                        isMine = communityViewModel.isMine(post.authorUid),
                        isLiked = communityViewModel.isLikedByMe(post),
                        isEditing = editingPostId == post.id,
                        onLikeClick = { communityViewModel.toggleLike(post.id) },
                        onCommentClick = { activeCommentsPostId = post.id },
                        onEditClick = { editingPostId = post.id },
                        onCancelEdit = { editingPostId = null },
                        onSaveEdit = { newText ->
                            communityViewModel.editPost(post.id, newText)
                            editingPostId = null
                        },
                        onDeleteClick = { postPendingDelete = post.id }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // ── Floating "+" — opens New Post as a full-screen overlay ──────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(58.dp)
                .shadow(16.dp, CircleShape, ambientColor = AccentBlue.copy(alpha = 0.5f), spotColor = AccentBlue.copy(alpha = 0.6f))
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(Color(0xFF1E5EF3), Color(0xFF0CBD85)))
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.5f), Color.Transparent)),
                    shape = CircleShape
                )
                .clickable { showNewPostScreen = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.community_new_post_description), tint = Color.White, modifier = Modifier.size(26.dp))
        }

        // ── New Post overlay ─────────────────────────────────────────────
        if (showNewPostScreen) {
            NewPostScreen(
                onBack = { showNewPostScreen = false },
                onSubmit = { text, category ->
                    communityViewModel.addPost(text, category)
                    showNewPostScreen = false
                }
            )
        }

        // ── Comments sheet ───────────────────────────────────────────────
        val activePost = communityViewModel.posts.find { it.id == activeCommentsPostId }
        if (activePost != null) {
            CommentsBottomSheet(
                post = activePost,
                // CHANGED: pass the live count in, same as PostCard above.
                commentCount = communityViewModel.commentCounts[activePost.id] ?: 0,
                isMine = { authorUid -> communityViewModel.isMine(authorUid) },
                isCommentLiked = { comment -> communityViewModel.isCommentLikedByMe(comment) },
                onDismiss = { activeCommentsPostId = null },
                // CHANGED: replyToCommentId -> parentId (matches
                // CommunityViewModel.addComment's renamed parameter).
                onAddComment = { text, parentId -> communityViewModel.addComment(activePost.id, text, parentId) },
                onEditComment = { commentId, newText -> communityViewModel.editComment(activePost.id, commentId, newText) },
                onDeleteComment = { commentId -> communityViewModel.deleteComment(activePost.id, commentId) },
                onLikeComment = { commentId -> communityViewModel.toggleCommentLike(activePost.id, commentId) }
            )
        } else if (activeCommentsPostId != null) {
            // post was somehow removed from the list — clear the dangling id
            activeCommentsPostId = null
        }

        // ── Delete post confirmation ─────────────────────────────────────
        if (postPendingDelete != null) {
            AlertDialog(
                onDismissRequest = { postPendingDelete = null },
                title = { Text(stringResource(R.string.community_delete_post_title)) },
                text = { Text(stringResource(R.string.community_delete_post_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        communityViewModel.deletePost(postPendingDelete!!)
                        postPendingDelete = null
                    }) { Text(stringResource(R.string.community_delete_button), color = Color(0xFFE53935)) }
                },
                dismissButton = {
                    TextButton(onClick = { postPendingDelete = null }) { Text(stringResource(R.string.community_cancel_button)) }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostCard(
    post: CommunityPost,
    // CHANGED: new param — the live comment count, passed in from
    // CommunityScreen instead of read off post.commentCount here.
    commentCount: Int,
    isMine: Boolean,
    isLiked: Boolean,
    isEditing: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onEditClick: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    var editedText by remember(post.id, isEditing) { mutableStateOf(post.text) }
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .premiumLift(RoundedCornerShape(18.dp), elevation = 12.dp)
            .background(Brush.verticalGradient(listOf(CardWhite, PostCardGradientBottom)), RoundedCornerShape(18.dp))
            .border(1.dp, PostCardBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Header: avatar LEFT, name+time centered, menu RIGHT ────────
            Box(modifier = Modifier.fillMaxWidth().height(48.dp)) {

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .align(Alignment.CenterStart)
                        .shadow(6.dp, CircleShape, spotColor = parseHexColor(post.authorColorHex).copy(alpha = 0.4f))
                        .background(parseHexColor(post.authorColorHex), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(post.authorInitial, color = Color.White,
                        fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(post.authorName, fontWeight = FontWeight.Bold,
                        fontSize = 15.sp, color = NavyBlue)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatTimeAgo(post.createdAt), fontSize = 12.sp, color = TextGray)
                        if (post.isEdited) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.community_post_edited_label), fontSize = 11.sp, color = EditedLabelColor)
                        }
                    }
                }

                // Only the post's own author sees the edit/delete menu,
                // and only when not already editing.
                if (isMine && !isEditing) {
                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.community_post_options_description),
                                tint = TextGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.community_edit_label)) },
                                onClick = {
                                    onEditClick()
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.community_delete_button), color = Color(0xFFE53935)) },
                                onClick = {
                                    onDeleteClick()
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isEditing) {
                // ── Inline edit mode ─────────────────────────────────────
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF1E5EF3),
                        unfocusedBorderColor = ChipBorderUnselected
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onCancelEdit) {
                        Text(stringResource(R.string.community_cancel_button), color = TextGray)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    TextButton(
                        onClick = { onSaveEdit(editedText) },
                        enabled = editedText.isNotBlank()
                    ) {
                        Text(stringResource(R.string.community_save_button), color = Color(0xFF1E5EF3), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text(
                    text       = post.text,
                    fontSize   = 14.sp,
                    color      = PostTextColor,
                    textAlign  = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier   = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(color = DividerColor, thickness = 1.dp)

            Spacer(modifier = Modifier.height(12.dp))

            // ── Like + Comment row ────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable { onLikeClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = stringResource(R.string.community_like_description),
                        tint = if (isLiked) Color(0xFFE53935) else TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("${post.likeCount}", fontSize = 14.sp, color = TextGray)
                }

                Spacer(modifier = Modifier.width(22.dp))

                Row(
                    modifier = Modifier.clickable { onCommentClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = stringResource(R.string.community_comments_description),
                        tint = TextGray, modifier = Modifier.size(19.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    // CHANGED: was post.commentCount, maintained via transaction
                    // increments. That approach silently failed whenever a non-author
                    // replied (the rules only let the post's author touch that field),
                    // so it's now a live count derived from the comments subcollection
                    // and passed in as a parameter instead.
                    Text("$commentCount", fontSize = 14.sp, color = TextGray)
                }
            }
        }
    }
}