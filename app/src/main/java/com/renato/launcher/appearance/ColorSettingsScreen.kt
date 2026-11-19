package com.renato.launcher.appearance

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.renato.launcher.core.model.InstalledApp
import com.renato.launcher.ui.icons.rememberLauncherAppIcon
import com.renato.launcher.ui.theme.LauncherTheme
import java.util.Locale
import kotlin.math.roundToInt

private val swatches = listOf("F6F3ED", "20242E", "4268DA", "187C68", "BB466C", "FFBF61")
private fun hex(argb: Int) = "%06X".format(Locale.ROOT, argb and 0xFFFFFF)
private fun parseHex(text: String): Int? = text.trim().removePrefix("#")
    .takeIf { it.matches(Regex("[0-9a-fA-F]{6}")) }?.toInt(16)?.or(0xFF000000.toInt())

@Composable
fun ColorSettingsScreen(
    saved: LauncherColors,
    apps: List<InstalledApp>,
    onCancel: () -> Unit,
    onSave: (LauncherColors) -> Unit
) {
    var custom by rememberSaveable { mutableStateOf(saved.custom) }
    var accent by rememberSaveable { mutableStateOf(hex(saved.accent)) }
    var panel by rememberSaveable { mutableStateOf(hex(saved.panel)) }
    var opacity by rememberSaveable { mutableIntStateOf(saved.opacity) }
    var editAccent by rememberSaveable { mutableStateOf(true) }
    val draft = LauncherColors(custom, parseHex(accent) ?: saved.accent, parseHex(panel) ?: saved.panel, opacity)
    val valid = !custom || (parseHex(accent) != null && parseHex(panel) != null)
    BackHandler(onBack = onCancel)

    // The transparent space around the preview is the Activity's live wallpaper window.
    LauncherTheme(colors = draft) {
        val scheme = MaterialTheme.colorScheme
        val action = if (custom) Color(draft.accent) else scheme.primary
        StatusBarOn(scheme.surface)
        Column(Modifier.fillMaxSize().imePadding()) {
            Surface {
                Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text("Apariencia", style = MaterialTheme.typography.labelLarge)
                    Text("Colores", style = MaterialTheme.typography.headlineMedium)
                }
            }
            ColorPreview(apps)
            Surface(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
                    listOf(false to "Seguir los colores del sistema", true to "Personalizar").forEach { (mode, label) ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp)
                            .selectable(custom == mode, role = Role.RadioButton, onClick = { custom = mode }),
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = custom == mode, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(label)
                        }
                    }
                    if (custom) {
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(editAccent, { editAccent = true }, label = { Text("Acento") })
                            FilterChip(!editAccent, { editAccent = false }, label = { Text("Paneles") })
                        }
                        val value = if (editAccent) accent else panel
                        val change: (String) -> Unit = { if (editAccent) accent = it else panel = it }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            swatches.forEach { sample ->
                                val color = Color(parseHex(sample)!!)
                                val selected = parseHex(value) == color.toArgb()
                                Box(Modifier.size(44.dp).clip(CircleShape)
                                    .selectable(selected, role = Role.RadioButton, onClick = { change(sample) })
                                    .semantics { contentDescription = "#$sample" }, contentAlignment = Alignment.Center) {
                                    Box(Modifier.size(32.dp).background(color, CircleShape)
                                        .border(1.dp, scheme.outline, CircleShape), contentAlignment = Alignment.Center) {
                                        if (selected) Text("✓", color = ink(color))
                                    }
                                }
                            }
                        }
                        OutlinedTextField(value, change, Modifier.fillMaxWidth(), singleLine = true,
                            label = { Text(if (editAccent) "HEX del acento" else "HEX de los paneles") },
                            placeholder = { Text("#RRGGBB") }, isError = parseHex(value) == null,
                            supportingText = if (parseHex(value) == null) {
                                { Text("Introduce seis dígitos HEX, por ejemplo #4268DA.") }
                            } else null)
                        Spacer(Modifier.height(16.dp))
                        Text("Opacidad de los paneles · $opacity %", style = MaterialTheme.typography.titleSmall)
                        Slider(opacity.toFloat(), { opacity = it.roundToInt() }, valueRange = 0f..100f,
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Opacidad de los paneles" })
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Más fondo", style = MaterialTheme.typography.labelMedium)
                            Text("Más opaco", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    TextButton(onClick = {
                        val defaults = LauncherColors()
                        custom = false
                        accent = hex(defaults.accent)
                        panel = hex(defaults.panel)
                        opacity = defaults.opacity
                    }) { Text("Restablecer colores del sistema") }
                }
            }
            Surface {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                    TextButton(onClick = onCancel) { Text("Cancelar") }
                    Button(onClick = { onSave(draft) }, enabled = valid,
                        colors = ButtonDefaults.buttonColors(containerColor = action, contentColor = ink(action))) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorPreview(apps: List<InstalledApp>) {
    val shape = RoundedCornerShape(16.dp)
    PanelTheme {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = shape, color = panelColor(.82f), contentColor = MaterialTheme.colorScheme.onSurface) {
                Row(Modifier.fillMaxWidth().padding(8.dp).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp)) { Text("Buscar aplicaciones") }
            }
            Surface(shape = shape, color = panelColor(1f), contentColor = MaterialTheme.colorScheme.onSurface) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Colección de muestra", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    apps.forEach { Image(rememberLauncherAppIcon(it), it.label, Modifier.size(24.dp)) }
                }
            }
        }
    }
}
