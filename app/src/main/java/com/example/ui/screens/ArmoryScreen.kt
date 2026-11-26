package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.WeaponId
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CrimsonStrike
import com.example.ui.theme.GlooCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalCard
import com.example.ui.theme.TacticalCardElevated
import com.example.ui.theme.TextSecondary

@Composable
fun ArmoryScreen(modifier: Modifier = Modifier) {
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
                Column {
                    Text(
                        text = "TACTICAL ARMORY & GLOO WALL",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Inspect weapon ballistics, headshot multipliers, and Clash Squad prices",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // Gloo Wall Feature Spec Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(TacticalCardElevated)
                        .border(1.5.dp, GlooCyan.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                        .testTag("armory_gloo_wall_card"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GLOO WALL BARRIER",
                            style = MaterialTheme.typography.titleLarge,
                            color = GlooCyan
                        )
                        Text(
                            text = "260 HP • INSTANT DEPLOY",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GlooCyan.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = "Deploys a reinforced hexagonal cryo-shield directly in front of your aim line. Blocks all gunfire and movement, counters sniper peeks, and combos with Skyler for instant +35 HP healing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            items(WeaponId.entries) { weapon ->
                val fireRateNorm = (1100f / weapon.fireIntervalMs).coerceIn(1f, 14f) / 14f
                val dmgNorm = ((weapon.damage * weapon.pellets) / 125f).coerceIn(0.1f, 1f)
                val rangeNorm = (weapon.range / 1000f).coerceIn(0.15f, 1f)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TacticalCard)
                        .border(1.dp, TacticalBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                        .testTag("weapon_card_${weapon.name.lowercase()}"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            Text(
                                text = weapon.displayName.uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = weapon.category.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = weapon.accentColor,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(weapon.accentColor.copy(alpha = 0.16f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = if (weapon.csPrice == 0) "STARTER" else "CS $${weapon.csPrice}",
                            style = MaterialTheme.typography.labelLarge,
                            color = AmberGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    WeaponStatRow("DAMAGE", "${weapon.damage}${if (weapon.pellets > 1) " x${weapon.pellets}" else ""}", dmgNorm, CrimsonStrike)
                    WeaponStatRow("FIRE RATE", "${(1000f / weapon.fireIntervalMs).toInt()}/s", fireRateNorm, AmberGold)
                    WeaponStatRow("RANGE", "${weapon.range.toInt()}m", rangeNorm, GlooCyan)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "MAG: ${weapon.magSize}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "HEADSHOT: ${weapon.headshotMultiplier}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = CrimsonStrike
                        )
                        Text(
                            text = "SCOPE: ${weapon.scopeZoom}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = GlooCyan
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeaponStatRow(label: String, value: String, progress: Float, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.widthIn(min = 72.dp)
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = ObsidianBg
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.widthIn(min = 52.dp)
        )
    }
}
