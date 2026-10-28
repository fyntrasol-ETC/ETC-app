package com.evolvingtheconversation.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.evolvingtheconversation.app.navigation.Screen
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme
import com.evolvingtheconversation.app.R
private val NavBackground: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val ActiveBlue    = Color(0xFF1E5EF3)
private val ActivePillBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1B2540) else Color(0xFFE8EFFE)
private val InactiveTint: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)

// labelResId instead of a raw String — this list is built outside a
// Composable scope, so it can't call stringResource() itself. The actual
// string is resolved inside NavBarItem, which IS a Composable.
private data class NavItem(
    val screen     : Screen,
    val icon       : ImageVector,
    val labelResId : Int
)

private val navItems = listOf(
    NavItem(Screen.Home,      Icons.Outlined.Home,   R.string.nav_home),
    NavItem(Screen.Courses,   Icons.Outlined.Book,   R.string.nav_courses),
    NavItem(Screen.Practice,  Icons.Outlined.Mic,    R.string.nav_practice),
    NavItem(Screen.Community, Icons.Outlined.Group,  R.string.nav_community),
    NavItem(Screen.Profile,   Icons.Outlined.Person, R.string.nav_profile)
)

@Composable
fun AppBottomNavigation(
    navController : NavController,
    currentRoute  : String?
) {
    Surface(
        shadowElevation = 12.dp,
        color           = NavBackground,
        tonalElevation  = 0.dp
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val selected = currentRoute == item.screen.route
                NavBarItem(
                    item     = item,
                    selected = selected,
                    onClick  = {
                        // Always pop back to Home (inclusive=false keeps Home in stack),
                        // then navigate to the target tab. This ensures Home is always
                        // reachable and tabs don't stack on top of each other.
                        navController.navigate(item.screen.route) {
                            popUpTo(Screen.Home.route) {
                                inclusive   = false
                                saveState   = false
                            }
                            launchSingleTop = true
                            restoreState    = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    item     : NavItem,
    selected : Boolean,
    onClick  : () -> Unit
) {
    val iconTint    = if (selected) ActiveBlue else InactiveTint
    val labelColor  = if (selected) ActiveBlue else InactiveTint
    val labelWeight = if (selected) FontWeight.Bold else FontWeight.Normal
    val label       = stringResource(item.labelResId)

    Column(
        modifier            = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            )
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (selected) ActivePillBg else Color.Transparent,
                    shape = RoundedCornerShape(50.dp)
                )
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = item.icon,
                contentDescription = label,
                tint               = iconTint,
                modifier           = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.height(2.dp))

        Text(
            text       = label,
            fontSize   = 10.sp,
            color      = labelColor,
            fontWeight = labelWeight
        )
    }
}