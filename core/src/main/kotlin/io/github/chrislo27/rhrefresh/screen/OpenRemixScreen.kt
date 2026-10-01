package io.github.chrislo27.rhrefresh.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.rhrefresh.PreferenceKeys
import io.github.chrislo27.rhrefresh.RHREfresh
import io.github.chrislo27.rhrefresh.RHREfreshApplication
import io.github.chrislo27.rhrefresh.RemixRecovery
import io.github.chrislo27.rhrefresh.editor.Editor
import io.github.chrislo27.rhrefresh.editor.stage.EditorStage
import io.github.chrislo27.rhrefresh.entity.Entity
import io.github.chrislo27.rhrefresh.entity.model.ISoundDependent
import io.github.chrislo27.rhrefresh.entity.model.ModelEntity
import io.github.chrislo27.rhrefresh.sfxdb.SFXDatabase
import io.github.chrislo27.rhrefresh.sfxdb.datamodel.Datamodel
import io.github.chrislo27.rhrefresh.soundsystem.SoundCache
import io.github.chrislo27.rhrefresh.stage.GenericStage
import io.github.chrislo27.rhrefresh.stage.LoadingIcon
import io.github.chrislo27.rhrefresh.track.Remix
import io.github.chrislo27.rhrefresh.util.*
import io.github.chrislo27.rhrefresh.util.err.MusicLoadingException
import io.github.chrislo27.toolboks.ToolboksScreen
import io.github.chrislo27.toolboks.i18n.Localization
import io.github.chrislo27.toolboks.registry.AssetRegistry
import io.github.chrislo27.toolboks.registry.ScreenRegistry
import io.github.chrislo27.toolboks.ui.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.zip.ZipFile
import javax.sound.midi.MidiSystem


