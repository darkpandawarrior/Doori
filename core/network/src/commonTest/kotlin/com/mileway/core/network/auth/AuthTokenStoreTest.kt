package com.mileway.core.network.auth

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** PLAN_V34 P2/A6: [AuthTokenStore] round-trips through an in-memory [MapSettings] fake. */
class AuthTokenStoreTest {
    @Test
    fun storeThenReadRoundTripsBothTokens() {
        val store = AuthTokenStore(MapSettings())

        store.store(accessToken = "access-1", refreshToken = "refresh-1")

        assertEquals("access-1", store.accessToken.value)
        assertEquals("refresh-1", store.refreshToken())
    }

    @Test
    fun clearWipesBothTokens() {
        val store = AuthTokenStore(MapSettings())
        store.store(accessToken = "access-1", refreshToken = "refresh-1")

        store.clear()

        assertNull(store.accessToken.value)
        assertNull(store.refreshToken())
    }

    @Test
    fun freshStoreHasNoTokens() {
        val store = AuthTokenStore(MapSettings())

        assertNull(store.accessToken.value)
        assertNull(store.refreshToken())
    }

    @Test
    fun refreshTokenSurvivesANewStoreInstanceOverTheSameSettings() {
        val settings = MapSettings()
        AuthTokenStore(settings).store(accessToken = "access-1", refreshToken = "refresh-1")

        val reloaded = AuthTokenStore(settings)

        // The access token is deliberately never persisted — only the refresh token survives a
        // fresh AuthTokenStore instance (e.g. process relaunch) reading the same backing settings.
        assertEquals("refresh-1", reloaded.refreshToken())
        assertNull(reloaded.accessToken.value)
    }
}
