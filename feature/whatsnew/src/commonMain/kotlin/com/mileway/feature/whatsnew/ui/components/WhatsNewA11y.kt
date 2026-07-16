package com.mileway.feature.whatsnew.ui.components

import androidx.compose.runtime.Composable
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.whatsnew_cd_media_screenshot
import com.mileway.core.ui.resources.whatsnew_cd_media_step
import org.jetbrains.compose.resources.stringResource

/**
 * PLAN_V36 P6 (spec §6.4) — every media item's TalkBack description: the catalog author's
 * [caption] when present, else a generated one from the entry [title] and, for multi-media
 * entries, the step position — shared by the list card's hero image and the detail carousel's
 * pages/thumbnails so the phrasing is consistent across both screens.
 */
@Composable
internal fun whatsNewMediaContentDescription(
    caption: String?,
    title: String,
    index: Int,
    total: Int,
): String =
    caption ?: if (total > 1) {
        stringResource(Res.string.whatsnew_cd_media_step, title, index + 1, total)
    } else {
        stringResource(Res.string.whatsnew_cd_media_screenshot, title)
    }
