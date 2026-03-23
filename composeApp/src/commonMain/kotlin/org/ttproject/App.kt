package org.ttproject

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.koin.compose.koinInject
import org.ttproject.components.DesktopSidebar
import org.ttproject.components.MobileBottomNav
import org.ttproject.components.MobileTopBar
import org.ttproject.data.TokenStorage
import org.ttproject.screens.LoginScreen
import org.ttproject.screens.MapScreen
import org.ttproject.screens.MatchScreen
import org.ttproject.screens.ProfileScreen
import org.ttproject.util.LocalThemeMode
import org.ttproject.util.SetStatusBarColors
import org.ttproject.util.ThemeMode
import org.ttproject.util.changePlatformLanguage

@Composable
fun App() {
    val tokenStorage: TokenStorage = koinInject()
    val navController = rememberNavController()
    var isLoggedIn by remember { mutableStateOf(tokenStorage.getToken() != null) }

    val systemLanguage = Locale.current.language

    val supportedSystemLanguage = if (systemLanguage == "hu") "hu" else "en"

    var currentLanguage by remember { mutableStateOf(tokenStorage.getLanguage() ?: supportedSystemLanguage) }
    println("Current Language: $currentLanguage") // Debug log to verify language loading

    var isLanguageApplied by remember { mutableStateOf(false) }
    LaunchedEffect(currentLanguage) {
        changePlatformLanguage(currentLanguage)
        isLanguageApplied = true
    }

    if (!isLanguageApplied) {
        Box(modifier = Modifier.fillMaxSize().background(AppColors.Background))
        return
    }

    var currentThemeMode by remember {
        mutableStateOf(
            when (tokenStorage.getThemeMode()) {
                "light" -> ThemeMode.Light
                "dark" -> ThemeMode.Dark
                else -> ThemeMode.System
            }
        )
    }

    SetStatusBarColors(isDark = currentThemeMode == ThemeMode.Dark || (currentThemeMode == ThemeMode.System && isSystemInDarkTheme()))

    key(currentLanguage) {
        CompositionLocalProvider(LocalThemeMode provides currentThemeMode) {


            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val currentRoute = remember(currentDestination) {
                when {
                    currentDestination?.hasRoute(NavRoute.Match::class) == true -> NavRoute.Match
                    currentDestination?.hasRoute(NavRoute.Coach::class) == true -> NavRoute.Coach
                    currentDestination?.hasRoute(NavRoute.Messages::class) == true -> NavRoute.Messages
                    currentDestination?.hasRoute(NavRoute.Profile::class) == true -> NavRoute.Profile
                    else -> NavRoute.Map
                }
            }

            val onNavigate: (NavRoute) -> Unit = { targetRoute ->
                navController.navigate(targetRoute) {
                    popUpTo<NavRoute.Map> {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }

            // Determine the title for the TopBar
            val topBarTitle = when (currentRoute) {
                NavRoute.Messages -> "Messages" // Or use stringResource(SharedStrings.home)
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
                            if (isMobile && currentRoute != NavRoute.Map && currentRoute != NavRoute.Profile) {
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
                            startDestination = NavRoute.Map,
                            modifier = Modifier
                                .fillMaxSize(),
                            enterTransition = { fadeIn(animationSpec = tween(200)) },
                            exitTransition = { fadeOut(animationSpec = tween(200)) },
                            popEnterTransition = { fadeIn(animationSpec = tween(200)) },
                            popExitTransition = { fadeOut(animationSpec = tween(200)) }
                        ) {
                            composable<NavRoute.Map> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
//                                        .padding(innerPadding)
                                        .padding(bottom = innerPadding.calculateBottomPadding())
                                ) {
                                    MapScreen()
                                }
                            }
                            composable<NavRoute.Match> {
                                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                                    MatchScreen()
                                }
                            }
                            composable<NavRoute.Coach> {
                                Box(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
                                    Text("Coach Screen", color = AppColors.TextPrimary)
                                }
                            }
                            composable<NavRoute.Messages> {
                                Box(
                                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                                        .padding(16.dp)
                                ) {
                                    Text("Messages Screen", color = AppColors.TextPrimary)
                                    Button(
                                        onClick = {
                                            tokenStorage.clearToken()
                                            tokenStorage.clearLanguage()
                                            isLoggedIn = false
                                        },
                                        modifier = Modifier.padding(top = 16.dp)
                                    ) {
                                        Text("Logout")
                                    }
                                }
                            }
                            composable<NavRoute.Profile> {
                                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                                    if (isLoggedIn) {
                                        ProfileScreen(
                                            currentLanguage = currentLanguage,
                                            currentThemeMode = currentThemeMode,
                                            onLogoutClick = {
                                                tokenStorage.clearToken()
                                                tokenStorage.clearLanguage()
                                                isLoggedIn = false
                                            },
                                            onChangeLanguage = { newLangCode ->
                                                tokenStorage.saveLanguage(newLangCode)
                                                currentLanguage = newLangCode
                                                changePlatformLanguage(newLangCode)
                                            },
                                            onChangeTheme = { newThemeMode ->
                                                tokenStorage.saveThemeMode(
                                                    when (newThemeMode) {
                                                        ThemeMode.Light -> "light"
                                                        ThemeMode.Dark -> "dark"
                                                        ThemeMode.System -> "system"
                                                    }
                                                )
                                                currentThemeMode = newThemeMode
                                            }
                                        )
                                    } else {
                                        LoginScreen(
                                            onLoginSuccess = {
                                                if (tokenStorage.getToken() != null) {
                                                    tokenStorage.saveLanguage(currentLanguage)
                                                    isLoggedIn = true
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
    }
}