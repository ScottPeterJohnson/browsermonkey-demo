package net.justmachinery.browsermonkey.ui

import java.awt.Rectangle

fun main() {
    // Test cases
    val testCases = listOf(
        // Test 1: Simple overlapping rectangles
        listOf(
            Rectangle(0, 0, 10, 10),
            Rectangle(5, 5, 10, 10)
        ),

        // Test 2: Non-overlapping rectangles
        listOf(
            Rectangle(0, 0, 5, 5),
            Rectangle(10, 10, 5, 5)
        ),

        // Test 3: Completely contained rectangle
        listOf(
            Rectangle(0, 0, 20, 20),
            Rectangle(5, 5, 5, 5)
        ),

        // Test 4: L-shaped arrangement
        listOf(
            Rectangle(0, 0, 10, 5),
            Rectangle(0, 5, 5, 5)
        ),

        // Test 5: Complex overlapping set
        listOf(
            Rectangle(0, 0, 10, 10),
            Rectangle(5, 5, 10, 10),
            Rectangle(15, 0, 5, 20),
            Rectangle(2, 8, 20, 5)
        )
    )

    // Run tests
    for ((index, testCase) in testCases.withIndex()) {
        println("Test Case ${index + 1}:")
        println("Input rectangles:")
        testCase.forEachIndexed { i, r -> println("  ${i + 1}: $r") }

        val result = mergeRectangles(testCase.asSequence())

        println("Result (${result.size} non-overlapping rectangles):")
        result.forEachIndexed { i, r -> println("  ${i + 1}: $r") }

        // Simple verification: Total area should be the same or less than union of original
        val originalUnion = computeUnionArea(testCase)
        val resultUnion = computeUnionArea(result)

        println("Original union area: $originalUnion")
        println("Result union area: $resultUnion")
        println("Areas match: ${originalUnion == resultUnion}")
        println()
    }
}

// Helper function to compute area of the union of rectangles
fun computeUnionArea(rectangles: List<Rectangle>): Int {
    if (rectangles.isEmpty()) return 0

    // For this simple test, we'll create a bitmap and count pixels
    // This is inefficient but conceptually simple for verification

    // Find bounds
    var minX = Int.MAX_VALUE
    var minY = Int.MAX_VALUE
    var maxX = Int.MIN_VALUE
    var maxY = Int.MIN_VALUE

    for (rect in rectangles) {
        minX = minOf(minX, rect.x)
        minY = minOf(minY, rect.y)
        maxX = maxOf(maxX, rect.x + rect.width)
        maxY = maxOf(maxY, rect.y + rect.height)
    }

    if (maxX - minX > 1000 || maxY - minY > 1000) {
        // Too large for bitmap approach, approximate with sum of areas
        return rectangles.sumOf { it.width * it.height }
    }

    // Create bitmap
    val width = maxX - minX
    val height = maxY - minY
    val bitmap = Array(height) { BooleanArray(width) { false } }

    // Mark rectangles
    for (rect in rectangles) {
        for (y in rect.y until rect.y + rect.height) {
            for (x in rect.x until rect.x + rect.width) {
                if (x - minX in 0 until width && y - minY in 0 until
                    height) {
                    bitmap[y - minY][x - minX] = true
                }
            }
        }
    }

    // Count pixels
    return bitmap.sumOf { row -> row.count { it } }
}


fun mergeRectangles(rectangles: Sequence<Rectangle>): List<Rectangle> {
    // Create events for rectangle boundaries
    data class Event(val x: Int, val rect: Rectangle, val isStart: Boolean)
    val events = mutableListOf<Event>()

    for (rect in rectangles) {
        events.add(Event(rect.x, rect, true))
        events.add(Event(rect.x + rect.width, rect, false))
    }

    if(events.isEmpty()){ return emptyList() }

    // Sort events by x-coordinate
    events.sortBy { it.x }

    // Active rectangles (those that intersect the current sweep line)
    val active = mutableListOf<Rectangle>()

    // Result rectangles
    val result = mutableListOf<Rectangle>()

    var lastX = events.first().x

    for (event in events) {
        val currentX = event.x

        // Process segments between events
        if (currentX > lastX && active.isNotEmpty()) {
            // Find y-ranges covered by active rectangles
            val yRanges = mutableListOf<IntRange>()

            for (rect in active) {
                val yStart = rect.y
                val yEnd = rect.y + rect.height

                // Merge with existing ranges if overlapping
                var merged = false
                for (i in yRanges.indices) {
                    val range = yRanges[i]

                    // Check for overlap or adjacency
                    if (!(yEnd < range.first || yStart > range.last)) {
                        // Merge ranges
                        yRanges[i] = minOf(yStart,
                            range.first)..maxOf(yEnd, range.last)
                        merged = true
                        break
                    }
                }

                if (!merged) {
                    yRanges.add(yStart..yEnd)
                }

                // Merge adjacent or overlapping ranges
                var i = 0
                while (i < yRanges.size - 1) {
                    val r1 = yRanges[i]
                    val r2 = yRanges[i + 1]

                    if (r1.last >= r2.first - 1) {
                        yRanges[i] = r1.first..maxOf(r1.last, r2.last)
                        yRanges.removeAt(i + 1)
                    } else {
                        i++
                    }
                }
            }

            // Create rectangles from segments
            for (range in yRanges) {
                result.add(Rectangle(lastX, range.first, currentX - lastX,
                    range.last - range.first))
            }
        }

        // Update active rectangles
        if (event.isStart) {
            active.add(event.rect)
        } else {
            active.remove(event.rect)
        }

        lastX = currentX
    }

    return result
}
