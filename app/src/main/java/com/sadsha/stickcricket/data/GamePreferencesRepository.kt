package com.sadsha.stickcricket.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sadsha.stickcricket.model.CosmeticCategory
import com.sadsha.stickcricket.model.CosmeticsCatalog
import com.sadsha.stickcricket.model.Difficulty
import com.sadsha.stickcricket.model.LeaderboardEntry
import com.sadsha.stickcricket.model.PowerUpType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "game_preferences")

data class UserGameProfile(
    val coins: Int,
    val highScore: Int,
    val unlockedCosmetics: Set<String>,
    val selectedBatId: String,
    val selectedHelmetId: String,
    val selectedJerseyId: String,
    val selectedBallId: String,
    val tutorialCompleted: Boolean,
    val soundEnabled: Boolean,
    val musicEnabled: Boolean,
    val hapticsEnabled: Boolean,
    val difficulty: Difficulty,
    val playerName: String,
    val matchesPlayed: Int,
    val totalRuns: Int,
    val totalFours: Int,
    val totalSixes: Int,
    val totalWickets: Int,
    val dailyRewardDay: Int,
    val lastRewardClaimTimestamp: Long,
    val lastChallengeCompletedDate: String,
    val lastChallengeClaimedDate: String,
    val challengeStreak: Int = 0,
    val leaderboardEntries: List<LeaderboardEntry>,
    val goldenBatCount: Int = 2,
    val slowMotionCount: Int = 2
) {
    val completedDailyChallengeDate: String get() = lastChallengeCompletedDate
    val dailyRewardCurrentDay: Int get() = dailyRewardDay
    val lastDailyRewardTimestamp: Long get() = lastRewardClaimTimestamp
    val matchHistory: List<LeaderboardEntry> get() = leaderboardEntries
    val totalMatches: Int get() = matchesPlayed
    val totalRunsScored: Int get() = totalRuns
}

class GamePreferencesRepository(private val context: Context) {

    private object Keys {
        val COINS = intPreferencesKey("coins")
        val HIGH_SCORE = intPreferencesKey("high_score")
        val UNLOCKED_COSMETICS = stringSetPreferencesKey("unlocked_cosmetics")
        val SELECTED_BAT = stringPreferencesKey("selected_bat")
        val SELECTED_HELMET = stringPreferencesKey("selected_helmet")
        val SELECTED_JERSEY = stringPreferencesKey("selected_jersey")
        val SELECTED_BALL = stringPreferencesKey("selected_ball")
        val TUTORIAL_COMPLETED = booleanPreferencesKey("tutorial_completed")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val DIFFICULTY = stringPreferencesKey("difficulty")
        val PLAYER_NAME = stringPreferencesKey("player_name")
        val MATCHES_PLAYED = intPreferencesKey("matches_played")
        val TOTAL_RUNS = intPreferencesKey("total_runs")
        val TOTAL_FOURS = intPreferencesKey("total_fours")
        val TOTAL_SIXES = intPreferencesKey("total_sixes")
        val TOTAL_WICKETS = intPreferencesKey("total_wickets")
        val DAILY_REWARD_DAY = intPreferencesKey("daily_reward_day")
        val LAST_REWARD_CLAIM_TIME = longPreferencesKey("last_reward_claim_time")
        val LAST_CHALLENGE_COMPLETED_DATE = stringPreferencesKey("last_challenge_completed_date")
        val LAST_CHALLENGE_CLAIMED_DATE = stringPreferencesKey("last_challenge_claimed_date")
        val CHALLENGE_STREAK = intPreferencesKey("challenge_streak")
        val LEADERBOARD_RAW = stringPreferencesKey("leaderboard_raw")
        val GOLDEN_BAT_COUNT = intPreferencesKey("golden_bat_count")
        val SLOW_MOTION_COUNT = intPreferencesKey("slow_motion_count")
    }

    private val defaultUnlocked = setOf(
        CosmeticsCatalog.getDefault(CosmeticCategory.BAT).id,
        CosmeticsCatalog.getDefault(CosmeticCategory.HELMET).id,
        CosmeticsCatalog.getDefault(CosmeticCategory.JERSEY).id,
        CosmeticsCatalog.getDefault(CosmeticCategory.BALL).id
    )

