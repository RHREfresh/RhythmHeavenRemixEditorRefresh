package io.github.chrislo27.rhrefresh.sfxdb.datamodel.impl

import io.github.chrislo27.rhrefresh.entity.model.multipart.MultiCueEntity
import io.github.chrislo27.rhrefresh.sfxdb.Game
import io.github.chrislo27.rhrefresh.sfxdb.SFXDatabase
import io.github.chrislo27.rhrefresh.sfxdb.datamodel.ContainerModel
import io.github.chrislo27.rhrefresh.sfxdb.datamodel.Datamodel
import io.github.chrislo27.rhrefresh.sfxdb.datamodel.PreviewableModel
import io.github.chrislo27.rhrefresh.track.Remix

class MultiCue(game: Game, id: String, deprecatedIDs: List<String>, name: String, subtext: String = "",
               override val cues: List<CuePointer>, val stretchable: Boolean, val width: Float, val canSplit: Boolean)
    : Datamodel(game, id, deprecatedIDs, name, subtext), ContainerModel, PreviewableModel {

    override val canBePreviewed: Boolean by lazy { PreviewableModel.determineFromCuePointers(cues) }

    val repitchable: Boolean by lazy {
        cues.any {
            (SFXDatabase.data.objectMap[it.id] as? Cue)?.repitchable == true
        }
    }

    override val duration: Float by lazy {
        cues.map { it.beat + it.duration }.maxOrNull()!!
    }

    override fun createEntity(remix: Remix,
                              cuePointer: CuePointer?): MultiCueEntity {
        return MultiCueEntity(remix, this).apply {
            if (cuePointer != null) {
                semitone = cuePointer.semitone
                volumePercent = cuePointer.volume
            }
        }
    }

    override fun dispose() {
    }

}
