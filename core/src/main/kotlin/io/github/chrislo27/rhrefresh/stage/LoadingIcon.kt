package io.github.chrislo27.rhrefresh.stage

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import io.github.chrislo27.toolboks.ToolboksScreen
import io.github.chrislo27.toolboks.registry.AssetRegistry
import io.github.chrislo27.toolboks.ui.ImageLabel
import io.github.chrislo27.toolboks.ui.Stage
import io.github.chrislo27.toolboks.ui.UIPalette
import io.github.chrislo27.toolboks.util.MathHelper
import kotlin.random.Random


open class LoadingIcon<S : ToolboksScreen<*, *>>(private val palette: UIPalette, stage: Stage<S>)
    : Stage<S>(stage, stage.camera) {

    companion object {
        private const val VARIATIONS = 7
        private const val FRAMES = 5
        private const val SECONDS_PER_VARIATION = 1.25f
        private const val REGION_SIZE = 32

        private const val PADDLER_COLUMNS = 10
        private const val PADDLER_ROWS = 16
        private const val PADDLER_FRAMERATE = 25.0f
        private const val PADDLER_REGION_SIZE = 64

        @Volatile var usePaddlerAnimation: Boolean = true
    }

    private data class Variation(val index: Int, val weight: Int = 100)
    private val variations: List<Variation> = listOf(
        Variation(1), // Karate Joe
        Variation(2), // Hairy Onion
        Variation(3), // Electric Fish
        Variation(4), // Fillbot
        Variation(5), // Shoot em Up alien
        Variation(6), // Stepswitcher
        Variation(7), // Marshall!
        Variation(8), // Screwbot
        Variation(9), // Seal
        Variation(10), // Goat
        Variation(11), // Courtney
        Variation(12), // Lumbearjack cat
        Variation(13), // Hoop Trundler
        Variation(14), // Warping Alien
        Variation(15), // Thunder cloud
        Variation(16,50), // Cannery
        Variation(17,50), // Mountain Maneuver
        Variation(18,20), // RHRE2 clown
        Variation(19,20), // Glass Tapper
        Variation(20,50), // Happy Face
    )
    private var totalVariationWeight = 0

    var speed = 1f
    private var inited = false
    private val mainImage: ImageLabel<S> = ImageLabel(palette, this, this)
    private val penImage: ImageLabel<S> = ImageLabel(palette, this, this)
    private val paddlerImage: ImageLabel<S> = ImageLabel(palette, this, this)

    private var oldFrame = 0
    private var currentVariation = 1
    private var oldVariation = 0

    var renderType: ImageLabel.ImageRendering
        get() = mainImage.renderType
        set(value) {
            mainImage.renderType = value
            penImage.renderType = value
            paddlerImage.renderType = value
        }
    var alpha: Float = 1f
        set(value) {
            field = value
            mainImage.tint.a = value
            penImage.tint.a = value
            paddlerImage.tint.a = value
        }
    init {
        elements += mainImage
        elements += penImage
        elements += paddlerImage
    }

    override fun render(screen: S, batch: SpriteBatch, shapeRenderer: ShapeRenderer) {
        if (!inited) {
            mainImage.image = TextureRegion(AssetRegistry.get<Texture>("ui_loading_icon"))
            penImage.image = TextureRegion(AssetRegistry.get<Texture>("ui_loading_icon"))
            paddlerImage.image = TextureRegion(AssetRegistry.get<Texture>("ui_loading_paddler"))
            inited = true
        }

        if (usePaddlerAnimation) {
            val frameCount = PADDLER_ROWS * PADDLER_COLUMNS
            val currentFrame: Int = (MathHelper.getSawtoothWave(frameCount / PADDLER_FRAMERATE) * frameCount).toInt().coerceIn(0, frameCount - 1)
            paddlerImage.image?.also {img ->
                img.setRegion(PADDLER_REGION_SIZE * (currentFrame % PADDLER_COLUMNS), PADDLER_REGION_SIZE * (currentFrame / PADDLER_COLUMNS), PADDLER_REGION_SIZE, PADDLER_REGION_SIZE)
            }
            paddlerImage.visible = true
            mainImage.visible = false
            penImage.visible = false
        } else {
            val currentFrame: Int = (MathHelper.getSawtoothWave(SECONDS_PER_VARIATION / speed) * FRAMES).toInt().coerceIn(0, FRAMES - 1)
            if (currentFrame<oldFrame) {
                do {
                    currentVariation = randomVariation()
                } while(currentVariation == oldVariation)
                oldVariation = currentVariation
            }
            oldFrame = currentFrame
            mainImage.image?.also { img ->
                img.setRegion(REGION_SIZE * (currentFrame + 1), REGION_SIZE * (currentVariation), REGION_SIZE, REGION_SIZE)
            }
            penImage.image?.also { img ->
                img.setRegion(REGION_SIZE * (currentFrame + 1), 0, REGION_SIZE, REGION_SIZE)
            }
            paddlerImage.visible = false
            mainImage.visible = true
            penImage.visible = true
        }

        super.render(screen, batch, shapeRenderer)
    }

    fun randomVariation(): Int{
        if (totalVariationWeight == 0) {
            for((_, weight) in variations) {
                totalVariationWeight += weight
            }
        }
        val random = Random.nextInt(totalVariationWeight)
        var variationWeight = 0
        for((index, weight) in variations) {
            variationWeight += weight
            if (variationWeight>random) {
                return index
            }
        }
        return variations.first().index
    }

}
