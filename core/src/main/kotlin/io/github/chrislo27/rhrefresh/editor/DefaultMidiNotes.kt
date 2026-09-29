package io.github.chrislo27.rhrefresh.editor

enum class DefaultMidiNotes(val cue: String, val localizationKey: String) {

    GLEE_CLUB_SING("gleeClubEn/singLoop", "midinote.gleeClub"),
    BUILT_TO_SCALE("builtToScaleDS/c", "midinote.btsds"),
    BIG_ROCK_FINISH("bigRockFinishMegamixEn/strum", "midinote.bigRockFinish"),
    LAUNCH_PARTY_BOWLING("launchParty/greyCount", "midinote.launchParty"),
    NON_DEFAULT("","");

    companion object {
        val VALUES = DefaultMidiNotes.values().toList()
        val MAP = VALUES.associateBy { it.name }
        fun findByCue(cue: String): DefaultMidiNotes{
            for (value in VALUES) {
                if (value.cue == cue) {
                    return value
                }
            }
            return NON_DEFAULT
        }
    }

}