package com.renato.launcher.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule =
        BaselineProfileRule()

    @Test
    fun generate() =
        baselineProfileRule.collect(
            packageName = TARGET_PACKAGE,
            maxIterations = 8,
            stableIterations = 3,
            includeInStartupProfile = true
        ) {
            /*
             * The target application IS the system Home.
             *
             * Do not call pressHome() before this,
             * because doing so already launches the
             * target launcher.
             *
             * Starting MainActivity directly still
             * exercises the same launcher startup
             * code that we want ART to precompile.
             */
            startActivityAndWait()

            device.waitForIdle()
        }

    private companion object {
        const val TARGET_PACKAGE =
            "com.renato.launcher"
    }
}
