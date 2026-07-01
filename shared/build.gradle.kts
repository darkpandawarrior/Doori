plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

/**
 * iOS umbrella module: produces the single `Mileway.framework` the Xcode app links against.
 *
 * The framework can't live in `core:ui`, because the Swift app also calls into `feature:tracking`
 * (`IosTrackingEntryKt.MilwayViewController`, `IosBgTaskDispatcher`), and a core module must not depend
 * on a feature (that would be a dependency cycle: feature:tracking already depends on core:ui). This
 * module sits *above* both and re-exports their iOS entrypoints, so adding more iOS-facing features later
 * means depending on them here — never making a feature depend on its siblings.
 *
 * Exported API surfaced to Swift:
 *  - core:ui          → MainViewController, ReferralBridge, PushBridge, DeepLinkBridge
 *  - feature:tracking → MilwayViewController (IosTrackingEntry), IosBgTaskDispatcher
 *  - feature:agent    → iosAgentModule (IosAgentEntry)
 */
kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Mileway"
            export(project(":core:ui"))
            export(project(":feature:tracking"))
            export(project(":feature:agent"))
        }
    }

    sourceSets {
        commonMain.dependencies {
            // api(...) is required for export(...) above to surface these modules' public API in the framework.
            api(project(":core:ui"))
            api(project(":feature:tracking"))
            api(project(":feature:agent"))
        }
        iosMain.dependencies {
            // No extra deps — MilwayAppViewController() uses api deps already imported above.
        }
    }
}
