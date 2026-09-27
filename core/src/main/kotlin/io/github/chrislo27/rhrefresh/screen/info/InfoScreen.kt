package io.github.chrislo27.rhrefresh.screen.info

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.rhrefresh.PreferenceKeys
import io.github.chrislo27.rhrefresh.RHREfresh
import io.github.chrislo27.rhrefresh.RHREfreshApplication
import io.github.chrislo27.rhrefresh.analytics.AnalyticsHandler
import io.github.chrislo27.rhrefresh.editor.Editor
import io.github.chrislo27.rhrefresh.screen.*
import io.github.chrislo27.rhrefresh.soundsystem.BeadsSoundSystem
import io.github.chrislo27.rhrefresh.stage.GenericStage
import io.github.chrislo27.toolboks.Toolboks
import io.github.chrislo27.toolboks.ToolboksScreen
import io.github.chrislo27.toolboks.i18n.Localization
import io.github.chrislo27.toolboks.registry.AssetRegistry
import io.github.chrislo27.toolboks.registry.ScreenRegistry
import io.github.chrislo27.toolboks.ui.*
import io.github.chrislo27.toolboks.util.gdxutils.isAltDown
import io.github.chrislo27.toolboks.util.gdxutils.isControlDown
import io.github.chrislo27.toolboks.util.gdxutils.isShiftDown


