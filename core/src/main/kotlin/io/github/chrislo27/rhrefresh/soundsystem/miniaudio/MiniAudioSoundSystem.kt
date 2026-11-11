package io.github.chrislo27.rhrefresh.soundsystem.miniaudio

import com.badlogic.gdx.files.FileHandle
import games.rednblack.miniaudio.MiniAudio
import games.rednblack.miniaudio.config.MAEngineConfiguration
import io.github.chrislo27.rhrefresh.soundsystem.FFmpeg
import io.github.chrislo27.toolboks.Toolboks

object MiniAudioSoundSystem{

    val miniAudio = MiniAudio()

    fun newMusic(handle: FileHandle): MiniAudioMusic {
        val musicStart = FFmpeg.detectMusicStart(handle)
        Toolboks.LOGGER.info("Estimated music start to ${musicStart}s")
        val music = MiniAudioMusic(miniAudio.createSound(handle.file().absolutePath), musicStart)
        return music
    }

    fun dispose(){
        miniAudio.dispose()
    }
}