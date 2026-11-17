package com.sadsha.stickcricket.game.engine

import com.sadsha.stickcricket.model.DeliveryType
import com.sadsha.stickcricket.model.OutcomeType
import com.sadsha.stickcricket.model.ShotDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-precision ball physics engine for 2.5D Stick Cricket.
 *
 * Coordinates are modeled in pitch space:
 * - progress (Y-axis along pitch): 0.0f at bowler bowling crease -> 1.0f at batsman batting crease
 * - lateral (X-axis across pitch): -1.0f (far off side) to +1.0f (far leg side), 0.0f is middle stump
 * - altitude (Z-axis above turf): 0.0f on turf surface, >0.0f in the air
 */
data class BallPhysicsState(
    val progress: Float = 0f,
    val lateral: Float = 0f,
    val altitude: Float = 0f,
    val radiusScale: Float = 1f,
    val shadowAlpha: Float = 0.5f,
    val shadowScale: Float = 1f,
    val isPitchContact: Boolean = false,
    val pitchContactProgress: Float = 0f,
    val seamAngle: Float = 0f
)

data class ExitTrajectoryState(
    val posX: Float = 0f,
    val posY: Float = 0f,
    val posZ: Float = 0f,
    val radiusScale: Float = 1f,
    val shadowAlpha: Float = 0f,
    val trailAlpha: Float = 0f,
    val isBoundaryCleared: Boolean = false
)

class BallPhysicsEngine {

    companion object {
        const val GRAVITY = 9.81f
        const val PITCH_BOUNCE_RESTITUTION = 0.65f // Turf energy conservation
        const val SEAM_ROTATION_SPEED = 720f // Degrees per second
    }

    /**
     * Calculates the ball's in-flight position, altitude, lateral swing drift, and turf bounce.
     *
     * @param normalizedTime Normalized flight time from 0.0f to 1.15f
     * @param delivery Delivery characteristics (speed, bounce point, swing curve, bounce height scale)
     * @param lateralDriftMultiplier Multiplier for sideways seam movement
     * @param unitScale Responsive canvas scale unit
     * @return BallPhysicsState containing 3D pitch coordinates and rendering parameters
     */
    fun calculateInFlightState(
        normalizedTime: Float,
        delivery: DeliveryType,
        lateralDriftMultiplier: Float = 1.0f,
        unitScale: Float = 1.0f
    ): BallPhysicsState {
        val bouncePoint = delivery.pitchBouncePoint
        val progress = calculateProgressCurve(normalizedTime, bouncePoint)

        // Lateral swing / drift calculation:
        // Aerodynamic swing curves quadratically as the air laminar boundary separates
        val swingMagnitude = delivery.swingCurve * 0.35f * lateralDriftMultiplier
        val lateral = if (swingMagnitude != 0f) {
            // Magnus/aerodynamic effect grows with time squared
            sin(progress * PI.toFloat()) * swingMagnitude
        } else {
            0f
        }

        // Altitude calculation:
        // Stage 1: Released high from bowler's hand (~36 * u height) down to turf impact
        // Stage 2: Post-bounce parabolic rebound up toward batsman's waist/chest
        val baseReleaseHeight = 36f * unitScale
        val bounceHeightScale = delivery.bounceHeightScale

        val altitude: Float
        val isContact: Boolean
        val contactProgress: Float

        if (progress <= bouncePoint) {
            val descentFraction = (progress / bouncePoint).coerceIn(0f, 1f)
            // Parabolic release arc descending to ground
            altitude = (1f - descentFraction) * baseReleaseHeight
            isContact = false
            contactProgress = 0f
        } else {
            val bounceFraction = ((progress - bouncePoint) / (1.0f - bouncePoint)).coerceIn(0f, 1.3f)
            // Parabolic rebound arc
            val reboundApex = 26f * unitScale * bounceHeightScale
            altitude = sin(bounceFraction * PI.toFloat()).coerceAtLeast(0f) * reboundApex

            // Contact dust ring window
            val dustWindow = 0.12f
            isContact = progress <= bouncePoint + dustWindow
            contactProgress = if (isContact) ((progress - bouncePoint) / dustWindow).coerceIn(0f, 1f) else 0f
        }

        // Perspective scaling: Ball starts larger near camera/bowler (~1.0) and scales down (~0.52) at the batting crease
        val perspectiveRadiusScale = (1.0f - progress * 0.48f).coerceIn(0.45f, 1.25f)

        // Shadow parameters: shadow tightens and darkens as ball nears the turf
        val groundProximity = (1.0f - (altitude / (80f * unitScale))).coerceIn(0.2f, 1.0f)
        val shadowAlpha = 0.55f * groundProximity
        val shadowScale = perspectiveRadiusScale * (0.8f + (1f - groundProximity) * 0.6f)

        // Seam spinning around axis
        val seamAngle = (normalizedTime * SEAM_ROTATION_SPEED) % 360f

        return BallPhysicsState(
            progress = progress,
            lateral = lateral,
            altitude = altitude,
            radiusScale = perspectiveRadiusScale,
            shadowAlpha = shadowAlpha,
            shadowScale = shadowScale,
            isPitchContact = isContact,
            pitchContactProgress = contactProgress,
            seamAngle = seamAngle
        )
    }

