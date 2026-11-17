package com.sadsha.stickcricket.game

import com.sadsha.stickcricket.model.BallOutcome
import com.sadsha.stickcricket.model.DeliveryType
import com.sadsha.stickcricket.model.Difficulty
import com.sadsha.stickcricket.model.OutcomeType
import com.sadsha.stickcricket.model.ShotDirection
import com.sadsha.stickcricket.model.TimingResult
import kotlin.random.Random

class GameEngine {

    companion object {
        const val HIT_SWEET_SPOT = 0.85f
        const val HIT_WINDOW_PERFECT = 0.068f // +- around sweet spot
        const val HIT_WINDOW_GOOD = 0.135f
        const val HIT_WINDOW_ACCEPTABLE = 0.22f

        /**
         * Standard linear interpolation helper.
         * Clamps amount to [0.0, 1.0] to guarantee bounded interpolation.
         */
        fun lerp(start: Float, stop: Float, amount: Float): Float {
            val clamped = amount.coerceIn(0f, 1f)
            return start + (stop - start) * clamped
        }

        /**
         * Calculates natural, readable ball progress down the pitch using a piecewise
         * linear interpolation (lerp) velocity curve.
         *
         * Physical flight stages:
         * 1. Pre-bounce aerial flight (0.0 to t_bounce):
         *    Ball travels through the air from bowler's release hand down the pitch.
         * 2. Turf impact (t_bounce):
         *    Ball impacts the pitch at the delivery's bounce point.
         * 3. Post-bounce rise to hitting sweet spot (t_bounce to t_sweet):
         *    Ball decelerates slightly upon pitch contact (pitch friction), providing
         *    the batsman with a clear, readable hitting window right at HIT_SWEET_SPOT (0.85).
         * 4. Follow-through past the batsman (t_sweet to 1.0):
         *    Ball crosses the batting crease and travels past the stumps.
         *
         * @param normalizedTime Raw elapsed flight time normalized to [0.0, 1.0]
         * @param bouncePoint Spatial pitch bounce position (e.g. 0.55 for good length, 0.38 for bouncer, 0.88 for yorker)
         * @return Interpolated spatial ball progress on the pitch
         */
        fun calculateBallProgress(normalizedTime: Float, bouncePoint: Float): Float {
            val t = normalizedTime.coerceIn(0f, 1.25f)

            return if (bouncePoint < HIT_SWEET_SPOT) {
                // Pitch bounce occurs at a calculated time fraction based on length
                val tBounce = (bouncePoint * 0.90f).coerceIn(0.28f, 0.65f)
                val tSweet = 0.85f // Batsman hitting zone time mark

                when {
                    t <= tBounce -> {
                        // Stage 1: Bowler release to pitch bounce
                        val fraction = t / tBounce
                        lerp(0.0f, bouncePoint, fraction)
                    }
                    t <= tSweet -> {
                        // Stage 2: Post-bounce rise to batsman's sweet spot
                        val fraction = (t - tBounce) / (tSweet - tBounce)
                        lerp(bouncePoint, HIT_SWEET_SPOT, fraction)
                    }
                    else -> {
                        // Stage 3: Past the batsman to the wicket-keeper / stumps
                        val fraction = (t - tSweet) / (1.0f - tSweet)
                        lerp(HIT_SWEET_SPOT, 1.06f, fraction)
                    }
                }
            } else {
                // Full delivery / Yorker pitching directly in the blockhole
                val tBounce = (bouncePoint * 0.94f).coerceIn(0.68f, 0.82f)
                when {
                    t <= tBounce -> {
                        val fraction = t / tBounce
                        lerp(0.0f, bouncePoint, fraction)
                    }
                    else -> {
                        val fraction = (t - tBounce) / (1.0f - tBounce)
                        lerp(bouncePoint, 1.06f, fraction)
                    }
                }
            }
        }
    }

