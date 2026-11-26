package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AimPrecision
import com.example.model.AppDataState
import com.example.model.GameSettings
import com.example.model.GraphicsQuality
import com.example.model.LeftFireMode
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BlazingOrange
import com.example.ui.theme.CrimsonStrike
import com.example.ui.theme.EmeraldHeal
import com.example.ui.theme.GlooCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalCard
import com.example.ui.theme.TacticalCardElevated
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

private enum class SettingsTab(val label: String) {
    SENSITIVITY("Sensitivity"),
    CONTROLS("Controls & HUD"),
    DISPLAY("Display"),
    BASIC("Basic & Sound")
}

@Composable
fun SettingsScreen(
    appData: AppDataState,
    onUpdateSettings: ((GameSettings) -> GameSettings) -> Unit,
    onResetSensitivity: () -> Unit,
    onUpdateCallsign: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = appData.settings
    var activeTab by remember { mutableStateOf(SettingsTab.SENSITIVITY) }
    var callsignDraft by remember(appData.profile.callsign) { mutableStateOf(appData.profile.callsign) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "SYSTEM SETTINGS",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )
            }

            // Tab Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingsTab.entries.forEach { tab ->
                        val selected = tab == activeTab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) AmberGold else TacticalCard)
                                .border(1.dp, if (selected) AmberGold else TacticalBorder, RoundedCornerShape(10.dp))
                                .clickable { activeTab = tab }
                                .testTag("settings_tab_${tab.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) ObsidianBg else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            when (activeTab) {
                SettingsTab.SENSITIVITY -> {
                    item {
                        SettingsSectionCard(title = "SENSITIVITY CALIBRATION (0 - 100)") {
                            SensitivitySliderRow("General", settings.generalSens) { v ->
                                onUpdateSettings { it.copy(generalSens = v) }
                            }
                            SensitivitySliderRow("Red Dot", settings.redDotSens) { v ->
                                onUpdateSettings { it.copy(redDotSens = v) }
                            }
                            SensitivitySliderRow("2x Scope", settings.scope2xSens) { v ->
                                onUpdateSettings { it.copy(scope2xSens = v) }
                            }
                            SensitivitySliderRow("4x Scope", settings.scope4xSens) { v ->
                                onUpdateSettings { it.copy(scope4xSens = v) }
                            }
                            SensitivitySliderRow("Sniper Scope", settings.sniperSens) { v ->
                                onUpdateSettings { it.copy(sniperSens = v) }
                            }
                            SensitivitySliderRow("Free Look", settings.freeLookSens) { v ->
                                onUpdateSettings { it.copy(freeLookSens = v) }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = onResetSensitivity,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("reset_sensitivity_button")
                            ) {
                                Text("RESET TO DEFAULT", color = AmberGold, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                SettingsTab.CONTROLS -> {
                    item {
                        SettingsSectionCard(title = "AIM PRECISION") {
                            AimPrecision.entries.forEach { mode ->
                                val selected = settings.aimPrecision == mode
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) AmberGold.copy(alpha = 0.16f) else ObsidianBg)
                                        .border(1.dp, if (selected) AmberGold else TacticalBorder, RoundedCornerShape(8.dp))
                                        .clickable { onUpdateSettings { it.copy(aimPrecision = mode) } }
                                        .padding(12.dp)
                                        .testTag("aim_precision_${mode.name.lowercase()}"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(mode.label, style = MaterialTheme.typography.titleMedium, color = Color.White)
                                        Text(mode.desc, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    }
                                    if (selected) {
                                        Text("ACTIVE", style = MaterialTheme.typography.labelSmall, color = AmberGold)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        SettingsSectionCard(title = "COMBAT CONTROLS") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Left Fire Button", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LeftFireMode.entries.forEach { mode ->
                                        val selected = settings.leftFireMode == mode
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (selected) GlooCyan else ObsidianBg)
                                                .border(1.dp, if (selected) GlooCyan else TacticalBorder, RoundedCornerShape(8.dp))
                                                .clickable { onUpdateSettings { it.copy(leftFireMode = mode) } }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = mode.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (selected) ObsidianBg else Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            ToggleRow("Quick Weapon Switch", "Fast tap switch slot on HUD", settings.quickWeaponSwitch) { checked ->
                                onUpdateSettings { it.copy(quickWeaponSwitch = checked) }
                            }
                            ToggleRow("Quick Reload", "Auto-reload when magazine hits 0", settings.quickReload) { checked ->
                                onUpdateSettings { it.copy(quickReload = checked) }
                            }
                            ToggleRow("Smart Gloo Wall", "Instant 1-tap Gloo Wall deploy in front of aim", settings.smartGlooWall) { checked ->
                                onUpdateSettings { it.copy(smartGlooWall = checked) }
                            }
                        }
                    }

                    item {
                        SettingsSectionCard(title = "CUSTOM HUD BUTTON TUNER") {
                            FloatSliderRow(
                                label = "Fire Button Size",
                                value = settings.fireButtonScale,
                                range = 0.75f..1.35f,
                                display = "${(settings.fireButtonScale * 100).roundToInt()}%"
                            ) { v -> onUpdateSettings { it.copy(fireButtonScale = v) } }

                            FloatSliderRow(
                                label = "Gloo Wall Button Size",
                                value = settings.glooButtonScale,
                                range = 0.75f..1.35f,
                                display = "${(settings.glooButtonScale * 100).roundToInt()}%"
                            ) { v -> onUpdateSettings { it.copy(glooButtonScale = v) } }

                            FloatSliderRow(
                                label = "HUD Opacity",
                                value = settings.hudOpacity,
                                range = 0.4f..1.0f,
                                display = "${(settings.hudOpacity * 100).roundToInt()}%"
                            ) { v -> onUpdateSettings { it.copy(hudOpacity = v) } }

                            // Live HUD Button Preview
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ObsidianBg)
                                    .border(1.dp, TacticalBorder, RoundedCornerShape(10.dp))
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size((62f * settings.glooButtonScale).dp)
                                        .alpha(settings.hudOpacity)
                                        .clip(CircleShape)
                                        .background(GlooCyan.copy(alpha = 0.25f))
                                        .border(2.dp, GlooCyan, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("GLOO", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .size((72f * settings.fireButtonScale).dp)
                                        .alpha(settings.hudOpacity)
                                        .clip(CircleShape)
                                        .background(CrimsonStrike.copy(alpha = 0.35f))
                                        .border(2.dp, CrimsonStrike, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("FIRE", style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                SettingsTab.DISPLAY -> {
                    item {
                        SettingsSectionCard(title = "GRAPHICS QUALITY") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GraphicsQuality.entries.forEach { quality ->
                                    val selected = settings.graphicsQuality == quality
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) AmberGold else ObsidianBg)
                                            .border(1.dp, if (selected) AmberGold else TacticalBorder, RoundedCornerShape(8.dp))
                                            .clickable { onUpdateSettings { it.copy(graphicsQuality = quality) } },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = quality.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (selected) ObsidianBg else Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            ToggleRow("High FPS (60 FPS)", "Ultra-smooth combat rendering", settings.highFps) { checked ->
                                onUpdateSettings { it.copy(highFps = checked) }
                            }
                            ToggleRow("New Damage Indicator", "Floating red headshot & critical popups", settings.newDamageIndicator) { checked ->
                                onUpdateSettings { it.copy(newDamageIndicator = checked) }
                            }
                        }
                    }
                }

                SettingsTab.BASIC -> {
                    item {
                        SettingsSectionCard(title = "PLAYER PROFILE") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = callsignDraft,
                                    onValueChange = { callsignDraft = it.take(16) },
                                    label = { Text("Operative Callsign") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("callsign_input")
                                )
                                Button(
                                    onClick = { onUpdateCallsign(callsignDraft) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BlazingOrange),
                                    modifier = Modifier
                                        .height(52.dp)
                                        .testTag("save_callsign_button")
                                ) {
                                    Text("SAVE", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    item {
                        SettingsSectionCard(title = "AUDIO & SOUND EFFECTS") {
                            SensitivitySliderRow("Master Volume", settings.masterVolume) { v ->
                                onUpdateSettings { it.copy(masterVolume = v) }
                            }
                            SensitivitySliderRow("Combat SFX", settings.sfxVolume) { v ->
                                onUpdateSettings { it.copy(sfxVolume = v) }
                            }
                            ToggleRow("UI Click Audio", "Tactical feedback sounds on menus", settings.uiAudioEnabled) { checked ->
                                onUpdateSettings { it.copy(uiAudioEnabled = checked) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TacticalCard)
            .border(1.dp, TacticalBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = AmberGold
        )
        content()
    }
}

@Composable
private fun SensitivitySliderRow(
    label: String,
    value: Int,
    onChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = Color.White)
            Text(
                text = "$value",
                style = MaterialTheme.typography.titleMedium,
                color = GlooCyan,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = AmberGold,
                activeTrackColor = AmberGold,
                inactiveTrackColor = TacticalCardElevated
            )
        )
    }
}

@Composable
private fun FloatSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: String,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = Color.White)
            Text(display, style = MaterialTheme.typography.titleMedium, color = AmberGold, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = GlooCyan,
                activeTrackColor = GlooCyan,
                inactiveTrackColor = TacticalCardElevated
            )
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianBg,
                checkedTrackColor = EmeraldHeal
            )
        )
    }
}
