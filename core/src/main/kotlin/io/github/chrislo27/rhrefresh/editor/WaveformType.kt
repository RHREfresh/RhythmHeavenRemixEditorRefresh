package io.github.chrislo27.rhrefresh.editor


enum class WaveformType(val localizationKey: String) {

    WAVEFORM_NONE("waveformType.none"),
    WAVEFORM_LIVE("waveformType.live"),
    WAVEFORM_STATIC("waveformType.static");

    companion object {
        val VALUES = values().toList()
        val MAP = VALUES.associateBy { it.name }
    }

}