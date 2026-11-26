package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.match.MatchScreen
import com.example.ui.screens.ArmoryScreen
import com.example.ui.screens.CustomHudEditorScreen
import com.example.ui.screens.LobbyScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AmberGold
import com.example.ui.theme.FireZoneTheme
import com.example.ui.theme.GunmetalSurface
import com.example.ui.theme.ObsidianBg
import com.example.viewmodel.GameViewModel

private const val ROUTE_LOBBY = "lobby"
private const val ROUTE_ARMORY = "armory"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_PROFILE = "profile"
private const val ROUTE_CUSTOM_HUD = "custom_hud"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            FireZoneTheme {
                FireZoneApp()
            }
        }
    }
}

@Composable
fun FireZoneApp(gameViewModel: GameViewModel = viewModel()) {
    val appData by gameViewModel.appData.collectAsStateWithLifecycle()
    val selectedMode by gameViewModel.selectedLobbyMode.collectAsStateWithLifecycle()
    val arenaState by gameViewModel.arenaState.collectAsStateWithLifecycle()

    val currentMatch = arenaState
    if (currentMatch != null) {
        MatchScreen(
            arenaState = currentMatch,
            settings = appData.settings,
            onMoveVector = gameViewModel::setMovementVector,
            onSetFiring = gameViewModel::setFiring,
            onRotateAim = gameViewModel::rotateAim,
            onEjectFromPlane = gameViewModel::ejectFromPlane,
            onOpenParachute = gameViewModel::openParachute,
            onDeployGlooWall = gameViewModel::deployGlooWall,
            onActivateSkill = gameViewModel::activateSkill,
            onToggleScope = gameViewModel::toggleScope,
            onJump = gameViewModel::jump,
            onToggleCrouch = gameViewModel::toggleCrouch,
            onToggleSprint = gameViewModel::toggleSprint,
            onSwitchWeapon = gameViewModel::switchWeapon,
            onReloadWeapon = gameViewModel::reloadWeapon,
            onUseMedkit = gameViewModel::useMedkit,
            onPickupLoot = gameViewModel::pickupLoot,
            onBuyCsItem = gameViewModel::buyCsItem,
            onStartCombatFromShop = gameViewModel::startCombatFromShop,
            onSelectLoneWolfLoadout = gameViewModel::selectLoneWolfLoadout,
            onSaveCustomHud = gameViewModel::saveHudLayout,
            onPauseChanged = gameViewModel::setPaused,
            onPlayAgain = { gameViewModel.startMatch(currentMatch.mode) },
            onExitMatch = gameViewModel::exitMatch
        )
        return
    }

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ROUTE_LOBBY,
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        composable(ROUTE_LOBBY) {
            LobbyScreen(
                appData = appData,
                selectedMode = selectedMode,
                onSelectMode = gameViewModel::selectLobbyMode,
                onStartMatch = gameViewModel::startMatch,
                onSelectOrUnlockCharacter = gameViewModel::selectOrUnlockCharacter,
                onUpgradeCharacter = gameViewModel::upgradeCharacter,
                onOpenProfile = {
                    gameViewModel.soundEngine.playUiClick()
                    navController.navigate(ROUTE_PROFILE)
                },
                onOpenArmory = {
                    gameViewModel.soundEngine.playUiClick()
                    navController.navigate(ROUTE_ARMORY)
                },
                onOpenSettings = {
                    gameViewModel.soundEngine.playUiClick()
                    navController.navigate(ROUTE_SETTINGS)
                },
                onOpenCustomHud = {
                    gameViewModel.soundEngine.playUiClick()
                    navController.navigate(ROUTE_CUSTOM_HUD)
                }
            )
        }
        composable(ROUTE_PROFILE) {
            BackHandler { navController.popBackStack() }
            ProfileScreen(
                appData = appData,
                onLikeProfile = gameViewModel::likeProfile,
                onSaveProfileMeta = gameViewModel::updateProfileMeta,
                onClose = { navController.popBackStack() }
            )
        }
        composable(ROUTE_CUSTOM_HUD) {
            BackHandler { navController.popBackStack() }
            CustomHudEditorScreen(
                settings = appData.settings,
                onSaveLayout = gameViewModel::saveHudLayout,
                onClose = { navController.popBackStack() }
            )
        }
        composable(ROUTE_ARMORY) {
            BackHandler { navController.popBackStack() }
            SubPageContainer(
                title = "WEAPON ARMORY",
                onBack = { navController.popBackStack() }
            ) {
                ArmoryScreen()
            }
        }
        composable(ROUTE_SETTINGS) {
            BackHandler { navController.popBackStack() }
            SubPageContainer(
                title = "FREE FIRE SETTINGS",
                onBack = { navController.popBackStack() }
            ) {
                SettingsScreen(
                    appData = appData,
                    onUpdateSettings = gameViewModel::updateSettings,
                    onResetSensitivity = gameViewModel::resetSensitivity,
                    onUpdateCallsign = gameViewModel::updateCallsign,
                    onOpenCustomHud = {
                        gameViewModel.soundEngine.playUiClick()
                        navController.navigate(ROUTE_CUSTOM_HUD)
                    },
                    onSelectHudPreset = gameViewModel::setActiveHudPreset
                )
            }
        }
    }
}

@Composable
private fun SubPageContainer(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GunmetalSurface)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("subpage_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Lobby",
                    tint = AmberGold
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}
