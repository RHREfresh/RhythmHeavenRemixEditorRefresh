package io.github.chrislo27.rhrefresh.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import io.github.chrislo27.rhrefresh.PreferenceKeys
import io.github.chrislo27.rhrefresh.RHREfresh
import io.github.chrislo27.rhrefresh.RHREfreshApplication
import io.github.chrislo27.rhrefresh.stage.GenericStage
import io.github.chrislo27.toolboks.ToolboksScreen
import io.github.chrislo27.toolboks.i18n.Localization
import io.github.chrislo27.toolboks.registry.AssetRegistry
import io.github.chrislo27.toolboks.registry.ScreenRegistry
import io.github.chrislo27.toolboks.ui.Button
import io.github.chrislo27.toolboks.ui.ImageLabel
import io.github.chrislo27.toolboks.ui.TextLabel
import java.awt.Desktop
import java.io.File
import kotlin.collections.plusAssign


class ImportOlderRHREScreen(main: RHREfreshApplication) : ToolboksScreen<RHREfreshApplication, ImportOlderRHREScreen>(main) {

    override val stage: GenericStage<ImportOlderRHREScreen> = GenericStage(main.uiPalette, null, main.defaultCamera)

    private val folderButton: Button<ImportOlderRHREScreen>
    private val iUnderstandButton: Button<ImportOlderRHREScreen>
    private val importLoadTextLabel: TextLabel<ImportOlderRHREScreen>
    private var startedImporting = false

