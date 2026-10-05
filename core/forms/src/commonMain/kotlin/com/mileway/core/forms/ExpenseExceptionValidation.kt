package com.mileway.core.forms

import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.JustificationReason

/** A missing-receipt declaration requires both explicit consent and an explanation. */
fun ExpenseLine.hasCompleteAffidavit(): Boolean = affidavitAccepted && affidavitNote.isNotBlank()

/** The offline OTHER fallback needs text; choosing a reason never changes policy severity. */
fun ExpenseLine.hasValidJustification(): Boolean = justificationReason != JustificationReason.OTHER || justificationNote.isNotBlank()
