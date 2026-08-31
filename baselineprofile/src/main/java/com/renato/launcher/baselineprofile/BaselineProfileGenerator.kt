package com.renato.launcher.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule =
        BaselineProfileRule()

    /*
     * ============================================================
     * STARTUP
     * ============================================================
     *
     * Startup Profile:
     *
     * application start
     *       ↓
     * MainActivity
     *       ↓
     * Home
     */
    @Test
    fun startup() =
        baselineProfileRule.collect(
            packageName =
                TARGET_PACKAGE,
            includeInStartupProfile =
                true,
            maxIterations =
                8,
            stableIterations =
                3
        ) {
            /*
             * For startup profiling we still launch
             * MainActivity directly.
             */
            startActivityAndWait()

            device.waitForIdle()
        }

    /*
     * ============================================================
     * SEARCH
     * ============================================================
     *
     * Baseline Profile only:
     *
     * Home
     *  ↓
     * swipe up
     *  ↓
     * Search
     *  ↓
     * keyboard
     *  ↓
     * type query
     *  ↓
     * ranking
     *  ↓
     * first result
     *  ↓
     * launch
     */
    @Test
    fun search() =
        baselineProfileRule.collect(
            packageName =
                TARGET_PACKAGE,
            includeInStartupProfile =
                false,
            maxIterations =
                8,
            stableIterations =
                3
        ) {

            /*
             * IMPORTANT:
             *
             * Our application IS Android's Home app.
             *
             * Instead of launching MainActivity and
             * assuming that its internal state is HOME,
             * explicitly press Android's Home button.
             */
            device.pressHome()

            /*
             * Wait until the actual Home composable
             * is visible.
             */
            val homeVisible =
                device.wait(
                    Until.hasObject(
                        By.res(
                            HOME_ROOT_TAG
                        )
                    ),
                    UI_TIMEOUT_MS
                )

            check(
                homeVisible
            ) {
                "Launcher Home did not appear."
            }

            device.waitForIdle()

            /*
             * Swipe upward through an empty part of Home.
             *
             * 60 steps intentionally produces a gesture
             * closer to a real finger than the previous
             * very short swipe.
             */
            val centerX =
                device.displayWidth / 2

            val startY =
                (
                    device.displayHeight *
                        0.80f
                    ).toInt()

            val endY =
                (
                    device.displayHeight *
                        0.38f
                    ).toInt()

            device.swipe(
                centerX,
                startY,
                centerX,
                endY,
                60
            )

            /*
             * First verify that the gesture actually
             * changed the launcher from Home to Search.
             */
            val searchVisible =
                device.wait(
                    Until.hasObject(
                        By.res(
                            SEARCH_ROOT_TAG
                        )
                    ),
                    UI_TIMEOUT_MS
                )

            check(
                searchVisible
            ) {
                "Search screen did not open after swipe."
            }

            /*
             * Now verify the search field.
             */
            val searchFieldVisible =
                device.wait(
                    Until.hasObject(
                        By.res(
                            SEARCH_FIELD_TAG
                        )
                    ),
                    UI_TIMEOUT_MS
                )

            check(
                searchFieldVisible
            ) {
                "Search field did not appear."
            }

            val searchField =
                device.findObject(
                    By.res(
                        SEARCH_FIELD_TAG
                    )
                )

            /*
             * Search already requests focus itself,
             * but clicking makes the benchmark robust
             * if IME focus is slightly delayed.
             */
            searchField.click()

            device.waitForIdle()

            /*
             * Generic query.
             *
             * We intentionally don't depend on
             * WhatsApp/Discord/etc. being installed.
             */
            device.executeShellCommand(
                "input text a"
            )

            /*
             * Give Compose + ranking + grid a chance
             * to expose the first result.
             */
            val firstResultVisible =
                device.wait(
                    Until.hasObject(
                        By.res(
                            FIRST_SEARCH_RESULT_TAG
                        )
                    ),
                    RESULT_TIMEOUT_MS
                )

            /*
             * If there is a result, also profile the
             * complete Search -> launch app path.
             *
             * If a device happens to have no matching
             * application, Search itself can still
             * generate a valid profile.
             */
            if (firstResultVisible) {

                device
                    .findObject(
                        By.res(
                            FIRST_SEARCH_RESULT_TAG
                        )
                    )
                    .click()

                device.waitForIdle()
            }
        }

    private companion object {

        const val TARGET_PACKAGE =
            "com.renato.launcher"

        const val HOME_ROOT_TAG =
            "launcher_home_root"

        const val SEARCH_ROOT_TAG =
            "launcher_search_root"

        const val SEARCH_FIELD_TAG =
            "launcher_search_field"

        const val FIRST_SEARCH_RESULT_TAG =
            "launcher_search_result_0"

        const val UI_TIMEOUT_MS =
            5_000L

        const val RESULT_TIMEOUT_MS =
            3_000L
    }
}
