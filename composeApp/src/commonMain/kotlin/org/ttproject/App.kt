package org.ttproject

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.ttproject.components.DesktopSidebar
import org.ttproject.components.MobileBottomNav
import org.ttproject.components.MobileTopBar
import org.ttproject.data.TokenStorage
import org.ttproject.screens.ChatDetailScreen
import org.ttproject.screens.LoginScreen
import org.ttproject.screens.MapScreen
import org.ttproject.screens.MatchScreen
import org.ttproject.screens.MessagesScreen
import org.ttproject.screens.ProfileScreen
import org.ttproject.screens.RegisterScreen
import org.ttproject.util.LocalThemeMode
import org.ttproject.util.SetStatusBarColors
import org.ttproject.util.ThemeMode
import org.ttproject.util.changePlatformLanguage
import org.ttproject.viewmodel.ChatViewModel
import org.ttproject.viewmodel.MessagesViewModel

enum class AuthRoute {
    Login, Register
}

@Composable
fun App(
    pendingChatId: String? = null,
    onChatConsumed: () -> Unit = {}
) {
    val tokenStorage: TokenStorage = koinInject()
    val navController = rememberNavController()
    var isLoggedIn by remember { mutableStateOf(tokenStorage.getToken() != null) }

    var playMessagesAnimation by remember { mutableStateOf(true) }

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

    // 👇 ADD THIS LAUNCHED EFFECT:
    // If pendingChatId is not null, instantly jump to that chat!
    LaunchedEffect(pendingChatId) {
        if (pendingChatId != null) {
            navController.navigate(NavRoute.Messages) // Use your actual route name here
            onChatConsumed() // Clear it so it doesn't trigger again on rotation
        }
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

            var currentRoute = remember(currentDestination) {
                when {
                    currentDestination?.hasRoute(NavRoute.Match::class) == true -> NavRoute.Match
                    currentDestination?.hasRoute(NavRoute.Coach::class) == true -> NavRoute.Coach
                    currentDestination?.hasRoute(NavRoute.Messages::class) == true || currentDestination?.hasRoute(NavRoute.ChatDetail::class) == true -> NavRoute.Messages
                    currentDestination?.hasRoute(NavRoute.Profile::class) == true -> NavRoute.Profile
                    else -> NavRoute.Map
                }
            }

            // 2. Remember which Auth screen the user is on (Defaults to Login)
            var currentAuthRoute by remember { mutableStateOf(AuthRoute.Login) }

            val onNavigate: (NavRoute) -> Unit = { targetRoute ->
                if (targetRoute == NavRoute.Messages) {
                    playMessagesAnimation = true
                }
                navController.navigate(targetRoute) {
                    popUpTo<NavRoute.Map> {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = targetRoute != NavRoute.Messages
                }
            }

            // Determine the title for the TopBar
            val topBarTitle = when (currentRoute) {
                NavRoute.Messages -> "Messages" // Or use stringResource(SharedStrings.home)
                NavRoute.Map -> "Map"
                NavRoute.Coach -> "AI Coach"
                NavRoute.Match -> "Match"
                NavRoute.Profile -> "Profile"
                NavRoute.ChatDetail -> "Chat"
                is NavRoute.ChatDetail -> "Chat"
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
                            if (isMobile && currentRoute != NavRoute.Map && currentRoute != NavRoute.Messages) {
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
                        containerColor = Color.Transparent,
                        modifier = Modifier.weight(1f) // Takes the remaining width next to the Sidebar
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            val isMapActive = currentRoute == NavRoute.Map

                            // 👇 2. THE PERMANENT MAP LAYER
                            // This never gets destroyed. It stays alive in the background.
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = innerPadding.calculateBottomPadding())
                            ) {
                                MapScreen(isActive = isMapActive)
                            }

                            Column(modifier = Modifier.fillMaxSize()) {
                                AnimatedVisibility(
                                    visible = currentRoute != NavRoute.Map,
                                    enter = fadeIn(tween(200)),
                                    exit = fadeOut(tween(200))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(AppColors.Background)
                                    )
                                }
                            }

                            NavHost(
                                navController = navController,
                                startDestination = NavRoute.Map,
                                modifier = Modifier
                                    .fillMaxSize(),
                                enterTransition = { EnterTransition.None },
                                exitTransition = { ExitTransition.None },
                                popEnterTransition = { EnterTransition.None },
                                popExitTransition = { ExitTransition.None }
                            ) {
                                composable<NavRoute.Map> {
//                                    Box(
//                                        modifier = Modifier
//                                            .fillMaxSize()
////                                        .padding(innerPadding)
//                                            .padding(bottom = innerPadding.calculateBottomPadding())
//                                    ) {
//                                        MapScreen()
//                                    }
                                    Spacer(modifier = Modifier.fillMaxSize())
                                }
                                composable<NavRoute.Match> {
                                    Box(modifier = Modifier.fillMaxSize().background(AppColors.Background).padding(innerPadding)) {
                                        MatchScreen(
                                            onNavigateToLogin = {
                                                currentAuthRoute = AuthRoute.Login
                                                onNavigate(NavRoute.Profile)
                                            },
                                            onNavigateToMessages = {
                                                onNavigate(NavRoute.Messages)
                                            }
                                        )
                                    }
                                }
                                composable<NavRoute.Coach> {
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(AppColors.Background).padding(innerPadding)
                                            .padding(16.dp)
                                    ) {
                                        Text("Coach Screen", color = AppColors.TextPrimary)
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
                                composable<NavRoute.Messages>(
                                    exitTransition = {
                                        if (targetState.destination.hasRoute(NavRoute.ChatDetail::class)) {
                                            // 👇 iOS Parallax Exit: Slide left only 33% and slightly fade to simulate a shadow
                                            slideOutHorizontally(
                                                targetOffsetX = { -it / 3 },
                                                animationSpec = tween(300, easing = LinearEasing)
                                            ) + fadeOut(
                                                targetAlpha = 0.5f,
                                                animationSpec = tween(300, easing = LinearEasing)
                                            )
                                        } else {
                                            fadeOut(tween(200))
                                        }
                                    },
                                    popEnterTransition = {
                                        if (initialState.destination.hasRoute(NavRoute.ChatDetail::class)) {
                                            // 👇 iOS Parallax Enter: Slide back to center from the 33% mark
                                            slideInHorizontally(
                                                initialOffsetX = { -it / 3 },
                                                animationSpec = tween(300, easing = LinearEasing)
                                            ) + fadeIn(
                                                initialAlpha = 0.5f,
                                                animationSpec = tween(300, easing = LinearEasing)
                                            )
                                        } else {
                                            fadeIn(tween(200))
                                        }
                                    }
                                ) {
                                    MessagesScreen(
                                        // 👇 3. Pass the state to the screen
                                        playAnimation = playMessagesAnimation,
                                        bottomNavPadding = innerPadding.calculateBottomPadding(),
                                        onNavigateToChat = { chatId, otherUsername, otherUserImageUrl ->
                                            // 👇 4. We are going to a chat! Turn off the animation for when we come back.
                                            playMessagesAnimation = false
                                            navController.navigate(NavRoute.ChatDetail(chatId, otherUsername, otherUserImageUrl))
                                        }
                                    )
                                }

                                composable<NavRoute.ChatDetail>(
                                    enterTransition = {
                                        // 👇 Chat Detail slides in fully from the right edge
                                        slideInHorizontally(
                                            initialOffsetX = { it },
                                            animationSpec = tween(300, easing = LinearEasing)
                                        )
                                    },
                                    popExitTransition = {
                                        // 👇 Chat Detail slides out fully to the right edge (tracks finger!)
                                        slideOutHorizontally(
                                            targetOffsetX = { it },
                                            animationSpec = tween(300, easing = LinearEasing)
                                        )
                                    }
                                ) { backStackEntry ->
                                    val route = backStackEntry.toRoute<NavRoute.ChatDetail>()
                                    val chatViewModel =
                                        org.koin.compose.viewmodel.koinViewModel<ChatViewModel>(
                                            parameters = {
                                                org.koin.core.parameter.parametersOf(
                                                    route.chatId
                                                )
                                            }
                                        )

                                    Box(modifier = Modifier.fillMaxSize()) {
                                        ChatDetailScreen(
                                            viewModel = chatViewModel,
                                            chatId = route.chatId,
                                            otherUsername = route.otherUsername,
                                            otherUserImageUrl = route.otherUserImageUrl,
                                            bottomNavPadding = innerPadding.calculateBottomPadding(),
                                            onBack = { navController.popBackStack() }
                                        )
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


                                            // 3. Switch between them!
                                            when (currentAuthRoute) {
                                                AuthRoute.Login -> {
                                                    LoginScreen(
                                                        onLoginSuccess = {
                                                            if (tokenStorage.getToken() != null) {
                                                                tokenStorage.saveLanguage(
                                                                    currentLanguage
                                                                )
                                                                isLoggedIn = true
                                                            }
                                                        },
                                                        // Pass a lambda to change the state to Register
                                                        onNavigateToRegister = {
                                                            currentAuthRoute = AuthRoute.Register
                                                        }
                                                    )
                                                }

                                                AuthRoute.Register -> {
                                                    RegisterScreen(
                                                        onRegisterSuccess = {
                                                            if (tokenStorage.getToken() != null) {
                                                                tokenStorage.saveLanguage(
                                                                    currentLanguage
                                                                )
                                                                isLoggedIn =
                                                                    true // Auto-login after registration!
                                                            }
                                                        },
                                                        // Pass a lambda to change the state back to Login
                                                        onNavigateToLogin = {
                                                            currentAuthRoute = AuthRoute.Login
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
        }
    }
}