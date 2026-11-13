package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthManager
import com.example.data.FirestoreGameRoom
import com.example.data.FirestoreLeaderboard
import com.example.data.FirestoreRepository
import com.example.data.FirestoreRoomPlayer
import com.example.data.FirestoreTransaction
import com.example.data.FirestoreUserProfile
import com.example.model.BingoCard
import com.example.model.BingoCell
import com.example.model.LeaderboardEntry
import com.example.model.PaymentMethod
import com.example.model.Room
import com.example.model.TransactionItem
import com.example.model.UserProfile
import com.example.sound.SoundManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

enum class NavTab {
    PLAY,
    TOURNAMENT,
    PROFILE
}

enum class PlayView {
    LOBBY,
    TICKET_SELECTION,
    GAMEPLAY
}

enum class TournamentTab(val title: String) {
    ENKUTATASH("👑 Enkutatash Grand"),
    DAILY("⭐ Daily Top 10"),
    WEEKLY("🏆 Weekly League"),
    MONTHLY("💎 Monthly VIP")
}

data class WinCelebrationData(
    val reason: String,
    val payoutEtb: Int
)

data class BingoUiState(
    val isLoggedIn: Boolean = false,
    val selectedTab: NavTab = NavTab.PLAY,
    val playView: PlayView = PlayView.LOBBY,
    val userProfile: UserProfile = UserProfile(),
    val rooms: List<Room> = emptyList(),
    val selectedRoom: Room? = null,
    val selectedTicketNumber: Int = 77,
    val currentCard: BingoCard? = null,
    val calledNumbers: List<Int> = emptyList(),
    val latestCalledBall: Int? = null,
    val latestAmharicAnnouncement: String? = null,
    val isAutoDaub: Boolean = true,
    val isCalling: Boolean = false,
    val isSoundEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val isAmharicVoiceEnabled: Boolean = true,
    val isFastCallerSpeed: Boolean = true,
    val activeRoomPlayers: List<FirestoreRoomPlayer> = emptyList(),
    val winCelebration: WinCelebrationData? = null,
    val showDepositModal: Boolean = false,
    val showWithdrawModal: Boolean = false,
    val tournamentTab: TournamentTab = TournamentTab.ENKUTATASH,
    val tournamentLeaderboards: Map<TournamentTab, List<LeaderboardEntry>> = emptyMap(),
    val transactions: List<TransactionItem> = emptyList(),
    val jackpotCurrentEtb: Int = 432,
    val jackpotTargetEtb: Int = 500,
    val jackpotSecondsLeft: Int = 258,
    val tournamentSecondsLeft: Int = 48,
    val notificationMessage: String? = null
)

class BingoViewModel(application: Application) : AndroidViewModel(application) {

    val soundManager = SoundManager(application)
    val repository: FirestoreRepository? = try {
        if (AuthManager.isFirebaseInitialized(application)) {
            FirestoreRepository(application)
        } else {
            null
        }
    } catch (e: Exception) {
        Log.w("BingoViewModel", "Firestore unavailable at startup: ${e.message}")
        null
    }

    private val _uiState = MutableStateFlow(BingoUiState())
    val uiState: StateFlow<BingoUiState> = _uiState.asStateFlow()

    private var gameJob: Job? = null
    private var roomSyncJob: Job? = null
    private var playersJob: Job? = null
    private var countdownJob: Job? = null
    private var roomsObserveJob: Job? = null
    private var leaderboardObserveJob: Job? = null
    private var profileObserveJob: Job? = null
    private var transactionsObserveJob: Job? = null

