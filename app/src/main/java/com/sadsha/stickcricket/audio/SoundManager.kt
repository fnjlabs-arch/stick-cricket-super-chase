package com.sadsha.stickcricket.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.PlaybackParams
import android.os.Build
import android.util.Log
import com.sadsha.stickcricket.R
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/**
 * SoundManager using direct low-latency AudioTrack PCM playback for short
 * audio clips (ball impact, cheers, UI interactions).
 * Avoids Android MediaCodec / Codec2 HAL queries on virtual/emulated devices.
 */
class SoundManager(context: Context? = null) {

    var isSoundEnabled: Boolean = true
    var isMusicEnabled: Boolean = true

    private val effectiveContext: Context? = context?.applicationContext ?: appContext

    // Map of SoundType to preloaded PCM audio clip
    private val soundClips = ConcurrentHashMap<SoundType, PcmClip>()
    // Debounce tracking to prevent audio hardware resource exhaustion
    private val lastPlayedTime = ConcurrentHashMap<SoundType, Long>()

    enum class SoundType {
        BAT_HIT,
        PERFECT_HIT,
        CROWD_CHEER,
        FOUR,
        SIX,
        WICKET,
        BUTTON_TAP,
        COIN_REWARD,
        POWER_UP_ACTIVATE
    }

    init {
        effectiveContext?.let { ctx ->
            thread(isDaemon = true, name = "CricketAudioLoader") {
                loadAllSounds(ctx)
            }
        }
    }

    /**
     * Preload all audio clips into static AudioTrack memory for instant zero-latency playback.
     */
    fun loadAllSounds(context: Context) {
        val ctx = context.applicationContext
        loadClip(SoundType.BAT_HIT, ctx, R.raw.ball_impact, poolSize = 2)
        loadClip(SoundType.PERFECT_HIT, ctx, R.raw.perfect_hit, poolSize = 2)
        loadClip(SoundType.CROWD_CHEER, ctx, R.raw.crowd_cheer, poolSize = 1)
        loadClip(SoundType.FOUR, ctx, R.raw.four_cheer, poolSize = 1)
        loadClip(SoundType.SIX, ctx, R.raw.six_cheer, poolSize = 1)
        loadClip(SoundType.WICKET, ctx, R.raw.wicket_fall, poolSize = 1)
        loadClip(SoundType.BUTTON_TAP, ctx, R.raw.ui_tap, poolSize = 2)
        loadClip(SoundType.COIN_REWARD, ctx, R.raw.coin_reward, poolSize = 1)
        loadClip(SoundType.POWER_UP_ACTIVATE, ctx, R.raw.power_up, poolSize = 1)
    }

