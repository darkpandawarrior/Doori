package com.mileway.core.data.location

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SavedLocationsCodecTest {
    private fun place(name: String) = SavedPlace(name = name, subtitle = "sub-$name", lat = 1.0, lng = 2.0)

    @Test
    fun decodeOfNullOrGarbageIsEmptyNotThrown() {
        assertEquals(SavedLocationsData(), SavedLocationsCodec.decode(null))
        assertEquals(SavedLocationsData(), SavedLocationsCodec.decode(""))
        assertEquals(SavedLocationsData(), SavedLocationsCodec.decode("{not json"))
    }

    @Test
    fun encodeDecodeRoundTrips() {
        val data =
            SavedLocationsData(
                recent = listOf(place("A")),
                favorites = listOf(place("B").copy(favorite = true)),
                saved = listOf(place("C").copy(label = "Home")),
            )
        assertEquals(data, SavedLocationsCodec.decode(SavedLocationsCodec.encode(data)))
    }

    @Test
    fun addRecentDedupesAndCapsMostRecentFirst() {
        var data = SavedLocationsData()
        // Add more than the cap; each new one goes to the front.
        for (i in 1..(SavedLocationsCodec.RECENT_CAP + 3)) {
            data = SavedLocationsCodec.addRecent(data, place("P$i"))
        }
        assertEquals(SavedLocationsCodec.RECENT_CAP, data.recent.size)
        assertEquals("P${SavedLocationsCodec.RECENT_CAP + 3}", data.recent.first().name)

        // Re-adding an existing name moves it to the front without duplicating.
        data = SavedLocationsCodec.addRecent(data, place("P${SavedLocationsCodec.RECENT_CAP + 1}"))
        assertEquals("P${SavedLocationsCodec.RECENT_CAP + 1}", data.recent.first().name)
        assertEquals(1, data.recent.count { it.name == "P${SavedLocationsCodec.RECENT_CAP + 1}" })
    }

    @Test
    fun toggleFavoriteAddsThenRemoves() {
        var data = SavedLocationsData()
        data = SavedLocationsCodec.toggleFavorite(data, place("X"))
        assertTrue(data.favorites.any { it.name == "X" && it.favorite })
        data = SavedLocationsCodec.toggleFavorite(data, place("X"))
        assertFalse(data.favorites.any { it.name == "X" })
    }

    @Test
    fun saveAsIsUniquePerLabel() {
        var data = SavedLocationsData()
        data = SavedLocationsCodec.saveAs(data, place("Office A"), "Work")
        data = SavedLocationsCodec.saveAs(data, place("Office B"), "Work")
        assertEquals(1, data.saved.count { it.label == "Work" })
        assertEquals("Office B", data.saved.single { it.label == "Work" }.name)

        data = SavedLocationsCodec.removeSaved(data, "Work")
        assertTrue(data.saved.none { it.label == "Work" })
    }

    @Test
    fun addRecentStripsSavedLabel() {
        val data = SavedLocationsCodec.addRecent(SavedLocationsData(), place("Y").copy(label = "Home"))
        assertNull(data.recent.single().label)
    }
}
