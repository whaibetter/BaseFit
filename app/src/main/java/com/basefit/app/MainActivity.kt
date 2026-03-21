package com.basefit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.basefit.app.ui.navigation.BaseFitNavGraph
import com.basefit.app.ui.navigation.Screen
import com.basefit.app.ui.theme.BaseFitTheme
import com.basefit.app.ui.theme.Background
import com.basefit.app.ui.theme.Primary
import com.basefit.app.ui.theme.TextHint
import com.basefit.app.ui.theme.TextPrimary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BaseFitTheme {
                BaseFitMainScreen()
            }
        }
    }
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem(Screen.Home.route, "首页", Icons.Default.Home)
    object Record : BottomNavItem(Screen.Record.route, "记录", Icons.Default.CalendarMonth)
    object Stats : BottomNavItem(Screen.Stats.route, "统计", Icons.Default.BarChart)
    object Settings : BottomNavItem(Screen.Settings.route, "设置", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseFitMainScreen() {
    val navController = rememberNavController()
    val navItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Record,
        BottomNavItem.Stats,
        BottomNavItem.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Record.route,
        Screen.Stats.route,
        Screen.Settings.route
    )

    Scaffold(
        containerColor = Background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Background,
                    contentColor = Primary
                ) {
                    navItems.forEach { item ->
                        val selected = currentRoute == item.route
                        
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    item.icon,
                                    contentDescription = item.title,
                                    tint = if (selected) Primary else TextHint
                                )
                            },
                            label = {
                                Text(
                                    item.title,
                                    color = if (selected) Primary else TextHint
                                )
                            },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        BaseFitNavGraph(
            navController = navController,
            bottomBarPadding = padding
        )
    }
}
