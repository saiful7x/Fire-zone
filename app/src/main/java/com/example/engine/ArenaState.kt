package com.example.engine

import com.example.model.CharacterId
import com.example.model.GameMode
import com.example.model.WeaponId

enum class MatchPhase {
    SHOPPING,
    LONE_WOLF_PICK,
    ACTIVE_COMBAT,
    ROUND_OVER,
    MATCH_OVER
}

enum class ObstacleType {
    BUILDING,
    CRATE,
    ROCK,
    TREE
}

enum class LootType {
    WEAPON,
    GLOO_WALL,
    MEDKIT,
    VEST,
    HELMET,
    AMMO
}

data class MapObstacle(
    val id: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val type: ObstacleType
)

data class LootDrop(
    val id: Int,
    val x: Float,
    val y: Float,
    val type: LootType,
    val weaponId: WeaponId? = null,
    val label: String
)

data class GlooWallBarrier(
    val id: Int,
    val ownerId: Int,
    val team: Int,
    val centerX: Float,
    val centerY: Float,
    val angleRad: Float,
    val width: Float = 112f,
    val thickness: Float = 22f,
    val hp: Int = 260,
    val maxHp: Int = 260,
    val remainingSec: Float = 18f
)

data class ChronoDome(
    val ownerId: Int,
    val team: Int,
    val x: Float,
    val y: Float,
    val radius: Float = 74f,
    val remainingSec: Float
)

data class AlokAura(
    val ownerId: Int,
    val team: Int,
    val x: Float,
    val y: Float,
    val radius: Float = 130f,
    val remainingSec: Float
)

data class Bullet(
    val id: Int,
    val ownerId: Int,
    val ownerName: String,
    val team: Int,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val damage: Int,
    val headshotMultiplier: Float,
    val headshotChance: Float,
    val weaponName: String,
    val maxRange: Float,
    val traveled: Float = 0f,
    val isSonicWave: Boolean = false,
    val colorHex: Long = 0xFFFFD54F
)

data class DamagePopup(
    val id: Int,
    val x: Float,
    val y: Float,
    val text: String,
    val isHeadshot: Boolean,
    val isHeal: Boolean = false,
    val ttlSec: Float = 0.85f
)

data class KillFeedItem(
    val id: Int,
    val killerName: String,
    val victimName: String,
    val weaponName: String,
    val isHeadshot: Boolean,
    val killerIsAlly: Boolean,
    val ttlSec: Float = 4.5f
)

data class SafeZone(
    val centerX: Float,
    val centerY: Float,
    val currentRadius: Float,
    val targetRadius: Float,
    val shrinkPhase: Int,
    val countdownSec: Float,
    val damagePerSec: Float
)

enum class DropState {
    IN_PLANE,
    SKYDIVING,
    PARACHUTE,
    GROUNDED
}

data class PlaneState(
    val active: Boolean = false,
    val startX: Float = 0f,
    val startY: Float = 0f,
    val endX: Float = 0f,
    val endY: Float = 0f,
    val x: Float = 0f,
    val y: Float = 0f,
    val altitudeMeters: Float = 46f,
    val headingRad: Float = 0f,
    val progress: Float = 0f
)

data class Combatant(
    val id: Int,
    val name: String,
    val team: Int,
    val isPlayer: Boolean,
    val x: Float,
    val y: Float,
    val altitudeMeters: Float = 0f,
    val dropState: DropState = DropState.GROUNDED,
    val isMoving: Boolean = false,
    val aimAngle: Float = 0f,
    val hp: Float = 200f,
    val maxHp: Float = 200f,
    val ep: Float = 0f,
    val maxEp: Float = 200f,
    val armor: Float = 0f,
    val helmet: Float = 0f,
    val characterId: CharacterId = CharacterId.ALOK,
    val characterLevel: Int = 1,
    val weapon1: WeaponId = WeaponId.USP,
    val weapon2: WeaponId? = null,
    val activeSlot: Int = 0,
    val ammoInMag1: Int = 12,
    val ammoInMag2: Int = 0,
    val reserveAmmo: Int = 90,
    val glooWalls: Int = 2,
    val medkits: Int = 2,
    val reloadSecLeft: Float = 0f,
    val fireCooldownSec: Float = 0f,
    val skillCooldownSec: Float = 0f,
    val skillActiveSec: Float = 0f,
    val medkitChannelSec: Float = 0f,
    val isScoped: Boolean = false,
    val isBushDisguised: Boolean = false,
    val muzzleFlashSec: Float = 0f,
    val kills: Int = 0,
    val headshots: Int = 0,
    val damageDealt: Int = 0,
    val isAlive: Boolean = true,
    val aiStrafeSign: Float = 1f,
    val aiDecisionSec: Float = 0f,
    val aiGoalX: Float = 0f,
    val aiGoalY: Float = 0f,
    val aiJumpProgress: Float = 0.2f,
    val lastShotNoiseSec: Float = 0f
) {
    val activeWeapon: WeaponId
        get() = if (activeSlot == 1 && weapon2 != null) weapon2 else weapon1

    val activeAmmoInMag: Int
        get() = if (activeSlot == 1 && weapon2 != null) ammoInMag2 else ammoInMag1
}

data class MatchSummary(
    val mode: GameMode,
    val victory: Boolean,
    val placement: Int,
    val totalTeamsOrPlayers: Int,
    val kills: Int,
    val headshots: Int,
    val damageDealt: Int,
    val survivalSeconds: Int,
    val playerRoundsWon: Int,
    val enemyRoundsWon: Int,
    val goldEarned: Int,
    val rankDelta: Int
)

data class ArenaState(
    val mode: GameMode = GameMode.BATTLE_ROYALE,
    val phase: MatchPhase = MatchPhase.ACTIVE_COMBAT,
    val mapWidth: Float = 2200f,
    val mapHeight: Float = 2200f,
    val plane: PlaneState = PlaneState(),
    val roundNumber: Int = 1,
    val maxRounds: Int = 5,
    val playerRoundsWon: Int = 0,
    val enemyRoundsWon: Int = 0,
    val phaseTimerSec: Float = 0f,
    val matchElapsedSec: Float = 0f,
    val clashCash: Int = 800,
    val loneWolfLoadoutOptions: List<Pair<WeaponId, WeaponId>> = emptyList(),
    val loneWolfChosenIndex: Int = 0,
    val combatants: List<Combatant> = emptyList(),
    val obstacles: List<MapObstacle> = emptyList(),
    val lootDrops: List<LootDrop> = emptyList(),
    val glooWalls: List<GlooWallBarrier> = emptyList(),
    val chronoDomes: List<ChronoDome> = emptyList(),
    val alokAuras: List<AlokAura> = emptyList(),
    val bullets: List<Bullet> = emptyList(),
    val popups: List<DamagePopup> = emptyList(),
    val killFeed: List<KillFeedItem> = emptyList(),
    val safeZone: SafeZone = SafeZone(1100f, 1100f, 1050f, 750f, 1, 25f, 4f),
    val nearbyLoot: LootDrop? = null,
    val hitmarkerTimerSec: Float = 0f,
    val hitmarkerIsHeadshot: Boolean = false,
    val bannerMessage: String? = null,
    val bannerTimerSec: Float = 0f,
    val summary: MatchSummary? = null
) {
    val player: Combatant?
        get() = combatants.firstOrNull { it.isPlayer }

    val aliveCount: Int
        get() = combatants.count { it.isAlive }

    val allyAliveCount: Int
        get() = combatants.count { it.isAlive && it.team == 0 }

    val enemyAliveCount: Int
        get() = combatants.count { it.isAlive && it.team == 1 }
}
