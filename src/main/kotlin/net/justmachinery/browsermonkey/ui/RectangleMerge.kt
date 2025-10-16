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
private fun computeUnionArea(rectangles: List<Rectangle>): Int {
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
    data class Event(val x: Int, val rect: Rectangle, val isStart: Boolean)

    val events = rectangles.flatMap { rect ->
        listOf(Event(rect.x, rect, true), Event(rect.x + rect.width, rect, false))
    }.sortedBy { it.x }.toList()

    if (events.isEmpty()) return emptyList()

    val active = mutableSetOf<Rectangle>()
    val result = mutableListOf<Rectangle>()
    var lastX = events.first().x

    for (event in events) {
        if (event.x > lastX && active.isNotEmpty()) {
            val yRanges = active
                .asSequence()
                .map { it.y .. (it.y + it.height) }
                .sortedBy { it.first }
                .fold(mutableListOf<IntRange>()) { acc, range ->
                    if (acc.isEmpty() || acc.last().last < range.first) {
                        acc.add(range)
                    } else {
                        acc[acc.lastIndex] = acc.last().first..maxOf(acc.last().last, range.last)
                    }
                    acc
                }

            yRanges.forEach { range ->
                result.add(Rectangle(lastX, range.first, event.x - lastX, range.last - range.first))
            }
        }

        if (event.isStart) active.add(event.rect) else active.remove(event.rect)
        lastX = event.x
    }

    return result
}