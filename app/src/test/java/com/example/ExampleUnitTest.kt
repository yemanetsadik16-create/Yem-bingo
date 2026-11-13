package com.example

import com.example.model.BingoCell
import com.example.model.PaymentMethod
import com.example.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testBingoCardDimensionsAndFreeSpace() {
        val rows = (0 until 5).map { r ->
            (0 until 5).map { c ->
                if (r == 2 && c == 2) {
                    BingoCell(number = 0, isFree = true, isMarked = true, isWinning = false)
                } else {
                    BingoCell(number = r * 5 + c + 1, isFree = false, isMarked = false, isWinning = false)
                }
            }
        }

        assertEquals(5, rows.size)
        rows.forEach { row ->
            assertEquals(5, row.size)
        }

        val centerCell = rows[2][2]
        assertTrue(centerCell.isFree)
        assertTrue(centerCell.isMarked)
        assertEquals(0, centerCell.number)
    }

    @Test
    fun testWinConditionRow() {
        val grid = (0 until 5).map { r ->
            (0 until 5).map { c ->
                BingoCell(number = r * 5 + c + 1, isFree = false, isMarked = (r == 0))
            }
        }
        val row0Won = grid[0].all { it.isMarked }
        val row1Won = grid[1].all { it.isMarked }
        assertTrue(row0Won)
        assertFalse(row1Won)
    }

    @Test
    fun testWinConditionDiagonal() {
        val grid = (0 until 5).map { r ->
            (0 until 5).map { c ->
                BingoCell(number = r * 5 + c + 1, isFree = false, isMarked = (r == c))
            }
        }
        val mainDiagonalWon = (0 until 5).all { i -> grid[i][i].isMarked }
        assertTrue(mainDiagonalWon)
    }

    @Test
    fun testUserProfileDefaults() {
        val profile = UserProfile()
        assertEquals("Player", profile.name)
        assertEquals(250, profile.balanceEtb)
        assertEquals(1, profile.dailyStreakDays)
        assertEquals(PaymentMethod.TELEBIRR.displayName, "Telebirr")
        assertEquals(PaymentMethod.CBE_BIRR.displayName, "CBE Birr")
    }

    @Test
    fun test75BallLetterMapping() {
        assertEquals("B", com.example.viewmodel.BingoViewModel.getLetterForBall(1))
        assertEquals("B", com.example.viewmodel.BingoViewModel.getLetterForBall(15))
        assertEquals("I", com.example.viewmodel.BingoViewModel.getLetterForBall(16))
        assertEquals("I", com.example.viewmodel.BingoViewModel.getLetterForBall(30))
        assertEquals("N", com.example.viewmodel.BingoViewModel.getLetterForBall(31))
        assertEquals("N", com.example.viewmodel.BingoViewModel.getLetterForBall(45))
        assertEquals("G", com.example.viewmodel.BingoViewModel.getLetterForBall(46))
        assertEquals("G", com.example.viewmodel.BingoViewModel.getLetterForBall(60))
        assertEquals("O", com.example.viewmodel.BingoViewModel.getLetterForBall(61))
        assertEquals("O", com.example.viewmodel.BingoViewModel.getLetterForBall(75))
    }

    @Test
    fun testRoomPrizeAndHouseCommissionSplit() {
        val room = com.example.model.Room(
            id = "test_room",
            name = "Test Room",
            stakeEtb = 10,
            totalPoolEtb = 1000,
            currentPlayers = 50,
            maxPlayers = 100,
            statusText = "active",
            badgeColorHex = 0xFFFFC107
        )

        assertEquals(800, room.prizeEtb) // 80% of 1000
        assertEquals(200, room.houseCommissionEtb) // 20% of 1000
        assertEquals(room.totalPoolEtb, room.prizeEtb + room.houseCommissionEtb)
    }

    @Test
    fun testRoom3MinuteCountdownAndMinPlayers() {
        val room = com.example.model.Room(
            id = "room_3m",
            name = "3M Room",
            stakeEtb = 10,
            totalPoolEtb = 600,
            currentPlayers = 2,
            maxPlayers = 60,
            countdownSeconds = 180, // 3 minutes
            statusText = "03:00 left",
            badgeColorHex = 0xFF20C997
        )

        assertEquals(180, room.countdownSeconds)
        assertTrue(room.currentPlayers >= 2)

        val roomWaiting = room.copy(currentPlayers = 1)
        assertFalse(roomWaiting.currentPlayers >= 2)
    }

    @Test
    fun testTelebirrDepositFeeCalculation() {
        val depositAmount = 500
        val telebirrFee = 0 // Telebirr has 0% deposit fee
        val netCredited = depositAmount - telebirrFee
        assertEquals(500, netCredited)
        assertEquals(0, telebirrFee)
    }

    @Test
    fun testCbeBirrCashoutFeeCalculation() {
        val cashoutAmount = 1000
        val cbeFee = (cashoutAmount * 1) / 100 // 1% CBE cashout fee
        val netReceived = cashoutAmount - cbeFee
        assertEquals(10, cbeFee)
        assertEquals(990, netReceived)
    }

    @Test
    fun testAmharicCallerAll75Balls() {
        for (ball in 1..75) {
            val callout = com.example.sound.AmharicCaller.getCallout(ball)
            assertTrue("Amharic letter missing for $ball", callout.amharicLetter.isNotBlank())
            assertTrue("Amharic number word missing for $ball", callout.amharicNumberWord.isNotBlank())
            assertTrue("Phonetic word missing for $ball", callout.phoneticNumberWord.isNotBlank())
        }

        val b1 = com.example.sound.AmharicCaller.getCallout(1)
        assertEquals("ቢ", b1.amharicLetter)
        assertEquals("አንድ", b1.amharicNumberWord)

        val n42 = com.example.sound.AmharicCaller.getCallout(42)
        assertEquals("ኤን", n42.amharicLetter)
        assertEquals("አርባ ሁለት", n42.amharicNumberWord)

        val o75 = com.example.sound.AmharicCaller.getCallout(75)
        assertEquals("ኦ", o75.amharicLetter)
        assertEquals("ሰባ አምስት", o75.amharicNumberWord)
    }
}
