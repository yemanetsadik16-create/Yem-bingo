package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.viewmodel.BingoViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Yem Bingo", appName)
  }

  @Test
  fun `main activity launches without crashing when signed out or without firebase`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    assertNotNull(controller.get())
  }

  @Test
  fun `bingo view model initializes cleanly without crashing`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = BingoViewModel(app)
    assertFalse(vm.uiState.value.isLoggedIn)
    assertEquals(5, vm.uiState.value.rooms.size)
  }

  @Test
  fun `caller mute volume clamping and 6s countdown state behave correctly`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = BingoViewModel(app)

    assertEquals(6, vm.uiState.value.nextBallCountdownSeconds)
    assertFalse(vm.uiState.value.isCallerMuted)

    vm.setCallerVolume(1.5f)
    assertEquals(1.0f, vm.uiState.value.callerVolume, 0.001f)
    assertFalse(vm.uiState.value.isCallerMuted)

    vm.setCallerVolume(0f)
    assertEquals(0f, vm.uiState.value.callerVolume, 0.001f)
    assertTrue(vm.uiState.value.isCallerMuted)

    vm.toggleCallerMute()
    assertFalse(vm.uiState.value.isCallerMuted)
    assertEquals(0.85f, vm.uiState.value.callerVolume, 0.001f)
  }

  @Test
  fun `caller voice profile switching updates ui state and sound manager`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = BingoViewModel(app)

    assertEquals("Iapetus", vm.uiState.value.callerVoiceProfile)

    vm.setCallerVoiceProfile("Orus")
    assertEquals("Orus", vm.uiState.value.callerVoiceProfile)
    assertEquals("Orus", vm.soundManager.callerVoiceProfile)

    vm.setCallerVoiceProfile("Charon")
    assertEquals("Charon", vm.uiState.value.callerVoiceProfile)
    assertEquals("Charon", vm.soundManager.callerVoiceProfile)
  }

  @Test
  fun `joinRoomGame marks joined, increments player count, avoids double charge, and redirects to lobby`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = BingoViewModel(app)

    val room = vm.uiState.value.rooms.first { it.id == "mini_starter" }
    val initialBalance = vm.uiState.value.userProfile.balanceEtb

    // Previewing ticket does not join or charge stake
    vm.enterTicketSelection(room)
    assertEquals(com.example.viewmodel.PlayView.TICKET_SELECTION, vm.uiState.value.playView)
    assertFalse(vm.uiState.value.isSelectedRoomJoined)
    assertEquals(0, vm.uiState.value.rooms.first { it.id == "mini_starter" }.currentPlayers)
    assertEquals(initialBalance, vm.uiState.value.userProfile.balanceEtb)

    // Clicking Join Game joins, deducts stake once, increments count, and redirects to LOBBY
    vm.joinRoomGame()
    assertEquals(com.example.viewmodel.PlayView.LOBBY, vm.uiState.value.playView)
    assertTrue(vm.uiState.value.joinedRoomIds.contains("mini_starter"))
    assertEquals(1, vm.uiState.value.rooms.first { it.id == "mini_starter" }.currentPlayers)
    assertEquals(initialBalance - room.stakeEtb, vm.uiState.value.userProfile.balanceEtb)
    assertTrue(vm.uiState.value.notificationMessage?.contains("Joined") == true)

    // Re-entering and tapping Join Game again does NOT double-charge or double-count
    vm.enterTicketSelection(vm.uiState.value.rooms.first { it.id == "mini_starter" })
    assertTrue(vm.uiState.value.isSelectedRoomJoined)
    vm.joinRoomGame()
    assertEquals(com.example.viewmodel.PlayView.LOBBY, vm.uiState.value.playView)
    assertEquals(1, vm.uiState.value.rooms.first { it.id == "mini_starter" }.currentPlayers)
    assertEquals(initialBalance - room.stakeEtb, vm.uiState.value.userProfile.balanceEtb)

    // Leaving before countdown completes refunds stake and decrements count
    vm.leaveJoinedRoom("mini_starter")
    assertFalse(vm.uiState.value.joinedRoomIds.contains("mini_starter"))
    assertEquals(0, vm.uiState.value.rooms.first { it.id == "mini_starter" }.currentPlayers)
    assertEquals(initialBalance, vm.uiState.value.userProfile.balanceEtb)
  }

  @Test
  fun `three minute countdown auto-starts on lobby only when two or more players joined`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = BingoViewModel(app)

    val room = vm.uiState.value.rooms.first { it.id == "mini_starter" }
    val initialBalance = vm.uiState.value.userProfile.balanceEtb

    vm.enterTicketSelection(room)
    vm.joinRoomGame()
    assertEquals(com.example.viewmodel.PlayView.LOBBY, vm.uiState.value.playView)
    val balanceAfterJoin = vm.uiState.value.userProfile.balanceEtb
    assertEquals(initialBalance - room.stakeEtb, balanceAfterJoin)

    // Case 1: Only 1 player joined when 3-minute countdown ends -> reset to 180s and stay in LOBBY
    vm.setRoomStateForTesting("mini_starter", currentPlayers = 1, countdownSeconds = 1)
    vm.processCountdownTick()
    assertEquals(com.example.viewmodel.PlayView.LOBBY, vm.uiState.value.playView)
    assertTrue(vm.uiState.value.joinedRoomIds.contains("mini_starter"))
    assertEquals(180, vm.uiState.value.rooms.first { it.id == "mini_starter" }.countdownSeconds)

    // Case 2: 2+ players joined when 3-minute countdown ends -> auto-start GAMEPLAY without double charge
    vm.setRoomStateForTesting("mini_starter", currentPlayers = 2, countdownSeconds = 1)
    vm.processCountdownTick()
    assertEquals(com.example.viewmodel.PlayView.GAMEPLAY, vm.uiState.value.playView)
    assertTrue(vm.uiState.value.isCalling)
    assertEquals(balanceAfterJoin, vm.uiState.value.userProfile.balanceEtb)
  }

  @Test
  fun `cloud lobby snapshot counts two real players across devices and syncs 3-minute countdown`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = BingoViewModel(app)

    val nowMs = 1_700_000_100_000L
    val startedAt = com.google.firebase.Timestamp(java.util.Date(nowMs - 60_000L)) // 60s elapsed -> 120s remaining

    val cloudRoom = com.example.data.FirestoreGameRoom(
      id = "mini_starter",
      name = "Mini Starter",
      stakeEtb = 10L,
      currentPlayers = 2L,
      countdownSeconds = 180L,
      countdownStartedAt = startedAt,
      status = "COUNTDOWN"
    )
    val players = listOf(
      com.example.data.FirestoreRoomPlayer(
        userId = "uid_player_1",
        deviceId = "dev_phone_1",
        name = "Player One",
        ticketNumber = 71L,
        joinedAt = com.google.firebase.Timestamp(java.util.Date(nowMs - 55_000L))
      ),
      com.example.data.FirestoreRoomPlayer(
        userId = "uid_player_2",
        deviceId = "dev_tablet_2",
        name = "Player Two",
        ticketNumber = 77L,
        joinedAt = com.google.firebase.Timestamp(java.util.Date(nowMs - 10_000L))
      )
    )

    vm.applyCloudLobbySnapshot(
      firestoreRooms = listOf(cloudRoom),
      playersByRoom = mapOf("mini_starter" to players),
      currentUserId = "uid_player_1",
      currentDeviceId = "dev_phone_1",
      nowEpochMs = nowMs
    )

    val updatedMini = vm.uiState.value.rooms.first { it.id == "mini_starter" }
    assertEquals(2, updatedMini.currentPlayers)
    assertEquals(20, updatedMini.totalPoolEtb)
    assertEquals(16, updatedMini.prizeEtb)
    assertEquals(4, updatedMini.houseCommissionEtb)
    assertEquals(120, updatedMini.countdownSeconds)
    assertTrue(vm.uiState.value.joinedRoomIds.contains("mini_starter"))
  }

  @Test
  fun `phone and pin login, create account, remember me, and reset pin validate properly`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = BingoViewModel(app)

    // Invalid short phone rejected
    assertFalse(vm.prepareLoginWithPhonePin("123", "1234", rememberMe = true))
    assertNotNull(vm.uiState.value.authError)

    // Valid account registration saves phone and PIN hash
    assertTrue(
      vm.prepareCreateAccount(
        fullName = "Dawit Bekele",
        phone = "0911223344",
        city = "Addis Ababa",
        pin = "4567",
        promoCode = "YEM100"
      )
    )
    assertEquals("0911223344", vm.getRememberedPhone())

    // Wrong PIN rejected
    assertFalse(vm.prepareLoginWithPhonePin("+251911223344", "9999", rememberMe = true))
    assertTrue(vm.uiState.value.authError?.contains("Incorrect") == true)

    // Matching PIN accepted with rememberMe = false clears remembered phone & flag
    assertTrue(vm.prepareLoginWithPhonePin("+251911223344", "4567", rememberMe = false))
    assertEquals("", vm.getRememberedPhone())
    assertFalse(vm.isRememberMeDefault())

    // Reset PIN updates stored hash
    val resetErr = vm.resetSecurityPin("0911223344", "8899")
    assertEquals(null, resetErr)
    assertTrue(vm.prepareLoginWithPhonePin("+251911223344", "8899", rememberMe = true))
    assertTrue(vm.isRememberMeDefault())

    // prepareDirectGoogleSignIn clears stale authError
    vm.setAuthError("Some error")
    vm.prepareDirectGoogleSignIn()
    assertEquals(null, vm.uiState.value.authError)
  }

  @Test
  fun `deposit uses account holder Yemane and account number 0939383425 for telebirr and cbe birr`() {
    assertEquals("Yemane", com.example.ui.components.DEPOSIT_ACCOUNT_HOLDER)
    assertEquals("0939383425", com.example.ui.components.DEPOSIT_ACCOUNT_NUMBER)

    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = BingoViewModel(app)
    val startBal = vm.uiState.value.userProfile.balanceEtb

    vm.confirmDeposit(100, com.example.model.PaymentMethod.TELEBIRR, "0911223344")
    assertEquals(startBal + 100, vm.uiState.value.userProfile.balanceEtb)
    assertTrue(vm.uiState.value.transactions.first().title.contains("Yemane"))
    assertTrue(vm.uiState.value.transactions.first().title.contains("0939383425"))

    vm.confirmDeposit(200, com.example.model.PaymentMethod.CBE_BIRR, "0911223344")
    assertEquals(startBal + 300, vm.uiState.value.userProfile.balanceEtb)
    assertTrue(vm.uiState.value.transactions.first().title.contains("Yemane"))
    assertTrue(vm.uiState.value.transactions.first().title.contains("0939383425"))
  }
}
