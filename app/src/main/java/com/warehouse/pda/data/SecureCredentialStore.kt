package com.warehouse.pda.data

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.gson.Gson
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class SavedCredentials(
  val username: String,
  val password: String
)

class SecureCredentialStore(
  private val preferences: SharedPreferences,
  private val gson: Gson
) {
  init {
    migrateLegacyPlaintext()
  }

  fun load(): SavedCredentials? {
    val encrypted = preferences.getString(KEY_CIPHERTEXT, null) ?: return null
    val iv = preferences.getString(KEY_IV, null) ?: return null
    return runCatching {
      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(
        Cipher.DECRYPT_MODE,
        getOrCreateKey(),
        GCMParameterSpec(TAG_LENGTH_BITS, Base64.decode(iv, Base64.NO_WRAP))
      )
      val cleartext = cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)).decodeToString()
      gson.fromJson(cleartext, SavedCredentials::class.java)
    }.getOrElse {
      clear()
      null
    }
  }

  fun save(credentials: SavedCredentials): Boolean {
    return runCatching {
      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
      val encrypted = cipher.doFinal(gson.toJson(credentials).encodeToByteArray())
      preferences.edit()
        .putString(KEY_CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
        .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
        .remove(LEGACY_USERNAME)
        .remove(LEGACY_PASSWORD)
        .apply()
      true
    }.getOrElse {
      clear()
      false
    }
  }

  fun clear() {
    preferences.edit()
      .remove(KEY_CIPHERTEXT)
      .remove(KEY_IV)
      .remove(LEGACY_USERNAME)
      .remove(LEGACY_PASSWORD)
      .apply()
  }

  private fun migrateLegacyPlaintext() {
    if (preferences.contains(KEY_CIPHERTEXT)) {
      preferences.edit().remove(LEGACY_USERNAME).remove(LEGACY_PASSWORD).apply()
      return
    }
    val username = preferences.getString(LEGACY_USERNAME, null)
    val password = preferences.getString(LEGACY_PASSWORD, null)
    if (username.isNullOrBlank() || password.isNullOrBlank()) {
      preferences.edit().remove(LEGACY_USERNAME).remove(LEGACY_PASSWORD).apply()
      return
    }
    save(SavedCredentials(username = username, password = password))
  }

  private fun getOrCreateKey(): SecretKey {
    val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
    val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
    keyGenerator.init(
      KeyGenParameterSpec.Builder(
        KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
      )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setRandomizedEncryptionRequired(true)
        .build()
    )
    return keyGenerator.generateKey()
  }

  private companion object {
    const val ANDROID_KEYSTORE = "AndroidKeyStore"
    const val KEY_ALIAS = "warehouse_pda_credentials_v1"
    const val TRANSFORMATION = "AES/GCM/NoPadding"
    const val TAG_LENGTH_BITS = 128
    const val KEY_CIPHERTEXT = "credentials_ciphertext"
    const val KEY_IV = "credentials_iv"
    const val LEGACY_USERNAME = "saved_username"
    const val LEGACY_PASSWORD = "saved_password"
  }
}
