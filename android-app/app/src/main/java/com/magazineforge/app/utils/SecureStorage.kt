package com.magazineforge.app.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStorage(context: Context) {
    companion object {
        /** Settings' Visual Style is not overriding the contract. */
        const val VISUAL_REGISTER_AUTO = "auto"

        /** Every value [saveVisualRegister] will persist. */
        val VISUAL_REGISTERS = listOf(VISUAL_REGISTER_AUTO, "editorial", "modern", "technical")
    }

    private val sharedPreferences: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "magboy_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Clear corrupted encrypted prefs file if Android Keystore key was invalidated/rotated
        context.deleteSharedPreferences("magboy_secure_prefs")
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "magboy_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e2: Exception) {
            context.getSharedPreferences("magboy_plain_prefs", Context.MODE_PRIVATE)
        }
    }

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

    // User-supplied stock-photo keys. Verified in Settings before saving, so a
    // bad paste never silently blanks a magazine's images. Stored blank when
    // the user clears a box — the deployment's own keys take over.

    fun savePixabayKey(key: String) {
        sharedPreferences.edit().putString("pixabay_key", key.trim()).apply()
    }

    fun getPixabayKey(): String {
        return sharedPreferences.getString("pixabay_key", "")?.trim() ?: ""
    }

    fun clearPixabayKey() {
        sharedPreferences.edit().remove("pixabay_key").apply()
    }

    fun savePexelsKey(key: String) {
        sharedPreferences.edit().putString("pexels_key", key.trim()).apply()
    }

    fun getPexelsKey(): String {
        return sharedPreferences.getString("pexels_key", "")?.trim() ?: ""
    }

    fun clearPexelsKey() {
        sharedPreferences.edit().remove("pexels_key").apply()
    }

    fun saveThemeId(themeId: String) {
        sharedPreferences.edit().putString("active_theme_id", themeId).apply()
    }
    
    fun getThemeId(): String? {
        return sharedPreferences.getString("active_theme_id", null)
    }

    /**
     * Visual Style override for the Intent Gate. "auto" (the default, and what
     * an unknown or missing value resolves to) means the confirmed contract's
     * own visual_register decides the cover variant; anything else overrides it.
     * Always stored lowercase so comparisons never need to case-fold.
     */
    fun saveVisualRegister(register: String) {
        val normalized = register.trim().lowercase()
        val safe = if (normalized in VISUAL_REGISTERS) normalized else VISUAL_REGISTER_AUTO
        sharedPreferences.edit().putString("visual_register", safe).apply()
    }

    fun getVisualRegister(): String {
        val stored = sharedPreferences.getString("visual_register", VISUAL_REGISTER_AUTO)?.lowercase()
        return if (stored != null && stored in VISUAL_REGISTERS) stored else VISUAL_REGISTER_AUTO
    }

    fun saveUserName(name: String) {
        val trimmed = name.trim().take(7)
        sharedPreferences.edit().putString("user_name", if (trimmed.isEmpty()) "Maker" else trimmed).apply()
    }

    fun getUserName(): String {
        val name = sharedPreferences.getString("user_name", "Maker")
        return if (name.isNullOrBlank()) "Maker" else name.take(7)
    }
}
