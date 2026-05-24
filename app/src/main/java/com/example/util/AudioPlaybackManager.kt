package com.example.util

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

object AudioPlaybackManager {

    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Plays a crisp pop/click sound for likes or tab switches.
     */
    fun playPopSound() {
        scope.launch {
            val sampleRate = 44100
            val durationMs = 80
            val numSamples = durationMs * sampleRate / 1000
            val sample = ShortArray(numSamples)

            val fadeSamples = 200
            for (i in 0 until numSamples) {
                // High frequency burst
                val frequency = 800.0 - (i * 200.0 / numSamples)
                var value = sin(2.0 * Math.PI * i / (sampleRate / frequency))
                
                // Envelope
                val envelope = when {
                    i < fadeSamples -> i.toFloat() / fadeSamples
                    i > numSamples - fadeSamples -> (numSamples - i).toFloat() / fadeSamples
                    else -> 1f
                }
                sample[i] = (value * 32767 * envelope * 0.5f).toInt().toShort()
            }

            playSound(sample, sampleRate)
        }
    }

    /**
     * Plays a futuristic 'woosh' sound for sending messages or reels
     */
    fun playWooshSound() {
        scope.launch {
            val sampleRate = 44100
            val durationMs = 300
            val numSamples = durationMs * sampleRate / 1000
            val sample = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                // Sweeping frequency from low to high with white noise
                val frequency = 100.0 + (i * 500.0 / numSamples)
                val noise = (Math.random() * 2.0 - 1.0) * 0.3
                val value = sin(2.0 * Math.PI * i / (sampleRate / frequency)) + noise
                
                // Smooth bell envelope
                val envelope = sin(Math.PI * i / numSamples)
                sample[i] = (value * 32767 * envelope * 0.4f).toInt().toShort()
            }

            playSound(sample, sampleRate)
        }
    }

    private suspend fun playSound(audioData: ShortArray, sampleRate: Int) {
        try {
            val audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                audioData.size * 2,
                AudioTrack.MODE_STATIC
            )
            audioTrack.write(audioData, 0, audioData.size)
            audioTrack.play()
            
            kotlinx.coroutines.delay(1000)
            audioTrack.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
