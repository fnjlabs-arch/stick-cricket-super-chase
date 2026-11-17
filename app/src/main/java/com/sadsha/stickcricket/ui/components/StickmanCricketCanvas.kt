package com.sadsha.stickcricket.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.sadsha.stickcricket.game.BatsmanPose
import com.sadsha.stickcricket.game.BowlerPhase
import com.sadsha.stickcricket.game.GameEngine
import com.sadsha.stickcricket.game.GameState
import com.sadsha.stickcricket.model.CosmeticItem
import com.sadsha.stickcricket.model.DeliveryType
import com.sadsha.stickcricket.model.OutcomeType
import com.sadsha.stickcricket.model.ShotDirection
import com.sadsha.stickcricket.model.TimingResult
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 2.5D Stick Cricket Canvas with Bowler-to-Batter Broadcast Perspective.
 *
 * Coordinate System & Layout:
 * - BOWLER: Top-Center (Far end of the pitch, running in towards the batsman).
 * - BATSMAN: Bottom-Center (Near end of the pitch, standing at the batting crease facing UP towards the bowler).
 * - PITCH: Perspective pitch tapering from wide at bottom (near batsman) to narrow at top (near bowler).
 * - BALL FLIGHT: Delivered from bowler at top-center downwards towards batsman at bottom-center.
 * - HITTING ZONE: Positioned on the pitch in front of the batsman (sweet spot at 85% progress).
 * - STADIUM: Grandstand, floodlights, sightscreen, and cheering crowd in the background behind the bowler.
 */
@Composable
fun StickmanCricketCanvas(
    gameState: GameState,
    modifier: Modifier = Modifier,
    onCanvasTap: ((x: Float, y: Float, width: Float, height: Float) -> Unit)? = null
) {
    val pulseAnim = remember { Animatable(0.85f) }
    LaunchedEffect(Unit) {
        pulseAnim.animateTo(
            targetValue = 1.10f,
            animationSpec = infiniteRepeatable(
                animation = tween(700, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    val crowdAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        crowdAnim.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    val tapModifier = if (onCanvasTap != null) {
        Modifier.pointerInput(gameState.isBallInFlight) {
            detectTapGestures { offset ->
                if (gameState.isBallInFlight && !gameState.isGameOver && !gameState.isPaused) {
                    onCanvasTap(offset.x, offset.y, size.width.toFloat(), size.height.toFloat())
                }
            }
        }
    } else Modifier

    Canvas(modifier = modifier.fillMaxSize().then(tapModifier)) {
        val width = size.width
        val height = size.height
        val centerX = width * 0.5f

        // Responsive scale factor based on screen width
        val u = (width / 360f).coerceIn(1.0f, 4.5f)

        // 1. LAYERED STADIUM IN BACKGROUND (Behind the batsman in distance)
        val horizonY = height * 0.17f
        drawStadium(width = width, height = height, horizonY = horizonY, u = u, crowdWave = crowdAnim.value)

        // 2. LAYERED OUTFIELD GRASS
        drawOutfield(width = width, height = height, horizonY = horizonY)

        // 3. PERSPECTIVE PITCH (Trapezoidal: Bowler in foreground near camera, Batsman in distance in front)
        val pitchTopY = height * 0.22f       // Batsman's end in front (far distance)
        val pitchBottomY = height * 0.76f    // Bowler's end near camera (foreground)
        val pitchTopWidth = width * 0.22f    // Far end width (narrower at top)
        val pitchBottomWidth = width * 0.48f // Near end width (wider at bottom - trapezoid)

        drawPitch(
            centerX = centerX,
            topY = pitchTopY,
            bottomY = pitchBottomY,
            topWidth = pitchTopWidth,
            bottomWidth = pitchBottomWidth,
            u = u
        )

        // 4. CREASE MARKINGS (Batting crease at top, Bowling crease at bottom)
        drawCreases(
            centerX = centerX,
            topY = pitchTopY,
            bottomY = pitchBottomY,
            topWidth = pitchTopWidth,
            bottomWidth = pitchBottomWidth,
            u = u
        )

        // 5. FAR WICKETS (Striker's stumps at top behind batsman)
        val isWicket = gameState.lastOutcome?.isWicket == true
        val knockProgress = if (isWicket && !gameState.isBallInFlight) (1f - gameState.contactFlashProgress).coerceIn(0f, 1f) else 0f
        drawWickets(
            centerX = centerX,
            baseY = pitchTopY,
            stumpHeight = 22f * u,
            stumpWidth = 2.4f * u,
            spread = 11f * u,
            u = u,
            isKnocked = isWicket && !gameState.isBallInFlight,
            knockProgress = knockProgress
        )

            // 6. BATSMAN IN FRONT (Facing Bowler and Camera)
            val batsmanBaseX = centerX - 6f * u
            val batsmanBaseY = pitchTopY + 12f * u
            drawBatsmanFacingFront(
                x = batsmanBaseX,
                y = batsmanBaseY,
                pose = gameState.batsmanPose,
                swingAngle = gameState.batsmanSwingAngle,
                helmet = gameState.equippedHelmet,
                jersey = gameState.equippedJersey,
                bat = gameState.equippedBat,
                u = u,
                poseProgress = gameState.batsmanPoseProgress,
                isGoldenBat = gameState.isGoldenBatActive
            )

            // 7. HITTING SWEET SPOT ZONE (On pitch in front of batsman)
            // Progress = 0.85f (HIT_SWEET_SPOT)
            val sweetSpotY = pitchBottomY + (pitchTopY - pitchBottomY) * GameEngine.HIT_SWEET_SPOT
            val sweetSpotWidth = (pitchBottomWidth + (pitchTopWidth - pitchBottomWidth) * GameEngine.HIT_SWEET_SPOT) * 0.92f

            drawHittingZone(
                centerX = centerX,
                sweetSpotY = sweetSpotY,
                zoneWidth = sweetSpotWidth,
                pulse = pulseAnim.value,
                u = u,
                isBallApproaching = gameState.isBallInFlight && gameState.ballProgress in 0.55f..0.95f
            )

            // 8. NEAR WICKETS (Non-striker's stumps at bowler's end in foreground)
            val bowlerStumpY = pitchBottomY + 4f * u
            drawWickets(
                centerX = centerX,
                baseY = bowlerStumpY,
                stumpHeight = 38f * u,
                stumpWidth = 3.8f * u,
                spread = 18f * u,
                u = u,
                isKnocked = false
            )

            // 9. BOWLER IN FOREGROUND (Camera behind bowler, bowling forward towards batsman)
            val bowlerBaseX = centerX + 18f * u
            val bowlerBaseY = pitchBottomY + 16f * u
            drawBowlerFromBehind(
                centerX = bowlerBaseX,
                baseY = bowlerBaseY,
                phase = gameState.bowlerPhase,
                runUpProgress = gameState.bowlerRunUpProgress,
                u = u
            )

            // 10. CRICKET BALL IN FLIGHT (From Bowler at bottom up to Batsman at top)
            if (gameState.isBallInFlight) {
                drawBallInFlight(
                    centerX = centerX,
                    bowlerReleaseX = bowlerBaseX - 3f * u,
                    pitchTopY = pitchTopY,
                    pitchBottomY = pitchBottomY,
                    progress = gameState.ballProgress,
                    sideDrift = gameState.ballSideDrift,
                    delivery = gameState.currentDelivery,
                    ballCosmetic = gameState.equippedBall,
                    u = u
                )
            }

            // 11. BALL EXIT TRAJECTORY (After Shot Contact)
            // SIX: Rockets down towards camera / over bowler's head into foreground!
            // FOUR: Sizzles to boundaries past the bowler.
            if (!gameState.isBallInFlight && gameState.lastOutcome != null) {
                drawHitBallTrajectory(
                    centerX = centerX,
                    batsmanBaseX = batsmanBaseX + 6f * u,
                    sweetSpotY = sweetSpotY,
                    outcome = gameState.lastOutcome.outcomeType,
                    direction = gameState.lastOutcome.direction,
                    exitProgress = gameState.hitExitProgress,
                    ballCosmetic = gameState.equippedBall,
                    u = u
                )
            }

            // 12. BAT CONTACT IMPACT FLASH & WICKET-HIT EVENTS
            if (gameState.contactFlashProgress > 0.01f) {
                if (isWicket) {
                    drawWicketHitImpact(
                        centerX = centerX,
                        stumpTopY = pitchTopY - 14f * u,
                        progress = gameState.contactFlashProgress,
                        u = u
                    )
                } else {
                    val impactX = batsmanBaseX + 6f * u
                    val impactY = sweetSpotY
                    drawBatContactImpact(
                        x = impactX,
                        y = impactY,
                        progress = gameState.contactFlashProgress,
                        outcome = gameState.lastOutcome?.outcomeType,
                        timing = gameState.lastOutcome?.timing,
                        u = u
                    )
                }
            }

            // 13. BOUNDARY CELEBRATION (Confetti on 4 or 6)
            if (!gameState.isBallInFlight && gameState.lastOutcome != null &&
                (gameState.lastOutcome.outcomeType == OutcomeType.SIX || gameState.lastOutcome.outcomeType == OutcomeType.FOUR)
            ) {
                drawBoundaryCelebration(
                    width = width,
                    height = height,
                    outcome = gameState.lastOutcome.outcomeType,
                    progress = gameState.hitExitProgress,
                    u = u
                )
            }

            // 14. SLOW MOTION ATMOSPHERE VIGNETTE (Cyan time-dilation field)
            if (gameState.isSlowMotionActive) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0x2200E5FF), Color(0x440284C7)),
                        center = Offset(centerX, height * 0.5f),
                        radius = width * 0.75f
                    ),
                    topLeft = Offset.Zero,
                    size = Size(width, height)
                )
            }

            // 15. TOUCH PRECISION RIPPLE & SHOT STRENGTH FEEDBACK
            if (gameState.tapFeedbackProgress > 0.01f && gameState.lastTapXRatio >= 0f && gameState.lastTapYRatio >= 0f) {
                val tapX = gameState.lastTapXRatio * width
                val tapY = gameState.lastTapYRatio * height
                drawTapPrecisionFeedback(
                    tapX = tapX,
                    tapY = tapY,
                    progress = gameState.tapFeedbackProgress,
                    shotStrength = gameState.lastShotStrength,
                    precisionRating = gameState.lastPrecisionRating,
                    u = u
                )
            }

            // 16. TOUCH DIRECTION GUIDES DURING BALL FLIGHT
            if (gameState.isBallInFlight) {
                drawTouchDirectionGuides(width = width, height = height, u = u)
            }
        }
    }

