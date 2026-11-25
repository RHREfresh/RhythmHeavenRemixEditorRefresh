package io.github.chrislo27.rhrefresh.soundsystem.miniaudio

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.MathUtils
import games.rednblack.miniaudio.MASound
import games.rednblack.miniaudio.MiniAudio
import io.github.chrislo27.rhrefresh.soundsystem.FFmpeg
import io.github.chrislo27.toolboks.Toolboks

object MiniAudioSoundSystem{

    val miniAudio = MiniAudio()

    private fun frameToMs(frame: Int, samplerate: Int, channelCount: Int): Float{
        return (frame/channelCount).div(samplerate.toFloat())
    }

    fun newMusic(handle: FileHandle): MiniAudioMusic {
        val musicStartFrames:Float = run{
            val musicDecoder = miniAudio.createDecoder(handle.file().absolutePath)
            val musicLength = musicDecoder.lengthInFrames.toInt()
            val musicInfo = FFmpeg.createMultimediaObject(handle.file()).info.audio
            val chunk = FloatArray(musicLength * musicInfo.channels)
            musicDecoder.readFrames(chunk)
            for((i,frame) in chunk.withIndex()){
                if(!MathUtils.isEqual(frame, 0f, 0.0005f)){
                    return@run frameToMs(i, musicInfo.samplingRate, musicInfo.channels)
                }
            }
            -1f
        }
        val music = MiniAudioMusic(miniAudio.createSound(handle.file().absolutePath), musicStartFrames)
        return music
    }

    fun newSound(handle: FileHandle): MASound {
        return miniAudio.createSound(handle.file().absolutePath)
    }

    fun dispose(){
        miniAudio.dispose()
    }
}