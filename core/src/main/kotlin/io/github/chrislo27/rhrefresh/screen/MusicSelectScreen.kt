package io.github.chrislo27.rhrefresh.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.rhrefresh.PreferenceKeys
import io.github.chrislo27.rhrefresh.RHREfreshApplication
import io.github.chrislo27.rhrefresh.editor.Editor
import io.github.chrislo27.rhrefresh.undoredo.ReversibleAction
import io.github.chrislo27.rhrefresh.stage.GenericStage
import io.github.chrislo27.rhrefresh.stage.LoadingIcon
import io.github.chrislo27.rhrefresh.track.MusicData
import io.github.chrislo27.rhrefresh.track.PlayState
import io.github.chrislo27.rhrefresh.track.Remix
import io.github.chrislo27.rhrefresh.track.tracker.TrackerValueChange
import io.github.chrislo27.rhrefresh.track.tracker.tempo.TempoChange
import io.github.chrislo27.rhrefresh.util.*
import io.github.chrislo27.rhrefresh.util.err.MusicLoadingException
import io.github.chrislo27.toolboks.ToolboksScreen
import io.github.chrislo27.toolboks.i18n.Localization
import io.github.chrislo27.toolboks.registry.AssetRegistry
import io.github.chrislo27.toolboks.registry.ScreenRegistry
import io.github.chrislo27.toolboks.ui.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue


