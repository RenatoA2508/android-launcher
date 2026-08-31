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
     * search field
     *  ↓
     * type query
     *  ↓
     * shared AppSearchEngine
     *  ↓
     * ranking
     *  ↓
     * Compose results
     *  ↓
     * first result
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
             * Our app is Android's Home application.
             *
             * Explicitly return to Home before each
             * iteration.
             */
            device.pressHome()

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
             * Human-like upward swipe.
             *
             * This version was already verified on
             * the physical Samsung.
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
             * Search itself MUST open.
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
             * The real Compose search field MUST
             * also be exposed.
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
             * Explicit focus.
             */
            searchField.click()

            device.waitForIdle()

            /*
             * IMPORTANT:
             *
             * Put the text directly through UiAutomator
             * rather than depending on shell keyboard
             * injection.
             *
             * Setting this text forces:
             *
             * - query state update
             * - AppSearchEngine.search()
             * - normalization
             * - aliases/acronym evaluation
             * - ranking
             * - result recomposition
             */
            searchField.setText(
                SEARCH_QUERY
            )

            /*
             * Unlike the previous generator, a result
             * is now MANDATORY.
             *
             * If the shared search engine isn't executed
             * correctly, profile generation fails instead
             * of silently producing an incomplete profile.
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

            check(
                firstResultVisible
            ) {
                "Search ranking did not produce a result."
            }

            /*
             * Touch the result as part of the critical
             * Search journey.
             *
             * pressHome() at the beginning of the next
             * iteration returns us to the launcher.
             */
            device
                .findObject(
                    By.res(
                        FIRST_SEARCH_RESULT_TAG
                    )
                )
                .click()

            device.waitForIdle()
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

        /*
         * Generic query suitable for the physical
         * test device and broad enough to exercise
         * several ranking candidates.
         */
        const val SEARCH_QUERY =
            "a"

        const val UI_TIMEOUT_MS =
            5_000L

        const val RESULT_TIMEOUT_MS =
            5_000L
    }
}
