package io.github.chrislo27.rhrefresh.stage.bg

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import io.github.chrislo27.rhrefresh.stage.bg.Background.Companion.BgData
import io.github.chrislo27.toolboks.registry.AssetRegistry


abstract class Background(val id: String) {
    
    abstract fun render(camera: OrthographicCamera, batch: SpriteBatch, shapeRenderer: ShapeRenderer, delta: Float)
    
    companion object {
        data class BgData(val bg: Background, val name: String)
        
        val backgroundsData: List<BgData> by lazy {
            listOf(
                    BgData(TengokuBackground("tengoku"), "menuTheme.gbaMenu"),
                    BgData(KarateManBackground("karateMan"), "menuTheme.karateMan"),
                    BgData(TilingBackground("rhdsPolkaDots", 5f,
                                            speedX = 3f, speedY = -3f, widthCoeff = 0.5f, heightCoeff = 0.5f)
                           { AssetRegistry["bg_polkadot"] }, "menuTheme.dsMenu"),
                    BgData(SpaceDanceBackground("spaceDance"), "menuTheme.spaceDance"),
                    BgData(RetroBackground("retro"), "menuTheme.retro"),
                    BgData(TilingBackground("tapTrial", 5f, speedX = 0f, speedY = 1f) { AssetRegistry["bg_tapTrial"] }, "menuTheme.tapTrial"),
                    BgData(TilingBackground("tiled", 5f, speedX = 1f, speedY = 1f) { AssetRegistry["bg_tile"] }, "menuTheme.notes"),
                    BgData(LaunchPartyBackground("launchParty"), "menuTheme.launchParty"),
                    BgData(KittiesBackground("kitties"), "menuTheme.kitties"),
                    BgData(SeesawBackground("seesaw"), "menuTheme.seeSaw"),
                    BgData(KarateManStripesBackground("karateManStripes1", stripe1 = Color.valueOf("FEC652"),
                                                      stripe2 = Color.valueOf("FFE86C")), "menuTheme.karateManGba"),
                    BgData(KarateManStripesBackground("karateManStripes2"), "menuTheme.karateManGba2"),
                    BgData(BTSDSBackground("btsDS"), "menuTheme.btsDs"),
                    BgData(BTSDSBackground("btsDS2", Color.valueOf("E1E11FFF")), "menuTheme.btsDs2"),
                    BgData(BTSDSBackground("btsDSBlue", Color.valueOf("2963FFFF")), "menuTheme.btsDsBlue"),
                    BgData(StaticBackground("lightningBolting") { AssetRegistry["bg_thunder"] }, "menuTheme.lightningBolting")
//                    BgData(PolyrhythmBackground("polyrhythm"), "Polyrhythm")
                  )
        }
        val backgrounds: List<Background> by lazy { backgroundsData.map { it.bg } }
        val backgroundMap: Map<String, Background> by lazy { backgrounds.associateBy(Background::id) }
        val backgroundMapByBg: Map<Background, BgData> by lazy { backgroundsData.associateBy { it.bg } }
        val defaultBackground: Background
            get() = backgroundMap.getValue("tengoku")
    }
    
}