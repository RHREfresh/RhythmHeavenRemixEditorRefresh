package io.github.chrislo27.rhrefresh.soundsystem

import io.github.chrislo27.rhrefresh.PreferenceKeys
import io.github.chrislo27.rhrefresh.RHREfresh
import io.github.chrislo27.toolboks.Toolboks
import ws.schild.jave.Encoder
import ws.schild.jave.MultimediaObject
import ws.schild.jave.encode.ArgType
import ws.schild.jave.encode.AudioAttributes
import ws.schild.jave.encode.EncodingAttributes
import ws.schild.jave.encode.ValueArgument
import ws.schild.jave.process.ProcessLocator
import java.io.File
import java.util.Locale
import java.util.Optional
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
    val isSupported: Boolean get() = currentARCH_OS.supported

    // Return the encoder built for the platform or the one selected by the user, otherwise uhhhhh
    fun createEncoder(): Encoder{
        val ffmpegLocation = RHREfresh.PREFERENCES.getString(PreferenceKeys.SETTINGS_FFMPEG_LOCATION, "")
        return if (File(ffmpegLocation).exists()) {
            Encoder(ProcessLocator { ffmpegLocation })
        } else {
            if (isSupported) {
                Encoder(ProcessLocator { RHREfresh.FFMPEG_FOLDER.child(currentARCH_OS.executableName).file().absolutePath })
            } else {
                Encoder()
            }
        }
    }

    fun createMultimediaObject(file: File): MultimediaObject{
        return if (isSupported) {
            MultimediaObject(file, ProcessLocator { RHREfresh.FFMPEG_FOLDER.child(currentARCH_OS.executableName).file().absolutePath })
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
    fun processStreams(input: File, output: File, tempoPercent: Float, pitchSemitones: Float, ratePercent: Float, quick: Boolean) {

        val audio = AudioAttributes();
        audio.setCodec("pcm_s16le")
        val attrs = EncodingAttributes()
        attrs.setAudioAttributes(audio)

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
            Encoder.setOptionAtIndex(ValueArgument(ArgType.OUTFILE, "-af") { Optional.of(filterChain) }, 33)
        } else {
            Encoder.removeOptionAtIndex(33)
        }
        val multimediaFile = createMultimediaObject(input)
        Toolboks.LOGGER.info("FFMPEG ran for file ${input.path} with arguments `$filterChain`")
        encoder.encode(multimediaFile, output, attrs)
    }
}