/**
 * 15. TOUCH PRECISION RIPPLE & SHOT STRENGTH RETICLE
 * Renders an expanding holographic reticle and floating power badge at the exact touch location.
 */
private fun DrawScope.drawTapPrecisionFeedback(
    tapX: Float,
    tapY: Float,
    progress: Float,
    shotStrength: Float,
    precisionRating: String?,
    u: Float
) {
    val alpha = progress.coerceIn(0f, 1f)
    val expandFactor = 1.0f + (1.0f - progress) * 1.6f
    val baseRadius = 26f * u * expandFactor

    val themeColor = when {
        shotStrength >= 0.90f -> Color(0xFFFFD700) // Gold
        shotStrength >= 0.75f -> Color(0xFF10B981) // Emerald
        shotStrength >= 0.55f -> Color(0xFF00E5FF) // Cyan
        shotStrength >= 0.35f -> Color(0xFFF59E0B) // Amber
        else -> Color(0xFFEF4444) // Red
    }

    // Outer expanding precision ring
    drawCircle(
        color = themeColor.copy(alpha = alpha * 0.8f),
        radius = baseRadius,
        center = Offset(tapX, tapY),
        style = Stroke(width = (2.6f * u * progress).coerceAtLeast(1.2f))
    )

    // Inner bright sweet-spot precision ring
    drawCircle(
        color = Color.White.copy(alpha = alpha * 0.95f),
        radius = baseRadius * 0.45f,
        center = Offset(tapX, tapY),
        style = Stroke(width = (1.8f * u * progress).coerceAtLeast(1f))
    )

    // Center precision pip
    drawCircle(
        color = themeColor.copy(alpha = alpha),
        radius = 4.5f * u,
        center = Offset(tapX, tapY)
    )

    // Crosshairs
    val crossLen = baseRadius * 1.35f
    drawLine(
        color = themeColor.copy(alpha = alpha * 0.7f),
        start = Offset(tapX - crossLen, tapY),
        end = Offset(tapX + crossLen, tapY),
        strokeWidth = 1.4f * u
    )
    drawLine(
        color = themeColor.copy(alpha = alpha * 0.7f),
        start = Offset(tapX, tapY - crossLen),
        end = Offset(tapX, tapY + crossLen),
        strokeWidth = 1.4f * u
    )

    // Draw text badge using nativeCanvas
    val label = precisionRating ?: "${(shotStrength * 100).toInt()}% POWER"
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 12.5f * u
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
            this.alpha = (alpha * 255).toInt()
            setShadowLayer(8f, 0f, 2f, android.graphics.Color.BLACK)
        }
        drawText(label, tapX, tapY - baseRadius - 8f * u, paint)
    }
}

/**
 * 16. SUBTLE TOUCH DIRECTION LANE GUIDES
 * Displays unobtrusive lane guides across the bottom to communicate screen touch controls.
 */
private fun DrawScope.drawTouchDirectionGuides(
    width: Float,
    height: Float,
    u: Float
) {
    val bottomY = height * 0.89f
    val lineStartY = height * 0.76f
    val lineEndY = height * 0.94f

    val leftDivX = width * 0.35f
    val rightDivX = width * 0.65f

    // Soft dashed lane dividers
    drawLine(
        color = Color(0x3538BDF8),
        start = Offset(leftDivX, lineStartY),
        end = Offset(leftDivX, lineEndY),
        strokeWidth = 1.4f * u
    )
    drawLine(
        color = Color(0x3538BDF8),
        start = Offset(rightDivX, lineStartY),
        end = Offset(rightDivX, lineEndY),
        strokeWidth = 1.4f * u
    )

    // Direction text hints
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            textSize = 10f * u
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
            setShadowLayer(4f, 0f, 1f, android.graphics.Color.BLACK)
        }

        paint.color = android.graphics.Color.argb(130, 186, 230, 253)
        drawText("⇦ OFF SIDE", width * 0.175f, bottomY, paint)

        paint.color = android.graphics.Color.argb(170, 255, 215, 0)
        drawText("⇧ LOFT 6", width * 0.50f, bottomY, paint)

        paint.color = android.graphics.Color.argb(130, 186, 230, 253)
        drawText("LEG SIDE ⇨", width * 0.825f, bottomY, paint)
    }
}

/**
 * 1. LAYERED STADIUM IN BACKGROUND (Behind the bowler at top)
 * Dusk sky, floodlight towers, grandstand tiers, animated crowd, white sightscreen, and boundary hoardings.
 */
private fun DrawScope.drawStadium(width: Float, height: Float, horizonY: Float, u: Float, crowdWave: Float) {
    // Dusk Gradient Sky
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF070F1E),
                Color(0xFF0E1C33),
                Color(0xFF162D4A),
                Color(0xFF1E3A52)
            ),
            startY = 0f,
            endY = horizonY
        ),
        topLeft = Offset(0f, 0f),
        size = Size(width, horizonY)
    )

    // 4 Floodlight Towers
    val lightY = horizonY * 0.35f
    drawFloodlightTower(x = width * 0.08f, y = lightY, u = u)
    drawFloodlightTower(x = width * 0.28f, y = lightY - 2f * u, u = u)
    drawFloodlightTower(x = width * 0.72f, y = lightY - 2f * u, u = u)
    drawFloodlightTower(x = width * 0.92f, y = lightY, u = u)

    // Grandstand Seating Tiers & Roof Canopy
    val standTopY = horizonY * 0.28f
    val standHeight = horizonY - standTopY

    // Canopy Roof
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B)),
            startY = standTopY,
            endY = standTopY + standHeight * 0.30f
        ),
        topLeft = Offset(0f, standTopY),
        size = Size(width, standHeight * 0.30f)
    )

    // Concrete Tier Riser
    val tierY = standTopY + standHeight * 0.28f
    val tierHeight = standHeight * 0.72f
    drawRect(
        color = Color(0xFF162032),
        topLeft = Offset(0f, tierY),
        size = Size(width, tierHeight)
    )

    // Spectator Crowd Silhouettes
    drawCrowd(
        width = width,
        startY = tierY + 1f * u,
        height = (tierHeight - 3f * u).coerceAtLeast(4f * u),
        u = u,
        wave = crowdWave
    )

    // Traditional White Sightscreen (Behind bowler at center)
    val sightscreenWidth = 48f * u
    val sightscreenHeight = 11f * u
    val sightscreenY = (horizonY - 12f * u).coerceAtLeast(2f * u)
    drawRect(
        color = Color(0xFFF1F5F9),
        topLeft = Offset(width * 0.5f - sightscreenWidth / 2f, sightscreenY),
        size = Size(sightscreenWidth, sightscreenHeight)
    )
    // Sightscreen slats
    for (i in 1..7) {
        val slatX = (width * 0.5f - sightscreenWidth / 2f) + i * (sightscreenWidth / 8f)
        drawLine(
            color = Color(0xFFCBD5E1),
            start = Offset(slatX, sightscreenY),
            end = Offset(slatX, sightscreenY + sightscreenHeight),
            strokeWidth = 1.0f * u
        )
    }

    // Perimeter Advertising LED Boards
    drawBoundaryHoardings(width = width, y = horizonY - 4f * u, u = u)

    // Curved Perspective Boundary Rope
    drawBoundaryRope(width = width, y = horizonY + 1f * u, u = u)
}

/**
 * Floodlight Tower with luminous bloom
 */
private fun DrawScope.drawFloodlightTower(x: Float, y: Float, u: Float) {
    val bloomRadius = 48f * u
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x77FFFBEB),
                Color(0x33FEF08A),
                Color(0x11FDE047),
                Color.Transparent
            ),
            center = Offset(x, y),
            radius = bloomRadius
        ),
        radius = bloomRadius,
        center = Offset(x, y)
    )

    // Pylon structure
    drawLine(
        color = Color(0xFF475569),
        start = Offset(x, y + 3f * u),
        end = Offset(x - 9f * u, y + 26f * u),
        strokeWidth = 2f * u
    )
    drawLine(
        color = Color(0xFF475569),
        start = Offset(x, y + 3f * u),
        end = Offset(x + 9f * u, y + 26f * u),
        strokeWidth = 2f * u
    )

    // Light head
    drawRect(
        color = Color(0xFFFEF9C3),
        topLeft = Offset(x - 12f * u, y - 4f * u),
        size = Size(24f * u, 7f * u)
    )
}

