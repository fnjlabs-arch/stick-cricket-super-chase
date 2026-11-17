package com.sadsha.stickcricket.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadsha.stickcricket.economy.DailyRewardManager
import com.sadsha.stickcricket.game.GameViewModel
import com.sadsha.stickcricket.ui.components.CoinBadge
import com.sadsha.stickcricket.ui.components.PrimaryGameButton
import com.sadsha.stickcricket.ui.components.TopBarWithBack
import kotlinx.coroutines.launch

@Composable
fun DailyRewardScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val profile by viewModel.profileState.collectAsState()
    val rewardManager = remember { DailyRewardManager() }
    val currentDay = profile?.dailyRewardCurrentDay ?: 1
    val lastClaim = profile?.lastDailyRewardTimestamp ?: 0L
    val canClaim = rewardManager.canClaimToday(lastClaim)
    val schedule = rewardManager.getDaysRewardInfo(currentDay, lastClaim)
    val todayRewardCoins = DailyRewardManager.REWARD_SCHEDULE[(currentDay - 1).coerceIn(0, 6)]

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .testTag("daily_reward_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopBarWithBack(
                title = "DAILY REWARD",
                onBack = onBack,
                trailingContent = {
                    CoinBadge(coins = profile?.coins ?: 0)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Gift Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981).copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "7-Day Login Bonus",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Log in every day to claim increasing free coin rewards!",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 7 Days Visual Grid / Row
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // First 4 days
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    schedule.take(4).forEach { item ->
                        DayRewardTile(
                            day = item.day,
                            coins = item.coins,
                            isClaimed = item.isClaimed,
                            isAvailable = item.isAvailableToday,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Next 3 days (Day 7 is Mega reward!)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    schedule.drop(4).take(2).forEach { item ->
                        DayRewardTile(
                            day = item.day,
                            coins = item.coins,
                            isClaimed = item.isClaimed,
                            isAvailable = item.isAvailableToday,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Day 7 Mega Card
                    val day7 = schedule.last()
                    DayRewardTile(
                        day = day7.day,
                        coins = day7.coins,
                        isClaimed = day7.isClaimed,
                        isAvailable = day7.isAvailableToday,
                        isMega = true,
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            if (canClaim) {
                // Claim standard
                PrimaryGameButton(
                    text = "CLAIM +$todayRewardCoins COINS",
                    icon = Icons.Default.MonetizationOn,
                    containerColor = Color(0xFF10B981),
                    onClick = {
                        scope.launch {
                            viewModel.repository.claimDailyReward(todayRewardCoins)
                            viewModel.soundManager.playSound(com.sadsha.stickcricket.audio.SoundManager.SoundType.COIN_REWARD)
                            viewModel.analyticsManager.logDailyRewardClaimed(currentDay, todayRewardCoins)
                            snackbarHostState.showSnackbar("Claimed $todayRewardCoins coins!")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "claim_daily_reward_button"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Double reward — free in v1, no ad
                Button(
                    onClick = {
                        scope.launch {
                            val doubleCoins = todayRewardCoins * 2
                            viewModel.repository.claimDailyReward(doubleCoins)
                            viewModel.soundManager.playSound(com.sadsha.stickcricket.audio.SoundManager.SoundType.COIN_REWARD)
                            viewModel.analyticsManager.logDailyRewardClaimed(currentDay, doubleCoins)
                            snackbarHostState.showSnackbar("DOUBLE CLAIM! Received $doubleCoins coins!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("double_daily_reward_button")
                ) {
                    Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Double Reward (${todayRewardCoins * 2} Coins) — Free", fontWeight = FontWeight.Bold)
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Today's reward already claimed!\nCome back tomorrow for Day ${if (currentDay >= 7) 1 else currentDay + 1} reward.",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun DayRewardTile(
    day: Int,
    coins: Int,
    isClaimed: Boolean,
    isAvailable: Boolean,
    isMega: Boolean = false,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isAvailable -> Color(0xFFFFD700)
        isClaimed -> Color(0xFF10B981)
        else -> Color(0xFF334155)
    }
    val bgColor = when {
        isAvailable -> Color(0xFF1E293B)
        isClaimed -> Color(0xFF064E3B).copy(alpha = 0.4f)
        else -> Color(0xFF0F172A)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(if (isAvailable) 2.dp else 1.dp, borderColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "DAY $day",
                color = if (isMega) Color(0xFFFFD700) else Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Icon(
                imageVector = when {
                    isClaimed -> Icons.Default.Check
                    isAvailable -> Icons.Default.MonetizationOn
                    else -> Icons.Default.Lock
                },
                contentDescription = null,
                tint = when {
                    isClaimed -> Color(0xFF10B981)
                    isAvailable -> Color(0xFFFFD700)
                    else -> Color(0xFF64748B)
                },
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$coins",
                color = Color.White,
                fontSize = if (isMega) 14.sp else 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
