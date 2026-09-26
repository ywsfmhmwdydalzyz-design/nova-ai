package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AiStudioTab
import com.example.ui.AiStudioViewModel
import com.example.ui.components.AiStudioBottomBar
import com.example.ui.components.AiStudioTopBar
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ImageScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoScreen
import com.example.ui.screens.WelcomeSplashScreen
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(
                android.graphics.Color.parseColor("#0D0F17")
            )
        )
        setContent {
            val viewModel: AiStudioViewModel = viewModel()
            val preferences by viewModel.preferences.collectAsState()

            MyApplicationTheme(
                themeMode = preferences.themeMode,
                accentColorName = preferences.accentColor
            ) {
                AiStudioApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun AiStudioApp(viewModel: AiStudioViewModel = viewModel()) {
    val currentTab by viewModel.selectedTab.collectAsState()
    var showWelcomeScreen by remember { mutableStateOf(true) }
    var showSettingsScreen by remember { mutableStateOf(false) }

    BackHandler(enabled = showWelcomeScreen) {
        showWelcomeScreen = false
    }

    BackHandler(enabled = showSettingsScreen && !showWelcomeScreen) {
        showSettingsScreen = false
    }

    AnimatedVisibility(
        visible = showWelcomeScreen,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300))
    ) {
        WelcomeSplashScreen(
            onDismiss = { showWelcomeScreen = false }
        )
    }

    if (!showWelcomeScreen) {
        if (showSettingsScreen) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { showSettingsScreen = false },
                onOpenWelcome = {
                    showSettingsScreen = false
                    showWelcomeScreen = true
                }
            )
        } else {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("nova_ai_scaffold"),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    AiStudioTopBar(
                        onSettingsClick = { showSettingsScreen = true },
                        onInfoClick = { showWelcomeScreen = true }
                    )
                },
                bottomBar = {
                    AiStudioBottomBar(
                        currentTab = currentTab,
                        onTabSelected = { viewModel.selectTab(it) }
                    )
                }
            ) { innerPadding ->
                Crossfade(
                    targetState = currentTab,
                    animationSpec = tween(250),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) { tab ->
                    when (tab) {
                        AiStudioTab.CHAT -> ChatScreen(viewModel = viewModel)
                        AiStudioTab.IMAGE -> ImageScreen(viewModel = viewModel)
                        AiStudioTab.VIDEO -> VideoScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
