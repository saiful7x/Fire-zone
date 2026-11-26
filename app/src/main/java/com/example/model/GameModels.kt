package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ArmorBlue
import com.example.ui.theme.BlazingOrange
import com.example.ui.theme.CrimsonStrike
import com.example.ui.theme.EmeraldHeal
import com.example.ui.theme.GlooCyan
import com.example.ui.theme.PurpleMythic

enum class GameMode(
    val title: String,
    val subtitle: String,
    val mapName: String,
    val badge: String,
    val squadLabel: String
) {
    BATTLE_ROYALE(
        title = "Battle Royale",
        subtitle = "12-Player Drop • Loot & Shrinking Safe Zone",
        mapName = "Bermuda Island",
        badge = "BR RANKED",
        squadLabel = "SOLO"
    ),
    CLASH_SQUAD(
        title = "Clash Squad",
        subtitle = "4v4 Tactical Rounds • Buy Shop Economy",
        mapName = "Clock Tower Arena",
        badge = "CS 4v4",
        squadLabel = "4v4 SQUAD"
    ),
    LONE_WOLF(
        title = "Lone Wolf",
        subtitle = "1v1 Iron Cage Duel • Alternating Weapon Pick",
        mapName = "Iron Cage",
        badge = "1v1 DUEL",
        squadLabel = "1v1 DUEL"
    )
}

enum class WeaponCategory(val label: String) {
    PISTOL("Pistol"),
    SMG("SMG"),
    SHOTGUN("Shotgun"),
    AR("Assault Rifle"),
    SNIPER("Sniper")
}

enum class WeaponId(
    val displayName: String,
    val category: WeaponCategory,
    val damage: Int,
    val headshotMultiplier: Float,
    val fireIntervalMs: Long,
    val range: Float,
    val spreadRad: Float,
    val pellets: Int,
    val magSize: Int,
    val reloadMs: Long,
    val bulletSpeed: Float,
    val csPrice: Int,
    val scopeZoom: Float,
    val accentColor: Color
) {
    USP(
        displayName = "USP-2",
        category = WeaponCategory.PISTOL,
        damage = 22,
        headshotMultiplier = 2.0f,
        fireIntervalMs = 240L,
        range = 420f,
        spreadRad = 0.04f,
        pellets = 1,
        magSize = 12,
        reloadMs = 1100L,
        bulletSpeed = 980f,
        csPrice = 0,
        scopeZoom = 1.15f,
        accentColor = Color(0xFF90A4AE)
    ),
    DESERT_EAGLE(
        displayName = "Desert Eagle",
        category = WeaponCategory.PISTOL,
        damage = 54,
        headshotMultiplier = 2.6f,
        fireIntervalMs = 390L,
        range = 540f,
        spreadRad = 0.025f,
        pellets = 1,
        magSize = 7,
        reloadMs = 1350L,
        bulletSpeed = 1220f,
        csPrice = 800,
        scopeZoom = 1.25f,
        accentColor = AmberGold
    ),
    MP5(
        displayName = "MP5",
        category = WeaponCategory.SMG,
        damage = 23,
        headshotMultiplier = 1.85f,
        fireIntervalMs = 105L,
        range = 430f,
        spreadRad = 0.06f,
        pellets = 1,
        magSize = 30,
        reloadMs = 1400L,
        bulletSpeed = 1050f,
        csPrice = 1100,
        scopeZoom = 1.3f,
        accentColor = GlooCyan
    ),
    MP40(
        displayName = "MP40 Cobra",
        category = WeaponCategory.SMG,
        damage = 21,
        headshotMultiplier = 1.9f,
        fireIntervalMs = 76L,
        range = 360f,
        spreadRad = 0.085f,
        pellets = 1,
        magSize = 24,
        reloadMs = 1350L,
        bulletSpeed = 1020f,
        csPrice = 1400,
        scopeZoom = 1.15f,
        accentColor = CrimsonStrike
    ),
    M1887(
        displayName = "M1887",
        category = WeaponCategory.SHOTGUN,
        damage = 20,
        headshotMultiplier = 1.75f,
        fireIntervalMs = 680L,
        range = 245f,
        spreadRad = 0.22f,
        pellets = 6,
        magSize = 2,
        reloadMs = 1550L,
        bulletSpeed = 960f,
        csPrice = 1500,
        scopeZoom = 1.1f,
        accentColor = BlazingOrange
    ),
    SCAR(
        displayName = "SCAR-L",
        category = WeaponCategory.AR,
        damage = 31,
        headshotMultiplier = 2.0f,
        fireIntervalMs = 135L,
        range = 620f,
        spreadRad = 0.045f,
        pellets = 1,
        magSize = 30,
        reloadMs = 1600L,
        bulletSpeed = 1280f,
        csPrice = 1600,
        scopeZoom = 1.55f,
        accentColor = AmberGold
    ),
    AK47(
        displayName = "AK47 Draco",
        category = WeaponCategory.AR,
        damage = 37,
        headshotMultiplier = 2.15f,
        fireIntervalMs = 160L,
        range = 650f,
        spreadRad = 0.062f,
        pellets = 1,
        magSize = 30,
        reloadMs = 1750L,
        bulletSpeed = 1300f,
        csPrice = 1800,
        scopeZoom = 1.55f,
        accentColor = CrimsonStrike
    ),
    AWM(
        displayName = "AWM",
        category = WeaponCategory.SNIPER,
        damage = 115,
        headshotMultiplier = 2.5f,
        fireIntervalMs = 1050L,
        range = 980f,
        spreadRad = 0.008f,
        pellets = 1,
        magSize = 5,
        reloadMs = 2000L,
        bulletSpeed = 1900f,
        csPrice = 2400,
        scopeZoom = 2.1f,
        accentColor = PurpleMythic
    )
}

