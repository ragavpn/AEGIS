package com.aegis.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aegis.app.data.model.ALL_MODULES
import com.aegis.app.ui.viewmodel.PreferencesViewModel

private val BgDark     = Color(0xFF0A0E1A)
private val AccBlue    = Color(0xFF3B82F6)
private val TxtPrimary = Color(0xFFF1F5F9)
private val TxtSecond  = Color(0xFF94A3B8)
private val CardSrf    = Color(0xFF1A2235)

// Emoji icon map for modules
private val MODULE_ICONS = mapOf(
    "GEOPOLITICS" to "🌍",
    "ENERGY"      to "⚡",
    "FINANCE"     to "📈",
    "DEFENCE"     to "🛡️",
    "ECONOMICS"   to "💹",
    "TECHNOLOGY"  to "🔬",
    "CLIMATE"     to "🌡️",
    "TRADE"       to "🚢"
)

// 1-line description for each module (PLAN.md §7.1)
private val MODULE_DESCRIPTIONS = mapOf(
    "GEOPOLITICS" to "Global power & conflict",
    "ENERGY"      to "Oil, gas & renewables",
    "FINANCE"     to "Markets & capital flows",
    "DEFENCE"     to "Military & security",
    "ECONOMICS"   to "Macro trends & policy",
    "TECHNOLOGY"  to "Tech, AI & cyber",
    "CLIMATE"     to "Environment & weather",
    "TRADE"       to "Supply chains & tariffs"
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: PreferencesViewModel = hiltViewModel()
) {
    val prefs by viewModel.prefs.collectAsState()
    val saved by viewModel.saved.collectAsState()

    LaunchedEffect(saved) {
        if (saved) {
            viewModel.resetSaved()
            onComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))

            // Header
            Text("⚡", fontSize = 48.sp)
            Spacer(Modifier.height(20.dp))
            Text(
                "AEGIS",
                color = TxtPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                letterSpacing = 6.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Select your intelligence modules",
                color = TxtSecond,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Text(
                "We'll personalise your feed around these domains",
                color = TxtSecond.copy(alpha = 0.7f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Module grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(ALL_MODULES) { module ->
                    val selected = prefs.modules.contains(module)
                    ModuleCard(
                        module = module,
                        icon = MODULE_ICONS[module] ?: "📌",
                        description = MODULE_DESCRIPTIONS[module] ?: "",
                        selected = selected,
                        onClick = { viewModel.toggleModule(module) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Selection counter
            Text(
                "${prefs.modules.size} of ${ALL_MODULES.size} selected",
                color = TxtSecond,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(12.dp))

            // Continue button
            Button(
                onClick = { viewModel.save() },
                enabled = prefs.modules.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccBlue,
                    disabledContainerColor = Color(0xFF1E293B)
                )
            ) {
                Text(
                    "Enter the Intelligence Feed",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ModuleCard(
    module: String,
    icon: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) AccBlue.copy(alpha = 0.15f) else CardSrf,
        animationSpec = spring(),
        label = "module_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) AccBlue else Color(0xFF1E3A5F),
        animationSpec = spring(),
        label = "module_border"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.3f)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(icon, fontSize = 28.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                module,
                color = if (selected) AccBlue else TxtSecond,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                letterSpacing = 1.sp
            )
            if (description.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    description,
                    color = TxtSecond.copy(alpha = 0.65f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 13.sp,
                    maxLines = 1
                )
            }
        }
    }
}
