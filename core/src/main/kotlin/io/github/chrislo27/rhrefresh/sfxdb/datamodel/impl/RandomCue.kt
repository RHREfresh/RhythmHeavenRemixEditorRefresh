package io.github.chrislo27.rhrefresh.sfxdb.datamodel.impl

import io.github.chrislo27.rhrefresh.entity.model.multipart.RandomCueEntity
import io.github.chrislo27.rhrefresh.sfxdb.CuePickerBehavior
import io.github.chrislo27.rhrefresh.sfxdb.Game
import io.github.chrislo27.rhrefresh.sfxdb.datamodel.ContainerModel
import io.github.chrislo27.rhrefresh.sfxdb.datamodel.Datamodel
import io.github.chrislo27.rhrefresh.sfxdb.datamodel.PreviewableModel
import io.github.chrislo27.rhrefresh.sfxdb.datamodel.ResponseModel
import io.github.chrislo27.rhrefresh.track.Remix

class RandomCue(game: Game, id: String, deprecatedIDs: List<String>, name: String, cuePickerBehavior: CuePickerBehavior = CuePickerBehavior.default, subtext: String = "",
                override val cues: List<CuePointer>, override val responseIDs: List<String>, val stretchable: Boolean)
    : Datamodel(game, id, deprecatedIDs, name, cuePickerBehavior, subtext), ContainerModel, ResponseModel, PreviewableModel {

    override val canBePreviewed: Boolean by lazy { PreviewableModel.determineFromCuePointers(cues) }

    override val duration: Float by lazy {
        cues.maxByOrNull(CuePointer::duration)?.duration ?: error("No cues found")
    }

    override fun createEntity(remix: Remix,
                              cuePointer: CuePointer?): RandomCueEntity {
        return RandomCueEntity(remix, this).apply {
            if (cuePointer != null) {
                semitone = cuePointer.semitone
                volumePercent = cuePointer.volume
            }
        }
    }

    override fun dispose() {
    }

}
