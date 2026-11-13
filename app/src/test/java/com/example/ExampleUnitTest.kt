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

        val b4 = com.example.sound.AmharicCaller.getCallout(4)
        assertEquals("ቢ አራት! (B-4)", b4.amharicDisplayText)

        val i22 = com.example.sound.AmharicCaller.getCallout(22)
        assertEquals("አይ ሃያ ሁለት! (I-22)", i22.amharicDisplayText)

        val n35 = com.example.sound.AmharicCaller.getCallout(35)
        assertEquals("ኤን ሰላሳ አምስት! (N-35)", n35.amharicDisplayText)

        val g50 = com.example.sound.AmharicCaller.getCallout(50)
        assertEquals("ጂ ሃምሳ! (G-50)", g50.amharicDisplayText)

        val o75 = com.example.sound.AmharicCaller.getCallout(75)
        assertEquals("ኦ ሰባ አምስት! (O-75)", o75.amharicDisplayText)

        assertEquals(
            "ጨዋታው ተጀምሯል! መልካም ዕድል!",
            com.example.sound.CallerEvent.GAME_START.amharicPhrase
        )
        assertEquals(
            "ቢንጎ! እንኳን ደስ አለዎት፣ አሸናፊ አግኝተናል!",
            com.example.sound.CallerEvent.BINGO_WIN.amharicPhrase
        )
        assertTrue(
            com.example.sound.AmharicCaller.buildExpressiveSpokenPhrase(4, 2).contains("ቀጣዩ ቁጥር")
        )
        assertTrue(
            com.example.sound.AmharicCaller.buildExpressiveSpokenPhrase(35, 12).contains("ቢንጎ ተቃርቧል")
        )
    }

    @Test
    fun testAmharicAtomicVocabularyCoversAll75Balls() {
        val allVocab = com.example.sound.AmharicCaller.getAllAtomicVocabulary()
        assertEquals(22, allVocab.size)
        val validKeys = allVocab.map { it.first }.toSet()

        for (ball in 1..75) {
            val parts = com.example.sound.AmharicCaller.getAtomicComponentsForBall(ball)
            assertTrue("Missing atomic parts for $ball", parts.size in 2..3)
            assertTrue("First atom must be letter for $ball", parts.first().first.startsWith("atom_letter_"))
            for ((key, phrase) in parts) {
                assertTrue("Unknown atomic key $key for ball $ball", validKeys.contains(key))
                assertTrue("Blank Amharic phrase for $key", phrase.isNotBlank())
            }
        }
    }

    @Test
    fun testAmharicPcmTrimAndStitch() {
        // At 24kHz: 12,000 samples (500ms) silence + 4,000 samples (167ms) signal + 12,000 samples (500ms) silence
        val totalSamples = 28_000
        val clip1 = ByteArray(totalSamples * 2)
        for (i in 12_000 until 16_000) {
            clip1[i * 2] = 0x10
            clip1[i * 2 + 1] = 0x20
        }
        val stitched = com.example.sound.AmharicCaller.stitchPcmClips(listOf(clip1, clip1), sampleRate = 24_000, gapMs = 20)
        assertTrue("Stitched PCM should be non-empty and even-aligned", stitched.isNotEmpty() && stitched.size % 2 == 0)
        assertTrue("Trimmed stitched clip should be smaller than un-trimmed concatenation", stitched.size < clip1.size * 2)
    }

    @Test
    fun testAmharicFadeEnvelopeSmoothsBoundaryClicks() {
        // Constant loud amplitude signal that would otherwise click at start/end boundaries
        val sampleCount = 2400 // 100ms at 24kHz
        val raw = ByteArray(sampleCount * 2)
        for (i in 0 until sampleCount) {
            val sample = 12_000
            raw[i * 2] = (sample and 0xFF).toByte()
            raw[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
        }
        val faded = com.example.sound.AmharicCaller.applyFadeEnvelope16BitMono(raw, sampleRate = 24_000)
        assertEquals(raw.size, faded.size)

        val firstSample = ((faded[1].toInt() shl 8) or (faded[0].toInt() and 0xFF))
        val lastSample = ((faded[faded.size - 1].toInt() shl 8) or (faded[faded.size - 2].toInt() and 0xFF))
        val midIdx = (sampleCount / 2) * 2
        val midSample = ((faded[midIdx + 1].toInt() shl 8) or (faded[midIdx].toInt() and 0xFF))

        assertEquals("First sample should ramp smoothly from zero", 0, firstSample)
        assertEquals("Last sample should ramp smoothly to zero", 0, lastSample)
        assertEquals("Middle sample should retain full speech amplitude", 12_000, midSample)
    }
}
