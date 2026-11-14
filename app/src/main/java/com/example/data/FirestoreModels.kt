package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreUserProfile(
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val avatarUrl: String? = null,
    val city: String = "Addis Ababa",
    val balanceEtb: Long = 250L,
    val gamesPlayed: Long = 0L,
    val gamesWon: Long = 0L,
    val totalEarningsEtb: Long = 0L,
    val pinHash: String? = null,
    val promoCode: String? = null,
    val welcomeBonusClaimed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreTransaction(
    val id: String = "",
    val userId: String = "",
    val type: String = "DEPOSIT",
    val amountEtb: Long = 0L,
    val status: String = "COMPLETED",
    val paymentMethod: String = "TELEBIRR",
    val reference: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreRoomPlayer(
    val userId: String = "",
    val deviceId: String? = null,
    val name: String = "",
    val ticketNumber: Long = 77L,
    @ServerTimestamp val joinedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreGameRoom(
    val id: String = "",
    val name: String = "",
    val stakeEtb: Long = 10L,
    val prizeEtb: Long = 480L,
    val houseCommissionEtb: Long = 120L,
    val totalPoolEtb: Long = 600L,
    val currentPlayers: Long = 0L,
    val countdownSeconds: Long = 180L,
    @ServerTimestamp val countdownStartedAt: Timestamp? = null,
    val status: String = "COUNTDOWN",
    val calledNumbers: List<Long> = emptyList(),
    val latestBall: Long = 0L,
    val winnerId: String = "",
    val winnerName: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreLeaderboard(
    val userId: String = "",
    val name: String = "",
    val wonEtb: Long = 0L,
    val gamesWon: Long = 0L,
    val city: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)
