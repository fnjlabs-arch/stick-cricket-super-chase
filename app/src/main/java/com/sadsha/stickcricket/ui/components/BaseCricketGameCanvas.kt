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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.sadsha.stickcricket.game.BatsmanPose
import com.sadsha.stickcricket.game.BowlerPhase
import com.sadsha.stickcricket.game.GameEngine
import com.sadsha.stickcricket.game.engine.BallPhysicsEngine
import com.sadsha.stickcricket.game.engine.BallPhysicsState
import com.sadsha.stickcricket.game.engine.BaseGameEngine
import com.sadsha.stickcricket.game.engine.CricketEngineRenderState
import com.sadsha.stickcricket.game.engine.ExitTrajectoryState
import com.sadsha.stickcricket.game.engine.OverSequenceState
import com.sadsha.stickcricket.game.engine.StickmanBattingEngine
import com.sadsha.stickcricket.game.engine.StickmanJoints
import com.sadsha.stickcricket.model.CosmeticCategory
import com.sadsha.stickcricket.model.CosmeticsCatalog
import com.sadsha.stickcricket.model.OutcomeType
import com.sadsha.stickcricket.model.ShotDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Base Game Canvas Component implemented with Jetpack Compose Canvas.
 * Directly visualizes:
 * - The 6-ball over sequence HUD.
 * - Ball flight physics and turf aerodynamics.
 * - Dynamic stickman batsman kinematic animations.
 */
