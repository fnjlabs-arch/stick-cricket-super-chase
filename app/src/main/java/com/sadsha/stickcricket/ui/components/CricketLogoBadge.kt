package com.sadsha.stickcricket.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Official 'Stick Cricket Super Chase' Logo Emblem Component.
 * Highly detailed, vibrant cricket crest featuring:
 * - Crossed willow cricket bats with grip
 * - Three wooden stumps & glowing bails
 * - 3D glossy red cricket ball with stitching and sheen
 * - Dynamic athletic gold & fiery orange banners
 * - Shimmering animated stadium sunburst aura
 */
@Composable
fun CricketLogoBadge(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 260.dp,
    showAura: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_aura")

    val auraRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aura_rot"
    )

    val ballPulse by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ball_pulse"
    )

    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        // 1. ROTATING SUNBURST LIGHT RAYS (Aura behind emblem)
        if (showAura) {
            Canvas(
                modifier = Modifier
                    .size(sizeDp * 1.25f)
                    .rotate(auraRotation)
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val rayRadius = size.width * 0.48f
                val rayCount = 12

                for (i in 0 until rayCount) {
                    val angleDeg = i * (360f / rayCount)
                    val angleRad = angleDeg * (PI.toFloat() / 180f)
                    val halfWidth = 14f

                    val p1 = Offset(
                        center.x + halfWidth * cos(angleRad + PI.toFloat() / 2f),
                        center.y + halfWidth * sin(angleRad + PI.toFloat() / 2f)
                    )
                    val p2 = Offset(
                        center.x + rayRadius * cos(angleRad),
                        center.y + rayRadius * sin(angleRad)
                    )
                    val p3 = Offset(
                        center.x - halfWidth * cos(angleRad + PI.toFloat() / 2f),
                        center.y - halfWidth * sin(angleRad + PI.toFloat() / 2f)
                    )

                    val rayPath = Path().apply {
                        moveTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        lineTo(p3.x, p3.y)
                        close()
                    }

                    drawPath(
                        path = rayPath,
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x33FFB300),
                                Color(0x11FF6D00),
                                Color.Transparent
                            ),
                            center = center,
                            radius = rayRadius
                        )
                    )
                }
            }
        }

        // 2. VECTOR CANVAS: CROSSED BATS, WICKETS, AND BALL
        Canvas(
            modifier = Modifier.size(sizeDp)
        ) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * 0.52f

            // Circular Shield Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F2B20),
                        Color(0xFF0A1E17),
                        Color(0xFF040D0A)
                    ),
                    center = Offset(cx, cy),
                    radius = w * 0.42f
                ),
                radius = w * 0.42f,
                center = Offset(cx, cy)
            )

            // Outer Gold Crest Ring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFFFD700),
                        Color(0xFFFF8F00),
                        Color(0xFFFFD700),
                        Color(0xFFFFECB3),
                        Color(0xFFFFD700)
                    ),
                    center = Offset(cx, cy)
                ),
                radius = w * 0.42f,
                center = Offset(cx, cy),
                style = Stroke(width = 5f)
            )

            // Inner Ring Accent
            drawCircle(
                color = Color(0x66FFD700),
                radius = w * 0.39f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5f)
            )

            // Crossed Bats behind wickets
            drawCrossedBats(cx, cy - 8f, w * 0.36f)

            // Stumps & Glowing LED Bails
            drawStumpsBadge(cx, cy + 10f, w * 0.16f)

            // Hero 3D Cricket Ball (Elevated center)
            val ballR = w * 0.145f * ballPulse
            val ballCenter = Offset(cx, cy + 12f)
            drawHeroCricketBall(ballCenter, ballR)
        }

        // 3. EMBEDDED TYPOGRAPHY & SHIELD BANNERS
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Badge Pill: "T20 PREMIER"
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xEE1E293B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700)),
                modifier = Modifier.offset(y = (-2).dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "T20 PREMIER",
                        color = Color(0xFFFFE082),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Massive Athletic "6"
            Text(
                text = "6",
                fontSize = 58.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                color = Color(0xFFFFD700),
                modifier = Modifier
                    .offset(y = (-6).dp)
                    .shadow(12.dp, spotColor = Color(0xFFFF8F00))
            )

            // Bold "BALL"
            Text(
                text = "BALL",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 4.sp,
                modifier = Modifier.offset(y = (-22).dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Fiery "STICK CRICKET" Ribbon Banner
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFF6D00),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD700)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .offset(y = (-10).dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFD50000),
                                    Color(0xFFFF6D00),
                                    Color(0xFFFF8F00),
                                    Color(0xFFD50000)
                                )
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "STICK CRICKET",
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawCrossedBats(cx: Float, cy: Float, length: Float) {
    val angles = listOf(-38f, 38f)
    val batW = 7f
    val handleW = 3.5f
    val handleH = length * 0.35f
    val bladeH = length * 0.65f

    for (ang in angles) {
        rotate(degrees = ang, pivot = Offset(cx, cy)) {
            // Blade
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFE4C690), Color(0xFFD2B075), Color(0xFFA8864C)),
                    startX = cx - batW / 2f,
                    endX = cx + batW / 2f
                ),
                topLeft = Offset(cx - batW / 2f, cy - bladeH * 0.5f),
                size = Size(batW, bladeH),
                cornerRadius = CornerRadius(2f, 2f)
            )

            // Colored Bat Sticker
            drawRect(
                color = Color(0xFFFF3D00),
                topLeft = Offset(cx - batW / 2f, cy - bladeH * 0.3f),
                size = Size(batW, 6f)
            )

            // Rubber Grip Handle
            drawRect(
                color = if (ang < 0) Color(0xFF0284C7) else Color(0xFF10B981),
                topLeft = Offset(cx - handleW / 2f, cy - bladeH * 0.5f - handleH),
                size = Size(handleW, handleH)
            )
        }
    }
}

