package com.mileway

import com.mileway.core.data.model.db.VoucherCategory
import com.mileway.core.data.model.db.VoucherStatus
import com.mileway.feature.tracking.repository.VoucherRecord
import com.mileway.feature.tracking.repository.VoucherRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

/**
 * P3.2: [VoucherRepository]'s status-lifecycle writers — `moveToApproval` (the DRAFT -> PENDING
 * step `CreateVoucherViewModel.submit()` performs) and `advance` (the lightweight mock action
 * that rotates a PENDING voucher toward a terminal status for demo realism) — both gated by
 * [com.mileway.feature.tracking.repository.VoucherTransitions] so an illegal jump is never
 * persisted.
 */
class VoucherRepositoryTest {
    private fun record(number: String) =
        VoucherRecord(
            voucherNumber = number,
            title = "Test Voucher",
            category = VoucherCategory.MILEAGE,
            totalAmount = 100.0,
            notes = "",
            expenseRouteIds = listOf("T1"),
            createdAtMs = 0L,
        )

    @Test
    fun `a newly saved voucher starts DRAFT`() =
        runTest {
            val dao = FakeVoucherDao()
            val repo = VoucherRepository(dao)
            repo.save(record("V-1"))
            assertEquals(VoucherStatus.DRAFT.label, repo.getAll().first().status)
        }

    @Test
    fun `moveToApproval transitions a DRAFT voucher to PENDING`() =
        runTest {
            val dao = FakeVoucherDao()
            val repo = VoucherRepository(dao)
            repo.save(record("V-1"))
            repo.moveToApproval("V-1")
            assertEquals(VoucherStatus.PENDING.label, repo.getAll().first().status)
        }

    @Test
    fun `moveToApproval is a no-op for an unknown voucher number`() =
        runTest {
            val dao = FakeVoucherDao()
            val repo = VoucherRepository(dao)
            repo.moveToApproval("does-not-exist")
            assertEquals(0, repo.getAll().size)
        }

    @Test
    fun `advance moves a PENDING voucher to one of the legal terminal statuses`() =
        runTest {
            val dao = FakeVoucherDao()
            val repo = VoucherRepository(dao)
            repo.save(record("V-1"))
            repo.moveToApproval("V-1")
            repo.advance("V-1")
            val status = repo.getAll().first().status
            assertEquals(
                true,
                status in setOf(VoucherStatus.APPROVED.label, VoucherStatus.REJECTED.label, VoucherStatus.SETTLED.label),
            )
        }

    @Test
    fun `advance is a no-op for a DRAFT voucher`() =
        runTest {
            val dao = FakeVoucherDao()
            val repo = VoucherRepository(dao)
            repo.save(record("V-1"))
            repo.advance("V-1")
            assertEquals(VoucherStatus.DRAFT.label, repo.getAll().first().status)
        }

    @Test
    fun `advance is deterministic for the same voucher number`() =
        runTest {
            val dao1 = FakeVoucherDao()
            val repo1 = VoucherRepository(dao1)
            repo1.save(record("V-DETERMINISTIC"))
            repo1.moveToApproval("V-DETERMINISTIC")
            repo1.advance("V-DETERMINISTIC")

            val dao2 = FakeVoucherDao()
            val repo2 = VoucherRepository(dao2)
            repo2.save(record("V-DETERMINISTIC"))
            repo2.moveToApproval("V-DETERMINISTIC")
            repo2.advance("V-DETERMINISTIC")

            assertEquals(repo1.getAll().first().status, repo2.getAll().first().status)
        }

    /** P3.6: withdrawing a DRAFT voucher removes it from the store outright. */
    @Test
    fun `withdraw removes a DRAFT voucher`() =
        runTest {
            val dao = FakeVoucherDao()
            val repo = VoucherRepository(dao)
            repo.save(record("V-1"))

            repo.withdraw("V-1")

            assertEquals(0, repo.getAll().size)
        }

    /** P3.6's gate: withdraw is a no-op once the voucher has moved past DRAFT. */
    @Test
    fun `withdraw is a no-op for a non-DRAFT voucher`() =
        runTest {
            val dao = FakeVoucherDao()
            val repo = VoucherRepository(dao)
            repo.save(record("V-1"))
            repo.moveToApproval("V-1")

            repo.withdraw("V-1")

            assertEquals(1, repo.getAll().size)
            assertEquals(VoucherStatus.PENDING.label, repo.getAll().first().status)
        }

    /** Defensive: withdraw on an unknown voucher number must not throw. */
    @Test
    fun `withdraw is a no-op for an unknown voucher number`() =
        runTest {
            val dao = FakeVoucherDao()
            val repo = VoucherRepository(dao)

            repo.withdraw("does-not-exist")

            assertEquals(0, repo.getAll().size)
        }
}
