package io.github.chrislo27.rhrefresh.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.rhrefresh.PreferenceKeys
import io.github.chrislo27.rhrefresh.RHREfresh
import io.github.chrislo27.rhrefresh.RHREfreshApplication
import io.github.chrislo27.rhrefresh.analytics.AnalyticsHandler
import io.github.chrislo27.rhrefresh.editor.Editor
import io.github.chrislo27.rhrefresh.editor.stage.advopt.ExportImageButton
import io.github.chrislo27.rhrefresh.screen.ExportRemixScreen.ExportFileType.MP3
import io.github.chrislo27.rhrefresh.sfxdb.SFXDatabase
import io.github.chrislo27.rhrefresh.sfxdb.Series
import io.github.chrislo27.rhrefresh.sfxdb.datamodel.impl.Cue
import io.github.chrislo27.rhrefresh.soundsystem.beads.BeadsSoundSystem
import io.github.chrislo27.rhrefresh.soundsystem.FFmpeg
import io.github.chrislo27.rhrefresh.stage.GenericStage
import io.github.chrislo27.rhrefresh.track.Remix
import io.github.chrislo27.rhrefresh.util.*
import io.github.chrislo27.toolboks.Toolboks
import io.github.chrislo27.toolboks.ToolboksScreen
import io.github.chrislo27.toolboks.i18n.Localization
import io.github.chrislo27.toolboks.registry.AssetRegistry
import io.github.chrislo27.toolboks.registry.ScreenRegistry
import io.github.chrislo27.toolboks.ui.*
import io.github.chrislo27.toolboks.util.gdxutils.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ws.schild.jave.utils.RBufferedReader
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import kotlin.math.*


