package com.sadsha.stickcricket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sadsha.stickcricket.audio.SoundManager
import com.sadsha.stickcricket.game.GameViewModel
import com.sadsha.stickcricket.ui.ScreenRoutes
import com.sadsha.stickcricket.ui.screens.AboutPrivacyScreen
import com.sadsha.stickcricket.ui.screens.CollectionScreen
import com.sadsha.stickcricket.ui.screens.DailyChallengeScreen
import com.sadsha.stickcricket.ui.screens.DailyRewardScreen
import com.sadsha.stickcricket.ui.screens.GameplayScreen
import com.sadsha.stickcricket.ui.screens.HomeScreen
import com.sadsha.stickcricket.ui.screens.LeaderboardScreen
import com.sadsha.stickcricket.ui.screens.ResultScreen
import com.sadsha.stickcricket.ui.screens.SettingsScreen
import com.sadsha.stickcricket.ui.screens.SplashScreen
import com.sadsha.stickcricket.ui.screens.TutorialScreen
import com.sadsha.stickcricket.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            SoundManager.initialize(applicationContext)
        } catch (e: Throwable) {
            // Non-fatal audio fallback
        }
        enableEdgeToEdge()

        try {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.navigationBars())
        } catch (_: Throwable) {
            // Safe fallback if running in cloud streaming emulator
        }

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val viewModel: GameViewModel = viewModel()
                val profile by viewModel.profileState.collectAsState()
                val selectedOvers by viewModel.selectedMatchOvers.collectAsState()
                val scope = rememberCoroutineScope()

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A)),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    // innerPadding handled in edge-to-edge screens
                    NavHost(
                        navController = navController,
                        startDestination = ScreenRoutes.SPLASH
                    ) {
                        composable(ScreenRoutes.SPLASH) {
                            SplashScreen(
                                profile = profile,
                                onNavigateNext = {
                                    navController.navigate(ScreenRoutes.HOME) {
                                        popUpTo(ScreenRoutes.SPLASH) { inclusive = true }
                                    }
                                },
                                onPlayDirectMatch = {
                                    viewModel.startNewMatch()
                                    navController.navigate(ScreenRoutes.GAMEPLAY) {
                                        popUpTo(ScreenRoutes.SPLASH) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(ScreenRoutes.HOME) {
                            HomeScreen(
                                profile = profile,
                                selectedOvers = selectedOvers,
                                onOversSelected = { viewModel.setMatchOvers(it) },
                                onPlayClicked = {
                                    viewModel.startNewMatch()
                                    navController.navigate(ScreenRoutes.GAMEPLAY)
                                },
                                onDailyChallengeClicked = {
                                    navController.navigate(ScreenRoutes.DAILY_CHALLENGE)
                                },
                                onLeaderboardClicked = {
                                    navController.navigate(ScreenRoutes.LEADERBOARD)
                                },
                                onCollectionClicked = {
                                    navController.navigate(ScreenRoutes.COLLECTION)
                                },
                                onDailyRewardClicked = {
                                    navController.navigate(ScreenRoutes.DAILY_REWARD)
                                },
                                onSettingsClicked = {
                                    navController.navigate(ScreenRoutes.SETTINGS)
                                },
                                onClaimBonusCoins = {
                                    viewModel.earnBonusReward(150) {}
                                }
                            )
                        }

                        composable(ScreenRoutes.TUTORIAL) {
                            TutorialScreen(
                                onTutorialComplete = {
                                    scope.launch {
                                        viewModel.repository.setTutorialCompleted(true)
                                        viewModel.startNewMatch()
                                        navController.navigate(ScreenRoutes.GAMEPLAY) {
                                            popUpTo(ScreenRoutes.TUTORIAL) { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        composable(ScreenRoutes.GAMEPLAY) {
                            GameplayScreen(
                                viewModel = viewModel,
                                onMatchFinished = {
                                    navController.navigate(ScreenRoutes.RESULT) {
                                        popUpTo(ScreenRoutes.GAMEPLAY) { inclusive = true }
                                    }
                                },
                                onQuitToHome = {
                                    viewModel.quitMatch()
                                    navController.navigate(ScreenRoutes.HOME) {
                                        popUpTo(ScreenRoutes.GAMEPLAY) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(ScreenRoutes.RESULT) {
                            ResultScreen(
                                viewModel = viewModel,
                                onPlayAgain = {
                                    viewModel.startNewMatch()
                                    navController.navigate(ScreenRoutes.GAMEPLAY) {
                                        popUpTo(ScreenRoutes.RESULT) { inclusive = true }
                                    }
                                },
                                onGoHome = {
                                    navController.navigate(ScreenRoutes.HOME) {
                                        popUpTo(ScreenRoutes.RESULT) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(ScreenRoutes.DAILY_CHALLENGE) {
                            DailyChallengeScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onPlayChallengeMatch = {
                                    navController.navigate(ScreenRoutes.GAMEPLAY)
                                }
                            )
                        }

                        composable(ScreenRoutes.COLLECTION) {
                            CollectionScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ScreenRoutes.DAILY_REWARD) {
                            DailyRewardScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ScreenRoutes.LEADERBOARD) {
                            LeaderboardScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ScreenRoutes.SETTINGS) {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onReplayTutorial = {
                                    navController.navigate(ScreenRoutes.TUTORIAL)
                                },
                                onAboutPrivacyClicked = {
                                    navController.navigate(ScreenRoutes.ABOUT_PRIVACY)
                                }
                            )
                        }

                        composable(ScreenRoutes.ABOUT_PRIVACY) {
                            AboutPrivacyScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }

                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        SoundManager.getInstance().pauseAll()
    }

    override fun onResume() {
        super.onResume()
        SoundManager.getInstance().resumeAll()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.navigationBars())
        }
    }
}

