package com.mileway.feature.profile.repository

import com.mileway.core.network.model.DemoAccount
import com.mileway.core.network.model.EmployeeProfile
import com.mileway.core.network.model.ProfileCompletion
import com.mileway.core.network.model.UserSession
import com.mileway.feature.profile.model.ProfileHeader
import com.mileway.stub.ProfileMockData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface ProfileRepository {
    fun header(): ProfileHeader

    /** Full employee profile for the profile detail screen. */
    fun richProfile(): EmployeeProfile = ProfileMockData.primaryProfile()

    /** Profile-completion checklist (headline percent + per-category counts). */
    fun completion(): ProfileCompletion = ProfileMockData.completion()

    /** Device sessions this account is signed in on. */
    fun sessions(): List<UserSession> = ProfileMockData.sessions()

    /**
     * Static snapshot of switchable accounts. Kept as a default for simple test doubles;
     * [observeAccounts] is the live, Room-backed source production code should collect from
     * (see [MockAccountRepository]).
     */
    fun accounts(): List<DemoAccount> = ProfileMockData.accounts()

    /** Live list of accounts the user can switch between (P1.2: Room-backed, not static). */
    fun observeAccounts(): Flow<List<DemoAccount>> = flowOf(accounts())

    /** Seeds the Room-backed account store on first access; a no-op for static test doubles. */
    suspend fun seedAccountsIfEmpty() {}

    /** P1.3: adds a new, non-active persona; a no-op for static test doubles. */
    suspend fun addAccount(
        displayName: String,
        employeeCode: String,
        organization: String,
    ) {}

    /** P1.3: removes a persona outright; a no-op for static test doubles. */
    suspend fun removeAccount(accountId: String) {}

    /** P2.2: marks [accountId] the sole active persona in the Room-backed store; a no-op for static test doubles. */
    suspend fun setActiveAccount(accountId: String) {}
}