/**
 * Spectator Crowd with cheering wave motion
 */
private fun DrawScope.drawCrowd(width: Float, startY: Float, height: Float, u: Float, wave: Float) {
    val crowdColors = listOf(
        Color(0xFF38BDF8),
        Color(0xFFF59E0B),
        Color(0xFFEF4444),
        Color(0xFF10B981),
        Color(0xFFE2E8F0),
        Color(0xFFA855F7),
        Color(0xFFF43F5E)
    )

    val rows = 3
    val cols = 30
    val colSpacing = width / cols
    val rowSpacing = height / rows

    for (r in 0 until rows) {
        val y = startY + r * rowSpacing
        for (c in 0 until cols) {
            val color = crowdColors[(c * 11 + r * 23) % crowdColors.size]
            val x = c * colSpacing + (if (r % 2 == 1) colSpacing * 0.5f else 0f)
            val bob = sin(wave * PI.toFloat() * 2f + c * 0.4f + r * 0.9f) * 1.8f * u

            drawCircle(
                color = Color(0xFF0B132B),
                radius = 2.2f * u,
                center = Offset(x, y + bob)
            )
            drawRect(
                color = color.copy(alpha = 0.9f),
                topLeft = Offset(x - 2.5f * u, y + 2.2f * u + bob),
                size = Size(5f * u, 4.5f * u)
            )
        }
    }
}

/**
 * Perimeter Advertising LED Boards
 */
private fun DrawScope.drawBoundaryHoardings(width: Float, y: Float, u: Float) {
    val panelCount = 8
    val panelWidth = width / panelCount
    val panelHeight = 10f * u
    val boardColors = listOf(
        Color(0xFF0369A1),
        Color(0xFFB45309),
        Color(0xFF047857),
        Color(0xFF6D28D9),
        Color(0xFFBE123C),
        Color(0xFF0F766E)
    )

    for (i in 0 until panelCount) {
        val x = i * panelWidth
        val color = boardColors[i % boardColors.size]
        drawRect(
            color = color,
            topLeft = Offset(x, y),
            size = Size(panelWidth - 2f * u, panelHeight)
        )
        drawRect(
            color = Color.White.copy(alpha = 0.45f),
            topLeft = Offset(x, y),
            size = Size(panelWidth - 2f * u, 1.5f * u)
        )
    }
}

/**
 * Curved Boundary Rope with red & white pennants
 */
private fun DrawScope.drawBoundaryRope(width: Float, y: Float, u: Float) {
    drawLine(
        color = Color.White.copy(alpha = 0.95f),
        start = Offset(0f, y),
        end = Offset(width, y),
        strokeWidth = 2.5f * u
    )
    val segWidth = 20f * u
    val count = (width / segWidth).toInt()
    for (i in 0 until count step 2) {
        drawLine(
            color = Color(0xFFEF4444),
            start = Offset(i * segWidth, y),
            end = Offset((i + 1) * segWidth, y),
            strokeWidth = 2.6f * u
        )
    }
}

/**
 * 2. LAYERED OUTFIELD GRASS
 */
private fun DrawScope.drawOutfield(width: Float, height: Float, horizonY: Float) {
    val outfieldHeight = height - horizonY

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1B4D2C),
                Color(0xFF26663B),
                Color(0xFF2E7544),
                Color(0xFF215B34)
            ),
            startY = horizonY,
            endY = height
        ),
        topLeft = Offset(0f, horizonY),
        size = Size(width, outfieldHeight)
    )

    // Perspective radial mowing stripes
    val stripeCount = 12
    val stripeHeight = outfieldHeight / stripeCount
    for (i in 0 until stripeCount) {
        if (i % 2 == 1) {
            val y = horizonY + i * stripeHeight
            drawRect(
                color = Color(0x14FFFFFF),
                topLeft = Offset(0f, y),
                size = Size(width, stripeHeight)
            )
        }
    }

    // 30-Yard Field Restriction Circle Discs
    val discY = horizonY + outfieldHeight * 0.38f
    val discPositions = listOf(
        width * 0.12f,
        width * 0.26f,
        width * 0.74f,
        width * 0.88f
    )
    for (dx in discPositions) {
        drawOval(
            color = Color.White.copy(alpha = 0.75f),
            topLeft = Offset(dx - 6f, discY - 3f),
            size = Size(12f, 6f)
        )
    }

    // Lateral vignettes
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0x35000000), Color.Transparent),
            startX = 0f,
            endX = width * 0.16f
        ),
        topLeft = Offset(0f, horizonY),
        size = Size(width * 0.16f, outfieldHeight)
    )
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color(0x35000000)),
            startX = width * 0.84f,
            endX = width
        ),
        topLeft = Offset(width * 0.84f, horizonY),
        size = Size(width * 0.16f, outfieldHeight)
    )
}

/**
 * 3. PERSPECTIVE PITCH
 * Narrow at top (bowler's end), wide at bottom (batsman's end).
 */
private fun DrawScope.drawPitch(
    centerX: Float,
    topY: Float,
    bottomY: Float,
    topWidth: Float,
    bottomWidth: Float,
    u: Float
) {
    val pitchPath = Path().apply {
        moveTo(centerX - topWidth / 2f, topY)
        lineTo(centerX + topWidth / 2f, topY)
        lineTo(centerX + bottomWidth / 2f, bottomY)
        lineTo(centerX - bottomWidth / 2f, bottomY)
        close()
    }

    // Grass fringe shadow
    val fringePath = Path().apply {
        moveTo(centerX - topWidth / 2f - 3f * u, topY)
        lineTo(centerX + topWidth / 2f + 3f * u, topY)
        lineTo(centerX + bottomWidth / 2f + 5f * u, bottomY + 3f * u)
        lineTo(centerX - bottomWidth / 2f - 5f * u, bottomY + 3f * u)
        close()
    }
    drawPath(path = fringePath, color = Color(0x30000000))

    // Pitch Clay Turf Gradient
    drawPath(
        path = pitchPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFDFCBB5),
                Color(0xFFD2BCA0),
                Color(0xFFC7AF90),
                Color(0xFFBEA484)
            ),
            startY = topY,
            endY = bottomY
        )
    )

    // Border stroke
    drawPath(
        path = pitchPath,
        color = Color(0xFF8D6E63).copy(alpha = 0.5f),
        style = Stroke(width = 1.8f * u)
    )

    // Good-length wear patch (where deliveries pitch on landing, ~52% from bowler towards batsman)
    val wearY = bottomY + (topY - bottomY) * 0.52f
    val wearWidth = (bottomWidth + (topWidth - bottomWidth) * 0.52f) * 0.65f
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x338D6E63),
                Color(0x18A1887F),
                Color.Transparent
            ),
            center = Offset(centerX, wearY),
            radius = wearWidth * 0.5f
        ),
        topLeft = Offset(centerX - wearWidth * 0.5f, wearY - 10f * u),
        size = Size(wearWidth, 20f * u)
    )

    // Bowler landing footmarks near bottom bowling crease
    drawOval(
        color = Color(0x2E5D4037),
        topLeft = Offset(centerX + bottomWidth * 0.14f, bottomY - 10f * u),
        size = Size(9f * u, 5f * u)
    )
}

/**
 * 4. CREASE MARKINGS
 * Batting crease at top (far batsman), Bowling crease at bottom (near bowler).
 */
private fun DrawScope.drawCreases(
    centerX: Float,
    topY: Float,
    bottomY: Float,
    topWidth: Float,
    bottomWidth: Float,
    u: Float
) {
    val whiteCrease = Color.White.copy(alpha = 0.92f)

    // Top Bowling Crease (Where striker stumps stand)
    val topBowlingY = topY
    drawLine(
        color = whiteCrease,
        start = Offset(centerX - topWidth * 0.46f, topBowlingY),
        end = Offset(centerX + topWidth * 0.46f, topBowlingY),
        strokeWidth = 1.8f * u
    )

    // Top Popping Crease (Prominent batting crease where batsman stands)
    val topPoppingY = topY + 12f * u
    drawLine(
        color = Color.White,
        start = Offset(centerX - topWidth * 0.50f, topPoppingY),
        end = Offset(centerX + topWidth * 0.50f, topPoppingY),
        strokeWidth = 2.4f * u
    )

    // Top Return Creases
    val leftTopX = centerX - topWidth * 0.48f
    val rightTopX = centerX + topWidth * 0.48f
    drawLine(color = whiteCrease, start = Offset(leftTopX, topBowlingY - 4f * u), end = Offset(leftTopX, topPoppingY + 6f * u), strokeWidth = 1.5f * u)
    drawLine(color = whiteCrease, start = Offset(rightTopX, topBowlingY - 4f * u), end = Offset(rightTopX, topPoppingY + 6f * u), strokeWidth = 1.5f * u)

    // Bottom Bowling Crease (Where bowler releases near camera)
    val bottomBowlingY = bottomY
    drawLine(
        color = whiteCrease,
        start = Offset(centerX - bottomWidth * 0.46f, bottomBowlingY),
        end = Offset(centerX + bottomWidth * 0.46f, bottomBowlingY),
        strokeWidth = 2.4f * u
    )

    // Bottom Return Creases
    val leftBottomX = centerX - bottomWidth * 0.46f
    val rightBottomX = centerX + bottomWidth * 0.46f
    drawLine(color = whiteCrease, start = Offset(leftBottomX, bottomBowlingY - 14f * u), end = Offset(leftBottomX, bottomBowlingY + 10f * u), strokeWidth = 1.8f * u)
    drawLine(color = whiteCrease, start = Offset(rightBottomX, bottomBowlingY - 14f * u), end = Offset(rightBottomX, bottomBowlingY + 10f * u), strokeWidth = 1.8f * u)
}

