package io.github.chrislo27.rhrefresh.editor.stage

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import io.github.chrislo27.rhrefresh.PreferenceKeys
import io.github.chrislo27.rhrefresh.editor.Editor
import io.github.chrislo27.rhrefresh.screen.EditorScreen
import io.github.chrislo27.rhrefresh.track.PlayState
import io.github.chrislo27.toolboks.i18n.Localization
import io.github.chrislo27.toolboks.ui.*


class SnapButton(val editor: Editor, palette: UIPalette, parent: UIElement<EditorScreen>,
                 stage: Stage<EditorScreen>)
    : Button<EditorScreen>(palette, parent, stage) {

    companion object {
        val snapLevels = intArrayOf(4, 6, 8, 12, 16, 24, 32, 48)
    }

    val preferences = Gdx.app.getPreferences("RHREFRESH")

    private var index: Int = 0
        set(value) {
            field = value
            fractionString = "1/$snapLevel"
        }
    private val snapLevel: Int
        get() = snapLevels[index]
    private val snapFloat: Float
        get() = if (snapLevel == 0) 0f else 1f / snapLevel
    private var fractionString: String = "1/$snapLevel"

    private fun updateAndFlash() {
        editor.subbeatSection.setFlash(0.5f)
        editor.snap = snapFloat
        hoverTime = 0f
    }

    override var tooltipText: String?
        set(_) {}
        get() {
            return Localization["editor.snap.tooltip", "$fractionString ♩"]
        }
    
    init {
        addLabel(TextLabel(palette, this, stage).apply {
            this.text = "editor.snap"
            this.isLocalizationKey = true
            this.textWrapping = false
            this.fontScaleMultiplier = 0.5f
            this.location.set(screenWidth = 0.4f, pixelX = 2f, pixelWidth = -2f)
        })
        addLabel(object : TextLabel<EditorScreen>(palette, this, stage) {
            override fun getRealText(): String {
                return fractionString
            }
        }.apply {
            this.isLocalizationKey = false
            this.fontScaleMultiplier = 0.85f
            this.textWrapping = false
            this.location.set(screenX = 0.4f, screenWidth = 0.6f)
        })
    }

    override fun render(screen: EditorScreen, batch: SpriteBatch, shapeRenderer: ShapeRenderer) {
        super.render(screen, batch, shapeRenderer)
        val minHover = 1f
        if (hoverTime > minHover && !wasClickedOn && editor.remix.playState == PlayState.STOPPED) {
            editor.subbeatSection.setFlash(Gdx.graphics.deltaTime)
        }
    }

    override fun scrolled(amountX: Float, amountY: Float) :Boolean {
        if (isMouseOver()) {
            super.scrolled(amountX, amountY)
            val direction = if (amountY> 0f) 1 else -1
            val maxSnapLevel = if (preferences.getBoolean(PreferenceKeys.SETTINGS_ADVANCED_OPTIONS, false)) snapLevels.size else snapLevels.size - 1
            index =
                if (index+direction >= maxSnapLevel) 0
                else if (index+direction < 0) maxSnapLevel-1
                else index + direction
            updateAndFlash()
            return true
        }
        return false
    }

    override fun onLeftClick(xPercent: Float, yPercent: Float) {
        super.onLeftClick(xPercent, yPercent)
        val advancedOptionsEnabled = preferences.getBoolean(PreferenceKeys.SETTINGS_ADVANCED_OPTIONS, false)
        if (advancedOptionsEnabled) {
            index = if (index + 1 >= snapLevels.size) 0 else index + 1
        } else {
            index = if (index + 1 >= snapLevels.size-1) 0 else index + 1
        }
        updateAndFlash()
    }

    override fun onRightClick(xPercent: Float, yPercent: Float) {
        super.onRightClick(xPercent, yPercent)
        index = 0
        updateAndFlash()
    }
}