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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadsha.stickcricket.game.GameViewModel
import com.sadsha.stickcricket.model.LeaderboardEntry
import com.sadsha.stickcricket.ui.components.TopBarWithBack

@Composable
fun LeaderboardScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val profile by viewModel.profileState.collectAsState()
    val matchHistory = profile?.matchHistory ?: emptyList()
    var selectedTab by remember { mutableStateOf(0) } // 0: Top Scores, 1: Career Stats

    val sortedByRuns = remember(matchHistory) {
        matchHistory.sortedByDescending { it.runs }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .testTag("leaderboard_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBarWithBack(
                title = "LEADERBOARD",
                onBack = onBack
            )

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFFFFD700),
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "TOP SCORES",
                            fontWeight = if (selectedTab == 0) FontWeight.Black else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.testTag("tab_top_scores")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "CAREER STATS",
                            fontWeight = if (selectedTab == 1) FontWeight.Black else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.testTag("tab_career_stats")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedTab) {
                0 -> {
                    if (sortedByRuns.isEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Leaderboard,
                                    contentDescription = null,
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No matches played yet!\nTap Play on the Home screen to set your first score.",
                                    color = Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(sortedByRuns) { index, match ->
                                LeaderboardRowItem(rank = index + 1, match = match)
                            }
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }
                1 -> {
                    // Career Stats
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CareerStatRow(label = "Matches Played", value = "${profile?.totalMatches ?: 0}")
                        CareerStatRow(label = "Career Highest Score", value = "${profile?.highScore ?: 0} Runs", isHighlight = true)
                        CareerStatRow(label = "Career Total Runs", value = "${profile?.totalRunsScored ?: 0}")
                        CareerStatRow(label = "Total Fours (4s)", value = "${profile?.totalFours ?: 0}", valueColor = Color(0xFF00E5FF))
                        CareerStatRow(label = "Total Sixes (6s)", value = "${profile?.totalSixes ?: 0}", valueColor = Color(0xFFFFD700))
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRowItem(rank: Int, match: LeaderboardEntry) {
    val rankBadgeColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> Color(0xFF334155)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, if (rank <= 3) rankBadgeColor else Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            // Rank Number
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(rankBadgeColor)
            ) {
                Text(
                    text = "$rank",
                    color = if (rank <= 3) Color.Black else Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = match.playerName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "${match.difficulty} Mode • ${match.dateString} • ${match.fours} 4s, ${match.sixes} 6s",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

            Text(
                text = "${match.runs}",
                color = Color(0xFFFFD700),
                fontWeight = FontWeight.Black,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "runs",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CareerStatRow(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    valueColor: Color = Color.White
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, if (isHighlight) Color(0xFFFFD700) else Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color(0xFFCBD5E1),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                color = if (isHighlight) Color(0xFFFFD700) else valueColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