/**
 * 5 & 8. WICKETS / STUMPS (With Zing LED Flash & Realistic Flying Bails on Knockout)
 */
private fun DrawScope.drawWickets(
    centerX: Float,
    baseY: Float,
    stumpHeight: Float,
    stumpWidth: Float,
    spread: Float,
    u: Float,
    isKnocked: Boolean,
    knockProgress: Float = 0f
) {
    val woodColor = Color(0xFF8D6E63)
    val bailColor = Color(0xFFFFCA28)
    val ledColor = Color(0xFFFF1744)

    if (isKnocked) {
        val kp = knockProgress.coerceIn(0f, 1f)

        // Off Stump (Tilted Left and pushed back)
        rotate(degrees = -26f - kp * 36f, pivot = Offset(centerX - spread * 0.4f, baseY)) {
            drawLine(
                color = woodColor,
                start = Offset(centerX - spread * 0.4f, baseY),
                end = Offset(centerX - spread * 0.4f, baseY - stumpHeight),
                strokeWidth = stumpWidth,
                cap = StrokeCap.Round
            )
            // Zing Red LED cap flashing on impact
            drawCircle(
                color = ledColor,
                radius = stumpWidth * 0.85f,
                center = Offset(centerX - spread * 0.4f, baseY - stumpHeight)
            )
        }

        // Leg Stump (Tilted Right and pushed back)
        rotate(degrees = 24f + kp * 34f, pivot = Offset(centerX + spread * 0.4f, baseY)) {
            drawLine(
                color = woodColor,
                start = Offset(centerX + spread * 0.4f, baseY),
                end = Offset(centerX + spread * 0.4f, baseY - stumpHeight),
                strokeWidth = stumpWidth,
                cap = StrokeCap.Round
            )
            // Zing Red LED cap
            drawCircle(
                color = ledColor,
                radius = stumpWidth * 0.85f,
                center = Offset(centerX + spread * 0.4f, baseY - stumpHeight)
            )
        }

        // Middle Stump (Tilted back / pushed with impact momentum)
        rotate(degrees = kp * 22f, pivot = Offset(centerX, baseY)) {
            drawLine(
                color = woodColor,
                start = Offset(centerX, baseY),
                end = Offset(centerX + 3f * u * kp, baseY - stumpHeight * (0.85f - kp * 0.15f)),
                strokeWidth = stumpWidth,
                cap = StrokeCap.Round
            )
            // Zing Red LED cap
            drawCircle(
                color = ledColor,
                radius = stumpWidth * 0.85f,
                center = Offset(centerX + 3f * u * kp, baseY - stumpHeight * (0.85f - kp * 0.15f))
            )
        }

        // Flying Zing Bails in dynamic parabolic arc with rotational tumble
        // Left Bail
        val leftBailX = centerX - spread * 0.2f - kp * 22f * u
        val leftBailArc = sin(kp * PI.toFloat()) * 26f * u
        val leftBailY = (baseY - stumpHeight - 3f * u) - leftBailArc + kp * 8f * u
        rotate(degrees = kp * 480f, pivot = Offset(leftBailX, leftBailY)) {
            drawLine(
                color = bailColor,
                start = Offset(leftBailX - 5f * u, leftBailY),
                end = Offset(leftBailX + 5f * u, leftBailY),
                strokeWidth = stumpWidth * 0.75f,
                cap = StrokeCap.Round
            )
            drawCircle(color = ledColor, radius = stumpWidth * 0.6f, center = Offset(leftBailX - 5f * u, leftBailY))
            drawCircle(color = ledColor, radius = stumpWidth * 0.6f, center = Offset(leftBailX + 5f * u, leftBailY))
        }

        // Right Bail
        val rightBailX = centerX + spread * 0.2f + kp * 26f * u
        val rightBailArc = sin(kp * PI.toFloat()) * 30f * u
        val rightBailY = (baseY - stumpHeight - 3f * u) - rightBailArc + kp * 10f * u
        rotate(degrees = -kp * 540f, pivot = Offset(rightBailX, rightBailY)) {
            drawLine(
                color = bailColor,
                start = Offset(rightBailX - 5f * u, rightBailY),
                end = Offset(rightBailX + 5f * u, rightBailY),
                strokeWidth = stumpWidth * 0.75f,
                cap = StrokeCap.Round
            )
            drawCircle(color = ledColor, radius = stumpWidth * 0.6f, center = Offset(rightBailX - 5f * u, rightBailY))
            drawCircle(color = ledColor, radius = stumpWidth * 0.6f, center = Offset(rightBailX + 5f * u, rightBailY))
        }
    } else {
        val xOffsets = listOf(-spread * 0.4f, 0f, spread * 0.4f)
        for (dx in xOffsets) {
            val stumpX = centerX + dx
            drawLine(
                color = woodColor,
                start = Offset(stumpX, baseY),
                end = Offset(stumpX, baseY - stumpHeight),
                strokeWidth = stumpWidth,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color(0xFFD7CCC8),
                radius = stumpWidth * 0.45f,
                center = Offset(stumpX, baseY - stumpHeight)
            )
        }
        // Bails
        drawLine(
            color = bailColor,
            start = Offset(centerX - spread * 0.46f, baseY - stumpHeight - 1.2f * u),
            end = Offset(centerX + spread * 0.46f, baseY - stumpHeight - 1.2f * u),
            strokeWidth = stumpWidth * 0.75f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * 6. BATSMAN IN FRONT (Facing Bowler and Camera)
 * Standing at the batting crease at the top of the pitch, facing DOWN towards bowler and camera.
 * Complete front view details:
 * - Cricket helmet with front peak and metallic face grill / bars
 * - Front of team jersey with collar and crest
 * - White batting pads (front view) with knee rolls and vertical shin protectors
 * - Batting gloves and English Willow bat responding dynamically to STANCE, BACKLIFT, and SHOT SWINGS!
 */
private fun DrawScope.drawBatsmanFacingFront(
    x: Float,
    y: Float,
    pose: BatsmanPose,
    swingAngle: Float,
    helmet: CosmeticItem,
    jersey: CosmeticItem,
    bat: CosmeticItem,
    u: Float,
    poseProgress: Float = 0f,
    isGoldenBat: Boolean = false
) {
    val s = u * 0.65f
    val skinColor = Color(0xFF0F172A)
    val helmetPrimary = Color(helmet.primaryColorHex)
    val jerseyPrimary = Color(jersey.primaryColorHex)
    val jerseyAccent = Color(jersey.accentColorHex)
    val batBladeColor = if (isGoldenBat) Color(0xFFFFD700) else Color(bat.primaryColorHex)
    val batGripColor = if (isGoldenBat) Color(0xFFFFA000) else Color(bat.secondaryColorHex)
    val batStickerColor = if (isGoldenBat) Color(0xFFFFF9C4) else Color(bat.accentColorHex)
    val padColor = Color(0xFFF8FAFC)
    val padRibColor = Color(0xFFCBD5E1)
    val gloveColor = if (isGoldenBat) Color(0xFFFFECB3) else Color(0xFFFFFFFF)

    // Golden Aura Glow around batsman if Golden Bat is active
    if (isGoldenBat) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x66FFD700), Color(0x22FFB300), Color.Transparent),
                center = Offset(x, y + 6f * s),
                radius = 50f * s
            ),
            radius = 50f * s,
            center = Offset(x, y + 6f * s)
        )
    }

    // Ground Shadow beneath batsman
    val shadowWidth = 36f * s
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x55000000), Color.Transparent),
            center = Offset(x, y + 26f * s),
            radius = shadowWidth * 0.5f
        ),
        topLeft = Offset(x - shadowWidth * 0.5f, y + 21f * s),
        size = Size(shadowWidth, 10f * s)
    )

    // Stance base reference (Ready pose)
    val stanceLeanX = 0f
    val stanceLeanY = 0f
    val stanceHands = Offset(x + 3f * s, y - 1f * s)
    val stanceBatAngle = 18f

    // Target pose parameters
    val targetLeanX: Float
    val targetLeanY: Float
    val targetHands: Offset
    val targetBatAngle: Float

    when (pose) {
        BatsmanPose.STANCE -> {
            targetLeanX = 0f
            targetLeanY = 0f
            targetHands = stanceHands
            targetBatAngle = stanceBatAngle
        }
        BatsmanPose.BACKLIFT -> {
            targetLeanX = 2f * s
            targetLeanY = -1f * s
            targetHands = Offset(x + 8f * s, y - 8f * s)
            targetBatAngle = -35f
        }
        BatsmanPose.SWING_OFF -> {
            targetLeanX = -8f * s
            targetLeanY = 1f * s
            targetHands = Offset(x - 12f * s, y - 6f * s)
            targetBatAngle = 50f + swingAngle * 0.5f
        }
        BatsmanPose.SWING_STRAIGHT -> {
            targetLeanX = 0f
            targetLeanY = 3f * s
            targetHands = Offset(x, y + 2f * s)
            targetBatAngle = 180f + swingAngle * 0.4f
        }
        BatsmanPose.SWING_LEG -> {
            targetLeanX = 8f * s
            targetLeanY = 1f * s
            targetHands = Offset(x + 12f * s, y - 6f * s)
            targetBatAngle = -50f - swingAngle * 0.5f
        }
        BatsmanPose.DEFENSIVE -> {
            targetLeanX = -1f * s
            targetLeanY = 2f * s
            targetHands = Offset(x - 1f * s, y + 4f * s)
            targetBatAngle = 8f
        }
        BatsmanPose.BOWLED -> {
            targetLeanX = 3f * s
            targetLeanY = 4f * s
            targetHands = Offset(x - 3f * s, y + 10f * s)
            targetBatAngle = 55f
        }
        BatsmanPose.CELEBRATION -> {
            targetLeanX = 0f
            targetLeanY = -6f * s
            targetHands = Offset(x + 4f * s, y - 24f * s)
            targetBatAngle = -110f
        }
    }

    // Linear interpolation between stance (ready) and active action pose
    val t = if (pose == BatsmanPose.STANCE) 1f else poseProgress.coerceIn(0f, 1f)
    val leanX = stanceLeanX + (targetLeanX - stanceLeanX) * t
    val leanY = stanceLeanY + (targetLeanY - stanceLeanY) * t
    val hands = Offset(
        stanceHands.x + (targetHands.x - stanceHands.x) * t,
        stanceHands.y + (targetHands.y - stanceHands.y) * t
    )
    val finalBatAngle = stanceBatAngle + (targetBatAngle - stanceBatAngle) * t

    // 1. Batting Pads on Both Legs (Front View with Knee Rolls & Ribs)
    val hipY = y + 4f * s + leanY * 0.5f
    val leftKnee = Offset(x - 6f * s + leanX * 0.3f, y + 14f * s)
    val leftFoot = Offset(x - 7f * s, y + 26f * s)
    val rightKnee = Offset(x + 6f * s + leanX * 0.3f, y + 14f * s)
    val rightFoot = Offset(x + 7f * s, y + 26f * s)

    // White Padded Leg Guards (Thighs + Shins)
    drawLine(color = padColor, start = Offset(x - 5f * s, hipY), end = leftKnee, strokeWidth = 7f * s, cap = StrokeCap.Round)
    drawLine(color = padColor, start = Offset(x + 5f * s, hipY), end = rightKnee, strokeWidth = 7f * s, cap = StrokeCap.Round)
    drawLine(color = padColor, start = leftKnee, end = leftFoot, strokeWidth = 8.5f * s, cap = StrokeCap.Round)
    drawLine(color = padColor, start = rightKnee, end = rightFoot, strokeWidth = 8.5f * s, cap = StrokeCap.Round)

    // Horizontal Knee Rolls (Classic cricket pad feature)
    drawLine(color = padRibColor, start = Offset(leftKnee.x - 4f * s, leftKnee.y), end = Offset(leftKnee.x + 4f * s, leftKnee.y), strokeWidth = 2.2f * s, cap = StrokeCap.Round)
    drawLine(color = padRibColor, start = Offset(rightKnee.x - 4f * s, rightKnee.y), end = Offset(rightKnee.x + 4f * s, rightKnee.y), strokeWidth = 2.2f * s, cap = StrokeCap.Round)

    // Vertical Shin Ribs
    drawLine(color = padRibColor, start = Offset(leftKnee.x, leftKnee.y + 2f * s), end = Offset(leftFoot.x, leftFoot.y - 2f * s), strokeWidth = 1.4f * s)
    drawLine(color = padRibColor, start = Offset(rightKnee.x, rightKnee.y + 2f * s), end = Offset(rightFoot.x, rightFoot.y - 2f * s), strokeWidth = 1.4f * s)

    // Spiked Cricket Shoes pointing towards bowler
    drawLine(color = Color(0xFF0F172A), start = leftFoot, end = Offset(leftFoot.x - 2f * s, leftFoot.y + 3f * s), strokeWidth = 3.6f * s, cap = StrokeCap.Round)
    drawLine(color = Color(0xFF0F172A), start = rightFoot, end = Offset(rightFoot.x + 2f * s, rightFoot.y + 3f * s), strokeWidth = 3.6f * s, cap = StrokeCap.Round)

    // 2. Torso (Front of Team Jersey)
    val neck = Offset(x + leanX, y - 18f * s + leanY)
    val hips = Offset(x + leanX * 0.5f, hipY)
    drawLine(
        brush = Brush.verticalGradient(
            colors = listOf(jerseyPrimary, jerseyPrimary.copy(alpha = 0.92f)),
            startY = neck.y,
            endY = hips.y
        ),
        start = neck,
        end = hips,
        strokeWidth = 9f * s,
        cap = StrokeCap.Round
    )

    // Collar / V-neck detail
    drawLine(
        color = Color.White,
        start = Offset(neck.x - 4f * s, neck.y),
        end = Offset(neck.x, neck.y + 4f * s),
        strokeWidth = 1.6f * s
    )
    drawLine(
        color = Color.White,
        start = Offset(neck.x + 4f * s, neck.y),
        end = Offset(neck.x, neck.y + 4f * s),
        strokeWidth = 1.6f * s
    )

    // Team Crest / Sponsor Badge on Chest
    drawCircle(
        color = jerseyAccent,
        radius = 2.4f * s,
        center = Offset(neck.x - 2.5f * s, neck.y + 7f * s)
    )

    // 3. Head & Cricket Helmet Facing Front
    val headRadius = 11f * s
    val headCenter = Offset(x + leanX, y - 30f * s + leanY)

    // Face / Skin
    drawCircle(color = skinColor, radius = headRadius * 0.92f, center = headCenter)

    // Helmet Dome
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                helmetPrimary.copy(alpha = 0.95f),
                helmetPrimary,
                Color(0xFF0F172A)
            ),
            center = Offset(headCenter.x, headCenter.y - 3f * s),
            radius = headRadius * 1.15f
        ),
        radius = headRadius,
        center = headCenter
    )

    // Helmet Peak / Visor pointing towards bowler
    drawLine(
        color = helmetPrimary,
        start = Offset(headCenter.x - headRadius * 0.9f, headCenter.y + 3f * s),
        end = Offset(headCenter.x + headRadius * 0.9f, headCenter.y + 3f * s),
        strokeWidth = 2.8f * s,
        cap = StrokeCap.Round
    )

    // Metallic Face Grill (Silver bars over face)
    val grillColor = Color(0xFFCBD5E1)
    drawLine(color = grillColor, start = Offset(headCenter.x - 6f * s, headCenter.y + 5f * s), end = Offset(headCenter.x + 6f * s, headCenter.y + 5f * s), strokeWidth = 1.2f * s)
    drawLine(color = grillColor, start = Offset(headCenter.x - 5f * s, headCenter.y + 8f * s), end = Offset(headCenter.x + 5f * s, headCenter.y + 8f * s), strokeWidth = 1.2f * s)
    drawLine(color = grillColor, start = Offset(headCenter.x, headCenter.y + 3f * s), end = Offset(headCenter.x, headCenter.y + 9f * s), strokeWidth = 1.2f * s)

    // 4. Arms, Gloves & English Willow Bat
    val shoulderLeft = Offset(x - 5f * s + leanX, y - 16f * s + leanY)
    val shoulderRight = Offset(x + 5f * s + leanX, y - 16f * s + leanY)

    // Arms in jersey sleeves
    drawLine(color = jerseyPrimary, start = shoulderLeft, end = hands, strokeWidth = 4.8f * s, cap = StrokeCap.Round)
    drawLine(color = jerseyPrimary, start = shoulderRight, end = hands, strokeWidth = 4.8f * s, cap = StrokeCap.Round)

    // Batting Gloves
    drawCircle(color = gloveColor, radius = 4.5f * s, center = hands)
    drawCircle(color = Color(0xFF94A3B8), radius = 2f * s, center = Offset(hands.x + 1f * s, hands.y - 0.5f * s))

    // English Willow Bat
    rotate(degrees = finalBatAngle, pivot = hands) {
        val handleEnd = Offset(hands.x, hands.y - 12f * s)
        val bladeStart = hands
        val bladeEnd = Offset(hands.x, hands.y + 32f * s)

        // Golden power aura along blade if Golden Bat is active
        if (isGoldenBat) {
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFD700), Color(0xFFFF9100), Color(0xFFFFD700)),
                    startY = bladeStart.y,
                    endY = bladeEnd.y
                ),
                start = bladeStart,
                end = bladeEnd,
                strokeWidth = 14f * s,
                cap = StrokeCap.Round
            )
        }

        // Cane Handle
        drawLine(
            color = batGripColor,
            start = handleEnd,
            end = bladeStart,
            strokeWidth = 3.2f * s,
            cap = StrokeCap.Round
        )

        // Willow Blade
        drawLine(
            color = batBladeColor,
            start = bladeStart,
            end = bladeEnd,
            strokeWidth = 8.5f * s,
            cap = StrokeCap.Square
        )

        // Brand Sticker
        drawLine(
            color = batStickerColor,
            start = Offset(hands.x, bladeStart.y + 8f * s),
            end = Offset(hands.x, bladeStart.y + 20f * s),
            strokeWidth = 6f * s
        )

        // Spine highlight
        drawLine(
            color = Color.White.copy(alpha = 0.40f),
            start = bladeStart,
            end = bladeEnd,
            strokeWidth = 1.3f * s
        )
    }
}

