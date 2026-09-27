package io.github.chrislo27.rhrefresh.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.sun.jna.Platform
import io.github.chrislo27.rhrefresh.PreferenceKeys
import io.github.chrislo27.rhrefresh.RHREfresh
import io.github.chrislo27.rhrefresh.RHREfreshApplication
import io.github.chrislo27.rhrefresh.RemixRecovery
import io.github.chrislo27.rhrefresh.analytics.AnalyticsHandler
import io.github.chrislo27.rhrefresh.editor.Editor
import io.github.chrislo27.rhrefresh.editor.stage.EditorStage
import io.github.chrislo27.rhrefresh.editor.stage.Java32BitWarningStage
import io.github.chrislo27.rhrefresh.editor.stage.StartupStage
import io.github.chrislo27.rhrefresh.track.PlayState
import io.github.chrislo27.toolboks.ToolboksScreen
import io.github.chrislo27.toolboks.registry.ScreenRegistry
import java.io.File


class EditorScreen(main: RHREfreshApplication) : ToolboksScreen<RHREfreshApplication, EditorScreen>(main) {

    companion object {
        var enteredEditor: Boolean = false
            private set
    }

    val editor: Editor = Editor(main, main.defaultCamera, true)
    override val stage: EditorStage
        get() = editor.stage

    private var firstShowing = true

    private val presenceCycle = 60f
    private var refreshPresence: Float = 0f

    override fun show() {
        super.show()
        (Gdx.input.inputProcessor as? InputMultiplexer)?.addProcessor(editor)
        enteredEditor = true

        if (firstShowing) {
            firstShowing = false
            RemixRecovery.cacheRemixChecksum(editor.remix)
            
            if (main.preferences.getBoolean(PreferenceKeys.SHOW_STARTUP_SCREEN, true)) {
                stage.elements += StartupStage(this)
            }
            if (!Platform.is64Bit()) {
                stage.elements += Java32BitWarningStage(this)
                AnalyticsHandler.track("32-bit Warning Screen", mapOf())
            }
            stage.updatePositions()
        }

        refreshPresence = presenceCycle

        stage.updateFilters()

        editor.updateMessageLabel()

        if (RHREfresh.remixPath != "") {
            val screen = ScreenRegistry.getNonNullAsType<OpenRemixScreen>("openRemix")
            editor.main.screen = screen
            val remixFile = File(RHREfresh.remixPath)
            RHREfresh.remixPath = "" //Resetting so we don't enter an infinite loop
            screen.loadFile(remixFile)
            main.screen = ScreenRegistry["editor"] // This forces the last checksum to be of a blank remix
            main.screen = screen
        }
    }

    override fun hide() {
        super.hide()
        editor.remix.playState = PlayState.STOPPED
        (Gdx.input.inputProcessor as? InputMultiplexer)?.removeProcessor(editor)
    }

    override fun render(delta: Float) {
        editor.render()
        super.render(delta)
        editor.postStageRender()
    }

    override fun renderUpdate() {
        super.renderUpdate()
        editor.renderUpdate()

        refreshPresence -= Gdx.graphics.deltaTime
        if (refreshPresence <= 0) {
            refreshPresence = presenceCycle
            if (!stage.playalongStage.visible && !stage.presentationModeStage.visible && !(editor.remix.playState != PlayState.STOPPED && editor.remix.midiInstruments > 0)) {
            }
        }
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        editor.resize()
    }

    override fun dispose() {
        editor.dispose()
    }

    override fun getDebugString(): String? {
        return editor.getDebugString()
    }

    override fun tickUpdate() {
    }
}