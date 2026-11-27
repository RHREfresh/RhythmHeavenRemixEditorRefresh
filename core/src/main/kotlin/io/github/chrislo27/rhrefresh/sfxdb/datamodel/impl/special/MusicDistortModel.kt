package io.github.chrislo27.rhrefresh.sfxdb.datamodel.impl.special

import io.github.chrislo27.rhrefresh.entity.model.special.MusicDistortEntity
import io.github.chrislo27.rhrefresh.sfxdb.CuePickerBehavior
import io.github.chrislo27.rhrefresh.sfxdb.Game
import io.github.chrislo27.rhrefresh.sfxdb.datamodel.impl.CuePointer
import io.github.chrislo27.rhrefresh.track.Remix


class MusicDistortModel(game: Game, id: String, deprecatedIDs: List<String>, name: String)
    : SpecialDatamodel(game, id, deprecatedIDs, name, CuePickerBehavior.default, "[LIGHT_GRAY]Applies bandpass filter on music[]", 1f) {

    override val hideInPresentationMode: Boolean = true

    override fun createEntity(remix: Remix,
                              cuePointer: CuePointer?): MusicDistortEntity {
        return MusicDistortEntity(remix, this)
    }

    override fun dispose() {
    }
}