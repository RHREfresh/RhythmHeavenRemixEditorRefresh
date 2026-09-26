package io.github.chrislo27.rhrefresh.stage.bg

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import kotlin.math.roundToInt

class StaticBackground(id: String, val widthCoeff: Float = 1f, val heightCoeff: Float = 1f, val textureProvider: () -> Texture) : Background(id)  {

    override fun render(camera: OrthographicCamera, batch: SpriteBatch, shapeRenderer: ShapeRenderer, delta: Float) {
        batch.setColor(1f, 1f, 1f, 1f)
        val tex: Texture = textureProvider()
        val w = (Gdx.graphics.width * widthCoeff).roundToInt().toFloat()
        val h = (Gdx.graphics.height * heightCoeff).roundToInt().toFloat()
        batch.draw(tex, 0f, 0f, w, h)
    }
}