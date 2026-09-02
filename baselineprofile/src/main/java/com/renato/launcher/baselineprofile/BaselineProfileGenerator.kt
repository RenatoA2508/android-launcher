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

    /*
     * ============================================================
     * ALL APPS
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
     * Todas las aplicaciones
     *  ↓
     * All Apps
     *  ↓
     * initial grid composition
     *  ↓
     * several catalog rows / lazy icon loading
     */
    @Test
    fun allApps() =
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
             * Each iteration starts from the real launcher Home.
             *
             * MainActivity is singleTask, so pressHome() also resets
             * any internal launcher screen left from the prior iteration.
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
             * Open Search using the same verified physical gesture as
             * the existing Search profile.
             */
            val centerX =
                device.displayWidth / 2

            val searchStartY =
                (
                    device.displayHeight *
                        0.80f
                    ).toInt()

            val searchEndY =
                (
                    device.displayHeight *
                        0.38f
                    ).toInt()

            device.swipe(
                centerX,
                searchStartY,
                centerX,
                searchEndY,
                60
            )

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
             * The All Apps launcher is explicitly tagged so this journey
             * never depends on visible text, locale, or pixel coordinates.
             */
            val allAppsButtonVisible =
                device.wait(
                    Until.hasObject(
                        By.res(
                            ALL_APPS_BUTTON_TAG
                        )
                    ),
                    UI_TIMEOUT_MS
                )

            check(
                allAppsButtonVisible
            ) {
                "All Apps button did not appear in empty Search."
            }

            device
                .findObject(
                    By.res(
                        ALL_APPS_BUTTON_TAG
                    )
                )
                .click()

            val allAppsVisible =
                device.wait(
                    Until.hasObject(
                        By.res(
                            ALL_APPS_ROOT_TAG
                        )
                    ),
                    UI_TIMEOUT_MS
                )

            check(
                allAppsVisible
            ) {
                "All Apps screen did not open."
            }

            device.waitForIdle()

            /*
             * Exercise LazyVerticalGrid composition beyond the first screen.
             *
             * The swipe stays in the center of the display, away from the
             * alphabet rail on the right, so it profiles normal catalog
             * scrolling instead of invoking fast-index navigation.
             */
            val catalogStartY =
                (
                    device.displayHeight *
                        0.78f
                    ).toInt()

            val catalogEndY =
                (
                    device.displayHeight *
                        0.30f
                    ).toInt()

            repeat(
                ALL_APPS_SCROLL_COUNT
            ) {
                device.swipe(
                    centerX,
                    catalogStartY,
                    centerX,
                    catalogEndY,
                    55
                )

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

        const val ALL_APPS_BUTTON_TAG =
            "launcher_all_apps_button"

        const val ALL_APPS_ROOT_TAG =
            "launcher_all_apps_root"

        const val ALL_APPS_SCROLL_COUNT =
            3

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
