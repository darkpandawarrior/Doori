package com.mileway.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/**
 * P2.1: the Wear app's single entry point (mirrors [com.mileway.LauncherActivity]'s role on the
 * phone, minus its login/PIN theatre — the watch renders whatever `WatchFacade` reports for the
 * signed-in phone session, it never authenticates on its own). Boots [WearAppGraph] once, then hosts
 * [WearRootScreen] (P2.4: the dashboard; P2.5 adds trip-list/detail states to the same screen).
 */
class WearActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WearAppGraph.start(this)
        setContent { WearRootScreen() }
    }
}
