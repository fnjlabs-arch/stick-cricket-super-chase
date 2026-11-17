package com.sadsha.stickcricket.challenge

import com.sadsha.stickcricket.model.BallOutcome
import com.sadsha.stickcricket.model.DailyChallenge
import com.sadsha.stickcricket.model.MatchSummary
import com.sadsha.stickcricket.model.TimingResult
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class DailyChallengeManager {

    fun getTodayKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun getTodayChallenge(): DailyChallenge {
        val todayKey = getTodayKey()
        val dayHash = abs(todayKey.hashCode())

        val templates = listOf(
            DailyChallenge(
                dateKey = todayKey,
                title = "Boundary Blitz",
                description = "Hit at least 3 boundaries (Fours or Sixes) in a single match.",
                targetBoundaries = 3,
                rewardCoins = 350,
                proTip = "Look for balls pitched up and drive firmly through the gaps!",
                iconEmoji = "🏏"
            ),
            DailyChallenge(
                dateKey = todayKey,
                title = "Sky High Sixes",
                description = "Smash 2 towering MAXIMUM Sixes into the stadium stands.",
                targetSixes = 2,
                rewardCoins = 400,
                proTip = "Wait until the ball reaches the golden sweet spot, then tap straight!",
                iconEmoji = "⚡"
            ),
            DailyChallenge(
                dateKey = todayKey,
                title = "Power Striker 25",
                description = "Score 25 or more runs in a single 6-ball innings.",
                targetRuns = 25,
                rewardCoins = 300,
                proTip = "Maintain a high strike rate by scoring at least 4 runs every ball.",
                iconEmoji = "🔥"
            ),
            DailyChallenge(
                dateKey = todayKey,
                title = "Wicketless Warrior",
                description = "Score 20+ runs without losing your wicket (no OUTs).",
                targetRuns = 20,
                mustNotGetOut = true,
                rewardCoins = 450,
                proTip = "Defend tricky yorkers and save your big lofted swings for slower balls.",
                iconEmoji = "🛡️"
            ),
            DailyChallenge(
                dateKey = todayKey,
                title = "The 30 Run Club",
                description = "Rack up a massive 30 runs against today's bowling attack!",
                targetRuns = 30,
                rewardCoins = 500,
                proTip = "Hit 5 Sixes or combine Fours and Sixes with perfect timing.",
                iconEmoji = "👑"
            ),
            DailyChallenge(
                dateKey = todayKey,
                title = "Timing Virtuoso",
                description = "Achieve at least 2 PERFECT timing hits in a single match.",
                targetPerfectHits = 2,
                rewardCoins = 380,
                proTip = "Tap exactly when the ball reaches the sweet spot ring at the crease.",
                iconEmoji = "🎯"
            ),
            DailyChallenge(
                dateKey = todayKey,
                title = "Boundary Master 4",
                description = "Smash 4 boundaries (Fours or Sixes) across 6 deliveries.",
                targetBoundaries = 4,
                rewardCoins = 420,
                proTip = "Use Golden Bat or Slow Motion power-ups if you need an extra edge!",
                iconEmoji = "🌟"
            )
        )

        return templates[dayHash % templates.size]
    }

    fun getTimeRemainingUntilMidnight(): String {
        val now = Calendar.getInstance()
        val midnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMs = (midnight.timeInMillis - now.timeInMillis).coerceAtLeast(0)
        val hours = (diffMs / (1000 * 60 * 60)).toInt()
        val minutes = ((diffMs % (1000 * 60 * 60)) / (1000 * 60)).toInt()
        return "${hours}h ${minutes}m"
    }

    fun getProgress(
        challenge: DailyChallenge,
        runs: Int,
        fours: Int,
        sixes: Int,
        wickets: Int,
        outcomes: List<BallOutcome>
    ): Pair<Int, Int> {
        return when {
            challenge.targetBoundaries > 0 -> {
                val current = fours + sixes
                Pair(current.coerceAtMost(challenge.targetBoundaries), challenge.targetBoundaries)
            }
            challenge.targetSixes > 0 -> {
                Pair(sixes.coerceAtMost(challenge.targetSixes), challenge.targetSixes)
            }
            challenge.targetPerfectHits > 0 -> {
                val perfects = outcomes.count { it.timing == TimingResult.PERFECT }
                Pair(perfects.coerceAtMost(challenge.targetPerfectHits), challenge.targetPerfectHits)
            }
            challenge.mustNotGetOut -> {
                if (wickets > 0) Pair(0, challenge.targetRuns)
                else Pair(runs.coerceAtMost(challenge.targetRuns), challenge.targetRuns)
            }
            challenge.targetRuns > 0 -> {
                Pair(runs.coerceAtMost(challenge.targetRuns), challenge.targetRuns)
            }
            else -> Pair(0, 1)
        }
    }

    fun isGoalMet(
        challenge: DailyChallenge,
        runs: Int,
        fours: Int,
        sixes: Int,
        wickets: Int,
        outcomes: List<BallOutcome>
    ): Boolean {
        if (challenge.targetRuns > 0 && runs < challenge.targetRuns) return false
        if (challenge.targetSixes > 0 && sixes < challenge.targetSixes) return false
        val totalBoundaries = fours + sixes
        if (challenge.targetBoundaries > 0 && totalBoundaries < challenge.targetBoundaries) return false
        if (challenge.mustNotGetOut && wickets > 0) return false
        if (challenge.targetPerfectHits > 0) {
            val perfects = outcomes.count { it.timing == TimingResult.PERFECT }
            if (perfects < challenge.targetPerfectHits) return false
        }
        return true
    }

    fun evaluateMatch(challenge: DailyChallenge, match: MatchSummary): Boolean {
        return isGoalMet(
            challenge = challenge,
            runs = match.totalRuns,
            fours = match.fours,
            sixes = match.sixes,
            wickets = match.wickets,
            outcomes = match.ballOutcomes
        )
    }
}

