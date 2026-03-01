package org.ttproject

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.ttproject.components.DesktopSidebar
import org.ttproject.components.MobileBottomNav
import org.ttproject.components.MobileTopBar
import org.ttproject.screens.LoginScreen
import org.ttproject.screens.MapScreen
import org.ttproject.screens.MatchScreen

@Composable
fun App() {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val currentRoute = remember(currentDestination) {
        when {
            currentDestination?.hasRoute(NavRoute.Map::class) == true -> NavRoute.Map
            currentDestination?.hasRoute(NavRoute.Coach::class) == true -> NavRoute.Coach
            currentDestination?.hasRoute(NavRoute.Match::class) == true -> NavRoute.Match
            currentDestination?.hasRoute(NavRoute.Profile::class) == true -> NavRoute.Profile
            else -> NavRoute.Home
        }
    }

    val onNavigate: (NavRoute) -> Unit = { targetRoute ->
        navController.navigate(targetRoute) {
            popUpTo<NavRoute.Home> {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Determine the title for the TopBar
    val topBarTitle = when (currentRoute) {
        NavRoute.Home -> "Home" // Or use stringResource(SharedStrings.home)
        NavRoute.Map -> "Map"
        NavRoute.Coach -> "AI Coach"
        NavRoute.Match -> "Match"
        NavRoute.Profile -> "Profile"
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

        val isMobile = maxWidth < 600.dp

        Row(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {

            // Show Desktop Sidebar if width is >= 800.dp
            if (!isMobile) {
                DesktopSidebar(
                    currentRoute = currentRoute,
                    onNavigate = onNavigate
                )
            }

            // Main Content Area: Replaced Column with Scaffold
            Scaffold(
                topBar = {
                    if (isMobile && currentRoute != NavRoute.Map) {
                        MobileTopBar()
                    }
                },
                bottomBar = {
                    if (isMobile) {
                        MobileBottomNav(
                            currentRoute = currentRoute,
                            onNavigate = onNavigate
                        )
                    }
                },
                containerColor = AppColors.Background,
                modifier = Modifier.weight(1f) // Takes the remaining width next to the Sidebar
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = NavRoute.Home,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    enterTransition = { fadeIn(animationSpec = tween(200)) },
                    exitTransition = { fadeOut(animationSpec = tween(200)) },
                    popEnterTransition = { fadeIn(animationSpec = tween(200)) },
                    popExitTransition = { fadeOut(animationSpec = tween(200)) }
                ) {
                    composable<NavRoute.Home> {
                        LoginScreen()
                    }
                    composable<NavRoute.Map> {
                        MapScreen()
                    }
                    composable<NavRoute.Coach> {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Text("Coach Screen", color = AppColors.TextPrimary)
                        }
                    }
                    composable<NavRoute.Match> {
                        MatchScreen()
                    }
                    composable<NavRoute.Profile> {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Text("Profile Screen", color = AppColors.TextPrimary)
                        }
                    }
                }
            }
        }
    }
}