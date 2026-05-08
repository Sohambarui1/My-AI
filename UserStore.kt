package com.example.myai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONObject

object UserStore {
    private lateinit var encryptedPrefs: SharedPreferences
    private const val PREF_NAME = "secure_user_store"
    private const val KEY_USERS = "encrypted_users"

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

    private fun getUsers(): MutableMap<String, String> {
        val usersJson = encryptedPrefs.getString(KEY_USERS, "{}") ?: "{}"
        return try {
            val jsonObject = JSONObject(usersJson)
            val map = mutableMapOf<String, String>()
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = jsonObject.getString(key)
            }
            map
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    private fun saveUsers(users: Map<String, String>) {
        val jsonObject = JSONObject(users)
        encryptedPrefs.edit().putString(KEY_USERS, jsonObject.toString()).apply()
    }

    fun login(email: String, password: String): Boolean {
        val users = getUsers()
        return users[email] == password
    }

    fun signUp(email: String, password: String): Boolean {
        val users = getUsers()
        return if (users.containsKey(email)) false else {
            users[email] = password
            saveUsers(users)
            true
        }
    }

    fun clearAllUsers() {
        encryptedPrefs.edit().remove(KEY_USERS).apply()
    }
}

