package net.justmachinery.browsermonkey.ui

import com.jme3.app.Application
import com.jme3.input.RawInputListener
import com.jme3.input.event.*
import kotlinx.coroutines.launch
import kotlinx.html.DIV
import kotlinx.html.HtmlBlockTag
import kotlinx.html.classes
import kotlinx.serialization.Serializable
import net.justmachinery.feral.client.ui.browser.JMonkeyCefBrowser
import net.justmachinery.feral.client.ui.browser.flexRow
import net.justmachinery.feral.client.ui.browser.json
import net.justmachinery.shade.component.Component
import net.justmachinery.shade.component.MountingContext

class BrowserOverlayInputListener(
    private val application: Application,
) {
    private lateinit var browser : JMonkeyCefBrowser

    private val browserClickRects = mutableMapOf<Long, BrowserClickRect>()
    private val inputListener = object : RawInputListener {
        override fun beginInput() {}
        override fun endInput() {}
        override fun onJoyAxisEvent(evt: JoyAxisEvent?) {}
        override fun onJoyButtonEvent(evt: JoyButtonEvent?) {}
        override fun onTouchEvent(evt: TouchEvent?) {}

        override fun onMouseMotionEvent(evt: MouseMotionEvent) {
            if(evt.deltaWheel != 0){
                if(clickInRects(evt.x, evt.y, true)){
                    evt.setConsumed()
                    browser.onMouseMotionEvent(evt)
                }
            } else {
                browser.onMouseMotionEvent(evt)
            }
        }

        override fun onMouseButtonEvent(evt: MouseButtonEvent) {
            if(clickInRects(evt.x, evt.y, false)){
                browser.onMouseButtonEvent(evt)
                evt.setConsumed()
            }
        }

        override fun onKeyEvent(evt: KeyInputEvent) {
            browser.onKeyEvent(evt)
        }
    }
    fun clickInRects(x : Int, y : Int, forScroll : Boolean) : Boolean {
        val yOff = application.camera.height
        for(rect in browserClickRects.values){
            if(forScroll && !rect.captureScroll){ continue }
            val browserY = yOff - y
            if(x>= rect.x && x<rect.x + rect.width && browserY>= rect.y && browserY<rect.y + rect.height){
                return true
            }
        }
        return false
    }
    fun onRectChanges(changes : List<ClickRectChange>){
        for(change in changes){
            if(change.newRect == null){
                browserClickRects.remove(change.targetId)
            } else {
                browserClickRects[change.targetId] = change.newRect
            }
        }
    }
    fun init(browser : JMonkeyCefBrowser){
        this.browser = browser
        application.inputManager.addRawInputListener(inputListener)
    }
}

class InputRectListener : Component<InputRectListener.Props>() {
    data class Props(
        val browserOverlayInputListener: BrowserOverlayInputListener
    )

    override fun MountingContext.mounted() {
        pumpClickRectReports()
    }

    private fun pumpClickRectReports(){
        launch {
            while(true){
                val report = client.runWithCallback("window.registerReportCallback ? window.registerReportCallback(shadeCb) : shadeCb([])").await()
                val changes = json.decodeFromString<List<ClickRectChange>>(report.raw)
                props.browserOverlayInputListener.onRectChanges(changes)
            }
        }
    }

    override fun HtmlBlockTag.render() {}
}

@Serializable
data class ClickRectChange(
    val targetId : Long,
    val newRect: BrowserClickRect?,
)
@Serializable
data class BrowserClickRect(
    val x : Double,
    val y : Double,
    val width : Double,
    val height : Double,
    val captureScroll: Boolean,
)

fun HtmlBlockTag.browserClickableDiv(captureScroll : Boolean = true, cb : DIV.()->Unit){
    flexRow {
        classes += "browserClickCapture"
        if(captureScroll){
            classes += "browserScrollCapture"
        }
        cb()
    }
}