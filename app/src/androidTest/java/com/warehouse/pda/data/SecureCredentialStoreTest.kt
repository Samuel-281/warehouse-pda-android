package com.warehouse.pda.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SecureCredentialStoreTest {
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
  private lateinit var store: SecureCredentialStore

  @Before
  fun setUp() {
    preferences.edit().clear().commit()
    store = SecureCredentialStore(preferences, Gson())
  }

  @After
  fun tearDown() {
    store.clear()
  }

  @Test
  fun savesCredentialsAsCiphertextAndRestoresThem() {
    assertTrue(store.save(SavedCredentials("warehouse_admin", "correct-password")))

    assertEquals("warehouse_admin", store.load()?.username)
    assertEquals("correct-password", store.load()?.password)
    assertFalse(preferences.contains("saved_username"))
    assertFalse(preferences.contains("saved_password"))
    assertTrue(preferences.contains("credentials_ciphertext"))
  }

  @Test
  fun migratesLegacyPlaintextAndDeletesOldKeys() {
    store.clear()
    preferences.edit()
      .putString("saved_username", "legacy_user")
      .putString("saved_password", "legacy_password")
      .commit()

    val migratedStore = SecureCredentialStore(preferences, Gson())

    assertEquals("legacy_user", migratedStore.load()?.username)
    assertEquals("legacy_password", migratedStore.load()?.password)
    assertFalse(preferences.contains("saved_username"))
    assertFalse(preferences.contains("saved_password"))
    assertTrue(preferences.contains("credentials_ciphertext"))
  }

  private companion object {
    const val PREFERENCES_NAME = "secure_credential_store_test"
  }
}