@Composable
fun BaseCricketGameCanvas(
    renderState: CricketEngineRenderState,
    modifier: Modifier = Modifier,
    bowlerPhase: BowlerPhase = BowlerPhase.READY,
    bowlerRunUpProgress: Float = 0f,
    batsmanPose: BatsmanPose = BatsmanPose.STANCE,
    batsmanPoseProgress: Float = 0f,
    contactFlashProgress: Float = 0f,
    hitExitProgress: Float = 0f,
    isGoldenBat: Boolean = false,
    onTapShot: ((direction: ShotDirection, xRatio: Float, yRatio: Float) -> Unit)? = null
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

    val battingEngine = remember { StickmanBattingEngine() }
    val physicsEngine = remember { BallPhysicsEngine() }

    val defaultBat = remember { CosmeticsCatalog.getDefault(CosmeticCategory.BAT) }
    val defaultHelmet = remember { CosmeticsCatalog.getDefault(CosmeticCategory.HELMET) }
    val defaultJersey = remember { CosmeticsCatalog.getDefault(CosmeticCategory.JERSEY) }
    val defaultBall = remember { CosmeticsCatalog.getDefault(CosmeticCategory.BALL) }

    val tapModifier = if (onTapShot != null) {
        Modifier.pointerInput(renderState.isBallInFlight) {
            detectTapGestures { offset ->
                if (renderState.isBallInFlight) {
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val xRatio = (offset.x / w).coerceIn(0f, 1f)
                    val yRatio = (offset.y / h).coerceIn(0f, 1f)

                    val direction = when {
                        xRatio < 0.35f -> ShotDirection.LEFT // Off-side
                        xRatio > 0.65f -> ShotDirection.RIGHT // Leg-side
                        else -> ShotDirection.LOFT // Straight / lofted
                    }
                    onTapShot(direction, xRatio, yRatio)
                }
            }
        }
    } else Modifier

    Canvas(modifier = modifier.fillMaxSize().then(tapModifier)) {
        val width = size.width
        val height = size.height
        val centerX = width * 0.5f
        val u = (width / 360f).coerceIn(1.0f, 4.5f)

        val horizonY = height * 0.17f
        val pitchTopY = height * 0.22f
        val pitchBottomY = height * 0.76f
        val pitchTopWidth = width * 0.22f
        val pitchBottomWidth = width * 0.48f

        // 1. Stadium background & outfield
        drawCanvasStadium(width, height, horizonY, u)
        drawCanvasOutfield(width, height, horizonY)

        // 2. Perspective cricket pitch
        drawCanvasPitch(centerX, pitchTopY, pitchBottomY, pitchTopWidth, pitchBottomWidth, u)
        drawCanvasCreases(centerX, pitchTopY, pitchBottomY, pitchTopWidth, pitchBottomWidth, u)

        // 3. Stumps (Far end striker's wickets)
        val isWicket = renderState.lastOutcome?.isWicket == true
        drawCanvasWickets(
            centerX = centerX,
            baseY = pitchTopY,
            stumpHeight = 22f * u,
            stumpWidth = 2.4f * u,
            spread = 11f * u,
            u = u,
            isKnocked = isWicket && !renderState.isBallInFlight
        )

        // 4. Stickman Batsman (Rendered via StickmanBattingEngine)
        val batsmanBaseX = centerX - 6f * u
        val batsmanBaseY = pitchTopY + 12f * u
        val joints = battingEngine.calculateJoints(
            baseX = batsmanBaseX,
            baseY = batsmanBaseY,
            pose = batsmanPose,
            poseProgress = batsmanPoseProgress,
            unitScale = u
        )
        drawStickmanBatsmanRig(
            joints = joints,
            helmetColor = Color(defaultHelmet.primaryColorHex),
            jerseyColor = Color(defaultJersey.primaryColorHex),
            batBladeColor = if (isGoldenBat) Color(0xFFFFD700) else Color(defaultBat.primaryColorHex),
            batGripColor = Color(defaultBat.secondaryColorHex),
            isGoldenBat = isGoldenBat,
            u = u
        )

        // 5. Hitting Sweet Spot Zone
        val sweetSpotY = pitchBottomY + (pitchTopY - pitchBottomY) * GameEngine.HIT_SWEET_SPOT
        val sweetSpotWidth = (pitchBottomWidth + (pitchTopWidth - pitchBottomWidth) * GameEngine.HIT_SWEET_SPOT) * 0.92f
        drawCanvasHittingZone(
            centerX = centerX,
            sweetSpotY = sweetSpotY,
            zoneWidth = sweetSpotWidth,
            pulse = pulseAnim.value,
            u = u,
            isBallApproaching = renderState.isBallInFlight && renderState.ballPhysics.progress in 0.55f..0.95f
        )

        // 6. Near Wickets (Bowler's end)
        drawCanvasWickets(
            centerX = centerX,
            baseY = pitchBottomY + 4f * u,
            stumpHeight = 38f * u,
            stumpWidth = 3.8f * u,
            spread = 18f * u,
            u = u,
            isKnocked = false
        )

        // 7. Bowler in Foreground
        val bowlerBaseX = centerX + 18f * u
        val bowlerBaseY = pitchBottomY + 16f * u
        drawCanvasBowler(
            centerX = bowlerBaseX,
            baseY = bowlerBaseY,
            phase = bowlerPhase,
            runUpProgress = bowlerRunUpProgress,
            u = u
        )

        // 8. Cricket Ball in Flight (Using BallPhysicsEngine)
        if (renderState.isBallInFlight) {
            drawPhysicsBallInFlight(
                centerX = centerX,
                bowlerReleaseX = bowlerBaseX - 3f * u,
                pitchTopY = pitchTopY,
                pitchBottomY = pitchBottomY,
                physics = renderState.ballPhysics,
                ballColor = Color(defaultBall.primaryColorHex),
                u = u
            )
        }

        // 9. Ball Exit Trajectory (Boundaries 4/6, singles, wickets)
        if (!renderState.isBallInFlight && renderState.lastOutcome != null) {
            val exitState = renderState.exitTrajectory ?: physicsEngine.calculateExitTrajectory(
                exitProgress = hitExitProgress,
                outcome = renderState.lastOutcome.outcomeType,
                direction = renderState.lastOutcome.direction,
                originX = batsmanBaseX + 6f * u,
                originY = sweetSpotY,
                canvasWidth = width,
                canvasHeight = height,
                unitScale = u
            )
            drawExitBallTrajectory(exitState, Color(defaultBall.primaryColorHex), u)
        }

        // 10. Bat Contact Impact Flash
        if (contactFlashProgress > 0.01f) {
            drawCanvasContactImpact(
                x = batsmanBaseX + 6f * u,
                y = sweetSpotY,
                progress = contactFlashProgress,
                outcome = renderState.lastOutcome?.outcomeType,
                u = u
            )
        }

        // 11. 6-Ball Over Sequence HUD Bar (Canvas overlay)
        draw6BallOverHud(
            overState = renderState.overState,
            width = width,
            u = u
        )
    }
}

