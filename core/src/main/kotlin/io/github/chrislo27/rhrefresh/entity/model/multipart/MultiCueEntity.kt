package io.github.chrislo27.rhrefresh.entity.model.multipart

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.chrislo27.rhrefresh.editor.Editor
import io.github.chrislo27.rhrefresh.entity.model.IRepitchable
import io.github.chrislo27.rhrefresh.entity.model.IStretchable
import io.github.chrislo27.rhrefresh.entity.model.IVolumetric
import io.github.chrislo27.rhrefresh.entity.model.MultipartEntity
import io.github.chrislo27.rhrefresh.sfxdb.SFXDatabase
import io.github.chrislo27.rhrefresh.sfxdb.datamodel.impl.MultiCue
import io.github.chrislo27.rhrefresh.theme.Theme
import io.github.chrislo27.rhrefresh.track.Remix


class MultiCueEntity(remix: Remix, datamodel: MultiCue)
    : MultipartEntity<MultiCue>(remix, datamodel), IStretchable {

    override val canBeRepitched: Boolean by IRepitchable.anyInModel(datamodel)
    override val isStretchable: Boolean = datamodel.stretchable
    val canSplit = datamodel.canSplit
    override val shouldRenderInternal = canSplit


    val shouldCrop = datamodel.shouldCrop
    val minLength = datamodel.minLength
    val maxLength = datamodel.maxLength

    init {
        bounds.width = datamodel.duration
        bounds.height = datamodel.height
        populate()
    }

    override fun getRenderColor(editor: Editor, theme: Theme): Color {
        return if(canSplit) {theme.entities.multiCue} else {theme.entities.cue}
    }

    override fun onBoundsChange(old: Rectangle) {
        bounds.width = bounds.width.coerceAtLeast(minLength)

        if (maxLength != 0f) {
            bounds.width = bounds.width.coerceAtMost(maxLength)
        }

        super.onBoundsChange(old)
    }

    override fun updateInternalCache(oldBounds: Rectangle) {
        translateInternal(oldBounds)
        val widthChanged = oldBounds.width != bounds.width

        if (widthChanged) {
            populate()
        }
    }


    fun populate() {

        val thisSemitone = semitone
        val thisVolume = volumePercent

        // Set semitone and volume to zero, repopulate, and reset it so the setters in MultipartEntity take effect
        semitone = 0
        volumePercent = IVolumetric.DEFAULT_VOLUME

        internal.clear()

        datamodel.cues.forEach { pointer ->
            val originalStart = pointer.beat
            val originalEnd = pointer.beat + pointer.duration

            val currentStart = if (pointer.stretchBehavior.stretchEnd == true) {
                originalStart + (bounds.width - datamodel.duration)
            } else {
                originalStart
            }

            val currentEnd = if (pointer.stretchBehavior.stretchStart == true) {
                bounds.width + (originalEnd - datamodel.duration)
            } else {
                originalEnd
            }

            val childStart = bounds.x + currentStart
            val parentEnd = bounds.x + bounds.width

            // crop out children that exceed parents end (or for invalid lengths)
            if ((shouldCrop && (childStart >= parentEnd || currentStart < 0f)) ||
                currentEnd <= currentStart) {
                return@forEach
            }

            val entity = SFXDatabase.data.objectMap[pointer.id]?.createEntity(remix, pointer)
                ?: error("Object with id ${pointer.id} not found")

            entity.updateBounds {
                entity.bounds.x = childStart
                entity.bounds.y = bounds.y + pointer.track
                entity.bounds.width = currentEnd - currentStart
            }
            internal.add(entity)
        }
        // Re-set semitone and volume so it takes effect in the internals
        semitone = thisSemitone
        volumePercent = thisVolume
    }

    override fun readData(objectNode: ObjectNode) {
        super.readData(objectNode)
        if (!datamodel.stretchable) {
            if (bounds.width != datamodel.duration) {
                // The new duration overrides the persisted one if the pattern is not stretchable
                updateBounds {
                    bounds.width = datamodel.duration
                }
            }
        }
    }

    override fun copy(remix: Remix): MultiCueEntity {
        return MultiCueEntity(remix, datamodel).also {
            it.updateBounds {
                it.bounds.set(this@MultiCueEntity.bounds)
            }
            it.semitone = this.semitone
            it.volumePercent = this.volumePercent
        }
    }
}