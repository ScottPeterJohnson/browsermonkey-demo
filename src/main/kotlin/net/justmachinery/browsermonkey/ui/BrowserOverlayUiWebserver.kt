package net.justmachinery.browsermonkey.ui

import io.javalin.Javalin
import io.javalin.websocket.WsConfig
import kotlinx.html.*
import mu.KLogging
import net.justmachinery.shade.AddScriptStrategy
import net.justmachinery.shade.ShadeRoot
import net.justmachinery.shade.component.Component
import org.eclipse.jetty.websocket.api.Session
import org.intellij.lang.annotations.Language
import java.io.File
import java.net.InetAddress
import java.time.Duration
import java.util.*
import kotlin.reflect.KClass


class BrowserOverlayUiWebserver(
    private val browserOverlayInputListener: BrowserOverlayInputListener,
    private val uiToRender: UiToRender<Any>,
) {
    companion object : KLogging()

    private val shadeRoot by lazy {
        ShadeRoot(
            endpoint = "/shade",
            host = InetAddress.getLoopbackAddress().hostAddress,
            onUncaughtJavascriptException = { _, err ->
                logger.error(err){ "Uncaught JS exception" }
            },
            addScriptStrategy = AddScriptStrategy.AtPath("/shade.js"),
        )
    }

    private lateinit var javalin: Javalin
    private val shadeHandlers = ShadeHandlers(shadeRoot)
    fun init() : String {
        javalin = Javalin.create {
                it.showJavalinBanner = false
            }
            .ws("/shade"){ ws -> shadeHandlers.install(ws) }
            .get("/bundle.js") { it.result(ClassLoader.getSystemClassLoader().getResource("js/bundle.js")!!.readText()) }
            .get("/shade.js") { it.result(ShadeRoot.shadeDevScript) }
            .get("/classpath/*") { it.result(ClassLoader.getSystemClassLoader().getResource(it.path().removePrefix("/classpath/"))!!.readBytes()) }
            .get("/assets/*") {
                val pathUnprefixed = it.path().removePrefix("/")
                val standardFolder = File(pathUnprefixed)
                if(standardFolder.exists()) {
                    it.result(standardFolder.readBytes())
                } else {
                    it.result(File("build").resolve(pathUnprefixed).readBytes())
                }
            }
            .get("/index") { ctx ->
                ctx.contentType("text/html")
                ctx.res().writer.use {
                    shadeRoot.render(it){
                        head {
                            it.style {
                                unsafe {
                                    +"body { margin:0; padding: 0; background: transparent;}"
                                }
                            }
                            it.script(src = "/bundle.js"){}
                        }
                        body {
                            it.add(BrowserOverlayRootComponent::class, BrowserOverlayRootComponent.Props(
                                ui = uiToRender,
                                isGameEngine = ctx.queryParam("fromGameEngine") != null,
                                browserOverlayInputListener = browserOverlayInputListener,
                            ))
                        }
                    }
                }
            }
            .start(InetAddress.getLoopbackAddress().hostAddress, 0)
        val internalBrowserUrl = "http://${InetAddress.getLoopbackAddress().hostAddress}:${javalin.port()}/index"
        logger.info { "Browser url: $internalBrowserUrl" }
        return internalBrowserUrl
    }
    fun destroy(){
        javalin.stop()
    }
}
data class UiToRender<T : Any>(val component : KClass<out Component<T>>, val props : T)


private class BrowserOverlayRootComponent : Component<BrowserOverlayRootComponent.Props>(){
    data class Props(
        val ui : UiToRender<Any>,
        val isGameEngine : Boolean,
        val browserOverlayInputListener: BrowserOverlayInputListener,
    )
    override fun HtmlBlockTag.render() {
        if(props.isGameEngine){
            add(InputRectListener::class, InputRectListener.Props(props.browserOverlayInputListener))
        }
        render {
            div {
                add(props.ui.component, props.ui.props)
            }
        }
    }
}

private class ShadeHandlers(private val shadeRoot: ShadeRoot) {
    val sessions = Collections.synchronizedMap(mutableMapOf<Session, ShadeRoot.MessageHandler>())
    companion object : KLogging()
    fun install(handler : WsConfig){
        handler.onConnect {
            it.session.idleTimeout = Duration.ofDays(999999)
        }
        handler.onError {
            logger.error(it.error()){ "In Shade"}
        }
        handler.onMessage {
            val session = it.session
            if(session.isOpen){
                try {
                    sessions.getOrPut(session) {
                        shadeRoot.handler(
                            send = { session.remote.sendString(it, null) },
                            disconnect = {
                                if(session.isOpen){
                                    session.disconnect()
                                }
                            }
                        )
                    }.onMessage(it.message())
                } catch(t : Throwable){
                    session.disconnect()
                    throw t
                }
            }

        }
        handler.onClose {
            sessions.remove(it.session)?.onDisconnect()
            it.session.close()
        }
    }
}

fun FlowOrMetaDataOrPhrasingContent.rawScript(@Language("JavaScript 1.8") text : String){
    script { unsafe { raw("(()=>{\n$text\n})()") } }
}