package com.example.ui.match

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Paragliding
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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
import com.example.model.HudButtonId
import com.example.model.HudElementLayout
import com.example.model.LeftFireMode
import com.example.model.WeaponId
import com.example.ui.screens.CustomHudEditorScreen
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ArmorBlue
import com.example.ui.theme.BlazingOrange
import com.example.ui.theme.CrimsonStrike
import com.example.ui.theme.EmeraldHeal
import com.example.ui.theme.GlooCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PurpleMythic
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalCard
import com.example.ui.theme.TacticalCardElevated
import com.example.ui.theme.TextSecondary
import kotlin.math.hypot
import kotlin.math.roundToInt

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
    onJump: () -> Unit,
    onToggleCrouch: () -> Unit,
    onToggleSprint: () -> Unit,
    onSwitchWeapon: () -> Unit,
    onReloadWeapon: () -> Unit,
    onUseMedkit: () -> Unit,
    onPickupLoot: () -> Unit,
    onBuyCsItem: (CsShopItem) -> Unit,
    onStartCombatFromShop: () -> Unit,
    onSelectLoneWolfLoadout: (Int) -> Unit,
    onSaveCustomHud: (Int, Map<HudButtonId, HudElementLayout>) -> Unit,
    onPauseChanged: (Boolean) -> Unit,
    onPlayAgain: () -> Unit,
    onExitMatch: () -> Unit
) {
    var showPauseDialog by remember { mutableStateOf(false) }
    var showInMatchHudEditor by remember { mutableStateOf(false) }

    BackHandler {
        if (showInMatchHudEditor) {
            showInMatchHudEditor = false
        } else if (arenaState.phase != MatchPhase.MATCH_OVER) {
            showPauseDialog = !showPauseDialog
            onPauseChanged(showPauseDialog)
        } else {
            onExitMatch()
        }
    }

    val player = arenaState.player
    val hudLayout = settings.activeHudLayout

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        val density = LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val screenHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

        fun layoutOf(id: HudButtonId): HudElementLayout =
            hudLayout[id] ?: HudElementLayout(id.defaultX, id.defaultY, id.defaultScale, settings.hudOpacity)

        fun placedModifier(id: HudButtonId, widthDp: Float, heightDp: Float): Modifier {
            val spec = layoutOf(id)
            val wPx = with(density) { (widthDp * spec.scale).dp.toPx() }
            val hPx = with(density) { (heightDp * spec.scale).dp.toPx() }
            val ox = (spec.xFraction * screenWidthPx - wPx * 0.5f).roundToInt()
            val oy = (spec.yFraction * screenHeightPx - hPx * 0.5f).roundToInt()
            return Modifier
                .offset { IntOffset(ox, oy) }
                .alpha(spec.alpha)
        }

        // 1. Live 3D Filament Battleground
        FilamentArenaViewport(
            arenaState = arenaState,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Right-half Drag-to-Aim Touch Surface
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxWidth(0.56f)
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
                val gap = if (player.isScoped) 6f else if (player.isCrouching) 8f else 11f
                val arm = if (player.isScoped) 14f else 10f
                val cColor = if (player.isScoped) CrimsonStrike else Color.White.copy(alpha = 0.88f)
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

        // 4. Top-Center Free Fire Bearing Compass + Scoreboard + Zone Timer + Kill Feed (Screenshot #4)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (player != null) {
                BearingCompassBar(aimAngleRad = player.aimAngle)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScoreboardBanner(state = arenaState)
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
                Text(
                    text = "ZONE ${arenaState.safeZone.countdownSec.toInt()}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = GlooCyan,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianBg.copy(alpha = 0.82f))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
                IconButton(
                    onClick = {
                        showPauseDialog = true
                        onPauseChanged(true)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianBg.copy(alpha = 0.82f))
                        .border(1.dp, TacticalBorder, RoundedCornerShape(8.dp))
                        .testTag("pause_match_button")
                ) {
                    Icon(Icons.Filled.Pause, contentDescription = "Pause Match", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            arenaState.killFeed.take(2).forEach { item ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ObsidianBg.copy(alpha = 0.78f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(item.killerName, style = MaterialTheme.typography.labelSmall, color = if (item.killerIsAlly) AmberGold else CrimsonStrike)
                    Text(if (item.isHeadshot) "[HS]" else "->", style = MaterialTheme.typography.labelSmall, color = if (item.isHeadshot) CrimsonStrike else TextSecondary)
                    Text(item.victimName, style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }

        // 5. Draggable/Placed Minimap (Screenshot #4 Top-Left default, or user-placed anywhere!)
        val minimapSpec = layoutOf(HudButtonId.MINIMAP)
        Box(modifier = placedModifier(HudButtonId.MINIMAP, 96f, 96f)) {
            TacticalMinimapWithFlight(
                state = arenaState,
                sizeDp = (96f * minimapSpec.scale).roundToInt()
            )
        }

        // Round / Drop Toast
        arenaState.bannerMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 86.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BlazingOrange.copy(alpha = 0.92f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(msg, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // 6. FLIGHT & PARACHUTE HUD OVERLAY (Battle Royale Drop!)
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
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .navigationBarsPadding()
                        .padding(start = 20.dp, bottom = 20.dp)
                ) {
                    VirtualJoystick(sizeDp = 116, onMove = onMoveVector, modifier = Modifier.testTag("parachute_joystick"))
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

        // 7. GROUNDED 3D COMBAT HUD — EVERY CONTROL PLACED BY USER'S CUSTOM HUD LAYOUT!
        if (player != null && arenaState.phase == MatchPhase.ACTIVE_COMBAT && player.dropState == DropState.GROUNDED) {
            // Bottom-Center HP / EP / Armor Bar + Loot Pickup (Screenshot #4)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 10.dp)
                    .widthIn(max = 260.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                arenaState.nearbyLoot?.let { loot ->
                    Button(
                        onClick = onPickupLoot,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianBg),
                        modifier = Modifier.height(44.dp).testTag("pickup_loot_button")
                    ) {
                        Text("PICK UP ${loot.label.uppercase()}", fontWeight = FontWeight.Bold)
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(ObsidianBg.copy(alpha = 0.82f))
                        .border(1.dp, TacticalBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("HP ${player.hp.toInt()}/${player.maxHp.toInt()}", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("EP ${player.ep.toInt()} • ARM ${player.armor.toInt()}", style = MaterialTheme.typography.labelSmall, color = ArmorBlue)
                    }
                    LinearProgressIndicator(
                        progress = { (player.ep / player.maxEp).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                        color = AmberGold,
                        trackColor = TacticalCard
                    )
                    LinearProgressIndicator(
                        progress = { (player.hp / player.maxHp).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = if (player.hp < 65f) CrimsonStrike else EmeraldHeal,
                        trackColor = TacticalCard
                    )
                }
            }

            // Weapon Slot Bar (Top-Right default, customizable)
            val weaponSpec = layoutOf(HudButtonId.WEAPON_BAR)
            Row(
                modifier = placedModifier(HudButtonId.WEAPON_BAR, 126f, 52f)
                    .size((126f * weaponSpec.scale).dp, (52f * weaponSpec.scale).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianBg.copy(alpha = 0.84f))
                    .border(1.5.dp, AmberGold.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .clickable(onClick = onSwitchWeapon)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("weapon_switch_bar"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(player.activeWeapon.displayName.uppercase(), style = MaterialTheme.typography.labelLarge, color = AmberGold, fontSize = 11.sp)
                    Text(
                        text = if (player.reloadSecLeft > 0f) "RELOAD..." else "${player.activeAmmoInMag} / ${player.reserveAmmo}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
                if (player.weapon2 != null && settings.quickWeaponSwitch) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = "Switch Weapon", tint = GlooCyan, modifier = Modifier.size(18.dp))
                }
            }

            // Virtual Joystick
            val joySpec = layoutOf(HudButtonId.JOYSTICK)
            Box(modifier = placedModifier(HudButtonId.JOYSTICK, 114f, 114f)) {
                VirtualJoystick(
                    sizeDp = (114f * joySpec.scale).roundToInt(),
                    onMove = onMoveVector,
                    modifier = Modifier.testTag("movement_joystick")
                )
            }

            // Sprint Lock Button
            val sprintSpec = layoutOf(HudButtonId.SPRINT)
            Box(modifier = placedModifier(HudButtonId.SPRINT, 48f, 48f)) {
                HudActionCircle(
                    label = "RUN",
                    badge = null,
                    accent = if (player.isSprinting) AmberGold else Color.White,
                    sizeDp = (48f * sprintSpec.scale).roundToInt(),
                    onClick = onToggleSprint,
                    testTag = "sprint_button"
                ) {
                    Icon(Icons.Filled.DirectionsRun, contentDescription = "Sprint Lock", tint = if (player.isSprinting) AmberGold else Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Medkit Button
            val medSpec = layoutOf(HudButtonId.MEDKIT)
            Box(modifier = placedModifier(HudButtonId.MEDKIT, 52f, 52f)) {
                HudActionCircle(
                    label = "MED",
                    badge = "${player.medkits}",
                    accent = EmeraldHeal,
                    sizeDp = (52f * medSpec.scale).roundToInt(),
                    onClick = onUseMedkit,
                    testTag = "medkit_button"
                ) {
                    Icon(Icons.Filled.LocalHospital, contentDescription = "Use Medkit", tint = EmeraldHeal, modifier = Modifier.size(20.dp))
                }
            }

            // Left Fire Button
            val showLeftFire = settings.leftFireMode == LeftFireMode.ALWAYS ||
                (settings.leftFireMode == LeftFireMode.SCOPE_ONLY && player.isScoped)
            if (showLeftFire) {
                val lfSpec = layoutOf(HudButtonId.LEFT_FIRE)
                Box(modifier = placedModifier(HudButtonId.LEFT_FIRE, 56f * settings.fireButtonScale, 56f * settings.fireButtonScale)) {
                    HoldToFireButton(
                        sizeDp = (56f * lfSpec.scale * settings.fireButtonScale).roundToInt(),
                        onFiringChanged = onSetFiring,
                        onDragAim = onRotateAim,
                        testTag = "left_fire_button"
                    )
                }
            }

            // Gloo Wall Button
            val glooSpec = layoutOf(HudButtonId.GLOO_WALL)
            Box(modifier = placedModifier(HudButtonId.GLOO_WALL, 64f * settings.glooButtonScale, 64f * settings.glooButtonScale)) {
                HudActionCircle(
                    label = "GLOO",
                    badge = "${player.glooWalls}",
                    accent = GlooCyan,
                    sizeDp = (64f * glooSpec.scale * settings.glooButtonScale).roundToInt(),
                    onClick = onDeployGlooWall,
                    testTag = "gloo_wall_button"
                ) {
                    Icon(Icons.Filled.Shield, contentDescription = "Deploy 3D Gloo Wall", tint = GlooCyan, modifier = Modifier.size(24.dp))
                }
            }

            // Primary Right Fire Button
            val fireSpec = layoutOf(HudButtonId.FIRE)
            Box(modifier = placedModifier(HudButtonId.FIRE, 78f * settings.fireButtonScale, 78f * settings.fireButtonScale)) {
                HoldToFireButton(
                    sizeDp = (78f * fireSpec.scale * settings.fireButtonScale).roundToInt(),
                    onFiringChanged = onSetFiring,
                    onDragAim = onRotateAim,
                    testTag = "primary_fire_button"
                )
            }

            // Scope Button
            val scopeSpec = layoutOf(HudButtonId.SCOPE)
            Box(modifier = placedModifier(HudButtonId.SCOPE, 52f, 52f)) {
                HudActionCircle(
                    label = if (player.isScoped) "AIM+" else "SCOPE",
                    badge = null,
                    accent = if (player.isScoped) CrimsonStrike else Color.White,
                    sizeDp = (52f * scopeSpec.scale).roundToInt(),
                    onClick = onToggleScope,
                    testTag = "scope_button"
                ) {
                    Icon(Icons.Filled.CenterFocusStrong, contentDescription = "Scope", tint = if (player.isScoped) CrimsonStrike else Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Jump Button
            val jumpSpec = layoutOf(HudButtonId.JUMP)
            Box(modifier = placedModifier(HudButtonId.JUMP, 50f, 50f)) {
                HudActionCircle(
                    label = "JUMP",
                    badge = null,
                    accent = Color.White,
                    sizeDp = (50f * jumpSpec.scale).roundToInt(),
                    onClick = onJump,
                    testTag = "jump_button"
                ) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Jump", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Crouch Button
            val crouchSpec = layoutOf(HudButtonId.CROUCH)
            Box(modifier = placedModifier(HudButtonId.CROUCH, 50f, 50f)) {
                HudActionCircle(
                    label = if (player.isCrouching) "STAND" else "CROUCH",
                    badge = null,
                    accent = if (player.isCrouching) AmberGold else Color.White,
                    sizeDp = (50f * crouchSpec.scale).roundToInt(),
                    onClick = onToggleCrouch,
                    testTag = "crouch_button"
                ) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Crouch", tint = if (player.isCrouching) AmberGold else Color.White, modifier = Modifier.size(22.dp))
                }
            }

            // Reload Button
            val reloadSpec = layoutOf(HudButtonId.RELOAD)
            Box(modifier = placedModifier(HudButtonId.RELOAD, 48f, 48f)) {
                HudActionCircle(
                    label = "RLD",
                    badge = null,
                    accent = AmberGold,
                    sizeDp = (48f * reloadSpec.scale).roundToInt(),
                    onClick = onReloadWeapon,
                    testTag = "reload_button"
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Reload", tint = AmberGold, modifier = Modifier.size(20.dp))
                }
            }

            // Active Character Skill Button
            val skillSpec = layoutOf(HudButtonId.SKILL)
            val cdSec = player.skillCooldownSec.toInt()
            val skillReady = player.skillCooldownSec <= 0f
            Box(modifier = placedModifier(HudButtonId.SKILL, 54f, 54f)) {
                HudActionCircle(
                    label = if (skillReady) "SKILL" else "${cdSec}s",
                    badge = null,
                    accent = if (skillReady) player.characterId.accentColor else TextSecondary,
                    sizeDp = (54f * skillSpec.scale).roundToInt(),
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
            }
        }

        // 8. CS Buy Shop
        if (arenaState.phase == MatchPhase.SHOPPING) {
            ClashSquadShopOverlay(state = arenaState, onBuyItem = onBuyCsItem, onReady = onStartCombatFromShop)
        }

        // 9. Lone Wolf Weapon Picker
        if (arenaState.phase == MatchPhase.LONE_WOLF_PICK) {
            LoneWolfPickOverlay(state = arenaState, onSelectLoadout = onSelectLoneWolfLoadout)
        }

        // 10. Pause Overlay (with Mid-Match Custom HUD Editor shortcut!)
        if (showPauseDialog && arenaState.phase != MatchPhase.MATCH_OVER && !showInMatchHudEditor) {
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("MATCH PAUSED", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Button(
                        onClick = {
                            showPauseDialog = false
                            onPauseChanged(false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldHeal, contentColor = ObsidianBg),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("RESUME COMBAT", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { showInMatchHudEditor = true },
                        colors = ButtonDefaults.buttonColors(containerColor = GlooCyan, contentColor = ObsidianBg),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("CUSTOM HUD LAYOUT", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            showPauseDialog = false
                            onExitMatch()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("RETURN TO LOBBY", color = CrimsonStrike, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 11. In-Match Custom HUD Editor Overlay
        if (showInMatchHudEditor) {
            CustomHudEditorScreen(
                settings = settings,
                onSaveLayout = { preset, layout -> onSaveCustomHud(preset, layout) },
                onClose = { showInMatchHudEditor = false }
            )
        }

        // 12. Match Summary Overlay
        arenaState.summary?.let { summary ->
            MatchSummaryOverlay(summary = summary, onPlayAgain = onPlayAgain, onExitToLobby = onExitMatch)
        }
    }
}

@Composable
private fun BearingCompassBar(aimAngleRad: Float) {
    val deg = (((Math.toDegrees(aimAngleRad.toDouble()).toInt()) % 360) + 360) % 360
    val cardinal = when {
        deg in 338..360 || deg in 0..22 -> "E"
        deg in 23..67 -> "SE"
        deg in 68..112 -> "S"
        deg in 113..157 -> "SW"
        deg in 158..202 -> "W"
        deg in 203..247 -> "NW"
        deg in 248..292 -> "N"
        else -> "NE"
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ObsidianBg.copy(alpha = 0.72f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
            .padding(horizontal = 14.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${(deg - 30 + 360) % 360}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text("$cardinal $deg°", style = MaterialTheme.typography.labelLarge, color = AmberGold, fontWeight = FontWeight.Bold)
        Text("${(deg + 30) % 360}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun TacticalMinimapWithFlight(state: ArenaState, sizeDp: Int = 96) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ObsidianBg.copy(alpha = 0.84f))
            .border(1.5.dp, TacticalBorder, RoundedCornerShape(8.dp))
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

            drawCircle(
                color = Color(0xFF40C4FF),
                radius = state.safeZone.currentRadius * sx,
                center = Offset(state.safeZone.centerX * sx, state.safeZone.centerY * sy),
                style = Stroke(width = 2f)
            )

            if (state.plane.active) {
                drawLine(
                    color = Color.White.copy(alpha = 0.7f),
                    start = Offset(state.plane.startX * sx, state.plane.startY * sy),
                    end = Offset(state.plane.endX * sx, state.plane.endY * sy),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                )
                drawCircle(color = Color.White, radius = 4.5f, center = Offset(state.plane.x * sx, state.plane.y * sy))
            }

            state.glooWalls.forEach { gw ->
                drawCircle(color = GlooCyan, radius = 2.5f, center = Offset(gw.centerX * sx, gw.centerY * sy))
            }

            state.combatants.filter { it.isAlive && it.dropState != DropState.IN_PLANE }.forEach { c ->
                when {
                    c.isPlayer -> drawCircle(color = AmberGold, radius = 4f, center = Offset(c.x * sx, c.y * sy))
                    c.team == 0 -> drawCircle(color = ArmorBlue, radius = 3f, center = Offset(c.x * sx, c.y * sy))
                    c.markedSec > 0f -> drawCircle(color = PurpleMythic, radius = 3.5f, center = Offset(c.x * sx, c.y * sy))
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
            .padding(horizontal = 12.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (state.mode) {
            GameMode.BATTLE_ROYALE -> Text("ALIVE ${state.aliveCount}", style = MaterialTheme.typography.titleMedium, color = AmberGold, fontWeight = FontWeight.Bold)
            GameMode.CLASH_SQUAD, GameMode.LONE_WOLF -> {
                Text("YOU ${state.playerRoundsWon}", style = MaterialTheme.typography.titleMedium, color = GlooCyan, fontWeight = FontWeight.Bold)
                Text("RD ${state.roundNumber}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text("${state.enemyRoundsWon} ENEMY", style = MaterialTheme.typography.titleMedium, color = CrimsonStrike, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun VirtualJoystick(
    sizeDp: Int,
    onMove: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadiusPx = 90f

    Box(
        modifier = modifier
            .size(sizeDp.dp)
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
            drawCircle(color = AmberGold.copy(alpha = 0.85f), radius = (size.minDimension * 0.19f), center = center + knobOffset)
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
        Text("FIRE", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
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
                Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Text(badge, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = ObsidianBg, fontWeight = FontWeight.Bold)
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
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("LOBBY", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onPlayAgain,
                    colors = ButtonDefaults.buttonColors(containerColor = BlazingOrange),
                    modifier = Modifier.weight(1f).height(48.dp)
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