    fun pickDelivery(difficulty: Difficulty, ballNumber: Int): DeliveryType {
        val candidates = DeliveryType.values().filter { it.minDifficulty.ordinal <= difficulty.ordinal }
        // For first ball, give a standard delivery
        if (ballNumber == 1) return DeliveryType.NORMAL

        // Random pick with weighted preference
        val rand = Random.nextFloat()
        return when (difficulty) {
            Difficulty.EASY -> {
                if (rand < 0.6f) DeliveryType.NORMAL else DeliveryType.SLOW
            }
            Difficulty.NORMAL -> {
                when {
                    rand < 0.40f -> DeliveryType.NORMAL
                    rand < 0.65f -> DeliveryType.FAST
                    rand < 0.85f -> DeliveryType.SWING
                    else -> DeliveryType.SLOW
                }
            }
            Difficulty.HARD -> {
                when {
                    rand < 0.25f -> DeliveryType.FAST
                    rand < 0.50f -> DeliveryType.SWING
                    rand < 0.70f -> DeliveryType.YORKER
                    rand < 0.85f -> DeliveryType.BOUNCER
                    else -> DeliveryType.NORMAL
                }
            }
            Difficulty.INSANE -> {
                when {
                    rand < 0.30f -> DeliveryType.YORKER
                    rand < 0.55f -> DeliveryType.BOUNCER
                    rand < 0.75f -> DeliveryType.FAST
                    rand < 0.90f -> DeliveryType.SWING
                    else -> DeliveryType.SLOW
                }
            }
        }
    }

    data class TimingAndStrength(
        val timing: TimingResult,
        val shotStrength: Float, // 0.0f to 1.0f (0% to 100%)
        val timingPrecision: Float, // 0.0f to 1.0f
        val spatialPrecision: Float, // 0.0f to 1.0f
        val ratingText: String // e.g. "CRUSHED! 98%", "SWEET SPOT 85%"
    )

    fun calculateTiming(ballProgress: Float, difficulty: Difficulty): TimingResult {
        val tolerance = difficulty.timingTolerance
        val diff = ballProgress - HIT_SWEET_SPOT

        val perfectMargin = HIT_WINDOW_PERFECT * tolerance
        val goodMargin = HIT_WINDOW_GOOD * tolerance
        val acceptableMargin = HIT_WINDOW_ACCEPTABLE * tolerance

        return when {
            kotlin.math.abs(diff) <= perfectMargin -> TimingResult.PERFECT
            kotlin.math.abs(diff) <= goodMargin -> TimingResult.GOOD
            diff < -acceptableMargin -> TimingResult.MISS
            diff < -goodMargin -> TimingResult.EARLY
            diff > acceptableMargin -> TimingResult.MISS
            else -> TimingResult.LATE
        }
    }