/**
 * 6-Ball Over Tracker HUD rendered directly on the Canvas.
 */
private fun DrawScope.draw6BallOverHud(
    overState: OverSequenceState,
    width: Float,
    u: Float
) {
    val barHeight = 36f * u
    val topY = 12f * u
    val hudWidth = 240f * u
    val startX = (width - hudWidth) * 0.5f

    // HUD background pill
    drawRoundRect(
        color = Color(0xCC0F172A),
        topLeft = Offset(startX, topY),
        size = Size(hudWidth, barHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f * u, 18f * u)
    )
    drawRoundRect(
        color = Color(0x4438BDF8),
        topLeft = Offset(startX, topY),
        size = Size(hudWidth, barHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f * u, 18f * u),
        style = Stroke(width = 1.2f * u)
    )

    // Render 6 ball dots
    val dotSpacing = hudWidth / 7f
    for (i in 1..6) {
        val dotCenterX = startX + dotSpacing * i
        val dotCenterY = topY + barHeight * 0.5f
        val radius = 9.5f * u

        val outcomeForBall = overState.ballOutcomes.getOrNull(i - 1)
        val isCurrentBall = i == overState.currentBallNumber && !overState.isOverFinished

        val (dotColor, dotBorderColor) = when {
            outcomeForBall != null -> {
                when (outcomeForBall.outcomeType) {
                    OutcomeType.SIX -> Color(0xFFFFD700) to Color(0xFFFFA000) // Gold
                    OutcomeType.FOUR -> Color(0xFF10B981) to Color(0xFF059669) // Emerald green
                    OutcomeType.WICKET -> Color(0xFFEF4444) to Color(0xFFDC2626) // Red
                    OutcomeType.DOT -> Color(0xFF64748B) to Color(0xFF475569) // Slate gray
                    else -> Color(0xFF38BDF8) to Color(0xFF0284C7) // Sky blue
                }
            }
            isCurrentBall -> Color(0xFF38BDF8) to Color.White
            else -> Color(0xFF1E293B) to Color(0xFF334155)
        }

        drawCircle(
            color = dotColor,
            radius = radius,
            center = Offset(dotCenterX, dotCenterY)
        )
        drawCircle(
            color = dotBorderColor,
            radius = radius,
            center = Offset(dotCenterX, dotCenterY),
            style = Stroke(width = if (isCurrentBall) 2f * u else 1.2f * u)
        )

        // Text label inside dot
        val label = when {
            outcomeForBall != null -> {
                when (outcomeForBall.outcomeType) {
                    OutcomeType.SIX -> "6"
                    OutcomeType.FOUR -> "4"
                    OutcomeType.WICKET -> "W"
                    OutcomeType.DOT -> "•"
                    else -> outcomeForBall.runs.toString()
                }
            }
            isCurrentBall -> "$i"
            else -> "$i"
        }

        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = if (outcomeForBall?.outcomeType == OutcomeType.SIX) {
                    android.graphics.Color.BLACK
                } else {
                    android.graphics.Color.WHITE
                }
                textSize = 9.5f * u
                isFakeBoldText = true
                textAlign = android.graphics.Paint.Align.CENTER
            }
            drawText(label, dotCenterX, dotCenterY + 3.4f * u, paint)
        }
    }
}

/**
 * Draws the stickman batsman using the calculated StickmanJoints skeletal rig.
 */
