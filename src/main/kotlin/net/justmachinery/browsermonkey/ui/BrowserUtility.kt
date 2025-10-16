package net.justmachinery.feral.client.ui.browser

import com.jme3.post.SceneProcessor
import com.jme3.profile.AppProfiler
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.renderer.queue.RenderQueue
import com.jme3.texture.FrameBuffer
import kotlinx.css.*
import kotlinx.html.DIV
import kotlinx.html.HtmlBlockTag
import kotlinx.html.div
import kotlinx.html.span
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import net.justmachinery.shade.utility.withStyle
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

//These are from top-left right and down, unlike jMonkey which is right and up
data class BrowserScreenCoords(val x : Int, val y : Int)

inline fun HtmlBlockTag.flexColumn(grow:Int=0, crossinline cb : DIV.()->Unit){
    div {
        withStyle {
            display = Display.flex
            flexDirection = FlexDirection.column
            overflow = Overflow.hidden
            if(grow != 0){
                flex = Flex(grow)
            }
        }
        cb()
    }
}

inline fun HtmlBlockTag.flexRow(grow:Int=0, crossinline cb : DIV.()->Unit){
    div {
        withStyle {
            display = Display.flex
            overflow = Overflow.hidden
            if(grow != 0){
                flex = Flex(grow)
            }
        }
        cb()
    }
}

fun HtmlBlockTag.flexSpacer(){
    span {
        withStyle {
            flex = Flex(flexGrow = 1.0)
        }
    }
}

fun SocketChannel.readInt() : Int {
    val result = ByteArray(4)
    val wrapped = ByteBuffer.wrap(result)
    while(wrapped.hasRemaining()){
        read(wrapped)
    }
    return wrapped.getInt(0)
}


fun SocketChannel.readExactly(buffer : ByteBuffer){
    while(buffer.hasRemaining()){
        read(buffer)
    }
}
fun SocketChannel.readExactly(bytes : Int) : ByteArray {
    val result = ByteArray(bytes)
    val wrapped = ByteBuffer.wrap(result)
    while(wrapped.hasRemaining()){
        read(wrapped)
    }
    return result
}


fun SocketChannel.writeInt(int : Int) {
    val result = ByteArray(4)
    val wrapped = ByteBuffer.wrap(result)
    wrapped.putInt(0, int)
    write(wrapped)
}

val json by lazy {
    Json {
        allowStructuredMapKeys = true
        //So long as it deserializes correctly, unknown keys are okay. This makes adding new behaviors with defaults easier.
        ignoreUnknownKeys = true
    }
}

val proto by lazy {
    ProtoBuf {
    }
}


open class AbstractSceneProcessor : SceneProcessor {
    var initialized = false
    override fun initialize(rm: RenderManager?, vp: ViewPort?) {
        initialized = true
    }
    override fun isInitialized() = initialized

    override fun reshape(vp: ViewPort?, w: Int, h: Int) {}


    override fun preFrame(tpf: Float) {}

    override fun postQueue(rq: RenderQueue?) {}

    override fun postFrame(out: FrameBuffer?) {}

    override fun cleanup() {}

    override fun setProfiler(profiler: AppProfiler?) {}

}