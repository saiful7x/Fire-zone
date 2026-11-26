package com.example

import com.example.audio.TacticalSoundEngine
import com.example.engine.ArenaEngine
import com.example.engine.DropState
import com.example.engine.MatchPhase
import com.example.model.CharacterId
import com.example.model.GameMode
import com.example.model.GameSettings
import com.example.model.PlayerProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArenaEngineTest {
    private val engine = ArenaEngine(TacticalSoundEngine())

    @Test
    fun createsAllThreeGameModes() {
        val profile = PlayerProfile(selectedCharacter = CharacterId.ALOK)

        val br = engine.createMatch(GameMode.BATTLE_ROYALE, profile)
        assertEquals(MatchPhase.ACTIVE_COMBAT, br.phase)
        assertEquals(12, br.combatants.size)
        assertTrue(br.plane.active)
        assertEquals(DropState.IN_PLANE, br.player!!.dropState)

        val cs = engine.createMatch(GameMode.CLASH_SQUAD, profile)
        assertEquals(MatchPhase.SHOPPING, cs.phase)
        assertEquals(8, cs.combatants.size)

        val lw = engine.createMatch(GameMode.LONE_WOLF, profile)
        assertEquals(MatchPhase.LONE_WOLF_PICK, lw.phase)
        assertEquals(2, lw.combatants.size)
    }

    @Test
    fun ejectFromPlaneAndOpenParachuteWorks() {
        val profile = PlayerProfile(selectedCharacter = CharacterId.ALOK)
        var state = engine.createMatch(GameMode.BATTLE_ROYALE, profile)
        assertEquals(DropState.IN_PLANE, state.player!!.dropState)

        state = engine.ejectFromPlane(state)
        assertEquals(DropState.SKYDIVING, state.player!!.dropState)

        state = engine.openParachute(state)
        assertEquals(DropState.PARACHUTE, state.player!!.dropState)
    }

    @Test
    fun deployGlooWallConsumesStackAndSkylerHeals() {
        val profile = PlayerProfile(selectedCharacter = CharacterId.SKYLER)
        var state = engine.createMatch(GameMode.BATTLE_ROYALE, profile)
        val groundedPlayer = state.player!!.copy(hp = 120f, glooWalls = 2, dropState = DropState.GROUNDED, altitudeMeters = 0f)
        state = state.copy(combatants = state.combatants.map { if (it.isPlayer) groundedPlayer else it })

        val afterGloo = engine.deployPlayerGlooWall(state)
        assertEquals(1, afterGloo.glooWalls.size)
        assertEquals(1, afterGloo.player!!.glooWalls)
        assertEquals(155f, afterGloo.player!!.hp, 0.1f)
    }

    @Test
    fun sensitivityControlsAimRotationSpeed() {
        val profile = PlayerProfile()
        val state = engine.createMatch(GameMode.BATTLE_ROYALE, profile)
        val startAngle = state.player!!.aimAngle

        val lowSens = engine.rotatePlayerAim(state, 0.5f, GameSettings(generalSens = 10))
        val highSens = engine.rotatePlayerAim(state, 0.5f, GameSettings(generalSens = 100))

        val lowDelta = kotlin.math.abs(lowSens.player!!.aimAngle - startAngle)
        val highDelta = kotlin.math.abs(highSens.player!!.aimAngle - startAngle)
        assertTrue(highDelta > lowDelta * 2f)
    }
}
