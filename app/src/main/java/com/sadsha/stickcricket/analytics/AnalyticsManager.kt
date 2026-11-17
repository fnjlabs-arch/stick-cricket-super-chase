package com.sadsha.stickcricket.analytics

import android.os.Bundle
import android.util.Log

class AnalyticsManager {

    companion object {
        private const val TAG = "StickCricketAnalytics"
    }

    fun logEvent(eventName: String, params: Map<String, Any> = emptyMap()) {
        val paramString = params.entries.joinToString(", ") { "${it.key}=${it.value}" }
        Log.d(TAG, "Event: $eventName | Params: [$paramString]")

        // Analytics are local-only: events are written to logcat for debugging.
        // No analytics SDK is integrated and no event ever leaves the device.
    }

    fun logAppOpen() = logEvent("app_open")

    fun logTutorialStarted() = logEvent("tutorial_started")

    fun logTutorialCompleted() = logEvent("tutorial_completed")

    fun logGameStarted(difficulty: String, target: Int) = logEvent(
        "game_started",
        mapOf("difficulty" to difficulty, "target" to target)
    )

    fun logBallPlayed(ballNumber: Int, outcome: String, runs: Int, timing: String) = logEvent(
        "ball_played",
        mapOf("ball_number" to ballNumber, "outcome" to outcome, "runs" to runs, "timing" to timing)
    )

    fun logFourHit() = logEvent("four_hit")

    fun logSixHit() = logEvent("six_hit")

    fun logWicket(ballNumber: Int) = logEvent("wicket", mapOf("ball_number" to ballNumber))

    fun logGameCompleted(score: Int, targetAchieved: Boolean, difficulty: String) = logEvent(
        "game_completed",
        mapOf("score" to score, "target_achieved" to targetAchieved, "difficulty" to difficulty)
    )

    fun logGameRestarted() = logEvent("game_restarted")

    fun logDailyChallengeStarted(dateKey: String) = logEvent("daily_challenge_started", mapOf("date" to dateKey))

    fun logDailyChallengeCompleted(dateKey: String, rewardCoins: Int) = logEvent(
        "daily_challenge_completed",
        mapOf("date" to dateKey, "reward" to rewardCoins)
    )

    fun logDailyRewardClaimed(day: Int, coins: Int) = logEvent(
        "daily_reward_claimed",
        mapOf("day" to day, "coins" to coins)
    )

    fun logCosmeticUnlocked(itemId: String, category: String, cost: Int) = logEvent(
        "cosmetic_unlocked",
        mapOf("item_id" to itemId, "category" to category, "cost" to cost)
    )

    fun logShareClicked(score: Int) = logEvent("share_clicked", mapOf("score" to score))

    fun logPowerUpUsed(powerUp: String) = logEvent("power_up_used", mapOf("type" to powerUp))
}
