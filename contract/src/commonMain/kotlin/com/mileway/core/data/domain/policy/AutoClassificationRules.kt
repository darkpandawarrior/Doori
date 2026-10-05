package com.mileway.core.data.domain.policy

private const val LastHourOfDay = 23

/** Automatic suggestions do not replace an explicit user classification. */
enum class TripClassification { BUSINESS, PERSONAL }

enum class ClassificationSource { VEHICLE, PLACE, HOURS, LAST_TRIP }

data class ClassificationDecision(
    val classification: TripClassification,
    val source: ClassificationSource,
)

/** A configured working-hours window, inclusive start and exclusive end, including overnight shifts. */
data class ClassificationHours(
    val startHour: Int,
    val endHour: Int,
    val classification: TripClassification,
) {
    init {
        require(startHour in 0..LastHourOfDay && endHour in 0..LastHourOfDay && startHour != endHour)
    }

    fun contains(hour: Int): Boolean = if (startHour < endHour) hour in startHour until endHour else hour >= startHour || hour < endHour
}

/** Input is local trip-time data, not the current wall clock. First matching rule wins. */
data class AutoClassificationRules(
    val vehicles: Map<String, TripClassification> = emptyMap(),
    val places: Map<String, TripClassification> = emptyMap(),
    val hours: ClassificationHours? = null,
) {
    fun resolve(
        vehicleKey: String?,
        placeIds: List<String>,
        localHour: Int?,
        lastTrip: TripClassification?,
    ): ClassificationDecision? {
        vehicles[vehicleKey]?.let { return ClassificationDecision(it, ClassificationSource.VEHICLE) }
        placeIds.firstNotNullOfOrNull { places[it] }?.let { return ClassificationDecision(it, ClassificationSource.PLACE) }
        if (localHour != null && localHour in 0..LastHourOfDay && hours?.contains(localHour) == true) {
            return ClassificationDecision(hours.classification, ClassificationSource.HOURS)
        }
        return lastTrip?.let { ClassificationDecision(it, ClassificationSource.LAST_TRIP) }
    }
}

/** Extends policy evaluation without altering any existing violation messages. */
fun PolicyEngine.classifyTrip(
    rules: AutoClassificationRules,
    vehicleKey: String?,
    placeIds: List<String>,
    localHour: Int?,
    lastTrip: TripClassification?,
): ClassificationDecision? = rules.resolve(vehicleKey, placeIds, localHour, lastTrip)
