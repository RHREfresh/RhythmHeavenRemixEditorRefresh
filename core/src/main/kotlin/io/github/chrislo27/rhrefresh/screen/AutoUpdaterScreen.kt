package io.github.chrislo27.rhrefresh.screen

import RHREUpdateCopier
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import io.github.chrislo27.rhrefresh.RHREfresh
import io.github.chrislo27.rhrefresh.RHREfreshApplication
import io.github.chrislo27.rhrefresh.RemixRecovery
import io.github.chrislo27.rhrefresh.analytics.AnalyticsHandler
import io.github.chrislo27.rhrefresh.stage.GenericStage
import io.github.chrislo27.rhrefresh.stage.TrueCheckbox
import io.github.chrislo27.rhrefresh.util.JsonHandler
import io.github.chrislo27.toolboks.Toolboks
import io.github.chrislo27.toolboks.ToolboksScreen
import io.github.chrislo27.toolboks.i18n.Localization
import io.github.chrislo27.toolboks.registry.AssetRegistry
import io.github.chrislo27.toolboks.registry.ScreenRegistry
import io.github.chrislo27.toolboks.ui.Button
import io.github.chrislo27.toolboks.ui.TextLabel
import net.lingala.zip4j.ZipFile
import org.asynchttpclient.AsyncCompletionHandlerBase
import org.asynchttpclient.AsyncHandler
import org.asynchttpclient.HttpResponseBodyPart
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.security.MessageDigest
import java.util.Locale
import java.util.Locale.getDefault
import kotlin.io.path.Path
import kotlin.math.roundToLong
import kotlin.system.exitProcess