    init {
        stage.updatePositions()
        stage.titleIcon.apply {
            this.image = TextureRegion(AssetRegistry.get<Texture>("ui_icon_warn"))
            this.renderType = ImageLabel.ImageRendering.ASPECT_RATIO
        }
        stage.titleLabel.apply {
            this.isLocalizationKey = true
            this.text = "screen.folderChangeWarning.title"
        }
        stage.backButton.apply {
            this.enabled = false
            this.visible = false
        }


        val palette = main.uiPalette

        importLoadTextLabel= TextLabel(palette, stage.centreStage, stage.centreStage).apply {
            this.isLocalizationKey = false
            this.textWrapping = false
            this.text = Localization["screen.folderChangeWarning.placeholder"]
            this.visible = true
        }
        stage.centreStage.elements += importLoadTextLabel

        stage.centreStage.elements += TextLabel(palette, stage.centreStage, stage.centreStage).apply {
            this.isLocalizationKey = false
            this.textWrapping = false
            this.text = when (RHREfresh.CURRENT_OS) {
                RHREfresh.OS.WINDOWS -> Localization["screen.folderChangeWarning.content", System.getProperty("user.home", "???")+"\\"+RHREfresh.RHREFRESH_FOLDER.path().replace('/','\\')]
                RHREfresh.OS.LINUX,RHREfresh.OS.MACOS -> Localization["screen.folderChangeWarning.content", "~/"+RHREfresh.RHREFRESH_FOLDER]
                RHREfresh.OS.UNKNOWN -> Localization["screen.folderChangeWarning.content", RHREfresh.RHREFRESH_FOLDER]
            }

            this.textColor = Color.LIGHT_GRAY
            this.visible = false

        }
        iUnderstandButton = Button(palette.copy(highlightedBackColor = Color(0f, 1f, 0f, 0.5f), clickedBackColor = Color(0.5f, 1f, 0.5f, 0.5f)), stage.bottomStage, stage.bottomStage).apply {
            this.location.set(screenX = 0.2f, screenWidth = 0.6f)
            addLabel(TextLabel(palette, this, this.stage).apply {
                this.text = "screen.folderChangeWarning.button"
                this.isLocalizationKey = true
            })
            this.leftClickAction = { _, _ ->
                main.preferences.putBoolean(PreferenceKeys.PASSED_FOLDER_CHANGE_WARNING, true)
                main.preferences.flush()
                val screen = ScreenRegistry[if (RHREfresh.skipGitScreen) "sfxdbLoad" else "databaseUpdate"]
                main.screen = screen
            }
            this.visible = false
        }
        stage.bottomStage.elements += iUnderstandButton

        folderButton = Button(palette, stage.bottomStage, stage.bottomStage).apply {
            this.addLabel(ImageLabel(palette, this, this.stage).apply {
                this.image = TextureRegion(AssetRegistry.get<Texture>("ui_icon_folder"))
            })
            this.tooltipText = "screen.folderChangeWarning.openFoldersButton"
            this.tooltipTextIsLocalizationKey = true
            this.leftClickAction = { _, _ ->
                val newCustomSounds = RHREfresh.RHREFRESH_FOLDER.child("customSounds/").file()
                val oldCustomSounds = Gdx.files.external(".rhre3/customSounds").file()
                if (newCustomSounds != null) {
                    Desktop.getDesktop().open(newCustomSounds)
                }
                Desktop.getDesktop().open(oldCustomSounds)
            }

            this.location.set(this@ImportOlderRHREScreen.stage.backButton.location)
            this.location.set(screenX = 1f - this.location.screenWidth)
            this.visible = false
        }
        if (RHREfresh.CURRENT_OS != RHREfresh.OS.MACOS) {
            stage.bottomStage.elements += folderButton
        }

        stage.updatePositions()
        val thread = Thread {

                // Copy the legacy folder over, so that the two can coexist
                val legacyFolder = File(System.getProperty("user.home") + "/.rhre3")
                val legacyAdvFolder = File(System.getProperty("user.home") + "/.rhre3adv")
                val legacyRefreshFolder = File(System.getProperty("user.home") + "/.rhrefresh")
                val newFolder = RHREfresh.RHREFRESH_FOLDER.file()
                if (legacyRefreshFolder.exists()) {
                    updateState(1, legacyRefreshFolder.path)
                    legacyRefreshFolder.copyRecursively(newFolder, onError = {file, ioException ->
                        if (!(ioException is FileAlreadyExistsException)) {
                            OnErrorAction.TERMINATE
                        } else {
                            OnErrorAction.SKIP
                        }
                    })
                } else if (legacyAdvFolder.exists()) {
                    updateState(1, legacyAdvFolder.path)
                    legacyAdvFolder.copyRecursively(newFolder, onError = {file, ioException ->
                        if (!(ioException is FileAlreadyExistsException)) {
                            OnErrorAction.TERMINATE
                        } else {
                            OnErrorAction.SKIP
                        }
                    })
                } else if (legacyFolder.exists()) {
                    updateState(1, legacyFolder.path)
                    legacyFolder.copyRecursively(newFolder, onError = {file, ioException ->
                        if (!(ioException is FileAlreadyExistsException)) {
                            OnErrorAction.TERMINATE
                        } else {
                            OnErrorAction.SKIP
                        }
                    })
                    File(newFolder,"customSounds").deleteRecursively()
                }
                // Transfer the preferences
                if (!File(newFolder,"prefs/RHREFRESH").exists() && File(newFolder,"prefs/RHRE3").exists()) {
                    updateState(2)
                    val prefFile = File(newFolder,"prefs/RHRE3")
                    val prefFileRecovery = File(newFolder,"prefs/RHRE3-recovery")
                    prefFile.copyTo(File(newFolder,"prefs/RHREFRESH"))
                    prefFileRecovery.copyTo(File(newFolder,"prefs/RHREFRESH-recovery"))
                    prefFile.delete()
                    prefFileRecovery.delete()
                }
                //Moves the SFXDB to its rightful place
                if (File(newFolder,"sfx/${RHREfresh.MASTER_DATABASE_BRANCH}/.git").exists()) {
                    updateState(3)
                    val target = File(newFolder,"sfx/")
                    val source = File(newFolder,"sfx/${RHREfresh.MASTER_DATABASE_BRANCH}")
                    source.copyRecursively(target, overwrite = true)
                    source.deleteRecursively()
                }
                updateState(4)
            }
        thread.start()
    }

    fun updateState(state: Int, extra: String = "") {
        when (state) {
            1 -> importLoadTextLabel.text = Localization["screen.folderChangeWarning.importingFolder", extra]
            2 -> importLoadTextLabel.text = Localization["screen.folderChangeWarning.importingPrefs"]
            3 -> importLoadTextLabel.text = Localization["screen.folderChangeWarning.movingSFXDB"]
            else -> {
                for(element in stage.centreStage.elements) {
                    if (element != importLoadTextLabel) {
                        element.visible = true
                    }
                }
                iUnderstandButton.visible = true
                folderButton.visible = true
                importLoadTextLabel.visible = false
            }
        }
    }

    override fun tickUpdate() {
    }

    override fun dispose() {
    }
}
