package com.example.jehun.gulliver

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented splash_container, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under splash_container.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("com.example.jehun.gulliver", appContext.packageName)
    }
}
