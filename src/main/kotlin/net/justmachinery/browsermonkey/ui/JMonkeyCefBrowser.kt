package net.justmachinery.browsermonkey.ui

import com.jme3.input.KeyInput
import com.jme3.input.event.KeyInputEvent
import com.jme3.input.event.MouseButtonEvent
import com.jme3.input.event.MouseMotionEvent
import com.jme3.renderer.opengl.GLRenderer
import com.jme3.texture.Image
import com.jme3.texture.Texture2D
import com.jme3.texture.image.ColorSpace
import mu.KLogging
import org.cef.browser.CefBrowser
import org.cef.browser.CefBrowserInitData
import org.cef.browser.CefBrowserProxy
import org.cef.browser.ProxiedCefBrowserCallbacks
import org.lwjgl.BufferUtils
import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.KEY_LOCATION_UNKNOWN
import java.awt.event.KeyEvent.VK_UNDEFINED
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.nio.ByteBuffer
import javax.swing.SwingUtilities


class JMonkeyCefBrowser(
    val init : CefBrowserInitData,
    val jMonkeyRenderer: GLRenderer,
    val onTextureChange: (Texture2D)->Unit,
) : ProxiedCefBrowserCallbacks {
    companion object : KLogging()

    val browserRect = Rectangle(0, 0, init.initialWidth, init.initialHeight)
    private val proxy = CefBrowserProxy.create(init, this)
    val cef get() = proxy.browser

    private var popupRect = Rectangle()

    fun notifyResize(width: Int, height: Int) {
        synchronized(paint){
            SwingUtilities.invokeLater {
                logger.debug { "Notifying resize to $width x $height" }
                browserRect.setBounds(0, 0, width, height)
                proxy.wasResized(width, height)
            }
        }
    }
    private class PaintData(
        val buffer: ByteBuffer,
        val width : Int,
        val height : Int,
    ) {
        var toPaint : PaintRequest? = null
    }
    private sealed class PaintRequest {
        data object All : PaintRequest()
        class Dirty(val rects : MutableList<Array<Rectangle>>) : PaintRequest()
    }
    private var paintData : PaintData? = null
    private val paint = Object()

    override fun browserRect() = browserRect

    override fun onPaint(
        browser: CefBrowser?,
        popup: Boolean,
        dirtyRects: Array<Rectangle>,
        buffer: ByteBuffer,
        width: Int,
        height: Int
    ) {
        val size = (width * height) shl 2
        if(buffer.limit() != size){
            logger.error { "Got buffer of ${buffer.limit()} but expect max of $size" }
            return
        }

        synchronized(paint) {
            val existing = paintData
            if(existing == null || (!popup && (existing.width != width || existing.height != height))){
                logger.debug { "New paint frame: $width, $height" }
                val new = PaintData(
                    buffer = BufferUtils.createByteBuffer(size),
                    width = width,
                    height = height,
                )
                paintData = new
                new.toPaint = (PaintRequest.All)

                //Copy the whole buffer over
                buffer.position(0)
                new.buffer.put(buffer)
                new.buffer.position(0)

                new
            } else {
                val target = existing.buffer
                logger.debug { "Got repaint (${existing.width}, ${existing.height})" }
                val finalDirty = if(popup){
                    val finalDirty = mutableListOf<Rectangle>()
                    for(dirty in dirtyRects){
                        //First, translate and clamp the dirty rect from the popup to the browser window
                        val browser = Rectangle(popupRect.x + dirty.x, popupRect.y + dirty.y, dirty.width, dirty.height)
                        val clampedBrowser = constrain(browserRect, browser)
                        if(clampedBrowser == null){ continue }

                        val popup = Rectangle(clampedBrowser.x - popupRect.x, clampedBrowser.y - popupRect.y, clampedBrowser.width, clampedBrowser.height)

                        finalDirty.add(browser)
                        //Translate back into popup buffer

                        for(y in (0 until popup.height)){
                            val popupY = popup.y + y
                            val browserY = browser.y + y
                            val popupStart = (popup.x + popupY*width)*4
                            val browserStart = (browser.x + browserY * browserRect.width)*4
                            target.put(browserStart, buffer, popupStart, popup.width*4)
                        }
                    }
                    finalDirty.toTypedArray()
                } else {
                    //Copy just the dirty rects
                    for(rect in dirtyRects){
                        for(y in (rect.y until (rect.y+rect.height))){
                            val startIndex = (rect.x + y*width)*4
                            target.put(startIndex, buffer, startIndex, rect.width*4)
                        }
                    }
                    dirtyRects
                }
                val existingToPaint = existing.toPaint
                if(existingToPaint == null){
                    existing.toPaint = PaintRequest.Dirty(mutableListOf(finalDirty))
                } else {
                    when(existingToPaint){
                        PaintRequest.All -> {}
                        is PaintRequest.Dirty -> existingToPaint.rects.add(finalDirty)
                    }
                }
            }
        }
    }

    private fun constrain(outer : Rectangle, inner : Rectangle) : Rectangle? {
        val intersectX = maxOf(inner.x, outer.x)
        val intersectY = maxOf(inner.y, outer.y)
        val intersectRight = minOf(inner.x + inner.width, outer.x + outer.width)
        val intersectBottom = minOf(inner.y + inner.height, outer.y + outer.height)

        if (intersectRight <= intersectX || intersectBottom <= intersectY) {
            return null
        }

        return Rectangle(intersectX, intersectY, intersectRight - intersectX, intersectBottom - intersectY)
    }

    private var texture = Texture2D(0, 0, Image.Format.BGRA8)
    fun renderIfNecessary(){
        synchronized(paint) {
            val paintData = this.paintData
            val toPaint = paintData?.toPaint
            if(paintData != null && toPaint != null){
                paintData.toPaint = null
                val draw = Image(Image.Format.BGRA8, paintData.width, paintData.height, paintData.buffer, ColorSpace.Linear)
                when(toPaint){
                    PaintRequest.All -> {
                        logger.debug { "Repaint all: (${paintData.width}, ${paintData.height})" }
                        // Update/resize the whole texture.
                        texture.image.dispose()
                        texture = Texture2D(draw)
                        onTextureChange(texture)
                    }
                    is PaintRequest.Dirty -> {
                        logger.debug { "Repaint dirty: (${paintData.width}, ${paintData.height})" }
                        val rects = mergeRectangles(toPaint.rects.asSequence().flatMap { it.asSequence() })
                        for(rect in rects){
                            jMonkeyRenderer.modifyTexture(texture, draw, rect.x, rect.y, rect.x, rect.y, rect.width, rect.height)
                        }
                    }
                }
            }
        }
    }

    override fun clearPopupRects() {
        popupRect.setBounds(0,0,0,0)
    }

    override fun onPopupSize(size: Rectangle) {
        if(size.width <= 0 || size.height <= 0){ return }
        val rc = size.clone() as Rectangle
        if(rc.x < 0){ rc.x = 0}
        if(rc.y < 0){ rc.y = 0}
        // if popup goes outside the view, try to reposition origin
        if (rc.x + rc.width > browserRect.width) rc.x = browserRect.width - rc.width
        if (rc.y + rc.height > browserRect.height) rc.y = browserRect.height - rc.height

        // if x or y became negative, move them to 0 again.
        if (rc.x < 0) rc.x = 0
        if (rc.y < 0) rc.y = 0
        popupRect = rc
    }

    private val downMasks = mutableSetOf<Int>()
    private fun computeDownMasks() : Int {
        return downMasks.fold(0) { acc, mask -> acc.or(mask) }
    }
    fun onMouseMotionEvent(event : MouseMotionEvent){
        SwingUtilities.invokeLater {
            logger.trace { "Sending mouse motion event to browser: $event" }
            if(event.deltaWheel != 0){
                proxy.sendMouseWheelEvent(MouseWheelEvent(
                    /* source = */ proxy.browser.uiComponent,
                    /* id = */ MouseEvent.MOUSE_WHEEL,
                    /* when = */ event.time,
                    /* modifiers = */ computeDownMasks(),
                    /* x = */ event.x,
                    /* y = */ browserRect.height - event.y,
                    /* clickCount = */ 0,
                    /* popupTrigger = */ false,
                    /* scrollType = */ MouseWheelEvent.WHEEL_BLOCK_SCROLL,
                    /* scrollAmount = */ event.deltaWheel,
                    /* wheelRotation = */ event.deltaWheel
                ))
            } else {
                proxy.sendMouseEvent(MouseEvent(
                    /* source = */ proxy.browser.uiComponent,
                    /* id = */ MouseEvent.MOUSE_MOVED,
                    /* when = */ event.time,
                    /* modifiers = */ computeDownMasks(),
                    /* x = */ event.x,
                    /* y = */ browserRect.height - event.y,
                    /* clickCount = */ 0,
                    /* popupTrigger = */ false
                ))
            }
        }
    }
    fun onMouseButtonEvent(event : MouseButtonEvent){
        SwingUtilities.invokeLater {
            proxy.sendMouseEvent(
                MouseEvent(
                    /* source = */ proxy.browser.uiComponent,
                    /* id = */ if (event.isPressed) MouseEvent.MOUSE_PRESSED else MouseEvent.MOUSE_RELEASED,
                    /* when = */ event.time,
                    /* modifiers = */ computeDownMasks(),
                    /* x = */ event.x,
                    /* y = */ browserRect.height - event.y,
                    /* clickCount = */ 1,
                    /* popupTrigger = */ false,
                    /* button = */ event.buttonIndex + 1
                )
            )
            val mouseMask = MouseEvent.getMaskForButton(event.buttonIndex+1)
            if(event.isPressed){
                downMasks.add(mouseMask)
            } else {
                downMasks.remove(mouseMask)
            }
        }
    }

    fun onKeyEvent(event : KeyInputEvent){
        SwingUtilities.invokeLater {
            val awtKeyCode = jmeKeyCodeToAwt(event.keyCode)
            if(!event.isRepeating){
                proxy.sendKeyEvent(
                    KeyEvent(
                        /* source = */ proxy.browser.uiComponent,
                        /* id = */ if (event.isPressed) KeyEvent.KEY_PRESSED else KeyEvent.KEY_RELEASED,
                        /* when = */ event.time,
                        /* modifiers = */ computeDownMasks(),
                        /* keyCode = */ awtKeyCode,
                        /* keyChar = */ event.keyChar,
                        /* keyLocation = */ KEY_LOCATION_UNKNOWN
                    )
                )
            }
            if(event.isPressed && event.keyChar != (0).toChar()){
                proxy.sendKeyEvent(
                    KeyEvent(
                        /* source = */ proxy.browser.uiComponent,
                        /* id = */ KeyEvent.KEY_TYPED,
                        /* when = */ event.time,
                        /* modifiers = */ computeDownMasks(),
                        /* keyCode = */ VK_UNDEFINED,
                        /* keyChar = */ event.keyChar,
                        /* keyLocation = */ KEY_LOCATION_UNKNOWN
                    )
                )
            }

            val mask = when(event.keyCode){
                KeyInput.KEY_LSHIFT, KeyInput.KEY_RSHIFT -> KeyEvent.SHIFT_DOWN_MASK
                KeyInput.KEY_LMENU, KeyInput.KEY_RMENU -> KeyEvent.ALT_DOWN_MASK
                KeyInput.KEY_LMETA, KeyInput.KEY_RMETA -> KeyEvent.META_DOWN_MASK
                KeyInput.KEY_LCONTROL, KeyInput.KEY_RCONTROL -> KeyEvent.CTRL_DOWN_MASK
                else -> null
            }
            if(mask != null){
                if(event.isPressed){
                    downMasks.add(mask)
                } else {
                    downMasks.remove(mask)
                }
            }

        }
    }
}

