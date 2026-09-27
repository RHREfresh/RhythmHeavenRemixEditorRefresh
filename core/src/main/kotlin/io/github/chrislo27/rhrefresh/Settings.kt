package io.github.chrislo27.rhrefresh

import com.badlogic.gdx.Preferences
import io.github.chrislo27.rhrefresh.PreferenceKeys.ADVOPT_EXPLODING_ENTITIES
import io.github.chrislo27.rhrefresh.PreferenceKeys.ADVOPT_IGNORE_PITCH_RESTRICTIONS
import io.github.chrislo27.rhrefresh.PreferenceKeys.MIDI_NOTE
import io.github.chrislo27.rhrefresh.PreferenceKeys.SETTINGS_ADVANCED_OPTIONS
import io.github.chrislo27.rhrefresh.PreferenceKeys.SETTINGS_CHORUS_KIDS
import io.github.chrislo27.rhrefresh.PreferenceKeys.SETTINGS_DISABLE_MINIMAP
import io.github.chrislo27.rhrefresh.PreferenceKeys.SETTINGS_DISABLE_TIME_STRETCHING
import io.github.chrislo27.rhrefresh.PreferenceKeys.SETTINGS_GAME_BOUNDARIES
import io.github.chrislo27.rhrefresh.PreferenceKeys.SETTINGS_GLASS_ENTITIES
import io.github.chrislo27.rhrefresh.PreferenceKeys.SETTINGS_LIVE_WAVEFORM
import io.github.chrislo27.rhrefresh.PreferenceKeys.SETTINGS_MINIMAP_PREVIEW
import io.github.chrislo27.rhrefresh.PreferenceKeys.SETTINGS_SMOOTH_DRAGGING
import io.github.chrislo27.rhrefresh.PreferenceKeys.SETTINGS_SUBTITLE_ORDER
import io.github.chrislo27.rhrefresh.PreferenceKeys.SETTINGS_NEW_TRACKS_ON_TOP
import io.github.chrislo27.rhrefresh.PreferenceKeys.SETTINGS_ORDER_BY_GAME_ORDER
import io.github.chrislo27.rhrefresh.PreferenceKeys.SETTINGS_ENABLE_ANALYTICS
import io.github.chrislo27.rhrefresh.PreferenceKeys.THEME_USES_MENU
import io.github.chrislo27.rhrefresh.editor.CameraBehaviour
import io.github.chrislo27.rhrefresh.editor.Editor


class Settings(private val main: RHREfreshApplication) {
    
    private val preferences: Preferences get() = main.preferences
    
    var advancedOptions: Boolean = false
    var disableTimeStretching: Boolean = false
    var themeUsesMenu: Boolean = false
    var glassEntities: Boolean = false
    var disableMinimap: Boolean = false
    var minimapPreview: Boolean = true
    var subtitlesBelow: Boolean = false
    var smoothDragging: Boolean = true
    var cameraBehaviour: CameraBehaviour = Editor.DEFAULT_CAMERA_BEHAVIOUR
    var gameBoundaries: Boolean = false
    var newTracksOnTop: Boolean = true //
    var orderByGameOrder: Boolean = true //
    var enableAnalytics: Boolean = false //

    var advExplodingEntities: Boolean = false
    var advIgnorePitchRestrictions: Boolean = false

    var liveWaveform: Boolean = false
    var chorusKids: Boolean = false
    var midiNote: String = "gleeClubEn/singLoop"

