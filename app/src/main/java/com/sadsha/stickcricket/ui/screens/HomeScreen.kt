package com.sadsha.stickcricket.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadsha.stickcricket.R
import com.sadsha.stickcricket.data.UserGameProfile
import com.sadsha.stickcricket.model.CosmeticsCatalog
import com.sadsha.stickcricket.model.Difficulty
import com.sadsha.stickcricket.ui.components.CoinBadge
import com.sadsha.stickcricket.ui.components.CricketPitchHeroVisual
import com.sadsha.stickcricket.ui.components.PrimaryGameButton

@Composable
fun HomeScreen(
    profile: UserGameProfile?,
    selectedOvers: Int = 1,
    onOversSelected: (Int) -> Unit = {},
    onPlayClicked: () -> Unit,
    onDailyChallengeClicked: () -> Unit,
    onLeaderboardClicked: () -> Unit,
    onCollectionClicked: () -> Unit,
    onDailyRewardClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onClaimBonusCoins: (() -> Unit)? = null
) {
    val coins = profile?.coins ?: 150
    val highScore = profile?.highScore ?: 0
    val difficulty = profile?.difficulty ?: Difficulty.NORMAL
    val playerName = profile?.playerName ?: "Player"

    val challengeManager = remember { com.sadsha.stickcricket.challenge.DailyChallengeManager() }
    val todayChallenge = remember { challengeManager.getTodayChallenge() }
    val isChallengeCompleted = profile?.completedDailyChallengeDate == todayChallenge.dateKey
    val isChallengeClaimed = profile?.lastChallengeClaimedDate == todayChallenge.dateKey
    val challengeSubtitle = when {
        isChallengeClaimed -> "Completed ✅"
        isChallengeCompleted -> "Claim Reward! 🎁"
        else -> "${todayChallenge.title} (+${todayChallenge.rewardCoins})"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Dark slate
                        Color(0xFF132E22), // Deep stadium emerald
                        Color(0xFF0B1F16)
                    )
                )
            )
            .testTag("home_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // TOP BAR: Coin Balance + Settings Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoinBadge(coins = coins, onClick = onDailyRewardClicked)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "BEST: $highScore",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onSettingsClicked,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // CRICKET STADIUM HERO ARENA CARD
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.5.dp, Color(0xFFFF9800).copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cricket_stadium_hero_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                ) {
                    // Stadium cover background
                    Image(
                        painter = painterResource(id = R.drawable.img_cricket_stadium_cover),
                        contentDescription = "Cricket Stadium",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Dark gradient scrim
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xCC0F172A),
                                        Color(0x440F172A),
                                        Color(0xF00B171F)
                                    )
                                )
                            )
                    )

                    // Dynamic Cricket Canvas (Pitch, Wickets, Seamed Ball)
                    CricketPitchHeroVisual(
                        modifier = Modifier.fillMaxSize(),
                        isAnimated = true
                    )

                    // Overlay Badges & Title
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xDD0F172A),
                            border = BorderStroke(1.dp, Color(0xFFFF9800))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsCricket,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "FLOODLIT T20 STADIUM",
                                    color = Color(0xFFFFD54F),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$selectedOvers-OVER STICK CRICKET",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            val batName = CosmeticsCatalog.BATS.find { it.id == profile?.selectedBatId }?.name ?: "Classic Willow"
                            Text(
                                text = "$playerName • ${difficulty.title} • Bat: $batName",
                                fontSize = 12.sp,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // MATCH LENGTH SELECTOR (1, 2, or 3 Overs)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("overs_selector_surface")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MATCH FORMAT",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = when (selectedOvers) {
                                1 -> "QUICK BLAST (6 Balls)"
                                2 -> "T20 THRILLER (12 Balls)"
                                else -> "SUPER CHASE (18 Balls)"
                            },
                            color = Color(0xFFFFD700),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(1, "1 OVER", "6 Balls • Fast"),
                            Triple(2, "2 OVERS", "12 Balls • Popular"),
                            Triple(3, "3 OVERS", "18 Balls • Super Chase")
                        ).forEach { (overs, title, desc) ->
                            val isSelected = selectedOvers == overs
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFFFF6D00) else Color(0xFF0F172A),
                                border = BorderStroke(
                                    1.8.dp,
                                    if (isSelected) Color(0xFFFFD700) else Color(0xFF334155)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onOversSelected(overs) }
                                    .testTag("overs_option_$overs")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (overs == 2 || overs == 3) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isSelected) Color(0xFFFFD700) else Color(0xFF1E293B)
                                        ) {
                                            Text(
                                                text = if (overs == 2) "POPULAR" else "SUPER",
                                                color = if (isSelected) Color.Black else Color(0xFF38BDF8),
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                    Text(
                                        text = title,
                                        color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = desc,
                                        color = if (isSelected) Color(0xFFFFF3E0) else Color(0xFF64748B),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // LARGE PLAY BUTTON (Visually dominant!)
            PrimaryGameButton(
                text = "PLAY $selectedOvers OVER${if (selectedOvers > 1) "S" else ""} MATCH",
                icon = Icons.Default.PlayArrow,
                containerColor = Color(0xFFFF6D00),
                onClick = onPlayClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                testTag = "play_match_button"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 4 FEATURE TILES: DAILY CHALLENGE, LEADERBOARD, COLLECTION, DAILY REWARD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeFeatureCard(
                    title = "DAILY CHALLENGE",
                    subtitle = challengeSubtitle,
                    icon = Icons.Default.FlashOn,
                    iconTint = if (isChallengeCompleted && !isChallengeClaimed) Color(0xFFFFD700) else Color(0xFFFFD600),
                    modifier = Modifier.weight(1f),
                    onClick = onDailyChallengeClicked,
                    testTag = "home_daily_challenge"
                )
                HomeFeatureCard(
                    title = "LEADERBOARD",
                    subtitle = "Rankings & Stats",
                    icon = Icons.Default.Leaderboard,
                    iconTint = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f),
                    onClick = onLeaderboardClicked,
                    testTag = "home_leaderboard"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeFeatureCard(
                    title = "COLLECTION",
                    subtitle = "Bats, Helmets, Gear",
                    icon = Icons.Default.ShoppingBag,
                    iconTint = Color(0xFFA855F7),
                    modifier = Modifier.weight(1f),
                    onClick = onCollectionClicked,
                    testTag = "home_collection"
                )
                HomeFeatureCard(
                    title = "DAILY REWARD",
                    subtitle = "Free Coin Gifts",
                    icon = Icons.Default.CardGiftcard,
                    iconTint = Color(0xFF10B981),
                    modifier = Modifier.weight(1f),
                    onClick = onDailyRewardClicked,
                    testTag = "home_daily_reward"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // FREE COIN BONUS CARD (free in v1 — no ads)
            if (onClaimBonusCoins != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F2B48),
                    border = BorderStroke(1.2.dp, Color(0xFF0284C7)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClaimBonusCoins() }
                        .testTag("home_bonus_coins_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "FREE COIN BONUS",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Tap to claim +150 free coins",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0284C7)
                        ) {
                            Text(
                                text = "+150 🪙",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun HomeFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = modifier
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
        }
    }
}
