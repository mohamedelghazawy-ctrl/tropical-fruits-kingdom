package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.model.BoosterType
import java.util.Random
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/**
 * محرك صوتي عالي الأداء لتطبيق كاندي كراش الاستوائي
 * يولد نغمات موسيقية غنية بالهارمونيات وأصوات فرقعة الفواكه الطازجة
 * وأهازيج الفوز المبهجة والمؤثرات الصوتية للأدوات المساعدة (Boosters)
 */
class SoundHelper(private val context: Context) {

    private val sampleRate = 22050
    private var toneGenerator: ToneGenerator? = null
    private var vibrator: Vibrator? = null
    @Volatile private var audioTrack: AudioTrack? = null

    // Multi-voice dynamic mixer
    private class SoundVoice(val samples: ShortArray, var offset: Int = 0)
    private val mixerLock = Object()
    private val activeVoices = ArrayList<SoundVoice>()
    @Volatile private var isRunning = true
    private var mixerThread: Thread? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var popIndex = 0

    // Pre-synthesized audio buffers for 0ms latency
    private val popBuffers: Array<ShortArray>
    private val comboChimes: Array<ShortArray>
    private val cascadeDropBuffer: ShortArray
    private val specialExplosionBuffer: ShortArray
    private val largeImpactBuffer: ShortArray
    private val victoryFanfareBuffer: ShortArray
    private val defeatBuffer: ShortArray
    private val hammerBuffer: ShortArray
    private val lightningBuffer: ShortArray
    private val whirlwindBuffer: ShortArray
    private val tapBuffer: ShortArray
    private val swapBuffer: ShortArray
    private val invalidSwapBuffer: ShortArray

    init {
        // Fallback ToneGenerator
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (e: Exception) {
            Log.w("SoundHelper", "Could not init ToneGenerator: ${e.message}")
        }

        // Haptic Vibrator
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            Log.w("SoundHelper", "Could not init Vibrator: ${e.message}")
        }

        // Pre-render sound waveforms
        popBuffers = arrayOf(
            createPop(baseFreq = 360.0, durationMs = 60),
            createPop(baseFreq = 410.0, durationMs = 60),
            createPop(baseFreq = 460.0, durationMs = 60),
            createPop(baseFreq = 510.0, durationMs = 60)
        )

        // Pentatonic Scale Chimes for Combos (C5, D5, E5, G5, A5, C6, E6)
        comboChimes = arrayOf(
            createChime(freq = 523.25, durationMs = 230),  // C5
            createChime(freq = 587.33, durationMs = 230),  // D5
            createChime(freq = 659.25, durationMs = 240),  // E5
            createChime(freq = 783.99, durationMs = 250),  // G5
            createChime(freq = 880.00, durationMs = 260),  // A5
            createChime(freq = 1046.50, durationMs = 280), // C6
            createChime(freq = 1318.51, durationMs = 300)  // E6
        )

        cascadeDropBuffer = createCascadeDrop(durationMs = 55)
        specialExplosionBuffer = createExplosion(durationMs = 280)
        largeImpactBuffer = createLargeImpact(durationMs = 360)
        victoryFanfareBuffer = createVictoryFanfare()
        defeatBuffer = createDefeatMelody()
        hammerBuffer = createHammerSmash(durationMs = 150)
        lightningBuffer = createLightningZap(durationMs = 140)
        whirlwindBuffer = createWhirlwindSwoosh(durationMs = 220)
        tapBuffer = createTap(durationMs = 25)
        swapBuffer = createSwap(durationMs = 45)
        invalidSwapBuffer = createInvalidSwap(durationMs = 90)

        // Initialize Streaming AudioTrack
        try {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
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
                    .setBufferSizeInBytes(minBuf * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf * 2,
                    AudioTrack.MODE_STREAM
                )
            }