private fun DrawScope.drawStumpsBadge(cx: Float, groundY: Float, width: Float) {
    val stumpW = 3.5f
    val stumpH = 40f
    val spacing = width * 0.45f

    val stumpColors = listOf(Color(0xFFF1C88E), Color(0xFFC79854))

    for (i in -1..1) {
        val sx = cx + i * spacing
        drawRect(
            brush = Brush.horizontalGradient(stumpColors, sx - stumpW / 2f, sx + stumpW / 2f),
            topLeft = Offset(sx - stumpW / 2f, groundY - stumpH),
            size = Size(stumpW, stumpH)
        )
    }

    // Glowing LED bails
    drawRect(
        color = Color(0xFFFFD54F),
        topLeft = Offset(cx - spacing - 2f, groundY - stumpH - 3f),
        size = Size(spacing * 2 + 4f, 2.5f)
    )
    drawLine(
        color = Color(0xFFFFF9C4),
        start = Offset(cx - spacing - 2f, groundY - stumpH - 2f),
        end = Offset(cx + spacing + 2f, groundY - stumpH - 2f),
        strokeWidth = 1.2f
    )
}

private fun DrawScope.drawHeroCricketBall(center: Offset, radius: Float) {
    // Drop shadow
    drawOval(
        color = Color(0x99000000),
        topLeft = Offset(center.x - radius * 0.85f, center.y + radius * 0.65f),
        size = Size(radius * 1.7f, radius * 0.55f)
    )

    // Red 3D Sphere
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFF5252),
                Color(0xFFD32F2F),
                Color(0xFF8B0000),
                Color(0xFF3E0000)
            ),
            center = Offset(center.x - radius * 0.32f, center.y - radius * 0.32f),
            radius = radius * 1.25f
        ),
        radius = radius,
        center = center
    )

    // Specular Highlight
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.9f), Color.Transparent),
            center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f),
            radius = radius * 0.35f
        ),
        radius = radius * 0.35f,
        center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f)
    )

    // Curved Stitched Seam
    val seamPath = Path().apply {
        moveTo(center.x - radius * 0.1f, center.y - radius + 1.5f)
        cubicTo(
            center.x + radius * 0.5f, center.y - radius * 0.4f,
            center.x + radius * 0.5f, center.y + radius * 0.4f,
            center.x - radius * 0.1f, center.y + radius - 1.5f
        )
    }

    drawPath(
        path = seamPath,
        color = Color(0xFFF8FAFC),
        style = Stroke(width = 2.2f, cap = StrokeCap.Round)
    )

    // Stitch dashes
    for (i in 1..8) {
        val t = i / 9f
        val y = center.y - radius + 2f * radius * t
        val x = center.x - radius * 0.1f + (radius * 0.55f) * 4f * t * (1f - t)
        drawLine(
            color = Color.White,
            start = Offset(x - 3f, y - 1f),
            end = Offset(x + 3f, y + 1f),
            strokeWidth = 1.4f,
            cap = StrokeCap.Round
        )
    }
}
