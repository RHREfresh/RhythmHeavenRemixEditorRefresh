package io.github.chrislo27.rhrefresh.screen.info

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Colors
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.rhrefresh.PreferenceKeys
import io.github.chrislo27.rhrefresh.RHREfresh
import io.github.chrislo27.rhrefresh.RHREfreshApplication
import io.github.chrislo27.rhrefresh.editor.Editor
import io.github.chrislo27.rhrefresh.screen.CreditsScreen
import io.github.chrislo27.rhrefresh.sfxdb.SFXDatabase
import io.github.chrislo27.rhrefresh.stage.LoadingIcon
import io.github.chrislo27.rhrefresh.util.Semitones
import io.github.chrislo27.toolboks.i18n.Localization
import io.github.chrislo27.toolboks.registry.AssetRegistry
import io.github.chrislo27.toolboks.registry.ScreenRegistry
import io.github.chrislo27.toolboks.transition.TransitionScreen
import io.github.chrislo27.toolboks.ui.*
import io.github.chrislo27.toolboks.util.MathHelper
import io.github.chrislo27.toolboks.util.gdxutils.isAltDown
import io.github.chrislo27.toolboks.util.gdxutils.isControlDown
import io.github.chrislo27.toolboks.util.gdxutils.isShiftDown
import java.awt.Desktop


