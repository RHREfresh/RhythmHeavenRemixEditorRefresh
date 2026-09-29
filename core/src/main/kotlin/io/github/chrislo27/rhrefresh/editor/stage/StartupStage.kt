package io.github.chrislo27.rhrefresh.editor.stage

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import io.github.chrislo27.rhrefresh.PreferenceKeys
import io.github.chrislo27.rhrefresh.RHREfresh
import io.github.chrislo27.rhrefresh.screen.EditorScreen
import io.github.chrislo27.rhrefresh.stage.GenericStage
import io.github.chrislo27.rhrefresh.stage.TrueCheckbox
import io.github.chrislo27.toolboks.registry.AssetRegistry
import io.github.chrislo27.toolboks.ui.*


class StartupStage(val screen: EditorScreen) : Stage<EditorScreen>(screen.stage, screen.stage.camera) {

    val genericStage: GenericStage<EditorScreen> = GenericStage(screen.main.uiPalette, this, this.camera)

    init {
        genericStage.run {
            drawBackground = false
            titleLabel.text = "Welcome to RHRE!"
            titleLabel.isLocalizationKey = false
            titleIcon.image = TextureRegion(AssetRegistry.get<Texture>("logo_256"))
            backButton.visible = true
            onBackButtonClick = {
                Gdx.app.postRunnable {
                    (this@StartupStage.parent as Stage<EditorScreen>).elements.remove(this@StartupStage)
                }
            }
        }

        elements += ColourPane(this, this).apply {
            this.colour.set(0.5f, 0.5f, 0.5f, 0.75f)
        }
        elements += InputSponge(this, this).apply {
            this.shouldAbsorbInput = true
        }
        elements += genericStage

        val palette = screen.main.uiPalette
        genericStage.centreStage.elements += TextLabel(palette, genericStage.centreStage, genericStage.centreStage).apply {
            this.text = """Welcome to Rhythm Heaven Remix Editor Refresh! (RHREfresh for short.) I hope you can enjoy this tool made by many members of the Rhythm Heaven community.
                |
                |Please refer to the [CYAN]Online Documentation[], accessible in your web browser left-clicking the button below. You should start with the [CYAN]Starting a remix[] section.
                |
                |I encourage you to also make use of the various functionalities available,
|like News, Info and Settings, and Themes.
                |
                |Enjoy, and have fun remixing!
            """.trimMargin()
            this.isLocalizationKey = false
            this.textWrapping = true
            this.fontScaleMultiplier = 0.85f
        }
        genericStage.bottomStage.elements += Button(palette, genericStage.bottomStage, genericStage.bottomStage).apply {
            this.leftClickAction = { _, _ ->
                Gdx.net.openURI(RHREfresh.DOCS_URL)
            }
            addLabel(TextLabel(palette, this, this.stage).apply {
                this.isLocalizationKey = false
                this.textWrapping = false
                this.text = "Open online documentation\n[#8CB8FF]https://docs.rhre.dev[]"
            })
            this.location.set(0.275f, 0f, 0.45f, 1f)
        }
        genericStage.bottomStage.elements += TrueCheckbox(palette, genericStage.bottomStage, genericStage.bottomStage).apply {
            this.location.set(0.75f, 0f, 0.25f, 1f)
            this.textLabel.apply {
                this.isLocalizationKey = false
                this.textWrapping = false
                this.text = "Show on\nstart-up"
                this.fontScaleMultiplier = 0.9f
            }
            this.checked = screen.main.preferences.getBoolean(PreferenceKeys.SHOW_STARTUP_SCREEN, true)
            this.checkedStateChanged = {
                screen.main.preferences.putBoolean(PreferenceKeys.SHOW_STARTUP_SCREEN, it).flush()
            }
        }
    }

}