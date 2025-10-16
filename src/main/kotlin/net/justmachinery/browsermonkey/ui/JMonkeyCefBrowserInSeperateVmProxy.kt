package net.justmachinery.browsermonkey.ui

import com.jme3.input.event.KeyInputEvent
import com.jme3.input.event.MouseButtonEvent
import com.jme3.input.event.MouseMotionEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import mu.KLogging
import net.justmachinery.browsermonkey.ui.PaintHolder.PaintBuffer
import net.justmachinery.feral.client.ui.browser.JMonkeyCefBrowser
import net.justmachinery.feral.client.ui.browser.proto
import net.justmachinery.feral.client.ui.browser.readExactly
import net.justmachinery.feral.client.ui.browser.readInt
import net.justmachinery.feral.client.ui.browser.writeInt
import net.justmachinery.futility.execution.ShutdownHookPriority
import net.justmachinery.futility.execution.addJvmShutdownHook
import java.awt.Rectangle
import java.io.File
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.jvm.optionals.getOrNull

class JMonkeyCefBrowserInSeperateVmProxy(
    var initialWidth : Int,
    var initialHeight : Int,
    val javaCommand : String = ProcessHandle.current().info().command().getOrNull() ?: throw IllegalStateException("Java command not found"),
) : JMonkeyCefBrowser {
    companion object : KLogging()
    private var instanceCount = 0
    private inner class VmInstance() {
        val instanceShutdown = AtomicBoolean(false)
        var count = instanceCount++
        var process : Process? = null
        var serverChannel : ServerSocketChannel? = null
        var socket : SocketChannel? = null
        var initThread : Thread? = null
        var readThread : Thread? = null
        var writeThread : Thread? = null

        fun start(){
            initThread = thread(name = "JCEF Init $count (Main VM)", isDaemon = true){
                try {
                    val tempDirectory = Files.createTempDirectory("jcefsocket")
                    val socketAddress = UnixDomainSocketAddress.of(tempDirectory.resolve("jcef.socket"))
                    val serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
                    serverChannel.bind(socketAddress)
                    serverChannel.configureBlocking(true)

                    logger.trace { "Starting process..." }
                    val process = startProcess(socketAddress)
                    this.process = process
                    logger.trace { "Started PID ${process.pid()}, waiting for socket connect" }

                    socket = serverChannel.accept()

                    logger.trace { "Socket connected" }
                    readThread = thread(name = "JCEF Reader $count (Main VM)", isDaemon = true) {
                        readLoop()
                    }
                    writeThread = thread(name = "JCEF Writer $count (Main VM)", isDaemon = true) {
                        sendUrl()
                        writeLoop()
                    }

                    val result = process.waitFor()
                    logger.info { "JCEF $count exited with code $result" }
                } catch(t : Throwable) {
                    if(t is InterruptedException){
                        logger.info { "JCEF $count interrupted" }
                    } else {
                        logger.error(t){ "In JCEF $count" }
                    }
                }finally {
                    if(destroyIfActive() && !browserShutdown){
                        logger.info { "JCEF $count restarting from init thread"}
                        this@JMonkeyCefBrowserInSeperateVmProxy.start()
                    }
                }
            }
        }

        fun sendUrl(){
            eventQueue.add(BrowserEvent.SetUrlEvent(desiredUrl))
        }

        private fun startProcess(socketAddress: UnixDomainSocketAddress) : Process {
            val classPath = System.getProperty("java.class.path")
            return ProcessBuilder(javaCommand,
                "-cp", classPath,
                "-Xmx32m",
                "-Xms8m",
                "-XX:MaxMetaspaceSize=32m",
                "-XX:CompressedClassSpaceSize=32m",
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+UseCompactObjectHeaders",
                "--enable-native-access=ALL-UNNAMED",
                "--sun-misc-unsafe-memory-access=allow",
                "--add-exports", "java.base/java.lang=ALL-UNNAMED",
                "--add-exports", "java.desktop/sun.awt=ALL-UNNAMED",
                "--add-exports", "java.desktop/sun.java2d=ALL-UNNAMED",
                JMonkeyCefBrowserInSeperateVmServer::class.qualifiedName!!,
                proto.encodeToByteArray(SubVmStartupArguments(
                    socketAddress = socketAddress.toString(),
                    initialWidth = initialWidth,
                    initialHeight = initialHeight,
                )).toHexString()
            )
                .inheritIO()
                .start()
        }


        val paint = PaintHolder()
        private fun readLoop(){
            while(true){
                try {
                    val messageSize = socket!!.readInt()
                    val message = socket!!.readExactly(messageSize)
                    val evt = proto.decodeFromByteArray<JMonkeyCefBrowserInSeperateVmServer.PaintEvent>(message)
                    logger.trace { "Read $evt" }
                    when (evt) {
                        is JMonkeyCefBrowserInSeperateVmServer.PaintEvent.DrawRequest -> {
                            val paintBuffer = PaintBuffer.create(evt.width, evt.height)
                            val dataBuffer = paintBuffer.raw
                            socket!!.readExactly(dataBuffer)
                            logger.trace { "Read new paint buffer" }
                            if (evt.all) {
                                paint.paintFromScreenBuffer(
                                    buffer = paintBuffer,
                                    dirtyRects = arrayOf(Rectangle(0, 0, evt.width, evt.height))
                                )
                            } else {
                                paint.paintFromPartialBuffer(
                                    sourceBuffer = paintBuffer,
                                    sourceDirty = listOf(Rectangle(0, 0, evt.width, evt.height)),
                                    targetDirty = listOf(Rectangle(evt.x, evt.y, evt.width, evt.height))
                                )
                            }
                        }
                    }
                } catch(t : Throwable){
                    if(t is ClosedChannelException && instanceShutdown.get()){
                        logger.info { "Read loop channel closed" }
                    } else {
                        logger.error(t) { "Read loop channel unexpected error" }
                        destroyIfActive()
                    }
                    break
                }
            }
        }


        val eventQueue = LinkedBlockingQueue<BrowserEvent>()
        private fun writeLoop(){
            while(true){
                try {
                    val next = eventQueue.take()
                    logger.trace { "Writing $next" }
                    val serialized = proto.encodeToByteArray(BrowserEvent.serializer(), next)
                    socket!!.writeInt(serialized.size)
                    socket!!.write(ByteBuffer.wrap(serialized))
                    logger.trace { "Wrote $next" }
                } catch(t : Throwable){
                    if((t is ClosedChannelException || t is InterruptedException) && instanceShutdown.get()){
                        logger.info { "Write loop channel closed" }
                    } else {
                        logger.error(t) { "Write loop channel unexpected error" }
                        destroyIfActive()
                    }
                    break
                }
            }
        }

        fun destroyIfActive(): Boolean {
            return if(instanceShutdown.compareAndSet(false, true)){
                socket?.close()
                serverChannel?.close()
                process?.destroy()
                instance = null
                initThread?.interrupt()
                true
            } else {
                false
            }
        }
    }
    private var instance : VmInstance? = null

    @Volatile private var browserShutdown = false
    private val shutdownHook by lazy {
        addJvmShutdownHook(ShutdownHookPriority(0.0)){
            browserShutdown = true
            synchronized(this){
                instance?.destroyIfActive()
            }
        }
    }
    fun start(){
        synchronized(this){
            val oldInstance = instance
            oldInstance?.destroyIfActive()
            instance = VmInstance()
            instance!!.start()
        }
        shutdownHook
    }



    override fun renderIfNecessary(draw: (JMonkeyCefBrowser.DrawRequest) -> Unit, wait: Boolean) {
        instance?.paint?.renderIfNecessary(draw, wait)
    }

    private var desiredUrl : String = "about:blank"
    override fun setUrl(url: String) {
        desiredUrl = url
        instance?.sendUrl()
    }

    override fun getUrl(): String {
        return desiredUrl
    }


    @Serializable
    data class SubVmStartupArguments(
        val socketAddress : String,
        val initialWidth : Int,
        val initialHeight : Int,
    )

    @Serializable
    sealed class BrowserEvent {
        @Serializable
        @SerialName("a")
        data class ResizeEvent(val width : Int, val height : Int) : BrowserEvent()
        @Serializable
        @SerialName("b")
        data class OnMouseButtonEvent(val x : Int, val y : Int, val btnIndex : Int, val pressed : Boolean) : BrowserEvent()
        @Serializable
        @SerialName("c")
        data class OnKeyEvent(val keyCode : Int, val keyChar : Char, val pressed : Boolean, val repeating : Boolean) : BrowserEvent()
        @Serializable
        @SerialName("d")
        data class OnMouseMotionEvent(val x : Int, val y : Int, val dx : Int, val dy : Int, val wheel : Int, val deltaWheel : Int) : BrowserEvent()
        @Serializable
        @SerialName("e")
        data class SetUrlEvent(val url : String) : BrowserEvent()
    }
    private fun notifyEvent(event : BrowserEvent){
        instance?.eventQueue?.add(event)
    }
    override fun notifyResize(width: Int, height: Int) {
        synchronized(this){
            initialWidth = width
            initialHeight = height
            notifyEvent(BrowserEvent.ResizeEvent(width, height))
        }

    }

    override fun onMouseButtonEvent(event: MouseButtonEvent) {
        notifyEvent(BrowserEvent.OnMouseButtonEvent(
            x = event.x,
            y = event.y,
            btnIndex = event.buttonIndex,
            pressed = event.isPressed
        ))
    }

    override fun onKeyEvent(event: KeyInputEvent) {
        notifyEvent(BrowserEvent.OnKeyEvent(
            keyCode = event.keyCode,
            keyChar = event.keyChar,
            pressed = event.isPressed,
            repeating = event.isRepeating
        ))
    }

    override fun onMouseMotionEvent(event: MouseMotionEvent) {
        notifyEvent(BrowserEvent.OnMouseMotionEvent(
            x = event.x,
            y = event.y,
            dx = event.dx,
            dy = event.dy,
            wheel = event.wheel,
            deltaWheel = event.deltaWheel
        ))
    }
}