/**
 * 7. HITTING SWEET SPOT ZONE
 * Holographic golden perspective oval right on the pitch in front of the batsman.
 */
private fun DrawScope.drawHittingZone(
    centerX: Float,
    sweetSpotY: Float,
    zoneWidth: Float,
    pulse: Float,
    u: Float,
    isBallApproaching: Boolean
) {
    val dynamicWidth = zoneWidth * (if (isBallApproaching) pulse * 1.05f else pulse)
    val dynamicHeight = 22f * u * pulse
    val primaryGlow = if (isBallApproaching) Color(0xFFFFD700) else Color(0xAA38BDF8)
    val alphaMultiplier = if (isBallApproaching) 0.70f else 0.35f

    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                primaryGlow.copy(alpha = alphaMultiplier),
                primaryGlow.copy(alpha = alphaMultiplier * 0.35f),
                Color.Transparent
            ),
            center = Offset(centerX, sweetSpotY),
            radius = dynamicWidth * 0.55f
        ),
        topLeft = Offset(centerX - dynamicWidth / 2f, sweetSpotY - dynamicHeight / 2f),
        size = Size(dynamicWidth, dynamicHeight)
    )

    // Alignment guide line across pitch
    drawLine(
        color = primaryGlow.copy(alpha = 0.85f),
        start = Offset(centerX - dynamicWidth * 0.38f, sweetSpotY),
        end = Offset(centerX + dynamicWidth * 0.38f, sweetSpotY),
        strokeWidth = if (isBallApproaching) 2.2f * u else 1.5f * u
    )

    // Cross tick
    drawLine(
        color = primaryGlow,
        start = Offset(centerX, sweetSpotY - 4f * u),
        end = Offset(centerX, sweetSpotY + 4f * u),
        strokeWidth = 2f * u
    )
}

