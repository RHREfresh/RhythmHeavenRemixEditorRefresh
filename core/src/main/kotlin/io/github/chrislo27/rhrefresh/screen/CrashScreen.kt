package io.github.chrislo27.rhrefresh.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.rhrefresh.PreferenceKeys
import io.github.chrislo27.rhrefresh.RHREfresh
import io.github.chrislo27.rhrefresh.RHREfreshApplication
import io.github.chrislo27.toolboks.Toolboks
import io.github.chrislo27.toolboks.ToolboksScreen
import io.github.chrislo27.toolboks.logging.SysOutPiper
import io.github.chrislo27.toolboks.ui.ImageLabel
import io.github.chrislo27.toolboks.ui.Stage
import io.github.chrislo27.toolboks.ui.TextLabel
import io.github.chrislo27.toolboks.ui.UIPalette
import java.io.PrintWriter
import java.io.StringWriter


class CrashScreen(main: RHREfreshApplication, val throwable: Throwable, val lastScreen: Screen?)
    : ToolboksScreen<RHREfreshApplication, CrashScreen>(main), HidesVersionText {

    private data class Splash(val title: String, val subtitle: String, val titleScale: Float = 1f, val alignment: Int = Align.left)

    companion object {
        private val splashes: List<Splash> = listOf(
                Splash("Rhythm League notes:", "You crashed near the end.", 0.85f),

                Splash("Hey baby, how's it going?", "Not very well.", 0.75f),
                Splash("Yo, it's crash time, huh?", "Maybe so, huh?", 0.8f),
                Splash("Wot!", "The program fell in a hole.", 1.5f, Align.center),
                Splash("And the correct response is...", "Aww, too bad! The program has crashed.",0.7f),

                Splash("S-crash-o, hey!", "I don't think you wanted the program to break (c'mon, ooh)."),
                Splash("AAAAAAAAAAAAAAAAAA", "Together now!"),
                Splash("It leaves me on my own,", "Oh, what can I do?", 0.85f),
                Splash("Crash inbound!", "Don't forget to take a break every once in a while! Ha! Just kidding!"),

                Splash("I'm a broken man...", "...I'm just a shattering storm..."),
                Splash("You can't do it like that, Mandrill.", "This is even more painful than it looks...", 0.6f),
                Splash("Wubba dubba dubba", "Is that a crash?"),
                Splash("Exported success-BOING!", "Donaiyanen!", 0.8f),

                Splash("Martian: \uE06B\uE06B\uE06B\uE06B\uE06B  \uE06B\uE06B", "Translator Tom: RHREfresh has crashed.", 0.75f),
                Splash("One who relies only on Kotlin will...", "Soon crash RHREfresh on something.", 0.55f),
                Splash("Oh, Tangotronic...", "PLEASE TAKE ME BACK DARLING I CAN DO BETTER 01110"),
                Splash("Pwaaaaah!", "I was just holding my breath to get rid of the crash.", 1f, Align.center),

                Splash("Ack! Is this a bug?!", "The developer tried very hard."),
                Splash("Chu-pa chu-pa BOW!", "You got crashed, huh?",0.95f),
                Splash("ah taka-OW!", "How could just grabbing sticks possibly improve my remixing abilities?"),
                Splash("Hmm... there seemed to be\na problem with RHREfresh.", "Beep!", 0.7f)
                                                   )
    }

    override val stage: Stage<CrashScreen> = Stage(null, main.defaultCamera, 1280f, 720f)

    private var crashIcon: Texture? = null

    init {
        val palette = main.uiPalette

        fun label(pal: UIPalette = palette, st: Stage<CrashScreen> = stage): TextLabel<CrashScreen> = TextLabel(pal, st, st).apply {
            this.isLocalizationKey = false
            this.textWrapping = false
        }

        val selectedSplash = splashes.random()

        stage.elements += label(palette.copy(ftfont = main.defaultFontLargeFTF)).apply {
            this.text = selectedSplash.title
            this.textAlign = selectedSplash.alignment
            this.fontScaleMultiplier = selectedSplash.titleScale
            if (selectedSplash.alignment == Align.center) {
                this.location.set(screenX = 0f, screenWidth = 1f /*0.75f*/, screenY = 0.8f, screenHeight = 0.2f)
            } else {
                this.location.set(screenX = 0.2f, screenWidth = 1f /*0.75f*/, screenY = 0.8f, screenHeight = 0.2f)
            }
        }
        try {
            val icon = Texture("images/icon/crash_icon.png")
            icon.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear)
            crashIcon = icon
            stage.elements += ImageLabel(palette, stage, stage).apply {
                this.image = TextureRegion(icon)
                this.renderType = ImageLabel.ImageRendering.ASPECT_RATIO
                this.location.set(screenX = -0.025f, screenY = 0.725f, screenWidth = .25f, screenHeight = 0.25f)
//                                  pixelWidth = 128f, pixelHeight = 128f, pixelX = -64f, pixelY = -64f)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toolboks.LOGGER.warn("Failed to load crash screen icon")
        }

        stage.elements += label().apply {
            this.location.set(screenY = 0.5f, screenHeight = 0.3f)
            this.fontScaleMultiplier = 0.9f
            this.text = "${selectedSplash.subtitle}\n\nThe program has crashed, but we're able to display this crash info screen.\nWe've attempted to save your remix (if any) and you should be able to recover it the next time\nyou start the program. " + (if (main.preferences.getBoolean(
                    PreferenceKeys.SETTINGS_ENABLE_ANALYTICS)) "An anonymous crash report has also been sent to the developer." else "") + "\nIf you can, take a screenshot of this screen as it contains useful info for the developer.\nConsider submitting a bug report at\n[#8CCFFF]${RHREfresh.GITHUB}/issues/new/choose[]."
        }

        stage.elements += label().apply {
            this.location.set(screenY = 0.05f, screenHeight = 0.45f, screenX = 0.1f, screenWidth = 0.875f)
            this.fontScaleMultiplier = 0.85f
            this.textAlign = Align.topLeft
            this.textWrapping = true
            this.text = "Last screen: ${lastScreen?.javaClass?.canonicalName}\nLog file: ${if (RHREfresh.portableMode) "./.rhre3/logs/" else "${RHREfresh.RHREFRESH_FOLDER}/logs"}/${SysOutPiper.logFile.name}\nException: [#FF6B68]${StringWriter().apply {
                val pw = PrintWriter(this)
                throwable.printStackTrace(pw)
                pw.flush()
            }.toString().replace("\t", "    ")}[]"
        }

        stage.elements += label().apply {
            this.location.set(screenWidth = 0.5f, screenHeight = 0.03333f, pixelHeight = 8f, pixelWidth = -6f)
            this.textAlign = Align.left
            this.fontScaleMultiplier = 0.75f
            this.text = "When you're done, you can close the program."
            this.background = true
        }

        stage.elements += label().apply {
            this.location.set(screenX = 0.5f, screenWidth = 0.5f, screenHeight = 0.03333f, pixelX = 6f, pixelWidth = -6f, pixelHeight = 8f)
            this.textAlign = Align.right
            this.fontScaleMultiplier = 0.75f
            this.text = RHREfresh.VERSION.toString()
            this.background = true
        }
    }

    override fun render(delta: Float) {
        if (main.batch.isDrawing) {
            main.batch.end()
        }
        super.render(delta)
    }

    override fun renderUpdate() {
        super.renderUpdate()
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
//            main.screen = ScreenRegistry["editor"]
        }
    }

    override fun tickUpdate() {
    }

    override fun dispose() {
        crashIcon?.dispose()
    }

}
