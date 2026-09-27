package io.github.chrislo27.rhrefresh.soundsystem

import io.github.chrislo27.rhrefresh.PreferenceKeys
import io.github.chrislo27.rhrefresh.RHREfresh
import io.github.chrislo27.toolboks.Toolboks
import ws.schild.jave.Encoder
import ws.schild.jave.MultimediaObject
import ws.schild.jave.process.ProcessLocator
import ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator
import ws.schild.jave.utils.RBufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*
import kotlin.math.pow


/**
 * A simple wrapper around the [FFMPEG](https://ffmpeg.org/) executables.
 */
object Ffmpeg{

    enum class ARCH_OS(val supported: Boolean, val executableName: String) {
        UNSUPPORTED(false, ""),
        WINDOWS_X64(true, "ffmpeg_win_x64.exe"),
        MACOS_ARM64(true, "ffmpeg_macOS_arm64"),
        LINUX_X64(true, "ffmpeg_linux_x64");

        companion object {
            val ALL_VALUES: List<ARCH_OS> = values().toList()
            val SUPPORTED: List<ARCH_OS> = ALL_VALUES - UNSUPPORTED
        }
    }

    val currentARCH_OS: ARCH_OS = try {
        val osName: String = System.getProperty("os.name", "???")?.toLowerCase(Locale.ROOT) ?: "???"
        val archName: String = System.getProperty("os.arch", "???")?.toLowerCase(Locale.ROOT) ?: "???"
        when {
            "win" in osName && archName == "amd64" -> ARCH_OS.WINDOWS_X64
            "mac" in osName && archName == "aarch64"  -> ARCH_OS.MACOS_ARM64
            osName.startsWith("linux") && archName == "amd64" -> ARCH_OS.LINUX_X64
            else -> ARCH_OS.UNSUPPORTED
        }
    } catch (e: Exception) {
        e.printStackTrace()
        ARCH_OS.UNSUPPORTED
    }
    val isSupported: Boolean get() = currentARCH_OS.supported

    // Return the encoder built for the platform or the one selected by the user, otherwise uhhhhh
    fun createEncoder(): Encoder{
        return Encoder(createProcessLocator())
    }
    // Return the ProcessLocator built for the platform or the one selected by the user, otherwise uhhhhh
    fun createProcessLocator(): ProcessLocator{
        val ffmpegLocation = RHREfresh.PREFERENCES.getString(PreferenceKeys.SETTINGS_FFMPEG_LOCATION, "")
        return if(File(ffmpegLocation).exists()){
            ProcessLocator { ffmpegLocation }
        }else {
            if(isSupported){
                ProcessLocator { RHREfresh.FFMPEG_FOLDER.child(currentARCH_OS.executableName).file().absolutePath }
            } else{
                DefaultFFMPEGLocator()
            }
        }
    }

    fun createMultimediaObject(file: File): MultimediaObject{
        return if(isSupported){
            MultimediaObject(file, createProcessLocator())
        }else {
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
    fun processStreams(input: File, output: File, tempoPercent: Float, pitchSemitones: Float, ratePercent: Float, quick: Boolean) {

        val ffmpegExecutor = createProcessLocator().createExecutor()
        ffmpegExecutor.addArgument("-y")
        ffmpegExecutor.addArgument("-i")
        ffmpegExecutor.addArgument(input.absolutePath)

        val encoder = createEncoder()
        var filterChain = ""
        if(tempoPercent>1f){
            filterChain += "atempo=$tempoPercent"
        } else if(tempoPercent<1f) {
            val tempoMultiplier = tempoPercent.toDouble().pow(0.2)
            for(i in 1..5){
                filterChain += "atempo=$tempoMultiplier,"
            }
        }
        if(pitchSemitones!=0f){
            if(!filterChain.endsWith(",")) filterChain+=","
            filterChain += "rubberband=pitch="+(2.0.pow(pitchSemitones.div(12).toDouble()))
        }
        if(filterChain.isNotEmpty()){
            ffmpegExecutor.addArgument("-af")
            ffmpegExecutor.addArgument(filterChain)
        }
        ffmpegExecutor.addArgument(output.absolutePath)
        Toolboks.LOGGER.info("FFMPEG ran for file ${input.path} with arguments `$filterChain`")
        try {
            ffmpegExecutor.execute()
            val reader =
                RBufferedReader(InputStreamReader(ffmpegExecutor.errorStream))
            var line: String?
            while (((reader.readLine().also { line = it })) != null) {
                Toolboks.LOGGER.info(line!!)
            }
            if (ffmpegExecutor.getProcessExitCode() !== 0) {
                // it failed, and the lines above say why
            }
        } finally {
            ffmpegExecutor.destroy()
        }
    }
}