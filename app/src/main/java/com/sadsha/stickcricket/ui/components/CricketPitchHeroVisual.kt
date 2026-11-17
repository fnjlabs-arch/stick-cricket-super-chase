package com.sadsha.stickcricket.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dedicated, highly polished visual component showcasing an unmistakable cricket scene:
 * Clay pitch, white popping crease, wooden stumps with illuminated bails,
 * crossed willow bat, and a spinning, glossy red leather cricket ball with stitched seam.
 */
@Composable
fun CricketPitchHeroVisual(
    modifier: Modifier = Modifier,
    isAnimated: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cricket_hero")

    val ballFloatY by if (isAnimated) {
        infiniteTransition.animateFloat(
            initialValue = -6f,
            targetValue = 6f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ballFloat"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    val seamRotation by if (isAnimated) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "seamRotation"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(25f) }
    }

    val lightPulse by if (isAnimated) {
        infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.65f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "lightPulse"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0.5f) }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0 || h <= 0) return@Canvas

        val cx = w * 0.5f
        val groundY = h * 0.72f

        // 1. FLOODLIGHT BEAMS FROM TOP CORNERS
        drawFloodlights(w, h, lightPulse)

        // 2. OUTFIELD TURF WITH GRASS PATTERN
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x22134E3F), Color(0x660F3D30), Color(0xAA0B2E24)),
                startY = groundY - 40f,
                endY = h
            ),
            topLeft = Offset(0f, groundY - 30f),
            size = Size(w, h - (groundY - 30f))
        )

        // 3. PERSPECTIVE CLAY CRICKET PITCH
        val pitchTopW = w * 0.46f
        val pitchBottomW = w * 0.78f
        val pitchTopY = groundY - 40f
        val pitchBottomY = h

        val pitchPath = Path().apply {
            moveTo(cx - pitchTopW * 0.5f, pitchTopY)
            lineTo(cx + pitchTopW * 0.5f, pitchTopY)
            lineTo(cx + pitchBottomW * 0.5f, pitchBottomY)
            lineTo(cx - pitchBottomW * 0.5f, pitchBottomY)
            close()
        }

        drawPath(
            path = pitchPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFB59364), Color(0xFFC7A774), Color(0xFF9E7E52)),
                startY = pitchTopY,
                endY = pitchBottomY
            )
        )

        // White Popping Crease
        val creaseY = groundY + 15f
        val creaseHalfW = w * 0.32f
        drawLine(
            color = Color.White.copy(alpha = 0.9f),
            start = Offset(cx - creaseHalfW, creaseY),
            end = Offset(cx + creaseHalfW, creaseY),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
        )

        // Bowling crease / Return crease lines
        drawLine(
            color = Color.White.copy(alpha = 0.75f),
            start = Offset(cx - creaseHalfW, creaseY - 20f),
            end = Offset(cx - creaseHalfW, creaseY + 25f),
            strokeWidth = 2.5f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.75f),
            start = Offset(cx + creaseHalfW, creaseY - 20f),
            end = Offset(cx + creaseHalfW, creaseY + 25f),
            strokeWidth = 2.5f
        )

        // 4. WOODEN CRICKET BAT LEANING AT THE CREASE
        drawCricketBat(cx = cx - 55f, cy = groundY - 10f, angle = 26f)

        // 5. THREE WOODEN STUMPS WITH BAILS
        drawStumpsAndBails(cx = cx - 50f, baseGroundY = groundY + 12f)

        // 6. ICONIC 3D RED LEATHER CRICKET BALL (Foreground Hero!)
        val ballCenter = Offset(cx + 42f, groundY - 25f + ballFloatY)
        val ballRadius = 38f

        // Ball Shadow on Pitch
        drawOval(
            color = Color(0x88000000),
            topLeft = Offset(ballCenter.x - ballRadius * 0.9f, groundY + 18f),
            size = Size(ballRadius * 1.8f, 14f)
        )

        // Red Leather Body (3D Radial Gradient)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFF4D4D), // Bright highlight
                    Color(0xFFD32F2F), // Cricket Red
                    Color(0xFF8B0000), // Dark Shadow
                    Color(0xFF4A0000)
                ),
                center = Offset(ballCenter.x - ballRadius * 0.3f, ballCenter.y - ballRadius * 0.35f),
                radius = ballRadius * 1.25f
            ),
            radius = ballRadius,
            center = ballCenter
        )

        // Specular Glint (Glossy Leather reflection)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.85f), Color.Transparent),
                center = Offset(ballCenter.x - ballRadius * 0.32f, ballCenter.y - ballRadius * 0.36f),
                radius = ballRadius * 0.35f
            ),
            radius = ballRadius * 0.32f,
            center = Offset(ballCenter.x - ballRadius * 0.32f, ballCenter.y - ballRadius * 0.36f)
        )

        // Realistic White Stitched Seam
        rotate(degrees = seamRotation, pivot = ballCenter) {
            drawBallSeam(center = ballCenter, radius = ballRadius)
        }
    }
}