    fun load() {
        advancedOptions = preferences.getBoolean(SETTINGS_ADVANCED_OPTIONS, advancedOptions)
        disableTimeStretching = preferences.getBoolean(SETTINGS_DISABLE_TIME_STRETCHING, disableTimeStretching)
        themeUsesMenu = preferences.getBoolean(THEME_USES_MENU, themeUsesMenu)
        glassEntities = preferences.getBoolean(SETTINGS_GLASS_ENTITIES, glassEntities)
        disableMinimap = preferences.getBoolean(SETTINGS_DISABLE_MINIMAP, disableMinimap)
        minimapPreview = preferences.getBoolean(SETTINGS_MINIMAP_PREVIEW, minimapPreview)
        subtitlesBelow = preferences.getBoolean(SETTINGS_SUBTITLE_ORDER, subtitlesBelow)
        smoothDragging = preferences.getBoolean(SETTINGS_SMOOTH_DRAGGING, smoothDragging)
        liveWaveform = preferences.getBoolean(SETTINGS_LIVE_WAVEFORM, liveWaveform)
        chorusKids = preferences.getBoolean(SETTINGS_CHORUS_KIDS, chorusKids)
        gameBoundaries = preferences.getBoolean(SETTINGS_GAME_BOUNDARIES, gameBoundaries)
        newTracksOnTop = preferences.getBoolean(SETTINGS_NEW_TRACKS_ON_TOP, newTracksOnTop)
        orderByGameOrder = preferences.getBoolean(SETTINGS_ORDER_BY_GAME_ORDER, orderByGameOrder)
        enableAnalytics = preferences.getBoolean(SETTINGS_ENABLE_ANALYTICS, enableAnalytics)
        midiNote = preferences.getString(MIDI_NOTE, midiNote)
        val oldChaseCamera = "settings_chaseCamera"
        if (oldChaseCamera in preferences) {
            // Retroactively apply settings
            val oldSetting = preferences.getBoolean(oldChaseCamera, true)
            cameraBehaviour = if (oldSetting) CameraBehaviour.FOLLOW_PLAYBACK else CameraBehaviour.PAN_OVER_INSTANT
            // Delete
            preferences.remove(oldChaseCamera)
            preferences.flush()
        } else {
            cameraBehaviour = CameraBehaviour.MAP.getOrDefault(preferences.getString(PreferenceKeys.SETTINGS_CAMERA_BEHAVIOUR), Editor.DEFAULT_CAMERA_BEHAVIOUR)
        }

        advExplodingEntities = preferences.getBoolean(ADVOPT_EXPLODING_ENTITIES, advExplodingEntities)
        advIgnorePitchRestrictions = preferences.getBoolean(ADVOPT_IGNORE_PITCH_RESTRICTIONS, advIgnorePitchRestrictions)
    }
    
    fun persist() {
        preferences
                .putBoolean(SETTINGS_ADVANCED_OPTIONS, advancedOptions)
                .putBoolean(SETTINGS_DISABLE_TIME_STRETCHING, disableTimeStretching)
                .putBoolean(THEME_USES_MENU, themeUsesMenu)
                .putBoolean(SETTINGS_GLASS_ENTITIES, glassEntities)
                .putBoolean(SETTINGS_DISABLE_MINIMAP, disableTimeStretching)
                .putBoolean(SETTINGS_MINIMAP_PREVIEW, minimapPreview)
                .putBoolean(SETTINGS_SUBTITLE_ORDER, subtitlesBelow)
                .putBoolean(SETTINGS_SMOOTH_DRAGGING, smoothDragging)
                .putBoolean(SETTINGS_LIVE_WAVEFORM, liveWaveform)
                .putBoolean(SETTINGS_CHORUS_KIDS, chorusKids)
                .putBoolean(SETTINGS_GAME_BOUNDARIES, gameBoundaries)
                .putString(MIDI_NOTE, midiNote)
                .putBoolean(SETTINGS_NEW_TRACKS_ON_TOP, newTracksOnTop)
                .putBoolean(SETTINGS_ORDER_BY_GAME_ORDER, orderByGameOrder)
                .putBoolean(SETTINGS_ENABLE_ANALYTICS, enableAnalytics)

                .putBoolean(ADVOPT_EXPLODING_ENTITIES, advExplodingEntities)
                .putBoolean(ADVOPT_IGNORE_PITCH_RESTRICTIONS, advIgnorePitchRestrictions)
                .flush()
    }
}
