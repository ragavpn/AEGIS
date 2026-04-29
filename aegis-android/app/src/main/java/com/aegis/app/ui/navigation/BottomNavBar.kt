package com.aegis.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.aegis.app.ui.viewmodel.NotificationListState
import com.aegis.app.ui.viewmodel.NotificationViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

private val NavBg     = Color(0xFF111827)
private val AccBlue   = Color(0xFF3B82F6)
private val TxtSecond = Color(0xFF475569)

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Feed     : BottomNavItem("feed",     "Feed",     Icons.Filled.Home,     Icons.Outlined.Home)
    object Chat     : BottomNavItem("chat",     "Analyst",  Icons.Filled.Chat,     Icons.Outlined.Chat)
    object Notifications : BottomNavItem("notifications", "Alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications)
    object Settings : BottomNavItem("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun BottomNavBar(
    navController: NavController,
    notificationViewModel: NotificationViewModel = hiltViewModel()
) {
    val items = listOf(BottomNavItem.Feed, BottomNavItem.Chat, BottomNavItem.Notifications, BottomNavItem.Settings)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val notificationState by notificationViewModel.listState.collectAsState()
    
    val unreadCount = if (notificationState is NotificationListState.Success) {
        (notificationState as NotificationListState.Success).notifications.count { !it.isRead }
    } else 0

    NavigationBar(containerColor = NavBg, tonalElevation = 0.dp) {
        items.forEach { item ->
            // Special handling for chat to support args
            val selected = currentRoute == item.route || (item.route == "chat" && currentRoute?.startsWith("chat") == true)
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    if (item == BottomNavItem.Notifications && unreadCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = AccBlue, contentColor = Color.White) { 
                                    Text(unreadCount.toString()) 
                                }
                            }
                        ) {
                            Icon(
                                if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        }
                    } else {
                        Icon(
                            if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label
                        )
                    }
                },
                label = {
                    Text(
                        item.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = AccBlue,
                    selectedTextColor   = AccBlue,
                    unselectedIconColor = TxtSecond,
                    unselectedTextColor = TxtSecond,
                    indicatorColor      = AccBlue.copy(alpha = 0.12f)
                )
            )
        }
    }
}
