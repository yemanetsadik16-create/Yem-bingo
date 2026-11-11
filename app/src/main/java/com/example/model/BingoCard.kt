package com.example.model

data class BingoCell(
    val number: Int,
    val isFree: Boolean = false,
    val isMarked: Boolean = isFree,
    val isWinning: Boolean = false
)

data class BingoCard(
    val ticketNumber: Int,
    val grid: List<List<BingoCell>>
)
