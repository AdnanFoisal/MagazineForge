package com.magazineforge.app.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStorage(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "magboy_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveApiKey(key: String) {
        sharedPreferences.edit().putString("gemini_api_key", key).apply()
    }

    fun getApiKey(): String? {
        return sharedPreferences.getString("gemini_api_key", null)
    }

    fun clearApiKey() {
        sharedPreferences.edit().remove("gemini_api_key").apply()
    }

    fun saveBackupApiKey(key: String) {
        sharedPreferences.edit().putString("gemini_backup_api_key", key).apply()
    }

    fun getBackupApiKey(): String? {
        return sharedPreferences.getString("gemini_backup_api_key", null)
    }

    fun clearBackupApiKey() {
        sharedPreferences.edit().remove("gemini_backup_api_key").apply()
    }

    fun saveTertiaryApiKey(key: String) {
        sharedPreferences.edit().putString("gemini_tertiary_api_key", key).apply()
    }

    fun getTertiaryApiKey(): String? {
        return sharedPreferences.getString("gemini_tertiary_api_key", null)
    }

    fun clearTertiaryApiKey() {
        sharedPreferences.edit().remove("gemini_tertiary_api_key").apply()
    }
    
    fun saveThemeId(themeId: String) {
        sharedPreferences.edit().putString("active_theme_id", themeId).apply()
    }
    
    fun getThemeId(): String? {
        return sharedPreferences.getString("active_theme_id", null)
    }
}
