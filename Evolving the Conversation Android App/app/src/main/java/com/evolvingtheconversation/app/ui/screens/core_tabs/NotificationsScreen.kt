package com.evolvingtheconversation.app.ui.screens.core_tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Calendar

private val NBlue    = Color(0xFF1E5EF3)
private val NGreen   = Color(0xFF0CBD85)
private val NOrange  = Color(0xFFFFA800)
private val NRed     = Color(0xFFE53935)
private val NBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFF4F6FB)
private val NText: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)
private val NSub: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)
private val NCard: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val NDivider: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color(0xFFE5E7EB)

// NEW: plain data class matching what's saved in Firestore
data class NotifItem(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val timeText: String = "",
    val read: Boolean = false
)

// NEW: pick an icon/color based on the "type" field written by Cloud Functions
private fun iconFor(type: String): Triple<ImageVector, Color, Color> = when (type) {
    "community_new_post" -> Triple(Icons.Outlined.MenuBook, Color(0xFFEEF3FF), NBlue)
    "reply_to_you" -> Triple(Icons.Outlined.Videocam, Color(0xFFFFEEEE), NRed)
    "daily_challenge_reminder" -> Triple(Icons.Outlined.EmojiEvents, Color(0xFFEAFFF7), NGreen)
    else -> Triple(Icons.Outlined.LocalOffer, Color(0xFFFFF4E5), NOrange)
}

@Composable
fun NotificationsScreen(onBack: () -> Unit = {}) {
    // NEW: live list of today's notifications, updates automatically
    var notifications by remember { mutableStateOf<List<NotifItem>>(emptyList()) }

    DisposableEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        var registration: com.google.firebase.firestore.ListenerRegistration? = null

        if (uid != null) {
            val startOfToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            registration = FirebaseFirestore.getInstance()
                .collection("users").document(uid).collection("notifications")
                .whereGreaterThanOrEqualTo("createdAt", startOfToday)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snap, _ ->
                    if (snap != null) {
                        notifications = snap.documents.map { doc ->
                            NotifItem(
                                id = doc.id,
                                type = doc.getString("type") ?: "",
                                title = doc.getString("title") ?: "",
                                body = doc.getString("body") ?: "",
                                timeText = "now", // you can format doc.getTimestamp("createdAt") later
                                read = doc.getBoolean("read") ?: false
                            )
                        }
                    }
                }
        }

        onDispose { registration?.remove() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NBg)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NCard)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .shadow(4.dp, RoundedCornerShape(10.dp), spotColor = Color.Black.copy(0.12f))
                    .background(NCard, RoundedCornerShape(10.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = "Back",
                    tint = NText, modifier = Modifier.size(18.dp))
            }
            Text("Notifications", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = NText)
        }

        Divider(color = NDivider, thickness = 1.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (notifications.isEmpty()) {
                Text("No notifications yet today.", color = NSub, fontSize = 14.sp)
            }
            notifications.forEach { notif ->
                NotifCard(notif)
            }
        }
    }
}

@Composable
private fun NotifCard(item: NotifItem) {
    val (icon, iconBg, iconTint) = iconFor(item.type)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation    = 6.dp,
                shape        = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(0.06f),
                spotColor    = Color.Black.copy(0.10f)
            )
            .background(NCard, RoundedCornerShape(16.dp))
            .clickable {
                FirebaseFirestore.getInstance()
                    .collection("users").document(FirebaseAuth.getInstance().currentUser?.uid ?: "")
                    .collection("notifications").document(item.id)
                    .update("read", true)
            }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .shadow(4.dp, RoundedCornerShape(13.dp), spotColor = iconTint.copy(0.25f))
                .background(iconBg, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NText)
            Spacer(Modifier.height(4.dp))
            Text(item.body, fontSize = 13.sp, color = NSub, lineHeight = 19.sp)
        }

        Text(item.timeText, fontSize = 12.sp, color = NSub, fontWeight = FontWeight.Medium)
    }
}