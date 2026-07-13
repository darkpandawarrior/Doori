package com.mileway.core.data.support

import com.mileway.core.data.dao.BugReportDao
import com.mileway.core.data.model.db.BugReportEntity
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// ponytail: no real app-version source is wired into core:data yet (that lives in :app's
// BuildConfig, not reachable from a KMP commonMain module without a new expect/actual seam).
// Shake-to-report is a debug/demo capture path today, so a fixed label is enough context in the
// saved row; wire a real per-platform appVersionName() when this graduates past demo.
object BugReportAppVersion {
    const val CURRENT: String = "dev"
}

/** P31.MISC.1: the persisted shake-to-report store — see [BugReportEntity]. */
class BugReportRepository(private val dao: BugReportDao) {
    fun observeAll(): Flow<List<BugReportEntity>> = dao.observeAll()

    @OptIn(ExperimentalUuidApi::class)
    suspend fun submit(
        title: String,
        description: String,
        screen: String,
        timestampMs: Long,
        appVersion: String = BugReportAppVersion.CURRENT,
    ) {
        dao.insert(
            BugReportEntity(
                id = "bug_${Uuid.random()}",
                title = title,
                description = description,
                screen = screen,
                timestampMs = timestampMs,
                appVersion = appVersion,
            ),
        )
    }
}
