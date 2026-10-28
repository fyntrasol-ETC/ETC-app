package com.evolvingtheconversation.app.ui.screens.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evolvingtheconversation.app.data.CourseCatalog
import com.evolvingtheconversation.app.ui.components.BackButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme
import com.evolvingtheconversation.app.R

private val SurfaceBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFF4F6FB)
private val CardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val TextPrimary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)
private val TextSecondary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)

// New — the locked-certificate placeholder card's border/icon/label
private val LockedBorder: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF3A3A40) else Color(0xFFD1D5DB)
private val LockedMuted: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF6B7280) else Color(0xFFB0B7C3)

private data class Certificate(
    val courseId: String,
    val title: String,
    val issued: String,
    val gradient: List<Color>,
    val locked: Boolean
)

private val gradients = listOf(
    listOf(Color(0xFF1E5EF3), Color(0xFF1E3A8A)),
    listOf(Color(0xFF0CBD85), Color(0xFF059669)),
    listOf(Color(0xFFFFA800), Color(0xFFE67E00)),
    listOf(Color(0xFF7C3AED), Color(0xFF5B21B6))
)

// Returns just the formatted date (e.g. "Jun 2026"), or "" — the "Issued …"
// wrapper text is applied separately via stringResource so it's translatable.
private fun formatIssuedDate(millis: Long): String {
    if (millis <= 0L) return ""
    return SimpleDateFormat("MMM yyyy", Locale.US).format(Date(millis))
}

// earnedCertificates: courseId -> earnedAt millis, fetched from Firestore.
// A course whose id isn't in the map is still locked.
@Composable
fun CertificatesScreen(
    earnedCertificates: Map<String, Long> = emptyMap(),
    onBack: () -> Unit = {}
) {
    val certificates = CourseCatalog.courses.mapIndexed { index, course ->
        val earnedAt = earnedCertificates[course.id]
        val issuedText = earnedAt?.let { millis ->
            stringResource(R.string.certificates_issued_label, formatIssuedDate(millis))
        } ?: ""
        Certificate(
            courseId = course.id,
            title    = course.title,
            issued   = issuedText,
            gradient = gradients[index % gradients.size],
            locked   = earnedAt == null
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(onBack = onBack)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.certificates_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
        }

        Column(
            modifier            = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            certificates.chunked(2).forEach { row ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    row.forEach { cert -> CertCard(cert = cert, modifier = Modifier.weight(1f)) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CertCard(cert: Certificate, modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(if (cert.locked) 0.dp else 3.dp)
    ) {
        if (cert.locked) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .border(1.5.dp, LockedBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Lock, contentDescription = null,
                        tint = LockedMuted, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.certificates_locked_message),
                        fontSize = 11.sp, color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(cert.title,
                        fontSize = 10.sp, color = LockedMuted, fontWeight = FontWeight.SemiBold,   // was Color(0xFFB0B7C3)
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp))
                }
            }
        } else {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.4f)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(Brush.linearGradient(cert.gradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.WorkspacePremium, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(40.dp))
                }
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(cert.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text(cert.issued, fontSize = 11.sp, color = TextSecondary)
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
fun CertificatesScreenPreview() { CertificatesScreen() }