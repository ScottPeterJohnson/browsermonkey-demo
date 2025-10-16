package net.justmachinery.feral.client.ui.browser

import me.friwi.jcefmaven.CefAppBuilder
import mu.KLogging
import net.justmachinery.browsermonkey.ui.JMonkeyCefBrowserImpl
import net.justmachinery.browsermonkey.ui.JMonkeyCefBrowserInSeperateVmProxy
import net.justmachinery.futility.execution.ShutdownHookPriority
import net.justmachinery.futility.execution.addJvmShutdownHook
import net.justmachinery.futility.nullOnException
import org.cef.CefSettings
import org.cef.browser.CefBrowser
import org.cef.browser.CefBrowserInitData
import org.cef.browser.CefMessageRouter
import org.cef.handler.CefDisplayHandlerAdapter
import sun.misc.Signal
import sun.misc.SignalHandler
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.concurrent.thread
import kotlin.system.exitProcess


class JMonkeyCefBrowserFactory(){
    companion object : KLogging(){
        fun createActualBrowser(
            initialWidth : Int,
            initialHeight : Int,
        ) : JMonkeyCefBrowser {
            val builder = CefAppBuilder()
            val cefBundlePath  = if(File("cef/bundle").exists()){
                File("cef/bundle")
            } else {
                File("build/jcef-bundle")
            }
            val cefCachePath : File = reserveCefCachePath()
            builder.cefSettings.root_cache_path = cefCachePath.absolutePath

            builder.setInstallDir(cefBundlePath)
            builder.setProgressHandler { state, percent -> logger.info { "CEF Init $state $percent" } }
            builder.cefSettings.windowless_rendering_enabled = true
            builder.cefSettings.uncaught_exception_stack_size = 100
            val app = builder.build()

            logger.trace { "Create CEF client" }
            val client = restoreJvmSignalHandlers { app.createClient() }
            logger.trace { "CEF client ready" }
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

            return JMonkeyCefBrowserImpl(
                init = CefBrowserInitData(
                    initialWidth = initialWidth,
                    initialHeight = initialHeight,
                    client = client,
                    isTransparent = true,
                    context = null,
                    inspectAt = null,
                    settings = null,
                ),
            )
        }

        private fun reserveCefCachePath() : File {
            val intoFolder = File("cef").takeIf { it.exists() } ?: File("build/jcef-cache")
            intoFolder.mkdirs()

            var i = 0
            while(true){
                val cacheFolder = intoFolder.resolve("cache$i")
                cacheFolder.mkdirs()
                if(cacheFolder.isDirectory){
                    val lockFile = cacheFolder.resolve("feralcache.lock")
                    val suitable = if(lockFile.createNewFile()){
                        true
                    } else {
                        val old = Instant.ofEpochMilli(lockFile.lastModified()).isBefore(Instant.now().minus(12, ChronoUnit.HOURS))
                        if(old){
                            lockFile.delete()
                            lockFile.createNewFile()
                        } else {
                            false
                        }
                    }
                    if(suitable){
                        thread(name = "JCEF Cache Lock Updater", isDaemon = true) {
                            while(true){
                                lockFile.setLastModified(Instant.now().toEpochMilli())
                                try {
                                    Thread.sleep(30 * 60 * 1000)
                                } catch(t : InterruptedException){}
                            }
                        }
                        addJvmShutdownHook(ShutdownHookPriority(0.0)){
                            lockFile.delete()
                        }
                        return cacheFolder
                    }
                    i += 1
                }
            }
        }


        private fun <T> restoreJvmSignalHandlers(cb : ()->T) : T{
            //These are the default signal handlers that CEF seems to override
            val handler = SignalHandler {
                println("FATAL: Signal handler triggered while initializing CEF")
                exitProcess(it.number + 512)
            }
            val signals = listOf("HUP", "INT", "TERM").mapNotNull { nullOnException {   Signal(it) } }
            val oldHandlers = signals.map { Signal.handle(it, handler) }
            try {
                return cb()
            } finally {
                signals.forEachIndexed { index, signal ->
                    Signal.handle(signal, oldHandlers[index])
                }
            }
        }
    }

    fun createCefBrowser(
        inSeparateVm : Boolean,
        initialWidth : Int,
        initialHeight : Int,
    ) : JMonkeyCefBrowser {
        if(inSeparateVm){
            val browser = JMonkeyCefBrowserInSeperateVmProxy(
                initialWidth = initialWidth,
                initialHeight = initialHeight,
            )
            browser.start()
            return browser
        } else {
            return createActualBrowser(initialWidth, initialHeight)
        }
    }

}