package com.mileway.core.data.plugin

/*
 * PLAN_V24 P0.1 — the vocabulary of the Plugin Registry, Mileway's single feature-composition
 * mechanism. Every feature surface below the composition root is gated by a PluginDescriptor;
 * there is no hardcoded feature visibility after this plan.
 *
 * The registry merges three reference-app patterns deliberately: a 3-tier resolution (debug
 * force → per-user unlock → config default), a dual-layer gating (a menu tag AND a capability
 * flag — modelled here as distinct PluginKinds, not two parallel systems), and a
 * server-composed side menu (whole modules toggled — modelled as TILE plugins in CORE_MODULES).
 */

/**
 * What a plugin controls:
 * - [TILE]: a whole surface/module/nav-destination that renders or not (home tiles, feature
 *   modules, profile-hub rows).
 * - [CAPABILITY]: a behaviour flag inside an already-visible surface (e.g. "OTP via call",
 *   "multiple vehicles allowed").
 * - [VALUE]: a tunable scalar/enum ([valueSpec] required) — sync interval, spike threshold, etc.
 */
enum class PluginKind { TILE, CAPABILITY, VALUE }

/** Grouping used by the Master Plugin page (P0.3) and the profile-hub reorg (P14.1). */
enum class PluginCategory {
    AUTH,
    ONBOARDING,
    PROFILE,
    VERIFICATION,
    GROWTH,
    MEMBERSHIP,
    LIFECYCLE,
    PAYMENTS,
    TRACKING,
    TRACKING_TUNING,
    VEHICLES,
    ENGAGEMENT,
    BANNERS,
    CORE_MODULES,
}

/**
 * Which resolution layer supplied a plugin's current value — surfaced as a source chip on the
 * Master Plugin page so a demo can show *why* a flag is on. Resolution order (highest wins):
 * [FORCED] > [USER] > [PRESET] > [DEFAULT] (see [PluginRegistry]).
 */
enum class PluginSource { DEFAULT, PRESET, USER, FORCED }

/** The typed spec for a [PluginKind.VALUE] plugin: bounds + unit drive the Master Plugin editor. */
sealed interface PluginValueSpec {
    val default: PluginValue

    data class IntSpec(
        val defaultValue: Int,
        val min: Int,
        val max: Int,
        val step: Int = 1,
        val unit: String? = null,
    ) : PluginValueSpec {
        override val default: PluginValue = PluginValue.IntVal(defaultValue)
    }

    data class DoubleSpec(
        val defaultValue: Double,
        val min: Double,
        val max: Double,
        val step: Double = 0.1,
        val unit: String? = null,
    ) : PluginValueSpec {
        override val default: PluginValue = PluginValue.DoubleVal(defaultValue)
    }

    data class EnumSpec(
        val defaultValue: String,
        val options: List<String>,
    ) : PluginValueSpec {
        override val default: PluginValue = PluginValue.Str(defaultValue)
    }
}

/**
 * A resolved plugin value. All layers persist as raw [String] (Room TEXT column / DataStore
 * string / preset map) and interpret per descriptor — see [toRaw]/[parse]. This uniform raw-string
 * representation is what lets one resolution path serve TILE, CAPABILITY, and VALUE plugins.
 */
sealed interface PluginValue {
    data class Bool(val value: Boolean) : PluginValue

    data class IntVal(val value: Int) : PluginValue

    data class DoubleVal(val value: Double) : PluginValue

    data class Str(val value: String) : PluginValue

    fun toRaw(): String =
        when (this) {
            is Bool -> value.toString()
            is IntVal -> value.toString()
            is DoubleVal -> value.toString()
            is Str -> value
        }

    companion object {
        /** Interpret a persisted raw string against a descriptor's kind/spec. */
        fun parse(
            raw: String,
            descriptor: PluginDescriptor,
        ): PluginValue =
            when (descriptor.kind) {
                PluginKind.TILE, PluginKind.CAPABILITY ->
                    Bool(raw.toBooleanStrictOrNull() ?: descriptor.defaultOn)
                PluginKind.VALUE ->
                    when (descriptor.valueSpec) {
                        is PluginValueSpec.IntSpec -> IntVal(raw.toIntOrNull() ?: descriptor.valueSpec.defaultValue)
                        is PluginValueSpec.DoubleSpec -> DoubleVal(raw.toDoubleOrNull() ?: descriptor.valueSpec.defaultValue)
                        is PluginValueSpec.EnumSpec -> Str(raw.ifBlank { descriptor.valueSpec.defaultValue })
                        null -> Str(raw)
                    }
            }
    }
}

/**
 * The static definition of one plugin. [titleKey]/[descriptionKey] are Compose-resource string
 * names resolved dynamically at the UI layer (`Res.allStringResources[key]`) so this data-layer
 * type carries no resource dependency (core:data does not depend on compose-resources).
 *
 * @param defaultOn the DEFAULT-layer value for TILE/CAPABILITY plugins (ignored for VALUE).
 * @param valueSpec required for VALUE plugins; its [PluginValueSpec.default] is the DEFAULT layer.
 * @param requiresRestart true if toggling only takes effect after an app restart (shows a chip).
 * @param experimental true to hide behind the 7-tap experimental unlock on the Master Plugin page.
 */
data class PluginDescriptor(
    val id: String,
    val kind: PluginKind,
    val category: PluginCategory,
    val titleKey: String,
    val descriptionKey: String,
    val defaultOn: Boolean = true,
    val valueSpec: PluginValueSpec? = null,
    val requiresRestart: Boolean = false,
    val experimental: Boolean = false,
) {
    init {
        require((kind == PluginKind.VALUE) == (valueSpec != null)) {
            "VALUE plugins must have a valueSpec and non-VALUE plugins must not: id=$id kind=$kind"
        }
    }

    /** The DEFAULT-layer resolved value with no overrides applied. */
    val defaultValue: PluginValue
        get() =
            when (kind) {
                PluginKind.TILE, PluginKind.CAPABILITY -> PluginValue.Bool(defaultOn)
                PluginKind.VALUE -> valueSpec!!.default
            }
}