    private fun loadClip(type: SoundType, context: Context, resId: Int, poolSize: Int) {
        try {
            val clip = parseWavResource(context, resId, poolSize)
            if (clip != null) {
                soundClips[type] = clip
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading audio clip $resId for $type", e)
        }
    }

    /**
     * Play a sound effect by SoundType with throttling protection.
     * @param type The sound effect type.
     * @param volume Volume level between 0.0f and 1.0f (default 1.0f).
     * @param rate Playback rate between 0.5f and 2.0f (default 1.0f).
     * @return 1 if played, 0 if skipped.
     */
    fun playSound(type: SoundType, volume: Float = 1.0f, rate: Float = 1.0f): Int {
        if (!isSoundEnabled) return 0

        val clip = soundClips[type] ?: return 0

        // Debounce frequent identical sounds
        val now = System.currentTimeMillis()
        val minIntervalMs = when (type) {
            SoundType.BUTTON_TAP -> 50L
            SoundType.BAT_HIT, SoundType.PERFECT_HIT -> 40L
            else -> 80L
        }
        val lastPlayed = lastPlayedTime[type] ?: 0L
        if (now - lastPlayed < minIntervalMs) {
            return 0
        }
        lastPlayedTime[type] = now

        val clampedVol = volume.coerceIn(0.0f, 1.0f)
        val clampedRate = rate.coerceIn(0.5f, 2.0f)
        return clip.play(clampedVol, clampedRate)
    }

    /**
     * Stop a specific stream / sound.
     */
    fun stopSound(streamId: Int) {
        // No-op for individual stream IDs or can stop all
    }

    /**
     * Pause all playing sound streams.
     */
    fun pauseAll() {
        soundClips.values.forEach { it.pause() }
    }

    /**
     * Resume all paused sound streams.
     */
    fun resumeAll() {
        soundClips.values.forEach { it.resume() }
    }

    /**
     * Releases all audio resources.
     */
    fun release() {
        try {
            soundClips.values.forEach { it.release() }
            soundClips.clear()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing audio tracks", e)
        }
    }

    /**
     * Lightweight container holding static AudioTrack instances for zero-latency playback.
     */
    private class PcmClip(
        val sampleRate: Int,
        val channelMask: Int,
        val pcmData: ByteArray,
        poolSize: Int
    ) {
        private val tracks = Array(poolSize) { createTrack() }
        private var currentIndex = 0

        private fun createTrack(): AudioTrack? {
            return try {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(channelMask)
                    .build()

                val track = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(pcmData.size)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                val written = track.write(pcmData, 0, pcmData.size)
                if (written > 0) track else {
                    track.release()
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to create AudioTrack for PCM clip", e)
                null
            }
        }

        @Synchronized
        fun play(volume: Float, rate: Float): Int {
            if (tracks.isEmpty()) return 0
            val track = tracks[currentIndex] ?: return 0
            currentIndex = (currentIndex + 1) % tracks.size

            return try {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
                track.reloadStaticData()
                track.setVolume(volume.coerceIn(0.0f, 1.0f))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && rate != 1.0f) {
                    try {
                        track.playbackParams = PlaybackParams().apply {
                            speed = rate.coerceIn(0.5f, 2.0f)
                        }
                    } catch (_: Exception) {}
                }
                track.play()
                1
            } catch (e: Exception) {
                Log.w(TAG, "Error playing AudioTrack", e)
                0
            }
        }

        @Synchronized
        fun pause() {
            tracks.forEach {
                try {
                    if (it?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        it.pause()
                    }
                } catch (_: Exception) {}
            }
        }

        @Synchronized
        fun resume() {
            tracks.forEach {
                try {
                    if (it?.playState == AudioTrack.PLAYSTATE_PAUSED) {
                        it.play()
                    }
                } catch (_: Exception) {}
            }
        }

        @Synchronized
        fun stop() {
            tracks.forEach {
                try {
                    if (it?.playState == AudioTrack.PLAYSTATE_PLAYING || it?.playState == AudioTrack.PLAYSTATE_PAUSED) {
                        it.stop()
                    }
                } catch (_: Exception) {}
            }
        }

        @Synchronized
        fun release() {
            tracks.forEach {
                try {
                    it?.release()
                } catch (_: Exception) {}
            }
        }
    }

    companion object {
        private const val TAG = "SoundManager"

        @Volatile
        private var appContext: Context? = null

        @Volatile
        private var instance: SoundManager? = null

        /**
         * Initialize SoundManager with application context.
         */
        fun initialize(context: Context) {
            appContext = context.applicationContext
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = SoundManager(appContext)
                    }
                }
            }
        }

        /**
         * Get singleton instance of SoundManager.
         */
        fun getInstance(context: Context? = null): SoundManager {
            return instance ?: synchronized(this) {
                instance ?: SoundManager(context ?: appContext).also { instance = it }
            }
        }

        /**
         * Extract raw PCM bytes from standard WAV resource.
         */
        private fun parseWavResource(context: Context, resId: Int, poolSize: Int): PcmClip? {
            return try {
                context.resources.openRawResource(resId).use { input ->
                    val bytes = input.readBytes()
                    if (bytes.size < 44) return null
                    if (bytes[0] != 'R'.code.toByte() || bytes[1] != 'I'.code.toByte() ||
                        bytes[2] != 'F'.code.toByte() || bytes[3] != 'F'.code.toByte()
                    ) {
                        return null
                    }
                    var pos = 12
                    var channels = 1
                    var sampleRate = 44100
                    var pcmData: ByteArray? = null

                    while (pos + 8 <= bytes.size) {
                        val chunkId = String(bytes, pos, 4, Charsets.US_ASCII)
                        val chunkSize = (bytes[pos + 4].toInt() and 0xFF) or
                                ((bytes[pos + 5].toInt() and 0xFF) shl 8) or
                                ((bytes[pos + 6].toInt() and 0xFF) shl 16) or
                                ((bytes[pos + 7].toInt() and 0xFF) shl 24)
                        pos += 8

                        if (chunkId == "fmt ") {
                            if (chunkSize >= 16 && pos + 16 <= bytes.size) {
                                channels = (bytes[pos + 2].toInt() and 0xFF) or ((bytes[pos + 3].toInt() and 0xFF) shl 8)
                                sampleRate = (bytes[pos + 4].toInt() and 0xFF) or
                                        ((bytes[pos + 5].toInt() and 0xFF) shl 8) or
                                        ((bytes[pos + 6].toInt() and 0xFF) shl 16) or
                                        ((bytes[pos + 7].toInt() and 0xFF) shl 24)
                            }
                            pos += chunkSize
                        } else if (chunkId == "data") {
                            val available = (bytes.size - pos).coerceAtLeast(0)
                            val len = chunkSize.coerceIn(0, available)
                            pcmData = bytes.copyOfRange(pos, pos + len)
                            break
                        } else {
                            pos += chunkSize
                        }
                        if (chunkSize % 2 == 1) {
                            pos += 1
                        }
                    }

                    if (pcmData != null) {
                        val channelMask = if (channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
                        PcmClip(sampleRate, channelMask, pcmData, poolSize)
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse WAV resource $resId", e)
                null
            }
        }
    }
}
