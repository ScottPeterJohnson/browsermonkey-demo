@file:Suppress("PackageDirectoryMismatch")

package org.cef.browser;

import org.apache.commons.lang3.reflect.FieldUtils
import org.cef.CefBrowserSettings
import org.cef.CefClient
import org.cef.callback.CefDragData
import org.cef.handler.CefRenderHandler
import org.cef.handler.CefScreenInfo
import java.awt.Component
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.nio.ByteBuffer
import java.util.function.Consumer

/**
 * Since CefBrowser_N is package private, we extend it but are forced to create a proxy class
 * to expose all of its calls. Real logic lives elsewhere.
 */
class CefBrowserProxy private constructor(private val actual : ProxiedCefBrowser){
    companion object {
        fun create(init: CefBrowserInitData, control: ProxiedCefBrowserCallbacks) : CefBrowserProxy = CefBrowserProxy(ProxiedCefBrowser(init, control))
    }
    val browser : CefBrowser get() = actual
    fun wasResized(width: Int, height: Int) {
        actual.wasResized(width, height)
    }
    fun sendMouseWheelEvent(event : MouseWheelEvent){
        actual.sendMouseWheelEvent(event)
    }
    fun sendMouseEvent(event : MouseEvent){
        actual.sendMouseEvent(event)
    }
    fun sendKeyEvent(event : KeyEvent){
        actual.sendKeyEvent(event)
    }
}

data class CefBrowserInitData(
    val initialWidth: Int,
    val initialHeight : Int,
    val client: CefClient,
    val isTransparent: Boolean,
    val context: CefRequestContext?,
    val inspectAt: Point?,
    val settings : CefBrowserSettings?,
)

class DummyComponent : Component() {
    override fun getLocationOnScreen(): Point {
        return Point(0,0)
    }
}

interface ProxiedCefBrowserCallbacks {
    fun browserRect() : Rectangle
    fun onPaint(
        browser: CefBrowser?,
        popup: Boolean,
        dirtyRects: Array<Rectangle>,
        buffer: ByteBuffer,
        width: Int,
        height: Int
    )
    fun clearPopupRects()
    fun onPopupSize(size : Rectangle)
}
private class ProxiedCefBrowser(
    init : CefBrowserInitData,
    private val control: ProxiedCefBrowserCallbacks,
) : CefBrowser_N(init.client, "about:blank", init.context, null, init.inspectAt, init.settings), CefRenderHandler {
    private val settings = init.settings
    init {
        createBrowser(this.client, 0, "about:blank", true, init.isTransparent, null, init.context)
        //Okay, but it's not ACTUALLY CREATED until CEF sets the pending flag.
        while(true){
            if(FieldUtils.readField(this, "isPending_", true) == true){
                break
            }
            Thread.sleep(1)
        }
    }

    private val dummy = DummyComponent()
    override fun getUIComponent() = dummy
    override fun createImmediately() {}
    override fun getRenderHandler() = this
    override fun createScreenshot(nativeResolution: Boolean) = throw NotImplementedError()
    override fun createDevToolsBrowser(
        client: CefClient,
        url: String,
        context: CefRequestContext?,
        parent: CefBrowser_N?,
        inspectAt: Point?
    ) = CefBrowserFactory.create(client, url, false, false, context, settings) as CefBrowser_N

    @Synchronized
    override fun close(force: Boolean) {
        super.close(force)
        requestContext?.dispose()
    }

    override fun getViewRect(browser: CefBrowser?): Rectangle {
        return control.browserRect()
    }

    override fun getScreenInfo(browser: CefBrowser?, screenInfo: CefScreenInfo): Boolean {
        screenInfo.Set(
            1.0, 32, 8, false, control.browserRect().bounds,
            control.browserRect().bounds,
        )

        return true
    }

    override fun getScreenPoint(browser: CefBrowser?, viewPoint: Point): Point {
        return viewPoint
    }

    override fun onPaint(
        browser: CefBrowser?,
        popup: Boolean,
        dirtyRects: Array<Rectangle>,
        buffer: ByteBuffer,
        width: Int,
        height: Int
    ) {
        control.onPaint(browser, popup, dirtyRects, buffer, width, height)
    }


    override fun onPopupShow(browser: CefBrowser?, show: Boolean) {
        if(!show){
            control.clearPopupRects()
            invalidate()
        }
    }

    override fun onPopupSize(browser: CefBrowser?, size: Rectangle) {
        control.onPopupSize(size)
    }

    override fun onCursorChange(browser: CefBrowser?, cursorType: Int) = true

    override fun startDragging(
        browser: CefBrowser?,
        dragData: CefDragData?,
        mask: Int,
        x: Int,
        y: Int
    ): Boolean {
        return false
    }
    override fun updateDragCursor(browser: CefBrowser?, operation: Int) {
    }
    override fun addOnPaintListener(listener: Consumer<CefPaintEvent>?) = throw NotImplementedError()
    override fun setOnPaintListener(listener: Consumer<CefPaintEvent>?)  = throw NotImplementedError()
    override fun removeOnPaintListener(listener: Consumer<CefPaintEvent>?) = throw NotImplementedError()

    override fun setFocus(enable: Boolean) {
        //See https://github.com/chromiumembedded/java-cef/issues/489#issuecomment-2835464454
    }

    override fun isLoading(): Boolean {
        return super.isLoading()
    }
}