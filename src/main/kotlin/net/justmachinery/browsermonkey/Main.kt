package net.justmachinery.browsermonkey

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import com.jme3.app.Application
import com.jme3.app.SimpleApplication
import com.jme3.asset.plugins.HttpZipLocator
import com.jme3.input.KeyInput
import com.jme3.input.controls.ActionListener
import com.jme3.input.controls.InputListener
import com.jme3.input.controls.KeyTrigger
import com.jme3.light.AmbientLight
import com.jme3.light.DirectionalLight
import com.jme3.math.ColorRGBA
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.system.AppSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.html.HtmlBlockTag
import kotlinx.html.button
import kotlinx.html.div
import net.justmachinery.browsermonkey.ui.BrowserOverlayInputListener
import net.justmachinery.browsermonkey.ui.BrowserOverlayUi
import net.justmachinery.browsermonkey.ui.BrowserOverlayUiWebserver
import net.justmachinery.browsermonkey.ui.UiToRender
import net.justmachinery.shade.component.Component
import net.justmachinery.shade.component.MountingContext
import net.justmachinery.shade.state.observable
import net.justmachinery.shade.utility.withStyle
import org.slf4j.LoggerFactory
import kotlin.math.roundToInt


fun main(){
    setupLogging()
    val main = Main()
    main.setSettings(AppSettings(true).apply {
        width = 800
        height = 600
        isResizable = true
    })
    main.start()
}

fun setupLogging(){
    val lc = LoggerFactory.getILoggerFactory() as LoggerContext
    val root = lc.getLogger(Logger.ROOT_LOGGER_NAME)
    root.level = Level.INFO
    lc.getLogger("net.justmachinery").level = Level.DEBUG
}

class Main : SimpleApplication() {
    val browserOverlayInputListener = BrowserOverlayInputListener(this)

    @Suppress("UNCHECKED_CAST")
    val webserver = BrowserOverlayUiWebserver(
        browserOverlayInputListener = browserOverlayInputListener,
        uiToRender = UiToRender(MainUi::class, MainUi.Props(this)) as UiToRender<Any>
    )
    val ui = BrowserOverlayUi(
        jm = this,
        browserOverlayUiWebserver = webserver,
        browserOverlayInputListener = browserOverlayInputListener
    )
    override fun simpleInitApp() {
        ui.init()


        //Set up a simple 3d scene to look at
        assetManager.registerLocator(
            "https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/jmonkeyengine/town.zip",
            HttpZipLocator::class.java
        )
        val sceneModel = assetManager.loadModel("main.scene")
        sceneModel.setLocalScale(2f)
        rootNode.attachChild(sceneModel)

        val al = AmbientLight()
        al.setColor(ColorRGBA.White.mult(1.3f))
        rootNode.addLight(al)

        val dl = DirectionalLight()
        dl.setColor(ColorRGBA.White)
        dl.setDirection(Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal())
        rootNode.addLight(dl)

        cam.location = cam.location.add(0f,5f,0f)
        flyCam.setMoveSpeed(100f)
        inputManager.addMapping("ToggleFlyCam", KeyTrigger(KeyInput.KEY_SPACE))
        inputManager.addListener(object : ActionListener {
            override fun onAction(name: String, isPressed: Boolean, tpf: Float) {
                if(isPressed){
                    flyCam.isEnabled = !flyCam.isEnabled
                }
            }
        }, "ToggleFlyCam")
    }

    override fun reshape(w: Int, h: Int) {
        super.reshape(w, h)
        ui.onWindowSizeChange(w, h)
    }

    override fun simpleUpdate(tpf: Float) {
        ui.render(tpf)
    }

    override fun destroy() {
        super.destroy()
        webserver.destroy()
        ui.destroy()
        System.exit(0) //TODO: For some reason, can't shut down the AWT threads?
    }
}

class MainUi : Component<MainUi.Props>() {
    data class Props(val application: Application)

    var cameraX by observable(0)
    var cameraY by observable(0)
    var cameraZ by observable(0)

    override fun MountingContext.mounted() {
        launch {
            while(true){
                cameraX = props.application.camera.location.x.roundToInt()
                cameraY = props.application.camera.location.y.roundToInt()
                cameraZ = props.application.camera.location.z.roundToInt()
                delay(50)
            }
        }
    }
    override fun HtmlBlockTag.render() {
        div {
            withStyle {
                width = 100.vw
                height = 100.vh
                display = Display.grid
                gridTemplateColumns = GridTemplateColumns("repeat(3, 1fr)")
                gridTemplateRows = GridTemplateRows("repeat(3, 1fr)")
            }
            div(classes = "browserClickCapture browserScrollCapture") {
                withStyle {
                    declarations["grid-area"] = "3 / 2 / 4 / 3"
                    backgroundColor = Color("#282828").changeAlpha(0.9)
                    border = "1px solid black"
                    padding = "5px"
                    color = Color.whiteSmoke
                    overflow = Overflow.hidden
                    maxWidth = 20.vw
                    width = 20.vw
                }
                div {
                    +"This is HTML browser content. Space to toggle flycam/cursor."
                }
                button {
                    onClick {
                        props.application.camera.location = Vector3f(0f,5f,0f)
                        props.application.camera.rotation = Quaternion.DIRECTION_Z
                    }
                    +"Reset Camera"
                }
                div {
                    +"Camera X/Y/Z: $cameraX/$cameraY/$cameraZ"
                }
            }
        }
    }

}