class ExportRemixScreen(main: RHREfreshApplication)
    : ToolboksScreen<RHREfreshApplication, ExportRemixScreen>(main) {
    
    private val editorScreen: EditorScreen by lazy { ScreenRegistry.getNonNullAsType<EditorScreen>("editor") }
    private val editor: Editor
        get() = editorScreen.editor
    override val stage: GenericStage<ExportRemixScreen> = GenericStage(main.uiPalette, null, main.defaultCamera)
    private val remix: Remix
        get() = editor.remix
    
    @Volatile
    private var isChooserOpen = false
        set(value) {
            field = value
            setBackButtonEnabled()
        }
    @Volatile
    private var isExporting = false
        set(value) {
            field = value
            setBackButtonEnabled()
        }
    private var partial = false
    private var hasEndRemix = false
    private val mainLabel: TextLabel<ExportRemixScreen>
    private val folderButton: Button<ExportRemixScreen>
    private var folderFile: File? = null
    private val readyButton: Button<ExportRemixScreen>
    private val copyGamesButton: Button<ExportRemixScreen>
    private val exportImageButton: ExportImageButton
    private val selectionStage: SelectionStage
    
    private enum class ExportFileType(val extension: String) {
        WAV("wav"), MP3("mp3"), OGG_VORBIS("ogg"), FLAC("flac"), AAC("aac");
        
        companion object {
            val VALUES: List<ExportFileType> by lazy { values().toList() }
            val EXTENSIONS: List<String> by lazy { VALUES.map(ExportFileType::extension) }
        }
    }
    
    private fun setBackButtonEnabled() {
        stage.backButton.enabled = !isChooserOpen && !isExporting
    }
    
    init {
        stage.titleIcon.image = TextureRegion(AssetRegistry.get<Texture>("ui_icon_export"))
        stage.titleLabel.text = "screen.export.title"
        stage.backButton.visible = true
        stage.onBackButtonClick = {
            if (!isChooserOpen && !isExporting) {
                main.screen = ScreenRegistry.getNonNull("editor")
            }
        }
        
        val palette = main.uiPalette
        
        stage.centreStage.elements += object : TextLabel<ExportRemixScreen>(palette, stage.centreStage,
                                                                            stage.centreStage) {
            override fun frameUpdate(screen: ExportRemixScreen) {
                super.frameUpdate(screen)
                this.visible = isChooserOpen
            }
        }.apply {
            this.location.set(screenHeight = 0.1f)
            this.textAlign = Align.center
            this.isLocalizationKey = true
            this.text = "closeChooser"
            this.visible = false
        }
        
        mainLabel = TextLabel(palette, stage.centreStage, stage.centreStage).apply {
            this.location.set(screenHeight = 0.8f, screenY = 0.2f)
            this.textAlign = Align.center
            this.isLocalizationKey = false
            this.text = ""
        }
        stage.centreStage.elements += mainLabel
        
        selectionStage = SelectionStage(palette, stage.centreStage, stage.centreStage.camera).apply {
            this.location.set(screenHeight = 0.1f, screenY = 0.1f)
            
        }
        stage.centreStage.elements += selectionStage
        
        readyButton = Button(palette, stage.bottomStage, stage.bottomStage).apply {
            this.addLabel(TextLabel(palette, this, this.stage).apply {
                this.isLocalizationKey = true
                this.textWrapping = false
                this.text = "screen.export.fileChooserTitle"
            })
            this.leftClickAction = { _, _ ->
                openPicker()
                updateLabels()

                this.visible = false
                selectionStage.visible = false
                copyGamesButton.visible = false
                exportImageButton.visible = false
            }
            this.visible = false
            
            this.location.set(screenX = 0.225f, screenWidth = 0.55f)
        }
        stage.bottomStage.elements += readyButton

        if (RHREfresh.CURRENT_OS != RHREfresh.OS.MACOS) {
            folderButton = Button(palette, stage.bottomStage, stage.bottomStage).apply {
                this.addLabel(ImageLabel(palette, this, this.stage).apply {
                    this.image = TextureRegion(AssetRegistry.get<Texture>("ui_icon_folder"))
                })
                this.leftClickAction = { _, _ ->
                    val ff = folderFile
                    if (ff != null) {
                        Desktop.getDesktop().open(ff.takeUnless { it.isFile } ?: ff.parentFile)
                    }
                }
                this.tooltipTextIsLocalizationKey = true
                this.tooltipText = "screen.export.openFolder"
                this.visible = false

                this.location.set(this@ExportRemixScreen.stage.backButton.location)
                this.location.set(screenX = 1f - this.location.screenWidth)
            }
        } else {
            folderButton = Button(palette, stage.bottomStage, stage.bottomStage).apply {
                this.addLabel(ImageLabel(palette, this, this.stage).apply {
                    this.image = TextureRegion(AssetRegistry.get<Texture>("ui_icon_clipboard"))
                })
                this.tooltipTextIsLocalizationKey = true
                this.tooltipText = "screen.export.copyFolder"
                this.leftClickAction = { _, _ ->
                    val ff = folderFile
                    if (ff != null) {
                        Gdx.app.clipboard.contents = ff.parentFile.absolutePath
                    }
                }
                this.visible = false

                this.location.set(this@ExportRemixScreen.stage.backButton.location)
                this.location.set(screenX = 1f - this.location.screenWidth)
            }
        }
        stage.bottomStage.elements += folderButton

        copyGamesButton = object: Button<ExportRemixScreen>(palette, stage.bottomStage, stage.bottomStage) {
            val strings: List<String> = listOf("screen.export.copyGames", "screen.export.copyGames.copied", "screen.export.copyGames.noGames")
            override fun onLeftClick(xPercent: Float, yPercent: Float) {
                super.onLeftClick(xPercent, yPercent)
                val games = editor.getGamesUsedInRemix()

                val label = this.labels.first() as TextLabel
                if (games.isEmpty()) {
                    label.text = strings[2]
                } else {
                    label.text = strings[1]
                    Gdx.app.clipboard.contents = games
                }
            }
            override var tooltipText: String?
                set(_) {}
                get() {
                    val label = this.labels.first() as TextLabel
                    return when (label.text) {
                        strings[1] -> "screen.export.copyGames.tooltip.copied"
                        strings[2] -> "screen.export.copyGames.tooltip.noGames"
                        else -> "screen.export.copyGames.tooltip"
                    }
                }
        }.apply {
            this.addLabel(
                object : TextLabel<ExportRemixScreen>(palette, this, this.stage) {
                }.apply {
                    this.setText(
                        "screen.export.copyGames",
                        Align.center, false, false
                    )
                    this.isLocalizationKey = true
                    this.background = false
                    this.fontScaleMultiplier = 0.8f
                })
            this.tooltipTextIsLocalizationKey = true
            this.location.set(this@ExportRemixScreen.stage.backButton.location)
            this.location.set(screenX = 1f - this.location.screenWidth)
        }
        stage.bottomStage.elements += copyGamesButton

        exportImageButton = ExportImageButton(editor, palette, stage.bottomStage, stage.bottomStage).apply {
            this.location.set(this@ExportRemixScreen.stage.backButton.location)
            this.location.set(screenX = 1f - (this.location.screenWidth*2+0.0125f))
        }
        stage.bottomStage.elements += exportImageButton
        
        stage.updatePositions()
        updateLabels(null)
    }
    
    override fun renderUpdate() {
        super.renderUpdate()
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            stage.onBackButtonClick()
        }
    }
    
    @Synchronized
    private fun export(file: File, playSuccessDing: Boolean,
                       startSeconds: Float = selectionStage.percentToSeconds(selectionStage.startPercent),
                       endSeconds: Float = selectionStage.percentToSeconds(selectionStage.endPercent),
                       exportOptions: ExportOptions = RHREfresh.exportOptions) {
        if (isExporting || !hasEndRemix)
            return
        isExporting = true
        remix.isExporting = true
        
        var maxProgressStages: Int = -1
        
        fun updateProgress(localization: String, localPercent: Int, stage: Int) {
            mainLabel.text = Localization["screen.export.progress",
                    Localization["screen.export.progress.$localization"],
                    "$localPercent",
                    stage,
                    maxProgressStages,
                    "${((localPercent + (stage - 1) * 100f) / (maxProgressStages * 100f) * 100).roundToLong()}"]
        }

        var success = true
        val exportStartTime = System.currentTimeMillis()
        mainLabel.text = Localization["screen.export.progress.start"]
        // First we prepare the remix part bits
        val remixPartsCues = FFmpeg.generateParts(remix, startSeconds, endSeconds)
        maxProgressStages = remixPartsCues.size+1
        updateProgress("ffmpeg.part",0,1)

        // Then, we export them to files
        val remixPartFiles: MutableList<FFmpeg.RemixPartFile> = mutableListOf()
        for((i, remixPartCues) in remixPartsCues.withIndex()) {
            updateProgress("ffmpeg.part", 0, i + 1)
            val part = FFmpeg.exportPart(remixPartCues, exportOptions.sampleRate)
            try {
                part.ffmpegExecutor.execute()
                val reader =
                    RBufferedReader(InputStreamReader(part.ffmpegExecutor.errorStream))
                var line: String?
                while (((reader.readLine().also { line = it })) != null) {
                    val currentTime = FFmpeg.getCurrentTime(line!!)
                    // currentTime would be -1 if the line doesn't include any sort of time indicator, which might happen sometimes
                    if(currentTime!=-1f){
                        var percent = ((currentTime / remixPartCues.durationMs) * 100).toInt()
                        if(percent>100) percent = 100 // Cheating for the lack of precision I have in determining duration :)
                        updateProgress("ffmpeg.part", percent, i + 1)
                    }
                }
                if (part.ffmpegExecutor.getProcessExitCode() != 0) {
                    Toolboks.LOGGER.info("Error on the export.")
                    success = false
                    break
                } else {
                    remixPartFiles += part
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toolboks.LOGGER.info("Error on the export.")
                success = false
                break
            } finally {
                part.ffmpegExecutor.destroy()
            }
        }

        // If it succeeded, we merge them all together!
        if(success){
            // First we get the approximate final duration of the export
            var maxDuration = 0f
            for(file in remixPartFiles){
                val fileDuration = FFmpeg.createMultimediaObject(file.file).info.duration + file.initialDelay
                if(fileDuration > maxDuration){
                    maxDuration = fileDuration
                }
            }
            if(remix.music != null){
                val musicDuration = (remix.musicStartSec*1000)+FFmpeg.createMultimediaObject(remix.music!!.handle.file()).info.duration
                if(musicDuration > maxDuration){
                    maxDuration = musicDuration
                }
            }

            // Then we start the FFmpeg export
            updateProgress("ffmpeg.full",0,maxProgressStages)
            val ffmpegExecutor = FFmpeg.createFinalRemixExecutor(remix, file, startSeconds, endSeconds, exportOptions.sampleRate, remixPartFiles)
            try {
                ffmpegExecutor.execute()
                val reader =
                    RBufferedReader(InputStreamReader(ffmpegExecutor.errorStream))
                var line: String?
                while (((reader.readLine().also { line = it })) != null) {
                    val currentTime = FFmpeg.getCurrentTime(line!!)
                    // currentTime would be -1 if the line doesn't include any sort of time indicator, which might happen sometimes
                    if(currentTime!=-1f){
                        updateProgress("ffmpeg.full", ((currentTime / maxDuration) * 100).toInt(), maxProgressStages)
                    }
                }
                if (ffmpegExecutor.getProcessExitCode() != 0) {
                    Toolboks.LOGGER.info("Error on the export.")
                }
            } catch (e: IOException){
                Toolboks.LOGGER.info("Error on the export.")
            }
            finally {
                ffmpegExecutor.destroy()
                for(part in remixPartFiles){
                    part.file.delete()
                }
            }
            Toolboks.LOGGER.info("Remix exported in ${(System.currentTimeMillis()-exportStartTime)/1000}s")
        } else{
            mainLabel.text = Localization["screen.export.failed"]
            Toolboks.LOGGER.info("Remix not exported in ${(System.currentTimeMillis()-exportStartTime)/1000}s")
        }

        isExporting = false
        remix.isExporting = false

        BeadsSoundSystem.isRealtime = true
        BeadsSoundSystem.resume()
        val indicatorCue: Cue? = if (success) {
            (SFXDatabase.data.objectMap["mrUpbeatWii/applause"] as? Cue)?.takeIf { playSuccessDing }
        } else {
            (SFXDatabase.data.objectMap["extraSFX/audience/Wii/disappointed"] as? Cue)
        }
        if (indicatorCue != null) {
            BeadsSoundSystem.newSound(indicatorCue.soundHandle).play(loop = false, pitch = 1f, rate = 1f, volume = 1f, position = 0.0)
        } else {
            Toolboks.LOGGER.warn("Export SFX (success=$success) not found")
        }
            if (playSuccessDing) {
                folderFile = file
                folderButton.visible = true
            }


            if (!playSuccessDing) {
                stage.backButton.enabled = false
            }
//        }
//
//        try {
//            // prep triggers
//            val startMs = min(remix.musicStartSec.toDouble(), remix.tempos.beatsToSeconds(remix.entities.minByOrNull { it.getLowerUpdateableBound() }?.getLowerUpdateableBound() ?: 0.0f).toDouble()).toFloat() * 1000.0
//            val endMs = endSeconds * 1000.0
//            val durationMs = endMs - startMs
//
//            // reset things
//            remix.playbackStart = remix.tempos.secondsToBeats(startMs.toFloat() / 1000f)
//            remix.playState = PlayState.PLAYING
//            remix.playState = PlayState.STOPPED
//            remix.speedMultiplier = 1f
//
//            // music trigger
//            if (remix.music != null) {
//                val music = MiniAudioMusic((remix.music!!.music as MiniAudioMusic).audio)
//                music.stop()
//
//                val musicStartMs = (remix.musicStartSec * 1000) - startMs
//
////                context.out.addDependent(DelayTrigger(context, musicStartMs, addBead {
////                    music.also(MiniAudioMusic::play)
////                }))
//
//                // volumes
//                context.out.addDependent(Clock(context, 10f).apply {
//                    addMessageListener(addBead {
//                        music.setVolume(remix.musicVolumes.volumeAt(remix.beat))
//                        // Music distort
////                        val inDistortion = remix.entities.any { it is MusicDistortEntity && remix.beat in it.bounds.x..it.bounds.maxX }
////                        val currentlyDistorted = music.player.doBandpass
////                        if (inDistortion != currentlyDistorted) {
////                            music.player.doBandpass = inDistortion
////                        }
//                    })
//                })
//            }
//
//            if (startSeconds * 1000 <= startMs) {
//                context.out.addDependent(recorder)
//            } else {
//                // Start recording at a certain time
//                context.out.addDependent(DelayTrigger(context, startSeconds * 1000 - startMs, addBead {
//                    context.out.addDependent(recorder)
//                }))
//            }
//
//            // SFX timing
//            // The clock resolution will change depending on the tempo in the remix at any point in time
//            // The theoretical coarsest resolution should be 1/32 beats per clock cycle (0.03125) (based on snap divisions)
//            // But, to be safe, the coarsest ideal resolution will be 1/50 beats per clock cycle
//            // The computed ms interval value should be between 0.1 and 10 ms to ensure precision
//            val clockTimerControl: UGen = object : UGen(context, 1) {
//                override fun calculateBuffer() {
//                    // NO-OP
//                }
//
//                override fun setValue(value: Float) {
//                    // NO-OP, always computed
//                }
//
//                override fun getValueDouble(): Double {
//                    // To compute:
//                    // Find out how many ms are in a beat, based on the tempo.
//                    val tempo = remix.tempos.tempoAtSeconds(remix.seconds)
//                    val msPerBeat = (1.0 / tempo) * 60_000
//                    // Target resolution is 1/50 beats per cycle, so divide by 50
//                    return (msPerBeat / 50.0).coerceIn(0.1, 10.0)
//                    // In theory, the 0.1 ms threshold will not be met. The maximum BPM is 600, which results in a timing of 2 ms.
//                }
//
//                override fun getValue(i: Int, j: Int): Float {
//                    return getValueDouble(i, j).toFloat()
//                }
//
//                override fun getValue(): Float {
//                    return valueDouble.toFloat()
//                }
//
//                override fun getValueDouble(i: Int, j: Int): Double {
//                    return valueDouble
//                }
//            }
//            context.out.addDependent(Clock(context, clockTimerControl).apply {
//                addMessageListener(addBead {
//                    remix.seconds = (context.time + startMs).toFloat() / 1000.0f
//                    remix.entities.forEach {
//                        remix.entityUpdate(it)
//                    }
//                })
//            })
//            // Update labels at 60 fps
//            context.out.addDependent(Clock(context, 1000f / 60).apply {
//                addMessageListener(addBead {
//                    val percent = (context.time / (endMs - startMs) * 100).roundToLong().coerceIn(0, 100).toInt()
//                    updateProgress("pcm", percent, 1)
//                })
//            })
//
//            // scale gain
//            context.out.gain = 0.5f
//
//            // run!
//            context.out.pause(false)
//            context.runForNMillisecondsNonRealTime(durationMs.coerceAtLeast(1.0))
//        } catch (e: Exception) {
//            e.printStackTrace()
//            finalize(false)
//            throw e
//        }
//
//        try {
//            finalize(true)
//        } catch (e: Exception) {
//            e.printStackTrace()
//            finalize(false)
//            throw e
//        }
    }
    
    @Synchronized
    private fun openPicker() {
        if (!isChooserOpen && !isExporting && hasEndRemix) {
            GlobalScope.launch {
                isChooserOpen = true
                Gdx.app.postRunnable {
                    mainLabel.text = ""
                }
                val filter = if (FFmpeg.isSupported){
                    TinyFDWrapper.FileExtFilter(Localization["screen.export.fileFilter", "MP3, OGG, FLAC, WAV, AAC"], "*.mp3", "*.ogg", "*.flac", "*.wav", "*.aac")
                } else {
                    TinyFDWrapper.FileExtFilter(Localization["screen.export.fileFilter", "MP3, WAV"], "*.mp3", "*.wav")
                }
                TinyFDWrapper.saveFile(Localization["screen.export.fileChooserTitle"], attemptRememberDirectory(main, PreferenceKeys.FILE_CHOOSER_EXPORT) ?: getDefaultDirectory(), filter) { file ->
                    isChooserOpen = false
                    Gdx.app.postRunnable {
                        mainLabel.text = ""
                    }
                    if (file != null) {
                        val newInitialDirectory = if (!file.isDirectory) file.parentFile else file
                        persistDirectory(main, PreferenceKeys.FILE_CHOOSER_EXPORT, newInitialDirectory)
                        GlobalScope.launch {
                            try {
                                val correctFile = if (file.extension.lowercase(Locale.ROOT) !in ExportFileType.EXTENSIONS)
                                    file.parentFile.resolve("${file.name}.mp3")
                                else
                                    file
                                val fileType = ExportFileType.VALUES.firstOrNull {
                                    it.extension == file.extension.lowercase(Locale.ROOT)
                                } ?: MP3
                                
                                export(correctFile, true)
                                
                                Gdx.app.postRunnable {
                                    mainLabel.text = Localization["screen.export.success"]
                                }
                                
                                // Analytics
                                AnalyticsHandler.track("Export Remix",
                                                       mapOf(
                                                               "sfxDatabase" to SFXDatabase.data.version,
                                                               "fileType" to fileType.extension,
                                                               "fileSize" to correctFile.length(),
                                                               "partial" to partial
                                                            ))
                            } catch (t: Throwable) {
                                t.printStackTrace()
                                updateLabels(t)
                                isExporting = false
                            }
                        }
                    } else {
                        stage.onBackButtonClick()
                    }
                }
            }
        }
    }
    
    private fun updateLabels(throwable: Throwable? = null) {
        val label = mainLabel
        hasEndRemix = remix.duration < Float.POSITIVE_INFINITY
        readyButton.visible = false
        folderButton.visible = false
        exportImageButton.visible = false
        folderFile = null
        selectionStage.visible = false
        if (!hasEndRemix) {
                label.text = Localization["screen.export.cannot", Localization["screen.export.needsEndRemix", SFXDatabase.data.objectMap[SFXDatabase.END_REMIX_ENTITY_ID]?.name] + "\n[LIGHT_GRAY]${Localization[Series.OTHER.localization]} ➡ ${SFXDatabase.data.specialGame.name} ➡ ${SFXDatabase.data.objectMap[SFXDatabase.END_REMIX_ENTITY_ID]?.name ?: "End Remix"}[]"]
        } else {
            if (throwable == null) {
                label.text = Localization["screen.export.prepare"]
                readyButton.visible = true
                copyGamesButton.visible = true
                exportImageButton.visible = true
                selectionStage.visible = true
            } else {
                label.text = Localization["screen.export.failed", throwable::class.java.canonicalName]
            }
        }
    }
    
    override fun show() {
        super.show()
        updateLabels()
    }
    
    override fun hide() {
        super.hide()
        BeadsSoundSystem.stop()
        folderButton.visible = false
        folderFile = null
    }
    
    override fun dispose() {
    }
    
    override fun tickUpdate() {
    }
    
    inner class SelectionStage(palette: UIPalette, parent: UIElement<ExportRemixScreen>?, camera: OrthographicCamera)
        : Stage<ExportRemixScreen>(parent, camera) {
        
        private val remix: Remix
            get() = editor.remix
        
        val leftLabel: TextLabel<ExportRemixScreen>
        val rightLabel: TextLabel<ExportRemixScreen>
        
        val remixStartSeconds: Float = min(remix.musicStartSec.toDouble(), remix.tempos.beatsToSeconds(remix.entities.minByOrNull { it.bounds.x }?.bounds?.x ?: 0.0f).toDouble()).toFloat()
        val remixEndSeconds: Float = remix.tempos.beatsToSeconds(if (remix.duration == Float.POSITIVE_INFINITY) remix.lastPoint else remix.duration)
        private val remixAbsDuration = remixEndSeconds - remixStartSeconds
        
        private val playbackStartSeconds = remix.tempos.beatsToSeconds(remix.playbackStart)
        var startPercent: Float = if (remixAbsDuration > 0f && playbackStartSeconds in remixStartSeconds..remixEndSeconds - 1f) (secondsToPercent(playbackStartSeconds)).coerceIn(0f, 1f) else 0f
        var endPercent: Float = 1f
        
        init {
            val timeLabelWidth = 0.2f
            
            leftLabel = TextLabel(palette, this, this.stage).apply {
                this.location.set(screenX = 0f, screenWidth = timeLabelWidth)
                this.isLocalizationKey = false
                this.textWrapping = false
                this.textAlign = Align.right
            }
            rightLabel = TextLabel(palette, this, this.stage).apply {
                this.location.set(screenX = 1f - timeLabelWidth, screenWidth = timeLabelWidth)
                this.isLocalizationKey = false
                this.textWrapping = false
                this.textAlign = Align.left
            }
            
            elements += leftLabel
            elements += rightLabel
            elements += TextLabel(palette, this, this.stage).apply {
                this.location.set(screenY = -1f)
                this.isLocalizationKey = true
                this.textWrapping = false
                this.text = "screen.export.durationControls"
                this.fontScaleMultiplier = 0.75f
            }
            
            elements += object : UIElement<ExportRemixScreen>(this, this) {
                val color = Color.valueOf("26AB57")
                override fun render(screen: ExportRemixScreen, batch: SpriteBatch, shapeRenderer: ShapeRenderer) {
                    val loc = this.location
                    val x = loc.realX
                    val y = loc.realY
                    val width = loc.realWidth
                    val height = loc.realHeight
                    val lineThickness = 4f
                    val selectionPercent = endPercent - startPercent
                    
                    // background
                    batch.setColor(0f, 0f, 0f, 0.8f)
                    batch.fillRect(x, y, width, height)
                    
                    // bar
                    val barFullWidth = width - lineThickness * 2f
                    val barWidth = barFullWidth * selectionPercent
                    val barX = x + lineThickness + barFullWidth * startPercent
                    batch.setColor(color.r, color.g, color.b, 1f)
                    batch.fillRect(barX, y, barWidth, height)
                    // bar darker outline
                    val arrowTex = AssetRegistry.get<Texture>("entity_stretchable_arrow")
                    val arrowHeight = height - lineThickness * 2f
                    val arrowheadWidth = min(barWidth / 2f, height * 0.5f)
                    batch.setColor(0f, 0f, 0f, 0.2f)
                    batch.draw(arrowTex, barX + arrowheadWidth, y + lineThickness, barWidth - arrowheadWidth * 2, arrowHeight, arrowTex.width / 2, 0, arrowTex.width / 2, arrowTex.height, false, false)
                    batch.draw(arrowTex, barX, y + lineThickness, arrowheadWidth, arrowHeight, 0, 0, arrowTex.width / 2, arrowTex.height, false, false)
                    batch.draw(arrowTex, barX + barWidth, y + lineThickness, -arrowheadWidth, arrowHeight, 0, 0, arrowTex.width / 2, arrowTex.height, false, false)
                    
                    // border
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.drawRect(x, y, width, height, lineThickness)
                    
                    if (wasClickedOn) {
                        var inputPercent = ((stage.camera.getInputX() - x - lineThickness) / barFullWidth).coerceIn(0f, 1f)
                        
                        if (Gdx.input.isShiftDown() && !Gdx.input.isControlDown() && !Gdx.input.isAltDown()) {
                            inputPercent = secondsToPercent(remix.tempos.beatsToSeconds(remix.tempos.secondsToBeats(percentToSeconds(inputPercent)).roundToInt().toFloat()).coerceIn(remixStartSeconds, remixEndSeconds)).coerceIn(0f, 1f)
                        }
                        
                        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                            if (inputPercent < endPercent) {
                                startPercent = inputPercent
                                updateLabels()
                            }
                        } else if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
                            if (inputPercent > startPercent) {
                                endPercent = inputPercent
                                updateLabels()
                            }
                        }
                    }
                }
                
                override fun canBeClickedOn(): Boolean {
                    return true
                }
                
                override fun keyTyped(character: Char): Boolean {
                    val sup = super.keyTyped(character)
                    if (!sup) {
                        if (character == 'r') {
                            startPercent = 0f
                            endPercent = 1f
                            updateLabels()
                            return true
                        }
                    }
                    
                    return sup
                }
            }.apply {
                this.location.set(screenX = timeLabelWidth, screenWidth = 1f - timeLabelWidth * 2)
            }
            
            updateLabels()
        }
        
        fun percentToSeconds(percent: Float): Float = MathUtils.lerp(remixStartSeconds, remixEndSeconds, percent)
        fun secondsToPercent(seconds: Float): Float = (seconds - remixStartSeconds) / (remixEndSeconds - remixStartSeconds)
        
        fun updateLabels() {
            val startSeconds = percentToSeconds(startPercent)
            val endSeconds = percentToSeconds(endPercent)
            leftLabel.text = "♩ ${remix.tempos.secondsToBeats(startSeconds).toInt()} (${secondsToText(startSeconds.toInt())}) "
            rightLabel.text = " ♩ ${remix.tempos.secondsToBeats(endSeconds).roundToInt()} (${secondsToText(endSeconds.roundToInt())})"
            partial = startPercent != 0f || endPercent != 1f
        }
        
        private fun secondsToText(seconds: Int): String {
            val abs = seconds.absoluteValue
            val sec = abs % 60
            return "${if (seconds < 0) "-" else ""}${abs / 60}:${if (sec < 10) "0" else ""}${max(0, sec)}"
        }
        
    }
}