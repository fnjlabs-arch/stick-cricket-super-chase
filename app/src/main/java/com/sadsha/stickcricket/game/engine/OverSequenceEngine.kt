package com.sadsha.stickcricket.game.engine

import com.sadsha.stickcricket.model.BallOutcome
import com.sadsha.stickcricket.model.DeliveryType
import com.sadsha.stickcricket.model.Difficulty
import com.sadsha.stickcricket.model.OutcomeType

/**
 * Lifecycle phases for each delivery in the 6-ball sequence.
 */
enum class BallSequencePhase {
    PREPARING,
    BOWLER_RUNUP,
    DELIVERY_STRIDE,
    RELEASE,
    IN_FLIGHT,
    HITTING_WINDOW,
    CONTACT_FLASH,
    EXIT_TRAJECTORY,
    BALL_SUMMARY,
    OVER_COMPLETE
}

/**
 * Immutable snapshot of the 6-ball over progress.
 */
data class OverSequenceState(
    val currentBallNumber: Int = 1, // 1 to 6
    val ballsFaced: Int = 0, // 0 to 6
    val maxBalls: Int = 6,
    val score: Int = 0,
    val target: Int = 24,
    val wickets: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val dots: Int = 0,
    val phase: BallSequencePhase = BallSequencePhase.PREPARING,
    val currentDelivery: DeliveryType = DeliveryType.NORMAL,
    val ballOutcomes: List<BallOutcome> = emptyList(),
    val isOverFinished: Boolean = false,
    val isTargetAchieved: Boolean = false
) {
    val ballsRemaining: Int get() = (maxBalls - ballsFaced).coerceAtLeast(0)
    val runsNeeded: Int get() = (target - score).coerceAtLeast(0)
    val runRate: Float get() = if (ballsFaced > 0) (score.toFloat() / ballsFaced) * 6f else 0f
    val requiredRunRate: Float get() = if (ballsRemaining > 0) (runsNeeded.toFloat() / ballsRemaining) * 6f else 0f
}

/**
 * Controller for managing the 6-ball cricket over sequence.
 */
class OverSequenceEngine(
    val maxBalls: Int = 6
) {
    private var state = OverSequenceState(maxBalls = maxBalls)

    fun getState(): OverSequenceState = state

    /**
     * Resets or begins a fresh 6-ball over.
     */
    fun startOver(targetRuns: Int, initialDelivery: DeliveryType = DeliveryType.NORMAL): OverSequenceState {
        state = OverSequenceState(
            currentBallNumber = 1,
            ballsFaced = 0,
            maxBalls = maxBalls,
            score = 0,
            target = targetRuns,
            wickets = 0,
            fours = 0,
            sixes = 0,
            dots = 0,
            phase = BallSequencePhase.PREPARING,
            currentDelivery = initialDelivery,
            ballOutcomes = emptyList(),
            isOverFinished = false,
            isTargetAchieved = false
        )
        return state
    }

    /**
     * Transitions to the next phase in the delivery sequence.
     */
    fun setPhase(newPhase: BallSequencePhase): OverSequenceState {
        state = state.copy(phase = newPhase)
        return state
    }

    /**
     * Sets the active delivery for the upcoming ball.
     */
    fun setDelivery(delivery: DeliveryType): OverSequenceState {
        state = state.copy(currentDelivery = delivery)
        return state
    }

    /**
     * Records the outcome of the current ball and updates the 6-ball over stats.
     */
    fun recordBallOutcome(outcome: BallOutcome): OverSequenceState {
        val newBallsFaced = state.ballsFaced + 1
        val newScore = state.score + outcome.runs
        val newWickets = state.wickets + (if (outcome.isWicket) 1 else 0)
        val newFours = state.fours + (if (outcome.outcomeType == OutcomeType.FOUR) 1 else 0)
        val newSixes = state.sixes + (if (outcome.outcomeType == OutcomeType.SIX) 1 else 0)
        val newDots = state.dots + (if (outcome.outcomeType == OutcomeType.DOT) 1 else 0)
        val updatedOutcomes = state.ballOutcomes + outcome
        val targetMet = newScore >= state.target
        val overFinished = newBallsFaced >= maxBalls || targetMet

        state = state.copy(
            ballsFaced = newBallsFaced,
            score = newScore,
            wickets = newWickets,
            fours = newFours,
            sixes = newSixes,
            dots = newDots,
            ballOutcomes = updatedOutcomes,
            isTargetAchieved = targetMet,
            isOverFinished = overFinished,
            phase = if (overFinished) BallSequencePhase.OVER_COMPLETE else BallSequencePhase.BALL_SUMMARY
        )
        return state
    }

    /**
     * Advances to prepare the next ball in the over if any balls remain.
     */
    fun advanceToNextBall(nextDelivery: DeliveryType): OverSequenceState {
        if (state.isOverFinished) return state

        val nextBall = state.ballsFaced + 1
        state = state.copy(
            currentBallNumber = nextBall,
            currentDelivery = nextDelivery,
            phase = BallSequencePhase.PREPARING
        )
        return state
    }
}
