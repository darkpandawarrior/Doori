package com.mileway.feature.profile.viewmodel

import com.mileway.core.data.session.CREDENTIAL_ACCOUNT_ID
import com.mileway.core.data.session.CredentialSource
import com.mileway.core.data.session.DEFAULT_PASSWORD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * PLAN_V24 P1.5 — covers [ChangePasswordViewModel]'s validation branches (wrong current, too
 * short, mismatch) and the happy path over an in-memory [CredentialSource] seeded to the default.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChangePasswordViewModelTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeCredentialSource : CredentialSource {
        private val passwords = mutableMapOf(CREDENTIAL_ACCOUNT_ID to DEFAULT_PASSWORD)

        override suspend fun ensureSeeded(accountId: String) {
            passwords.getOrPut(accountId) { DEFAULT_PASSWORD }
        }

        override suspend fun verify(
            accountId: String,
            password: String,
        ) = passwords[accountId] == password

        override suspend fun setPassword(
            accountId: String,
            password: String,
        ) {
            passwords[accountId] = password
        }

        fun current(accountId: String = CREDENTIAL_ACCOUNT_ID) = passwords[accountId]
    }

    @Test
    fun `wrong current password is rejected`() =
        runTest {
            val vm = ChangePasswordViewModel(FakeCredentialSource())
            vm.onCurrentChange("nope")
            vm.onNewChange("newpassword1")
            vm.onConfirmChange("newpassword1")
            vm.submit()
            advanceUntilIdle()

            assertEquals(ChangePasswordError.WRONG_CURRENT, vm.state.value.error)
        }

    @Test
    fun `too-short new password is rejected`() =
        runTest {
            val vm = ChangePasswordViewModel(FakeCredentialSource())
            vm.onCurrentChange(DEFAULT_PASSWORD)
            vm.onNewChange("short")
            vm.onConfirmChange("short")
            vm.submit()
            advanceUntilIdle()

            assertEquals(ChangePasswordError.TOO_SHORT, vm.state.value.error)
        }

    @Test
    fun `mismatched confirmation is rejected`() =
        runTest {
            val vm = ChangePasswordViewModel(FakeCredentialSource())
            vm.onCurrentChange(DEFAULT_PASSWORD)
            vm.onNewChange("newpassword1")
            vm.onConfirmChange("different1234")
            vm.submit()
            advanceUntilIdle()

            assertEquals(ChangePasswordError.MISMATCH, vm.state.value.error)
        }

    @Test
    fun `valid change persists the new password and completes`() =
        runTest {
            val credentials = FakeCredentialSource()
            val vm = ChangePasswordViewModel(credentials)
            vm.onCurrentChange(DEFAULT_PASSWORD)
            vm.onNewChange("newpassword1")
            vm.onConfirmChange("newpassword1")
            vm.submit()
            advanceUntilIdle()

            assertTrue(vm.state.value.done)
            assertEquals("newpassword1", credentials.current())
        }
}
