package com.example.util

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.example.ui.AiStudioTab
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Manages Firebase Analytics event tracking across Chat, Image, and Video features.
 * Safely handles environments where Google Play Services or Firebase configurations
 * might be offline or in test mode.
 */
class AnalyticsManager(context: Context) {
    private val appContext = context.applicationContext
    private var firebaseAnalytics: FirebaseAnalytics? = null

    init {
        try {
            val app = if (FirebaseApp.getApps(appContext).isEmpty()) {
                FirebaseApp.initializeApp(appContext)
            } else {
                FirebaseApp.getInstance()
            }
            if (app != null) {
                firebaseAnalytics = FirebaseAnalytics.getInstance(appContext)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Analytics initialization skipped or offline: ${e.message}")
        }
    }

    /**
     * Safely log a custom event to Firebase Analytics
     */
    fun logEvent(eventName: String, params: (Bundle.() -> Unit)? = null) {
        try {
            val bundle = if (params != null) Bundle().apply(params) else null
            firebaseAnalytics?.logEvent(eventName, bundle)
            Log.d(TAG, "Analytics Event: $eventName -> $bundle")
        } catch (e: Exception) {
            Log.w(TAG, "Could not log event $eventName: ${e.message}")
        }
    }

    /**
     * Track user switching between Chat, Image, and Video tabs
     */
    fun trackTabSelected(tab: AiStudioTab) {
        val tabId = when (tab) {
            AiStudioTab.CHAT -> "chat_tab"
            AiStudioTab.IMAGE -> "image_tab"
            AiStudioTab.VIDEO -> "video_tab"
        }

        val tabTitle = when (tab) {
            AiStudioTab.CHAT -> "المحادثة الذكية"
            AiStudioTab.IMAGE -> "توليد الصور"
            AiStudioTab.VIDEO -> "توليد الفيديو"
        }

        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, tabId)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "AiStudioMainScreen")
        }

        logEvent(FirebaseAnalytics.Event.SELECT_CONTENT) {
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, "navigation_tab")
            putString(FirebaseAnalytics.Param.ITEM_ID, tabId)
            putString(FirebaseAnalytics.Param.ITEM_NAME, tabTitle)
        }

        logEvent("tab_interaction") {
            putString("tab_name", tabId)
            putLong("timestamp", System.currentTimeMillis())
        }
    }

    // --- Chat Tab Interactions ---

    fun trackChatMessageSent(messageLength: Int, hasImage: Boolean, isVoice: Boolean) {
        logEvent("chat_message_sent") {
            putInt("message_length", messageLength)
            putBoolean("has_image", hasImage)
            putString("input_type", if (isVoice) "voice" else "text")
            putString("model_engine", "gemini_3.5_flash")
        }
    }

    fun trackVoiceAssistantModeOpened() {
        logEvent("chat_voice_mode_opened") {
            putLong("timestamp", System.currentTimeMillis())
        }
    }

    fun trackVoiceDictationUsed(success: Boolean) {
        logEvent("chat_voice_dictation_used") {
            putBoolean("success", success)
        }
    }

    fun trackChatExported(format: String, messageCount: Int) {
        logEvent("chat_exported") {
            putString("format", format)
            putInt("message_count", messageCount)
        }
    }

    fun trackChatCleared() {
        logEvent("chat_history_cleared")
    }

    // --- Image Tab Interactions ---

    fun trackImageGenerated(
        promptLength: Int,
        style: String,
        aspectRatio: String,
        isEnhanced: Boolean
    ) {
        logEvent("image_generated") {
            putInt("prompt_length", promptLength)
            putString("style", style)
            putString("aspect_ratio", aspectRatio)
            putBoolean("is_enhanced", isEnhanced)
            putString("engine", "flux_pro")
        }
    }

    fun trackImagePromptEnhanced() {
        logEvent("image_prompt_enhanced") {
            putString("engine", "gemini_prompt_optimizer")
        }
    }

    fun trackImageSaved() {
        logEvent("image_saved_to_gallery")
    }

    fun trackImageShared() {
        logEvent("image_shared")
    }

    // --- Video Tab Interactions ---

    fun trackVideoGenerated(
        promptLength: Int,
        style: String,
        durationSeconds: Int,
        quality: String
    ) {
        logEvent("video_generated") {
            putInt("prompt_length", promptLength)
            putString("style", style)
            putInt("duration_seconds", durationSeconds)
            putString("quality", quality)
            putString("engine", "veo_cinematic")
        }
    }

    fun trackVideoPlayed(durationSeconds: Int) {
        logEvent("video_played") {
            putInt("duration_seconds", durationSeconds)
        }
    }

    fun trackVideoShared() {
        logEvent("video_shared")
    }

    fun trackVideoDownloaded() {
        logEvent("video_downloaded")
    }

    companion object {
        private const val TAG = "AnalyticsManager"
    }
}
