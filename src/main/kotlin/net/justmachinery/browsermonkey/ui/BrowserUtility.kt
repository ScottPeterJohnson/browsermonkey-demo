package net.justmachinery.browsermonkey.ui

import kotlinx.css.*
import kotlinx.html.DIV
import kotlinx.html.HtmlBlockTag
import kotlinx.html.div
import kotlinx.html.span
import kotlinx.serialization.json.Json
import net.justmachinery.shade.utility.withStyle

//These are from top-left right and down, unlike jMonkey which is right and up
data class BrowserScreenCoords(val x : Int, val y : Int)

inline fun HtmlBlockTag.flexColumn(grow:Int=0, crossinline cb : DIV.()->Unit){
    div {
        withStyle {
            display = Display.flex
            flexDirection = FlexDirection.column
            overflow = Overflow.hidden
            if(grow != 0){
                flex(grow)
            }
        }
        cb()
    }
}

inline fun HtmlBlockTag.flexRow(grow:Int=0, crossinline cb : DIV.()->Unit){
    div {
        withStyle {
            display = Display.flex
            overflow = Overflow.hidden
            if(grow != 0){
                flex(grow)
            }
        }
        cb()
    }
}

fun HtmlBlockTag.flexSpacer(){
    span {
        withStyle {
            flex(flexGrow = 1.0)
        }
    }
}

val json by lazy {
    Json {
        allowStructuredMapKeys = true
        //So long as it deserializes correctly, unknown keys are okay. This makes adding new behaviors with defaults easier.
        ignoreUnknownKeys = true
    }
}