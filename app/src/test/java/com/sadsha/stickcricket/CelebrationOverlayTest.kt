package com.sadsha.stickcricket

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sadsha.stickcricket.ui.components.CelebrationOverlay
import com.sadsha.stickcricket.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class CelebrationOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun celebration_overlay_renders_victory_state() {
        var dismissed = false
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            MyApplicationTheme {
                CelebrationOverlay(
                    isHighScore = false,
                    isVictory = true,
                    runs = 28,
                    target = 25,
                    onDismiss = { dismissed = true }
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(500)

        composeTestRule.onNodeWithTag("celebration_overlay").assertIsDisplayed()
        composeTestRule.onNodeWithText("VICTORY!").assertIsDisplayed()
        composeTestRule.onNodeWithText("MATCH WINNER!").assertIsDisplayed()
        composeTestRule.onNodeWithTag("celebration_continue_button").assertIsDisplayed()

        // Test dismiss button
        composeTestRule.onNodeWithTag("celebration_continue_button").performClick()
        assertTrue("Dismiss callback should be invoked", dismissed)
    }

    @Test
    fun celebration_overlay_renders_high_score_state() {
        var dismissed = false
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            MyApplicationTheme {
                CelebrationOverlay(
                    isHighScore = true,
                    isVictory = true,
                    runs = 36,
                    target = 30,
                    onDismiss = { dismissed = true }
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(500)

        composeTestRule.onNodeWithTag("celebration_overlay").assertIsDisplayed()
        composeTestRule.onNodeWithText("DOUBLE TRIUMPH!").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dismiss_celebration_button").assertIsDisplayed()

        // Test dismiss icon
        composeTestRule.onNodeWithTag("dismiss_celebration_button").performClick()
        assertTrue("Dismiss callback should be invoked on close button click", dismissed)
    }
}
