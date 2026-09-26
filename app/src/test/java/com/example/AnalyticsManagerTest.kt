package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.AiStudioTab
import com.example.util.AnalyticsManager
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AnalyticsManagerTest {

    private lateinit var context: Context
    private lateinit var analyticsManager: AnalyticsManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        analyticsManager = AnalyticsManager(context)
    }

    @Test
    fun testAnalyticsManagerInitialization() {
        assertNotNull(analyticsManager)
    }

    @Test
    fun testTabTrackingDoesNotThrow() {
        // Verify tracking all tabs works seamlessly
        analyticsManager.trackTabSelected(AiStudioTab.CHAT)
        analyticsManager.trackTabSelected(AiStudioTab.IMAGE)
        analyticsManager.trackTabSelected(AiStudioTab.VIDEO)
    }

    @Test
    fun testChatInteractionsTracking() {
        analyticsManager.trackChatMessageSent(
            messageLength = 42,
            hasImage = true,
            isVoice = false
        )
        analyticsManager.trackVoiceAssistantModeOpened()
        analyticsManager.trackVoiceDictationUsed(success = true)
        analyticsManager.trackChatExported("txt", messageCount = 10)
        analyticsManager.trackChatCleared()
    }

    @Test
    fun testImageInteractionsTracking() {
        analyticsManager.trackImageGenerated(
            promptLength = 35,
            style = "Realistic",
            aspectRatio = "1:1",
            isEnhanced = true
        )
        analyticsManager.trackImagePromptEnhanced()
        analyticsManager.trackImageSaved()
        analyticsManager.trackImageShared()
    }

    @Test
    fun testVideoInteractionsTracking() {
        analyticsManager.trackVideoGenerated(
            promptLength = 50,
            style = "Cinematic",
            durationSeconds = 15,
            quality = "1080p"
        )
        analyticsManager.trackVideoPlayed(durationSeconds = 15)
        analyticsManager.trackVideoShared()
        analyticsManager.trackVideoDownloaded()
    }
}
