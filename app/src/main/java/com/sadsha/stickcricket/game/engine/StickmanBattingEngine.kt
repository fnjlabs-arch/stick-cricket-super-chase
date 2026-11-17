package com.sadsha.stickcricket.game.engine

import androidx.compose.ui.geometry.Offset
import com.sadsha.stickcricket.game.BatsmanPose
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural Skeletal Rig for the Stickman Batsman.
 * Encapsulates all 2D bone joints, bat geometry, and dynamic batting animation poses.
 */
data class StickmanJoints(
    val headCenter: Offset,
    val headRadius: Float,
    val neck: Offset,
    val hips: Offset,
    val shoulderLeft: Offset,
    val shoulderRight: Offset,
    val elbowLeft: Offset,
    val elbowRight: Offset,
    val hands: Offset,
    val batGripStart: Offset,
    val batBladeEnd: Offset,
    val batAngleDegrees: Float,
    val hipLeft: Offset,
    val hipRight: Offset,
    val kneeLeft: Offset,
    val kneeRight: Offset,
    val footLeft: Offset,
    val footRight: Offset,
    val torsoLeanX: Float,
    val torsoLeanY: Float,
    val swingArcAngle: Float = 0f
)

class StickmanBattingEngine {

    /**
     * Calculates the animated 2D skeletal joints of the stickman batsman for any pose and progress.
     *
     * @param baseX World anchor position X (centered at batting crease)
     * @param baseY World anchor position Y (pitch surface at batting crease)
     * @param pose Active batsman animation pose
     * @param poseProgress Animation transition progress [0.0f, 1.0f]
     * @param dynamicSwingAngle Additional fine-grain swing angle from user swipe/timing
     * @param unitScale Responsive canvas scale unit
     * @return StickmanJoints containing all bone coordinates ready for drawing
     */
    fun calculateJoints(
        baseX: Float,
        baseY: Float,
        pose: BatsmanPose,
        poseProgress: Float,
        dynamicSwingAngle: Float = 0f,
        unitScale: Float = 1.0f
    ): StickmanJoints {
        val s = unitScale * 0.65f

        // 1. Base stance joint targets
        val stanceLeanX = 0f
        val stanceLeanY = 0f
        val stanceHands = Offset(baseX + 3f * s, baseY - 1f * s)
        val stanceBatAngle = 18f

        // 2. Pose-specific kinematic targets
        val targetLeanX: Float
        val targetLeanY: Float
        val targetHands: Offset
        val targetBatAngle: Float

        when (pose) {
            BatsmanPose.STANCE -> {
                targetLeanX = 0f
                targetLeanY = 0f
                targetHands = stanceHands
                targetBatAngle = stanceBatAngle
            }
            BatsmanPose.BACKLIFT -> {
                // High backlift: bat cocked behind shoulder, slight back foot weight transfer
                targetLeanX = 2.5f * s
                targetLeanY = -1.5f * s
                targetHands = Offset(baseX + 8.5f * s, baseY - 8.5f * s)
                targetBatAngle = -38f
            }
            BatsmanPose.SWING_OFF -> {
                // Front foot cover drive: stepping towards off stump, high left elbow
                targetLeanX = -8.5f * s
                targetLeanY = 1.2f * s
                targetHands = Offset(baseX - 12.5f * s, baseY - 6f * s)
                targetBatAngle = 52f + dynamicSwingAngle * 0.5f
            }
            BatsmanPose.SWING_STRAIGHT -> {
                // Straight drive / Lofted 6: arms extended down the pitch, full vertical face
                targetLeanX = 0f
                targetLeanY = 3.2f * s
                targetHands = Offset(baseX, baseY + 2.5f * s)
                targetBatAngle = 180f + dynamicSwingAngle * 0.4f
            }
            BatsmanPose.SWING_LEG -> {
                // Pull / Hook shot: horizontal bat rolled across mid-wicket
                targetLeanX = 8.5f * s
                targetLeanY = 1.2f * s
                targetHands = Offset(baseX + 12.5f * s, baseY - 6f * s)
                targetBatAngle = -52f - dynamicSwingAngle * 0.5f
            }
            BatsmanPose.DEFENSIVE -> {
                // Forward defensive: soft hands, bat close to front pad
                targetLeanX = -1.2f * s
                targetLeanY = 2.2f * s
                targetHands = Offset(baseX - 1.2f * s, baseY + 4.2f * s)
                targetBatAngle = 8f
            }
            BatsmanPose.BOWLED -> {
                // Dismay: slumped shoulders, bat dropped forward
                targetLeanX = 3.2f * s
                targetLeanY = 4.2f * s
                targetHands = Offset(baseX - 3.2f * s, baseY + 10.5f * s)
                targetBatAngle = 56f
            }
            BatsmanPose.CELEBRATION -> {
                // Triumphant salute: bat raised high to the pavilion
                targetLeanX = 0f
                targetLeanY = -6.5f * s
                targetHands = Offset(baseX + 4.2f * s, baseY - 25f * s)
                targetBatAngle = -115f
            }
        }

        // Interpolation between stance and target pose
        val t = if (pose == BatsmanPose.STANCE) 1f else poseProgress.coerceIn(0f, 1f)
        // Smooth easing curve
        val smoothT = smoothStep(t)

        val leanX = lerp(stanceLeanX, targetLeanX, smoothT)
        val leanY = lerp(stanceLeanY, targetLeanY, smoothT)
        val hands = Offset(
            lerp(stanceHands.x, targetHands.x, smoothT),
            lerp(stanceHands.y, targetHands.y, smoothT)
        )
        val finalBatAngle = lerp(stanceBatAngle, targetBatAngle, smoothT)

        // Torso and head coordinates
        val neck = Offset(baseX + leanX, baseY - 18f * s + leanY)
        val hipY = baseY + 4f * s + leanY * 0.5f
        val hips = Offset(baseX + leanX * 0.5f, hipY)

        val headRadius = 11f * s
        val headCenter = Offset(baseX + leanX, baseY - 30f * s + leanY)

        // Arms & Shoulders
        val shoulderLeft = Offset(baseX - 5f * s + leanX, baseY - 16f * s + leanY)
        val shoulderRight = Offset(baseX + 5f * s + leanX, baseY - 16f * s + leanY)

        // Elbows calculate naturally along the arm vector
        val elbowLeft = Offset(
            (shoulderLeft.x + hands.x) * 0.5f - 3f * s,
            (shoulderLeft.y + hands.y) * 0.5f
        )
        val elbowRight = Offset(
            (shoulderRight.x + hands.x) * 0.5f + 3f * s,
            (shoulderRight.y + hands.y) * 0.5f
        )

        // Lower body (Legs, Knees, Feet)
        val hipLeft = Offset(hips.x - 5f * s, hips.y)
        val hipRight = Offset(hips.x + 5f * s, hips.y)

        val kneeLeft = Offset(baseX - 6f * s + leanX * 0.3f, baseY + 14f * s)
        val kneeRight = Offset(baseX + 6f * s + leanX * 0.3f, baseY + 14f * s)

        val footLeft = Offset(baseX - 7f * s, baseY + 26f * s)
        val footRight = Offset(baseX + 7f * s, baseY + 26f * s)

        // Bat blade endpoints from hands and bat angle
        val rad = Math.toRadians(finalBatAngle.toDouble())
        val handleLength = 12f * s
        val bladeLength = 32f * s

        // Bat handle extends upward from hands
        val batGripStart = Offset(
            (hands.x - sin(rad) * handleLength).toFloat(),
            (hands.y - cos(rad) * handleLength).toFloat()
        )
        // Bat blade extends downward to toe
        val batBladeEnd = Offset(
            (hands.x + sin(rad) * bladeLength).toFloat(),
            (hands.y + cos(rad) * bladeLength).toFloat()
        )

        return StickmanJoints(
            headCenter = headCenter,
            headRadius = headRadius,
            neck = neck,
            hips = hips,
            shoulderLeft = shoulderLeft,
            shoulderRight = shoulderRight,
            elbowLeft = elbowLeft,
            elbowRight = elbowRight,
            hands = hands,
            batGripStart = batGripStart,
            batBladeEnd = batBladeEnd,
            batAngleDegrees = finalBatAngle,
            hipLeft = hipLeft,
            hipRight = hipRight,
            kneeLeft = kneeLeft,
            kneeRight = kneeRight,
            footLeft = footLeft,
            footRight = footRight,
            torsoLeanX = leanX,
            torsoLeanY = leanY,
            swingArcAngle = finalBatAngle
        )
    }

    private fun lerp(start: Float, stop: Float, amount: Float): Float {
        val clamped = amount.coerceIn(0f, 1f)
        return start + (stop - start) * clamped
    }

    private fun smoothStep(t: Float): Float {
        val c = t.coerceIn(0f, 1f)
        return c * c * (3f - 2f * c)
    }
}
