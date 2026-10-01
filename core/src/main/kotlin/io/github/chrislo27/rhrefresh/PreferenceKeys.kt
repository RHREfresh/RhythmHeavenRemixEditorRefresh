package io.github.chrislo27.rhrefresh


object PreferenceKeys {

    val DATABASE_VERSION = "databaseVersion_"
    val DATABASE_VERSION_COMMIT = "_commit"
    val THEME_INDEX = "themeIndex"
    val THEME_USES_MENU = "themeUsesMenu"
    val LANG_INDEX = "languageIndex"
    val LANGUAGE = "language"
    val WINDOW_STATE = "windowState"
    val MIDI_NOTE = "midiNote"
    val SHOW_STARTUP_SCREEN = "showStartupScreen"

    // settings
    val SETTINGS_DISABLE_MINIMAP = "settings_minimap"
    val SETTINGS_MINIMAP_PREVIEW = "settings_minimapPreview"
    val SETTINGS_AUTOSAVE = "settings_autosave"
    val SETTINGS_CAMERA_BEHAVIOUR = "settings_cameraBehaviour"
    val SETTINGS_SUBTITLE_ORDER = "settings_subtitleOrder"
    val SETTINGS_SMOOTH_DRAGGING = "settings_smoothDragging"
    val SETTINGS_DISCORD_RPC_ENABLED = "settings_discordRPCEnabled"
    val SETTINGS_GLASS_ENTITIES = "settings_glassEntities"
    val SETTINGS_ADVANCED_OPTIONS = "settings_advancedOptions"
    val SETTINGS_CLOSE_WARNING = "settings_closeWarning"
    val SETTINGS_DISABLE_TIME_STRETCHING = "settings_disableTimeStretching"
    val SETTINGS_AUDIO_MIXER = "settings_audioMixer"
    val SETTINGS_AUDIO_VOLUME = "settings_audioVolume"
    val SETTINGS_NEW_TRACKS_ON_TOP = "settings_newTrackPosition"
    val SETTINGS_ORDER_BY_GAME_ORDER = "settings_chronologicalGameOrder"
    val SETTINGS_ENABLE_ANALYTICS = "settings_enableAnalytics"
    val SETTINGS_FFMPEG_LOCATION = "settings_ffmpegLocation"

    val SETTINGS_LIVE_WAVEFORM = "settings_liveWaveform"
    val SETTINGS_CHORUS_KIDS = "settings_chorusKids"
    val SETTINGS_GAME_BOUNDARIES = "settings_gameBoundaries"

    val allSettingsKeys: List<String> =
        listOf(SETTINGS_DISABLE_MINIMAP, SETTINGS_MINIMAP_PREVIEW, SETTINGS_AUTOSAVE, SETTINGS_CAMERA_BEHAVIOUR,
               SETTINGS_SUBTITLE_ORDER, SETTINGS_SMOOTH_DRAGGING,
               SETTINGS_DISCORD_RPC_ENABLED, SETTINGS_ADVANCED_OPTIONS, SETTINGS_GLASS_ENTITIES, SETTINGS_CLOSE_WARNING,
               SETTINGS_DISABLE_TIME_STRETCHING, SETTINGS_AUDIO_MIXER, SETTINGS_AUDIO_VOLUME, SETTINGS_NEW_TRACKS_ON_TOP,
               SETTINGS_ORDER_BY_GAME_ORDER, SETTINGS_ENABLE_ANALYTICS, SETTINGS_FFMPEG_LOCATION,
               SETTINGS_LIVE_WAVEFORM, SETTINGS_CHORUS_KIDS, SETTINGS_GAME_BOUNDARIES)

    val ADVOPT_REF_RH_GAME = "advOpt_referenceRHGame"
    val ADVOPT_PITCH_STYLE = "advOpt_pitchStyle"
    val ADVOPT_EXPLODING_ENTITIES = "advOpt_explodingEntities"
    val ADVOPT_IGNORE_PITCH_RESTRICTIONS = "advOpt_ignorePitchRestrictions"
    val ADVOPT_SFXDB_USE_DEV_BRANCH = "advOpt_sfxDbUseDevBranch"

    val allAdvOptsKeys: List<String> =
            listOf(ADVOPT_REF_RH_GAME, ADVOPT_PITCH_STYLE, ADVOPT_EXPLODING_ENTITIES, ADVOPT_IGNORE_PITCH_RESTRICTIONS, ADVOPT_SFXDB_USE_DEV_BRANCH)

    val FILE_CHOOSER_MUSIC = "fileChooser_musicSelect"
    val FILE_CHOOSER_SAVE = "fileChooser_save"
    val FILE_CHOOSER_LOAD = "fileChooser_load"
    val FILE_CHOOSER_EXPORT = "fileChooser_export"
    val FILE_CHOOSER_TEXENT = "fileChooser_texEnt"
    val FILE_CHOOSER_EXPORT_IMAGE = "fileChooser_exportImage"
    val FILE_CHOOSER_THEME_EDITOR_IMAGE = "fileChooser_themeEditorImage"

    val FAVOURITES = "favourites"
    val RECENT_GAMES = "recentGames"
    val LAST_VERSION = "lastVersion"
    val TIMES_SKIPPED_UPDATE = "timesSkippedUpdate"
    val BACKGROUND = "background"
    val LAST_NEWS = "lastNewsArticles"
    val READ_NEWS = "readNewsArticles"
    val PADDLER_LOADING_ICON = "paddlerLoadingIcon"
    val PLAYALONG_CONTROLS = "playalongControls"
    val PLAYALONG_CONTROLLER_MAPPINGS = "playalongControllerMappings"
    val PLAYALONG_CALIBRATION_KEY = "playalongCalibrationKey"
    val PLAYALONG_CALIBRATION_MOUSE = "playalongCalibrationMouse"
    val PLAYALONG_SFX_PERFECT_FAIL = "playalongSfxPerfectFail"
    val PLAYALONG_SFX_MONSTER_FAIL = "playalongSfxMonsterFail"
    val PLAYALONG_SFX_MONSTER_ACE = "playalongSfxMonsterAce"
    val EVENT_PREFIX = "event_"

    val PASSED_FOLDER_CHANGE_WARNING = "passedFolderChangeWarning"
    val IMPORTED_RHRE3 = "importedRHRE3"
}