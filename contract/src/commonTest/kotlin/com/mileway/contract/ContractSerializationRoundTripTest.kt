package com.mileway.contract

import com.mileway.core.data.model.network.AllTypesResponseV2
import com.mileway.core.data.model.network.AuthRequest
import com.mileway.core.data.model.network.AuthResponse
import com.mileway.core.data.model.network.CheckInDetailsResponseV2
import com.mileway.core.data.model.network.CheckInItem
import com.mileway.core.data.model.network.CheckInRequestV2
import com.mileway.core.data.model.network.CoordsV2
import com.mileway.core.data.model.network.EventPayloadV2
import com.mileway.core.data.model.network.EventResponseV2
import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.core.data.model.network.LocationPayloadV2
import com.mileway.core.data.model.network.LocationResponseV2
import com.mileway.core.data.model.network.PolicyViolation
import com.mileway.core.data.model.network.RefreshRequest
import com.mileway.core.data.model.network.SubmissionStatus
import com.mileway.core.data.model.network.SubmitMilesRequestK
import com.mileway.core.data.model.network.SubmittedCheckInResponseV2
import com.mileway.core.data.model.network.TransactionRef
import com.mileway.core.data.model.network.ViolationSeverity
import com.mileway.core.data.model.network.Voucher
import com.mileway.core.data.model.network.VoucherStatus
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * PLAN_V33 A1: locks the wire shape of the two DTOs a future `:server` module must produce/consume
 * byte-identically to the client — a request (`SubmitMilesRequestK`) and a response
 * (`ExpenseSubmissionResponse`). If either drifts (a field renamed, a default changed in a way that
 * changes the encoded JSON), this test breaks before the client/server contract does.
 */
class ContractSerializationRoundTripTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun submitMilesRequestK_roundTrips() {
        val original =
            SubmitMilesRequestK(
                token = "tok-123",
                vehicleType = "CAR",
                origin = CoordsV2(lat = 12.9716, lng = 77.5946, name = "Origin"),
                destination = CoordsV2(lat = 13.0827, lng = 80.2707, name = "Destination"),
                distance = 42.5,
                originalDistance = 40.0,
                forms = mapOf(1L to "form-a"),
                notes = "client demo trip",
            )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<SubmitMilesRequestK>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun expenseSubmissionResponse_roundTrips() {
        val original =
            ExpenseSubmissionResponse(
                status = 1,
                amount = 250.0,
                currency = "INR",
                distance = 42.5,
                submissionStatus = SubmissionStatus.NEEDS_APPROVAL,
                violations =
                    listOf(
                        PolicyViolation(
                            id = "max-distance-per-day",
                            title = "Over daily limit",
                            message = "Trip exceeds the configured daily distance limit.",
                            severity = ViolationSeverity.VIOLATION,
                        ),
                    ),
                issuedVoucher = Voucher(id = 7L, number = "O-INDIAN-000048769", amount = 250.0, status = VoucherStatus.FILED),
                transaction = TransactionRef(id = "O-INDIAN-000048769", createdAtMillis = 1_700_000_000_000L, amount = 250.0),
            )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<ExpenseSubmissionResponse>(encoded)

        assertEquals(original, decoded)
    }

    // An encode-then-decode round trip can't catch a `@SerialName` rename because both sides of the
    // pair change together — the two tests below assert the money-path DTOs against a literal wire
    // string too, so a rename breaks here instead of silently passing the gate.

    @Test
    fun submitMilesRequestK_wireNamesMatchExpectedJson() {
        val original =
            SubmitMilesRequestK(
                token = "tok-123",
                vehicleType = "CAR",
                origin = CoordsV2(lat = 12.9716, lng = 77.5946, name = "Origin"),
                destination = CoordsV2(lat = 13.0827, lng = 80.2707, name = "Destination"),
                distance = 42.5,
                originalDistance = 40.0,
                forms = mapOf(1L to "form-a"),
                notes = "client demo trip",
            )
        val expectedJson =
            """{"token":"tok-123","vehicleType":"CAR","origin":{"lat":12.9716,"lng":77.5946,"name":"Origin"},""" +
                """"destination":{"lat":13.0827,"lng":80.2707,"name":"Destination"},"distance":42.5,""" +
                """"originalDistance":40.0,"forms":{"1":"form-a"},"notes":"client demo trip"}"""

        assertEquals(expectedJson, json.encodeToString(original))
        assertEquals(original, json.decodeFromString<SubmitMilesRequestK>(expectedJson))
    }

