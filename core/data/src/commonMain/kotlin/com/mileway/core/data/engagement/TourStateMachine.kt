package com.mileway.core.data.engagement

/**
 * PLAN_V24 P12.5 — the interactive training tour's pure state machine. This is the offline, testable
 * heart of the tour: the ordered walkthrough steps and the advance/skip/complete transitions, with
 * no Compose, Room, or timing coupling. [TourRepository] persists a [TourState] per account;
 * `TrainingTourViewModel` renders it and feeds a simulated distance ramp between steps.
 *
 * The eight steps walk a simulated trip lifecycle end to end: offer → start → live HUD → pause →
 * stop → classify → submit → completion (which awards the [BadgeId.TOUR_COMPLETE] badge + confetti).
 */
enum class TourStep {
    /** Intro offer coach-mark — "take a quick tour?". */
    INTRO,

    /** Anchored to the Start control — begins the simulated trip. */
    START,

    /** The live HUD (distance/duration) as the simulated ramp runs. */
    LIVE_HUD,

    /** Anchored to the Pause/Resume control. */
    PAUSE,

    /** Anchored to the Stop control — ends the simulated trip. */
    STOP,

    /** The classify step — pick a trip purpose. */
    CLASSIFY,

    /** The submit step. */
    SUBMIT,

    /** Completion — confetti + tour-complete badge. Reaching this marks the tour completed. */
    COMPLETE,
}

/** Terminal-or-not outcome of a tour run. */
enum class TourStatus { IN_PROGRESS, COMPLETED, SKIPPED }

/** One immutable snapshot of tour progress: which step is showing and whether it has terminated. */
data class TourState(
    val step: TourStep = TourStep.INTRO,
    val status: TourStatus = TourStatus.IN_PROGRESS,
)

/** The ordered step list the overlay walks. */
val TOUR_STEPS: List<TourStep> = TourStep.entries

/**
 * Advance one step. A no-op once the tour has terminated (completed/skipped) or is already at the
 * final [TourStep.COMPLETE] step. Advancing onto [TourStep.COMPLETE] flips the status to
 * [TourStatus.COMPLETED] — that is the single point the completion badge is awarded.
 */
fun tourAdvance(state: TourState): TourState {
    if (state.status != TourStatus.IN_PROGRESS) return state
    val nextOrdinal = state.step.ordinal + 1
    if (nextOrdinal > TourStep.COMPLETE.ordinal) return state
    val nextStep = TOUR_STEPS[nextOrdinal]
    val status = if (nextStep == TourStep.COMPLETE) TourStatus.COMPLETED else TourStatus.IN_PROGRESS
    return TourState(step = nextStep, status = status)
}

/** Skip the tour: terminal [TourStatus.SKIPPED], keeping the step the user bailed on. No-op if already terminal. */
fun tourSkip(state: TourState): TourState {
    if (state.status != TourStatus.IN_PROGRESS) return state
    return state.copy(status = TourStatus.SKIPPED)
}

/** Restart the tour from the beginning (re-entry from the Support hub). */
fun tourRestart(): TourState = TourState()
