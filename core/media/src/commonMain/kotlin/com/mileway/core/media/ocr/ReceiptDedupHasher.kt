package com.mileway.core.media.ocr

/** 64-bit horizontal difference hash. Absolute brightness does not affect adjacent comparisons. */
object ReceiptDedupHasher {
    const val WIDTH = 9
    const val HEIGHT = 8
    private const val MAX_LUMA = 255

    /** Hashes a row-major luma image, resampling larger synthetic grids to [WIDTH] by [HEIGHT]. */
    fun hash(luma: IntArray, width: Int = WIDTH, height: Int = HEIGHT): Long {
        require(width >= WIDTH && height >= HEIGHT && luma.size == width * height)
        require(luma.all { it in 0..MAX_LUMA })
        var hash = 0L
        for (y in 0 until HEIGHT) {
            val row = y * (height - 1) / (HEIGHT - 1) * width
            for (x in 0 until WIDTH - 1) {
                val left = luma[row + x * (width - 1) / (WIDTH - 1)]
                val right = luma[row + (x + 1) * (width - 1) / (WIDTH - 1)]
                if (left > right) hash = hash or (1L shl (y * (WIDTH - 1) + x))
            }
        }
        return hash
    }
}

/** Samples a local image to a 9 by 8 grayscale grid, or null if the platform cannot read it. */
expect suspend fun sampleReceiptLuma(uri: String): IntArray?
