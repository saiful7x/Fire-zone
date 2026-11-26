package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AimPrecision
import com.example.model.AppDataState
import com.example.model.CharacterId
import com.example.model.GameSettings
import com.example.model.GraphicsQuality
import com.example.model.LeftFireMode
import com.example.model.PlayerProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.fireZoneDataStore: DataStore<Preferences> by preferencesDataStore(name = "firezone_prefs")

class GameRepository(private val context: Context) {

    private object Keys {
        val CALLSIGN = stringPreferencesKey("callsign")
        val GOLD = intPreferencesKey("gold")
        val DIAMONDS = intPreferencesKey("diamonds")
        val RANK_POINTS = intPreferencesKey("rank_points")
        val TOTAL_KILLS = intPreferencesKey("total_kills")
        val TOTAL_WINS = intPreferencesKey("total_wins")
        val MATCHES_PLAYED = intPreferencesKey("matches_played")
        val SELECTED_CHAR = stringPreferencesKey("selected_char")
        val UNLOCKED_CHARS = stringPreferencesKey("unlocked_chars")
        val CHAR_LEVELS = stringPreferencesKey("char_levels")

        val GENERAL_SENS = intPreferencesKey("general_sens")
        val RED_DOT_SENS = intPreferencesKey("red_dot_sens")
        val SCOPE_2X_SENS = intPreferencesKey("scope_2x_sens")
        val SCOPE_4X_SENS = intPreferencesKey("scope_4x_sens")
        val SNIPER_SENS = intPreferencesKey("sniper_sens")
        val FREE_LOOK_SENS = intPreferencesKey("free_look_sens")
        val AIM_PRECISION = stringPreferencesKey("aim_precision")
        val LEFT_FIRE_MODE = stringPreferencesKey("left_fire_mode")
        val QUICK_WEAPON_SWITCH = booleanPreferencesKey("quick_weapon_switch")
        val QUICK_RELOAD = booleanPreferencesKey("quick_reload")
        val SMART_GLOO_WALL = booleanPreferencesKey("smart_gloo_wall")
        val FIRE_BUTTON_SCALE = floatPreferencesKey("fire_button_scale")
        val GLOO_BUTTON_SCALE = floatPreferencesKey("gloo_button_scale")
        val HUD_OPACITY = floatPreferencesKey("hud_opacity")
        val GRAPHICS_QUALITY = stringPreferencesKey("graphics_quality")
        val HIGH_FPS = booleanPreferencesKey("high_fps")
        val NEW_DAMAGE_INDICATOR = booleanPreferencesKey("new_damage_indicator")
        val MASTER_VOLUME = intPreferencesKey("master_volume")
        val SFX_VOLUME = intPreferencesKey("sfx_volume")
        val UI_AUDIO_ENABLED = booleanPreferencesKey("ui_audio_enabled")
    }

