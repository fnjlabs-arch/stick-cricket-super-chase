package com.sadsha.stickcricket.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadsha.stickcricket.game.GameViewModel
import com.sadsha.stickcricket.model.BallOutcome
import com.sadsha.stickcricket.model.DeliveryType
import com.sadsha.stickcricket.model.OutcomeType
import com.sadsha.stickcricket.model.TimingResult
import com.sadsha.stickcricket.ui.components.CelebrationOverlay
import com.sadsha.stickcricket.ui.components.PrimaryGameButton
import com.sadsha.stickcricket.ui.components.SecondaryGameButton

@Composable
fun ResultScreen(
    viewModel: GameViewModel,
    onPlayAgain: () -> Unit,
    onGoHome: () -> Unit
) {
    val context = LocalContext.current
    val gameState by viewModel.gameState.collectAsState()
    val summary = gameState.matchSummary

    var isDoubled by remember { mutableStateOf(false) }

    val runs = summary?.totalRuns ?: gameState.score
    val target = summary?.target ?: gameState.target
    val isTargetAchieved = summary?.isTargetAchieved ?: (runs >= target)
    val fours = summary?.fours ?: gameState.foursCount
    val sixes = summary?.sixes ?: gameState.sixesCount
    val baseCoins = summary?.totalCoinsEarned ?: 50
    val totalCoins = if (isDoubled) baseCoins * 2 else baseCoins

    val profile by viewModel.profileState.collectAsState()
    val isHighScore = summary?.isNewHighScore ?: (runs > (profile?.highScore ?: 0) && runs > 0)

    val todayChallenge = remember { viewModel.challengeManager.getTodayChallenge() }
    val isChallengeCompleted = profile?.completedDailyChallengeDate == todayChallenge.dateKey
    val isChallengeClaimed = profile?.lastChallengeClaimedDate == todayChallenge.dateKey
    val canClaimChallenge = isChallengeCompleted && !isChallengeClaimed
    val wasChallengeCompletedThisMatch = summary?.isDailyChallengeCompletedInThisMatch == true
    var showCelebration by remember(summary) {
        mutableStateOf(isTargetAchieved || isHighScore)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF132E22),
                        Color(0xFF0B1F16)
                    )
                )
            )
            .testTag("result_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // TARGET STATUS BANNER
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isTargetAchieved) Color(0xFF10B981) else Color(0xFFEF4444)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (isTargetAchieved) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTargetAchieved) "TARGET ACHIEVED!" else "CHALLENGE FAILED",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            if (isTargetAchieved || isHighScore) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x33FFD700),
                    border = BorderStroke(1.dp, Color(0x80FFD700)),
                    modifier = Modifier
                        .clickable { showCelebration = true }
                        .testTag("replay_celebration_chip")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isHighScore && isTargetAchieved) "🏆 NEW RECORD & WIN! (TAP FOR CELEBRATION 🎉)"
                            else if (isHighScore) "🔥 NEW HIGH SCORE! (TAP FOR CELEBRATION 🎉)"
                            else "🎉 MATCH WON! (TAP FOR CELEBRATION 🏆)",
                            color = Color(0xFFFFD700),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SCORE HIGHLIGHT
            Text(
                text = "$runs",
                fontSize = 76.sp,
                fontWeight = FontWeight.Black,
                color = if (isTargetAchieved) Color(0xFFFFD700) else Color.White
            )
            Text(
                text = "RUNS SCORED IN 6 BALLS",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp
            )
            Text(
                text = "Target was $target runs (${gameState.difficulty.title})",
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // STATS BREAKDOWN CARD
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(label = "FOURS", value = "$fours", valueColor = Color(0xFF00E5FF))
                        StatItem(label = "SIXES", value = "$sixes", valueColor = Color(0xFFFFD700))
                        StatItem(label = "WICKETS", value = "${summary?.wickets ?: gameState.wicketsCount}", valueColor = Color(0xFFEF4444))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF334155)))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Coins Breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Coins Earned",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            text = "+$totalCoins",
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // DOUBLE COINS BUTTON (free in v1)
            if (!isDoubled) {
                Button(
                    onClick = {
                        viewModel.doubleMatchCoins {
                            isDoubled = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("double_coins_button")
                ) {
                    Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Double Coins (+$baseCoins) — Free", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // DAILY CHALLENGE RESULT BANNER
            if (wasChallengeCompletedThisMatch || isChallengeCompleted) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (canClaimChallenge) Color(0xFF1E293B) else Color(0xFF132E22)
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        if (canClaimChallenge) Color(0xFFFFD700) else Color(0xFF10B981)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("result_daily_challenge_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = "🎯", fontSize = 26.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (wasChallengeCompletedThisMatch) "CHALLENGE CRUSHED!" else "DAILY CHALLENGE",
                                        color = if (canClaimChallenge) Color(0xFFFFD700) else Color(0xFF34D399),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = todayChallenge.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            Text(
                                text = "+${todayChallenge.rewardCoins} 🪙",
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }

                        if (canClaimChallenge) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.claimDailyChallengeReward() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB703)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("claim_challenge_reward_result_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CLAIM +${todayChallenge.rewardCoins} COINS NOW",
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }
                        } else if (isChallengeClaimed) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "✅ Reward claimed!",
                                color = Color(0xFF34D399),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

        }

        // FLOATING ACTION BAR: PLAY AGAIN BUTTON FLOATING AT BOTTOM
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("floating_play_again_bar"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xF20F172A),
            border = BorderStroke(1.5.dp, Color(0xFFFF6D00).copy(alpha = 0.8f)),
            shadowElevation = 16.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Home Button
                IconButton(
                    onClick = onGoHome,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .testTag("result_home_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = Color.White
                    )
                }

                // Floating Play Again Primary Button
                val matchOvers = summary?.matchOvers ?: gameState.matchOvers
                PrimaryGameButton(
                    text = "PLAY AGAIN ($matchOvers OVERS)",
                    icon = Icons.Default.PlayArrow,
                    containerColor = Color(0xFFFF6D00),
                    onClick = {
                        // Crucial fix: Let onPlayAgain() show the ad first.
                        // The game starts only AFTER the ad finishes/is closed.
                        onPlayAgain()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    testTag = "play_again_button"
                )

                // Share Button
                IconButton(
                    onClick = {
                        viewModel.analyticsManager.logShareClicked(runs)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "🏏 I scored $runs runs in $matchOvers Over${if (matchOvers > 1) "s" else ""} in Stick Cricket Super Chase! ($fours Fours, $sixes Sixes). Can you beat my score?"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Score"))
                    },
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .testTag("share_score_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color(0xFF38BDF8)
                    )
                }
            }
        }

        // CELEBRATION OVERLAY (Confetti & Dynamic Pop-ups on Win or High Score)
        if (showCelebration) {
            CelebrationOverlay(
                isHighScore = isHighScore,
                isVictory = isTargetAchieved,
                runs = runs,
                target = target,
                onDismiss = { showCelebration = false }
            )
        }

    }
}

@Composable
private fun StatItem(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = valueColor, fontSize = 24.sp, fontWeight = FontWeight.Black)
    }
}

/**
 * Sponsored advertisement card displayed at match end.
 * Replaces the tedious ball-by-ball breakdown with an engaging, interactive ad unit
 * featuring a video ad simulation and rewarded video bonus (+100 coins).
 */