/**
 * 9. BOWLER IN FOREGROUND (Camera Behind Bowler)
 * Seen from behind, bowling forwards down the pitch towards the batsman in distance.
 * Foreground perspective (scale s = u * 0.95f).
 * Details:
 * - Cap & head seen from behind
 * - Broad athletic back of team jersey with player number
 * - Athletic cricket trousers and spikes
 * - Windmill bowling action rotating from behind forward towards the batsman!
 */
private fun DrawScope.drawBowlerFromBehind(
    centerX: Float,
    baseY: Float,
    phase: BowlerPhase,
    runUpProgress: Float,
    u: Float
) {
    val s = u * 0.95f
    val stickColor = Color(0xFF0F172A)
    val jerseyColor = Color(0xFF0284C7)
    val trouserColor = Color(0xFFE2E8F0)
    val capColor = Color(0xFF0369A1)

    // Run-up moves bowler forward from foreground towards bowling crease (upwards on screen)
    val runUpApproachY = when (phase) {
        BowlerPhase.READY -> 0f
        BowlerPhase.RUN_UP -> -runUpProgress * 16f * s
        BowlerPhase.DELIVERY_STRIDE -> -16f * s
        BowlerPhase.RELEASE, BowlerPhase.FOLLOW_THROUGH -> -16f * s
    }

    // Follow-through steps to off-side (right) to keep line of sight completely clear
    val followThroughX = if (phase == BowlerPhase.FOLLOW_THROUGH) 14f * s else 0f
    val bowlerX = centerX + followThroughX
    val bowlerY = baseY + runUpApproachY

    // Ground Shadow beneath bowler
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x55000000), Color.Transparent),
            center = Offset(bowlerX, bowlerY),
            radius = 24f * s
        ),
        topLeft = Offset(bowlerX - 22f * s, bowlerY - 6f * s),
        size = Size(44f * s, 12f * s)
    )

    // 1. Head & Cricket Cap Seen From Behind
    val headRadius = 14f * s
    val headCenter = Offset(bowlerX, bowlerY - 74f * s)

    // Hair / Back of Head
    drawCircle(color = stickColor, radius = headRadius, center = headCenter)

    // Cap Dome from Behind
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(capColor.copy(alpha = 0.95f), capColor, Color(0xFF0F172A)),
            center = Offset(headCenter.x, headCenter.y - 4f * s),
            radius = headRadius * 1.15f
        ),
        radius = headRadius,
        center = headCenter
    )

    // Cap Size Adjuster / Arch Gap at Back
    drawLine(
        color = Color(0xFF0F172A),
        start = Offset(headCenter.x - 7f * s, headCenter.y + 5f * s),
        end = Offset(headCenter.x + 7f * s, headCenter.y + 5f * s),
        strokeWidth = 2.8f * s,
        cap = StrokeCap.Round
    )

    // 2. Torso (Broad Back of Team Jersey)
    val neck = Offset(bowlerX, bowlerY - 60f * s)
    val hips = Offset(bowlerX, bowlerY - 26f * s)
    drawLine(
        brush = Brush.verticalGradient(
            colors = listOf(jerseyColor, jerseyColor.copy(alpha = 0.90f)),
            startY = neck.y,
            endY = hips.y
        ),
        start = neck,
        end = hips,
        strokeWidth = 14f * s,
        cap = StrokeCap.Round
    )

    // Back Jersey Stripe
    drawLine(
        color = Color.White.copy(alpha = 0.70f),
        start = Offset(neck.x, neck.y + 6f * s),
        end = Offset(hips.x, hips.y - 6f * s),
        strokeWidth = 3f * s,
        cap = StrokeCap.Round
    )

    // 3. Legs & Trousers
    val legStride = if (phase == BowlerPhase.RUN_UP) {
        sin(runUpProgress * PI.toFloat() * 6f) * 14f * s
    } else if (phase == BowlerPhase.DELIVERY_STRIDE) {
        -10f * s
    } else {
        3f * s
    }

    val leftFoot = Offset(bowlerX - 8f * s - legStride, bowlerY)
    val rightFoot = Offset(bowlerX + 8f * s + legStride, bowlerY)

    drawLine(color = trouserColor, start = hips, end = leftFoot, strokeWidth = 7.5f * s, cap = StrokeCap.Round)
    drawLine(color = trouserColor, start = hips, end = rightFoot, strokeWidth = 7.5f * s, cap = StrokeCap.Round)

    // Spiked Cricket Shoes
    drawLine(color = Color(0xFF0F172A), start = leftFoot, end = Offset(leftFoot.x - 4f * s, leftFoot.y + 2f * s), strokeWidth = 4.2f * s, cap = StrokeCap.Round)
    drawLine(color = Color(0xFF0F172A), start = rightFoot, end = Offset(rightFoot.x + 4f * s, rightFoot.y + 2f * s), strokeWidth = 4.2f * s, cap = StrokeCap.Round)

    // 4. Arms & Windmill Bowling Action (from Behind, bowling forwards towards Batsman)
    val shoulder = Offset(bowlerX, bowlerY - 56f * s)
    val bowlingHand: Offset
    val nonBowlingHand: Offset

    when (phase) {
        BowlerPhase.READY -> {
            bowlingHand = Offset(bowlerX + 12f * s, bowlerY - 42f * s)
            nonBowlingHand = Offset(bowlerX - 12f * s, bowlerY - 42f * s)
        }
        BowlerPhase.RUN_UP -> {
            val armSwing = sin(runUpProgress * PI.toFloat() * 6f) * 16f * s
            bowlingHand = Offset(bowlerX + 14f * s, bowlerY - 42f * s + armSwing)
            nonBowlingHand = Offset(bowlerX - 14f * s, bowlerY - 42f * s - armSwing)
        }
        BowlerPhase.DELIVERY_STRIDE -> {
            // High gather behind head & non-bowling arm pointing forward to batsman
            bowlingHand = Offset(bowlerX + 10f * s, bowlerY - 92f * s)
            nonBowlingHand = Offset(bowlerX - 14f * s, bowlerY - 48f * s)
        }
        BowlerPhase.RELEASE -> {
            // Windmill release straight forward and down towards batsman
            bowlingHand = Offset(bowlerX + 4f * s, bowlerY - 88f * s)
            nonBowlingHand = Offset(bowlerX - 14f * s, bowlerY - 34f * s)
        }
        BowlerPhase.FOLLOW_THROUGH -> {
            // Follow-through across left hip
            bowlingHand = Offset(bowlerX - 16f * s, bowlerY - 24f * s)
            nonBowlingHand = Offset(bowlerX + 10f * s, bowlerY - 28f * s)
        }
    }

    drawLine(color = jerseyColor, start = shoulder, end = nonBowlingHand, strokeWidth = 6.2f * s, cap = StrokeCap.Round)
    drawLine(color = jerseyColor, start = shoulder, end = bowlingHand, strokeWidth = 7f * s, cap = StrokeCap.Round)

    // Ball in hand before release
    if (phase == BowlerPhase.READY || phase == BowlerPhase.RUN_UP || phase == BowlerPhase.DELIVERY_STRIDE) {
        drawCircle(color = Color(0xFFD32F2F), radius = 6.5f * s, center = bowlingHand)
    }
}

