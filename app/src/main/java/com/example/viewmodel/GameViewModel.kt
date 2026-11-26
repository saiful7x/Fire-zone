package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.TacticalSoundEngine
import com.example.data.GameRepository
import com.example.engine.ArenaEngine
import com.example.engine.ArenaState
import com.example.engine.CsShopItem
import com.example.engine.MatchPhase
import com.example.engine.MatchSummary
import com.example.model.AppDataState
import com.example.model.CharacterId
import com.example.model.GameMode
import com.example.model.GameSettings
import com.example.model.HudButtonId
import com.example.model.HudElementLayout
import com.example.model.defaultHudLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GameRepository(application.applicationContext)
    val soundEngine = TacticalSoundEngine()
    private val arenaEngine = ArenaEngine(soundEngine)

    val appData: StateFlow<AppDataState> = repository.appDataFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = AppDataState()
        )

    private val _selectedLobbyMode = MutableStateFlow(GameMode.BATTLE_ROYALE)
    val selectedLobbyMode: StateFlow<GameMode> = _selectedLobbyMode.asStateFlow()

    private val _arenaState = MutableStateFlow<ArenaState?>(null)
    val arenaState: StateFlow<ArenaState?> = _arenaState.asStateFlow()

    private var simJob: Job? = null
    private var matchResultRecorded = false

    // Real-time input vector updated from touch controls
    @Volatile private var moveInputX: Float = 0f
    @Volatile private var moveInputY: Float = 0f
    @Volatile private var firingActive: Boolean = false
    @Volatile private var isPaused: Boolean = false

    init {
        viewModelScope.launch {
            appData.collect { data ->
                soundEngine.updateSettings(data.settings)
            }
        }
    }

    fun selectLobbyMode(mode: GameMode) {
        soundEngine.playUiClick()
        _selectedLobbyMode.value = mode
    }

    fun selectOrUnlockCharacter(id: CharacterId) {
        soundEngine.playUiClick()
        val data = appData.value
        viewModelScope.launch {
            if (id in data.profile.unlockedCharacters) {
                repository.selectCharacter(id)
            } else {
                repository.unlockCharacter(id)
            }
        }
    }

    fun upgradeCharacter(id: CharacterId) {
        soundEngine.playUiClick()
        viewModelScope.launch {
            repository.upgradeCharacter(id)
        }
    }

    fun updateSettings(transform: (GameSettings) -> GameSettings) {
        viewModelScope.launch {
            repository.updateSettings(transform)
        }
    }

    fun resetSensitivity() {
        soundEngine.playUiClick()
        updateSettings {
            it.copy(
                generalSens = 78,
                redDotSens = 72,
                scope2xSens = 65,
                scope4xSens = 55,
                sniperSens = 45,
                freeLookSens = 70
            )
        }
    }

    fun updateCallsign(callsign: String) {
        viewModelScope.launch {
            repository.updateCallsign(callsign)
        }
    }

    fun updateProfileMeta(callsign: String, guildName: String, bio: String) {
        soundEngine.playUiClick()
        viewModelScope.launch {
            repository.updateProfileMeta(callsign, guildName, bio)
        }
    }

    fun likeProfile() {
        soundEngine.playUiClick()
        viewModelScope.launch {
            repository.incrementLikes()
        }
    }

    fun setActiveHudPreset(preset: Int) {
        soundEngine.playUiClick()
        updateSettings { it.copy(activeHudPreset = preset.coerceIn(1, 2)) }
    }

    fun saveHudLayout(preset: Int, layout: Map<HudButtonId, HudElementLayout>) {
        soundEngine.playUiClick()
        updateSettings {
            if (preset == 2) {
                it.copy(activeHudPreset = 2, hudLayoutPreset2 = layout)
            } else {
                it.copy(activeHudPreset = 1, hudLayoutPreset1 = layout)
            }
        }
    }

    fun resetHudLayout(preset: Int) {
        soundEngine.playUiClick()
        val defaults = defaultHudLayout()
        updateSettings {
            if (preset == 2) it.copy(hudLayoutPreset2 = defaults) else it.copy(hudLayoutPreset1 = defaults)
        }
    }

    fun startMatch(mode: GameMode) {
        soundEngine.playUiClick()
        simJob?.cancel()
        matchResultRecorded = false
        moveInputX = 0f
        moveInputY = 0f
        firingActive = false
        isPaused = false

        val initial = arenaEngine.createMatch(mode, appData.value.profile)
        _arenaState.value = initial

        simJob = viewModelScope.launch {
            var lastNs = System.nanoTime()
            while (isActive) {
                val frameMs = if (appData.value.settings.highFps) 16L else 33L
                delay(frameMs)
                val nowNs = System.nanoTime()
                val dt = ((nowNs - lastNs) / 1_000_000_000f).coerceIn(0.005f, 0.05f)
                lastNs = nowNs

                val current = _arenaState.value ?: break
                if (isPaused || current.phase == MatchPhase.MATCH_OVER) continue

                val next = arenaEngine.stepSimulation(
                    currentState = current,
                    dt = dt,
                    moveX = moveInputX,
                    moveY = moveInputY,
                    isFiring = firingActive,
                    settings = appData.value.settings,
                    profile = appData.value.profile,
                    onMatchFinished = { summary -> recordSummaryOnce(summary) }
                )
                _arenaState.value = next
            }
        }
    }

    private fun recordSummaryOnce(summary: MatchSummary) {
        if (matchResultRecorded) return
        matchResultRecorded = true
        viewModelScope.launch {
            repository.recordMatchResult(
                modeName = summary.mode.badge,
                kills = summary.kills,
                damage = summary.damageDealt,
                won = summary.victory,
                goldEarned = summary.goldEarned,
                rankDelta = summary.rankDelta
            )
        }
    }

    fun setPaused(paused: Boolean) {
        isPaused = paused
    }

    fun exitMatch() {
        simJob?.cancel()
        simJob = null
        moveInputX = 0f
        moveInputY = 0f
        firingActive = false
        _arenaState.value = null
    }

    fun setMovementVector(x: Float, y: Float) {
        moveInputX = x
        moveInputY = y
    }

    fun setFiring(firing: Boolean) {
        firingActive = firing
    }

    fun rotateAim(deltaRadians: Float) {
        val current = _arenaState.value ?: return
        _arenaState.value = arenaEngine.rotatePlayerAim(current, deltaRadians, appData.value.settings)
    }

    fun deployGlooWall() {
        val current = _arenaState.value ?: return
        _arenaState.value = arenaEngine.deployPlayerGlooWall(current)
    }

    fun ejectFromPlane() {
        val current = _arenaState.value ?: return
        _arenaState.value = arenaEngine.ejectFromPlane(current)
    }

    fun openParachute() {
        val current = _arenaState.value ?: return
        _arenaState.value = arenaEngine.openParachute(current)
    }

    fun activateSkill() {
        val current = _arenaState.value ?: return
        _arenaState.value = arenaEngine.activatePlayerSkill(current)
    }

    fun toggleScope() {
        val current = _arenaState.value ?: return
        _arenaState.value = arenaEngine.togglePlayerScope(current)
    }

    fun jump() {
        val current = _arenaState.value ?: return
        _arenaState.value = arenaEngine.jumpPlayer(current)
    }

    fun toggleCrouch() {
        val current = _arenaState.value ?: return
        _arenaState.value = arenaEngine.togglePlayerCrouch(current)
    }

    fun toggleSprint() {
        val current = _arenaState.value ?: return
        _arenaState.value = arenaEngine.togglePlayerSprint(current)
    }

    fun switchWeapon() {
        val current = _arenaState.value ?: return
        _arenaState.value = arenaEngine.switchPlayerWeapon(current)
    }

    fun reloadWeapon() {
        val current = _arenaState.value ?: return
        _arenaState.value = arenaEngine.reloadPlayerWeapon(current)
    }

    fun useMedkit() {
        val current = _arenaState.value ?: return
        _arenaState.value = arenaEngine.usePlayerMedkit(current)
    }

    fun pickupLoot() {
        val current = _arenaState.value ?: return
        _arenaState.value = arenaEngine.pickupNearbyLoot(current)
    }

    fun buyCsItem(item: CsShopItem) {
        val current = _arenaState.value ?: return
        _arenaState.value = arenaEngine.buyCsItem(current, item)
    }

    fun startCombatFromShop() {
        val current = _arenaState.value ?: return
        _arenaState.value = arenaEngine.startCombatFromShop(current)
    }

    fun selectLoneWolfLoadout(index: Int) {
        val current = _arenaState.value ?: return
        _arenaState.value = arenaEngine.selectLoneWolfLoadout(current, index)
    }

    override fun onCleared() {
        simJob?.cancel()
        super.onCleared()
    }
}
