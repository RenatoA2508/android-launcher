package com.renato.launcher.core.model

import android.content.ComponentName
import android.graphics.drawable.Drawable
import android.os.UserHandle

data class InstalledApp(
    val label: String,
    val packageName: String,
    val componentName: ComponentName,
    val user: UserHandle,
    val icon: Drawable
)
