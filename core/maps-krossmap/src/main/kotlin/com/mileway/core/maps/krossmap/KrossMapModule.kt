package com.mileway.core.maps.krossmap

import com.mileway.core.maps.MapSurface
import org.koin.dsl.module

val krossMapModule = module {
    single<MapSurface> { KrossMapSurface() }
}
