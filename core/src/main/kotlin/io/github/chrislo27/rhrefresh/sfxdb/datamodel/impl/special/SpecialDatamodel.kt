package io.github.chrislo27.rhrefresh.sfxdb.datamodel.impl.special

import io.github.chrislo27.rhrefresh.sfxdb.CuePickerBehavior
import io.github.chrislo27.rhrefresh.sfxdb.Game
import io.github.chrislo27.rhrefresh.sfxdb.datamodel.Datamodel


abstract class SpecialDatamodel(game: Game, id: String, deprecatedIDs: List<String>, name: String, cuePickerBehaviour: CuePickerBehavior = CuePickerBehavior.default, subtext: String, duration: Float)
    : Datamodel(game, id, deprecatedIDs, name, duration, cuePickerBehaviour, subtext)