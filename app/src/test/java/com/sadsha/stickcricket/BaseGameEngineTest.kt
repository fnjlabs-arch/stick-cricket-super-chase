package com.sadsha.stickcricket

import com.sadsha.stickcricket.game.BatsmanPose
import com.sadsha.stickcricket.game.engine.BallPhysicsEngine
import com.sadsha.stickcricket.game.engine.BaseGameEngine
import com.sadsha.stickcricket.game.engine.OverSequenceEngine
import com.sadsha.stickcricket.game.engine.StickmanBattingEngine
import com.sadsha.stickcricket.model.BallOutcome
import com.sadsha.stickcricket.model.DeliveryType
import com.sadsha.stickcricket.model.Difficulty
import com.sadsha.stickcricket.model.OutcomeType
import com.sadsha.stickcricket.model.ShotDirection
import com.sadsha.stickcricket.model.TimingResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BaseGameEngineTest {

    private lateinit var engine: BaseGameEngine
    private lateinit var physicsEngine: BallPhysicsEngine
    private lateinit var battingEngine: StickmanBattingEngine
    private lateinit var sequenceEngine: OverSequenceEngine

    @Before
    fun setUp() {
        engine = BaseGameEngine()
        physicsEngine = BallPhysicsEngine()
        battingEngine = StickmanBattingEngine()
        sequenceEngine = OverSequenceEngine(maxBalls = 6)
    }

    @Test
    fun overSequence_initializesWithExactSixBalls() {
        val state = sequenceEngine.startOver(targetRuns = 24)
        assertEquals(1, state.currentBallNumber)
        assertEquals(0, state.ballsFaced)
        assertEquals(6, state.ballsRemaining)
        assertEquals(0, state.score)
        assertEquals(24, state.runsNeeded)
        assertFalse(state.isOverFinished)
    }

    @Test
    fun overSequence_recordsSixBallsAndFinishes() {
        sequenceEngine.startOver(targetRuns = 50) // High target so doesn't finish early

        for (ball in 1..6) {
            val outcome = BallOutcome(
                ballIndex = ball,
                outcomeType = if (ball == 6) OutcomeType.SIX else OutcomeType.FOUR,
                runs = if (ball == 6) 6 else 4,
                isWicket = false,
                timing = TimingResult.PERFECT,
                direction = ShotDirection.LOFT,
                delivery = DeliveryType.NORMAL,
                commentary = "Superb shot!"
            )
            val updated = sequenceEngine.recordBallOutcome(outcome)
            assertEquals(ball, updated.ballsFaced)
            if (ball < 6) {
                assertFalse(updated.isOverFinished)
                sequenceEngine.advanceToNextBall(DeliveryType.NORMAL)
            } else {
                assertTrue(updated.isOverFinished)
            }
        }

        val finalState = sequenceEngine.getState()
        assertEquals(6, finalState.ballsFaced)
        assertEquals(0, finalState.ballsRemaining)
        assertEquals(26, finalState.score) // 5 * 4 + 6 = 26
        assertEquals(5, finalState.fours)
        assertEquals(1, finalState.sixes)
        assertTrue(finalState.isOverFinished)
    }

    @Test
    fun ballPhysics_calculatesFlightTrajectoryAndBounce() {
        val delivery = DeliveryType.NORMAL
        val preBounce = physicsEngine.calculateInFlightState(
            normalizedTime = 0.2f,
            delivery = delivery,
            unitScale = 2.0f
        )
        assertTrue("Pre-bounce progress should be non-negative", preBounce.progress >= 0f)
        assertTrue("Pre-bounce ball should have altitude", preBounce.altitude > 0f)

        val postBounce = physicsEngine.calculateInFlightState(
            normalizedTime = 0.85f, // At sweet spot
            delivery = delivery,
            unitScale = 2.0f
        )
        assertTrue("Post-bounce progress should reach sweet spot", postBounce.progress in 0.80f..0.90f)
        assertTrue("Ball should be elevated at batsman crease", postBounce.altitude > 0f)
    }

    @Test
    fun ballPhysics_calculatesExitTrajectoryForSix() {
        val exit = physicsEngine.calculateExitTrajectory(
            exitProgress = 0.5f,
            outcome = OutcomeType.SIX,
            direction = ShotDirection.LOFT,
            originX = 200f,
            originY = 300f,
            canvasWidth = 400f,
            canvasHeight = 800f,
            unitScale = 2.0f
        )

        assertNotNull(exit)
        assertTrue("Six should have substantial altitude", exit.posZ > 50f)
        assertTrue("Six should scale up towards camera", exit.radiusScale > 1.0f)
    }

    @Test
    fun stickmanBatting_calculatesKinematicJoints() {
        val stanceJoints = battingEngine.calculateJoints(
            baseX = 200f,
            baseY = 400f,
            pose = BatsmanPose.STANCE,
            poseProgress = 1.0f,
            unitScale = 2.0f
        )

        assertNotNull(stanceJoints.headCenter)
        assertNotNull(stanceJoints.hands)
        assertNotNull(stanceJoints.batBladeEnd)
        assertTrue("Hands should be positioned above feet", stanceJoints.hands.y < stanceJoints.footLeft.y)

        val driveJoints = battingEngine.calculateJoints(
            baseX = 200f,
            baseY = 400f,
            pose = BatsmanPose.SWING_OFF,
            poseProgress = 1.0f,
            unitScale = 2.0f
        )

        assertTrue("Bat angle should differ in swing pose", driveJoints.batAngleDegrees != stanceJoints.batAngleDegrees)
    }

    @Test
    fun baseGameEngine_integratesSequencePhysicsAndBatting() {
        val renderState = engine.startOver(targetRuns = 18, difficulty = Difficulty.NORMAL)
        assertEquals(1, renderState.overState.currentBallNumber)
        assertEquals(6, renderState.overState.ballsRemaining)

        // Simulate flight update
        val physics = engine.updateBallPhysics(0.85f, unitScale = 2.0f)
        assertTrue(engine.getRenderState().isSweetSpotActive)

        // Simulate shot execution
        val outcome = engine.playShot(
            ballProgress = 0.85f,
            direction = ShotDirection.LOFT,
            difficulty = Difficulty.NORMAL,
            isGoldenBat = false
        )

        assertNotNull(outcome)
        assertEquals(1, engine.getRenderState().overState.ballsFaced)
    }
}
