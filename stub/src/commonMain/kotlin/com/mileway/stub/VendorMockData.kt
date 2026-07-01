package com.mileway.stub

import com.mileway.core.network.model.FrequentRoute
import com.mileway.core.network.model.TrackingContext
import com.mileway.core.network.model.TrackingContextType
import com.mileway.core.network.model.VendorCenter

/**
 * Static vendor directory, frequent routes and tracking contexts for the offline demo.
 *
 * All values are deterministic constants. The vendor coordinates reuse and extend the
 * urban cluster used by [DemoMockData.checkInLocations] (Pune, ~18.5 N / 73.85 E) so demo
 * trips naturally pass near several centers.
 */
object VendorMockData {
    /**
     * Seven vendor / partner centers spread across the demo city.
     * The first five share the coordinate cluster of [DemoMockData.checkInLocations];
     * the last two extend it to the western IT corridor and the eastern logistics belt.
     */
    fun vendorCenters(): List<VendorCenter> =
        listOf(
            VendorCenter(
                id = "VND-001",
                name = "Speedline Transport Co.",
                address = "14 FC Road, Shivajinagar",
                city = "Pune",
                lat = 18.5204,
                lng = 73.8567,
                radiusMeters = 100.0,
            ),
            VendorCenter(
                id = "VND-002",
                name = "Metro Cargo Movers",
                address = "Plot 7, Old Mumbai Highway, Khadki",
                city = "Pune",
                lat = 18.5480,
                lng = 73.8718,
                radiusMeters = 150.0,
            ),
            VendorCenter(
                id = "VND-003",
                name = "CityLink Telecom Services",
                address = "2nd Floor, DP Road, Aundh",
                city = "Pune",
                lat = 18.5601,
                lng = 73.8234,
                radiusMeters = 120.0,
            ),
            VendorCenter(
                id = "VND-004",
                name = "Eastern Freight Lines",
                address = "Gate 3, EON Cluster, Kharadi",
                city = "Pune",
                lat = 18.5120,
                lng = 73.9012,
                radiusMeters = 200.0,
            ),
            VendorCenter(
                id = "VND-005",
                name = "Southside Auto Works",
                address = "31 Sinhagad Road, Vadgaon",
                city = "Pune",
                lat = 18.4890,
                lng = 73.8350,
                radiusMeters = 80.0,
            ),
            VendorCenter(
                id = "VND-006",
                name = "Westgate Fleet Services",
                address = "Phase 1, Rajiv Gandhi Infotech Park, Hinjawadi",
                city = "Pune",
                lat = 18.5913,
                lng = 73.7389,
                radiusMeters = 250.0,
            ),
            VendorCenter(
                id = "VND-007",
                name = "Hadapsar Logistics Park",
                address = "Survey 211, Pune-Solapur Road, Hadapsar",
                city = "Pune",
                lat = 18.5089,
                lng = 73.9260,
                radiusMeters = 180.0,
            ),
        )

    /**
     * Three frequently travelled routes between the demo centers.
     * Distances approximate the straight-line distance between the named endpoints.
     */
    fun frequentRoutes(): List<FrequentRoute> =
        listOf(
            FrequentRoute(
                id = "RT-001",
                label = "Office to Warehouse run",
                fromName = "Head Office",
                toName = "Warehouse / Supply Center",
                distanceKm = 3.4,
                timesUsed = 24,
            ),
            FrequentRoute(
                id = "RT-002",
                label = "Office to Westgate fleet hub",
                fromName = "Head Office",
                toName = "Westgate Fleet Services",
                distanceKm = 14.7,
                timesUsed = 11,
            ),
            FrequentRoute(
                id = "RT-003",
                label = "Office to Hadapsar logistics belt",
                fromName = "Head Office",
                toName = "Hadapsar Logistics Park",
                distanceKm = 7.5,
                timesUsed = 8,
            ),
        )

    /**
     * Three contexts a tracked journey can be tagged against:
     * one trip, one petty-cash wallet and one event.
     */
    fun trackingContexts(): List<TrackingContext> =
        listOf(
            TrackingContext(
                id = "TRIP-2401",
                title = "Quarterly Client Visits",
                subtitle = "Active business trip",
                type = TrackingContextType.TRIP,
            ),
            TrackingContext(
                id = "PETTY-77",
                title = "Field Operations Wallet",
                subtitle = "Petty cash for on-road expenses",
                type = TrackingContextType.PETTY_CASH,
            ),
            TrackingContext(
                id = "EVT-5",
                title = "Annual Sales Conference",
                subtitle = "Off-site company event",
                type = TrackingContextType.EVENT,
            ),
        )
}
