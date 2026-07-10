package com.mileway.core.forms

/** The value a user has entered/selected for one [MockFormSchema] field. */
sealed interface FormFieldValue {
    data class Text(val value: String) : FormFieldValue

    data class Number(val value: Double?) : FormFieldValue

    data class Select(val value: String?) : FormFieldValue

    data class MultiSelect(val values: List<String>) : FormFieldValue

    /** ISO-8601 date string, e.g. `"2026-07-10"`; null when unset. */
    data class Date(val isoValue: String?) : FormFieldValue

    /** `"HH:mm"`-shaped time string; null when unset. */
    data class Time(val value: String?) : FormFieldValue

    data class FileRef(val paths: List<String>) : FormFieldValue

    data class Currency(val amount: Double?, val currencyCode: String) : FormFieldValue

    data class Rating(val value: Int) : FormFieldValue

    data class Location(val lat: Double?, val lng: Double?, val label: String?) : FormFieldValue

    data class Declaration(val accepted: Boolean) : FormFieldValue
}