    /**
     * Calculates natural, readable ball progress down the pitch using a piecewise velocity curve.
     */
    fun calculateProgressCurve(normalizedTime: Float, bouncePoint: Float): Float {
        val t = normalizedTime.coerceIn(0f, 1.25f)
        val sweetSpot = 0.85f

        return if (bouncePoint < sweetSpot) {
            val tBounce = (bouncePoint * 0.90f).coerceIn(0.28f, 0.65f)
            when {
                t <= tBounce -> {
                    val frac = t / tBounce
                    lerp(0.0f, bouncePoint, frac)
                }
                t <= sweetSpot -> {
                    val frac = (t - tBounce) / (sweetSpot - tBounce)
                    lerp(bouncePoint, sweetSpot, frac)
                }
                else -> {
                    val frac = (t - sweetSpot) / (1.0f - sweetSpot)
                    lerp(sweetSpot, 1.06f, frac)
                }
            }
        } else {
            val tBounce = (bouncePoint * 0.94f).coerceIn(0.68f, 0.82f)
            if (t <= tBounce) {
                val frac = t / tBounce
                lerp(0.0f, bouncePoint, frac)
            } else {
                val frac = (t - tBounce) / (1.0f - tBounce)
                lerp(bouncePoint, 1.06f, frac)
            }
        }
    }

