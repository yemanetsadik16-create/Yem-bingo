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
        val repository = FirestoreRepository(firestore)

        val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
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
        val repository = FirestoreRepository(firestore)

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
    fun getOtherUserProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreRepository(firestore)
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
        val bobRepo = FirestoreRepository(firestore)

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                // Bob attempts to query Alice's profile doc
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
        val repository = FirestoreRepository(firestore)

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

    private companion object {
        const val ALICE_EMAIL = "alice_rule_test@test.com"
        const val BOB_EMAIL = "bob_rule_test@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
