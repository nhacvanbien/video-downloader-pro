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

package com.smarttool.videodownloader.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        Assert.assertEquals("com.tevo.template.android", appContext.packageName)
    }
}
