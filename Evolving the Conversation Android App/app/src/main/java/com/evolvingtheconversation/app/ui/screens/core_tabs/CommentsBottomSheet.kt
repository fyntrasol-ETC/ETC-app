package com.evolvingtheconversation.app.ui.screens.core_tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evolvingtheconversation.app.viewmodel.CommentItem
import com.evolvingtheconversation.app.viewmodel.CommunityPost
import com.evolvingtheconversation.app.viewmodel.CurrentUserColor
import com.evolvingtheconversation.app.viewmodel.currentUserInitial
import com.evolvingtheconversation.app.viewmodel.formatTimeAgo
import androidx.compose.runtime.getValue
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme
import com.evolvingtheconversation.app.R

/** Safely parses a "#RRGGBB" hex string into a Compose Color, falling back
 *  to the app's accent blue if the stored value is missing/malformed. */
private fun parseHexColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Color(0xFF2979FF)
}

private val SheetBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val TextPrimary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF07112A)
private val TextSecondary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF8E8E93)
private val PlaceholderGray: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF6B7280) else Color(0xFF9CA4B6)
private val MutedLabel: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF6B7280) else Color(0xFFB0B6C3)
private val DividerColor: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color(0xFFF0F0F0)
private val InputFieldBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color(0xFFF4F4F8)
private val InputBorder: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color(0xFFDDE1E7)
private val EmptyIconTint: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF3A3A40) else Color(0xFFDDE1E7)
private val DisabledSendTint: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF3A3A40) else Color(0xFFCCD3E0)
private val CommentBodyText: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFD1D5DB) else Color(0xFF3C3C43)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: CommunityPost,
    // CHANGED: caller now passes the live count from
    // viewModel.commentCounts[post.id] instead of us reading post.commentCount,
    // which is no longer kept up to date server-side. Default to 0 so nothing
    // breaks if a call site hasn't been updated yet, but update your call site.
    commentCount: Int = 0,
    isMine: (authorUid: String) -> Boolean,
    isCommentLiked: (CommentItem) -> Boolean,
    onDismiss: () -> Unit,
    // CHANGED: replyToCommentId -> parentId (matches CommunityViewModel.addComment).
    onAddComment: (text: String, parentId: String?) -> Unit,
    onEditComment: (commentId: String, newText: String) -> Unit,
    onDeleteComment: (commentId: String) -> Unit,
    onLikeComment: (commentId: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var commentInput by remember { mutableStateOf("") }

    // If non-null, the input bar is composing a reply to this comment
    // instead of a fresh top-level comment.
    var replyingToCommentId by remember { mutableStateOf<String?>(null) }

    var editingCommentId by remember { mutableStateOf<String?>(null) }
    var editedText by remember { mutableStateOf("") }
    var commentPendingDelete by remember { mutableStateOf<String?>(null) }

    val maxSheetHeight = (LocalConfiguration.current.screenHeightDp * 0.75f).dp

    // Top-level comments first, each immediately followed by its replies
    // (chronological order) — gives the indented thread look.
    // CHANGED: replyToCommentId.isBlank()/.isNotBlank() -> parentId == null / != null.
    val topLevel = post.comments.filter { it.parentId == null }
    val repliesByParent = post.comments.filter { it.parentId != null }
        .groupBy { it.parentId }
    val orderedComments: List<Pair<CommentItem, Boolean>> = buildList {
        topLevel.forEach { parent ->
            add(parent to false)
            repliesByParent[parent.id]?.forEach { reply -> add(reply to true) }
        }
    }
    val replyingToName = post.comments.find { it.id == replyingToCommentId }?.authorName
    val defaultReplyTargetLabel = stringResource(R.string.comments_default_comment_label)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.comments_sheet_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                // CHANGED: was post.commentCount (frozen field, only ever written as 0
                // and never incremented under the new rules-driven flow).
                Text("$commentCount", fontSize = 14.sp, color = TextSecondary)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = DividerColor, thickness = 1.dp)

            Box(modifier = Modifier.weight(1f, fill = false)) {
                if (orderedComments.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            tint = EmptyIconTint,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(stringResource(R.string.comments_empty_message), fontSize = 14.sp, color = PlaceholderGray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(orderedComments, key = { it.first.id }) { (comment, isReply) ->
                            CommentRow(
                                comment = comment,
                                isReply = isReply,
                                isMine = isMine(comment.authorUid),
                                isLiked = isCommentLiked(comment),
                                isEditing = editingCommentId == comment.id,
                                editedText = editedText,
                                onEditedTextChange = { editedText = it },
                                onStartEdit = {
                                    editedText = comment.text
                                    editingCommentId = comment.id
                                },
                                onCancelEdit = { editingCommentId = null },
                                onSaveEdit = {
                                    onEditComment(comment.id, editedText)
                                    editingCommentId = null
                                },
                                onDeleteClick = { commentPendingDelete = comment.id },
                                onReplyClick = {
                                    // Replying to a reply still threads under the
                                    // original top-level comment, one level deep.
                                    // CHANGED: comment.replyToCommentId -> comment.parentId
                                    replyingToCommentId = if (isReply) comment.parentId else comment.id
                                    commentInput = ""
                                },
                                onLikeClick = { onLikeComment(comment.id) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = DividerColor, thickness = 1.dp)

            // ── "Replying to X" banner ───────────────────────────────────
            if (replyingToCommentId != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.comments_replying_to_format, replyingToName ?: defaultReplyTargetLabel),
                        fontSize = 12.sp,
                        color = Color(0xFF1E5EF3),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(R.string.comments_cancel_button),
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.clickable { replyingToCommentId = null }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(CurrentUserColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(currentUserInitial(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                OutlinedTextField(
                    value = commentInput,
                    onValueChange = { commentInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            if (replyingToCommentId != null) stringResource(R.string.comments_placeholder_reply) else stringResource(R.string.comments_placeholder_new),
                            color = PlaceholderGray
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = InputFieldBg,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color(0xFF1E5EF3)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                val canSend = commentInput.isNotBlank()
                IconButton(
                    onClick = {
                        onAddComment(commentInput, replyingToCommentId)
                        commentInput = ""
                        replyingToCommentId = null
                    },
                    enabled = canSend
                ) {
                    Icon(
                        Icons.Filled.Send,
                        contentDescription = stringResource(R.string.comments_send_description),
                        tint = if (canSend) Color(0xFF1E5EF3) else DisabledSendTint
                    )
                }
            }
        }
    }

    if (commentPendingDelete != null) {
        AlertDialog(
            onDismissRequest = { commentPendingDelete = null },
            title = { Text(stringResource(R.string.comments_delete_confirm_title)) },
            text = { Text(stringResource(R.string.comments_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteComment(commentPendingDelete!!)
                    commentPendingDelete = null
                }) { Text(stringResource(R.string.comments_delete_button), color = Color(0xFFE53935)) }
            },
            dismissButton = {
                TextButton(onClick = { commentPendingDelete = null }) { Text(stringResource(R.string.comments_cancel_button)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentRow(
    comment: CommentItem,
    isReply: Boolean,
    isMine: Boolean,
    isLiked: Boolean,
    isEditing: Boolean,
    editedText: String,
    onEditedTextChange: (String) -> Unit,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    onDeleteClick: () -> Unit,
    onReplyClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val unknownInitial = stringResource(R.string.comments_unknown_initial)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isReply) 32.dp else 0.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (isReply) 28.dp else 34.dp)
                .background(parseHexColor(comment.authorColorHex), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                comment.authorName.firstOrNull()?.uppercaseChar()?.toString() ?: unknownInitial,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = if (isReply) 11.sp else 13.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comment.authorName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(formatTimeAgo(comment.createdAt), fontSize = 12.sp, color = PlaceholderGray)
                if (comment.isEdited) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.comments_edited_label), fontSize = 11.sp, color = MutedLabel)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = editedText,
                    onValueChange = onEditedTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF1E5EF3),
                        unfocusedBorderColor = InputBorder
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row {
                    TextButton(onClick = onCancelEdit) { Text(stringResource(R.string.comments_cancel_button), color = TextSecondary) }
                    Spacer(modifier = Modifier.width(6.dp))
                    TextButton(onClick = onSaveEdit, enabled = editedText.isNotBlank()) {
                        Text(stringResource(R.string.community_save_button), color = Color(0xFF1E5EF3), fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Text(comment.text, fontSize = 14.sp, color = CommentBodyText)
                Spacer(modifier = Modifier.height(4.dp))

                // ── Reply + Like row ─────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.comments_reply_label),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.clickable { onReplyClick() }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onLikeClick() }
                    ) {
                        Icon(
                            if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = stringResource(R.string.comments_like_description),
                            tint = if (isLiked) Color(0xFFE53935) else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${comment.likeCount}", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        }

        if (isMine && !isEditing) {
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.comments_options_description),
                        tint = PlaceholderGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.comments_menu_edit_label)) },
                        onClick = { onStartEdit(); menuExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.comments_menu_delete_label), color = Color(0xFFE53935)) },
                        onClick = { onDeleteClick(); menuExpanded = false }
                    )
                }
            }
        }
    }
}