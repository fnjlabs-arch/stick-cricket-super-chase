package com.sadsha.stickcricket.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.sadsha.stickcricket.model.CosmeticCategory
import com.sadsha.stickcricket.model.CosmeticItem
import com.sadsha.stickcricket.model.CosmeticsCatalog
import com.sadsha.stickcricket.ui.components.CoinBadge
import com.sadsha.stickcricket.ui.components.TopBarWithBack
import kotlinx.coroutines.launch

@Composable
fun CollectionScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val profile by viewModel.profileState.collectAsState()
    val coins = profile?.coins ?: 0
    val unlockedIds = profile?.unlockedCosmetics ?: setOf()

    val categories = CosmeticCategory.values()
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    val currentCategory = categories[selectedCategoryIndex]

    val items = when (currentCategory) {
        CosmeticCategory.BAT -> CosmeticsCatalog.BATS
        CosmeticCategory.HELMET -> CosmeticsCatalog.HELMETS
        CosmeticCategory.JERSEY -> CosmeticsCatalog.JERSEYS
        CosmeticCategory.BALL -> CosmeticsCatalog.BALLS
    }

    val selectedId = when (currentCategory) {
        CosmeticCategory.BAT -> profile?.selectedBatId ?: "bat_willow"
        CosmeticCategory.HELMET -> profile?.selectedHelmetId ?: "helmet_navy"
        CosmeticCategory.JERSEY -> profile?.selectedJerseyId ?: "jersey_blue"
        CosmeticCategory.BALL -> profile?.selectedBallId ?: "ball_red"
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .testTag("collection_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBarWithBack(
                title = "COLLECTION",
                onBack = onBack,
                trailingContent = {
                    CoinBadge(coins = coins)
                }
            )

            // Category Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedCategoryIndex,
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedCategoryIndex]),
                        color = Color(0xFFFF9800),
                        height = 3.dp
                    )
                }
            ) {
                categories.forEachIndexed { index, category ->
                    Tab(
                        selected = selectedCategoryIndex == index,
                        onClick = { selectedCategoryIndex = index },
                        text = {
                            Text(
                                text = category.title.uppercase(),
                                fontWeight = if (selectedCategoryIndex == index) FontWeight.Black else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier.testTag("tab_${category.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Items Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items) { item ->
                    val isUnlocked = item.costCoins == 0 || unlockedIds.contains(item.id)
                    val isEquipped = item.id == selectedId

                    CosmeticItemCard(
                        item = item,
                        isUnlocked = isUnlocked,
                        isEquipped = isEquipped,
                        userCoins = coins,
                        onEquip = {
                            // Golden Bat is free in v1 — equip directly, no ad.
                            scope.launch {
                                viewModel.repository.equipCosmetic(item.category, item.id)
                                viewModel.soundManager.playSound(com.sadsha.stickcricket.audio.SoundManager.SoundType.BUTTON_TAP)
                            }
                        },
                        onBuy = {
                            if (item.id == "bat_golden") {
                                // Golden Bat is free in v1 — unlock and equip directly, no ad.
                                scope.launch {
                                    viewModel.repository.unlockCosmetic(item.id, 0)
                                    viewModel.repository.equipCosmetic(item.category, item.id)
                                    viewModel.soundManager.playSound(com.sadsha.stickcricket.audio.SoundManager.SoundType.COIN_REWARD)
                                    viewModel.analyticsManager.logCosmeticUnlocked(item.id, item.category.name, 0)
                                    snackbarHostState.showSnackbar("⚡ Golden Bat Unlocked & Equipped!")
                                }
                            } else if (coins >= item.costCoins) {
                                scope.launch {
                                    val success = viewModel.repository.unlockCosmetic(item.id, item.costCoins)
                                    if (success) {
                                        viewModel.repository.equipCosmetic(item.category, item.id)
                                        viewModel.soundManager.playSound(com.sadsha.stickcricket.audio.SoundManager.SoundType.COIN_REWARD)
                                        viewModel.analyticsManager.logCosmeticUnlocked(item.id, item.category.name, item.costCoins)
                                        snackbarHostState.showSnackbar("Unlocked & equipped ${item.name}!")
                                    }
                                }
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Not enough coins! You need ${item.costCoins - coins} more.")
                                }
                            }
                        }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun CosmeticItemCard(
    item: CosmeticItem,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    userCoins: Int,
    onEquip: () -> Unit,
    onBuy: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(
            1.5.dp,
            if (isEquipped) Color(0xFF10B981) else Color(0xFF334155)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("item_card_${item.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Visual Preview Swatch
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(item.primaryColorHex),
                                Color(item.secondaryColorHex)
                            )
                        )
                    )
            ) {
                // Secondary accent ring
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(item.accentColorHex).copy(alpha = 0.85f))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.description,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                isEquipped -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.2f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "EQUIPPED",
                                color = Color(0xFF34D399),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
                isUnlocked -> {
                    Button(
                        onClick = onEquip,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("equip_button_${item.id}")
                    ) {
                        Text("EQUIP", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                else -> {
                    Button(
                        onClick = onBuy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("buy_button_${item.id}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${item.costCoins}", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
