package com.renato.launcher

import android.app.role.RoleManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.renato.launcher.ui.theme.LauncherTheme
import androidx.compose.runtime.LaunchedEffect
import com.renato.launcher.apps.AppDiscoveryScreen
import com.renato.launcher.apps.AppRepository
import com.renato.launcher.core.model.InstalledApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LauncherTheme {
                val roleManager = getSystemService(RoleManager::class.java)

                var isHomeApp by remember {
                    mutableStateOf(
                        roleManager.isRoleHeld(RoleManager.ROLE_HOME)
                    )
                }

                var installedApps by remember {
                    mutableStateOf(emptyList<InstalledApp>())
                }

                val appRepository = remember {
                    AppRepository(applicationContext)
                }

                val homeRoleLauncher =
                    rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) {
                        isHomeApp =
                            roleManager.isRoleHeld(RoleManager.ROLE_HOME)
                    }

                LaunchedEffect(isHomeApp) {
                    if (isHomeApp) {
                        installedApps = appRepository.getInstalledApps()
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isHomeApp) {
                            "Launcher is the default Home app"
                        } else {
                            "Launcher is not the default Home app"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (!isHomeApp) {
                        Button(
                            onClick = {
                                val intent =
                                    roleManager.createRequestRoleIntent(
                                        RoleManager.ROLE_HOME
                                    )

                                homeRoleLauncher.launch(intent)
                            }
                        ) {
                        }
                    }else{
                        AppDiscoveryScreen(
                            apps = installedApps,
                            onAppClick = appRepository::launch
                        )
                    }
                }
            }
        }
    }
}
