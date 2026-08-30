package com.renato.launcher.apps

import android.content.Context
import android.content.pm.LauncherApps
import com.renato.launcher.core.model.InstalledApp
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

class AppRepository(
    private val context: Context
) {

    private val launcherApps =
        context.getSystemService(LauncherApps::class.java)

    private val appNameCollator =
        Collator.getInstance(Locale.getDefault()).apply {
            strength = Collator.PRIMARY
        }

    private fun normalizeLabelForSorting(label: String): String {
        return Normalizer
            .normalize(label, Normalizer.Form.NFKC)
            .replace(Regex("\\p{Cf}"), "")
            .trim()
    }

    fun getInstalledApps(): List<InstalledApp> {
        return launcherApps.profiles
            .flatMap { user ->
                launcherApps
                    .getActivityList(null, user)
                    .filter { activityInfo ->
                        activityInfo.applicationInfo.packageName != context.packageName
                    }
                    .map { activityInfo ->
                        InstalledApp(
                            label = activityInfo.label.toString(),
                            packageName = activityInfo.applicationInfo.packageName,
                            componentName = activityInfo.componentName,
                            user = user,
                            icon = activityInfo.getIcon(0)
                        )
                    }
            }
            .sortedWith { first, second ->
                appNameCollator.compare(
                    normalizeLabelForSorting(first.label),
                    normalizeLabelForSorting(second.label)
                )
            }
    }

    fun launch(app: InstalledApp) {
        launcherApps.startMainActivity(
            app.componentName,
            app.user,
            null,
            null
        )
    }
}
