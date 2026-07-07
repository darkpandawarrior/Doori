package com.mileway.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * A terminal-style world map painted entirely on [Canvas] from an embedded, equirectangular
 * land mask — no raster asset. Because it is pure vector drawing (dots), it renders
 * identically on device *and* in host-side Roborazzi screenshots, which cannot rasterize PNG
 * drawables. Optionally drops a pulsing pin at a geographic coordinate (e.g. the user's current
 * location).
 *
 * The mask is a 168x84 1-bit grid (row-major, MSB-first), packed to hex. It was derived once
 * from the source world-map artwork; land = set bit.
 */

private const val MAP_W = 168
private const val MAP_H = 84

private val LAND_MASK_HEX: String =
    "000000000000000000000000000000000000000000000000000007f9fff000000000000000000000000000000000003f" +
        "fffff00000000000000000000000000000000000bc7fffe000000000060000000000000000000001e7f00fffc0000000" +
        "007fc00000000000000000003fdff80fff8000000011ffff800000000000000e002fcdfc0fff000030003ffffffff800" +
        "00000000fffffffd9e0ffe0001fe01fffffffffffe00000000ffffffff9f1ff00003ffbfffffffffffffe0000007ffff" +
        "fffe7e1f81e007fffffffffffffffff8000007fffffff41c1f00800fffffffffffffffffe000001fffffffc0c00e0000" +
        "1f7fffffffffffffffc000003f9fffff01e00000001f3fffffffffffffe70000001807ffff01f80000021f3fffffffff" +
        "fffc060000000007ffffc3f80000060e3ffffffffffff8070000000007ffffe7fc00000f0efffffffffffffc03800000" +
        "0007ffffeffe00000bbfffffffffffffff818000000007fffffffe000003ffffffffffffffffc00000000007ffffffc6" +
        "000001ffffffffffffffffe00000000007fffdffce000003ffffffffffffffffc00000000007ffffffc0000001ffffff" +
        "ffffffffffc0000000000fffffffc0000001fdfcbfffffffffff88000000001ffffffc0000001fc6fc1fffffffffff8c" +
        "000000001ffffff80000001f837fffffffffffff08000000003fffffe00000001f0977ffffffffffee04000000003fff" +
        "ffc00000001f0337ffffffffffe304000000003fffffc00000000ef803fffffffffff98c000000001fffff800000000f" +
        "f8003ffffffffff83c000000000fffff000000001ffc007ffffffffff830000000000ffffc000000003fff3d7fffffff" +
        "fffc20000000000fffdc000000003ffffffffffffffffc00000000000ffc04000000007fffffffbffffffffc00000000" +
        "000ff80400000000ffffffbfdffffffffc000000000005f80000000000ffffffdfe9bffffffc000000000000f8000000" +
        "0001ffffffdffc1ffffffa000000000000f80600000003ffffffeffe0ffdffe0000000000000f8c100000001ffffffef" +
        "fc07f8ff400000000000007f8060000001fffffff7fc03f07f000000000000003f8000000001fffffff3f003e07f0100" +
        "000000000003e000000003fffffffbe003c01f8100000000000001e000000003ffffffff8001c01fc180000000000000" +
        "6040000001fffffffe0001c00bc04000000000000021fe000001ffffffffe000c00180400000000000001ffe000000ff" +
        "ffffffc000a008006000000000000003ff0000007fffffffc000200c006000000000000003ffe000003f3fffffc00000" +
        "06060000000000000003fff00000000fffff8000001e0e0000000000000007fff80000000fffff0000000e1e00000000" +
        "0000000ffff80000000ffffe0000000e3ec00000000000000ffffe0000000ffffc000000063d820000000000000fffff" +
        "c0000007fff8000000073d83e000000000000fffffe0000007fff8000000030181f800000000000ffffff8000003fff0" +
        "0000000180007d000000000007fffff8000003fff80000000070007c000000000007fffff8000003fff8000000000100" +
        "16000000000003fffff0000001fff800000000000000000000000003ffffe0000003fff8000000000007100000000000" +
        "01ffffe0000003fff840000000002f30000000000000ffffe0000003fff8c0000000007f380000000000007fffe00000" +
        "07fff1c000000000fff80000000000003fffe0000003ffe1c000000001fff80000000000003fffc0000003ffc3800000" +
        "0007fffc0000000000003fffc0000001ffc3800000001ffffc0000000000001fff00000001ffc3000000001ffffe0000" +
        "000000001ffe00000001ff81000000003ffffe0000000000001ffe00000001ff80000000003ffffe0000000000001ffc" +
        "00000000ff00000000003ffffe0000000000001ffc00000000ff00000000001ffffc0000000000001ff8000000007e00" +
        "000000003fdffc0000000000001ff8000000007c00000000003e0ff80000000000001fe0000000000000000000000007" +
        "f00000000000001fe0000000000000000000000003e00100000000001fe0000000000000000000000003800300000000" +
        "001f80000000000000000000000000000200000000000f80000000000000000000000001001800000000000f80000000" +
        "00000000000000000000300000000000070000000000000000000000000000c000000000000780000000000000000000" +
        "000000000000000000000780000000000000000000000000000000000000000300000000000000000000000000000000" +
        "0000000003800000000000000000000000000000000000000001c0000000000000000000000000000000000000000000" +
        "000000000000000000000000000000000000000000000000000000000000000000000000"

