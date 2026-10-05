package com.mileway.core.forms

/**
 * The dynamic field types dynamic form schemas can describe. A few DiCE field kinds collapse into one Mileway type where
 * they share rendering/validation shape:
 * - [MASTER] covers master / multimaster / multiselect lookups.
 * - [FILE_PDF] covers file / pdf / multiattachments uploads.
 * - [EMPLOYEE_DEPARTMENT] covers employee_department / cost_center pickers.
 */
enum class FormFieldType {
    TEXT,
    TEXTAREA,
    NUMBER,
    EMAIL,
    CURRENCY,
    SELECT,
    RATING,
    DATE,
    TIME,
    LOCATION,
    DECLARATION,
    CITY_AIRPORT,
    IRN,
    MASTER,
    FILE_PDF,
    EMPLOYEE_DEPARTMENT,
    PERCENTAGE_SPLIT,
    ATTENDEE_LIST,
    ITEMIZED_LINES,
}