class OpenRemixScreen(main: RHREfreshApplication)
    : ToolboksScreen<RHREfreshApplication, OpenRemixScreen>(main) {

    private val editorScreen: EditorScreen by lazy { ScreenRegistry.getNonNullAsType<EditorScreen>("editor") }
    private val editor: Editor
        get() = editorScreen.editor
    override val stage: GenericStage<OpenRemixScreen> = GenericStage(main.uiPalette, null, main.defaultCamera)

    @Volatile
    private var isChooserOpen = false
        set(value) {
            field = value
            stage.backButton.enabled = !isChooserOpen
        }
    @Volatile
    private var isLoading = false
    @Volatile
    private var isLoadingSounds = false

    private enum class RemixType {
        RHREFRESH, RHRE3, RHRE2, MIDI
    }

    private val loadButton: LoadButton
    private val mainLabel: TextLabel<OpenRemixScreen>
    private val loadingCountLabel: TextLabel<OpenRemixScreen>
    @Volatile
    private var remix: Remix? = null
        set(value) {
            field = value
            loadButton.visible = field != null
        }

    private fun List<Entity>.applyFilter(): List<ModelEntity<*>> {
        return filterIsInstance<ISoundDependent>()
                .filterIsInstance<ModelEntity<*>>()
                .distinctBy { it.datamodel.id }
    }

    private val icon: TextureRegion by lazy { TextureRegion(AssetRegistry.get<Texture>("ui_icon_folder")) }
    private val rhre3Icon: TextureRegion by lazy { TextureRegion(AssetRegistry.get<Texture>("logo_legacy_rhre3_128")) }
    private val rhre2Icon: TextureRegion by lazy { TextureRegion(AssetRegistry.get<Texture>("logo__legacy_rhre2_128")) }

    init {
        stage.titleIcon.image = icon
        stage.titleLabel.text = "screen.open.title"
        stage.backButton.visible = true
        stage.onBackButtonClick = {
            if (!isChooserOpen && !isLoading) {
                SoundCache.unloadAll()
                editor.remix.entities.applyFilter().forEach { entity ->
                    if (entity is ISoundDependent) {
                        entity.preloadSounds()
                    }
                }
                main.screen = ScreenRegistry.getNonNull("editor")
            }
        }

        val palette = main.uiPalette

        stage.centreStage.elements += object : LoadingIcon<OpenRemixScreen>(palette, stage.centreStage) {
            override var visible: Boolean = true
                get() = field && (isLoading || isLoadingSounds)
        }.apply {
            this.renderType = ImageLabel.ImageRendering.ASPECT_RATIO
            this.location.set(screenHeight = 0.125f, screenY = 0.125f / 2f)
        }
        stage.centreStage.elements += object : TextLabel<OpenRemixScreen>(palette, stage.centreStage,
                                                                          stage.centreStage) {
            override fun frameUpdate(screen: OpenRemixScreen) {
                super.frameUpdate(screen)
                this.visible = isChooserOpen
            }
        }.apply {
            this.location.set(screenHeight = 0.125f)
            this.textAlign = Align.center
            this.isLocalizationKey = true
            this.text = "closeChooser"
            this.visible = false
        }
        mainLabel = TextLabel(palette, stage.centreStage, stage.centreStage).apply {
            this.location.set(screenHeight = 0.875f, screenY = 0.125f)
            this.textAlign = Align.center
            this.isLocalizationKey = false
            this.text = ""
        }
        stage.centreStage.elements += mainLabel
        loadingCountLabel = TextLabel(palette, stage.centreStage, stage.centreStage).apply {
            this.location.set(screenHeight = 0.125f / 2)
            this.textAlign = Align.center
            this.isLocalizationKey = false
            this.fontScaleMultiplier = 0.8f
            this.text = "test / test"
        }
        stage.centreStage.elements += loadingCountLabel
        loadButton = LoadButton(palette, stage.bottomStage, stage.bottomStage).apply {
            this.location.set(screenX = 0.25f, screenWidth = 0.5f)
            this.addLabel(TextLabel(palette, this, this.stage).apply {
                this.textAlign = Align.center
                this.text = "screen.open.button"
                this.isLocalizationKey = true
            })
            this.visible = false
        }
        stage.bottomStage.elements += loadButton

        stage.updatePositions()
    }

    override fun renderUpdate() {
        super.renderUpdate()

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            stage.onBackButtonClick()
        }
    }

    fun loadFile(file: File, overrideAutosave: Boolean? = null) {
        if (isLoading)
            return
        isLoading = true
        GlobalScope.launch {
            try {
                remix = null
                System.gc()

                val newRemix = editor.createRemix()
                val remixType: RemixType
                stage.titleIcon.image = icon
                val result = if (file.extension.equals("mid", ignoreCase = true)) {
                    remixType = RemixType.MIDI
                    Remix.fromMidiSequence(newRemix, MidiSystem.getSequence(file))
                } else {
                    val zipFile = ZipFile(file)
                    val isRHRE2 = zipFile.getEntry("remix.json") == null
                    
                    if (file.extension.equals("rhre3", ignoreCase = true)) {
                        remixType = RemixType.RHRE3
                    }
                    else if (zipFile.getEntry("remix.json") == null) {
                        remixType = RemixType.RHRE2
                    }
                    else {
                        remixType = RemixType.RHREFRESH
                    }

                    try {
                        if (remixType == RemixType.RHRE2) {
                            stage.titleIcon.image = rhre2Icon
                            Remix.unpackRHRE2(newRemix, zipFile)
                        }
                        else if (remixType == RemixType.RHRE3) {
                            stage.titleIcon.image = rhre3Icon
                            Remix.unpack(newRemix, zipFile, false)
                        }
                        else {
                            Remix.unpack(newRemix, zipFile, false)
                        }
                    } catch (e: Exception) {
                        throw e
                    } finally {
                        zipFile.close()
                    }
                }

                val toLoad = result.remix.entities.applyFilter()
                val toLoadIDs = toLoad.map { it.datamodel.id }.toSet()
                val toUnload = editor.remix.entities.applyFilter().filter { it.datamodel.id !in toLoadIDs }

                val isAutosave = overrideAutosave ?: result.isAutosave

                val coroutine: Job = launch {
                    isLoadingSounds = true
                    val total = toUnload.size + toLoad.size
                    fun updateLabel(progress: Int) {
                        Gdx.app.postRunnable {
                            loadingCountLabel.text = if (progress >= total) "" else Localization["screen.open.loadingEntities", "$progress / $total"]
                        }
                    }
                    toUnload.forEachIndexed { i, entity ->
                        if (entity is ISoundDependent) {
                            entity.unloadSounds()
                        }
                        updateLabel(i + 1)
                    }
                    toLoad.forEachIndexed { i, entity ->
                        if (entity is ISoundDependent) {
                            entity.preloadSounds()
                        }
                        updateLabel(i + 1 + toUnload.size)
                    }
                    isLoadingSounds = false
                }

                fun goodBad(str: String, bad: Boolean, badColour: String = "ORANGE"): String {
                    return if (bad) "[$badColour]$str[]" else "[LIGHT_GRAY]$str[]"
                }

                loadButton.alsoDo = {
                    runBlocking {
                        coroutine.join()
                    }

                    if (!isAutosave) {
                        val fh = FileHandle(file)
                        editor.setFileHandles(fh)
                        RemixRecovery.cacheRemixChecksum(result.remix)
                    }
                }

                remix = result.remix
                val remix = remix!!
                val missingAssets = result.missing
                val databaseStr = if (remixType == RemixType.RHRE2)
                    goodBad(remix.version.toString(), true)
                else
                    goodBad(remix.databaseVersion.toString(),
                            remix.databaseVersion != SFXDatabase.data.version)
                val databaseCommitStr = if (remixType == RemixType.RHRE2) ""
                else
                    goodBad(remix.databaseVersionCommit,
                        remix.databaseVersionCommit != RHREfresh.DATABASE_CURRENT_COMMIT)
                val wasDevBranch = remixType != RemixType.RHRE2 && remix.wasDevBranch

                mainLabel.text = ""

                if (isAutosave) {
                    mainLabel.text += Localization["screen.open.autosave"] + "\n\n"
                }

                mainLabel.text += if (remixType == RemixType.MIDI) {
                    val noteCue = result.extra["noteCue"] as Datamodel?
                    val noteCount: Any = if (noteCue == null) "N/A" else remix.entities.count { it is ModelEntity<*> && it.datamodel == noteCue }
                    Localization["screen.open.info.midi", remix.midiInstruments, noteCount, "${noteCue?.name} (${noteCue?.game?.name})"]
                } else {
                    val dbVersionString = if (!wasDevBranch) databaseStr else "DEV-$databaseStr-$databaseCommitStr"
                    Localization["screen.open.info",
                            goodBad(remix.version.toString(), remix.version != RHREfresh.VERSION),
                            dbVersionString,
                            goodBad(missingAssets.first.toString(), missingAssets.first > 0, "RED"),
                            goodBad(if (remixType != RemixType.RHRE3) "0" else missingAssets.second.toString(),
                                    missingAssets.second > 0, "RED")]
                }
                if ((wasDevBranch && !main.preferences.getBoolean(PreferenceKeys.ADVOPT_SFXDB_USE_DEV_BRANCH, false))
                    && RHREfresh.DATABASE_CURRENT_COMMIT != remix.databaseVersionCommit) {
                    mainLabel.text += "\n\n" + Localization["screen.open.devDatabase"]
                } else if ((wasDevBranch && main.preferences.getBoolean(PreferenceKeys.ADVOPT_SFXDB_USE_DEV_BRANCH, false))
                    && RHREfresh.DATABASE_CURRENT_COMMIT != remix.databaseVersionCommit) {
                    mainLabel.text += "\n\n" + Localization["screen.open.differentCommit"]
                }
                if (SFXDatabase.data.version < remix.databaseVersion)  {
                    mainLabel.text += "\n\n" + Localization["screen.open.oldDatabase"]
                } else if (remix.version < RHREfresh.VERSION) {
                    mainLabel.text += "\n\n" +
                            Localization[if (remixType == RemixType.RHRE2)
                                "screen.open.rhre2Warning"
                            else
                                "screen.open.oldWarning"]
                }
                if (remix.version > RHREfresh.VERSION) {
                    mainLabel.text += "\n\n" + Localization["screen.open.oldWarning2"]
                }
                isLoading = false
            } catch (t: Throwable) {
                t.printStackTrace()
                mainLabel.text = when (t) {
                    is MusicLoadingException -> t.getLocalizedText()
                    else -> Localization["screen.open.failed", t::class.java.canonicalName]
                }
                remix?.dispose()
                remix = null
                isLoading = false
            }
        }
    }

    @Synchronized
    private fun openPicker() {
        if (!isChooserOpen) {
            GlobalScope.launch {
                isChooserOpen = true
                val initialDirectory: File? = attemptRememberDirectory(main, PreferenceKeys.FILE_CHOOSER_LOAD) ?: getDefaultDirectory()
                 val fileFilter = TinyFDWrapper.FileExtFilter(Localization["screen.open.fileFilterSupported"] + "(.${RHREfresh.REMIX_FILE_EXTENSION}, .rhre3, .brhre2, .mid)", "*.${RHREfresh.REMIX_FILE_EXTENSION}", "*.rhre3", "*.brhre2", "*.mid")
                TinyFDWrapper.openFile(Localization["screen.open.fileChooserTitle"], initialDirectory, fileFilter) { file ->
                    isChooserOpen = false
                    if (file != null) {
                        val newInitialDirectory = if (!file.isDirectory) file.parentFile else file
                        persistDirectory(main, PreferenceKeys.FILE_CHOOSER_LOAD, newInitialDirectory)
                        loadFile(file)
                    } else {
                        loadButton.alsoDo = {}
                        Gdx.app.postRunnable {
                            stage.onBackButtonClick()
                        }
                    }
                }
            }
        }
    }

    fun attemptOpenPicker() {
        if (!isLoading) {
            openPicker()
            remix = null
        }
    }

    override fun show() {
        super.show()
        stage.titleIcon.image = icon
        loadingCountLabel.text = ""
    }

    override fun hide() {
        super.hide()
        mainLabel.text = ""
        remix = null
        loadButton.alsoDo = {}
        isLoadingSounds = false
    }

    override fun dispose() {
    }

    override fun tickUpdate() {
    }

    inner class LoadButton(palette: UIPalette, parent: UIElement<OpenRemixScreen>,
                           stage: Stage<OpenRemixScreen>)
        : Button<OpenRemixScreen>(palette, parent, stage) {

        @Volatile
        var alsoDo = {}

        override fun onLeftClick(xPercent: Float, yPercent: Float) {
            super.onLeftClick(xPercent, yPercent)
            val remix = remix ?: return
            editor.remix = remix
            alsoDo()
            editor.remix.recomputeCachedData()
            editor.stage.updateSelected(EditorStage.DirtyType.SEARCH_DIRTY)
            this@OpenRemixScreen.stage.onBackButtonClick()
            Gdx.app.postRunnable {
                System.gc()
            }
        }
    }
}
