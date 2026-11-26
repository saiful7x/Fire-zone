package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameSettings
import com.example.model.HudButtonId
import com.example.model.HudElementLayout
import com.example.model.defaultHudLayout
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BlazingOrange
import com.example.ui.theme.CrimsonStrike
import com.example.ui.theme.EmeraldHeal
import com.example.ui.theme.GlooCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalCard
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun CustomHudEditorScreen(
    settings: GameSettings,
    onSaveLayout: (preset: Int, layout: Map<HudButtonId, HudElementLayout>) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activePreset by remember { mutableIntStateOf(settings.activeHudPreset) }
    var preset1Draft by remember { mutableStateOf(settings.hudLayoutPreset1) }
    var preset2Draft by remember { mutableStateOf(settings.hudLayoutPreset2) }
    var selectedButton by remember { mutableStateOf(HudButtonId.FIRE) }
    var toolbarExpanded by remember { mutableStateOf(true) }

    BackHandler { onClose() }

    val currentLayout = if (activePreset == 2) preset2Draft else preset1Draft
    fun updateCurrentLayout(updated: Map<HudButtonId, HudElementLayout>) {
        if (activePreset == 2) preset2Draft = updated else preset1Draft = updated
    }

    val selectedSpec = currentLayout[selectedButton] ?: HudElementLayout(selectedButton.defaultX, selectedButton.defaultY)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF3E6C9A), Color(0xFF7CA7CE), Color(0xFF4D5E33), Color(0xFF2B351E))
                )
            )
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val heightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

        // Subtle alignment grid + center crosshair preview
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cols = 8
            val rows = 5
            for (i in 1 until cols) {
                val x = size.width * (i / cols.toFloat())
                drawLine(Color.White.copy(alpha = 0.08f), Offset(x, 0f), Offset(x, size.height), 1f)
            }
            for (j in 1 until rows) {
                val y = size.height * (j / rows.toFloat())
                drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, y), Offset(size.width, y), 1f)
            }
            val cx = size.width * 0.5f
            val cy = size.height * 0.48f
            drawCircle(Color.White.copy(alpha = 0.45f), radius = 12f, center = Offset(cx, cy), style = Stroke(2f))
            drawCircle(Color.White.copy(alpha = 0.8f), radius = 2.5f, center = Offset(cx, cy))
        }

        // Draggable HUD Buttons
        HudButtonId.entries.forEach { btnId ->
            val elem = currentLayout[btnId] ?: HudElementLayout(btnId.defaultX, btnId.defaultY, btnId.defaultScale, 0.88f)
            val isSelected = selectedButton == btnId
            val baseDp = when (btnId) {
                HudButtonId.JOYSTICK -> 112f
                HudButtonId.MINIMAP -> 94f
                HudButtonId.WEAPON_BAR -> 118f
                HudButtonId.FIRE -> 76f
                HudButtonId.GLOO_WALL -> 64f
                else -> 54f
            }
            val heightDp = when (btnId) {
                HudButtonId.WEAPON_BAR -> 52f * elem.scale
                else -> baseDp * elem.scale
            }
            val widthDp = baseDp * elem.scale
            val wPx = with(density) { widthDp.dp.toPx() }
            val hPx = with(density) { heightDp.dp.toPx() }

            val offsetX = (elem.xFraction * widthPx - wPx * 0.5f).roundToInt()
            val offsetY = (elem.yFraction * heightPx - hPx * 0.5f).roundToInt()

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX, offsetY) }
                    .size(widthDp.dp, heightDp.dp)
                    .alpha(elem.alpha)
                    .clip(
                        if (btnId == HudButtonId.MINIMAP || btnId == HudButtonId.WEAPON_BAR) {
                            RoundedCornerShape(8.dp)
                        } else CircleShape
                    )
                    .background(
                        when (btnId) {
                            HudButtonId.FIRE, HudButtonId.LEFT_FIRE -> CrimsonStrike.copy(alpha = 0.45f)
                            HudButtonId.GLOO_WALL -> GlooCyan.copy(alpha = 0.30f)
                            else -> ObsidianBg.copy(alpha = 0.72f)
                        }
                    )
                    .border(
                        width = if (isSelected) 3.dp else 1.5.dp,
                        color = if (isSelected) AmberGold else Color.White.copy(alpha = 0.7f),
                        shape = if (btnId == HudButtonId.MINIMAP || btnId == HudButtonId.WEAPON_BAR) RoundedCornerShape(8.dp) else CircleShape
                    )
                    .pointerInput(btnId, activePreset) {
                        detectDragGestures(
                            onDragStart = { selectedButton = btnId },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                selectedButton = btnId
                                val curMap = if (activePreset == 2) preset2Draft else preset1Draft
                                val cur = curMap[btnId] ?: elem
                                val nx = (cur.xFraction + dragAmount.x / widthPx).coerceIn(0.04f, 0.96f)
                                val ny = (cur.yFraction + dragAmount.y / heightPx).coerceIn(0.07f, 0.93f)
                                val updated = curMap.toMutableMap()
                                updated[btnId] = cur.copy(xFraction = nx, yFraction = ny)
                                updateCurrentLayout(updated)
                            }
                        )
                    }
                    .clickable { selectedButton = btnId }
                    .testTag("hud_editor_item_${btnId.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                EditorButtonContent(btnId)
            }
        }

        // Top-Center Floating Custom HUD Inspector Panel (collapsible!)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp)
                .width(440.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(TacticalCard.copy(alpha = 0.92f))
                .border(1.5.dp, AmberGold.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("CUSTOM HUD • DRAG ANY BUTTON", style = MaterialTheme.typography.labelLarge, color = AmberGold, fontWeight = FontWeight.Bold)
                    Text("Selected: ${selectedButton.label.uppercase()}", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { toolbarExpanded = !toolbarExpanded }, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = if (toolbarExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "Toggle Toolbar",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = TextSecondary)
                    }
                }
            }

            if (toolbarExpanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Size ${(selectedSpec.scale * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = GlooCyan, modifier = Modifier.width(66.dp))
                    Slider(
                        value = selectedSpec.scale,
                        onValueChange = { v ->
                            val updated = currentLayout.toMutableMap()
                            updated[selectedButton] = selectedSpec.copy(scale = v)
                            updateCurrentLayout(updated)
                        },
                        valueRange = 0.55f..1.55f,
                        modifier = Modifier.weight(1f).height(26.dp),
                        colors = SliderDefaults.colors(thumbColor = AmberGold, activeTrackColor = AmberGold)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Alpha ${(selectedSpec.alpha * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = EmeraldHeal, modifier = Modifier.width(66.dp))
                    Slider(
                        value = selectedSpec.alpha,
                        onValueChange = { v ->
                            val updated = currentLayout.toMutableMap()
                            updated[selectedButton] = selectedSpec.copy(alpha = v)
                            updateCurrentLayout(updated)
                        },
                        valueRange = 0.25f..1.0f,
                        modifier = Modifier.weight(1f).height(26.dp),
                        colors = SliderDefaults.colors(thumbColor = GlooCyan, activeTrackColor = GlooCyan)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 2).forEach { p ->
                            val sel = activePreset == p
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (sel) AmberGold else ObsidianBg)
                                    .border(1.dp, if (sel) AmberGold else TacticalBorder, RoundedCornerShape(6.dp))
                                    .clickable { activePreset = p }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("PRESET $p", style = MaterialTheme.typography.labelSmall, color = if (sel) ObsidianBg else Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { updateCurrentLayout(defaultHudLayout()) },
                            colors = ButtonDefaults.buttonColors(containerColor = ObsidianBg),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = "Reset", tint = BlazingOrange, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("RESET", style = MaterialTheme.typography.labelSmall, color = BlazingOrange)
                        }
                        Button(
                            onClick = {
                                onSaveLayout(activePreset, currentLayout)
                                onClose()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianBg),
                            modifier = Modifier.height(34.dp).testTag("hud_editor_save_button")
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = "Save HUD", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("SAVE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorButtonContent(id: HudButtonId) {
    val icon = when (id) {
        HudButtonId.GLOO_WALL -> Icons.Filled.Shield
        HudButtonId.SCOPE -> Icons.Filled.CenterFocusStrong
        HudButtonId.JUMP -> Icons.Filled.ArrowUpward
        HudButtonId.CROUCH -> Icons.Filled.KeyboardArrowDown
        HudButtonId.SPRINT -> Icons.Filled.DirectionsRun
        HudButtonId.MEDKIT -> Icons.Filled.LocalHospital
        HudButtonId.RELOAD -> Icons.Filled.Refresh
        HudButtonId.SKILL -> Icons.Filled.Bolt
        else -> null
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon != null) {
            Icon(icon, contentDescription = id.label, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Text(
            text = id.label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}
