package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.model.GameSettings
import com.example.model.WeaponCategory
import com.example.model.WeaponId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class TacticalSoundEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastShotMs = 0L
    private var settings = GameSettings()

    fun updateSettings(newSettings: GameSettings) {
        settings = newSettings
    }

    private fun effectiveSfxGain(): Float {
        return (settings.masterVolume / 100f) * (settings.sfxVolume / 100f)
    }

    fun playWeaponShot(weapon: WeaponId) {
        val now = System.currentTimeMillis()
        if (now - lastShotMs < 55L) return
        lastShotMs = now
        val gain = effectiveSfxGain()
        if (gain <= 0.02f) return

        when (weapon.category) {
            WeaponCategory.SHOTGUN -> playToneBurst(freqStart = 180f, freqEnd = 52f, durationMs = 135, noiseMix = 0.72f, gain = gain * 0.65f)
            WeaponCategory.SNIPER -> playToneBurst(freqStart = 420f, freqEnd = 70f, durationMs = 180, noiseMix = 0.55f, gain = gain * 0.75f)
            WeaponCategory.SMG -> playToneBurst(freqStart = 320f, freqEnd = 110f, durationMs = 52, noiseMix = 0.5f, gain = gain * 0.42f)
            WeaponCategory.AR -> playToneBurst(freqStart = 260f, freqEnd = 85f, durationMs = 75, noiseMix = 0.58f, gain = gain * 0.52f)
            WeaponCategory.PISTOL -> playToneBurst(freqStart = 360f, freqEnd = 120f, durationMs = 70, noiseMix = 0.45f, gain = gain * 0.48f)
        }
    }

    fun playGlooDeploy() {
        val gain = effectiveSfxGain()
        if (gain <= 0.02f) return
        playToneBurst(freqStart = 520f, freqEnd = 980f, durationMs = 140, noiseMix = 0.22f, gain = gain * 0.5f)
    }

    fun playHeadshotPing() {
        val gain = effectiveSfxGain()
        if (gain <= 0.02f) return
        playToneBurst(freqStart = 1280f, freqEnd = 1640f, durationMs = 110, noiseMix = 0.05f, gain = gain * 0.55f)
    }

    fun playSkillActivate() {
        val gain = effectiveSfxGain()
        if (gain <= 0.02f) return
        playToneBurst(freqStart = 440f, freqEnd = 880f, durationMs = 190, noiseMix = 0.1f, gain = gain * 0.5f)
    }

    fun playUiClick() {
        if (!settings.uiAudioEnabled) return
        val gain = (settings.masterVolume / 100f) * 0.3f
        if (gain <= 0.02f) return
        playToneBurst(freqStart = 760f, freqEnd = 620f, durationMs = 30, noiseMix = 0.0f, gain = gain)
    }

    private fun playToneBurst(
        freqStart: Float,
        freqEnd: Float,
        durationMs: Int,
        noiseMix: Float,
        gain: Float
    ) {
        scope.launch {
            runCatching {
                val sampleRate = 22050
                val count = (sampleRate * durationMs / 1000).coerceAtLeast(256)
                val samples = ShortArray(count)
                var phase = 0.0
                for (i in 0 until count) {
                    val t = i.toFloat() / count
                    val freq = freqStart + (freqEnd - freqStart) * t
                    phase += 2.0 * PI * freq / sampleRate
                    val env = exp(-5.0 * t).toFloat()
                    val tone = sin(phase).toFloat()
                    val noise = (Random.nextFloat() * 2f - 1f)
                    val mixed = (tone * (1f - noiseMix) + noise * noiseMix) * env * gain
                    samples[i] = (mixed.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(samples, 0, samples.size)
                track.play()
                kotlinx.coroutines.delay(durationMs.toLong() + 40L)
                track.stop()
                track.release()
            }
        }
    }
}