    @Test
    fun expenseSubmissionResponse_wireNamesMatchExpectedJson() {
        val original =
            ExpenseSubmissionResponse(
                status = 1,
                amount = 500.0,
                currency = "INR",
                distance = 10.0,
                submissionStatus = SubmissionStatus.POLICY_VIOLATION,
                violations =
                    listOf(
                        PolicyViolation(
                            id = "max-distance-per-day",
                            title = "Over daily limit",
                            message = "Trip exceeds the configured daily distance limit.",
                            severity = ViolationSeverity.HARDSTOP,
                        ),
                    ),
                issuedVoucher = Voucher(id = 7L, number = "O-000001", amount = 500.0, status = VoucherStatus.FILED),
                transaction = TransactionRef(id = "O-000001", createdAtMillis = 1_700_000_000_000L, amount = 500.0),
            )
        val expectedJson =
            """{"status":1,"amount":500.0,"currency":"INR","distance":10.0,"submissionStatus":"POLICY_VIOLATION",""" +
                """"violations":[{"id":"max-distance-per-day","title":"Over daily limit",""" +
                """"message":"Trip exceeds the configured daily distance limit.","severity":"HARDSTOP"}],""" +
                """"issuedVoucher":{"id":7,"number":"O-000001","amount":500.0,"status":"FILED"},""" +
                """"transaction":{"id":"O-000001","createdAtMillis":1700000000000,"amount":500.0}}"""

        assertEquals(expectedJson, json.encodeToString(original))
        assertEquals(original, json.decodeFromString<ExpenseSubmissionResponse>(expectedJson))
    }

    // ── location/event sync: opId is the idempotency key the server dedups replays on ─────────
    // (see LocationPayloadV2.opId / EventPayloadV2.opId kdoc) — a plain round trip can't catch a
    // `@SerialName` rename here either, so assert the literal wire name directly.

    @Test
    fun locationPayloadV2_roundTripsAndOpIdMatchesWireName() {
        val original =
            LocationPayloadV2(
                lat = 12.5,
                lng = 77.5,
                token = "tok-loc-1",
                date = 1_700_000_000_000L,
                speed = 1.5f,
                activity = "walking",
                isMock = true,
                isAbnormal = true,
                displacement = 2.5,
                accuracy = 1.5f,
                provider = "gps",
                opId = "op-loc-123",
            )
        val expectedJson =
            """{"lat":12.5,"lng":77.5,"token":"tok-loc-1","date":1700000000000,"speed":1.5,""" +
                """"activity":"walking","isMock":true,"isAbnormal":true,"displacement":2.5,""" +
                """"accuracy":1.5,"provider":"gps","opId":"op-loc-123"}"""

        assertEquals(expectedJson, json.encodeToString(original))
        assertEquals(original, json.decodeFromString<LocationPayloadV2>(expectedJson))
    }

    @Test
    fun eventPayloadV2_roundTripsAndOpIdMatchesWireName() {
        val original =
            EventPayloadV2(
                token = "tok-evt-1",
                event = "TRIP_START",
                time = 1_700_000_000_000L,
                eventType = "START",
                audience = "driver",
                lat = 12.5,
                lng = 77.5,
                metadata = "meta-1",
                opId = "op-evt-123",
            )
        val expectedJson =
            """{"token":"tok-evt-1","event":"TRIP_START","time":1700000000000,"eventType":"START",""" +
                """"audience":"driver","lat":12.5,"lng":77.5,"metadata":"meta-1","opId":"op-evt-123"}"""

        assertEquals(expectedJson, json.encodeToString(original))
        assertEquals(original, json.decodeFromString<EventPayloadV2>(expectedJson))
    }

