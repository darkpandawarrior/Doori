package com.mileway.feature.profile.repository

import com.mileway.core.network.model.DemoAccount
import com.mileway.core.network.model.EmployeeProfile
import com.mileway.core.network.model.ProfileCompletion
import com.mileway.core.network.model.UserSession
import com.mileway.feature.profile.model.ProfileHeader
import com.mileway.stub.DemoMockData
import com.mileway.stub.ProfileMockData
import kotlinx.coroutines.flow.Flow

/**
 * Offline implementation that maps the mocked [DemoMockData] profile into a [ProfileHeader]
 * and serves the rich profile surfaces straight from [ProfileMockData].
 *
 * P1.2: [accounts]/[observeAccounts] now delegate to [mockAccountRepository] (Room-backed, via
 * [MockAccountDao][com.mileway.core.data.dao.MockAccountDao]) instead of the static
 * `ProfileMockData.accounts()` list — the other surfaces here stay deterministic mock data.
 */
class FakeProfileRepository(private val mockAccountRepository: MockAccountRepository) : ProfileRepository {
    override fun richProfile(): EmployeeProfile = ProfileMockData.primaryProfile()

    override fun completion(): ProfileCompletion = ProfileMockData.completion()

    override fun sessions(): List<UserSession> = ProfileMockData.sessions()

    override fun accounts(): List<DemoAccount> = ProfileMockData.accounts()

    override fun observeAccounts(): Flow<List<DemoAccount>> = mockAccountRepository.observeAll()

    override suspend fun seedAccountsIfEmpty() = mockAccountRepository.seedIfEmpty()

    override suspend fun addAccount(
        displayName: String,
        employeeCode: String,
        organization: String,
    ) = mockAccountRepository.add(displayName, employeeCode, organization)

    override suspend fun removeAccount(accountId: String) = mockAccountRepository.remove(accountId)

    override suspend fun setActiveAccount(accountId: String) = mockAccountRepository.setActive(accountId)

    override fun header(): ProfileHeader {
        val profile = DemoMockData.userConfig().profile
        return ProfileHeader(
            name = profile?.name.orEmpty(),
            email = profile?.email.orEmpty(),
            code = profile?.code.orEmpty(),
            tenant = profile?.tenant.orEmpty(),
            initials = initialsFrom(profile?.name.orEmpty()),
        )
    }

    private fun initialsFrom(name: String): String =
        name.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .ifEmpty { "?" }
}
