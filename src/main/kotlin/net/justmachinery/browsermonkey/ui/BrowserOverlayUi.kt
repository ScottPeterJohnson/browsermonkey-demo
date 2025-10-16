package net.justmachinery.feral.client.ui.browser

import com.jme3.app.SimpleApplication
import com.jme3.renderer.ViewPort
import com.jme3.renderer.opengl.GLRenderer
import com.jme3.renderer.opengl.TextureUtil
import com.jme3.system.lwjgl.LwjglWindow
import com.jme3.texture.FrameBuffer
import com.jme3.texture.Image
import com.jme3.texture.Texture2D
import com.jme3.texture.image.ColorSpace
import com.jme3.ui.Picture
import com.jme3.util.BufferUtils
import mu.KLogging
import net.justmachinery.browsermonkey.ui.BrowserOverlayInputListener
import net.justmachinery.browsermonkey.ui.BrowserOverlayUiWebserver
import net.justmachinery.browsermonkey.ui.PaintHolder
import net.justmachinery.futility.execution.background
import net.justmachinery.futility.execution.future
import net.justmachinery.shade.component.Component
import org.apache.commons.lang3.reflect.FieldUtils
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL32C
import org.lwjgl.opengl.GL46
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.reflect.KClass

class BrowserOverlayUi(
    private val jm: SimpleApplication,
    private val browserOverlayUiWebserver: BrowserOverlayUiWebserver,
    private val browserOverlayInputListener: BrowserOverlayInputListener,
) {
    companion object : KLogging()

    private inner class Processor : AbstractSceneProcessor() {
        override fun postFrame(out: FrameBuffer?) {
            frameUpdate()
        }
    }

    private val geom = Picture("Browser", true)

    fun updateGeomSize(){
        if(jm.camera != null){
            val width = jm.camera.width
            val height = jm.camera.height
            geom.setLocalScale(width.toFloat(), height.toFloat(), 1f)
        }
    }

    private inner class QuadRender {
        @Volatile var texture = Texture2D(0, 0, Image.Format.BGRA8)

        fun attach(){
            geom.setTexture(jm.assetManager, texture, true)
        }
    }
    private val red = QuadRender()
    private val blue = QuadRender()
    @Volatile private var usingRed : Boolean = true
    @Volatile private var swapFence : Long? = null
    private val readyToSwap = AtomicBoolean(false)
    private val swapWait = Object()

    fun onWindowSizeChange(){
        updateGeomSize()
        browser?.notifyResize(jm.camera.width, jm.camera.height)
    }


    private var browser : JMonkeyCefBrowser? = null
    fun init(){
        updateGeomSize()
        jm.guiNode.attachChild(geom)
        //Put a transparent texture (otherwise would be pure white while JCEF loaded)
        val buffer = BufferUtils.createByteBuffer(4).apply {
            putInt(0)
            rewind()
        }
        geom.setTexture(jm.assetManager, Texture2D(Image(Image.Format.BGRA8, 1, 1, buffer, ColorSpace.sRGB)), true)

        background {
            val internalBrowserUrl = future {
                val url = browserOverlayUiWebserver.init()
                "$url?fromGameEngine=true"
            }
            browser = JMonkeyCefBrowserFactory().createCefBrowser(
                inSeparateVm = true,
                initialWidth = jm.camera.width,
                initialHeight = jm.camera.height,
            )
            browserOverlayInputListener.init(browser!!)
            browser!!.setUrl(internalBrowserUrl.get())
            renderThread.start()
            jm.viewPort.addProcessor(Processor())
        }
    }
    fun reloadPage(){
        browser?.getUrl()?.let {
            browser?.setUrl(it)
        }
    }

    private val jMonkeyRenderer by lazy {
        (jm.context as LwjglWindow).renderer as GLRenderer
    }
    private val textureUtil by lazy {
        FieldUtils.readField(jMonkeyRenderer, "texUtil", true) as TextureUtil
    }

    private val renderThread = thread(name = "Browser Overlay Renderer", start = false, isDaemon = true) {
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        val existingWindow = (jm.context as LwjglWindow).windowHandle
        val sharedWindow = glfwCreateWindow(1, 1, "", 0, existingWindow)
        glfwMakeContextCurrent(sharedWindow)
        GL.createCapabilities()


        try {
            while(true){
                browser?.renderIfNecessary({ draw ->
                    val drawNow : QuadRender
                    val drawLater : QuadRender
                    if(usingRed){
                        drawNow = blue
                        drawLater = red
                    } else {
                        drawNow = red
                        drawLater = blue
                    }
                    val img = Image(Image.Format.BGRA8, draw.buffer.width, draw.buffer.height, draw.buffer.raw, ColorSpace.sRGB)
                    fun drawTo(destination : QuadRender, isFirst : Boolean){
                        if(draw.all){
                            destination.texture.image.dispose()
                            if(isFirst){
                                //Need a copy.
                                val buf2 = PaintHolder.PaintBuffer.create(draw.buffer.width, draw.buffer.height)
                                buf2.copyAll(draw.buffer)
                                val img2 = Image(Image.Format.BGRA8, draw.buffer.width, draw.buffer.height, buf2.raw, ColorSpace.sRGB)
                                destination.texture = Texture2D(img2)
                            } else {
                                destination.texture = Texture2D(img)
                            }
                            createTexture(destination.texture)
                            //jMonkeyRenderer.setTexture(0, destination.texture)
                        } else {
                            draw.dirty.forEach { dirty ->
                                modifyTexture(destination.texture, img, dirty.x, dirty.y, dirty.x, dirty.y, dirty.width, dirty.height)
                            }
                        }
                    }

                    drawTo(drawNow, isFirst = true)
                    GL11C.glFinish()

                    synchronized(swapWait){
                        readyToSwap.set(true)
                        swapWait.wait()
                    }

                    val sync = swapFence
                    swapFence = null
                    if(sync != null){
                        GL32C.glWaitSync(sync, 0, GL32C.GL_TIMEOUT_IGNORED)
                        GL32C.glDeleteSync(sync)
                    }

                    drawTo(drawLater, isFirst = false)

                }, wait = true)
            }
        } finally {
            glfwMakeContextCurrent(0)
        }
    }

    private fun createTexture(texture2D: Texture2D){
        val intBuffer = BufferUtils.createIntBuffer(1)
        GL11C.glGenTextures(intBuffer)
        val id = intBuffer.get(0)
        val image = texture2D.image
        image.id = id
        GL46.glActiveTexture(GL46.GL_TEXTURE0)
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, id)
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MAX_LEVEL, 0)

        val getSrgbFormat = image.colorSpace == ColorSpace.sRGB && jMonkeyRenderer.isLinearizeSrgbImages
        val jmeFormat = image.format
        val oglFormat = textureUtil.getImageFormatWithError(jmeFormat, getSrgbFormat)
        val data = image.getData(0)
        data.position(0)
        GL46.glTexImage2D(
            GL46.GL_TEXTURE_2D,
            0,
            oglFormat.internalFormat,
            texture2D.image.width,
            texture2D.image.height,
            0,
            oglFormat.format,
            oglFormat.dataType,
            data
        )
        image.clearUpdateNeeded()
    }
    private fun modifyTexture(dest : Texture2D, src : Image, destX : Int, destY: Int, srcX : Int, srcY : Int, areaW : Int, areaH : Int){
        val image = dest.image
        val id = image.id
        GL46.glActiveTexture(GL46.GL_TEXTURE0)
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, id)
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MAX_LEVEL, 0)
        textureUtil.uploadSubTexture(GL46.GL_TEXTURE_2D, src, 0, destX, destY, srcX, srcY, areaW, areaH, jMonkeyRenderer.isLinearizeSrgbImages)
    }

    fun frameUpdate() {
        if(readyToSwap.compareAndSet(true, false)){
            swapFence = GL32C.glFenceSync(GL32C.GL_SYNC_GPU_COMMANDS_COMPLETE, 0)
            val to = if(usingRed){
                blue
            } else {
                red
            }
            jm.enqueue {
                geom.setTexture(jm.assetManager, to.texture, true)
                to.attach()
            }
            usingRed = !usingRed
            synchronized(swapWait){
                swapWait.notify()
            }
        }
    }
}