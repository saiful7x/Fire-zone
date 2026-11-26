package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CharacterId
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ArmorBlue
import com.example.ui.theme.BlazingOrange
import com.example.ui.theme.EmeraldHeal
import com.example.ui.theme.GlooCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TacticalCardElevated

@Composable
fun CharacterPortrait(
    characterId: CharacterId,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp
) {
    val drawableRes = when (characterId) {
        CharacterId.ALOK -> R.drawable.img_char_alok
        CharacterId.CHRONO -> R.drawable.img_char_chrono
        CharacterId.SKYLER -> R.drawable.img_char_skyler
        else -> null
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    listOf(TacticalCardElevated, ObsidianBg)
                )
            )
            .border(1.5.dp, characterId.accentColor.copy(alpha = 0.65f), RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        if (drawableRes != null) {
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = "${characterId.displayName} portrait",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(characterId.accentColor.copy(alpha = 0.42f), Color.Transparent),
                        center = Offset(w * 0.5f, h * 0.45f),
                        radius = w * 0.65f
                    )
                )
                val hex = Path().apply {
                    val cx = w * 0.5f
                    val cy = h * 0.44f
                    val r = minOf(w, h) * 0.28f
                    for (i in 0 until 6) {
                        val a = Math.toRadians((60 * i - 30).toDouble())
                        val px = cx + (r * kotlin.math.cos(a)).toFloat()
                        val py = cy + (r * kotlin.math.sin(a)).toFloat()
                        if (i == 0) moveTo(px, py) else lineTo(px, py)
                    }
                    close()
                }
                drawPath(hex, color = characterId.accentColor.copy(alpha = 0.22f))
                drawPath(hex, color = characterId.accentColor, style = Stroke(width = 4f))
            }
            val icon = when (characterId) {
                CharacterId.WUKONG -> Icons.Filled.Shield
                CharacterId.KELLY -> Icons.Filled.Bolt
                CharacterId.MOCO -> Icons.Filled.Visibility
                CharacterId.RAFAEL -> Icons.Filled.CenterFocusStrong
                CharacterId.A124 -> Icons.Filled.Memory
                CharacterId.ALVARO -> Icons.Filled.LocalFireDepartment
                CharacterId.NIKITA -> Icons.Filled.Speed
                CharacterId.FORD -> Icons.Filled.Security
                else -> Icons.Filled.Stars
            }
            Icon(
                imageVector = icon,
                contentDescription = "${characterId.displayName} emblem",
                tint = characterId.accentColor,
                modifier = Modifier.size(34.dp)
            )
        }

        // Subtle bottom vignette + character initials tag for non-image portraits
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.55f)),
                        startY = 80f
                    )
                )
        )
    }
}

@Composable
fun CurrencyBadge(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(TacticalCardElevated.copy(alpha = 0.9f))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
        )
        Text(
            text = "$label $value",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RankChip(rankTier: String, rankPoints: Int, modifier: Modifier = Modifier) {
    val color = when (rankTier) {
        "Grandmaster" -> AmberGold
        "Heroic" -> BlazingOrange
        "Diamond" -> GlooCyan
        "Platinum" -> EmeraldHeal
        else -> ArmorBlue
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Stars,
            contentDescription = "Rank badge",
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "$rankTier • $rankPoints RP",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
