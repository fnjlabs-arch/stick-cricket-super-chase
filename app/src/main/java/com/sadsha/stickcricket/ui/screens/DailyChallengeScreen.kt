package com.sadsha.stickcricket.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.sadsha.stickcricket.game.GameViewModel
import com.sadsha.stickcricket.ui.components.CoinBadge
import com.sadsha.stickcricket.ui.components.PrimaryGameButton
import com.sadsha.stickcricket.ui.components.SecondaryGameButton
import com.sadsha.stickcricket.ui.components.TopBarWithBack

@Composable
fun DailyChallengeScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
    onPlayChallengeMatch: () -> Unit
) {
    val profile by viewModel.profileState.collectAsState()
    val challenge = viewModel.challengeManager.getTodayChallenge()
    val isCompleted = profile?.completedDailyChallengeDate == challenge.dateKey
    val isClaimed = profile?.lastChallengeClaimedDate == challenge.dateKey
    val canClaim = isCompleted && !isClaimed
    val streak = profile?.challengeStreak ?: 0
    val timeRemaining = viewModel.challengeManager.getTimeRemainingUntilMidnight()

    var showClaimCelebration by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF0F172A)
                    )
                )
            )
            .testTag("daily_challenge_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopBarWithBack(
                title = "DAILY CHALLENGE",
                onBack = onBack,
                trailingContent = {
                    CoinBadge(coins = profile?.coins ?: 0)
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Challenge Badge & Emoji Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFB703).copy(alpha = 0.35f),
                                Color(0xFFFB8500).copy(alpha = 0.10f)
                            )
                        )
                    )
            ) {
                Text(
                    text = challenge.iconEmoji,
                    fontSize = 42.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = challenge.title,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Time Remaining & Streak Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x3338BDF8),
                    border = BorderStroke(1.dp, Color(0x6638BDF8))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "New in $timeRemaining",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (streak > 0) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x33F97316),
                        border = BorderStroke(1.dp, Color(0x66F97316))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFFB923C),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$streak Day Streak",
                                color = Color(0xFFFB923C),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Challenge Mission Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(
                    1.5.dp,
                    when {
                        isClaimed -> Color(0xFF10B981)
                        canClaim -> Color(0xFFFFD700)
                        else -> Color(0xFFF59E0B)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status Chip
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when {
                            isClaimed -> Color(0xFF10B981).copy(alpha = 0.2f)
                            canClaim -> Color(0xFFFFD700).copy(alpha = 0.2f)
                            else -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    isClaimed -> Icons.Default.CheckCircle
                                    canClaim -> Icons.Default.CardGiftcard
                                    else -> Icons.Default.Star
                                },
                                contentDescription = null,
                                tint = when {
                                    isClaimed -> Color(0xFF10B981)
                                    canClaim -> Color(0xFFFFD700)
                                    else -> Color(0xFFF59E0B)
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isClaimed -> "REWARD CLAIMED"
                                    canClaim -> "COMPLETED - CLAIM REWARD!"
                                    else -> "ACTIVE TODAY"
                                },
                                color = when {
                                    isClaimed -> Color(0xFF34D399)
                                    canClaim -> Color(0xFFFFD700)
                                    else -> Color(0xFFFBBF24)
                                },
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = challenge.description,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Reward Tag
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFFFFD700))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "+${challenge.rewardCoins} Coins",
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp
                            )
                        }
                    }

                    // Claim Reward Button (when completed but unclaimed)
                    if (canClaim) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.claimDailyChallengeReward {
                                    showClaimCelebration = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB703)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("claim_challenge_reward_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "CLAIM +${challenge.rewardCoins} COINS",
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    // Claimed Confirmation Toast
                    AnimatedVisibility(
                        visible = showClaimCelebration || isClaimed,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Text(
                                text = "🎉 Challenge Completed & Reward Claimed!",
                                color = Color(0xFF34D399),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Pro Tip Card
            if (challenge.proTip.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.8f)),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "PRO STRATEGY",
                                color = Color(0xFFFFD700),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = challenge.proTip,
                                color = Color(0xFFCBD5E1),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // PLAY BUTTON
            PrimaryGameButton(
                text = if (isCompleted) "PLAY BONUS MATCH" else "ATTEMPT CHALLENGE NOW",
                icon = Icons.Default.PlayArrow,
                onClick = {
                    viewModel.analyticsManager.logDailyChallengeStarted(challenge.dateKey)
                    viewModel.startNewMatch()
                    onPlayChallengeMatch()
                },
                modifier = Modifier.fillMaxWidth(),
                containerColor = if (isCompleted) Color(0xFF2563EB) else Color(0xFFFF8A00),
                testTag = "start_challenge_button"
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
