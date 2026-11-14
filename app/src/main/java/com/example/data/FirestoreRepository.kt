package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreRepository(
    val db: FirebaseFirestore,
    val deviceId: String = DEFAULT_TEST_DEVICE_ID
) {

    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        deviceId = getOrCreateDeviceId(context.applicationContext)
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun playerEntryId(userId: String): String = "${userId}_${deviceId}"

    // --- USER PROFILE & WALLET ---

    fun observeUserProfile(userId: String): Flow<FirestoreUserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(FirestoreUserProfile::class.java)
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createInitialUserProfile(
        userId: String,
        name: String,
        phone: String,
        city: String,
        initialBalanceEtb: Long = 250L,
        pinHash: String? = null,
        promoCode: String? = null,
        welcomeBonusClaimed: Boolean = false
    ): Pair<FirestoreUserProfile, Long> {
        val uid = requireUserId()
        require(uid == userId) { "Unauthorized profile creation" }
        val docRef = db.collection("users").document(uid)

        val existing = docRef.get().await()
        if (existing.exists()) {
            val profile = existing.toObject(FirestoreUserProfile::class.java)
                ?: FirestoreUserProfile(userId = uid, name = name, phone = phone, city = city)
            // Touch updatedAt to measure live server clock offset for synchronized countdowns
            try {
                docRef.update("updatedAt", FieldValue.serverTimestamp()).await()
                val refreshed = docRef.get().await()
                val serverMs = refreshed.getTimestamp("updatedAt")?.toDate()?.time
                val offsetMs = if (serverMs != null) serverMs - System.currentTimeMillis() else 0L
                return profile to offsetMs
            } catch (_: Exception) {
                return profile to 0L
            }
        }

        val cleanName = name.trim().ifBlank { "Habesha Player" }.take(60).let {
            if (it.length < 2) "Habesha Player" else it
        }
        val cleanPhone = phone.trim().let { if (it.length in 9..20) it else "Google Verified" }
        val cleanCity = city.trim().ifBlank { "Addis Ababa" }.take(50)
        val cleanBalance = initialBalanceEtb.coerceAtLeast(0L)
        val cleanPinHash = pinHash?.trim()?.take(128)?.ifBlank { null }
        val cleanPromoCode = promoCode?.trim()?.take(40)?.ifBlank { null }

        val profilePayload = mutableMapOf<String, Any>(
            "userId" to uid,
            "name" to cleanName,
            "phone" to cleanPhone,
            "city" to cleanCity,
            "balanceEtb" to cleanBalance,
            "gamesPlayed" to 0L,
            "gamesWon" to 0L,
            "totalEarningsEtb" to 0L,
            "welcomeBonusClaimed" to welcomeBonusClaimed,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (cleanPinHash != null) {
            profilePayload["pinHash"] = cleanPinHash
        }
        if (cleanPromoCode != null) {
            profilePayload["promoCode"] = cleanPromoCode
        }

        try {
            docRef.set(profilePayload).await()
            val savedSnap = docRef.get().await()
            val serverMs = savedSnap.getTimestamp("updatedAt")?.toDate()?.time
            val offsetMs = if (serverMs != null) serverMs - System.currentTimeMillis() else 0L
            return FirestoreUserProfile(
                userId = uid,
                name = cleanName,
                phone = cleanPhone,
                city = cleanCity,
                balanceEtb = cleanBalance,
                pinHash = cleanPinHash,
                promoCode = cleanPromoCode,
                welcomeBonusClaimed = welcomeBonusClaimed
            ) to offsetMs
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateUserProfile(profile: FirestoreUserProfile) {
        val uid = requireUserId()
        require(uid == profile.userId) { "Unauthorized profile update" }
        val docRef = db.collection("users").document(uid)

        val cleanName = profile.name.trim().ifBlank { "Habesha Player" }.take(60).let {
            if (it.length < 2) "Habesha Player" else it
        }
        val updatePayload = mutableMapOf<String, Any>(
            "name" to cleanName,
            "phone" to profile.phone.trim().let { if (it.length in 9..20) it else "Google Verified" },
            "city" to profile.city.trim().ifBlank { "Addis Ababa" }.take(50),
            "balanceEtb" to profile.balanceEtb.coerceAtLeast(0L),
            "gamesPlayed" to profile.gamesPlayed.coerceAtLeast(0L),
            "gamesWon" to profile.gamesWon.coerceAtLeast(0L),
            "totalEarningsEtb" to profile.totalEarningsEtb.coerceAtLeast(0L),
            "welcomeBonusClaimed" to profile.welcomeBonusClaimed,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        profile.pinHash?.trim()?.take(128)?.ifBlank { null }?.let {
            updatePayload["pinHash"] = it
        }
        profile.promoCode?.trim()?.take(40)?.ifBlank { null }?.let {
            updatePayload["promoCode"] = it
        }

        try {
            docRef.update(updatePayload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateUserPinHash(userId: String, pinHash: String, phone: String? = null) {
        val uid = requireUserId()
        require(uid == userId) { "Unauthorized PIN update" }
        val cleanHash = pinHash.trim().take(128)
        require(cleanHash.isNotEmpty()) { "PIN hash cannot be empty" }
        val docRef = db.collection("users").document(uid)

        val payload = mutableMapOf<String, Any>(
            "pinHash" to cleanHash,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val cleanPhone = phone?.trim()
        if (!cleanPhone.isNullOrEmpty() && cleanPhone.length in 9..20) {
            payload["phone"] = cleanPhone
        }

        try {
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun recordTransaction(userId: String, transaction: FirestoreTransaction) {
        val uid = requireUserId()
        require(uid == userId) { "Unauthorized transaction record" }
        val txId = transaction.id.ifBlank { "tx_${System.currentTimeMillis()}" }
        val docRef = db.collection("users").document(uid).collection("transactions").document(txId)

        val payload = mapOf(
            "id" to txId,
            "userId" to uid,
            "type" to transaction.type,
            "amountEtb" to transaction.amountEtb,
            "status" to transaction.status,
            "paymentMethod" to transaction.paymentMethod,
            "reference" to transaction.reference,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeTransactions(userId: String): Flow<List<FirestoreTransaction>> = flow {
        val uid = requireUserId()
        require(uid == userId) { "Unauthorized transactions access" }
        val path = "users/$uid/transactions"
        emitAll(
            db.collection("users").document(uid).collection("transactions")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(FirestoreTransaction::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    // --- MULTIPLAYER ROOMS & LIVE SYNCHRONIZATION ---

    fun observeRooms(): Flow<List<FirestoreGameRoom>> = flow {
        val path = "rooms"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(FirestoreGameRoom::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeRoom(roomId: String): Flow<FirestoreGameRoom?> = flow {
        val path = "rooms/$roomId"
        emitAll(
            db.collection("rooms").document(roomId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) snapshot.toObject(FirestoreGameRoom::class.java) else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeRoomPlayers(roomId: String): Flow<List<FirestoreRoomPlayer>> = flow {
        val path = "rooms/$roomId/players"
        emitAll(
            db.collection("rooms").document(roomId).collection("players")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(FirestoreRoomPlayer::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeAllRoomPlayers(
        roomIds: List<String> = DEFAULT_ROOM_IDS
    ): Flow<Map<String, List<FirestoreRoomPlayer>>> {
        val perRoomFlows = roomIds.map { roomId ->
            observeRoomPlayers(roomId).map { players -> roomId to players }
        }
        return combine(perRoomFlows) { pairs -> pairs.toMap() }
    }

    suspend fun joinRoom(
        roomId: String,
        userId: String,
        name: String,
        ticketNumber: Int,
        stakeEtb: Int
    ): Int {
        val uid = requireUserId()
        require(uid == userId) { "Unauthorized room join" }
        val entryId = playerEntryId(uid)
        val playersCollection = db.collection("rooms").document(roomId).collection("players")
        val playerRef = playersCollection.document(entryId)
        val roomRef = db.collection("rooms").document(roomId)

        val cleanName = name.trim().ifBlank { "Habesha Player" }.take(60)
        val roomMeta = DEFAULT_ROOMS_META.find { it.id == roomId }
        val roomDisplayName = roomMeta?.name ?: "Live Room"
        val resolvedStake = roomMeta?.stakeEtb ?: stakeEtb.toLong()

        val playerPayload = mapOf(
            "userId" to uid,
            "deviceId" to deviceId,
            "name" to cleanName,
            "ticketNumber" to ticketNumber.toLong().coerceIn(1L, 999L),
            "joinedAt" to FieldValue.serverTimestamp()
        )

        try {
            db.runTransaction { tx ->
                val roomSnap = tx.get(roomRef)
                val playerSnap = tx.get(playerRef)

                tx.set(playerRef, playerPayload)

                if (!roomSnap.exists()) {
                    val pool = resolvedStake
                    tx.set(
                        roomRef,
                        mapOf(
                            "id" to roomId,
                            "name" to roomDisplayName,
                            "stakeEtb" to resolvedStake,
                            "prizeEtb" to (pool * 80L) / 100L,
                            "houseCommissionEtb" to (pool * 20L) / 100L,
                            "totalPoolEtb" to pool,
                            "currentPlayers" to 1L,
                            "countdownSeconds" to 180L,
                            "countdownStartedAt" to FieldValue.serverTimestamp(),
                            "status" to "COUNTDOWN",
                            "calledNumbers" to emptyList<Long>(),
                            "latestBall" to 0L,
                            "winnerId" to "",
                            "winnerName" to "",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    val roomStatus = roomSnap.getString("status") ?: "COUNTDOWN"
                    val wasFinished = roomStatus == "FINISHED"
                    val alreadyJoined = playerSnap.exists() && !wasFinished
                    val basePlayers = if (wasFinished) 0L else (roomSnap.getLong("currentPlayers") ?: 0L)
                    val basePool = if (wasFinished) 0L else (roomSnap.getLong("totalPoolEtb") ?: 0L)

                    if (!alreadyJoined || wasFinished) {
                        val currentP = basePlayers + 1L
                        val currentPool = basePool + resolvedStake
                        val updates = mutableMapOf<String, Any>(
                            "currentPlayers" to currentP,
                            "totalPoolEtb" to currentPool,
                            "prizeEtb" to (currentPool * 80L) / 100L,
                            "houseCommissionEtb" to (currentPool * 20L) / 100L,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        if (wasFinished || basePlayers == 0L || roomSnap.getTimestamp("countdownStartedAt") == null) {
                            updates["status"] = "COUNTDOWN"
                            updates["countdownSeconds"] = 180L
                            updates["countdownStartedAt"] = FieldValue.serverTimestamp()
                            updates["calledNumbers"] = emptyList<Long>()
                            updates["latestBall"] = 0L
                            updates["winnerId"] = ""
                            updates["winnerName"] = ""
                        }
                        tx.update(roomRef, updates)
                    }
                }
                null
            }.await()

            // Reconcile room counter & pool strictly against live players subcollection
            val livePlayersSnap = playersCollection.get().await()
            val actualCount = livePlayersSnap.size().coerceAtLeast(1)
            val actualPool = actualCount.toLong() * resolvedStake
            roomRef.update(
                mapOf(
                    "currentPlayers" to actualCount.toLong(),
                    "totalPoolEtb" to actualPool,
                    "prizeEtb" to (actualPool * 80L) / 100L,
                    "houseCommissionEtb" to (actualPool * 20L) / 100L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            return actualCount
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, playerRef.path)
            throw e
        }
    }

    suspend fun leaveRoom(roomId: String, userId: String, stakeEtb: Int = 0) {
        val uid = requireUserId()
        require(uid == userId) { "Unauthorized room leave" }
        val entryId = playerEntryId(uid)
        val playersCollection = db.collection("rooms").document(roomId).collection("players")
        val playerRef = playersCollection.document(entryId)
        val legacyPlayerRef = playersCollection.document(uid)
        val roomRef = db.collection("rooms").document(roomId)

        try {
            val deletedDeviceEntry = try {
                if (playerRef.get().await().exists()) {
                    playerRef.delete().await()
                    true
                } else false
            } catch (_: Exception) {
                false
            }
            val deletedLegacyEntry = try {
                if (legacyPlayerRef.get().await().exists()) {
                    legacyPlayerRef.delete().await()
                    true
                } else false
            } catch (_: Exception) {
                false
            }
            if (!deletedDeviceEntry && !deletedLegacyEntry) return

            val roomDoc = roomRef.get().await()
            if (roomDoc.exists()) {
                val remainingSnap = playersCollection.get().await()
                val remainingCount = remainingSnap.size().toLong()
                val resolvedStake = if (stakeEtb > 0) stakeEtb.toLong() else (roomDoc.getLong("stakeEtb") ?: 10L)
                val currentPool = remainingCount * resolvedStake
                roomRef.update(
                    mapOf(
                        "currentPlayers" to remainingCount,
                        "totalPoolEtb" to currentPool,
                        "prizeEtb" to (currentPool * 80L) / 100L,
                        "houseCommissionEtb" to (currentPool * 20L) / 100L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, playerRef.path)
            throw e
        }
    }

    suspend fun restartRoomCountdown(roomId: String) {
        val roomRef = db.collection("rooms").document(roomId)
        try {
            roomRef.update(
                mapOf(
                    "countdownSeconds" to 180L,
                    "countdownStartedAt" to FieldValue.serverTimestamp(),
                    "status" to "COUNTDOWN",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
        }
    }

    suspend fun updateRoomState(
        roomId: String,
        countdownSeconds: Long,
        status: String,
        calledNumbers: List<Long>,
        latestBall: Long
    ) {
        val roomRef = db.collection("rooms").document(roomId)
        try {
            roomRef.update(
                mapOf(
                    "countdownSeconds" to countdownSeconds,
                    "status" to status,
                    "calledNumbers" to calledNumbers,
                    "latestBall" to latestBall,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
        }
    }

    suspend fun resetRoomForNewRound(roomId: String) {
        val roomRef = db.collection("rooms").document(roomId)
        try {
            roomRef.update(
                mapOf(
                    "countdownSeconds" to 0L,
                    "status" to "IN_PROGRESS",
                    "calledNumbers" to emptyList<Long>(),
                    "latestBall" to 0L,
                    "winnerId" to "",
                    "winnerName" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
        }
    }

    suspend fun claimBingoWin(
        roomId: String,
        userId: String,
        winnerName: String,
        prizeEtb: Long,
        city: String
    ) {
        val uid = requireUserId()
        require(uid == userId) { "Unauthorized win claim" }
        val roomRef = db.collection("rooms").document(roomId)
        val userRef = db.collection("users").document(uid)
        val leaderboardRef = db.collection("leaderboard").document(uid)

        try {
            roomRef.update(
                mapOf(
                    "status" to "FINISHED",
                    "winnerId" to uid,
                    "winnerName" to winnerName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val userDoc = userRef.get().await()
            val curBalance = userDoc.getLong("balanceEtb") ?: 0L
            val curWins = userDoc.getLong("gamesWon") ?: 0L
            val curEarnings = userDoc.getLong("totalEarningsEtb") ?: 0L
            userRef.update(
                mapOf(
                    "balanceEtb" to curBalance + prizeEtb,
                    "gamesWon" to curWins + 1L,
                    "totalEarningsEtb" to curEarnings + prizeEtb,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            recordTransaction(
                uid,
                FirestoreTransaction(
                    id = "tx_win_${System.currentTimeMillis()}",
                    userId = uid,
                    type = "WIN",
                    amountEtb = prizeEtb,
                    status = "COMPLETED",
                    paymentMethod = "GAME_WIN",
                    reference = "BINGO-$roomId"
                )
            )

            val lbDoc = leaderboardRef.get().await()
            val totalWon = (lbDoc.getLong("wonEtb") ?: 0L) + prizeEtb
            val totalWinsCount = (lbDoc.getLong("gamesWon") ?: 0L) + 1L
            leaderboardRef.set(
                mapOf(
                    "userId" to uid,
                    "name" to winnerName,
                    "wonEtb" to totalWon,
                    "gamesWon" to totalWinsCount,
                    "city" to city,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
            throw e
        }
    }

    fun observeLeaderboard(): Flow<List<FirestoreLeaderboard>> = flow {
        val path = "leaderboard"
        emitAll(
            db.collection(path)
                .orderBy("wonEtb", Query.Direction.DESCENDING)
                .limit(25)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(FirestoreLeaderboard::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun syncDefaultRooms() {
        val path = "rooms"
        for (room in DEFAULT_ROOMS_META) {
            val docRef = db.collection(path).document(room.id)
            val doc = docRef.get().await()
            if (!doc.exists()) {
                val payload = mapOf(
                    "id" to room.id,
                    "name" to room.name,
                    "stakeEtb" to room.stakeEtb,
                    "prizeEtb" to room.prizeEtb,
                    "houseCommissionEtb" to room.houseCommissionEtb,
                    "totalPoolEtb" to room.totalPoolEtb,
                    "currentPlayers" to room.currentPlayers,
                    "countdownSeconds" to room.countdownSeconds,
                    "countdownStartedAt" to FieldValue.serverTimestamp(),
                    "status" to room.status,
                    "calledNumbers" to emptyList<Long>(),
                    "latestBall" to 0L,
                    "winnerId" to "",
                    "winnerName" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                try {
                    docRef.set(payload).await()
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.WRITE, docRef.path)
                }
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "yem_bingo_device_prefs"
        private const val KEY_DEVICE_ID = "installation_device_id"
        private const val DEFAULT_TEST_DEVICE_ID = "testdev1"

        val DEFAULT_ROOM_IDS = listOf(
            "mini_starter",
            "standard",
            "high_roller",
            "mega_addis",
            "habesha_vip"
        )

        val DEFAULT_ROOMS_META = listOf(
            FirestoreGameRoom(id = "mini_starter", name = "Mini Starter", stakeEtb = 10L, prizeEtb = 0L, houseCommissionEtb = 0L, totalPoolEtb = 0L, currentPlayers = 0L, countdownSeconds = 180L, status = "COUNTDOWN"),
            FirestoreGameRoom(id = "standard", name = "Standard Room", stakeEtb = 20L, prizeEtb = 0L, houseCommissionEtb = 0L, totalPoolEtb = 0L, currentPlayers = 0L, countdownSeconds = 180L, status = "COUNTDOWN"),
            FirestoreGameRoom(id = "high_roller", name = "High Roller Suite", stakeEtb = 50L, prizeEtb = 0L, houseCommissionEtb = 0L, totalPoolEtb = 0L, currentPlayers = 0L, countdownSeconds = 180L, status = "COUNTDOWN"),
            FirestoreGameRoom(id = "mega_addis", name = "Mega Addis Jackpot", stakeEtb = 100L, prizeEtb = 0L, houseCommissionEtb = 0L, totalPoolEtb = 0L, currentPlayers = 0L, countdownSeconds = 180L, status = "COUNTDOWN"),
            FirestoreGameRoom(id = "habesha_vip", name = "Habesha VIP Club", stakeEtb = 200L, prizeEtb = 0L, houseCommissionEtb = 0L, totalPoolEtb = 0L, currentPlayers = 0L, countdownSeconds = 180L, status = "COUNTDOWN")
        )

        fun getOrCreateDeviceId(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val existing = prefs.getString(KEY_DEVICE_ID, null)
            if (!existing.isNullOrBlank()) return existing
            val generated = "dev" + UUID.randomUUID().toString().replace("-", "").take(10)
            prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
            return generated
        }
    }
}
