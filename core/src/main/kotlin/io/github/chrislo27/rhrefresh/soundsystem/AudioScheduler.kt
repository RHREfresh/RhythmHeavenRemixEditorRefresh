package io.github.chrislo27.rhrefresh.soundsystem

import net.beadsproject.beads.core.AudioContext
import net.beadsproject.beads.core.Bead
import net.beadsproject.beads.ugens.SamplePlayer
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.roundToLong

/**
 * Starts sounds on the exact audio sample they belong to.
 *
 * RHRE used to start every sound from the drawing loop: a sound only started on the next drawn frame
 * and then on the next audio block, so each one was late by a different amount (measured: up to
 * ±30 ms, the spacing of a steady clap varying between 476 and 534 ms at 120 BPM).
 *
 * Instead, the remix asks for a sound a little ahead of time ([schedule]). On the audio thread, at the
 * start of every block, the pending sounds are started with a negative sample position: the sample
 * player then outputs exactly that much silence first, so the sound begins on the right sample.
 * The music is started the same way, so sounds and music share one clock.
 *
 * Remix time is mapped onto the audio stream with an anchor: remix second [anchorSeconds] plays at
 * audio sample [anchorSample]; later times follow at the playback speed.
 */
object AudioScheduler {

    /** How far behind "play" pressed the remix starts, so the first sounds can still be placed exactly. */
    private const val START_LEAD_SECONDS = 0.1

    /**
     * How far ahead the remix asks for its sounds. Must cover a slow frame plus one audio block;
     * a stall of 0.26 s was seen in testing, so leave plenty. A sound asked for too late still plays,
     * just late (never cut).
     */
    const val LOOKAHEAD_SECONDS = 0.3f

    /** Set RHRE_TIMING=legacy to use the old frame-based timing (for comparison). */
    val enabled: Boolean = (System.getenv("RHRE_TIMING") ?: "").lowercase() != "legacy"

    private class Pending(val atSeconds: Double, val start: (delayMs: Double) -> Unit)

    /**
     * Players waiting through their silent lead-in. A sample player that is set to remove itself at
     * its end treats a negative position as "outside the sample" and removes itself at once, so that
     * is switched on only after the lead-in. Only touched on the audio thread.
     */
    private val leadIns = ArrayList<SamplePlayer>()

    /** Called from a scheduled start (audio thread): remove [player] at its end once it really started. */
    fun killOnEndAfterLeadIn(player: SamplePlayer) {
        player.killOnEnd = false
        leadIns.add(player)
    }

    private val queue = ConcurrentLinkedQueue<Pending>()
    @Volatile private var pendingAnchor: DoubleArray? = null   // [seconds, speed]
    @Volatile private var anchorSample: Long = -1L
    @Volatile private var anchorSeconds: Double = 0.0
    @Volatile private var speed: Double = 1.0
    @Volatile private var installedOn: AudioContext? = null

    /** True while a playback is running that sounds can be scheduled into. */
    val active: Boolean
        get() = enabled && (anchorSample >= 0 || pendingAnchor != null)

    private val beforeBlock = object : Bead() {
        override fun messageReceived(message: Bead?) {
            val ctx = installedOn ?: return
            if (leadIns.isNotEmpty()) {
                val it = leadIns.iterator()
                while (it.hasNext()) {
                    val p = it.next()
                    if (p.isDeleted) {
                        it.remove()
                    } else if (p.position >= 0) {
                        p.killOnEnd = true
                        if (p.position >= p.sample.length) p.kill()
                        it.remove()
                    }
                }
            }
            val blockStart = ctx.timeStep * ctx.bufferSize
            val sr = ctx.sampleRate.toDouble()
            pendingAnchor?.let {
                anchorSample = blockStart + (START_LEAD_SECONDS * sr).roundToLong()
                anchorSeconds = it[0]
                speed = it[1]
                pendingAnchor = null
            }
            if (anchorSample < 0) return
            while (true) {
                val p = queue.poll() ?: break
                val target = anchorSample + ((p.atSeconds - anchorSeconds) / speed * sr).roundToLong()
                if (TimingProbe.enabled) TimingProbe.event("START", p.atSeconds, target - blockStart)
                val delaySamples = (target - blockStart).coerceAtLeast(0L)   // never cut a sound short
                try {
                    p.start(delaySamples * 1000.0 / sr)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun ensureInstalled() {
        val ctx = BeadsSoundSystem.audioContext
        if (installedOn !== ctx) {
            installedOn?.stopInvokingBeforeEveryFrame(beforeBlock)
            ctx.invokeBeforeEveryFrame(beforeBlock)
            installedOn = ctx
        }
    }

    /** Playback starts (or resumes) at remix second [seconds]. */
    fun begin(seconds: Double, playbackSpeed: Float) {
        if (!enabled || !BeadsSoundSystem.isRealtime) return
        ensureInstalled()
        queue.clear()
        anchorSample = -1L
        pendingAnchor = doubleArrayOf(seconds, playbackSpeed.toDouble().coerceAtLeast(0.01))
    }

    /** Playback stopped or paused: forget everything not started yet. */
    fun end() {
        pendingAnchor = null
        // (leadIns is cleared on the audio thread as the players are stopped and deleted)
        anchorSample = -1L
        queue.clear()
    }

    /**
     * Runs [start] on the audio thread so that a sound begins at remix second [atSeconds].
     * [start] receives how many milliseconds of real time are left before that moment.
     * Returns false (and does nothing) when no playback is running.
     */
    fun schedule(atSeconds: Double, start: (delayMs: Double) -> Unit): Boolean {
        if (!active) return false
        queue.add(Pending(atSeconds, start))
        return true
    }

    /** The remix time that the audio is playing right now (block accurate), or null if not playing. */
    fun clockSeconds(): Double? {
        val ctx = installedOn ?: return null
        if (!active || anchorSample < 0) return null
        return anchorSeconds + (ctx.timeStep * ctx.bufferSize - anchorSample) / ctx.sampleRate.toDouble() * speed
    }
}