class MusicSelectScreen(main: RHREfreshApplication)
    : ToolboksScreen<RHREfreshApplication, MusicSelectScreen>(main) {

    private val editorScreen: EditorScreen by lazy { ScreenRegistry.getNonNullAsType<EditorScreen>("editor") }
    private val editor: Editor
        get() = editorScreen.editor
    override val stage: GenericStage<MusicSelectScreen> = GenericStage(main.uiPalette, null, main.defaultCamera)

    @Volatile
    private var isChooserOpen = false
    @Volatile
    private var isLoading = false
    private val mainLabel: TextLabel<MusicSelectScreen>
    private val moveMusicStartButton: Button<MusicSelectScreen>
    private val tempoField: TextField<MusicSelectScreen>
    private val tempoFieldLabel: TextLabel<MusicSelectScreen>
    private val tempoFieldUnit: TextLabel<MusicSelectScreen>
    private var globalTempoChangeId: Float = 0f
    private val musicStartField: TextField<MusicSelectScreen>
    private val musicStartFieldLabel: TextLabel<MusicSelectScreen>
    private val musicStartFieldUnit: TextLabel<MusicSelectScreen>

    init {
        stage.titleIcon.image = TextureRegion(AssetRegistry.get<Texture>("ui_icon_music_button"))
        stage.titleLabel.text = "screen.music.title"
        stage.backButton.visible = true
        stage.onBackButtonClick = {
            if (!isChooserOpen && !isLoading) {
                main.screen = ScreenRegistry.getNonNull("editor")
            }
        }

        val palette = main.uiPalette

        stage.bottomStage.elements += MusicFileChooserButton(palette, stage.bottomStage, stage.bottomStage).apply {
            this.location.set(screenX = 0.25f, screenWidth = 0.5f)
            this.addLabel(TextLabel(palette, this, this.stage).apply {
                this.textAlign = Align.center
                this.text = "screen.music.select"
                this.isLocalizationKey = true
//                this.fontScaleMultiplier = 0.9f
                this.location.set(pixelX = 4f, pixelWidth = -8f)
            })
        }
        moveMusicStartButton = object : Button<MusicSelectScreen>(palette, stage.bottomStage, stage.bottomStage) {
            override fun onLeftClick(xPercent: Float, yPercent: Float) {
                super.onLeftClick(xPercent, yPercent)
                val remix = editor.remix
                val start = remix.music?.music?.getStartOfSound() ?: -1f
                if (start >= 0f && remix.musicStartSec != -start.absoluteValue) {

                    musicStartField.text = (-(start.absoluteValue)*1000).toInt().toString()
                    remix.mutate(object : ReversibleAction<Remix> {
                        private val oldMusicStart = remix.musicStartSec
                        private val newMusicStart = -(start.absoluteValue)
                        override fun redo(context: Remix) {
                            remix.musicStartSec = newMusicStart
                        }

                        override fun undo(context: Remix) {
                            remix.musicStartSec = oldMusicStart
                        }
                    })
                }
            }

            override fun frameUpdate(screen: MusicSelectScreen) {
                super.frameUpdate(screen)
                val start = editor.remix.music?.music?.getStartOfSound() ?: -1f
                this.visible = start >= 0f
                this.enabled = editor.remix.musicStartSec != -start
            }
        }.apply {
            this.location.set(screenX = 0.775f, screenWidth = 0.225f)
            this.addLabel(TextLabel(palette, this, this.stage).apply {
                this.textWrapping = true
                this.fontScaleMultiplier = 0.75f
                this.text = "screen.music.moveMusicStartToEstimate"
                this.location.set(pixelX = 2f, pixelWidth = -4f)
            })
        }
        stage.bottomStage.elements += moveMusicStartButton
        stage.centreStage.elements += object : TextLabel<MusicSelectScreen>(palette, stage.centreStage, stage.centreStage) {
            override fun frameUpdate(screen: MusicSelectScreen) {
                super.frameUpdate(screen)
                this.visible = isChooserOpen
            }
        }.apply {
            this.location.set(screenHeight = 0.25f)
            this.textAlign = Align.center
            this.isLocalizationKey = true
            this.text = "closeChooser"
            this.visible = false
        }
        mainLabel = TextLabel(palette, stage.centreStage, stage.centreStage).apply {
            this.textAlign = Align.center
            this.isLocalizationKey = false
            this.text = ""
        }
        stage.centreStage.elements += mainLabel

        stage.centreStage.elements += object : LoadingIcon<MusicSelectScreen>(palette, stage.centreStage) {
            override var visible: Boolean = true
                get() = field && isLoading
        }.apply {
            this.renderType = ImageLabel.ImageRendering.ASPECT_RATIO
            this.location.set(screenHeight = 0.125f, screenY = 0.125f / 2f)
        }

        tempoField = object: TextField<MusicSelectScreen>(palette, stage.centreStage, stage.centreStage) {
            override fun onTextChange(oldText: String) {
                super.onTextChange(oldText)
                try{
                    val oldChange = editor.remix.tempos.map[globalTempoChangeId]!!
                    val newTempo = TempoChange(editor.remix.tempos, oldChange.beat, text.toFloat(), oldChange.swing, oldChange.width, true)
                    editor.remix.mutate(TrackerValueChange(oldChange, newTempo))
                } catch(e:Exception) {
                    //We just ignore, the user is probably in the middle of typing
                }
            }
        }.apply {
            for(tempoChange in editor.remix.tempos.map) {
                if (tempoChange.value.immutable) {
                    globalTempoChangeId = tempoChange.key as Float
                    this.text = tempoChange.value.bpm.toString()
                }
            }
            this.background = true
            this.visible = false
            this.location.set(screenX = 0.25f, screenWidth = 0.24f, screenHeight = 0.10f)
        }
        stage.centreStage.elements += tempoField
        tempoFieldUnit = TextLabel(palette, stage.centreStage, stage.centreStage).apply{
            this.text = "bpm" //TODO swap to localization
            this.textColor = Color.GRAY
            this.textAlign = Align.right
            this.visible = false
            this.location.set(screenX = 0.25f, screenWidth = 0.23f, screenHeight = 0.10f)
        }
        stage.centreStage.elements += tempoFieldUnit
        tempoFieldLabel = TextLabel(palette, stage.centreStage, stage.centreStage).apply{
            this.text = "Tempo start:" //TODO swap to localization
            this.textAlign = Align.left
            this.visible = false
            this.location.set(screenX = 0.25f, screenY = 0.10f, screenWidth = 0.24f, screenHeight = 0.10f)
        }
        stage.centreStage.elements += tempoFieldLabel

        musicStartField = object: TextField<MusicSelectScreen>(palette, stage.centreStage, stage.centreStage) {
            override fun onTextChange(oldText: String) {
                super.onTextChange(oldText)
                try{
                    editor.remix.musicStartSec = (text.toFloat())/1000
                } catch(e:Exception) {
                    //We just ignore, the user is probably in the middle of typing
                }
            }
        }.apply {
            this.text = (editor.remix.musicStartSec*1000).toInt().toString()
            this.background = true
            this.visible = false
            this.location.set(screenX = 0.51f, screenWidth = 0.24f, screenHeight = 0.10f)
        }
        stage.centreStage.elements += musicStartField
        musicStartFieldUnit = TextLabel(palette, stage.centreStage, stage.centreStage).apply{
            this.text = "ms" //TODO swap to localization
            this.textColor = Color.GRAY
            this.textAlign = Align.right
            this.visible = false
            this.location.set(screenX = 0.51f, screenWidth = 0.23f, screenHeight = 0.10f)
        }
        stage.centreStage.elements += musicStartFieldUnit
        musicStartFieldLabel = TextLabel(palette, stage.centreStage, stage.centreStage).apply{
            this.text = "Music start:" //TODO swap to localization
            this.textAlign = Align.left
            this.visible = false
            this.location.set(screenX = 0.51f, screenY = 0.10f, screenWidth = 0.24f, screenHeight = 0.10f)
        }
        stage.centreStage.elements += musicStartFieldLabel

        stage.updatePositions()
        updateLabels(null)
    }

    override fun renderUpdate() {
        super.renderUpdate()

        stage.backButton.enabled = !(isChooserOpen || isLoading)

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            stage.onBackButtonClick()
        }
    }

    @Synchronized
    private fun openPicker() {
        if (!isChooserOpen) {
            GlobalScope.launch {
                isChooserOpen = true
                val filter = TinyFDWrapper.FileExtFilter(Localization["screen.music.fileFilter"] + "(.ogg, .mp3, .wav)", "*.ogg", "*.mp3", "*.wav")
                TinyFDWrapper.openFile(Localization["screen.music.fileChooserTitle"], attemptRememberDirectory(main, PreferenceKeys.FILE_CHOOSER_MUSIC) ?: getDefaultDirectory(), filter) { file ->
                    isChooserOpen = false
                    if (file != null) {
                        isLoading = true
                        val newInitialDirectory = if (!file.isDirectory) file.parentFile else file
                        persistDirectory(main, PreferenceKeys.FILE_CHOOSER_MUSIC, newInitialDirectory)
                        try {
                            updateLabels(null)
                            val handle = FileHandle(file)
                            val musicData = MusicData(handle, editor.remix)
                            editor.remix.music = musicData
                            isLoading = false
                            updateLabels(null)
                        } catch (t: Throwable) {
                            t.printStackTrace()
                            updateLabels(t)
                        } finally {
                            isLoading = false
                        }
                    }
                }
            }
        }
    }

    private fun updateLabels(throwable: Throwable? = null) {
        val label = mainLabel
        if (throwable == null) {
            val music = editor.remix.music
            if (isLoading) {
                tempoField.visible = false
                tempoFieldUnit.visible = false
                tempoFieldLabel.visible = false
                musicStartField.visible = false
                musicStartFieldUnit.visible = false
                musicStartFieldLabel.visible = false
                label.text = Localization["screen.music.loadingMusic"]
            } else {
                label.text = Localization["screen.music.currentMusic",
                        if (music == null) Localization["screen.music.noMusic"] else music.handle.name()]
                if (music != null) {
                    val start = music.music.getStartOfSound()
                    if (start >= 0f) {
                        label.text += "\n\n${Localization["screen.music.estimatedMusicStart", (Editor.TRACKER_MINUTES_FORMATTER.format((start / 60).toLong()) + ":" + Editor.TRACKER_TIME_FORMATTER.format(start % 60.0))]}"
                    }
                    tempoField.visible = true
                    tempoFieldUnit.visible = true
                    tempoFieldLabel.visible = true
                    musicStartField.visible = true
                    musicStartFieldUnit.visible = true
                    musicStartFieldLabel.visible = true
                    tempoField.text = editor.remix.tempos.map[globalTempoChangeId]!!.bpm.toString()
                    musicStartField.text = (editor.remix.musicStartSec*1000).toInt().toString()
                } else {
                    tempoField.visible = false
                    tempoFieldUnit.visible = false
                    tempoFieldLabel.visible = false
                    musicStartField.visible = false
                    musicStartFieldUnit.visible = false
                    musicStartFieldLabel.visible = false
                }
            }


//            if (music != null) {
//                if (music.handle.extension().equals("wav", true)) {
//                    label.text += "\n\n${Localization["screen.music.warning.wav"]}"
//                } else if (music.handle.extension().equals("mp3", true)) {
//                    label.text += "\n\n${Localization["screen.music.warning.mp3"]}"
//                }
//            }
        } else {
            label.text = when (throwable) {
                is MusicLoadingException -> throwable.getLocalizedText()
                else -> Localization["screen.music.invalid", throwable::class.java.canonicalName]
            }
        }
    }

    override fun show() {
        super.show()
        updateLabels()
        if (editor.remix.music == null) {
            openPicker()
        }
    }

    override fun dispose() {
    }

    override fun tickUpdate() {
    }

    inner class MusicFileChooserButton(palette: UIPalette, parent: UIElement<MusicSelectScreen>,
                                       stage: Stage<MusicSelectScreen>)
        : Button<MusicSelectScreen>(palette, parent, stage) {

        override fun onLeftClick(xPercent: Float, yPercent: Float) {
            super.onLeftClick(xPercent, yPercent)
            openPicker()
        }

        override fun onRightClick(xPercent: Float, yPercent: Float) {
            super.onRightClick(xPercent, yPercent)
            if (!isChooserOpen) {
                editor.remix.music = null
                updateLabels()
            }
        }

        override fun frameUpdate(screen: MusicSelectScreen) {
            super.frameUpdate(screen)
            this.enabled = !(isChooserOpen || isLoading)
        }
    }
}