    /**
     * Calculates shot strength and timing classification based on both:
     * 1. Temporal precision: How close the ball is to the hitting sweet spot (0.85).
     * 2. Spatial precision: User's tap precision relative to the ball's position.
     */
    fun calculateTimingAndStrength(
        ballProgress: Float,
        difficulty: Difficulty,
        tapXRatio: Float? = null,
        tapYRatio: Float? = null,
        ballXRatio: Float? = null,
        ballYRatio: Float? = null,
        isGoldenBat: Boolean = false
    ): TimingAndStrength {
        val tolerance = difficulty.timingTolerance
        val diff = ballProgress - HIT_SWEET_SPOT
        val absDiff = kotlin.math.abs(diff)

        val perfectMargin = HIT_WINDOW_PERFECT * tolerance
        val goodMargin = HIT_WINDOW_GOOD * tolerance
        val acceptableMargin = HIT_WINDOW_ACCEPTABLE * tolerance

        val timing = when {
            absDiff <= perfectMargin -> TimingResult.PERFECT
            absDiff <= goodMargin -> TimingResult.GOOD
            diff < -acceptableMargin -> TimingResult.MISS
            diff < -goodMargin -> TimingResult.EARLY
            diff > acceptableMargin -> TimingResult.MISS
            else -> TimingResult.LATE
        }

        // Timing precision factor: 1.0f right on sweet spot, decaying smoothly towards 0.0f
        val maxTimingDelta = acceptableMargin * 1.25f
        val timingPrecision = (1.0f - (absDiff / maxTimingDelta)).coerceIn(0f, 1f)

        // Spatial precision factor: user's tap precision relative to the ball's position
        val spatialPrecision: Float = if (tapXRatio != null && tapYRatio != null &&
            ballXRatio != null && ballYRatio != null) {
            val dx = tapXRatio - ballXRatio
            val dy = tapYRatio - ballYRatio
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
            // Normalized radius: within 0.35 distance is full precision, tapers smoothly
            (1.0f - (distance / 0.42f)).coerceIn(0.18f, 1.0f)
        } else {
            // Button-based or center tap precision mapped directly from timing
            when (timing) {
                TimingResult.PERFECT -> 0.95f
                TimingResult.GOOD -> 0.82f
                TimingResult.EARLY, TimingResult.LATE -> 0.58f
                TimingResult.MISS -> 0.20f
            }
        }

        // Weighted shot strength calculation (70% timing precision + 30% spatial tap precision)
        val baseStrength = (timingPrecision * 0.70f + spatialPrecision * 0.30f)
        val boostedStrength = if (isGoldenBat) {
            (baseStrength * 1.30f).coerceIn(0.40f, 1.0f)
        } else {
            baseStrength.coerceIn(0.08f, 1.0f)
        }

        val percentage = (boostedStrength * 100).toInt()
        val rating = when {
            boostedStrength >= 0.90f -> "CRUSHED! ${percentage}%"
            boostedStrength >= 0.75f -> "SWEET SPOT ${percentage}%"
            boostedStrength >= 0.55f -> "SOLID TIMING ${percentage}%"
            boostedStrength >= 0.35f -> "MISTIMED ${percentage}%"
            else -> "WEAK CONTACT ${percentage}%"
        }

        return TimingAndStrength(
            timing = timing,
            shotStrength = boostedStrength,
            timingPrecision = timingPrecision,
            spatialPrecision = spatialPrecision,
            ratingText = rating
        )
    }

