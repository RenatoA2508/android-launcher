package com.renato.launcher.search

import com.renato.launcher.core.model.InstalledApp
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/**
 * Shared deterministic application search engine.
 *
 * This is the single source of truth for app ranking across
 * the launcher.
 *
 * Current consumers:
 *
 * - Main Search
 * - Favorite Picker
 *
 * Future consumers can include:
 *
 * - All Apps
 * - Collections
 *
 * Ranking priority:
 *
 * 0 -> Exact application label
 * 1 -> Exact known alias / exact automatic acronym
 * 2 -> Application label starts with query
 * 3 -> Any word starts with query
 * 4 -> Partial alias / partial acronym
 * 5 -> Query appears anywhere in the label
 *
 * Ties are resolved by:
 *
 * 1. Shorter normalized label
 * 2. Locale-aware alphabetical order
 */
class AppSearchEngine(
    apps: List<InstalledApp>,
    locale: Locale = Locale.getDefault()
) {

    private val collator: Collator =
        Collator
            .getInstance(locale)
            .apply {
                strength =
                    Collator.PRIMARY
            }

    /*
     * The expensive normalization work happens once when this
     * engine is created.
     *
     * SearchScreen and FavoritePickerScreen should therefore
     * create the engine with remember(apps).
     */
    private val searchIndex: List<SearchIndexEntry> =
        apps.map { app ->

            val normalizedLabel =
                normalizeSearchText(
                    app.label
                )

            val words =
                splitSearchWords(
                    normalizedLabel
                )

            SearchIndexEntry(
                app =
                    app,
                normalizedLabel =
                    normalizedLabel,
                normalizedWords =
                    words,
                acronym =
                    buildAcronym(
                        words
                    ),
                aliases =
                    searchAliasesFor(
                        normalizedLabel
                    )
            )
        }

    /**
     * Returns applications ordered according to the launcher's
     * shared deterministic ranking rules.
     *
     * A blank query intentionally returns an empty list.
     * Screens decide themselves what should appear when no
     * query exists.
     */
    fun search(
        query: String
    ): List<InstalledApp> {

        val normalizedQuery =
            normalizeSearchText(
                query
            )

        if (
            normalizedQuery.isBlank()
        ) {
            return emptyList()
        }

        val rankedResults =
            searchIndex
                .mapNotNull { entry ->

                    val score =
                        searchScore(
                            entry =
                                entry,
                            query =
                                normalizedQuery
                        )

                    if (
                        score == null
                    ) {

                        null

                    } else {

                        RankedSearchResult(
                            entry =
                                entry,
                            score =
                                score
                        )
                    }
                }

        val comparator =
            Comparator<RankedSearchResult> {
                    first,
                    second ->

                when {

                    /*
                     * Best ranking category first.
                     */
                    first.score !=
                        second.score -> {

                        first.score
                            .compareTo(
                                second.score
                            )
                    }

                    /*
                     * Within the same ranking category,
                     * prefer shorter names.
                     */
                    first.entry
                        .normalizedLabel
                        .length !=
                        second.entry
                            .normalizedLabel
                            .length -> {

                        first.entry
                            .normalizedLabel
                            .length
                            .compareTo(
                                second.entry
                                    .normalizedLabel
                                    .length
                            )
                    }

                    /*
                     * Final deterministic tie breaker.
                     */
                    else -> {

                        collator.compare(
                            first.entry
                                .app
                                .label,
                            second.entry
                                .app
                                .label
                        )
                    }
                }
            }

        return rankedResults
            .sortedWith(
                comparator
            )
            .map { rankedResult ->

                rankedResult
                    .entry
                    .app
            }
    }

    private fun searchScore(
        entry: SearchIndexEntry,
        query: String
    ): Int? {

        val compactQuery =
            query.replace(
                " ",
                ""
            )

        return when {

            /*
             * Exact application name.
             *
             * spotify -> Spotify
             */
            entry.normalizedLabel ==
                query -> 0

            /*
             * Known aliases.
             *
             * wsp -> WhatsApp
             * ds  -> Discord
             * gpt -> ChatGPT
             */
            entry.aliases
                .contains(
                    compactQuery
                ) -> 1

            /*
             * Automatic acronym.
             *
             * gm -> Google Maps
             * gp -> Google Photos
             * sn -> Samsung Notes
             */
            entry.acronym
                .isNotEmpty() &&
                entry.acronym ==
                    compactQuery -> 1

            /*
             * Label starts with query.
             *
             * spo -> Spotify
             */
            entry.normalizedLabel
                .startsWith(
                    query
                ) -> 2

            /*
             * Any individual word starts with query.
             *
             * notes -> Samsung Notes
             */
            entry.normalizedWords
                .any { word ->

                    word.startsWith(
                        query
                    )
                } -> 3

            /*
             * Partial known alias.
             *
             * wp -> WhatsApp because wpp is known.
             *
             * We require at least two characters so a
             * one-character query doesn't become noisy.
             */
            compactQuery.length >= 2 &&
                entry.aliases
                    .any { alias ->

                        alias.startsWith(
                            compactQuery
                        )
                    } -> 4

            /*
             * Partial automatic acronym.
             */
            compactQuery.length >= 2 &&
                entry.acronym
                    .isNotEmpty() &&
                entry.acronym
                    .startsWith(
                        compactQuery
                    ) -> 4

            /*
             * Last fallback:
             *
             * query appears somewhere inside the label.
             */
            entry.normalizedLabel
                .contains(
                    query
                ) -> 5

            else ->
                null
        }
    }

    private data class SearchIndexEntry(
        val app: InstalledApp,
        val normalizedLabel: String,
        val normalizedWords: List<String>,
        val acronym: String,
        val aliases: Set<String>
    )

    private data class RankedSearchResult(
        val entry: SearchIndexEntry,
        val score: Int
    )

    private companion object {

        fun normalizeSearchText(
            value: String
        ): String {

            return Normalizer
                .normalize(
                    value,
                    Normalizer.Form.NFD
                )
                .replace(
                    Regex("\\p{Mn}+"),
                    ""
                )
                .lowercase(
                    Locale.getDefault()
                )
                .trim()
        }

        fun splitSearchWords(
            normalizedLabel: String
        ): List<String> {

            return normalizedLabel
                .split(
                    Regex(
                        "[^\\p{L}\\p{N}]+"
                    )
                )
                .filter {
                    it.isNotBlank()
                }
        }

        fun buildAcronym(
            words: List<String>
        ): String {

            if (
                words.size < 2
            ) {
                return ""
            }

            return words
                .mapNotNull { word ->
                    word.firstOrNull()
                }
                .joinToString(
                    separator = ""
                )
        }

        fun searchAliasesFor(
            normalizedLabel: String
        ): Set<String> {

            return when {

                normalizedLabel
                    .startsWith(
                        "whatsapp"
                    ) -> {

                    setOf(
                        "wsp",
                        "ws",
                        "wa",
                        "wpp"
                    )
                }

                normalizedLabel
                    .startsWith(
                        "discord"
                    ) -> {

                    setOf(
                        "ds",
                        "dc"
                    )
                }

                normalizedLabel
                    .startsWith(
                        "instagram"
                    ) -> {

                    setOf(
                        "ig"
                    )
                }

                normalizedLabel
                    .startsWith(
                        "youtube"
                    ) -> {

                    setOf(
                        "yt"
                    )
                }

                normalizedLabel
                    .startsWith(
                        "telegram"
                    ) -> {

                    setOf(
                        "tg"
                    )
                }

                normalizedLabel
                    .startsWith(
                        "chatgpt"
                    ) -> {

                    setOf(
                        "gpt"
                    )
                }

                else ->
                    emptySet()
            }
        }
    }
}