enum class CharacterId(
    val displayName: String,
    val title: String,
    val skillName: String,
    val skillType: String,
    val shortSummary: String,
    val detailedEffect: String,
    val baseCooldownSec: Float,
    val baseDurationSec: Float,
    val unlockCostGold: Int,
    val accentColor: Color
) {
    ALOK(
        displayName = "Alok",
        title = "DJ Pulse",
        skillName = "Drop the Beat",
        skillType = "ACTIVE",
        shortSummary = "5m Aura: +5 HP/s & +18% Move Speed",
        detailedEffect = "Creates a tactical sound aura around you that heals +5 HP/sec and boosts movement speed by 18% for you and nearby allies.",
        baseCooldownSec = 32f,
        baseDurationSec = 8f,
        unlockCostGold = 0,
        accentColor = AmberGold
    ),
    CHRONO(
        displayName = "Chrono",
        title = "Time Turner",
        skillName = "Force Field",
        skillType = "ACTIVE",
        shortSummary = "Impenetrable Dome Shield (Blocks Enemy Fire)",
        detailedEffect = "Projects an energy dome shield that blocks all incoming enemy bullets while letting you fire outward from inside.",
        baseCooldownSec = 38f,
        baseDurationSec = 5f,
        unlockCostGold = 1200,
        accentColor = GlooCyan
    ),
    SKYLER(
        displayName = "Skyler",
        title = "Riptide Rhythm",
        skillName = "Sonic Wave",
        skillType = "ACTIVE + PASSIVE",
        shortSummary = "Shatters Gloo Walls + Heals +35 HP per Gloo Wall",
        detailedEffect = "Unleashes a forward sonic blast that instantly destroys enemy Gloo Walls. Passively heals +35 HP every time you deploy a Gloo Wall.",
        baseCooldownSec = 28f,
        baseDurationSec = 1.2f,
        unlockCostGold = 1500,
        accentColor = CrimsonStrike
    ),
    WUKONG(
        displayName = "Wukong",
        title = "Monkey King",
        skillName = "Camouflage",
        skillType = "ACTIVE",
        shortSummary = "Bush Stealth (Resets Cooldown on Elimination)",
        detailedEffect = "Transforms into a tactical bush, breaking enemy bot target lock until you fire. Eliminating an enemy immediately resets cooldown.",
        baseCooldownSec = 35f,
        baseDurationSec = 6f,
        unlockCostGold = 1400,
        accentColor = EmeraldHeal
    ),
    KELLY(
        displayName = "Kelly",
        title = "The Swift",
        skillName = "Deadly Velocity",
        skillType = "ACTIVE + PASSIVE",
        shortSummary = "+16% Permanent Speed & +25% Burst Shot",
        detailedEffect = "Always moves 16% faster across the map. Activating Deadly Velocity grants a 5s adrenaline surge boosting damage by 25%.",
        baseCooldownSec = 24f,
        baseDurationSec = 5f,
        unlockCostGold = 800,
        accentColor = BlazingOrange
    ),
    K_MASTER(
        displayName = "K",
        title = "Master of All",
        skillName = "Jiu-Jitsu",
        skillType = "ACTIVE + PASSIVE",
        shortSummary = "+50 Max EP & Rapid EP-to-HP Conversion",
        detailedEffect = "Increases Max EP by 50 and steadily regenerates EP. Activating Jiu-Jitsu converts EP into HP at 6 HP/sec for rapid recovery.",
        baseCooldownSec = 20f,
        baseDurationSec = 7f,
        unlockCostGold = 1800,
        accentColor = ArmorBlue
    )
}

