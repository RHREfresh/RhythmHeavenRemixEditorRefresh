package io.github.chrislo27.rhrefresh.entity.model

import io.github.chrislo27.rhrefresh.sfxdb.SFXDatabase
import io.github.chrislo27.rhrefresh.sfxdb.datamodel.ContainerModel
import io.github.chrislo27.rhrefresh.sfxdb.datamodel.impl.Cue
import io.github.chrislo27.rhrefresh.util.Semitones


interface IRepitchable {

    companion object {

        fun anyInModel(model: ContainerModel): Lazy<Boolean> {
            return lazy {
                model.cues.any {
                    (SFXDatabase.data.objectMap[it.id] as? Cue)?.repitchable == true
                }
            }
        }

        val DEFAULT_RANGE: IntRange = -(Semitones.SEMITONES_IN_OCTAVE * 2)..(Semitones.SEMITONES_IN_OCTAVE * 2)
    }

    var semitone: Int
    val canBeRepitched: Boolean
    val ignorePitching: Boolean
    val semitoneRange: IntRange
        get() = DEFAULT_RANGE
    val rangeWrapsAround: Boolean
        get() = false
    val persistSemitoneData: Boolean
        get() = true
    val showPitchOnTooltip: Boolean
        get() = true

}