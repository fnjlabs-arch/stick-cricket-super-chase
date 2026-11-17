package com.sadsha.stickcricket.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadsha.stickcricket.R
import com.sadsha.stickcricket.audio.SoundManager
import com.sadsha.stickcricket.data.UserGameProfile
import com.sadsha.stickcricket.ui.components.CricketLogoBadge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Vibrant, Cricket-Themed Splash Screen:
 * Entry point when the application is launched.
 *
 * Visual & Animation Highlights:
 * 1. Atmospheric floodlit cricket stadium backdrop with ambient light cones.
 * 2. Bowling Delivery Animation: A spinning red cricket ball bowls down the pitch and bounces.
 * 3. Bat Impact: Crisp bat crack sound & explosive golden spark burst.
 * 4. 'Stick Cricket Super Chase' Logo Reveal: Dynamic spring physics reveal of the official logo emblem.
 * 5. 6-Ball Over Meter: Match readiness progress with 6 balls lighting up sequentially.
 * 6. Smooth automatic navigation to the home screen (or tutorial), with instant tap-to-skip.
 */
@Composable
fun SplashScreen(
    profile: UserGameProfile?,
    onNavigateNext: (isTutorialCompleted: Boolean) -> Unit,
    onPlayDirectMatch: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val soundManager = remember(context) { SoundManager.getInstance(context) }
    val tutorialCompleted = profile?.tutorialCompleted ?: false

    // Multi-phase animation states
    val bowlingBallProgress = remember { Animatable(0f) }
    val impactFlash = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.2f) }
    val logoAlpha = remember { Animatable(0f) }
    val contentFadeIn = remember { Animatable(0f) }
    val loadingProgress = remember { Animatable(0f) }
    var activeBallsCount by remember { mutableIntStateOf(0) }
    var loadingStatusText by remember { mutableStateOf("Prepping the pitch...") }
    var hasNavigated by remember { mutableStateOf(false) }

    fun proceedToNext() {
        if (!hasNavigated) {
            hasNavigated = true
            soundManager.playSound(SoundManager.SoundType.BUTTON_TAP)
            onNavigateNext(tutorialCompleted)
        }
    }

    // Orchestrated splash animation timeline
    LaunchedEffect(Unit) {
        // Phase 1: Ball Delivery Down the Pitch (0 - 300ms)
        bowlingBallProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(320, easing = FastOutSlowInEasing)
        )

        // Phase 2: Bat Strike Impact!
        soundManager.playSound(SoundManager.SoundType.BAT_HIT)
        soundManager.playSound(SoundManager.SoundType.PERFECT_HIT)

        launch {
            impactFlash.animateTo(1f, animationSpec = tween(60))
            impactFlash.animateTo(0f, animationSpec = tween(200))
        }

        // Phase 3: 'Stick Cricket Super Chase' Logo Springs into View
        soundManager.playSound(SoundManager.SoundType.CROWD_CHEER, volume = 0.8f)
        logoAlpha.animateTo(1f, animationSpec = tween(150))
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )

        // Phase 4: Match Preparation & 6-Ball Progress
        contentFadeIn.animateTo(1f, animationSpec = tween(200))

        launch {
            loadingProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(600, easing = LinearEasing)
            )
        }

        // Increment 6-ball over indicator sequentially
        val statusList = listOf(
            "Inspecting the pitch...",
            "Polishing the seam...",
            "Aligning the wickets...",
            "Powering floodlights...",
            "Equipping willow bat...",
            "Ready to bat!"
        )

        for (i in 1..6) {
            delay(70)
            soundManager.playSound(SoundManager.SoundType.BUTTON_TAP, volume = 0.5f)
            activeBallsCount = i
            loadingStatusText = statusList[i - 1]
        }

        // Phase 5: Complete & Navigate to Home Screen
        delay(200)
        proceedToNext()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Instant tap-to-skip anywhere
                proceedToNext()
            }
            .testTag("splash_screen")
    ) {
        // 1. VIBRANT CRICKET STADIUM COVER ART
        Image(
            painter = painterResource(id = R.drawable.img_cricket_stadium_cover),
            contentDescription = "Cricket Stadium",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. ATMOSPHERIC CRICKET NIGHT VIGNETTE
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xF008101E), // Dark sky navy
                            Color(0x770B1E2D),
                            Color(0x44063B2B), // Cricket turf green tint
                            Color(0xCC07141E),
                            Color(0xFB040810)  // Solid bottom for contrast
                        )
                    )
                )
        )

        // 3. AMBIENT CRICKET STADIUM FLOODLIGHTS & FLOATING PARTICLES
        CricketStadiumAtmosphere()

        // 4. ANIMATED BOWLING BALL DELIVERY & IMPACT FLASH (Phase 1 & 2)
        if (bowlingBallProgress.value < 1f || impactFlash.value > 0f) {
            DeliveryAndImpactOverlay(
                deliveryProgress = bowlingBallProgress.value,
                flashIntensity = impactFlash.value
            )
        }

        // 5. MAIN FOREGROUND CONTENT (Logo, Badges & 6-Ball Loader)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP: League Header Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .alpha(contentFadeIn.value)
                    .padding(top = 10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xCC0F172A),
                    border = BorderStroke(1.2.dp, Color(0xFFFF9800).copy(alpha = 0.8f)),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsCricket,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "T20 STICK CRICKET ARENA",
                            color = Color(0xFFFFD54F),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
            }

            // CENTER: The Official 'Stick Cricket Super Chase' Logo Emblem
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            ) {
                // The Official Crest Badge
                CricketLogoBadge(
                    sizeDp = 270.dp,
                    showAura = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Tagline with dynamic cricket icons
                Text(
                    text = "6 BALLS  •  ONE OVER  •  MAXIMUM THRILL",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD700),
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Time the sweet spot and clear the ropes!",
                    fontSize = 13.sp,
                    color = Color(0xFFCBD5E1),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            // BOTTOM: 6-Ball Over Progress Meter & Match Launch Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(contentFadeIn.value)
                    .padding(bottom = 12.dp)
            ) {
                // 6 Cricket Balls Over Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    for (ballIndex in 1..6) {
                        val isLit = ballIndex <= activeBallsCount
                        CricketBallProgressIndicator(
                            isLit = isLit,
                            ballNumber = ballIndex
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Smooth Linear Loading Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF1E293B))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(loadingProgress.value)
                            .height(6.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFFFF9800),
                                        Color(0xFFFFD700),
                                        Color(0xFFFF3D00)
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Real-time status text
                Text(
                    text = loadingStatusText,
                    fontSize = 12.5.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Direct Enter Home Button
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFFF6D00),
                    border = BorderStroke(1.5.dp, Color(0xFFFFD700)),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .clickable { proceedToNext() }
                        .testTag("splash_enter_home_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = "ENTER HOME",
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Enter Home",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual ball indicator in the 6-ball over meter.
 */
@Composable
private fun CricketBallProgressIndicator(
    isLit: Boolean,
    ballNumber: Int
) {
    val scale = if (isLit) 1.15f else 0.95f
    val bgColor = if (isLit) Color(0xFFD32F2F) else Color(0xFF334155)
    val borderColor = if (isLit) Color(0xFFFFD700) else Color(0xFF475569)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(24.dp)
            .scale(scale)
            .shadow(if (isLit) 8.dp else 0.dp, CircleShape, spotColor = Color(0xFFFF5252))
            .background(bgColor, CircleShape)
            .background(
                if (isLit) {
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFF8A80), Color(0xFFD32F2F), Color(0xFF8B0000))
                    )
                } else {
                    Brush.radialGradient(listOf(Color(0xFF475569), Color(0xFF1E293B)))
                },
                CircleShape
            )
            .clip(CircleShape)
            .background(Color.Transparent)
    ) {
        // Seam Line
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (isLit) {
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(size.width * 0.5f, 2f),
                    end = Offset(size.width * 0.5f, size.height - 2f),
                    strokeWidth = 1.5f,
                    cap = StrokeCap.Round
                )
            }
        }
        Text(
            text = "$ballNumber",
            color = if (isLit) Color.White else Color(0xFF94A3B8),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black
        )
    }
}

