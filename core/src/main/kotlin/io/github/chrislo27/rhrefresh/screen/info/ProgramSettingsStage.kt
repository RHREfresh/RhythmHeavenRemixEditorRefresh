package io.github.chrislo27.rhrefresh.screen.info

import com.badlogic.gdx.Preferences
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.rhrefresh.PreferenceKeys
import io.github.chrislo27.rhrefresh.PreferenceKeys.LANGUAGE
import io.github.chrislo27.rhrefresh.RHREfreshApplication
import io.github.chrislo27.rhrefresh.VersionHistory
import io.github.chrislo27.rhrefresh.editor.Editor
import io.github.chrislo27.rhrefresh.sfxdb.GameMetadata
import io.github.chrislo27.rhrefresh.soundsystem.*
import io.github.chrislo27.rhrefresh.stage.TrueCheckbox
import io.github.chrislo27.rhrefresh.util.JsonHandler
import io.github.chrislo27.rhrefresh.util.Semitones
import io.github.chrislo27.rhrefresh.util.TinyFDWrapper
import io.github.chrislo27.rhrefresh.util.attemptRememberDirectory
import io.github.chrislo27.rhrefresh.util.getDefaultDirectory
import io.github.chrislo27.toolboks.i18n.Localization
import io.github.chrislo27.toolboks.registry.AssetRegistry
import io.github.chrislo27.toolboks.ui.*
import io.github.chrislo27.toolboks.version.Version
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.sign


