package com.renato.launcher.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class LauncherNotificationListenerService :
    NotificationListenerService() {

    /*
     * Notifications currently known to Android.
     *
     * The badge is deliberately NOT an independent
     * unread-message counter.
     *
     * It mirrors Android's active notification state,
     * which is how notification badging is designed
     * to work.
     */
    private val activeNotificationsByKey =
        mutableMapOf<
            String,
            StatusBarNotification
        >()

    /*
     * ------------------------------------------------------------
     * LISTENER CONNECTED
     * ------------------------------------------------------------
     *
     * Rebuild the complete state from Android.
     *
     * This is important after:
     *
     * - launcher process recreation
     * - notification access being enabled
     * - service reconnection
     */
    override fun onListenerConnected() {
        super.onListenerConnected()

        activeNotificationsByKey.clear()

        runCatching {
            activeNotifications
                ?.forEach {
                        notification ->

                    activeNotificationsByKey[
                        notification.key
                    ] =
                        notification
                }
        }

        publishBadgeCounts()
    }

    /*
     * ------------------------------------------------------------
     * NEW / UPDATED NOTIFICATION
     * ------------------------------------------------------------
     *
     * Android can post the same notification key again
     * when its contents or its notification.number changes.
     *
     * Replacing by key therefore keeps our state synchronized
     * with the current version of the notification.
     */
    override fun onNotificationPosted(
        sbn: StatusBarNotification
    ) {
        activeNotificationsByKey[
            sbn.key
        ] =
            sbn

        publishBadgeCounts()
    }

    /*
     * ------------------------------------------------------------
     * REMOVED NOTIFICATION
     * ------------------------------------------------------------
     *
     * Clearing a notification from the panel removes it
     * from the badge calculation as well.
     *
     * We deliberately do not preserve an independent count.
     */
    override fun onNotificationRemoved(
        sbn: StatusBarNotification
    ) {
        activeNotificationsByKey
            .remove(
                sbn.key
            )

        publishBadgeCounts()
    }

    /*
     * ------------------------------------------------------------
     * RANKING / CHANNEL CHANGES
     * ------------------------------------------------------------
     *
     * Android's notification ranking contains information such
     * as whether a particular notification is allowed to create
     * an app-icon badge.
     *
     * Recalculate when that information changes.
     */
    override fun onNotificationRankingUpdate(
        rankingMap: RankingMap
    ) {
        super.onNotificationRankingUpdate(
            rankingMap
        )

        publishBadgeCounts()
    }

    /*
     * ------------------------------------------------------------
     * DISCONNECTED
     * ------------------------------------------------------------
     *
     * Without notification-listener access we cannot claim that
     * a badge count is current, so clear the visible state.
     */
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()

        activeNotificationsByKey.clear()

        NotificationBadgeStore.clear()
    }

    /*
     * ------------------------------------------------------------
     * BADGE CALCULATION
     * ------------------------------------------------------------
     *
     * Follows Android launcher badging semantics:
     *
     * notification.number > 0
     *      → notification contributes that number.
     *
     * notification.number <= 0
     *      → notification contributes 1.
     *
     * Example:
     *
     * Notification A: number = 0  → 1
     * Notification B: number = 3  → 3
     *
     * Badge total = 4
     */
    private fun publishBadgeCounts() {
        val rankingMap =
            currentRanking

        val counts =
            mutableMapOf<
                NotificationAppKey,
                Int
            >()

        activeNotificationsByKey
            .values
            .asSequence()
            .filter {
                    sbn ->

                notificationIsValidForBadge(
                    sbn =
                        sbn,
                    rankingMap =
                        rankingMap
                )
            }
            .forEach {
                    sbn ->

                val appKey =
                    NotificationAppKey(
                        packageName =
                            sbn.packageName,
                        user =
                            sbn.user
                    )

                /*
                 * Android's launcher reference implementation
                 * treats zero as one notification and honors
                 * Notification.number when the app supplies
                 * a custom count.
                 */
                val notificationCount =
                    sbn.notification
                        .number
                        .coerceAtLeast(
                            1
                        )

                counts[
                    appKey
                ] =
                    (counts[appKey] ?: 0) +
                        notificationCount
            }

        NotificationBadgeStore
            .replaceCounts(
                counts
            )
    }

    /*
     * ------------------------------------------------------------
     * NOTIFICATIONS THAT SHOULD CREATE A BADGE
     * ------------------------------------------------------------
     *
     * Mirrors the documented Android launcher rules rather than
     * simply counting every StatusBarNotification object.
     */
    private fun notificationIsValidForBadge(
        sbn: StatusBarNotification,
        rankingMap: RankingMap
    ): Boolean {
        val notification =
            sbn.notification

        /*
         * Android exposes whether this notification/channel
         * is permitted to contribute to launcher badging.
         */
        val ranking =
            Ranking()

        val rankingAvailable =
            rankingMap.getRanking(
                sbn.key,
                ranking
            )

        if (
            !rankingAvailable
        ) {
            return false
        }

        if (
            !ranking.canShowBadge()
        ) {
            return false
        }

        /*
         * Launcher3 excludes ongoing notifications from the
         * legacy/default notification channel.
         */
        val channel =
            ranking.channel

        if (
            channel != null &&
            channel.id ==
                NotificationChannel.DEFAULT_CHANNEL_ID &&
            (
                notification.flags and
                    Notification.FLAG_ONGOING_EVENT
                ) != 0
        ) {
            return false
        }

        /*
         * A group summary is infrastructure used to represent
         * a group of child notifications.
         *
         * Counting both the summary and its children would
         * artificially inflate the launcher badge.
         */
        val isGroupSummary =
            (
                notification.flags and
                    Notification.FLAG_GROUP_SUMMARY
                ) != 0

        if (
            isGroupSummary
        ) {
            return false
        }

        /*
         * Android Launcher3 also filters notifications that
         * contain neither a title nor text.
         */
        val title =
            notification.extras
                .getCharSequence(
                    Notification.EXTRA_TITLE
                )

        val text =
            notification.extras
                .getCharSequence(
                    Notification.EXTRA_TEXT
                )

        val missingTitleAndText =
            title.isNullOrBlank() &&
                text.isNullOrBlank()

        if (
            missingTitleAndText
        ) {
            return false
        }

        return true
    }
}
