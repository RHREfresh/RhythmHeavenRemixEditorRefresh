package io.github.chrislo27.rhrefresh.desktop

import com.badlogic.gdx.Files
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.HdpiMode
import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException
import io.github.chrislo27.rhrefresh.RHREfresh
import io.github.chrislo27.rhrefresh.RHREfreshApplication
import io.github.chrislo27.toolboks.desktop.ToolboksDesktopLauncher3
import io.github.chrislo27.toolboks.lazysound.LazySound
import io.github.chrislo27.toolboks.logging.Logger
import java.io.File
import java.util.Locale

object DesktopLauncher {
    
    private fun printHelp(jCommander: JCommander) {
        println("${RHREfresh.TITLE} ${RHREfresh.VERSION}\n${RHREfresh.GITHUB}\n\n${StringBuilder().apply { jCommander.usage() }}")
    }
    
    @JvmStatic
    fun main(args: Array<String>) {
        // https://github.com/chrislo27/RhythmHeavenRemixEditor/issues/273
        System.setProperty("jna.nosys", "true")
        
        RHREfresh.launchArguments = args.toList()
        
        val osName: String = System.getProperty("os.name", "???")?.lowercase(Locale.ROOT) ?: "???"
        println("Running on \"$osName\"")
        
		val refreshFolder: String = if (osName.startsWith("linux")) {
			".config/RHREfresh"
		} else if (osName.contains("win")) {
			"AppData/Roaming/RHREfresh/"
		} else if (osName.contains("mac")) {
			"Library/Application Support/RHREfresh/"
		} else {
			".rhrefresh"
		}
        //val refreshExternalFolder: String = System.getProperty("user.home") +"/"+ refreshFolder

        val arguments = Arguments()
        val jcommander = JCommander.newBuilder().acceptUnknownOptions(false).addObject(arguments).build()
        try {
            jcommander.parse(*args)
        } catch (e: ParameterException) {
            println("WARNING: Failed to parse arguments. Check below for details and help documentation. You may have strange parse results from ignoring unknown options.\n")
            e.printStackTrace()
            println("\n\n")
            printHelp(JCommander(Arguments()))
            println("\n\n")
        }


        if (arguments.printHelp) {
            printHelp(jcommander)
            return
        }
        
        val logger = Logger()
        val portable = arguments.portableMode

        if (portable && File("/$refreshFolder/sfx/${RHREfresh.MASTER_DATABASE_BRANCH}/.git").exists()) {
            val target = File("/$refreshFolder/sfx/")
            val source = File("/$refreshFolder/sfx/${RHREfresh.MASTER_DATABASE_BRANCH}")
            source.copyRecursively(target)
            source.deleteRecursively()
        }

        val app = RHREfreshApplication(logger, File(if (portable) "$refreshFolder/logs/" else System.getProperty("user.home") + "/$refreshFolder/logs/"))
        ToolboksDesktopLauncher3(app)
                .editConfig {
                    this.setAutoIconify(true)
                    this.setWindowedMode(app.emulatedSize.first, app.emulatedSize.second)
                    this.setWindowSizeLimits(RHREfresh.MINIMUM_SIZE.first, RHREfresh.MINIMUM_SIZE.second, -1, -1)
                    this.setTitle(app.getTitle())
                    this.setResizable(true)
                    this.useVsync(arguments.fps <= 60)
                    RHREfresh.targetFramerate = arguments.fps.coerceAtLeast(30)
                    this.setInitialBackgroundColor(Color(0f, 0f, 0f, 1f))
                    this.setAudioConfig(100, 4096, 16)
                    this.setHdpiMode(HdpiMode.Logical)
//                    this.setBackBufferConfig(8, 8, 8, 8, 16, 0, 2)
                    if (portable) {
                        this.setPreferencesConfig("$refreshFolder/.prefs/", Files.FileType.Local)
                    } else {
                        logger.info("Setting preference folder to "+System.getProperty("user.home")+"/$refreshFolder/prefs")
                        val newPrefFolder = File(System.getProperty("user.home")+"/$refreshFolder/prefs")
                        if (!newPrefFolder.exists()) {
                            val prefFolder = File(System.getProperty("user.home")+"/.prefs")
                            if (prefFolder.exists() && prefFolder.isDirectory()) {
                                prefFolder.copyRecursively(File(System.getProperty("user.home") + "/$refreshFolder/prefs"))
                                logger.info("Copied older preference folder")
                            }
                        }
                        this.setPreferencesConfig(System.getProperty("user.home") + "/$refreshFolder/prefs/", Files.FileType.Absolute)
                    }
                    
                    RHREfresh.portableMode = portable
                    RHREfresh.skipGitScreen = arguments.skipGit
                    RHREfresh.forceGitFetch = arguments.forceGitFetch
                    RHREfresh.forceGitCheck = arguments.forceGitCheck
                    RHREfresh.verifySfxDb = arguments.verifySfxdb
                    RHREfresh.immediateEvent = when {
                        arguments.eventImmediateAnniversaryLikeNew -> 2
                        arguments.eventImmediateAnniversary -> 1
                        arguments.eventImmediateXmas -> 3
                        else -> 0
                    }
                    RHREfresh.noOnlineCounter = arguments.noOnlineCounter
                    RHREfresh.outputGeneratedDatamodels = arguments.outputGeneratedDatamodels
                    RHREfresh.outputCustomSfx = arguments.outputCustomSfx
                    RHREfresh.showTapalongMarkersByDefault = arguments.showTapalongMarkers
                    RHREfresh.midiRecording = arguments.midiRecording
                    RHREfresh.logMissingLocalizations = arguments.logMissingLocalizations
                    RHREfresh.disableCustomSounds = arguments.disableCustomSounds
                    RHREfresh.lc = arguments.lc
                    RHREfresh.triggerUpdateScreen = arguments.triggerUpdateScreen
                    RHREfresh.triggerFolderChangeScreen = arguments.triggerFolderChangeScreen
                    RHREfresh.remixPath = arguments.remixPath
                    LazySound.loadLazilyWithAssetManager = !arguments.lazySoundsForceLoad

                    logger.info("Opening remix: "+ RHREfresh.remixPath)

                    val sizes: List<Int> = listOf(256, 128, 64, 32, 24, 16)
                    this.setWindowIcon(Files.FileType.Internal, *sizes.map { "images/icon/$it.png" }.toTypedArray())
                }
                .launch()
    }
}
