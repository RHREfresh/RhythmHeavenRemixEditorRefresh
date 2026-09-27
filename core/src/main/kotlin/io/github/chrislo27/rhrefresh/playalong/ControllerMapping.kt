package io.github.chrislo27.rhrefresh.playalong

import com.badlogic.gdx.controllers.Controller
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName


data class ControllerMapping(var inUse: Boolean, val name: String,
                             var buttonA: ControllerInput = ControllerInput.None,
                             var buttonB: ControllerInput = ControllerInput.None,
                             var buttonLeft: ControllerInput = ControllerInput.None,
                             var buttonRight: ControllerInput = ControllerInput.None,
                             var buttonUp: ControllerInput = ControllerInput.None,
                             var buttonDown: ControllerInput = ControllerInput.None,
                             var buttonStart: ControllerInput = ControllerInput.None,
                             var buttonSelect: ControllerInput = ControllerInput.None) {

    companion object {
        val INVALID = ControllerMapping(false, "<none>")
        fun convertGdx(controller: Controller): ControllerMapping{
            var gdxMapping = controller.mapping
            val buttonA = if (controller.name.contains("Nintendo")) gdxMapping.buttonB else gdxMapping.buttonA
            val buttonB = if (controller.name.contains("Nintendo")) gdxMapping.buttonA else gdxMapping.buttonB
            return ControllerMapping(false, controller.name,
                    buttonA = ControllerInput.Button(buttonA), buttonB = ControllerInput.Button(buttonB),
                    buttonLeft = ControllerInput.Button(gdxMapping.buttonDpadLeft),
                    buttonRight = ControllerInput.Button(gdxMapping.buttonDpadRight),
                    buttonUp = ControllerInput.Button(gdxMapping.buttonDpadUp),
                    buttonDown = ControllerInput.Button(gdxMapping.buttonDpadDown),
                    buttonSelect = ControllerInput.Button(gdxMapping.buttonBack),
                    buttonStart = ControllerInput.Button(gdxMapping.buttonStart),
            )
        }
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
        JsonSubTypes.Type(ControllerInput.None::class),
        JsonSubTypes.Type(ControllerInput.Button::class)
             )
sealed class ControllerInput {
    @JsonTypeName("none")
    object None : ControllerInput() {
        override fun isNothing(): Boolean = true
        override fun toString(): String {
            return "<none>"
        }
    }
    @JsonTypeName("button")
    class Button(val code: Int) : ControllerInput() {
        override fun isNothing(): Boolean = code < 0
        override fun toString(): String {
            return "Button $code"
        }
    }
//    class Axis(val axisCode: Int, val range: ClosedRange<Float>) : ControllerInput() // Not implemented

    abstract fun isNothing(): Boolean
}