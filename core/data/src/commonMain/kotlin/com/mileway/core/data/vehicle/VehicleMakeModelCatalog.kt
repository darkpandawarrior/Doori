package com.mileway.core.data.vehicle

/**
 * PLAN_V24 P11.2: a small seeded make→model catalog for the add-vehicle form's brand/model pickers.
 * Local/offline seed data (no backend) — enough to make the form real without a catalog service.
 */
object VehicleMakeModelCatalog {
    val makesToModels: Map<String, List<String>> =
        mapOf(
            "Honda" to listOf("Activa", "Shine", "City", "Amaze"),
            "Hero" to listOf("Splendor", "HF Deluxe", "Passion"),
            "Bajaj" to listOf("Pulsar", "Chetak", "RE Auto"),
            "Maruti Suzuki" to listOf("Swift", "Baleno", "Dzire", "WagonR"),
            "Hyundai" to listOf("i20", "Creta", "Venue"),
            "Tata" to listOf("Nexon", "Nexon EV", "Tiago", "Ace"),
            "Mahindra" to listOf("Bolero", "XUV300", "Treo Auto"),
            "TVS" to listOf("Jupiter", "Apache", "iQube"),
            "Ola" to listOf("S1", "S1 Pro"),
            "Ather" to listOf("450X", "450S"),
        )

    val makes: List<String> = makesToModels.keys.toList()

    fun modelsFor(make: String): List<String> = makesToModels[make].orEmpty()
}
