package io.github.chrislo27.rhrefresh.util

import kotlin.math.abs
import kotlin.math.pow

object Semitones {

    const val SEMITONES_IN_OCTAVE = 12
    const val SEMITONE_VALUE = 1f / SEMITONES_IN_OCTAVE
    const val SHARP = "♯"
    const val FLAT = "♭"

    private val sharpKeyNames = mutableMapOf(
            0 to "C",
            1 to "C$SHARP",
            2 to "D",
            3 to "D$SHARP",
            4 to "E",
            5 to "F",
            6 to "F$SHARP",
            7 to "G",
            8 to "G$SHARP",
            9 to "A",
            10 to "A$SHARP",
            11 to "B")
    private val flatKeyNames = mutableMapOf(
            0 to "C",
            1 to "D$FLAT",
            2 to "D",
            3 to "E$FLAT",
            4 to "E",
            5 to "F",
            6 to "G$FLAT",
            7 to "G",
            8 to "A$FLAT",
            9 to "A",
            10 to "B$FLAT",
            11 to "B")

    private val sharpKeyNamesDoremi = mutableMapOf(
            0 to "Do",
            1 to "Do$SHARP",
            2 to "Ré",
            3 to "Ré$SHARP",
            4 to "Mi",
            5 to "Fa",
            6 to "Fa$SHARP",
            7 to "Sol",
            8 to "Sol$SHARP",
            9 to "La",
            10 to "La$SHARP",
            11 to "Si")
    private val flatKeyNamesDoremi = mutableMapOf(
            0 to "Do",
            1 to "Ré$FLAT",
            2 to "Ré",
            3 to "Mi$FLAT",
            4 to "Mi",
            5 to "Fa",
            6 to "Sol$FLAT",
            7 to "Sol",
            8 to "La$FLAT",
            9 to "La",
            10 to "Si$FLAT",
            11 to "Si")

    enum class PitchStyle(val converter: (Int) -> String, val example: String, val displayName: String) {
        SHARPS({ convertToName(sharpKeyNames, it)}, "A$SHARP", "pitchNoteStyle.sharpsUS"),
        FLATS({ convertToName(flatKeyNames, it) }, "B$FLAT", "pitchNoteStyle.flatsUS"),
        SHARPSDOREMI({ convertToName(sharpKeyNamesDoremi, it)}, "La$SHARP", "pitchNoteStyle.sharpsEU"),
        FLATSDOREMI({ convertToName(flatKeyNamesDoremi, it) }, "Si$FLAT", "pitchNoteStyle.flatsEU"),
        INTEGRAL({ if (it == 0) "0" else if (it < 0) "$it" else "+$it" }, "+3, -5", "pitchNoteStyle.interval");

        val usedKeyNames = mutableMapOf<Int, String>()

        fun getSemitoneName(semitone: Int): String {
            return usedKeyNames.getOrPut(semitone) { converter(semitone) }
        }

        companion object {
            val VALUES = values().toList()

            private fun convertToName(baseMap: Map<Int, String>, semitone: Int): String {
                val abs = abs(Math.floorMod(semitone, SEMITONES_IN_OCTAVE))
                val repeats = kotlin.math.abs(Math.floorDiv(semitone, SEMITONES_IN_OCTAVE))
                return baseMap[abs] +
                        if ((semitone >= 12 || semitone <= -1))
                            (if (repeats > 1) "$repeats" else "") + (if (semitone > 0) "+" else "-")
                        else ""
            }
        }
    }


    private val cachedPitches = mutableMapOf<Float, Float>()
    var pitchStyle: PitchStyle = Semitones.PitchStyle.SHARPS

    fun getSemitoneName(semitone: Int): String {
        return pitchStyle.getSemitoneName(semitone)
    }

    fun getALPitch(semitone: Float): Float {
        return cachedPitches.getOrPut(semitone) { 2.0.pow((semitone * SEMITONE_VALUE).toDouble()).toFloat() }
    }

}