private fun DrawScope.drawStickmanBatsmanRig(
    joints: StickmanJoints,
    helmetColor: Color,
    jerseyColor: Color,
    batBladeColor: Color,
    batGripColor: Color,
    isGoldenBat: Boolean,
    u: Float
) {
    val s = u * 0.65f
    val skinColor = Color(0xFF0F172A)
    val padColor = Color(0xFFF8FAFC)
    val padRibColor = Color(0xFFCBD5E1)
    val gloveColor = if (isGoldenBat) Color(0xFFFFECB3) else Color(0xFFFFFFFF)

    // Golden Bat aura glow
    if (isGoldenBat) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x66FFD700), Color(0x22FFB300), Color.Transparent),
                center = Offset(joints.headCenter.x, joints.headCenter.y + 36f * s),
                radius = 50f * s
            ),
            radius = 50f * s,
            center = Offset(joints.headCenter.x, joints.headCenter.y + 36f * s)
        )
    }

    // Ground Shadow beneath batsman
    val shadowWidth = 36f * s
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x55000000), Color.Transparent),
            center = Offset(joints.footLeft.x + 7f * s, joints.footLeft.y),
            radius = shadowWidth * 0.5f
        ),
        topLeft = Offset(joints.footLeft.x - 11f * s, joints.footLeft.y - 5f * s),
        size = Size(shadowWidth, 10f * s)
    )

    // 1. Batting Pads & Legs
    drawLine(color = padColor, start = joints.hipLeft, end = joints.kneeLeft, strokeWidth = 7f * s, cap = StrokeCap.Round)
    drawLine(color = padColor, start = joints.hipRight, end = joints.kneeRight, strokeWidth = 7f * s, cap = StrokeCap.Round)
    drawLine(color = padColor, start = joints.kneeLeft, end = joints.footLeft, strokeWidth = 8.5f * s, cap = StrokeCap.Round)
    drawLine(color = padColor, start = joints.kneeRight, end = joints.footRight, strokeWidth = 8.5f * s, cap = StrokeCap.Round)

    // Knee rolls
    drawLine(color = padRibColor, start = Offset(joints.kneeLeft.x - 4f * s, joints.kneeLeft.y), end = Offset(joints.kneeLeft.x + 4f * s, joints.kneeLeft.y), strokeWidth = 2f * s)
    drawLine(color = padRibColor, start = Offset(joints.kneeRight.x - 4f * s, joints.kneeRight.y), end = Offset(joints.kneeRight.x + 4f * s, joints.kneeRight.y), strokeWidth = 2f * s)

    // Spiked Cricket Shoes
    drawLine(color = skinColor, start = joints.footLeft, end = Offset(joints.footLeft.x - 2f * s, joints.footLeft.y + 3f * s), strokeWidth = 3.6f * s, cap = StrokeCap.Round)
    drawLine(color = skinColor, start = joints.footRight, end = Offset(joints.footRight.x + 2f * s, joints.footRight.y + 3f * s), strokeWidth = 3.6f * s, cap = StrokeCap.Round)

    // 2. Torso (Team Jersey)
    drawLine(
        brush = Brush.verticalGradient(
            colors = listOf(jerseyColor, jerseyColor.copy(alpha = 0.92f)),
            startY = joints.neck.y,
            endY = joints.hips.y
        ),
        start = joints.neck,
        end = joints.hips,
        strokeWidth = 9f * s,
        cap = StrokeCap.Round
    )

    // 3. Head and Helmet
    drawCircle(color = skinColor, radius = joints.headRadius * 0.92f, center = joints.headCenter)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(helmetColor.copy(alpha = 0.95f), helmetColor, Color(0xFF0F172A)),
            center = Offset(joints.headCenter.x, joints.headCenter.y - 3f * s),
            radius = joints.headRadius * 1.15f
        ),
        radius = joints.headRadius,
        center = joints.headCenter
    )
    // Helmet peak & grille
    drawLine(
        color = helmetColor,
        start = Offset(joints.headCenter.x - joints.headRadius * 0.9f, joints.headCenter.y + 3f * s),
        end = Offset(joints.headCenter.x + joints.headRadius * 0.9f, joints.headCenter.y + 3f * s),
        strokeWidth = 2.8f * s,
        cap = StrokeCap.Round
    )
    val grillColor = Color(0xFFCBD5E1)
    drawLine(color = grillColor, start = Offset(joints.headCenter.x - 5f * s, joints.headCenter.y + 5f * s), end = Offset(joints.headCenter.x + 5f * s, joints.headCenter.y + 5f * s), strokeWidth = 1.2f * s)
    drawLine(color = grillColor, start = Offset(joints.headCenter.x - 4f * s, joints.headCenter.y + 8f * s), end = Offset(joints.headCenter.x + 4f * s, joints.headCenter.y + 8f * s), strokeWidth = 1.2f * s)

    // 4. Arms & Batting Gloves
    drawLine(color = jerseyColor, start = joints.shoulderLeft, end = joints.hands, strokeWidth = 4.8f * s, cap = StrokeCap.Round)
    drawLine(color = jerseyColor, start = joints.shoulderRight, end = joints.hands, strokeWidth = 4.8f * s, cap = StrokeCap.Round)
    drawCircle(color = gloveColor, radius = 4.5f * s, center = joints.hands)

    // 5. English Willow Cricket Bat
    rotate(degrees = joints.batAngleDegrees, pivot = joints.hands) {
        val handleEnd = Offset(joints.hands.x, joints.hands.y - 12f * s)
        val bladeStart = joints.hands
        val bladeEnd = Offset(joints.hands.x, joints.hands.y + 32f * s)

        // Cane Handle
        drawLine(color = batGripColor, start = handleEnd, end = bladeStart, strokeWidth = 3.2f * s, cap = StrokeCap.Round)

        // Willow Blade
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(batBladeColor, batBladeColor.copy(alpha = 0.88f)),
                startY = bladeStart.y,
                endY = bladeEnd.y
            ),
            start = bladeStart,
            end = bladeEnd,
            strokeWidth = 8.5f * s,
            cap = StrokeCap.Square
        )
    }
}

