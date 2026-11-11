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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreRepository(val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

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
        city: String
    ): FirestoreUserProfile {
        val uid = requireUserId()
        require(uid == userId) { "Unauthorized profile creation" }
        val docRef = db.collection("users").document(uid)

        val existing = docRef.get().await()
        if (existing.exists()) {
            return existing.toObject(FirestoreUserProfile::class.java) ?: FirestoreUserProfile(userId = uid, name = name, phone = phone, city = city)
        }

        val profilePayload = mapOf(
            "userId" to uid,
            "name" to name.ifBlank { "Habesha Player" },
            "phone" to phone.ifBlank { "+251911000000" },
            "city" to city.ifBlank { "Addis Ababa" },
            "balanceEtb" to 250L, // 250 ETB welcome gift
            "gamesPlayed" to 0L,
            "gamesWon" to 0L,
            "totalEarningsEtb" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(profilePayload).await()
            return FirestoreUserProfile(
                userId = uid,
                name = name.ifBlank { "Habesha Player" },
                phone = phone.ifBlank { "+251911000000" },
                city = city.ifBlank { "Addis Ababa" },
                balanceEtb = 250L
            )
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

        val updatePayload = mapOf(
            "name" to profile.name,
            "phone" to profile.phone,
            "city" to profile.city,
            "balanceEtb" to profile.balanceEtb,
            "gamesPlayed" to profile.gamesPlayed,
            "gamesWon" to profile.gamesWon,
            "totalEarningsEtb" to profile.totalEarningsEtb,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.update(updatePayload).await()
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

    suspend fun joinRoom(
        roomId: String,
        userId: String,
        name: String,
        ticketNumber: Int,
        stakeEtb: Int
    ) {
        val uid = requireUserId()
        require(uid == userId) { "Unauthorized room join" }
        val playerRef = db.collection("rooms").document(roomId).collection("players").document(uid)
        val roomRef = db.collection("rooms").document(roomId)

        val playerPayload = mapOf(
            "userId" to uid,
            "name" to name,
            "ticketNumber" to ticketNumber.toLong(),
            "joinedAt" to FieldValue.serverTimestamp()
        )

        try {
            playerRef.set(playerPayload).await()

            // Update room pool and player count
            val roomDoc = roomRef.get().await()
            if (roomDoc.exists()) {
                val currentP = (roomDoc.getLong("currentPlayers") ?: 0L) + 1L
                val currentPool = (roomDoc.getLong("totalPoolEtb") ?: 0L) + stakeEtb.toLong()
                val prize = (currentPool * 80L) / 100L
                val house = (currentPool * 20L) / 100L
                roomRef.update(
                    mapOf(
                        "currentPlayers" to currentP,
                        "totalPoolEtb" to currentPool,
                        "prizeEtb" to prize,
                        "houseCommissionEtb" to house,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, playerRef.path)
            throw e
        }
    }

    suspend fun leaveRoom(roomId: String, userId: String) {
        val uid = requireUserId()
        require(uid == userId) { "Unauthorized room leave" }
        val playerRef = db.collection("rooms").document(roomId).collection("players").document(uid)
        val roomRef = db.collection("rooms").document(roomId)

        try {
            playerRef.delete().await()
            val roomDoc = roomRef.get().await()
            if (roomDoc.exists()) {
                val currentP = (roomDoc.getLong("currentPlayers") ?: 1L) - 1L
                roomRef.update(
                    mapOf(
                        "currentPlayers" to if (currentP >= 0L) currentP else 0L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, playerRef.path)
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
            // Update room winner
            roomRef.update(
                mapOf(
                    "status" to "FINISHED",
                    "winnerId" to uid,
                    "winnerName" to winnerName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Credit user wallet
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

            // Record transaction
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

            // Update real leaderboard
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
        val defaultRooms = listOf(
            FirestoreGameRoom(id = "mini_starter", name = "Mini Starter", stakeEtb = 10L, prizeEtb = 0L, houseCommissionEtb = 0L, totalPoolEtb = 0L, currentPlayers = 0L, countdownSeconds = 180L, status = "COUNTDOWN"),
            FirestoreGameRoom(id = "standard", name = "Standard Room", stakeEtb = 20L, prizeEtb = 0L, houseCommissionEtb = 0L, totalPoolEtb = 0L, currentPlayers = 0L, countdownSeconds = 180L, status = "COUNTDOWN"),
            FirestoreGameRoom(id = "high_roller", name = "High Roller Suite", stakeEtb = 50L, prizeEtb = 0L, houseCommissionEtb = 0L, totalPoolEtb = 0L, currentPlayers = 0L, countdownSeconds = 180L, status = "COUNTDOWN"),
            FirestoreGameRoom(id = "mega_addis", name = "Mega Addis Jackpot", stakeEtb = 100L, prizeEtb = 0L, houseCommissionEtb = 0L, totalPoolEtb = 0L, currentPlayers = 0L, countdownSeconds = 180L, status = "COUNTDOWN"),
            FirestoreGameRoom(id = "habesha_vip", name = "Habesha VIP Club", stakeEtb = 200L, prizeEtb = 0L, houseCommissionEtb = 0L, totalPoolEtb = 0L, currentPlayers = 0L, countdownSeconds = 180L, status = "COUNTDOWN")
        )

        for (room in defaultRooms) {
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
}
