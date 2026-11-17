package com.sadsha.stickcricket.ui.components

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
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadsha.stickcricket.audio.SoundManager
import com.sadsha.stickcricket.haptics.HapticManager
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Shape types for confetti pieces.
 */
enum class ConfettiShape {
    RECTANGLE,
    CIRCLE,
    STAR,
    RIBBON
}

/**
 * Data model for a single animated confetti particle.
 */
private class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val shape: ConfettiShape,
    val size: Float,
    var rotation: Float,
    val rotationSpeed: Float,
    var flipPhase: Float,
    val flipSpeed: Float,
    val swayFrequency: Float,
    val swayAmplitude: Float,
    val drag: Float = 0.985f
)

/**
 * CelebrationOverlay displays celebratory particle animations (colorful confetti,
 * star bursts) and dynamic text pop-up badges when the player achieves a high score
 * or wins a match.
 */
@Composable
fun CelebrationOverlay(
    isHighScore: Boolean,
    isVictory: Boolean,
    runs: Int,
    target: Int,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val soundManager = remember(context) { SoundManager.getInstance(context) }
    val hapticManager = remember(context) { HapticManager(context) }

    // Play celebration audio and haptics when overlay enters
    LaunchedEffect(isHighScore, isVictory) {
        soundManager.playSound(SoundManager.SoundType.CROWD_CHEER, volume = 1.0f)
        if (isHighScore) {
            soundManager.playSound(SoundManager.SoundType.COIN_REWARD, volume = 0.8f)
        }
        hapticManager.vibrateSix()
    }

    // Spring entrance animations for badges & dynamic text pop-up
    val badgeScale = remember { Animatable(1f) }
    val cardAlpha = remember { Animatable(1f) }
    val cardScale = remember { Animatable(1f) }

    // Infinite pulsing glow for celebration badge
    val infiniteTransition = rememberInfiniteTransition(label = "celebration_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("celebration_overlay")
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xD90F172A),
                        Color(0xF0090D16)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        // 1. Confetti Physics Simulation Canvas
        ConfettiCanvas(
            widthPx = widthPx,
            heightPx = heightPx,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Central Dynamic Pop-Up Card & Badges
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Trophy / Fire Badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(badgeScale.value * pulseScale)
                    .size(64.dp)
            ) {
                // Outer Radial Glow
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = if (isHighScore) {
                                    listOf(Color(0x80FFD700), Color.Transparent)
                                } else {
                                    listOf(Color(0x8010B981), Color.Transparent)
                                }
                            ),
                            shape = CircleShape
                        )
                )

                // Golden / Emerald Ring Container
                Surface(
                    shape = CircleShape,
                    color = if (isHighScore) Color(0xFF1E1B10) else Color(0xFF0D251C),
                    border = BorderStroke(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = if (isHighScore) {
                                listOf(Color(0xFFFFD700), Color(0xFFFF9100))
                            } else {
                                listOf(Color(0xFF34D399), Color(0xFF059669))
                            }
                        )
                    ),
                    modifier = Modifier.size(52.dp),
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isHighScore) Icons.Default.LocalFireDepartment else Icons.Default.EmojiEvents,
                            contentDescription = "Celebration Trophy",
                            tint = if (isHighScore) Color(0xFFFFD700) else Color(0xFF10B981),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Dynamic Pop-Up Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = if (isHighScore && isVictory) {
                            listOf(Color(0xFFFFD700), Color(0xFF10B981), Color(0xFF38BDF8))
                        } else if (isHighScore) {
                            listOf(Color(0xFFFFD700), Color(0xFFFF6D00))
                        } else {
                            listOf(Color(0xFF10B981), Color(0xFF00E5FF))
                        }
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(cardScale.value)
                    .shadow(16.dp, RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* Consume click so card itself doesn't dismiss unintentionally */ }
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Close button in top right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("dismiss_celebration_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Celebration",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Top Ribbon Tag
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isHighScore && isVictory) {
                            Color(0x33FFD700)
                        } else if (isHighScore) {
                            Color(0x33FF9800)
                        } else {
                            Color(0x3310B981)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isHighScore) Color(0x66FFD700) else Color(0x6610B981)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = null,
                                tint = if (isHighScore) Color(0xFFFFD700) else Color(0xFF34D399),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when {
                                    isHighScore && isVictory -> "DOUBLE TRIUMPH!"
                                    isHighScore -> "RECORD SMASHED!"
                                    else -> "MATCH WINNER!"
                                },
                                color = if (isHighScore) Color(0xFFFFD700) else Color(0xFF34D399),
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Dynamic Headline Pop-Up
                    Text(
                        text = when {
                            isHighScore && isVictory -> "NEW HIGH SCORE &\nMATCH WON!"
                            isHighScore -> "NEW HIGH SCORE!"
                            else -> "VICTORY!"
                        },
                        color = Color.White,
                        fontSize = if (isHighScore && isVictory) 19.sp else 22.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Score Display Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$runs",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFFD700)
                                )
                                Text(
                                    text = "RUNS SCORED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8),
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(28.dp)
                                    .background(Color(0xFF334155))
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$target",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isVictory) Color(0xFF10B981) else Color.White
                                )
                                Text(
                                    text = "TARGET RUNS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8),
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Celebratory Subtitle
                    Text(
                        text = when {
                            isHighScore && isVictory -> "Legendary batting! You shattered your personal record and conquered the target with supreme mastery!"
                            isHighScore -> "Unbelievable strokeplay! You set a sensational new personal best that puts you among the cricket greats!"
                            else -> "Sensational batting masterclass! You crushed the match target and brought the stadium to their feet!"
                        },
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Primary Action Button
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isHighScore) Color(0xFFFF9800) else Color(0xFF10B981)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("celebration_continue_button")
                    ) {
                        Text(
                            text = if (isHighScore) "CLAIM GLORY 🏆" else "AWESOME! 🎉",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * GPU-accelerated Confetti Canvas Particle Simulation.
 */
@Composable
private fun ConfettiCanvas(
    widthPx: Float,
    heightPx: Float,
    modifier: Modifier = Modifier
) {
    // Vibrant confetti palette: Gold, Orange, Emerald, Cyan, Magenta, White
    val confettiColors = remember {
        listOf(
            Color(0xFFFFD700), // Pure Gold
            Color(0xFFFF9100), // Vivid Orange
            Color(0xFF00E676), // Bright Emerald
            Color(0xFF00E5FF), // Electric Cyan
            Color(0xFFFF4081), // Neon Pink
            Color(0xFFA855F7), // Royal Purple
            Color(0xFFFFFFFF)  // White flash
        )
    }

    val particleCount = 85
    val particles = remember(widthPx, heightPx) {
        val list = ArrayList<Particle>(particleCount)
        val w = if (widthPx > 0f) widthPx else 1080f
        val h = if (heightPx > 0f) heightPx else 1920f

        for (i in 0 until particleCount) {
            val fromLeft = i % 2 == 0
            // Cannon burst origin: bottom left or bottom right
            val originX = if (fromLeft) Random.nextFloat() * (w * 0.35f) else w - Random.nextFloat() * (w * 0.35f)
            val originY = h * 0.2f + Random.nextFloat() * (h * 0.5f)

            val vx = if (fromLeft) Random.nextFloat() * 320f + 60f else -(Random.nextFloat() * 320f + 60f)
            val vy = -(Random.nextFloat() * 650f + 250f)

            val shape = when (i % 4) {
                0 -> ConfettiShape.RECTANGLE
                1 -> ConfettiShape.CIRCLE
                2 -> ConfettiShape.STAR
                else -> ConfettiShape.RIBBON
            }

            list.add(
                Particle(
                    x = originX,
                    y = originY,
                    vx = vx,
                    vy = vy,
                    color = confettiColors[i % confettiColors.size],
                    shape = shape,
                    size = Random.nextFloat() * 12f + 8f,
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = (Random.nextFloat() * 360f - 180f) * 2f,
                    flipPhase = Random.nextFloat() * 6.28f,
                    flipSpeed = Random.nextFloat() * 6f + 3f,
                    swayFrequency = Random.nextFloat() * 4f + 2f,
                    swayAmplitude = Random.nextFloat() * 35f + 15f
                )
            )
        }
        list
    }

    var frameTick by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(widthPx, heightPx) {
        var lastTime = 0L
        while (true) {
            withFrameNanos { timeNanos ->
                if (lastTime != 0L) {
                    val dt = ((timeNanos - lastTime) / 1_000_000_000f).coerceIn(0.005f, 0.033f)
                    val w = if (widthPx > 0f) widthPx else 1080f
                    val h = if (heightPx > 0f) heightPx else 1920f
                    val gravity = 450f

                    for (p in particles) {
                        p.vy += gravity * dt
                        p.vx *= p.drag

                        // Sway with sinusoidal wind
                        p.flipPhase += p.flipSpeed * dt
                        val sway = sin(p.flipPhase * p.swayFrequency) * p.swayAmplitude * dt
                        p.x += (p.vx * dt) + sway
                        p.y += p.vy * dt
                        p.rotation += p.rotationSpeed * dt

                        // Wrap or respawn particles if fallen below screen
                        if (p.y > h + 50f) {
                            p.y = -20f
                            p.x = Random.nextFloat() * w
                            p.vy = Random.nextFloat() * 180f + 60f
                            p.vx = (Random.nextFloat() - 0.5f) * 120f
                        }
                    }
                    frameTick += dt
                }
                lastTime = timeNanos
            }
        }
    }

    Canvas(modifier = modifier) {
        // Suppress unused warning by referencing frameTick to ensure re-draw on each frame
        if (frameTick < 0f) return@Canvas

        for (p in particles) {
            val flipFactor = abs(cos(p.flipPhase)).coerceIn(0.05f, 1.0f)

            rotate(degrees = p.rotation, pivot = Offset(p.x, p.y)) {
                when (p.shape) {
                    ConfettiShape.RECTANGLE -> {
                        val rectWidth = p.size * flipFactor
                        val rectHeight = p.size * 1.8f
                        drawRoundRect(
                            color = p.color,
                            topLeft = Offset(p.x - rectWidth / 2f, p.y - rectHeight / 2f),
                            size = Size(rectWidth, rectHeight)
                        )
                    }
                    ConfettiShape.CIRCLE -> {
                        drawCircle(
                            color = p.color,
                            radius = (p.size / 2f) * flipFactor,
                            center = Offset(p.x, p.y)
                        )
                    }
                    ConfettiShape.STAR -> {
                        drawStar(
                            center = Offset(p.x, p.y),
                            radius = (p.size * 0.8f) * flipFactor,
                            color = p.color
                        )
                    }
                    ConfettiShape.RIBBON -> {
                        val ribbonWidth = (p.size * 0.7f) * flipFactor
                        val ribbonHeight = p.size * 2.4f
                        drawRoundRect(
                            color = p.color,
                            topLeft = Offset(p.x - ribbonWidth / 2f, p.y - ribbonHeight / 2f),
                            size = Size(ribbonWidth, ribbonHeight)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Draws a 4-point star on the canvas for celebration sparkles.
 */
private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    if (radius <= 0f) return
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x, center.y, center.x + radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + radius)
        quadraticTo(center.x, center.y, center.x - radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - radius)
        close()
    }
    drawPath(path = path, color = color)
}
