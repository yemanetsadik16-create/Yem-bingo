package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.viewmodel.BingoViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
}
