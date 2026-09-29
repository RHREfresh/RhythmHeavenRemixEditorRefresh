package io.github.chrislo27.rhrefresh.editor.stage.theme

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.rhrefresh.RHREfresh
import io.github.chrislo27.rhrefresh.editor.Editor
import io.github.chrislo27.rhrefresh.editor.stage.EditorStage
import io.github.chrislo27.rhrefresh.screen.EditorScreen
import io.github.chrislo27.rhrefresh.theme.LoadedThemes
import io.github.chrislo27.rhrefresh.theme.Theme
import io.github.chrislo27.rhrefresh.theme.Themes
import io.github.chrislo27.toolboks.registry.AssetRegistry
import io.github.chrislo27.toolboks.ui.*
import java.awt.Desktop


class ThemeChooserStage(val editor: Editor, val palette: UIPalette, parent: EditorStage, camera: OrthographicCamera, pixelsWidth: Float, pixelsHeight: Float)
    : Stage<EditorScreen>(parent, camera, pixelsWidth, pixelsHeight) {

    private val preferences: Preferences
        get() = editor.main.preferences

    val title: TextLabel<EditorScreen>
    val closeButton: Button<EditorScreen>
    private val chooserButtonBar: Stage<EditorScreen>
    private val themeList: ThemeListStage<Theme>
    val themeEditor: ThemeEditorStage

    init {
        this.elements += ColourPane(this, this).apply {
            this.colour.set(Editor.TRANSLUCENT_BLACK)
            this.colour.a = 0.8f
        }

        themeList = object : ThemeListStage<Theme>(editor, palette, this@ThemeChooserStage, this@ThemeChooserStage.camera, 362f, 318f) {
            override val itemList: List<Theme> get() = LoadedThemes.themes

            override fun getItemName(item: Theme): String = item.name
            override fun isItemNameLocalizationKey(item: Theme): Boolean = item.nameIsLocalization
            override fun getItemBgColor(item: Theme): Color = item.background
            override fun getItemLineColor(item: Theme): Color = item.trackLine
            override fun isItemSelected(item: Theme): Boolean = item == LoadedThemes.themes[LoadedThemes.index]
            override fun getBlueBarLimit(): Int = Themes.defaultThemes.size

            override fun onItemButtonSelected(leftClick: Boolean, realIndex: Int, buttonIndex: Int) {
                if (leftClick) {
                    LoadedThemes.index = realIndex
                    LoadedThemes.persistIndex(preferences)
                    editor.theme = LoadedThemes.currentTheme
                }
            }
        }.apply {
            location.set(screenX = 0f, screenY = 0f, screenWidth = 0f, screenHeight = 0f,
                         pixelX = 20f, pixelY = 87f, pixelWidth = 362f, pixelHeight = 318f)
        }
        this.elements += themeList

        closeButton = object : Button<EditorScreen>(palette, this, this.stage) {
            override fun onLeftClick(xPercent: Float, yPercent: Float) {
                super.onLeftClick(xPercent, yPercent)
                val chooserStage = editor.stage.themeChooserStage
                val wasVisible = chooserStage.visible
                editor.stage.paneLikeStages.forEach { it.visible = false }
                chooserStage.visible = !wasVisible
                if (chooserStage.visible) {
                    chooserStage.resetEverything()
                }
            }
        }.apply {
            this.location.set(screenX = 0f, screenWidth = 0f, screenY = 0f, screenHeight = 0f,
                              pixelX = 20f, pixelY = 50f, pixelWidth = 34f, pixelHeight = 34f)
            this.alignment = Align.topLeft
            this.addLabel(ImageLabel(palette, this, this.stage).apply {
                this.image = TextureRegion(AssetRegistry.get<Texture>("ui_search_clear"))
            })
        }
        this.elements += closeButton

        title = TextLabel(palette, this, this).apply {
            this.location.set(screenX = 0f, screenWidth = 1f, screenY = 0.875f, screenHeight = 0.125f)

            this.textAlign = Align.center
            this.textWrapping = false
            this.isLocalizationKey = true
            this.text = "editor.themeChooser.title"
            this.location.set(screenWidth = 0.95f, screenX = 0.025f)
        }
        this.elements += title

        themeEditor = ThemeEditorStage(editor, palette, this, this.camera, 362f, 392f).apply {
            this.location.set(screenX = 0f, screenY = 0f, screenWidth = 0f, screenHeight = 0f,
                              pixelX = 20f, pixelY = 13f, pixelWidth = 362f, pixelHeight = 392f)
            this.visible = false
        }
        this.elements += themeEditor

        chooserButtonBar = Stage(this, this.camera, 346f, 34f).apply {
            location.set(screenX = 0f, screenY = 0f, screenWidth = 0f, screenHeight = 0f,
                         pixelX = 20f, pixelY = 47f, pixelWidth = 346f, pixelHeight = 34f)
            this.elements += Button(palette, this, this.stage).apply {
                this.location.set(0f, 0f, 0f, 1f, 0f, 0f, 346f - 34f * 2f - 8f * 2f, 0f)
                this.addLabel(TextLabel(palette, this, this.stage).apply {
                    this.isLocalizationKey = true
                    this.text = "editor.themeEditor"
                    this.textWrapping = true
                    this.fontScaleMultiplier = 0.75f
                    this.location.set(pixelWidth = -4f, pixelX = 2f)
                })
                this.leftClickAction = { _, _ ->
                    themeEditor.state = ThemeEditorStage.State.ChooseTheme
                    themeEditor.update()
                    updateVisibility(true)
                }
            }

            this.elements += object : Button<EditorScreen>(palette, this, this.stage) {
                override fun onLeftClick(xPercent: Float, yPercent: Float) {
                    super.onLeftClick(xPercent, yPercent)
                    LoadedThemes.reloadThemes(preferences, false)
                    LoadedThemes.persistIndex(preferences)
                    editor.theme = LoadedThemes.currentTheme
                    themeList.apply {
                        buttonScroll = 0
                        resetButtons()
                    }
                }
            }.apply {
                this.location.set(0f, 0f, 0f, 1f, 346f - 34f * 2f - 8f, 0f, 34f, 0f)
                this.tooltipTextIsLocalizationKey = true
                this.tooltipText = "editor.themeChooser.reset"
                this.addLabel(ImageLabel(palette, this, this.stage).apply {
                    this.image = TextureRegion(AssetRegistry.get<Texture>("ui_icon_refresh"))
                })
            }

            if (RHREfresh.CURRENT_OS != RHREfresh.OS.MACOS) {
                this.elements += Button(palette, this, this.stage).apply {
                    this.location.set(0f, 0f, 0f, 1f, 346f - 34f, 0f, 34f, 0f)
                    this.addLabel(ImageLabel(palette, this, this.stage).apply {
                        this.renderType = ImageLabel.ImageRendering.ASPECT_RATIO
                        this.image = TextureRegion(AssetRegistry.get<Texture>("ui_icon_folder"))
                    })
                    this.leftClickAction = { _, _ ->
                        Desktop.getDesktop().open(LoadedThemes.THEMES_FOLDER.file())
                    }
                    this.tooltipTextIsLocalizationKey = true
                    this.tooltipText = "editor.themeEditor.openContainingFolder"
                }
            } else {
                this.elements += Button(palette, this, this.stage).apply {
                    this.location.set(0f, 0f, 0f, 1f, 346f - 34f, 0f, 34f, 0f)
                    this.addLabel(ImageLabel(palette, this, this.stage).apply {
                        this.renderType = ImageLabel.ImageRendering.ASPECT_RATIO
                        this.image = TextureRegion(AssetRegistry.get<Texture>("ui_icon_clipboard"))
                    })
                    this.leftClickAction = { _, _ ->
                        Gdx.app.clipboard.contents = LoadedThemes.THEMES_FOLDER.path()
                    }
                    this.tooltipTextIsLocalizationKey = true
                    this.tooltipText = "editor.themeEditor.copyContainingFolder"
                }
            }
        }
        this.elements += chooserButtonBar

        this.elements += object : Button<EditorScreen>(palette, this, this.stage) {
            override fun onLeftClick(xPercent: Float, yPercent: Float) {
                super.onLeftClick(xPercent, yPercent)
                val main = editor.main
                main.settings.themeUsesMenu = !main.settings.themeUsesMenu
                main.settings.persist()
            }
        }.apply {
            this.location.set(screenWidth= 0f, screenHeight = 0f, pixelX = 20f, pixelY = 7f, pixelHeight = 34f, pixelWidth = 346f)
            this.addLabel(TextLabel(palette, this, this.stage).apply {
                this.isLocalizationKey = true
                this.text = "editor.themeMenuBackgroundToggle"
                this.textWrapping = true
                this.fontScaleMultiplier = 0.75f
                this.location.set(pixelWidth = -4f, pixelX = 2f)
            })
        }
    }

    fun updateVisibility(inThemeEditor: Boolean) {
        themeEditor.visible = inThemeEditor
        chooserButtonBar.visible = !inThemeEditor
        closeButton.visible = !inThemeEditor
        themeList.visible = !inThemeEditor
    }
    
    fun resetEverything() {
        updateVisibility(false)
        themeList.resetButtons()
        title.text = "editor.themeChooser.title"
    }

}