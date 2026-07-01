package com.mileway.core.network.model

// Policy / submission-outcome models for the network API surface.
// Concrete types are declared in core:data; this file re-exports them under the network model
// package so API consumers can import policy types without depending on core:data directly.

/** Overall outcome of a mileage submission: SUCCESS, NEEDS_APPROVAL, REIMBURSABLE_ADJUSTED, POLICY_VIOLATION or HARD_STOP. */
typealias SubmissionStatus = com.mileway.core.data.model.network.SubmissionStatus

/** Severity of a single policy violation: REIMBURSABLE, VIOLATION or HARDSTOP. */
typealias ViolationSeverity = com.mileway.core.data.model.network.ViolationSeverity

/** A single policy violation (e.g. `max-distance-per-day`) attached to a submission. */
typealias PolicyViolation = com.mileway.core.data.model.network.PolicyViolation

/** Declaration text the user must acknowledge before a voucher can be filed. */
typealias VoucherDeclaration = com.mileway.core.data.model.network.VoucherDeclaration

/** Lifecycle state of a voucher: UNCLAIMED, FILED or CREATED. */
typealias VoucherStatus = com.mileway.core.data.model.network.VoucherStatus

/** Voucher record created for a reimbursable submission. */
typealias Voucher = com.mileway.core.data.model.network.Voucher

/** Reference to the ledger transaction created for a submission (id like `O-INDIAN-000048769`). */
typealias TransactionRef = com.mileway.core.data.model.network.TransactionRef
