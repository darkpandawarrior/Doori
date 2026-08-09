package com.mileway.core.maps.canvas

import com.mileway.core.maps.MapCoordinate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MercatorProjectionTest {
    // -- raw projection --------------------------------------------------

    @Test
    fun project_origin_maps_to_zero_zero() {
        val (x, y) = MercatorProjection.project(0.0, 0.0)
        assertEquals(0.0, x, 1e-9)
        assertEquals(0.0, y, 1e-9)
    }

    @Test
    fun project_90_degrees_east_on_the_equator_is_a_quarter_turn_with_no_vertical_component() {
        val (x, y) = MercatorProjection.project(0.0, 90.0)
        assertEquals(PI / 2, x, 1e-9)
        assertEquals(0.0, y, 1e-9)
    }

    // -- known lat/lng -> known canvas coords -----------------------------

    @Test
    fun equator_segment_fits_edge_to_edge_and_stays_vertically_centered() {
        val coords = listOf(MapCoordinate(lat = 0.0, lng = 0.0), MapCoordinate(lat = 0.0, lng = 90.0))
        val fitted = MercatorProjection.fitToCanvas(coords, canvasWidth = 200f, canvasHeight = 100f, padding = 0f)

        assertEquals(0f, fitted[0].x, 0.5f)
        assertEquals(200f, fitted[1].x, 0.5f)
        assertEquals(50f, fitted[0].y, 0.5f)
        assertEquals(50f, fitted[1].y, 0.5f)
    }

    // -- bounds fit preserves aspect --------------------------------------

    @Test
    fun bounds_fit_applies_one_uniform_scale_to_both_axes() {
        // Near the equator, mercator y and x scale ~1:1 for small deltas, so this bbox is
        // geographically ~square. The canvas is 3:1, so the fit must be height-bound and
        // letterbox horizontally — NOT stretch x to fill 300 independently of y.
        val square =
            listOf(
                MapCoordinate(0.0, 0.0),
                MapCoordinate(0.0, 1.0),
                MapCoordinate(1.0, 0.0),
                MapCoordinate(1.0, 1.0),
            )
        val fitted = MercatorProjection.fitToCanvas(square, canvasWidth = 300f, canvasHeight = 100f, padding = 0f)

        val pixelSpanX = fitted.maxOf { it.x } - fitted.minOf { it.x }
        val pixelSpanY = fitted.maxOf { it.y } - fitted.minOf { it.y }

        assertTrue(pixelSpanY > 95f, "expected the constrained (height) axis to fill nearly the full 100px, got $pixelSpanY")
        assertTrue(pixelSpanX < 150f, "expected letterboxing on the unconstrained axis, got $pixelSpanX of 300px")
        assertTrue(
            abs(pixelSpanX - pixelSpanY) < 5f,
            "a squashed route is worse than no route: x/y spans should match under one uniform scale, got x=$pixelSpanX y=$pixelSpanY",
        )
    }

    // -- degenerate input ---------------------------------------------------

    @Test
    fun zero_points_renders_nothing() {
        val fitted = MercatorProjection.fitToCanvas(emptyList(), canvasWidth = 100f, canvasHeight = 100f)
        assertTrue(fitted.isEmpty())
    }

    @Test
    fun one_point_lands_on_canvas_center() {
        val fitted =
            MercatorProjection.fitToCanvas(
                listOf(MapCoordinate(12.3, 45.6)),
                canvasWidth = 100f,
                canvasHeight = 200f,
                padding = 10f,
            )
        assertEquals(50f, fitted.single().x, 0.01f)
        assertEquals(100f, fitted.single().y, 0.01f)
    }

    @Test
    fun all_identical_points_collapse_to_a_dot_at_center_not_a_crash() {
        val samePoint = MapCoordinate(1.0, 1.0)
        val fitted = MercatorProjection.fitToCanvas(List(5) { samePoint }, canvasWidth = 100f, canvasHeight = 100f)

        assertTrue(fitted.all { it.x == 50f && it.y == 50f })
    }

    @Test
    fun antimeridian_crossing_stays_a_monotonic_eastward_line_not_a_jump_across_the_world() {
        // A route heading due east through 179 -> -179 (the dateline) is geographically
        // continuous. Without unwrapping, -179 would be treated as "far to the west", yanking
        // the polyline back across the entire projected width.
        val lngs = listOf(170.0, 179.0, -179.0, -170.0)
        assertEquals(listOf(170.0, 179.0, 181.0, 190.0), MercatorProjection.unwrapLongitudes(lngs))

        val coords = lngs.map { MapCoordinate(lat = 0.0, lng = it) }
        val fitted = MercatorProjection.fitToCanvas(coords, canvasWidth = 400f, canvasHeight = 100f, padding = 0f)
        val xs = fitted.map { it.x }
        for (i in 1 until xs.size) {
            assertTrue(xs[i] >= xs[i - 1] - 0.01f, "expected a monotonic eastward line, got $xs")
        }
    }
}
