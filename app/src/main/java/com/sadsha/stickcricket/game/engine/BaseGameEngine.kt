package com.sadsha.stickcricket.game.engine

import com.sadsha.stickcricket.game.BatsmanPose
import com.sadsha.stickcricket.game.GameEngine
import com.sadsha.stickcricket.model.BallOutcome
import com.sadsha.stickcricket.model.DeliveryType
import com.sadsha.stickcricket.model.Difficulty
import com.sadsha.stickcricket.model.OutcomeType
import com.sadsha.stickcricket.model.ShotDirection
import com.sadsha.stickcricket.model.TimingResult

/**
 * Unified Snapshot of Engine State for Canvas Rendering.
 */
data class CricketEngineRenderState(
    val overState: OverSequenceState = OverSequenceState(),
    val ballPhysics: BallPhysicsState = BallPhysicsState(),
    val batsmanJoints: StickmanJoints? = null,
    val exitTrajectory: ExitTrajectoryState? = null,
    val isBallInFlight: Boolean = false,
    val isSweetSpotActive: Boolean = false,
    val sweetSpotTargetProgress: Float = GameEngine.HIT_SWEET_SPOT,
    val lastOutcome: BallOutcome? = null
)

/**
 * Base Game Engine implementation handling:
 * 1. The 6-ball over sequence state machine.
 * 2. 2.5D ball aerodynamics, bounce physics, and exit trajectories.
 * 3. Kinematic stickman batsman joint positioning and batting animations.
 */
class BaseGameEngine(
    val maxBalls: Int = 6,
    val physicsEngine: BallPhysicsEngine = BallPhysicsEngine(),
    val battingEngine: StickmanBattingEngine = StickmanBattingEngine(),
    val sequenceEngine: OverSequenceEngine = OverSequenceEngine(maxBalls),
    val outcomeEngine: GameEngine = GameEngine()
) {

    private var renderState = CricketEngineRenderState()

    fun getRenderState(): CricketEngineRenderState = renderState

    /**
     * Initializes a new match or 6-ball over.
     */
    fun startOver(targetRuns: Int, difficulty: Difficulty): CricketEngineRenderState {
        val firstDelivery = outcomeEngine.pickDelivery(difficulty, 1)
        val overState = sequenceEngine.startOver(targetRuns, firstDelivery)
        renderState = CricketEngineRenderState(
            overState = overState,
            ballPhysics = BallPhysicsState(),
            isBallInFlight = false,
            lastOutcome = null
        )
        return renderState
    }

    /**
     * Advances ball flight physics for the current frame.
     */
    fun updateBallPhysics(
        normalizedTime: Float,
        delivery: DeliveryType = renderState.overState.currentDelivery,
        lateralDriftMultiplier: Float = 1.0f,
        unitScale: Float = 1.0f
    ): BallPhysicsState {
        val physics = physicsEngine.calculateInFlightState(
            normalizedTime = normalizedTime,
            delivery = delivery,
            lateralDriftMultiplier = lateralDriftMultiplier,
            unitScale = unitScale
        )
        val inSweetSpot = physics.progress in (GameEngine.HIT_SWEET_SPOT - 0.08f)..(GameEngine.HIT_SWEET_SPOT + 0.08f)

        renderState = renderState.copy(
            ballPhysics = physics,
            isBallInFlight = normalizedTime in 0f..1.15f,
            isSweetSpotActive = inSweetSpot
        )
        return physics
    }

    /**
     * Updates stickman batsman kinematics and skeletal rig.
     */
    fun updateBatsmanPose(
        baseX: Float,
        baseY: Float,
        pose: BatsmanPose,
        poseProgress: Float,
        dynamicSwingAngle: Float = 0f,
        unitScale: Float = 1.0f
    ): StickmanJoints {
        val joints = battingEngine.calculateJoints(
            baseX = baseX,
            baseY = baseY,
            pose = pose,
            poseProgress = poseProgress,
            dynamicSwingAngle = dynamicSwingAngle,
            unitScale = unitScale
        )
        renderState = renderState.copy(batsmanJoints = joints)
        return joints
    }

    /**
     * Executes a batting shot, evaluating timing, strength, and resolving the ball outcome.
     */
    fun playShot(
        ballProgress: Float,
        direction: ShotDirection,
        difficulty: Difficulty,
        tapXRatio: Float? = null,
        tapYRatio: Float? = null,
        ballXRatio: Float? = null,
        ballYRatio: Float? = null,
        isGoldenBat: Boolean = false
    ): BallOutcome {
        val timingResult = outcomeEngine.calculateTimingAndStrength(
            ballProgress = ballProgress,
            difficulty = difficulty,
            tapXRatio = tapXRatio,
            tapYRatio = tapYRatio,
            ballXRatio = ballXRatio,
            ballYRatio = ballYRatio,
            isGoldenBat = isGoldenBat
        )

        val outcome = outcomeEngine.resolveShot(
            ballNumber = renderState.overState.currentBallNumber,
            timing = timingResult.timing,
            direction = direction,
            delivery = renderState.overState.currentDelivery,
            difficulty = difficulty,
            isGoldenBat = isGoldenBat,
            shotStrength = timingResult.shotStrength
        )

        val updatedOver = sequenceEngine.recordBallOutcome(outcome)

        renderState = renderState.copy(
            overState = updatedOver,
            lastOutcome = outcome,
            isBallInFlight = false,
            isSweetSpotActive = false
        )

        return outcome
    }

    /**
     * Updates post-contact ball exit flight trajectory.
     */
    fun updateExitTrajectory(
        exitProgress: Float,
        originX: Float,
        originY: Float,
        canvasWidth: Float,
        canvasHeight: Float,
        unitScale: Float = 1.0f
    ): ExitTrajectoryState? {
        val outcome = renderState.lastOutcome ?: return null
        val exit = physicsEngine.calculateExitTrajectory(
            exitProgress = exitProgress,
            outcome = outcome.outcomeType,
            direction = outcome.direction,
            originX = originX,
            originY = originY,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            unitScale = unitScale
        )
        renderState = renderState.copy(exitTrajectory = exit)
        return exit
    }

    /**
     * Prepares the next ball in the 6-ball sequence.
     */
    fun prepareNextBall(difficulty: Difficulty): OverSequenceState {
        val nextBallNum = renderState.overState.ballsFaced + 1
        val delivery = outcomeEngine.pickDelivery(difficulty, nextBallNum)
        val updatedOver = sequenceEngine.advanceToNextBall(delivery)

        renderState = renderState.copy(
            overState = updatedOver,
            ballPhysics = BallPhysicsState(),
            exitTrajectory = null,
            isBallInFlight = false,
            isSweetSpotActive = false
        )
        return updatedOver
    }
}