/**
 * Draws the cricket ball in flight using 3D coordinates from BallPhysicsEngine.
 */
private fun DrawScope.drawPhysicsBallInFlight(
    centerX: Float,
    bowlerReleaseX: Float,
    pitchTopY: Float,
    pitchBottomY: Float,
    physics: BallPhysicsState,
    ballColor: Color,
    u: Float
) {
    val clampedProgress = physics.progress.coerceIn(0f, 1.15f)
    val ballGroundX = bowlerReleaseX + (centerX - bowlerReleaseX) * clampedProgress + physics.lateral * 40f * clampedProgress
    val ballGroundY = pitchBottomY + (pitchTopY - pitchBottomY) * clampedProgress
    val ballActualY = ballGroundY - physics.altitude

    val ballRadius = 13.5f * u * physics.radiusScale

    // 1. Dynamic Pitch Shadow
    val shadowWidth = ballRadius * 2.2f * physics.shadowScale
    val shadowHeight = shadowWidth * 0.40f
    drawOval(
        color = Color(0xFF000000).copy(alpha = physics.shadowAlpha),
        topLeft = Offset(ballGroundX - shadowWidth / 2f, ballGroundY - shadowHeight / 2f),
        size = Size(shadowWidth, shadowHeight)
    )

    // 2. Pitch Bounce Dust Ring
    if (physics.isPitchContact) {
        val ringRadius = 20f * u * physics.pitchContactProgress
        drawCircle(
            color = Color(0x70D7CCC8).copy(alpha = 1f - physics.pitchContactProgress),
            radius = ringRadius,
            center = Offset(ballGroundX, ballGroundY),
            style = Stroke(width = 2.2f * u)
        )
    }

    // 3. Spherical Ball Body with Radial Lighting
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.90f),
                ballColor,
                Color(0xFF881337),
                Color(0xFF4C0519)
            ),
            center = Offset(ballGroundX - ballRadius * 0.35f, ballActualY - ballRadius * 0.35f),
            radius = ballRadius * 1.3f
        ),
        radius = ballRadius,
        center = Offset(ballGroundX, ballActualY)
    )

    // 4. White Raised Seam rotating in air
    rotate(degrees = physics.seamAngle, pivot = Offset(ballGroundX, ballActualY)) {
        drawLine(
            color = Color.White.copy(alpha = 0.85f),
            start = Offset(ballGroundX, ballActualY - ballRadius * 0.88f),
            end = Offset(ballGroundX, ballActualY + ballRadius * 0.88f),
            strokeWidth = 1.4f * u
        )
    }
}

/**
 * Draws post-contact ball exit trajectory.
 */
