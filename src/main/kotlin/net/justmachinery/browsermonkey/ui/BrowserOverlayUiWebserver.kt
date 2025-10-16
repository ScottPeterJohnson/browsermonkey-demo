package net.justmachinery.browsermonkey.ui

import io.javalin.Javalin
import io.javalin.util.ConcurrencyUtil
import kotlinx.html.*
import mu.KLogging
import net.justmachinery.shade.component.Component
import net.justmachinery.shade.state.ObservableValue
import org.intellij.lang.annotations.Language
import java.net.InetAddress
import kotlin.reflect.KClass

class BrowserOverlayUiWebserver(
    private val browserOverlayInputListener: BrowserOverlayInputListener,
    private val uiToRender: UiToRender<Any>,
) {
    companion object : KLogging()


    private lateinit var javalin: Javalin
    fun init() : String {
        val browserShared = BrowserShared()
        javalin = Javalin.create {
            it.jetty.threadPool = ConcurrencyUtil.jettyThreadPool("JettyServerThreadPool", 1, 10, false)
            it.showJavalinBanner = false
        }
            .also {
                browserShared.configureJavalinForShade(it)
            }
            .get("/index") { ctx ->
                ctx.contentType("text/html")
                ctx.res().writer.use {
                    browserShared.shadeRoot.render(it){
                        head {
                            it.style {
                                unsafe {
                                    +"body { background: transparent;}\n"
                                    +"* { margin: 0; padding: 0; box-sizing: border-box; }\n"
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


fun FlowOrMetaDataOrPhrasingContent.rawScript(@Language("JavaScript 1.8") text : String){
    script { unsafe { raw("(()=>{\n$text\n})()") } }
}