/**
 * 10. CRICKET BALL IN FLIGHT (From Bowler in foreground up to Batsman in distance)
 * Starts at bowler release (near end, progress=0f), descends and bounces on good length,
 * then rises to bat height right at the hitting zone (progress=0.85f).
 * Ball recedes with perspective into distance (starts large ~13.5f*u, reaches batsman ~7.0f*u)!
 */
private fun DrawScope.drawBallInFlight(
    centerX: Float,
    bowlerReleaseX: Float,
    pitchTopY: Float,
    pitchBottomY: Float,
    progress: Float,
    sideDrift: Float,
    delivery: DeliveryType,
    ballCosmetic: CosmeticItem,
    u: Float
) {
    val clampedProgress = progress.coerceIn(0f, 1.15f)

    // Interpolate Ground X & Y from Bowler (near end at bottom) up to Batsman (far end at top)
    val ballGroundX = bowlerReleaseX + (centerX - bowlerReleaseX) * clampedProgress + sideDrift * clampedProgress
    val ballGroundY = pitchBottomY + (pitchTopY - pitchBottomY) * clampedProgress

    // Parabolic Bounce Trajectory
    val bouncePoint = delivery.pitchBouncePoint
    val bounceHeightScale = delivery.bounceHeightScale

    val ballAltitude: Float = if (clampedProgress < bouncePoint) {
        // Descending from bowler's high delivery release point down to the pitch surface
        val p = clampedProgress / bouncePoint
        (1f - p) * 36f * u
    } else {
        // Rising arc from pitch surface towards batsman's waist/bat
        val p = ((clampedProgress - bouncePoint) / (1f - bouncePoint)).coerceIn(0f, 1.2f)
        sin(p * PI.toFloat()) * 26f * u * bounceHeightScale
    }

    val ballActualY = ballGroundY - ballAltitude

    // Ball Radius recedes with perspective: starts large near bowler (~13.5f*u) and scales down to batsman (~7.0f*u)
    val ballRadius = (13.5f - clampedProgress * 6.5f) * u

    // 0. Pitch Landing Target Reticle (Helps batsman gauge delivery length and timing)
    if (clampedProgress < bouncePoint) {
        val swingAtBounce = if (delivery.swingCurve != 0f) sin(bouncePoint * PI.toFloat()) * delivery.swingCurve * 40f * bouncePoint else 0f
        val bounceX = bowlerReleaseX + (centerX - bowlerReleaseX) * bouncePoint + swingAtBounce
        val bounceY = pitchBottomY + (pitchTopY - pitchBottomY) * bouncePoint
        val landingAlpha = (0.30f + (clampedProgress / bouncePoint) * 0.45f).coerceIn(0f, 0.75f)
        val markerRadius = (9f + (1f - (clampedProgress / bouncePoint)) * 7f) * u
        drawCircle(
            color = Color(0xFFFFD700).copy(alpha = landingAlpha),
            radius = markerRadius,
            center = Offset(bounceX, bounceY),
            style = Stroke(width = 1.8f * u)
        )
        drawCircle(
            color = Color(0xFFFFD700).copy(alpha = landingAlpha * 0.35f),
            radius = markerRadius * 0.45f,
            center = Offset(bounceX, bounceY)
        )
    }

    // 1. Dynamic Pitch Shadow directly beneath ball on the turf
    val shadowWidth = ballRadius * 2.0f * (1f - (ballAltitude / (80f * u)).coerceIn(0f, 0.40f))
    val shadowHeight = shadowWidth * 0.40f
    val shadowAlpha = (0.50f - (ballAltitude / (90f * u)).coerceIn(0f, 0.25f))
    drawOval(
        color = Color(0xFF000000).copy(alpha = shadowAlpha),
        topLeft = Offset(ballGroundX - shadowWidth / 2f, ballGroundY - shadowHeight / 2f),
        size = Size(shadowWidth, shadowHeight)
    )

    // 2. Pitch Bounce Dust Ring (expands right as ball hits clay)
    if (clampedProgress >= bouncePoint && clampedProgress <= bouncePoint + 0.14f) {
        val ringP = (clampedProgress - bouncePoint) / 0.14f
        val ringRadius = 18f * u * ringP
        drawCircle(
            color = Color(0x70D7CCC8).copy(alpha = 1f - ringP),
            radius = ringRadius,
            center = Offset(ballGroundX, ballGroundY),
            style = Stroke(width = 2.2f * u)
        )
    }

    // 3. Fast Delivery Speed Trails
    if (delivery == DeliveryType.FAST || delivery == DeliveryType.YORKER) {
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.5f)),
                startY = ballActualY + ballRadius * 2.2f,
                endY = ballActualY
            ),
            start = Offset(ballGroundX, ballActualY + ballRadius * 2.2f),
            end = Offset(ballGroundX, ballActualY),
            strokeWidth = ballRadius * 0.8f,
            cap = StrokeCap.Round
        )
    }

    // 4. 3D Spherical Cricket Ball
    val ballColor = Color(ballCosmetic.primaryColorHex)
    val seamColor = Color(ballCosmetic.secondaryColorHex)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.75f),
                ballColor,
                ballColor.copy(alpha = 0.95f),
                Color(0xFF0F172A)
            ),
            center = Offset(ballGroundX - ballRadius * 0.35f, ballActualY - ballRadius * 0.35f),
            radius = ballRadius * 1.30f
        ),
        radius = ballRadius,
        center = Offset(ballGroundX, ballActualY)
    )

    // Raised Leather Seam
    drawLine(
        color = seamColor,
        start = Offset(ballGroundX - ballRadius * 0.70f, ballActualY - ballRadius * 0.28f),
        end = Offset(ballGroundX + ballRadius * 0.70f, ballActualY - ballRadius * 0.28f),
        strokeWidth = if (ballRadius > 9f * u) 2.2f * u else 1.4f * u,
        cap = StrokeCap.Round
    )

    // Seam Stitches
    if (ballRadius > 8f * u) {
        val stitchCount = 4
        for (i in 0 until stitchCount) {
            val sx = ballGroundX - ballRadius * 0.45f + i * (ballRadius * 0.30f)
            val sy = ballActualY - ballRadius * 0.28f
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(sx, sy - 2f * u),
                end = Offset(sx, sy + 2f * u),
                strokeWidth = 1.1f * u
            )
        }
    }
}

/**
 * 11. BALL EXIT TRAJECTORY (AFTER SHOT CONTACT)
 * SIX: Ball launches high in the air down towards camera / over bowler into foreground!
 * FOUR: Screams across the pitch/outfield to foreground boundary ropes.
 */
private fun DrawScope.drawHitBallTrajectory(
    centerX: Float,
    batsmanBaseX: Float,
    sweetSpotY: Float,
    outcome: OutcomeType,
    direction: ShotDirection,
    exitProgress: Float,
    ballCosmetic: CosmeticItem,
    u: Float
) {
    if (exitProgress <= 0.01f || outcome == OutcomeType.WICKET) return

    val startX = batsmanBaseX
    val startY = sweetSpotY

    val currentX: Float
    val currentY: Float
    val ballRadius: Float

    if (outcome == OutcomeType.SIX) {
        // High parabolic launch DOWN the screen towards and over the camera into the foreground!
        val targetX = when (direction) {
            ShotDirection.LEFT -> centerX - size.width * 0.35f
            ShotDirection.LOFT -> centerX
            ShotDirection.RIGHT -> centerX + size.width * 0.35f
        }
        val targetY = size.height + 40f * u

        currentX = startX + (targetX - startX) * exitProgress
        val arcRise = sin(exitProgress * PI.toFloat()) * 110f * u
        currentY = startY + (targetY - startY) * exitProgress - arcRise

        // Scale: expands as it hurtles towards the camera
        ballRadius = (7f + exitProgress * 17f) * u

        // Glowing golden fire trail
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFD700).copy(alpha = 0.2f), Color(0xFFFFD700)),
                startY = startY,
                endY = currentY
            ),
            start = Offset(startX, startY),
            end = Offset(currentX, currentY),
            strokeWidth = 5f * u,
            cap = StrokeCap.Round
        )
    } else {
        // Flat boundary bullet past the bowler towards foreground boundary ropes
        val targetX = when (direction) {
            ShotDirection.LEFT -> 0f
            ShotDirection.LOFT -> centerX
            ShotDirection.RIGHT -> size.width
        }
        val targetY = when (direction) {
            ShotDirection.LEFT, ShotDirection.RIGHT -> size.height * 0.85f
            ShotDirection.LOFT -> size.height * 0.95f
        }

        currentX = startX + (targetX - startX) * exitProgress
        currentY = startY + (targetY - startY) * exitProgress
        ballRadius = (7f + exitProgress * 4f) * u

        // Cyan speed streak
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.2f), Color(0xFF00E5FF)),
                start = Offset(startX, startY),
                end = Offset(currentX, currentY)
            ),
            start = Offset(startX, startY),
            end = Offset(currentX, currentY),
            strokeWidth = 3.5f * u,
            cap = StrokeCap.Round
        )
    }

    val ballColor = Color(ballCosmetic.primaryColorHex)
    drawCircle(
        color = ballColor,
        radius = ballRadius,
        center = Offset(currentX, currentY)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.6f),
        radius = ballRadius * 0.45f,
        center = Offset(currentX - ballRadius * 0.25f, currentY - ballRadius * 0.25f)
    )
}

