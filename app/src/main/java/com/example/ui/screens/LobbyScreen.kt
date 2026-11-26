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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.filament.FilamentLobbyViewport
import com.example.model.AppDataState
import com.example.model.CharacterId
import com.example.model.GameMode
import com.example.ui.components.CharacterPortrait
import com.example.ui.components.CurrencyBadge
import com.example.ui.components.RankChip
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

private enum class LobbyOverlayPanel {
    NONE,
    CHARACTERS,
    RANK
}

@Composable
fun LobbyScreen(
    appData: AppDataState,
    selectedMode: GameMode,
    onSelectMode: (GameMode) -> Unit,
    onStartMatch: (GameMode) -> Unit,
    onSelectOrUnlockCharacter: (CharacterId) -> Unit,
    onUpgradeCharacter: (CharacterId) -> Unit,
    onOpenArmory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = appData.profile
    val activeChar = profile.selectedCharacter
    var activePanel by remember { mutableStateOf(LobbyOverlayPanel.NONE) }
    var showModeSheet by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // 1. Live 3D Filament Lobby Character & Pedestal Viewport
        FilamentLobbyViewport(
            characterId = activeChar,
            offsetCharacterLeft = activePanel != LobbyOverlayPanel.NONE,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle left-to-right vignette for crisp HUD contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            ObsidianBg.copy(alpha = 0.55f),
                            Color.Transparent,
                            ObsidianBg.copy(alpha = 0.55f)
                        )
                    )
                )
        )

        // 2. Top Free Fire Status Bar (Profile, Gold, Diamonds, Settings)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Top-Left Player Card
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianBg.copy(alpha = 0.78f))
                    .border(1.dp, AmberGold.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                CharacterPortrait(
                    characterId = activeChar,
                    modifier = Modifier.size(40.dp),
                    cornerRadius = 6.dp
                )
                Column {
                    Text(
                        text = profile.callsign,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    RankChip(rankTier = profile.rankTier, rankPoints = profile.rankPoints)
                }
                Spacer(modifier = Modifier.width(6.dp))
                CurrencyBadge(label = "F", value = "${profile.gold}", accent = AmberGold)
                CurrencyBadge(label = "DIA", value = "${profile.diamonds}", accent = GlooCyan)
            }

            // Top-Right Action Icons (Settings, Armory)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianBg.copy(alpha = 0.78f))
                        .border(1.dp, TacticalBorder, RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenArmory)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("lobby_armory_button")
                ) {
                    Text("WEAPON ARMORY", style = MaterialTheme.typography.labelLarge, color = AmberGold)
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianBg.copy(alpha = 0.78f))
                        .border(1.dp, TacticalBorder, RoundedCornerShape(8.dp))
                        .testTag("lobby_settings_button")
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }
        }

        // 3. Left Free Fire Sidebar Rail (Lobby, Character, Rank, Settings)
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LobbyRailButton(
                label = "LOBBY",
                icon = Icons.Filled.Home,
                selected = activePanel == LobbyOverlayPanel.NONE,
                onClick = { activePanel = LobbyOverlayPanel.NONE },
                testTag = "rail_lobby"
            )
            LobbyRailButton(
                label = "CHARACTER",
                icon = Icons.Filled.Person,
                selected = activePanel == LobbyOverlayPanel.CHARACTERS,
                onClick = {
                    activePanel = if (activePanel == LobbyOverlayPanel.CHARACTERS) LobbyOverlayPanel.NONE else LobbyOverlayPanel.CHARACTERS
                },
                testTag = "rail_character"
            )
            LobbyRailButton(
                label = "RANK",
                icon = Icons.Filled.EmojiEvents,
                selected = activePanel == LobbyOverlayPanel.RANK,
                onClick = {
                    activePanel = if (activePanel == LobbyOverlayPanel.RANK) LobbyOverlayPanel.NONE else LobbyOverlayPanel.RANK
                },
                testTag = "rail_rank"
            )
            LobbyRailButton(
                label = "ARMORY",
                icon = Icons.Filled.Shield,
                selected = false,
                onClick = onOpenArmory,
                testTag = "rail_armory"
            )
        }

        // 4. Bottom-Left Active Skill & 3D Rotate Hint
        if (activePanel == LobbyOverlayPanel.NONE) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 14.dp, bottom = 14.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianBg.copy(alpha = 0.78f))
                    .border(1.dp, activeChar.accentColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${activeChar.displayName.uppercase()} • LV.${profile.levelOf(activeChar)} (3D)",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${activeChar.skillName}: ${activeChar.shortSummary}",
                    style = MaterialTheme.typography.labelSmall,
                    color = activeChar.accentColor
                )
                Text(
                    text = "Drag center screen to rotate 3D character • Kills ${profile.totalKills} • Wins ${profile.totalWins}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        // 5. Right-Side Overlay Panel (Characters Vault OR Rank Season Screen like screenshot #3)
        if (activePanel == LobbyOverlayPanel.CHARACTERS) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(0.76f)
                    .fillMaxWidth(0.46f)
                    .padding(end = 14.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianBg.copy(alpha = 0.88f))
                    .border(1.5.dp, AmberGold.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text("3D CHARACTER VAULT", style = MaterialTheme.typography.titleLarge, color = AmberGold)
                    }
                    items(CharacterId.entries) { character ->
                        val isUnlocked = character in profile.unlockedCharacters
                        val isSelected = profile.selectedCharacter == character
                        val level = profile.levelOf(character)
                        val upgradeCost = level * 350

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) TacticalCardElevated else TacticalCard)
                                .border(1.dp, if (isSelected) character.accentColor else TacticalBorder, RoundedCornerShape(10.dp))
                                .clickable { onSelectOrUnlockCharacter(character) }
                                .padding(10.dp)
                                .testTag("char_item_${character.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CharacterPortrait(characterId = character, modifier = Modifier.size(44.dp))
                                    Column {
                                        Text(
                                            text = "${character.displayName.uppercase()} (LV.$level)",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = character.skillName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = character.accentColor
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = EmeraldHeal)
                                } else if (!isUnlocked) {
                                    Button(
                                        onClick = { onSelectOrUnlockCharacter(character) },
                                        enabled = profile.gold >= character.unlockCostGold,
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianBg),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("${character.unlockCostGold}", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (level < 6) {
                                            OutlinedButton(
                                                onClick = { onUpgradeCharacter(character) },
                                                enabled = profile.gold >= upgradeCost,
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Filled.ArrowUpward, contentDescription = null, modifier = Modifier.size(14.dp), tint = GlooCyan)
                                                Text("$upgradeCost", color = GlooCyan, fontSize = 11.sp)
                                            }
                                        }
                                        Button(
                                            onClick = { onSelectOrUnlockCharacter(character) },
                                            colors = ButtonDefaults.buttonColors(containerColor = BlazingOrange),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("EQUIP", color = Color.White, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(character.shortSummary, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                }
            }
        } else if (activePanel == LobbyOverlayPanel.RANK) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(0.42f)
                    .padding(end = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianBg.copy(alpha = 0.88f))
                    .border(2.dp, AmberGold, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("SEASON RANKED • BR / CS", style = MaterialTheme.typography.labelLarge, color = AmberGold)
                Icon(Icons.Filled.EmojiEvents, contentDescription = "Rank Trophy", tint = AmberGold, modifier = Modifier.size(56.dp))
                Text(profile.rankTier.uppercase(), style = MaterialTheme.typography.headlineLarge, color = Color.White)
                Text("${profile.rankPoints} RANK POINTS", style = MaterialTheme.typography.titleMedium, color = GlooCyan)
                LinearProgressIndicator(
                    progress = { ((profile.rankPoints - 800) / 2400f).coerceIn(0.05f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = AmberGold,
                    trackColor = TacticalCard
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("MATCHES: ${profile.matchesPlayed}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text("WINS: ${profile.totalWins}", style = MaterialTheme.typography.labelSmall, color = EmeraldHeal)
                    Text("KILLS: ${profile.totalKills}", style = MaterialTheme.typography.labelSmall, color = CrimsonStrike)
                }
            }
        }

        // 6. Bottom-Right Free Fire Mode Selector + Big Gold START Button
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Mode Picker Card
            Row(
                modifier = Modifier
                    .height(58.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianBg.copy(alpha = 0.86f))
                    .border(1.5.dp, AmberGold.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .clickable { showModeSheet = !showModeSheet }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = when (selectedMode) {
                        GameMode.BATTLE_ROYALE -> Icons.Filled.Shield
                        GameMode.CLASH_SQUAD -> Icons.Filled.Groups
                        GameMode.LONE_WOLF -> Icons.Filled.Person
                    },
                    contentDescription = selectedMode.title,
                    tint = AmberGold
                )
                Column {
                    Text(
                        text = selectedMode.badge,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedMode.mapName,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Icon(Icons.Filled.SwapHoriz, contentDescription = "Switch Mode", tint = AmberGold)
            }

            // Iconic Free Fire Amber-Gold START Button
            Button(
                onClick = { onStartMatch(selectedMode) },
                modifier = Modifier
                    .height(58.dp)
                    .width(164.dp)
                    .testTag("start_match_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberGold,
                    contentColor = ObsidianBg
                )
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Start", modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "START",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Mode Selection Popup (BR / CS / Lone Wolf)
        if (showModeSheet) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBg.copy(alpha = 0.6f))
                    .clickable { showModeSheet = false },
                contentAlignment = Alignment.BottomEnd
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(end = 14.dp, bottom = 82.dp)
                        .width(340.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TacticalCard)
                        .border(1.5.dp, AmberGold, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("SELECT MODE (3D ARENA)", style = MaterialTheme.typography.titleLarge, color = AmberGold)
                    GameMode.entries.forEach { mode ->
                        val isCur = mode == selectedMode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCur) AmberGold.copy(alpha = 0.18f) else ObsidianBg)
                                .border(1.dp, if (isCur) AmberGold else TacticalBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    onSelectMode(mode)
                                    showModeSheet = false
                                }
                                .padding(10.dp)
                                .testTag("mode_card_${mode.name.lowercase()}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(mode.title.uppercase(), style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("${mode.mapName} • ${mode.subtitle}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            if (isCur) Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = AmberGold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LobbyRailButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .width(126.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) AmberGold else ObsidianBg.copy(alpha = 0.78f))
            .border(1.dp, if (selected) AmberGold else TacticalBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) ObsidianBg else Color.White,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) ObsidianBg else Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}