            if (audioTrack?.state == AudioTrack.STATE_INITIALIZED) {
                audioTrack?.play()
                startMixerThread()
            } else {
                audioTrack = null
            }
        } catch (e: Throwable) {
            Log.w("SoundHelper", "Could not init AudioTrack: ${e.message}")
            audioTrack = null
        }
    }

    private fun startMixerThread() {
        mixerThread = Thread {
            val chunkSize = 512
            val mixBuffer = ShortArray(chunkSize)

            while (isRunning) {
                var hasAudio = false
                synchronized(mixerLock) {
                    while (isRunning && activeVoices.isEmpty()) {
                        try {
                            mixerLock.wait()
                        } catch (e: InterruptedException) {
                            return@Thread
                        }
                    }
                    if (!isRunning) return@Thread

                    mixBuffer.fill(0)
                    val iterator = activeVoices.iterator()
                    while (iterator.hasNext()) {
                        val voice = iterator.next()
                        hasAudio = true
                        val remaining = voice.samples.size - voice.offset
                        val count = minOf(chunkSize, remaining)
                        for (i in 0 until count) {
                            val mixed = mixBuffer[i] + voice.samples[voice.offset + i]
                            mixBuffer[i] = mixed.coerceIn(-32767, 32767).toShort()
                        }
                        voice.offset += count
                        if (voice.offset >= voice.samples.size) {
                            iterator.remove()
                        }
                    }
                }

                if (hasAudio && audioTrack != null) {
                    try {
                        audioTrack?.write(mixBuffer, 0, chunkSize)
                    } catch (ignored: Exception) {}
                }
            }
        }.apply {
            name = "TropicalAudioMixer"
            isDaemon = true
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    private fun playPcm(samples: ShortArray) {
        if (audioTrack != null) {
            synchronized(mixerLock) {
                if (activeVoices.size < 12) {
                    activeVoices.add(SoundVoice(samples))
                    mixerLock.notifyAll()
                }
            }
        }
    }

    // ==========================================
    // Public Sound Triggers
    // ==========================================

    fun playTap() {
        playPcm(tapBuffer)
        if (audioTrack == null) playToneFallback(ToneGenerator.TONE_PROP_BEEP, 30)
        vibrate(15)
    }

    fun playSwap() {
        playPcm(swapBuffer)
        if (audioTrack == null) playToneFallback(ToneGenerator.TONE_PROP_PROMPT, 50)
        vibrate(25)
    }

    fun playInvalidSwap() {
        playPcm(invalidSwapBuffer)
        if (audioTrack == null) playToneFallback(ToneGenerator.TONE_PROP_NACK, 80)
        vibrate(55)
    }

    /**
     * صوت فرقعة الفواكه الطازجة أثناء حركة التطهير وتلاشي الفواكه
     */
    fun playFruitSquishPop() {
        val buffer = popBuffers[popIndex % popBuffers.size]
        popIndex++
        playPcm(buffer)
        if (audioTrack == null) playToneFallback(ToneGenerator.TONE_PROP_ACK, 40)
        vibrate(18)
    }

    /**
     * نغمات موسيقية ماريمبا تصاعدية بحسب مضاعف الكومبو
     */
    fun playMatch(comboLevel: Int = 1) {
        val idx = (comboLevel - 1).coerceIn(0, comboChimes.size - 1)
        playPcm(comboChimes[idx])
        if (audioTrack == null) {
            val tone = when (comboLevel) {
                1 -> ToneGenerator.TONE_PROP_ACK
                2 -> ToneGenerator.TONE_DTMF_A
                3 -> ToneGenerator.TONE_DTMF_B
                4 -> ToneGenerator.TONE_DTMF_C
                else -> ToneGenerator.TONE_DTMF_D
            }
            playToneFallback(tone, 100)
        }
        vibrate(28 + (comboLevel * 10).toLong())
    }

    /**
     * صوت هبوط الفواكه المتساقطة لملء الأماكن الشاغرة
     */
    fun playCascadeDrop() {
        playPcm(cascadeDropBuffer)
        if (audioTrack == null) playToneFallback(ToneGenerator.TONE_PROP_BEEP, 30)
    }

    /**
     * انفجار الفواكه الخاصة (صواعق الصف والعمود وقنابل الفواكه)
     */
    fun playSpecialExplosion() {
        playPcm(specialExplosionBuffer)
        if (audioTrack == null) playToneFallback(ToneGenerator.TONE_CDMA_HIGH_L, 160)
        vibrate(110)
    }

    /**
     * تأثير قوي مع اهتزاز الشاشة لمطابقات المجموعات الكبيرة (أكثر من 4 فواكه)
     */
    fun playLargeMatchImpact(clearedCount: Int) {
        playPcm(largeImpactBuffer)
        if (audioTrack == null) playToneFallback(ToneGenerator.TONE_CDMA_HIGH_L, 180)
        val hapticDuration = (75 + (clearedCount - 4) * 20).coerceIn(80, 240).toLong()
        vibrate(hapticDuration)
    }

    /**
     * مؤثرات الأدوات المساعدة المختلفة
     */
    fun playBoosterUse(booster: BoosterType? = null) {
        when (booster) {
            BoosterType.HAMMER -> playHammerSmash()
            BoosterType.ROW_BLAST, BoosterType.COL_BLAST -> playLightningZap()
            BoosterType.SHUFFLE, null -> playWhirlwindSwoosh()
        }
    }

    fun playHammerSmash() {
        playPcm(hammerBuffer)
        if (audioTrack == null) playToneFallback(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 120)
        vibrate(85)
    }

    fun playLightningZap() {
        playPcm(lightningBuffer)
        if (audioTrack == null) playToneFallback(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 140)
        vibrate(70)
    }

    fun playWhirlwindSwoosh() {
        playPcm(whirlwindBuffer)
        if (audioTrack == null) playToneFallback(ToneGenerator.TONE_PROP_PROMPT, 100)
        vibrate(50)
    }

    /**
     * معزوفة نصر احتفالية استوائية مبهجة عند إكمال المستوى بنجاح
     */
    fun playVictory() {
        playPcm(victoryFanfareBuffer)
        if (audioTrack == null) {
            // بدون خيط جديد: نتابع النغمات عبر Handler ويُلغى في release()
            playToneFallback(ToneGenerator.TONE_PROP_ACK, 90)
            mainHandler.postDelayed({ playToneFallback(ToneGenerator.TONE_DTMF_B, 90) }, 190)
            mainHandler.postDelayed({ playToneFallback(ToneGenerator.TONE_DTMF_D, 240) }, 380)
        }

        // Celebratory haptic pulse rhythm
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 60, 80, 60, 180), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 80, 60, 80, 60, 180), -1)
            }
        } catch (ignored: Exception) {}
    }

    /**
     * نغمة دافئة عند استنفاد الحركات وخسارة المستوى
     */
    fun playDefeat() {
        playPcm(defeatBuffer)
        if (audioTrack == null) playToneFallback(ToneGenerator.TONE_CDMA_LOW_L, 300)
        vibrate(150)
    }

    private fun playToneFallback(toneType: Int, durationMs: Int) {
        try {
            toneGenerator?.startTone(toneType, durationMs)
        } catch (ignored: Exception) {}
    }

    private fun vibrate(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (ignored: Exception) {}
    }

    fun release() {
        isRunning = false
        mainHandler.removeCallbacksAndMessages(null)
        synchronized(mixerLock) {
            activeVoices.clear()
            mixerLock.notifyAll()
        }
        // ننتظر انتهاء خيط المزج قبل تحرير الـ AudioTrack لتفادي الكتابة على مسار محرَّر
        try {
            mixerThread?.join(300)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        mixerThread = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        } catch (_: Exception) {
        } finally {
            audioTrack = null
        }
        try {
            toneGenerator?.release()
        } catch (_: Exception) {
        } finally {
            toneGenerator = null
        }
    }

    // ==========================================
    // Procedural Waveform Synthesizers
    // ==========================================

    private fun createPop(baseFreq: Double, durationMs: Int): ShortArray {
        val totalSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(totalSamples)
        var phase = 0.0
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val freq = baseFreq * (0.85 + 1.35 * progress * progress)
            phase += 2.0 * PI * freq / sampleRate
            val attack = (progress * 12.0).coerceAtMost(1.0)
            val decay = exp(-progress * 5.2)
            val envelope = attack * decay
            val sampleVal = (sin(phase) * envelope * 27000).toInt()
            samples[i] = sampleVal.coerceIn(-32767, 32767).toShort()
        }
        return samples
    }

    private fun createChime(freq: Double, durationMs: Int): ShortArray {
        val totalSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / totalSamples
            val attack = (progress * 20.0).coerceAtMost(1.0)
            val decay = exp(-progress * 4.2)
            val envelope = attack * decay
            val fundamental = sin(2.0 * PI * freq * t) * 0.70
            val harmonic2 = sin(2.0 * PI * (freq * 2.0) * t) * 0.22
            val harmonic3 = sin(2.0 * PI * (freq * 3.0) * t) * 0.08
            val sampleVal = ((fundamental + harmonic2 + harmonic3) * envelope * 29000).toInt()
            samples[i] = sampleVal.coerceIn(-32767, 32767).toShort()
        }
        return samples
    }

    private fun createCascadeDrop(durationMs: Int): ShortArray {
        val totalSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(totalSamples)
        var phase = 0.0
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val freq = 1150.0 - (progress * 380.0)
            phase += 2.0 * PI * freq / sampleRate
            val attack = (progress * 15.0).coerceAtMost(1.0)
            val decay = exp(-progress * 6.5)
            val sampleVal = (sin(phase) * attack * decay * 22000).toInt()
            samples[i] = sampleVal.coerceIn(-32767, 32767).toShort()
        }
        return samples
    }

    private fun createExplosion(durationMs: Int): ShortArray {
        val totalSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(totalSamples)
        var phase = 0.0
        val rng = Random(1234)
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val freq = 130.0 * (1.0 - progress * 0.75).pow(2.0)
            phase += 2.0 * PI * freq / sampleRate
            val subBass = sin(phase) * exp(-progress * 3.8) * 0.75
            val noiseEnv = exp(-progress * 7.0)
            val noise = (rng.nextDouble() * 2.0 - 1.0) * noiseEnv * 0.35
            val sampleVal = ((subBass + noise) * 31000).toInt()
            samples[i] = sampleVal.coerceIn(-32767, 32767).toShort()
        }
        return samples
    }

    private fun createLargeImpact(durationMs: Int): ShortArray {
        val totalSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(totalSamples)
        var phase = 0.0
        var phaseBell = 0.0
        val rng = Random(5678)
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val bassFreq = 95.0 * (1.0 - progress * 0.7)
            phase += 2.0 * PI * bassFreq / sampleRate
            val subBass = sin(phase) * exp(-progress * 3.0) * 0.70

            phaseBell += 2.0 * PI * 880.0 / sampleRate
            val bell = sin(phaseBell) * exp(-progress * 4.5) * 0.25

            val noiseEnv = exp(-progress * 9.0)
            val crunch = (rng.nextDouble() * 2.0 - 1.0) * noiseEnv * 0.25

            val sampleVal = ((subBass + bell + crunch) * 31000).toInt()
            samples[i] = sampleVal.coerceIn(-32767, 32767).toShort()
        }
        return samples
    }

    private fun createVictoryFanfare(): ShortArray {
        val noteDurationMs = 85
        val chordDurationMs = 600
        val totalMs = (noteDurationMs * 4) + chordDurationMs
        val totalSamples = (sampleRate * totalMs) / 1000
        val samples = ShortArray(totalSamples)

        val arpeggioFreqs = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
        val noteSampleCount = (sampleRate * noteDurationMs) / 1000

        for (noteIdx in 0..3) {
            val freq = arpeggioFreqs[noteIdx]
            val startSample = noteIdx * noteSampleCount
            for (i in 0 until noteSampleCount) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / noteSampleCount
                val env = (progress * 15.0).coerceAtMost(1.0) * exp(-progress * 3.5)
                val tone = (sin(2.0 * PI * freq * t) * 0.75 + sin(4.0 * PI * freq * t) * 0.25) * env
                val idx = startSample + i
                if (idx < totalSamples) {
                    samples[idx] = (tone * 28000).toInt().coerceIn(-32767, 32767).toShort()
                }
            }
        }

        val chordStart = 4 * noteSampleCount
        val chordSampleCount = totalSamples - chordStart
        for (i in 0 until chordSampleCount) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / chordSampleCount
            val vibrato = 1.0 + 0.015 * sin(2.0 * PI * 5.5 * t)
            val c6 = sin(2.0 * PI * (1046.50 * vibrato) * t) * 0.35
            val e6 = sin(2.0 * PI * (1318.51 * vibrato) * t) * 0.30
            val g6 = sin(2.0 * PI * (1567.98 * vibrato) * t) * 0.25
            val c7 = sin(2.0 * PI * (2093.00 * vibrato) * t) * 0.15
            val env = (progress * 25.0).coerceAtMost(1.0) * exp(-progress * 2.8)
            val chord = (c6 + e6 + g6 + c7) * env
            val idx = chordStart + i
            if (idx < totalSamples) {
                samples[idx] = (chord * 29000).toInt().coerceIn(-32767, 32767).toShort()
            }
        }
        return samples
    }

    private fun createDefeatMelody(): ShortArray {
        val noteDurationMs = 130
        val finalNoteMs = 280
        val totalMs = (noteDurationMs * 2) + finalNoteMs
        val totalSamples = (sampleRate * totalMs) / 1000
        val samples = ShortArray(totalSamples)

        val freqs = doubleArrayOf(392.00, 329.63, 261.63)
        var sampleOffset = 0
        for (noteIdx in 0..2) {
            val freq = freqs[noteIdx]
            val durationThisNote = if (noteIdx == 2) finalNoteMs else noteDurationMs
            val count = (sampleRate * durationThisNote) / 1000
            for (i in 0 until count) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / count
                val env = (progress * 10.0).coerceAtMost(1.0) * exp(-progress * 3.2)
                val tone = (sin(2.0 * PI * freq * t) * 0.8 + sin(4.0 * PI * freq * t) * 0.2) * env
                val idx = sampleOffset + i
                if (idx < totalSamples) {
                    samples[idx] = (tone * 26000).toInt().coerceIn(-32767, 32767).toShort()
                }
            }
            sampleOffset += count
        }
        return samples
    }

    private fun createHammerSmash(durationMs: Int): ShortArray {
        val totalSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(totalSamples)
        var phase = 0.0
        val rng = Random(999)
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val crackEnv = exp(-progress * 35.0)
            val crack = (rng.nextDouble() * 2.0 - 1.0) * crackEnv * 0.5
            val freq = 90.0 * (1.0 - progress * 0.5)
            phase += 2.0 * PI * freq / sampleRate
            val thud = sin(phase) * exp(-progress * 5.0) * 0.65
            val sampleVal = ((crack + thud) * 30000).toInt()
            samples[i] = sampleVal.coerceIn(-32767, 32767).toShort()
        }
        return samples
    }

    private fun createLightningZap(durationMs: Int): ShortArray {
        val totalSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(totalSamples)
        var phase = 0.0
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val freq = 2400.0 * (1.0 - progress * 0.88).pow(1.8)
            phase += 2.0 * PI * freq / sampleRate
            val env = (progress * 15.0).coerceAtMost(1.0) * exp(-progress * 4.5)
            val tone = (sin(phase) * 0.75 + sin(phase * 2.0) * 0.25) * env
            val sampleVal = (tone * 28000).toInt()
            samples[i] = sampleVal.coerceIn(-32767, 32767).toShort()
        }
        return samples
    }

    private fun createWhirlwindSwoosh(durationMs: Int): ShortArray {
        val totalSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(totalSamples)
        var phase = 0.0
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val freq = 320.0 + sin(progress * PI) * 530.0
            phase += 2.0 * PI * freq / sampleRate
            val env = sin(progress * PI).pow(1.5)
            val tone = sin(phase) * env
            val sampleVal = (tone * 26000).toInt()
            samples[i] = sampleVal.coerceIn(-32767, 32767).toShort()
        }
        return samples
    }

    private fun createTap(durationMs: Int): ShortArray {
        val totalSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(totalSamples)
        var phase = 0.0
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val freq = 650.0 - progress * 200.0
            phase += 2.0 * PI * freq / sampleRate
            val env = exp(-progress * 9.0)
            samples[i] = (sin(phase) * env * 22000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return samples
    }

    private fun createSwap(durationMs: Int): ShortArray {
        val totalSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(totalSamples)
        var phase = 0.0
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val freq = 450.0 - progress * 150.0
            phase += 2.0 * PI * freq / sampleRate
            val env = sin(progress * PI)
            samples[i] = (sin(phase) * env * 24000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return samples
    }

    private fun createInvalidSwap(durationMs: Int): ShortArray {
        val totalSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(totalSamples)
        var phase = 0.0
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val pulse = if (progress < 0.45) progress / 0.45 else (progress - 0.5) / 0.5
            val freq = 190.0 - (pulse.coerceIn(0.0, 1.0) * 50.0)
            phase += 2.0 * PI * freq / sampleRate
            val env = if (progress < 0.45) exp(-(progress / 0.45) * 6.0) else if (progress > 0.5) exp(-((progress - 0.5) / 0.5) * 6.0) else 0.0
            samples[i] = (sin(phase) * env * 25000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return samples
    }
}
