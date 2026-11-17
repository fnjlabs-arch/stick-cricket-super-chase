package com.sadsha.stickcricket.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sadsha.stickcricket.analytics.AnalyticsManager
import com.sadsha.stickcricket.audio.SoundManager
import com.sadsha.stickcricket.challenge.DailyChallengeManager
import com.sadsha.stickcricket.data.GamePreferencesRepository
import com.sadsha.stickcricket.data.UserGameProfile
import com.sadsha.stickcricket.haptics.HapticManager
import com.sadsha.stickcricket.model.CosmeticCategory
import com.sadsha.stickcricket.model.CosmeticsCatalog
import com.sadsha.stickcricket.model.Difficulty
import com.sadsha.stickcricket.model.MatchSummary
import com.sadsha.stickcricket.model.OutcomeType
import com.sadsha.stickcricket.model.PowerUpType
import com.sadsha.stickcricket.model.ShotDirection
import com.sadsha.stickcricket.model.TimingResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

    val repository = GamePreferencesRepository(application)
    val soundManager = SoundManager.getInstance(application)
    val hapticManager = HapticManager(application)
    val analyticsManager = AnalyticsManager()
    val challengeManager = DailyChallengeManager()
    val gameEngine = GameEngine()

    val profileState: StateFlow<UserGameProfile?> = repository.profileFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    private val defaultBat = CosmeticsCatalog.getDefault(CosmeticCategory.BAT)
    private val defaultHelmet = CosmeticsCatalog.getDefault(CosmeticCategory.HELMET)
    private val defaultJersey = CosmeticsCatalog.getDefault(CosmeticCategory.JERSEY)
    private val defaultBall = CosmeticsCatalog.getDefault(CosmeticCategory.BALL)

    private val _gameState = MutableStateFlow(
        GameState(
            equippedBat = defaultBat,
            equippedHelmet = defaultHelmet,
            equippedJersey = defaultJersey,
            equippedBall = defaultBall
        )
    )
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _selectedMatchOvers = MutableStateFlow(1) // 1, 2, or 3
    val selectedMatchOvers: StateFlow<Int> = _selectedMatchOvers.asStateFlow()

    private var ballAnimationJob: Job? = null
    private var isShotRegisteredForCurrentBall = false

    fun setMatchOvers(overs: Int) {
        _selectedMatchOvers.value = overs.coerceIn(1, 3)
    }

    init {
        viewModelScope.launch {
            repository.profileFlow.collect { profile ->
                soundManager.isSoundEnabled = profile.soundEnabled
                soundManager.isMusicEnabled = profile.musicEnabled
                hapticManager.isHapticsEnabled = profile.hapticsEnabled

                val bat = CosmeticsCatalog.findById(profile.selectedBatId) ?: defaultBat
                val helmet = CosmeticsCatalog.findById(profile.selectedHelmetId) ?: defaultHelmet
                val jersey = CosmeticsCatalog.findById(profile.selectedJerseyId) ?: defaultJersey
                val ball = CosmeticsCatalog.findById(profile.selectedBallId) ?: defaultBall

                _gameState.update {
                    it.copy(
                        equippedBat = bat,
                        equippedHelmet = helmet,
                        equippedJersey = jersey,
                        equippedBall = ball
                    )
                }
            }
        }
    }

    fun startNewMatch(customDifficulty: Difficulty? = null, customOvers: Int? = null) {
        ballAnimationJob?.cancel()
        val profile = profileState.value
        val difficulty = customDifficulty ?: profile?.difficulty ?: Difficulty.NORMAL
        val overs = customOvers ?: _selectedMatchOvers.value

        val baseTargetMin = difficulty.targetMin * overs
        val baseTargetMax = difficulty.targetMax * overs
        val target = Random.nextInt(baseTargetMin, baseTargetMax + 1)
        val maxBalls = overs * 6

        val bat = profile?.let { CosmeticsCatalog.findById(it.selectedBatId) } ?: defaultBat
        val helmet = profile?.let { CosmeticsCatalog.findById(it.selectedHelmetId) } ?: defaultHelmet
        val jersey = profile?.let { CosmeticsCatalog.findById(it.selectedJerseyId) } ?: defaultJersey
        val ball = profile?.let { CosmeticsCatalog.findById(it.selectedBallId) } ?: defaultBall

        analyticsManager.logGameStarted("${difficulty.title} (${overs} Overs)", target)

        _gameState.value = GameState(
            score = 0,
            ballsFaced = 0,
            maxBalls = maxBalls,
            target = target,
            matchOvers = overs,
            difficulty = difficulty,
            equippedBat = bat,
            equippedHelmet = helmet,
            equippedJersey = jersey,
            equippedBall = ball
        )

        prepareNextBall()
    }

    private fun prepareNextBall() {
        if (_gameState.value.isGameOver || _gameState.value.isPaused) return

        val nextBallNumber = _gameState.value.ballsFaced + 1
        if (nextBallNumber > _gameState.value.maxBalls) {
            finishMatch()
            return
        }

        val delivery = gameEngine.pickDelivery(_gameState.value.difficulty, nextBallNumber)
        isShotRegisteredForCurrentBall = false

        _gameState.update {
            it.copy(
                currentDelivery = delivery,
                ballProgress = 0f,
                ballSideDrift = 0f,
                isBowlerRunUp = false,
                isBallInFlight = false,
                batsmanSwingAngle = 0f,
                showBannerFeedback = false,
                bowlerPhase = BowlerPhase.READY,
                bowlerRunUpProgress = 0f,
                batsmanPose = BatsmanPose.STANCE,
                batsmanPoseProgress = 0f,
                cameraShakeX = 0f,
                cameraShakeY = 0f,
                contactFlashProgress = 0f,
                hitExitProgress = 0f
            )
        }

        ballAnimationJob?.cancel()
        ballAnimationJob = viewModelScope.launch {
            // 1. Ready phase (pause at bowling mark for anticipation)
            _gameState.update { it.copy(bowlerPhase = BowlerPhase.READY, isBowlerRunUp = false) }
            delay(250)
            if (_gameState.value.isPaused) return@launch

            // 2. Run-up phase (approaching crease with smooth stride cycles)
            _gameState.update { it.copy(bowlerPhase = BowlerPhase.RUN_UP, isBowlerRunUp = true) }
            val runUpDurationMs = 550L
            val runUpStart = System.currentTimeMillis()
            while (System.currentTimeMillis() - runUpStart < runUpDurationMs) {
                if (_gameState.value.isPaused) { delay(50); continue }
                val p = (System.currentTimeMillis() - runUpStart).toFloat() / runUpDurationMs
                _gameState.update { it.copy(bowlerRunUpProgress = p) }
                delay(16)
            }

            // 3. Delivery stride (front foot plant, gathering arm)
            _gameState.update { it.copy(bowlerPhase = BowlerPhase.DELIVERY_STRIDE) }
            delay(160)
            if (_gameState.value.isPaused) return@launch

            // 4. Ball Release snap (high arm windmill whip)
            _gameState.update {
                it.copy(
                    bowlerPhase = BowlerPhase.RELEASE,
                    isBowlerRunUp = false
                )
            }
            delay(75)
            if (_gameState.value.isPaused) return@launch

            // 5. Follow-through and Ball In Flight
            _gameState.update {
                it.copy(
                    bowlerPhase = BowlerPhase.FOLLOW_THROUGH,
                    isBallInFlight = true
                )
            }

            // Ball flight duration based on delivery and difficulty (tuned for playable, arcade reaction time)
            val baseFlightMs = 2000L
            val speedFactor = _gameState.value.difficulty.speedMultiplier * delivery.speedScale
            val isSlowMo = _gameState.value.isSlowMotionActive
            val slowMoMultiplier = if (isSlowMo) 1.85f else 1.0f
            val totalFlightMs = ((baseFlightMs / speedFactor) * slowMoMultiplier).toLong().coerceIn(1600L, 4500L)

            val startTime = System.currentTimeMillis()
            var currentProgress = 0f

            while (currentProgress < 1.05f) {
                if (_gameState.value.isPaused) {
                    delay(50)
                    continue
                }

                val elapsed = System.currentTimeMillis() - startTime
                val rawNormalizedTime = (elapsed.toFloat() / totalFlightMs).coerceIn(0f, 1.15f)

                // Piecewise linear interpolation velocity curve for natural, readable flight
                currentProgress = GameEngine.calculateBallProgress(rawNormalizedTime, delivery.pitchBouncePoint)

                val drift = if (delivery.swingCurve != 0f) {
                    sin(currentProgress * Math.PI.toFloat()) * delivery.swingCurve * 40f
                } else 0f

                val isBacklift = currentProgress > 0.32f && !isShotRegisteredForCurrentBall
                val batsmanPose = if (isBacklift) BatsmanPose.BACKLIFT else _gameState.value.batsmanPose
                val poseProgress = if (isBacklift) {
                    ((currentProgress - 0.32f) / 0.38f).coerceIn(0f, 1f)
                } else {
                    _gameState.value.batsmanPoseProgress
                }

                _gameState.update {
                    it.copy(
                        ballProgress = currentProgress,
                        ballSideDrift = drift,
                        batsmanPose = batsmanPose,
                        batsmanPoseProgress = poseProgress
                    )
                }

                if (isShotRegisteredForCurrentBall) {
                    // Shot has already been played
                    break
                }

                delay(16) // ~60fps step
            }

            // If player didn't tap before ball crossed stumps
            if (!isShotRegisteredForCurrentBall && !_gameState.value.isGameOver && !_gameState.value.isPaused) {
                handleMissedBall()
            }
        }
    }

    fun playShot(
        direction: ShotDirection,
        tapXRatio: Float? = null,
        tapYRatio: Float? = null
    ) {
        val state = _gameState.value
        if (state.isGameOver || state.isPaused || !state.isBallInFlight || isShotRegisteredForCurrentBall) {
            return
        }

        isShotRegisteredForCurrentBall = true

        // Ball's current normalized on-screen position (perspective pitch: top is 0.22, bottom is 0.76)
        val ballXRatio = 0.50f + (state.ballSideDrift / 360f) * state.ballProgress
        val ballYRatio = 0.76f - (0.54f * state.ballProgress)

        val timingAndStrength = gameEngine.calculateTimingAndStrength(
            ballProgress = state.ballProgress,
            difficulty = state.difficulty,
            tapXRatio = tapXRatio,
            tapYRatio = tapYRatio,
            ballXRatio = ballXRatio,
            ballYRatio = ballYRatio,
            isGoldenBat = state.isGoldenBatActive
        )

        val ballNumber = state.ballsFaced + 1
        val outcome = gameEngine.resolveShot(
            ballNumber = ballNumber,
            timing = timingAndStrength.timing,
            direction = direction,
            delivery = state.currentDelivery,
            difficulty = state.difficulty,
            isGoldenBat = state.isGoldenBatActive,
            shotStrength = timingAndStrength.shotStrength
        )

        _gameState.update {
            it.copy(
                lastShotStrength = timingAndStrength.shotStrength,
                lastPrecisionRating = timingAndStrength.ratingText,
                lastTapXRatio = tapXRatio ?: -1f,
                lastTapYRatio = tapYRatio ?: -1f,
                tapFeedbackProgress = if (tapXRatio != null) 1.0f else 0.85f
            )
        }

        applyShotOutcome(outcome)
    }

    /**
     * Touch-based shot handler: calculates shot direction & precision from the touch tap coordinates!
     * @param tapX Pixel X of user tap on canvas
     * @param tapY Pixel Y of user tap on canvas
     * @param canvasWidth Width of the canvas in pixels
     * @param canvasHeight Height of the canvas in pixels
     */
    fun handleTouchShot(tapX: Float, tapY: Float, canvasWidth: Float, canvasHeight: Float) {
        if (canvasWidth <= 0f || canvasHeight <= 0f) return
        val tapXRatio = (tapX / canvasWidth).coerceIn(0f, 1f)
        val tapYRatio = (tapY / canvasHeight).coerceIn(0f, 1f)

        // Calculate direction based on tap X relative to pitch center:
        // Left 35%: Off-side cut/drive
        // Center 30%: Straight loft
        // Right 35%: Leg-side pull/flick
        val direction = when {
            tapXRatio < 0.35f -> ShotDirection.LEFT
            tapXRatio > 0.65f -> ShotDirection.RIGHT
            else -> ShotDirection.LOFT
        }

        playShot(direction = direction, tapXRatio = tapXRatio, tapYRatio = tapYRatio)
    }

    private fun handleMissedBall() {
        isShotRegisteredForCurrentBall = true
        val state = _gameState.value
        val ballNumber = state.ballsFaced + 1
        val outcome = gameEngine.resolveShot(
            ballNumber = ballNumber,
            timing = TimingResult.MISS,
            direction = ShotDirection.LOFT,
            delivery = state.currentDelivery,
            difficulty = state.difficulty,
            isGoldenBat = state.isGoldenBatActive,
            shotStrength = 0.12f
        )

        _gameState.update {
            it.copy(
                lastShotStrength = 0.12f,
                lastPrecisionRating = "BEATEN! 12%",
                lastTapXRatio = -1f,
                lastTapYRatio = -1f,
                tapFeedbackProgress = 0f
            )
        }

        applyShotOutcome(outcome)
    }

    private fun applyShotOutcome(outcome: com.sadsha.stickcricket.model.BallOutcome) {
        val swingAngle = when (outcome.direction) {
            ShotDirection.LEFT -> -42f
            ShotDirection.LOFT -> -20f
            ShotDirection.RIGHT -> 38f
        }

        val pose = when {
            outcome.isWicket -> BatsmanPose.BOWLED
            outcome.timing == TimingResult.MISS -> BatsmanPose.DEFENSIVE
            outcome.direction == ShotDirection.LEFT -> BatsmanPose.SWING_OFF
            outcome.direction == ShotDirection.LOFT -> BatsmanPose.SWING_STRAIGHT
            else -> BatsmanPose.SWING_LEG
        }

        // Haptics & Audio
        when (outcome.outcomeType) {
            OutcomeType.SIX -> {
                soundManager.playSound(SoundManager.SoundType.PERFECT_HIT)
                soundManager.playSound(SoundManager.SoundType.SIX)
                hapticManager.vibrateSix()
                analyticsManager.logSixHit()
            }
            OutcomeType.FOUR -> {
                soundManager.playSound(SoundManager.SoundType.BAT_HIT)
                soundManager.playSound(SoundManager.SoundType.FOUR)
                hapticManager.vibrateHit()
                analyticsManager.logFourHit()
            }
            OutcomeType.WICKET -> {
                soundManager.playSound(SoundManager.SoundType.WICKET)
                hapticManager.vibrateWicket()
                analyticsManager.logWicket(outcome.ballIndex)
            }
            else -> {
                if (outcome.timing == TimingResult.PERFECT) {
                    soundManager.playSound(SoundManager.SoundType.PERFECT_HIT)
                    hapticManager.vibratePerfect()
                } else if (outcome.runs > 0) {
                    soundManager.playSound(SoundManager.SoundType.BAT_HIT)
                    hapticManager.vibrateHit()
                }
            }
        }

        analyticsManager.logBallPlayed(
            ballNumber = outcome.ballIndex,
            outcome = outcome.outcomeType.label,
            runs = outcome.runs,
            timing = outcome.timing.displayName
        )

        val newScore = _gameState.value.score + outcome.runs
        val newBallsFaced = _gameState.value.ballsFaced + 1
        val newFours = _gameState.value.foursCount + if (outcome.outcomeType == OutcomeType.FOUR) 1 else 0
        val newSixes = _gameState.value.sixesCount + if (outcome.outcomeType == OutcomeType.SIX) 1 else 0
        val newWickets = _gameState.value.wicketsCount + if (outcome.isWicket) 1 else 0
        val updatedOutcomes = _gameState.value.ballOutcomes + outcome

        val todayChallenge = challengeManager.getTodayChallenge()
        val isChallengeGoalMet = challengeManager.isGoalMet(
            challenge = todayChallenge,
            runs = newScore,
            fours = newFours,
            sixes = newSixes,
            wickets = newWickets,
            outcomes = updatedOutcomes
        )
        if (!_gameState.value.isDailyChallengeTargetMet && isChallengeGoalMet) {
            hapticManager.vibrateSix()
        }

        val nextGoldenBatRemaining = if (_gameState.value.goldenBatBallsRemaining > 0) {
            _gameState.value.goldenBatBallsRemaining - 1
        } else 0

        val nextSlowMotionRemaining = if (_gameState.value.slowMotionBallsRemaining > 0) {
            _gameState.value.slowMotionBallsRemaining - 1
        } else 0

        _gameState.update {
            it.copy(
                score = newScore,
                ballsFaced = newBallsFaced,
                foursCount = newFours,
                sixesCount = newSixes,
                wicketsCount = newWickets,
                lastOutcome = outcome,
                ballOutcomes = updatedOutcomes,
                batsmanSwingAngle = swingAngle,
                batsmanPose = pose,
                batsmanPoseProgress = 0f,
                isBallInFlight = false,
                showBannerFeedback = true,
                contactFlashProgress = if (outcome.runs > 0 || outcome.isWicket) 1.0f else 0f,
                goldenBatBallsRemaining = nextGoldenBatRemaining,
                slowMotionBallsRemaining = nextSlowMotionRemaining,
                isDailyChallengeTargetMet = isChallengeGoalMet
            )
        }

        // Animate batsman swing follow-through smoothly
        viewModelScope.launch {
            val swingDurationMs = 260L
            val startSwing = System.currentTimeMillis()
            while (System.currentTimeMillis() - startSwing < swingDurationMs) {
                val p = (System.currentTimeMillis() - startSwing).toFloat() / swingDurationMs
                _gameState.update { it.copy(batsmanPoseProgress = p.coerceIn(0f, 1f)) }
                delay(16)
            }
            _gameState.update { it.copy(batsmanPoseProgress = 1.0f) }
        }

        // Screen-shake on boundary hits or wickets to increase visual feedback and excitement
        viewModelScope.launch {
            val (baseIntensity, shakeDurationMs) = when (outcome.outcomeType) {
                OutcomeType.SIX -> 20f to 380L
                OutcomeType.WICKET -> 22f to 400L
                OutcomeType.FOUR -> 13f to 280L
                else -> if (outcome.runs > 0) 5f to 160L else 0f to 0L
            }

            if (baseIntensity > 0f) {
                val startShakeTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startShakeTime < shakeDurationMs) {
                    val elapsed = System.currentTimeMillis() - startShakeTime
                    val progress = elapsed.toFloat() / shakeDurationMs
                    val decay = (1f - progress) * (1f - progress)
                    val frequency = 0.055f * elapsed
                    val currentAmp = baseIntensity * decay
                    val jitterX = (Random.nextFloat() * 2f - 1f) * (currentAmp * 0.35f)
                    val jitterY = (Random.nextFloat() * 2f - 1f) * (currentAmp * 0.35f)
                    val sx = kotlin.math.sin(frequency) * currentAmp + jitterX
                    val sy = kotlin.math.cos(frequency * 1.25f) * (currentAmp * 0.75f) + jitterY
                    _gameState.update { it.copy(cameraShakeX = sx, cameraShakeY = sy) }
                    delay(16)
                }
                _gameState.update { it.copy(cameraShakeX = 0f, cameraShakeY = 0f) }
            }
        }

        // Hit Exit ball trajectory & contact flash decay
        viewModelScope.launch {
            val totalAnimMs = if (outcome.outcomeType == OutcomeType.SIX) 1200L else 800L
            val startMs = System.currentTimeMillis()
            while (System.currentTimeMillis() - startMs < totalAnimMs) {
                val elapsed = System.currentTimeMillis() - startMs
                val p = (elapsed.toFloat() / totalAnimMs).coerceIn(0f, 1f)
                val flashP = (1f - (elapsed.toFloat() / 380f)).coerceIn(0f, 1f)
                _gameState.update {
                    it.copy(
                        hitExitProgress = p,
                        contactFlashProgress = flashP
                    )
                }
                delay(16)
            }
            _gameState.update { it.copy(contactFlashProgress = 0f) }
        }

        viewModelScope.launch {
            val pauseDelay = when (outcome.outcomeType) {
                OutcomeType.SIX -> 1500L
                OutcomeType.FOUR -> 1300L
                OutcomeType.WICKET -> 1400L
                else -> 1000L
            }
            delay(pauseDelay)

            if (outcome.isWicket) {
                // If wicket fell before last ball and continue not yet used, show continue dialogue
                if (newBallsFaced < _gameState.value.maxBalls && !_gameState.value.hasUsedContinue) {
                    _gameState.update { it.copy(isWicketContinueDialogVisible = true) }
                } else {
                    finishMatch()
                }
            } else if (newBallsFaced >= _gameState.value.maxBalls) {
                finishMatch()
            } else {
                prepareNextBall()
            }
        }
    }

    // Free second chance in v1 — no ads. Continuing is granted immediately.
    fun continueAfterWicket() {
        _gameState.update {
            it.copy(
                isWicketContinueDialogVisible = false,
                hasUsedContinue = true
            )
        }
        prepareNextBall()
    }

    fun declineWicketContinue() {
        _gameState.update { it.copy(isWicketContinueDialogVisible = false) }
        finishMatch()
    }

    private fun finishMatch() {
        val state = _gameState.value
        val isTargetAchieved = state.score >= state.target
        val profile = profileState.value

        // Coin calculation scaled by overs
        val oversMultiplier = state.matchOvers.coerceAtLeast(1)
        val baseCoins = (50 * state.difficulty.coinMultiplier * oversMultiplier).toInt()
        val boundaryCoins = (state.foursCount * 10) + (state.sixesCount * 25)
        val targetBonus = if (isTargetAchieved) (100 * state.difficulty.coinMultiplier * oversMultiplier).toInt() else 0
        val totalCoinsEarned = baseCoins + boundaryCoins + targetBonus

        val previousHighScore = profile?.highScore ?: 0
        val isNewHighScore = state.score > previousHighScore && state.score > 0

        val challenge = challengeManager.getTodayChallenge()
        val isChallengeMet = challengeManager.evaluateMatch(challenge, MatchSummary(
            totalRuns = state.score,
            ballsFaced = state.ballsFaced,
            fours = state.foursCount,
            sixes = state.sixesCount,
            wickets = state.wicketsCount,
            target = state.target,
            difficulty = state.difficulty,
            isTargetAchieved = isTargetAchieved,
            baseCoins = baseCoins,
            bonusCoins = boundaryCoins + targetBonus,
            totalCoinsEarned = totalCoinsEarned,
            ballOutcomes = state.ballOutcomes,
            isNewHighScore = isNewHighScore,
            matchOvers = state.matchOvers
        ))
        val wasAlreadyCompleted = profile?.completedDailyChallengeDate == challenge.dateKey
        val isNewlyCompleted = isChallengeMet && !wasAlreadyCompleted

        val summary = MatchSummary(
            totalRuns = state.score,
            ballsFaced = state.ballsFaced,
            fours = state.foursCount,
            sixes = state.sixesCount,
            wickets = state.wicketsCount,
            target = state.target,
            difficulty = state.difficulty,
            isTargetAchieved = isTargetAchieved,
            baseCoins = baseCoins,
            bonusCoins = boundaryCoins + targetBonus,
            totalCoinsEarned = totalCoinsEarned,
            ballOutcomes = state.ballOutcomes,
            isNewHighScore = isNewHighScore,
            isDailyChallengeCompletedInThisMatch = isNewlyCompleted,
            dailyChallengeTitle = challenge.title,
            dailyChallengeReward = challenge.rewardCoins,
            matchOvers = state.matchOvers
        )

        analyticsManager.logGameCompleted(state.score, isTargetAchieved, state.difficulty.title)

        if (isTargetAchieved || isNewlyCompleted) {
            soundManager.playSound(SoundManager.SoundType.CROWD_CHEER)
        }

        viewModelScope.launch {
            val pName = profile?.playerName ?: "Player"
            repository.recordMatch(
                runs = state.score,
                fours = state.foursCount,
                sixes = state.sixesCount,
                wickets = state.wicketsCount,
                difficulty = state.difficulty,
                targetMet = isTargetAchieved,
                coinsEarned = totalCoinsEarned,
                playerName = pName
            )

            // Check daily challenge
            if (isChallengeMet) {
                repository.markDailyChallengeCompleted(challenge.dateKey)
                analyticsManager.logDailyChallengeCompleted(challenge.dateKey, challenge.rewardCoins)
            }
        }

        soundManager.playSound(SoundManager.SoundType.COIN_REWARD)

        _gameState.update {
            it.copy(
                isGameOver = true,
                isWicketContinueDialogVisible = false,
                matchSummary = summary
            )
        }
    }

    fun claimDailyChallengeReward(onSuccess: (() -> Unit)? = null) {
        val profile = profileState.value ?: return
        val challenge = challengeManager.getTodayChallenge()
        if (profile.completedDailyChallengeDate == challenge.dateKey &&
            profile.lastChallengeClaimedDate != challenge.dateKey
        ) {
            viewModelScope.launch {
                repository.claimDailyChallengeReward(challenge.dateKey, challenge.rewardCoins)
                soundManager.playSound(SoundManager.SoundType.COIN_REWARD)
                hapticManager.vibrateSix()
                onSuccess?.invoke()
            }
        }
    }

    // Free in v1: doubles the match coins immediately, no ad.
    fun doubleMatchCoins(onComplete: () -> Unit) {
        val currentCoins = _gameState.value.matchSummary?.totalCoinsEarned ?: 0
        if (currentCoins <= 0) return

        viewModelScope.launch {
            repository.addCoins(currentCoins)
            soundManager.playSound(SoundManager.SoundType.COIN_REWARD)
            onComplete()
        }
    }

    // Free bonus coins in v1: granted immediately, no ad.
    fun earnBonusReward(amount: Int = 100, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.addCoins(amount)
            soundManager.playSound(SoundManager.SoundType.COIN_REWARD)
            onComplete()
        }
    }

    fun togglePause() {
        val nextPause = !_gameState.value.isPaused
        _gameState.update { it.copy(isPaused = nextPause) }
    }

    fun resumeGame() {
        _gameState.update { it.copy(isPaused = false) }
    }

    fun quitMatch() {
        ballAnimationJob?.cancel()
        _gameState.update { it.copy(isGameOver = true, isPaused = false) }
    }

    fun activatePowerUp(type: PowerUpType) {
        val state = _gameState.value
        if (state.isGameOver) return

        viewModelScope.launch {
            when (type) {
                PowerUpType.GOLDEN_BAT -> {
                    if (state.isGoldenBatActive) return@launch
                    val currentCount = profileState.value?.goldenBatCount ?: 0
                    if (currentCount > 0) {
                        val used = repository.usePowerUp(PowerUpType.GOLDEN_BAT)
                        if (used) {
                            soundManager.playSound(SoundManager.SoundType.POWER_UP_ACTIVATE)
                            hapticManager.vibratePowerUp()
                            analyticsManager.logPowerUpUsed("golden_bat")
                            _gameState.update {
                                it.copy(
                                    goldenBatBallsRemaining = 2,
                                    powerUpBannerMessage = "⚡ GOLDEN BAT ACTIVATED! (+2 RUNS ON HITS)"
                                )
                            }
                        }
                    }
                }
                PowerUpType.SLOW_MOTION -> {
                    if (state.isSlowMotionActive) return@launch
                    val currentCount = profileState.value?.slowMotionCount ?: 0
                    if (currentCount > 0) {
                        val used = repository.usePowerUp(PowerUpType.SLOW_MOTION)
                        if (used) {
                            soundManager.playSound(SoundManager.SoundType.POWER_UP_ACTIVATE)
                            hapticManager.vibratePowerUp()
                            analyticsManager.logPowerUpUsed("slow_motion")
                            _gameState.update {
                                it.copy(
                                    slowMotionBallsRemaining = 2,
                                    powerUpBannerMessage = "⏱️ SLOW MOTION ACTIVATED! (EASY TIMING)"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun buyPowerUp(type: PowerUpType, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val success = repository.buyPowerUpWithCoins(type, costCoins = 40, amount = 2)
            if (success) {
                soundManager.playSound(SoundManager.SoundType.COIN_REWARD)
                hapticManager.vibrateButton()
            }
            onComplete?.invoke(success)
        }
    }

    // Free in v1: refills the power-up immediately, no ad.
    fun refillPowerUp(type: PowerUpType) {
        viewModelScope.launch {
            repository.addPowerUp(type, amount = 2)
            soundManager.playSound(SoundManager.SoundType.POWER_UP_ACTIVATE)
            hapticManager.vibratePowerUp()
            _gameState.update {
                it.copy(
                    powerUpBannerMessage = "${type.iconEmoji} REFILLED +2 ${type.title.uppercase()}!"
                )
            }
        }
    }

    fun clearPowerUpBanner() {
        _gameState.update { it.copy(powerUpBannerMessage = null) }
    }

    fun activateGoldenBat() {
        val state = _gameState.value
        if (state.isGameOver || state.isGoldenBatActive) return
        val currentCount = profileState.value?.goldenBatCount ?: 0
        if (currentCount > 0) {
            activatePowerUp(PowerUpType.GOLDEN_BAT)
        } else {
            refillPowerUp(PowerUpType.GOLDEN_BAT)
        }
    }
}