    val profileFlow: Flow<UserGameProfile> = context.dataStore.data.map { prefs ->
        val coins = prefs[Keys.COINS] ?: 150 // Welcome bonus of 150 coins
        val highScore = prefs[Keys.HIGH_SCORE] ?: 0
        val unlocked = prefs[Keys.UNLOCKED_COSMETICS] ?: defaultUnlocked
        val bat = prefs[Keys.SELECTED_BAT] ?: "bat_classic"
        val helmet = prefs[Keys.SELECTED_HELMET] ?: "helmet_navy"
        val jersey = prefs[Keys.SELECTED_JERSEY] ?: "jersey_white"
        val ball = prefs[Keys.SELECTED_BALL] ?: "ball_red"
        val tutorial = prefs[Keys.TUTORIAL_COMPLETED] ?: true
        val sound = prefs[Keys.SOUND_ENABLED] ?: true
        val music = prefs[Keys.MUSIC_ENABLED] ?: true
        val haptics = prefs[Keys.HAPTICS_ENABLED] ?: true
        val diffName = prefs[Keys.DIFFICULTY] ?: Difficulty.NORMAL.name
        val difficulty = runCatching { Difficulty.valueOf(diffName) }.getOrDefault(Difficulty.NORMAL)
        val defaultName = "Player${Random.nextInt(1000, 9999)}"
        val playerName = prefs[Keys.PLAYER_NAME] ?: defaultName
        val matches = prefs[Keys.MATCHES_PLAYED] ?: 0
        val totalRuns = prefs[Keys.TOTAL_RUNS] ?: 0
        val totalFours = prefs[Keys.TOTAL_FOURS] ?: 0
        val totalSixes = prefs[Keys.TOTAL_SIXES] ?: 0
        val totalWickets = prefs[Keys.TOTAL_WICKETS] ?: 0
        val rewardDay = prefs[Keys.DAILY_REWARD_DAY] ?: 1
        val lastClaim = prefs[Keys.LAST_REWARD_CLAIM_TIME] ?: 0L
        val challengeComp = prefs[Keys.LAST_CHALLENGE_COMPLETED_DATE] ?: ""
        val challengeClaim = prefs[Keys.LAST_CHALLENGE_CLAIMED_DATE] ?: ""
        val challengeStreak = prefs[Keys.CHALLENGE_STREAK] ?: 0
        val lbRaw = prefs[Keys.LEADERBOARD_RAW] ?: ""
        val parsedLb = parseLeaderboard(lbRaw, playerName)
        val goldenBat = prefs[Keys.GOLDEN_BAT_COUNT] ?: 2
        val slowMotion = prefs[Keys.SLOW_MOTION_COUNT] ?: 2

        UserGameProfile(
            coins = coins,
            highScore = highScore,
            unlockedCosmetics = unlocked + defaultUnlocked,
            selectedBatId = bat,
            selectedHelmetId = helmet,
            selectedJerseyId = jersey,
            selectedBallId = ball,
            tutorialCompleted = tutorial,
            soundEnabled = sound,
            musicEnabled = music,
            hapticsEnabled = haptics,
            difficulty = difficulty,
            playerName = playerName,
            matchesPlayed = matches,
            totalRuns = totalRuns,
            totalFours = totalFours,
            totalSixes = totalSixes,
            totalWickets = totalWickets,
            dailyRewardDay = rewardDay,
            lastRewardClaimTimestamp = lastClaim,
            lastChallengeCompletedDate = challengeComp,
            lastChallengeClaimedDate = challengeClaim,
            challengeStreak = challengeStreak,
            leaderboardEntries = parsedLb,
            goldenBatCount = goldenBat,
            slowMotionCount = slowMotion
        )
    }

