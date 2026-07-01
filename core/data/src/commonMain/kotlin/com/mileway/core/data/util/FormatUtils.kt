package com.mileway.core.data.util

import kotlin.math.abs
import kotlin.math.roundToLong

internal fun Int.pad2(): String = toString().padStart(2, '0')

internal fun Double.fmt1d(): String {
    val scaled = (this * 10.0).roundToLong()
    val intPart = scaled / 10
    val fracPart = abs(scaled % 10)
    return "$intPart.$fracPart"
}

internal fun Double.fmt2d(): String {
    val scaled = (this * 100.0).roundToLong()
    val intPart = scaled / 100
    val fracPart = abs(scaled % 100).toString().padStart(2, '0')
    return "$intPart.$fracPart"
}
