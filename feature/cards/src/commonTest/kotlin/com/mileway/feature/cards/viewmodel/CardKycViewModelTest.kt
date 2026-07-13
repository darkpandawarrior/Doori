package com.mileway.feature.cards.viewmodel

import com.mileway.core.data.otp.LocalOtpEngine
import com.mileway.core.data.otp.OtpPurpose
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** P29.C.1: the sealed `KycStep` progression (Intro -> PersonalInfo -> OTP -> Document -> Selfie -> Success). */
@OptIn(ExperimentalCoroutinesApi::class)
class CardKycViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `wrong otp keeps the wizard on the otp step and flags the error`() =
        runTest {
            val engine = LocalOtpEngine()
            val vm = CardKycViewModel(engine)
            vm.onAction(CardKycAction.Next) // Intro -> PersonalInfo
            vm.onAction(CardKycAction.SetFullName("Asha Rao"))
            vm.onAction(CardKycAction.SetIdNumber("ID123"))
            vm.onAction(CardKycAction.SetPhone("9876543210"))
            vm.onAction(CardKycAction.Next) // -> OTP step, dispatches a code

            vm.onAction(CardKycAction.SetOtp("000000"))
            vm.onAction(CardKycAction.Next)

            assertEquals(2, vm.state.value.step)
            assertTrue(vm.state.value.otpError)
        }

    @Test
    fun `full wizard walk-through reaches done`() =
        runTest {
            val engine = LocalOtpEngine()
            val vm = CardKycViewModel(engine)

            vm.onAction(CardKycAction.Next) // Intro -> PersonalInfo
            vm.onAction(CardKycAction.SetFullName("Asha Rao"))
            vm.onAction(CardKycAction.SetIdNumber("ID123"))
            vm.onAction(CardKycAction.SetPhone("9876543210"))
            vm.onAction(CardKycAction.Next) // -> OTP (dispatches a code)

            val sentTo = vm.state.value.otpSentTo!!
            val code = engine.codeFor(OtpPurpose.CARD_KYC, sentTo)
            vm.onAction(CardKycAction.SetOtp(code))
            vm.onAction(CardKycAction.Next) // -> Document

            vm.onAction(CardKycAction.AttachDocument("file://doc.jpg"))
            vm.onAction(CardKycAction.Next) // -> Selfie

            vm.onAction(CardKycAction.AttachSelfie)
            vm.onAction(CardKycAction.Next) // submit()
            advanceUntilIdle()

            assertTrue(vm.state.value.done)
            assertFalse(vm.state.value.isProcessing)
        }
}