    /**
     * Calculates the post-hit exit trajectory for boundaries (6s, 4s), singles, and defensive taps.
     *
     * @param exitProgress Normalized exit animation progress (0.0f right after bat contact to 1.0f)
     * @param outcome Ball outcome type (SIX, FOUR, TWO, ONE, DOT, WICKET)
     * @param direction Shot direction (LEFT / off-side, LOFT / straight, RIGHT / leg-side)
     * @param originX Contact point X on canvas
     * @param originY Contact point Y on canvas
     * @param canvasWidth Available canvas width
     * @param canvasHeight Available canvas height
     * @param unitScale Responsive canvas scale unit
     */
    fun calculateExitTrajectory(
        exitProgress: Float,
        outcome: OutcomeType,
        direction: ShotDirection,
        originX: Float,
        originY: Float,
        canvasWidth: Float,
        canvasHeight: Float,
        unitScale: Float = 1.0f
    ): ExitTrajectoryState {
        val p = exitProgress.coerceIn(0f, 1.0f)

        return when (outcome) {
            OutcomeType.SIX -> {
                // Towering lofted six: Launches high into the air, arcs forward over bowler and camera into the stands!
                val dirAngle = when (direction) {
                    ShotDirection.LEFT -> -0.42f // Off side loft
                    ShotDirection.RIGHT -> 0.42f // Leg side hook
                    ShotDirection.LOFT -> 0.0f // Straight down the ground
                }

                // Horizontal movement toward and beyond the camera
                val targetDistanceX = dirAngle * (canvasWidth * 0.45f)
                val currentX = originX + targetDistanceX * p

                // Y movement: travels down the screen (towards camera and beyond)
                val targetDistanceY = canvasHeight * 0.85f
                val currentY = originY + targetDistanceY * p

                // Z / Altitude: High parabolic arc reaching peak at ~0.45 of flight
                val apexHeight = 110f * unitScale
                val altitude = sin(p * PI.toFloat()) * apexHeight

                // Perspective radius: starts standard at batsman, balloons as it flies directly over camera into grandstand!
                val radiusScale = 1.0f + p * 1.8f
                val shadowAlpha = ((1.0f - (altitude / (120f * unitScale))) * 0.45f).coerceIn(0f, 0.45f)
                val trailAlpha = (1.0f - p).coerceIn(0f, 1f)

                ExitTrajectoryState(
                    posX = currentX,
                    posY = currentY - altitude,
                    posZ = altitude,
                    radiusScale = radiusScale,
                    shadowAlpha = shadowAlpha,
                    trailAlpha = trailAlpha,
                    isBoundaryCleared = p > 0.70f
                )
            }
            OutcomeType.FOUR -> {
                // Ground boundary four: Sizzling ground drive with high exit velocity and grass skidding
                val dirAngle = when (direction) {
                    ShotDirection.LEFT -> -0.75f // Cover drive / cut
                    ShotDirection.RIGHT -> 0.75f // Square cut / leg glance
                    ShotDirection.LOFT -> 0.0f // Straight drive past bowler
                }

                val targetX = originX + dirAngle * (canvasWidth * 0.50f)
                val currentX = lerp(originX, targetX, p)

                val targetY = originY + canvasHeight * 0.65f
                val currentY = lerp(originY, targetY, p)

                // Stays close to turf, with small decaying hops
                val hops = sin(p * PI.toFloat() * 3f) * max(0f, (1f - p) * 14f * unitScale)
                val altitude = max(0f, hops)
                val radiusScale = 1.0f + p * 0.75f
                val shadowAlpha = 0.50f
                val trailAlpha = (1.0f - p * 0.8f).coerceIn(0f, 0.9f)

                ExitTrajectoryState(
                    posX = currentX,
                    posY = currentY - altitude,
                    posZ = altitude,
                    radiusScale = radiusScale,
                    shadowAlpha = shadowAlpha,
                    trailAlpha = trailAlpha,
                    isBoundaryCleared = p > 0.85f
                )
            }
            OutcomeType.ONE, OutcomeType.TWO, OutcomeType.THREE -> {
                // Push/scamper for runs: Controlled placement into outfield
                val dirAngle = when (direction) {
                    ShotDirection.LEFT -> -0.45f
                    ShotDirection.RIGHT -> 0.45f
                    ShotDirection.LOFT -> 0.10f
                }
                val currentX = originX + dirAngle * (canvasWidth * 0.30f) * p
                val currentY = originY + canvasHeight * 0.35f * p
                val altitude = max(0f, sin(p * PI.toFloat() * 2f) * (1f - p) * 8f * unitScale)

                ExitTrajectoryState(
                    posX = currentX,
                    posY = currentY - altitude,
                    posZ = altitude,
                    radiusScale = 1.0f + p * 0.35f,
                    shadowAlpha = 0.45f,
                    trailAlpha = 0.4f * (1f - p),
                    isBoundaryCleared = false
                )
            }
            OutcomeType.DOT -> {
                // Defensive block / missed: ball drops dead on the crease
                val currentX = originX + (if (direction == ShotDirection.LEFT) -8f else 8f) * unitScale * p
                val currentY = originY + 12f * unitScale * p
                val altitude = max(0f, sin(p * PI.toFloat()) * 4f * unitScale * (1f - p))

                ExitTrajectoryState(
                    posX = currentX,
                    posY = currentY - altitude,
                    posZ = altitude,
                    radiusScale = 1.0f,
                    shadowAlpha = 0.5f,
                    trailAlpha = 0f,
                    isBoundaryCleared = false
                )
            }
            OutcomeType.WICKET -> {
                // Stumps shattered / edge to keeper: ball ricochets backward past stumps
                val currentX = originX + (if (direction == ShotDirection.LEFT) 12f else -12f) * unitScale * p
                val currentY = originY - 30f * unitScale * p // Deflects backward
                val altitude = sin(p * PI.toFloat()) * 18f * unitScale

                ExitTrajectoryState(
                    posX = currentX,
                    posY = currentY - altitude,
                    posZ = altitude,
                    radiusScale = (1.0f - p * 0.3f).coerceAtLeast(0.6f),
                    shadowAlpha = 0.35f,
                    trailAlpha = 0.3f * (1f - p),
                    isBoundaryCleared = false
                )
            }
        }
    }

    private fun lerp(start: Float, stop: Float, amount: Float): Float {
        val clamped = amount.coerceIn(0f, 1f)
        return start + (stop - start) * clamped
    }
}
