package com.example.ui.match

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Paragliding
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ArenaState
import com.example.engine.CsShopItem
import com.example.engine.DropState
import com.example.engine.MatchPhase
import com.example.engine.MatchSummary
import com.example.filament.FilamentArenaViewport
import com.example.model.GameMode
import com.example.model.GameSettings
import com.example.model.LeftFireMode
import com.example.model.WeaponId
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ArmorBlue
import com.example.ui.theme.BlazingOrange
import com.example.ui.theme.CrimsonStrike
import com.example.ui.theme.EmeraldHeal
import com.example.ui.theme.GlooCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalCard
import com.example.ui.theme.TacticalCardElevated
import com.example.ui.theme.TextSecondary
import kotlin.math.hypot

@Composable
fun MatchScreen(
    arenaState: ArenaState,
    settings: GameSettings,
    onMoveVector: (Float, Float) -> Unit,
    onSetFiring: (Boolean) -> Unit,
    onRotateAim: (Float) -> Unit,
    onEjectFromPlane: () -> Unit,
    onOpenParachute: () -> Unit,
    onDeployGlooWall: () -> Unit,
    onActivateSkill: () -> Unit,
    onToggleScope: () -> Unit,
    onSwitchWeapon: () -> Unit,
    onReloadWeapon: () -> Unit,
    onUseMedkit: () -> Unit,
    onPickupLoot: () -> Unit,
    onBuyCsItem: (CsShopItem) -> Unit,
    onStartCombatFromShop: () -> Unit,
    onSelectLoneWolfLoadout: (Int) -> Unit,
    onPauseChanged: (Boolean) -> Unit,
    onPlayAgain: () -> Unit,
    onExitMatch: () -> Unit
) {
    var showPauseDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (arenaState.phase != MatchPhase.MATCH_OVER) {
            showPauseDialog = !showPauseDialog
            onPauseChanged(showPauseDialog)
        } else {
            onExitMatch()
        }
    }

    val player = arenaState.player

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // 1. Live 3D Filament Battleground (Bermuda Island / CS Arena / Iron Cage + Plane + Characters + 3D Gloo Walls)
        FilamentArenaViewport(
            arenaState = arenaState,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Right-half Drag-to-Aim Touch Surface
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxWidth(0.55f)
                .fillMaxHeight()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onRotateAim(dragAmount.x * 0.0085f)
                    }
                }
        )

        // 3. Center TPS Crosshair + Hitmarker when Grounded
        if (player != null && player.dropState == DropState.GROUNDED && arenaState.phase == MatchPhase.ACTIVE_COMBAT) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width * 0.5f
                val cy = size.height * 0.48f
                val gap = if (player.isScoped) 6f else 11f
                val arm = if (player.isScoped) 14f else 10f
                val cColor = if (player.isScoped) CrimsonStrike else Color.White.copy(alpha = 0.85f)
                drawLine(cColor, Offset(cx - gap - arm, cy), Offset(cx - gap, cy), strokeWidth = 2.5f)
                drawLine(cColor, Offset(cx + gap, cy), Offset(cx + gap + arm, cy), strokeWidth = 2.5f)
                drawLine(cColor, Offset(cx, cy - gap - arm), Offset(cx, cy - gap), strokeWidth = 2.5f)
                drawLine(cColor, Offset(cx, cy + gap), Offset(cx, cy + gap + arm), strokeWidth = 2.5f)
                drawCircle(cColor, radius = 2.5f, center = Offset(cx, cy))

                if (arenaState.hitmarkerTimerSec > 0f) {
                    val hmColor = if (arenaState.hitmarkerIsHeadshot) CrimsonStrike else AmberGold
                    val d1 = 9f
                    val d2 = 22f
                    drawLine(hmColor, Offset(cx - d2, cy - d2), Offset(cx - d1, cy - d1), strokeWidth = 3.5f)
                    drawLine(hmColor, Offset(cx + d1, cy - d1), Offset(cx + d2, cy - d2), strokeWidth = 3.5f)
                    drawLine(hmColor, Offset(cx - d2, cy + d2), Offset(cx - d1, cy + d1), strokeWidth = 3.5f)
                    drawLine(hmColor, Offset(cx + d1, cy + d1), Offset(cx + d2, cy + d2), strokeWidth = 3.5f)
                }
            }
        }

        // 4. Top Combat HUD (Minimap with Flight Path, Scoreboard, Kill Feed, Pause)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TacticalMinimapWithFlight(state = arenaState)
                Text(
                    text = "ZONE ${arenaState.safeZone.countdownSec.toInt()}s • PH.${arenaState.safeZone.shrinkPhase}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GlooCyan,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ObsidianBg.copy(alpha = 0.8f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ScoreboardBanner(state = arenaState)
                arenaState.killFeed.take(3).forEach { item ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ObsidianBg.copy(alpha = 0.78f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = item.killerName,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.killerIsAlly) AmberGold else CrimsonStrike
                        )
                        Text(
                            text = if (item.isHeadshot) "[HS]" else "->",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.isHeadshot) CrimsonStrike else TextSecondary
                        )
                        Text(
                            text = item.victimName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "KILLS ${player?.kills ?: 0}",
                    style = MaterialTheme.typography.labelLarge,
                    color = AmberGold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianBg.copy(alpha = 0.82f))
                        .border(1.dp, AmberGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
                IconButton(
                    onClick = {
                        showPauseDialog = true
                        onPauseChanged(true)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianBg.copy(alpha = 0.82f))
                        .border(1.dp, TacticalBorder, RoundedCornerShape(10.dp))
                        .testTag("pause_match_button")
                ) {
                    Icon(Icons.Filled.Pause, contentDescription = "Pause Match", tint = Color.White)
                }
            }
        }

        // Round / Drop Toast
        arenaState.bannerMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 66.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BlazingOrange.copy(alpha = 0.92f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 5. FLIGHT & PARACHUTE HUD OVERLAY (Battle Royale Drop!)
        if (player != null && arenaState.phase == MatchPhase.ACTIVE_COMBAT && player.dropState != DropState.GROUNDED) {
            if (player.dropState == DropState.IN_PLANE) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianBg.copy(alpha = 0.84f))
                            .border(1.dp, AmberGold.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Filled.FlightTakeoff, contentDescription = "Flight", tint = AmberGold)
                        Text(
                            text = "C-130 FLIGHT OVER BERMUDA • ALT ${arenaState.plane.altitudeMeters.toInt()}m",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = onEjectFromPlane,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianBg),
                        modifier = Modifier
                            .height(58.dp)
                            .width(200.dp)
                            .testTag("eject_plane_button")
                    ) {
                        Text("EJECT / JUMP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // SKYDIVING or PARACHUTE
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .navigationBarsPadding()
                        .padding(start = 20.dp, bottom = 20.dp)
                ) {
                    VirtualJoystick(onMove = onMoveVector, modifier = Modifier.testTag("parachute_joystick"))
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 24.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ALTITUDE: ${player.altitudeMeters.toInt()}m • ${if (player.dropState == DropState.PARACHUTE) "PARACHUTE OPEN" else "SKYDIVING"}",
                        style = MaterialTheme.typography.titleMedium,
                        color = GlooCyan,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianBg.copy(alpha = 0.85f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                    if (player.dropState == DropState.SKYDIVING) {
                        Button(
                            onClick = onOpenParachute,
                            colors = ButtonDefaults.buttonColors(containerColor = BlazingOrange),
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("open_parachute_button")
                        ) {
                            Icon(Icons.Filled.Paragliding, contentDescription = "Open Parachute", tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("OPEN PARACHUTE", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 6. GROUNDED 3D COMBAT HUD (Bottom-Center HP/Weapon, Left Joystick, Right Fire/Gloo/Skill/Scope)
        if (player != null && arenaState.phase == MatchPhase.ACTIVE_COMBAT && player.dropState == DropState.GROUNDED) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 10.dp)
                    .widthIn(max = 310.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                arenaState.nearbyLoot?.let { loot ->
                    Button(
                        onClick = onPickupLoot,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianBg),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("pickup_loot_button")
                    ) {
                        Text("PICK UP ${loot.label.uppercase()}", fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianBg.copy(alpha = 0.84f))
                        .border(1.dp, TacticalBorder, RoundedCornerShape(8.dp))
                        .clickable(onClick = onSwitchWeapon)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                        .testTag("weapon_switch_bar"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column {
                        Text(player.activeWeapon.displayName.uppercase(), style = MaterialTheme.typography.labelLarge, color = AmberGold)
                        Text(
                            text = if (player.reloadSecLeft > 0f) "RELOADING..." else "${player.activeAmmoInMag} / ${player.reserveAmmo}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                    if (player.weapon2 != null && settings.quickWeaponSwitch) {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = "Switch Weapon", tint = GlooCyan, modifier = Modifier.size(20.dp))
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianBg.copy(alpha = 0.84f))
                        .border(1.dp, TacticalBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("HP ${player.hp.toInt()}/${player.maxHp.toInt()}", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("EP ${player.ep.toInt()} • ARMOR ${player.armor.toInt()}", style = MaterialTheme.typography.labelSmall, color = ArmorBlue)
                    }
                    LinearProgressIndicator(
                        progress = { (player.hp / player.maxHp).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (player.hp < 65f) CrimsonStrike else EmeraldHeal,
                        trackColor = TacticalCard
                    )
                }
            }

            // Left Thumb Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 18.dp, bottom = 18.dp)
                    .alpha(settings.hudOpacity),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HudActionCircle(
                        label = "MED",
                        badge = "${player.medkits}",
                        accent = EmeraldHeal,
                        sizeDp = 52,
                        onClick = onUseMedkit,
                        testTag = "medkit_button"
                    ) {
                        Icon(Icons.Filled.LocalHospital, contentDescription = "Use Medkit", tint = EmeraldHeal, modifier = Modifier.size(20.dp))
                    }
                    val showLeftFire = settings.leftFireMode == LeftFireMode.ALWAYS ||
                        (settings.leftFireMode == LeftFireMode.SCOPE_ONLY && player.isScoped)
                    if (showLeftFire) {
                        HoldToFireButton(
                            sizeDp = (56f * settings.fireButtonScale).toInt(),
                            onFiringChanged = onSetFiring,
                            onDragAim = onRotateAim,
                            testTag = "left_fire_button"
                        )
                    }
                }
                VirtualJoystick(onMove = onMoveVector, modifier = Modifier.testTag("movement_joystick"))
            }

            // Right Thumb Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 18.dp, bottom = 18.dp)
                    .alpha(settings.hudOpacity),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val cdSec = player.skillCooldownSec.toInt()
                    val skillReady = player.skillCooldownSec <= 0f
                    HudActionCircle(
                        label = if (skillReady) "SKILL" else "${cdSec}s",
                        badge = null,
                        accent = if (skillReady) player.characterId.accentColor else TextSecondary,
                        sizeDp = 52,
                        onClick = onActivateSkill,
                        testTag = "skill_button"
                    ) {
                        Icon(
                            Icons.Filled.Bolt,
                            contentDescription = "Character Skill",
                            tint = if (skillReady) player.characterId.accentColor else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    HudActionCircle(
                        label = "RLD",
                        badge = null,
                        accent = AmberGold,
                        sizeDp = 48,
                        onClick = onReloadWeapon,
                        testTag = "reload_button"
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reload", tint = AmberGold, modifier = Modifier.size(20.dp))
                    }
                    HudActionCircle(
                        label = if (player.isScoped) "AIM+" else "SCOPE",
                        badge = null,
                        accent = if (player.isScoped) CrimsonStrike else Color.White,
                        sizeDp = 50,
                        onClick = onToggleScope,
                        testTag = "scope_button"
                    ) {
                        Icon(
                            Icons.Filled.CenterFocusStrong,
                            contentDescription = "Scope",
                            tint = if (player.isScoped) CrimsonStrike else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    HudActionCircle(
                        label = "GLOO",
                        badge = "${player.glooWalls}",
                        accent = GlooCyan,
                        sizeDp = (64f * settings.glooButtonScale).toInt(),
                        onClick = onDeployGlooWall,
                        testTag = "gloo_wall_button"
                    ) {
                        Icon(Icons.Filled.Shield, contentDescription = "Deploy 3D Gloo Wall", tint = GlooCyan, modifier = Modifier.size(26.dp))
                    }
                    HoldToFireButton(
                        sizeDp = (78f * settings.fireButtonScale).toInt(),
                        onFiringChanged = onSetFiring,
                        onDragAim = onRotateAim,
                        testTag = "primary_fire_button"
                    )
                }
            }
        }

        // 7. CS Buy Shop
        if (arenaState.phase == MatchPhase.SHOPPING) {
            ClashSquadShopOverlay(state = arenaState, onBuyItem = onBuyCsItem, onReady = onStartCombatFromShop)
        }

        // 8. Lone Wolf Weapon Picker
        if (arenaState.phase == MatchPhase.LONE_WOLF_PICK) {
            LoneWolfPickOverlay(state = arenaState, onSelectLoadout = onSelectLoneWolfLoadout)
        }

        // 9. Pause Overlay
        if (showPauseDialog && arenaState.phase != MatchPhase.MATCH_OVER) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBg.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 360.dp)
                        .fillMaxWidth(0.86f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(TacticalCard)
                        .border(1.5.dp, AmberGold, RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("MATCH PAUSED", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Button(
                        onClick = {
                            showPauseDialog = false
                            onPauseChanged(false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldHeal, contentColor = ObsidianBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("RESUME COMBAT", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            showPauseDialog = false
                            onExitMatch()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("RETURN TO LOBBY", color = CrimsonStrike, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 10. Match Summary Overlay
        arenaState.summary?.let { summary ->
            MatchSummaryOverlay(summary = summary, onPlayAgain = onPlayAgain, onExitToLobby = onExitMatch)
        }
    }
}

@Composable
private fun TacticalMinimapWithFlight(state: ArenaState) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ObsidianBg.copy(alpha = 0.82f))
            .border(1.dp, TacticalBorder, RoundedCornerShape(8.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sx = size.width / state.mapWidth
            val sy = size.height / state.mapHeight

            state.obstacles.forEach { obs ->
                drawRect(
                    color = Color(0xFF39465B),
                    topLeft = Offset(obs.x * sx, obs.y * sy),
                    size = androidx.compose.ui.geometry.Size(obs.width * sx, obs.height * sy)
                )
            }

            // Safe Zone
            drawCircle(
                color = Color(0xFF40C4FF),
                radius = state.safeZone.currentRadius * sx,
                center = Offset(state.safeZone.centerX * sx, state.safeZone.centerY * sy),
                style = Stroke(width = 2f)
            )

            // Flight Path & Plane
            if (state.plane.active) {
                drawLine(
                    color = Color.White.copy(alpha = 0.7f),
                    start = Offset(state.plane.startX * sx, state.plane.startY * sy),
                    end = Offset(state.plane.endX * sx, state.plane.endY * sy),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.5f,
                    center = Offset(state.plane.x * sx, state.plane.y * sy)
                )
            }

            state.glooWalls.forEach { gw ->
                drawCircle(color = GlooCyan, radius = 2.5f, center = Offset(gw.centerX * sx, gw.centerY * sy))
            }

            state.combatants.filter { it.isAlive && it.dropState != DropState.IN_PLANE }.forEach { c ->
                when {
                    c.isPlayer -> drawCircle(color = AmberGold, radius = 4f, center = Offset(c.x * sx, c.y * sy))
                    c.team == 0 -> drawCircle(color = ArmorBlue, radius = 3f, center = Offset(c.x * sx, c.y * sy))
                    c.lastShotNoiseSec > 0f -> drawCircle(color = CrimsonStrike, radius = 3f, center = Offset(c.x * sx, c.y * sy))
                }
            }
        }
    }
}

@Composable
private fun ScoreboardBanner(state: ArenaState) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianBg.copy(alpha = 0.85f))
            .border(1.dp, TacticalBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (state.mode) {
            GameMode.BATTLE_ROYALE -> Text("ALIVE ${state.aliveCount}", style = MaterialTheme.typography.titleLarge, color = AmberGold)
            GameMode.CLASH_SQUAD, GameMode.LONE_WOLF -> {
                Text("YOU ${state.playerRoundsWon}", style = MaterialTheme.typography.titleLarge, color = GlooCyan)
                Text("RD ${state.roundNumber}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text("${state.enemyRoundsWon} ENEMY", style = MaterialTheme.typography.titleLarge, color = CrimsonStrike)
            }
        }
    }
}

@Composable
private fun VirtualJoystick(
    onMove: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadiusPx = 90f

    Box(
        modifier = modifier
            .size(116.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        knobOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val raw = knobOffset + dragAmount
                        val len = hypot(raw.x, raw.y)
                        val clamped = if (len > maxRadiusPx) {
                            Offset(raw.x / len * maxRadiusPx, raw.y / len * maxRadiusPx)
                        } else raw
                        knobOffset = clamped
                        onMove(clamped.x / maxRadiusPx, clamped.y / maxRadiusPx)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 0.5f)
            drawCircle(color = ObsidianBg.copy(alpha = 0.55f), radius = size.minDimension * 0.45f, center = center)
            drawCircle(color = Color.White.copy(alpha = 0.35f), radius = size.minDimension * 0.45f, center = center, style = Stroke(width = 3f))
            drawCircle(color = AmberGold.copy(alpha = 0.85f), radius = 22.dp.toPx(), center = center + knobOffset)
        }
    }
}

@Composable
private fun HoldToFireButton(
    sizeDp: Int,
    onFiringChanged: (Boolean) -> Unit,
    onDragAim: (Float) -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(CrimsonStrike.copy(alpha = 0.42f))
            .border(2.5.dp, CrimsonStrike, CircleShape)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    var lastX: Float? = null
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.any { it.pressed }
                        onFiringChanged(pressed)
                        val first = event.changes.firstOrNull()
                        if (first != null && pressed) {
                            if (lastX != null) {
                                val dx = first.position.x - lastX
                                if (dx != 0f) onDragAim(dx * 0.01f)
                            }
                            lastX = first.position.x
                        } else {
                            lastX = null
                        }
                    }
                }
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text("FIRE", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HudActionCircle(
    label: String,
    badge: String?,
    accent: Color,
    sizeDp: Int,
    onClick: () -> Unit,
    testTag: String,
    iconContent: @Composable () -> Unit
) {
    Box(modifier = Modifier.size(sizeDp.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(ObsidianBg.copy(alpha = 0.72f))
                .border(2.dp, accent, CircleShape)
                .clickable(onClick = onClick)
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                iconContent()
                Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Text(badge, style = MaterialTheme.typography.labelSmall, color = ObsidianBg, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ClashSquadShopOverlay(
    state: ArenaState,
    onBuyItem: (CsShopItem) -> Unit,
    onReady: () -> Unit
) {
    val shopItems = remember {
        listOf(
            CsShopItem.GlooWall,
            CsShopItem.VestAndHelmet,
            CsShopItem.Medkit,
            CsShopItem.Weapon(WeaponId.DESERT_EAGLE),
            CsShopItem.Weapon(WeaponId.MP5),
            CsShopItem.Weapon(WeaponId.MP40),
            CsShopItem.Weapon(WeaponId.M1887),
            CsShopItem.Weapon(WeaponId.SCAR),
            CsShopItem.Weapon(WeaponId.AK47),
            CsShopItem.Weapon(WeaponId.AWM)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg.copy(alpha = 0.88f))
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(0.86f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(14.dp))
                .background(TacticalCard)
                .border(1.5.dp, GlooCyan, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("CS BUY PHASE • ROUND ${state.roundNumber}", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text("Starts in ${state.phaseTimerSec.toInt()}s", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                Text("$${state.clashCash}", style = MaterialTheme.typography.headlineMedium, color = AmberGold, fontWeight = FontWeight.Bold)
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(shopItems) { item ->
                    val affordable = state.clashCash >= item.price
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(TacticalCardElevated)
                            .border(1.dp, if (affordable) TacticalBorder else CrimsonStrike.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .clickable(enabled = affordable) { onBuyItem(item) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text(item.subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Text("$${item.price}", style = MaterialTheme.typography.titleLarge, color = if (affordable) AmberGold else TextSecondary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Button(
                onClick = onReady,
                colors = ButtonDefaults.buttonColors(containerColor = BlazingOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("cs_shop_ready_button")
            ) {
                Text("DEPLOY NOW", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LoneWolfPickOverlay(
    state: ArenaState,
    onSelectLoadout: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(14.dp))
                .background(TacticalCard)
                .border(1.5.dp, CrimsonStrike, RoundedCornerShape(14.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("LONE WOLF • ROUND ${state.roundNumber}", style = MaterialTheme.typography.headlineMedium, color = CrimsonStrike)
            Text("Choose 1v1 duel weapons (${state.phaseTimerSec.toInt()}s):", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            state.loneWolfLoadoutOptions.forEachIndexed { idx, pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TacticalCardElevated)
                        .border(1.dp, AmberGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { onSelectLoadout(idx) }
                        .padding(12.dp)
                        .testTag("lone_wolf_pick_$idx"),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${pair.first.displayName} + ${pair.second.displayName}", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text("PICK", style = MaterialTheme.typography.labelLarge, color = AmberGold)
                }
            }
        }
    }
}

@Composable
private fun MatchSummaryOverlay(
    summary: MatchSummary,
    onPlayAgain: () -> Unit,
    onExitToLobby: () -> Unit
) {
    val title = when {
        summary.mode == GameMode.BATTLE_ROYALE && summary.victory -> "BOOYAH!"
        summary.victory -> "VICTORY!"
        summary.mode == GameMode.BATTLE_ROYALE -> "ELIMINATED (#${summary.placement}/${summary.totalTeamsOrPlayers})"
        else -> "DEFEAT (${summary.playerRoundsWon} - ${summary.enemyRoundsWon})"
    }
    val accent = if (summary.victory) AmberGold else CrimsonStrike

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(16.dp))
                .background(TacticalCard)
                .border(2.dp, accent, RoundedCornerShape(16.dp))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.displayMedium, color = accent, fontWeight = FontWeight.Bold)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianBg)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SummaryStatLine("Eliminations", "${summary.kills}")
                SummaryStatLine("Headshots", "${summary.headshots}")
                SummaryStatLine("Damage Dealt", "${summary.damageDealt}")
                SummaryStatLine("Gold Earned", "+${summary.goldEarned} G", AmberGold)
                SummaryStatLine(
                    "Rank Points",
                    if (summary.rankDelta >= 0) "+${summary.rankDelta} RP" else "${summary.rankDelta} RP",
                    if (summary.rankDelta >= 0) EmeraldHeal else CrimsonStrike
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onExitToLobby,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("LOBBY", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onPlayAgain,
                    colors = ButtonDefaults.buttonColors(containerColor = BlazingOrange),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("PLAY AGAIN", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SummaryStatLine(label: String, value: String, valueColor: Color = Color.White) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor, fontWeight = FontWeight.Bold)
    }
}
