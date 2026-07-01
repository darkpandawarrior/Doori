package com.mileway.debug

import android.content.Context
import android.content.Intent

/**
 * Release no-op of WormaCeptorHelper.
 *
 * The debug source set provides the real implementation backed by
 * `com.github.azikar24.WormaCeptor:api-impl-persistence` (debugImplementation only).
 * This object compiles in release builds without the WormaCeptor or OkHttp dependency on the classpath.
 */
object WormaCeptorHelper {

    fun init(context: Context) = Unit

    fun getLaunchIntent(context: Context): Intent? = null
}