    // ── GET /v2/location and /v2/location.event envelopes: {status, count, data:[...]} ───────

    @Test
    fun locationResponseV2_envelopeMatchesExpectedJson() {
        val original =
            LocationResponseV2(
                status = 200,
                count = 1,
                data = listOf(LocationPayloadV2(lat = 1.0, lng = 2.0, token = "tok-a", date = 1000L)),
            )
        val expectedJson = """{"status":200,"count":1,"data":[{"lat":1.0,"lng":2.0,"token":"tok-a","date":1000}]}"""

        assertEquals(expectedJson, json.encodeToString(original))
        assertEquals(original, json.decodeFromString<LocationResponseV2>(expectedJson))
    }

    @Test
    fun eventResponseV2_envelopeMatchesExpectedJson() {
        val original =
            EventResponseV2(
                status = 200,
                count = 1,
                data = listOf(EventPayloadV2(token = "tok-b", event = "TRIP_END", time = 2000L)),
            )
        val expectedJson = """{"status":200,"count":1,"data":[{"token":"tok-b","event":"TRIP_END","time":2000}]}"""

        assertEquals(expectedJson, json.encodeToString(original))
        assertEquals(original, json.decodeFromString<EventResponseV2>(expectedJson))
    }

    // ── PLAN_V34 P2/A2: auth DTOs ──────────────────────────────────────────────

    @Test
    fun authRequest_roundTrips() {
        val original = AuthRequest(email = "demo@mileway.app", password = "mileway-demo-2026")

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<AuthRequest>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun authResponse_roundTrips() {
        val original = AuthResponse(accessToken = "access-123", refreshToken = "refresh-456", expiresInSeconds = 900L)

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<AuthResponse>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun refreshRequest_roundTrips() {
        val original = RefreshRequest(refreshToken = "refresh-456")

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<RefreshRequest>(encoded)

        assertEquals(original, decoded)
    }

    // ── PLAN_V33.1: check-in contract parity gaps ─────────────────────────────

    @Test
    fun checkInDetailsResponseV2_roundTrips() {
        val original =
            CheckInDetailsResponseV2(
                id = 3L,
                name = "Head Office",
                radius = 100.0,
                lat = 18.5204,
                lng = 73.8567,
                title = "Head Office",
                type = "OFFICE",
                des = "Main campus entrance",
            )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<CheckInDetailsResponseV2>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun submittedCheckInResponseV2_roundTrips() {
        val original =
            SubmittedCheckInResponseV2(
                checkIns =
                    listOf(
                        CheckInItem(
                            id = 42L,
                            time = 1_700_000_000_000L,
                            lat = 18.5204,
                            lng = 73.8567,
                            type = "OFFICE",
                            distance = 12.5,
                            forms = mapOf(1L to "form-a"),
                            vendorData = "vendor-ref-9",
                        ),
                    ),
            )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<SubmittedCheckInResponseV2>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun submittedCheckInResponseV2_defaultsRoundTrip() {
        val original = SubmittedCheckInResponseV2(checkIns = listOf(CheckInItem(id = 1L, time = 0L)))

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<SubmittedCheckInResponseV2>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun checkInRequestV2_matchesExpectedJson() {
        val original = CheckInRequestV2(lat = 10.0, lng = 20.0, typeId = 5L)
        val expectedJson = """{"lat":10.0,"lng":20.0,"typeId":5}"""

        assertEquals(expectedJson, json.encodeToString(original))
        assertEquals(original, json.decodeFromString<CheckInRequestV2>(expectedJson))
    }

    @Test
    fun allTypesResponseV2_roundTrips() {
        val original =
            AllTypesResponseV2(
                types = listOf(CheckInDetailsResponseV2(id = 7L, name = "Field Office", radius = 50.0)),
            )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<AllTypesResponseV2>(encoded)

        assertEquals(original, decoded)
    }
}
