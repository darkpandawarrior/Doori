package com.mileway.core.data.session

/**
 * PLAN_V22 P2.3: a pure, `commonMain`-only SHA-256 so [PinHashSource] implementations never need
 * `java.security.MessageDigest` (JVM-only) or a platform crypto framework — this is a demo PIN
 * gate, not a security boundary, so a small self-contained hash beats pulling in a KMP crypto
 * dependency for one call site. Not swappable/pluggable by design: this is the one hash Mileway's
 * PIN gate uses everywhere (Android + iOS), so a single top-level function is simpler than an
 * interface with one implementation.
 */
private const val WORDS_IN_BLOCK = 64
private const val HASH_WORD_COUNT = 8

private val K =
    intArrayOf(
        0x428a2f98.toInt(),
        0x71374491.toInt(),
        -0x4a3f0431,
        -0x164a245b,
        0x3956c25b.toInt(),
        0x59f111f1.toInt(),
        -0x6dc07d5c,
        -0x54e3a12b,
        -0x27f85568,
        0x12835b01.toInt(),
        0x243185be.toInt(),
        0x550c7dc3.toInt(),
        0x72be5d74.toInt(),
        -0x7f214e02,
        -0x6423f959,
        -0x3e640e8c,
        -0x1b64963f,
        -0x1041b87a,
        0x0fc19dc6.toInt(),
        0x240ca1cc.toInt(),
        0x2de92c6f.toInt(),
        0x4a7484aa.toInt(),
        0x5cb0a9dc.toInt(),
        0x76f988da.toInt(),
        -0x67c1aeae,
        -0x57ce3993,
        -0x4ffcd838,
        -0x40a68039,
        -0x391ff40d,
        -0x2a586eb9,
        0x06ca6351.toInt(),
        0x14292967.toInt(),
        0x27b70a85.toInt(),
        0x2e1b2138.toInt(),
        0x4d2c6dfc.toInt(),
        0x53380d13.toInt(),
        0x650a7354.toInt(),
        0x766a0abb.toInt(),
        -0x7e3d36d2,
        -0x6d8dd37b,
        -0x5d40175f,
        -0x57e599b5,
        -0x3db47490,
        -0x3893ae5d,
        -0x2e6d17e7,
        -0x2966f9dc,
        -0xbf1ca7b,
        0x106aa070.toInt(),
        0x19a4c116.toInt(),
        0x1e376c08.toInt(),
        0x2748774c.toInt(),
        0x34b0bcb5.toInt(),
        0x391c0cb3.toInt(),
        0x4ed8aa4a.toInt(),
        0x5b9cca4f.toInt(),
        0x682e6ff3.toInt(),
        0x748f82ee.toInt(),
        0x78a5636f.toInt(),
        -0x7b3787ec,
        -0x7338fdf8,
        -0x6f410006,
        -0x5baf9315,
        -0x41065c09,
        -0x398e870e,
    )

private fun rotr(
    x: Int,
    n: Int,
): Int = (x ushr n) or (x shl (32 - n))

/** SHA-256 of [input], returned as a lowercase hex digest. Pure function, deterministic. */
fun sha256Hex(input: String): String {
    val message = input.encodeToByteArray()
    val h =
        intArrayOf(
            0x6a09e667.toInt(),
            -0x4498517b,
            0x3c6ef372.toInt(),
            -0x5ab00ac6,
            0x510e527f.toInt(),
            -0x64fa9774,
            0x1f83d9ab.toInt(),
            0x5be0cd19.toInt(),
        )

    // Padding: append 0x80, zero-pad, then 64-bit big-endian bit length.
    val bitLength = message.size.toLong() * 8L
    val paddedLength = ((message.size + 9 + 63) / 64) * 64
    val padded = ByteArray(paddedLength)
    message.copyInto(padded)
    padded[message.size] = 0x80.toByte()
    for (i in 0 until 8) {
        padded[paddedLength - 1 - i] = ((bitLength ushr (8 * i)) and 0xFF).toByte()
    }

    val w = IntArray(WORDS_IN_BLOCK)
    var offset = 0
    while (offset < padded.size) {
        for (t in 0 until 16) {
            val base = offset + t * 4
            w[t] =
                ((padded[base].toInt() and 0xFF) shl 24) or
                ((padded[base + 1].toInt() and 0xFF) shl 16) or
                ((padded[base + 2].toInt() and 0xFF) shl 8) or
                (padded[base + 3].toInt() and 0xFF)
        }
        for (t in 16 until WORDS_IN_BLOCK) {
            val s0 = rotr(w[t - 15], 7) xor rotr(w[t - 15], 18) xor (w[t - 15] ushr 3)
            val s1 = rotr(w[t - 2], 17) xor rotr(w[t - 2], 19) xor (w[t - 2] ushr 10)
            w[t] = w[t - 16] + s0 + w[t - 7] + s1
        }

        var a = h[0]
        var b = h[1]
        var c = h[2]
        var d = h[3]
        var e = h[4]
        var f = h[5]
        var g = h[6]
        var hh = h[7]

        for (t in 0 until WORDS_IN_BLOCK) {
            val s1 = rotr(e, 6) xor rotr(e, 11) xor rotr(e, 25)
            val ch = (e and f) xor (e.inv() and g)
            val temp1 = hh + s1 + ch + K[t] + w[t]
            val s0 = rotr(a, 2) xor rotr(a, 13) xor rotr(a, 22)
            val maj = (a and b) xor (a and c) xor (b and c)
            val temp2 = s0 + maj

            hh = g
            g = f
            f = e
            e = d + temp1
            d = c
            c = b
            b = a
            a = temp1 + temp2
        }

        h[0] += a
        h[1] += b
        h[2] += c
        h[3] += d
        h[4] += e
        h[5] += f
        h[6] += g
        h[7] += hh

        offset += 64
    }

    val hex = StringBuilder(HASH_WORD_COUNT * 8)
    for (word in h) {
        for (shift in intArrayOf(28, 24, 20, 16, 12, 8, 4, 0)) {
            hex.append("0123456789abcdef"[(word ushr shift) and 0xF])
        }
    }
    return hex.toString()
}
