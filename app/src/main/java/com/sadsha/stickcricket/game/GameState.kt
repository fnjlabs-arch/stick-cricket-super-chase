package com.sadsha.stickcricket.game

import com.sadsha.stickcricket.model.BallOutcome
import com.sadsha.stickcricket.model.CosmeticItem
import com.sadsha.stickcricket.model.DeliveryType
import com.sadsha.stickcricket.model.Difficulty
import com.sadsha.stickcricket.model.MatchSummary

enum class BowlerPhase {
    READY,
    RUN_UP,
    DELIVERY_STRIDE,
    RELEASE,
    FOLLOW_THROUGH
}

enum class BatsmanPose {
    STANCE,
    BACKLIFT,
    SWING_OFF,
    SWING_STRAIGHT,
    SWING_LEG,
    DEFENSIVE,
    BOWLED,
    CELEBRATION
}

data class GameState(
    val score: Int = 0,
    val ballsFaced: Int = 0,
    val maxBalls: Int = 6,
    val target: Int = 24,
    val matchOvers: Int = 1,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val currentDelivery: DeliveryType = DeliveryType.NORMAL,
    val ballProgress: Float = 0f, // 0.0 at bowler hand, 1.0 at batsman crease
    val ballSideDrift: Float = 0f, // sideways drift in DP
    val isBowlerRunUp: Boolean = false,
    val isBallInFlight: Boolean = false,
    val batsmanSwingAngle: Float = 0f, // 0 for stance, -45 to +45 for swing
    val isGameOver: Boolean = false,
    val isPaused: Boolean = false,
    val isWicketContinueDialogVisible: Boolean = false,
    val hasUsedContinue: Boolean = false,
    val lastOutcome: BallOutcome? = null,
    val ballOutcomes: List<BallOutcome> = emptyList(),
    val foursCount: Int = 0,
    val sixesCount: Int = 0,
    val wicketsCount: Int = 0,
    val matchSummary: MatchSummary? = null,
    val equippedBat: CosmeticItem,
    val equippedHelmet: CosmeticItem,
    val equippedJersey: CosmeticItem,
    val equippedBall: CosmeticItem,
    val showBannerFeedback: Boolean = false,
    val bowlerPhase: BowlerPhase = BowlerPhase.READY,
    val bowlerRunUpProgress: Float = 0f,
    val batsmanPose: BatsmanPose = BatsmanPose.STANCE,
    val batsmanPoseProgress: Float = 0f,
    val cameraShakeX: Float = 0f,
    val cameraShakeY: Float = 0f,
    val contactFlashProgress: Float = 0f,
    val hitExitProgress: Float = 0f,
    val goldenBatBallsRemaining: Int = 0,
    val slowMotionBallsRemaining: Int = 0,
    val powerUpBannerMessage: String? = null,
    val lastShotStrength: Float = 0f,
    val lastPrecisionRating: String? = null,
    val lastTapXRatio: Float = -1f,
    val lastTapYRatio: Float = -1f,
    val tapFeedbackProgress: Float = 0f,
    val isDailyChallengeTargetMet: Boolean = false
) {
    val ballsRemaining: Int get() = (maxBalls - ballsFaced).coerceAtLeast(0)
    val runsNeeded: Int get() = (target - score).coerceAtLeast(0)
    val isTargetAchieved: Boolean get() = score >= target
    val isGoldenBatActive: Boolean get() = goldenBatBallsRemaining > 0
    val isSlowMotionActive: Boolean get() = slowMotionBallsRemaining > 0
    val currentOverIndex: Int get() = (ballsFaced / 6).coerceAtMost(matchOvers - 1)
    val currentOverDisplay: Int get() = currentOverIndex + 1
    val ballInCurrentOver: Int get() = (ballsFaced % 6) + 1
    val oversBowledText: String get() = "${ballsFaced / 6}.${ballsFaced % 6}"
    val totalOversText: String get() = "$matchOvers.0"
}

