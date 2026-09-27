package io.github.chrislo27.rhrefresh.screen.info

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.rhrefresh.PreferenceKeys
import io.github.chrislo27.rhrefresh.RHREfreshApplication
import io.github.chrislo27.rhrefresh.VersionHistory
import io.github.chrislo27.rhrefresh.editor.CameraBehaviour
import io.github.chrislo27.rhrefresh.editor.Editor
import io.github.chrislo27.rhrefresh.editor.view.ViewType
import io.github.chrislo27.rhrefresh.stage.FalseCheckbox
import io.github.chrislo27.rhrefresh.stage.GenericStage
import io.github.chrislo27.rhrefresh.stage.TrueCheckbox
import io.github.chrislo27.rhrefresh.stage.bg.Background
import io.github.chrislo27.toolboks.i18n.Localization
import io.github.chrislo27.toolboks.registry.AssetRegistry
import io.github.chrislo27.toolboks.registry.ScreenRegistry
import io.github.chrislo27.toolboks.ui.*
import io.github.chrislo27.toolboks.version.Version


class VisualSettingsStage(parent: UIElement<InfoScreen>?, camera: OrthographicCamera, val infoScreen: InfoScreen)
    : Stage<InfoScreen>(parent, camera) {

    private val main: RHREfreshApplication get() = infoScreen.main
    private val preferences: Preferences get() = infoScreen.preferences
    private val editor: Editor get() = infoScreen.editor
    var didChangeSettings: Boolean = Version.fromStringOrNull(preferences.getString(PreferenceKeys.LAST_VERSION, ""))?.let {
        !it.isUnknown && (it < VersionHistory.ANALYTICS || it < VersionHistory.RE_ADD_STRETCHABLE_TEMPO)
    } ?: false

    private var backgroundOnly = false
    private val menuBgButton: Button<InfoScreen>


    init {
        val palette = infoScreen.stage.palette
        val padding = 0.025f
        val buttonHeight = 0.1f
        val fontScale = 0.75f
        val settings = this
        val buttonWidth = 0.45f
        // Settings
        // Smooth dragging
        settings.elements += TrueCheckbox(palette, settings, settings).apply {
            this.checked = main.settings.smoothDragging
            this.textLabel.apply {
                this.fontScaleMultiplier = fontScale
                this.isLocalizationKey = true
                this.textWrapping = false
                this.textAlign = Align.left
                this.text = "screen.info.smoothDragging"
            }
            this.leftClickAction = { _, _ ->
                main.settings.smoothDragging = checked
                main.settings.persist()
                didChangeSettings = true
            }
            this.location.set(
                screenX = padding,
                screenY = padding * 7 + buttonHeight * 6,
                screenWidth = buttonWidth,
                screenHeight = buttonHeight
            )
        }

        // Minimap preview
        settings.elements += object : TrueCheckbox<InfoScreen>(palette, settings, settings) {
            private var bufferSupported = true

            override fun render(screen: InfoScreen, batch: SpriteBatch, shapeRenderer: ShapeRenderer) {
                if (bufferSupported && !editor.stage.minimap.bufferSupported) {
                    bufferSupported = false
                    textLabel.text = "screen.info.minimapPreview.unsupported"
                    textLabel.fontScaleMultiplier = fontScale * fontScale
                    checked = false
                }
                enabled = bufferSupported && !main.settings.disableMinimap

            }

            override fun onLeftClick(xPercent: Float, yPercent: Float) {
                super.onLeftClick(xPercent, yPercent)
                if (bufferSupported) {
                    main.settings.minimapPreview = checked
                    main.settings.persist()
                    didChangeSettings = true
                } else {
                    main.settings.minimapPreview = false
                    main.settings.persist()
                    preferences.putString(PreferenceKeys.SETTINGS_MINIMAP_PREVIEW, null).flush()
                }
            }
        }.apply {
            this.checked = main.settings.minimapPreview

            this.textLabel.apply {
                this.fontScaleMultiplier = fontScale
                this.isLocalizationKey = true
                this.textAlign = Align.left
                this.text = "screen.info.minimapPreview"
            }

            this.location.set(
                screenX = padding,
                screenY = padding * 6 + buttonHeight * 5,
                screenWidth = buttonWidth,
                screenHeight = buttonHeight
            )
        }

        // Disable minimap
        settings.elements += FalseCheckbox(palette, settings, settings).apply {
            this.checked = main.settings.disableMinimap
            this.textLabel.apply {
                this.fontScaleMultiplier = fontScale
                this.isLocalizationKey = true
                this.textWrapping = false
                this.textAlign = Align.left
                this.text = "screen.info.disableMinimap"
            }
            this.leftClickAction = { _, _ ->
                main.settings.disableMinimap = checked
                main.settings.persist()
                didChangeSettings = true
            }
            this.location.set(
                screenX = padding,
                screenY = padding * 5 + buttonHeight * 4,
                screenWidth = buttonWidth,
                screenHeight = buttonHeight
            )
        }

        // Chase camera
        settings.elements += object : Button<InfoScreen>(palette, settings, settings) {
            private val label: TextLabel<InfoScreen> = TextLabel(palette, this, this.stage).apply {
                this.isLocalizationKey = false
                this.text = ""
                this.textWrapping = false
                this.fontScaleMultiplier = fontScale
                this.location.set(pixelX = 2f, pixelWidth = -4f)
                addLabel(this)
            }

            private fun updateText() {
                label.text =
                    Localization["screen.info.cameraBehaviour", Localization[main.settings.cameraBehaviour.localizationKey]]
            }

            private fun cycle(dir: Int) {
                val values = CameraBehaviour.VALUES
                val index = values.indexOf(main.settings.cameraBehaviour) + dir
                val normalized = if (index < 0) values.size - 1 else if (index >= values.size) 0 else index
                main.settings.cameraBehaviour = values[normalized]
                if (dir != 0) {
                    main.settings.persist()
                    didChangeSettings = true
                }
                updateText()
            }

            override fun onLeftClick(xPercent: Float, yPercent: Float) {
                super.onLeftClick(xPercent, yPercent)
                cycle(1)
            }

            override fun onRightClick(xPercent: Float, yPercent: Float) {
                super.onRightClick(xPercent, yPercent)
                cycle(-1)
            }

            init {
                Localization.addListener { updateText() }
                updateText()
            }
        }.apply {
            this.location.set(
                screenX = padding,
                screenY = padding * 4 + buttonHeight * 3,
                screenWidth = buttonWidth,
                screenHeight = buttonHeight
            )
        }


        // Subtitle order
        settings.elements += object : TrueCheckbox<InfoScreen>(palette, settings, settings) {
            override fun onLeftClick(xPercent: Float, yPercent: Float) {
                super.onLeftClick(xPercent, yPercent)
                main.settings.subtitlesBelow = checked
                main.settings.persist()
                didChangeSettings = true
            }
        }.apply {
            this.checked = main.settings.subtitlesBelow

            this.textLabel.apply {
                this.fontScaleMultiplier = fontScale * 0.9f
                this.isLocalizationKey = true
                this.textWrapping = false
                this.textAlign = Align.left
                this.text = "screen.info.subtitleOrder"
            }

            this.location.set(
                screenX = padding,
                screenY = padding * 6 + buttonHeight * 5,
                screenWidth = buttonWidth,
                screenHeight = buttonHeight
            )
        }

        // Glass entities
        settings.elements += object : TrueCheckbox<InfoScreen>(palette, settings, settings) {
            private var bufferSupported = true

            override fun render(screen: InfoScreen, batch: SpriteBatch, shapeRenderer: ShapeRenderer) {
                if (bufferSupported && !editor.glassEffect.fboSupported) {
                    bufferSupported = false
                    textLabel.text = "screen.info.glassEntities.unsupported"
                    textLabel.fontScaleMultiplier = fontScale * fontScale
                    checked = false
                    enabled = false
                }

                super.render(screen, batch, shapeRenderer)
            }

            override fun onLeftClick(xPercent: Float, yPercent: Float) {
                super.onLeftClick(xPercent, yPercent)
                if (bufferSupported) {
                    main.settings.glassEntities = checked
                    main.settings.persist()
                    didChangeSettings = true
                } else {
                    main.settings.glassEntities = false
                    main.settings.persist()
                    preferences.putString(PreferenceKeys.SETTINGS_GLASS_ENTITIES, null).flush()
                }
            }
        }.apply {
            this.checked = main.settings.glassEntities
            this.tooltipText = "screen.info.glassEntities.tooltip"
            this.tooltipTextIsLocalizationKey = true

            this.textLabel.apply {
                this.fontScaleMultiplier = fontScale
                this.isLocalizationKey = true
                this.textWrapping = false
                this.textAlign = Align.left
                this.text = "screen.info.glassEntities"
            }

            this.location.set(
                screenX = 1f - (padding + buttonWidth),
                screenY = padding * 6 + buttonHeight * 5,
                screenWidth = buttonWidth,
                screenHeight = buttonHeight
            )
        }

        // Game Boundaries
        settings.elements += object : TrueCheckbox<InfoScreen>(palette, settings, settings) {

            override fun onLeftClick(xPercent: Float, yPercent: Float) {
                super.onLeftClick(xPercent, yPercent)
                if (checked) {
                    editor.views.add(ViewType.GAME_BOUNDARIES)
                } else {
                    editor.views.remove(ViewType.GAME_BOUNDARIES)
                }
                main.settings.gameBoundaries = checked
                main.settings.persist()
                didChangeSettings = true

            }
        }.apply {
            this.checked = main.settings.gameBoundaries

            this.textLabel.apply {
                this.fontScaleMultiplier = fontScale
                this.isLocalizationKey = true
                this.textAlign = Align.left
                this.text = "editor.view.gameBoundaries"
            }

            this.location.set(
                screenX = 1f - (padding + buttonWidth),
                screenY = padding * 5 + buttonHeight * 4,
                screenWidth = buttonWidth,
                screenHeight = buttonHeight
            )
        }


        // Fullscreen
        settings.elements += object: Button<InfoScreen>(palette, settings, settings) {
            private val label: TextLabel<InfoScreen> = TextLabel(palette, this, this.stage).apply {
                this.isLocalizationKey = true
                this.text = "editor.unfullscreen"
                this.textWrapping = false
                this.fontScaleMultiplier = fontScale * 0.9f
                this.location.set(pixelX = 2f, pixelWidth = -4f)
                addLabel(this)
            }

            override fun onLeftClick(xPercent: Float, yPercent: Float) {
                super.onLeftClick(xPercent, yPercent)
                if (Gdx.graphics.isFullscreen) {
                    editor.main.attemptEndFullscreen()
                    label.text = "editor.unfullscreen"
                } else {
                    editor.main.attemptFullscreen()
                    label.text = "editor.fullscreen"
                }
                editor.main.persistWindowSettings()
            }
        }.apply{
            this.location.set(
                screenX = 1f - (padding + buttonWidth),
                screenY = padding * 7 + buttonHeight * 6,
                screenWidth = buttonWidth / 2 - padding / 2,
                screenHeight = buttonHeight
            )
        }

        //Reset view
        settings.elements += object: Button<InfoScreen>(palette, settings, settings) {
            private val label: TextLabel<InfoScreen> = TextLabel(palette, this, this.stage).apply {
                this.isLocalizationKey = true
                this.text = "editor.resetwindow"
                this.textWrapping = false
                this.fontScaleMultiplier = fontScale
                this.location.set(pixelX = 2f, pixelWidth = -4f)
                addLabel(this)
            }

            override fun onLeftClick(xPercent: Float, yPercent: Float) {
                super.onLeftClick(xPercent, yPercent)
                editor.main.attemptResetWindow()
                editor.main.persistWindowSettings()
            }
        }.apply{
            this.location.set(
                screenX = 1f - (padding + buttonWidth)/2,
                screenY = padding * 7 + buttonHeight * 6,
                screenWidth = buttonWidth / 2 - padding / 2,
                screenHeight = buttonHeight
            )
        }

        // Menu theme
        menuBgButton = object : Button<InfoScreen>(palette, settings, settings) {
            val paletteLabel = ImageLabel(palette, this, this.stage).apply {
                this.image = TextureRegion(AssetRegistry.get<Texture>("ui_icon_palette"))
                this.renderType = ImageLabel.ImageRendering.ASPECT_RATIO
                this.location.set(screenX = -(buttonWidth))
            }
            val nameLabel = TextLabel(palette.copy(ftfont = main.defaultBorderedFontFTF), this, this.stage).apply {
                this.textAlign = Align.left
                this.isLocalizationKey = false
                this.fontScaleMultiplier = fontScale
                this.textWrapping = false
                this.location.set(screenX = 0.1f)
            }

            override fun onLeftClick(xPercent: Float, yPercent: Float) {
                super.onLeftClick(xPercent, yPercent)
                cycle(1)
                hoverTime = 0f
            }

            override fun onRightClick(xPercent: Float, yPercent: Float) {
                super.onRightClick(xPercent, yPercent)
                cycle(-1)
                hoverTime = 0f
            }

            fun cycle(dir: Int) {
                val values = Background.backgrounds
                if (dir > 0) {
                    val index = values.indexOf(GenericStage.backgroundImpl) + 1
                    GenericStage.backgroundImpl = if (index >= values.size) {
                        values.first()
                    } else {
                        values[index]
                    }
                } else if (dir < 0) {
                    val index = values.indexOf(GenericStage.backgroundImpl) - 1
                    GenericStage.backgroundImpl = if (index < 0) {
                        values.last()
                    } else {
                        values[index]
                    }
                }

                nameLabel.text = "Menu theme: ${Background.backgroundMapByBg[GenericStage.backgroundImpl]?.name}"

                main.preferences.putString(PreferenceKeys.BACKGROUND, GenericStage.backgroundImpl.id).flush()
            }
        }.apply {
            this.addLabel(paletteLabel)
            this.addLabel(nameLabel)

            this.cycle(0)

            this.location.set(
                screenX = 1f - (padding + buttonWidth),
                screenY = padding * 2 + buttonHeight,
                screenWidth = buttonWidth,
                screenHeight = buttonHeight
            )
        }
        settings.elements += menuBgButton

        // Menu theme
        settings.elements += object : Button<InfoScreen>(palette, settings, settings) {
            val paletteLabel = ImageLabel(palette, this, this.stage).apply {
                this.image = TextureRegion(AssetRegistry.get<Texture>("ui_icon_palette"))
                this.renderType = ImageLabel.ImageRendering.ASPECT_RATIO
                this.location.set(screenX = -(buttonWidth))
            }
            val nameLabel = TextLabel(palette.copy(ftfont = main.defaultBorderedFontFTF), this, this.stage).apply {
                this.textAlign = Align.left
                this.isLocalizationKey = false
                this.fontScaleMultiplier = fontScale
                this.textWrapping = false
                this.location.set(screenX = 0.1f)
            }

            override fun onLeftClick(xPercent: Float, yPercent: Float) {
                super.onLeftClick(xPercent, yPercent)
                main.screen = ScreenRegistry.getNonNull("editor")
                val chooserStage = editor.stage.themeChooserStage
                val wasVisible = chooserStage.visible
                editor.stage.paneLikeStages.forEach { it.visible = false }
                chooserStage.visible = !wasVisible
                if (chooserStage.visible) {
                    chooserStage.resetEverything()
                }
            }

            fun cycle(dir: Int) {
                val values = Background.backgrounds
                if (dir > 0) {
                    val index = values.indexOf(GenericStage.backgroundImpl) + 1
                    GenericStage.backgroundImpl = if (index >= values.size) {
                        values.first()
                    } else {
                        values[index]
                    }
                } else if (dir < 0) {
                    val index = values.indexOf(GenericStage.backgroundImpl) - 1
                    GenericStage.backgroundImpl = if (index < 0) {
                        values.last()
                    } else {
                        values[index]
                    }
                }

                nameLabel.text = "Open the editor theme menu"

//                main.preferences.putString(PreferenceKeys.BACKGROUND, GenericStage.backgroundImpl.id).flush()
            }
        }.apply {
            this.addLabel(paletteLabel)
            this.addLabel(nameLabel)

            this.tooltipText = "screen.info.glassEntities.tooltip"
            this.tooltipTextIsLocalizationKey = false

            this.cycle(0)

            this.location.set(
                screenX = 1f - (padding + buttonWidth),
                screenY = padding,
                screenWidth = buttonWidth,
                screenHeight = buttonHeight
            )
        }

    }

    override fun render(screen: InfoScreen, batch: SpriteBatch, shapeRenderer: ShapeRenderer) {
        super.render(screen, batch, shapeRenderer)
        if (backgroundOnly || menuBgButton.hoverTime >= 1.5f) {
            infoScreen.makeDisappears = true
        } else {
            infoScreen.makeDisappears = false
        }
    }

}