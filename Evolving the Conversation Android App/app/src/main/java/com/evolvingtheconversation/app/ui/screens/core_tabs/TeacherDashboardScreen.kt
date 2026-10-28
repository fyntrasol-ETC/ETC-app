package com.evolvingtheconversation.app.ui.screens.core_tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.evolvingtheconversation.viewmodel.LiveClass
import com.evolvingtheconversation.viewmodel.TeacherViewModel
import com.evolvingtheconversation.viewmodel.fetchAllLiveClasses
import java.text.SimpleDateFormat
import java.util.*
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme

private val BlueStart = Color(0xFF1E5EF3)
private val SurfaceBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFF4F6FB)
private val TextPrimary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)
private val TextSecondary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)
private val CardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val RedDate = Color(0xFFE53935)
private val YoursPillBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF232B45) else Color(0xFFE8EFFE)
private val YoursPillText = Color(0xFF1E5EF3)

/**
 * "My Classes" — actually shows EVERY teacher's scheduled classes (any signed-in
 * account can see the full shared schedule), so teachers can coordinate. Delete
 * is only offered on classes THIS teacher created — Firestore rules already
 * enforce that server-side (liveClasses delete requires resource.teacherId ==
 * request.auth.uid), this just keeps the button from being shown where it'd
 * fail anyway.
 *
 * No back arrow here on purpose — "Exit teacher mode" at the bottom is the
 * only way out, so a redundant top-left arrow was removed.
 */
@Composable
fun TeacherDashboardScreen(
    teacherViewModel: TeacherViewModel,
    onAddClassClick: () -> Unit = {},
    onDone: () -> Unit = {}
) {
    val teacher = teacherViewModel.currentTeacher
    var allClasses by remember { mutableStateOf<List<LiveClass>>(emptyList()) }

    // Refetch every time this screen becomes visible (covers coming back
    // from AddClassScreen after saving a new class, or another teacher
    // having added one in the meantime).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                fetchAllLiveClasses { fetched -> allClasses = fetched.sortedBy { it.dateMillis } }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 40.dp)
    ) {
        // ── TOP BAR (no back arrow — "Exit teacher mode" below is the exit) ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text("My Classes", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text("Everyone's scheduled sessions", fontSize = 14.sp, color = TextSecondary)
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text("Welcome, ${teacher?.name ?: ""}", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(teacher?.email ?: "", fontSize = 14.sp, color = TextSecondary)
        }

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .shadow(8.dp, RoundedCornerShape(14.dp))
                .background(BlueStart, RoundedCornerShape(14.dp))
                .clickable { onAddClassClick() }
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add a live class", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(Modifier.height(26.dp))

        if (allClasses.isEmpty()) {
            Text(
                "No classes have been scheduled yet.",
                color = TextSecondary, fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                allClasses.forEach { cls ->
                    val isMine = teacher != null && cls.teacherId == teacher.uid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(3.dp, RoundedCornerShape(14.dp))
                            .background(CardWhite, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(cls.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                if (isMine) {
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(YoursPillBg, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text("Yours", color = YoursPillText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                SimpleDateFormat("d MMM yyyy 'at' h:mm a", Locale.US).format(Date(cls.dateMillis)),
                                fontSize = 13.sp, color = TextSecondary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "with ${cls.teacherName}",
                                fontSize = 12.sp, color = TextSecondary
                            )
                        }
                        if (isMine) {
                            Icon(
                                Icons.Filled.Delete, contentDescription = "Delete",
                                tint = RedDate,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable {
                                        teacherViewModel.deleteLiveClass(cls.id) {
                                            fetchAllLiveClasses { updated -> allClasses = updated.sortedBy { it.dateMillis } }
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "Exit teacher mode",
            color = Color.Red,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    teacherViewModel.exitTeacherMode()
                    onDone()
                }
                .padding(vertical = 10.dp)
        )
    }
}