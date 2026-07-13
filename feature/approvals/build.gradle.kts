plugins {
    id("shared.cmp.feature")
}

kotlin {
    android {
        namespace = "com.mileway.feature.approvals"
        compileSdk = 37
        minSdk = 30
        // PLAN_V28 P28.2: run commonTest (ClarificationRepository/ViewModel tests) on the JVM host.
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:data"))
            implementation(project(":core:ui"))
            // PLAN_V28 P28.8: AuditFlags.receiptVerified can read a real core:ai DocumentAnalysis
            // when the approval's receipt was actually scanned.
            implementation(project(":core:ai"))
            // PLAN_V28 P28.6/V26 P-STR.1: SeekClarificationSheet's attach button reuses core:media's
            // AttachmentItem/rememberMediaCaptureLauncher — no new attachment model.
            implementation(project(":core:media"))
            // Date separators in SeekClarificationSheet (V28 P28.6) — same lib core:data/core:ui already use.
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
