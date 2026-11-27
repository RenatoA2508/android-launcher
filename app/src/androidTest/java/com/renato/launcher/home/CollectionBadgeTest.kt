package com.renato.launcher.home

import android.content.ComponentName
import android.graphics.drawable.ColorDrawable
import android.os.Process
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.renato.launcher.core.model.InstalledApp
import com.renato.launcher.data.database.collection.CollectionEntity
import com.renato.launcher.notifications.NotificationAppKey
import com.renato.launcher.notifications.NotificationBadgeStore
import org.junit.Rule
import org.junit.Test

class CollectionBadgeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun collectionUpdatesWhenOnlyNotificationCountsChange() {
        val user = Process.myUserHandle()
        val apps = listOf("qa.first", "qa.second").map { pkg ->
            InstalledApp(pkg, pkg, ComponentName(pkg, "App"), user, ColorDrawable(0xff336699.toInt()))
        }
        val first = NotificationAppKey(apps[0].packageName, user)
        val second = NotificationAppKey(apps[1].packageName, user)
        val counts = mutableStateOf(emptyMap<NotificationAppKey, Int>())
        val lookup: (InstalledApp) -> Int = { NotificationBadgeStore.countFor(counts.value, it) }
        val collections = listOf(CollectionEntity(1, "Colección de prueba", 0))
        val members = mapOf(1L to apps)
        compose.setContent {
            HomeCollectionsSection(
                collections = collections, collectionAppsById = members, notificationCountFor = lookup,
                onAppClick = {}, onAppInfo = {}, onEditCollection = {}, onDeleteCollection = {},
                onReorderCollections = {}, onReorderCollectionApps = { _, _ -> },
                onRemoveAppFromCollection = { _, _ -> }, onUninstallApp = {}, textColor = Color.White
            )
        }
        compose.onNodeWithText("Colección de prueba", useUnmergedTree = true).assertIsDisplayed()
        compose.runOnIdle { counts.value = mapOf(first to 2, second to 1) }
        compose.onNodeWithText("3", useUnmergedTree = true).assertIsDisplayed()
        compose.runOnIdle { counts.value = mapOf(first to 6, second to 1) }
        compose.onNodeWithText("7", useUnmergedTree = true).assertIsDisplayed()
        compose.runOnIdle { counts.value = mapOf(second to 1) }
        compose.onNodeWithText("1", useUnmergedTree = true).assertIsDisplayed()
        compose.runOnIdle { counts.value = emptyMap() }
        compose.onNodeWithText("1", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("0", useUnmergedTree = true).assertDoesNotExist()
    }
}
