package net.justmachinery.browsermonkey.ui

import com.jme3.input.event.KeyInputEvent
import com.jme3.input.event.MouseButtonEvent
import com.jme3.input.event.MouseMotionEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToByteArray
import mu.KLogging
import net.justmachinery.browsermonkey.setupLogging
import net.justmachinery.feral.client.ui.browser.JMonkeyCefBrowserFactory
import net.justmachinery.feral.client.ui.browser.proto
import net.justmachinery.feral.client.ui.browser.readExactly
import net.justmachinery.feral.client.ui.browser.readInt
import net.justmachinery.feral.client.ui.browser.writeInt
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

class JMonkeyCefBrowserInSeperateVmServer(
    val socketAddress: UnixDomainSocketAddress,
    val initialWidth : Int,
    val initialHeight : Int,
) {
    companion object : KLogging() {
        @JvmStatic
        fun main(args : Array<String>){
            Thread.currentThread().name = "JCEF Init (SubVM)"
            val args = proto.decodeFromByteArray(JMonkeyCefBrowserInSeperateVmProxy.SubVmStartupArguments.serializer(), args[0].hexToByteArray())

            setupLogging()

            logger.info { "Initialized" }

            SwingUtilities.invokeLater {
                Thread.currentThread().name = "AWT-EventQueue-0 (SubVM)"
            }

            val browser = JMonkeyCefBrowserInSeperateVmServer(
                socketAddress = UnixDomainSocketAddress.of(args.socketAddress),
                initialWidth = args.initialWidth,
                initialHeight = args.initialHeight
            )
            browser.start()
        }
    }

    private val socket = SocketChannel.open(StandardProtocolFamily.UNIX).apply {
        configureBlocking(true)
    }
    private val browser = JMonkeyCefBrowserFactory.Companion.createActualBrowser(
        initialWidth = initialWidth,
        initialHeight = initialHeight,
    )

    fun start(){
        require(socket.connect(socketAddress))
        startReader()
        startWriter()
    }

    private fun startReader(){
        thread(name = "JCEF Reader (SubVM)"){
            while(true){
                val messageSize = socket.readInt()
                val message = socket.readExactly(messageSize)
                val evt = proto.decodeFromByteArray(JMonkeyCefBrowserInSeperateVmProxy.BrowserEvent.serializer(),message)
                logger.trace { "Read $evt" }
                when(evt){
                    is JMonkeyCefBrowserInSeperateVmProxy.BrowserEvent.OnKeyEvent -> {
                        browser.onKeyEvent(KeyInputEvent(
                            /* keyCode = */ evt.keyCode,
                            /* keyChar = */ evt.keyChar,
                            /* pressed = */ evt.pressed,
                            /* repeating = */ evt.repeating
                        ))
                    }
                    is JMonkeyCefBrowserInSeperateVmProxy.BrowserEvent.OnMouseButtonEvent -> {
                        browser.onMouseButtonEvent(MouseButtonEvent(
                            /* btnIndex = */ evt.btnIndex,
                            /* pressed = */ evt.pressed,
                            /* x = */ evt.x,
                            /* y = */ evt.y
                        ))
                    }
                    is JMonkeyCefBrowserInSeperateVmProxy.BrowserEvent.OnMouseMotionEvent -> {
                        browser.onMouseMotionEvent(MouseMotionEvent(
                            /* x = */ evt.x,
                            /* y = */ evt.y,
                            /* dx = */ evt.dx,
                            /* dy = */ evt.dy,
                            /* wheel = */ evt.wheel,
                            /* deltaWheel = */ evt.deltaWheel
                        ))
                    }
                    is JMonkeyCefBrowserInSeperateVmProxy.BrowserEvent.ResizeEvent -> {
                        browser.notifyResize(evt.width, evt.height)
                    }

                    is JMonkeyCefBrowserInSeperateVmProxy.BrowserEvent.SetUrlEvent -> {
                        browser.setUrl(evt.url)
                    }
                }
            }
        }
    }

    private fun startWriter(){
        thread(name = "JCEF Writer (SubVM)") {
            while (true) {
                val toSend = mutableListOf<Pair<PaintEvent, PaintHolder.PaintBuffer>>()
                browser.renderIfNecessary({ draw ->
                    for (dirty in draw.dirty) {
                        val request = PaintEvent.DrawRequest(
                            all = draw.all,
                            x = dirty.x,
                            y = dirty.y,
                            width = dirty.width,
                            height = dirty.height
                        )
                        val buffer = PaintHolder.PaintBuffer.create(dirty.width, dirty.height)
                        if(draw.all){
                            buffer.copyAll(draw.buffer)
                        } else {
                            buffer.copyFrom(draw.buffer, dirty.x, dirty.y, 0, 0, dirty.width, dirty.height)
                        }
                        toSend.add(request to buffer)
                    }
                }, wait = true)
                for ((request, paint) in toSend) {
                    logger.trace { "Sending paint" }
                    val ser = proto.encodeToByteArray(request)
                    socket.writeInt(ser.size)
                    socket.write(ByteBuffer.wrap(ser))
                    paint.raw.rewind()
                    socket.write(paint.raw)
                    logger.trace { "Paint sent" }
                }
            }
        }
    }

    @Serializable
    sealed class PaintEvent {
        @Serializable
        @SerialName("a")
        data class DrawRequest(
            val all : Boolean,
            val x : Int,
            val y : Int,
            val width: Int,
            val height: Int,
        ) : PaintEvent()
    }
}