package io.github.chrislo27.rhrefresh.soundsystem

import com.badlogic.gdx.files.FileHandle
import io.github.chrislo27.rhrefresh.PreferenceKeys
import io.github.chrislo27.rhrefresh.RHREfresh
import io.github.chrislo27.toolboks.Toolboks
import ws.schild.jave.Encoder
import ws.schild.jave.MultimediaObject
import ws.schild.jave.process.ProcessLocator
import ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator
import ws.schild.jave.utils.RBufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import kotlin.math.pow


/**
 * A simple wrapper around the [FFmpeg](https://ffmpeg.org/) executables.
 */
object FFmpeg {

    enum class ARCH_OS(val supported: Boolean, val executableName: String) {
        UNSUPPORTED(false, "unsupported"),
        WINDOWS_X64(true, "ffmpeg_win_x64.exe"),
        MACOS_ARM64(true, "ffmpeg_macOS_arm64"),
        LINUX_X64(true, "ffmpeg_linux_x64");

        companion object {
            val ALL_VALUES: List<ARCH_OS> = values().toList()
            val SUPPORTED: List<ARCH_OS> = ALL_VALUES - UNSUPPORTED
        }
    }

    val currentARCH_OS: ARCH_OS = try {
        val osName: String = System.getProperty("os.name", "???")?.lowercase(Locale.ROOT) ?: "???"
        val archName: String = System.getProperty("os.arch", "???")?.lowercase(Locale.ROOT) ?: "???"
        when {
            "win" in osName && archName == "amd64" -> ARCH_OS.WINDOWS_X64
            "mac" in osName && archName == "aarch64" -> ARCH_OS.MACOS_ARM64
            osName.startsWith("linux") && archName == "amd64" -> ARCH_OS.LINUX_X64
            else -> ARCH_OS.UNSUPPORTED
        }
    } catch (e: Exception) {
        e.printStackTrace()
        ARCH_OS.UNSUPPORTED
    }

    // We assume it is supported if there's a custom FFMPEG setup
    val isSupported: Boolean get() {
        val ffmpegLocation = RHREfresh.PREFERENCES.getString(PreferenceKeys.SETTINGS_FFMPEG_LOCATION, "")
        if(ffmpegLocation.isNotEmpty()){
            return true
        }
        return !forceUnsupported && currentARCH_OS.supported
    }

    var forceUnsupported = false

    // Return the encoder built for the platform or the one selected by the user, otherwise uhhhhh
    fun createEncoder(): Encoder {
        return Encoder(createProcessLocator())
    }
    
    // Return the ProcessLocator built for the platform or the one selected by the user, otherwise uhhhhh
    fun createProcessLocator(): ProcessLocator {
        val ffmpegLocation = RHREfresh.PREFERENCES.getString(PreferenceKeys.SETTINGS_FFMPEG_LOCATION, "")
        Toolboks.LOGGER.info("FFMPEG loading: [${ffmpegLocation.isNotEmpty() && File(ffmpegLocation).exists()}] && [${isSupported}]")
        return if (ffmpegLocation.isNotEmpty() && File(ffmpegLocation).exists()) {
            ProcessLocator { ffmpegLocation }
        } else {
            if (isSupported) {
                ProcessLocator { RHREfresh.FFMPEG_FOLDER.child(currentARCH_OS.executableName).file().absolutePath }
            } else {
                DefaultFFMPEGLocator()
            }
        }
    }

    fun createMultimediaObject(file: File): MultimediaObject {
        return if(isSupported) {
            MultimediaObject(file, createProcessLocator())
        } else {
            MultimediaObject(file)
        }
    }

    /**
     * Returns a WAV output stream with SoundStretch having applied the result.
     * Throws an error if the change parameters are not in bounds.
     * @param executableDir The File object pointing to the directory of the executables
     * @param input A WAV file
     * @param output The file to which to output the modified WAV data
     * @param tempoPercent Changes the sound tempo by this amount of percents. See [TEMPO_CHANGE_RANGE]
     * @param pitchSemitones Changes the sound pitch by this amount of semitones. See [PITCH_CHANGE_RANGE]
     * @param ratePercent Changes the sound rate by this amount of percents. See [RATE_CHANGE_RANGE]
     * @param quick Enables the -quick parameter. Gains speed but will probably lose quality.
     */
    fun processStreams(input: File, output: File, tempoPercent: Float, pitchSemitones: Float) {

        val ffmpegExecutor = createProcessLocator().createExecutor()
        ffmpegExecutor.addArgument("-y")
        ffmpegExecutor.addArgument("-hide_banner")
        ffmpegExecutor.addArgument("-i")
        ffmpegExecutor.addArgument(input.absolutePath)

        val encoder = createEncoder()
        var filterChain = ""
        if (tempoPercent>1f) {
            filterChain += "atempo=$tempoPercent"
        } else if (tempoPercent<1f) {
            val tempoMultiplier = tempoPercent.toDouble().pow(0.2)
            for(i in 1..5) {
                filterChain += "atempo=$tempoMultiplier,"
            }
        }
        if (pitchSemitones!=0f) {
            if (!filterChain.endsWith(",")) filterChain+=","
            filterChain += "rubberband=pitch="+(2.0.pow(pitchSemitones.div(12).toDouble()))
        }
        if (filterChain.isNotEmpty()) {
            ffmpegExecutor.addArgument("-af")
            ffmpegExecutor.addArgument(filterChain)
        }
        ffmpegExecutor.addArgument(output.absolutePath)
        Toolboks.LOGGER.info("${createProcessLocator().executablePath} ran for file ${input.path} with arguments `$filterChain`")
        try {
            ffmpegExecutor.execute()
            val reader =
                RBufferedReader(InputStreamReader(ffmpegExecutor.errorStream))
            var line: String?
            while (((reader.readLine().also { line = it } )) != null) {
                Toolboks.LOGGER.info(line!!)
            }
            if (ffmpegExecutor.getProcessExitCode() !== 0) {
                // it failed, and the lines above say why
            }
        } finally {
            ffmpegExecutor.destroy()
        }
    }

