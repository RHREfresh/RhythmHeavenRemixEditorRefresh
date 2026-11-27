package io.github.chrislo27.rhrefresh.soundsystem

import com.badlogic.gdx.files.FileHandle
import io.github.chrislo27.rhrefresh.PreferenceKeys
import io.github.chrislo27.rhrefresh.RHREfresh
import io.github.chrislo27.rhrefresh.entity.model.MultipartEntity
import io.github.chrislo27.rhrefresh.entity.model.cue.CueEntity
import io.github.chrislo27.rhrefresh.sfxdb.datamodel.impl.Cue
import io.github.chrislo27.rhrefresh.track.Remix
import io.github.chrislo27.toolboks.Toolboks
import ws.schild.jave.Encoder
import ws.schild.jave.MultimediaObject
import ws.schild.jave.process.ProcessLocator
import ws.schild.jave.process.ProcessWrapper
import ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator
import ws.schild.jave.utils.RBufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.collections.set
import kotlin.math.ceil
import kotlin.math.floor
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
     */
    fun processStreams(input: File, output: File, tempoPercent: Float, pitchSemitones: Float) {

        val ffmpegExecutor = createProcessLocator().createExecutor()
        ffmpegExecutor.addArgument("-y")
        ffmpegExecutor.addArgument("-hide_banner")
        ffmpegExecutor.addArgument("-i")
        ffmpegExecutor.addArgument(input.absolutePath)

        val filterChain = generateSoundstretch(tempoPercent, pitchSemitones)
        if (filterChain.isNotEmpty()) {
            ffmpegExecutor.addArgument("-af")
            ffmpegExecutor.addArgument(filterChain.joinToString(","))
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
            if (ffmpegExecutor.getProcessExitCode() != 0) {
                Toolboks.LOGGER.info("Failed to apply tempo and pitch!")
            }
        } finally {
            ffmpegExecutor.destroy()
        }
    }

    /**
     * Generates the complex filter used for soundstretch sounds.
     * @param tempoPercent Difference between the old tempo and the new, in percent (1.0 being 100%)
     * @param pitchSemitones The amount of semitones to change the output file to.
     * @return A string containing a full complex filter chain.
     */
    private fun generateSoundstretch(tempoPercent: Float, pitchSemitones: Float): List<String>{
        var filterChain: MutableList<String> = mutableListOf()
        if (pitchSemitones!=0f) {
            filterChain += "rubberband=pitch="+(2.0.pow(pitchSemitones.div(12).toDouble()))
        }
        if (tempoPercent>1f) {
            filterChain += "atempo=$tempoPercent"
        } else if (tempoPercent<1f) {
            val tempoMultiplier = tempoPercent.toDouble().pow(0.2)
            for(i in 1..5) {
                filterChain += "atempo=$tempoMultiplier"
            }
        }
        return filterChain
    }

    /**
     * Checks whether FFmpeg is supported. Sets forceUnsupported to true if not.
     */
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
            if (ffmpegExecutor.getProcessExitCode() != 0) {
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
     * Get the actual metadata start of the song.
     * For most formats, such a field doesn't exist.
     * However, MP3 (and to a certain extent OGG) are special and may start earlier or later.
     */
    fun getMusicStart(file: String): Float{
        val ffmpegExecutor = createProcessLocator().createExecutor()
        ffmpegExecutor.addArgument("-hide_banner")
        ffmpegExecutor.addArgument("-i")
        ffmpegExecutor.addArgument(file)
        ffmpegExecutor.addArgument("-vn")
        ffmpegExecutor.addArgument("-f")
        ffmpegExecutor.addArgument("null")
        ffmpegExecutor.addArgument("-")

        try {
            ffmpegExecutor.execute()
            val reader =
                RBufferedReader(InputStreamReader(ffmpegExecutor.errorStream))
            var line: String?
            while (((reader.readLine().also { line = it } )) != null) {
                // Detecting the start of the song
                if(line!!.trimStart().startsWith("Duration") && line.contains("start:")){
                    Toolboks.LOGGER.info(line)
                    val startStart = line.indexOf("start:")
                    val startEnd = line.indexOf(',', startStart)
                    return line.substring(startStart+7, startEnd).toFloat()
                }
            }
            if (ffmpegExecutor.getProcessExitCode() != 1) {
                //What the fuck happened
                Toolboks.LOGGER.info("Finding music start failed!")
            }
        }
        finally {
            ffmpegExecutor.destroy()
        }
        return 0f
    }

    /**
     * Get the actual metadata start of the song.
     * For most formats, such a field doesn't exist.
     * However, MP3 (and to a certain extent OGG) are special and may start earlier or later.
     */
    fun getCurrentTime(line: String): Float{
        if(line.contains("time=")) {
            Toolboks.LOGGER.info(line)
            val timeStart = line.indexOf("time=")
            val timeEnd = line.indexOf(' ', timeStart)
            val timeString = line.substring(timeStart + 5, timeEnd)
            val hours = timeString.substring(0,2).toFloat() //Probably unnecessary, but you never know what kind of stupid stuff RHRE users are up to nowadays
            val minutes = timeString.substring(3,5).toFloat()
            val seconds = timeString.substring(6,8).toFloat()
            val milliseconds = timeString.substring(9,11).toFloat()
            return ((milliseconds * 10f)
                        + (seconds * 1000f)
                        + (minutes * 60f * 1000f)
                        + (hours * 60f * 60f * 1000f));
        }
        return -1f
    }


    data class Audio(val soundId: String, var samplerate: Int = -1){
        var duration: Long = -1
        var beatDuration: Float = 0f
        var replacementFile: String = ""
    }

    data class FFmpegCueEntity(val cue: CueEntity){
        val filterList: MutableList<String> = mutableListOf()
        var delay: Float = -1f
        var endDelay: Float = -1f
        var audio: Audio = Audio("UNUSED")
    }

    data class RemixPartData(val cues: List<FFmpegCueEntity>, val durationMs:Float, val earliestCue: Float)

    /**
     * Exported remix part
     * @ffmpegExecutor = FFmpeg Executor, ready to be executed to generate the file
     * @initialDelay = Delay to apply to the file, in milliseconds
     * @file The file FFmpeg exports to.
     */
    data class RemixPartFile(val ffmpegExecutor: ProcessWrapper, val initialDelay: Float, val file: File)

    fun generateParts(remix: Remix, startSeconds: Float, endSeconds: Float): List<RemixPartData>{
        val maxFilters = 500
        val cueList: MutableMap<String, Cue> = mutableMapOf()
        val soundList: MutableList<Audio> = mutableListOf()
        val flattenedRemix: MutableList<FFmpegCueEntity> = mutableListOf()
        val remixParts: MutableList<RemixPartData> = mutableListOf()

        // First we flatten the remix (convert multiparts into single cues)
        for(entity in remix.entities){
            //Stupid workaround using floor and ceil because sometimes it converts beats with a slight offsync?
            if(remix.tempos.beatsToSeconds(entity.bounds.x) in floor(startSeconds)..ceil(endSeconds)){
                when (entity) {
                    is MultipartEntity<*> -> {
                        val flattenedEntity = flattenMultipart(entity)
                        for (entity in flattenedEntity) {
                            if (!cueList.containsKey(entity.cue.datamodel.id)) {
                                cueList[entity.cue.datamodel.id] = entity.cue.datamodel
                            }
                        }
                        flattenedRemix.addAll(flattenedEntity)
                    }

                    is CueEntity -> {
                        if (!cueList.containsKey(entity.datamodel.id)) {
                            cueList[entity.datamodel.id] = entity.datamodel
                        }
                        flattenedRemix.add(FFmpegCueEntity(entity))
                    }
                }
            }
        }

        // Second, we generate the Fillbots sounds.
        // Since they're the most complex, it's better to do it *now* rather than in the export, to gain some time
        val fillbotsIdRegex: Regex = "fillbots(.+)?/water".toRegex()
        for(entity in flattenedRemix){
            if(entity.cue.datamodel.id.matches(fillbotsIdRegex)){
                var hasFillSound = false
                for(sound in soundList){
                    if(sound.beatDuration == entity.cue.bounds.width && sound.soundId == entity.cue.datamodel.id){
                        hasFillSound = true
                    }
                }
                if(!hasFillSound){
                    val timeDuration = remix.tempos.beatsToSeconds(entity.cue.bounds.x+entity.cue.bounds.width) - remix.tempos.beatsToSeconds(entity.cue.bounds.x)
                    var samplerate = createMultimediaObject(entity.cue.datamodel.soundFile).info.audio.samplingRate
                    val fillFile = generateFillbots(entity.cue, timeDuration, samplerate)
                    val fillAudio = Audio(entity.cue.datamodel.id)
                    fillAudio.beatDuration = entity.cue.bounds.width
                    if(fillFile == null){
                        throw IOException("Could not generate Fillbots fill")
                    }
                    fillAudio.replacementFile = fillFile.absolutePath
                    soundList.add(fillAudio)
                }
            }
        }


        // Finally, we set up the ffmpeg filters!
        var filterAmount = 0
        var currentCueStack: MutableList<FFmpegCueEntity> = mutableListOf()
        var earliestCue = Float.MAX_VALUE
        var lastCue = Float.MIN_VALUE

        for(entity in flattenedRemix){
            if(!entity.cue.datamodel.isSpecial){
                if(earliestCue > remix.tempos.beatsToSeconds(entity.cue.bounds.x)+entity.cue.datamodel.earliness){
                    earliestCue = remix.tempos.beatsToSeconds(entity.cue.bounds.x)+entity.cue.datamodel.earliness
                }
                //I should take the ending sound if it exists but close enough I Think
                if(lastCue < remix.tempos.beatsToSeconds(entity.cue.bounds.x)){
                    lastCue = remix.tempos.beatsToSeconds(entity.cue.bounds.x)
                }
                if(entity.cue.datamodel.id.matches(fillbotsIdRegex)){
                    for(sound in soundList){
                        if(sound.beatDuration == entity.cue.bounds.width && sound.soundId == entity.cue.datamodel.id){
                            entity.audio = sound
                        }
                    }
                    entity.delay = (remix.tempos.beatsToSeconds(entity.cue.bounds.x)*1000) - (entity.cue.datamodel.earliness*1000)
                    filterAmount ++
                    if(entity.cue.datamodel.introSoundCue != null){
                        generateExportSingleCueFilter(entity, entity.cue.datamodel.introSoundCue, soundList, remix)
                        currentCueStack.add(entity)
                        filterAmount += entity.filterList.size+1
                    }
                    if(entity.cue.datamodel.endingSoundCue != null){
                        generateExportSingleCueFilter(entity, entity.cue.datamodel.endingSoundCue, soundList, remix, true)
                        currentCueStack.add(entity)
                        filterAmount += entity.filterList.size+1
                    }
                } else{
                    generateExportSingleCueFilter(entity, soundList, remix)
                    currentCueStack.add(entity)
                    // We're adding one to account for the delay/trim filter, who isn't in the filter list
                    filterAmount += entity.filterList.size+1
                    if(entity.cue.datamodel.introSoundCue != null){
                        generateExportSingleCueFilter(entity, entity.cue.datamodel.introSoundCue, soundList, remix)
                        currentCueStack.add(entity)
                        filterAmount += entity.filterList.size+1
                    }
                    if(entity.cue.datamodel.endingSoundCue != null){
                        generateExportSingleCueFilter(entity, entity.cue.datamodel.endingSoundCue, soundList, remix, true)
                        currentCueStack.add(entity)
                        filterAmount += entity.filterList.size+1
                    }
                }
            }
            if(filterAmount > maxFilters){
                Toolboks.LOGGER.info("Generating part with $filterAmount filters")
                remixParts.add(RemixPartData(currentCueStack,(lastCue-earliestCue)*1000, earliestCue*1000))
                filterAmount = 0
                currentCueStack = mutableListOf()
                earliestCue = Float.MAX_VALUE
                lastCue = Float.MIN_VALUE
            }
        }
        //We just clean off the end of the filters
        if(filterAmount > 0){
            Toolboks.LOGGER.info("Last part! Generating with $filterAmount filters")
            remixParts.add(RemixPartData(currentCueStack,(lastCue-earliestCue)*1000, earliestCue*1000))
        }
        return remixParts
    }

    fun createFinalRemixExecutor(remix: Remix, file: File, startSeconds: Float, endSeconds: Float, samplerate: Int, remixParts: List<RemixPartFile>): ProcessWrapper{
        val ffmpegExecutor = createProcessLocator().createExecutor()
        var complexFilter = ""
        ffmpegExecutor.addArgument("-y")
        ffmpegExecutor.addArgument("-hide_banner")


        // Sets up volumes in the music
        // Hopefully should only be a handful of filters so I'm fine with letting it outside a part
        if(remix.music != null){
            complexFilter +="[${remixParts.size}]"
            if(remix.musicVolumes.map.isNotEmpty()){
                var asendcmdChain = ""
                for((_, volumeChange) in remix.musicVolumes.map){
                    val newVol: Float = volumeChange.volume/100f
                    val beat = volumeChange.beat
                    if(remix.musicVolumes.volumeAt(beat) != newVol){
                        val startTime = remix.tempos.beatsToSeconds(beat)+remix.musicStartSec*(-1)
                        val width = volumeChange.width
                        if(width != 0f){
                            val duration = (remix.tempos.beatsToSeconds(beat+width)+remix.musicStartSec*(-1))-startTime
                            val currentVol = remix.musicVolumes.volumeAt(beat)
                            if(currentVol>newVol){
                                complexFilter += "afade=enable='between(t,$startTime,${startTime+duration})':t=out:st=$startTime:d=$duration:silence=$newVol:unity=$currentVol,"
                                asendcmdChain += "${startTime + duration} volume volume $newVol;"
                            } else{
                                complexFilter += "afade=enable='between(t,$startTime,${startTime + duration})':t=in:st=$startTime:d=$duration:silence=$currentVol:unity=$newVol,"
                                asendcmdChain += "$startTime volume volume $newVol;"
                            }
                        } else{
                            asendcmdChain += "$startTime volume volume $newVol;"
                        }
                    }
                }
                if(asendcmdChain.isNotEmpty()){
                    complexFilter += "asendcmd=c='$asendcmdChain', volume,"
                }
            }
            var musicStart = remix.musicStartSec
            // TODO Gigamix is weirdly offsync?
            if(remix.music!!.handle.extension() == "mp3"){
                musicStart += getMusicStart(remix.music!!.handle.file().absolutePath)*2
            }
            if(musicStart<0){
                complexFilter += "atrim=start=${musicStart*(-1)}"
            } else{
                complexFilter += "adelay=delays=${musicStart*1000}:all=1"
            }
            complexFilter += "[song];"
        }

        for((currentPart, part) in remixParts.withIndex()){
            ffmpegExecutor.addArgument("-i")
            ffmpegExecutor.addArgument(part.file!!.absolutePath)
            if(part.initialDelay < 0){
                complexFilter +="[$currentPart]atrim=start=${(part.initialDelay/1000)*-1}[part$currentPart];"
            } else{
                complexFilter +="[$currentPart]adelay=delays=${part.initialDelay}:all=1[part$currentPart];"
            }
        }

        for(i in remixParts.indices){
            complexFilter += "[part$i]"
        }
        if(remix.music != null){
            ffmpegExecutor.addArgument("-i")
            ffmpegExecutor.addArgument(remix.music!!.handle.file().absolutePath)
            complexFilter += "[song]amix=${remixParts.size+1}:normalize=false,"
        } else{
            complexFilter += "amix=${remixParts.size}:normalize=false,"
        }
        complexFilter += "aresample=$samplerate,atrim=$startSeconds:$endSeconds"


        ffmpegExecutor.addArgument("-filter_complex")
        ffmpegExecutor.addArgument(complexFilter)
        ffmpegExecutor.addArgument(file.absolutePath)

        return ffmpegExecutor


    }

    /**
     * Exports a single part of a remix.
     */
    fun exportPart(remixPart: RemixPartData, samplerate:Int): RemixPartFile {
        val partFile: File = File.createTempFile("rhre-ffmpeg-export-part-", ".wav").apply {
            deleteOnExit()
        }
        var complexFilter = ""
        val ffmpegExecutor = createProcessLocator().createExecutor()
        ffmpegExecutor.addArgument("-y")
        ffmpegExecutor.addArgument("-hide_banner")

        // First we figure out the earliest entity
        // That way we can skip the silence generation

        val cueList: MutableList<Cue> = mutableListOf()
        var audioList: MutableList<Audio> = mutableListOf()
        //First, we map the sounds
        for(entity in remixPart.cues){
            if(entity.audio.soundId != "UNUSED"){
                audioList.add(entity.audio)
                ffmpegExecutor.addArgument("-i")
                ffmpegExecutor.addArgument(entity.audio.replacementFile)
            }
            if(!cueList.contains(entity.cue.datamodel)){
                cueList.add(entity.cue.datamodel)
            }
        }
        audioList.addAll(addSoundFiles(ffmpegExecutor, cueList))

        var sfxNumber = 0
        //Next, we build the filter!
        for(entity in remixPart.cues){
            if(entity.audio.soundId != "UNUSED"){
                complexFilter +="[${findSoundIndex(audioList, entity.audio.replacementFile)}]"
            } else{
                complexFilter +="[${findSoundIndex(audioList, entity.cue.datamodel)}]"
            }
            if(entity.filterList.isNotEmpty()){
                complexFilter += entity.filterList.joinToString(",")+","
            }
            val position = entity.delay-remixPart.earliestCue
            if(position < 0){
                complexFilter +="atrim=start=${(position/1000)*-1}"
            } else{
                complexFilter +="adelay=delays=$position:all=1"
            }
            complexFilter += "[sfx$sfxNumber];"
            sfxNumber++
            if(entity.cue.datamodel.introSoundCue != null){
                complexFilter +="[${findSoundIndex(audioList, entity.cue.datamodel.introSoundCue!!)}]"
                if(entity.filterList.isNotEmpty()){
                    complexFilter += entity.filterList.joinToString(",")+","
                }
                val position = entity.delay-remixPart.earliestCue
                if(position < 0){
                    complexFilter +="atrim=start=${(position/1000)*-1}"
                } else{
                    complexFilter +="adelay=delays=$position:all=1"
                }
                complexFilter += "[sfx$sfxNumber];"
                sfxNumber++
            }
            if(entity.cue.datamodel.endingSoundCue != null){
                complexFilter +="[${findSoundIndex(audioList, entity.cue.datamodel.endingSoundCue!!)}]"
                if(entity.filterList.isNotEmpty()){
                    complexFilter += entity.filterList.joinToString(",")+","
                }
                val position = entity.endDelay-remixPart.earliestCue
                if(position < 0){
                    complexFilter +="atrim=start=${(position/1000)*-1}"
                } else{
                    complexFilter +="adelay=delays=$position:all=1"
                }
                complexFilter += "[sfx$sfxNumber];"
                sfxNumber++
            }
        }

        for(i in 0..<sfxNumber){
            complexFilter += "[sfx$i]"
        }
        complexFilter += "amix=${sfxNumber}:normalize=false,aresample=$samplerate"

        val filterFile: File = File.createTempFile("rhre-ffmpeg-export-filters-", ".txt").apply {
            deleteOnExit()
        }
        filterFile.writeText(complexFilter)
        ffmpegExecutor.addArgument("-/filter_complex")
        ffmpegExecutor.addArgument(filterFile.absolutePath)
        ffmpegExecutor.addArgument(partFile.absolutePath)
        return RemixPartFile(ffmpegExecutor, remixPart.earliestCue, partFile)
    }

    private fun addSoundFiles(ffmpegExecutor: ProcessWrapper, cueList: List<Cue>): List<Audio>{
        val soundList: MutableList<Audio> = mutableListOf()
        for(cue in cueList){
            if(!cue.isSpecial){
                soundList.add(Audio(cue.id))
                ffmpegExecutor.addArgument("-i")
                ffmpegExecutor.addArgument(cue.soundFile.absolutePath)
                if(cue.introSoundCue != null){
                    soundList.add(Audio(cue.introSoundCue!!.id))
                    ffmpegExecutor.addArgument("-i")
                    ffmpegExecutor.addArgument(cue.introSoundCue!!.soundFile.absolutePath)
                }
                if(cue.endingSoundCue != null){
                    soundList.add(Audio(cue.endingSoundCue!!.id))
                    ffmpegExecutor.addArgument("-i")
                    ffmpegExecutor.addArgument(cue.endingSoundCue!!.soundFile.absolutePath)
                }
            }
        }
        return soundList
    }

    /**
     * Flattens multiparts into their CueEntity components.
     * Works with any multipart.
     */
    private fun flattenMultipart(multipartEntity: MultipartEntity<*>): List<FFmpegCueEntity>{
        val cueList: MutableList<FFmpegCueEntity> = mutableListOf()

        for(entity in multipartEntity.getInternalEntities()){
            if(entity is MultipartEntity<*>){
                val flattenedEntity = flattenMultipart(entity)
                cueList.addAll(flattenedEntity)
            } else if(entity is CueEntity){
                cueList.add(FFmpegCueEntity(entity))
            }
        }

        return cueList
    }

    private fun generateExportSingleCueFilter(entity: FFmpegCueEntity, soundList: MutableList<Audio>, remix: Remix){
        generateExportSingleCueFilter(entity, null, soundList, remix)
    }

    /**
     * Generates the filters required by FFmpeg to play the cue at the right time.
     */
    private fun generateExportSingleCueFilter(entity: FFmpegCueEntity, datamodel: Cue?, soundList: MutableList<Audio>, remix: Remix, isEndingSound: Boolean = false){
        val cue = datamodel ?: entity.cue.datamodel
        var soundIndex: Int = findSoundIndex(soundList, cue)
        val position = if(!isEndingSound){
             (remix.tempos.beatsToSeconds(entity.cue.bounds.x)*1000) - (cue.earliness*1000)
        } else{
            (remix.tempos.beatsToSeconds(entity.cue.bounds.x+entity.cue.bounds.width)*1000) - (cue.earliness*1000)
        }
        //Sound not added to the list, so we do it right now
        if(soundIndex == -1){
            soundList.add(Audio(cue.id))
            soundIndex = soundList.size-1
        }
        var soundSampleRate = soundList[soundIndex].samplerate

        // This handles looping sounds.
        // TODO ogg has a weird delay thing
        // Apparently OGG has a start value with my own ffmpeg???
        if(cue.loops && cue.loopEnd < 1e10){
            if(soundSampleRate == -1){
                soundSampleRate = createMultimediaObject(cue.soundFile).info.audio.samplingRate
                soundList[soundIndex].samplerate = soundSampleRate
            }
            if(soundList[soundIndex].duration == (-1).toLong()){
                soundList[soundIndex].duration = createMultimediaObject(cue.soundFile).info.duration
            }
            val soundLength = soundList[soundIndex].duration
            var loopFilter = "aloop=loop=-1"
            if(cue.loopStart != 0f && cue.loopStart != -1f){
                loopFilter += ":start=${floor(cue.loopStart*soundSampleRate)}"
            }
            if(cue.loopEnd != 0f && cue.loopEnd != -1f){
                loopFilter += ":size=${floor((cue.loopEnd-cue.loopStart) * soundSampleRate)}"
            } else{
                loopFilter += ":size=${soundLength*soundSampleRate}"
            }
            entity.filterList.add(loopFilter)
        }

        // This handles soundstretch sounds, and pitch
        if(cue.useTimeStretching && cue.baseBpm != 0f && remix.tempos.tempoAt(entity.cue.bounds.x) != cue.baseBpm){
            entity.filterList.addAll(generateSoundstretch(remix.tempos.tempoAt(entity.cue.bounds.x)/cue.baseBpm,entity.cue.semitone.toFloat()))
        }else if(entity.cue.semitone != 0 || (cue.randomPitchLowCents != 0 || cue.randomPitchHighCents != 0)){
            val randomPitchVariation = ThreadLocalRandom.current().nextInt(cue.randomPitchLowCents, cue.randomPitchHighCents+1)/100f
            if(soundSampleRate == -1){
                soundSampleRate = createMultimediaObject(cue.soundFile).info.audio.samplingRate
                soundList[soundIndex].samplerate = soundSampleRate
            }
            entity.filterList.addAll(listOf("asetrate=${(soundSampleRate*entity.cue.getSemitonePitch(randomPitchVariation)).toInt()}","aresample=$soundSampleRate"))
        }

        // This handles looping again, trimming the end of the loop
        if(cue.loops){
            val loopStart = remix.tempos.beatsToSeconds(entity.cue.bounds.x)
            val loopEnd = remix.tempos.beatsToSeconds(entity.cue.bounds.width+entity.cue.bounds.x)
            entity.filterList += "atrim=end=${loopEnd-loopStart}"
        }
        if(cue.stopAtEnd){
            val blockStart = remix.tempos.beatsToSeconds(entity.cue.bounds.x)
            val blockEnd = remix.tempos.beatsToSeconds(entity.cue.bounds.x+entity.cue.bounds.width)
            entity.filterList += "atrim=end=${blockEnd-blockStart}"
        }

        if(!isEndingSound){
            entity.delay = position
        } else{
            entity.endDelay = position
        }
    }

    private fun findSoundIndex(soundList: List<Audio>, cue: Cue): Int{
        for((i, element) in soundList.withIndex()){
            if(element.soundId == cue.id){
                return i
            }
        }
        return -1
    }
    private fun findSoundIndex(soundList: List<Audio>, audioFile: String): Int{
        for((i, element) in soundList.withIndex()){
            if(element.replacementFile == audioFile){
                return i
            }
        }
        return -1
    }

    /**
     * Generates a fillbot fill WAVE file to add back later
     * Fillbots fill has a gradual pitch, which FFmpeg cannot recreate
     * So instead, I cut the sound in 127 parts and pitch that,
     * which means doing it once ends up gaining a lot of time
     */
    private fun generateFillbots(entity: CueEntity,durationSeconds: Float, sampleRate: Int): File? {
        val ffmpegExecutor = createProcessLocator().createExecutor()
        ffmpegExecutor.addArgument("-y")
        ffmpegExecutor.addArgument("-hide_banner")
        ffmpegExecutor.addArgument("-i")
        ffmpegExecutor.addArgument(entity.datamodel.soundFile.absolutePath)
        var filter = ""
        var sfxStack = ""
        val duration = entity.bounds.width
        val beatPart: Float = entity.bounds.width/127f
        val secondsPart: Float = durationSeconds/127f
        val leftSide: Float = (0.6f/duration)
        val rightSide: Float = if(duration <=3){
            -0.2f * duration + 1.2f
        } else{
            -0.025f * duration + 0.675f
        }
        var sampleCursor: Int = 0
        for(i in 0..127){ //`i` is technically beat
            val currentBeat = beatPart*i
            val currentPitch = leftSide*currentBeat+rightSide
            val newSamplerate = (sampleRate*currentPitch).toInt()
            val amtSamplesUsed = (newSamplerate*secondsPart).toInt()
            filter += "[0]"
            filter += "asetrate=$newSamplerate,aresample=$sampleRate,"
            filter += "atrim=start_pts=$sampleCursor:end_pts=${sampleCursor+amtSamplesUsed},"
            filter += "adelay=delays=${secondsPart*i*1000}:all=1"
            filter += "[fbots${i}];"
            sfxStack += "[fbots${i}]"
            sampleCursor += amtSamplesUsed
        }
        filter += "${sfxStack}amix=128:normalize=false,aresample=$sampleRate"

        ffmpegExecutor.addArgument("-filter_complex")
        ffmpegExecutor.addArgument(filter)

        val file: File = File.createTempFile("rhre-export-fillbots-", ".wav").apply {
            deleteOnExit()
        }
        ffmpegExecutor.addArgument(file.absolutePath)

        try {
            ffmpegExecutor.execute()
            val reader =
                RBufferedReader(InputStreamReader(ffmpegExecutor.errorStream))
            var line: String?
            while (((reader.readLine().also { line = it } )) != null) {
                Toolboks.LOGGER.info(line!!)
            }
            if (ffmpegExecutor.getProcessExitCode() != 0) {
                Toolboks.LOGGER.info("Error on the export.")
                return null
            }
        } catch (e: IOException){
            Toolboks.LOGGER.info("Error on the export.")
            return null
        }
        finally {
            ffmpegExecutor.destroy()
        }
        return file
    }

    class ExportException: Exception("Error on the export.")
}