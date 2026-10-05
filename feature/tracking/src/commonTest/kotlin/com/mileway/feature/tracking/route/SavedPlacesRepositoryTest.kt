package com.mileway.feature.tracking.route

import com.mileway.core.data.model.db.FavouriteRouteEntity
import com.mileway.core.data.model.db.SavedPlaceEntity
import com.mileway.feature.tracking.places.SavedPlacesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SavedPlacesRepositoryTest {
    @Test
    fun homeIsProtectedFromRoutingExportRetypeAndDeletion() =
        runTest {
            val repository = SavedPlacesRepository(RoutePlaceDao(), RouteFavouriteDao())
            val home = SavedPlaceEntity("home", "HOME", "Home", "Private address", 12.0, 34.0, 0L)
            repository.save(home)
            repository.save(home.copy(id = "work", type = "WORK", label = "Office", latitude = 13.0))
            assertEquals(listOf("work"), repository.approverPlaces.first().map { it.id })
            assertTrue(requireNotNull(repository.routePoint(home)).protected)
            assertTrue(requireNotNull(repository.routePoint(home.copy(id = "raw", type = "OTHER"))).protected)
            assertFailsWith<IllegalArgumentException> { repository.delete("home") }
            assertFailsWith<IllegalArgumentException> { repository.save(home.copy(type = "OTHER")) }
            assertFailsWith<IllegalArgumentException> { repository.save(home.copy(id = "second-home")) }
            repository.save(home.copy(address = "Updated private address"))
            assertEquals(
                "Updated private address",
                repository.savedPlaces
                    .first()
                    .first { it.id == "home" }
                    .address,
            )
        }

    @Test
    fun placesAndFavouritesUseExistingStores() =
        runTest {
            val repository = SavedPlacesRepository(RoutePlaceDao(), RouteFavouriteDao())
            repository.save(SavedPlaceEntity("office", "WORK", "Office", "Office road", null, null, 0L))
            assertEquals(null, repository.routePoint(repository.savedPlaces.first().single()))
            val favourite = FavouriteRouteEntity("fav", "trip", "Office route", "BUSINESS", 10.0, 0L)
            repository.pin(favourite)
            assertEquals(listOf(favourite), repository.favouriteRoutes.first())
            repository.unpin("fav")
            repository.delete("office")
            assertEquals(emptyList(), repository.savedPlaces.first())
            assertEquals(emptyList(), repository.favouriteRoutes.first())
        }
}
