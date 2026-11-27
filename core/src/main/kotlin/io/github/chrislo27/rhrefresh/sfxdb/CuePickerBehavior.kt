package io.github.chrislo27.rhrefresh.sfxdb

enum class CuePickerBehavior() {

    default(),
    hide(),
    above(),
    below();

    companion object {
        val VALUES: List<CuePickerBehavior> = CuePickerBehavior.entries
    }
}