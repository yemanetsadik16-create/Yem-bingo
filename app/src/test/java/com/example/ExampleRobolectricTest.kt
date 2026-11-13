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
}