class InfoStage(parent: UIElement<InfoScreen>?, camera: OrthographicCamera, val infoScreen: InfoScreen)
    : Stage<InfoScreen>(parent, camera) {

    private val main: RHREfreshApplication get() = infoScreen.main
    private val editor: Editor get() = infoScreen.editor

    private val loadingIcon: LoadingIcon<InfoScreen>
    private val dbVersionLabel: TextLabel<InfoScreen>
    private val versionLabel: TextLabel<InfoScreen>

    init {
        val palette = infoScreen.stage.palette
        val padding = 0.025f
        val buttonHeight = 0.1f
        val fontScale = 0.75f

        val info = this
        val buttonWidth = 0.4f
        // Loading icon for paddler
        loadingIcon = LoadingIcon(palette, info).apply {
            this.location.set(screenX = 1f - (padding + buttonWidth) - buttonWidth * 0.09f + buttonWidth,
                              screenY = 1f - (padding + buttonHeight * 0.8f) * 3,
                              screenWidth = buttonWidth * 0.09f,
                              screenHeight = buttonHeight * 0.8f)
            this.visible = true
            this.alpha = 0f
            this.renderType = ImageLabel.ImageRendering.ASPECT_RATIO
        }
        info.elements += loadingIcon
        // current program version
        versionLabel = object : TextLabel<InfoScreen>(palette, info, info) {
            private var clicks = 0
            private var timeSinceLastClick = System.currentTimeMillis()
            private val CLICKS_RESET = 3000L
            private val color = Color(1f, 1f, 1f, 1f)
            private val notes = listOf(0f, 2f, 4f, 5f, 7f)

            init {
                this.textColor = color
            }

            override fun canBeClickedOn(): Boolean = true

            override fun onLeftClick(xPercent: Float, yPercent: Float) {
                super.onLeftClick(xPercent, yPercent)
                if (System.currentTimeMillis() - timeSinceLastClick >= CLICKS_RESET) {
                    clicks = 0
                }
                AssetRegistry.get<Sound>("weird_sfx_bts_c").play(0.5f, Semitones.getALPitch(notes.getOrElse(clicks) { 0f }), 0f)
                clicks++
                timeSinceLastClick = System.currentTimeMillis()

                if (clicks >= 5) {
                    clicks = 0
                    main.screen = ScreenRegistry.getNonNull("advancedOptions")
                    AssetRegistry.get<Sound>("weird_sfx_bts_pew").play(0.5f)
                }
            }

            override fun render(screen: InfoScreen, batch: SpriteBatch, shapeRenderer: ShapeRenderer) {
                val alpha = ((1f - (System.currentTimeMillis() - timeSinceLastClick) / 1000f * 4f)).coerceIn(0f, 1f)
                color.set(1f, 1f, 1f, 1f).lerp(0f, 1f, 1f, 1f, alpha)
                super.render(screen, batch, shapeRenderer)
            }
        }.apply {
            this.location.set(screenX = 1f - (padding + buttonWidth),
                              screenY = 1f - (padding + buttonHeight * 0.8f) * 2,
                              screenWidth = buttonWidth,
                              screenHeight = buttonHeight * 0.8f)
            this.isLocalizationKey = false
            this.textWrapping = false
            this.text = RHREfresh.VERSION.toString()
        }
        info.elements += versionLabel
        dbVersionLabel = object : TextLabel<InfoScreen>(palette, info, info) {
            private var clicks = 0
            private var timeSinceLastClick = System.currentTimeMillis()
            private val color = Color(1f, 1f, 1f, 1f)
            private var resetTime = 0L

            init {
                this.textColor = color
            }

            override fun canBeClickedOn(): Boolean = true

            override fun onLeftClick(xPercent: Float, yPercent: Float) {
                super.onLeftClick(xPercent, yPercent)
                clicks++
                timeSinceLastClick = System.currentTimeMillis()

                AssetRegistry.get<Sound>("weird_sfx_honk").play(0.5f)
                LoadingIcon.usePaddlerAnimation = !LoadingIcon.usePaddlerAnimation
                main.preferences.putBoolean(PreferenceKeys.PADDLER_LOADING_ICON, LoadingIcon.usePaddlerAnimation).flush()
                resetTime = System.currentTimeMillis() + 6000L
            }

            override fun render(screen: InfoScreen, batch: SpriteBatch, shapeRenderer: ShapeRenderer) {
                val alpha = ((1f - (System.currentTimeMillis() - timeSinceLastClick) / 1000f * 4f)).coerceIn(0f, 1f)
                color.set(1f, 1f, 1f, 1f).lerp(1f, 0f, 0f, 1f, alpha)

                loadingIcon.alpha = ((resetTime - System.currentTimeMillis()) / 1000f * 2).coerceIn(0f, 1f)

                super.render(screen, batch, shapeRenderer)
            }
        }.apply {
            this.location.set(screenX = 1f - (padding + buttonWidth) + buttonWidth * 0.09f,
                              screenY = 1f - (padding + buttonHeight * 0.8f) * 3,
                              screenWidth = buttonWidth - buttonWidth * 0.09f * 2,
                              screenHeight = buttonHeight * 0.8f,
                              pixelX = 2f, pixelWidth = -4f)
            this.isLocalizationKey = false
            this.textWrapping = false
            this.text = "SFXDB VERSION"
        }
        info.elements += dbVersionLabel
        if (RHREfresh.CURRENT_OS != RHREfresh.OS.MACOS) {
            info.elements += Button(palette, info, info).apply {
                this.location.set(
                    screenX = 1f - (padding + buttonWidth),
                    screenY = 1f - (padding + buttonHeight * 0.8f) * 3,
                    screenWidth = buttonWidth * 0.085f,
                    screenHeight = buttonHeight * 0.8f
                )
                this.addLabel(ImageLabel(palette, this, this.stage).apply {
                    renderType = ImageLabel.ImageRendering.ASPECT_RATIO
                    image = TextureRegion(AssetRegistry.get<Texture>("ui_icon_folder"))
                })
                this.leftClickAction = { _, _ ->
                    Desktop.getDesktop().open(SFXDatabase.CUSTOM_SFX_FOLDER.file())
                }
                this.tooltipTextIsLocalizationKey = true
                this.tooltipText = "editor.customSfx.openFolder"
            }
        } else {

            info.elements += Button(palette, info, info).apply {
                this.location.set(
                    screenX = 1f - (padding + buttonWidth),
                    screenY = 1f - (padding + buttonHeight * 0.8f) * 3,
                    screenWidth = buttonWidth * 0.085f,
                    screenHeight = buttonHeight * 0.8f
                )
                this.addLabel(ImageLabel(palette, this, this.stage).apply {
                    renderType = ImageLabel.ImageRendering.ASPECT_RATIO
                    image = TextureRegion(AssetRegistry.get<Texture>("ui_icon_clipboard"))
                })
                this.leftClickAction = { _, _ ->
                    Gdx.app.clipboard.contents = SFXDatabase.CUSTOM_SFX_FOLDER.path()
                }
                this.tooltipTextIsLocalizationKey = true
                this.tooltipText = "editor.customSfx.copyFolder"
            }
        }

        // Donate button
        info.elements += Button(palette, info, info).apply {
            this.leftClickAction = { _, _ ->
                Gdx.net.openURI(RHREfresh.DONATION_URL)
            }
            addLabel(ImageLabel(palette, this, this.stage).apply {
                this.renderType = ImageLabel.ImageRendering.ASPECT_RATIO
                this.image = TextureRegion(AssetRegistry.get<Texture>("ui_icon_donate"))
            })

            this.location.set(screenX = 0.5f - (0.1f / 2),
                              screenY = padding,
                              screenWidth = 0.1f,
                              screenHeight = buttonHeight)
            this.visible = false
        }

        // info buttons
        // Credits
        info.elements += object : Button<InfoScreen>(palette, info, info) {
            init {
                addLabel(TextLabel(palette, this, this.stage).apply {
                    this.fontScaleMultiplier = fontScale
                    this.isLocalizationKey = true
                    this.textWrapping = false
                    this.text = "screen.info.credits"
                })
            }

            override fun onLeftClick(xPercent: Float, yPercent: Float) {
                super.onRightClick(xPercent, yPercent)

                main.screen = CreditsScreen(main)

                super.onLeftClick(xPercent, yPercent)
            }
        }.apply {
            this.location.set(screenX = 1f - (padding + buttonWidth),
                              screenY = padding,
                              screenWidth = buttonWidth,
                              screenHeight = buttonHeight)
        }


        // Open logs
        if (RHREfresh.CURRENT_OS != RHREfresh.OS.MACOS) {
            info.elements += object : Button<InfoScreen>(palette, info, info) {

                override fun onLeftClick(xPercent: Float, yPercent: Float) {
                    Desktop.getDesktop().open(RHREfresh.RHREFRESH_FOLDER.child("logs").file())
                }
            }.apply {
                addLabel(TextLabel(palette, this, this.stage).apply {
                    this.fontScaleMultiplier = fontScale
                    this.isLocalizationKey = true
                    this.textWrapping = false
                    this.textAlign = Align.center
                    this.text = "screen.info.openLogs"
                })

                this.location.set(screenX = padding,
                    screenY = padding * 4 + buttonHeight * 3,
                    screenWidth = buttonWidth,
                    screenHeight = buttonHeight)
            }
        } else {
            info.elements += object : Button<InfoScreen>(palette, info, info) {

                override fun onLeftClick(xPercent: Float, yPercent: Float) {
                    Gdx.app.clipboard.contents = RHREfresh.RHREFRESH_FOLDER.child("logs").path()
                }
            }.apply {
                addLabel(TextLabel(palette, this, this.stage).apply {
                    this.fontScaleMultiplier = fontScale
                    this.isLocalizationKey = true
                    this.textWrapping = false
                    this.textAlign = Align.center
                    this.text = "screen.info.copyLogs"
                })

                this.location.set(screenX = padding,
                    screenY = padding * 4 + buttonHeight * 3,
                    screenWidth = buttonWidth,
                    screenHeight = buttonHeight)
            }
        }


        // Editor version screen
        info.elements += Button(palette, info, info).apply {
            this.leftClickAction = { _, _ ->
                main.screen = ScreenRegistry.getNonNull("editorVersion")
            }
            addLabel(TextLabel(palette, this, this.stage).apply {
                this.fontScaleMultiplier = fontScale
                this.isLocalizationKey = true
                this.textWrapping = false
                this.text = "screen.info.version"
            })

            this.location.set(screenX = padding,
                              screenY = padding * 7 + buttonHeight * 6,
                              screenWidth = buttonWidth,
                              screenHeight = buttonHeight)
        }
        // Database version changelog
        info.elements += Button(palette, info, info).apply {
            this.leftClickAction = { _, _ ->
                Gdx.net.openURI(RHREfresh.DATABASE_RELEASES)
            }
            addLabel(TextLabel(palette, this, this.stage).apply {
                this.fontScaleMultiplier = fontScale
                this.isLocalizationKey = true
                this.textWrapping = false
                this.text = "screen.info.database"
            })

            this.location.set(screenX = padding,
                              screenY = padding * 6 + buttonHeight * 5,
                              screenWidth = buttonWidth,
                              screenHeight = buttonHeight)
        }
        info.elements += Button(palette, info, info).apply {
            this.leftClickAction = { _, _ ->
                Gdx.net.openURI(RHREfresh.DOCS_URL)
            }
            addLabel(TextLabel(palette, this, this.stage).apply {
                this.fontScaleMultiplier = fontScale
                this.isLocalizationKey = true
                this.textWrapping = false
                this.text = "screen.info.docs"
            })

            this.location.set(screenX = padding,
                              screenY = padding * 5 + buttonHeight * 4,
                              screenWidth = buttonWidth,
                              screenHeight = buttonHeight)
        }
    }

    fun show() {
        dbVersionLabel.text = Localization["screen.info.databaseVersion", "v${SFXDatabase.data.version}"]
        versionLabel.text = Localization["screen.info.programVersion", RHREfresh.VERSION.toString()]
    }

}