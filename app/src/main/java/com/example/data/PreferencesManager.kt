package com.example.data

import android.content.Context
import android.content.SharedPreferences
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

data class AppPreferences(
    val themeMode: String = "dark", // "dark", "light", "system"
    val accentColor: String = "cyan", // "cyan", "violet", "emerald", "pink", "amber"
    val aiCreativity: String = "balanced", // "creative", "balanced", "precise"
    val defaultAspectRatio: String = "1:1",
    val defaultVideoQuality: String = "1080p",
    val hapticFeedback: Boolean = true,
    val streamResponses: Boolean = true
)

class PreferencesManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nova_ai_preferences", Context.MODE_PRIVATE)

    private val _preferences = MutableStateFlow(loadPreferences())
    val preferences: StateFlow<AppPreferences> = _preferences.asStateFlow()

    private fun loadPreferences(): AppPreferences {
        return AppPreferences(
            themeMode = prefs.getString("theme_mode", "dark") ?: "dark",
            accentColor = prefs.getString("accent_color", "cyan") ?: "cyan",
            aiCreativity = prefs.getString("ai_creativity", "balanced") ?: "balanced",
            defaultAspectRatio = prefs.getString("default_aspect_ratio", "1:1") ?: "1:1",
            defaultVideoQuality = prefs.getString("default_video_quality", "1080p") ?: "1080p",
            hapticFeedback = prefs.getBoolean("haptic_feedback", true),
            streamResponses = prefs.getBoolean("stream_responses", true)
        )
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _preferences.value = _preferences.value.copy(themeMode = mode)
    }

    fun setAccentColor(color: String) {
        prefs.edit().putString("accent_color", color).apply()
        _preferences.value = _preferences.value.copy(accentColor = color)
    }

    fun setAiCreativity(creativity: String) {
        prefs.edit().putString("ai_creativity", creativity).apply()
        _preferences.value = _preferences.value.copy(aiCreativity = creativity)
    }

    fun setDefaultAspectRatio(ratio: String) {
        prefs.edit().putString("default_aspect_ratio", ratio).apply()
        _preferences.value = _preferences.value.copy(defaultAspectRatio = ratio)
    }

    fun setDefaultVideoQuality(quality: String) {
        prefs.edit().putString("default_video_quality", quality).apply()
        _preferences.value = _preferences.value.copy(defaultVideoQuality = quality)
    }

    fun setHapticFeedback(enabled: Boolean) {
        prefs.edit().putBoolean("haptic_feedback", enabled).apply()
        _preferences.value = _preferences.value.copy(hapticFeedback = enabled)
    }

    fun setStreamResponses(enabled: Boolean) {
        prefs.edit().putBoolean("stream_responses", enabled).apply()
        _preferences.value = _preferences.value.copy(streamResponses = enabled)
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
        _preferences.value = AppPreferences()
    }

    suspend fun calculateCacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        var size = 0L
        try {
            size += getFolderSize(context.cacheDir)
            context.externalCacheDir?.let { size += getFolderSize(it) }
            context.codeCacheDir?.let { size += getFolderSize(it) }
        } catch (_: Exception) {}
        size
    }

    @OptIn(ExperimentalCoilApi::class)
    suspend fun clearCache(): Long = withContext(Dispatchers.IO) {
        var clearedBytes = 0L
        try {
            clearedBytes += getFolderSize(context.cacheDir)
            deleteDir(context.cacheDir)

            context.externalCacheDir?.let {
                clearedBytes += getFolderSize(it)
                deleteDir(it)
            }

            // Clear Coil image memory and disk cache
            try {
                val imageLoader = Coil.imageLoader(context)
                imageLoader.memoryCache?.clear()
                imageLoader.diskCache?.clear()
            } catch (_: Exception) {}
        } catch (_: Exception) {}
        clearedBytes
    }

    private fun getFolderSize(file: File?): Long {
        if (file == null || !file.exists()) return 0L
        var size: Long = 0
        file.listFiles()?.forEach { child ->
            size += if (child.isDirectory) getFolderSize(child) else child.length()
        }
        return size
    }

    private fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list() ?: return false
            for (child in children) {
                val success = deleteDir(File(dir, child))
                if (!success) {
                    return false
                }
            }
            return dir.delete()
        } else if (dir != null && dir.isFile) {
            return dir.delete()
        }
        return false
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0.00 ميغابايت"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(java.util.Locale.US, "%.2f جيجابايت", gb)
                mb >= 1.0 -> String.format(java.util.Locale.US, "%.2f ميغابايت", mb)
                kb >= 1.0 -> String.format(java.util.Locale.US, "%.1f كيلوبايت", kb)
                else -> "$bytes بايت"
            }
        }
    }
}
