package net.justmachinery.browsermonkey.ui

import mu.KLogging
import net.justmachinery.feral.client.ui.browser.JMonkeyCefBrowser
import org.lwjgl.BufferUtils
import java.awt.Rectangle
import java.nio.ByteBuffer

/**
 * Coordinates and holds screen paint data between threads.
 */
class PaintHolder {
    companion object : KLogging()

    class PaintBuffer private constructor(
        val raw : ByteBuffer,
        val width : Int,
        val height : Int,
    ){
        companion object {
            fun create(width : Int, height : Int) : PaintBuffer {
                val size = (width * height) shl 2
                return PaintBuffer(
                    raw = BufferUtils.createByteBuffer(size),
                    width = width,
                    height = height
                )
            }
            fun wrap(buffer : ByteBuffer, width : Int, height : Int) : PaintBuffer {
                val size = (width * height) shl 2
                if(buffer.limit() != size){
                    throw IllegalArgumentException("Got buffer of ${buffer.limit()} but expect max of $size")
                }
                return PaintBuffer(buffer, width, height)
            }
        }

        fun copyAll(other : PaintBuffer){
            raw.position(0)
            other.raw.position(0)
            raw.put(other.raw)
        }
        fun copyFrom(other : PaintBuffer, otherStartX : Int, otherStartY : Int, destinationStartX : Int, destinationStartY : Int, width : Int, height : Int){
            for(y in (0 until height)){
                val sourceY = otherStartY + y
                val existingY = destinationStartY + y
                val sourceStart = (otherStartX + sourceY*other.width)*4
                val existingStart = (destinationStartX + existingY * this.width)*4
                raw.put(existingStart, other.raw, sourceStart, width*4)
            }
        }
    }

    private class PaintData(
        val buffer: PaintBuffer
    ) {
        val width get() = buffer.width
        val height get() = buffer.height

        var toPaint : PaintRequest? = null
        fun addDirty(rects : Array<Rectangle>){
            val paint = toPaint
            if (paint != null) {
                paint.addDirty(rects)
            } else {
                toPaint = PaintRequest.Dirty(mutableListOf(rects))
            }
        }
    }
    private sealed class PaintRequest {
        data object All : PaintRequest()
        class Dirty(val rects : MutableList<Array<Rectangle>>) : PaintRequest()

        fun addDirty(rects : Array<Rectangle>){
            when(this){
                All -> {}
                is Dirty -> {
                    this.rects.add(rects)
                }
            }
        }
    }
    private var paintData : PaintData? = null
    private val paint = Object()

    fun paintFromScreenBuffer(
        buffer: PaintBuffer,
        dirtyRects: Array<Rectangle>
    ) {

        synchronized(paint) {
            val existing = paintData
            if(existing == null || (existing.width != buffer.width || existing.height != buffer.height)){
                logger.trace { "New paint frame: ${buffer.width}, ${buffer.height}" }
                val newBuffer = PaintBuffer.create(buffer.width, buffer.height)
                val new = PaintData(
                    buffer = newBuffer
                )
                paintData = new
                new.toPaint = (PaintRequest.All)

                //Copy the whole buffer over
                newBuffer.copyAll(buffer)
            } else {
                val target = existing.buffer
                logger.trace { "Got repaint (${existing.width}, ${existing.height})" }
                //Copy just the dirty rects
                for(rect in dirtyRects){
                    target.copyFrom(
                        other = buffer,
                        otherStartX = rect.x,
                        otherStartY = rect.y,
                        destinationStartX = rect.x,
                        destinationStartY = rect.y,
                        width = rect.width,
                        height = rect.height
                    )
                }
                existing.addDirty(dirtyRects)
            }
            paint.notifyAll()
        }
    }
    fun paintFromPartialBuffer(
        sourceBuffer : PaintBuffer,
        sourceDirty : Iterable<Rectangle>,
        sourceInExisting : Rectangle,
    ){
        synchronized(paint){
            val existing = paintData
            if(existing != null){
                val newSourceDirty = mutableListOf<Rectangle>()
                val targetDirty = mutableListOf<Rectangle>()
                for (originalSourceDirty in sourceDirty) {
                    val inExisting = Rectangle(sourceInExisting.x + originalSourceDirty.x, sourceInExisting.y + originalSourceDirty.y, originalSourceDirty.width, originalSourceDirty.height)
                    val constrainedToExisting = constrain(Rectangle(0, 0, existing.width, existing.height), inExisting) ?: continue
                    targetDirty.add(constrainedToExisting)
                    newSourceDirty.add(Rectangle(constrainedToExisting.x - sourceInExisting.x, constrainedToExisting.y - sourceInExisting.y, constrainedToExisting.width, constrainedToExisting.height))
                }

                paintFromPartialBuffer(
                    sourceBuffer = sourceBuffer,
                    sourceDirty = newSourceDirty,
                    targetDirty = targetDirty,
                )
            }
        }
    }
    fun paintFromPartialBuffer(
        sourceBuffer : PaintBuffer,
        sourceDirty : Iterable<Rectangle>,
        targetDirty : Iterable<Rectangle>,
    ){
        synchronized(paint){
            val existing = paintData
            if(existing != null){
                for ((source, target) in sourceDirty.zip(targetDirty)) {
                    existing.buffer.copyFrom(sourceBuffer, source.x, source.y, target.x, target.y, source.width, source.height)
                }
                existing.addDirty(targetDirty.toList().toTypedArray<Rectangle>())
                paint.notifyAll()
            }
        }
    }

    private fun constrain(outer : Rectangle, inner : Rectangle) : Rectangle? {
        val intersectX = maxOf(inner.x, outer.x)
        val intersectY = maxOf(inner.y, outer.y)
        val intersectRight = minOf(inner.x + inner.width, outer.x + outer.width)
        val intersectBottom = minOf(inner.y + inner.height, outer.y + outer.height)

        if (intersectRight <= intersectX || intersectBottom <= intersectY) {
            return null
        }

        return Rectangle(intersectX, intersectY, intersectRight - intersectX, intersectBottom - intersectY)
    }

    fun renderIfNecessary(draw : (JMonkeyCefBrowser.DrawRequest)->Unit, wait : Boolean){
        synchronized(paint) {
            while(true){
                val paintData = this.paintData
                val toPaint = paintData?.toPaint
                if(paintData != null && toPaint != null){
                    paintData.toPaint = null
                    when(toPaint){
                        PaintRequest.All -> {
                            logger.trace { "Repaint all: (${paintData.width}, ${paintData.height})" }
                            // Update/resize the whole texture.
                            draw(
                                JMonkeyCefBrowser.DrawRequest(
                                    all = true,
                                    buffer = paintData.buffer,
                                    dirty = listOf(Rectangle(0, 0, paintData.width, paintData.height))
                                )
                            )
                        }
                        is PaintRequest.Dirty -> {
                            logger.trace  { "Repaint dirty: (${paintData.width}, ${paintData.height})" }
                            val rects = mergeRectangles(toPaint.rects.asSequence().flatMap { it.asSequence() })
                            draw(
                                JMonkeyCefBrowser.DrawRequest(
                                    all = false,
                                    buffer = paintData.buffer,
                                    dirty = rects,
                                )
                            )
                        }
                    }
                    break
                } else {
                    if(!wait){
                        break
                    } else {
                        paint.wait()
                    }
                }
            }
        }
    }
}