private fun DrawScope.drawExitBallTrajectory(
    exit: ExitTrajectoryState,
    ballColor: Color,
    u: Float
) {
    val ballRadius = 8.5f * u * exit.radiusScale

    // Trail effect
    if (exit.trailAlpha > 0.05f) {
        drawCircle(
            color = Color(0x55FFD700).copy(alpha = exit.trailAlpha * 0.4f),
            radius = ballRadius * 1.4f,
            center = Offset(exit.posX, exit.posY)
        )
    }

    // Ground Shadow if near turf
    if (exit.shadowAlpha > 0.05f) {
        drawOval(
            color = Color.Black.copy(alpha = exit.shadowAlpha),
            topLeft = Offset(exit.posX - ballRadius, exit.posY + exit.posZ - ballRadius * 0.3f),
            size = Size(ballRadius * 2f, ballRadius * 0.6f)
        )
    }

    // Ball sphere
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, ballColor, Color(0xFF4C0519)),
            center = Offset(exit.posX - ballRadius * 0.3f, exit.posY - ballRadius * 0.3f),
            radius = ballRadius * 1.2f
        ),
        radius = ballRadius,
        center = Offset(exit.posX, exit.posY)
    )
}

// Visual helpers for stadium, pitch, wickets, bowler, and contact impact
private fun DrawScope.drawCanvasStadium(width: Float, height: Float, horizonY: Float, u: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF070F1E), Color(0xFF0E1C33), Color(0xFF162D4A), Color(0xFF1E3A52)),
            startY = 0f,
            endY = horizonY
        ),
        topLeft = Offset.Zero,
        size = Size(width, horizonY)
    )
}

private fun DrawScope.drawCanvasOutfield(width: Float, height: Float, horizonY: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFF388E3C)),
            startY = horizonY,
            endY = height
        ),
        topLeft = Offset(0f, horizonY),
        size = Size(width, height - horizonY)
    )
}

private fun DrawScope.drawCanvasPitch(
    centerX: Float,
    topY: Float,
    bottomY: Float,
    topWidth: Float,
    bottomWidth: Float,
    u: Float
) {
    val path = Path().apply {
        moveTo(centerX - topWidth * 0.5f, topY)
        lineTo(centerX + topWidth * 0.5f, topY)
        lineTo(centerX + bottomWidth * 0.5f, bottomY)
        lineTo(centerX - bottomWidth * 0.5f, bottomY)
        close()
    }
    drawPath(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFBCAAA4), Color(0xFFD7CCC8), Color(0xFFA1887F)),
            startY = topY,
            endY = bottomY
        ),
        path = path
    )
}

private fun DrawScope.drawCanvasCreases(
    centerX: Float,
    topY: Float,
    bottomY: Float,
    topWidth: Float,
    bottomWidth: Float,
    u: Float
) {
    val creaseColor = Color.White.copy(alpha = 0.85f)
    // Batting crease (at top near batsman)
    drawLine(
        color = creaseColor,
        start = Offset(centerX - topWidth * 0.65f, topY + 8f * u),
        end = Offset(centerX + topWidth * 0.65f, topY + 8f * u),
        strokeWidth = 2.2f * u
    )
    // Bowling crease (at bottom near bowler)
    drawLine(
        color = creaseColor,
        start = Offset(centerX - bottomWidth * 0.55f, bottomY - 6f * u),
        end = Offset(centerX + bottomWidth * 0.55f, bottomY - 6f * u),
        strokeWidth = 2.8f * u
    )
}

private fun DrawScope.drawCanvasWickets(
    centerX: Float,
    baseY: Float,
    stumpHeight: Float,
    stumpWidth: Float,
    spread: Float,
    u: Float,
    isKnocked: Boolean = false
) {
    val stumpColor = Color(0xFFD7CCC8)
    val offsets = listOf(-spread, 0f, spread)

    offsets.forEachIndexed { index, xOffset ->
        val x = centerX + xOffset
        val angle = if (isKnocked) (index - 1) * 35f else 0f
        rotate(degrees = angle, pivot = Offset(x, baseY)) {
            drawLine(
                color = stumpColor,
                start = Offset(x, baseY),
                end = Offset(x, baseY - stumpHeight),
                strokeWidth = stumpWidth,
                cap = StrokeCap.Round
            )
        }
    }

    // Bails if not knocked
    if (!isKnocked) {
        drawLine(
            color = Color(0xFFBCAAA4),
            start = Offset(centerX - spread * 1.2f, baseY - stumpHeight),
            end = Offset(centerX + spread * 1.2f, baseY - stumpHeight),
            strokeWidth = stumpWidth * 0.8f
        )
    }
}

