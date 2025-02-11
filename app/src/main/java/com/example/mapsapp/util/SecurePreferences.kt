package com.example.mapsapp.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SecurePreferences(context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val sharedPreferences = EncryptedSharedPreferences.create(
        "secure_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveCredentials(email: String, password: String) {
        sharedPreferences.edit().apply {
            putString("EMAIL", email)
            putString("PASSWORD", password)
            putBoolean("STAY_SIGNED_IN", true)
            apply()
        }
    }

    fun getEmail(): String? {
        return sharedPreferences.getString("EMAIL", null)
    }

    fun getPassword(): String? {
        return sharedPreferences.getString("PASSWORD", null)
    }

    fun clearCredentials() {
        sharedPreferences.edit().apply {
            remove("EMAIL")
            remove("PASSWORD")
            putBoolean("STAY_SIGNED_IN", false)
            apply()
        }
    }

    fun shouldStaySignedIn(): Boolean {
        return sharedPreferences.getBoolean("STAY_SIGNED_IN", false)
    }
}