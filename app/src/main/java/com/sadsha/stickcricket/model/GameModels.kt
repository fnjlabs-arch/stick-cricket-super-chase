package com.sadsha.stickcricket.model

enum class Difficulty(
    val title: String,
    val targetMin: Int,
    val targetMax: Int,
    val speedMultiplier: Float,
    val timingTolerance: Float,
    val coinMultiplier: Float,
    val description: String
) {
    EASY("Easy", 15, 20, 0.85f, 1.35f, 1.0f, "Generous timing & steady deliveries"),
    NORMAL("Normal", 20, 30, 1.0f, 1.15f, 1.25f, "Balanced speed & mixed deliveries"),
    HARD("Hard", 30, 40, 1.10f, 0.95f, 1.5f, "Faster deliveries with swing & yorkers"),
    INSANE("Insane", 40, 60, 1.20f, 0.82f, 2.0f, "Express pace & tricky variations")
}

enum class DeliveryType(
    val displayName: String,
    val speedScale: Float,
    val swingCurve: Float,
    val pitchBouncePoint: Float, // 0.0 to 1.0 along the pitch
    val bounceHeightScale: Float,
    val minDifficulty: Difficulty
) {
    NORMAL("Standard Seam", 1.0f, 0.0f, 0.55f, 1.0f, Difficulty.EASY),
    FAST("Express Thunderbolt", 1.12f, 0.0f, 0.58f, 1.05f, Difficulty.NORMAL),
    SLOW("Deceptive Off-Cutter", 0.82f, -0.06f, 0.52f, 0.9f, Difficulty.EASY),
    SWING("Banana In-Swinger", 1.0f, 0.28f, 0.54f, 1.0f, Difficulty.NORMAL),
    YORKER("Laser Toe-Crusher", 1.08f, 0.0f, 0.88f, 0.35f, Difficulty.HARD),
    BOUNCER("Rib-Cage Bouncer", 1.06f, 0.05f, 0.38f, 1.55f, Difficulty.HARD)
}

enum class TimingResult(val displayName: String, val ratingScore: Int) {
    PERFECT("PERFECT!", 100),
    GOOD("GOOD!", 75),
    EARLY("EARLY!", 40),
    LATE("LATE!", 40),
    MISS("MISSED!", 0)
}

enum class ShotDirection(val title: String, val angleDegrees: Float) {
    LEFT("Off Side", -45f),
    LOFT("Straight Loft", 0f),
    RIGHT("Leg Side", 45f)
}

enum class OutcomeType(val label: String, val defaultRuns: Int) {
    DOT("DOT BALL", 0),
    ONE("SINGLE", 1),
    TWO("DOUBLE", 2),
    THREE("3 RUNS", 3),
    FOUR("FOUR!!", 4),
    SIX("MAXIMUM SIX!!", 6),
    WICKET("OUT!!", 0)
}

data class BallOutcome(
    val ballIndex: Int, // 1 to 6
    val outcomeType: OutcomeType,
    val runs: Int,
    val isWicket: Boolean,
    val timing: TimingResult,
    val direction: ShotDirection,
    val delivery: DeliveryType,
    val commentary: String,
    val shotStrength: Float = 0.5f // 0.0f to 1.0f (0% to 100% power)
)

enum class CosmeticCategory(val label: String) {
    BAT("Cricket Bats"),
    HELMET("Helmets"),
    JERSEY("Jerseys"),
    BALL("Balls");

    val title: String get() = label
}

data class CosmeticItem(
    val id: String,
    val name: String,
    val category: CosmeticCategory,
    val price: Int,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val accentColorHex: Long,
    val description: String,
    val isDefault: Boolean = false
) {
    val costCoins: Int get() = price
}

data class DailyChallenge(
    val dateKey: String, // e.g. "2026-09-14"
    val title: String,
    val description: String,
    val targetRuns: Int = 0,
    val targetSixes: Int = 0,
    val targetBoundaries: Int = 0,
    val targetPerfectHits: Int = 0,
    val mustNotGetOut: Boolean = false,
    val rewardCoins: Int = 350,
    val proTip: String = "",
    val iconEmoji: String = "⚡"
)

data class MatchSummary(
    val totalRuns: Int,
    val ballsFaced: Int,
    val fours: Int,
    val sixes: Int,
    val wickets: Int,
    val target: Int,
    val difficulty: Difficulty,
    val isTargetAchieved: Boolean,
    val baseCoins: Int,
    val bonusCoins: Int,
    val totalCoinsEarned: Int,
    val ballOutcomes: List<BallOutcome>,
    val timestamp: Long = System.currentTimeMillis(),
    val isNewHighScore: Boolean = false,
    val isDailyChallengeCompletedInThisMatch: Boolean = false,
    val dailyChallengeTitle: String? = null,
    val dailyChallengeReward: Int = 0,
    val matchOvers: Int = 1
)

data class LeaderboardEntry(
    val id: String,
    val playerName: String,
    val score: Int,
    val difficulty: String,
    val dateString: String,
    val fours: Int,
    val sixes: Int,
    val targetMet: Boolean
) {
    val runs: Int get() = score
}

enum class PowerUpType(
    val id: String,
    val title: String,
    val description: String,
    val costCoins: Int,
    val ballsDuration: Int,
    val iconEmoji: String
) {
    GOLDEN_BAT(
        id = "golden_bat",
        title = "Golden Bat",
        description = "+2 bonus runs on all hits & anti-wicket protection",
        costCoins = 40,
        ballsDuration = 2,
        iconEmoji = "⚡"
    ),
    SLOW_MOTION(
        id = "slow_motion",
        title = "Slow Motion",
        description = "Slows down the ball flight for effortless sweet-spot timing",
        costCoins = 40,
        ballsDuration = 2,
        iconEmoji = "⏱️"
    )
}
