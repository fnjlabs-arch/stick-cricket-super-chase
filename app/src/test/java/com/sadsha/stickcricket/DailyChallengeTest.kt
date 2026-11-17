package com.sadsha.stickcricket

import com.sadsha.stickcricket.challenge.DailyChallengeManager
import com.sadsha.stickcricket.model.BallOutcome
import com.sadsha.stickcricket.model.DeliveryType
import com.sadsha.stickcricket.model.Difficulty
import com.sadsha.stickcricket.model.MatchSummary
import com.sadsha.stickcricket.model.OutcomeType
import com.sadsha.stickcricket.model.TimingResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DailyChallengeTest {

    private lateinit var challengeManager: DailyChallengeManager

    @Before
    fun setUp() {
        challengeManager = DailyChallengeManager()
    }

    @Test
    fun getTodayChallenge_returnsValidChallenge() {
        val challenge = challengeManager.getTodayChallenge()
        assertNotNull(challenge)
        assertTrue(challenge.title.isNotBlank())
        assertTrue(challenge.description.isNotBlank())
        assertTrue(challenge.rewardCoins >= 250)
        assertTrue(challenge.proTip.isNotBlank())
    }

    @Test
    fun evaluateMatch_boundaryMaster_succeedsWithSufficientBoundaries() {
        val challenge = challengeManager.getTodayChallenge().copy(
            dateKey = "2026-09-14",
            title = "Boundary Master",
            targetBoundaries = 3,
            targetSixes = 0,
            targetRuns = 0,
            mustNotGetOut = false,
            targetPerfectHits = 0
        )

        val successSummary = MatchSummary(
            totalRuns = 24,
            ballsFaced = 6,
            fours = 2,
            sixes = 1,
            wickets = 0,
            target = 20,
            difficulty = Difficulty.NORMAL,
            isTargetAchieved = true,
            baseCoins = 50,
            bonusCoins = 50,
            totalCoinsEarned = 100,
            ballOutcomes = emptyList()
        )

        assertTrue(challengeManager.evaluateMatch(challenge, successSummary))

        val failSummary = successSummary.copy(fours = 1, sixes = 0)
        assertFalse(challengeManager.evaluateMatch(challenge, failSummary))
    }

    @Test
    fun getTimeRemainingUntilMidnight_returnsFormattedTime() {
        val remaining = challengeManager.getTimeRemainingUntilMidnight()
        assertNotNull(remaining)
        assertTrue(remaining.contains("h") && remaining.contains("m"))
    }
}