class InfoScreen(main: RHREfreshApplication)
    : ToolboksScreen<RHREfreshApplication, InfoScreen>(main), HidesVersionText {

    companion object {
        const val DEFAULT_AUTOSAVE_TIME = 5
        val autosaveTimers = listOf(0, 1, 2, 3, 4, 5, 10, 15)
        var glowButtonInEditor: Boolean = true
            private set
    }

    enum class Page(val heading: String) {
        INFO("screen.info.info"), AUDIO_SETTINGS("screen.info.audio.settings"), PROGRAM_SETTINGS("screen.info.program.settings"), VISUAL_SETTINGS("screen.info.visual.settings"), EXTRAS("screen.info.extras");

        companion object {
            val VALUES = values().toList()
        }
    }

    val preferences: Preferences
        get() = main.preferences
    val editor: Editor
        get() = ScreenRegistry.getNonNullAsType<EditorScreen>("editor").editor

    private var backgroundOnly = false
    private var currentPage: Page = Page.PROGRAM_SETTINGS
        set(value) {
            field = value
            pageStages.forEach { it.visible = false }
            when (value) {
                Page.INFO -> {
                    infoStage.visible = true
                }
                Page.AUDIO_SETTINGS -> {
                    audioSettingsStage.visible = true
                }
                Page.PROGRAM_SETTINGS -> {
                    programSettingsStage.visible = true
                }
                Page.VISUAL_SETTINGS -> {
                    visualSettingsStage.visible = true
                }
                Page.EXTRAS -> {
                    extrasStage.visible = true
                }
            }
            headingLabel.text = value.heading
            leftPageButton.visible = false
            rightPageButton.visible = false
            val index = Page.VALUES.indexOf(value)
            if (index > 0) {
                leftPageButton.run {
                    visible = true
                    targetPage = Page.VALUES[index - 1]
                    label.text = targetPage.heading
                }
            }
            if (index < Page.VALUES.size - 1) {
                rightPageButton.run {
                    visible = true
                    targetPage = Page.VALUES[index + 1]
                    label.text = targetPage.heading
                }
            }
        }
    override val hidesVersionText: Boolean
        get() = currentPage == Page.INFO
    override val stage: GenericStage<InfoScreen> = GenericStage(main.uiPalette, null, main.defaultCamera)

    private val audioSettingsStage: AudioSettingsStage
    private val programSettingsStage: ProgramSettingsStage
    private val visualSettingsStage: VisualSettingsStage
    private val infoStage: InfoStage
    private val extrasStage: ExtrasStage
    
    private val pageStages: List<Stage<InfoScreen>>
    private val leftPageButton: PageChangeButton
    private val rightPageButton: PageChangeButton
    private val headingLabel: TextLabel<InfoScreen>
    private val onlineLabel: TextLabel<InfoScreen>

    var lockKeys = false
    var makeDisappears = false

    init {
        val palette = stage.palette

        stage.titleIcon.apply {
            this.image = TextureRegion(AssetRegistry.get<Texture>("ui_icon_small_gear"))
        }
        stage.titleLabel.apply {
            this.text = "editor.info"
        }
        stage.backButton.visible = true
        stage.onBackButtonClick = {
            main.screen = ScreenRegistry.getNonNull("editor")
        }

        stage.bottomStage.elements += object : Button<InfoScreen>(palette, stage.bottomStage, stage.bottomStage) {
            override fun onLeftClick(xPercent: Float, yPercent: Float) {
                super.onLeftClick(xPercent, yPercent)
                Gdx.net.openURI(RHREfresh.GITHUB)
            }
        }.apply {
            this.addLabel(TextLabel(palette, this, this.stage).apply {
                fun updateText() {
                    this.text = Localization["screen.info.github", RHREfresh.GITHUB]
                }

                this.isLocalizationKey = false
                this.textWrapping = false
                updateText()
                this.fontScaleMultiplier = 0.9f

                Localization.addListener { updateText() }
            })

            this.location.set(screenX = 0.175f, screenWidth = 0.65f)
        }


        onlineLabel = object : TextLabel<InfoScreen>(palette, stage.bottomStage, stage.bottomStage) {
            var last = Int.MIN_VALUE

            init {
                Localization.addListener {
                    last = Int.MIN_VALUE
                }
            }

            override fun render(screen: InfoScreen, batch: SpriteBatch, shapeRenderer: ShapeRenderer) {
                val current = main.liveUsers
                if (last != current) {
                    last = current
                    this.text = if (current > 0) Localization["screen.info.online", current] else ""
                }
                super.render(screen, batch, shapeRenderer)
            }
        }.apply {
            this.isLocalizationKey = false
            this.textAlign = Align.right
            this.fontScaleMultiplier = 0.5f
            this.alignment = Align.bottomRight
            this.location.set(screenHeight = 1f / 3,
                              screenWidth = this.stage.percentageOfWidth(this.stage.location.realHeight))
            this.location.set(screenX = this.location.screenWidth + 0.025f * 1.25f, screenY = -0.75f + 1f / 3)
        }
        stage.bottomStage.elements += onlineLabel
        stage.bottomStage.elements += object : Button<InfoScreen>(palette, stage.bottomStage, stage.bottomStage) {
            override var visible: Boolean = true
                get() = field && main.liveUsers > 0 && !RHREfresh.noOnlineCounter

            override fun onLeftClick(xPercent: Float, yPercent: Float) {
                super.onLeftClick(xPercent, yPercent)
                if (!visible) return
                main.screen = OnlineCounterScreen(main, onlineLabel.text)
            }
        }.apply {
            this.addLabel(ImageLabel(palette, this, this.stage).apply {
                this.renderType = ImageLabel.ImageRendering.ASPECT_RATIO
                this.image = TextureRegion(AssetRegistry.get<Texture>("ui_icon_history"))
            })
            this.alignment = Align.bottomRight
            this.location.set(screenHeight = onlineLabel.location.screenHeight, screenY = onlineLabel.location.screenY)
            this.location.set(screenX = 0.025f, screenWidth = 0.025f)
        }

        infoStage = InfoStage(stage.centreStage, stage.camera, this)
        audioSettingsStage = AudioSettingsStage(stage.centreStage, stage.camera, this)
        programSettingsStage = ProgramSettingsStage(stage.centreStage, stage.camera, this)
        visualSettingsStage = VisualSettingsStage(stage.centreStage, stage.camera, this)
        extrasStage = ExtrasStage(stage.centreStage, stage.camera, this)

        val padding = 0.025f
        val buttonHeight = 0.1f
        val fontScale = 0.75f
        stage.centreStage.also { centre ->
            val buttonWidth = 0.35f
            headingLabel = TextLabel(palette, centre, centre).apply {
                val width = 1f - (buttonWidth * 1.85f)
                this.location.set(screenX = 0.5f - width / 2f,
                                  screenY = 1f - (padding + buttonHeight * 0.8f),
                                  screenWidth = width,
                                  screenHeight = buttonHeight)
                this.isLocalizationKey = true
                this.text = "screen.info.program.settings"
            }
            centre.elements += headingLabel

            leftPageButton = PageChangeButton(palette, centre, centre, false).apply {
                this.location.set(0f, 1f - (padding + buttonHeight * 0.8f), buttonWidth * 0.75f, buttonHeight)
            }
            centre.elements += leftPageButton
            rightPageButton = PageChangeButton(palette, centre, centre, true).apply {
                this.location.set(1f - (buttonWidth * 0.75f), 1f - (padding + buttonHeight * 0.8f), buttonWidth * 0.75f, buttonHeight)
            }
            centre.elements += rightPageButton
        }
        
        pageStages = listOf(infoStage, audioSettingsStage, programSettingsStage, visualSettingsStage, extrasStage).onEach {
            stage.centreStage.elements += it
        }
        stage.updatePositions()
        currentPage = currentPage // force update
    }

    override fun render(delta: Float) {
        super.render(delta)
        if (makeDisappears) {
            val batch = main.batch
            batch.begin()
            GenericStage.backgroundImpl.render(main.defaultCamera, batch, main.shapeRenderer, 0f)
            batch.end()
        }
    }

    override fun renderUpdate() {
        super.renderUpdate()
        if (!lockKeys) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && stage.backButton.visible && stage.backButton.enabled) {
                stage.onBackButtonClick()
            } else if (!Gdx.input.isShiftDown() && !Gdx.input.isAltDown()) {
                if (Gdx.input.isControlDown()) {
                    if (Gdx.input.isKeyJustPressed(Input.Keys.A)) {
                        main.screen = ScreenRegistry.getNonNull("advancedOptions")
                    }
                } else {
                    if (Gdx.input.isKeyJustPressed(Input.Keys.A) || Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
                        if (leftPageButton.visible)
                            leftPageButton.onLeftClick(0f, 0f)
                    }
                    if (Gdx.input.isKeyJustPressed(Input.Keys.D) || Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
                        if (rightPageButton.visible)
                            rightPageButton.onLeftClick(0f, 0f)
                    }
                }
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.Q) && Gdx.input.isKeyPressed(Toolboks.DEBUG_KEY)) {
                backgroundOnly = !backgroundOnly
            }
        }
    }

    override fun show() {
        super.show()
        infoStage.show()
        audioSettingsStage.show()
        programSettingsStage.show()
    }

    override fun hide() {
        super.hide()

        audioSettingsStage.hide()

        // Analytics
        if (programSettingsStage.didChangeSettings) {
            val map: Map<String, *> = preferences.get()
            AnalyticsHandler.track("Exit Info and Settings",
                                   mapOf(
                                           "settings" to PreferenceKeys.allSettingsKeys.associate {
                                               it.replace("settings_", "") to (map[it] ?: "null")
                                           } + mapOf("background" to map[PreferenceKeys.BACKGROUND], "defaultMixer" to BeadsSoundSystem.getDefaultMixer().mixerInfo.name)
                                        ))
        }
        programSettingsStage.didChangeSettings = false
    }

    override fun getDebugString(): String? {
        return "CTRL+A - Open Advanced Options\n${Input.Keys.toString(Toolboks.DEBUG_KEY)}+Q - Render background on top"
    }

    override fun tickUpdate() {
    }

    override fun dispose() {
    }

    inner class PageChangeButton(palette: UIPalette, parent: UIElement<InfoScreen>, stage: Stage<InfoScreen>, right: Boolean)
        : Button<InfoScreen>(palette, parent, stage) {

        var targetPage: Page = Page.INFO
        val label: TextLabel<InfoScreen> = TextLabel(palette, this, this.stage).apply {
            this.location.set(screenX = 0.15f, screenWidth = 0.85f)
            this.isLocalizationKey = true
            this.textAlign = if (right) Align.right else Align.left
            this.fontScaleMultiplier = 0.75f
            this.text = "screen.info.program.settings"
        }

        init {
            addLabel(label)
            if (right) {
                label.location.screenX = 0f
                addLabel(TextLabel(palette, this, this.stage).apply {
                    this.location.set(screenX = 0.85f, screenWidth = 0.15f)
                    this.isLocalizationKey = false
                    this.text = "\uE14A"
                })
            } else {
                addLabel(TextLabel(palette, this, this.stage).apply {
                    this.location.set(screenX = 0f, screenWidth = 0.15f)
                    this.isLocalizationKey = false
                    this.text = "\uE149"
                })
            }

            leftClickAction = { _, _ ->
                currentPage = targetPage
            }
        }
    }
}