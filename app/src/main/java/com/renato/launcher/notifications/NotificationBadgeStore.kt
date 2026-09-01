package com.renato.launcher.notifications

import android.os.UserHandle
import com.renato.launcher.core.model.InstalledApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NotificationAppKey(
    val packageName: String,
    val user: UserHandle
)

object NotificationBadgeStore {

    private val mutableCounts =
        MutableStateFlow<
            Map<NotificationAppKey, Int>
        >(
            emptyMap()
        )

    val counts: StateFlow<
        Map<NotificationAppKey, Int>
    > =
        mutableCounts.asStateFlow()

    fun countFor(
        counts: Map<NotificationAppKey, Int>,
        app: InstalledApp
    ): Int {
        return counts[
            NotificationAppKey(
                packageName =
                    app.packageName,
                user =
                    app.user
            )
        ] ?: 0
    }

    internal fun replaceCounts(
        counts: Map<NotificationAppKey, Int>
    ) {
        mutableCounts.value =
            counts
    }

    internal fun clear() {
        mutableCounts.value =
            emptyMap()
    }
}
