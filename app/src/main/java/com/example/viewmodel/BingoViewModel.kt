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
    val joinedRoomIds: Set<String> = emptySet(),
    val joinedCardsByRoom: Map<String, BingoCard> = emptyMap(),
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
    val isCallerMuted: Boolean = false,
    val callerVolume: Float = 0.9f,
    val nextBallCountdownSeconds: Int = 6,
    val callerVoiceProfile: String = "Iapetus",
    val callerStatusText: String = "Amharic Caller Man (Iapetus)",
    val amharicVoiceError: String? = null,
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
) {
    val isSelectedRoomJoined: Boolean
        get() = selectedRoom?.id?.let { it in joinedRoomIds } ?: false
}

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
        soundManager.setCallerStatusListener { status, errorMsg ->
            _uiState.update {
                it.copy(
                    callerStatusText = status,
                    amharicVoiceError = errorMsg
                )
            }
        }
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
                        var remoteAutoStartRoom: Room? = null
                        _uiState.update { st ->
                            val existingById = st.rooms.associateBy { it.id }
                            val mappedRooms = firestoreRooms.map { fr ->
                                val existing = existingById[fr.id]
                                val badgeColor = when (fr.id) {
                                    "mini_starter" -> 0xFF20C997
                                    "standard" -> 0xFFFFC107
                                    "high_roller" -> 0xFFFF5722
                                    "mega_addis" -> 0xFFFFC107
                                    "habesha_vip" -> 0xFFFF5722
                                    else -> 0xFF20C997
                                }
                                val isJoinedLocally = fr.id in st.joinedRoomIds
                                val playerCount = if (isJoinedLocally && fr.currentPlayers == 0L) {
                                    maxOf(1, existing?.currentPlayers ?: 1)
                                } else {
                                    fr.currentPlayers.toInt()
                                }
                                val liveCountdown = if (
                                    fr.status != "IN_PROGRESS" &&
                                    existing != null &&
                                    existing.countdownSeconds in 1..179
                                ) {
                                    existing.countdownSeconds
                                } else {
                                    fr.countdownSeconds.toInt().coerceIn(0, 180)
                                }
                                val mins = liveCountdown / 60
                                val secs = liveCountdown % 60
                                val timeStr = String.format("%02d:%02d", mins, secs)
                                val statusText = when {
                                    fr.status == "IN_PROGRESS" -> "Game Live 🔴"
                                    playerCount >= 2 -> "$timeStr • 2+ Ready"
                                    else -> "$timeStr (Need 2+)"
                                }
                                val pool = if (isJoinedLocally && fr.totalPoolEtb == 0L) {
                                    maxOf(fr.stakeEtb.toInt(), existing?.totalPoolEtb ?: fr.stakeEtb.toInt())
                                } else {
                                    fr.totalPoolEtb.toInt()
                                }
                                val room = Room(
                                    id = fr.id,
                                    name = fr.name,
                                    stakeEtb = fr.stakeEtb.toInt(),
                                    totalPoolEtb = pool,
                                    prizeEtb = (pool * 80) / 100, // 80% to winner
                                    houseCommissionEtb = (pool * 20) / 100, // 20% house commission
                                    currentPlayers = playerCount,
                                    maxPlayers = existing?.maxPlayers ?: 100,
                                    countdownSeconds = liveCountdown,
                                    statusText = statusText,
                                    badgeColorHex = badgeColor
                                )
                                if (
                                    remoteAutoStartRoom == null &&
                                    isJoinedLocally &&
                                    fr.status == "IN_PROGRESS" &&
                                    playerCount >= 2 &&
                                    st.playView != PlayView.GAMEPLAY
                                ) {
                                    remoteAutoStartRoom = room
                                }
                                room
                            }.sortedBy { r ->
                                val idx = orderedIds.indexOf(r.id)
                                if (idx >= 0) idx else 99
                            }

                            val updatedSel = st.selectedRoom?.let { sel ->
                                mappedRooms.find { it.id == sel.id } ?: sel
                            }
                            st.copy(rooms = mappedRooms, selectedRoom = updatedSel)
                        }

                        remoteAutoStartRoom?.let { roomToStart ->
                            launchJoinedGame(roomToStart)
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
                totalPoolEtb = 0,
                prizeEtb = 0,
                houseCommissionEtb = 0,
                currentPlayers = 0,
                maxPlayers = 60,
                countdownSeconds = 180,
                statusText = "03:00 (Need 2+)",
                badgeColorHex = 0xFF20C997
            ),
            Room(
                id = "standard",
                name = "Standard Room",
                stakeEtb = 20,
                totalPoolEtb = 0,
                prizeEtb = 0,
                houseCommissionEtb = 0,
                currentPlayers = 0,
                maxPlayers = 100,
                countdownSeconds = 180,
                statusText = "03:00 (Need 2+)",
                badgeColorHex = 0xFFFFC107
            ),
            Room(
                id = "high_roller",
                name = "High Roller Suite",
                stakeEtb = 50,
                totalPoolEtb = 0,
                prizeEtb = 0,
                houseCommissionEtb = 0,
                currentPlayers = 0,
                maxPlayers = 100,
                countdownSeconds = 180,
                statusText = "03:00 (Need 2+)",
                badgeColorHex = 0xFFFF5722
            ),
            Room(
                id = "mega_addis",
                name = "Mega Addis Jackpot",
                stakeEtb = 100,
                totalPoolEtb = 0,
                prizeEtb = 0,
                houseCommissionEtb = 0,
                currentPlayers = 0,
                maxPlayers = 100,
                countdownSeconds = 180,
                statusText = "03:00 (Need 2+)",
                badgeColorHex = 0xFFFFC107
            ),
            Room(
                id = "habesha_vip",
                name = "Habesha VIP Club",
                stakeEtb = 200,
                totalPoolEtb = 0,
                prizeEtb = 0,
                houseCommissionEtb = 0,
                currentPlayers = 0,
                maxPlayers = 150,
                countdownSeconds = 180,
                statusText = "03:00 (Need 2+)",
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
                processCountdownTick()
            }
        }
    }

    internal fun processCountdownTick() {
        var autoStartRoom: Room? = null
        var waitingNotification: String? = null

        _uiState.update { state ->
            val nextJackpot = if (state.jackpotSecondsLeft <= 0) 300 else state.jackpotSecondsLeft - 1
            val nextTournamentSec = if (state.tournamentSecondsLeft <= 0) 59 else state.tournamentSecondsLeft - 1

            // Countdown 3 minutes (180 seconds) for each room
            val updatedRooms = state.rooms.map { room ->
                val nextSec = room.countdownSeconds - 1
                if (nextSec <= 0) {
                    // 3-minute timer finished
                    if (room.currentPlayers >= 2) {
                        // 2 or more players joined -> Auto-start joined room!
                        if (
                            autoStartRoom == null &&
                            room.id in state.joinedRoomIds &&
                            state.playView != PlayView.GAMEPLAY
                        ) {
                            autoStartRoom = room
                        }
                        room.copy(
                            countdownSeconds = 180,
                            statusText = "03:00 • 2+ Ready"
                        )
                    } else {
                        // Fewer than 2 players -> Stay joined and restart 3-minute countdown
                        if (room.id in state.joinedRoomIds) {
                            waitingNotification =
                                "⏳ ${room.name}: ${room.currentPlayers}/2 players joined. Need 2+ players to start — timer reset to 03:00."
                        }
                        room.copy(
                            countdownSeconds = 180,
                            statusText = "03:00 (Need 2+)"
                        )
                    }
                } else {
                    val mins = nextSec / 60
                    val secs = nextSec % 60
                    val timeStr = String.format("%02d:%02d", mins, secs)
                    val status = if (room.currentPlayers >= 2) "$timeStr • 2+ Ready" else "$timeStr (Need 2+)"
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
                tournamentSecondsLeft = nextTournamentSec,
                notificationMessage = waitingNotification ?: state.notificationMessage
            )
        }

        autoStartRoom?.let { roomToStart ->
            _uiState.update {
                it.copy(
                    notificationMessage = "🎉 3-minute countdown finished! Starting ${roomToStart.name} with ${roomToStart.currentPlayers} joined players!"
                )
            }
            launchJoinedGame(roomToStart)
        }
    }

    internal fun setRoomStateForTesting(
        roomId: String,
        currentPlayers: Int? = null,
        countdownSeconds: Int? = null
    ) {
        _uiState.update { state ->
            val updated = state.rooms.map { room ->
                if (room.id == roomId) {
                    val p = currentPlayers ?: room.currentPlayers
                    val c = countdownSeconds ?: room.countdownSeconds
                    val pool = if (currentPlayers != null) p * room.stakeEtb else room.totalPoolEtb
                    room.copy(
                        currentPlayers = p,
                        totalPoolEtb = pool,
                        prizeEtb = (pool * 80) / 100,
                        houseCommissionEtb = (pool * 20) / 100,
                        countdownSeconds = c
                    )
                } else room
            }
            state.copy(
                rooms = updated,
                selectedRoom = state.selectedRoom?.let { sel -> updated.find { it.id == sel.id } ?: sel }
            )
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
        soundManager.warmAmharicVocabulary()
        _uiState.update { state ->
            val joinedCard = state.joinedCardsByRoom[room.id]
            state.copy(
                selectedRoom = room,
                playView = PlayView.TICKET_SELECTION,
                selectedTicketNumber = joinedCard?.ticketNumber ?: state.selectedTicketNumber,
                currentCard = joinedCard ?: state.currentCard ?: generateCardForTicket(state.selectedTicketNumber)
            )
        }
        val user = currentFirebaseUser()
        if (user != null && repository != null) {
            observeRoomPlayers(room.id)
        }
    }

    fun backToLobby() {
        soundManager.playButtonTap()
        val state = _uiState.value
        if (state.playView == PlayView.GAMEPLAY) {
            gameJob?.cancel()
            roomSyncJob?.cancel()
            playersJob?.cancel()
            val curRoom = state.selectedRoom
            val user = currentFirebaseUser()
            val repo = repository
            if (curRoom != null && user != null && repo != null) {
                viewModelScope.launch {
                    try {
                        repo.leaveRoom(curRoom.id, user.uid, 0)
                    } catch (_: Exception) {}
                }
            }
            _uiState.update {
                val roomId = curRoom?.id
                it.copy(
                    playView = PlayView.LOBBY,
                    isCalling = false,
                    joinedRoomIds = if (roomId != null) it.joinedRoomIds - roomId else it.joinedRoomIds,
                    joinedCardsByRoom = if (roomId != null) it.joinedCardsByRoom - roomId else it.joinedCardsByRoom
                )
            }
        } else {
            // Keep any joined room active when returning from Ticket Selection to Lobby
            _uiState.update {
                it.copy(
                    playView = PlayView.LOBBY,
                    isCalling = false
                )
            }
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

    fun joinRoomGame() {
        val state = _uiState.value
        val room = state.selectedRoom ?: return

        if (room.id in state.joinedRoomIds) {
            soundManager.playButtonTap()
            _uiState.update {
                it.copy(
                    playView = PlayView.LOBBY,
                    notificationMessage = "✓ Joined ${room.name}! (${room.currentPlayers} players joined)"
                )
            }
            return
        }

        if (state.userProfile.balanceEtb < room.stakeEtb) {
            _uiState.update {
                it.copy(
                    showDepositModal = true,
                    notificationMessage = "Insufficient balance! Please top up your wallet."
                )
            }
            return
        }

        soundManager.playButtonTap()
        soundManager.warmAmharicVocabulary()

        val newBalance = state.userProfile.balanceEtb - room.stakeEtb
        val joinedCard = state.currentCard ?: generateCardForTicket(state.selectedTicketNumber)
        val newTx = TransactionItem(
            id = "tx_bet_${System.currentTimeMillis()}",
            title = "Joined ${room.name}",
            amountEtb = room.stakeEtb,
            isCredit = false,
            timestampFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
            method = "Room Stake"
        )

        var newJoinedCount = room.currentPlayers + 1
        _uiState.update { st ->
            val updatedRooms = st.rooms.map { r ->
                if (r.id == room.id) {
                    val count = r.currentPlayers + 1
                    newJoinedCount = count
                    val pool = r.totalPoolEtb + r.stakeEtb
                    val mins = r.countdownSeconds / 60
                    val secs = r.countdownSeconds % 60
                    val timeStr = String.format("%02d:%02d", mins, secs)
                    val status = if (count >= 2) "$timeStr • 2+ Ready" else "$timeStr (Need 2+)"
                    r.copy(
                        currentPlayers = count,
                        totalPoolEtb = pool,
                        prizeEtb = (pool * 80) / 100,
                        houseCommissionEtb = (pool * 20) / 100,
                        statusText = status
                    )
                } else r
            }
            val updatedRoom = updatedRooms.find { it.id == room.id } ?: room
            st.copy(
                userProfile = st.userProfile.copy(balanceEtb = newBalance),
                transactions = listOf(newTx) + st.transactions,
                rooms = updatedRooms,
                selectedRoom = updatedRoom,
                joinedRoomIds = st.joinedRoomIds + room.id,
                joinedCardsByRoom = st.joinedCardsByRoom + (room.id to joinedCard),
                playView = PlayView.LOBBY,
                notificationMessage = "✓ Joined ${room.name}! ($newJoinedCount joined • Auto-starts after 3-min countdown with 2+ players)"
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
                        name = state.userProfile.name,
                        ticketNumber = joinedCard.ticketNumber,
                        stakeEtb = room.stakeEtb
                    )
                    repo.recordTransaction(
                        userId = user.uid,
                        transaction = FirestoreTransaction(
                            id = newTx.id,
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
                            gamesPlayed = state.userProfile.gamesPlayed.toLong(),
                            totalEarningsEtb = state.userProfile.totalWinningsEtb.toLong()
                        )
                    )
                } catch (_: Exception) {}
            }
        }
    }

    fun leaveJoinedRoom(roomId: String) {
        val state = _uiState.value
        if (roomId !in state.joinedRoomIds) return
        val room = state.rooms.find { it.id == roomId } ?: return
        soundManager.playButtonTap()

        val refundedBalance = state.userProfile.balanceEtb + room.stakeEtb
        _uiState.update { st ->
            val updatedRooms = st.rooms.map { r ->
                if (r.id == roomId) {
                    val count = (r.currentPlayers - 1).coerceAtLeast(0)
                    val pool = (r.totalPoolEtb - r.stakeEtb).coerceAtLeast(0)
                    val mins = r.countdownSeconds / 60
                    val secs = r.countdownSeconds % 60
                    val timeStr = String.format("%02d:%02d", mins, secs)
                    val status = if (count >= 2) "$timeStr • 2+ Ready" else "$timeStr (Need 2+)"
                    r.copy(
                        currentPlayers = count,
                        totalPoolEtb = pool,
                        prizeEtb = (pool * 80) / 100,
                        houseCommissionEtb = (pool * 20) / 100,
                        statusText = status
                    )
                } else r
            }
            st.copy(
                userProfile = st.userProfile.copy(balanceEtb = refundedBalance),
                rooms = updatedRooms,
                joinedRoomIds = st.joinedRoomIds - roomId,
                joinedCardsByRoom = st.joinedCardsByRoom - roomId,
                notificationMessage = "Left ${room.name}. ${room.stakeEtb} ETB refunded to wallet."
            )
        }

        val user = currentFirebaseUser()
        val repo = repository
        if (user != null && repo != null) {
            viewModelScope.launch {
                try {
                    repo.leaveRoom(roomId, user.uid, room.stakeEtb)
                    repo.updateUserProfile(
                        FirestoreUserProfile(
                            userId = user.uid,
                            name = state.userProfile.name,
                            phone = state.userProfile.phoneMasked,
                            city = state.userProfile.location,
                            balanceEtb = refundedBalance.toLong(),
                            gamesPlayed = state.userProfile.gamesPlayed.toLong(),
                            totalEarningsEtb = state.userProfile.totalWinningsEtb.toLong()
                        )
                    )
                } catch (_: Exception) {}
            }
        }
    }

    private fun launchJoinedGame(room: Room) {
        val state = _uiState.value
        val card = state.joinedCardsByRoom[room.id]
            ?: generateCardForTicket(state.selectedTicketNumber, randomize = false)
        launchGameplaySession(room = room, card = card, deductStake = false)
    }

    fun startLiveGame() {
        val state = _uiState.value
        val room = state.selectedRoom ?: return
        val card = generateCardForTicket(state.selectedTicketNumber, randomize = true)
        launchGameplaySession(room = room, card = card, deductStake = true)
    }

    private fun launchGameplaySession(room: Room, card: BingoCard, deductStake: Boolean) {
        val state = _uiState.value
        val user = currentFirebaseUser()

        if (deductStake && state.userProfile.balanceEtb < room.stakeEtb) {
            _uiState.update {
                it.copy(
                    showDepositModal = true,
                    notificationMessage = "Insufficient balance! Please top up your wallet."
                )
            }
            return
        }

        val newBalance = if (deductStake) state.userProfile.balanceEtb - room.stakeEtb else state.userProfile.balanceEtb

        soundManager.clearSessionAudio()
        soundManager.pauseBackgroundWarming()
        soundManager.prefetchGameEvents()

        _uiState.update {
            it.copy(
                userProfile = it.userProfile.copy(
                    balanceEtb = newBalance,
                    gamesPlayed = it.userProfile.gamesPlayed + 1
                ),
                selectedTab = NavTab.PLAY,
                selectedRoom = room,
                joinedRoomIds = it.joinedRoomIds - room.id,
                joinedCardsByRoom = it.joinedCardsByRoom - room.id,
                currentCard = card,
                playView = PlayView.GAMEPLAY,
                calledNumbers = emptyList(),
                latestCalledBall = null,
                latestAmharicAnnouncement = com.example.sound.CallerEvent.GAME_START.amharicPhrase,
                nextBallCountdownSeconds = 6,
                isCalling = true,
                winCelebration = null
            )
        }

        soundManager.playRoundStart()

        val repo = repository
        if (user != null && repo != null) {
            viewModelScope.launch {
                try {
                    if (deductStake) {
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
                    }
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
        val lastFirestoreBallAtMs = java.util.concurrent.atomic.AtomicLong(android.os.SystemClock.elapsedRealtime())
        val actingAsHost = java.util.concurrent.atomic.AtomicBoolean(false)

        // 1. Reset stale room state before observing Firestore (prevents replaying old rounds or stale FINISHED state)
        if (repo != null && currentFirebaseUser() != null) {
            roomSyncJob = viewModelScope.launch {
                val players = _uiState.value.activeRoomPlayers
                val isHostInitially = players.isEmpty() || players.minByOrNull { it.userId }?.userId == currentUid
                if (isHostInitially) {
                    actingAsHost.set(true)
                    try {
                        repo.resetRoomForNewRound(roomId)
                    } catch (_: Exception) {}
                }

                repo.observeRoom(roomId)
                    .catch { error ->
                        if (error is CancellationException) throw error
                        Log.w("BingoViewModel", "observeRoom error: ${error.message}")
                    }
                    .collect { liveRoom ->
                        if (liveRoom == null) return@collect

                        // Ignore stale FINISHED snapshots from a previous round before any ball has been drawn in this session
                        if (liveRoom.status == "FINISHED" && _uiState.value.calledNumbers.isEmpty()) {
                            actingAsHost.set(true)
                            try {
                                repo.resetRoomForNewRound(roomId)
                            } catch (_: Exception) {}
                            return@collect
                        }

                        // Check if another player in this room won BINGO during this live round
                        if (liveRoom.status == "FINISHED" && liveRoom.winnerId.isNotBlank()) {
                            if (liveRoom.winnerId != currentUid && _uiState.value.calledNumbers.isNotEmpty()) {
                                gameJob?.cancel()
                                soundManager.playWin()
                                _uiState.update {
                                    it.copy(
                                        isCalling = false,
                                        latestAmharicAnnouncement = com.example.sound.CallerEvent.BINGO_WIN.amharicPhrase,
                                        notificationMessage = "🎉 BINGO! ${liveRoom.winnerName} won ${liveRoom.prizeEtb} ETB!"
                                    )
                                }
                                return@collect
                            }
                        }

                        // Sync called balls across devices
                        val firestoreNumbers = liveRoom.calledNumbers.map { it.toInt() }
                        val currentCalled = _uiState.value.calledNumbers

                        if (firestoreNumbers.size > currentCalled.size) {
                            lastFirestoreBallAtMs.set(android.os.SystemClock.elapsedRealtime())
                            val newNumbers = firestoreNumbers.drop(currentCalled.size)
                            if (newNumbers.size == 1) {
                                handleNextBallCalled(newNumbers.first())
                            } else {
                                // Apply catch-up batch cleanly and only announce the newest ball aloud
                                newNumbers.dropLast(1).forEach { catchUpNum ->
                                    handleNextBallCalled(catchUpNum, playAudio = false)
                                }
                                handleNextBallCalled(newNumbers.last(), playAudio = true)
                            }
                        }
                    }
            }
        }

        // 2. Room Caller loop with strict 6,000 ms (6-second) interval and 2-ball lookahead pre-fetching:
        gameJob = viewModelScope.launch {
            val poolNumbers = (1..75).toMutableList()
            poolNumbers.shuffle()

            val calledSoFar = mutableListOf<Long>()

            while (isActive && poolNumbers.isNotEmpty()) {
                // Remove any numbers already called via Firestore sync
                val alreadyCalledSet = _uiState.value.calledNumbers.toSet()
                poolNumbers.removeAll { alreadyCalledSet.contains(it) }
                if (poolNumbers.isEmpty()) break

                val upcomingBall = poolNumbers.firstOrNull()
                val secondUpcomingBall = poolNumbers.getOrNull(1)
                val upcomingIndex = _uiState.value.calledNumbers.size + 1

                // Strict 6,000 ms drift-compensated countdown per ball
                val cycleStartMs = android.os.SystemClock.elapsedRealtime()
                for (secRemaining in 6 downTo 1) {
                    _uiState.update { it.copy(nextBallCountdownSeconds = secRemaining) }
                    if (secRemaining == 5 && upcomingBall != null) {
                        // Queue the immediate next ball cleanly after GAME_START / previous call
                        soundManager.prefetchBallCall(upcomingBall, upcomingIndex)
                    }
                    if (secRemaining == 3) {
                        if (upcomingBall != null) {
                            soundManager.prefetchBallCall(upcomingBall, upcomingIndex)
                        }
                        if (secondUpcomingBall != null) {
                            soundManager.prefetchBallCall(secondUpcomingBall, upcomingIndex + 1)
                        }
                    }
                    val targetElapsedMs = (7 - secRemaining) * 1_000L
                    val remainingWait = (cycleStartMs + targetElapsedMs) - android.os.SystemClock.elapsedRealtime()
                    if (remainingWait > 0L) {
                        delay(remainingWait)
                    }
                    if (!isActive || _uiState.value.winCelebration != null) break
                }

                if (!isActive || _uiState.value.winCelebration != null) {
                    break
                }

                // Re-sync pool against any balls received during the 6s wait
                val currentSet = _uiState.value.calledNumbers.toSet()
                poolNumbers.removeAll { currentSet.contains(it) }
                if (poolNumbers.isEmpty()) break

                if (repo != null && currentFirebaseUser() != null) {
                    val players = _uiState.value.activeRoomPlayers
                    val isHost = actingAsHost.get() ||
                        players.isEmpty() ||
                        players.minByOrNull { it.userId }?.userId == currentUid
                    val hostStalled = (android.os.SystemClock.elapsedRealtime() - lastFirestoreBallAtMs.get()) > 7_000L

                    if ((isHost || hostStalled) && _uiState.value.winCelebration == null) {
                        actingAsHost.set(true)
                        val next = poolNumbers.removeAt(0)
                        if (calledSoFar.isEmpty() && _uiState.value.calledNumbers.isNotEmpty()) {
                            calledSoFar.addAll(_uiState.value.calledNumbers.map { it.toLong() })
                        }
                        calledSoFar.add(next.toLong())

                        // Immediately pre-fetch next 2 balls ahead for zero-latency calls
                        poolNumbers.firstOrNull()?.let { b1 ->
                            soundManager.prefetchBallCall(b1, calledSoFar.size + 1)
                        }
                        poolNumbers.getOrNull(1)?.let { b2 ->
                            soundManager.prefetchBallCall(b2, calledSoFar.size + 2)
                        }
                        if (calledSoFar.size == 3) {
                            soundManager.prefetchWinAnnouncement()
                        }

                        var syncedToFirestore = false
                        try {
                            repo.updateRoomState(
                                roomId = roomId,
                                countdownSeconds = 0L,
                                status = "IN_PROGRESS",
                                calledNumbers = calledSoFar.toList(),
                                latestBall = next.toLong()
                            )
                            syncedToFirestore = true
                        } catch (_: Exception) {}

                        // If Firestore update failed or offline, call locally so zero balls are ever skipped
                        if (!syncedToFirestore && !_uiState.value.calledNumbers.contains(next)) {
                            handleNextBallCalled(next)
                        }
                    }
                } else if (_uiState.value.winCelebration == null) {
                    val next = poolNumbers.removeAt(0)
                    calledSoFar.add(next.toLong())

                    // Immediately pre-fetch next 2 balls ahead for zero-latency calls
                    poolNumbers.firstOrNull()?.let { b1 ->
                        soundManager.prefetchBallCall(b1, calledSoFar.size + 1)
                    }
                    poolNumbers.getOrNull(1)?.let { b2 ->
                        soundManager.prefetchBallCall(b2, calledSoFar.size + 2)
                    }
                    if (calledSoFar.size == 3) {
                        soundManager.prefetchWinAnnouncement()
                    }

                    handleNextBallCalled(next)
                }

                if (_uiState.value.winCelebration != null) {
                    break
                }
            }
        }
    }

    private fun handleNextBallCalled(ball: Int, playAudio: Boolean = true) {
        val callIndex = _uiState.value.calledNumbers.size + 1
        val callout = com.example.sound.AmharicCaller.getCallout(ball)
        if (playAudio) {
            soundManager.playBallCall(ball, callIndex)
        }

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
                if (daubed && playAudio) {
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
                    latestAmharicAnnouncement = com.example.sound.CallerEvent.BINGO_WIN.amharicPhrase,
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
            val roomId = it.selectedRoom?.id
            it.copy(
                winCelebration = null,
                playView = PlayView.LOBBY,
                joinedRoomIds = if (roomId != null) it.joinedRoomIds - roomId else it.joinedRoomIds,
                joinedCardsByRoom = if (roomId != null) it.joinedCardsByRoom - roomId else it.joinedCardsByRoom
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
                joinedRoomIds = emptySet(),
                joinedCardsByRoom = emptyMap(),
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

    fun setCallerVoiceProfile(profile: String) {
        soundManager.callerVoiceProfile = profile
        _uiState.update {
            it.copy(
                callerVoiceProfile = profile,
                callerStatusText = "Amharic Caller Man ($profile)"
            )
        }
        soundManager.previewAmharicVoice()
    }

    fun previewAmharicVoice() {
        soundManager.previewAmharicVoice()
    }

    fun toggleCallerMute() {
        val current = _uiState.value
        val nextMuted = !current.isCallerMuted
        val restoredVolume = if (!nextMuted && current.callerVolume <= 0.01f) 0.85f else current.callerVolume
        soundManager.callerVolume = restoredVolume
        soundManager.isCallerMuted = nextMuted
        _uiState.update {
            it.copy(
                isCallerMuted = nextMuted,
                callerVolume = restoredVolume
            )
        }
    }

    fun setCallerVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        val shouldMute = clamped <= 0.01f
        soundManager.callerVolume = clamped
        soundManager.isCallerMuted = shouldMute
        _uiState.update {
            it.copy(
                callerVolume = clamped,
                isCallerMuted = shouldMute
            )
        }
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
