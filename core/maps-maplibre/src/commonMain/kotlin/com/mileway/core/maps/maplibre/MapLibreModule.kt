package com.mileway.core.maps.maplibre

import com.mileway.core.maps.MapSurface
import org.koin.dsl.module

val mapLibreModule =
    module {
        single<MapSurface> { MapLibreSurface() }
    }