private fun DrawScope.drawCanvasHittingZone(
    centerX: Float,
    sweetSpotY: Float,
    zoneWidth: Float,
    pulse: Float,
    u: Float,
    isBallApproaching: Boolean
) {
    val zoneHeight = 16f * u
    val glowColor = if (isBallApproaching) Color(0xFFFFD700) else Color(0x5538BDF8)

    drawRoundRect(
        color = glowColor.copy(alpha = if (isBallApproaching) 0.35f * pulse else 0.15f),
        topLeft = Offset(centerX - zoneWidth * 0.5f, sweetSpotY - zoneHeight * 0.5f),
        size = Size(zoneWidth, zoneHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * u, 8f * u)
    )
    drawRoundRect(
        color = glowColor.copy(alpha = if (isBallApproaching) 0.85f else 0.40f),
        topLeft = Offset(centerX - zoneWidth * 0.5f, sweetSpotY - zoneHeight * 0.5f),
        size = Size(zoneWidth, zoneHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * u, 8f * u),
        style = Stroke(width = 1.4f * u)
    )
}

private fun DrawScope.drawCanvasBowler(
    centerX: Float,
    baseY: Float,
    phase: BowlerPhase,
    runUpProgress: Float,
    u: Float
) {
    val s = u * 0.85f
    val bodyColor = Color(0xFF0F172A)
    val jerseyColor = Color(0xFF0284C7)

    // Bowler head
    drawCircle(color = bodyColor, radius = 9f * s, center = Offset(centerX, baseY - 50f * s))

    // Bowler torso
    drawLine(color = jerseyColor, start = Offset(centerX, baseY - 42f * s), end = Offset(centerX, baseY - 24f * s), strokeWidth = 8f * s, cap = StrokeCap.Round)

    // Bowling arm
    val armAngle = when (phase) {
        BowlerPhase.READY -> 25f
        BowlerPhase.RUN_UP -> sin(runUpProgress * PI.toFloat() * 6f) * 35f
        BowlerPhase.DELIVERY_STRIDE -> -45f
        BowlerPhase.RELEASE -> -160f
        BowlerPhase.FOLLOW_THROUGH -> 65f
    }
    rotate(degrees = armAngle, pivot = Offset(centerX - 4f * s, baseY - 40f * s)) {
        drawLine(
            color = jerseyColor,
            start = Offset(centerX - 4f * s, baseY - 40f * s),
            end = Offset(centerX - 4f * s, baseY - 20f * s),
            strokeWidth = 4.5f * s,
            cap = StrokeCap.Round
        )
    }

    // Legs
    val strideOffset = if (phase == BowlerPhase.RUN_UP) sin(runUpProgress * PI.toFloat() * 8f) * 12f * s else 0f
    drawLine(color = Color.White, start = Offset(centerX - 3f * s, baseY - 24f * s), end = Offset(centerX - 4f * s + strideOffset, baseY), strokeWidth = 6f * s, cap = StrokeCap.Round)
    drawLine(color = Color.White, start = Offset(centerX + 3f * s, baseY - 24f * s), end = Offset(centerX + 4f * s - strideOffset, baseY), strokeWidth = 6f * s, cap = StrokeCap.Round)
}

private fun DrawScope.drawCanvasContactImpact(
    x: Float,
    y: Float,
    progress: Float,
    outcome: OutcomeType?,
    u: Float
) {
    val alpha = progress.coerceIn(0f, 1f)
    val ringColor = when (outcome) {
        OutcomeType.SIX -> Color(0xFFFFD700)
        OutcomeType.FOUR -> Color(0xFF10B981)
        else -> Color.White
    }
    val radius = (1f - progress) * 28f * u
    drawCircle(
        color = ringColor.copy(alpha = alpha * 0.8f),
        radius = radius,
        center = Offset(x, y),
        style = Stroke(width = 2.4f * u)
    )
}
