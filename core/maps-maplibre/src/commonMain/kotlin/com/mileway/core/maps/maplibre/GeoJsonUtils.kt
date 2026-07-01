package com.mileway.core.maps.maplibre

import com.mileway.core.maps.MapCoordinate

internal fun List<MapCoordinate>.toLineStringJson(): String {
    if (isEmpty()) return """{"type":"LineString","coordinates":[]}"""
    val coords = joinToString(",") { "[${it.lng},${it.lat}]" }
    return """{"type":"LineString","coordinates":[$coords]}"""
}

internal fun MapCoordinate.toPointJson(): String = """{"type":"Point","coordinates":[$lng,$lat]}"""

internal fun List<MapCoordinate>.toMultiPointJson(): String {
    if (isEmpty()) return """{"type":"MultiPoint","coordinates":[]}"""
    val coords = joinToString(",") { "[${it.lng},${it.lat}]" }
    return """{"type":"MultiPoint","coordinates":[$coords]}"""
}
