package com.pairplay

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.pairplay.domain.models.GamePhase
import com.pairplay.presentation.ui.game.GameHostScreen
import com.pairplay.presentation.ui.home.HomeScreen
import com.pairplay.presentation.ui.lobby.LobbyScreen
import com.pairplay.presentation.ui.result.ResultScreen
import com.pairplay.presentation.ui.theme.PairPlayTheme
import com.pairplay.presentation.viewmodel.PairPlayViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PairPlayTheme {
                Surface(Modifier.fillMaxSize()) {
                    PairPlayAppContent()
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PairPlayAppContent() {
    val vm: PairPlayViewModel = hiltViewModel()
    val navController = rememberNavController()
    val phase by vm.phase.collectAsState()
    val devices by vm.devices.collectAsState()
    val currentGame by vm.currentGame.collectAsState()

    val perms = buildList {
        if (Build.VERSION.SDK_INT >= 31) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        add(Manifest.permission.RECORD_AUDIO)
    }
    val permState = rememberMultiplePermissionsState(perms)
    LaunchedEffect(Unit) { permState.launchMultiplePermissionRequest() }

    var isHost by remember { mutableStateOf(false) }

    // Navigation réactive selon la phase du GameEngine
    LaunchedEffect(phase) {
        when (phase) {
            is GamePhase.Playing, is GamePhase.Syncing -> {
                if (navController.currentDestination?.route != "game") {
                    navController.navigate("game") { popUpTo("home") }
                }
            }
            is GamePhase.Finished -> {
                vm.onSessionFinished((phase as GamePhase.Finished).result)
                if (navController.currentDestination?.route != "result") {
                    navController.navigate("result") { popUpTo("home") }
                }
            }
            else -> Unit
        }
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                games = vm.availableGames,
                onSolo = {
                    isHost = true
                    vm.startSolo()
                    navController.navigate("game")
                },
                onHost = {
                    isHost = true
                    vm.startAsHost()
                    navController.navigate("lobby")
                },
                onJoin = {
                    isHost = false
                    vm.beginDiscovery()
                    navController.navigate("lobby")
                }
            )
        }
        composable("lobby") {
            LobbyScreen(
                isHost = isHost,
                phase = phase,
                devices = devices,
                onRescan = { vm.beginDiscovery() },
                onConnect = { vm.connect(it); vm.stopDiscovery() },
                onStart = { vm.hostStarts() },
                onBack = { vm.reset(); navController.popBackStack() }
            )
        }
        composable("game") {
            GameHostScreen(
                phase = phase,
                game = currentGame,
                seed = vm.seed(),
                onFinishGame = { id, score -> vm.onGameFinished(id, score) }
            )
        }
        composable("result") {
            val p = phase
            if (p is GamePhase.Finished) {
                ResultScreen(
                    result = p.result,
                    myId = vm.meId(),
                    playerName = vm::playerName,
                    onRestart = {
                        vm.reset()
                        navController.navigate("home") { popUpTo("home") { inclusive = true } }
                    }
                )
            } else {
                Box(Modifier.fillMaxSize().padding(16.dp))
            }
        }
    }
}