class ProgramSettingsStage(parent: UIElement<InfoScreen>?, camera: OrthographicCamera, val infoScreen: InfoScreen)
    : Stage<InfoScreen>(parent, camera) {

    private val main: RHREfreshApplication get() = infoScreen.main
    private val preferences: Preferences get() = infoScreen.preferences
    private val editor: Editor get() = infoScreen.editor
    var didChangeSettings: Boolean = Version.fromStringOrNull(preferences.getString(PreferenceKeys.LAST_VERSION, ""))?.let {
        !it.isUnknown && (it < VersionHistory.ANALYTICS || it < VersionHistory.RE_ADD_STRETCHABLE_TEMPO)
    } ?: false

    private val pitchStyleButton: Button<InfoScreen>
    private val clearRecentsButton: Button<InfoScreen>
    private val ffmpegExecutableButton: Button<InfoScreen>

    private var isChooserOpen = false
        set(value) {
            field = value
            this.infoScreen.stage.backButton.enabled = !isChooserOpen
        }

    init {
        val palette = infoScreen.stage.palette
        val padding = 0.025f
        val buttonHeight = 0.1f
        val fontScale = 0.75f
        val settings = this
        val buttonWidth = 0.45f
        // Settings
        // Language
        settings.elements += object : Button<InfoScreen>(palette, settings, settings) {

            private fun updateText() {
                textLabel.text = "${Localization["editor.language"]} ${Localization.currentBundle.locale.name}"
            }

            private fun persist() {
                val current = Localization.currentBundle
                val str = current.locale.locale.toPrefsString()
                main.preferences.putString(LANGUAGE, str).flush()
            }

            private fun Locale.toPrefsString(): String {
                val obj = LangObj(language, country, variant)
                return JsonHandler.toJson(obj, LangObj::class.java)
            }

            private var index: Int = run {
                val default = InfoScreen.DEFAULT_AUTOSAVE_TIME
                val pref = preferences.getInteger(PreferenceKeys.SETTINGS_AUTOSAVE, default)
                InfoScreen.autosaveTimers.indexOf(InfoScreen.autosaveTimers.find { it == pref } ?: default).coerceIn(0, InfoScreen.autosaveTimers.size - 1)
            }

            private val textLabel: TextLabel<InfoScreen>
                get() = labels.first() as TextLabel<InfoScreen>

            private val imageLabel: ImageLabel<InfoScreen>
                get() = labels[1] as ImageLabel<InfoScreen>

            override fun render(screen: InfoScreen, batch: SpriteBatch, shapeRenderer: ShapeRenderer) {
                if (textLabel.text.isEmpty()) {
                    updateText()
                }
                super.render(screen, batch, shapeRenderer)
            }

            override fun onLeftClick(xPercent: Float, yPercent: Float) {
                super.onLeftClick(xPercent, yPercent)
                Localization.cycle(1)
                persist()
                updateLabels()
            }

            override fun onRightClick(xPercent: Float, yPercent: Float) {
                super.onRightClick(xPercent, yPercent)
                Localization.cycle(-1)
                persist()
                updateLabels()
            }

            override fun onResize(width: Float, height: Float, pixelUnitX: Float, pixelUnitY: Float) {
                super.onResize(width, height, pixelUnitX, pixelUnitY)
                imageLabel.location.set(screenX = 0f, screenY = 0f, screenWidth = 0.1f, screenHeight = 1f)
                imageLabel.onResize(this.location.realWidth, this.location.realHeight, pixelUnitX, pixelUnitY)
            }

            init {
                Localization.addListener {
                    updateText()
                }
            }
        }.apply {
            this.addLabel(TextLabel(palette, this, this.stage).apply {
                this.isLocalizationKey = false
                this.text = ""
                this.textWrapping = false
                this.fontScaleMultiplier = fontScale
            })
            this.addLabel(ImageLabel(palette, this, this.stage).apply {
                renderType = ImageLabel.ImageRendering.ASPECT_RATIO
                image = TextureRegion(AssetRegistry.get<Texture>("ui_icon_language"))
            })

            this.location.set(screenX = 1f - (padding + buttonWidth),
                              screenY = padding * 7 + buttonHeight * 6,
                              screenWidth = buttonWidth,
                              screenHeight = buttonHeight)
            this.tooltipText = "editor.translationsMayNotBeAccurate"
            this.tooltipTextIsLocalizationKey = true
        }

        // Autosave timer
        settings.elements += object : Button<InfoScreen>(palette, settings, settings) {
            private fun updateText() {
                textLabel.text = Localization["screen.info.autosaveTimer",
                        if (InfoScreen.autosaveTimers[index] == 0) Localization["screen.info.autosaveTimerOff"]
                        else Localization["screen.info.autosaveTimerMin", InfoScreen.autosaveTimers[index]]]
                editor.resetAutosaveTimer()
            }

            private fun persist() {
                preferences.putInteger(PreferenceKeys.SETTINGS_AUTOSAVE, InfoScreen.autosaveTimers[index]).flush()
                didChangeSettings = true
            }

            private var index: Int = run {
                val default = InfoScreen.DEFAULT_AUTOSAVE_TIME
                val pref = preferences.getInteger(PreferenceKeys.SETTINGS_AUTOSAVE, default)
                InfoScreen.autosaveTimers.indexOf(InfoScreen.autosaveTimers.find { it == pref } ?: default).coerceIn(0, InfoScreen.autosaveTimers.size - 1)
            }

            private val textLabel: TextLabel<InfoScreen>
                get() = labels.first() as TextLabel<InfoScreen>

            override fun render(screen: InfoScreen, batch: SpriteBatch, shapeRenderer: ShapeRenderer) {
                if (textLabel.text.isEmpty()) {
                    updateText()
                }
                super.render(screen, batch, shapeRenderer)
            }

            override fun onLeftClick(xPercent: Float, yPercent: Float) {
                super.onLeftClick(xPercent, yPercent)
                index++
                if (index >= InfoScreen.autosaveTimers.size)
                    index = 0

                persist()
                updateText()
            }

            override fun onRightClick(xPercent: Float, yPercent: Float) {
                super.onRightClick(xPercent, yPercent)
                index--
                if (index < 0)
                    index = InfoScreen.autosaveTimers.size - 1

                persist()
                updateText()
            }

            init {
                Localization.addListener {
                    updateText()
                }
            }
        }.apply {
            this.addLabel(TextLabel(palette, this, this.stage).apply {
                this.isLocalizationKey = false
                this.text = ""
                this.textWrapping = false
                this.fontScaleMultiplier = fontScale
            })
            this.tooltipText = "screen.info.autosaveTimer.tooltip"
            this.tooltipTextIsLocalizationKey = true

            this.location.set(screenX = 1f - (padding + buttonWidth),
                              screenY = padding * 6 + buttonHeight * 5,
                              screenWidth = buttonWidth,
                              screenHeight = buttonHeight)
        }

        // Semitone major/minor
        pitchStyleButton = object : Button<InfoScreen>(palette, settings, settings) {
            private val textLabel: TextLabel<InfoScreen>
                get() = labels.first() as TextLabel

            private fun cycle(dir: Int) {
                val values = Semitones.PitchStyle.VALUES
                val index = values.indexOf(Semitones.pitchStyle).coerceAtLeast(0)
                val absNextIndex = index + sign(dir.toFloat()).toInt()
                val nextIndex = if (absNextIndex < 0) values.size - 1 else if (absNextIndex >= values.size) 0 else absNextIndex
                val next = values[nextIndex]
                Semitones.pitchStyle = next
                main.preferences.putString(PreferenceKeys.ADVOPT_PITCH_STYLE, next.name).flush()
                updateLabels()
            }

            override fun onLeftClick(xPercent: Float, yPercent: Float) {
                super.onLeftClick(xPercent, yPercent)
                cycle(1)
            }

            override fun onRightClick(xPercent: Float, yPercent: Float) {
                super.onRightClick(xPercent, yPercent)
                cycle(-1)
            }
        }.apply {
            this.addLabel(TextLabel(palette, this, this.stage).apply {
                this.isLocalizationKey = false
                this.text = Localization["screen.info.pitchNoteStyle", ""]
                this.textWrapping = false
                this.fontScaleMultiplier = 0.8f
            })

            this.location.set(screenX = padding,
                screenY = padding * 5 + buttonHeight * 4,
                screenWidth = buttonWidth,
                screenHeight = buttonHeight)
        }
        settings.elements += pitchStyleButton

        // Disable time stretching
        settings.elements += TrueCheckbox(palette, settings, settings).apply {
            this.checked = main.settings.disableTimeStretching

            this.textLabel.apply {
                this.fontScaleMultiplier = fontScale * 0.9f
                this.isLocalizationKey = true
                this.textWrapping = false
                this.textAlign = Align.left
                this.text = "screen.info.disableTimeStretching"
            }

            this.tooltipTextIsLocalizationKey = true
            this.tooltipText = if (FFmpeg.isSupported) "screen.info.disableTimeStretching.tooltip" else "screen.info.disableTimeStretching.notSupported.tooltip"

            this.checkedStateChanged = {
                if (!main.settings.disableTimeStretching && it) {
                    SoundCache.unloadAllDerivatives()
                }
                main.settings.disableTimeStretching = it
                main.settings.persist()
                didChangeSettings = true
            }

            this.location.set(screenX = padding,
                              screenY = padding * 7 + buttonHeight * 6,
                              screenWidth = buttonWidth-padding-(buttonWidth * 0.085f),
                              screenHeight = buttonHeight)
            this.enabled = FFmpeg.isSupported
        }
        // Select executable FFMPEG
        ffmpegExecutableButton = Button(palette, settings, settings).apply {

            this.location.set(screenX = buttonWidth-(buttonWidth * 0.085f)+padding,
                screenY = padding * 7 + buttonHeight * 6,
                screenWidth = buttonWidth * 0.085f,
                screenHeight = buttonHeight)

            val clipboardLabel = ImageLabel(palette, this, this.stage).apply {
                renderType = ImageLabel.ImageRendering.ASPECT_RATIO
                if (preferences.getString(PreferenceKeys.SETTINGS_FFMPEG_LOCATION, "").isNotEmpty()) {
                    image = TextureRegion(AssetRegistry.get<Texture>("ui_icon_clipboard_pen"))
                } else {
                    image = TextureRegion(AssetRegistry.get<Texture>("ui_icon_clipboard"))
                }
            }
            this.addLabel(clipboardLabel)
            this.leftClickAction = { _, _ ->
                openFfmpegPicker()
            }
            this.rightClickAction = { _, _ ->
                preferences.putString(PreferenceKeys.SETTINGS_FFMPEG_LOCATION, "").flush()
                clipboardLabel.image = TextureRegion(AssetRegistry.get<Texture>("ui_icon_clipboard"))
                this.tooltipText = Localization["screen.info.timeStretching.selectFfmpeg","[CYAN]${Localization["screen.info.timeStretching.bundledFfmpeg"]}"]
            }
            this.tooltipTextIsLocalizationKey = false
            val ffmpegLocation = preferences.getString(PreferenceKeys.SETTINGS_FFMPEG_LOCATION, "")
            this.tooltipText = if (ffmpegLocation.isNotEmpty()) {
                Localization["screen.info.timeStretching.selectFfmpeg", "[PINK]$ffmpegLocation"]
            } else {
                Localization["screen.info.timeStretching.selectFfmpeg","[CYAN]${Localization["screen.info.timeStretching.bundledFfmpeg"]}"]
            }
        }
        settings.elements += ffmpegExecutableButton

        // Exploding entities
        settings.elements += TrueCheckbox(palette, settings, settings).apply {
            this.leftClickAction = { _, _ ->
                main.settings.advExplodingEntities = this@apply.checked
                main.settings.persist()
            }
            this.textLabel.also {
                it.isLocalizationKey = true
                it.text = "screen.info.entitiesExplode"
                it.textWrapping = false
                it.fontScaleMultiplier = 0.8f
                it.textAlign = Align.left
            }
            this.checked = main.settings.advExplodingEntities
            this.location.set(screenX = padding,
                screenY = padding * 6 + buttonHeight * 5,
                screenWidth = buttonWidth,
                screenHeight = buttonHeight)
        }
        settings.elements += TrueCheckbox(palette, settings, settings).apply {
            this.leftClickAction = { _, _ ->
                main.settings.advIgnorePitchRestrictions = this@apply.checked
                main.settings.persist()
            }
            this.textLabel.also {
                it.isLocalizationKey = true
                it.text = "screen.info.ignorePitchRestrictions"
                it.textWrapping = false
                it.fontScaleMultiplier = 0.8f
                it.textAlign = Align.left
            }
            this.checked = main.settings.advIgnorePitchRestrictions
            this.location.set(screenX = padding,
                screenY = padding * 4 + buttonHeight * 3,
                screenWidth = buttonWidth,
                screenHeight = buttonHeight)
        }

        // Discord rich presence
        settings.elements += object : TrueCheckbox<InfoScreen>(palette, settings, settings) {
            val discordIcon = ImageLabel(palette, this, this.stage).apply {
                this.renderType = ImageLabel.ImageRendering.ASPECT_RATIO
                this.image = TextureRegion(AssetRegistry.get<Texture>("ui_icon_discord"))
            }

            override fun onLeftClick(xPercent: Float, yPercent: Float) {
                super.onLeftClick(xPercent, yPercent)
                preferences.putBoolean(PreferenceKeys.SETTINGS_DISCORD_RPC_ENABLED, checked).flush()
                didChangeSettings = true
            }

            override fun computeTextX(): Float {
                return computeCheckWidth() * 2.1f
            }

            override fun onResize(width: Float, height: Float, pixelUnitX: Float, pixelUnitY: Float) {
                super.onResize(width, height, pixelUnitX, pixelUnitY)
                val checkWidth = computeCheckWidth()
                discordIcon.location.set(screenX = checkWidth, screenY = 0f, screenWidth = checkWidth, screenHeight = 1f)
                discordIcon.onResize(this.location.realWidth, this.location.realHeight, pixelUnitX, pixelUnitY)
            }
        }.apply {
//            this.checked = preferences.getBoolean(PreferenceKeys.SETTINGS_DISCORD_RPC_ENABLED, true)
            this.checked = false
            this.enabled = false
            this.tooltipTextIsLocalizationKey = true
            this.tooltipText = "screen.info.discordRichPresence.unavailable"
            this.textLabel.apply {
                this.fontScaleMultiplier = fontScale
                this.isLocalizationKey = true
                this.textWrapping = false
                this.textAlign = Align.left
                this.text = "screen.info.discordRichPresence"
            }

            this.location.set(screenX = 1f - (padding + buttonWidth),
                              screenY = padding * 5 + buttonHeight * 4,
                              screenWidth = buttonWidth,
                              screenHeight = buttonHeight)

            addLabel(discordIcon)
        }

        // Close warning
        settings.elements += TrueCheckbox(palette, settings, settings).apply {
            this.checked = preferences.getBoolean(PreferenceKeys.SETTINGS_CLOSE_WARNING, true)

            this.textLabel.apply {
                this.fontScaleMultiplier = fontScale * 0.9f
                this.isLocalizationKey = true
                this.textWrapping = false
                this.textAlign = Align.left
                this.text = "screen.info.closeWarning"
            }

            this.checkedStateChanged = {
                preferences.putBoolean(PreferenceKeys.SETTINGS_CLOSE_WARNING, it).flush()
                didChangeSettings = true
            }

            this.location.set(screenX = 1f - (padding + buttonWidth),
                screenY = padding * 4 + buttonHeight * 3,
                screenWidth = buttonWidth,
                screenHeight = buttonHeight)
        }

        // Reenable welcome screen
        settings.elements += object: TrueCheckbox<InfoScreen>(palette, settings, settings) {
            override fun render(screen: InfoScreen, batch: SpriteBatch, shapeRenderer: ShapeRenderer) {
                super.render(screen, batch, shapeRenderer)
                this.checked = preferences.getBoolean(PreferenceKeys.SHOW_STARTUP_SCREEN, false)
            }
        }.apply {
            this.checked = preferences.getBoolean(PreferenceKeys.SHOW_STARTUP_SCREEN, false)

            this.textLabel.apply {
                this.fontScaleMultiplier = fontScale * 0.9f
                this.isLocalizationKey = true
                this.textWrapping = false
                this.textAlign = Align.left
                this.text = "screen.info.enableWelcomeScreen"
            }

            this.checkedStateChanged = {
                preferences.putBoolean(PreferenceKeys.SHOW_STARTUP_SCREEN, it).flush()
                didChangeSettings = true
            }

            this.location.set(screenX = 1f - (padding + buttonWidth),
                screenY = padding * 3 + buttonHeight * 2,
                screenWidth = buttonWidth,
                screenHeight = buttonHeight)
        }

        // Enable analytics
//        settings.elements += TrueCheckbox(palette, settings, settings).apply {
//            this.checked = preferences.getBoolean(PreferenceKeys.SETTINGS_ENABLE_ANALYTICS, false)
//
//            this.textLabel.apply {
//                this.fontScaleMultiplier = fontScale * 0.9f
//                this.isLocalizationKey = true
//                this.textWrapping = false
//                this.textAlign = Align.left
//                this.text = "screen.info.enableAnalytics"
//            }
//
//            this.checkedStateChanged = {
//                preferences.putBoolean(PreferenceKeys.SETTINGS_ENABLE_ANALYTICS, it).flush()
//                didChangeSettings = true
//            }
//
//            this.tooltipText = "screen.info.enableAnalytics.tooltip"
//            this.tooltipTextIsLocalizationKey = true
//
//            this.location.set(screenX = 1f - (padding + buttonWidth),
//                screenY = padding * 3 + buttonHeight * 2,
//                screenWidth = buttonWidth,
//                screenHeight = buttonHeight)
//        }

        // New track behaviour
        settings.elements += TrueCheckbox(palette, settings, settings).apply {
            this.checked = preferences.getBoolean(PreferenceKeys.SETTINGS_NEW_TRACKS_ON_TOP, true)

            this.textLabel.apply {
                this.fontScaleMultiplier = fontScale * 0.9f
                this.isLocalizationKey = true
                this.textWrapping = false
                this.textAlign = Align.left
                this.text = "screen.info.newTrackBehaviour"
            }

            this.checkedStateChanged = {
                preferences.putBoolean(PreferenceKeys.SETTINGS_NEW_TRACKS_ON_TOP, it).flush()
                didChangeSettings = true
            }

            this.location.set(screenX = padding,
                screenY = padding * 3 + buttonHeight * 2,
                screenWidth = buttonWidth,
                screenHeight = buttonHeight)
        }

        // Ordering behaviour
        settings.elements += TrueCheckbox(palette, settings, settings).apply {
            this.checked = preferences.getBoolean(PreferenceKeys.SETTINGS_ORDER_BY_GAME_ORDER, true)

            this.textLabel.apply {
                this.fontScaleMultiplier = fontScale * 0.9f
                this.isLocalizationKey = true
                this.textWrapping = false
                this.textAlign = Align.left
                this.text = "screen.info.orderByGame"
            }

            this.checkedStateChanged = {
                preferences.putBoolean(PreferenceKeys.SETTINGS_ORDER_BY_GAME_ORDER, it)
                didChangeSettings = true
            }

            this.location.set(screenX = padding,
                screenY = padding * 2 + buttonHeight * 1,
                screenWidth = buttonWidth,
                screenHeight = buttonHeight)
        }

        // Clear recent games
        clearRecentsButton = Button(palette, settings, settings).apply {
            addLabel(TextLabel(palette, this, this.stage).apply {
                this.fontScaleMultiplier = fontScale
                this.isLocalizationKey = true
                this.textWrapping = false
                this.text = "screen.info.clearRecents"
            })
            this.enabled = GameMetadata.recents.isNotEmpty()
            this.leftClickAction = { _, _ ->
                editor.updateRecentsList(null)
                enabled = false
                GameMetadata.persist()
            }
            this.location.set(screenX = 1f - (padding + buttonWidth),
                screenY = padding,
                screenWidth = buttonWidth,
                screenHeight = buttonHeight)
        }
        settings.elements += clearRecentsButton

        updateLabels()
    }

    private fun updateLabels() {
        val ffmpegLocation = preferences.getString(PreferenceKeys.SETTINGS_FFMPEG_LOCATION, "")
        ffmpegExecutableButton.tooltipText = if (ffmpegLocation.isNotEmpty()) {
            Localization["screen.info.timeStretching.selectFfmpeg", "[PINK]$ffmpegLocation"]
        } else {
            Localization["screen.info.timeStretching.selectFfmpeg","[CYAN]${Localization["screen.info.timeStretching.bundledFfmpeg"]}"]
        }
        (pitchStyleButton.labels.first() as TextLabel).text =
            Localization["screen.info.pitchNoteStyle", Localization[Semitones.pitchStyle.displayName], Semitones.pitchStyle.example]
    }

    fun show() {
        clearRecentsButton.enabled = GameMetadata.recents.isNotEmpty()
    }

    class LangObj() {
        var language: String? = null
        var country: String? = null
        var variant: String? = null

        constructor(language: String = "", country: String = "", variant: String = "") : this() {
            this.language = language
            this.country = country
            this.variant = variant
        }
    }

    @Synchronized
    private fun openFfmpegPicker() {
        if (!isChooserOpen) {
            GlobalScope.launch {
                isChooserOpen = true
                val initialDirectory: File? = attemptRememberDirectory(main, PreferenceKeys.FILE_CHOOSER_LOAD) ?: getDefaultDirectory()
                val fileFilter = TinyFDWrapper.FileExtFilter(Localization["screen.info.timeStretching.selectFfmpegFilter"])
                TinyFDWrapper.openFile(Localization["screen.open.fileChooserTitle"], initialDirectory, fileFilter) { file ->
                    isChooserOpen = false
                    if (file != null) {
                        (ffmpegExecutableButton.labels.first() as ImageLabel).image = TextureRegion(AssetRegistry.get<Texture>("ui_icon_clipboard_pen"))
                        ffmpegExecutableButton.tooltipText = Localization["screen.info.timeStretching.selectFfmpeg","[PINK]${file.absolutePath}"]
                        preferences.putString(PreferenceKeys.SETTINGS_FFMPEG_LOCATION, file.absolutePath).flush()
                    }
                }
            }
        }
    }
}