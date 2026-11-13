package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.BottomNavBar
import com.example.ui.components.TopLiveHeader
import com.example.ui.components.WalletPaymentModal
import com.example.ui.components.WinCelebrationModal
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.GameplayScreen
import com.example.ui.screens.LobbyScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.TicketSelectionScreen
import com.example.ui.screens.TournamentScreen
import com.example.ui.theme.BgDark
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.BingoViewModel
import com.example.viewmodel.NavTab
import com.example.viewmodel.PlayView
import kotlinx.coroutines.launch

import androidx.credentials.CredentialManager
import com.example.data.AuthManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                YemBingoApp()
            }
        }
    }
}

@Composable
fun YemBingoApp(
    viewModel: BingoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val credentialManager = remember { CredentialManager.create(context) }

    // Attempt Silent Auto-Sign-In on Launch
    LaunchedEffect(Unit) {
        AuthManager.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { viewModel.handleGoogleSignInSuccess() },
            onUnauthenticated = {},
            scope = scope
        )
    }

    // Show notifications from ViewModel
    LaunchedEffect(uiState.notificationMessage) {
        uiState.notificationMessage?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(msg)
            }
            viewModel.clearNotification()
        }
    }

    // Android System Back Navigation Handler
    BackHandler(enabled = true) {
        when {
            !uiState.isLoggedIn -> {
                (context as? ComponentActivity)?.finish()
            }
            uiState.showDepositModal || uiState.showWithdrawModal -> viewModel.closeModal()
            uiState.winCelebration != null -> viewModel.dismissWinModal()
            uiState.selectedTab == NavTab.PLAY && uiState.playView == PlayView.GAMEPLAY -> {
                viewModel.backToLobby()
            }
            uiState.selectedTab == NavTab.PLAY && uiState.playView == PlayView.TICKET_SELECTION -> {
                viewModel.backToLobby()
            }
            uiState.selectedTab != NavTab.PLAY -> {
                viewModel.switchTab(NavTab.PLAY)
            }
            else -> {
                (context as? ComponentActivity)?.finish()
            }
        }
    }

    // IF NOT LOGGED IN: Show Auth Section (Log In / Create Account / Google Sign-In)
    if (!uiState.isLoggedIn) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = BgDark,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(BgDark),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                ) {
                    AuthScreen(
                        onLogin = { phone, pin -> viewModel.login(phone, pin) },
                        onRegister = { name, phone, city, pin, promo ->
                            viewModel.register(name, phone, city, pin, promo)
                        },
                        onGoogleSignIn = {
                            AuthManager.onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = { viewModel.handleGoogleSignInSuccess() },
                                onAuthError = { err ->
                                    scope.launch { snackbarHostState.showSnackbar(err) }
                                },
                                scope = scope
                            )
                        }
                    )
                }
            }
        }
    } else {
        // IF LOGGED IN: Main Game Application
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = BgDark,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopLiveHeader(
                    balanceEtb = uiState.userProfile.balanceEtb,
                    jackpotCurrentEtb = uiState.jackpotCurrentEtb,
                    jackpotTargetEtb = uiState.jackpotTargetEtb,
                    secondsLeft = uiState.jackpotSecondsLeft,
                    onWalletClick = { viewModel.openDepositModal() }
                )
            },
            bottomBar = {
                BottomNavBar(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = { viewModel.switchTab(it) }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(BgDark),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (uiState.selectedTab) {
                        NavTab.PLAY -> {
                            when (uiState.playView) {
                                PlayView.LOBBY -> {
                                    LobbyScreen(
                                        rooms = uiState.rooms,
                                        isDailyClaimed = uiState.userProfile.isDailyClaimed,
                                        onClaimReward = { viewModel.claimDailyReward() },
                                        onSelectRoom = { room -> viewModel.enterTicketSelection(room) }
                                    )
                                }
                                PlayView.TICKET_SELECTION -> {
                                    uiState.selectedRoom?.let { room ->
                                        TicketSelectionScreen(
                                            room = room,
                                            selectedTicketNumber = uiState.selectedTicketNumber,
                                            card = uiState.currentCard,
                                            onBackToLobby = { viewModel.backToLobby() },
                                            onSelectTicket = { num -> viewModel.selectTicket(num) },
                                            onRefreshCard = { viewModel.refreshCard() },
                                            onStartGame = { viewModel.startLiveGame() }
                                        )
                                    }
                                }
                                PlayView.GAMEPLAY -> {
                                    GameplayScreen(
                                        currentCard = uiState.currentCard,
                                        calledNumbers = uiState.calledNumbers,
                                        latestCalledBall = uiState.latestCalledBall,
                                        amharicAnnouncement = uiState.latestAmharicAnnouncement,
                                        isAutoDaub = uiState.isAutoDaub,
                                        onToggleAutoDaub = { viewModel.toggleAutoDaub() },
                                        onDaubCell = { r, c -> viewModel.daubCell(r, c) },
                                        onExitGame = { viewModel.backToLobby() }
                                    )
                                }
                            }
                        }

                        NavTab.TOURNAMENT -> {
                            TournamentScreen(
                                currentTab = uiState.tournamentTab,
                                leaderboards = uiState.tournamentLeaderboards,
                                tournamentSecondsLeft = uiState.tournamentSecondsLeft,
                                onTabSelected = { tab -> viewModel.setTournamentTab(tab) }
                            )
                        }

                        NavTab.PROFILE -> {
                            ProfileScreen(
                                profile = uiState.userProfile,
                                transactions = uiState.transactions,
                                isSoundEnabled = uiState.isSoundEnabled,
                                isVibrationEnabled = uiState.isVibrationEnabled,
                                isAmharicVoiceEnabled = uiState.isAmharicVoiceEnabled,
                                isFastCallerSpeed = uiState.isFastCallerSpeed,
                                onToggleSound = { viewModel.setSoundEnabled(it) },
                                onToggleVibration = { viewModel.setVibrationEnabled(it) },
                                onToggleAmharicVoice = { viewModel.setAmharicVoiceEnabled(it) },
                                onToggleFastCallerSpeed = { viewModel.setFastCallerSpeed(it) },
                                onDepositClick = { viewModel.openDepositModal() },
                                onWithdrawClick = { viewModel.openWithdrawModal() },
                                onLogout = { viewModel.logout() }
                            )
                        }
                    }
                }
            }
        }

        // Deposit Modal
        if (uiState.showDepositModal) {
            WalletPaymentModal(
                isDeposit = true,
                currentBalanceEtb = uiState.userProfile.balanceEtb,
                onDismiss = { viewModel.closeModal() },
                onConfirm = { amount, method, phone ->
                    viewModel.confirmDeposit(amount, method, phone)
                }
            )
        }

        // Cashout Modal
        if (uiState.showWithdrawModal) {
            WalletPaymentModal(
                isDeposit = false,
                currentBalanceEtb = uiState.userProfile.balanceEtb,
                onDismiss = { viewModel.closeModal() },
                onConfirm = { amount, method, phone ->
                    viewModel.confirmWithdraw(amount, method, phone)
                }
            )
        }

        // Win Fanfare Celebration Modal
        uiState.winCelebration?.let { winData ->
            WinCelebrationModal(
                winData = winData,
                onContinue = { viewModel.dismissWinModal() }
            )
        }
    }
}
