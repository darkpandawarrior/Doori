package com.mileway.platform.gms

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import com.siddharth.kmp.appshell.AppReviewManager
import com.siddharth.kmp.appshell.AppReviewManagerFactory

/**
 * Play in-app review (gms flavor only). [promptForReview] runs Play's request→launch flow; if the store
 * declines (quota, no Play services, sideloaded demo) it falls back to opening the Play Store listing.
 * Both paths are wrapped in runCatching → never crashes.
 */
class PlayAppReviewManager(private val activity: Activity) : AppReviewManager {
    private val manager = ReviewManagerFactory.create(activity)

    override suspend fun promptForReview() {
        runCatching {
            val info = manager.requestReview()
            manager.launchReview(activity, info)
        }.onFailure { openStoreListing() }
    }

    private fun openStoreListing() {
        runCatching {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${activity.packageName}")),
            )
        }
    }
}

/** gms factory binding for [AppReviewManagerFactory]. */
class PlayAppReviewManagerFactoryImpl : AppReviewManagerFactory {
    override fun create(activity: Activity): AppReviewManager = PlayAppReviewManager(activity)
}