    suspend fun addCoins(amount: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.COINS] ?: 150
            prefs[Keys.COINS] = (current + amount).coerceAtLeast(0)
        }
    }

    suspend fun setTutorialCompleted(completed: Boolean = true) {
        context.dataStore.edit { it[Keys.TUTORIAL_COMPLETED] = completed }
    }

    suspend fun updatePlayerName(name: String) = setPlayerName(name)

    suspend fun resetAllData() = resetAllProgress()

    suspend fun unlockCosmetic(itemId: String, costCoins: Int): Boolean {
        var success = false
        context.dataStore.edit { prefs ->
            val currentCoins = prefs[Keys.COINS] ?: 150
            if (currentCoins >= costCoins) {
                prefs[Keys.COINS] = currentCoins - costCoins
                val currentUnlocked = (prefs[Keys.UNLOCKED_COSMETICS] ?: defaultUnlocked).toMutableSet()
                currentUnlocked.add(itemId)
                prefs[Keys.UNLOCKED_COSMETICS] = currentUnlocked
                val item = CosmeticsCatalog.findById(itemId)
                if (item != null) {
                    when (item.category) {
                        CosmeticCategory.BAT -> prefs[Keys.SELECTED_BAT] = item.id
                        CosmeticCategory.HELMET -> prefs[Keys.SELECTED_HELMET] = item.id
                        CosmeticCategory.JERSEY -> prefs[Keys.SELECTED_JERSEY] = item.id
                        CosmeticCategory.BALL -> prefs[Keys.SELECTED_BALL] = item.id
                    }
                }
                success = true
            }
        }
        return success
    }

    suspend fun equipCosmetic(category: CosmeticCategory, itemId: String) {
        context.dataStore.edit { prefs ->
            when (category) {
                CosmeticCategory.BAT -> prefs[Keys.SELECTED_BAT] = itemId
                CosmeticCategory.HELMET -> prefs[Keys.SELECTED_HELMET] = itemId
                CosmeticCategory.JERSEY -> prefs[Keys.SELECTED_JERSEY] = itemId
                CosmeticCategory.BALL -> prefs[Keys.SELECTED_BALL] = itemId
            }
        }
    }

    suspend fun claimDailyReward(rewardCoins: Int) {
        context.dataStore.edit { prefs ->
            val currentCoins = prefs[Keys.COINS] ?: 150
            prefs[Keys.COINS] = currentCoins + rewardCoins
            prefs[Keys.LAST_REWARD_CLAIM_TIME] = System.currentTimeMillis()
            val day = prefs[Keys.DAILY_REWARD_DAY] ?: 1
            val nextDay = if (day >= 7) 1 else day + 1
            prefs[Keys.DAILY_REWARD_DAY] = nextDay
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MUSIC_ENABLED] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setDifficulty(difficulty: Difficulty) {
        context.dataStore.edit { it[Keys.DIFFICULTY] = difficulty.name }
    }

    suspend fun setPlayerName(name: String) {
        if (name.isNotBlank()) {
            context.dataStore.edit { it[Keys.PLAYER_NAME] = name.trim().take(16) }
        }
    }

    suspend fun unlockCosmetic(item: com.sadsha.stickcricket.model.CosmeticItem): Boolean {
        var success = false
        context.dataStore.edit { prefs ->
            val currentCoins = prefs[Keys.COINS] ?: 150
            if (currentCoins >= item.price) {
                prefs[Keys.COINS] = currentCoins - item.price
                val currentUnlocked = (prefs[Keys.UNLOCKED_COSMETICS] ?: defaultUnlocked).toMutableSet()
                currentUnlocked.add(item.id)
                prefs[Keys.UNLOCKED_COSMETICS] = currentUnlocked
                // Automatically equip newly unlocked item
                when (item.category) {
                    CosmeticCategory.BAT -> prefs[Keys.SELECTED_BAT] = item.id
                    CosmeticCategory.HELMET -> prefs[Keys.SELECTED_HELMET] = item.id
                    CosmeticCategory.JERSEY -> prefs[Keys.SELECTED_JERSEY] = item.id
                    CosmeticCategory.BALL -> prefs[Keys.SELECTED_BALL] = item.id
                }
                success = true
            }
        }
        return success
    }

    suspend fun equipCosmetic(item: com.sadsha.stickcricket.model.CosmeticItem) {
        context.dataStore.edit { prefs ->
            when (item.category) {
                CosmeticCategory.BAT -> prefs[Keys.SELECTED_BAT] = item.id
                CosmeticCategory.HELMET -> prefs[Keys.SELECTED_HELMET] = item.id
                CosmeticCategory.JERSEY -> prefs[Keys.SELECTED_JERSEY] = item.id
                CosmeticCategory.BALL -> prefs[Keys.SELECTED_BALL] = item.id
            }
        }
    }

    suspend fun usePowerUp(type: PowerUpType): Boolean {
        var success = false
        context.dataStore.edit { prefs ->
            val key = when (type) {
                PowerUpType.GOLDEN_BAT -> Keys.GOLDEN_BAT_COUNT
                PowerUpType.SLOW_MOTION -> Keys.SLOW_MOTION_COUNT
            }
            val current = prefs[key] ?: 2
            if (current > 0) {
                prefs[key] = current - 1
                success = true
            }
        }
        return success
    }

    suspend fun addPowerUp(type: PowerUpType, amount: Int = 1) {
        context.dataStore.edit { prefs ->
            val key = when (type) {
                PowerUpType.GOLDEN_BAT -> Keys.GOLDEN_BAT_COUNT
                PowerUpType.SLOW_MOTION -> Keys.SLOW_MOTION_COUNT
            }
            val current = prefs[key] ?: 2
            prefs[key] = (current + amount).coerceAtLeast(0)
        }
    }

    suspend fun buyPowerUpWithCoins(type: PowerUpType, costCoins: Int = 40, amount: Int = 2): Boolean {
        var success = false
        context.dataStore.edit { prefs ->
            val coins = prefs[Keys.COINS] ?: 150
            if (coins >= costCoins) {
                prefs[Keys.COINS] = coins - costCoins
                val key = when (type) {
                    PowerUpType.GOLDEN_BAT -> Keys.GOLDEN_BAT_COUNT
                    PowerUpType.SLOW_MOTION -> Keys.SLOW_MOTION_COUNT
                }
                val current = prefs[key] ?: 2
                prefs[key] = current + amount
                success = true
            }
        }
        return success
    }

    suspend fun recordMatch(
        runs: Int,
        fours: Int,
        sixes: Int,
        wickets: Int,
        difficulty: Difficulty,
        targetMet: Boolean,
        coinsEarned: Int,
        playerName: String
    ) {
        context.dataStore.edit { prefs ->
            val currentCoins = prefs[Keys.COINS] ?: 150
            prefs[Keys.COINS] = currentCoins + coinsEarned

            val currentHighScore = prefs[Keys.HIGH_SCORE] ?: 0
            if (runs > currentHighScore) {
                prefs[Keys.HIGH_SCORE] = runs
            }

            prefs[Keys.MATCHES_PLAYED] = (prefs[Keys.MATCHES_PLAYED] ?: 0) + 1
            prefs[Keys.TOTAL_RUNS] = (prefs[Keys.TOTAL_RUNS] ?: 0) + runs
            prefs[Keys.TOTAL_FOURS] = (prefs[Keys.TOTAL_FOURS] ?: 0) + fours
            prefs[Keys.TOTAL_SIXES] = (prefs[Keys.TOTAL_SIXES] ?: 0) + sixes
            prefs[Keys.TOTAL_WICKETS] = (prefs[Keys.TOTAL_WICKETS] ?: 0) + wickets

            // Update leaderboard
            val rawLb = prefs[Keys.LEADERBOARD_RAW] ?: ""
            val list = parseLeaderboard(rawLb, playerName).toMutableList()
            val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
            val newEntry = LeaderboardEntry(
                id = System.currentTimeMillis().toString(),
                playerName = playerName,
                score = runs,
                difficulty = difficulty.title,
                dateString = dateFormat.format(Date()),
                fours = fours,
                sixes = sixes,
                targetMet = targetMet
            )
            list.add(newEntry)
            val sorted = list.sortedByDescending { it.score }.take(15)
            prefs[Keys.LEADERBOARD_RAW] = serializeLeaderboard(sorted)
        }
    }

    suspend fun claimDailyReward(rewardDay: Int, rewardCoins: Int) {
        context.dataStore.edit { prefs ->
            val currentCoins = prefs[Keys.COINS] ?: 150
            prefs[Keys.COINS] = currentCoins + rewardCoins
            prefs[Keys.LAST_REWARD_CLAIM_TIME] = System.currentTimeMillis()
            val nextDay = if (rewardDay >= 7) 1 else rewardDay + 1
            prefs[Keys.DAILY_REWARD_DAY] = nextDay
        }
    }

    suspend fun markDailyChallengeCompleted(todayKey: String) {
        context.dataStore.edit { prefs ->
            val lastCompleted = prefs[Keys.LAST_CHALLENGE_COMPLETED_DATE] ?: ""
            if (lastCompleted != todayKey) {
                val prevStreak = prefs[Keys.CHALLENGE_STREAK] ?: 0
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val cal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
                val yesterday = sdf.format(cal.time)
                val newStreak = if (lastCompleted == yesterday) prevStreak + 1 else 1
                prefs[Keys.CHALLENGE_STREAK] = newStreak
            }
            prefs[Keys.LAST_CHALLENGE_COMPLETED_DATE] = todayKey
        }
    }

    suspend fun claimDailyChallengeReward(todayKey: String, rewardCoins: Int) {
        context.dataStore.edit { prefs ->
            val currentCoins = prefs[Keys.COINS] ?: 150
            prefs[Keys.COINS] = currentCoins + rewardCoins
            prefs[Keys.LAST_CHALLENGE_CLAIMED_DATE] = todayKey
        }
    }

    suspend fun resetAllProgress() {
        context.dataStore.edit { prefs ->
            prefs.clear()
            prefs[Keys.COINS] = 150
            prefs[Keys.UNLOCKED_COSMETICS] = defaultUnlocked
            prefs[Keys.SELECTED_BAT] = "bat_classic"
            prefs[Keys.SELECTED_HELMET] = "helmet_navy"
            prefs[Keys.SELECTED_JERSEY] = "jersey_white"
            prefs[Keys.SELECTED_BALL] = "ball_red"
        }
    }

    private fun serializeLeaderboard(entries: List<LeaderboardEntry>): String {
        return entries.joinToString(";;") {
            "${it.id}::${it.playerName}::${it.score}::${it.difficulty}::${it.dateString}::${it.fours}::${it.sixes}::${it.targetMet}"
        }
    }

    private fun parseLeaderboard(raw: String, currentPlayer: String): List<LeaderboardEntry> {
        if (raw.isBlank()) {
            return generateDefaultLeaderboard(currentPlayer)
        }
        return try {
            raw.split(";;").mapNotNull { line ->
                val p = line.split("::")
                if (p.size >= 8) {
                    LeaderboardEntry(
                        id = p[0],
                        playerName = p[1],
                        score = p[2].toIntOrNull() ?: 0,
                        difficulty = p[3],
                        dateString = p[4],
                        fours = p[5].toIntOrNull() ?: 0,
                        sixes = p[6].toIntOrNull() ?: 0,
                        targetMet = p[7].toBooleanStrictOrNull() ?: false
                    )
                } else null
            }.ifEmpty { generateDefaultLeaderboard(currentPlayer) }
        } catch (e: Exception) {
            generateDefaultLeaderboard(currentPlayer)
        }
    }

    private fun generateDefaultLeaderboard(currentPlayer: String): List<LeaderboardEntry> {
        return listOf(
            LeaderboardEntry("1", "SixerSam", 36, "Hard", "Yesterday", 0, 6, true),
            LeaderboardEntry("2", "BladeMaster", 32, "Normal", "2 days ago", 2, 4, true),
            LeaderboardEntry("3", currentPlayer, 24, "Normal", "Today", 3, 2, true),
            LeaderboardEntry("4", "YorkerKing", 22, "Hard", "3 days ago", 1, 3, false),
            LeaderboardEntry("5", "StickFinisher", 18, "Easy", "5 days ago", 3, 1, true)
        )
    }
}