    private fun currentFirebaseUser(): FirebaseUser? {
        return try {
            if (AuthManager.isFirebaseInitialized(getApplication())) {
                Firebase.auth.currentUser
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("BingoViewModel", "Unable to check currentUser: ${e.message}")
            null
        }
    }

    init {
        initializeRooms()
        selectTicket(77)
        startTimers()

        if (currentFirebaseUser() != null) {
            handleGoogleSignInSuccess()
        }
    }

    private fun startCloudLobbySync() {
        val repo = repository ?: return
        if (currentFirebaseUser() == null) return

        viewModelScope.launch {
            try {
                repo.syncDefaultRooms()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("BingoViewModel", "syncDefaultRooms skipped: ${e.message}")
            }
        }

        roomsObserveJob?.cancel()
        roomsObserveJob = viewModelScope.launch {
            repo.observeRooms()
                .catch { error ->
                    if (error is CancellationException) throw error
                    Log.w("BingoViewModel", "observeRooms error: ${error.message}")
                }
                .collect { firestoreRooms ->
                    if (firestoreRooms.isNotEmpty()) {
                        val orderedIds = listOf("mini_starter", "standard", "high_roller", "mega_addis", "habesha_vip")
                        val mappedRooms = firestoreRooms.map { fr ->
                            val badgeColor = when (fr.id) {
                                "mini_starter" -> 0xFF20C997
                                "standard" -> 0xFFFFC107
                                "high_roller" -> 0xFFFF5722
                                "mega_addis" -> 0xFFFFC107
                                "habesha_vip" -> 0xFFFF5722
                                else -> 0xFF20C997
                            }
                            val mins = fr.countdownSeconds / 60
                            val secs = fr.countdownSeconds % 60
                            val timeStr = String.format("%02d:%02d", mins, secs)
                            val statusText = when {
                                fr.status == "IN_PROGRESS" -> "Game Live 🔴"
                                fr.currentPlayers >= 2 -> "$timeStr left"
                                else -> "$timeStr (min 2 req)"
                            }
                            val pool = fr.totalPoolEtb.toInt()
                            Room(
                                id = fr.id,
                                name = fr.name,
                                stakeEtb = fr.stakeEtb.toInt(),
                                totalPoolEtb = pool,
                                prizeEtb = (pool * 80) / 100, // 80% to winner
                                houseCommissionEtb = (pool * 20) / 100, // 20% house commission
                                currentPlayers = fr.currentPlayers.toInt(),
                                maxPlayers = 100,
                                countdownSeconds = fr.countdownSeconds.toInt(),
                                statusText = statusText,
                                badgeColorHex = badgeColor
                            )
                        }.sortedBy { r ->
                            val idx = orderedIds.indexOf(r.id)
                            if (idx >= 0) idx else 99
                        }

                        _uiState.update { st ->
                            val updatedSel = st.selectedRoom?.let { sel ->
                                mappedRooms.find { it.id == sel.id } ?: sel
                            }
                            st.copy(rooms = mappedRooms, selectedRoom = updatedSel)
                        }
                    }
                }
        }

        leaderboardObserveJob?.cancel()
        leaderboardObserveJob = viewModelScope.launch {
            repo.observeLeaderboard()
                .catch { error ->
                    if (error is CancellationException) throw error
                    Log.w("BingoViewModel", "observeLeaderboard error: ${error.message}")
                }
                .collect { entries ->
                    val curUid = currentFirebaseUser()?.uid
                    val mapped = entries.mapIndexed { idx, item ->
                        val isMe = (curUid != null && item.userId == curUid)
                        LeaderboardEntry(
                            rank = idx + 1,
                            name = if (isMe) "${item.name} (YOU)" else item.name,
                            phoneMasked = if (isMe) "YOU" else if (item.city.isNotBlank()) "📍 ${item.city}" else "Ethiopia",
                            wins = item.gamesWon.toInt(),
                            prizeEtb = item.wonEtb.toInt(),
                            isCurrentUser = isMe
                        )
                    }
                    _uiState.update { st ->
                        st.copy(
                            tournamentLeaderboards = mapOf(
                                TournamentTab.ENKUTATASH to mapped,
                                TournamentTab.DAILY to mapped,
                                TournamentTab.WEEKLY to mapped,
                                TournamentTab.MONTHLY to mapped
                            )
                        )
                    }
                }
        }
    }

    private fun initializeRooms() {
        val defaultRooms = listOf(
            Room(
                id = "mini_starter",
                name = "Mini Starter",
                stakeEtb = 10,
                totalPoolEtb = 600,
                prizeEtb = 480, // 80%
                houseCommissionEtb = 120, // 20%
                currentPlayers = 0,
                maxPlayers = 60,
                countdownSeconds = 180,
                statusText = "03:00 left",
                badgeColorHex = 0xFF20C997
            ),
            Room(
                id = "standard",
                name = "Standard Room",
                stakeEtb = 20,
                totalPoolEtb = 2000,
                prizeEtb = 1600, // 80%
                houseCommissionEtb = 400, // 20%
                currentPlayers = 0,
                maxPlayers = 100,
                countdownSeconds = 180,
                statusText = "03:00 left",
                badgeColorHex = 0xFFFFC107
            ),
            Room(
                id = "high_roller",
                name = "High Roller Suite",
                stakeEtb = 50,
                totalPoolEtb = 5000,
                prizeEtb = 4000, // 80%
                houseCommissionEtb = 1000, // 20%
                currentPlayers = 0,
                maxPlayers = 100,
                countdownSeconds = 180,
                statusText = "03:00 left",
                badgeColorHex = 0xFFFF5722
            ),
            Room(
                id = "mega_addis",
                name = "Mega Addis Jackpot",
                stakeEtb = 100,
                totalPoolEtb = 10000,
                prizeEtb = 8000, // 80%
                houseCommissionEtb = 2000, // 20%
                currentPlayers = 0,
                maxPlayers = 100,
                countdownSeconds = 180,
                statusText = "03:00 left",
                badgeColorHex = 0xFFFFC107
            ),
            Room(
                id = "habesha_vip",
                name = "Habesha VIP Club",
                stakeEtb = 200,
                totalPoolEtb = 30000,
                prizeEtb = 24000, // 80%
                houseCommissionEtb = 6000, // 20%
                currentPlayers = 0,
                maxPlayers = 150,
                countdownSeconds = 180,
                statusText = "03:00 left",
                badgeColorHex = 0xFFFF5722
            )
        )
        _uiState.update { it.copy(rooms = defaultRooms) }
    }

    private fun startTimers() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                var shouldAutoStart = false
                var roomNameForAutoStart = ""

                _uiState.update { state ->
                    val nextJackpot = if (state.jackpotSecondsLeft <= 0) 300 else state.jackpotSecondsLeft - 1
                    val nextTournamentSec = if (state.tournamentSecondsLeft <= 0) 59 else state.tournamentSecondsLeft - 1

                    // Countdown 3 minutes for each room
                    val updatedRooms = state.rooms.map { room ->
                        val nextSec = room.countdownSeconds - 1
                        if (nextSec <= 0) {
                            // 3-minute timer reached 0
                            if (room.currentPlayers >= 2) {
                                // 2 or more players joined -> Auto-start game!
                                if (state.playView == PlayView.TICKET_SELECTION && state.selectedRoom?.id == room.id) {
                                    shouldAutoStart = true
                                    roomNameForAutoStart = room.name
                                }
                                room.copy(
                                    countdownSeconds = 180,
                                    statusText = "03:00 left"
                                )
                            } else {
                                // Fewer than 2 players -> Reset to 3 minutes and wait
                                room.copy(
                                    countdownSeconds = 180,
                                    statusText = "Waiting for min 2 players"
                                )
                            }
                        } else {
                            val mins = nextSec / 60
                            val secs = nextSec % 60
                            val timeStr = String.format("%02d:%02d", mins, secs)
                            val status = if (room.currentPlayers >= 2) "$timeStr left" else "$timeStr (min 2 req)"
                            room.copy(
                                countdownSeconds = nextSec,
                                statusText = status
                            )
                        }
                    }

                    val updatedSelectedRoom = state.selectedRoom?.let { sel ->
                        updatedRooms.find { it.id == sel.id } ?: sel
                    }

                    state.copy(
                        rooms = updatedRooms,
                        selectedRoom = updatedSelectedRoom,
                        jackpotSecondsLeft = nextJackpot,
                        tournamentSecondsLeft = nextTournamentSec
                    )
                }

                if (shouldAutoStart) {
                    _uiState.update {
                        it.copy(notificationMessage = "3-minute countdown reached! $roomNameForAutoStart auto-started with 2+ players.")
                    }
                    startLiveGame()
                }
            }
        }
    }

    fun switchTab(tab: NavTab) {
        soundManager.playButtonTap()
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setTournamentTab(tab: TournamentTab) {
        soundManager.playButtonTap()
        _uiState.update { it.copy(tournamentTab = tab) }
    }

    fun enterTicketSelection(room: Room) {
        soundManager.playButtonTap()
        _uiState.update {
            it.copy(
                selectedRoom = room,
                playView = PlayView.TICKET_SELECTION
            )
        }
        val user = currentFirebaseUser()
        val repo = repository
        if (user != null && repo != null) {
            viewModelScope.launch {
                try {
                    repo.joinRoom(
                        roomId = room.id,
                        userId = user.uid,
                        name = _uiState.value.userProfile.name,
                        ticketNumber = _uiState.value.selectedTicketNumber,
                        stakeEtb = room.stakeEtb
                    )
                } catch (_: Exception) {}
            }
            observeRoomPlayers(room.id)
        }
    }

    fun backToLobby() {
        soundManager.playButtonTap()
        gameJob?.cancel()
        roomSyncJob?.cancel()
        playersJob?.cancel()
        val curRoom = _uiState.value.selectedRoom
        val user = currentFirebaseUser()
        val repo = repository
        if (curRoom != null && user != null && repo != null) {
            viewModelScope.launch {
                try {
                    repo.leaveRoom(curRoom.id, user.uid)
                } catch (_: Exception) {}
            }
        }
        _uiState.update {
            it.copy(
                playView = PlayView.LOBBY,
                isCalling = false
            )
        }
    }

    fun selectTicket(ticketNumber: Int) {
        soundManager.playButtonTap()
        val card = generateCardForTicket(ticketNumber)
        _uiState.update {
            it.copy(
                selectedTicketNumber = ticketNumber,
                currentCard = card
            )
        }
    }

    fun refreshCard() {
        soundManager.playButtonTap()
        val currentTicket = _uiState.value.selectedTicketNumber
        val newCard = generateCardForTicket(currentTicket, randomize = true)
        _uiState.update { it.copy(currentCard = newCard) }
    }

    private fun generateCardForTicket(ticketNumber: Int, randomize: Boolean = false): BingoCard {
        // Standard 75-Ball Bingo:
        // B: 1-15, I: 16-30, N: 31-45, G: 46-60, O: 61-75
        val seed = if (randomize) Random.nextInt(10000) else ticketNumber * 37
        val rng = Random(seed)

        val ranges = listOf(
            1..15,   // B (1-15)
            16..30,  // I (16-30)
            31..45,  // N (31-45, center is FREE)
            46..60,  // G (46-60)
            61..75   // O (61-75)
        )

        val columns = ranges.map { range ->
            val pool = range.toMutableList()
            val colNums = mutableListOf<Int>()
            repeat(5) {
                val index = rng.nextInt(pool.size)
                colNums.add(pool.removeAt(index))
            }
            colNums
        }

        val rows = (0 until 5).map { r ->
            (0 until 5).map { c ->
                if (r == 2 && c == 2) {
                    BingoCell(number = 0, isFree = true, isMarked = true, isWinning = false)
                } else {
                    BingoCell(
                        number = columns[c][r],
                        isFree = false,
                        isMarked = false,
                        isWinning = false
                    )
                }
            }
        }

        return BingoCard(ticketNumber = ticketNumber, grid = rows)
    }

    private fun observeRoomPlayers(roomId: String) {
        val repo = repository ?: return
        playersJob?.cancel()
        playersJob = viewModelScope.launch {
            repo.observeRoomPlayers(roomId)
                .catch { error ->
                    if (error is CancellationException) throw error
                    Log.w("BingoViewModel", "observeRoomPlayers error: ${error.message}")
                }
                .collect { players ->
                    _uiState.update { it.copy(activeRoomPlayers = players) }
                }
        }
    }

    fun startLiveGame() {
        val state = _uiState.value
        val room = state.selectedRoom ?: return
        val user = currentFirebaseUser()

        if (state.userProfile.balanceEtb < room.stakeEtb) {
            _uiState.update {
                it.copy(
                    showDepositModal = true,
                    notificationMessage = "Insufficient balance! Please top up your wallet."
                )
            }
            return
        }

        // Deduct stake
        val newBalance = state.userProfile.balanceEtb - room.stakeEtb
        val freshCard = generateCardForTicket(state.selectedTicketNumber, randomize = true)

        _uiState.update {
            it.copy(
                userProfile = it.userProfile.copy(
                    balanceEtb = newBalance,
                    gamesPlayed = it.userProfile.gamesPlayed + 1
                ),
                currentCard = freshCard,
                playView = PlayView.GAMEPLAY,
                calledNumbers = emptyList(),
                latestCalledBall = null,
                latestAmharicAnnouncement = null,
                isCalling = true,
                winCelebration = null
            )
        }

        soundManager.playRoundStart()

        val repo = repository
        if (user != null && repo != null) {
            viewModelScope.launch {
                try {
                    repo.recordTransaction(
                        userId = user.uid,
                        transaction = FirestoreTransaction(
                            id = "tx_${System.currentTimeMillis()}",
                            userId = user.uid,
                            type = "BET",
                            amountEtb = room.stakeEtb.toLong(),
                            status = "COMPLETED",
                            paymentMethod = "STAKE",
                            reference = "ROOM-${room.id}"
                        )
                    )
                    repo.updateUserProfile(
                        FirestoreUserProfile(
                            userId = user.uid,
                            name = state.userProfile.name,
                            phone = state.userProfile.phoneMasked,
                            city = state.userProfile.location,
                            balanceEtb = newBalance.toLong(),
                            gamesPlayed = (state.userProfile.gamesPlayed + 1).toLong(),
                            totalEarningsEtb = state.userProfile.totalWinningsEtb.toLong()
                        )
                    )
                } catch (_: Exception) {}
            }
        }

        startMultiplayerGameSession(room.id)
    }

    private fun startMultiplayerGameSession(roomId: String) {
        gameJob?.cancel()
        roomSyncJob?.cancel()

        val currentUid = currentFirebaseUser()?.uid ?: "local_player"
        val repo = repository

        // 1. Synchronize real-time room state from Firestore (called balls, winner propagation)
        if (repo != null && currentFirebaseUser() != null) {
            roomSyncJob = viewModelScope.launch {
                repo.observeRoom(roomId)
                    .catch { error ->
                        if (error is CancellationException) throw error
                        Log.w("BingoViewModel", "observeRoom error: ${error.message}")
                    }
                    .collect { liveRoom ->
                        if (liveRoom == null) return@collect

                        // Check if another player in this room won BINGO
                        if (liveRoom.status == "FINISHED" && liveRoom.winnerId.isNotBlank()) {
                            if (liveRoom.winnerId != currentUid) {
                                gameJob?.cancel()
                                _uiState.update {
                                    it.copy(
                                        isCalling = false,
                                        notificationMessage = "🎉 BINGO! ${liveRoom.winnerName} won ${liveRoom.prizeEtb} ETB!"
                                    )
                                }
                            }
                        }

                        // Sync called balls across devices
                        val firestoreNumbers = liveRoom.calledNumbers.map { it.toInt() }
                        val currentCalled = _uiState.value.calledNumbers

                        if (firestoreNumbers.size > currentCalled.size) {
                            val newNumbers = firestoreNumbers.drop(currentCalled.size)
                            for (num in newNumbers) {
                                handleNextBallCalled(num)
                            }
                        }
                    }
            }
        }

        // 2. Room Caller loop:
        // Host device generates and updates balls into the shared Firestore room document (or local fallback when offline)
        gameJob = viewModelScope.launch {
            delay(1000)
            val poolNumbers = (1..75).toMutableList()
            poolNumbers.shuffle()

            val calledSoFar = mutableListOf<Long>()

            while (isActive && poolNumbers.isNotEmpty()) {
                val intervalMs = if (_uiState.value.isFastCallerSpeed) 1800L else 2600L
                delay(intervalMs) // Fast or standard caller cadence

                if (repo != null && currentFirebaseUser() != null) {
                    val players = _uiState.value.activeRoomPlayers
                    val isHost = players.isEmpty() || players.minByOrNull { it.userId }?.userId == currentUid

                    if (isHost && _uiState.value.winCelebration == null) {
                        val next = poolNumbers.removeAt(0).toLong()
                        calledSoFar.add(next)
                        try {
                            repo.updateRoomState(
                                roomId = roomId,
                                countdownSeconds = 0L,
                                status = "IN_PROGRESS",
                                calledNumbers = calledSoFar.toList(),
                                latestBall = next
                            )
                        } catch (_: Exception) {}
                    }
                } else if (_uiState.value.winCelebration == null) {
                    val next = poolNumbers.removeAt(0)
                    handleNextBallCalled(next)
                }

                if (_uiState.value.winCelebration != null) {
                    break
                }
            }
        }
    }

    private fun handleNextBallCalled(ball: Int) {
        val callout = com.example.sound.AmharicCaller.getCallout(ball)
        soundManager.playBallCall(ball)

        _uiState.update { state ->
            val updatedCalled = state.calledNumbers + ball
            var updatedCard = state.currentCard

            // Auto-daub if enabled
            if (state.isAutoDaub && updatedCard != null) {
                var daubed = false
                val newGrid = updatedCard.grid.map { row ->
                    row.map { cell ->
                        if (cell.number == ball) {
                            daubed = true
                            cell.copy(isMarked = true)
                        } else {
                            cell
                        }
                    }
                }
                if (daubed) {
                    soundManager.playDaub()
                }
                updatedCard = updatedCard.copy(grid = newGrid)
            }

            state.copy(
                calledNumbers = updatedCalled,
                latestCalledBall = ball,
                latestAmharicAnnouncement = callout.amharicDisplayText,
                currentCard = updatedCard
            )
        }

        // Always check win after auto-daubing
        if (_uiState.value.isAutoDaub) {
            checkWinCondition()
        }
    }

    fun toggleAutoDaub() {
        soundManager.playButtonTap()
        _uiState.update { it.copy(isAutoDaub = !it.isAutoDaub) }
    }

    fun daubCell(row: Int, col: Int) {
        val state = _uiState.value
        val card = state.currentCard ?: return
        val cell = card.grid[row][col]

        if (cell.isFree || cell.isMarked) return

        // Only allow daubing if number has actually been called
        if (state.calledNumbers.contains(cell.number)) {
            soundManager.playDaub()
            val newGrid = card.grid.mapIndexed { r, rowCells ->
                rowCells.mapIndexed { c, item ->
                    if (r == row && c == col) item.copy(isMarked = true) else item
                }
            }
            _uiState.update { it.copy(currentCard = card.copy(grid = newGrid)) }
            checkWinCondition()
        }
    }

    private fun checkWinCondition() {
        val state = _uiState.value
        val card = state.currentCard ?: return
        val grid = card.grid

        var winningReason: String? = null
        val winningCoords = mutableSetOf<Pair<Int, Int>>()

        // Check 5 rows
        for (r in 0 until 5) {
            if (grid[r].all { it.isMarked }) {
                winningReason = "Horizontal Line Row ${r + 1} Completed!"
                for (c in 0 until 5) winningCoords.add(Pair(r, c))
                break
            }
        }

        // Check 5 columns
        if (winningReason == null) {
            for (c in 0 until 5) {
                if ((0 until 5).all { r -> grid[r][c].isMarked }) {
                    val colLetters = listOf('B', 'I', 'N', 'G', 'O')
                    winningReason = "Vertical Column ${colLetters[c]} Completed!"
                    for (r in 0 until 5) winningCoords.add(Pair(r, c))
                    break
                }
            }
        }

        // Check main diagonal
        if (winningReason == null) {
            if ((0 until 5).all { i -> grid[i][i].isMarked }) {
                winningReason = "Main Diagonal Line Completed!"
                for (i in 0 until 5) winningCoords.add(Pair(i, i))
            }
        }

        // Check anti diagonal
        if (winningReason == null) {
            if ((0 until 5).all { i -> grid[i][4 - i].isMarked }) {
                winningReason = "Anti-Diagonal Line Completed!"
                for (i in 0 until 5) winningCoords.add(Pair(i, 4 - i))
            }
        }

        if (winningReason != null) {
            // Player won!
            gameJob?.cancel()
            soundManager.playWin()

            val room = state.selectedRoom
            val prize = room?.prizeEtb ?: 480
            val user = currentFirebaseUser()

            // Highlight winning cells
            val winningGrid = grid.mapIndexed { r, rowCells ->
                rowCells.mapIndexed { c, cell ->
                    if (winningCoords.contains(Pair(r, c))) cell.copy(isWinning = true) else cell
                }
            }

            val newBalance = state.userProfile.balanceEtb + prize
            val newTotalWinnings = state.userProfile.totalWinningsEtb + prize
            val newWinRate = (newTotalWinnings.toFloat() / (state.userProfile.gamesPlayed * 50f)).coerceIn(15f, 65f)

            val updatedProfile = state.userProfile.copy(
                balanceEtb = newBalance,
                totalWinningsEtb = newTotalWinnings,
                winRatePercent = (newWinRate * 10).toInt() / 10f
            )

            // Claim win in Firestore so prize is credited and all devices see winner
            val repo = repository
            if (user != null && room != null && repo != null) {
                viewModelScope.launch {
                    try {
                        repo.claimBingoWin(
                            roomId = room.id,
                            userId = user.uid,
                            winnerName = state.userProfile.name,
                            prizeEtb = prize.toLong(),
                            city = state.userProfile.location
                        )
                    } catch (_: Exception) {}
                }
            }

            val winTx = TransactionItem(
                id = "tx_win_${System.currentTimeMillis()}",
                title = "Bingo Win: ${room?.name ?: "Room"}",
                amountEtb = prize,
                isCredit = true,
                timestampFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                method = "Game Reward"
            )

            _uiState.update {
                it.copy(
                    isCalling = false,
                    currentCard = card.copy(grid = winningGrid),
                    userProfile = updatedProfile,
                    transactions = listOf(winTx) + it.transactions,
                    winCelebration = WinCelebrationData(
                        reason = winningReason,
                        payoutEtb = prize
                    )
                )
            }
        }
    }

    fun dismissWinModal() {
        soundManager.playButtonTap()
        _uiState.update {
            it.copy(
                winCelebration = null,
                playView = PlayView.LOBBY
            )
        }
    }

    fun openDepositModal() {
        soundManager.playButtonTap()
        _uiState.update { it.copy(showDepositModal = true, showWithdrawModal = false) }
    }

    fun openWithdrawModal() {
        soundManager.playButtonTap()
        _uiState.update { it.copy(showWithdrawModal = true, showDepositModal = false) }
    }

    fun closeModal() {
        soundManager.playButtonTap()
        _uiState.update { it.copy(showDepositModal = false, showWithdrawModal = false) }
    }

    fun confirmDeposit(amount: Int, method: PaymentMethod, phoneNumber: String) {
        if (amount <= 0) return
        val newBalance = _uiState.value.userProfile.balanceEtb + amount
        val newTx = TransactionItem(
            id = "tx_dep_${System.currentTimeMillis()}",
            title = "Deposit via ${method.displayName}",
            amountEtb = amount,
            isCredit = true,
            timestampFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
            method = method.displayName
        )
        soundManager.playWin()
        _uiState.update {
            it.copy(
                userProfile = it.userProfile.copy(balanceEtb = newBalance),
                transactions = listOf(newTx) + it.transactions,
                showDepositModal = false,
                notificationMessage = "Successfully credited +$amount ETB via ${method.displayName} ($phoneNumber)!"
            )
        }
        val user = currentFirebaseUser()
        val repo = repository
        if (user != null && repo != null) {
            viewModelScope.launch {
                try {
                    repo.recordTransaction(
                        userId = user.uid,
                        transaction = FirestoreTransaction(
                            id = newTx.id,
                            userId = user.uid,
                            type = "DEPOSIT",
                            amountEtb = amount.toLong(),
                            status = "COMPLETED",
                            paymentMethod = method.name,
                            reference = phoneNumber
                        )
                    )
                    repo.updateUserProfile(
                        FirestoreUserProfile(
                            userId = user.uid,
                            name = _uiState.value.userProfile.name,
                            phone = _uiState.value.userProfile.phoneMasked,
                            city = _uiState.value.userProfile.location,
                            balanceEtb = newBalance.toLong(),
                            gamesPlayed = _uiState.value.userProfile.gamesPlayed.toLong(),
                            totalEarningsEtb = _uiState.value.userProfile.totalWinningsEtb.toLong()
                        )
                    )
                } catch (_: Exception) {}
            }
        }
    }

    fun confirmWithdraw(amount: Int, method: PaymentMethod, phoneNumber: String) {
        if (amount <= 0) return
        val currentBalance = _uiState.value.userProfile.balanceEtb
        if (amount > currentBalance) {
            _uiState.update { it.copy(notificationMessage = "Withdrawal amount exceeds balance!") }
            return
        }
        val newBalance = currentBalance - amount
        val newTx = TransactionItem(
            id = "tx_wd_${System.currentTimeMillis()}",
            title = "Cashout to ${method.displayName}",
            amountEtb = amount,
            isCredit = false,
            timestampFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
            method = method.displayName
        )
        soundManager.playButtonTap()
        _uiState.update {
            it.copy(
                userProfile = it.userProfile.copy(balanceEtb = newBalance),
                transactions = listOf(newTx) + it.transactions,
                showWithdrawModal = false,
                notificationMessage = "Cashout of $amount ETB sent to ${method.displayName} ($phoneNumber) successfully!"
            )
        }
        val user = currentFirebaseUser()
        val repo = repository
        if (user != null && repo != null) {
            viewModelScope.launch {
                try {
                    repo.recordTransaction(
                        userId = user.uid,
                        transaction = FirestoreTransaction(
                            id = newTx.id,
                            userId = user.uid,
                            type = "WITHDRAW",
                            amountEtb = amount.toLong(),
                            status = "COMPLETED",
                            paymentMethod = method.name,
                            reference = phoneNumber
                        )
                    )
                    repo.updateUserProfile(
                        FirestoreUserProfile(
                            userId = user.uid,
                            name = _uiState.value.userProfile.name,
                            phone = _uiState.value.userProfile.phoneMasked,
                            city = _uiState.value.userProfile.location,
                            balanceEtb = newBalance.toLong(),
                            gamesPlayed = _uiState.value.userProfile.gamesPlayed.toLong(),
                            totalEarningsEtb = _uiState.value.userProfile.totalWinningsEtb.toLong()
                        )
                    )
                } catch (_: Exception) {}
            }
        }
    }

    fun claimDailyReward() {
        if (_uiState.value.userProfile.isDailyClaimed) {
            _uiState.update { it.copy(notificationMessage = "Daily bonus already claimed for today!") }
            return
        }
        soundManager.playWin()
        val bonus = 50
        val current = _uiState.value.userProfile
        val newProfile = current.copy(
            balanceEtb = current.balanceEtb + bonus,
            dailyStreakDays = current.dailyStreakDays + 1,
            isDailyClaimed = true
        )
        val newTx = TransactionItem(
            id = "tx_daily_${System.currentTimeMillis()}",
            title = "Daily Free Tier Reward",
            amountEtb = bonus,
            isCredit = true,
            timestampFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
            method = "Daily Bonus"
        )
        _uiState.update {
            it.copy(
                userProfile = newProfile,
                transactions = listOf(newTx) + it.transactions,
                notificationMessage = "Daily bonus +$bonus ETB claimed! Streak: ${newProfile.dailyStreakDays} days 🔥"
            )
        }
        val user = currentFirebaseUser()
        val repo = repository
        if (user != null && repo != null) {
            viewModelScope.launch {
                try {
                    repo.recordTransaction(
                        userId = user.uid,
                        transaction = FirestoreTransaction(
                            id = newTx.id,
                            userId = user.uid,
                            type = "DEPOSIT",
                            amountEtb = bonus.toLong(),
                            status = "COMPLETED",
                            paymentMethod = "DAILY_REWARD",
                            reference = "DAY_${newProfile.dailyStreakDays}"
                        )
                    )
                    repo.updateUserProfile(
                        FirestoreUserProfile(
                            userId = user.uid,
                            name = newProfile.name,
                            phone = newProfile.phoneMasked,
                            city = newProfile.location,
                            balanceEtb = newProfile.balanceEtb.toLong(),
                            gamesPlayed = newProfile.gamesPlayed.toLong(),
                            totalEarningsEtb = newProfile.totalWinningsEtb.toLong()
                        )
                    )
                } catch (_: Exception) {}
            }
        }
    }

    fun login(phoneOrUsername: String, pin: String) {
        val cleanInput = phoneOrUsername.trim()
        val phoneDigits = cleanInput.filter { it.isDigit() }
        val cleanPhone = if (phoneDigits.startsWith("0")) phoneDigits else "0$phoneDigits"
        val displaySuffix = if (cleanPhone.length >= 4) cleanPhone.takeLast(4) else "Player"
        val profile = UserProfile(
            name = "Player $displaySuffix",
            phoneMasked = if (cleanPhone.length >= 9) "+251${cleanPhone.takeLast(9).replaceRange(4, 7, "****")}" else "+2519****777",
            location = "Addis Ababa",
            vipLevel = 1,
            balanceEtb = 250,
            promoCode = "YEM-PLAYER-77"
        )
        soundManager.playWin()
        _uiState.update {
            it.copy(
                isLoggedIn = true,
                userProfile = profile,
                notificationMessage = "Welcome back, ${profile.name}!"
            )
        }
    }

    fun register(fullName: String, phone: String, city: String, pin: String, promoCode: String) {
        val welcomeBonus = if (promoCode.isNotBlank()) 300 else 250
        val phoneDigits = phone.filter { it.isDigit() }
        val cleanPhone = if (phoneDigits.startsWith("0")) phoneDigits else "0$phoneDigits"
        val profile = UserProfile(
            name = fullName,
            phoneMasked = if (cleanPhone.length >= 9) "+251${cleanPhone.takeLast(9).replaceRange(4, 7, "****")}" else "+2519****100",
            location = if (city.isNotBlank()) city else "Addis Ababa",
            vipLevel = 1,
            balanceEtb = welcomeBonus,
            gamesPlayed = 0,
            totalWinningsEtb = 0,
            winRatePercent = 0.0f,
            dailyStreakDays = 1,
            promoCode = "YEM-${fullName.filter { it.isLetter() }.take(4).uppercase()}-77",
            isDailyClaimed = false
        )
        val bonusTx = TransactionItem(
            id = "tx_welcome_${System.currentTimeMillis()}",
            title = "New Player Welcome Bonus",
            amountEtb = welcomeBonus,
            isCredit = true,
            timestampFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
            method = "Welcome Reward"
        )
        soundManager.playWin()
        _uiState.update {
            it.copy(
                isLoggedIn = true,
                userProfile = profile,
                transactions = listOf(bonusTx) + it.transactions,
                notificationMessage = "Account created! Welcome bonus of +$welcomeBonus ETB credited! 🎁"
            )
        }
    }

    fun handleGoogleSignInSuccess() {
        val user = currentFirebaseUser() ?: return
        val repo = repository ?: return
        startCloudLobbySync()

        viewModelScope.launch {
            try {
                val name = user.displayName?.ifBlank { null } ?: "Habesha Player"
                val phone = user.phoneNumber?.ifBlank { null } ?: "+251911000000"
                val profile = repo.createInitialUserProfile(
                    userId = user.uid,
                    name = name,
                    phone = phone,
                    city = "Addis Ababa"
                )
                soundManager.playWin()
                _uiState.update {
                    it.copy(
                        isLoggedIn = true,
                        userProfile = UserProfile(
                            name = profile.name,
                            phoneMasked = if (profile.phone.length >= 9) "+251${profile.phone.takeLast(9).replaceRange(4, 7, "****")}" else "+2519****777",
                            location = profile.city,
                            balanceEtb = profile.balanceEtb.toInt(),
                            gamesPlayed = profile.gamesPlayed.toInt(),
                            totalWinningsEtb = profile.totalEarningsEtb.toInt()
                        ),
                        notificationMessage = "Signed in with Google! Connected to Firebase Cloud."
                    )
                }

                // Stream real-time user profile from Firestore
                profileObserveJob?.cancel()
                profileObserveJob = viewModelScope.launch {
                    repo.observeUserProfile(user.uid)
                        .catch { error ->
                            if (error is CancellationException) throw error
                            Log.w("BingoViewModel", "observeUserProfile error: ${error.message}")
                        }
                        .collect { updated ->
                            if (updated != null) {
                                _uiState.update { st ->
                                    st.copy(
                                        userProfile = st.userProfile.copy(
                                            name = updated.name,
                                            balanceEtb = updated.balanceEtb.toInt(),
                                            gamesPlayed = updated.gamesPlayed.toInt(),
                                            totalWinningsEtb = updated.totalEarningsEtb.toInt()
                                        )
                                    )
                                }
                            }
                        }
                }

                // Stream real transactions from Firestore
                transactionsObserveJob?.cancel()
                transactionsObserveJob = viewModelScope.launch {
                    repo.observeTransactions(user.uid)
                        .catch { error ->
                            if (error is CancellationException) throw error
                            Log.w("BingoViewModel", "observeTransactions error: ${error.message}")
                        }
                        .collect { firestoreTxs ->
                            val mappedTxs = firestoreTxs.map { tx ->
                                val isCredit = tx.type in listOf("DEPOSIT", "WIN")
                                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                                val dateStr = tx.createdAt?.toDate()?.let { sdf.format(it) } ?: "Just now"
                                TransactionItem(
                                    id = tx.id,
                                    title = when (tx.type) {
                                        "WIN" -> "Bingo Win (${tx.reference})"
                                        "BET" -> "Room Stake (${tx.reference})"
                                        "WITHDRAW" -> "Cashout (${tx.paymentMethod})"
                                        else -> "Deposit via ${tx.paymentMethod}"
                                    },
                                    amountEtb = tx.amountEtb.toInt(),
                                    isCredit = isCredit,
                                    timestampFormatted = dateStr,
                                    method = tx.paymentMethod
                                )
                            }
                            _uiState.update { it.copy(transactions = mappedTxs) }
                        }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(notificationMessage = "Firebase sync: ${e.localizedMessage}") }
            }
        }
    }

    fun logout() {
        soundManager.playButtonTap()
        gameJob?.cancel()
        roomSyncJob?.cancel()
        playersJob?.cancel()
        roomsObserveJob?.cancel()
        leaderboardObserveJob?.cancel()
        profileObserveJob?.cancel()
        transactionsObserveJob?.cancel()
        try {
            if (AuthManager.isFirebaseInitialized(getApplication())) {
                Firebase.auth.signOut()
            }
        } catch (_: Exception) {}
        _uiState.update {
            it.copy(
                isLoggedIn = false,
                playView = PlayView.LOBBY,
                selectedTab = NavTab.PLAY,
                isCalling = false,
                winCelebration = null,
                notificationMessage = "Logged out successfully"
            )
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        soundManager.isSoundEnabled = enabled
        _uiState.update { it.copy(isSoundEnabled = enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        soundManager.isVibrationEnabled = enabled
        _uiState.update { it.copy(isVibrationEnabled = enabled) }
    }

    fun setAmharicVoiceEnabled(enabled: Boolean) {
        soundManager.isAmharicVoiceEnabled = enabled
        _uiState.update { it.copy(isAmharicVoiceEnabled = enabled) }
    }

    fun setFastCallerSpeed(fast: Boolean) {
        soundManager.isFastCallerSpeed = fast
        _uiState.update { it.copy(isFastCallerSpeed = fast) }
    }

    fun clearNotification() {
        _uiState.update { it.copy(notificationMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        gameJob?.cancel()
        roomSyncJob?.cancel()
        playersJob?.cancel()
        countdownJob?.cancel()
        soundManager.release()
    }

    companion object {
        fun getLetterForBall(number: Int): String {
            return when (number) {
                in 1..15 -> "B"
                in 16..30 -> "I"
                in 31..45 -> "N"
                in 46..60 -> "G"
                in 61..75 -> "O"
                else -> ""
            }
        }
    }
}
