package com.mileway.core.data.util

/**
 * Wall-clock durations in milliseconds.
 *
 * `86_400_000L` and `3_600_000L` were written longhand in the repositories and screens that do
 * epoch-millis arithmetic, where the reader has to count zeroes to tell a day from an hour.
 * Expressed as a product so the value is its own derivation.
 *
 * They live in `:contract` because that is the one module every data and feature module already
 * sees — `:core:data` exposes it with `api`.
 *
 * ponytail: plain `Long`s, not `kotlin.time.Duration`. Every call site here divides or multiplies
 * a raw epoch-millis `Long` that comes off a DTO or a DAO row. Upgrade path: swap for
 * `1.days.inWholeMilliseconds` when those boundaries carry `Instant`/`Duration` instead of `Long`.
 */
const val MillisPerSecond: Long = 1_000L

const val MillisPerMinute: Long = 60L * MillisPerSecond

const val MillisPerHour: Long = 60L * MillisPerMinute

const val MillisPerDay: Long = 24L * MillisPerHour
