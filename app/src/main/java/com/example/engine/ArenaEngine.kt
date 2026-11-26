package com.example.engine

import com.example.audio.TacticalSoundEngine
import com.example.model.AimPrecision
import com.example.model.CharacterId
import com.example.model.GameMode
import com.example.model.GameSettings
import com.example.model.PlayerProfile
import com.example.model.WeaponId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class ArenaEngine(
    private val soundEngine: TacticalSoundEngine
) {
    private var nextId = 100
    private fun allocId(): Int = ++nextId

    private val botNames = listOf(
        "VIPER_X", "RAIDEN", "PHANTOM", "BLITZ_99", "KOBRA",
        "NOVA_FF", "REAPER", "SHADOW", "TITAN", "CYBER_Z", "FROST", "VENOM"
    )

    fun createMatch(
        mode: GameMode,
        profile: PlayerProfile
    ): ArenaState {
        nextId = 100
        return when (mode) {
            GameMode.BATTLE_ROYALE -> createBattleRoyaleState(profile)
            GameMode.CLASH_SQUAD -> createClashSquadState(profile, round = 1, cash = 800, pWins = 0, eWins = 0, existingPlayer = null)
            GameMode.LONE_WOLF -> createLoneWolfState(profile, round = 1, pWins = 0, eWins = 0)
        }
    }

    private fun createBattleRoyaleState(profile: PlayerProfile): ArenaState {
        val mapW = 2200f
        val mapH = 2200f
        val obstacles = generateBermudaObstacles(mapW, mapH)
        val loot = generateBattleRoyaleLoot(mapW, mapH, obstacles)

        val startX = 240f
        val startY = mapH - 240f
        val endX = mapW - 240f
        val endY = 240f
        val heading = atan2(endY - startY, endX - startX)

        val playerChar = profile.selectedCharacter
        val player = Combatant(
            id = 1,
            name = profile.callsign,
            team = 0,
            isPlayer = true,
            x = startX,
            y = startY,
            altitudeMeters = 46f,
            dropState = DropState.IN_PLANE,
            aimAngle = heading,
            hp = 200f,
            maxHp = 200f,
            ep = if (playerChar == CharacterId.K_MASTER) 70f else 40f,
            maxEp = if (playerChar == CharacterId.K_MASTER) 250f else 200f,
            armor = 25f,
            helmet = 25f,
            characterId = playerChar,
            characterLevel = profile.levelOf(playerChar),
            weapon1 = WeaponId.USP,
            weapon2 = null,
            ammoInMag1 = WeaponId.USP.magSize,
            reserveAmmo = 120,
            glooWalls = 2,
            medkits = 2
        )

        val bots = (0 until 11).map { idx ->
            val starterWeapon = listOf(WeaponId.MP5, WeaponId.SCAR, WeaponId.MP40, WeaponId.M1887, WeaponId.AK47).random()
            val botChar = CharacterId.entries.random()
            val jumpProg = 0.08f + idx * 0.075f
            Combatant(
                id = 10 + idx,
                name = botNames[idx % botNames.size],
                team = 10 + idx,
                isPlayer = false,
                x = startX,
                y = startY,
                altitudeMeters = 46f,
                dropState = DropState.IN_PLANE,
                aimAngle = Random.nextFloat() * 2f * PI.toFloat(),
                hp = 180f,
                maxHp = 200f,
                armor = 30f,
                helmet = 25f,
                characterId = botChar,
                characterLevel = 2,
                weapon1 = starterWeapon,
                weapon2 = WeaponId.DESERT_EAGLE,
                ammoInMag1 = starterWeapon.magSize,
                ammoInMag2 = WeaponId.DESERT_EAGLE.magSize,
                reserveAmmo = 180,
                glooWalls = 2,
                medkits = 2,
                aiGoalX = mapW * 0.35f + Random.nextFloat() * (mapW * 0.3f),
                aiGoalY = mapH * 0.35f + Random.nextFloat() * (mapH * 0.3f),
                aiJumpProgress = jumpProg.coerceIn(0.06f, 0.88f)
            )
        }

        return ArenaState(
            mode = GameMode.BATTLE_ROYALE,
            phase = MatchPhase.ACTIVE_COMBAT,
            mapWidth = mapW,
            mapHeight = mapH,
            plane = PlaneState(
                active = true,
                startX = startX,
                startY = startY,
                endX = endX,
                endY = endY,
                x = startX,
                y = startY,
                altitudeMeters = 46f,
                headingRad = heading,
                progress = 0f
            ),
            combatants = listOf(player) + bots,
            obstacles = obstacles,
            lootDrops = loot,
            safeZone = SafeZone(
                centerX = mapW * 0.5f,
                centerY = mapH * 0.5f,
                currentRadius = 1120f,
                targetRadius = 740f,
                shrinkPhase = 1,
                countdownSec = 18f,
                damagePerSec = 5f
            ),
            bannerMessage = "BERMUDA DROP — SURVIVE & BOOYAH!",
            bannerTimerSec = 2.8f
        )
    }

    private fun createClashSquadState(
        profile: PlayerProfile,
        round: Int,
        cash: Int,
        pWins: Int,
        eWins: Int,
        existingPlayer: Combatant?
    ): ArenaState {
        val mapW = 1400f
        val mapH = 1050f
        val obstacles = generateClashSquadObstacles(mapW, mapH)
        val pChar = profile.selectedCharacter

        val baseW1 = existingPlayer?.weapon1 ?: WeaponId.USP
        val baseW2 = existingPlayer?.weapon2
        val player = Combatant(
            id = 1,
            name = profile.callsign,
            team = 0,
            isPlayer = true,
            x = 180f,
            y = mapH * 0.5f,
            aimAngle = 0f,
            hp = 200f,
            maxHp = 200f,
            ep = if (pChar == CharacterId.K_MASTER) 90f else 50f,
            maxEp = if (pChar == CharacterId.K_MASTER) 250f else 200f,
            armor = existingPlayer?.armor ?: 0f,
            helmet = existingPlayer?.helmet ?: 0f,
            characterId = pChar,
            characterLevel = profile.levelOf(pChar),
            weapon1 = baseW1,
            weapon2 = baseW2,
            activeSlot = 0,
            ammoInMag1 = baseW1.magSize,
            ammoInMag2 = baseW2?.magSize ?: 0,
            reserveAmmo = 180,
            glooWalls = (existingPlayer?.glooWalls ?: 1).coerceAtLeast(1),
            medkits = (existingPlayer?.medkits ?: 2).coerceAtLeast(1),
            kills = existingPlayer?.kills ?: 0,
            headshots = existingPlayer?.headshots ?: 0,
            damageDealt = existingPlayer?.damageDealt ?: 0
        )

        val roundBotWeapon = when (round) {
            1 -> WeaponId.USP
            2 -> listOf(WeaponId.MP5, WeaponId.DESERT_EAGLE).random()
            3 -> listOf(WeaponId.MP40, WeaponId.M1887, WeaponId.SCAR).random()
            else -> listOf(WeaponId.AK47, WeaponId.M1887, WeaponId.MP40, WeaponId.AWM).random()
        }

        val allies = (0 until 3).map { i ->
            val w = if (round == 1) WeaponId.USP else listOf(WeaponId.MP5, WeaponId.SCAR, WeaponId.MP40).random()
            Combatant(
                id = 2 + i,
                name = "ALLY_${i + 1}",
                team = 0,
                isPlayer = false,
                x = 160f + (i % 2) * 55f,
                y = mapH * (0.28f + i * 0.22f),
                aimAngle = 0f,
                hp = 200f,
                maxHp = 200f,
                armor = if (round >= 2) 50f else 0f,
                helmet = if (round >= 2) 40f else 0f,
                characterId = CharacterId.entries[i % CharacterId.entries.size],
                weapon1 = w,
                ammoInMag1 = w.magSize,
                reserveAmmo = 180,
                glooWalls = 2,
                medkits = 1,
                aiGoalX = mapW * 0.55f,
                aiGoalY = mapH * (0.3f + i * 0.2f)
            )
        }

        val enemies = (0 until 4).map { i ->
            Combatant(
                id = 20 + i,
                name = "CS_${botNames[i]}",
                team = 1,
                isPlayer = false,
                x = mapW - 180f - (i % 2) * 55f,
                y = mapH * (0.22f + i * 0.18f),
                aimAngle = PI.toFloat(),
                hp = 200f,
                maxHp = 200f,
                armor = if (round >= 2) 50f else 0f,
                helmet = if (round >= 2) 40f else 0f,
                characterId = CharacterId.entries[(i + 2) % CharacterId.entries.size],
                weapon1 = roundBotWeapon,
                ammoInMag1 = roundBotWeapon.magSize,
                reserveAmmo = 180,
                glooWalls = 2,
                medkits = 1,
                aiGoalX = mapW * 0.48f,
                aiGoalY = mapH * (0.25f + i * 0.16f)
            )
        }

        return ArenaState(
            mode = GameMode.CLASH_SQUAD,
            phase = MatchPhase.SHOPPING,
            mapWidth = mapW,
            mapHeight = mapH,
            roundNumber = round,
            maxRounds = 5,
            playerRoundsWon = pWins,
            enemyRoundsWon = eWins,
            phaseTimerSec = 15f,
            clashCash = cash,
            combatants = listOf(player) + allies + enemies,
            obstacles = obstacles,
            safeZone = SafeZone(
                centerX = mapW * 0.5f,
                centerY = mapH * 0.5f,
                currentRadius = 820f,
                targetRadius = 240f,
                shrinkPhase = 1,
                countdownSec = 20f,
                damagePerSec = 10f
            ),
            bannerMessage = "ROUND $round — BUY LOADOUT"
        )
    }

    private fun createLoneWolfState(
        profile: PlayerProfile,
        round: Int,
        pWins: Int,
        eWins: Int,
        prevKills: Int = 0,
        prevHeadshots: Int = 0,
        prevDamage: Int = 0
    ): ArenaState {
        val mapW = 960f
        val mapH = 780f
        val obstacles = generateLoneWolfObstacles(mapW, mapH)
        val pChar = profile.selectedCharacter

        val loadoutOptions = listOf(
            WeaponId.M1887 to WeaponId.DESERT_EAGLE,
            WeaponId.MP40 to WeaponId.SCAR,
            WeaponId.AWM to WeaponId.DESERT_EAGLE,
            WeaponId.AK47 to WeaponId.M1887
        )
        val defaultPair = loadoutOptions.first()

        val player = Combatant(
            id = 1,
            name = profile.callsign,
            team = 0,
            isPlayer = true,
            x = 140f,
            y = mapH * 0.5f,
            aimAngle = 0f,
            hp = 200f,
            maxHp = 200f,
            ep = 80f,
            armor = 75f,
            helmet = 75f,
            characterId = pChar,
            characterLevel = profile.levelOf(pChar),
            weapon1 = defaultPair.first,
            weapon2 = defaultPair.second,
            ammoInMag1 = defaultPair.first.magSize,
            ammoInMag2 = defaultPair.second.magSize,
            reserveAmmo = 180,
            glooWalls = 3,
            medkits = 2,
            kills = prevKills,
            headshots = prevHeadshots,
            damageDealt = prevDamage
        )

        val enemy = Combatant(
            id = 50,
            name = "WOLF_${botNames[round % botNames.size]}",
            team = 1,
            isPlayer = false,
            x = mapW - 140f,
            y = mapH * 0.5f,
            aimAngle = PI.toFloat(),
            hp = 200f,
            maxHp = 200f,
            armor = 75f,
            helmet = 75f,
            characterId = CharacterId.entries[round % CharacterId.entries.size],
            weapon1 = defaultPair.first,
            weapon2 = defaultPair.second,
            ammoInMag1 = defaultPair.first.magSize,
            ammoInMag2 = defaultPair.second.magSize,
            reserveAmmo = 180,
            glooWalls = 3,
            medkits = 2,
            aiGoalX = mapW * 0.5f,
            aiGoalY = mapH * 0.5f
        )

        return ArenaState(
            mode = GameMode.LONE_WOLF,
            phase = MatchPhase.LONE_WOLF_PICK,
            mapWidth = mapW,
            mapHeight = mapH,
            roundNumber = round,
            maxRounds = 5,
            playerRoundsWon = pWins,
            enemyRoundsWon = eWins,
            phaseTimerSec = 10f,
            loneWolfLoadoutOptions = loadoutOptions,
            combatants = listOf(player, enemy),
            obstacles = obstacles,
            safeZone = SafeZone(
                centerX = mapW * 0.5f,
                centerY = mapH * 0.5f,
                currentRadius = 580f,
                targetRadius = 180f,
                shrinkPhase = 1,
                countdownSec = 16f,
                damagePerSec = 12f
            ),
            bannerMessage = "ROUND $round — PICK DUEL WEAPONS"
        )
    }

    fun selectLoneWolfLoadout(state: ArenaState, index: Int): ArenaState {
        if (state.phase != MatchPhase.LONE_WOLF_PICK) return state
        val pair = state.loneWolfLoadoutOptions.getOrNull(index) ?: return state
        val updatedCombatants = state.combatants.map { c ->
            c.copy(
                weapon1 = pair.first,
                weapon2 = pair.second,
                activeSlot = 0,
                ammoInMag1 = pair.first.magSize,
                ammoInMag2 = pair.second.magSize
            )
        }
        soundEngine.playUiClick()
        return state.copy(
            phase = MatchPhase.ACTIVE_COMBAT,
            loneWolfChosenIndex = index,
            combatants = updatedCombatants,
            bannerMessage = "FIGHT! (${pair.first.displayName} + ${pair.second.displayName})",
            bannerTimerSec = 2.0f
        )
    }

    fun buyCsItem(state: ArenaState, item: CsShopItem): ArenaState {
        if (state.phase != MatchPhase.SHOPPING) return state
        val player = state.player ?: return state
        if (state.clashCash < item.price) return state

        val updatedPlayer = when (item) {
            is CsShopItem.Weapon -> {
                if (player.weapon1 == WeaponId.USP && player.weapon2 == null) {
                    player.copy(
                        weapon1 = item.weapon,
                        ammoInMag1 = item.weapon.magSize,
                        activeSlot = 0
                    )
                } else if (player.weapon2 == null) {
                    player.copy(
                        weapon2 = item.weapon,
                        ammoInMag2 = item.weapon.magSize,
                        activeSlot = 1
                    )
                } else {
                    if (player.activeSlot == 0) {
                        player.copy(weapon1 = item.weapon, ammoInMag1 = item.weapon.magSize)
                    } else {
                        player.copy(weapon2 = item.weapon, ammoInMag2 = item.weapon.magSize)
                    }
                }
            }
            CsShopItem.GlooWall -> {
                if (player.glooWalls >= 3) return state
                player.copy(glooWalls = player.glooWalls + 1)
            }
            CsShopItem.VestAndHelmet -> {
                if (player.armor >= 90f && player.helmet >= 90f) return state
                player.copy(armor = 100f, helmet = 100f)
            }
            CsShopItem.Medkit -> {
                if (player.medkits >= 4) return state
                player.copy(medkits = player.medkits + 1)
            }
        }
        soundEngine.playUiClick()
        return state.copy(
            clashCash = state.clashCash - item.price,
            combatants = state.combatants.map { if (it.isPlayer) updatedPlayer else it }
        )
    }

    fun startCombatFromShop(state: ArenaState): ArenaState {
        if (state.phase != MatchPhase.SHOPPING && state.phase != MatchPhase.LONE_WOLF_PICK) return state
        soundEngine.playUiClick()
        return state.copy(
            phase = MatchPhase.ACTIVE_COMBAT,
            bannerMessage = "ROUND ${state.roundNumber} — FIGHT!",
            bannerTimerSec = 1.8f
        )
    }

    fun ejectFromPlane(state: ArenaState): ArenaState {
        val player = state.player ?: return state
        if (player.dropState != DropState.IN_PLANE) return state
        soundEngine.playUiClick()
        return state.copy(
            combatants = state.combatants.map {
                if (it.isPlayer) it.copy(dropState = DropState.SKYDIVING, altitudeMeters = state.plane.altitudeMeters) else it
            },
            bannerMessage = "SKYDIVING — GLIDE TO LOOT!",
            bannerTimerSec = 2.0f
        )
    }

    fun openParachute(state: ArenaState): ArenaState {
        val player = state.player ?: return state
        if (player.dropState != DropState.SKYDIVING) return state
        soundEngine.playUiClick()
        return state.copy(
            combatants = state.combatants.map {
                if (it.isPlayer) it.copy(dropState = DropState.PARACHUTE) else it
            }
        )
    }

    fun deployPlayerGlooWall(state: ArenaState): ArenaState {
        if (state.phase != MatchPhase.ACTIVE_COMBAT) return state
        val player = state.player ?: return state
        if (!player.isAlive || player.glooWalls <= 0 || player.dropState != DropState.GROUNDED) return state
        return deployGlooWallFor(state, player)
    }

    private fun deployGlooWallFor(state: ArenaState, actor: Combatant): ArenaState {
        val dist = 76f
        val gx = (actor.x + cos(actor.aimAngle) * dist).coerceIn(45f, state.mapWidth - 45f)
        val gy = (actor.y + sin(actor.aimAngle) * dist).coerceIn(45f, state.mapHeight - 45f)
        val wall = GlooWallBarrier(
            id = allocId(),
            ownerId = actor.id,
            team = actor.team,
            centerX = gx,
            centerY = gy,
            angleRad = actor.aimAngle,
            hp = 260,
            maxHp = 260,
            remainingSec = 18f
        )

        var updatedActor = actor.copy(glooWalls = (actor.glooWalls - 1).coerceAtLeast(0))
        val popups = state.popups.toMutableList()
        if (actor.characterId == CharacterId.SKYLER) {
            val heal = 35f + (actor.characterLevel - 1) * 4f
            updatedActor = updatedActor.copy(hp = min(updatedActor.maxHp, updatedActor.hp + heal))
            popups.add(DamagePopup(allocId(), actor.x, actor.y - 30f, "+${heal.toInt()} HP", isHeadshot = false, isHeal = true))
        }
        if (actor.isPlayer) {
            soundEngine.playGlooDeploy()
        }
        return state.copy(
            glooWalls = state.glooWalls + wall,
            combatants = state.combatants.map { if (it.id == actor.id) updatedActor else it },
            popups = popups
        )
    }

    fun activatePlayerSkill(state: ArenaState): ArenaState {
        if (state.phase != MatchPhase.ACTIVE_COMBAT) return state
        val player = state.player ?: return state
        if (!player.isAlive || player.skillCooldownSec > 0f) return state
        return activateSkillFor(state, player)
    }

    private fun activateSkillFor(state: ArenaState, actor: Combatant): ArenaState {
        val char = actor.characterId
        val levelBonus = (actor.characterLevel - 1) * 1.2f
        val cd = (char.baseCooldownSec - levelBonus).coerceAtLeast(12f)
        val dur = char.baseDurationSec + (actor.characterLevel - 1) * 0.4f

        val newDomes = state.chronoDomes.toMutableList()
        val newAuras = state.alokAuras.toMutableList()
        val newBullets = state.bullets.toMutableList()
        var updatedActor = actor.copy(
            skillCooldownSec = cd,
            skillActiveSec = dur
        )

        when (char) {
            CharacterId.ALOK -> {
                newAuras.removeAll { it.ownerId == actor.id }
                newAuras.add(AlokAura(actor.id, actor.team, actor.x, actor.y, remainingSec = dur))
            }
            CharacterId.CHRONO -> {
                newDomes.removeAll { it.ownerId == actor.id }
                newDomes.add(ChronoDome(actor.id, actor.team, actor.x, actor.y, remainingSec = dur))
            }
            CharacterId.WUKONG -> {
                updatedActor = updatedActor.copy(isBushDisguised = true)
            }
            CharacterId.SKYLER -> {
                val speed = 1100f
                newBullets.add(
                    Bullet(
                        id = allocId(),
                        ownerId = actor.id,
                        ownerName = actor.name,
                        team = actor.team,
                        x = actor.x + cos(actor.aimAngle) * 28f,
                        y = actor.y + sin(actor.aimAngle) * 28f,
                        vx = cos(actor.aimAngle) * speed,
                        vy = sin(actor.aimAngle) * speed,
                        damage = 25,
                        headshotMultiplier = 1f,
                        headshotChance = 0f,
                        weaponName = "Sonic Wave",
                        maxRange = 540f,
                        isSonicWave = true,
                        colorHex = 0xFFFF2E4C
                    )
                )
            }
            CharacterId.A124 -> {
                val conv = min(updatedActor.ep, 45f)
                updatedActor = updatedActor.copy(
                    ep = (updatedActor.ep - conv).coerceAtLeast(0f),
                    hp = min(updatedActor.maxHp, updatedActor.hp + conv)
                )
            }
            CharacterId.FORD -> {
                updatedActor = updatedActor.copy(
                    hp = min(updatedActor.maxHp, updatedActor.hp + 25f),
                    armor = min(100f, updatedActor.armor + 25f)
                )
            }
            CharacterId.KELLY, CharacterId.K_MASTER, CharacterId.MOCO,
            CharacterId.RAFAEL, CharacterId.ALVARO, CharacterId.NIKITA -> Unit
        }

        if (actor.isPlayer) soundEngine.playSkillActivate()

        return state.copy(
            combatants = state.combatants.map { if (it.id == actor.id) updatedActor else it },
            chronoDomes = newDomes,
            alokAuras = newAuras,
            bullets = newBullets
        )
    }

    fun togglePlayerScope(state: ArenaState): ArenaState {
        val player = state.player ?: return state
        return state.copy(
            combatants = state.combatants.map { if (it.isPlayer) it.copy(isScoped = !it.isScoped, isSprinting = false) else it }
        )
    }

    fun jumpPlayer(state: ArenaState): ArenaState {
        if (state.phase != MatchPhase.ACTIVE_COMBAT) return state
        val player = state.player ?: return state
        if (!player.isAlive || player.dropState != DropState.GROUNDED || player.jumpHeightMeters > 0.05f) return state
        return state.copy(
            combatants = state.combatants.map {
                if (it.isPlayer) it.copy(jumpVelocity = 6.8f, isCrouching = false) else it
            }
        )
    }

    fun togglePlayerCrouch(state: ArenaState): ArenaState {
        val player = state.player ?: return state
        if (player.dropState != DropState.GROUNDED) return state
        return state.copy(
            combatants = state.combatants.map {
                if (it.isPlayer) it.copy(isCrouching = !it.isCrouching, isSprinting = false) else it
            }
        )
    }

    fun togglePlayerSprint(state: ArenaState): ArenaState {
        val player = state.player ?: return state
        if (player.dropState != DropState.GROUNDED) return state
        return state.copy(
            combatants = state.combatants.map {
                if (it.isPlayer) it.copy(isSprinting = !it.isSprinting, isCrouching = false, isScoped = false) else it
            }
        )
    }

    fun switchPlayerWeapon(state: ArenaState): ArenaState {
        val player = state.player ?: return state
        if (player.weapon2 == null) return state
        val nextSlot = if (player.activeSlot == 0) 1 else 0
        soundEngine.playUiClick()
        return state.copy(
            combatants = state.combatants.map {
                if (it.isPlayer) it.copy(activeSlot = nextSlot, reloadSecLeft = 0f, isScoped = false) else it
            }
        )
    }

    fun reloadPlayerWeapon(state: ArenaState): ArenaState {
        val player = state.player ?: return state
        val w = player.activeWeapon
        if (player.reloadSecLeft > 0f || player.activeAmmoInMag >= w.magSize || player.reserveAmmo <= 0) {
            return state
        }
        val reloadMult = if (player.characterId == CharacterId.NIKITA) 0.65f else 1.0f
        return state.copy(
            combatants = state.combatants.map {
                if (it.isPlayer) it.copy(reloadSecLeft = (w.reloadMs / 1000f) * reloadMult) else it
            }
        )
    }

    fun usePlayerMedkit(state: ArenaState): ArenaState {
        val player = state.player ?: return state
        if (!player.isAlive || player.medkits <= 0 || player.hp >= player.maxHp || player.medkitChannelSec > 0f) {
            return state
        }
        return state.copy(
            combatants = state.combatants.map {
                if (it.isPlayer) it.copy(medkitChannelSec = 1.6f) else it
            }
        )
    }

    fun pickupNearbyLoot(state: ArenaState): ArenaState {
        val player = state.player ?: return state
        val loot = state.nearbyLoot ?: return state
        val updatedPlayer = applyLootToCombatant(player, loot)
        soundEngine.playUiClick()
        return state.copy(
            combatants = state.combatants.map { if (it.isPlayer) updatedPlayer else it },
            lootDrops = state.lootDrops.filterNot { it.id == loot.id },
            nearbyLoot = null
        )
    }

    fun rotatePlayerAim(state: ArenaState, deltaRadians: Float, settings: GameSettings): ArenaState {
        val player = state.player ?: return state
        val weapon = player.activeWeapon
        val sensValue = when {
            !player.isScoped -> settings.generalSens
            weapon == WeaponId.AWM -> settings.sniperSens
            weapon.scopeZoom >= 1.5f -> settings.scope2xSens
            else -> settings.redDotSens
        }
        val multiplier = 0.35f + (sensValue / 100f) * 1.45f
        val newAngle = player.aimAngle + deltaRadians * multiplier
        return state.copy(
            combatants = state.combatants.map { if (it.isPlayer) it.copy(aimAngle = newAngle) else it }
        )
    }

    fun stepSimulation(
        currentState: ArenaState,
        dt: Float,
        moveX: Float,
        moveY: Float,
        isFiring: Boolean,
        settings: GameSettings,
        profile: PlayerProfile,
        onMatchFinished: (MatchSummary) -> Unit
    ): ArenaState {
        var state = currentState
        val clampedDt = dt.coerceIn(0.001f, 0.05f)

        // Shopping or Weapon Pick countdown
        if (state.phase == MatchPhase.SHOPPING || state.phase == MatchPhase.LONE_WOLF_PICK) {
            val nextTimer = state.phaseTimerSec - clampedDt
            return if (nextTimer <= 0f) {
                state.copy(
                    phase = MatchPhase.ACTIVE_COMBAT,
                    phaseTimerSec = 0f,
                    bannerMessage = "ROUND ${state.roundNumber} — FIGHT!",
                    bannerTimerSec = 1.8f
                )
            } else {
                state.copy(phaseTimerSec = nextTimer)
            }
        }

        // Round transition delay
        if (state.phase == MatchPhase.ROUND_OVER) {
            val nextTimer = state.phaseTimerSec - clampedDt
            if (nextTimer <= 0f) {
                val playerWonMatch = state.playerRoundsWon >= 3
                val enemyWonMatch = state.enemyRoundsWon >= 3
                if (playerWonMatch || enemyWonMatch) {
                    val player = state.player
                    val gold = (if (playerWonMatch) 420 else 180) + (player?.kills ?: 0) * 35
                    val rankDelta = if (playerWonMatch) 38 else -14
                    val summary = MatchSummary(
                        mode = state.mode,
                        victory = playerWonMatch,
                        placement = if (playerWonMatch) 1 else 2,
                        totalTeamsOrPlayers = 2,
                        kills = player?.kills ?: 0,
                        headshots = player?.headshots ?: 0,
                        damageDealt = player?.damageDealt ?: 0,
                        survivalSeconds = state.matchElapsedSec.toInt(),
                        playerRoundsWon = state.playerRoundsWon,
                        enemyRoundsWon = state.enemyRoundsWon,
                        goldEarned = gold,
                        rankDelta = rankDelta
                    )
                    onMatchFinished(summary)
                    return state.copy(phase = MatchPhase.MATCH_OVER, summary = summary)
                } else {
                    val nextRound = state.roundNumber + 1
                    val p = state.player
                    return if (state.mode == GameMode.CLASH_SQUAD) {
                        val roundBonus = if (state.bannerMessage?.contains("VICTORY") == true) 1600 else 1100
                        createClashSquadState(
                            profile = profile,
                            round = nextRound,
                            cash = state.clashCash + roundBonus,
                            pWins = state.playerRoundsWon,
                            eWins = state.enemyRoundsWon,
                            existingPlayer = p
                        )
                    } else {
                        createLoneWolfState(
                            profile = profile,
                            round = nextRound,
                            pWins = state.playerRoundsWon,
                            eWins = state.enemyRoundsWon,
                            prevKills = p?.kills ?: 0,
                            prevHeadshots = p?.headshots ?: 0,
                            prevDamage = p?.damageDealt ?: 0
                        )
                    }
                }
            }
            return state.copy(phaseTimerSec = nextTimer)
        }

        if (state.phase == MatchPhase.MATCH_OVER) return state

        // Active Combat Simulation
        val mutableCombatants = state.combatants.toMutableList()
        val mutableBullets = state.bullets.toMutableList()
        val mutableGlooWalls = state.glooWalls.mapNotNull { gw ->
            val rem = gw.remainingSec - clampedDt
            if (rem > 0f && gw.hp > 0) gw.copy(remainingSec = rem) else null
        }.toMutableList()
        val mutableDomes = state.chronoDomes.mapNotNull { d ->
            val rem = d.remainingSec - clampedDt
            if (rem > 0f) d.copy(remainingSec = rem) else null
        }.toMutableList()
        val mutableAuras = state.alokAuras.mapNotNull { a ->
            val rem = a.remainingSec - clampedDt
            if (rem > 0f) a.copy(remainingSec = rem) else null
        }.toMutableList()
        val mutablePopups = state.popups.mapNotNull { p ->
            val rem = p.ttlSec - clampedDt
            if (rem > 0f) p.copy(y = p.y - 24f * clampedDt, ttlSec = rem) else null
        }.toMutableList()
        val mutableKillFeed = state.killFeed.mapNotNull { k ->
            val rem = k.ttlSec - clampedDt
            if (rem > 0f) k.copy(ttlSec = rem) else null
        }.toMutableList()
        val mutableLoot = state.lootDrops.toMutableList()

        // Update C-130 Airplane flight path (Battle Royale)
        var plane = state.plane
        if (plane.active) {
            val nextProg = (plane.progress + clampedDt / 22f).coerceAtMost(1f)
            val px = plane.startX + (plane.endX - plane.startX) * nextProg
            val py = plane.startY + (plane.endY - plane.startY) * nextProg
            plane = plane.copy(
                x = px,
                y = py,
                progress = nextProg,
                active = nextProg < 1f
            )
        }

        // Update Safe Zone
        val sz = updateSafeZone(state.safeZone, clampedDt)

        // Update Combatants (Timers, Flight/Parachute, Movement, Auras, Zone Damage, Shooting, AI)
        for (i in mutableCombatants.indices) {
            var c = mutableCombatants[i]
            if (!c.isAlive) continue

            // Cooldowns & active timers
            val newFireCd = (c.fireCooldownSec - clampedDt).coerceAtLeast(0f)
            val newSkillCd = (c.skillCooldownSec - clampedDt).coerceAtLeast(0f)
            val newSkillActive = (c.skillActiveSec - clampedDt).coerceAtLeast(0f)
            val newMuzzle = (c.muzzleFlashSec - clampedDt).coerceAtLeast(0f)
            val newNoise = (c.lastShotNoiseSec - clampedDt).coerceAtLeast(0f)
            var newHp = c.hp
            var newEp = c.ep
            var newReload = c.reloadSecLeft
            var ammo1 = c.ammoInMag1
            var ammo2 = c.ammoInMag2
            var reserve = c.reserveAmmo
            var medkits = c.medkits
            var medChannel = c.medkitChannelSec

            if (newReload > 0f) {
                newReload = (newReload - clampedDt).coerceAtLeast(0f)
                if (newReload == 0f) {
                    val w = c.activeWeapon
                    val needed = w.magSize - c.activeAmmoInMag
                    val take = min(needed, reserve)
                    if (c.activeSlot == 0) ammo1 += take else ammo2 += take
                    reserve -= take
                }
            }

            if (medChannel > 0f) {
                medChannel = (medChannel - clampedDt).coerceAtLeast(0f)
                if (medChannel == 0f && medkits > 0) {
                    medkits -= 1
                    val healed = min(c.maxHp - newHp, 75f)
                    newHp += healed
                    mutablePopups.add(DamagePopup(allocId(), c.x, c.y - 26f, "+75 HP", isHeadshot = false, isHeal = true))
                }
            }

            // K passive EP regen & active conversion
            if (c.characterId == CharacterId.K_MASTER) {
                newEp = min(c.maxEp, newEp + 2.5f * clampedDt)
                if (newSkillActive > 0f && newEp > 0f && newHp < c.maxHp) {
                    val conv = min(newEp, 7.5f * clampedDt)
                    newEp -= conv
                    newHp = min(c.maxHp, newHp + conv)
                }
            } else if (newEp > 0f && newHp < c.maxHp) {
                val passiveEp = min(newEp, 1.5f * clampedDt)
                newEp -= passiveEp
                newHp = min(c.maxHp, newHp + passiveEp)
            }

            // Alok Aura heal & speed check
            var inAlokAura = false
            for (idx in mutableAuras.indices) {
                val aura = mutableAuras[idx]
                if (aura.ownerId == c.id) {
                    mutableAuras[idx] = aura.copy(x = c.x, y = c.y)
                }
                if (aura.team == c.team && hypot(c.x - aura.x, c.y - aura.y) <= aura.radius) {
                    inAlokAura = true
                    newHp = min(c.maxHp, newHp + 6f * clampedDt)
                }
            }

            // Chrono dome follows owner
            for (idx in mutableDomes.indices) {
                if (mutableDomes[idx].ownerId == c.id) {
                    mutableDomes[idx] = mutableDomes[idx].copy(x = c.x, y = c.y)
                }
            }

            // Blue Zone Damage (Ford takes 35% less zone damage)
            val distToZone = hypot(c.x - sz.centerX, c.y - sz.centerY)
            if (distToZone > sz.currentRadius) {
                val zoneMult = if (c.characterId == CharacterId.FORD) 0.65f else 1.0f
                newHp -= sz.damagePerSec * zoneMult * clampedDt
            }

            // Jump vertical arc
            var nextJumpH = c.jumpHeightMeters
            var nextJumpV = c.jumpVelocity
            if (nextJumpH > 0f || nextJumpV > 0f) {
                nextJumpH += nextJumpV * clampedDt
                nextJumpV -= 18.5f * clampedDt
                if (nextJumpH <= 0f) {
                    nextJumpH = 0f
                    nextJumpV = 0f
                }
            }

            val bush = if (c.characterId == CharacterId.WUKONG && newSkillActive > 0f) c.isBushDisguised else false
            val nextMarked = (c.markedSec - clampedDt).coerceAtLeast(0f)

            c = c.copy(
                hp = newHp,
                ep = newEp,
                fireCooldownSec = newFireCd,
                skillCooldownSec = newSkillCd,
                skillActiveSec = newSkillActive,
                muzzleFlashSec = newMuzzle,
                lastShotNoiseSec = newNoise,
                reloadSecLeft = newReload,
                ammoInMag1 = ammo1,
                ammoInMag2 = ammo2,
                reserveAmmo = reserve,
                medkits = medkits,
                medkitChannelSec = medChannel,
                jumpHeightMeters = nextJumpH,
                jumpVelocity = nextJumpV,
                markedSec = nextMarked,
                isBushDisguised = bush
            )

            if (c.hp <= 0f) {
                c = c.copy(hp = 0f, isAlive = false)
                mutableKillFeed.add(
                    0,
                    KillFeedItem(
                        id = allocId(),
                        killerName = "BLUE ZONE",
                        victimName = c.name,
                        weaponName = "Zone",
                        isHeadshot = false,
                        killerIsAlly = false
                    )
                )
                mutableCombatants[i] = c
                continue
            }

            // Speed calculation
            var speed = 175f
            if (c.characterId == CharacterId.KELLY) speed *= 1.16f
            if (inAlokAura) speed *= 1.18f
            if (c.isSprinting) speed *= 1.22f
            if (c.isCrouching) speed *= 0.68f
            if (c.isScoped) speed *= 0.65f
            if (c.medkitChannelSec > 0f) speed *= 0.45f

            if (c.dropState == DropState.IN_PLANE) {
                val shouldEject = if (c.isPlayer) plane.progress >= 0.95f || !plane.active else plane.progress >= c.aiJumpProgress
                val nextDrop = if (shouldEject) DropState.SKYDIVING else DropState.IN_PLANE
                c = c.copy(
                    x = plane.x,
                    y = plane.y,
                    altitudeMeters = plane.altitudeMeters,
                    dropState = nextDrop,
                    isMoving = false
                )
                mutableCombatants[i] = c
                continue
            }

            if (c.dropState == DropState.SKYDIVING || c.dropState == DropState.PARACHUTE) {
                val fallRate = if (c.dropState == DropState.SKYDIVING) 7.2f else 3.2f
                val nextAlt = (c.altitudeMeters - fallRate * clampedDt).coerceAtLeast(0f)
                var nextDrop = c.dropState
                if (nextDrop == DropState.SKYDIVING && nextAlt <= 18f) {
                    nextDrop = DropState.PARACHUTE
                }
                if (nextAlt <= 0f) {
                    nextDrop = DropState.GROUNDED
                }
                val glideSpeed = if (nextDrop == DropState.PARACHUTE) 150f else 220f
                var gx = c.x
                var gy = c.y
                if (c.isPlayer) {
                    val mag = hypot(moveX, moveY)
                    val forward = if (mag > 0.05f) -moveY else 0.7f
                    val strafe = if (mag > 0.05f) moveX else 0f
                    val wx = cos(c.aimAngle) * forward - sin(c.aimAngle) * strafe
                    val wy = sin(c.aimAngle) * forward + cos(c.aimAngle) * strafe
                    gx = (gx + wx * glideSpeed * clampedDt).coerceIn(60f, state.mapWidth - 60f)
                    gy = (gy + wy * glideSpeed * clampedDt).coerceIn(60f, state.mapHeight - 60f)
                } else {
                    val dx = c.aiGoalX - c.x
                    val dy = c.aiGoalY - c.y
                    val d = hypot(dx, dy).coerceAtLeast(1f)
                    gx = (gx + (dx / d) * glideSpeed * 0.75f * clampedDt).coerceIn(60f, state.mapWidth - 60f)
                    gy = (gy + (dy / d) * glideSpeed * 0.75f * clampedDt).coerceIn(60f, state.mapHeight - 60f)
                }
                val landedPos = if (nextDrop == DropState.GROUNDED) {
                    pushOutOfObstacles(gx, gy, 20f, state.obstacles, state.mapWidth, state.mapHeight)
                } else gx to gy
                c = c.copy(
                    x = landedPos.first,
                    y = landedPos.second,
                    altitudeMeters = nextAlt,
                    dropState = nextDrop,
                    isMoving = true
                )
                mutableCombatants[i] = c
                continue
            }

            if (c.isPlayer) {
                // Camera/Aim-relative 3D Third-Person Movement (supports Sprint lock)
                val effMoveX = moveX
                val effMoveY = if (hypot(moveX, moveY) <= 0.05f && c.isSprinting) -1.0f else moveY
                val mag = hypot(effMoveX, effMoveY)
                var nx = c.x
                var ny = c.y
                if (mag > 0.05f) {
                    val forward = -effMoveY
                    val strafe = effMoveX
                    val wx = cos(c.aimAngle) * forward - sin(c.aimAngle) * strafe
                    val wy = sin(c.aimAngle) * forward + cos(c.aimAngle) * strafe
                    val wLen = hypot(wx, wy).coerceAtLeast(1f)
                    nx += (wx / wLen) * speed * clampedDt
                    ny += (wy / wLen) * speed * clampedDt
                    val resolved = resolveCollisions(nx, ny, 18f, state.obstacles, mutableGlooWalls, state.mapWidth, state.mapHeight)
                    nx = resolved.first
                    ny = resolved.second
                }
                c = c.copy(x = nx, y = ny, isMoving = mag > 0.05f)

                // Player Firing
                if (isFiring && c.fireCooldownSec <= 0f && c.reloadSecLeft <= 0f) {
                    if (c.activeAmmoInMag > 0) {
                        val fired = performFire(c, mutableCombatants, state.obstacles, mutableGlooWalls, mutableBullets, settings, isPlayer = true)
                        c = fired
                        soundEngine.playWeaponShot(c.activeWeapon)
                    } else if (settings.quickReload && c.reserveAmmo > 0) {
                        val reloadMult = if (c.characterId == CharacterId.NIKITA) 0.65f else 1.0f
                        c = c.copy(reloadSecLeft = (c.activeWeapon.reloadMs / 1000f) * reloadMult)
                    }
                }
            } else {
                // AI Bot Logic
                val prevX = c.x
                val prevY = c.y
                val botResult = stepBotBehavior(
                    bot = c,
                    dt = clampedDt,
                    speed = speed,
                    allCombatants = mutableCombatants,
                    obstacles = state.obstacles,
                    glooWalls = mutableGlooWalls,
                    bullets = mutableBullets,
                    safeZone = sz,
                    mapW = state.mapWidth,
                    mapH = state.mapHeight
                )
                val moved = hypot(botResult.updatedBot.x - prevX, botResult.updatedBot.y - prevY) > 0.5f
                c = botResult.updatedBot.copy(isMoving = moved)
                botResult.spawnedGlooWall?.let { mutableGlooWalls.add(it) }
            }

            mutableCombatants[i] = c
        }

        // Player auto-check nearby loot & auto-pickup ammo/medkit/armor if close
        var nearbyLoot: LootDrop? = null
        val playerIdx = mutableCombatants.indexOfFirst { it.isPlayer }
        if (playerIdx >= 0 && mutableCombatants[playerIdx].isAlive && mutableCombatants[playerIdx].dropState == DropState.GROUNDED) {
            var p = mutableCombatants[playerIdx]
            val toRemove = mutableListOf<Int>()
            for (loot in mutableLoot) {
                val d = hypot(p.x - loot.x, p.y - loot.y)
                if (d < 46f) {
                    if (loot.type != LootType.WEAPON) {
                        p = applyLootToCombatant(p, loot)
                        toRemove.add(loot.id)
                    } else if (p.weapon2 == null && loot.weaponId != p.weapon1) {
                        p = applyLootToCombatant(p, loot)
                        toRemove.add(loot.id)
                    } else {
                        nearbyLoot = loot
                    }
                }
            }
            if (toRemove.isNotEmpty()) {
                mutableLoot.removeAll { it.id in toRemove }
                mutableCombatants[playerIdx] = p
            }
        }

        // Step Bullets & Collisions
        var hitmarkerTimer = (state.hitmarkerTimerSec - clampedDt).coerceAtLeast(0f)
        var hitmarkerHead = state.hitmarkerIsHeadshot

        val survivingBullets = mutableListOf<Bullet>()
        for (b in mutableBullets) {
            val stepX = b.vx * clampedDt
            val stepY = b.vy * clampedDt
            val nextX = b.x + stepX
            val nextY = b.y + stepY
            val distStep = hypot(stepX, stepY)
            val newTraveled = b.traveled + distStep

            if (nextX < 0f || nextX > state.mapWidth || nextY < 0f || nextY > state.mapHeight || newTraveled > b.maxRange) {
                continue
            }

            // Sonic Wave destroys Gloo Walls!
            if (b.isSonicWave) {
                var shattered = false
                mutableGlooWalls.removeAll { gw ->
                    if (gw.team != b.team && pointCollidesGlooWall(nextX, nextY, 26f, gw)) {
                        shattered = true
                        mutablePopups.add(DamagePopup(allocId(), gw.centerX, gw.centerY - 20f, "SHATTERED!", isHeadshot = true))
                        true
                    } else {
                        false
                    }
                }
                if (!shattered) {
                    survivingBullets.add(b.copy(x = nextX, y = nextY, traveled = newTraveled))
                }
                continue
            }

            // Obstacle collision (trees allow bullets if thin, others block)
            val hitObs = state.obstacles.any { obs ->
                obs.type != ObstacleType.TREE &&
                    nextX >= obs.x && nextX <= obs.x + obs.width &&
                    nextY >= obs.y && nextY <= obs.y + obs.height
            }
            if (hitObs) continue

            // Gloo Wall collision (Alvaro deals 2x damage to Gloo Walls)
            var blockedByGloo = false
            val ownerForGloo = mutableCombatants.firstOrNull { it.id == b.ownerId }
            val glooDmgMult = if (ownerForGloo?.characterId == CharacterId.ALVARO) 2 else 1
            for (gIdx in mutableGlooWalls.indices) {
                val gw = mutableGlooWalls[gIdx]
                if (pointCollidesGlooWall(nextX, nextY, 5f, gw)) {
                    val newGwHp = gw.hp - b.damage * glooDmgMult
                    mutableGlooWalls[gIdx] = gw.copy(hp = newGwHp)
                    blockedByGloo = true
                    if (newGwHp <= 0) {
                        mutablePopups.add(DamagePopup(allocId(), gw.centerX, gw.centerY, "GLOO BROKEN", isHeadshot = false))
                    }
                    break
                }
            }
            mutableGlooWalls.removeAll { it.hp <= 0 }
            if (blockedByGloo) continue

            // Chrono Force Field collision (blocks enemy bullets entering dome)
            val blockedByChrono = mutableDomes.any { dome ->
                dome.team != b.team && hypot(nextX - dome.x, nextY - dome.y) <= dome.radius
            }
            if (blockedByChrono) continue

            // Combatant hit detection
            var hitTarget = false
            for (cIdx in mutableCombatants.indices) {
                val target = mutableCombatants[cIdx]
                if (!target.isAlive || target.dropState != DropState.GROUNDED || target.team == b.team) continue
                if (hypot(nextX - target.x, nextY - target.y) <= 21f) {
                    hitTarget = true
                    val isHeadshot = Random.nextFloat() < b.headshotChance
                    var rawDmg = if (isHeadshot) (b.damage * b.headshotMultiplier).toInt() else b.damage

                    // Character damage bonuses
                    val ownerIdx = mutableCombatants.indexOfFirst { it.id == b.ownerId }
                    val ownerChar = if (ownerIdx >= 0) mutableCombatants[ownerIdx].characterId else null
                    val ownerActive = ownerIdx >= 0 && mutableCombatants[ownerIdx].skillActiveSec > 0f
                    if (ownerChar == CharacterId.KELLY && ownerActive) {
                        rawDmg = (rawDmg * 1.25f).toInt()
                    } else if (ownerChar == CharacterId.MOCO && isHeadshot) {
                        rawDmg = (rawDmg * 1.12f).toInt()
                    } else if (ownerChar == CharacterId.RAFAEL) {
                        rawDmg = (rawDmg * 1.15f).toInt()
                    } else if (ownerChar == CharacterId.ALVARO) {
                        rawDmg = (rawDmg * 1.18f).toInt()
                    } else if (ownerChar == CharacterId.NIKITA) {
                        rawDmg = (rawDmg * 1.15f).toInt()
                    }

                    // Armor / Helmet reduction
                    var newArmor = target.armor
                    var newHelmet = target.helmet
                    val reducedDmg = if (isHeadshot && newHelmet > 0f) {
                        val absorb = min(newHelmet, rawDmg * 0.35f)
                        newHelmet -= absorb
                        (rawDmg - absorb).toInt().coerceAtLeast(8)
                    } else if (!isHeadshot && newArmor > 0f) {
                        val absorb = min(newArmor, rawDmg * 0.38f)
                        newArmor -= absorb
                        (rawDmg - absorb).toInt().coerceAtLeast(6)
                    } else {
                        rawDmg
                    }

                    val newTargetHp = (target.hp - reducedDmg).coerceAtLeast(0f)
                    val died = newTargetHp <= 0f
                    val markDur = if (ownerChar == CharacterId.MOCO && !died) 6f else target.markedSec
                    mutableCombatants[cIdx] = target.copy(
                        hp = newTargetHp,
                        armor = newArmor,
                        helmet = newHelmet,
                        markedSec = markDur,
                        isAlive = !died
                    )

                    // Update killer stats
                    if (ownerIdx >= 0) {
                        val owner = mutableCombatants[ownerIdx]
                        val resetSkill = if (died && owner.characterId == CharacterId.WUKONG) 0f else owner.skillCooldownSec
                        mutableCombatants[ownerIdx] = owner.copy(
                            kills = owner.kills + if (died) 1 else 0,
                            headshots = owner.headshots + if (died && isHeadshot) 1 else 0,
                            damageDealt = owner.damageDealt + reducedDmg,
                            skillCooldownSec = resetSkill
                        )
                        if (owner.isPlayer) {
                            hitmarkerTimer = 0.22f
                            hitmarkerHead = isHeadshot
                            if (isHeadshot) soundEngine.playHeadshotPing()
                        }
                    }

                    val dmgText = if (isHeadshot) "HS -$reducedDmg!" else "-$reducedDmg"
                    mutablePopups.add(
                        DamagePopup(
                            id = allocId(),
                            x = target.x + (Random.nextFloat() - 0.5f) * 16f,
                            y = target.y - 26f,
                            text = dmgText,
                            isHeadshot = isHeadshot
                        )
                    )

                    if (died) {
                        mutableKillFeed.add(
                            0,
                            KillFeedItem(
                                id = allocId(),
                                killerName = b.ownerName,
                                victimName = target.name,
                                weaponName = b.weaponName,
                                isHeadshot = isHeadshot,
                                killerIsAlly = b.team == 0
                            )
                        )
                        // Drop gloo wall & medkit on death in BR
                        if (state.mode == GameMode.BATTLE_ROYALE) {
                            mutableLoot.add(LootDrop(allocId(), target.x, target.y, LootType.GLOO_WALL, label = "Gloo Wall x2"))
                        }
                    }
                    break
                }
            }

            if (!hitTarget) {
                survivingBullets.add(b.copy(x = nextX, y = nextY, traveled = newTraveled))
            }
        }

        // Check End of Round / End of Match conditions
        val nextBannerTimer = (state.bannerTimerSec - clampedDt).coerceAtLeast(0f)
        var nextState = state.copy(
            matchElapsedSec = state.matchElapsedSec + clampedDt,
            plane = plane,
            combatants = mutableCombatants,
            bullets = survivingBullets,
            glooWalls = mutableGlooWalls,
            chronoDomes = mutableDomes,
            alokAuras = mutableAuras,
            popups = mutablePopups,
            killFeed = mutableKillFeed.take(4),
            lootDrops = mutableLoot,
            safeZone = sz,
            nearbyLoot = nearbyLoot,
            hitmarkerTimerSec = hitmarkerTimer,
            hitmarkerIsHeadshot = hitmarkerHead,
            bannerTimerSec = nextBannerTimer,
            bannerMessage = if (nextBannerTimer > 0f) state.bannerMessage else null
        )

        nextState = evaluateWinLoss(nextState, onMatchFinished)
        return nextState
    }

    private fun evaluateWinLoss(
        state: ArenaState,
        onMatchFinished: (MatchSummary) -> Unit
    ): ArenaState {
        val player = state.player ?: return state
        return when (state.mode) {
            GameMode.BATTLE_ROYALE -> {
                val aliveEnemies = state.combatants.count { it.isAlive && !it.isPlayer }
                if (!player.isAlive || aliveEnemies == 0) {
                    val won = player.isAlive && aliveEnemies == 0
                    val placement = if (won) 1 else (aliveEnemies + 1)
                    val gold = (if (won) 500 else max(80, (12 - placement) * 35)) + player.kills * 40
                    val rankDelta = if (won) 45 else ((6 - placement) * 6)
                    val summary = MatchSummary(
                        mode = GameMode.BATTLE_ROYALE,
                        victory = won,
                        placement = placement,
                        totalTeamsOrPlayers = 12,
                        kills = player.kills,
                        headshots = player.headshots,
                        damageDealt = player.damageDealt,
                        survivalSeconds = state.matchElapsedSec.toInt(),
                        playerRoundsWon = if (won) 1 else 0,
                        enemyRoundsWon = if (won) 0 else 1,
                        goldEarned = gold,
                        rankDelta = rankDelta
                    )
                    onMatchFinished(summary)
                    state.copy(phase = MatchPhase.MATCH_OVER, summary = summary)
                } else state
            }
            GameMode.CLASH_SQUAD, GameMode.LONE_WOLF -> {
                val alliesAlive = state.combatants.count { it.isAlive && it.team == 0 }
                val enemiesAlive = state.combatants.count { it.isAlive && it.team == 1 }
                if (alliesAlive == 0 || enemiesAlive == 0) {
                    val roundWon = enemiesAlive == 0 && alliesAlive > 0
                    val newPWins = state.playerRoundsWon + if (roundWon) 1 else 0
                    val newEWins = state.enemyRoundsWon + if (!roundWon) 1 else 0
                    state.copy(
                        phase = MatchPhase.ROUND_OVER,
                        phaseTimerSec = 2.4f,
                        playerRoundsWon = newPWins,
                        enemyRoundsWon = newEWins,
                        bannerMessage = if (roundWon) "ROUND VICTORY!" else "ROUND DEFEAT",
                        bannerTimerSec = 2.4f
                    )
                } else state
            }
        }
    }

    private fun performFire(
        shooter: Combatant,
        allCombatants: List<Combatant>,
        obstacles: List<MapObstacle>,
        glooWalls: List<GlooWallBarrier>,
        bulletsOut: MutableList<Bullet>,
        settings: GameSettings,
        isPlayer: Boolean
    ): Combatant {
        val weapon = shooter.activeWeapon
        var aimAngle = shooter.aimAngle

        // Aim Assist check
        val assistAllowed = when {
            !isPlayer -> true
            settings.aimPrecision == AimPrecision.DEFAULT -> true
            settings.aimPrecision == AimPrecision.PRECISE_ON_SCOPE -> !shooter.isScoped
            else -> false
        }

        if (assistAllowed) {
            val coneRad = if (isPlayer) {
                (0.24f + (settings.redDotSens / 100f) * 0.22f)
            } else 0.35f

            val bestTarget = allCombatants
                .filter { it.isAlive && it.team != shooter.team && !it.isBushDisguised }
                .mapNotNull { target ->
                    val dx = target.x - shooter.x
                    val dy = target.y - shooter.y
                    val dist = hypot(dx, dy)
                    if (dist > weapon.range * 1.05f) return@mapNotNull null
                    val targetAngle = atan2(dy, dx)
                    val diff = abs(normalizeAngle(targetAngle - aimAngle))
                    if (diff <= coneRad && hasLineOfSight(shooter.x, shooter.y, target.x, target.y, obstacles, glooWalls)) {
                        Triple(targetAngle, diff, dist)
                    } else null
                }
                .minByOrNull { it.second }

            if (bestTarget != null) {
                val pull = if (isPlayer) 0.72f else 0.85f
                val delta = normalizeAngle(bestTarget.first - aimAngle)
                aimAngle += delta * pull
            }
        }

        val scopeSpreadMult = (if (shooter.isScoped) 0.42f else 1f) * (if (shooter.isCrouching) 0.65f else 1f)
        val hsChance = (if (shooter.isScoped) 0.32f else 0.18f) + (if (weapon == WeaponId.DESERT_EAGLE || weapon == WeaponId.AWM) 0.12f else 0f)

        repeat(weapon.pellets) {
            val jitter = (Random.nextFloat() - 0.5f) * 2f * weapon.spreadRad * scopeSpreadMult
            val shotAngle = aimAngle + jitter
            bulletsOut.add(
                Bullet(
                    id = allocId(),
                    ownerId = shooter.id,
                    ownerName = shooter.name,
                    team = shooter.team,
                    x = shooter.x + cos(shotAngle) * 22f,
                    y = shooter.y + sin(shotAngle) * 22f,
                    vx = cos(shotAngle) * weapon.bulletSpeed,
                    vy = sin(shotAngle) * weapon.bulletSpeed,
                    damage = weapon.damage,
                    headshotMultiplier = weapon.headshotMultiplier,
                    headshotChance = hsChance.coerceAtMost(0.48f),
                    weaponName = weapon.displayName,
                    maxRange = weapon.range,
                    colorHex = if (weapon == WeaponId.AWM) 0xFFB388FF else 0xFFFFCA28
                )
            )
        }

        val newAmmo1 = if (shooter.activeSlot == 0) (shooter.ammoInMag1 - 1).coerceAtLeast(0) else shooter.ammoInMag1
        val newAmmo2 = if (shooter.activeSlot == 1) (shooter.ammoInMag2 - 1).coerceAtLeast(0) else shooter.ammoInMag2
        val noiseDuration = if (shooter.characterId == CharacterId.RAFAEL) 0f else 1.6f

        return shooter.copy(
            aimAngle = aimAngle,
            ammoInMag1 = newAmmo1,
            ammoInMag2 = newAmmo2,
            fireCooldownSec = weapon.fireIntervalMs / 1000f,
            muzzleFlashSec = 0.08f,
            isBushDisguised = false,
            lastShotNoiseSec = noiseDuration
        )
    }

    private data class BotStepResult(
        val updatedBot: Combatant,
        val spawnedGlooWall: GlooWallBarrier? = null
    )

    private fun stepBotBehavior(
        bot: Combatant,
        dt: Float,
        speed: Float,
        allCombatants: List<Combatant>,
        obstacles: List<MapObstacle>,
        glooWalls: List<GlooWallBarrier>,
        bullets: MutableList<Bullet>,
        safeZone: SafeZone,
        mapW: Float,
        mapH: Float
    ): BotStepResult {
        var b = bot
        var newWall: GlooWallBarrier? = null

        // Low HP defensive AI: deploy Gloo Wall or heal
        if (b.hp < 85f && b.glooWalls > 0 && b.aiDecisionSec <= 0f) {
            val gx = (b.x + cos(b.aimAngle) * 68f).coerceIn(45f, mapW - 45f)
            val gy = (b.y + sin(b.aimAngle) * 68f).coerceIn(45f, mapH - 45f)
            newWall = GlooWallBarrier(allocId(), b.id, b.team, gx, gy, b.aimAngle)
            b = b.copy(glooWalls = b.glooWalls - 1, aiDecisionSec = 2.5f)
        }
        if (b.hp < 75f && b.medkits > 0 && b.medkitChannelSec <= 0f) {
            b = b.copy(medkitChannelSec = 1.8f)
        }

        // Find nearest visible enemy (respect Wukong bush stealth beyond 130 units)
        val nearestEnemy = allCombatants
            .filter { it.isAlive && it.dropState == DropState.GROUNDED && it.team != b.team }
            .filter { target ->
                val d = hypot(target.x - b.x, target.y - b.y)
                (!target.isBushDisguised || d < 130f) && d < 760f
            }
            .minByOrNull { hypot(it.x - b.x, it.y - b.y) }

        val distFromZone = hypot(b.x - safeZone.centerX, b.y - safeZone.centerY)
        val mustRunToZone = distFromZone > safeZone.currentRadius * 0.88f

        var moveDx = 0f
        var moveDy = 0f
        var nextDec = (b.aiDecisionSec - dt).coerceAtLeast(0f)
        var strafeSign = b.aiStrafeSign
        var gx = b.aiGoalX
        var gy = b.aiGoalY

        if (mustRunToZone) {
            val zAngle = atan2(safeZone.centerY - b.y, safeZone.centerX - b.x)
            moveDx = cos(zAngle)
            moveDy = sin(zAngle)
            b = b.copy(aimAngle = zAngle)
        } else if (nearestEnemy != null) {
            val dx = nearestEnemy.x - b.x
            val dy = nearestEnemy.y - b.y
            val dist = hypot(dx, dy)
            val angleToEnemy = atan2(dy, dx)
            b = b.copy(aimAngle = angleToEnemy)

            if (nextDec <= 0f) {
                strafeSign = if (Random.nextBoolean()) 1f else -1f
                nextDec = 0.8f + Random.nextFloat() * 1.1f
            }

            val los = hasLineOfSight(b.x, b.y, nearestEnemy.x, nearestEnemy.y, obstacles, glooWalls)
            val desiredDist = b.activeWeapon.range * 0.58f

            if (dist > desiredDist || !los) {
                moveDx = cos(angleToEnemy) * 0.8f - sin(angleToEnemy) * 0.45f * strafeSign
                moveDy = sin(angleToEnemy) * 0.8f + cos(angleToEnemy) * 0.45f * strafeSign
            } else {
                // Tactical strafe
                moveDx = -sin(angleToEnemy) * strafeSign
                moveDy = cos(angleToEnemy) * strafeSign
            }

            if (los && dist <= b.activeWeapon.range && b.fireCooldownSec <= 0f && b.reloadSecLeft <= 0f) {
                if (b.activeAmmoInMag > 0) {
                    // Slight human inaccuracy for bots
                    val aimNoise = (Random.nextFloat() - 0.5f) * 0.16f
                    val fired = performFire(
                        b.copy(aimAngle = angleToEnemy + aimNoise),
                        allCombatants,
                        obstacles,
                        glooWalls,
                        bullets,
                        GameSettings(aimPrecision = AimPrecision.FULL_CONTROL),
                        isPlayer = false
                    )
                    // Slow down bot burst slightly vs player
                    b = fired.copy(fireCooldownSec = fired.fireCooldownSec * 1.45f)
                } else if (b.reserveAmmo > 0) {
                    b = b.copy(reloadSecLeft = b.activeWeapon.reloadMs / 1000f)
                }
            }
        } else {
            // Patrol toward goal inside safe zone
            val dx = gx - b.x
            val dy = gy - b.y
            val dist = hypot(dx, dy)
            if (dist < 45f || nextDec <= 0f) {
                val rAngle = Random.nextFloat() * 2f * PI.toFloat()
                val rDist = Random.nextFloat() * (safeZone.currentRadius * 0.65f)
                gx = (safeZone.centerX + cos(rAngle) * rDist).coerceIn(80f, mapW - 80f)
                gy = (safeZone.centerY + sin(rAngle) * rDist).coerceIn(80f, mapH - 80f)
                nextDec = 3.0f
            } else {
                val moveAngle = atan2(dy, dx)
                moveDx = cos(moveAngle)
                moveDy = sin(moveAngle)
                b = b.copy(aimAngle = moveAngle)
            }
        }

        val mLen = hypot(moveDx, moveDy)
        if (mLen > 0.05f) {
            val nx = b.x + (moveDx / mLen) * speed * 0.86f * dt
            val ny = b.y + (moveDy / mLen) * speed * 0.86f * dt
            val resolved = resolveCollisions(nx, ny, 18f, obstacles, glooWalls, mapW, mapH)
            b = b.copy(x = resolved.first, y = resolved.second)
        }

        return BotStepResult(
            updatedBot = b.copy(
                aiDecisionSec = nextDec,
                aiStrafeSign = strafeSign,
                aiGoalX = gx,
                aiGoalY = gy
            ),
            spawnedGlooWall = newWall
        )
    }

    private fun updateSafeZone(sz: SafeZone, dt: Float): SafeZone {
        val nextCountdown = sz.countdownSec - dt
        if (nextCountdown > 0f) {
            return sz.copy(countdownSec = nextCountdown)
        }
        val shrinkRate = 28f * dt
        val newRadius = max(sz.targetRadius, sz.currentRadius - shrinkRate)
        return if (newRadius <= sz.targetRadius + 1f && sz.targetRadius > 90f) {
            sz.copy(
                currentRadius = newRadius,
                targetRadius = max(85f, sz.targetRadius * 0.55f),
                shrinkPhase = sz.shrinkPhase + 1,
                countdownSec = 14f,
                damagePerSec = sz.damagePerSec + 4f
            )
        } else {
            sz.copy(currentRadius = newRadius)
        }
    }

    private fun applyLootToCombatant(c: Combatant, loot: LootDrop): Combatant {
        return when (loot.type) {
            LootType.GLOO_WALL -> c.copy(glooWalls = (c.glooWalls + 1).coerceAtMost(5))
            LootType.MEDKIT -> c.copy(medkits = (c.medkits + 1).coerceAtMost(5))
            LootType.VEST -> c.copy(armor = 100f)
            LootType.HELMET -> c.copy(helmet = 100f)
            LootType.AMMO -> c.copy(reserveAmmo = c.reserveAmmo + 45)
            LootType.WEAPON -> {
                val w = loot.weaponId ?: return c
                if (c.weapon1 == WeaponId.USP) {
                    c.copy(weapon1 = w, ammoInMag1 = w.magSize, activeSlot = 0)
                } else if (c.weapon2 == null) {
                    c.copy(weapon2 = w, ammoInMag2 = w.magSize, activeSlot = 1)
                } else if (c.activeSlot == 0) {
                    c.copy(weapon1 = w, ammoInMag1 = w.magSize)
                } else {
                    c.copy(weapon2 = w, ammoInMag2 = w.magSize)
                }
            }
        }
    }

    private fun resolveCollisions(
        x: Float,
        y: Float,
        radius: Float,
        obstacles: List<MapObstacle>,
        glooWalls: List<GlooWallBarrier>,
        mapW: Float,
        mapH: Float
    ): Pair<Float, Float> {
        var cx = x.coerceIn(radius + 12f, mapW - radius - 12f)
        var cy = y.coerceIn(radius + 12f, mapH - radius - 12f)

        for (obs in obstacles) {
            val closestX = cx.coerceIn(obs.x, obs.x + obs.width)
            val closestY = cy.coerceIn(obs.y, obs.y + obs.height)
            val dx = cx - closestX
            val dy = cy - closestY
            val dist = hypot(dx, dy)
            if (dist < radius && dist > 0.0001f) {
                val push = radius - dist
                cx += (dx / dist) * push
                cy += (dy / dist) * push
            } else if (dist == 0f) {
                cx = obs.x - radius
            }
        }

        for (gw in glooWalls) {
            val nx = cos(gw.angleRad)
            val ny = sin(gw.angleRad)
            val tx = -ny
            val ty = nx
            val dx = cx - gw.centerX
            val dy = cy - gw.centerY
            val localT = dx * tx + dy * ty
            val localN = dx * nx + dy * ny
            val halfW = gw.width * 0.5f + radius
            val halfH = gw.thickness * 0.5f + radius
            if (abs(localT) < halfW && abs(localN) < halfH) {
                val penT = halfW - abs(localT)
                val penN = halfH - abs(localN)
                if (penN < penT) {
                    val sign = if (localN >= 0f) 1f else -1f
                    cx += nx * penN * sign
                    cy += ny * penN * sign
                } else {
                    val sign = if (localT >= 0f) 1f else -1f
                    cx += tx * penT * sign
                    cy += ty * penT * sign
                }
            }
        }
        return cx.coerceIn(radius + 12f, mapW - radius - 12f) to cy.coerceIn(radius + 12f, mapH - radius - 12f)
    }

    private fun pushOutOfObstacles(
        x: Float,
        y: Float,
        radius: Float,
        obstacles: List<MapObstacle>,
        mapW: Float,
        mapH: Float
    ): Pair<Float, Float> = resolveCollisions(x, y, radius, obstacles, emptyList(), mapW, mapH)

    private fun pointCollidesGlooWall(px: Float, py: Float, radius: Float, gw: GlooWallBarrier): Boolean {
        val nx = cos(gw.angleRad)
        val ny = sin(gw.angleRad)
        val tx = -ny
        val ty = nx
        val dx = px - gw.centerX
        val dy = py - gw.centerY
        val localT = dx * tx + dy * ty
        val localN = dx * nx + dy * ny
        return abs(localT) <= gw.width * 0.5f + radius && abs(localN) <= gw.thickness * 0.5f + radius
    }

    private fun hasLineOfSight(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        obstacles: List<MapObstacle>,
        glooWalls: List<GlooWallBarrier>
    ): Boolean {
        val steps = 10
        for (s in 1 until steps) {
            val t = s.toFloat() / steps
            val px = x1 + (x2 - x1) * t
            val py = y1 + (y2 - y1) * t
            if (obstacles.any { it.type != ObstacleType.TREE && px >= it.x && px <= it.x + it.width && py >= it.y && py <= it.y + it.height }) {
                return false
            }
            if (glooWalls.any { pointCollidesGlooWall(px, py, 4f, it) }) {
                return false
            }
        }
        return true
    }

    private fun normalizeAngle(a: Float): Float {
        var r = a
        while (r > PI) r -= (2 * PI).toFloat()
        while (r < -PI) r += (2 * PI).toFloat()
        return r
    }

    private fun generateBermudaObstacles(mapW: Float, mapH: Float): List<MapObstacle> {
        val list = mutableListOf<MapObstacle>()
        // Clock Tower / Pochinok style central compounds
        list.add(MapObstacle(allocId(), mapW * 0.45f, mapH * 0.44f, 190f, 110f, ObstacleType.BUILDING))
        list.add(MapObstacle(allocId(), mapW * 0.30f, mapH * 0.32f, 150f, 95f, ObstacleType.BUILDING))
        list.add(MapObstacle(allocId(), mapW * 0.62f, mapH * 0.30f, 160f, 100f, ObstacleType.BUILDING))
        list.add(MapObstacle(allocId(), mapW * 0.28f, mapH * 0.62f, 170f, 95f, ObstacleType.BUILDING))
        list.add(MapObstacle(allocId(), mapW * 0.64f, mapH * 0.60f, 155f, 110f, ObstacleType.BUILDING))
        // Crates & Rocks
        val crateSpots = listOf(
            0.22f to 0.48f, 0.48f to 0.24f, 0.76f to 0.48f, 0.48f to 0.78f,
            0.38f to 0.50f, 0.57f to 0.50f, 0.48f to 0.37f, 0.48f to 0.64f
        )
        crateSpots.forEach { (fx, fy) ->
            list.add(MapObstacle(allocId(), mapW * fx, mapH * fy, 64f, 64f, ObstacleType.CRATE))
        }
        // Scenic tactical cover trees & rocks
        repeat(16) { idx ->
            val rx = 180f + ((idx * 347) % (mapW.toInt() - 360)).toFloat()
            val ry = 180f + ((idx * 523) % (mapH.toInt() - 360)).toFloat()
            val type = if (idx % 2 == 0) ObstacleType.TREE else ObstacleType.ROCK
            list.add(MapObstacle(allocId(), rx, ry, 52f, 52f, type))
        }
        return list
    }

    private fun generateClashSquadObstacles(mapW: Float, mapH: Float): List<MapObstacle> {
        // Hangar warehouse layout inspired by screenshot #2: blue/orange shipping containers, green vans & center cover
        return listOf(
            MapObstacle(allocId(), mapW * 0.25f, mapH * 0.18f, 115f, 52f, ObstacleType.CONTAINER_RED),
            MapObstacle(allocId(), mapW * 0.65f, mapH * 0.18f, 115f, 52f, ObstacleType.CONTAINER_RED),
            MapObstacle(allocId(), mapW * 0.25f, mapH * 0.74f, 115f, 52f, ObstacleType.CONTAINER_BLUE),
            MapObstacle(allocId(), mapW * 0.65f, mapH * 0.74f, 115f, 52f, ObstacleType.CONTAINER_BLUE),
            MapObstacle(allocId(), mapW * 0.45f, mapH * 0.28f, 92f, 44f, ObstacleType.VAN),
            MapObstacle(allocId(), mapW * 0.45f, mapH * 0.66f, 92f, 44f, ObstacleType.VAN),
            MapObstacle(allocId(), mapW * 0.46f, mapH * 0.44f, 100f, 115f, ObstacleType.CONTAINER_BLUE),
            MapObstacle(allocId(), mapW * 0.34f, mapH * 0.46f, 65f, 65f, ObstacleType.CRATE),
            MapObstacle(allocId(), mapW * 0.60f, mapH * 0.46f, 65f, 65f, ObstacleType.CRATE)
        )
    }

    private fun generateLoneWolfObstacles(mapW: Float, mapH: Float): List<MapObstacle> {
        // Iron Cage Container Warehouse duel layout matching screenshot #2
        return listOf(
            MapObstacle(allocId(), mapW * 0.44f, mapH * 0.42f, 95f, 54f, ObstacleType.CONTAINER_BLUE),
            MapObstacle(allocId(), mapW * 0.24f, mapH * 0.20f, 105f, 48f, ObstacleType.CONTAINER_RED),
            MapObstacle(allocId(), mapW * 0.64f, mapH * 0.20f, 105f, 48f, ObstacleType.CONTAINER_BLUE),
            MapObstacle(allocId(), mapW * 0.24f, mapH * 0.72f, 105f, 48f, ObstacleType.CONTAINER_BLUE),
            MapObstacle(allocId(), mapW * 0.64f, mapH * 0.72f, 105f, 48f, ObstacleType.CONTAINER_RED),
            MapObstacle(allocId(), mapW * 0.44f, mapH * 0.24f, 84f, 42f, ObstacleType.VAN),
            MapObstacle(allocId(), mapW * 0.44f, mapH * 0.68f, 84f, 42f, ObstacleType.VAN)
        )
    }

    private fun generateBattleRoyaleLoot(
        mapW: Float,
        mapH: Float,
        obstacles: List<MapObstacle>
    ): List<LootDrop> {
        val drops = mutableListOf<LootDrop>()
        val weapons = listOf(WeaponId.MP40, WeaponId.M1887, WeaponId.SCAR, WeaponId.AK47, WeaponId.AWM, WeaponId.MP5, WeaponId.DESERT_EAGLE)
        // Guarantee nice starter loot near player's drop zone
        drops.add(LootDrop(allocId(), mapW * 0.5f - 70f, mapH * 0.72f - 50f, LootType.WEAPON, WeaponId.MP40, "MP40 Cobra"))
        drops.add(LootDrop(allocId(), mapW * 0.5f + 70f, mapH * 0.72f - 50f, LootType.GLOO_WALL, label = "Gloo Wall"))
        drops.add(LootDrop(allocId(), mapW * 0.5f, mapH * 0.72f - 95f, LootType.WEAPON, WeaponId.SCAR, "SCAR-L"))

        repeat(24) { idx ->
            val rx = 180f + Random.nextFloat() * (mapW - 360f)
            val ry = 180f + Random.nextFloat() * (mapH - 360f)
            val pos = pushOutOfObstacles(rx, ry, 24f, obstacles, mapW, mapH)
            val roll = idx % 5
            val drop = when (roll) {
                0, 1 -> {
                    val w = weapons.random()
                    LootDrop(allocId(), pos.first, pos.second, LootType.WEAPON, w, w.displayName)
                }
                2 -> LootDrop(allocId(), pos.first, pos.second, LootType.GLOO_WALL, label = "Gloo Wall")
                3 -> LootDrop(allocId(), pos.first, pos.second, LootType.MEDKIT, label = "Medkit")
                else -> if (idx % 2 == 0) {
                    LootDrop(allocId(), pos.first, pos.second, LootType.VEST, label = "Lv.3 Vest")
                } else {
                    LootDrop(allocId(), pos.first, pos.second, LootType.HELMET, label = "Lv.3 Helmet")
                }
            }
            drops.add(drop)
        }
        return drops
    }
}

sealed class CsShopItem(val title: String, val subtitle: String, val price: Int) {
    data class Weapon(val weapon: WeaponId) : CsShopItem(
        title = weapon.displayName,
        subtitle = "${weapon.category.label} • DMG ${weapon.damage}",
        price = weapon.csPrice
    )
    data object GlooWall : CsShopItem("Gloo Wall", "Deployable ice shield (Max 3)", 300)
    data object VestAndHelmet : CsShopItem("Vest + Helmet Lv.3", "Full 100 Armor & Head Protection", 600)
    data object Medkit : CsShopItem("Medkit", "+75 HP Tactical Heal", 200)
}
