import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
    alias(libs.plugins.google.firebase.firebase.perf)
    alias(libs.plugins.google.firebase.distribution)
    alias(libs.plugins.navigation.safeargs)
    alias(libs.plugins.spotless)
    id("kotlin-kapt")
}

android {
    namespace = "com.smarttool.videodownloader.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.videodownloader.videoplayer.videosaver.download.video"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
        compose = true
    }

    flavorDimensions += "default"

    productFlavors {
        create("develop") {
            dimension = "default"
            buildConfigField("Long", "Minimum_Fetch", "5L")
            buildConfigField("String", "facebook_app_id", "\"1303924174289230\"")
            buildConfigField("String", "facebook_client_token", "\"ea374c985a08b0c8c400bcd17c54f4d4\"")
            buildConfigField("String", "INTER_SPLASH", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "BANNER_SPLASH", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "NATIVE_LANGUAGE_1_1", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "NATIVE_LANGUAGE_1_2", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "NATIVE_ONBOARD_FULLSCREEN_1_1", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "NATIVE_ONBOARD_FULLSCREEN_1_2", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "NATIVE_PERMISSION", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "NATIVE_HOME", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "BANNER_ALL", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "NATIVE_SMALL_ALL", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "OPEN_RESUME", "\"ca-app-pub-3940256099942544/9257395921\"")
            buildConfigField("String", "INTER_SPLASH_HIGH", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "NATIVE_LANGUAGE_1_1_HIGH", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "NATIVE_LANGUAGE_1_2_HIGH", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "NATIVE_ONBOARD_FULLSCREEN_1_1_HIGH", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "NATIVE_ONBOARD_FULLSCREEN_1_2_HIGH", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "INTER_ALL", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "NATIVE_FULL_ALL", "\"ca-app-pub-3940256099942544/2247696110\"")

        }
        create("production") {
            dimension = "default"
            buildConfigField("Long", "Minimum_Fetch", "3600L")
            buildConfigField("String", "facebook_app_id", "\"1303924174289230\"")
            buildConfigField("String", "facebook_client_token", "\"ea374c985a08b0c8c400bcd17c54f4d4\"")
            buildConfigField("String", "INTER_SPLASH", "\"ca-app-pub-1249320623511529/5683148735\"")
            buildConfigField("String", "BANNER_SPLASH", "\"ca-app-pub-1249320623511529/4370067061\"")
            buildConfigField("String", "NATIVE_LANGUAGE_1_1", "\"ca-app-pub-1249320623511529/1389638368\"")
            buildConfigField("String", "NATIVE_LANGUAGE_1_2", "\"ca-app-pub-1249320623511529/3056985394\"")
            buildConfigField("String", "NATIVE_ONBOARD_FULLSCREEN_1_1", "\"ca-app-pub-1249320623511529/4314226747\"")
            buildConfigField("String", "NATIVE_ONBOARD_FULLSCREEN_1_2", "\"ca-app-pub-1249320623511529/9272514731\"")
            buildConfigField("String", "NATIVE_PERMISSION", "\"ca-app-pub-1249320623511529/1688063405\"")
            buildConfigField("String", "NATIVE_HOME", "\"ca-app-pub-1249320623511529/8885731803\"")
            buildConfigField("String", "BANNER_ALL", "\"ca-app-pub-1249320623511529/8181319563\"")
            buildConfigField("String", "NATIVE_SMALL_ALL", "\"ca-app-pub-1249320623511529/8877246141\"")
            buildConfigField("String", "OPEN_RESUME", "\"ca-app-pub-1249320623511529/5555156222\"")
            buildConfigField("String", "INTER_SPLASH_HIGH", "\"ca-app-pub-1249320623511529/6082158815\"")
            buildConfigField("String", "NATIVE_LANGUAGE_1_1_HIGH", "\"ca-app-pub-1249320623511529/2142913807\"")
            buildConfigField("String", "NATIVE_LANGUAGE_1_2_HIGH", "\"ca-app-pub-1249320623511529/1346328144\"")
            buildConfigField("String", "NATIVE_ONBOARD_FULLSCREEN_1_1_HIGH", "\"ca-app-pub-1249320623511529/5062211878\"")
            buildConfigField("String", "NATIVE_ONBOARD_FULLSCREEN_1_2_HIGH", "\"ca-app-pub-1249320623511529/5144589463\"")
            buildConfigField("String", "INTER_ALL", "\"ca-app-pub-1249320623511529/9304113717\"")
            buildConfigField("String", "NATIVE_FULL_ALL", "\"ca-app-pub-1249320623511529/9583315311\"")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            manifestPlaceholders["app_id"] = "ca-app-pub-1249320623511529~7642263518"
            manifestPlaceholders["facebook_app_id"] = "1303924174289230"
            manifestPlaceholders["facebook_client_token"] = "ea374c985a08b0c8c400bcd17c54f4d4"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            manifestPlaceholders["app_id"] = "ca-app-pub-1249320623511529~7642263518"
            manifestPlaceholders["facebook_app_id"] = "1303924174289230"
            manifestPlaceholders["facebook_client_token"] = "ea374c985a08b0c8c400bcd17c54f4d4"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.messaging)

    // AndroidX
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.constraintlayout)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)


    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.viewbinding)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.material3)
    implementation(libs.compose.activity)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    // Moshi
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.moshi.adapters)
    kapt(libs.moshi.kotlin.codegen)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Glide
    implementation(libs.glide)
    kapt(libs.glide.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    // Misc
    implementation(libs.timber)
    implementation(libs.ssp)
    implementation(libs.sdp)
    implementation(libs.security.crypto)
    implementation(libs.dotsindicator)
    implementation(libs.review)
    implementation(libs.app.update)
    implementation(libs.facebook.ads)
    implementation(libs.facebook.sdk)
    implementation(libs.facebook.shimmer)
    implementation(libs.lottie)
    implementation(libs.circular.progress)
    implementation(libs.balloon)
//    implementation(libs.swipe.reveal.layout)
    implementation("com.github.zerobranch:SwipeLayout:1.3.1")
    implementation(libs.infer.annotation)
    implementation(libs.commons.io)
    implementation(libs.eventbus)
    implementation(libs.serialization.json)

    // Retrofit + OkHttp
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.coroutines.adapter)
    implementation(libs.retrofit.converter.moshi)

    // WorkManager
    implementation(libs.work.multiprocess)

    // ExoPlayer / Media3
    implementation(libs.exoplayer)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.rtsp)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)
    implementation(libs.media3.extractor)
    implementation(libs.media3.database)
    implementation(libs.media3.decoder)
    implementation(libs.media3.datasource)
    implementation(libs.media3.common)

    implementation(libs.youtube.dl)
    implementation(libs.youtube.dl.ffmpeg)
    implementation(libs.jsoup)
    implementation(libs.retrofit.converter.gson)
    implementation("com.daimajia.androidanimations:library:2.4@aar")
    implementation("com.daimajia.easing:library:2.4@aar")
    implementation(libs.material)
    coreLibraryDesugaring(libs.android.desugarJdkLibs)

    implementation(project(":rating-library"))
}