class AutoUpdaterScreen(main: RHREfreshApplication)
    : ToolboksScreen<RHREfreshApplication, AutoUpdaterScreen>(main) {
    
    companion object {
        const val UPDATER_FOLDER: String = ".rhreupdater/"
    }
    
    private enum class Progress {
        ERROR, DOWNLOADING, EXTRACTING, READY_TO_COMPLETE
    }

    private enum class Extension {
        JAR, EXE
    }

    private var spinSeconds: Float = 0f
    private val jarFileLocation: File = File(RHREfresh::class.java.protectionDomain.codeSource.location.toURI()) // Will crash if the jar file is not a file
    private val updaterFolder: File = jarFileLocation.resolveSibling(UPDATER_FOLDER)
    private val containingFolder: File = jarFileLocation.parentFile!!.parentFile!!
    private val extension:Extension
    private var progress: Progress = Progress.DOWNLOADING
    private val worker: Thread
    
    override val stage: GenericStage<AutoUpdaterScreen> = GenericStage(main.uiPalette, null, main.defaultCamera)
    private val changelogButton: Button<AutoUpdaterScreen>
    private val completeButton: Button<AutoUpdaterScreen>
    private val tryAgainButton: Button<AutoUpdaterScreen>
    private val autocompleteCheckbox: TrueCheckbox<AutoUpdaterScreen>
    private val label: TextLabel<AutoUpdaterScreen>
    
    init {
        require(jarFileLocation.exists())
        require(jarFileLocation.isFile)
        extension = when (jarFileLocation.extension) {
            "exe" -> Extension.EXE
            "jar" -> Extension.JAR
            else -> throw IllegalArgumentException(Localization["screen.autoUpdater.unknownExtension"])
        }
        val palette = stage.palette
        
        stage.titleLabel.apply {
            this.isLocalizationKey = true
            this.text = "screen.autoUpdater.title"
        }
        stage.titleIcon.apply {
            this.image = TextureRegion(AssetRegistry.get<Texture>("ui_icon_updatesfx"))
        }
        stage.backButton.apply {
            this.visible = false
            this.tooltipTextIsLocalizationKey = true
            this.tooltipText = "screen.autoUpdater.cancel"
        }
        stage.onBackButtonClick = {
            AnalyticsHandler.track("Auto-Update Cancelled", mapOf())
            this.dispose()
            main.screen = ScreenRegistry["editorVersion"]
        }
        
        label = TextLabel(palette, stage.centreStage, stage.centreStage).apply {
            this.isLocalizationKey = false
            this.text = Localization["screen.autoUpdater.progress.preparing"]
            this.textWrapping = false
        }
        stage.centreStage.elements += label
        
        changelogButton = Button(palette, stage.bottomStage, stage.bottomStage).apply {
            this.leftClickAction = { _, _ ->
                Gdx.net.openURI(RHREfresh.GITHUB_RELEASES + "/latest")
            }
            this.addLabel(TextLabel(palette, this, this.stage).apply {
                this.isLocalizationKey = true
                this.textWrapping = false
                this.fontScaleMultiplier = 0.9f
                this.text = "screen.version.viewChangelog.short"
            })
            this.location.set(screenX = 0.85f, screenWidth = 0.15f)
        }
        stage.bottomStage.elements += changelogButton
        completeButton = Button(palette, stage.bottomStage, stage.bottomStage).apply {
            this.addLabel(TextLabel(palette, this, this.stage).apply {
                this.isLocalizationKey = true
                this.textWrapping = false
                this.text = "screen.autoUpdater.complete"
            })
            this.location.set(screenX = 0.175f, screenWidth = 0.65f)
            this.visible = false
        }
        stage.bottomStage.elements += completeButton
        tryAgainButton = Button(palette, stage.bottomStage, stage.bottomStage).apply {
            this.addLabel(TextLabel(palette, this, this.stage).apply {
                this.isLocalizationKey = true
                this.textWrapping = false
                this.text = "screen.autoUpdater.tryAgain"
            })
            this.location.set(screenX = 0.175f, screenWidth = 0.65f)
            this.visible = false
        }
        stage.bottomStage.elements += tryAgainButton
        autocompleteCheckbox = TrueCheckbox(palette, stage.bottomStage, stage.bottomStage).apply {
            this.textLabel.apply {
                this.isLocalizationKey = true
                this.textWrapping = false
                this.fontScaleMultiplier = 0.9f
                this.text = "screen.autoUpdater.noUserInput"
            }
            this.checked = false
            this.location.set(screenX = 0.175f, screenWidth = 0.65f)
        }
        stage.bottomStage.elements += autocompleteCheckbox
        // End of UI code
        
        if (updaterFolder.exists()) {
            updaterFolder.deleteRecursively()
        }
        updaterFolder.mkdir()
        
        worker = Thread(Runnable {
            try {
                /*
                Stages:
                1. Fetch metadata from GitHub API about latest release
                2. Download archive, store in updater folder
                3. Unpack archive in updater folder
                4. Copy all files from update EXCEPT *.jar, *.sh, and *.bat into main folder
                5. Advance to Complete Update stage for user input
                6. Copy jar file atomically and exit forcibly (System.exit())
                 */
                val releaseResponseBody = RHREfreshApplication.httpClient
//                        .prepareGet("https://thealtdoc.fr/rhre/latest.json")
                        .prepareGet("https://api.github.com/repos/TheAlternateDoctor/RhythmHeavenRemixEditorRefresh/releases/latest")
                        .addHeader("Accept", "application/vnd.github.v3+json")
                        .execute().get().responseBody
                val releaseMeta: JsonNode = JsonHandler.OBJECT_MAPPER.readTree(releaseResponseBody)
                val assetNode = (releaseMeta["assets"] as ArrayNode).first {
                    val filename = it["name"].asText()
                    when (extension) {
                        Extension.EXE -> {
                            when (RHREfresh.CURRENT_ARCH) {
                                RHREfresh.ARCH.AMD64 -> filename.startsWith("RHREfresh_") && filename.endsWith("win_x64.zip")
                                RHREfresh.ARCH.AARCH64 -> filename.startsWith("RHREfresh_") && filename.endsWith("win_arm64.zip")
                                else -> false //Won't ever happen if you start with the EXE
                            }
                        }
                        Extension.JAR -> {
                            filename.startsWith("RHREfresh_") && filename.endsWith("multiplatform_jar.zip")
                        }
                    }
                }
                val zipUrl = assetNode["browser_download_url"].asText()
                val filesize = assetNode["size"].asLong(1L).coerceAtLeast(1L)
                val digestMethod = assetNode["digest"].asText().split(":").first()
                val digestHash = assetNode["digest"].asText().split(":").last()

                val zipFileLoc = updaterFolder.resolve("RHRE_archive.zip").apply {
                    createNewFile()
                }
                val fileStream = FileOutputStream(zipFileLoc)
                val download = RHREfreshApplication.httpClient.prepareGet(zipUrl)
                        .setReadTimeout(120_000)
                        .setRequestTimeout(2_000_000_000)
                        .execute(object : AsyncCompletionHandlerBase() {
                            private val speedUpdateRate = 500L
                            private var timeBetweenProgress: Long = System.currentTimeMillis()
                            private var lastSpeed = 0L
                            private var speedAcc = 0L
                            private var bytesSoFar = 0L
                            override fun onContentWriteProgress(amount: Long, current: Long, total: Long): AsyncHandler.State {
                                val time = System.currentTimeMillis() - timeBetweenProgress
                                speedAcc += amount
                                if (time >= speedUpdateRate) {
                                    timeBetweenProgress = System.currentTimeMillis()
                                    lastSpeed = (speedAcc / (time / 1000.0)).roundToLong()
                                    speedAcc = 0L
                                }

                                Gdx.app.postRunnable {
                                    val percent = (current.toDouble() / total).coerceIn(0.0, 1.0)
                                    val barLength = 30
                                    val barPortion = "${"█".repeat((percent * barLength).toInt())}[][GRAY]${"█".repeat(((1.0 - percent) * barLength).toInt())}"
                                    val bar = "[[[WHITE]$barPortion[]]"
                                    label.text = Localization["screen.autoUpdater.progress.downloadingArchive",
                                            (percent * 100).roundToLong(), bar, bytesSoFar / 1024, total / 1024,
                                            if (current >= total || lastSpeed <= 0L) "---" else (lastSpeed / 1024)]
                                }
                                return super.onContentWriteProgress(amount, current, total)
                            }

                            override fun onBodyPartReceived(content: HttpResponseBodyPart): AsyncHandler.State {
                                fileStream.channel.write(content.bodyByteBuffer)
                                val amount = content.length()
                                val total = filesize // FIXME response bytes won't always match filesize in metadata
                                val time = System.currentTimeMillis() - timeBetweenProgress
                                speedAcc += amount
                                if (time >= speedUpdateRate) {
                                    timeBetweenProgress = System.currentTimeMillis()
                                    lastSpeed = (speedAcc / (time / 1000.0)).roundToLong()
                                    speedAcc = 0L
                                }

                                bytesSoFar += amount

                                Gdx.app.postRunnable {
                                    val percent = (bytesSoFar.toDouble() / total).coerceIn(0.0, 1.0)
                                    val barLength = 30
                                    val barPortion = "${"█".repeat((percent * barLength).toInt())}[][DARK_GRAY]${"█".repeat(((1.0 - percent) * barLength).toInt())}"
                                    val bar = "[WHITE]$barPortion[]"
                                    label.text = Localization["screen.autoUpdater.progress.downloadingArchive",
                                            (percent * 100).roundToLong(), bar, bytesSoFar / 1024, total / 1024,
                                            if (bytesSoFar >= total || lastSpeed <= 0L) "---" else (lastSpeed / 1024)]
                                }
                                return AsyncHandler.State.CONTINUE
                            }
                        }).get()
                fileStream.close()

                val md = MessageDigest.getInstance("SHA-256")
                val sha256 = md.digest(zipFileLoc.readBytes()).fold("") { str, it -> str + "%02x".format(it) }
                if (sha256 != digestHash) {
                    throw Exception(Localization["screen.autoUpdater.downloadError.nonMatchingChecksum"])
                }

                Gdx.app.postRunnable {
                    progress = Progress.EXTRACTING
                    label.text = Localization["screen.autoUpdater.progress.extractingArchive"]
                }
                val extractFolder: File = updaterFolder.resolve("extract/")
                val zipFile: ZipFile = ZipFile(zipFileLoc)
                extractFolder.mkdir()
                zipFile.extractAll(extractFolder.canonicalPath)
                zipFileLoc.deleteOnExit()
                val mainFolder = extractFolder.resolve("Rhythm Heaven Remix Editor Refresh/")
                if (!mainFolder.exists()) error(Localization["screen.autoUpdater.downloadError.inexistant"])
                if (!mainFolder.isDirectory) error(Localization["screen.autoUpdater.downloadError.notADirectory"])
                // Copy over allowed files
                val fileList = mainFolder.listFiles()!!.toList()
                val newJarFile = when (extension) {
                    Extension.EXE -> fileList.firstOrNull { it.name == "RHREfresh.exe" } ?: error(Localization["screen.autoUpdater.downloadError.missingExe"])
                        Extension.JAR -> fileList.firstOrNull { it.isDirectory && it.name == "bin" }?.listFiles()?.firstOrNull { it.name == "RHREfresh.jar" } ?: error(Localization["screen.autoUpdater.downloadError.missingJar"])
                }
                fileList.filter {
                    (it.isDirectory && it.name in listOf("oss_licenses", "bin")) || (it.isFile && it.extension == "txt")
                }.forEach { f ->
                    if (f.isFile) {
                        f.copyTo(containingFolder.resolve(f.name), true)
                    } else if (f.isDirectory) {
                        f.copyRecursively(containingFolder.resolve(f.name + "/"), true)
                    }
                }
                progress = Progress.READY_TO_COMPLETE
                if (autocompleteCheckbox.checked) {
                    // Continue
                    Gdx.app.postRunnable {
                        when (extension) {
                            Extension.JAR -> completeJarCopy(newJarFile)
                            Extension.EXE -> completeExeCopy(newJarFile)
                        }
                    }
                } else {
                    Gdx.app.postRunnable {
                        autocompleteCheckbox.visible = false
                        completeButton.visible = true
                        label.text = Localization["screen.autoUpdater.progress.readyToComplete"]
                        completeButton.leftClickAction = { _, _ ->
                            completeButton.visible = false
                            when (extension) {
                                Extension.JAR -> completeJarCopy(newJarFile)
                                Extension.EXE -> completeExeCopy(newJarFile)
                            }
                        }
                    }
                }
            } catch (ie: InterruptedException) {
                ie.printStackTrace()
                AnalyticsHandler.track("Auto-Update Error", mapOf("state" to "downloading",
                                                                  "throwable" to ie::class.java.canonicalName,
                                                                  "stackTrace" to StringWriter().apply {
                                                                      val pw = PrintWriter(this)
                                                                      ie.printStackTrace(pw)
                                                                      pw.flush()
                                                                  }.toString()))
                cleanupAfterFail()
            } catch (e: Exception) {
                e.printStackTrace()
                AnalyticsHandler.track("Auto-Update Error", mapOf("state" to "downloading",
                                                                  "throwable" to e::class.java.canonicalName,
                                                                  "stackTrace" to StringWriter().apply {
                                                                      val pw = PrintWriter(this)
                                                                      e.printStackTrace(pw)
                                                                      pw.flush()
                                                                  }.toString()))
                Gdx.app.postRunnable {
                    progress = Progress.ERROR
                    label.textWrapping = true
                    label.text = Localization["screen.autoUpdater.error", "[LIGHT_GRAY]${e::class.java.canonicalName}\n${e.localizedMessage}[]"]
                    stage.backButton.visible = true
                    completeButton.visible = false
                    autocompleteCheckbox.visible = false
                    tryAgainButton.visible = true
                    tryAgainButton.leftClickAction = { _, _ ->
                        tryAgainButton.visible = false
                        Gdx.app.postRunnable { 
                            main.screen = AutoUpdaterScreen(main)
                        }
                    }
                }
                cleanupAfterFail()
            }
        }, "Auto-Updater Worker").apply {
            this.isDaemon = false
        }
        worker.start()
    }
    
    private fun completeJarCopy(newJarFile: File) {
        try {
            RemixRecovery.removeSelfFromShutdownHooks()
            val updaterFolderPath = updaterFolder.canonicalPath
            val updaterClassName = RHREUpdateCopier::class.java.simpleName
            ZipFile(jarFileLocation).extractFile("$updaterClassName.class", updaterFolderPath, "$updaterClassName.class")
            ProcessBuilder("java", "-cp", updaterFolderPath, updaterClassName, newJarFile.canonicalPath, jarFileLocation.canonicalPath)
                    .start()
            Toolboks.LOGGER.info("Launched Updater class ($updaterClassName), exiting now.")
            exitProcess(0)
        } catch (e: Exception) {
            e.printStackTrace()
            AnalyticsHandler.track("Auto-Update Error", mapOf("state" to "jar copying",
                                                              "throwable" to e::class.java.canonicalName,
                                                              "stackTrace" to StringWriter().apply {
                                                                  val pw = PrintWriter(this)
                                                                  e.printStackTrace(pw)
                                                                  pw.flush()
                                                              }.toString()))
            Gdx.app.postRunnable {
                progress = Progress.ERROR
                label.textWrapping = true
                label.text = Localization["screen.autoUpdater.error", "[LIGHT_GRAY]${e::class.java.canonicalName}\n${e.localizedMessage}[]"]
                stage.backButton.visible = true
                completeButton.visible = false
                autocompleteCheckbox.visible = false
            }
            cleanupAfterFail()
        }
    }

    private fun completeExeCopy(newJarFile: File) {
        //First, we download the utility
        try {
            /*
            Stages:
            1. Fetch metadata from GitHub API about latest release
            2. Download archive, store in updater folder
            5. Advance to Complete Update stage for user input
            6. Copy jar file atomically and exit forcibly (System.exit())
             */
            val releaseResponseBody = RHREfreshApplication.httpClient
                .prepareGet("https://git.thealtdoc.fr/api/v1/repos/thatzeogal/RHREUpdateUtility/releases/latest")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .execute().get().responseBody
            val releaseMeta: JsonNode = JsonHandler.OBJECT_MAPPER.readTree(releaseResponseBody)
            val assetNode = (releaseMeta["assets"] as ArrayNode).first {
                val filename = it["name"].asText()
                when (RHREfresh.CURRENT_ARCH) {
                    RHREfresh.ARCH.AMD64 -> filename.startsWith("RHREUpdateUtility") && filename.endsWith("win_x64.zip")
                    RHREfresh.ARCH.AARCH64 -> filename.startsWith("RHREUpdateUtility") && filename.endsWith("win_arm64.zip")
                    else -> false //Won't ever happen since it's the exe
                }
            }
            val zipUrl = assetNode["browser_download_url"].asText()
            val filesize = assetNode["size"].asLong(1L).coerceAtLeast(1L)

            val zipFileLoc = updaterFolder.resolve("RHREUpdateUtility.zip").apply {
                createNewFile()
            }
            val fileStream = FileOutputStream(zipFileLoc)
            val download = RHREfreshApplication.httpClient.prepareGet(zipUrl)
                .setReadTimeout(120_000)
                .setRequestTimeout(2_000_000_000)
                .execute(object : AsyncCompletionHandlerBase() {
                    private val speedUpdateRate = 500L
                    private var timeBetweenProgress: Long = System.currentTimeMillis()
                    private var lastSpeed = 0L
                    private var speedAcc = 0L
                    private var bytesSoFar = 0L
                    override fun onContentWriteProgress(amount: Long, current: Long, total: Long): AsyncHandler.State {
                        val time = System.currentTimeMillis() - timeBetweenProgress
                        speedAcc += amount
                        if (time >= speedUpdateRate) {
                            timeBetweenProgress = System.currentTimeMillis()
                            lastSpeed = (speedAcc / (time / 1000.0)).roundToLong()
                            speedAcc = 0L
                        }

                        Gdx.app.postRunnable {
                            val percent = (current.toDouble() / total).coerceIn(0.0, 1.0)
                            val barLength = 30
                            val barPortion = "${"█".repeat((percent * barLength).toInt())}[][GRAY]${"█".repeat(((1.0 - percent) * barLength).toInt())}"
                            val bar = "[[[WHITE]$barPortion[]]"
                            label.text = Localization["screen.autoUpdater.progress.downloadingUpdateUtility",
                                (percent * 100).roundToLong(), bar, bytesSoFar / 1024, total / 1024,
                                if (current >= total || lastSpeed <= 0L) "---" else (lastSpeed / 1024)]
                        }
                        return super.onContentWriteProgress(amount, current, total)
                    }

                    override fun onBodyPartReceived(content: HttpResponseBodyPart): AsyncHandler.State {
                        fileStream.channel.write(content.bodyByteBuffer)
                        val amount = content.length()
                        val total = filesize // FIXME response bytes won't always match filesize in metadata
                        val time = System.currentTimeMillis() - timeBetweenProgress
                        speedAcc += amount
                        if (time >= speedUpdateRate) {
                            timeBetweenProgress = System.currentTimeMillis()
                            lastSpeed = (speedAcc / (time / 1000.0)).roundToLong()
                            speedAcc = 0L
                        }

                        bytesSoFar += amount

                        Gdx.app.postRunnable {
                            val percent = (bytesSoFar.toDouble() / total).coerceIn(0.0, 1.0)
                            val barLength = 30
                            val barPortion = "${"█".repeat((percent * barLength).toInt())}[][DARK_GRAY]${"█".repeat(((1.0 - percent) * barLength).toInt())}"
                            val bar = "[WHITE]$barPortion[]"
                            label.text = Localization["screen.autoUpdater.progress.downloadingUpdateUtility",
                                (percent * 100).roundToLong(), bar, bytesSoFar / 1024, total / 1024,
                                if (bytesSoFar >= total || lastSpeed <= 0L) "---" else (lastSpeed / 1024)]
                        }
                        return AsyncHandler.State.CONTINUE
                    }
                }).get()
            fileStream.close()

            //Then, we use the utility
            Gdx.app.postRunnable {
                progress = Progress.EXTRACTING
                label.text = Localization["screen.autoUpdater.progress.extractingUpdateUtility"]
            }
            val extractFolder: File = updaterFolder.resolve("utility/")
            val zipFile = ZipFile(zipFileLoc)
            extractFolder.mkdir()
            zipFile.extractAll(extractFolder.canonicalPath)
            zipFileLoc.deleteOnExit()
            val executable = extractFolder.resolve("RHREUpdateUtility.exe")
            if (!executable.exists()) error(Localization["screen.autoUpdater.downloadError.updateUtility.missingExe",updaterFolder])
            RemixRecovery.removeSelfFromShutdownHooks()
            val updaterFolderPath = updaterFolder.canonicalPath
            Toolboks.LOGGER.info("Calling process `${Path(updaterFolderPath, "utility", "RHREUpdateUtility.exe").toAbsolutePath()}`, moving `${Path(updaterFolderPath, "Rhythm Heaven Remix Editor Refresh").toAbsolutePath()}` to `${jarFileLocation.parentFile.canonicalPath}`")
            ProcessBuilder(Path(updaterFolderPath, "utility", "RHREUpdateUtility.exe").toAbsolutePath().toString(), Path(updaterFolderPath, "extract", "Rhythm Heaven Remix Editor Refresh").toAbsolutePath().toString(), jarFileLocation.parentFile.resolve("test").canonicalPath)
                .redirectOutput(ProcessBuilder.Redirect.to(extractFolder.resolve("utility.log")))
                .redirectError(ProcessBuilder.Redirect.to(extractFolder.resolve("utility.log")))
                .start()
            Toolboks.LOGGER.info("Launched Update Utility EXE, exiting now.")
            exitProcess(0)

        } catch (ie: InterruptedException) {
            ie.printStackTrace()
            AnalyticsHandler.track("Auto-Update Error", mapOf("state" to "downloading",
                "throwable" to ie::class.java.canonicalName,
                "stackTrace" to StringWriter().apply {
                    val pw = PrintWriter(this)
                    ie.printStackTrace(pw)
                    pw.flush()
                }.toString()))
            cleanupUtilityAfterFail()
        } catch (e: Exception) {
            e.printStackTrace()
            AnalyticsHandler.track("Auto-Update Error", mapOf("state" to "downloading",
                "throwable" to e::class.java.canonicalName,
                "stackTrace" to StringWriter().apply {
                    val pw = PrintWriter(this)
                    e.printStackTrace(pw)
                    pw.flush()
                }.toString()))
            Gdx.app.postRunnable {
                progress = Progress.ERROR
                label.textWrapping = true
                label.text = Localization["screen.autoUpdater.error", "[LIGHT_GRAY]${e::class.java.canonicalName}\n${e.localizedMessage}[]"]
                stage.backButton.visible = true
                completeButton.visible = false
                autocompleteCheckbox.visible = false
                tryAgainButton.visible = true
                tryAgainButton.leftClickAction = { _, _ ->
                    tryAgainButton.visible = false
                    Gdx.app.postRunnable {
                        main.screen = AutoUpdaterScreen(main)
                    }
                }
            }
            cleanupUtilityAfterFail()
        }
    }
    
    private fun cleanupAfterFail() {
        // Delete any temporary files created
        updaterFolder.deleteRecursively()
    }
    private fun cleanupUtilityAfterFail() {
        // Delete any temporary files created
        updaterFolder.resolve("utility").deleteRecursively()
    }
    
    override fun renderUpdate() {
        super.renderUpdate()
        
        spinSeconds += Gdx.graphics.deltaTime * 3f
        if (spinSeconds > 600f) spinSeconds %= 600f
        stage.titleIcon.rotation = (MathUtils.sin(MathUtils.sin(spinSeconds * 2)) + spinSeconds * 0.9f) * -90f
    }
    
    override fun tickUpdate() {
    }
    
    override fun dispose() {
        // Cancel a download if it is happening
        worker.interrupt()
        cleanupAfterFail()
    }
    
}