private val landBits: BooleanArray by lazy {
    val hex = LAND_MASK_HEX
    val bytes =
        ByteArray(hex.length / 2) {
            ((hex[it * 2].digitToInt(16) shl 4) or hex[it * 2 + 1].digitToInt(16)).toByte()
        }
    BooleanArray(MAP_W * MAP_H) { i -> (bytes[i / 8].toInt() and (0x80 shr (i % 8))) != 0 }
}

/**
 * @param dotColor colour of the land dots.
 * @param dotAlpha per-dot opacity (kept low so the map stays a background texture).
 * @param markerLatLng optional (latitude, longitude) to pin; null draws no marker.
 * @param markerColor colour of the location pin (should contrast the header behind it).
 */
@Composable
fun WorldMapBackdrop(
    modifier: Modifier = Modifier,
    dotColor: Color = Color.White,
    dotAlpha: Float = 0.20f,
    markerLatLng: Pair<Double, Double>? = null,
    markerColor: Color = Color(0xFFFF5252),
) {
    val bits = landBits
    Canvas(modifier = modifier) {
        val cell = size.width / MAP_W // square cells; the full map is 2:1
        if (cell <= 0f) return@Canvas
        val mapH = cell * MAP_H
        val top = (size.height - mapH) / 2f // vertical-centre; crops poles on short headers
        val r = cell * 0.42f
        for (row in 0 until MAP_H) {
            val cy = top + (row + 0.5f) * cell
            if (cy < -r || cy > size.height + r) continue
            val base = row * MAP_W
            for (col in 0 until MAP_W) {
                if (bits[base + col]) {
                    drawCircle(dotColor, r, Offset((col + 0.5f) * cell, cy), alpha = dotAlpha)
                }
            }
        }
        markerLatLng?.let { (lat, lon) ->
            val mx = ((lon + 180.0) / 360.0).toFloat() * size.width
            val my = top + ((90.0 - lat) / 180.0).toFloat() * mapH
            val c = Offset(mx, my)
            drawCircle(markerColor, cell * 3.4f, c, alpha = 0.16f) // glow
            drawCircle(markerColor, cell * 1.7f, c, alpha = 0.32f) // ring
            drawCircle(markerColor, cell * 0.9f, c, alpha = 0.95f) // core
            drawCircle(Color.White, cell * 0.34f, c, alpha = 0.95f) // centre highlight
        }
    }
}
