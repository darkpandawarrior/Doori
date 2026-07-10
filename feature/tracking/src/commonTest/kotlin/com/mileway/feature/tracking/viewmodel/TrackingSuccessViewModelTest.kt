package com.mileway.feature.tracking.viewmodel

import com.mileway.core.data.dao.VoucherDao
import com.mileway.core.data.model.ExpenseSourceContext
import com.mileway.core.data.model.db.VoucherEntity
import com.mileway.core.data.model.db.VoucherStatus
import com.mileway.core.data.model.network.ApprovedVehicle
import com.mileway.feature.tracking.repository.VehiclePricingRepository
import com.mileway.feature.tracking.repository.VoucherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Unit tests for [TrackingSuccessViewModel]: reimbursement is computed by [PolicyRateEngine] from
 * the approved-vehicle rate table, onCreateVoucher persists a real DRAFT voucher, and the three
 * screen actions emit the expected navigation effects.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TrackingSuccessViewModelTest {
    // In-memory VoucherDao — only the methods VoucherRepository.save touches are backed.
    private class FakeVoucherDao : VoucherDao {
        val rows = mutableMapOf<String, VoucherEntity>()

        override fun observeAll(): Flow<List<VoucherEntity>> = MutableStateFlow(rows.values.toList())

        override suspend fun getAll(): List<VoucherEntity> = rows.values.toList()

        override suspend fun count(): Int = rows.size

        override suspend fun insert(voucher: VoucherEntity) {
            rows[voucher.voucherNumber] = voucher
        }

        override suspend fun insertAll(vouchers: List<VoucherEntity>) {
            vouchers.forEach { rows[it.voucherNumber] = it }
        }

        override suspend fun updateStatus(
            voucherNumber: String,
            status: String,
        ) {
            rows[voucherNumber]?.let { rows[voucherNumber] = it.copy(status = status) }
        }

        override suspend fun getByNumber(voucherNumber: String): VoucherEntity? = rows[voucherNumber]

        override suspend fun deleteByNumber(voucherNumber: String) {
            rows.remove(voucherNumber)
        }
    }

    private val car = ApprovedVehicle(vehicleKey = "car", vehicleName = "Car", vehiclePricing = 12.0)

    private fun buildVm(
        args: TrackingSuccessArgs,
        dao: FakeVoucherDao = FakeVoucherDao(),
        vehicles: List<ApprovedVehicle> = listOf(car),
    ) = TrackingSuccessViewModel(
        args = args,
        vehiclePricingRepository = VehiclePricingRepository(FakeNetworkApi(vehicles)),
        voucherRepository = VoucherRepository(dao),
    )

    private fun args(
        vehicleKey: String = "car",
        distanceKm: Double = 10.0,
        transactionId: String? = "TXN-1",
    ) = TrackingSuccessArgs(
        distanceKm = distanceKm,
        vehicleKey = vehicleKey,
        vehicleName = "Car",
        startTime = 1_000L,
        endTime = 2_000L,
        transactionId = transactionId,
        submissionStatus = "SUCCESS",
        violationCount = 0,
        violationMessage = null,
    )

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `reimbursement is computed from the policy rate engine for a known vehicle`() {
        val vm = buildVm(args(vehicleKey = "car", distanceKm = 10.0))

        // 12 ₹/km × 10 km = 120, no caps.
        assertEquals(12.0, vm.state.value.ratePerKm)
        assertEquals(120.0, vm.state.value.reimbursableAmount)
    }

    @Test
    fun `unknown vehicle falls back to the default rate`() {
        val vm = buildVm(args(vehicleKey = "spaceship", distanceKm = 10.0))

        assertEquals(TrackingSuccessViewModel.DEFAULT_RATE_PER_KM, vm.state.value.ratePerKm)
        assertEquals(TrackingSuccessViewModel.DEFAULT_RATE_PER_KM * 10.0, vm.state.value.reimbursableAmount)
    }

    @Test
    fun `onCreateVoucher persists a DRAFT voucher with the reimbursement amount`() =
        runTest {
            val dao = FakeVoucherDao()
            val vm = buildVm(args(distanceKm = 10.0), dao = dao)

            vm.onAction(TrackingSuccessAction.CreateVoucher)

            val saved = dao.rows.values.single()
            assertEquals(VoucherStatus.DRAFT.label, saved.status)
            assertEquals(120.0, saved.totalAmount)
            assertEquals(listOf("TXN-1"), VoucherEntity.decodeExpenseRouteIds(saved.expenseRouteIdsJson))
            // State reflects the new voucher so the screen reveals the voucher card.
            assertEquals(saved.voucherNumber, vm.state.value.voucherNumber)
            assertEquals(120.0, vm.state.value.voucherAmount)
        }

    @Test
    fun `onCreateVoucher is idempotent - a double tap creates only one voucher`() =
        runTest {
            val dao = FakeVoucherDao()
            val vm = buildVm(args(), dao = dao)

            vm.onAction(TrackingSuccessAction.CreateVoucher)
            vm.onAction(TrackingSuccessAction.CreateVoucher)

            assertEquals(1, dao.rows.size)
        }

    @Test
    fun `track new journey action emits navigate-to-hub effect`() =
        runTest {
            val vm = buildVm(args())
            vm.onAction(TrackingSuccessAction.TrackNewJourney)

            assertEquals(TrackingSuccessEffect.NavigateToHub, vm.effect.first())
        }

    @Test
    fun `view expense action emits navigate-to-expense-list effect`() =
        runTest {
            val vm = buildVm(args())
            vm.onAction(TrackingSuccessAction.ViewExpense)

            assertEquals(TrackingSuccessEffect.NavigateToExpenseList, vm.effect.first())
        }

    // P27.E.5: AddExpense must build an ExpenseSourceContext.Trip carrying the (only available)
    // per-trip identifier — the ledger transactionId — plus the vehicle name as its display label.
    @Test
    fun `add expense action emits navigate-to-add-expense carrying a Trip context`() =
        runTest {
            val vm = buildVm(args(transactionId = "TXN-1"))
            vm.onAction(TrackingSuccessAction.AddExpense)

            val effect = vm.effect.first()
            assertIs<TrackingSuccessEffect.NavigateToAddExpense>(effect)
            assertEquals(ExpenseSourceContext.Trip(tripId = "TXN-1", tripLabel = "Car"), effect.context)
        }

    @Test
    fun `add expense action is a no-op when no transaction was issued`() =
        runTest {
            val vm = buildVm(args(transactionId = null))
            vm.onAction(TrackingSuccessAction.AddExpense)
            vm.onAction(TrackingSuccessAction.TrackNewJourney)

            // The effect channel is FIFO: if AddExpense had emitted anything, it would arrive
            // before TrackNewJourney's NavigateToHub.
            assertEquals(TrackingSuccessEffect.NavigateToHub, vm.effect.first())
        }
}
