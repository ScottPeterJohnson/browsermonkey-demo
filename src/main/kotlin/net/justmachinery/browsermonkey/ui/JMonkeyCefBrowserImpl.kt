package net.justmachinery.browsermonkey.ui

import com.jme3.input.KeyInput
import com.jme3.input.event.KeyInputEvent
import com.jme3.input.event.MouseButtonEvent
import com.jme3.input.event.MouseMotionEvent
import mu.KLogging
import net.justmachinery.feral.client.ui.browser.JMonkeyCefBrowser
import net.justmachinery.feral.client.ui.browser.jmeKeyCodeToAwt
import org.cef.browser.CefBrowser
import org.cef.browser.CefBrowserInitData
import org.cef.browser.CefBrowserProxy
import org.cef.browser.ProxiedCefBrowserCallbacks
import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.KEY_LOCATION_UNKNOWN
import java.awt.event.KeyEvent.VK_UNDEFINED
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.nio.ByteBuffer
import javax.swing.SwingUtilities

class JMonkeyCefBrowserImpl(
    val init : CefBrowserInitData,
) : JMonkeyCefBrowser, ProxiedCefBrowserCallbacks {
    companion object : KLogging()


    private val browserRect = Rectangle(0, 0, init.initialWidth, init.initialHeight)
    private val proxy = CefBrowserProxy.create(init, this)

    private var popupRect = Rectangle()

    override fun notifyResize(width: Int, height: Int) {
        SwingUtilities.invokeLater {
            logger.debug { "Notifying resize to $width x $height" }
            browserRect.setBounds(0, 0, width, height)
            proxy.wasResized(width, height)
        }
    }

    override fun setUrl(url: String) {
        if(proxy.browser.url != url){
            proxy.browser.loadURL(url)
        }
    }

    override fun getUrl(): String? {
        return proxy.browser.url
    }

    override fun browserRect() = browserRect

    private val paintHolder = PaintHolder()

    override fun onPaint(
        browser: CefBrowser?,
        popup: Boolean,
        dirtyRects: Array<Rectangle>,
        buffer: ByteBuffer,
        width: Int,
        height: Int
    ) {
        val paintBuffer = try {
            PaintHolder.PaintBuffer.wrap(buffer, width, height)
        } catch(t : IllegalArgumentException){
            logger.error(t){ "onPaint" }
            return
        }

        if (popup) {
            paintHolder.paintFromPartialBuffer(
                sourceBuffer = paintBuffer,
                sourceDirty = dirtyRects.asIterable(),
                sourceInExisting = popupRect
            )
        } else {
            paintHolder.paintFromScreenBuffer(
                buffer = paintBuffer,
                dirtyRects = dirtyRects
            )
        }
    }

    override fun renderIfNecessary(draw : (JMonkeyCefBrowser.DrawRequest)->Unit, wait : Boolean){
        paintHolder.renderIfNecessary(draw, wait)
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
    override fun onMouseMotionEvent(event : MouseMotionEvent){
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
    override fun onMouseButtonEvent(event : MouseButtonEvent){
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

    override fun onKeyEvent(event : KeyInputEvent){
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
