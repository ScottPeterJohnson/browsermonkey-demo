package net.justmachinery.browsermonkey.ui

import com.jme3.app.SimpleApplication
import com.jme3.material.Material
import com.jme3.material.RenderState
import com.jme3.renderer.opengl.GLRenderer
import com.jme3.scene.Geometry
import com.jme3.scene.shape.Quad
import com.jme3.system.lwjgl.LwjglWindow
import com.jme3.texture.Texture2D
import me.friwi.jcefmaven.CefAppBuilder
import mu.KLogging
import net.justmachinery.futility.execution.background
import org.cef.CefSettings
import org.cef.browser.CefBrowser
import org.cef.browser.CefBrowserInitData
import org.cef.browser.CefMessageRouter
import org.cef.handler.CefDisplayHandlerAdapter
import sun.misc.Signal
import sun.misc.SignalHandler
import java.io.File
import kotlin.system.exitProcess

class BrowserOverlayUi(
    private val jm: SimpleApplication,
    private val browserOverlayUiWebserver: BrowserOverlayUiWebserver,
    private val browserOverlayInputListener: BrowserOverlayInputListener,
) {
    companion object : KLogging()

    private var mat : Material? = null
    private var geom : Geometry? = null

    private fun onTextureChange(newTexture : Texture2D){
        mat?.setTexture("ColorMap", newTexture)
    }
    fun onWindowSizeChange(w : Int, h : Int){
        updateGeomSize(w, h)
        browser?.notifyResize(w, h)
    }
    private fun updateGeomSize(width : Int, height : Int){
        geom?.setLocalScale(width.toFloat(), height.toFloat(), 1f)
    }


    private var browser : JMonkeyCefBrowser? = null
    fun init(){
        val mesh = Quad(1f, 1f, true)
        val mat = Material(jm.assetManager, "Common/MatDefs/Misc/Unshaded.j3md").apply {
            additionalRenderState.blendMode = RenderState.BlendMode.Alpha
        }
        this.mat = mat
        geom = Geometry("Browser", mesh).apply {
            material = mat
            jm.guiNode.attachChild(this)
        }
        updateGeomSize(jm.camera.width, jm.camera.height)
        background {
            val internalBrowserUrl = browserOverlayUiWebserver.init()
            createCefBrowser("$internalBrowserUrl?fromGameEngine=true")
            browserOverlayInputListener.init(browser!!)
        }
    }

    private fun createCefBrowser(url : String){
        val builder = CefAppBuilder()
        builder.cefSettings.root_cache_path = File("build/jcef-cache").absolutePath

        builder.setInstallDir(File("build/jcef-bundle"))
        builder.setProgressHandler { state, percent -> logger.info { "CEF Init $state $percent" } }
        val app = builder.build()

        val client = restoreJvmSignalHandlers { app.createClient() }
        client.addDisplayHandler(object : CefDisplayHandlerAdapter(){
            override fun onConsoleMessage(
                browser: CefBrowser?,
                level: CefSettings.LogSeverity?,
                message: String?,
                source: String?,
                line: Int
            ): Boolean {
                logger.info { "Console: ($source $line) $level: $message"}
                return super.onConsoleMessage(browser, level, message, source, line)
            }
        })


        val router = CefMessageRouter.create()
        client.addMessageRouter(router)

        browser = JMonkeyCefBrowser(
            init = CefBrowserInitData(
                initialWidth = jm.camera.width,
                initialHeight = jm.camera.height,
                client = client,
                currentUrl = url,
                isTransparent = true,
                context = null,
                inspectAt = null,
                settings = null,
            ),
            jMonkeyRenderer = (jm.context as LwjglWindow).renderer as GLRenderer,
            onTextureChange = ::onTextureChange
        )
    }


    private fun <T> restoreJvmSignalHandlers(cb : ()->T) : T{
        //These are the default signal handlers that CEF seems to override
        val handler = SignalHandler {
            println("FATAL: Signal handler triggered while initializing CEF")
            exitProcess(it.number + 512)
        }
        val signals = listOf("HUP", "INT", "TERM")
        val oldHandlers = signals.map { Signal.handle(Signal(it), handler) }
        try {
            return cb()
        } finally {
            signals.forEachIndexed { index, signal ->
                Signal.handle(Signal(signal), oldHandlers[index])
            }
        }
    }

    fun destroy(){
        browser?.cef?.close(true)
    }

    fun render(secondsSinceLastFrame: Float) {
        browser?.renderIfNecessary()
    }
}