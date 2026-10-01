package io.github.chrislo27.rhrefresh.track.tracker.tempo

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import io.github.chrislo27.rhrefresh.editor.Editor
import io.github.chrislo27.rhrefresh.theme.Theme
import io.github.chrislo27.rhrefresh.track.tracker.Tracker
import io.github.chrislo27.rhrefresh.util.Swing
import io.github.chrislo27.rhrefresh.util.SwingUtils
import io.github.chrislo27.rhrefresh.util.TempoUtils
import java.util.*


class TempoChange(container: TempoChanges, beat: Float, val bpm: Float, val swing: Swing, width: Float, immutable: Boolean = false)
    : Tracker<TempoChange>(container, beat, width) {

    companion object {
        val MIN_TEMPO: Float = 5.0f
        val MAX_TEMPO: Float = 600f

        fun getSecondsDuration(beatWidth: Float, startBpm: Float, endBpm: Float): Float {
            return ((2 * beatWidth) / (startBpm + endBpm)) * 60f
        }

        fun getBeatDuration(secondsWidth: Float, startBpm: Float, endBpm: Float): Float {
            return (secondsWidth / 60f) * (startBpm + endBpm) / 2f
        }

        fun getFormattedText(bpm: Float): String = "♩=${Editor.TEMPO_DECIMAL_PLACES_FORMATTER.format(bpm)}"
    }

    override val allowsResize: Boolean = true
    var seconds: Float = 0f
    var widthSeconds: Float = 0f

    val endSeconds: Float
        get() = seconds + widthSeconds

    val previousBpm: Float
        get() = (container.map as NavigableMap).lowerEntry(beat)?.value?.bpm ?: (container as TempoChanges).defaultTempo

    init {
        super.immutable = immutable
        text = getFormattedText(bpm)
    }

    override fun scroll(amount: Int, control: Boolean, shift: Boolean): TempoChange? {
        val change = amount * (if (shift) (if (control) 0.05f else 0.01f) else (if (control) 5f else 1f))

        if ((change < 0 && bpm <= MIN_TEMPO) || (change > 0 && bpm >= MAX_TEMPO))
            return null
        if (immutable) {
            (container as TempoChanges).defaultTempo = bpm+change
        }
        return TempoChange(container as TempoChanges, beat, (bpm + change).coerceIn(MIN_TEMPO, MAX_TEMPO), swing, width, immutable)
    }

    fun scrollSwing(amount: Int, control: Boolean, shift: Boolean): TempoChange? {
        if (shift && !control) {
            return TempoChange(container as TempoChanges, beat, bpm, swing.copy(division = if (swing.division == Swing.EIGHTH_DIVISION) Swing.SIXTEENTH_DIVISION else Swing.EIGHTH_DIVISION), width, immutable)
        } else if (control) {
            val change = amount * (if (shift) 5 else 1)
            if ((change < 0 && swing.ratio > Swing.MIN_SWING) || (change > 0 && swing.ratio < Swing.MAX_SWING)) {
                return TempoChange(container as TempoChanges, beat, bpm, swing.copy(ratio = (swing.ratio + change).coerceIn(Swing.MIN_SWING, Swing.MAX_SWING)), width, immutable)
            }
        } else {
            val list = Swing.SWING_LIST
            val currentIndex: Int = if (swing.ratio < list.first().ratio) -1 else list.let { _ ->
                var last: Int = 0

                for (it in list.indices) {
                    if (this.swing.ratio > list[last].ratio)
                        last = it
                }

                last
            }

            val nextIndex: Int = if (currentIndex == -1) {
                0
            } else {
                val futureNext = currentIndex + amount
                when {
                    futureNext < 0 -> list.size - 1
                    futureNext >= list.size -> 0
                    else -> futureNext
                }
            }

            if (nextIndex != currentIndex) {
                return TempoChange(container as TempoChanges, beat, bpm, swing.copy(ratio = list[nextIndex].ratio), width, immutable)
            }
        }

        return null
    }

    override fun createResizeCopy(beat: Float, width: Float): TempoChange {
        return TempoChange(container as TempoChanges, beat, bpm, swing, width, immutable)
    }

    override fun getColour(theme: Theme): Color {
        if (immutable) {
            val colour = Color(theme.trackers.tempoChange)
            colour.r -= 0.2f
            colour.g -= 0.2f
            colour.b -= 0.2f
            return colour
        } else {
            return theme.trackers.tempoChange
        }
    }

    fun secondsToBeats(seconds: Float): Float {
        val secondsWidth = seconds - this.seconds
        return if (seconds >= endSeconds) {
            endBeat + SwingUtils.linearToSwing(TempoUtils.secondsToBeats(seconds - this.endSeconds, tempoAtSeconds(seconds)), swing)
        } else {
            beat + SwingUtils.linearToSwing(getBeatDuration(secondsWidth, previousBpm, tempoAtSeconds(seconds)), swing)
        }
    }

    fun beatsToSeconds(beat: Float): Float {
        val beatWidth = beat - this.beat
        return if (beat >= endBeat) {
            endSeconds + TempoUtils.beatsToSeconds(SwingUtils.swingToLinear(beat - this.endBeat, swing), tempoAt(beat))
        } else {
            seconds + SwingUtils.swingToLinear(getSecondsDuration(beatWidth, previousBpm, tempoAt(beat)), swing)
        }
    }

    // Stretchable tempo changes

    fun tempoAt(beat: Float): Float {
        val endBeat = this.endBeat
        return if (!isZeroWidth && beat in this.beat..endBeat) {
            MathUtils.lerp(this.previousBpm, this.bpm, (beat - this.beat) / width)
        } else {
            bpm
        }
    }

    fun tempoAtSeconds(seconds: Float): Float {
        return if (!isZeroWidth && seconds in this.seconds..this.endSeconds) {
            MathUtils.lerp(this.previousBpm, this.bpm, (seconds - this.seconds) / widthSeconds)
        } else {
            bpm
        }
    }

}