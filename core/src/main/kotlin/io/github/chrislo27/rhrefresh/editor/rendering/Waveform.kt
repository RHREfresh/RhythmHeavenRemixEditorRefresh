package io.github.chrislo27.rhrefresh.editor.rendering

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import io.github.chrislo27.rhrefresh.editor.Editor
import com.badlogic.gdx.Gdx
import io.github.chrislo27.toolboks.util.gdxutils.fillRect
import net.beadsproject.beads.data.Sample
import io.github.chrislo27.rhrefresh.soundsystem.BeadsSoundSystem
import io.github.chrislo27.rhrefresh.soundsystem.getValues
import java.util.WeakHashMap
import java.lang.ref.WeakReference
import kotlin.math.*

fun Editor.renderLiveWaveform(batch: SpriteBatch, oldCameraX: Float, oldCameraY: Float, adjustedCameraX: Float, adjustedCameraY: Float) {
    batch.setColor(theme.waveform.r, theme.waveform.g, theme.waveform.b, theme.waveform.a * 0.65f)

    val samplesPerSecond = BeadsSoundSystem.audioContext.sampleRate.toInt()
    val samplesPerFrame = samplesPerSecond / 60
    val fineness: Int = (samplesPerFrame / 1).coerceAtMost(samplesPerFrame)

    val isPresentationMode = stage.presentationModeStage.visible
    val viewportWidth = if (isPresentationMode) camera.viewportWidth * 0.5f else camera.viewportWidth
    val height = if (!isPresentationMode) remix.trackCount / 2f else 1f
    val centre = remix.trackCount / 2f

    BeadsSoundSystem.audioContext.getValues(BeadsSoundSystem.sampleArray)

    val data = BeadsSoundSystem.sampleArray
    for (x in 0 until fineness) {
        val dataPoint = data[(x.toFloat() / fineness * data.size).toInt()]
        val h = height * (if (isPresentationMode) abs(dataPoint) else dataPoint.coerceIn(-1f, 1f))
        val width = viewportWidth / fineness.toFloat() * camera.zoom
        if (!isPresentationMode) {
            batch.fillRect((camera.position.x - viewportWidth / 2 * camera.zoom) + x * width,
                centre - h / 2, width, h)
        } else {
            val cameraAdjX = (oldCameraX - adjustedCameraX)
            val cameraAdjY = (oldCameraY - adjustedCameraY)
            batch.fillRect((camera.position.x - cameraAdjX - viewportWidth / 2 * camera.zoom) + x * width,
                -3f - cameraAdjY, width, h / 2)
        }
    }

    batch.setColor(1f, 1f, 1f, 1f)
}

// stores the minimum and maximum volume of a section of audio
class WaveformSummary(val min: Float, val max: Float)

private const val SUMMARY_SIZE = 64
// summaries help reduce the number of reads but arent massively impactful either way

private val waveformCache = WeakHashMap<Sample, WeakReference<Waveform>>()
private fun getWaveform(sample: Sample): Waveform
{
    val cached = waveformCache[sample]?.get()

    if (cached != null)
    {
        return cached
    }

    val waveform = Waveform(sample)
    waveformCache[sample] = WeakReference(waveform)

    return waveform
}

class Waveform(val sample: Sample)
{
    val frameCount = sample.numFrames.toInt()
    val channelCount = sample.numChannels
    val framesPerSecond = sample.sampleRate.toInt()

    private val buffer = FloatArray(channelCount)

    private val numSummaries = (frameCount + SUMMARY_SIZE - 1) / SUMMARY_SIZE
    private val summaries = createSummaries()

    private fun getFrameValue(frame: Int, buffer: FloatArray): Float
    {
        sample.getFrame(frame, buffer)

        var sum = 0f

        for (channel in 0 until channelCount)
        {
            sum += buffer[channel]
        }

        return (sum / channelCount).coerceIn(-1f, 1f)
    }

    private fun calculateSummary(startFrame: Int, endFrame: Int, frameBuffer: FloatArray): WaveformSummary
    {
        var minimum = Float.MAX_VALUE
        var maximum = -Float.MAX_VALUE

        for (frame in startFrame until endFrame)
        {
            val value = getFrameValue(frame, frameBuffer)

            if (value < minimum) minimum = value
            if (value > maximum) maximum = value
        }

        return WaveformSummary(minimum, maximum)
    }

    private fun createSummaries(): Array<WaveformSummary>
    {
        val frameBuffer = FloatArray(channelCount)
        val result = Array(numSummaries) { WaveformSummary(0f, 0f) }
        for (block in 0 until numSummaries)
        {
            val start = block * SUMMARY_SIZE
            val end = min(start + SUMMARY_SIZE, frameCount)

            result[block] = calculateSummary(start, end, frameBuffer)
        }

        return result
    }

    fun secondsToFrame(seconds: Double): Int
    {
        return (seconds * framesPerSecond).toInt()
    }

    fun getSummary(startFrame: Int, endFrame: Int): WaveformSummary
    {
        val start = startFrame.coerceIn(0, frameCount)
        val end = endFrame.coerceIn(start, frameCount)

        var minimum = Float.MAX_VALUE
        var maximum = -Float.MAX_VALUE

        var position = start

        while (position < end)
        {
            if (position % SUMMARY_SIZE == 0 && position + SUMMARY_SIZE <= end)
            {
                val summary = summaries[position / SUMMARY_SIZE]

                minimum = min(minimum, summary.min)
                maximum = max(maximum, summary.max)

                position += SUMMARY_SIZE
            }
            else
            {
                val summaryEnd = min(((position / SUMMARY_SIZE) + 1) * SUMMARY_SIZE, end)

                val summary = calculateSummary(position, summaryEnd, buffer)

                minimum = min(minimum, summary.min)
                maximum = max(maximum, summary.max)

                position = summaryEnd
            }
        }

        return WaveformSummary(minimum, maximum)
    }
}

fun Editor.renderWaveform(batch: SpriteBatch)
{
    // resolution of the bar is halved here
    val resolution = Gdx.graphics.width * 0.15f / 2 / camera.zoom

    val audioSample = remix.music?.music?.audio?.sample ?: return

    val waveform = getWaveform(audioSample)

    val height = remix.trackCount / 2f
    val entryWidth = 1f / resolution

    val firstEntry = floor((camera.position.x - camera.viewportWidth * camera.zoom / 2f) * resolution).toInt()
    val lastEntry = ceil((camera.position.x + camera.viewportWidth * camera.zoom / 2f) * resolution).toInt()

    batch.setColor(theme.waveform.r, theme.waveform.g, theme.waveform.b, theme.waveform.a * 0.65f)

    for (entry in firstEntry until lastEntry)
    {
        val beatStart = entry / resolution
        val beatEnd = (entry + 1) / resolution

        val secondsStart = (remix.tempos.beatsToSeconds(beatStart) - remix.musicStartSec).toDouble()
        val secondsEnd = (remix.tempos.beatsToSeconds(beatEnd) - remix.musicStartSec).toDouble()

        val startFrame = waveform.secondsToFrame(secondsStart)
        val endFrame = waveform.secondsToFrame(secondsEnd)

        val summary = waveform.getSummary(startFrame, endFrame)

        val top = height + summary.max * height / 2f * remix.musicVolumes.volumeAt(beatStart)
        val bottom = height + summary.min * height / 2f * remix.musicVolumes.volumeAt(beatStart)

        batch.fillRect(beatStart, bottom, entryWidth, max(top - bottom, 0.001f)
        )
    }

    batch.setColor(1f, 1f, 1f, 1f)
}