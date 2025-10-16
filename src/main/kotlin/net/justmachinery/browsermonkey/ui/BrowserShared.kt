package net.justmachinery.browsermonkey.ui

import io.javalin.Javalin
import io.javalin.websocket.WsConfig
import mu.KLogging
import net.justmachinery.shade.AddScriptStrategy
import net.justmachinery.shade.ShadeRoot
import org.eclipse.jetty.http.MimeTypes
import org.eclipse.jetty.websocket.api.Session
import java.io.File
import java.net.InetAddress
import java.time.Duration
import java.util.*

class BrowserShared(){
    companion object : KLogging()

    val shadeRoot by lazy {
        ShadeRoot(
            endpoint = "/shade",
            host = InetAddress.getLoopbackAddress().hostAddress,
            onUncaughtJavascriptException = { _, err ->
                logger.error(err){ "Uncaught JS exception" }
            },
            addScriptStrategy = AddScriptStrategy.AtPath("/shade.js"),
        )
    }
    private val shadeHandlers = ShadeHandlers(shadeRoot)

    fun configureJavalinForShade(javalin : Javalin){
        javalin
            .ws("/shade"){ ws -> shadeHandlers.install(ws) }
            .get("/bundle.js") { it.result(ClassLoader.getSystemClassLoader().getResource("js/bundle.js")!!.readText()) }
            .get("/shade.js") { it.result(ShadeRoot.shadeDevScript) }
            .get("/classpath/*") {
                it.contentType(MimeTypes.getDefaultMimeByExtension(it.path()))
                it.result(ClassLoader.getSystemClassLoader().getResource(it.path().removePrefix("/classpath/"))!!.readBytes())
            }
            .get("/assets/*") {
                val pathUnprefixed = it.path().removePrefix("/")
                val standardFolder = File(pathUnprefixed)
                if(standardFolder.exists()) {
                    it.result(standardFolder.readBytes())
                } else {
                    it.result(File("build").resolve(pathUnprefixed).readBytes())
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
}