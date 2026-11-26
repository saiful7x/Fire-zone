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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppDataState
import com.example.model.CharacterId
import com.example.ui.components.CharacterPortrait
import com.example.ui.components.CurrencyBadge
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BlazingOrange
import com.example.ui.theme.EmeraldHeal
import com.example.ui.theme.GlooCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalCard
import com.example.ui.theme.TacticalCardElevated
import com.example.ui.theme.TextSecondary

@Composable
fun CharactersScreen(
    appData: AppDataState,
    onSelectOrUnlock: (CharacterId) -> Unit,
    onUpgrade: (CharacterId) -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = appData.profile

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CHARACTER VAULT",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                        Text(
                            text = "Equip active skills & upgrade up to Level 6",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    CurrencyBadge(label = "GOLD", value = "${profile.gold}", accent = AmberGold)
                }
            }

            items(CharacterId.entries) { character ->
                val isUnlocked = character in profile.unlockedCharacters
                val isSelected = profile.selectedCharacter == character
                val level = profile.levelOf(character)
                val upgradeCost = level * 350
                val cooldown = (character.baseCooldownSec - (level - 1) * 1.2f).coerceAtLeast(12f)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) TacticalCardElevated else TacticalCard)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) character.accentColor else TacticalBorder,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onSelectOrUnlock(character) }
                        .padding(14.dp)
                        .testTag("character_card_${character.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CharacterPortrait(
                            characterId = character,
                            modifier = Modifier.size(86.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = character.displayName.uppercase(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isUnlocked) "LV.$level" else "LOCKED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isUnlocked) character.accentColor else TextSecondary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(character.accentColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Equipped",
                                        tint = EmeraldHeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Text(
                                text = "${character.skillName} (${character.skillType}) • CD ${cooldown.toInt()}s",
                                style = MaterialTheme.typography.labelLarge,
                                color = character.accentColor
                            )
                            Text(
                                text = character.detailedEffect,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Level bar + Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Skill Mastery ($level / 6)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { level / 6f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = character.accentColor,
                                trackColor = ObsidianBg
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        if (!isUnlocked) {
                            val canAfford = profile.gold >= character.unlockCostGold
                            Button(
                                onClick = { onSelectOrUnlock(character) },
                                enabled = canAfford,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmberGold,
                                    contentColor = ObsidianBg
                                ),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("unlock_${character.name.lowercase()}")
                            ) {
                                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${character.unlockCostGold} G", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (level < 6) {
                                    OutlinedButton(
                                        onClick = { onUpgrade(character) },
                                        enabled = profile.gold >= upgradeCost,
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("upgrade_${character.name.lowercase()}")
                                    ) {
                                        Icon(Icons.Filled.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp), tint = GlooCyan)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("$upgradeCost G", color = GlooCyan)
                                    }
                                }
                                Button(
                                    onClick = { onSelectOrUnlock(character) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) EmeraldHeal else BlazingOrange,
                                        contentColor = if (isSelected) ObsidianBg else Color.White
                                    ),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("equip_${character.name.lowercase()}")
                                ) {
                                    Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isSelected) "EQUIPPED" else "EQUIP", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
