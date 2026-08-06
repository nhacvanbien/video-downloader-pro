/*
 * Copyright (c) 2025. Tevo Global Limited
 *
 * This software and all accompanying documentation is the sole property of
 * Tevo Global Limited and is protected by copyright law and international treaties.
 *
 * Unauthorized copying, distribution, or reproduction of this software, or any
 * portion of it, is strictly prohibited. The software is licensed to you solely for
 * your personal use and may not be used for commercial purposes without
 * a separate license agreement.
 *
 * You may not modify, reverse engineer, decompile, or disassemble this software.
 * You are not permitted to remove or alter any copyright notices or proprietary
 * legends from the software.
 *
 * All rights not expressly granted herein are reserved by Tevo Global Limited.
 *
 * Contact information: hello@tevo.app
 */

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
