package com.mileway.feature.advances.data

import com.mileway.feature.advances.model.AdvanceSection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdvancesRepositoryTest {
    @Test
    fun `active petty cards cover healthy low and critical balance ratios`() =
        runTest {
            val cards = MockAdvancesRepository(AdvancesRequestStore()).activePettyCards().first()

            assertEquals(3, cards.size)
            assertTrue(cards.any { it.balance / it.amount >= 0.5 })
            assertTrue(
                cards.any {
                    val r = it.balance / it.amount
                    r >= 0.2 && r < 0.5
                },
            )
            assertTrue(cards.any { it.balance / it.amount < 0.2 })
        }

    @Test
    fun `submitPettyRequest appends a PETTY request to the shared open-requests list`() =
        runTest {
            val store = AdvancesRequestStore()
            val repository = MockAdvancesRepository(store)
            val before = repository.openRequests().first().size

            val result =
                repository.submitPettyRequest(
                    type = "Travel Petty Cash",
                    amount = 750.0,
                    title = "Site visit advance",
                    description = "Advance for the site visit next week.",
                    startMs = null,
                    endMs = null,
                    declarationAccepted = true,
                )

            assertTrue(result.isSuccess)
            assertTrue(result.getOrThrow().permissionId > 0)
            val after = repository.openRequests().first()
            assertEquals(before + 1, after.size)
            assertTrue(after.any { it.title == "Site visit advance" && it.section == AdvanceSection.PETTY })
        }

    @Test
    fun `submitPettyRequest without an accepted declaration fails and does not append`() =
        runTest {
            val store = AdvancesRequestStore()
            val repository = MockAdvancesRepository(store)
            val before = repository.openRequests().first().size

            val result =
                repository.submitPettyRequest(
                    type = "Travel Petty Cash",
                    amount = 750.0,
                    title = "Site visit advance",
                    description = "Advance for the site visit next week.",
                    startMs = null,
                    endMs = null,
                    declarationAccepted = false,
                )

            assertTrue(result.isFailure)
            assertEquals(before, repository.openRequests().first().size)
        }

    @Test
    fun `submitQrRequest appends a QR request into the same shared store as petty submits`() =
        runTest {
            val store = AdvancesRequestStore()
            val advancesRepository = MockAdvancesRepository(store)
            val qrRepository = MockQrCardsRepository(store)

            val result =
                qrRepository.submitQrRequest(
                    type = "Fuel QR",
                    amount = 300.0,
                    title = "Fuel QR recharge",
                    description = "Recharge for the delivery fleet.",
                    cardId = "1",
                    declarationAccepted = true,
                )

            assertTrue(result.isSuccess)
            assertTrue(result.getOrThrow().permissionId > 0)
            val open = advancesRepository.openRequests().first()
            assertTrue(open.any { it.title == "Fuel QR recharge" && it.section == AdvanceSection.QR })
        }

    @Test
    fun `active QR cards include exactly one transferable card`() =
        runTest {
            val cards = MockQrCardsRepository(AdvancesRequestStore()).activeQrCards().first()

            assertEquals(2, cards.size)
            assertEquals(1, cards.count { it.isTransfer })
        }
}
