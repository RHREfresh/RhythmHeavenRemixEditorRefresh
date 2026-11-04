package io.github.chrislo27.rhrefresh.entity.model.multipart

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.chrislo27.rhrefresh.editor.Editor
import io.github.chrislo27.rhrefresh.entity.model.IRepitchable
import io.github.chrislo27.rhrefresh.entity.model.IStretchable
import io.github.chrislo27.rhrefresh.entity.model.MultipartEntity
import io.github.chrislo27.rhrefresh.sfxdb.SFXDatabase
import io.github.chrislo27.rhrefresh.sfxdb.datamodel.impl.MultiCue
import io.github.chrislo27.rhrefresh.theme.Theme
import io.github.chrislo27.rhrefresh.track.Remix


class MultiCueEntity(remix: Remix, datamodel: MultiCue)
    : MultipartEntity<MultiCue>(remix, datamodel), IStretchable {

    override val canBeRepitched: Boolean by IRepitchable.anyInModel(datamodel)
    override val isStretchable: Boolean = datamodel.stretchable
    override val shouldRenderInternal = datamodel.canSplit
    val canSplit = datamodel.canSplit

    init {
        datamodel.cues.mapTo(internal) { pointer ->
            SFXDatabase.data.objectMap[pointer.id]?.createEntity(remix, pointer)?.apply {
                this.updateBounds {
                    this.bounds.x = this@MultiCueEntity.bounds.x + pointer.beat
                    this.bounds.y = this@MultiCueEntity.bounds.y + pointer.track
                    this.bounds.width = datamodel.width
                }
            } ?: error("Object with id ${pointer.id} not found")
        }

        this.bounds.width = datamodel.width
            //internal .maxByOrNull { it.bounds.x + it.bounds.width }?.run { this.bounds.x + this.bounds.width - this@MultiCueEntity.bounds.x } ?: error("Nothing in internal cache")
        this.bounds.height = 1f
            //internal.maxByOrNull { it.bounds.y + it.bounds.height }?.run { this.bounds.y + this.bounds.height - this@MultiCueEntity.bounds.y } ?: error("Nothing in internal cache")
    }

    override fun getRenderColor(editor: Editor, theme: Theme): Color {
        return if(datamodel.canSplit) {theme.entities.multiCue} else {theme.entities.cue}
    }

    override fun updateInternalCache(oldBounds: Rectangle) {
        translateInternal(oldBounds, changeWidths = true, scaleBeats = false)
    }

    override fun readData(objectNode: ObjectNode) {
        super.readData(objectNode)
        if (!datamodel.stretchable) {
            if (bounds.width != datamodel.width) {
                // The new duration overrides the persisted one if the pattern is not stretchable
                updateBounds {
                    bounds.width = datamodel.width
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