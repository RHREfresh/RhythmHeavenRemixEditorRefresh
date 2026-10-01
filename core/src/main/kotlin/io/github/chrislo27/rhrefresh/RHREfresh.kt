package io.github.chrislo27.rhrefresh

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.files.FileHandle
import io.github.chrislo27.rhrefresh.util.ExportOptions
import io.github.chrislo27.toolboks.version.Version
import java.time.LocalDate
import java.time.Month
import java.util.Locale


object RHREfresh {

    enum class OS{
        WINDOWS,
        LINUX,
        MACOS,
        UNKNOWN
    }
    enum class ARCH{
        AMD64,
        AARCH64,
        UNKNOWN
    }

    const val TITLE = "Rhythm Heaven Remix Editor Refresh"
    val VERSION: Version = Version(4, 0, 0, "")
    val EXPERIMENTAL: Boolean = VERSION.suffix.matches("DEVELOPMENT|SNAPSHOT.*|RC\\d+".toRegex())
    val enableEarlyAccessMessage: Boolean = EXPERIMENTAL && VERSION.suffix != "DEVELOPMENT"
    const val WIDTH = 1280
    const val HEIGHT = 720
    val DEFAULT_SIZE = WIDTH to HEIGHT
    val MINIMUM_SIZE: Pair<Int, Int> = 640 to 360
    val CURRENT_OS =
        if (System.getProperty("os.name", "???")?.lowercase(Locale.ROOT)!!.contains("win")) {
            OS.WINDOWS
        } else if (System.getProperty("os.name", "???")?.lowercase(Locale.ROOT)!!.contains("mac")) {
            OS.MACOS
        } else if (System.getProperty("os.name", "???")?.lowercase(Locale.ROOT)!!.startsWith("linux")) {
            OS.LINUX
        } else {
            OS.UNKNOWN
        }
    val CURRENT_ARCH =
        if (System.getProperty("os.arch") == "amd64") {
            ARCH.AMD64
        } else if (System.getProperty("os.arch") == "aarch64") {
            ARCH.AARCH64
        } else {
            ARCH.UNKNOWN
        }
    val RHREFRESH_FOLDER: FileHandle by lazy {
        (
                if (portableMode) {
                    Gdx.files.local(".rhrefresh/")
                } else {
                    if (CURRENT_OS == OS.LINUX) {
                        Gdx.files.external(".config/RHREfresh/")
                    } else if (CURRENT_OS == OS.WINDOWS) {
                        Gdx.files.external("AppData/Roaming/RHREfresh/")
                    } else if (CURRENT_OS == OS.MACOS) {
                        Gdx.files.external("Library/Application Support/RHREfresh/")
                    } else {
                        Gdx.files.external(".rhrefresh/")
                    }
                }).apply(FileHandle::mkdirs)
    }
    val FFMPEG_FOLDER: FileHandle by lazy { RHREFRESH_FOLDER.child("ffmpeg/") }

    val SUPPORTED_DECODING_SOUND_TYPES = listOf("ogg", "mp3", "wav")
    val tmpMusic: FileHandle by lazy {
        RHREFRESH_FOLDER.child("tmpMusic/").apply {
            mkdirs()
        }
    }
    const val REMIX_FILE_EXTENSION = "rhref"

    const val GITHUB: String = "https://github.com/RHREfresh/RhythmHeavenRemixEditorRefresh"
    const val GITHUB_RELEASES = "$GITHUB/releases"
    const val GITHUB_SHORTLINK: String = "https://rhre.thealtdoc.fr/github"
    const val DATABASE_URL: String = "https://github.com/RHREfresh/RHRE-database.git"
    const val DONATION_URL: String = "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=VA45DPLCC4958"
    val DEV_DATABASE_BRANCH: String = "dev"
    val MASTER_DATABASE_BRANCH: String = "master"
    var DATABASE_BRANCH: String = MASTER_DATABASE_BRANCH
    var DATABASE_CURRENT_COMMIT: String = ""
    val DATABASE_CURRENT_VERSION: String = "https://raw.githubusercontent.com/TheAlternateDoctor/RHRE-database/$DATABASE_BRANCH/current.json"
    const val DATABASE_RELEASES = "https://github.com/RHREfresh/RHRE-database/releases"
    const val RELEASE_API_URL = "https://api.github.com/repos/TheAlternateDoctor/RhythmHeavenRemixEditorRefresh/releases/latest"
    const val OUT_OF_MEMORY_DOC_LINK: String = "https://docs.rhre.dev/Out-of-memory-on-music/"
    const val DOCS_URL: String = "https://rhre.readthedocs.io/en/latest/"

    lateinit var PREFERENCES: Preferences

    val RHRE_ANNIVERSARY: LocalDate = LocalDate.of(2016, Month.MAY, 29)
    val RHREFRESH_ANNIVERSARY: LocalDate = LocalDate.of(2026, Month.OCTOBER, 1)
    private val RHRE3_ANNIVERSARY: LocalDate = LocalDate.of(2017, Month.AUGUST, 30)
    private val RHRE2_ANNIVERSARY: LocalDate = LocalDate.of(2016, Month.DECEMBER, 6)

    var targetFramerate: Int = 60
    var portableMode: Boolean = false
    var skipGitScreen: Boolean = false
    var forceGitFetch: Boolean = false
    var forceGitCheck: Boolean = false
    var verifySfxDb: Boolean = false
    var immediateEvent: Int = 0
    var noOnlineCounter: Boolean = false
    var outputGeneratedDatamodels: Boolean = false
    var outputCustomSfx: Boolean = false
    var showTapalongMarkersByDefault: Boolean = false
    var exportOptions: ExportOptions = ExportOptions.DEFAULT
    var midiRecording: Boolean = false
    var logMissingLocalizations: Boolean = false
    var disableCustomSounds: Boolean = false
    var lc: String? = null
    var triggerUpdateScreen: Boolean = false
    var remixPath: String = ""
    var triggerFolderChangeScreen: Boolean = false

    lateinit var launchArguments: List<String>

}