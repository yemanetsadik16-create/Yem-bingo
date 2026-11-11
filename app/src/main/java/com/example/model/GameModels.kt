package com.example.model

data class Room(
    val id: String,
    val name: String,
    val stakeEtb: Int,
    val totalPoolEtb: Int,
    val prizeEtb: Int = (totalPoolEtb * 80) / 100,
    val houseCommissionEtb: Int = (totalPoolEtb * 20) / 100,
    val currentPlayers: Int,
    val maxPlayers: Int,
    val countdownSeconds: Int = 180,
    val statusText: String = "03:00 left",
    val badgeColorHex: Long
)

data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val phoneMasked: String,
    val wins: Int,
    val prizeEtb: Int,
    val isCurrentUser: Boolean = false
)

enum class PaymentMethod(val displayName: String) {
    TELEBIRR("Telebirr"),
    CBE_BIRR("CBE Birr")
}

data class TransactionItem(
    val id: String,
    val title: String,
    val amountEtb: Int,
    val isCredit: Boolean,
    val timestampFormatted: String,
    val method: String
)

data class UserProfile(
    val name: String = "Player",
    val phoneMasked: String = "",
    val location: String = "Addis Ababa",
    val vipLevel: Int = 1,
    val balanceEtb: Int = 250,
    val gamesPlayed: Int = 0,
    val totalWinningsEtb: Int = 0,
    val winRatePercent: Float = 0.0f,
    val dailyStreakDays: Int = 1,
    val promoCode: String = "YEM-PLAYER-77",
    val isDailyClaimed: Boolean = false
)