    fun testFFmpeg(){
        val ffmpegExecutor = createProcessLocator().createExecutor()
        ffmpegExecutor.addArgument("-version")
        try {
            ffmpegExecutor.execute()
            val reader =
                RBufferedReader(InputStreamReader(ffmpegExecutor.errorStream))
            var line: String?
            while (((reader.readLine().also { line = it } )) != null) {
                Toolboks.LOGGER.info(line!!)
            }
            if (ffmpegExecutor.getProcessExitCode() !== 0) {
                Toolboks.LOGGER.info("Bundled FFmpeg sadly unsupported")
                forceUnsupported = true
            }
        } catch (e: IOException){
            forceUnsupported = true
        }
        finally {
            ffmpegExecutor.destroy()
        }
    }
    /**
     * "Detects" the start of the song using silencedetect. Not entirely accurate, but close enough.
     */
    fun detectMusicStart(handle: FileHandle): Float{
        val ffmpegExecutor = createProcessLocator().createExecutor()
        ffmpegExecutor.addArgument("-y")
        ffmpegExecutor.addArgument("-hide_banner")
        ffmpegExecutor.addArgument("-i")
        ffmpegExecutor.addArgument(handle.file().absolutePath)
        ffmpegExecutor.addArgument("-vn")
        ffmpegExecutor.addArgument("-af")
        ffmpegExecutor.addArgument("silencedetect=n=0.0005:d=1ms")
        ffmpegExecutor.addArgument("-f")
        ffmpegExecutor.addArgument("null")
        ffmpegExecutor.addArgument("-")

        var musicStart = 0f
        var silenceStart = -1f
        var silenceEnd = -1f
        var fileFormat = ""

        try {
            ffmpegExecutor.execute()
            val reader =
                RBufferedReader(InputStreamReader(ffmpegExecutor.errorStream))
            var line: String?
            while (((reader.readLine().also { line = it } )) != null) {
                // Detecting the format first!
                if(line!!.startsWith("Input #0,")){
                    val formatEnd = line.substring(10).indexOf(',')
                    fileFormat = line.substring(10, 10+formatEnd)
                }
                // Detecting the start of the song
                if(line.trimStart().startsWith("Duration") && line.contains("start:")){
                    val startStart = line.indexOf("start:")
                    val startEnd = line.indexOf(',', startStart)
                    val startString = line.substring(startStart+7, startEnd)
                    musicStart = line.substring(startStart+7, startEnd).toFloat()
                    Toolboks.LOGGER.info(line)
                }
                // Getting the first silence start
                if(line.contains("silencedetect") && line.contains("silence_start") && silenceStart == -1f){
                    val silenceStartStart = line.indexOf("silence_start:")
                    silenceStart = line.substring(silenceStartStart+15).toFloat()
                    Toolboks.LOGGER.info(line)
                }
                // Getting the first silence end
                if(line.contains("silencedetect") && line.contains("silence_end") && silenceEnd == -1f){
                    val silenceEndStart = line.indexOf("silence_end:")
                    val silenceEndEnd = line.indexOf('|', silenceEndStart)
                    silenceEnd = line.substring(silenceEndStart+14, silenceEndEnd).toFloat()
                    Toolboks.LOGGER.info(line)
                }
            }
            if (ffmpegExecutor.getProcessExitCode() != 0) {
                //What the fuck happened
                Toolboks.LOGGER.info("Detecting music start failed!")
            }
        }
        finally {
            ffmpegExecutor.destroy()
        }

        if(fileFormat == "ogg" && silenceStart+musicStart < 0.0001){
            return silenceEnd-silenceStart
        }
        if(silenceStart == 0f){
            if(fileFormat == "mp3"){
                return silenceEnd+musicStart*2
            }
            return silenceEnd
        }
        if(fileFormat == "mp3"){
            return musicStart*2
        }

        return 0f
    }
}