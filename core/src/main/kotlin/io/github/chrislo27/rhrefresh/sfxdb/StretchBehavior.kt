package io.github.chrislo27.rhrefresh.sfxdb

enum class StretchBehavior(val stretchStart: Boolean = false, val stretchEnd: Boolean = false) {

    stay(stretchStart = false, stretchEnd = false),
    start(stretchStart = false, stretchEnd = true),
    extend(stretchStart = true, stretchEnd = false),
    move(stretchStart = true, stretchEnd = true);

    companion object {
        val VALUES: List<StretchBehavior> = StretchBehavior.entries
    }
}