enum class AimPrecision(val label: String, val desc: String) {
    DEFAULT("Default", "Balanced auto-aim assist inside crosshair cone"),
    PRECISE_ON_SCOPE("Precise on Scope", "Manual precision when scoped; assist on hip-fire"),
    FULL_CONTROL("Full Control", "Zero auto-aim assist — pure manual skill")
}

enum class LeftFireMode(val label: String) {
    ALWAYS("Always"),
    SCOPE_ONLY("Scope Only")
}

enum class GraphicsQuality(val label: String) {
    SMOOTH("Smooth"),
    STANDARD("Standard"),
    ULTRA("Ultra")
}

data class GameSettings(
    val generalSens: Int = 78,
    val redDotSens: Int = 72,
    val scope2xSens: Int = 65,
    val scope4xSens: Int = 55,
    val sniperSens: Int = 45,
    val freeLookSens: Int = 70,
    val aimPrecision: AimPrecision = AimPrecision.DEFAULT,
    val leftFireMode: LeftFireMode = LeftFireMode.SCOPE_ONLY,
    val quickWeaponSwitch: Boolean = true,
    val quickReload: Boolean = true,
    val smartGlooWall: Boolean = true,
    val fireButtonScale: Float = 1.0f,
    val glooButtonScale: Float = 1.0f,
    val hudOpacity: Float = 0.88f,
    val graphicsQuality: GraphicsQuality = GraphicsQuality.ULTRA,
    val highFps: Boolean = true,
    val newDamageIndicator: Boolean = true,
    val masterVolume: Int = 85,
    val sfxVolume: Int = 90,
    val uiAudioEnabled: Boolean = true
)

data class PlayerProfile(
    val callsign: String = "STRIKER_01",
    val gold: Int = 2200,
    val diamonds: Int = 150,
    val rankPoints: Int = 1250,
    val totalKills: Int = 0,
    val totalWins: Int = 0,
    val matchesPlayed: Int = 0,
    val selectedCharacter: CharacterId = CharacterId.ALOK,
    val unlockedCharacters: Set<CharacterId> = setOf(CharacterId.ALOK),
    val characterLevels: Map<CharacterId, Int> = CharacterId.entries.associateWith { 1 }
) {
    val rankTier: String
        get() = when {
            rankPoints >= 3200 -> "Grandmaster"
            rankPoints >= 2600 -> "Heroic"
            rankPoints >= 2100 -> "Diamond"
            rankPoints >= 1700 -> "Platinum"
            rankPoints >= 1300 -> "Gold"
            rankPoints >= 1000 -> "Silver"
            else -> "Bronze"
        }

    fun levelOf(id: CharacterId): Int = characterLevels[id] ?: 1
}

data class AppDataState(
    val profile: PlayerProfile = PlayerProfile(),
    val settings: GameSettings = GameSettings(),
    val isLoaded: Boolean = false
)
