package io.github.chrislo27.rhrefresh.editor.view


enum class ViewType(val tag: String) {

    GAME_BOUNDARIES("gameBoundaries"),
    WAVEFORM("waveform"),
    GLEE_CLUB("gleeClub");

    companion object {
        val VALUES = values().toList()
    }

}