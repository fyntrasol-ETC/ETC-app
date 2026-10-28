package com.evolvingtheconversation.app.ui.screens.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evolvingtheconversation.app.ui.components.BackButton
import androidx.compose.runtime.getValue
import com.evolvingtheconversation.app.ui.theme.LocalIsDarkTheme
import com.evolvingtheconversation.app.R

private val SurfaceBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF121212) else Color(0xFFF4F6FB)
private val CardWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
private val BlueStart   = Color(0xFF1E5EF3)
private val GreenAccent = Color(0xFF0CBD85)
private val TextPrimary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)
private val TextSecondary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)

// New — PlanCard's unselected border
private val PlanBorderUnselected: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2E) else Color(0xFFE5E7EB)

// Held outside a composable, so labels/prices/badges are resource IDs — resolved
// with stringResource() only where they're actually rendered inside PlanCard.
private data class Plan(
    val id               : String,
    val labelResId       : Int,
    val priceResId       : Int,
    val unitResId        : Int,
    val badgeResId       : Int? = null,
    val savingLabelResId : Int? = null
)

private val plans = listOf(
    Plan("monthly",  R.string.premium_plan_monthly_label,  R.string.premium_plan_monthly_price,  R.string.premium_plan_monthly_unit),
    Plan("yearly",   R.string.premium_plan_yearly_label,   R.string.premium_plan_yearly_price,   R.string.premium_plan_yearly_unit,
        R.string.premium_plan_yearly_badge, R.string.premium_plan_yearly_saving),
    Plan("lifetime", R.string.premium_plan_lifetime_label, R.string.premium_plan_lifetime_price, R.string.premium_plan_lifetime_unit),
)

private val features = listOf(
    R.string.premium_feature_unlimited_courses,
    R.string.premium_feature_offline_access,
    R.string.premium_feature_verified_certificates,
    R.string.premium_feature_live_classes,
    R.string.premium_feature_ai_tutor_feedback,
)

// onUpgrade is called with the selected plan id when the user taps "Upgrade now".
// Actual payment processing isn't wired up yet — the caller decides what
// "purchased" means for now (e.g. unlocking a specific course).
@Composable
fun PremiumPlansScreen(
    onBack: () -> Unit = {},
    onUpgrade: (planId: String) -> Unit = {}
) {
    var selectedPlan by remember { mutableStateOf("yearly") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        // TOP BAR
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(onBack = onBack)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.premium_plans_title), fontWeight = FontWeight.Bold,
                fontSize = 20.sp, color = TextPrimary)
        }

        // HERO TEXT
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.premium_hero_title),
            fontWeight = FontWeight.ExtraBold,
            fontSize   = 26.sp,
            color      = TextPrimary,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.premium_hero_subtitle),
            fontSize  = 13.sp,
            color     = TextSecondary,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        // PLAN OPTIONS
        Column(
            modifier            = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            plans.forEach { plan ->
                PlanCard(
                    plan       = plan,
                    selected   = selectedPlan == plan.id,
                    onSelect   = { selectedPlan = plan.id }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // FEATURES CARD
        Card(
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier            = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                features.forEach { featureResId ->
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null,
                            tint = GreenAccent, modifier = Modifier.size(20.dp))
                        Text(stringResource(featureResId), fontSize = 14.sp, color = TextPrimary)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // UPGRADE BUTTON
        Button(
            onClick  = { onUpgrade(selectedPlan) },
            shape    = RoundedCornerShape(50.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = BlueStart),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(54.dp)
        ) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null,
                modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.premium_upgrade_button), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PlanCard(plan: Plan, selected: Boolean, onSelect: () -> Unit) {
    val borderColor = if (selected) BlueStart else PlanBorderUnselected
    val borderWidth = if (selected) 2.dp else 1.dp

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CardWhite)
                .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
                .clickable { onSelect() }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RadioButton(
                    selected = selected,
                    onClick  = onSelect,
                    colors   = RadioButtonDefaults.colors(selectedColor = BlueStart)
                )
                Column {
                    Text(stringResource(plan.labelResId), fontWeight = FontWeight.Bold,
                        fontSize = 15.sp, color = TextPrimary)
                    if (plan.savingLabelResId != null) {
                        Text(stringResource(plan.savingLabelResId), fontSize = 12.sp, color = BlueStart,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(stringResource(plan.priceResId), fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp, color = TextPrimary)
                Text(stringResource(plan.unitResId), fontSize = 13.sp, color = TextSecondary)
            }
        }

        // MOST POPULAR badge
        if (plan.badgeResId != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-12).dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(GreenAccent)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(stringResource(plan.badgeResId), fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
fun PremiumPlansScreenPreview() { PremiumPlansScreen() }