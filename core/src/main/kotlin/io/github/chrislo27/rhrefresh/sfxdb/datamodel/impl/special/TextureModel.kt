package io.github.chrislo27.rhrefresh.sfxdb.datamodel.impl.special

import io.github.chrislo27.rhrefresh.entity.model.special.TextureEntity
import io.github.chrislo27.rhrefresh.sfxdb.CuePickerBehavior
import io.github.chrislo27.rhrefresh.sfxdb.Game
import io.github.chrislo27.rhrefresh.sfxdb.datamodel.impl.CuePointer
import io.github.chrislo27.rhrefresh.track.Remix


class TextureModel(game: Game, id: String, deprecatedIDs: List<String>, name: String)
    : SpecialDatamodel(game, id, deprecatedIDs, name, CuePickerBehavior.default, "", 1f) {
    
    override val hideInPresentationMode: Boolean = false

    override fun createEntity(remix: Remix, cuePointer: CuePointer?): TextureEntity {
        return TextureEntity(remix, this)
    }

    override fun dispose() {
    }

}