    fun resolveShot(
        ballNumber: Int,
        timing: TimingResult,
        direction: ShotDirection,
        delivery: DeliveryType,
        difficulty: Difficulty,
        isGoldenBat: Boolean = false,
        shotStrength: Float = 0.5f
    ): BallOutcome {
        val rand = Random.nextFloat()

        val outcomeType: OutcomeType = when {
            timing == TimingResult.PERFECT || shotStrength >= 0.88f -> {
                when (direction) {
                    ShotDirection.LOFT -> {
                        if (isGoldenBat || shotStrength >= 0.90f || rand < 0.75f) OutcomeType.SIX else OutcomeType.FOUR
                    }
                    ShotDirection.LEFT, ShotDirection.RIGHT -> {
                        if (isGoldenBat || shotStrength >= 0.94f) OutcomeType.SIX
                        else if (shotStrength >= 0.78f || rand < 0.70f) OutcomeType.FOUR
                        else OutcomeType.TWO
                    }
                }
            }
            timing == TimingResult.GOOD || shotStrength >= 0.68f -> {
                when (direction) {
                    ShotDirection.LOFT -> {
                        if (isGoldenBat || shotStrength >= 0.80f || rand < 0.40f) OutcomeType.SIX
                        else if (shotStrength >= 0.65f || rand < 0.78f) OutcomeType.FOUR
                        else OutcomeType.TWO
                    }
                    ShotDirection.LEFT, ShotDirection.RIGHT -> {
                        if (isGoldenBat || shotStrength >= 0.78f || rand < 0.60f) OutcomeType.FOUR
                        else if (rand < 0.85f) OutcomeType.TWO
                        else OutcomeType.ONE
                    }
                }
            }
            timing == TimingResult.EARLY -> {
                if (isGoldenBat) {
                    OutcomeType.FOUR
                } else if (shotStrength >= 0.58f) {
                    OutcomeType.TWO
                } else {
                    when {
                        delivery == DeliveryType.BOUNCER && rand < 0.30f -> OutcomeType.WICKET // caught top edge
                        rand < 0.40f -> OutcomeType.ONE
                        rand < 0.65f -> OutcomeType.DOT
                        rand < 0.85f -> OutcomeType.TWO
                        rand < 0.95f -> OutcomeType.FOUR
                        else -> OutcomeType.WICKET // caught in deep
                    }
                }
            }
            timing == TimingResult.LATE -> {
                if (isGoldenBat) {
                    OutcomeType.TWO
                } else if (shotStrength >= 0.58f) {
                    OutcomeType.TWO
                } else {
                    when {
                        delivery == DeliveryType.FAST && rand < 0.35f -> OutcomeType.WICKET // beaten by pace / bowled
                        delivery == DeliveryType.YORKER && rand < 0.45f -> OutcomeType.WICKET // yorker bowled
                        rand < 0.45f -> OutcomeType.DOT
                        rand < 0.75f -> OutcomeType.ONE
                        rand < 0.88f -> OutcomeType.TWO
                        else -> OutcomeType.WICKET
                    }
                }
            }
            else -> { // TimingResult.MISS
                if (isGoldenBat) {
                    OutcomeType.ONE // Thick edge deflection protects wicket
                } else {
                    // Ball beat the bat
                    if (delivery == DeliveryType.YORKER || rand < 0.75f) {
                        OutcomeType.WICKET
                    } else {
                        OutcomeType.DOT
                    }
                }
            }
        }

        val baseRuns = outcomeType.defaultRuns
        val runs = if (isGoldenBat && outcomeType != OutcomeType.WICKET && baseRuns > 0) {
            (baseRuns + 2).coerceAtMost(10)
        } else {
            baseRuns
        }
        val isWicket = outcomeType == OutcomeType.WICKET

        val commentary = if (isGoldenBat && runs > 0) {
            "⚡ GOLDEN BAT POWER! Smashed with incredible ${(shotStrength * 100).toInt()}% power for $runs runs! ⚡"
        } else if (shotStrength >= 0.90f && runs >= 4) {
            "⚡ MAXIMUM POWER (${(shotStrength * 100).toInt()}%)! Sweet middle of the bat, cleared with supreme force!"
        } else {
            generateCommentary(outcomeType, timing, direction, delivery)
        }

        return BallOutcome(
            ballIndex = ballNumber,
            outcomeType = outcomeType,
            runs = runs,
            isWicket = isWicket,
            timing = timing,
            direction = direction,
            delivery = delivery,
            commentary = commentary,
            shotStrength = shotStrength
        )
    }

    private fun generateCommentary(
        outcome: OutcomeType,
        timing: TimingResult,
        direction: ShotDirection,
        delivery: DeliveryType
    ): String {
        return when (outcome) {
            OutcomeType.SIX -> when (direction) {
                ShotDirection.LOFT -> "SMASHED straight down the ground, high into the stands! MAXIMUM SIX!"
                ShotDirection.LEFT -> "Magnificent lofted drive over the covers! Six runs all the way!"
                ShotDirection.RIGHT -> "Hooked powerfully over deep mid-wicket for a massive SIX!"
            }
            OutcomeType.FOUR -> when (direction) {
                ShotDirection.LEFT -> "Exquisite cut past backward point, races away to the boundary!"
                ShotDirection.RIGHT -> "Swept neatly through square leg for FOUR crisp runs!"
                ShotDirection.LOFT -> "Drilled past the bowler, split the field for FOUR!"
            }
            OutcomeType.THREE -> "Great running between the wickets, scampered back for 3!"
            OutcomeType.TWO -> "Pushed into the gap, comfortably turning for two!"
            OutcomeType.ONE -> "Pushed down to long-on for a quick single."
            OutcomeType.DOT -> "Beaten outside off stump! Good delivery, no run."
            OutcomeType.WICKET -> when (delivery) {
                DeliveryType.YORKER -> "CLEAN BOWLED! The toe-crusher sneaks underneath the willow!"
                DeliveryType.BOUNCER -> "EDGED AND TAKEN! Caught behind off a vicious bouncer!"
                else -> "OUT! Caught by the fielder in the deep! Big wicket!"
            }
        }
    }
}
