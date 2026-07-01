package com.mileway.debug

import android.content.Context
import android.content.Intent
import com.mileway.showcase.ComponentShowcaseActivity

object ShowcaseLauncher {
    fun getLaunchIntent(context: Context): Intent? =
        Intent(context, ComponentShowcaseActivity::class.java)
}
