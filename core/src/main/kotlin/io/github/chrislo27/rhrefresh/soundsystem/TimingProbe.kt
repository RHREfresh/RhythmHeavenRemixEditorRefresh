package io.github.chrislo27.rhrefresh.soundsystem

import net.beadsproject.beads.core.Bead
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.PrintWriter

/**
 * Measuring tool for playback timing (development only; does nothing unless RHRE_TIMING_RECORD is set).
 *
 * - RHRE_TIMING_RECORD=path : writes exactly what the audio engine outputs (big-endian float32, stereo,
 *   interleaved) to `path`, and what the program did (frames, cue starts, music start) to `path.events`.
 * - RHRE_TIMING_RECORD_MUTE=1 : also silences the speakers while recording.
 * - RHRE_TIMING=legacy : use the old frame-based timing, for comparison (see [AudioScheduler]).
 */
object TimingProbe {

    private val path: String? = System.getenv("RHRE_TIMING_RECORD")
    val enabled: Boolean = path != null
    private val mute = System.getenv("RHRE_TIMING_RECORD_MUTE") == "1"

    private var attached = false
    private var out: DataOutputStream? = null
    private var events: PrintWriter? = null

    @Synchronized
    fun attach() {
        if (!enabled || attached) return
        attached = true
        val ctx = BeadsSoundSystem.audioContext
        val o = DataOutputStream(BufferedOutputStream(FileOutputStream(path), 1 shl 16))
        out = o
        events = PrintWriter(FileWriter("$path.events"), true).also {
            it.println("# sampleRate=${ctx.sampleRate} bufferSize=${ctx.bufferSize} exact=${AudioScheduler.enabled}")
        }
        ctx.invokeAfterEveryFrame(object : Bead() {
            override fun messageReceived(message: Bead?) {
                val l = ctx.out.getOutBuffer(0)
                val r = ctx.out.getOutBuffer(1)
                for (i in 0 until ctx.bufferSize) {
                    o.writeFloat(l[i])
                    o.writeFloat(r[i])
                }
                o.flush()
                if (mute) {
                    java.util.Arrays.fill(l, 0f)
                    java.util.Arrays.fill(r, 0f)
                }
            }
        })
    }

    /** One line: kind, audio block number, nanoTime, then the given fields. */
    fun event(kind: String, vararg fields: Any?) {
        val e = events ?: return
        e.println(kind + "\t" + BeadsSoundSystem.audioContext.timeStep + "\t" + System.nanoTime() + "\t" +
                          fields.joinToString("\t"))
    }
}
