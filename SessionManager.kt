package com.example.myai

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

object SessionManager {
    private const val PREF_NAME = "secure_user_session"
    private const val KEY_EMAIL = "logged_in_email"
    private const val KEY_PASSWORD = "encrypted_password"
    private const val KEY_REMEMBER_ME = "remember_me"

    // For encrypted preferences
    private lateinit var encryptedPrefs: SharedPreferences

    fun initialize(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveCredentials(context: Context, email: String, password: String, rememberMe: Boolean) {
        if (!this::encryptedPrefs.isInitialized) {
            initialize(context)
        }

        encryptedPrefs.edit().apply {
            putString(KEY_EMAIL, email)
            if (rememberMe) {
                putString(KEY_PASSWORD, encryptPassword(context, password))
            } else {
                remove(KEY_PASSWORD)
            }
            putBoolean(KEY_REMEMBER_ME, rememberMe)
            apply()
        }
    }

    fun getSavedCredentials(context: Context): Pair<String?, String?> {
        if (!this::encryptedPrefs.isInitialized) {
            initialize(context)
        }

        val email = encryptedPrefs.getString(KEY_EMAIL, null)
        val encryptedPassword = encryptedPrefs.getString(KEY_PASSWORD, null)
        val password = encryptedPassword?.let { decryptPassword(context, it) }

        return Pair(email, password)
    }

    fun shouldRememberMe(): Boolean {
        return encryptedPrefs.getBoolean(KEY_REMEMBER_ME, false)
    }

    // In SessionManager.kt
    @Synchronized
    fun clearSession(context: Context) {
        if (!this::encryptedPrefs.isInitialized) {
            initialize(context)
        }
        encryptedPrefs.edit().apply {
            remove(KEY_EMAIL)
            remove(KEY_PASSWORD)
            remove(KEY_REMEMBER_ME)
            apply()
        }
    }

    fun isLoggedIn(context: Context): Boolean {
        if (!this::encryptedPrefs.isInitialized) {
            initialize(context)
        }
        return encryptedPrefs.getString(KEY_EMAIL, null) != null &&
                encryptedPrefs.getBoolean(KEY_REMEMBER_ME, false)
    }

    private fun encryptPassword(context: Context, password: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = getOrCreateSecretKey(context)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(password.toByteArray(Charsets.UTF_8))

        // Store IV with encrypted data
        return Base64.encodeToString(iv + encrypted, Base64.DEFAULT)
    }

    private fun decryptPassword(context: Context, encryptedData: String): String? {
        try {
            val decoded = Base64.decode(encryptedData, Base64.DEFAULT)
            val iv = decoded.copyOfRange(0, 12) // IV is 12 bytes for GCM
            val encrypted = decoded.copyOfRange(12, decoded.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val key = getOrCreateSecretKey(context)
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

            return String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getOrCreateSecretKey(context: Context): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }

        if (!keyStore.containsAlias("password_encryption_key")) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )

            val keyGenSpec = KeyGenParameterSpec.Builder(
                "password_encryption_key",
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(false)
                .build()

            keyGenerator.init(keyGenSpec)
            keyGenerator.generateKey()
        }

        return keyStore.getKey("password_encryption_key", null) as SecretKey
    }
}