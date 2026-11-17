package com.sadsha.stickcricket.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadsha.stickcricket.audio.SoundManager
import com.sadsha.stickcricket.game.BatsmanPose
import com.sadsha.stickcricket.game.BowlerPhase
import com.sadsha.stickcricket.game.GameState
import com.sadsha.stickcricket.model.BallOutcome
import com.sadsha.stickcricket.model.CosmeticsCatalog
import com.sadsha.stickcricket.model.DeliveryType
import com.sadsha.stickcricket.model.Difficulty
import com.sadsha.stickcricket.model.OutcomeType
import com.sadsha.stickcricket.model.ShotDirection
import com.sadsha.stickcricket.model.TimingResult
import com.sadsha.stickcricket.ui.components.PrimaryGameButton
import com.sadsha.stickcricket.ui.components.StickmanCricketCanvas
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TutorialScreen(
    onTutorialComplete: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val soundManager = remember(context) { SoundManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    var step by remember { mutableIntStateOf(1) } // 1: instructions, 2: interactive pitch, 3: perfect result!
    var ballProgress by remember { mutableFloatStateOf(0f) }
    var isHitDone by remember { mutableStateOf(false) }
    var batsmanSwing by remember { mutableFloatStateOf(0f) }
    var bowlerPhase by remember { mutableStateOf(BowlerPhase.READY) }
    var bowlerRunUpProgress by remember { mutableFloatStateOf(0f) }
    var batsmanPose by remember { mutableStateOf(BatsmanPose.STANCE) }
    var deliveryCycle by remember { mutableIntStateOf(0) }

    val defaultBat = CosmeticsCatalog.BATS.first()
    val defaultHelmet = CosmeticsCatalog.HELMETS.first()
    val defaultJersey = CosmeticsCatalog.JERSEYS.first()
    val defaultBall = CosmeticsCatalog.BALLS.first()

    val dummyOutcome = remember(isHitDone) {
        if (isHitDone) {
            BallOutcome(
                ballIndex = 1,
                outcomeType = OutcomeType.SIX,
                runs = 6,
                isWicket = false,
                timing = TimingResult.PERFECT,
                direction = ShotDirection.LOFT,
                delivery = DeliveryType.NORMAL,
                commentary = "PERFECT TIMING! SMASHED FOR A TOWERING 6!"
            )
        } else null
    }

    val tutorialGameState = GameState(
        score = if (isHitDone) 6 else 0,
        ballsFaced = if (isHitDone) 1 else 0,
        maxBalls = 6,
        target = 20,
        difficulty = Difficulty.EASY,
        currentDelivery = DeliveryType.NORMAL,
        ballProgress = ballProgress,
        isBallInFlight = step == 2 && bowlerPhase == BowlerPhase.DELIVERY_STRIDE && !isHitDone,
        batsmanSwingAngle = batsmanSwing,
        lastOutcome = dummyOutcome,
        equippedBat = defaultBat,
        equippedHelmet = defaultHelmet,
        equippedJersey = defaultJersey,
        equippedBall = defaultBall,
        bowlerPhase = bowlerPhase,
        bowlerRunUpProgress = bowlerRunUpProgress,
        batsmanPose = batsmanPose
    )

    // Trigger shot action
    val executeShot: (direction: ShotDirection) -> Unit = { direction ->
        if (step == 2 && !isHitDone) {
            isHitDone = true
            batsmanPose = when (direction) {
                ShotDirection.LEFT -> BatsmanPose.SWING_OFF
                ShotDirection.RIGHT -> BatsmanPose.SWING_LEG
                ShotDirection.LOFT -> BatsmanPose.SWING_STRAIGHT
            }
            batsmanSwing = when (direction) {
                ShotDirection.LEFT -> -40f
                ShotDirection.RIGHT -> 38f
                ShotDirection.LOFT -> -20f
            }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            soundManager.playSound(SoundManager.SoundType.PERFECT_HIT)
            soundManager.playSound(SoundManager.SoundType.SIX)
            step = 3
        }
    }

    // Bowling run-up & delivery simulation loop during Step 2
    LaunchedEffect(step, deliveryCycle) {
        if (step == 2 && !isHitDone) {
            // 1. Ready Phase
            bowlerPhase = BowlerPhase.READY
            bowlerRunUpProgress = 0f
            ballProgress = 0f
            delay(300)

            // 2. Bowler Run-Up
            bowlerPhase = BowlerPhase.RUN_UP
            for (i in 1..20) {
                if (step != 2 || isHitDone) break
                bowlerRunUpProgress = i / 20f
                delay(30)
            }

            // 3. Delivery Release & Ball Flight
            if (step == 2 && !isHitDone) {
                bowlerPhase = BowlerPhase.DELIVERY_STRIDE
                ballProgress = 0f
                while (ballProgress < 1.0f && !isHitDone && step == 2) {
                    ballProgress += 0.022f
                    delay(24)
                }

                // If user did not hit in time, smoothly reset and deliver again
                if (!isHitDone && step == 2) {
                    delay(800)
                    deliveryCycle++ // trigger fresh delivery loop
                }
            }
        }
    }

    // Pulsing animation for high-impact CTA button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val buttonPulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "button_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .testTag("tutorial_screen")
    ) {
        // Cricket field in background (clickable during step 2 to hit!)
        StickmanCricketCanvas(
            gameState = tutorialGameState,
            modifier = Modifier.fillMaxSize(),
            onCanvasTap = { _, _, _, _ ->
                if (step == 2) {
                    executeShot(ShotDirection.LOFT)
                }
            }
        )

        // Top Navigation & Instructions Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row with Title and Skip Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xCC0F172A),
                    border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsCricket,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "STICK CRICKET ACADEMY",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Prominent Direct Skip Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xDD1E293B),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                    modifier = Modifier
                        .clickable {
                            soundManager.playSound(SoundManager.SoundType.BUTTON_TAP)
                            onTutorialComplete()
                        }
                        .testTag("tutorial_skip_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SKIP TO MATCH",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Skip",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Instructions Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xEE0B132B)),
                border = BorderStroke(
                    1.5.dp,
                    when (step) {
                        1 -> Color(0xFFFF9800)
                        2 -> Color(0xFFFFD700)
                        else -> Color(0xFF10B981)
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (step) {
                        1 -> {
                            Text(
                                text = "HOW TO SMASH SIXES",
                                color = Color(0xFFFF9800),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "1. Watch the bowler deliver down the pitch.\n2. Tap the screen when the ball hits the golden ring!",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 21.sp
                            )
                        }
                        2 -> {
                            Text(
                                text = "⚡ BOWLER RUNNING IN! ⚡",
                                color = Color(0xFFFFD700),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "TAP ANYWHERE ON SCREEN or press button to swing!",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                        }
                        3 -> {
                            Text(
                                text = "💥 TOWERING MAXIMUM! 💥",
                                color = Color(0xFF10B981),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "PERFECT TIMING! 6 RUNS INTO THE STANDS!\nYou are ready to play real matches.",
                                color = Color(0xFFE2E8F0),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (step) {
                1 -> {
                    // READY! TRY A SHOT Primary Action Button
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            soundManager.playSound(SoundManager.SoundType.BUTTON_TAP)
                            step = 2
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF6D00),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(18.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .scale(buttonPulse)
                            .testTag("tutorial_ready_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsCricket,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "READY! TRY A SHOT",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary Text Button to jump right into the game
                    TextButton(
                        onClick = {
                            soundManager.playSound(SoundManager.SoundType.BUTTON_TAP)
                            onTutorialComplete()
                        },
                        modifier = Modifier.testTag("tutorial_skip_match_text_button")
                    ) {
                        Text(
                            text = "Or skip straight to 6-ball match ➔",
                            color = Color(0xFF38BDF8),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                2 -> {
                    // Shot Selection Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { executeShot(ShotDirection.LEFT) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .testTag("tutorial_hit_left")
                        ) {
                            Text("⇦ OFF", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Button(
                            onClick = { executeShot(ShotDirection.LOFT) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6D00)),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(60.dp)
                                .scale(buttonPulse)
                                .testTag("tutorial_hit_loft")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("LOFT 6!", fontWeight = FontWeight.Black, fontSize = 18.sp)
                            }
                        }

                        Button(
                            onClick = { executeShot(ShotDirection.RIGHT) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .testTag("tutorial_hit_right")
                        ) {
                            Text("⇨ LEG", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "💡 Tip: You can also tap anywhere on the pitch!",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                3 -> {
                    // Finish & Start Match Button
                    Button(
                        onClick = {
                            soundManager.playSound(SoundManager.SoundType.BUTTON_TAP)
                            onTutorialComplete()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(18.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .testTag("tutorial_finish_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LET'S PLAY! (START MATCH)",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Replay tutorial shot
                    TextButton(
                        onClick = {
                            isHitDone = false
                            batsmanSwing = 0f
                            batsmanPose = BatsmanPose.STANCE
                            step = 2
                            deliveryCycle++
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Try another practice shot",
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

