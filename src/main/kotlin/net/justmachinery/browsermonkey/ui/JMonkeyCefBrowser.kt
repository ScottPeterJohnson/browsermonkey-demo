package net.justmachinery.feral.client.ui.browser

import com.jme3.input.event.KeyInputEvent
import com.jme3.input.event.MouseButtonEvent
import com.jme3.input.event.MouseMotionEvent
import net.justmachinery.browsermonkey.ui.PaintHolder
import java.awt.Rectangle

interface JMonkeyCefBrowser {
    fun notifyResize(width : Int, height : Int)
    fun onMouseButtonEvent(event : MouseButtonEvent)
    fun onKeyEvent(event : KeyInputEvent)
    fun onMouseMotionEvent(event : MouseMotionEvent)

    fun renderIfNecessary(draw : (DrawRequest)->Unit, wait : Boolean = false)
    data class DrawRequest(
        val all : Boolean,
        val buffer : PaintHolder.PaintBuffer,
        val dirty : List<Rectangle>
    )

    fun setUrl(url : String)
    fun getUrl() : String?
}