/**
 * 12A. BAT-BALL CONTACT IMPACT ANIMATION
 * Powerful visual feedback when the bat strikes the ball:
 * - Brilliant multi-ring shockwave radiating from the sweet-spot contact point
 * - Radiant starburst & dynamic comic impact sparks
 * - Distinctive electric cyan & gold corona for boundary hits (FOUR / SIX) and PERFECT timing
 * - Procedurally calculated for maximum 60fps performance without heap allocations
 */
private fun DrawScope.drawBatContactImpact(
    x: Float,
    y: Float,
    progress: Float, // 1.0f (contact moment) down to 0.0f (dissolved)
    outcome: OutcomeType?,
    timing: TimingResult?,
    u: Float
) {
    val expansion = 1f - progress // 0f -> 1f
    val alpha = progress.coerceIn(0f, 1f)

    val isBoundary = outcome == OutcomeType.SIX || outcome == OutcomeType.FOUR
    val isPerfect = timing == TimingResult.PERFECT

    // Vibrant colors based on Shot Quality
    val primaryColor = when {
        outcome == OutcomeType.SIX -> Color(0xFFFFD700) // Radiant Gold
        outcome == OutcomeType.FOUR -> Color(0xFF00E5FF) // Electric Cyan
        isPerfect -> Color(0xFF38BDF8) // Sky Blue Electric
        else -> Color(0xFFFFF176) // Bright Warm Yellow
    }

    val coreGlowColor = if (isBoundary || isPerfect) Color(0xFFFFFFFF) else Color(0xFFFFF9C4)

    // 1. Central Core Flash Burst
    val coreRadius = (12f + expansion * 24f) * u * (if (isBoundary) 1.25f else 1.0f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                coreGlowColor.copy(alpha = alpha),
                primaryColor.copy(alpha = alpha * 0.85f),
                primaryColor.copy(alpha = alpha * 0.35f),
                Color.Transparent
            ),
            center = Offset(x, y),
            radius = coreRadius
        ),
        radius = coreRadius,
        center = Offset(x, y)
    )

    // 2. High-energy Kinetic Shockwave Ring
    val shockwaveRadius = (8f + expansion * (if (isBoundary) 48f else 32f)) * u
    drawCircle(
        color = primaryColor.copy(alpha = alpha * 0.9f),
        radius = shockwaveRadius,
        center = Offset(x, y),
        style = Stroke(width = (3.2f * u * alpha).coerceAtLeast(1.2f))
    )

    // Secondary Outer Wave on Boundary Hits
    if (isBoundary) {
        val outerWaveRadius = (14f + expansion * 62f) * u
        drawCircle(
            color = Color.White.copy(alpha = alpha * 0.6f),
            radius = outerWaveRadius,
            center = Offset(x, y),
            style = Stroke(width = (1.8f * u * alpha).coerceAtLeast(1.0f))
        )
    }

    // 3. Dynamic Starburst Radiant Kinetic Lines
    val sparkCount = if (isBoundary) 16 else 10
    val maxSpokeLength = (if (isBoundary) 42f else 28f) * u
    for (i in 0 until sparkCount) {
        val angle = i * (2f * PI.toFloat() / sparkCount) + (i * 0.12f)
        val speedMultiplier = 0.75f + ((i * 17 % 10) / 20f)
        val startDist = (6f + expansion * 12f) * u
        val endDist = startDist + (maxSpokeLength * speedMultiplier * progress)

        val sx = x + cos(angle) * startDist
        val sy = y + sin(angle) * startDist
        val ex = x + cos(angle) * endDist
        val ey = y + sin(angle) * endDist

        val sparkColor = if (i % 2 == 0) Color.White.copy(alpha = alpha) else primaryColor.copy(alpha = alpha * 0.9f)
        drawLine(
            color = sparkColor,
            start = Offset(sx, sy),
            end = Offset(ex, ey),
            strokeWidth = (if (i % 4 == 0) 3.0f else 1.8f) * u * alpha,
            cap = StrokeCap.Round
        )
    }

    // 4. Four-Point Diamond Flash Star at Epicenter
    val diamondSize = (10f + expansion * 18f) * u * progress
    drawLine(
        color = Color.White.copy(alpha = alpha),
        start = Offset(x - diamondSize, y),
        end = Offset(x + diamondSize, y),
        strokeWidth = 2.4f * u * alpha,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color.White.copy(alpha = alpha),
        start = Offset(x, y - diamondSize),
        end = Offset(x, y + diamondSize),
        strokeWidth = 2.4f * u * alpha,
        cap = StrokeCap.Round
    )
}

/**
 * 12B. WICKET-HIT IMPACT ANIMATION
 * Fiery crimson-orange shockwave, shattered wood splinters, and explosive sparks
 * centered right at the batsman's stumps when bowled / caught!
 */
private fun DrawScope.drawWicketHitImpact(
    centerX: Float,
    stumpTopY: Float,
    progress: Float, // 1.0f (fresh impact) -> 0.0f (dissolved)
    u: Float
) {
    val expansion = 1f - progress // 0f -> 1f
    val alpha = progress.coerceIn(0f, 1f)

    // 1. Fiery Core Explosion Flash
    val coreRadius = (16f + expansion * 38f) * u
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = alpha),
                Color(0xFFFF1744).copy(alpha = alpha * 0.95f),
                Color(0xFFFF6D00).copy(alpha = alpha * 0.6f),
                Color.Transparent
            ),
            center = Offset(centerX, stumpTopY),
            radius = coreRadius
        ),
        radius = coreRadius,
        center = Offset(centerX, stumpTopY)
    )

    // 2. High-energy Dual Shockwave Rings expanding outward
    val innerRingRadius = (10f + expansion * 36f) * u
    drawCircle(
        color = Color(0xFFFF1744).copy(alpha = alpha * 0.9f),
        radius = innerRingRadius,
        center = Offset(centerX, stumpTopY),
        style = Stroke(width = (3.5f * u * alpha).coerceAtLeast(1.5f))
    )

    val outerRingRadius = (6f + expansion * 56f) * u
    drawCircle(
        color = Color(0xFFFF9100).copy(alpha = alpha * 0.65f),
        radius = outerRingRadius,
        center = Offset(centerX, stumpTopY),
        style = Stroke(width = (2.2f * u * alpha).coerceAtLeast(1.0f))
    )

    // 3. Explosive Flying Splinters and Sparks (procedural sparks shooting outward & upward)
    val sparkColors = listOf(
        Color(0xFFFF1744),
        Color(0xFFFF5252),
        Color(0xFFFFAB00),
        Color(0xFFFFD700),
        Color(0xFFFFFFFF),
        Color(0xFF8D6E63) // wood splinter
    )

    for (i in 0 until 14) {
        val angleDeg = -160f + i * 24f + (i * 7 % 11)
        val angleRad = angleDeg * (PI.toFloat() / 180f)
        val speed = 24f + (i * 13 % 28)
        val dist = speed * u * expansion
        val sparkX = centerX + cos(angleRad) * dist
        val sparkY = stumpTopY + sin(angleRad) * dist - (expansion * 12f * u) // upward bias
        val sparkLength = (6f + (i * 5 % 9)) * u * progress
        val sparkColor = sparkColors[i % sparkColors.size].copy(alpha = alpha)

        drawLine(
            color = sparkColor,
            start = Offset(sparkX, sparkY),
            end = Offset(sparkX + cos(angleRad) * sparkLength, sparkY + sin(angleRad) * sparkLength),
            strokeWidth = if (i % 3 == 0) 2.6f * u * alpha else 1.8f * u * alpha,
            cap = StrokeCap.Round
        )
    }

    // 4. "WICKET!" Comic Impact Star Burst
    val starPoints = 8
    val innerR = 4f * u * progress
    val outerR = (16f + expansion * 16f) * u
    for (i in 0 until starPoints) {
        val a = i * (2f * PI.toFloat() / starPoints)
        drawLine(
            color = Color.White.copy(alpha = alpha * 0.85f),
            start = Offset(centerX + cos(a) * innerR, stumpTopY + sin(a) * innerR),
            end = Offset(centerX + cos(a) * outerR, stumpTopY + sin(a) * outerR),
            strokeWidth = 2.4f * u * alpha,
            cap = StrokeCap.Round
        )
    }
}

/**
 * 13. BOUNDARY CELEBRATION (Confetti on 4 or 6)
 */
private fun DrawScope.drawBoundaryCelebration(
    width: Float,
    height: Float,
    outcome: OutcomeType,
    progress: Float,
    u: Float
) {
    if (progress <= 0.05f) return

    val confettiColors = listOf(
        Color(0xFFFFD700),
        Color(0xFFFF3D00),
        Color(0xFF00E5FF),
        Color(0xFF76FF03),
        Color(0xFFE040FB),
        Color(0xFFFFFFFF)
    )

    val confettiCount = 36
    val alpha = (1f - progress).coerceIn(0f, 1f)

    for (i in 0 until confettiCount) {
        val color = confettiColors[i % confettiColors.size].copy(alpha = alpha)
        val cx = ((i * 37 + 17) % 100) / 100f * width
        val initialY = height * 0.15f
        val fallDistance = height * 0.70f * progress
        val spreadX = sin((i * 19 + progress * 5f) * PI.toFloat()) * 30f * u
        val cy = initialY + fallDistance + ((i * 29) % 80) * u

        drawRect(
            color = color,
            topLeft = Offset(cx + spreadX, cy),
            size = Size(5f * u, 9f * u)
        )
    }
}
