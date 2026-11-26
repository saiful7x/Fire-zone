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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.filament.FilamentLobbyViewport
import com.example.model.AppDataState
import com.example.model.CharacterId
import com.example.model.WeaponId
import com.example.ui.components.CharacterPortrait
import com.example.ui.components.CurrencyBadge
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

private enum class ProfileTab(val label: String) {
    GALLERY("GALLERY"),
    OVERVIEW("OVERVIEW"),
    ROLE("ROLE"),
    HISTORY("HISTORY"),
    WEAPON("WEAPON"),
    HONOR_SCORE("HONOR SCORE")
}

@Composable
fun ProfileScreen(
    appData: AppDataState,
    onLikeProfile: () -> Unit,
    onSaveProfileMeta: (callsign: String, guildName: String, bio: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = appData.profile
    val activeChar = profile.selectedCharacter
    var selectedTab by remember { mutableStateOf(ProfileTab.GALLERY) }
    var showEditDialog by remember { mutableStateOf(false) }
    var draftCallsign by remember(profile.callsign) { mutableStateOf(profile.callsign) }
    var draftGuild by remember(profile.guildName) { mutableStateOf(profile.guildName) }
    var draftBio by remember(profile.bio) { mutableStateOf(profile.bio) }

    val accountLevel = (18 + profile.matchesPlayed * 2 + profile.totalKills / 3).coerceAtMost(100)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // 1. Live 3D Filament Character Showcase on Pedestal
        FilamentLobbyViewport(
            characterId = activeChar,
            offsetCharacterLeft = true,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            ObsidianBg.copy(alpha = 0.78f),
                            Color.Transparent,
                            ObsidianBg.copy(alpha = 0.72f)
                        )
                    )
                )
        )

        // 2. Top Profile Bar (matches Screenshot #1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianBg.copy(alpha = 0.85f))
                    .border(1.dp, AmberGold.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                CharacterPortrait(characterId = activeChar, modifier = Modifier.size(36.dp), cornerRadius = 6.dp)
                Column {
                    Text(profile.callsign, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(profile.guildName, style = MaterialTheme.typography.labelSmall, color = AmberGold)
                }
                CurrencyBadge(label = "F", value = "${profile.gold}", accent = AmberGold)
                CurrencyBadge(label = "DIA", value = "${profile.diamonds}", accent = GlooCyan)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("PROFILE", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianBg.copy(alpha = 0.85f))
                        .border(1.dp, TacticalBorder, RoundedCornerShape(8.dp))
                        .testTag("profile_close_button")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close Profile", tint = Color.White)
                }
            }
        }

        // 3. Left Vertical Menu (GALLERY, OVERVIEW, ROLE, HISTORY, WEAPON, HONOR SCORE)
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 14.dp, top = 28.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ProfileTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .width(138.dp)
                        .height(38.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) AmberGold else ObsidianBg.copy(alpha = 0.82f))
                        .border(1.dp, if (isSelected) AmberGold else TacticalBorder, RoundedCornerShape(4.dp))
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 12.dp)
                        .testTag("profile_tab_${tab.name.lowercase()}"),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) ObsidianBg else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Floating Region + Rank Emblem badge near 3D Character (Screenshot #1)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 170.dp, top = 64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.horizontalGradient(listOf(PurpleMythic.copy(alpha = 0.85f), ArmorBlue.copy(alpha = 0.72f))))
                .border(1.dp, AmberGold.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = AmberGold, modifier = Modifier.size(18.dp))
            Column {
                Text("Region • ${profile.rankTier}", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                Text("${profile.rankPoints} RP • ${activeChar.displayName}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = AmberGold)
            }
        }

        // 4. Right Panel: Gold-Framed Free Fire Profile Card or Sub-Tab View
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxWidth(0.45f)
                .fillMaxHeight(0.79f)
                .navigationBarsPadding()
                .padding(end = 14.dp, top = 36.dp, bottom = 8.dp)
        ) {
            when (selectedTab) {
                ProfileTab.GALLERY, ProfileTab.OVERVIEW -> {
                    FreeFireGoldProfileCard(
                        appData = appData,
                        accountLevel = accountLevel,
                        onLike = onLikeProfile,
                        onEdit = { showEditDialog = true }
                    )
                }
                ProfileTab.ROLE -> {
                    ProfileRolePanel(appData = appData)
                }
                ProfileTab.HISTORY -> {
                    ProfileHistoryPanel(appData = appData)
                }
                ProfileTab.WEAPON -> {
                    ProfileWeaponPanel()
                }
                ProfileTab.HONOR_SCORE -> {
                    ProfileHonorPanel(appData = appData)
                }
            }
        }

        // 5. Edit Profile Modal
        if (showEditDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBg.copy(alpha = 0.85f))
                    .clickable { showEditDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(380.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TacticalCard)
                        .border(1.5.dp, AmberGold, RoundedCornerShape(12.dp))
                        .clickable(enabled = false) {}
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("EDIT PROFILE CARD", style = MaterialTheme.typography.titleLarge, color = AmberGold)
                    OutlinedTextField(
                        value = draftCallsign,
                        onValueChange = { draftCallsign = it.take(16) },
                        label = { Text("Callsign") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draftGuild,
                        onValueChange = { draftGuild = it.take(18) },
                        label = { Text("Guild Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draftBio,
                        onValueChange = { draftBio = it.take(48) },
                        label = { Text("Signature / Bio") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { showEditDialog = false },
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Text("CANCEL", color = TextSecondary)
                        }
                        Button(
                            onClick = {
                                onSaveProfileMeta(draftCallsign, draftGuild, draftBio)
                                showEditDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianBg),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Text("SAVE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FreeFireGoldProfileCard(
    appData: AppDataState,
    accountLevel: Int,
    onLike: () -> Unit,
    onEdit: () -> Unit
) {
    val profile = appData.profile
    val activeChar = profile.selectedCharacter

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF141822).copy(alpha = 0.94f))
            .border(2.dp, AmberGold, RoundedCornerShape(10.dp))
    ) {
        // Header strip: FREE FIRE • Guild • Edit button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Color(0xFF1B2436), Color(0xFF28344B))))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("FIREZONE", style = MaterialTheme.typography.titleMedium, color = AmberGold, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(profile.guildName, style = MaterialTheme.typography.labelSmall, color = Color.White)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AmberGold.copy(alpha = 0.2f))
                        .clickable(onClick = onEdit),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Profile", tint = AmberGold, modifier = Modifier.size(16.dp))
                }
            }
        }

        // White-Gold Identity Banner (exact Screenshot #1 vibe)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE7ECF2))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CharacterPortrait(characterId = activeChar, modifier = Modifier.size(46.dp), cornerRadius = 6.dp)
                Column {
                    Text(profile.callsign, style = MaterialTheme.typography.titleMedium, color = ObsidianBg, fontWeight = FontWeight.Bold)
                    Text("English / Bangla • ${activeChar.displayName}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF455A64))
                }
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(ObsidianBg.copy(alpha = 0.1f))
                    .clickable(onClick = onLike)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("profile_like_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Filled.ThumbUp, contentDescription = "Like Profile", tint = Color(0xFF1E293B), modifier = Modifier.size(15.dp))
                Text("${profile.likes}", style = MaterialTheme.typography.titleMedium, color = ObsidianBg, fontWeight = FontWeight.Bold)
            }
        }

        // Gold Lv & UID strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AmberGold)
                .padding(horizontal = 10.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Lv. $accountLevel", style = MaterialTheme.typography.labelLarge, color = ObsidianBg, fontWeight = FontWeight.Bold)
            Text("UID: ${profile.uid}", style = MaterialTheme.typography.labelSmall, color = ObsidianBg, fontWeight = FontWeight.Bold)
        }

        // Rank Emblems + Mastery + Bio + Battle Title Ribbons
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileEmblemBadge("CLASH SQUAD", profile.rankTier.uppercase(), AmberGold)
                    ProfileEmblemBadge("BR RANKED", "${profile.rankPoints} RP", GlooCyan)
                    ProfileEmblemBadge("KILLS", "${profile.totalKills}", CrimsonStrike)
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(TacticalCard)
                        .border(1.dp, TacticalBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("WINS: ${profile.totalWins}", style = MaterialTheme.typography.labelSmall, color = EmeraldHeal, fontWeight = FontWeight.Bold)
                    Text("MATCHES: ${profile.matchesPlayed}", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    Text("CHARS: ${profile.unlockedCharacters.size}/${CharacterId.entries.size}", style = MaterialTheme.typography.labelSmall, color = AmberGold)
                }
            }

            item {
                Text(
                    text = profile.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(ObsidianBg.copy(alpha = 0.7f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BattleRibbon(profile.battleTitles.getOrElse(0) { "HEADSHOT ARTIST" }, CrimsonStrike, Modifier.weight(1f))
                    BattleRibbon(profile.battleTitles.getOrElse(1) { "CHAINKILLER" }, AmberGold, Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BattleRibbon(profile.battleTitles.getOrElse(2) { "GLOO MASTER" }, GlooCyan, Modifier.weight(1f))
                    BattleRibbon(profile.battleTitles.getOrElse(3) { "BOOYAH KING" }, BlazingOrange, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProfileEmblemBadge(title: String, value: String, accent: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(TacticalCard)
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(Icons.Filled.MilitaryTech, contentDescription = title, tint = accent, modifier = Modifier.size(22.dp))
        Text(value, style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold)
        Text(title, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = accent)
    }
}

@Composable
private fun BattleRibbon(text: String, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.35f), TacticalCardElevated)))
            .border(1.dp, accent, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProfileRolePanel(appData: AppDataState) {
    val p = appData.profile
    val winRate = if (p.matchesPlayed > 0) (p.totalWins * 100) / p.matchesPlayed else 0
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(TacticalCard.copy(alpha = 0.94f))
            .border(1.5.dp, AmberGold, RoundedCornerShape(10.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("COMBAT ROLE & STATS", style = MaterialTheme.typography.titleLarge, color = AmberGold)
        Text("Primary Role: ${p.selectedCharacter.roleTag}", style = MaterialTheme.typography.titleMedium, color = GlooCyan)
        StatBarRow("Win Rate", "$winRate%", winRate / 100f, EmeraldHeal)
        StatBarRow("Total Eliminations", "${p.totalKills}", (p.totalKills / 50f).coerceIn(0.05f, 1f), CrimsonStrike)
        StatBarRow("Booyahs / Wins", "${p.totalWins}", (p.totalWins / 25f).coerceIn(0.05f, 1f), AmberGold)
        StatBarRow("Rank Progress", "${p.rankPoints} RP", ((p.rankPoints - 800) / 2400f).coerceIn(0.05f, 1f), PurpleMythic)
    }
}

@Composable
private fun StatBarRow(label: String, valText: String, progress: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            Text(valText, style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = ObsidianBg
        )
    }
}

@Composable
private fun ProfileHistoryPanel(appData: AppDataState) {
    val history = appData.profile.matchHistory
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(TacticalCard.copy(alpha = 0.94f))
            .border(1.5.dp, AmberGold, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("MATCH HISTORY", style = MaterialTheme.typography.titleLarge, color = AmberGold)
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Play a BR, CS, or Lone Wolf match to record history!", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(history) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianBg)
                            .border(1.dp, if (item.victory) AmberGold.copy(alpha = 0.6f) else TacticalBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (item.victory) "VICTORY • BOOYAH!" else "DEFEAT",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (item.victory) AmberGold else CrimsonStrike,
                                fontWeight = FontWeight.Bold
                            )
                            Text("${item.modeName} • ${item.timestampLabel}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${item.kills} KILLS", style = MaterialTheme.typography.labelLarge, color = Color.White)
                            Text("DMG ${item.damage}", style = MaterialTheme.typography.labelSmall, color = GlooCyan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileWeaponPanel() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(TacticalCard.copy(alpha = 0.94f))
            .border(1.5.dp, AmberGold, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("FAVORITE WEAPON MASTERY", style = MaterialTheme.typography.titleLarge, color = AmberGold)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(WeaponId.entries) { w ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianBg)
                        .border(1.dp, w.accentColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(w.displayName, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("${w.category.label} • MAG ${w.magSize}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Text("DMG ${w.damage} • HS ${w.headshotMultiplier}x", style = MaterialTheme.typography.labelLarge, color = w.accentColor)
                }
            }
        }
    }
}

@Composable
private fun ProfileHonorPanel(appData: AppDataState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(TacticalCard.copy(alpha = 0.94f))
            .border(1.5.dp, EmeraldHeal, RoundedCornerShape(10.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Filled.Shield, contentDescription = "Honor Score", tint = EmeraldHeal, modifier = Modifier.size(56.dp))
        Text("HONOR SCORE: 100 / 100", style = MaterialTheme.typography.headlineMedium, color = EmeraldHeal, fontWeight = FontWeight.Bold)
        Text("Good Standing • Eligible for CS Ranked & BR Ranked", style = MaterialTheme.typography.bodyMedium, color = Color.White)
        Text("Matches Completed: ${appData.profile.matchesPlayed}", style = MaterialTheme.typography.labelLarge, color = AmberGold)
    }
}
