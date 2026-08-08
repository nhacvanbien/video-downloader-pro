import com.diffplug.gradle.spotless.SpotlessExtension

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    alias(libs.plugins.google.firebase.crashlytics) apply false
    alias(libs.plugins.google.firebase.distribution) apply false
    alias(libs.plugins.google.firebase.firebase.perf) apply false
}

subprojects {
    plugins.apply(rootProject.libs.plugins.spotless.get().pluginId)
    extensions.configure<SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            targetExclude("src/test/resources/**")
            ktlint(libs.ktlint.asProvider().get().version)
                .editorConfigOverride(
                    mapOf(
                        "indent_size" to "4",
                        "ktlint_compose_modifier-missing-check" to "disabled",
                        "ktlint_compose_compositionlocal-allowlist" to "disabled",
                    ),
                )
                .customRuleSets(listOf(libs.ktlint.composeRules.get().toString()))
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint(libs.ktlint.asProvider().get().version)
                .editorConfigOverride(
                    mapOf("indent_size" to "4"),
                )
        }
    }
}
