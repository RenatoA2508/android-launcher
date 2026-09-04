package com.renato.launcher.allapps

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renato.launcher.core.model.InstalledApp
import com.renato.launcher.notifications.NotificationBadgeStore
import com.renato.launcher.ui.components.AppContextMenu
import com.renato.launcher.ui.components.LauncherAppCatalog
import com.renato.launcher.ui.components.LauncherAppCatalogItem
import com.renato.launcher.ui.components.LauncherTextActionButton

@Composable
fun AllAppsScreen(
    apps: List<InstalledApp>,
    appsLoaded: Boolean,
    onAppClick: (InstalledApp) -> Unit,
    onAppInfo: (InstalledApp) -> Unit,
    onUninstallApp: (InstalledApp) -> Unit,
    onBack: () -> Unit
) {
    AllAppsWindowEffect()

    val notificationCounts by
        NotificationBadgeStore
            .counts
            .collectAsState()

    BackHandler {
        onBack()
    }

    Surface(
        modifier =
            Modifier
                .fillMaxSize()
                .semantics {
                    testTagsAsResourceId = true
                }
                .testTag(ALL_APPS_ROOT_TAG),
        color =
            MaterialTheme
                .colorScheme
                .surface
                .copy(alpha = 0.82f)
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
        ) {
            AllAppsHeader(
                onBack = onBack
            )

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
            ) {
                LauncherAppCatalog(
                    apps = apps,
                    appsLoaded = appsLoaded
                ) { app ->
                    AllAppsAppItem(
                        app = app,
                        notificationCount =
                            NotificationBadgeStore.countFor(
                                counts = notificationCounts,
                                app = app
                            ),
                        onClick = {
                            onAppClick(app)
                        },
                        onAppInfo = {
                            onAppInfo(app)
                        },
                        onUninstallApp = {
                            onUninstallApp(app)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AllAppsHeader(
    onBack: () -> Unit
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = 8.dp,
                    end = 18.dp,
                    top = 8.dp,
                    bottom = 8.dp
                ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LauncherTextActionButton(
            text = "‹",
            fontSize = 32.sp,
            onClick = onBack
        )

        Spacer(
            modifier = Modifier.width(4.dp)
        )

        Text(
            text = "Todas las aplicaciones",
            modifier = Modifier.weight(1f),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AllAppsAppItem(
    app: InstalledApp,
    notificationCount: Int,
    onClick: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstallApp: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current

    var menuExpanded by
        remember(
            app.componentName,
            app.user
        ) {
            mutableStateOf(false)
        }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        LauncherAppCatalogItem(
            app = app,
            notificationCount = notificationCount,
            onClickLabel = "Abrir ${app.label}",
            onLongClickLabel = "Opciones de ${app.label}",
            onLongClick = {
                hapticFeedback.performHapticFeedback(
                    HapticFeedbackType.LongPress
                )
                menuExpanded = true
            },
            onClick = onClick
        )

        AppContextMenu(
            expanded = menuExpanded,
            app = app,
            showRemoveFromHome = false,
            onDismiss = {
                menuExpanded = false
            },
            onAppInfo = {
                menuExpanded = false
                onAppInfo()
            },
            onRemoveFavorite = {
                /* Hidden in the alphabetical catalog. */
            },
            onUninstallApp = {
                menuExpanded = false
                onUninstallApp()
            }
        )
    }
}

@Composable
private fun AllAppsWindowEffect() {
    val context = LocalContext.current

    val activity =
        remember(context) {
            context.findActivity()
        }

    DisposableEffect(activity) {
        val window = activity?.window

        if (
            window != null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        ) {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            )

            val attributes = window.attributes
            attributes.setBlurBehindRadius(24)
            window.attributes = attributes
        }

        onDispose {
            if (
                window != null &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ) {
                val attributes = window.attributes
                attributes.setBlurBehindRadius(0)
                window.attributes = attributes

                window.clearFlags(
                    WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                )
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var currentContext = this

    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }

        currentContext = currentContext.baseContext
    }

    return null
}

private const val ALL_APPS_ROOT_TAG =
    "launcher_all_apps_root"
