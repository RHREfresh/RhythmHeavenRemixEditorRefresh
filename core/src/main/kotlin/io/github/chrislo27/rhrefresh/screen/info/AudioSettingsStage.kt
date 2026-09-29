package io.github.chrislo27.rhrefresh.screen.info

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.rhrefresh.PreferenceKeys
import io.github.chrislo27.rhrefresh.RHREfreshApplication
import io.github.chrislo27.rhrefresh.VersionHistory
import io.github.chrislo27.rhrefresh.editor.DefaultMidiNotes
import io.github.chrislo27.rhrefresh.editor.Editor
import io.github.chrislo27.rhrefresh.editor.view.ViewType
import io.github.chrislo27.rhrefresh.soundsystem.*
import io.github.chrislo27.rhrefresh.stage.TrueCheckbox
import io.github.chrislo27.toolboks.Toolboks
import io.github.chrislo27.toolboks.i18n.Localization
import io.github.chrislo27.toolboks.ui.*
import io.github.chrislo27.toolboks.version.Version
import javax.sound.sampled.Mixer
import kotlin.math.exp


class AudioSettingsStage(parent: UIElement<InfoScreen>?, camera: OrthographicCamera, val infoScreen: InfoScreen)
    : Stage<InfoScreen>(parent, camera) {

    private val main: RHREfreshApplication get() = infoScreen.main
    private val preferences: Preferences get() = infoScreen.preferences
    private val editor: Editor get() = infoScreen.editor
    var didChangeSettings: Boolean = Version.fromStringOrNull(preferences.getString(PreferenceKeys.LAST_VERSION, ""))?.let {
        !it.isUnknown && (it < VersionHistory.ANALYTICS || it < VersionHistory.RE_ADD_STRETCHABLE_TEMPO)
    } ?: false

    val volume: ImageLabel<InfoScreen>
    val volumeArrow: MovingArrow<InfoScreen>

    private var testSoundAudio: BeadsAudio? = null
    private var currentTestSound: BeadsSound? = null

    init {
        val palette = infoScreen.stage.palette
        val padding = 0.025f
        val buttonHeight = 0.1f
        val fontScale = 0.75f
        val settings = this
        val buttonWidth = 0.45f
        // Settings

        // Live Waveform
        settings.elements += object : TrueCheckbox<InfoScreen>(palette, settings, settings) {
            override fun onLeftClick(xPercent: Float, yPercent: Float) {
                super.onLeftClick(xPercent, yPercent)
                if (checked) {
                    editor.views.add(ViewType.WAVEFORM)
                } else {
                    editor.views.remove(ViewType.WAVEFORM)
                }
                main.settings.liveWaveform = checked
                main.settings.persist()
                didChangeSettings = true
            }
        }.apply {
            this.checked = main.settings.liveWaveform

            this.textLabel.apply {
                this.fontScaleMultiplier = fontScale * 0.9f
                this.isLocalizationKey = true
                this.textWrapping = false
                this.textAlign = Align.left
                this.text = "editor.view.waveform"
            }

            this.location.set(screenX = padding,
                screenY = padding * 7 + buttonHeight * 6,
                screenWidth = buttonWidth,
                screenHeight = buttonHeight)
        }

        // Glee Club
        settings.elements += TrueCheckbox(palette, settings, settings).apply {
            this.checked = main.settings.chorusKids
            this.textLabel.apply {
                this.fontScaleMultiplier = fontScale
                this.isLocalizationKey = true
                this.textWrapping = false
                this.textAlign = Align.left
                this.text = "editor.view.gleeClub"
            }
            this.leftClickAction = { _, _ ->
                if (checked) {
                    editor.views.add(ViewType.GLEE_CLUB)
                } else {
                    editor.views.remove(ViewType.GLEE_CLUB)
                }
                main.settings.chorusKids = checked
                main.settings.persist()
                didChangeSettings = true
            }
            this.location.set(screenX = padding,
                screenY = padding * 6 + buttonHeight * 5,
                screenWidth = buttonWidth,
                screenHeight = buttonHeight)
        }

        // Instrument select label
        settings.elements += TextLabel(palette, settings, settings).apply {
            this.isLocalizationKey = true
            this.text = "screen.info.midiInstrumentSelect"
            this.textWrapping = false
            this.fontScaleMultiplier = 0.9f
            this.location.set(screenX = padding,
                screenY = padding * 5 + buttonHeight * 4,
                screenWidth = buttonWidth * 0.834f,
                screenHeight = buttonHeight)
        }

        //Advanced Instrument Select tooltip
        settings.elements += object: TextLabel<InfoScreen>(palette, settings, settings) {
            override fun frameUpdate(screen: InfoScreen) {
//                super.frameUpdate(screen)
                this.visible = main.settings.advancedOptions
            }}.apply {
            this.isLocalizationKey = false
            this.text = "\uE152"
            this.tooltipText = "screen.info.midiNote.tooltip"
            this.tooltipTextIsLocalizationKey = true
            this.textWrapping = false
            this.location.set(screenX = (padding+buttonWidth) - (padding + buttonWidth * 0.083f),
                screenY = padding * 5 + buttonHeight * 4,
                screenWidth = buttonWidth * 0.083f,
                screenHeight = buttonHeight)
        }

        // Instrument Select
        settings.elements += object: TextField<InfoScreen>(palette, settings, settings) {
            override fun frameUpdate(screen: InfoScreen) {
                super.frameUpdate(screen)
                this.visible = main.settings.advancedOptions
                if (this.visible && !hasFocus) {
                    this.text = main.settings.midiNote
                }
            }
            override fun onEnterPressed(): Boolean {
                hasFocus = false
                infoScreen.lockKeys = false
                main.settings.midiNote = text
                main.settings.persist()
                didChangeSettings = true
                return true
            }

            override fun onLeftClick(xPercent: Float, yPercent: Float) {
                super.onLeftClick(xPercent, yPercent)
                infoScreen.lockKeys = true
            }
        }.apply {
            this.text = main.settings.midiNote
            this.background = true
            this.canInputNewlines = false
            this.visible = main.settings.advancedOptions
            this.canPaste = true
//            val acceptedChars = setOf('1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '.')
//            this.canTypeText = true
//            this.textAlign = Align.left
            this.location.set(screenX = padding,
                screenY = padding * 4 + buttonHeight * 3,
                screenWidth = buttonWidth,
                screenHeight = buttonHeight)
        }

        // Instrument Select (simple)
        settings.elements += object : Button<InfoScreen>(palette, settings, settings) {
            private val label: TextLabel<InfoScreen> = object:TextLabel<InfoScreen>(palette, this, this.stage) {
                override fun frameUpdate(screen: InfoScreen) {
                    super.frameUpdate(screen)
                    this.visible = !main.settings.advancedOptions
                }
            }.apply {
                this.isLocalizationKey = false
                this.text = ""
                this.textWrapping = false
                this.fontScaleMultiplier = fontScale
                this.location.set(pixelX = 2f, pixelWidth = -4f)
                addLabel(this)
            }

            override fun frameUpdate(screen: InfoScreen) {
                super.frameUpdate(screen)
                this.visible = !main.settings.advancedOptions
                    updateText()
            }

            private fun updateText() {
                val midiNote = DefaultMidiNotes.findByCue(main.settings.midiNote)
                if (midiNote != DefaultMidiNotes.NON_DEFAULT) {
                    label.text = Localization["screen.info.midiNote", Localization[DefaultMidiNotes.findByCue(main.settings.midiNote).localizationKey]]
                } else {
                    label.text = Localization["screen.info.midiNote", Localization[main.settings.midiNote]]
                }
            }

            private fun cycle(dir: Int) {
                val values = DefaultMidiNotes.VALUES.dropLast(1)// Removing NON_DEFAULT
                val currentNote = DefaultMidiNotes.findByCue(main.settings.midiNote)
                if (currentNote!=DefaultMidiNotes.NON_DEFAULT) {
                    val index = values.indexOf(currentNote) + dir
                    val normalized = if (index < 0) values.size - 1 else if (index >= values.size) 0 else index
                    main.settings.midiNote = values[normalized].cue
                } else {
                    main.settings.midiNote = values[0].cue
                }
                if (dir != 0) {
                    main.settings.persist()
                    didChangeSettings = true
                }
                updateText()
            }

            override fun onLeftClick(xPercent: Float, yPercent: Float) {
                if (visible) {
                    super.onLeftClick(xPercent, yPercent)
                    cycle(1)
                }
            }

            override fun onRightClick(xPercent: Float, yPercent: Float) {
                if (visible) {
                    super.onRightClick(xPercent, yPercent)
                    cycle(-1)
                }
            }

            init {
                Localization.addListener { updateText() }
                updateText()
            }
        }.apply {
            this.location.set(screenX = padding,
                screenY = padding * 4 + buttonHeight * 3,
                screenWidth = buttonWidth,
                screenHeight = buttonHeight)
        }
        
        // Sound mixer settings
        settings.elements += TextLabel(palette, settings, settings).apply {
            this.isLocalizationKey = true
            this.text = "screen.info.mixerSettings"
            this.textWrapping = false
            this.fontScaleMultiplier = 0.9f
            this.location.set(screenX = 1f - (padding + buttonWidth * 0.917f),
                              screenY = padding * 7 + buttonHeight * 6,
                              screenWidth = buttonWidth * 0.834f,
                              screenHeight = buttonHeight)
        }
        settings.elements += TextLabel(palette, settings, settings).apply {
            this.isLocalizationKey = false
            this.text = "\uE152"
            this.tooltipText = "screen.info.mixerSettings.tooltip"
            this.tooltipTextIsLocalizationKey = true
            this.textWrapping = false
            this.location.set(screenX = 1f - (padding + buttonWidth * 0.083f),
                              screenY = padding * 7 + buttonHeight * 6,
                              screenWidth = buttonWidth * 0.083f,
                              screenHeight = buttonHeight)
        }
        val mixerSettingsLabel = TextLabel(palette, this, this.stage).apply {
            this.isLocalizationKey = false
            this.text = "MIXER INFO NAME"
            this.textWrapping = false
            this.tooltipTextIsLocalizationKey= false
            this.tooltipText = "MIXER INFO TOOLTIP"
            this.fontScaleMultiplier = 0.85f
            this.location.set(screenX = 1f - (padding + buttonWidth * (1f - 0.1f)),
                              screenY = padding * 6 + buttonHeight * 5,
                              screenWidth = buttonWidth * (1f - 0.2f),
                              screenHeight = buttonHeight)
            this.background = true
        }
        settings.elements += mixerSettingsLabel
        val prevMixerButton = Button(palette, settings, settings).apply { 
            addLabel(TextLabel(palette, this, this.stage).apply {
                this.isLocalizationKey = false
                this.text = "\uE149"
                this.textWrapping = false
            })
            this.location.set(screenX = 1f - (padding + buttonWidth),
                              screenY = padding * 6 + buttonHeight * 5,
                              screenWidth = buttonWidth * 0.075f,
                              screenHeight = buttonHeight)
            this.tooltipText = "screen.info.mixerSettings.prev"
            this.tooltipTextIsLocalizationKey = true
        }
        settings.elements += prevMixerButton
        val nextMixerButton = Button(palette, settings, settings).apply {
            addLabel(TextLabel(palette, this, this.stage).apply {
                this.isLocalizationKey = false
                this.text = "\uE14A"
                this.textWrapping = false
            })
            this.location.set(screenX = 1f - (padding + buttonWidth * 0.075f),
                              screenY = padding * 6 + buttonHeight * 5,
                              screenWidth = buttonWidth * 0.075f,
                              screenHeight = buttonHeight)
            this.tooltipText = "screen.info.mixerSettings.next"
            this.tooltipTextIsLocalizationKey = true
        }
        settings.elements += nextMixerButton
        val resetMixerButton = Button(palette, settings, settings).apply {
            addLabel(TextLabel(palette, this, this.stage).apply {
                this.isLocalizationKey = true
                this.text = "screen.info.mixerSettings.resetToDefault"
                this.textWrapping = false
                this.fontScaleMultiplier = 0.75f
            })
            this.location.set(screenX = 1f - (padding + buttonWidth),
                              screenY = padding * 5 + buttonHeight * 4,
                              screenWidth = buttonWidth * 0.65f,
                              screenHeight = buttonHeight)
            this.tooltipText = "screen.info.mixerSettings.resetToDefault.tooltip"
            this.tooltipTextIsLocalizationKey = true
        }
        settings.elements += resetMixerButton
        val testMixerButton = Button(palette, settings, settings).apply {
            addLabel(TextLabel(palette, this, this.stage).apply {
                this.isLocalizationKey = true
                this.text = "screen.info.mixerSettings.test"
                this.textWrapping = false
                this.fontScaleMultiplier = 0.75f
            })
            this.location.set(screenX = 1f - (padding + buttonWidth * 0.325f),
                              screenY = padding * 5 + buttonHeight * 4,
                              screenWidth = buttonWidth * 0.325f,
                              screenHeight = buttonHeight)
            this.tooltipText = "screen.info.mixerSettings.test.tooltip"
            this.tooltipTextIsLocalizationKey = true
        }
        settings.elements += testMixerButton


        // Volume settings
        volume = ImageLabel(palette, this, this).apply {
            this.renderType = ImageLabel.ImageRendering.RENDER_FULL
            this.background = true
            this.image = TextureRegion(RHREfreshApplication.instance.volumeBar)
            this.location.set(screenX = 1f - (padding + buttonWidth),
                screenY = padding * 4 + buttonHeight * 3,
                screenWidth = buttonWidth,
                screenHeight = buttonHeight)
        }
        settings.elements += volume
        volumeArrow = MovingArrow(palette, this, this).apply {
            this.location.set(volume.location)
            this.percentage = preferences.getFloat(PreferenceKeys.SETTINGS_AUDIO_VOLUME, 1f)
            this.onPercentageChange = {
                Toolboks.LOGGER.info("Gain set to "+ exp(6.908*it)/1000)
                BeadsSoundSystem.audioContext.out.gain = (exp(6.908*it)/1000).toFloat()
                preferences.putFloat(PreferenceKeys.SETTINGS_AUDIO_VOLUME, it)
                didChangeSettings = true
            }
        }
        settings.elements += volumeArrow
        settings.elements += TextLabel(palette, settings, settings).apply {
            this.isLocalizationKey = false
            this.text = "Volume"
            this.textWrapping = false
            this.textAlign = Align.left
            this.fontScaleMultiplier = 0.9f
            this.location.set(screenX = 1f - (padding + buttonWidth) + 0.002f,
                screenY = padding * 4 + buttonHeight * 3,
                screenWidth = buttonWidth,
                screenHeight = buttonHeight)
        }
        
        fun updateAudioMixerUI() {
            val currentMixer = BeadsSoundSystem.currentMixer
            val mixerInfo = currentMixer.mixerInfo
            mixerSettingsLabel.text = mixerInfo.name
            mixerSettingsLabel.tooltipText = "${mixerInfo.name}\n${mixerInfo.description}"
        }
        fun playTestSoundToMixer() {
            if (testSoundAudio == null) {
                testSoundAudio = BeadsSoundSystem.newAudio(Gdx.files.internal("sound/mixer_test_sfx.ogg"))
            }
            val audio = testSoundAudio
            if (audio != null && currentTestSound == null) {
                currentTestSound = BeadsSound(audio)
            }
            currentTestSound?.play(false, 1f, 1f, 1f, 0.0)
        }
        fun changeToMixer(mixer: Mixer) {
            val oldMixer = BeadsSoundSystem.currentMixer
            if (mixer !== oldMixer) {
                Toolboks.LOGGER.info("Changing mixer to ${mixer.mixerInfo}")
                val c = currentTestSound
                if (c != null) {
                    c.dispose()
                    currentTestSound = null
                }
                
                BeadsSoundSystem.regenerateAudioContexts(mixer) // !!

                preferences.putString(PreferenceKeys.SETTINGS_AUDIO_MIXER, mixer.mixerInfo.name)
                didChangeSettings = true
                
                Gdx.app.postRunnable {
                    updateAudioMixerUI()
                }
            }
        }
        prevMixerButton.leftClickAction = { _, _ ->
            val mixers = BeadsSoundSystem.supportedMixers
            var i = mixers.indexOf(BeadsSoundSystem.currentMixer)
            i--
            if (i < 0) i = mixers.size - 1
            changeToMixer(mixers[i])
        }
        nextMixerButton.leftClickAction = { _, _ ->
            val mixers = BeadsSoundSystem.supportedMixers
            var i = mixers.indexOf(BeadsSoundSystem.currentMixer)
            i++
            if (i >= mixers.size) i = 0
            changeToMixer(mixers[i])
        }
        testMixerButton.leftClickAction = { _, _ ->
            playTestSoundToMixer()
        }
        resetMixerButton.leftClickAction = { _, _ ->
            changeToMixer(BeadsSoundSystem.getDefaultMixer())
        }
        updateAudioMixerUI()
    }
    
    fun show() {
        BeadsSoundSystem.isRealtime = true
        BeadsSoundSystem.stop()
        BeadsSoundSystem.resume()
    }
    
    fun hide() {
        currentTestSound?.dispose()
        currentTestSound = null
        testSoundAudio = null
        BeadsSoundSystem.stop()
        BeadsSoundSystem.resume()
    }

}