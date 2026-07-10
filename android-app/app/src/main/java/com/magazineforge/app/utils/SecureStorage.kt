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

    fun saveLiteLLMUrl(url: String) {
        sharedPreferences.edit().putString("litellm_url", url).apply()
    }

    fun getLiteLLMUrl(): String? {
        return sharedPreferences.getString("litellm_url", null)
    }

    fun clearLiteLLMUrl() {
        sharedPreferences.edit().remove("litellm_url").apply()
    }

    fun saveLiteLLMKey(key: String) {
        sharedPreferences.edit().putString("litellm_key", key).apply()
    }

    fun getLiteLLMKey(): String? {
        return sharedPreferences.getString("litellm_key", null)
    }

    fun clearLiteLLMKey() {
        sharedPreferences.edit().remove("litellm_key").apply()
    }
    
    fun saveThemeId(themeId: String) {
        sharedPreferences.edit().putString("active_theme_id", themeId).apply()
    }
    
    fun getThemeId(): String? {
        return sharedPreferences.getString("active_theme_id", null)
    }
}
