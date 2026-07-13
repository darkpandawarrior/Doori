package com.mileway.feature.profile.viewmodel

import com.mileway.core.data.dao.NotificationDao
import com.mileway.core.data.model.db.NotificationEntity
import com.mileway.feature.profile.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * PLAN_V22 P6.5: covers [NotificationViewModel]'s first-run seeding (from
 * [com.mileway.feature.profile.data.NotificationData.all]), [NotificationUiState.unreadCount]'s
 * derivation, and the mark-all-read action — real Room-backed persistence replacing
 * `NotificationCentreScreen`'s previous `remember { mutableStateOf(NOTIFICATIONS) }` seed (reset on
 * navigation away) and its hardcoded "174 unread" topbar subtitle.
 *
 * `viewModelScope` is hard-wired to `Dispatchers.Main.immediate`, so [Dispatchers.setMain] is
 * required here exactly as `DelegationViewModelTest` does for this module's `commonTest`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(dao: FakeNotificationDao = FakeNotificationDao()) = NotificationViewModel(NotificationRepository(dao))

    @Test
    fun `first run seeds NotificationData's demo set`() =
        runTest {
            val vm = newViewModel()
            advanceUntilIdle()

            assertEquals(8, vm.state.value.notifications.size)
        }

    @Test
    fun `unreadCount is derived from the persisted notifications, not hardcoded`() =
        runTest {
            val vm = newViewModel()
            advanceUntilIdle()

            val expectedUnread = vm.state.value.notifications.count { it.isUnread }
            assertEquals(expectedUnread, vm.state.value.unreadCount)
            assertTrue(vm.state.value.unreadCount < 174, "unreadCount must reflect real state, never the old hardcoded 174")
        }

    @Test
    fun `setUnread persists a single entry's read state across a fresh ViewModel over the same dao`() =
        runTest {
            val dao = FakeNotificationDao()
            val vm = newViewModel(dao)
            advanceUntilIdle()
            val target = vm.state.value.notifications.first { it.isUnread }

            vm.setUnread(target.id, isUnread = false)
            advanceUntilIdle()

            // A fresh ViewModel over the same (persisted) dao simulates process death/relaunch.
            val relaunched = newViewModel(dao)
            advanceUntilIdle()

            assertEquals(false, relaunched.state.value.notifications.first { it.id == target.id }.isUnread)
        }

    @Test
    fun `markAllRead clears the unread count to zero`() =
        runTest {
            val vm = newViewModel()
            advanceUntilIdle()
            assertTrue(vm.state.value.unreadCount > 0)

            vm.markAllRead()
            advanceUntilIdle()

            assertEquals(0, vm.state.value.unreadCount)
            assertTrue(vm.state.value.notifications.none { it.isUnread })
        }

    @Test
    fun `open marks the entry read and emits its deeplink`() =
        runTest {
            val vm = newViewModel()
            advanceUntilIdle()
            val target = vm.state.value.notifications.first { it.isUnread }
            assertTrue(target.deeplink.isNotBlank(), "every seeded notification carries a real deeplink now")

            val effects = mutableListOf<NotificationEffect>()
            val job = launch { effects += vm.effect.first() }
            vm.open(target)
            advanceUntilIdle()
            job.join()

            assertEquals(NotificationEffect.OpenDeepLink(target.deeplink), effects.single())
            assertEquals(false, vm.state.value.notifications.first { it.id == target.id }.isUnread)
        }
}

/** In-memory fake for [NotificationDao] — mirrors [NotificationDaoTest]'s fake shape. */
private class FakeNotificationDao : NotificationDao {
    private val rows = MutableStateFlow<Map<String, NotificationEntity>>(emptyMap())

    override fun observeAll(): Flow<List<NotificationEntity>> = rows.map { it.values.sortedByDescending { row -> row.createdAtMs } }

    override suspend fun count(): Int = rows.value.size

    override suspend fun upsertAll(entities: List<NotificationEntity>) {
        rows.value = rows.value + entities.associateBy { it.id }
    }

    override suspend fun setUnread(
        id: String,
        isUnread: Boolean,
    ) {
        val existing = rows.value[id] ?: return
        rows.value = rows.value + (id to existing.copy(isUnread = isUnread))
    }

    override suspend fun markAllRead() {
        rows.value = rows.value.mapValues { (_, entity) -> entity.copy(isUnread = false) }
    }
}
