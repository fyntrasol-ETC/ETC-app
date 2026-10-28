package com.evolvingtheconversation.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme

/**
 * Reusable back button used across all screens.
 *
 * Usage:
 *   BackButton(onBack = { navController.popBackStack() })
 *
 * @param onBack    Lambda called when the button is tapped.
 * @param tint      Icon color. Defaults to theme-aware dark navy / light grey.
 * @param modifier  Optional modifier for positioning (e.g. inside a Row).
 */
@Composable
fun BackButton(
    onBack   : () -> Unit,
    tint     : Color    = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827),
    modifier : Modifier = Modifier
) {
    val cardBg = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White

    Box(
        modifier = modifier
            .size(38.dp)
            .shadow(
                elevation    = 4.dp,
                shape        = RoundedCornerShape(10.dp),
                spotColor    = Color.Black.copy(alpha = 0.12f),
                ambientColor = Color.Black.copy(alpha = 0.06f)
            )
            .background(cardBg, RoundedCornerShape(10.dp))
            .clickable { onBack() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = Icons.Outlined.ArrowBackIosNew,
            contentDescription = "Back",
            tint               = tint,
            modifier           = Modifier.size(18.dp)
        )
    }
}