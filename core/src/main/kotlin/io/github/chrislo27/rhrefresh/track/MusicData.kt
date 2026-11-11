package io.github.chrislo27.rhrefresh.track

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.StreamUtils
import io.github.chrislo27.rhrefresh.soundsystem.beads.BeadsMusic
import io.github.chrislo27.rhrefresh.soundsystem.beads.BeadsSoundSystem
import io.github.chrislo27.rhrefresh.soundsystem.miniaudio.MiniAudioMusic
import io.github.chrislo27.rhrefresh.soundsystem.miniaudio.MiniAudioSoundSystem
import java.io.InputStream


class MusicData(val handle: FileHandle, val remix: Remix)
    : Disposable {

    val music: MiniAudioMusic = MiniAudioSoundSystem.newMusic(handle)
//    val music: BeadsMusic = BeadsSoundSystem.newMusic(handle)
    private val reader: InputStream = handle.read()

    override fun dispose() {
        music.dispose()
        StreamUtils.closeQuietly(reader)
    }
}