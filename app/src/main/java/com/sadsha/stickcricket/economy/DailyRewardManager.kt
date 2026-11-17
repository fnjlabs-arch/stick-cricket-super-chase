package com.sadsha.stickcricket.economy

import java.util.Calendar
import java.util.Date

data class DayRewardInfo(
    val day: Int,
    val coins: Int,
    val isClaimed: Boolean,
    val isAvailableToday: Boolean,
    val isUpcoming: Boolean
)

class DailyRewardManager {

    companion object {
        val REWARD_SCHEDULE = listOf(100, 150, 200, 300, 400, 500, 1000)
    }

    fun canClaimToday(lastClaimTimestamp: Long): Boolean {
        if (lastClaimTimestamp <= 0L) return true

        val now = Calendar.getInstance()
        val last = Calendar.getInstance().apply { time = Date(lastClaimTimestamp) }

        val sameDay = now.get(Calendar.YEAR) == last.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == last.get(Calendar.DAY_OF_YEAR)

        return !sameDay
    }

    fun getDaysRewardInfo(currentDay: Int, lastClaimTimestamp: Long): List<DayRewardInfo> {
        val canClaim = canClaimToday(lastClaimTimestamp)
        return (1..7).map { day ->
            val coins = REWARD_SCHEDULE[day - 1]
            when {
                day < currentDay -> DayRewardInfo(day, coins, isClaimed = true, isAvailableToday = false, isUpcoming = false)
                day == currentDay -> {
                    if (canClaim) {
                        DayRewardInfo(day, coins, isClaimed = false, isAvailableToday = true, isUpcoming = false)
                    } else {
                        DayRewardInfo(day, coins, isClaimed = true, isAvailableToday = false, isUpcoming = false)
                    }
                }
                else -> DayRewardInfo(day, coins, isClaimed = false, isAvailableToday = false, isUpcoming = true)
            }
        }
    }
}
