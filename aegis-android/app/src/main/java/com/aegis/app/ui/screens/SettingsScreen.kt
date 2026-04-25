package com.aegis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aegis.app.ui.viewmodel.PreferencesViewModel
import com.aegis.app.ui.viewmodel.SettingsViewModel

private val BgDark     = Color(0xFF0A0E1A)
private val SrfDark    = Color(0xFF111827)
private val CardSrf    = Color(0xFF1A2235)
private val AccBlue    = Color(0xFF3B82F6)
private val AccRed     = Color(0xFFEF4444)
private val TxtPrimary = Color(0xFFF1F5F9)
private val TxtSecond  = Color(0xFF94A3B8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onSignedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    prefsViewModel: PreferencesViewModel = hiltViewModel()
) {
    val signedOut by viewModel.signedOut.collectAsState()
    val prefs by prefsViewModel.prefs.collectAsState()
    val loading by prefsViewModel.loading.collectAsState()

    LaunchedEffect(Unit) {
        prefsViewModel.load()
    }

    LaunchedEffect(prefs) {
        if (!loading) prefsViewModel.save()
    }

    LaunchedEffect(signedOut) {
        if (signedOut) onSignedOut()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontWeight = FontWeight.Bold,
                        color = TxtPrimary,
                        letterSpacing = 1.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SrfDark)
            )
        },
        containerColor = BgDark
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Account card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSrf)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(AccBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = AccBlue, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text("Signed in as", color = TxtSecond, fontSize = 12.sp)
                        Text(viewModel.userEmail, color = TxtPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
            }

            // App version card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSrf)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("APPLICATION", color = TxtSecond, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    SettingsRow(label = "Version", value = "1.0.0 (Phase 9)")
                    SettingsRow(label = "Backend", value = "Railway · Active")
                    SettingsRow(label = "Intelligence DB", value = "Supabase · us-east-1")
                }
            }

            // Preferences Card
            if (!loading) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSrf)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("PREFERENCES", color = TxtSecond, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        SettingsRow(label = "Modules", value = "${prefs.modules.size} selected")
                        
                        Spacer(Modifier.height(4.dp))
                        Text("NOTIFICATIONS", color = TxtSecond, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        
                        SettingsSwitchRow(
                            label = "FLASH Alerts",
                            description = "Immediate notification for critical geopolitical events",
                            checked = prefs.notificationFlash,
                            onCheckedChange = { prefsViewModel.setNotificationFlash(it) }
                        )
                        SettingsSwitchRow(
                            label = "PRIORITY Alerts",
                            description = "Daily digest of high-importance signals",
                            checked = prefs.notificationPriority,
                            onCheckedChange = { prefsViewModel.setNotificationPriority(it) }
                        )
                        SettingsSwitchRow(
                            label = "ROUTINE Alerts",
                            description = "Background updates and minor shifts",
                            checked = prefs.notificationRoutine,
                            onCheckedChange = { prefsViewModel.setNotificationRoutine(it) }
                        )
                    }
                    }
                }

                // Voice & Audio Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSrf)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("VOICE & AUDIO", color = TxtSecond, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        
                        SettingsSwitchRow(
                            label = "Do Not Disturb",
                            description = "Mute all notifications except FLASH alerts",
                            checked = prefs.dndEnabled,
                            onCheckedChange = { prefsViewModel.setDndEnabled(it) }
                        )

                        Spacer(Modifier.height(4.dp))
                        
                        Text("Analyst Voice Speed", color = TxtPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("0.5x", color = TxtSecond, fontSize = 12.sp)
                            Slider(
                                value = prefs.ttsSpeed.toFloat(),
                                onValueChange = { prefsViewModel.setTtsSpeed(it.toDouble()) },
                                valueRange = 0.5f..2.0f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = AccBlue,
                                    activeTrackColor = AccBlue,
                                    inactiveTrackColor = Color(0xFF1E293B)
                                )
                            )
                            Text("2.0x", color = TxtSecond, fontSize = 12.sp)
                        }
                    }
                }

            Spacer(Modifier.weight(1f))

            // Sign out button
            Button(
                onClick = { viewModel.signOut() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccRed.copy(alpha = 0.15f),
                    contentColor = AccRed
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun SettingsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TxtSecond, fontSize = 14.sp)
        Text(value, color = Color(0xFF94A3B8), fontSize = 14.sp)
    }
}

@Composable
private fun SettingsSwitchRow(label: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(label, color = TxtPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(description, color = TxtSecond, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccBlue,
                uncheckedThumbColor = TxtSecond,
                uncheckedTrackColor = Color(0xFF1E293B)
            )
        )
    }
}