/**
 * Bowling animation & Bat Strike Impact shockwave.
 */
@Composable
private fun DeliveryAndImpactOverlay(
    deliveryProgress: Float,
    flashIntensity: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Bowling Ball Path
        if (deliveryProgress in 0.01f..0.99f) {
            val startY = h * 0.42f
            val endY = h * 0.52f
            val curY = startY + (endY - startY) * deliveryProgress

            // Parabolic bounce curve
            val bounceOffset = 40f * sin(deliveryProgress * Math.PI.toFloat())
            val ballY = curY - bounceOffset
            val ballX = w * 0.5f + 10f * sin(deliveryProgress * 4f)

            val ballRadius = 8f + 32f * deliveryProgress

            // Ground shadow
            drawOval(
                color = Color(0x66000000),
                topLeft = Offset(ballX - ballRadius * 0.9f, curY + 4f),
                size = Size(ballRadius * 1.8f, 10f)
            )

            // Red Bowling Ball
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF5252), Color(0xFFD32F2F), Color(0xFF8B0000)),
                    center = Offset(ballX - ballRadius * 0.3f, ballY - ballRadius * 0.3f),
                    radius = ballRadius
                ),
                radius = ballRadius,
                center = Offset(ballX, ballY)
            )

            // White seam rotation
            rotate(degrees = deliveryProgress * 360f, pivot = Offset(ballX, ballY)) {
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(ballX, ballY - ballRadius + 2f),
                    end = Offset(ballX, ballY + ballRadius - 2f),
                    strokeWidth = 2f
                )
            }
        }

        // 2. Bat Impact Shockwave & Golden Flash
        if (flashIntensity > 0f) {
            val cx = w * 0.5f
            val cy = h * 0.52f

            // Golden radial flash
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD700).copy(alpha = flashIntensity * 0.7f),
                        Color(0xFFFF6D00).copy(alpha = flashIntensity * 0.4f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = w * 0.65f * flashIntensity
                ),
                radius = w * 0.65f * flashIntensity,
                center = Offset(cx, cy)
            )

            // Expanding shockwave ring
            drawCircle(
                color = Color(0xFFFFECB3).copy(alpha = flashIntensity),
                radius = w * 0.45f * flashIntensity,
                center = Offset(cx, cy),
                style = Stroke(width = 4f * flashIntensity)
            )
        }
    }
}

/**
 * Animated floodlights and stadium atmosphere particles.
 */
@Composable
private fun CricketStadiumAtmosphere() {
    val infiniteTransition = rememberInfiniteTransition(label = "stadium_atmo")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Left floodlight beam
        val leftBeam = Path().apply {
            moveTo(w * 0.05f, 0f)
            lineTo(w * 0.55f, h * 0.85f)
            lineTo(w * 0.25f, h * 0.85f)
            close()
        }
        drawPath(
            path = leftBeam,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x55E0F2FE).copy(alpha = pulse * 0.4f),
                    Color(0x1138BDF8),
                    Color.Transparent
                )
            )
        )

        // Right floodlight beam
        val rightBeam = Path().apply {
            moveTo(w * 0.95f, 0f)
            lineTo(w * 0.75f, h * 0.85f)
            lineTo(w * 0.45f, h * 0.85f)
            close()
        }
        drawPath(
            path = rightBeam,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x55E0F2FE).copy(alpha = pulse * 0.4f),
                    Color(0x1138BDF8),
                    Color.Transparent
                )
            )
        )
    }
}
