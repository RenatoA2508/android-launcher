package com.renato.launcher.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.renato.launcher.core.model.InstalledApp
import com.renato.launcher.ui.icons.PreloadLauncherAppIcons
import com.renato.launcher.ui.icons.rememberLauncherAppIcon
import com.renato.launcher.ui.interactions.launcherAppClickable

@Composable
fun AppDiscoveryScreen(
    apps: List<InstalledApp>,
    onAppClick: (InstalledApp) -> Unit
) {

    PreloadLauncherAppIcons(
        apps = apps
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = apps,
            key = { app ->
		"${app.user.hashCode()}:${app.componentName.flattenToString()}"
	    }
    	) { app ->

            Column(
                modifier =
                    Modifier
                        .launcherAppClickable(
                            onClickLabel =
                                "Abrir ${app.label}",
                            onClick = {
                                onAppClick(
                                    app
                                )
                            }
                        )
                        .padding(
                            4.dp
                        ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val iconBitmap =
                    rememberLauncherAppIcon(
                        app
                    )

                Image(
                    bitmap = iconBitmap,
                    contentDescription = app.label,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = app.label
                )
            }
        }
    }
}
