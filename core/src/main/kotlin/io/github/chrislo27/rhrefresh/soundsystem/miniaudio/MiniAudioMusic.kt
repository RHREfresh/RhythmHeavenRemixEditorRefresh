package io.github.chrislo27.rhrefresh.soundsystem.miniaudio

import com.badlogic.gdx.ApplicationListener
import games.rednblack.miniaudio.MASound
import games.rednblack.miniaudio.MiniAudio
import games.rednblack.miniaudio.filter.MABandPassFilter

class MiniAudioMusic(val audio: MASound, val startOfSound:Float = 0f){

    var isDistorted = false

    fun play(){
        audio.play()
    }

    fun pause() {
        audio.pause()
    }

    fun resume() {
        audio.play()
    }

    fun stop() {
        audio.reset()
    }

    fun isPlaying(): Boolean{
        return audio.isPlaying
    }

    fun distortAudio(distort: Boolean){
        isDistorted = distort
//        val distortFilter = MABandPassFilter(, 1.5, 0)
    }

    fun getVolume(): Float{
        return audio.volume
    }

    fun setVolume(vol: Float) {
        audio.volume = vol.coerceAtLeast(0f)
    }

    fun setPitch(pitch: Float){
        audio.setPitch(pitch)
    }

    fun setPosition(seconds: Float) {
        audio.seekTo(seconds)
    }

    fun getPosition(): Float{
        return audio.cursorPosition
    }

    fun dispose() {
        audio.dispose()
    }
}