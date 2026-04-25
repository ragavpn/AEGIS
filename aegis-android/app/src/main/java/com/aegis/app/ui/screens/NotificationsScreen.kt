package com.aegis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aegis.app.data.model.Notification
import com.aegis.app.ui.viewmodel.NotificationListState
import com.aegis.app.ui.viewmodel.NotificationViewModel
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BgDark     = Color(0xFF0A0E1A)
private val SrfDark    = Color(0xFF111827)
private val CardSrf    = Color(0xFF1A2235)
private val AccBlue    = Color(0xFF3B82F6)
private val AccRed     = Color(0xFFEF4444)
private val AccGold    = Color(0xFFF59E0B)
private val TxtPrimary = Color(0xFFF1F5F9)
private val TxtSecond  = Color(0xFF94A3B8)

private fun formatDate(iso: String): String {
    return try {
        val zdt = ZonedDateTime.parse(iso)
        zdt.format(DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.US))
    } catch (e: Exception) { iso }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onArticleClick: (String) -> Unit = {},
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val state by viewModel.listState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Notifications",
                        fontWeight = FontWeight.Bold,
                        color = TxtPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SrfDark)
            )
        },
        containerColor = BgDark
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                is NotificationListState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = AccBlue
                    )
                }
                is NotificationListState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(s.message, color = AccRed, modifier = Modifier.padding(16.dp))
                        Button(onClick = { viewModel.loadNotifications() }) { Text("Retry") }
                    }
                }
                is NotificationListState.Success -> {
                    if (s.notifications.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = TxtSecond.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No notifications",
                                color = TxtSecond,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(s.notifications, key = { it.id }) { notif ->
                                NotificationCard(
                                    notification = notif,
                                    onClick = {
                                        if (!notif.isRead) viewModel.markAsRead(notif.id)
                                        if (notif.actionUrl?.startsWith("aegis://article/") == true) {
                                            val articleId = notif.actionUrl.removePrefix("aegis://article/")
                                            onArticleClick(articleId)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(notification: Notification, onClick: () -> Unit) {
    val iconTint = when (notification.type) {
        "flash" -> AccRed
        "priority" -> AccGold
        else -> AccBlue
    }

    val icon = when (notification.type) {
        "flash", "priority" -> Icons.Default.Warning
        else -> Icons.Default.Info
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (notification.isRead) SrfDark else CardSrf),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isRead) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        notification.title,
                        color = if (notification.isRead) TxtSecond else TxtPrimary,
                        fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        formatDate(notification.createdAt),
                        color = TxtSecond,
                        fontSize = 11.sp
                    )
                }
                
                Spacer(Modifier.height(4.dp))
                
                Text(
                    notification.body,
                    color = TxtSecond,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (!notification.isRead) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AccBlue)
                        .align(Alignment.CenterVertically)
                )
            }
        }
    }
}

