package com.mileway.feature.tracking.route

import com.mileway.core.data.dao.FavouriteRouteDao
import com.mileway.core.data.dao.SavedPlaceDao
import com.mileway.core.data.model.db.FavouriteRouteEntity
import com.mileway.core.data.model.db.SavedPlaceEntity
import kotlinx.coroutines.flow.MutableStateFlow

internal class RoutePlaceDao : SavedPlaceDao {
    val rows = MutableStateFlow<List<SavedPlaceEntity>>(emptyList())

    override fun observeAll() = rows

    override suspend fun upsert(entity: SavedPlaceEntity) {
        rows.value = rows.value.filterNot { it.id == entity.id } + entity
    }

    override suspend fun delete(id: String) {
        rows.value = rows.value.filterNot { it.id == id }
    }
}

internal class RouteFavouriteDao : FavouriteRouteDao {
    val rows = MutableStateFlow<List<FavouriteRouteEntity>>(emptyList())

    override fun observeAll() = rows

    override suspend fun upsert(entity: FavouriteRouteEntity) {
        rows.value = rows.value.filterNot { it.id == entity.id } + entity
    }

    override suspend fun get(id: String) = rows.value.firstOrNull { it.id == id }

    override suspend fun rename(
        id: String,
        name: String,
    ) {
        rows.value = rows.value.map { if (it.id == id) it.copy(name = name) else it }
    }

    override suspend fun delete(id: String) {
        rows.value = rows.value.filterNot { it.id == id }
    }
}
