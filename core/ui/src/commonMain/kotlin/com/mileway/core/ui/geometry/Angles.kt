package com.mileway.core.ui.geometry

import kotlin.math.PI

/**
 * Degrees in a half turn. This is the `180` that pairs with [PI] in every degrees-to-radians
 * conversion, and the widest a normalized angular distance can ever be.
 */
internal const val DegreesPerHalfTurn = 180f

/** Degrees to radians, without any `java.*` dependency (KMP-pure). */
internal fun Float.toRadians(): Float = this * PI.toFloat() / DegreesPerHalfTurn

/** Radians to degrees, without any `java.*` dependency (KMP-pure). */
internal fun Float.toDegrees(): Float = this * DegreesPerHalfTurn / PI.toFloat()