    val appDataFlow: Flow<AppDataState> = context.fireZoneDataStore.data.map { prefs ->
        val unlockedRaw = prefs[Keys.UNLOCKED_CHARS] ?: CharacterId.ALOK.name
        val unlocked = unlockedRaw.split(",")
            .mapNotNull { name -> runCatching { CharacterId.valueOf(name) }.getOrNull() }
            .toSet()
            .ifEmpty { setOf(CharacterId.ALOK) }

        val levelsRaw = prefs[Keys.CHAR_LEVELS] ?: ""
        val parsedLevels = CharacterId.entries.associateWith { 1 }.toMutableMap()
        levelsRaw.split(";").forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val cid = runCatching { CharacterId.valueOf(parts[0]) }.getOrNull()
                val lvl = parts[1].toIntOrNull()
                if (cid != null && lvl != null) {
                    parsedLevels[cid] = lvl.coerceIn(1, 6)
                }
            }
        }

        val selectedChar = prefs[Keys.SELECTED_CHAR]?.let {
            runCatching { CharacterId.valueOf(it) }.getOrNull()
        }?.takeIf { it in unlocked } ?: CharacterId.ALOK

        val profile = PlayerProfile(
            callsign = prefs[Keys.CALLSIGN] ?: "STRIKER_01",
            gold = prefs[Keys.GOLD] ?: 2200,
            diamonds = prefs[Keys.DIAMONDS] ?: 150,
            rankPoints = prefs[Keys.RANK_POINTS] ?: 1250,
            totalKills = prefs[Keys.TOTAL_KILLS] ?: 0,
            totalWins = prefs[Keys.TOTAL_WINS] ?: 0,
            matchesPlayed = prefs[Keys.MATCHES_PLAYED] ?: 0,
            selectedCharacter = selectedChar,
            unlockedCharacters = unlocked,
            characterLevels = parsedLevels
        )

        val settings = GameSettings(
            generalSens = prefs[Keys.GENERAL_SENS] ?: 78,
            redDotSens = prefs[Keys.RED_DOT_SENS] ?: 72,
            scope2xSens = prefs[Keys.SCOPE_2X_SENS] ?: 65,
            scope4xSens = prefs[Keys.SCOPE_4X_SENS] ?: 55,
            sniperSens = prefs[Keys.SNIPER_SENS] ?: 45,
            freeLookSens = prefs[Keys.FREE_LOOK_SENS] ?: 70,
            aimPrecision = prefs[Keys.AIM_PRECISION]?.let {
                runCatching { AimPrecision.valueOf(it) }.getOrNull()
            } ?: AimPrecision.DEFAULT,
            leftFireMode = prefs[Keys.LEFT_FIRE_MODE]?.let {
                runCatching { LeftFireMode.valueOf(it) }.getOrNull()
            } ?: LeftFireMode.SCOPE_ONLY,
            quickWeaponSwitch = prefs[Keys.QUICK_WEAPON_SWITCH] ?: true,
            quickReload = prefs[Keys.QUICK_RELOAD] ?: true,
            smartGlooWall = prefs[Keys.SMART_GLOO_WALL] ?: true,
            fireButtonScale = prefs[Keys.FIRE_BUTTON_SCALE] ?: 1.0f,
            glooButtonScale = prefs[Keys.GLOO_BUTTON_SCALE] ?: 1.0f,
            hudOpacity = prefs[Keys.HUD_OPACITY] ?: 0.88f,
            graphicsQuality = prefs[Keys.GRAPHICS_QUALITY]?.let {
                runCatching { GraphicsQuality.valueOf(it) }.getOrNull()
            } ?: GraphicsQuality.ULTRA,
            highFps = prefs[Keys.HIGH_FPS] ?: true,
            newDamageIndicator = prefs[Keys.NEW_DAMAGE_INDICATOR] ?: true,
            masterVolume = prefs[Keys.MASTER_VOLUME] ?: 85,
            sfxVolume = prefs[Keys.SFX_VOLUME] ?: 90,
            uiAudioEnabled = prefs[Keys.UI_AUDIO_ENABLED] ?: true
        )

        AppDataState(profile = profile, settings = settings, isLoaded = true)
    }

    suspend fun updateSettings(transform: (GameSettings) -> GameSettings) {
        context.fireZoneDataStore.edit { prefs ->
            val current = GameSettings(
                generalSens = prefs[Keys.GENERAL_SENS] ?: 78,
                redDotSens = prefs[Keys.RED_DOT_SENS] ?: 72,
                scope2xSens = prefs[Keys.SCOPE_2X_SENS] ?: 65,
                scope4xSens = prefs[Keys.SCOPE_4X_SENS] ?: 55,
                sniperSens = prefs[Keys.SNIPER_SENS] ?: 45,
                freeLookSens = prefs[Keys.FREE_LOOK_SENS] ?: 70,
                aimPrecision = prefs[Keys.AIM_PRECISION]?.let {
                    runCatching { AimPrecision.valueOf(it) }.getOrNull()
                } ?: AimPrecision.DEFAULT,
                leftFireMode = prefs[Keys.LEFT_FIRE_MODE]?.let {
                    runCatching { LeftFireMode.valueOf(it) }.getOrNull()
                } ?: LeftFireMode.SCOPE_ONLY,
                quickWeaponSwitch = prefs[Keys.QUICK_WEAPON_SWITCH] ?: true,
                quickReload = prefs[Keys.QUICK_RELOAD] ?: true,
                smartGlooWall = prefs[Keys.SMART_GLOO_WALL] ?: true,
                fireButtonScale = prefs[Keys.FIRE_BUTTON_SCALE] ?: 1.0f,
                glooButtonScale = prefs[Keys.GLOO_BUTTON_SCALE] ?: 1.0f,
                hudOpacity = prefs[Keys.HUD_OPACITY] ?: 0.88f,
                graphicsQuality = prefs[Keys.GRAPHICS_QUALITY]?.let {
                    runCatching { GraphicsQuality.valueOf(it) }.getOrNull()
                } ?: GraphicsQuality.ULTRA,
                highFps = prefs[Keys.HIGH_FPS] ?: true,
                newDamageIndicator = prefs[Keys.NEW_DAMAGE_INDICATOR] ?: true,
                masterVolume = prefs[Keys.MASTER_VOLUME] ?: 85,
                sfxVolume = prefs[Keys.SFX_VOLUME] ?: 90,
                uiAudioEnabled = prefs[Keys.UI_AUDIO_ENABLED] ?: true
            )
            val updated = transform(current)
            prefs[Keys.GENERAL_SENS] = updated.generalSens.coerceIn(0, 100)
            prefs[Keys.RED_DOT_SENS] = updated.redDotSens.coerceIn(0, 100)
            prefs[Keys.SCOPE_2X_SENS] = updated.scope2xSens.coerceIn(0, 100)
            prefs[Keys.SCOPE_4X_SENS] = updated.scope4xSens.coerceIn(0, 100)
            prefs[Keys.SNIPER_SENS] = updated.sniperSens.coerceIn(0, 100)
            prefs[Keys.FREE_LOOK_SENS] = updated.freeLookSens.coerceIn(0, 100)
            prefs[Keys.AIM_PRECISION] = updated.aimPrecision.name
            prefs[Keys.LEFT_FIRE_MODE] = updated.leftFireMode.name
            prefs[Keys.QUICK_WEAPON_SWITCH] = updated.quickWeaponSwitch
            prefs[Keys.QUICK_RELOAD] = updated.quickReload
            prefs[Keys.SMART_GLOO_WALL] = updated.smartGlooWall
            prefs[Keys.FIRE_BUTTON_SCALE] = updated.fireButtonScale.coerceIn(0.75f, 1.35f)
            prefs[Keys.GLOO_BUTTON_SCALE] = updated.glooButtonScale.coerceIn(0.75f, 1.35f)
            prefs[Keys.HUD_OPACITY] = updated.hudOpacity.coerceIn(0.4f, 1.0f)
            prefs[Keys.GRAPHICS_QUALITY] = updated.graphicsQuality.name
            prefs[Keys.HIGH_FPS] = updated.highFps
            prefs[Keys.NEW_DAMAGE_INDICATOR] = updated.newDamageIndicator
            prefs[Keys.MASTER_VOLUME] = updated.masterVolume.coerceIn(0, 100)
            prefs[Keys.SFX_VOLUME] = updated.sfxVolume.coerceIn(0, 100)
            prefs[Keys.UI_AUDIO_ENABLED] = updated.uiAudioEnabled
        }
    }

    suspend fun updateCallsign(callsign: String) {
        val sanitized = callsign.trim().take(16).ifEmpty { "STRIKER_01" }
        context.fireZoneDataStore.edit { prefs ->
            prefs[Keys.CALLSIGN] = sanitized
        }
    }

    suspend fun selectCharacter(characterId: CharacterId) {
        context.fireZoneDataStore.edit { prefs ->
            prefs[Keys.SELECTED_CHAR] = characterId.name
        }
    }

    suspend fun unlockCharacter(characterId: CharacterId): Boolean {
        var success = false
        context.fireZoneDataStore.edit { prefs ->
            val currentGold = prefs[Keys.GOLD] ?: 2200
            val unlockedRaw = prefs[Keys.UNLOCKED_CHARS] ?: CharacterId.ALOK.name
            val unlocked = unlockedRaw.split(",").filter { it.isNotBlank() }.toMutableSet()
            if (characterId.name in unlocked) {
                prefs[Keys.SELECTED_CHAR] = characterId.name
                success = true
            } else if (currentGold >= characterId.unlockCostGold) {
                prefs[Keys.GOLD] = currentGold - characterId.unlockCostGold
                unlocked.add(characterId.name)
                prefs[Keys.UNLOCKED_CHARS] = unlocked.joinToString(",")
                prefs[Keys.SELECTED_CHAR] = characterId.name
                success = true
            }
        }
        return success
    }

    suspend fun upgradeCharacter(characterId: CharacterId): Boolean {
        var upgraded = false
        context.fireZoneDataStore.edit { prefs ->
            val currentGold = prefs[Keys.GOLD] ?: 2200
            val levelsRaw = prefs[Keys.CHAR_LEVELS] ?: ""
            val map = CharacterId.entries.associateWith { 1 }.toMutableMap()
            levelsRaw.split(";").forEach { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val cid = runCatching { CharacterId.valueOf(parts[0]) }.getOrNull()
                    val lvl = parts[1].toIntOrNull()
                    if (cid != null && lvl != null) map[cid] = lvl
                }
            }
            val currentLvl = map[characterId] ?: 1
            val upgradeCost = currentLvl * 350
            if (currentLvl < 6 && currentGold >= upgradeCost) {
                prefs[Keys.GOLD] = currentGold - upgradeCost
                map[characterId] = currentLvl + 1
                prefs[Keys.CHAR_LEVELS] = map.entries.joinToString(";") { "${it.key.name}:${it.value}" }
                upgraded = true
            }
        }
        return upgraded
    }

    suspend fun recordMatchResult(kills: Int, won: Boolean, goldEarned: Int, rankDelta: Int) {
        context.fireZoneDataStore.edit { prefs ->
            val currentGold = prefs[Keys.GOLD] ?: 2200
            val currentRank = prefs[Keys.RANK_POINTS] ?: 1250
            val currentKills = prefs[Keys.TOTAL_KILLS] ?: 0
            val currentWins = prefs[Keys.TOTAL_WINS] ?: 0
            val currentMatches = prefs[Keys.MATCHES_PLAYED] ?: 0

            prefs[Keys.GOLD] = currentGold + goldEarned
            prefs[Keys.RANK_POINTS] = (currentRank + rankDelta).coerceAtLeast(800)
            prefs[Keys.TOTAL_KILLS] = currentKills + kills
            prefs[Keys.TOTAL_WINS] = currentWins + if (won) 1 else 0
            prefs[Keys.MATCHES_PLAYED] = currentMatches + 1
        }
    }
}