private fun DrawScope.drawFloodlights(w: Float, h: Float, intensity: Float) {
    // Left Light Cone
    val leftBeamPath = Path().apply {
        moveTo(w * 0.08f, 0f)
        lineTo(w * 0.45f, h)
        lineTo(w * 0.15f, h)
        close()
    }
    drawPath(
        path = leftBeamPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0x88E0F2FE).copy(alpha = intensity * 0.5f),
                Color(0x2238BDF8).copy(alpha = intensity * 0.2f),
                Color.Transparent
            ),
            startY = 0f,
            endY = h
        )
    )

    // Right Light Cone
    val rightBeamPath = Path().apply {
        moveTo(w * 0.92f, 0f)
        lineTo(w * 0.85f, h)
        lineTo(w * 0.55f, h)
        close()
    }
    drawPath(
        path = rightBeamPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0x88E0F2FE).copy(alpha = intensity * 0.5f),
                Color(0x2238BDF8).copy(alpha = intensity * 0.2f),
                Color.Transparent
            ),
            startY = 0f,
            endY = h
        )
    )
}

private fun DrawScope.drawStumpsAndBails(cx: Float, baseGroundY: Float) {
    val stumpW = 4.5f
    val stumpH = 58f
    val spacing = 11f

    val stumpColors = listOf(Color(0xFFF1C88E), Color(0xFFC79854), Color(0xFF8D6228))

    // Shadow under stumps
    drawOval(
        color = Color(0x66000000),
        topLeft = Offset(cx - spacing - 8f, baseGroundY - 4f),
        size = Size(spacing * 2 + 16f, 8f)
    )

    // 3 Stumps (Off, Middle, Leg)
    for (i in -1..1) {
        val sx = cx + i * spacing
        // Wooden Stump shaft
        drawRect(
            brush = Brush.horizontalGradient(
                colors = stumpColors,
                startX = sx - stumpW * 0.5f,
                endX = sx + stumpW * 0.5f
            ),
            topLeft = Offset(sx - stumpW * 0.5f, baseGroundY - stumpH),
            size = Size(stumpW, stumpH)
        )
        // Top Spigot (bail peg)
        drawRect(
            color = Color(0xFFDDB06D),
            topLeft = Offset(sx - 1.5f, baseGroundY - stumpH - 4f),
            size = Size(3f, 4f)
        )
    }

    // 2 Bails resting across the stumps
    val bailY = baseGroundY - stumpH - 3f
    val bailW = spacing + 2f
    val bailH = 3.2f

    // Bail 1 (Off-Middle)
    drawRect(
        color = Color(0xFFFFD54F),
        topLeft = Offset(cx - spacing - 1f, bailY),
        size = Size(bailW, bailH)
    )
    // Bail 2 (Middle-Leg)
    drawRect(
        color = Color(0xFFFFD54F),
        topLeft = Offset(cx - 1f, bailY),
        size = Size(bailW, bailH)
    )

    // Glowing LED bails effect
    drawLine(
        color = Color(0xFFFFE082).copy(alpha = 0.8f),
        start = Offset(cx - spacing - 2f, bailY + 1.5f),
        end = Offset(cx + spacing + 2f, bailY + 1.5f),
        strokeWidth = 1.5f
    )
}

private fun DrawScope.drawCricketBat(cx: Float, cy: Float, angle: Float) {
    rotate(degrees = angle, pivot = Offset(cx, cy)) {
        val bladeW = 10f
        val bladeH = 50f
        val handleW = 4.5f
        val handleH = 26f

        // Willow Blade
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFFE4C690), Color(0xFFD2B075), Color(0xFFA8864C)),
                startX = cx - bladeW * 0.5f,
                endX = cx + bladeW * 0.5f
            ),
            topLeft = Offset(cx - bladeW * 0.5f, cy),
            size = Size(bladeW, bladeH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
        )

        // Brand Sticker stripe
        drawRect(
            color = Color(0xFFFF6D00),
            topLeft = Offset(cx - bladeW * 0.5f, cy + 8f),
            size = Size(bladeW, 8f)
        )

        // Handle with Grip Wrap
        drawRect(
            color = Color(0xFF0284C7), // Neon Blue Rubber Grip
            topLeft = Offset(cx - handleW * 0.5f, cy - handleH),
            size = Size(handleW, handleH)
        )

        // Grip spiral lines
        for (step in 1..4) {
            val gy = cy - handleH + step * 5f
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(cx - handleW * 0.5f, gy),
                end = Offset(cx + handleW * 0.5f, gy + 2f),
                strokeWidth = 1f
            )
        }
    }
}

private fun DrawScope.drawBallSeam(center: Offset, radius: Float) {
    // Primary curved white seam track
    val seamPath = Path().apply {
        moveTo(center.x, center.y - radius + 1f)
        cubicTo(
            center.x + radius * 0.55f, center.y - radius * 0.5f,
            center.x + radius * 0.55f, center.y + radius * 0.5f,
            center.x, center.y + radius - 1f
        )
    }

    // White seam thread
    drawPath(
        path = seamPath,
        color = Color(0xFFF8FAFC),
        style = Stroke(width = 2.4f, cap = StrokeCap.Round)
    )

    // Stitched dashes / ticks across the seam
    val steps = 9
    for (i in 1 until steps) {
        val t = i.toFloat() / steps
        // Approximate point along cubic bezier
        val u = 1f - t
        val y = center.y - radius + 2f * radius * t
        val x = center.x + (radius * 0.55f) * 4f * t * (1f - t)

        val stitchLength = 4f
        drawLine(
            color = Color.White,
            start = Offset(x - stitchLength * 0.5f, y - 1f),
            end = Offset(x + stitchLength * 0.5f, y + 1f),
            strokeWidth = 1.6f,
            cap = StrokeCap.Round
        )
    }
}
