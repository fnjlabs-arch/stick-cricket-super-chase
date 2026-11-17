package com.sadsha.stickcricket.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.sadsha.stickcricket.game.GameViewModel
import com.sadsha.stickcricket.model.OutcomeType
import com.sadsha.stickcricket.model.PowerUpType
import com.sadsha.stickcricket.model.ShotDirection
import com.sadsha.stickcricket.model.TimingResult
import com.sadsha.stickcricket.ui.components.PrimaryGameButton
import com.sadsha.stickcricket.ui.components.StickmanCricketCanvas

@Composable
fun GameplayScreen(
    viewModel: GameViewModel,
    onMatchFinished: () -> Unit,
    onQuitToHome: () -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()
    val profile by viewModel.profileState.collectAsState()

    val todayChallenge = remember { viewModel.challengeManager.getTodayChallenge() }
    val (challengeCurrent, challengeTarget) = viewModel.challengeManager.getProgress(
        challenge = todayChallenge,
        runs = gameState.score,
        fours = gameState.foursCount,
        sixes = gameState.sixesCount,
        wickets = gameState.wicketsCount,
        outcomes = gameState.ballOutcomes
    )

    // When game is finished, navigate to Result Screen
    LaunchedEffect(gameState.isGameOver) {
        if (gameState.isGameOver) {
            onMatchFinished()
        }
    }

    // Sticky immersive mode for gameplay: hide system navigation bar so home button is not triggered
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.navigationBars())
        }
        onDispose {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.show(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .testTag("gameplay_screen")
    ) {
        // Main Stickman Cricket 2D Canvas with GPU-accelerated screen-shake & touch-based batting
        StickmanCricketCanvas(
            gameState = gameState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = gameState.cameraShakeX
                    translationY = gameState.cameraShakeY
                },
            onCanvasTap = { tapX, tapY, width, height ->
                viewModel.handleTouchShot(tapX, tapY, width, height)
            }
        )

        // TOP HUD: Two floating corner pods leaving bowler at top-center completely unobstructed
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // LEFT POD: Score & Balls
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xD90B1528),
                border = BorderStroke(1.dp, Color(0xFF1E3A5F)),
                shadowElevation = 6.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "SCORE",
                            color = Color(0xFF64748B),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${gameState.score}",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(26.dp)
                            .background(Color(0xFF1E3A5F))
                    )
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "OVERS",
                            color = Color(0xFF64748B),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${gameState.oversBowledText}/${gameState.totalOversText}",
                            color = Color(0xFF38BDF8),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // RIGHT POD: Target & Pause
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xD90B1528),
                border = BorderStroke(1.dp, Color(0xFF1E3A5F)),
                shadowElevation = 6.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "TARGET",
                            color = Color(0xFF64748B),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${gameState.target}",
                            color = Color(0xFFFFD700),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    IconButton(
                        onClick = { viewModel.togglePause() },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .testTag("gameplay_pause_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // FLOATING TOAST: Power-Up Activation Banner
        AnimatedVisibility(
            visible = gameState.powerUpBannerMessage != null,
            enter = fadeIn(animationSpec = tween(150)) + scaleIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(animationSpec = tween(200)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 68.dp)
        ) {
            gameState.powerUpBannerMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xF20F172A),
                    border = BorderStroke(1.5.dp, Color(0xFFFFD700)),
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = msg,
                        color = Color(0xFFFFD700),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // FLOATING DAILY CHALLENGE OBJECTIVE CHIP
        AnimatedVisibility(
            visible = gameState.powerUpBannerMessage == null,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 68.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (gameState.isDailyChallengeTargetMet) Color(0xE610B981) else Color(0xD90B1528),
                border = BorderStroke(1.dp, if (gameState.isDailyChallengeTargetMet) Color(0xFF34D399) else Color(0xFF1E3A5F)),
                shadowElevation = 4.dp,
                modifier = Modifier.testTag("gameplay_challenge_chip")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (gameState.isDailyChallengeTargetMet) "🎯 Challenge Met! (+${todayChallenge.rewardCoins}🪙)"
                               else "🎯 ${todayChallenge.title}: $challengeCurrent/$challengeTarget",
                        color = if (gameState.isDailyChallengeTargetMet) Color.White else Color(0xFFFFD700),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // CENTER: Dynamic Banner for Hit Feedback (FOUR, SIX, OUT, PERFECT, etc.)
        AnimatedVisibility(
            visible = gameState.showBannerFeedback && gameState.lastOutcome != null,
            enter = scaleIn(animationSpec = tween(150)) + fadeIn(),
            exit = scaleOut(animationSpec = tween(200)) + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            gameState.lastOutcome?.let { outcome ->
                val bannerBg = when (outcome.outcomeType) {
                    OutcomeType.SIX -> Color(0xFFFFD700)
                    OutcomeType.FOUR -> Color(0xFF00E5FF)
                    OutcomeType.WICKET -> Color(0xFFEF4444)
                    else -> Color(0xFF10B981)
                }
                val textColor = if (bannerBg == Color(0xFFFFD700) || bannerBg == Color(0xFF00E5FF)) Color.Black else Color.White

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = bannerBg),
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier.padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = outcome.outcomeType.label,
                            color = textColor,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${outcome.timing.displayName} • ${outcome.direction.title}",
                            color = textColor.copy(alpha = 0.85f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // BOTTOM CONTROLS: Delivery info, Over Tracker, Timing helper & 3 Arcade Shot Buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 54.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Delivery Info Bar & Over Tracker (Pill Bar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delivery Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xD90F172A),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "OVER ${gameState.currentOverDisplay}/${gameState.matchOvers} • B${gameState.ballInCurrentOver}: ",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = gameState.currentDelivery.displayName.uppercase(),
                            color = Color(0xFFFF9800),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // 6 Ball Status Pills for Current Over
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentOverStart = gameState.currentOverIndex * 6
                    for (slot in 0 until 6) {
                        val ballIndex = currentOverStart + slot
                        val outcome = gameState.ballOutcomes.getOrNull(ballIndex)
                        val ballColor = when {
                            outcome == null -> Color(0xFF334155) // Pending
                            outcome.isWicket -> Color(0xFFEF4444) // Wicket
                            outcome.outcomeType == OutcomeType.SIX -> Color(0xFFFFD700) // 6
                            outcome.outcomeType == OutcomeType.FOUR -> Color(0xFF00E5FF) // 4
                            outcome.runs > 0 -> Color(0xFF10B981) // Runs
                            else -> Color(0xFF64748B) // Dot
                        }
                        val ballLabel = when {
                            outcome == null -> "${slot + 1}"
                            outcome.isWicket -> "W"
                            else -> "${outcome.runs}"
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(ballColor)
                                .border(
                                    width = 1.dp,
                                    color = if (ballIndex == gameState.ballsFaced && gameState.isBallInFlight) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                text = ballLabel,
                                color = if (ballColor == Color(0xFFFFD700) || ballColor == Color(0xFF00E5FF)) Color.Black else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // POWER-UP SYSTEM CONTROLS (Golden Bat & Slow Motion)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Golden Bat Power-up (free in v1 — activates or refills directly)
                PowerUpActionChip(
                    name = "Golden Bat",
                    subtitle = if (gameState.isGoldenBatActive) "${gameState.goldenBatBallsRemaining} balls left" else "2x Runs & Guard",
                    count = profile?.goldenBatCount ?: 0,
                    isActive = gameState.isGoldenBatActive,
                    activeColor = Color(0xFFFFD700),
                    icon = Icons.Default.Bolt,
                    testTag = "gameplay_golden_bat_powerup",
                    onActivate = { viewModel.activateGoldenBat() },
                    onBuyOrRefill = { viewModel.refillPowerUp(PowerUpType.GOLDEN_BAT) },
                    modifier = Modifier.weight(1f)
                )

                // Slow Motion Power-up
                PowerUpActionChip(
                    name = "Slow Motion",
                    subtitle = if (gameState.isSlowMotionActive) "${gameState.slowMotionBallsRemaining} balls left" else "Easier Timing",
                    count = profile?.slowMotionCount ?: 0,
                    isActive = gameState.isSlowMotionActive,
                    activeColor = Color(0xFF00E5FF),
                    icon = Icons.Default.HourglassTop,
                    testTag = "gameplay_slow_motion_powerup",
                    onActivate = { viewModel.activatePowerUp(PowerUpType.SLOW_MOTION) },
                    onBuyOrRefill = { viewModel.buyPowerUp(PowerUpType.SLOW_MOTION) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // OFF SIDE (LEFT)
                Button(
                    onClick = { viewModel.playShot(ShotDirection.LEFT) },
                    enabled = gameState.isBallInFlight,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7),
                        disabledContainerColor = Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(
                        width = if (gameState.isBallInFlight) 1.5.dp else 1.dp,
                        color = if (gameState.isBallInFlight) Color(0xFF38BDF8) else Color(0xFF334155)
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 2.dp
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .testTag("batting_off_button")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "⇦ OFF SIDE",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = if (gameState.isBallInFlight) Color.White else Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Cut / Drive",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (gameState.isBallInFlight) Color(0xFFBAE6FD) else Color(0xFF475569)
                        )
                    }
                }

                // STRAIGHT LOFT (CENTER)
                Button(
                    onClick = { viewModel.playShot(ShotDirection.LOFT) },
                    enabled = gameState.isBallInFlight,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEA580C),
                        disabledContainerColor = Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(
                        width = if (gameState.isBallInFlight) 2.dp else 1.dp,
                        color = if (gameState.isBallInFlight) Color(0xFFFFD700) else Color(0xFF334155)
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 2.dp
                    ),
                    modifier = Modifier
                        .weight(1.35f)
                        .height(64.dp)
                        .testTag("batting_loft_button")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "⇧ STRAIGHT",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = if (gameState.isBallInFlight) Color.White else Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "LOFT 6!",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (gameState.isBallInFlight) Color(0xFFFFD700) else Color(0xFF475569)
                        )
                    }
                }

                // LEG SIDE (RIGHT)
                Button(
                    onClick = { viewModel.playShot(ShotDirection.RIGHT) },
                    enabled = gameState.isBallInFlight,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7),
                        disabledContainerColor = Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(
                        width = if (gameState.isBallInFlight) 1.5.dp else 1.dp,
                        color = if (gameState.isBallInFlight) Color(0xFF38BDF8) else Color(0xFF334155)
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 2.dp
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .testTag("batting_leg_button")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "LEG SIDE ⇨",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = if (gameState.isBallInFlight) Color.White else Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Pull / Flick",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (gameState.isBallInFlight) Color(0xFFBAE6FD) else Color(0xFF475569)
                        )
                    }
                }
            }
        }

        // PAUSE DIALOG
        if (gameState.isPaused) {
            PauseDialog(
                onResume = { viewModel.resumeGame() },
                onRestart = { viewModel.startNewMatch() },
                onQuit = onQuitToHome,
                isSoundOn = viewModel.soundManager.isSoundEnabled,
                onToggleSound = {
                    viewModel.soundManager.isSoundEnabled = it
                    kotlinx.coroutines.GlobalScope.let { _ ->
                        // update preference
                    }
                },
                isHapticsOn = viewModel.hapticManager.isHapticsEnabled,
                onToggleHaptics = {
                    viewModel.hapticManager.isHapticsEnabled = it
                }
            )
        }

        // WICKET REWARDED CONTINUE DIALOG
        if (gameState.isWicketContinueDialogVisible) {
            Dialog(onDismissRequest = { /* require button choice */ }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(2.dp, Color(0xFFEF4444)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("wicket_continue_dialog")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "YOU'RE OUT!",
                            color = Color(0xFFEF4444),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Lost your wicket on Ball ${gameState.ballsFaced} of 6.\nUse your free second chance to keep batting?",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        PrimaryGameButton(
                            text = "CONTINUE BATTING",
                            icon = Icons.Default.PlayArrow,
                            containerColor = Color(0xFF10B981),
                            onClick = { viewModel.continueAfterWicket() },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "continue_button"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { viewModel.declineWicketContinue() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("decline_continue_button")
                        ) {
                            Text("FINISH INNINGS", color = Color(0xFFCBD5E1), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PowerUpActionChip(
    name: String,
    subtitle: String,
    count: Int,
    isActive: Boolean,
    activeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    onActivate: () -> Unit,
    onBuyOrRefill: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isActive) activeColor else if (count > 0) Color(0xFF38BDF8) else Color(0xFF475569)
    val bgColor = if (isActive) activeColor.copy(alpha = 0.22f) else Color(0xD90F172A)

    Surface(
        onClick = {
            if (!isActive) {
                if (count > 0) onActivate() else onBuyOrRefill()
            }
        },
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(if (isActive) 1.5.dp else 1.dp, borderColor),
        shadowElevation = if (isActive) 6.dp else 2.dp,
        modifier = modifier
            .height(42.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isActive) activeColor else Color(0xFF1E293B))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = name,
                    tint = if (isActive) Color.Black else if (count > 0) Color.White else Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        color = if (isActive) activeColor else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    if (isActive) {
                        Text(
                            text = "ON",
                            color = activeColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    } else {
                        Text(
                            text = if (count > 0) "x$count" else "+2 (40🪙)",
                            color = if (count > 0) Color(0xFF38BDF8) else Color(0xFFFF9800),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = subtitle,
                    color = if (isActive) activeColor.copy(alpha = 0.9f) else Color(0xFF94A3B8),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
