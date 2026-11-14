package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.FirestoreRepository
import com.example.data.FirestoreTransaction
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.robolectric.annotation.Config

@Config(sdk = [36])
class FirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createUserProfile_validPayload_createsDocumentAndReadsBack() = runBlocking {
        val email = "alice_create_${System.currentTimeMillis()}@test.com"
        val uid = signInTestUser(email)
        val repository = FirestoreRepository(firestore, deviceId = "dev_alice_create")

        val (profile, _) = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createInitialUserProfile(
                userId = uid,
                name = "Alice Test",
                phone = "+251911223344",
                city = "Addis Ababa"
            )
        }
        assertNotNull(profile)
        assertEquals(uid, profile.userId)
        assertEquals("Alice Test", profile.name)
    }

    @Test
    fun recordTransaction_validPayload_storesTransaction() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreRepository(firestore, deviceId = "dev_alice_tx")

        val tx = FirestoreTransaction(
            id = "tx_${System.currentTimeMillis()}",
            userId = uid,
            type = "DEPOSIT",
            amountEtb = 500L,
            status = "COMPLETED",
            paymentMethod = "TELEBIRR",
            reference = "REF998877"
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordTransaction(uid, tx)
        }

        val transactions = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTransactions(uid).first { list -> list.any { it.id == tx.id } }
        }
        assertTrue(transactions.any { it.id == tx.id && it.amountEtb == 500L })
    }

    @Test
    fun joinRoom_twoPlayersDifferentAccounts_countsTwoPlayersAndUpdatesPool() = runBlocking {
        val roomId = "room_multi_users_${System.currentTimeMillis()}"

        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreRepository(firestore, deviceId = "phone_alice")
        val countAfterAlice = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.joinRoom(
                roomId = roomId,
                userId = aliceUid,
                name = "Alice Addis",
                ticketNumber = 71,
                stakeEtb = 10
            )
        }
        assertEquals(1, countAfterAlice)

        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepo = FirestoreRepository(firestore, deviceId = "phone_bob")
        val countAfterBob = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.joinRoom(
                roomId = roomId,
                userId = bobUid,
                name = "Bob Hawassa",
                ticketNumber = 77,
                stakeEtb = 10
            )
        }
        assertEquals(2, countAfterBob)

        val livePlayers = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeRoomPlayers(roomId).first { it.size == 2 }
        }
        assertEquals(2, livePlayers.size)

        val liveRoom = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeRoom(roomId).first { it != null && it.currentPlayers == 2L }
        }
        assertNotNull(liveRoom)
        assertEquals(2L, liveRoom!!.currentPlayers)
        assertEquals(20L, liveRoom.totalPoolEtb)
        assertEquals(16L, liveRoom.prizeEtb)
        assertEquals(4L, liveRoom.houseCommissionEtb)

        // Bob leaves -> drops cleanly back to 1
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.leaveRoom(roomId, bobUid, 10)
        }
        val roomAfterLeave = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeRoom(roomId).first { it != null && it.currentPlayers == 1L }
        }
        assertEquals(1L, roomAfterLeave!!.currentPlayers)
        assertEquals(10L, roomAfterLeave.totalPoolEtb)
    }

    @Test
    fun joinRoom_twoDevicesSameAccount_countsTwoPlayersNotOne() = runBlocking {
        val roomId = "room_multi_dev_${System.currentTimeMillis()}"
        val uid = signInTestUser(ALICE_EMAIL)

        val phoneRepo = FirestoreRepository(firestore, deviceId = "phone1_id")
        val tabletRepo = FirestoreRepository(firestore, deviceId = "tablet2_id")

        val count1 = withTimeout(DEFAULT_TIMEOUT_MS) {
            phoneRepo.joinRoom(roomId, uid, "Player Phone", 70, 10)
        }
        assertEquals(1, count1)

        val count2 = withTimeout(DEFAULT_TIMEOUT_MS) {
            tabletRepo.joinRoom(roomId, uid, "Player Tablet", 75, 10)
        }
        assertEquals(2, count2)

        val players = withTimeout(FLOW_TIMEOUT_MS) {
            tabletRepo.observeRoomPlayers(roomId).first { it.size == 2 }
        }
        assertEquals(2, players.size)
    }

    @Test
    fun getOtherUserProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreRepository(firestore, deviceId = "dev_alice_sec")
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createInitialUserProfile(
                userId = aliceUid,
                name = "Alice Secret",
                phone = "+251911000000",
                city = "Addis Ababa"
            )
        }

        // Sign in as Bob
        signInTestUser(BOB_EMAIL)
        val bobRepo = FirestoreRepository(firestore, deviceId = "dev_bob_sec")

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.observeUserProfile(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED exception when accessing other user's profile")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }
    }

    @Test
    fun observeUserProfile_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = FirestoreRepository(firestore, deviceId = "dev_unauth")

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeUserProfile("unauthenticated_uid").first()
            }
            fail("Expected PERMISSION_DENIED exception for unauthenticated user")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }
    }

    @Test
    fun createAccountWithPinHashPromoWelcomeBonusAndPinReset_passesRules() = runBlocking {
        val email = "reg_user_${System.currentTimeMillis()}@test.com"
        val uid = signInTestUser(email)
        val repository = FirestoreRepository(firestore, deviceId = "dev_reg_user")

        val initialHash = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f61234"
        val (created, _) = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createInitialUserProfile(
                userId = uid,
                name = "Dawit Bekele",
                phone = "+251911223344",
                city = "Addis Ababa",
                initialBalanceEtb = 150L,
                pinHash = initialHash,
                promoCode = "YEM100",
                welcomeBonusClaimed = true
            )
        }
        assertEquals(150L, created.balanceEtb)
        assertEquals(initialHash, created.pinHash)
        assertEquals("YEM100", created.promoCode)
        assertTrue(created.welcomeBonusClaimed)

        val updatedHash = "998877665544332211009988776655443322110099887766554433221100ffff"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateUserPinHash(uid, updatedHash, "+251911556677")
        }

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProfile(uid).first { it?.pinHash == updatedHash }
        }
        assertEquals("+251911556677", observed?.phone)
        assertEquals(true, observed?.welcomeBonusClaimed)

        val welcomeTxId = "tx_welcome_${System.currentTimeMillis()}"
        val dailyTxId = "tx_daily_${System.currentTimeMillis()}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordTransaction(
                uid,
                FirestoreTransaction(
                    id = welcomeTxId,
                    userId = uid,
                    type = "DEPOSIT",
                    amountEtb = 150L,
                    status = "COMPLETED",
                    paymentMethod = "WELCOME_BONUS",
                    reference = "REG_BONUS_YEM100"
                )
            )
            repository.recordTransaction(
                uid,
                FirestoreTransaction(
                    id = dailyTxId,
                    userId = uid,
                    type = "DEPOSIT",
                    amountEtb = 50L,
                    status = "COMPLETED",
                    paymentMethod = "DAILY_REWARD",
                    reference = "DAY_1"
                )
            )
        }

        val txs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTransactions(uid).first { list ->
                list.any { it.id == welcomeTxId } && list.any { it.id == dailyTxId }
            }
        }
        assertTrue(txs.any { it.id == welcomeTxId && it.paymentMethod == "WELCOME_BONUS" })
        assertTrue(txs.any { it.id == dailyTxId && it.paymentMethod == "DAILY_REWARD" })
    }

    private companion object {
        const val ALICE_EMAIL = "alice_rule_test@test.com"
        const val BOB_EMAIL = "bob_rule_test@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
