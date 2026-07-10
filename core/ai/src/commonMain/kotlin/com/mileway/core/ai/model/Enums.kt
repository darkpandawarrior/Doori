package com.mileway.core.ai.model

/** What kind of document [com.mileway.core.ai.DocumentIntelligence] believes it analyzed. */
enum class DocType { RECEIPT, INVOICE, ODOMETER, TRAVEL_TICKET, ID_DOCUMENT, OTHER }

/** A field [DocumentIntelligence] can extract from a document, regardless of doc type. */
enum class DocField { MERCHANT, TOTAL, TAX, DATE, INVOICE_NO, ODOMETER, CATEGORY, CURRENCY }

/**
 * Which analyzer tier produced an [com.mileway.core.ai.model.ExtractedValue] or a [DocType]
 * decision. [com.mileway.core.ai.AnalysisCombiner] breaks same-confidence ties in the order
 * ON_DEVICE_AI > HEURISTIC_CLASSIFIER > TEXT_RECOGNITION (AI > structured-heuristic > raw-regex).
 */
enum class AnalyzerSource { ON_DEVICE_AI, TEXT_RECOGNITION, HEURISTIC_CLASSIFIER }
